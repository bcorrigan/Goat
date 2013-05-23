from java.lang import String
from goat.core import Module
from goat.util import Passwords
from goat.util import StringUtil
from jarray import array

import BeautifulSoup as bs
import htmlentitydefs
import simplejson as json
import random
import re
import urllib
import urllib2
import goatpy.util as util

URL_RX = re.compile(r'https?://\S+')

random_urls = [
    "http://www.youtube.com/watch?v=dQw4w9WgXcQ", # rickroll
    "http://www.youtube.com/watch?v=wM89T74MPnE", # mahna-mahna
    "http://www.youtube.com/watch?v=kfVsfOSbJY0", # rebecca black - friday
    "http://www.youtube.com/watch?v=fWNaR-rxAic", # call me maybe
    "http://www.youtube.com/watch?v=Ktbhw0v186Q", # what is love
    "http://www.youtube.com/watch?v=wZZ7oFKsKzY", # nyan cat
    "http://www.youtube.com/watch?v=kxopViU98Xo", # epic sax guy
    "http://www.youtube.com/watch?v=eh7lp9umG2I", # he-man hey hey hey
    "http://www.youtube.com/watch?v=yzC4hFK5P3g", # ponponpon
]


def build_message(url):
    short_url = shorten_url(url)
    description = describe_url(url)

    if short_url is None and description is None:
        return None

    msg = ""
    if short_url is not None:
        msg = short_url

    if description is not None:
        msg = "%s  ## %s" % (msg, description)
    return msg


def describe_url(url):
    """Gives a textual description of the content of url."""
    code, content, resp = util.get_page(url)
    if code != 200:
        print "I tried to look at %s but it told me %s." % (url, str(code))
        return

    if resp.headers["content-type"].startswith("text/html"):
        return extract_article_text(url)
    else:
        msg = None
        try:
            msg = "%s, %s bytes" % (resp.headers["content-type"],
                resp.headers["content-length"])
        except KeyError:
            print "Missing headers for %s" % url
        return msg


def extract_article_text(url):
    """Uses a web service to extract the text of an article."""
    pwds = Passwords()
    token = pwds.getPassword('apibot.token')

    params = {}
    params['token'] = token
    params['url'] = url
    params['timeout'] = 20000  # timeout in ms

    summary_url = 'http://www.diffbot.com/api/article?%s' % urllib.urlencode(
        params)
    code, content, resp = util.get_page(summary_url, max_size=512*1024)
    if code != 200:
        print "Got %s requesting %s" % (str(code), summary_url)
        return None

    try:
        results = json.loads(content)
    except:
        print code, content, resp
        return "The summarizer doesn't like this."
    if "title" in results:
        if "text" in results:
            return "%s\f%s" % (results["title"], results["text"])
        else:
            return results["title"]
    elif "text" in results:
        return results["text"]
    else:
        print results
        return None


def shorten_url(url):
    """Uses a web service to shorten a long url."""
    short_url = None

    pwds = Passwords()
    token = pwds.getPassword('bitly.token')

    if random.random() < 0.01:
        url = random.choice(random_urls)

    params = {
        "access_token": token,
        "longUrl": url,
        "domain": "j.mp",   # bit.ly and bitly.com are also options.
    }

    shortener = 'https://api-ssl.bitly.com/v3/shorten?%s' % urllib.urlencode(
        params)
    (code, content, resp) = util.get_page(shortener)
    url = None
    if code == 200:
      try:
        results = json.loads(content)
      except:
        print "error loading json from", shortener, content

      try:
        url = results["data"]["url"]
      except:
        print "unexpected json response from", shortener, results
    else:
      print shortener, "returned", code, content
    return url




class Shortener(Module):

    def __init__(self):
        pass

    # methods past this point are for implementing parts of the goat module
    # API
    def processChannelMessage(self, m):
        match = URL_RX.search(
            StringUtil.removeFormattingAndColors(m.getTrailing()))
        if match:
            url = match.group()
            # arbitrary treshold for long urls
            if len(url) > 24 and "git.io" not in url:
                msg = build_message(url)
                if msg is not None:
                    m.reply(msg)

    def processPrivateMessage(self, m):
        pass

    def getCommands(self):
        return array([""], String)

    def messageType(self):
        return self.WANT_ALL_MESSAGES

# This should always return a new instance
def getInstance():
    return Shortener()
