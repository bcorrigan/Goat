package goat.module

import goat.core.Module
import goat.core.Message
import goat.core.Constants._
import goat.util.CommandParser
import com.omertron.rottentomatoesapi._
import com.omertron.rottentomatoesapi.model._

import scala.collection.JavaConversions._

//todo let user save personal picks - films they want to watch etc

class RottenTomatoes extends Module {

  val apiKey = "qdcearnjeudmt6kq5kmtsq5w"
  
  val api = new RottenTomatoesApi(apiKey)
  
  private val STAR="★"
  private val STAR_12="✫"
  private val STAR_34="✰"
  private val STAR_14="☆"
  
  var results:Seq[RTMovie] = Nil;
  
  def getCommands(): Array[String] = { Array("films","film") }

  def processPrivateMessage(m: Message): Unit = {
    processChannelMessage(m)
  }

  def processChannelMessage(m: Message): Unit = {
    val parser = new CommandParser(m);
    
    if(parser.hasWord("boxoffice") || parser.hasWord("top")) {
      results = api.getInTheaters()
      showResults(m, "Top box office:")
    } else if(parser.hasVar("num") && !parser.hasRemaining()) {
        var num=sanitiseAndScoldForNum(m,parser)
        if(num!=0) {
        	showFilm(m,num-1)
        }
    } else if(parser.hasWord("dvd") || parser.hasWord("pirate")) {
      var lead=""
      if(parser.hasWord("new")) {
    	  results = api.getNewReleaseDvds()
    	  lead = "New to pirate"
      } else {
    	  results = api.getCurrentReleaseDvds()
    	  lead = "Best to pirate"
      }
      showResults(m, lead)
    } else if(parser.hasWord("upcoming")) {
      results = api.getUpcomingMovies();
      showResults(m, "Upcoming")
    } else if(parser.hasWord("search")) {
      results=api.getMoviesSearch(parser.remainingAfterWord("search"))
      showResults(m,"Results");
    } else if(parser.hasWord("similar")) {
      if(parser.hasVar("num")) {
        var num=sanitiseAndScoldForNum(m,parser)
        if(num!=0) {
        	results = api.getMoviesSimilar(results.get(num-1).getId())
        	showResults(m, "Similar")
        }
      } else {
        m.reply(m.getSender() + ": Similar to what?")
      } 
    } else if(parser.hasWord("help")) {
      m.reply("Usage: films [search] query | [to pirate] | [new to pirate] | [boxoffice] | upcoming.  film num=N to view a film in result set.");
    }
  }
  
  private def sanitiseAndScoldForNum(m:Message, parser:CommandParser):Int = {
    var num=parser.getInt("num")
    
    if(results.length<num || num<0) {
      m.reply("You're not being funny, you know.") 
      return 0;
    }
    return num;
  }
  
  private def showResults(m:Message, lead:String):Unit = {
      if(results.length==1)
        showFilm(m,0)
      else if (results.length==0)
        m.reply(m.getSender+": There were no results :-(")
      else {
    	var i:Int=0;
        var reply="";
        for(movie <- results) {
          i=i+1;
          reply+= BOLD + i + ":" + BOLD + movie.getTitle()
          val condensedRating = getStarRating(movie)
          if(condensedRating.length()>0)
    	    reply+="(" + getStarRating(movie) + ") ";
        }
        m.reply(lead+", " + reply);
    }
  }
  
