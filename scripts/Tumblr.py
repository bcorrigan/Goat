from goat.core import KVStore
from goat.core import Module
from goat.util import CommandParser
from goat.util import Passwords
from goat.util import StringUtil
from java.lang import String
from jarray import array

import goatpy.tumblr
import re
import time

# this module posts images and videos to tumblr.  it picks these up when
# someone pastes them directly in the channel or when someone makes use of a
# search module like gis/yis/bis.
# TODO: implement bis, yis searches
# TODO: what about youtube search?
# TODO: prevent double-posting
# TODO: use tumblr until quota expires then fall back to imgur?

URL_RX = re.compile(r'https?://\S+\.(jpg|gif|png|bmp)')
VIDEO_RX = re.compile(r'https?://[^/]*youtube\S+')

class Tumblr(Module):
    def __init__(self):
        pass

    def processChannelMessage(self, m):
        commands = {
            "gis": goatpy.tumblr.gis_search,
            "bis": goatpy.tumblr.gis_search,  # TODO
            "yis": goatpy.tumblr.gis_search,  # TODO
        }

        msg = StringUtil.removeFormattingAndColors(m.getTrailing())
        img_match = URL_RX.search(msg)
        video_match = VIDEO_RX.search(msg)
        response = None
        if img_match:
            url = img_match.group()
            response = goatpy.tumblr.post_to_tumblr(url)
        elif video_match:
            url = video_match.group()
            response = goatpy.tumblr.post_to_tumblr(url, post_type='video')
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
