package goat.module

import goat.core.{IrcMessage, KVStore, Module, User, Users}
import goat.util.CommandParser
import goat.util.WeatherStore
import java.io.IOException;

class WeatherRecords extends Module {
  
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
   * weatherstats show=maxtemp , mintemp, maxwind, minwind, maxscore num=1,2,3,4,5
   * 
   * weatherscores
   *    => table of max scores in channel, using a list of recently active users somehow
   */
  override def processChannelMessage(m:Message):Unit = {
    val parser = new CommandParser(m)
    println("command:" + m.getModCommand)
    val userName = if(parser.hasVar("user")) parser.get("user") else m.getSender
    
    if(!Users.hasUser(userName)) {
      m.reply(m.getSender + ": Uh, who?")
      return
    }
    
    val user = Users.getUser(userName)
    
    val weatherStation = user.getWeatherStation()
    
    if(weatherStation=="") {
      m.reply(m.getSender + ": No weather station set for that user, bud.")
      return
    }
    
    val num =   if(parser.hasVar("num"))
                  parser.getInt("num")
                else 1
    
    if(num>5 && num<20) {
      m.reply(m.getSender + ": We don't store so many records - only 5.")
      return
    } else if(num<1 || num>=20) {
      m.reply(m.getSender + ": You're trying to be a dick, but everybody already knows you're a dick, so no need.")
      return
    } 
    
    if(parser.hasVar("view")) {
      parser.get("view") match {
        case "maxtemp" =>
          m.reply(m.getSender+": " + wstore.getReport(userName, weatherStation, "tempf", "max", "temperature"));
        case "mintemp" =>
          m.reply(m.getSender+": " + wstore.getReport(userName, weatherStation, "tempf", "min", "temperature"));
        case "maxwind" =>
          m.reply(m.getSender+": " + wstore.getReport(userName, weatherStation, "wind", "max", "wind"));
        case "maxgust" =>
          m.reply(m.getSender+": " + wstore.getReport(userName, weatherStation, "gust", "max", "wind gust"));
        case "maxscore" =>
          m.reply(m.getSender+": " + wstore.getScoreReport(userName, weatherStation, num))
        case _ =>
          m.reply(m.getSender+": That's not a record. Try: maxtemp,mintemp,maxwind,maxgust and maxscore")
      }
    } else if(parser.remaining().toLowerCase().contains("help")) {
        m.reply(m.getSender+ ": weatherstats [view=maxtemp|mintemp|maxwind|maxgust|maxscore] [num=1..5]")
    } else m.reply(wstore.getStatReport(userName,weatherStation))
    
  }
}