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
        r'(fuck|cunt|shit|piss|fag|jizz|cock|tits|pussy|pendejo|mierd|bitch|god\s*dam)'
    ),

    WORD_RACISM: re.compile(
        r'(kike|chink|nigger|spick?|gook|boche|wetback|\bkraut|honkey|porch\s*monkey|raghead|camel\s*jockey|darkie|greaser|jew|paddy|paddie|mick|pikey|fenian|gypsy|shylock|m[ou]sselman|moslem|gringo|porridge\s*wog|white\s*(pride|power)|slit[a-z]*\s*eye|red\s*indian|juden|dago|paki\b|haj|anglos?(\s+|$)|whitey)'
    ),

    WORD_HOMOPHOBIA: re.compile(
        r'(fag|gaylord|(that\'?s|so|how|be|more)\s*gay|fudge\s*pack|tranny|cock\s*sucker|butt\s*(ram|fuck)|sodomite|dyke|carpet\s*munch|muff\s*diver|cock\s*sucker|homo\b|gaa+y|gayy+)'
    ),
}

# purity constants
PURITY_SCORE = "purityScore"
PURITY_BEST = "purityBest"
PURITY_BEST_FAILED_MSG = "purityBestFailedMessage"
PURITY_BEST_FAILED_SENDER = "purityBestFailedSender"
PURITY_RECENT_FAILED_MSG = "purityRecentFailedMessage"
PURITY_RECENT_FAILED_SENDER = "purityRecentFailedSender"

class Stats(Module):
    def __init__(self):
        pass

    def updateStats(self, m):
        msg = StringUtil.removeFormattingAndColors(m.getTrailing())
        word_count = len(msg.split())
        seen_types = dict()
        pure = True
        for word_type, rx in WORD_TYPES_RX.items():
            if re.search(rx, msg):
                pure = False
                seen_types[word_type] = True

        chan_store = self.getChanStore(m)
        user_store = self.getUserStore(m)
        for store, is_channel in [(chan_store, True), (user_store, False)]:
            if pure:
                self.purity_update(m, store, is_channel)
            else:
                self.purity_fail(m, store, is_channel)
            self.incSave(store, LINE_COUNT)
            self.incSave(store, WORD_COUNT, word_count)
            for word_type, seen in seen_types.items():
                self.incSave(store, word_type, 1)

    def purity_update(self, m, store, is_channel):
        self.incSave(store, PURITY_SCORE)
        score = store.get(PURITY_SCORE)

        if is_channel:
            msg = "%s: The channel has now reached %d lines of pure text.  I just couldn't be more proud."
        else:
            msg = "%s: Well done! You've spoken %d times without the least bit of hate."

        if score in [50, 100, 150, 200, 250, 500, 1000]:
            m.reply(msg % (m.sender, score))

    def purity_fail(self, m, store, is_channel):
        score = self.get_default(store, PURITY_SCORE, 0)
        store.save(PURITY_SCORE, 0)
        store.save(PURITY_RECENT_FAILED_MSG, m.getTrailing())
        store.save(PURITY_RECENT_FAILED_SENDER, m.sender)

        if is_channel:
            msg = "%s: You've ruined it for everyone.  The channel had %d lines of pure discussion until you spouted off."
        else:
            msg = "%s: Shame on you for using that kind of language.  You were doing so well too, with %d lines of appropriate chat 'til now."

        best = self.get_default(store, PURITY_BEST, 0)
        if score > best:
            store.save(PURITY_BEST, score)
            store.save(PURITY_BEST_FAILED_MSG, m.getTrailing())
            store.save(PURITY_BEST_FAILED_SENDER, m.sender)
            msg += "  AND that was the best run ever, beating the previous best of %d.  What a disappointment." % best

        if score < 50:
            return

        m.reply(msg % (m.sender, score))

    def gen_purity_reply(self, sender, target, store):
        score = self.get_default(store, PURITY_SCORE, 0)
        best = self.get_default(store, PURITY_BEST, "")

        reply = "%s: %s has a current purity score of %d, and " % (sender, target, score)
        if best is "":
            reply += "is as pure as the driven snow."
        else:
            reply += "a previous best of %d." % best

        best_msg = self.get_default(store, PURITY_BEST_FAILED_MSG, "")
        best_sender = self.get_default(store, PURITY_BEST_FAILED_SENDER, "")
        if best_msg is not "":
            reply += "  The previous best run failed when %s said: \"%s\"" % (
                best_sender, best_msg)

        recent_msg = self.get_default(store, PURITY_RECENT_FAILED_MSG, "")
        recent_sender = self.get_default(store, PURITY_RECENT_FAILED_SENDER, "")
        if recent_msg is not "" and recent_msg != best_msg:
            reply += "  Most recently, %s was heard to say: \"%s\"" % (
                recent_sender, recent_msg)
        return reply

    # TODO this should really be a method on the stores themselves
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
        elif m.modCommand=="purity":
            parser = CommandParser(m)
            if parser.hasVar("user"):
                user = parser.get("user")
                if self.hasUserStore(user):
                    userStore = self.getUserStore(user)
                    reply = self.gen_purity_reply(m.sender, user, userStore)
                    m.reply(reply)
                else:
                    m.reply(m.sender+": I'm afraid I just don't know.")
            else:
                chanStore = self.getChanStore(m)
                reply = self.gen_purity_reply(m.sender, m.getChanname(),
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
