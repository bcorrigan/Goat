from java.lang import String
from goat.core import Module
from goat.util import StringUtil
from jarray import array

import htmlentitydefs
import re
import urllib

URL_RX = re.compile(r'https?://\S+')
TITLE_RX = re.compile(r"<title>([^<]*).*</title>", re.I)

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

def get_page_content(url):
    """Will not return any content for https urls in our jython config"""
    content = None
    try:
        fh = content = urllib.urlopen(url)
        content = ''.join(fh.readlines(1024*250))
    except IOError, e:
        content = None
    return content

def get_page_title(url):
    title = None
    page_content = get_page_content(url)
    if page_content is None:
        return title

    # half-assed attempt to parse title!
    match = TITLE_RX.search(page_content)
    if match:
        title = unescape(match.groups()[0])

        # strip extra whitespace
        title = " ".join(title.split())

        # cutoff extremely long titles
        if len(title) > 500:
            title = "%s ..." % title[:500]

    return title

def get_short_url(url):
    short_url = None
    shortener = 'http://be.gs/shorten?url=%s' % urllib.quote_plus(url)
    short_url = get_page_content(shortener)
    if "BACKTRACE" in short_url:
        short_url = None
    return short_url

def shorten_url_message(url):
    msg = None
    short_url = get_short_url(url)
    if short_url is None:
        return msg

    title = get_page_title(url)
    if title is not None:
        msg = "%s -- %s" % (short_url, title)
    else:
        msg = "%s" % short_url

    return msg

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
