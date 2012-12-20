from java.lang import System as javasystem
from java.lang import String
from goat.core import Message
from goat.core import Module
from goat.util import CommandParser
from jarray import array

import re

#A simple stats counting module to try out goat's 
#amazing new persistence solution

#todo curse ratio
#todo smiley counter
#todo homophobia counter
#religious counter
#violence meter

#example bc racism - show last few examples of racism
class Stats(Module):
  def __init__(self):
    pass

  def updateStats(self, m):
    chanStore = self.getChanStore(m)
    self.incSave(chanStore, "linesSeen")

    userStore = self.getUserStore(m)
    self.incSave(userStore, "linesSeen")

    wordCount=len(re.findall(r'\w+', m.getTrailing()))
    self.incSave(userStore, "wordCount", wordCount)
    self.incSave(chanStore, "wordCount", wordCount)

    curseCount=self.countCurses(m)
    self.incSave(userStore, "curseCount", curseCount)
    self.incSave(chanStore, "curseCount", curseCount)

    racistCount=self.countRacisms(m)
    self.incSave(userStore, "racistCount", racistCount)
    self.incSave(chanStore, "racistCount", racistCount)

    homoPhobiaCount=self.countHomophobia(m)
    self.incSave(userStore, "homoPhobiaCount", homoPhobiaCount)
    self.incSave(chanStore, "homoPhobiaCount", homoPhobiaCount)

  def processChannelMessage(self, m):
    if(m.modCommand=="stats"):
      parser = CommandParser(m)
      if(parser.hasVar("user")):
	user=parser.get("user")
        if(self.hasUserStore(user)):
          userStore=self.getUserStore(user)
          m.reply(m.sender+": "+user+" has written " + str(userStore.get("linesSeen")) + \
                  " lines with an average of " + self.getAvg(userStore) + " words per line. "+ \
                    user+" has cursed "+str(userStore.get("curseCount"))+" times, a ratio of " + \
                    self.getCurseAvg(userStore) + "." +" "+ user + " has indulged in race hatred " + \
                    str(userStore.get("racistCount")) + " times, a ratio of " +\
                    self.getRacistAvg(userStore) + ". " + \
                    user + " has been in ugly homophobic incidents " + str(userStore.get("homoPhobiaCount"))  +\
                    " times, a ratio of " + self.getHomoAvg(userStore) + ".")
	else:
	  m.reply(m.sender+": I have never seen that person.")
      else:
        chanStore=self.getChanStore(m)
	m.reply("I have seen:" + str(chanStore.get("linesSeen")) + " messages in " + m.getChanname() + \
                " and " + str(chanStore.get("wordCount")) + " words." + \
                " The channel has cursed "+str(chanStore.get("curseCount"))+" times, a ratio of " + \
                    self.getCurseAvg(chanStore)+"." +" "+ m.getChanname() + " has indulged in race hatred " + \
                    str(chanStore.get("racistCount")) + " times, a ratio of " +\
                    self.getRacistAvg(chanStore) + ". " + \
                    m.getChanname() + " has seen homophobic incidents " + str(chanStore.get("homoPhobiaCount"))  +\
                    " times, a ratio of " + self.getHomoAvg(chanStore) + ".")
    else:
      self.updateStats(m)
    
  def processPrivateMessage(self, m):
    self.processChannelMessage(m)

  def incSave(self, store, propName, inc=1):
    if(store.has(propName)):
      prop=store.get(propName)
      prop+=inc
      store.save(propName,prop)
    else:
      store.save(propName,inc)
    
  def getCommands(self):
    return array([""], String)
  
  def messageType(self):
    return self.WANT_UNCLAIMED_MESSAGES
  
  def runningAvg(self,oldAvg,newcount, newValue):
    return ((oldAvg*(newcount-1))+newValue)/newcount

  def getAvg(self,userStore):
    return '%0.3f'%(userStore.get("wordCount")/float(userStore.get("linesSeen")))

  def getRacistAvg(self, userStore):
    return '{0:.3%}'.format(userStore.get("racistCount")/float(userStore.get("wordCount")))

  def getCurseAvg(self,userStore):
    return '{0:.3%}'.format(userStore.get("curseCount")/float(userStore.get("wordCount")))

  def getHomoAvg(self, userStore):
    return '{0:.3%}'.format(userStore.get("homoPhobiaCount")/float(userStore.get("wordCount")))

  def countCurses(self,m):
    return len(re.findall(r'fuck|cunt|shit|piss|fag|jizz|cock|tits|pussy', m.getTrailing().lower()))

  def countRacisms(self,m):
    return len(re.findall(r'kike|chink|paki|nigger|spic|gook|boche|wetback|kraut|honkey|porch monkey|raghead|anglo|camel jockey|darkie|greaser|jew|paddy|mick|pikey|fenian|gypsy|eskimo|shylock|musselman|moslem|mosselman', m.getTrailing().lower()))

  def countHomophobia(self,m):
    return len(re.findall(r'fag|gaylord|fudgepack|fudge pack|tranny|queer|homo|cocksucker|buttrammer|poof|womyn|sodomite|dyke|carpetmunch|carpet munch|butch lesb|frigid|muff diver', m.getTrailing().lower()))

#This should always return a new instance
def getInstance():
  return Stats()
