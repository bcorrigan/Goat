from goat.core import KVStore
from goat.util import Passwords

from goatpy.util import get_page
import base64
import oauth2 as oauth # yes this oauth v1 not v2
import pickle
import random
import re
import sets
import simplejson as json
import time
import urllib
import urllib2

# this module posts images and videos to tumblr.

# TODO: implement yis search

###
### Tumblr API
###

def make_tumblr_request(url, params):
    """This is the root of the tumblr api.  Can perform any tumblr operation
by defining request_type and params correctly.
    returns (success, response, result)
"""

    # oauth stuff -- I should probably cache this.
    pwds = Passwords()
    consumer_key = pwds.getPassword('tumblr.consumerKey')
    consumer_secret = pwds.getPassword('tumblr.consumerSecret')
    access_key = pwds.getPassword('tumblr.accessKey')
    access_secret = pwds.getPassword('tumblr.accessSecret')

    if (consumer_key is None or consumer_secret is None or
            access_key is None or access_secret is None):
        print "I tried to make a tumblr but I'm not configured."
        return (None, None, None)

    consumer = oauth.Consumer(consumer_key, consumer_secret)
    token = oauth.Token(access_key, access_secret)
    oauth_client = oauth.Client(consumer, token)

    # set up request
    if url is not None:
        request_url = url
    else:
        request_url = 'http://api.tumblr.com/v2/blog/goat-blog.tumblr.com/%s' % (
            request_type)
    method = 'POST'
    body = urllib.urlencode(params)

    response, content = oauth_client.request(request_url, method, body)
    if response.status >= 200 and response.status < 300:
        success = True
    else:
        success = False

    try:
        results = json.loads(content)["response"]
    except:
        results = None
    return success, response, results

def _format_tumblr_error(response, results):
    """Generate consistent error message from make_tumblr_request response &
results"""
    if response is None:
        return

    try:
        message = "Tumblr said: %s" % results["errors"][0]
    except:
        message = "Tumblr didn't like that."
    return message

def followers():
    """generates a message about the blog's followers"""
    params = {}
    url = 'http://api.tumblr.com/v2/blog/goat-blog.tumblr.com/followers'
    success, response, results = make_tumblr_request(url, params)
    # this will list the first 20 followers.  recently acquired followers
    # seem to be first my testing, so that's really what we want.
    if success:
        # build message
        msg = "I have %s followers. Here are some recent ones: " % (
            results["total_users"])
        msg += ", ".join([i['name'] for i in results["users"]])
        return msg
    else:
        return _format_tumblr_error(response, results)

def dashboard():
    url = 'http://api.tumblr.com/v2/user/dashboard'
    params = {}
    params["reblog_info"] = True
    params["notes_info"] = True
    success, response, results = make_tumblr_request(url, params)
    return results

def post(url, post_type="photo", caption=None, tags=None):
    set_last_post_time()

    params = {}
    params['type'] = post_type
    if post_type == "photo_embed":
      params['type'] = "photo"
    if tags is not None:
        params['tags'] = ",".join(tags)
    if caption is not None:
        params['title'] = caption

    if post_type == "photo":
        params['source'] = url
        if caption is not None:
            params['caption'] = caption
    elif post_type == "photo_embed":
        params['type'] = 'text'
        params['body'] = '<img src="%s">' % url
    elif post_type == "video":
        embed_code = '<iframe width="640" height="480" src="%s" frameborder="0" allowfullscreen></iframe>'
        params['embed'] = embed_code % url

    url = 'http://api.tumblr.com/v2/blog/goat-blog.tumblr.com/post'
    success, response, results = make_tumblr_request(url, params)
    message = None
    if not success:
        message = _format_tumblr_error(response, results)
    return message

def safe_post(url, *args, **kwargs):
    """A posting method that falls back to posting images to imgur if  the
tumblr image quota is exceeded."""
    if cache_url(url):
        return

    msg = post(url, *args, **kwargs)
    if msg is not None:
        print msg
        if "post_type" not in kwargs or kwargs["post_type"] == "photo":
            try:
                imgur_url = post_to_imgur(url, kwargs["caption"])
            except KeyError:
                imgur_url = post_to_imgur(url)
            if imgur_url is None:
                return "couldn't post %s to to imgur or tumblr." % url
            kwargs["post_type"] = "photo_embed"
            msg = post(imgur_url, *args, **kwargs)
    else:
        if random.random() < 0.05:
            msg = get_blog_brag()
    return msg


###
### Higher level functions that make use of the Tumblr API to do things.
###

def post_search(search, tags=None):
    """Post a random image, using the preferred method."""
    bis_search(search, tags=tags)

def gis_search(search, tags=None):
    if cache_search(search):
        return
    params = {
        "v": "1.0",
        "start": "1",   # this can be incremented for more results.
        "q": search,
    }
    url = "https://ajax.googleapis.com/ajax/services/search/images?%s" % (
        urllib.urlencode(params))
    (code, content, resp) = get_page(url)
    if code != 200:
        # google just fails randomly sometimes.  let's just blindly retry
        # once.
        (code, content, resp) = get_page(url)
        if code != 200:
            return "google said %s" % str(resp)
    results = json.loads(content)

    try:
        images = [i['unescapedUrl'] for i in results['responseData']['results']]
    except:
        images = None

    if images is not None and len(images) > 0:
        return safe_post(random.choice(images), caption=search,
            post_type="photo", tags=tags)

