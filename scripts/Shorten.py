from java.lang import String
from goat.core import Module
from jarray import array

import re
import urllib

URL_RX = re.compile(r'https?://\S+')
TITLE_RX = re.compile(r"<title>([^<]*).*</title>", re.I)

def get_page_content(url):
    """Will not return any content for https urls in our jython config"""
    content = None
    try:
        fh = content = urllib.urlopen(url)
        content = ''.join(fh.readlines())
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
        title = match.groups()[0]

        # strip extra whitespace
        title = " ".join(title.split())

        # cutoff long titles
        if len(title) > 60:
            title = "%s ..." % title[:60]

    return title

def get_short_url(url):
    shortener = 'http://be.gs/shorten?url=%s' % urllib.quote_plus(url)
    short_url = get_page_content(shortener)
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
        match = URL_RX.search(m.getTrailing())
        if match:
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
