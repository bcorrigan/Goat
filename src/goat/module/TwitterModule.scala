package goat.module

import actors.TIMEOUT
import goat.core.Constants._
import goat.util.StringUtil
import goat.core.BotStats
import goat.core.Module
import goat.core.Message

import twitter4j.Trend
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.TwitterStream
import twitter4j.Tweet
import twitter4j.Status
import twitter4j.StatusStream
import twitter4j.StatusListener
import twitter4j.User
import twitter4j.Query
import twitter4j.QueryResult

import org.apache.commons.lang.StringEscapeUtils.unescapeHtml

import scala.actors.Actor._
import scala.collection.immutable.HashSet
import java.util.Date


/**
 * Lets have the vapid outpourings of the digerati in goat.
 * TODO make ids List not array and convert when sending and receiving it
 * TODO trends search would be nice
 * @author Barry Corrigan
 */
class TwitterModule extends Module {

  private var lastTweet:Long = 0
  private var lastPurge:Long = System.currentTimeMillis
  private var purgePeriod:Int = 10 //interval in minutes when we garbage collect
  private val twitter:Twitter = new Twitter("goatbot","slashnet")
  private val USER = "goatbot"
  private val PASSWORD = "slashnet"
  private var chan = "goat" //BotStats.getInstance().getChannels()(0)
  //for messaging actor
  private val TWEET = "TWEET"
  private val TWEET_TOPIC = "TWEET_TOPIC"
  private val MAINCHANNEL = "MAINCHANNEL"
  private val FOLLOW = "FOLLOW"
  private val UNFOLLOW = "UNFOLLOW"
  private val SEARCH = "SEARCH"
  private val TRENDS = "TRENDS"
	  
  //HashSet of tuples (queries and tweets), we will use this as a local cache
  //should make a facility for this generic
  private var searchResults:Set[Tuple2[String,List[Tweet]]] = new HashSet
            
  private var followedIDs:Array[Int] = null
  refreshIdsToFollow()
  
  //some random stats to see how effective the cache is
  private var searchesMade:Int = 0;
  private var cacheHits:Int = 0
  
  private val streamTwitter:TwitterStream = new TwitterStream(USER,PASSWORD, new GoatStatusListener()) 
  streamTwitter.follow(followedIDs)

  
  private def refreshTwitterStream() {
	  refreshIdsToFollow()
	  streamTwitter.follow(followedIDs)  
  }

  //this actor will send tweets, do searches, etc - we can fire up several of these
  private val twitterActor = actor {
	val twitter:Twitter = new Twitter("goatbot","slashnet")
    while(true) {
      receive {
        case (msg:Message, TWEET) =>
          if( tweetMessage(msg, msg.getModTrailing) )
        	  msg.createReply("Most beneficant Master " + msg.getSender + ", I have tweeted your wise words.").send()
        case (msg:Message, TWEET_TOPIC) =>
          tweetMessage(msg, msg.getTrailing)
        case (msg:Message, FOLLOW) =>
          enableNotification(msg, msg.getModTrailing.trim())
          refreshTwitterStream()
        case (msg:Message, UNFOLLOW) =>
          disableNotification(msg,msg.getModTrailing.trim())
          refreshTwitterStream()
        case (msg:Message, SEARCH) =>
          queryTwitter(msg, msg.getModTrailing.trim().toLowerCase())
        case (msg:Message, TRENDS) =>
          showTrends(msg)
      }     
    }
  }
  
  private def showTrends(m:Message) {
	try {
		val trends:List[Trend] = List.fromArray(twitter.getTrends().getTrends())
		//iterate over each trend and splat into channel
		var reply = "Today's Trends: "
		var count = 1
		for(trend <- trends) {
			reply += " " + BOLD + count + ":" + BOLD + trend.getName()
			count += 1
		}
	  	m.createPagedReply(reply).send()
	} catch {
		case ex:TwitterException =>
			ex.printStackTrace()
			m.createReply("Twitter is not providing me with trends, terribly sorry." + ex).send()
	}
  }
  
  //fire up a few actors
  twitterActor.start()
  twitterActor.start()
  twitterActor.start()
  
