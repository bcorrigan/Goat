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
    params = {
        'type': post_type,
    }
    if post_type == "photo":
        params['source'] = url
    elif post_type == "video":
        embed_code = '<iframe width="640" height="480" src="%s" frameborder="0" allowfullscreen></iframe>'
        params['embed'] = embed_code % url

    if caption is not None:
        params['caption'] = caption
    body = urllib.urlencode(params)

    # gogo!
    response, content = oauth_client.request(request_url, method, body)
    # TODO I should probably inspect the response here!  though I'm not sure
    # we'd do anything with the errors.

def get_page(url):
    # TODO This needs to be put into a python goat utility library.
    code = None
    content = None
    resp = None

    req = urllib2.Request(url)
    try:
        resp = urllib2.urlopen(req)
        content = resp.read(25*1024)
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
    (code, content, respo) = get_page(url)
    if code != 200:
        print "got code", code, "from google"
    results = json.loads(content)

    images = [i['unescapedUrl'] for i in results['responseData']['results']]
    post_to_tumblr(random.choice(images), search)

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
            post_to_tumblr(url)
        elif video_match:
            url = video_match.group()
            print "video", url
            post_to_tumblr(url, post_type='video')
        else:
            commands = {
                "gis": gis_search,
                "bis": gis_search,  # TODO
                "yis": gis_search,  # TODO
            }

            msg = str(StringUtil.removeFormattingAndColors(m.getTrailing()))
            tokens = msg.split()
            if tokens[0] in commands and len(tokens) > 1:
                provider = m.modCommand
                parser = CommandParser(m)
                if parser.hasVar("provider"):
                    provider = parser.get("provider")
                    if provider == "my mommy":
                        provider = "gis"

                commands[provider](" ".join(tokens[1:]))

    def processPrivateMessage(self, m):
        pass

    def getCommands(self):
        return array([""], String)

    def messageType(self):
        return self.WANT_UNCLAIMED_MESSAGES

#This should always return a new instance
def getInstance():
    return Tumblr()
