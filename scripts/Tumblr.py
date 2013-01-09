from goat.core import KVStore
from goat.core import Module
from goat.util import CommandParser
from goat.util import Passwords
from goat.util import StringUtil
from java.lang import String
from jarray import array

import oauth2 as oauth # yes this oauth v1 not v2
import random
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

URL_RX = re.compile(r'https?://\S+\.(jpg|gif|png|bmp)')
VIDEO_RX = re.compile(r'https?://[^/]*youtube\S+')

GENERATOR = "goatbot"

blog_brag = [
    "Have you guys seen my blog at %s",
    "Haha good one!  I posted that to my blog at %s",
    "That belongs on my blog at %s",
    "I have something related to that on my blog at %s",
    "You know, that reminds me of my blog at %s",
    "My blog -- %s -- is filled with just that kind of thing.",
]

# globals, mwa-ha-ha
errored = False

def post_to_tumblr(url, caption=None, post_type="photo", link=None):
    url_store = KVStore.getCustomStore("tumblr_urls")
    if url_store.has(url):
        return
    else:
        url_store.save(url, True)

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
    if post_type == "photo":
        #params['source'] = url
        #params['type'] = post_type
        # we post links to imgur instead of hosting images with tumblr because
        # of tumblr's stingy usage quotas.
        imgur_url = post_to_imgur(url, caption)
        if imgur_url is None:
            return
        url = imgur_url
        # TODO maybe post directly to tumblr until we hit the quota then fall
        # back to imgur?
        params['type'] = 'text'
        if caption is not None:
            params['title'] = caption
        if link is not None:
            params['body'] = '<a href="%s"><img src="%s"></a>' % (link, url)
        else:
            params['body'] = '<img src="%s">' % url
    elif post_type == "video":
        params['type'] = post_type
        if caption is not None:
            params['caption'] = caption
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

def get_page(url, params=None, headers=None, max_size=25*1024):
    # TODO This needs to be put into a python goat utility library.
    code = None
    content = None
    resp = None

    if headers is not None:
        req = urllib2.Request(url, headers=headers)
    else:
        req = urllib2.Request(url)

    try:
        if params is not None:
            resp = urllib2.urlopen(req, urllib.urlencode(params))
        else:
            resp = urllib2.urlopen(req)
        content = resp.read(max_size)
        code = 200
    except urllib2.URLError, e:
        code = e.code

    return (code, content, resp)

def gis_search(search, show_search=True):
    params = {
        "v": "1.0",
        "start": "1",   # this can be incremented for more results.
        "q": search,
    }
    url = "https://ajax.googleapis.com/ajax/services/search/images?%s" % (
        urllib.urlencode(params))
    (code, content, resp) = get_page(url)
    if code != 200:
        return "got code %d from google" % code
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

        return post_to_tumblr(random.choice(images), caption=search, link=link)

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

class Tumblr(Module):
    def __init__(self):
        pass

    # methods past this point are for implementing parts of the goat module
    # API
    def processChannelMessage(self, m):
        commands = {
            "gis": gis_search,
            "bis": gis_search,  # TODO
            "yis": gis_search,  # TODO
        }

        chan_store = KVStore.getChanStore(m)

        msg = StringUtil.removeFormattingAndColors(m.getTrailing())
        img_match = URL_RX.search(msg)
        video_match = VIDEO_RX.search(msg)
        response = None
        if img_match:
            url = img_match.group()
            response = post_to_tumblr(url)
            chan_store.save("lastPost", time.time())
        elif video_match:
            url = video_match.group()
            response = post_to_tumblr(url, post_type='video')
            chan_store.save("lastPost", time.time())
        elif m.modCommand in commands:
            msg = unicode(StringUtil.removeFormattingAndColors(m.getTrailing()))
            tokens = msg.split()
            if len(tokens) > 1:
                provider = m.modCommand
                parser = CommandParser(m)
                if parser.hasVar("provider"):
                    provider = parser.get("provider")
                    if provider == "my mommy":
                        provider = "gis"

                response = commands[provider](" ".join(tokens[1:]))
                chan_store.save("lastPost", time.time())
        else:
            last_post = chan_store.getOrElse("lastPost", 0.0)
            if time.time() - last_post > 60*60:
                # just post something!
                response = commands["gis"](msg, show_search=False)
                chan_store.save("lastPost", time.time())

        if response is not None:
            m.reply(response)

    def processPrivateMessage(self, m):
        pass

    def getCommands(self):
        return array([""], String)

    def messageType(self):
        return self.WANT_ALL_MESSAGES

#This should always return a new instance
def getInstance():
    return Tumblr()
