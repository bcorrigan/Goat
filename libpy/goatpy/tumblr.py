from goat.core import KVStore
from goat.util import Passwords

from goatpy.util import get_page
import oauth2 as oauth # yes this oauth v1 not v2
import pickle
import random
import sets
import simplejson as json
import re
import time
import urllib
import urllib2

# this module posts images and videos to tumblr.  it picks these up when
# someone pastes them directly in the channel or when someone makes use of a
# search module like gis/yis/bis.
# TODO: implement bis, yis searches
# TODO: what about youtube search?
# TODO: prevent double-posting
# TODO: use tumblr until quota expires then fall back to imgur?

GENERATOR = "goatbot"

blog_brag = [
    "Have you guys seen my blog at %s",
    "Haha good one!  I posted that to my blog at %s",
    "That belongs on my blog at %s",
    "I have something related to that on my blog at %s",
    "You know, that reminds me of my blog at %s",
    "My blog -- %s -- is filled with just that kind of thing.",
]

# load the set of stop words -- probably not safe to do this way but what's
# the worst that could happen?
stop_words_file = "resources/stop_words"
stop_words = None

def get_stop_words():
    global stop_words
    if stop_words is not None:
        return stop_words

    try:
        f = file(stop_words_file, "r")
        words = f.readlines()
        f.close()
    except IOError, e:
        print "error reading stopwords!"
        return None

    s = dict()
    for word in words:
        s[word.rstrip()] = True
    stop_words = s
    return stop_words

# globals, mwa-ha-ha
errored = False

def post_to_tumblr(url, *args, **kwargs):
    url_store = KVStore.getCustomStore("tumblr_urls")
    if url_store.has(url):
        return
    else:
        url_store.save(url, time.time())

    msg = post_to_tumblr_direct(url, *args, **kwargs)
    if msg is not None and kwargs["post_type"] == "photo":
        try:
            imgur_url = post_to_imgur(url, kwargs["caption"])
        except KeyError:
            imgur_url = post_to_imgur(url)
        if imgur_url is None:
            return
        kwargs["post_type"] = "photo_embed"
        msg = post_to_tumblr_direct(imgur_url, *args, **kwargs)
    return msg

def post_to_tumblr_direct(url, post_type="photo", caption=None, link=None, tags=None):
    # oauth stuff -- I should probably cache this.
    pwds = Passwords()
    consumer_key = pwds.getPassword('tumblr.consumerKey')
    consumer_secret = pwds.getPassword('tumblr.consumerSecret')
    consumer = oauth.Consumer(consumer_key, consumer_secret)

    access_key = pwds.getPassword('tumblr.accessKey')
    access_secret = pwds.getPassword('tumblr.accessSecret')
    token = oauth.Token(access_key, access_secret)

    oauth_client = oauth.Client(consumer, token)

    # set up request
    request_url = 'http://api.tumblr.com/v2/blog/goat-blog.tumblr.com/post'
    method = 'POST'

    params = { }
    params['type'] = post_type
    if tags is not None:
        params['tags'] = ",".join(tags)

    if post_type == "photo":
        params['source'] = url
        if caption is not None:
            params['caption'] = caption
        if link is not None:
            params['link'] = link

    elif post_type == "photo_embed":
        if caption is not None:
            params['title'] = caption
        params['type'] = 'text'
        if link is not None:
            params['body'] = '<a href="%s"><img src="%s"></a>' % (link, url)
        else:
            params['body'] = '<img src="%s">' % url

    elif post_type == "video":
        if caption is not None:
            params['title'] = caption
        embed_code = '<iframe width="640" height="480" src="%s" frameborder="0" allowfullscreen></iframe>'
        params['embed'] = embed_code % url

    body = urllib.urlencode(params)

    # gogo!
    global errored
    response, content = oauth_client.request(request_url, method, body)
    if response.status >= 200 and response.status < 300:
        errored = False
        if random.random() < 0.02:
            return random.choice(blog_brag) % 'http://goat-blog.tumblr.com'
    else:
        results = json.loads(content)
        try:
            message = "Tumblr said: %s" % results["response"]["errors"][0]
        except Exception, e:
            message = "Tumblr didn't like that."

        if not errored:
            errored = True
            return message
    set_last_post_time()

