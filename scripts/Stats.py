from java.lang import System as javasystem
from java.lang import String
from goat.core import Message
from goat.core import Module
from goat.util import CommandParser
from goat.util import StringUtil
from jarray import array

import re

#A simple stats counting module to try out goat's
#amazing new persistence solution

# TODO:
# smiley counter
# religious counter
# violence meter
# example bc racism - show last few examples of racism
# sexism
# open source
LINE_COUNT = "lineCount"
WORD_COUNT = "wordCount"

WORD_CURSE = "curseCount"
WORD_RACISM = "racistCount"
WORD_HOMOPHOBIA = "homoPhobiaCount"
WORD_TYPES_RX = {
    WORD_CURSE: re.compile(
        r'(fuck|cunt|shit|piss|fag|jizz|cock|tits|pussy|pendejo|mierd|bitch)'
    ),

    WORD_RACISM: re.compile(
        r'(kike|chink|nigger|spick?|gook|boche|wetback|\bkraut|honkey|porch\s*monkey|raghead|camel\s*jockey|darkie|greaser|jew|paddy|paddie|mick|pikey|fenian|gypsy|shylock|m[ou]sselman|moslem|gringo|porridge\s*wog|white\s*(pride|power)|slit[a-z]*\s*eye|red\s*indian|juden|dago|paki\b|haj)'
    ),

    WORD_HOMOPHOBIA: re.compile(
        r'(fag|gaylord|(that\'?s|so|how|be) gay|fudge ?pack|tranny|cock ?sucker|butt ?(ram|fuck)|sodomite|dyke|carpet ?munch|muff ?diver|cock ?sucker|homo\b|gaa+y|gayy+)'
    ),

}

class Stats(Module):
    def __init__(self):
        pass

    def updateStats(self, m):
        stores = []
        stores.append(self.getChanStore(m))
        stores.append(self.getUserStore(m))

        msg = StringUtil.removeFormattingAndColors(m.getTrailing())
        word_count = len(msg.split())
        seen_types = dict()
        for word_type, rx in WORD_TYPES_RX.items():
            if re.search(rx, msg):
                seen_types[word_type] = True

        for store in stores:
            self.incSave(store, LINE_COUNT)
            self.incSave(store, WORD_COUNT, word_count)
            for word_type, seen in seen_types.items():
                self.incSave(store, word_type, 1)

    # this should really be a method on the stores themselves, maybe it
    # already is? TODO check
    def get_default(self, store, key, default):
        if store.has(key):
            return store.get(key)
        else:
            return default

    # TODO this should also probably be a method of the store object.
    def incSave(self, store, propName, inc=1):
        if store.has(propName):
            if inc>0:
                prop=store.get(propName)
                prop+=inc
                store.save(propName,prop)
        else:
            store.save(propName,inc)


    def generate_stat_text(self, store, stat, source, verb, pure):
        """used by generate_reply to generate a sentence about a particular
        stat."""

        stat_count = self.get_default(store, stat, 0)
        line_count = self.get_default(store, LINE_COUNT, 0)
        reply = " %s" % source
        if stat_count:
            reply += " has %s %d times, a ratio of %.02f%%." % (
                verb, stat_count, float(stat_count) / line_count * 100)
        else:
            reply += " %s." % pure
        return reply

    def generate_reply(self, asker, source, store, channel=False):
        lines_seen = self.get_default(store, LINE_COUNT, 0)
        word_count = self.get_default(store, WORD_COUNT, 0)
        if lines_seen == 0:
            return "uhhh... what?"

        verb = channel and "seen" or "written"
        reply = "%s: %s has %s %d lines" % (asker, source, verb, lines_seen)
        reply += " with an average of %.2f words per line." % (
            word_count / float(lines_seen))

        if channel:
            reply += self.generate_stat_text(store, WORD_CURSE, source,
                "seen curses", "needs to get out more")
            reply += self.generate_stat_text(store, WORD_RACISM, source,
                "seen race hatred", "is colour blind")
            reply += self.generate_stat_text(store, WORD_HOMOPHOBIA, source,
                "seen ugly homophobic incidents",
                "is as gay friendly as Dan Savage")
        else:
            reply += self.generate_stat_text(store, WORD_CURSE, source,
                "cursed",  "is clean of mouth")
            reply += self.generate_stat_text(store, WORD_RACISM, source,
                "indulged in race hatred", "is colour blind")
            reply += self.generate_stat_text(store, WORD_HOMOPHOBIA, source,
                "been in ugly homophobic incidents",
                "is as gay friendly as Dan Savage")
        return reply

    # methods past this point are for implementing parts of the goat module
    # API
    def processChannelMessage(self, m):
        if m.modCommand=="stats":
            parser = CommandParser(m)
            if parser.hasVar("user"):
                user=parser.get("user")
                if self.hasUserStore(user):
                    userStore=self.getUserStore(user)
                    reply = self.generate_reply(m.sender, user, userStore)
                    m.reply(reply)
                else:
                    m.reply(m.sender+": I have never seen that person.")
            else:
                chanStore=self.getChanStore(m)
                reply = self.generate_reply(m.sender, m.getChanname(),
                    chanStore)
                m.reply(reply)
        else:
            self.updateStats(m)

    def processPrivateMessage(self, m):
        self.processChannelMessage(m)

    def getCommands(self):
        return array([""], String)

    def messageType(self):
        return self.WANT_UNCLAIMED_MESSAGES

#This should always return a new instance
def getInstance():
    return Stats()