def bis_search(search, tags=None):
    if cache_search(search):  # skip repeats
        return
    terms = " ".join(map(lambda q: "+" + q, search.split()))

    queryBingFor = "'%s'" % terms # the apostrophe's required as that is the format the API Url expects.
    quoted_query = urllib.quote(queryBingFor)

    rootURL = "https://api.datamarket.azure.com/Bing/Search/"
    searchURL = rootURL + "Image?Adult=%27Off%27&$format=json&Query=" + quoted_query

    print searchURL
    pwds = Passwords()
    username = pwds.getPassword('microsoft.accountKey')
    accountKey = pwds.getPassword('microsoft.accountKey')
    password_mgr = urllib2.HTTPPasswordMgrWithDefaultRealm()
    password_mgr.add_password(None, searchURL,username,accountKey)

    handler = urllib2.HTTPBasicAuthHandler(password_mgr)
    opener = urllib2.build_opener(handler)
    urllib2.install_opener(opener)
    try:
        resp = urllib2.urlopen(searchURL)
        content = resp.read()
        code = 200
    except urllib2.URLError, e:
        code = str(e)

    if code != 200:
        return "Bing said: %s" % (str(code))
    results = json.loads(content)

    try:
        images = [i['MediaUrl'] for i in results['d']['results']]
    except:
        images = None

    if images is not None and len(images) > 0:
        return safe_post(random.choice(images), caption=search,
            post_type="photo", tags=tags)


###
### Manage a store of recently seen words. (tumblr brain)
###

BRAIN_SIZE=24
TUMBLR_WORDS_KEY="tumblrWords"

stop_words = None   # stop words cached here once loaded
def get_stop_words():
    """Returns the set of stop_words."""
    global stop_words
    if stop_words is not None:
        return stop_words

    s = set()
    try:
        f = file("resources/stop_words", "r")
        words = f.readlines()
        f.close()
    except IOError, e:
        print "error reading stopwords!"
        return s

    for word in words:
        s.add(word.rstrip())
    stop_words = s
    return stop_words

def get_tumblr_store():
    store = KVStore.getCustomStore("tumblr")
    return store

def get_words():
    store = get_tumblr_store()
    words = store.getOrElse(TUMBLR_WORDS_KEY, None)
    if not words:
        words = []
    else:
        words = pickle.loads(words)
    return words

def save_words(words):
    # This adjusts the saved word count if BRAIN_SIZE changes
    if len(words) > BRAIN_SIZE:
        words = words[:BRAIN_SIZE]

    store = get_tumblr_store()
    store.save(TUMBLR_WORDS_KEY, pickle.dumps(words))

def get_random_words(count=3):
    words = get_words()
    random.shuffle(words)
    if len(words) < count:
        return words
    else:
        return words[:count]

def add_words(msg):
    words = get_words()
    stop_words = get_stop_words()
    msg = re.sub('[^a-z\s.]',  "", msg.lower())
    new_words = msg.split()
    random.shuffle(new_words)

    for word in new_words:
        if (len(word) < 4 or word.startswith("http") or "." in word or
            word in words or word in stop_words):
            continue

        if len(words) < BRAIN_SIZE:
            words.append(word)
        else:
            words[random.randint(0, BRAIN_SIZE-1)] = word
    save_words(words)
    return words


###
### Utility, etc.
###

def cache_search(search):
    """Adds a search to the cache.  Returns True if we've seen it before or
False if it's new."""
    tokens = re.sub("[^a-z\s]", "", search.lower()).split()
    key = " ".join(sorted(tokens))

    search_store = KVStore.getCustomStore("tumblr_searches")
    if search_store.has(key):
        return True
    else:
        search_store.save(key, time.time())
    return False

def cache_url(url):
    """Adds a url to the cache.  Returns True if we've seen it before or False
if it's new."""
    url_store = KVStore.getCustomStore("tumblr_urls")
    if url_store.has(url):
        return True
    else:
        url_store.save(url, time.time())
    return False

def get_blog_brag():
    blog_brag = [
        "Have you guys seen my blog at %s",
        "Haha good one!  I posted that to my blog at %s",
        "That belongs on my blog at %s",
        "I have something related to that on my blog at %s",
        "You know, that reminds me of my blog at %s",
        "My blog -- %s -- is filled with just that kind of thing.",
    ]
    return random.choice(blog_brag) % 'http://goat-blog.tumblr.com/'

def get_last_post_time():
    store = KVStore.getCustomStore("tumblr")
    last_post = store.getOrElse("lastPost", 0.0)
    return last_post

def post_to_imgur(url, title=None):
    imgur_url = None
    pwds = Passwords()
    client_id = pwds.getPassword('imgur.clientId')
    if client_id is None:
        return imgur_url

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
    elif code is not None:
        print "imgur said: %s" % code
    else:
        print "get_page returned None?"
    return imgur_url

def set_last_post_time():
    store = KVStore.getCustomStore("tumblr")
    now = time.time()
    last_post = store.save("lastPost", now)
    return now
