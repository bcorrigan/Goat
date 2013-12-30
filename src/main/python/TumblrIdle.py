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

# TODO: It would be better it might be better if this was triggered by
# a timer instead of channel activity.  The timer could trigger every X
# minutes and post to the blog if there has been any channel activity
# in the last X minutes.

IDLE_TIME = 900
BOTS = ["zuul", "goatsee" ]

class TumblrIdle(Module):
    def __init__(self):
        pass

    def processChannelMessage(self, m):
        if m.modCommand == "tumblr":
            helptext = "tumblr commands are: brain, followers"

            msg = unicode(StringUtil.removeFormattingAndColors(m.getTrailing()))
            commands = msg.split()
            if len(commands) == 1:
                m.reply(helptext)
            elif commands[1] == "brain":
                words = goatpy.tumblr.get_words()
                m.reply("%s: My brain is full of '%s'" % (m.sender,
                    " ".join(words)))
            elif commands[1] == "followers":
                msg = goatpy.tumblr.followers()
                m.reply(msg)
            else:
                m.reply(helptext)
        elif m.modCommand == "tumblrbrain":
            m.reply("It's 'tumblr brain' now.")
        elif m.sender not in BOTS:
            msg = unicode(StringUtil.removeFormattingAndColors(m.getTrailing()))
            words = goatpy.tumblr.add_words(msg)
            last_post = goatpy.tumblr.get_last_post_time()
            now = time.time()
            if now - last_post > IDLE_TIME:
                tags = [m.sender]
                count = random.randint(2, 3)
                words = goatpy.tumblr.get_random_words()
                tags.extend(words)
                response = goatpy.tumblr.post_search(" ".join(words), tags=tags)
                if response is not None:
                    m.reply(response)

    def processPrivateMessage(self, m):
        pass

    def getCommands(self):
        return array(["tumblr"], String)

    def messageType(self):
        return self.WANT_UNCLAIMED_MESSAGES

#This should always return a new instance
def getInstance():
    return TumblrIdle()