  private def queryTwitter(m:Message, queryString:String) {
	try {
		val query:Query = new Query(queryString)
	  	query.setRpp(50)
	  	val results = twitter.search(query).getTweets().toArray()
	  	addTweetsToCache(queryString, results)
	  	if(results.size==0)
		  m.createReply("Nobody has tweeted about that shit recently.").send()
		else 
		  popTweetToChannel(m, queryString)
	} catch {
		case ex:TwitterException =>
			ex.printStackTrace()
			m.createReply("Oh dear, there's a problem with twitter: " + ex ).send()
	}
  }
  
  private def addTweetsToCache(query:String, tweets:Array[Object]) {
		searchResults = searchResults + Tuple2(query,tweets.toList.asInstanceOf [List[Tweet]]) //Tuple2 needed or just for eclipse?
  }
  
  private def enableNotification(m:Message, user:String) {
	  try { 
		  if( twitter.enableNotification(user) == null )
			  m.createReply("Oh Master, I am now following that user just as you desire.").send()			  
		  else m.createReply("Looks like that user doesn't exist, my master.").send()
	  } catch {
		  case ex:TwitterException =>
		  	ex.printStackTrace()
		  	m.createReply("Master, I must beg forgiveness. Those lackies at twitter have failed us. I was unable to follow that user as a result. Error: " + ex.getMessage).send()
	  }
  }
  
  private def disableNotification(m:Message, user:String) {
	  try { 
		  if( twitter.disableNotification(user) == null )
			  m.createReply("Oh Wise Master, I am no longer following that user.").send()
		  else m.createReply("Master! That user - it does not exist. I beg forgiveness.").send() 
	  } catch {
		  case ex:TwitterException =>
		  	ex.printStackTrace()
		  	m.createReply("Master, I must beg forgiveness. Those lackies at twitter have failed us. I was unable to cease following that user as a result. Error: " + ex.getMessage).send()
	  }
  }
  
  private def tweetMessage(m:Message, message:String):Boolean = {
	  try {
		  twitter.updateStatus(m.getTrailing)
		  true
	  } catch {
		  case ex:TwitterException =>
		  	ex.printStackTrace()
		  	m.createReply("Some sort of problem with twitter: " + ex.getMessage ).send()
		  	false
	  }
  }
  
  //return true if we found one
  private def popTweetToChannel(m:Message, query:String):Boolean = {
	  //find a matching tweet in cache
	  searchResults.find(_._1 == query) match {
		  case None =>
		  	false
		  case Some(result) =>
        val tweet:Tweet = result._2.head
        searchResults = searchResults - result
        if(result._2.tail.length>0)
          searchResults = searchResults + Tuple2(result._1,result._2.tail)
		  	m.createReply(ageOfTweet(tweet) + " ago, " + BOLD + tweet.getFromUser + BOLD + ": " + unescapeHtml(tweet.getText)).send()
		  	true
	  }
  }

  private def ageOfTweet(tweet:Tweet):String = {
    return StringUtil.shortDurationString(System.currentTimeMillis-tweet.getCreatedAt.getTime)
  }

  //there has to be a better way to do this array munging ffs
  private def fetchFriendStatuses(twitter:Twitter):List[String] = {
    val statuses = twitter.getFriendsTimeline().toArray(new Array[Status](0))
    var strStatuses:List[String] = Nil
    for(status:Status <- statuses)
      strStatuses = ( BOLD + status.getUser.getName + NORMAL + ": " + status.getText ):: strStatuses 
    strStatuses.reverse
  }
  
  private def refreshIdsToFollow() {
  	followedIDs = twitter.getFriendsIDs().getIDs
  }

  private def sendStatusToChan(status:Status, chan:String) {
    Message.createPrivmsg(chan, BOLD + status.getUser.getName + NORMAL + ": " + status.getText ).send()
  }
  
  def processPrivateMessage(m:Message) {
    processChannelMessage(m)
  }
  
