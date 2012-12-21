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

          reply = m.sender+": "+user+" has written " + str(userStore.get("linesSeen")) + " lines"
          if(userStore.has("wordCount")):
            reply += " with an average of " + self.getAvg(userStore) + " words per line. "
          if(userStore.has("curseCount")):
            if(userStore.get("curseCount")>0):
              reply+=user+" has cursed "+str(userStore.get("curseCount"))+" times"
              reply+=", a ratio of " + self.getCurseAvg(userStore) + ". " 
            else:
              reply+=user+" is clean of mouth. "
          if(userStore.has("racistCount")):
            if(userStore.get("racistCount")>0):
              reply+=user + " has indulged in race hatred " + str(userStore.get("racistCount"))+" times"
              reply+=", a ratio of "+self.getRacistAvg(userStore) + ". "
            else:
              reply+=user + " is colour blind."
          if(userStore.has("homoPhobiaCount")):
            if(userStore.get("homoPhobiaCount")>0):
              reply+=user+" has been in ugly homophobic incidents "+str(userStore.get("homoPhobiaCount"))+" times"
              reply+=", a ratio of " + self.getHomoAvg(userStore) + "."
            else:
              reply+=user+" is as cisfriendly as Dan Savage. "
          m.reply(reply)
	else:
	  m.reply(m.sender+": I have never seen that person.")
      else:
        chanStore=self.getChanStore(m)
        reply = m.sender+": "+m.getChanname()+" has seen " + str(chanStore.get("linesSeen")) + " lines"
        if(chanStore.has("wordCount")):
          reply += " with an average of " + self.getAvg(chanStore) + " words per line. "
        if(chanStore.has("curseCount")):
          if(chanStore.get("curseCount")>0):
            reply+=m.getChanname()+" has seen curses "+str(chanStore.get("curseCount"))+" times"
            reply+=", a ratio of " + self.getCurseAvg(chanStore) + ". " 
          else:
            reply+=m.getChanname()+" needs to get out more. "
        if(chanStore.has("racistCount")):
          if(chanStore.get("racistCount")>0):
            reply+=m.getChanname() + " has seen race hatred " + str(chanStore.get("racistCount"))+" times"
            reply+=", a ratio of "+self.getRacistAvg(chanStore) + ". "
          else:
            reply+=m.getChanname() + " is colour blind."
        if(chanStore.has("homoPhobiaCount")):
          if(chanStore.get("homoPhobiaCount")>0):
            reply+=m.getChanname()+" has seen ugly homophobic incidents "+str(chanStore.get("homoPhobiaCount"))+" times"
            reply+=", a ratio of " + self.getHomoAvg(chanStore) + "."
          else:
            reply+=m.getChanname()+" is as cisfriendly as Dan Savage. "
        m.reply(reply)

    else:
      self.updateStats(m)
    
  def processPrivateMessage(self, m):
    self.processChannelMessage(m)

  def incSave(self, store, propName, inc=1):
    if(store.has(propName)):
      if(inc>0):
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
    return len(re.findall(r'fuck[a-z]*|cunt[a-z]*|shit[a-z]*|piss[a-z]*|fag[a-z]*|jizz[a-z]*|cock[a-z]*|tits[a-z]*|pussy[a-z]*|pendejo[a-z]*|mierd[a-z]*', m.getTrailing().lower()))

  def countRacisms(self,m):
    return len(re.findall(r'kike[a-z]*|chink[a-z]*|paki[a-z]*|nigger[a-z]*|spic[a-z]*|gook[a-z]*|boche[a-z]*|wetback[a-z]*|kraut[a-z]*|honkey[a-z]*|porch monkey|raghead[a-z]*|anglo[a-z]*|camel jockey|darkie[a-z]*|greaser[a-z]*|jew[a-z]*|paddy[a-z]*|paddie[a-z]*|mick[a-z]*|pikey[a-z]*|fenian[a-z]*|gypsy[a-z]*|eskimo[a-z]*|shylock[a-z]*|musselman[a-z]*|moslem[a-z]*|mosselman[a-z]*|gringo[a-z]*', m.getTrailing().lower()))

  def countHomophobia(self,m):
    return len(re.findall(r'fag[a-z]*|gaylord[a-z]*|fudgepack[a-z]*|fudge pack[a-z]*|tranny[a-z]*|queer[a-z]*|homo[a-z]*|cocksucker[a-z]*|buttrammer[a-z]*|poof[a-z]*|womyn[a-z]*|sodomite[a-z]*|dyke[a-z]*|carpetmunch[a-z]*|carpet munch[a-z]*|butch lesb[a-z]*|frigid[a-z]*|muff diver[a-z]*', m.getTrailing().lower()))

#This should always return a new instance
def getInstance():
  return Stats()
