package goat.module

import goat.core.Module
import goat.core.Message
import goat.core.Constants._
import goat.util.CommandParser
import com.omertron.rottentomatoesapi._
import com.omertron.rottentomatoesapi.model._

import scala.collection.JavaConversions._

class RottenTomatoes extends Module {

  val apiKey = "qdcearnjeudmt6kq5kmtsq5w"
  
  val api = new RottenTomatoesApi(apiKey)
  
  var results:Seq[RTMovie] = Nil;
  
  def getCommands(): Array[String] = { Array("films","film") }

  def processPrivateMessage(m: Message): Unit = {
    processChannelMessage(m)
  }

  def processChannelMessage(m: Message): Unit = {
    val parser = new CommandParser(m);
    
    if(parser.hasWord("boxoffice") || parser.hasWord("top")) {
      results = api.getInTheaters().toSeq
      showResults(m)
    } else if(parser.hasVar("num")) {
      showFilm(m,parser.getInt("num")+1)
    }
  }
  
  private def showResults(m:Message):Unit = {
    var i:Int=0;
    var reply="";
    for(movie <- results) {
      i=i+1;
      reply+= BOLD + i + ":" + BOLD + movie.getTitle() + " ";
    }
    
    m.reply(m.getSender() + ":" + reply);
  }
  
  private def showFilm(m:Message, num:Int):Unit = {
    if(results.length>0) {
      var movie = results.get(num);
      var reply = BOLD + movie.getTitle() + BOLD + "(" + movie.getYear() + ")" 
      if(movie.getCertification!=null)
        reply+=", certified " + movie.getCertification()
      if(movie.getDirectors().size()>0)
    	  reply += "Director(s):" + movie.getDirectors().map(_.getName()).mkString("&")
      reply+= " Runtime:" + movie.getRuntime()
      if(movie.getStudio()!=null)
    	  reply+= " Studio:" + movie.getStudio()
      if(movie.getRatings().size()>0)
        reply+=" Ratings: " + getRating(movie) 
      if(movie.getGenres().size()>0)
    	  reply += movie.getGenres().mkString(",")
      if(movie.getCriticsConsensus()!=null)
    	  reply+=" Critics say \"" + movie.getCriticsConsensus();
      if(movie.getSynopsis()!=null)
    	  reply+= "\" Synopsis:\"" + movie.getSynopsis() + "\""
      m.reply(reply);
    } else {
      m.createReply("You have to search for a film, before I can show you a film, foolish child.")
    }
  }
  
  //critics_rating:Certified Fresh, critics_score:73, audience_rating:Upright, audience_score:84
  private def getRating(film:RTMovie):String = {
    val ratings=film.getRatings().toMap
    var rating=""
    for(r<-ratings) {
      r._1 match {
      	case "critics_rating" => rating+=r._2+" "
      	case "critics_score" => rating+=" Critics:" + r._2
      	case "audience_score" => rating+=" Plebs:" + r._2
      	case _ => 
      }
    }
    rating
  }

}
