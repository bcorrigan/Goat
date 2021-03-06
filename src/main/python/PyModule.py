from java.lang import System as javasystem
from java.lang import String
from goat.core import Message
from goat.core import Module
from goat.util import CommandParser
from goat.core import Constants
from jarray import array

class HelloWorld(Module):
  def __init__(self):
    pass

  def processChannelMessage(self, m):
    if(m.modCommand=="pyhello"):
      m.reply("Hello from a python module!!!!")
    elif(m.modCommand=="poop"):
      parser = CommandParser(m)
      if(parser.hasNumber()):
	numPoops=int(parser.findNumber())
	if(numPoops>30):
	  m.reply("Too much poop")
	else:
	  m.reply((Constants.PILE_OF_POO+" ")*numPoops)
      else:
	m.reply("You must tell me how much to poop")


  def processPrivateMessage(self, m):
    self.processChannelMessage(m)

  def getCommands(self):
    return array(["pyhello","poop"], String)

#This should always return a new instance
def getInstance():
  return HelloWorld()
