from java.lang import System as javasystem
from java.lang import String
from goat.core import Constants
from goat.core import Message
from goat.core import Module
from goat.core import KVStore
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

CURSE_COUNT = "curseCount"
RACISM_COUNT = "racistCount"
HOMOPHOBIA_COUNT = "homoPhobiaCount"
SEX_COUNT = "sexCount"
WORD_TYPES_RX = {
    CURSE_COUNT: (True, re.compile(
        r'(fuck|cunt|shit|piss|jizz|cock|\btits?\b|pussy|pendejo|mierd|bitch|god\s*dam|bloody)'
    )),

    RACISM_COUNT: (True, re.compile(
        r'(kike|chink|nigger|spick?|gook|boche|wetback|\bkraut|honkey|porch\s*monkey|raghead|camel\s*jockey|darkie|greaser|paddy|paddie|mick|pikey|fenian|gypsy|shylock|m[ou]sselman|moslem|gringo|porridge\s*wog|white\s*(pride|power)|slit[a-z]*\s*eye|red\s*indian|juden|dago|paki\b|haj|anglos?(\s+|$)|whitey)'
    )),

    HOMOPHOBIA_COUNT: (True, re.compile(
        r'(fag|gaylord|(that\'?s|so|how)\s*gay|fudge\s*pack|tranny|cock\s*sucker|butt\s*(ram|fuck)|sodomite|dyke|carpet\s*munch|muff\s*diver|cock\s*sucker|homo\b|gaa+y|gayy+|bugger)'
    )),
    SEX_COUNT: (False, re.compile(
        # any sexual terms or even common euphemisms for sexual terms.  we
        # expect and want extremely loose matches here
        r'(sex|fuck|cock|butt|puss|vag\b|vagina|cunt|slit|kitty|schlong|penis|ball|pecker|member|muff|glans|head|blow|suck|lick|swallow|spit|deep|throat|tight|loose|hard|soft|tit|breast|nip|knocker|cup|double\s*d|porn|virgin|chaste|pure|impure|bondage|discipline|sado|sadism|masochis|punish|spank|tie|69|plow|rape|ream|bugger|ride|missionary|dog[a-z]*\s*style|cowgirl|wheelbarrow|sodom|anal|erotic|gaa*y|lesb|insert|slam|complet|fetish|fan\s*fic|fantasy|libid|pound|master|slave|subjugat|penetrat|thrust|use|cum|jiz|jism|semen|ejac|jack|get\s*(off|some)|enter|moan|scream|orgasm|deflower|ravage|ravish|violate|defile|maiden|celibate|vestal|foreplay|caress|cuddl|pet|kiss|love\s*mak|make\s*love|mak[a-z]*\s*out|oral|manhood|womanhood|fornicat|coitus|copulat|intima|relation|sleep[a-z]*\s*(around|with)|screw|carnal|nooky|intercourse|coupling|consumm|mate|mating|shove\s*it|up\s*your|mom|mother|dad|father|oedip|pa?edo|epheb|furr(y|ies)|yiff|ass|boot(y|ie)|junk|camel\s*toe|yoga\s*pants|dress|skirt|pant(y|ies)|\bbra\b|lingerie|negligee|condom|birth\s*control|the\s*pill|depo|estrogen|testosterone|yaz|facial|creampie|double\s*pen|teen|fap|beast.*two\s*backs|strap-?\s*on|dildo|finger|reproduc|cunnilingus|std|hiv|aids|incest|minors|prostitut|whore|pimp|slut|bestiality|pregna|pleasure|gonorrhea|clap|chlamydia|syphilis|\bhep\b|hepatitis|herpes|hpv|crab|anilingus|toss[a-z]*\s*salad|vulva|trib|labia|masturbat|scissor|frot|dagger|hump|scrump|grind|rub|job|phallus|phallic|venus|philia|dental\s*dam|latex|silicon|implant|enlarge|engorge|fla|flacid|turgid|passion|torrid|affair|extramarital|sultra|ardent|spurt|spray|shoot|throb|nut|huevo|fertil|egg|cervix|uter(us|ine)|fallopian|ovar(y|ies)|nail|(take|get)\s*it|cross|trans|gender|figure|bdsm|split|divide|cleave|cigar|banana|gun|pistol|ammunition|ammo|tower|sausage|wiener|hot\s*dog|pipe|tube|wank|jerk|drill)'
    ))
}

