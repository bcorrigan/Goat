from goat.core import Module
from goat.util import CommandParser
from goat.util import StringUtil
from java.lang import String
from jarray import array

import goatpy.tumblr
import time

# this module posts to tumblr automatically when no one has forced goat to
# post in a while

IDLE_TIME = 3600

class TumblrIdle(Module):
    def __init__(self):
        pass

    def processChannelMessage(self, m):
        msg = unicode(StringUtil.removeFormattingAndColors(m.getTrailing()))
        # TODO we want "interesting" phrases to search for, for now just
        # put a minimum size.  maybe minimum number of words would be better?
        if len(msg) > 25:
            last_post = goatpy.tumblr.get_last_post_time()
            now = time.time()
            if now - last_post > IDLE_TIME:
                response = goatpy.tumblr.gis_search(msg, show_search=False)
                if response is not None:
                    m.reply(response)

    def processPrivateMessage(self, m):
        pass

    def getCommands(self):
        return array([""], String)

    def messageType(self):
        return self.WANT_UNCLAIMED_MESSAGES

#This should always return a new instance
def getInstance():
    return TumblrIdle()
