from java.lang import String
from goat.core import Module
from goat.util import CommandParser
from goat.util import StringUtil
from goat.util import Passwords
from jarray import array

import oauth2 as oauth # yes this oauth v1 not v2
import random
import simplejson as json
import re
import urllib
import urllib2

# this module should implement the following behavior
# any time someone posts an image in the channel, post that image to
# tumblr.
# do the same with youtube videos.
# when someone does a gis/yis/bis search, pull a fixed or random
# image from the results and post that.
# if this gets abused too much, we should make it semi-random or
# make it so there are timers.

URL_RX = re.compile(r'https?://\S+\.(jpg|gif|png|bmp)')
VIDEO_RX = re.compile(r'https?://[^/]*youtube\S+')

GENERATOR = "goatbot"

# globals, mwa-ha-ha
errored = False

def post_to_tumblr(url, caption=None, post_type="photo"):
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
    request_url = 'http://api.tumblr.com/v2/blog/goatbot.tumblr.com/post'
    method = 'POST'
    params = { }
    if post_type == "photo":
        #params['source'] = url
        #params['type'] = post_type
        # we post links to imgur instead of hosting images with tumblr because
        # of tumblr's stingy usage quotas.
        # TODO maybe post directly to tumblr until we hit the quota then fall
        # back to imgur?
        params['type'] = 'text'
        if caption is not None:
            params['title'] = caption
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

def gis_search(search):
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
        imgur_url = post_to_imgur(random.choice(images), search)
        if imgur_url is not None:
            return post_to_tumblr(imgur_url, search)

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
        msg = StringUtil.removeFormattingAndColors(m.getTrailing())
        img_match = URL_RX.search(msg)
        video_match = VIDEO_RX.search(msg)
        if img_match:
            url = img_match.group()
            # XXX temporarily disable this.  we are bumping up against image posting
            # limits.
            #post_to_tumblr(url)
        elif video_match:
            url = video_match.group()
            post_to_tumblr(url, post_type='video')
        else:
            commands = {
                "gis": gis_search,
                "bis": gis_search,  # TODO
                "yis": gis_search,  # TODO
            }

            msg = unicode(StringUtil.removeFormattingAndColors(m.getTrailing()))
            tokens = msg.split()
            if tokens[0] in commands and len(tokens) > 1:
                provider = m.modCommand
                parser = CommandParser(m)
                if parser.hasVar("provider"):
                    provider = parser.get("provider")
                    if provider == "my mommy":
                        provider = "gis"

                response = commands[provider](" ".join(tokens[1:]))
                if response:
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