  private def showFilm(m:Message, num:Int):Unit = {
    if(results.length>0) {
      var movie = results.get(num);
      var reply = BOLD + movie.getTitle() + BOLD + "(" + movie.getYear() + ")" 
      if(movie.getCertification!=null && !movie.getCertification.equals(""))
        reply+=", certified " + movie.getCertification()
      if(movie.getDirectors().size()>0)
    	  reply += "Director(s):" + movie.getDirectors().map(_.getName()).mkString("&")
      if(movie.getRuntime()>0)
    	  reply+= " Runtime:" + movie.getRuntime()
      if(movie.getStudio()!=null)
    	  reply+= " Studio:" + movie.getStudio()
      if(movie.getRatings().size()>0)
        reply+=getRating(movie) 
      if(movie.getGenres().size()>0)
    	  reply += movie.getGenres().mkString(",")
      if(movie.getCriticsConsensus()!=null && !movie.getCriticsConsensus().equals(""))
    	  reply+=" Critics say \"" + movie.getCriticsConsensus();
      if(movie.getSynopsis()!=null && !movie.getSynopsis().equals(""))
    	  reply+= "\" Synopsis:\"" + movie.getSynopsis() + "\""
      m.reply(reply);
    } else {
      m.reply("You have to search for a film, before I can show you a film, foolish child.")
    }
  }
  
  //critics_rating:Certified Fresh, critics_score:73, audience_rating:Upright, audience_score:84
  private def getRating(film:RTMovie):String = {
    var rating=""
    var score = getRatings(film)

    if(score.ponceFactor.isDefined)
    	rating+=" ponceFactor:" + score.ponceFactorString.get
    
    
    if(score.cS>0)
    	rating+=" critics:" + score.cS 
    if(score.aS>0)	
    	rating+=" plebs:" + score.aS
    
    rating
  }
  
  private def getRatings(film:RTMovie):Ratings = {
    val ratings=film.getRatings().toMap
    
    var cS,aS = 0
    var aR,cR = "";
    for(r<-ratings) {
      r._1 match {
      	case "critics_rating" => cR=r._2
      	case "critics_score" => cS=r._2.toInt
      	case "audience_score" => aS=r._2.toInt
      	case "audience_rating" => aR=r._2
      	case _ => 
      }
    }
    new Ratings(cS,aS,aR,cR);
  }
  
  private def getCondensedRating(film:RTMovie):String = {
    var score = getRatings(film);
    var condensedRating=""
    if(score.cS>75)
      condensedRating+=GREEN+"C"+NORMAL
    else if(score.cS<50&&score.cS>0)
      condensedRating+=RED+"C"+NORMAL
    else if(score.cS<=75&&score.cS>=50)
      condensedRating+="C"
    
    if(score.aS>75)
      condensedRating+=GREEN+"P"+NORMAL
    else if(score.aS<50&&score.aS>0)
      condensedRating+=RED+"P"+NORMAL
    else if(score.aS<=75&&score.aS>=50)
      condensedRating+="P"
    
    condensedRating+=score.ponceFactorString.getOrElse("")
        
    condensedRating
  }
  
  private case class Ratings(cS:Int,aS:Int,aR:String,cR:String) {
    val ponceFactor:Option[Double] = if(cS>0&&aS>0) Some(cS/aS.toDouble) else None
    val ponceFactorString:Option[String] = if(ponceFactor.isDefined) Some("%1.2f" format ponceFactor.get) else None
  };
  
  private def getStarRating(film:RTMovie):String = {
    var score = getRatings(film)
    var starRating=""
    if(score.cS>0) {
      starRating+=ratingToStars(score.cS)
    } else if(score.aS>0) {
      starRating+=ratingToStars(score.aS)
    }
    return starRating
  }
  
  private def stepToStar(step:Int):String = {
    step match {
      case 20 => return STAR
      case 15 => return STAR_34
      case 10 => return STAR_12
      case 5 => return STAR_14
      case _ => return ""
    }
  }
  
  private def extractStars(rating:Int,step:Int):Tuple2[Int,String] = {
    var r=rating;
    var stars:String="";
    while(r>step) {
      r-=step
      stars+=stepToStar(step);
    }
    
    return new Tuple2(r,stars)
  }
  
  private def ratingToStars(rating:Int):String = {
    var r=rating
    var stars=""
    
    List(20,15,10,5) foreach { step=>
      var t=extractStars(r,step)
      stars+=t._2
      r=t._1
    }
      
    return stars
  }

}