  def processChannelMessage(m:Message) {
    manageCache()
    m.getModCommand.toLowerCase() match {
      case "tweet" =>
      	if(m.isAuthorised())
      		tweet(m)
      	else m.createReply("You are " + BOLD + "not" + BOLD + " my Master.").send()
      case "tweetchannel" =>
        chan = m.getChanname
        Message.createPrivmsg(chan, "This channel is now the main channel for twitter following.").send()
      case "follow" =>
        if(m.isAuthorised())
        	twitterActor ! (m, FOLLOW)
        else
        	m.createReply(m.getSender + ": You're no master of mine. Go and follow your own arsehole..").send()
      case "unfollow" =>
        if(m.isAuthorised())
        	twitterActor ! (m, UNFOLLOW)
        else 
        	m.createReply(m.getSender + ": You don't tell me what to do. I'll listen to who I like.").send()
      case "tweetsearch" | "twitsearch" | "twittersearch" | "inanity" =>
        searchesMade += 1
        if(!popTweetToChannel(m, m.getModTrailing.trim().toLowerCase))
        	twitterActor ! (m, SEARCH)
        else cacheHits += 1
      case "tweetstats" =>
        m.createReply(m.getSender + ": Searches made:" + searchesMade + " Cache hits:" + cacheHits + " Cache size:" + cacheSize ).send() 
      case "trends" =>
        	twitterActor ! (m, TRENDS)
      case "tweetpurge" =>
        tweetpurge(m)
    }
  }

  private def tweetpurge(m:Message) {
    if(m.getModTrailing.split("\\s+").size>1)
      m.createReply(m.getSender + ": that doesn't look at all right. Try just specifying a number, then I will purge all tweets older than that many minutes.").send()
    else {
      var mins:Int = 0;
      try {
        mins = Integer.parseInt(m.getModTrailing.trim)
        m.createReply(m.getSender + ": Purged " + purge(mins) + " tweets from cache.").send
      } catch {
        case ex:NumberFormatException =>
          m.createReply(m.getSender + ": that's no good at all. Try just specifying a number, then I will purge all tweets older than that many minutes.").send()
      }
    }
  }

  private def cacheSize():Int = {
    searchResults.foldLeft(0)((sum,x) => sum + x._2.length)
  }

  //purge all the tweets older than age (age is in minutes), return the number purged
  private def purge(age:Int):Int = {
    val ageMillis:Long = age*60*1000
    val now = System.currentTimeMillis
    val purged = searchResults.filter(x => (now-x._2.head.getCreatedAt.getTime)>ageMillis)
    searchResults = searchResults.filter(x => (now-x._2.head.getCreatedAt.getTime)<ageMillis)
    purged.foldLeft(0)((sum,x) => sum + x._2.length)
  }

  private def manageCache() {
    val now = System.currentTimeMillis
    if ((now-lastPurge)>purgePeriod*60*1000) {
      purge(purgePeriod)
    }
    lastPurge=now
  }
  
  override def processOtherMessage( m:Message) {
    val now = System.currentTimeMillis
	  if ( m.getCommand == TOPIC && (now-lastTweet)>10*MINUTE) {
		  //twitter.updateStatus(m.getTrailing)
		  twitterActor ! (m,TWEET_TOPIC)
		  lastTweet = now
	  }
  }

  private def tweet(m:Message) {
    val now = System.currentTimeMillis
    if((now-lastTweet)>HOUR || m.isAuthorised() )  {
      //twitter.updateStatus(m.getModTrailing)
      twitterActor ! (m,TWEET)
      m.createReply("Tweet!").send()
      lastTweet = now
    } else {
      m.createReply("Don't ask me to be a blabbermouth. I tweeted only " + StringUtil.durationString(now-lastTweet) + " ago." ).send()
    }
  }
  
  override def messageType = Module.WANT_COMMAND_MESSAGES
  
  def getCommands():Array[String] = {
	  Array("tweet", "tweetchannel", "follow", "unfollow", "tweetsearch", "twitsearch", "twittersearch", "inanity", "tweetstats", "trends", "tweetpurge")
  }
  
  private def filterIDs(ids: Array[Int]):Array[Int] = {
    var filteredIDs:List[Int] = Nil

    var idsList = List.fromArray(followedIDs)

    for(id<-ids) {
      if(idsList.contains(id)) {
        filteredIDs = id :: filteredIDs
      }
    }

    filteredIDs.toArray: Array[Int] //ugly shit
  }
  
  private def isFollowed(id:Int):Boolean = {
    var idsList = List.fromArray(followedIDs)
    if(idsList.contains(id)) {
      return true
    }
    false
  }

  class GoatStatusListener extends StatusListener {
	
    def onException(e:Exception) {
      //pass
      e.printStackTrace()
    }

    def onStatus(status:Status) {
      if( isFollowed(status.getUser.getId))
        sendStatusToChan(status,chan);
      println(status.getText)
    }
  }
}
