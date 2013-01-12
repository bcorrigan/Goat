from goat.core import KVStore
from goat.core import Module
from goat.util import CommandParser
from goat.util import Passwords
from goat.util import StringUtil
from java.lang import String
from jarray import array

import goatpy.tumblr
import random
import re
import time

# this module posts images and videos to tumblr.  it picks these up when
# someone pastes them directly in the channel or when someone makes use of a
# search module like gis/yis/bis.
# TODO: implement bis, yis searches
# TODO: what about youtube search?
# TODO: prevent double-posting

URL_RX = re.compile(r'https?://\S+\.(jpg|gif|png|bmp)')
VIDEO_RX = re.compile(r'https?://[^/]*youtube\S+')

refractory_period = 300 # random wait period from 0 .. this_number
base_chance = 0.90
random_bonus = 0.50 # start_level = base_chance + random(random_bonus)
chance_adjustment = 2.0/3 # should be < 1.0

KEY_COOLDOWN = "cooldownTime"
KEY_CHANCE =  "postChance"

def is_obsessed(m):
    obsessed = False
    store = goatpy.tumblr.get_tumblr_store()
    user = None
    if m.getPrefix().endswith(".lv.cox.net"):
        user = "ennui"
    elif m.getPrefix().endswith(".ks.cox.net"):
        user = "av"

    if user is None:
        return obsessed

    # a watched user has a randomized chance to be allowed to make a post.
    # each time they activate the module the user cooldown timer is set (or
    # reset).
    # the first time they activate the module, their chance to make further
    # posts is reduced until the timer expires.
    # each time they fail to post, their chance to make another post will be
    # reduced further until the timer expires.
    # once the timer expires, their chance to post will return to the default.
    now = time.time()
    cooldown_reset = False

    user_cooldown = store.getOrElse(user+KEY_COOLDOWN, now)
    if user_cooldown <= now:
        cooldown_reset = True
        user_cooldown = now
    new_cooldown = user_cooldown + random.randint(0, refractory_period)
    store.save(user+KEY_COOLDOWN, new_cooldown)

    user_chance = store.getOrElse(user+KEY_CHANCE, 0.0)
    if cooldown_reset or user_chance == 0.0:
        user_chance = base_chance + random.random() * random_bonus

    if random.random() > user_chance:
        obsessed = True

    if obsessed or cooldown_reset:
        user_chance *= chance_adjustment
    store.save(user+KEY_CHANCE, user_chance)

    if obsessed and random.random() < 0.15:
        # punish the user.
        words = ["awesome", "best", "forever", "smile", "amazing", "flag",
            "sexy", "hot", "kiss", "baby", "family", "heal", "pray",
            "cool", "hero"]
        tags = [user, "obama", random.choice(words)]
        search = " ".join(tags[1:])
        goatpy.tumblr.gis_search(search, tags, skip_repeats=False)

    return obsessed

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
        tags = [m.sender]


        if img_match:
            url = img_match.group()
            if is_obsessed(m):
                goatpy.tumblr.check_old_url(url) # saves as seen
            else:
                tags.append("picture")
                response = goatpy.tumblr.post_to_tumblr(url, tags=tags)
        elif video_match:
            url = video_match.group()
            if is_obsessed(m):
                goatpy.tumblr.check_old_url(url) # saves as seen
            else:
                tags.append("video")
                response = goatpy.tumblr.post_to_tumblr(url, post_type='video',
                    tags=tags)
        elif m.modCommand in commands:
            msg = unicode(StringUtil.removeFormattingAndColors(m.getTrailing()))
            tokens = msg.split()
            if len(tokens) > 1:
                search = " ".join(tokens[1:])
                if is_obsessed(m):
                    goatpy.tumblr.check_old_search(search) # saves as seen
                else:
                    provider = m.modCommand
                    parser = CommandParser(m)
                    if parser.hasVar("provider"):
                        provider = parser.get("provider")
                        if provider == "my mommy":
                            provider = "gis"

                    tags = [m.sender]
                    tags.extend(tokens[1:])
                    response = commands[provider](search, tags=tags)

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
