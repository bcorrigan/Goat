from java.lang import String
from goat.core import Module
from goat.util import StringUtil
from jarray import array

import BeautifulSoup as bs
import htmlentitydefs
import random
import re
import urllib
import urllib2
import goatpy.util as util

URL_RX = re.compile(r'https?://\S+')
TITLE_RX = re.compile(r"<title>([^<]*).*</title>", re.I)

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

##
# Removes HTML or XML character references and entities from a text string.
#
# @param text The HTML (or XML) source text.
# @return The plain text, as a Unicode string, if necessary.
# stolen from http://effbot.org/zone/re-sub.htm#unescape-html
def unescape(text):
    def fixup(m):
        text = m.group(0)
        if text[:2] == "&#":
            # character reference
            try:
                if text[:3] == "&#x":
                    return unichr(int(text[3:-1], 16))
                else:
                    return unichr(int(text[2:-1]))
            except ValueError:
                pass
        else:
            # named entity
            try:
                text = unichr(htmlentitydefs.name2codepoint[text[1:-1]])
            except KeyError:
                pass
        return text # leave as is
    return re.sub("&#?\w+;", fixup, text)

def get_page_title(url):
    title = None
    (code, content, resp) = util.get_page(url)
    if code == 200:
        # XXX probably check content type here

        # half-assed attempt to parse title!
        match = TITLE_RX.search(content)
        if match:
            title = match.groups()[0].decode("utf-8")

            # convert html entities
            title = unescape(title)

            # strip extra whitespace and trim long titles
            title = " ".join(title.split())[:500]
        else:
            title = "???"
    return (code, title)

def get_short_url(url):
    short_url = None

    if random.random() < 0.0025:
        url = random.choice(random_urls)
    shortener = 'http://be.gs/shorten?url=%s' % urllib.quote_plus(url)
    (code, content, resp) = util.get_page(shortener)
    if code == 200:
        short_url = content
    return (code, short_url)

def shorten_url_message(url):
    code, short_url = get_short_url(url)
    if code != 200 or short_url is None:
        return "The shortenizer said %s" % str(code)

    msg = "%s  ## " % short_url
    #code, title = get_page_title(url)
    #if code != 200 or title is None:
    #    msg += "The server told me %d when I asked for the title" % code
    #else:
    #    msg += title
    title, text = get_page_content(url)
    msg += title
    if text is not None:
        msg += "   ## %s" % text
    return msg

def visible(element):
    skiptags = ['style', 'script', '[document]', 'head', 'title']
    if element.parent.name in skiptags:
        return False
    elif re.match('<!--.*-->', str(element)):
        return False
    return True

def get_page_content(url):
    params = {}
    params['format'] = 'html'
    params['rl'] = 'false' # don't translate urls
    params['mld'] = '0.8'  # tunable parameter
    params['url'] = url

    summary_url = 'http://viewtext.org/api/text'
    code, content, resp = util.get_page(summary_url, params)
    # check content type before parsing
    if code != 200:
        return ("Summarizer returned %d" % code, None)

    if resp.headers['content-type'].startswith('text/html'):
        soup = bs.BeautifulSoup(content)
        try:
            title = unescape(soup.title.string)
        except (AttributeError, TypeError), e:
            title = ''


        text = " ".join(filter(visible, soup.findAll(text=True)))
        text = unescape(text)
        # TODO improve the following regex
        text = re.sub('<!--[^-]*-->', '', text)
        text = re.sub(r'Original\s*\|\s*Bookmarklet\s*\|\s*PDF\s*', '', text)
        text = re.sub(title, '', text)
        return title, text
    else:
        return "%s, %s bytes" % (
            resp.headers['content-type'], resp.headers['content-length']
        ), None


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
                msg = shorten_url_message(match.group())
                if msg is not None:
                    m.reply(msg)

    def processPrivateMessage(self, m):
        pass

    def getCommands(self):
        return array([""], String)

    def messageType(self):
        return self.WANT_ALL_MESSAGES

#This should always return a new instance
def getInstance():
    return Shortener()
