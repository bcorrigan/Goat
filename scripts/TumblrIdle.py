from goat.core import Module
from goat.util import CommandParser
from goat.util import StringUtil
from java.lang import String
from jarray import array

import goatpy.tumblr
import random
import time

# this module posts to tumblr automatically when no one has forced goat to
# post in a while

IDLE_TIME = 1800

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
]


class TumblrIdle(Module):
    def __init__(self):
        pass

    def processChannelMessage(self, m):
        msg = unicode(StringUtil.removeFormattingAndColors(m.getTrailing()))
        words = goatpy.tumblr.feed_random_words(msg)
        last_post = goatpy.tumblr.get_last_post_time()
        now = time.time()
        if now - last_post > IDLE_TIME:
            tags = [m.sender]
            words = goatpy.tumblr.get_random_words()
            tags.extend(words)
            response = goatpy.tumblr.gis_search(words, tags=tags)
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
