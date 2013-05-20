package goat.module

import goat.core.Module
import goat.core.Message
import goat.util.CommandParser
import goat.util.WeatherStore
import goat.core.KVStore
import goat.core.User

import java.io.IOException;

class WeatherRecords extends Module {
  
  val users = goat.Goat.getUsers()
  val wstore = new WeatherStore()
  
  override def getCommands(): Array[String] = {
    Array("weatherstats")
  }
  
  override def processPrivateMessage(m:Message):Unit = {
    processChannelMessage(m)
  }
  
  /*
   * weatherstats user=bc
   *    => max temp, min temp, max gust, min gust, 1..5 highest scores and dates
   * 
   * weatherscores
   *    => table of max scores in channel, using a list of recently active users somehow
   */
  override def processChannelMessage(m:Message):Unit = {
    val parser = new CommandParser(m)
    println("command:" + m.getModCommand)
    m.getModCommand match {
      case "weatherstats" =>
        val userName = if(parser.hasVar("user")) parser.get("user") else m.getSender
        
        if(!users.hasUser(userName)) {
          m.reply(m.getSender + ": Uh, who?")
          return
        }
        
        val user = users.getUser(userName)
        
        val weatherStation = user.getWeatherStation()
        
        if(weatherStation=="") {
          m.reply(m.getSender + ": No weather station set, bud.")
          return
        }
        
        m.reply(wstore.getStatReport(userName,weatherStation))
    }
  }
}