def gis_search(search, tags=None, show_search=True):
    params = {
        "v": "1.0",
        "start": "1",   # this can be incremented for more results.
        "q": search,
    }
    url = "https://ajax.googleapis.com/ajax/services/search/images?%s" % (
        urllib.urlencode(params))
    (code, content, resp) = get_page(url)
    if code != 200:
        return "google said %s" % str(code)
    results = json.loads(content)

    try:
        images = [i['unescapedUrl'] for i in results['responseData']['results']]
    except:
        images = None

    if images is not None and len(images) > 0:
        link = None
        if show_search is False:
            search = None
        else:
            link = 'http://images.google.com/images?%s' % urllib.urlencode({
                'safe': 'off',
                'q': search })

        return post_to_tumblr(random.choice(images), caption=search,
            post_type="photo", link=link, tags=tags)

def post_to_imgur(url, title=None):
    imgur_url = None
    pwds = Passwords()
    client_id = pwds.getPassword('imgur.clientId')

    headers = {}
    headers['Authorization'] = "Client-ID %s" %  client_id

    params = {}
    params['image'] = url
    if title is not None:
        params['title'] = title
        params['description'] = title
    code, content, resp = get_page('https://api.imgur.com/3/image', params,
                          headers)
    if code == 200:
        results = json.loads(content)
        try:
            imgur_url = results["data"]["link"]
        except:
            print "Invalid imgur response", content
            pass
    else:
        print "Got weird return code %d from imgur" % code
    return imgur_url

def get_word_seeds():
    store = KVStore.getCustomStore("tumblr")

    word_seeds = store.getOrElse(TUMBLR_WORDS_KEY, None)
    if not word_seeds:
        word_seeds = []
    else:
        word_seeds = pickle.loads(word_seeds)
    return word_seeds

def save_word_seeds(word_seeds):
    store = KVStore.getCustomStore("tumblr")
    store.save(TUMBLR_WORDS_KEY, pickle.dumps(word_seeds))

SEED_LENGTH=12
TUMBLR_WORDS_KEY="tumblrWords"
def feed_random_words(msg):
    word_seeds = get_word_seeds()
    msg = re.sub('[^a-z\s]',  "", msg.lower())
    new_words = msg.split()
    random.shuffle(new_words)

    if len(word_seeds) > SEED_LENGTH:
        word_seeds = word_seeds[:SEED_LENGTH]

    stop_words = get_stop_words()
    if stop_words is None:
        return None

    for word in new_words:
        if len(word) < 4:
            continue
        if word in stop_words:
            continue
        if word not in word_seeds:
            if len(word_seeds) < SEED_LENGTH:
                word_seeds.append(word)
            else:
                word_seeds[random.randint(0, SEED_LENGTH-1)] = word
    save_word_seeds(word_seeds)
    return word_seeds

def get_random_words(count=3):
    word_seeds = get_word_seeds()
    random.shuffle(word_seeds)
    if len(word_seeds) < count:
        return word_seeds
    else:
        return word_seeds[:count]

def get_random_tag():
    random_tags = [
        "bored",
        "restless",
        "antsy",
        "fidgety",
        "tired",
        "sleepy",
        "yawn",
        "tedium",
        "lol",
        "funny",
        "quotes",
        "love",
        "cute",
        "awkward",
        "what",
        "wtf",
        "omg",
    ]
    return random.choice(random_tags)

def get_last_post_time():
    store = KVStore.getCustomStore("tumblr")
    last_post = store.getOrElse("lastPost", 0.0)
    return last_post

def set_last_post_time():
    store = KVStore.getCustomStore("tumblr")
    now = time.time()
    last_post = store.save("lastPost", now)
    return now