# purity constants
PURITY_SCORE = "purityScore"
PURITY_BEST = "purityBest"
PURITY_BEST_FAILED_MSG = "purityBestFailedMessage"
PURITY_BEST_FAILED_SENDER = "purityBestFailedSender"
PURITY_RECENT_FAILED_MSG = "purityRecentFailedMessage"
PURITY_RECENT_FAILED_SENDER = "purityRecentFailedSender"
PURITY_IMPURE_COUNT = "purityImpureCount"

class Stats(Module):
    def __init__(self):
        pass

    def updateStats(self, m):
        msg = StringUtil.removeFormattingAndColors(m.getTrailing()).lower()
        word_count = len(msg.split())
        seen_types = dict()
        pure = True
        for word_type, (impure, rx) in WORD_TYPES_RX.items():
            if re.search(rx, msg):
                if impure:
                    pure = False
                seen_types[word_type] = True

        chan_store = KVStore.getChanStore(m)
        user_store = KVStore.getUserStore(m)
        for store, is_channel in [(chan_store, True), (user_store, False)]:
            if pure:
                self.purity_update(m, store, is_channel)
            else:
                self.purity_fail(m, store, is_channel)
            store.incSave(LINE_COUNT)
            store.incSave(WORD_COUNT, word_count)
            for word_type, seen in seen_types.items():
                store.incSave(word_type, 1)

    def purity_update(self, m, store, is_channel):
        store.incSave(PURITY_SCORE)
        score = store.get(PURITY_SCORE)

        if is_channel:
            msg = "%s: The channel has now reached %d lines of pure text.  I just couldn't be more proud."
        else:
            msg = "%s: Well done! You've spoken %d times without the least bit of hate."

        if score == 100 or score % 250 == 0:
            m.reply(msg % (m.sender, score))

    def purity_fail(self, m, store, is_channel):
        score = store.incSave(PURITY_IMPURE_COUNT, 1)
        score = store.getOrElse(PURITY_SCORE, 0)
        store.save(PURITY_SCORE, 0)
        store.save(PURITY_RECENT_FAILED_MSG, m.getTrailing())
        store.save(PURITY_RECENT_FAILED_SENDER, m.sender)

        if is_channel:
            msg = "%s: You've ruined it for everyone.  The channel had %d lines of pure discussion until you spouted off."
        else:
            msg = "%s: Shame on you for using that kind of language.  You were doing so well too, with %d lines of appropriate chat 'til now."

        best = store.getOrElse(PURITY_BEST, 0)
        new_best = False
        if score > best:
            new_best = True
            store.save(PURITY_BEST, score)
            store.save(PURITY_BEST_FAILED_MSG, m.getTrailing())
            store.save(PURITY_BEST_FAILED_SENDER, m.sender)
            msg += "  AND that was the best run ever, beating the previous best of %d.  What a disappointment." % best

        if score > 150 or (new_best and score > 100):
            m.reply(msg % (m.sender, score))

    def gen_sex_reply(self, target, store):
        sex_count = store.getOrElse(SEX_COUNT, 0)
        line_count = store.getOrElse(LINE_COUNT, 0)
        if line_count == 0:
            reply = "I haven't the slightest."
        else:
            reply = "%s has sex on the mind about %.1f%% of the time" % (
                target, 100 * sex_count / float(line_count))
        return reply

    def gen_purity_reply(self, target, store):
        score = store.getOrElse(PURITY_SCORE, 0)
        best = store.getOrElse(PURITY_BEST, 0)
        impure_count = store.getOrElse(PURITY_IMPURE_COUNT, 0)
        line_count = store.getOrElse(LINE_COUNT, 0)

        reply = "%s has a current purity score of %d, " % (target, score)
        if best == 0:
            reply += "and has never had an impure thought."
        else:
            reply += "has a previous best of %d and is impure %.1f%% of the time." % (best, 100 * impure_count / float(line_count))

        best_msg = store.getOrElse(PURITY_BEST_FAILED_MSG, "")
        best_sender = store.getOrElse(PURITY_BEST_FAILED_SENDER, "")
        if best_msg != "":
            reply += "  The previous best run failed when %s said: \"%s%s\"." % (
                best_sender, best_msg, Constants.NORMAL)

        recent_msg = store.getOrElse(PURITY_RECENT_FAILED_MSG, "")
        recent_sender = store.getOrElse(PURITY_RECENT_FAILED_SENDER, "")
        if recent_msg != "" and recent_msg != best_msg:
            reply += "  Most recently, %s was heard to say: \"%s%s\"." % (
                recent_sender, recent_msg, Constants.NORMAL)

        # let's test out some of these new kv store interfaces!
        if target.startswith('#'):
            reply += self.gen_purity_highscore()

        return reply

    def gen_purity_highscore(self):
        reply = ""

        user_line = KVStore.getAllUsers(LINE_COUNT)
        user_impure = KVStore.getAllUsers(PURITY_IMPURE_COUNT)
        purity_stats =  []
        for user in user_line:
            line_count = user_line[user]
            # TODO make the threshold adjustable to exclude the 20% least
            # talkative users.
            if line_count < 50: # up to a minimum threshold.
                continue
            impure_count = user_impure[user] or 0
            pure_ratio = (line_count - impure_count) / float(line_count) * 100
            purity_stats.append((pure_ratio, line_count, user))

        # sort for highest purity score to lowest purity score with line count
        # as a tie breaker.
        purity_stats.sort(reverse=True)
        if not purity_stats:
            return reply

        most_pure = purity_stats[0]
        least_pure = purity_stats[-1]
        if most_pure[0] == least_pure[0]:
            # it's not worth reporting if the first and last place user
            # have the same score. (or are the same user)
            return reply

        reply += "  %s is on the spoke at %.1f%% purity" % (
            most_pure[2], most_pure[0])
        reply += ", and %s is off in the weeds at %.1f%% purity." % (
            least_pure[2], least_pure[0])

        if len(purity_stats) > 2:
            reply += " Here's everyone else: "
            results = []
            for stat in purity_stats[1:-1]:
                results.append("%s %.1f%%" % (stat[2], stat[0]))
            reply += ", ".join(results)
            reply += "."

        return reply

    def generate_stat_text(self, store, stat, source, verb, pure):
        """used by gen_stat_reply to generate a sentence about a particular
        stat."""

        stat_count = store.getOrElse(stat, 0)
        line_count = store.getOrElse(LINE_COUNT, 0)
        reply = " %s" % source
        if stat_count:
            reply += " has %s %d times, a ratio of %.02f%%." % (
                verb, stat_count, float(stat_count) / line_count * 100)
        else:
            reply += " %s." % pure
        return reply

    def gen_stats_reply(self, target, store, channel=False):
        lines_seen = store.getOrElse(LINE_COUNT, 0)
        word_count = store.getOrElse(WORD_COUNT, 0)
        if lines_seen == 0:
            return "uhhh... what?"

        verb = channel and "seen" or "written"
        reply = "%s has %s %d lines" % (target, verb, lines_seen)
        reply += " with an average of %.2f words per line." % (
            word_count / float(lines_seen))

        if channel:
            reply += self.generate_stat_text(store, CURSE_COUNT, target,
                "seen curses", "needs to get out more")
            reply += self.generate_stat_text(store, RACISM_COUNT, target,
                "seen race hatred", "is colour blind")
            reply += self.generate_stat_text(store, HOMOPHOBIA_COUNT, target,
                "seen ugly homophobic incidents",
                "is as gay friendly as Dan Savage")
        else:
            reply += self.generate_stat_text(store, CURSE_COUNT, target,
                "cursed",  "is clean of mouth")
            reply += self.generate_stat_text(store, RACISM_COUNT, target,
                "indulged in race hatred", "is colour blind")
            reply += self.generate_stat_text(store, HOMOPHOBIA_COUNT, target,
                "been in ugly homophobic incidents",
                "is as gay friendly as Dan Savage")
        return reply

    # methods past this point are for implementing parts of the goat module
    # API
    def processChannelMessage(self, m):
        commands = {
            "stats": self.gen_stats_reply,
            "purity": self.gen_purity_reply,
            "sexstats": self.gen_sex_reply,
        }

        if m.modCommand in commands:
            reply = "I'm afraid I just don't know."
            parser = CommandParser(m)
            if parser.hasVar("user"):
                user = parser.get("user")
                if KVStore.hasUserStore(user):
                    user_store = KVStore.getUserStore(user)
                    reply = commands[m.modCommand](user, user_store)
            else:
                chan_store = KVStore.getChanStore(m)
                reply = commands[m.modCommand](m.getChanname(), chan_store)
            m.reply("%s: %s" % (m.sender, reply))
        else:
            self.updateStats(m)

    def processPrivateMessage(self, m):
        pass

    def getCommands(self):
        return array([""], String)

    def messageType(self):
        return self.WANT_UNCLAIMED_MESSAGES

#This should always return a new instance
def getInstance():
    return Stats()
