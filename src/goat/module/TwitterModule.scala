package goat.module

import goat.core.Constants._
import goat.util.StringUtil
import goat.core.Module
import goat.core.Message
import org.apache.commons.lang.StringEscapeUtils.unescapeHtml

import scala.actors.Actor._
import scala.collection.immutable.HashSet

import goat.util.Profile._
import goat.util.CommandParser

import java.lang.System
import goat.Goat
import twitter4j.auth.{Authorization, AccessToken}
import twitter4j.conf._
import twitter4j._

import scala.collection.JavaConversions._


/*
 * Lets have the vapid outpourings of the digerati in goat.
 * TODO make ids List not array and convert when sending and receiving it
 * TODO trends search would be nice
 * Consumer key: ZkKYRoR7lPZQBfFrxrpog
 * Consumer secret: lHbOOtBPi8JmIPOf3MAS0eXB2yUiPrUHnPtVkBWuG4
 * Access token: 57130163-b1BquTcyX6rHLOXwnGts9zfY9WymA99GQbIhH6VMg
 * Access token sekret: Ye9bo17hKMxyCb9IaGgv7RVnRY5qUxOaxjq5w6l0
 * useId: 57130163
 * Goat user is: goatbot/slashnet
 * @author Barry Corrigan
 */
class TwitterModule extends Module {
  private var lastFilterTime: Long = 0 //these few vars are for some stats keeping
  private var filterTimeAvg: Long = 0
  private var filterCount: Int = 0 
  
  private val consumerKey = "ZkKYRoR7lPZQBfFrxrpog"
  private val consumerSecret = "lHbOOtBPi8JmIPOf3MAS0eXB2yUiPrUHnPtVkBWuG4"
  private val accessToken = "57130163-b1BquTcyX6rHLOXwnGts9zfY9WymA99GQbIhH6VMg"
  private val accessTokenSecret = "Ye9bo17hKMxyCb9IaGgv7RVnRY5qUxOaxjq5w6l0"

  private var searchSize = 50 //how many tweets should a request retrieve at once

  private var lastTweet: Long = 0
  private var lastPurge: Long = System.currentTimeMillis
  private val purgePeriod: Int = 10 //interval in minutes when we garbage collect

  //OAuth connection bullshit
  private val token = new AccessToken(accessToken,accessTokenSecret)
  
  val cb = new ConfigurationBuilder();

  cb.setDebugEnabled(true)
  .setOAuthConsumerKey(consumerKey)
  .setOAuthConsumerSecret(consumerSecret)
  .setOAuthAccessToken(accessToken)
  .setOAuthAccessTokenSecret(accessTokenSecret);
  
  private val twitter = new TwitterFactory(cb.build()).getInstance()

  private val USER = "goatbot"
  private val PASSWORD = "slashnet"
  private var chan = "jism" //BotStats.getInstance().getChannels()(0)
  //for messaging actor
  private val TWEET = "TWEET"
  private val TWEET_TOPIC = "TWEET_TOPIC"
  private val MAINCHANNEL = "MAINCHANNEL"
  private val FOLLOW = "FOLLOW"
  private val UNFOLLOW = "UNFOLLOW"
  private val SEARCH = "SEARCH"
  private val TRENDS = "TRENDS"
  private val TRENDSNOTIFY = "TRENDSNOTIFY"

  //HashSet of tuples (queries and tweets), we will use this as a local cache
  //should make a facility for this generic
  private var searchResults: Set[Tuple2[String, List[Tweet]]] = new HashSet

  private var followedIDs: Array[Long] = null
  refreshIdsToFollow()

  //some random stats to see how effective the cache is
  private var searchesMade: Int = 0;
  private var cacheHits: Int = 0

  private val streamTwitter: TwitterStream = new TwitterStreamFactory().getInstance(twitter.getAuthorization())

  streamTwitter.addListener( new GoatStatusListener() )

  followIDs(followedIDs)


  private def refreshTwitterStream() {
    refreshIdsToFollow()
    followIDs(followedIDs)
  }

  private def followIDs(followIDs:Array[Long]) {
    val query = new FilterQuery(followIDs)
    streamTwitter.filter(query)
  }

  //this actor will send tweets, do searches, etc - we can fire up several of these
  private val twitterActor = actor {
    //val twitter: Twitter = new Twitter("goatbot", "slashnet")
    while (true) {
      receive {
        case (msg: Message, TWEET) =>
          if (tweetMessage(msg, msg.getModTrailing))
            msg.reply("Most beneficant Master " + msg.getSender + ", I have tweeted your wise words.")
        case (msg: Message, TWEET_TOPIC) =>
          tweetMessage(msg, msg.getTrailing)
        case (msg: Message, FOLLOW) =>
          enableNotification(msg, msg.getModTrailing.trim())
          refreshTwitterStream()
        case (msg: Message, UNFOLLOW) =>
          disableNotification(msg, msg.getModTrailing.trim())
          refreshTwitterStream()
        case (msg: Message, SEARCH) =>
          queryTwitter(msg, msg.getModTrailing.trim().toLowerCase())
        case (msg: Message, TRENDS) =>
          showTrends(msg)
      }
    }
  }
  
  private val trendsNotifyActor = actor {
    while (true) {
      receive {
        case (chan: String, TRENDSNOTIFY) =>
          trendsNotify(chan)
      }
    }
  }
  
  //TODO use receiveWithin and TIMEOUT
  private def trendsNotify(chan:String) {
    var seenTrends:List[String] = Nil
    
    while(true) {
      try {
	    val trends = twitter.getTrends.getTrends.toList.map(_.getName)
	    val newTrends = trends diff seenTrends
	
	    if(!newTrends.isEmpty) {
	      val msgTrends = newTrends map( t => if(t.startsWith("#") && t.length>1) {
	        								 t.substring(1)
            								 //hey rs, this is where you can hook in and turn "reasonstobeatgirlfriend" into somethign readable
	        							 } else t)
	        
		  val msg = msgTrends reduce ((t1,t2) => t1 + ", " + t2)
		  Message.createPrivmsg(chan, msg).send()
		      
		  seenTrends = (newTrends ++ seenTrends).take(1000)
	    }
	    Thread.sleep(60000)
	  } catch {
        case ex:Exception =>
        ex.printStackTrace()
        Thread.sleep(60000)
      }
    } 
  }
  
  private def showTrends(m: Message) {
    try {
	    val parser = new CommandParser(m);
	    val query: Query = new Query(parser.remaining())
	    val user = Goat.getUsers().getOrCreateUser(m.getSender)
	    var woeId: Option[Int] = None
	
	    if(parser.has("near") || m.getModTrailing.contains("near")) {
	      val woeIdStr = parser.get("near")
	      if(woeIdStr==null) {
	        woeId = Some(user.getWoeId())
	      } else {
	        if(woeIdStr.trim().matches("\\d+")) {
	          woeId = Some(Integer.parseInt(woeIdStr.trim()))
	        }
	      }
	    }
	    
	    var reply = "Trends of the moment"
	    
	    val trends: List[Trend] = if (woeId.isEmpty) {
	      reply += ": "
	      twitter.getTrends().getTrends().toList
	    } else {
	      val locTrends = twitter.getLocationTrends(woeId.get)
	      reply += " for " + locTrends.getLocation().getName() + ": "
	      locTrends.getTrends.toList
	    }
	    
	    var count = 1
	    for (trend <- trends) {
	      reply += " " + BOLD + count + ":" + BOLD + trend.getName()
	      count += 1
	    }
	    m.reply(reply)
    } catch {
      case ex: TwitterException =>
        ex.printStackTrace
        m.reply("Twitter is not providing me with trends, sorry." + ex.getMessage)
    }
  }

  private def queryTwitter(m: Message, queryString: String) {
    try {
      val parser = new CommandParser(m);
      val query: Query = new Query(parser.remaining())
      val user = Goat.getUsers().getOrCreateUser(m.getSender)
      if (parser.has("radius") || parser.has("location")) {
        //parse radius
        var radius: Double = 50 //50km by default
        if (parser.has("radius")) {
          try {
            radius = java.lang.Double.parseDouble(parser.get("radius"))
          } catch {
            case nfe: NumberFormatException =>
              m.reply(m.getSender + ": Radius argument expects a number.")
              return
          }
        }
        var latitude = user.getLatitude
        var longitude = user.getLongitude
        if (parser.has("location")) {
          val url = parser.get("location")
          try {
            val location: Array[Double] = StringUtil.getPositionFromMapsLink(url)
            latitude = location(0)
            longitude = location(1)
          } catch {
            case nfe: NumberFormatException =>
              m.reply(m.getSender + ": you need to supply a valid google maps link.")
              return
          }
        }
        val geo = new GeoLocation(latitude,longitude)
        query.setGeoCode(geo, radius, Query.KILOMETERS)
      }

      query.setRpp(searchSize)
      val results = twitter.search(query).getTweets().toArray()
      if (results.size == 0)
        m.reply("Nobody has tweeted about that shit recently.")
      else {
        addTweetsToCache(queryString, results)
        popTweetToChannel(m, queryString)
      }
    } catch {
      case ex: TwitterException =>
        ex.printStackTrace()
        val ind = ex.getMessage().indexOf("\":\"")
        val errorMsg = ex.getMessage().substring(ind + 3).replaceFirst("\"}", "")
        m.reply("Oh dear, twitter says: " + errorMsg)
    }
  }

  private def addTweetsToCache(query: String, tweetsArr: Array[Object]) {
    //condense tweets - strip dups
    val tweets: List[Tweet] = filterSimilarTweets(tweetsArr.toList.asInstanceOf[List[Tweet]])
    searchResults = searchResults + Tuple2(query, tweets) //Tuple2 needed or just for eclipse?
  }

  private def filterSimilarTweets(tweets: List[Tweet]): List[Tweet] = {
    val start = System.currentTimeMillis
    //a map of the tweets we have already seen, against the count of similar tweets (which we throw away)
    var filtCount: Map[Tweet, Int] = Map()

    for (t <- tweets) {
      val simTweet = firstSimilarTweet(filtCount.keys.toList, t)
      if (simTweet.isEmpty)
        filtCount += t -> 1
      else
        filtCount += simTweet.get -> (filtCount.get(simTweet.get).get + 1)
    }

    var filtTweetsPaired: List[Tuple2[Tweet, Int]] = filtCount.toList
    var sortedTweetsPaired: List[Tuple2[Tweet, Int]] = Nil
    //sort this by spamminess (ie the Int)
    filtTweetsPaired = filtTweetsPaired.sortWith((t1, t2) => t1._2 < t2._2)
    //now do takeWhile chunks and sort those chunks into new list by secondary sort key, the time posted at
    while (filtTweetsPaired.length > 0) {
      val chunk = filtTweetsPaired.takeWhile(_._2 == filtTweetsPaired.head._2)
      sortedTweetsPaired ++= chunk.sortWith(_._1.getCreatedAt.getTime > _._1.getCreatedAt.getTime)
      filtTweetsPaired = filtTweetsPaired.dropWhile(_._2 == filtTweetsPaired.head._2)
    }

    lastFilterTime = System.currentTimeMillis - start
    if (tweets.size == searchSize) {
      filterCount += 1
      filterTimeAvg = filterTimeAvg + (lastFilterTime - filterTimeAvg) / filterCount
    }
    sortedTweetsPaired.map(_._1)
  }

  /**
   * Are the two tweets passed in 50% or more similar to each other?
   */
  private def similar(t1: Tweet, t2: Tweet): Boolean = {
    val distLimit = t1.getText.length / 2
    var dist = 0
    //levlim is expensive call, guard just says don't call it for most obviously different cases based on string length
    if (t2.getText().length > distLimit && t2.getText().length < (distLimit * 3))
      dist = StringUtil.levlim(t1.getText, t2.getText, distLimit)
    else return false;
    return dist < distLimit
  }

  private def firstSimilarTweet(l: List[Tweet], t: Tweet): Option[Tweet] = {
    l.find(similar(t, _))
    //l.filter(similar(t,_))
  }

  private def enableNotification(m: Message, user: String) {
    try {
      if (twitter.enableNotification(user) == null)
        m.reply("Oh Master, I am now following that user just as you desire.")
      else m.reply("Looks like that user doesn't exist, my master.")
    } catch {
      case ex: TwitterException =>
        ex.printStackTrace()
        m.reply("Master, I must beg forgiveness. Those lackies at twitter have failed us. I was unable to follow that user as a result. Error: " + ex.getMessage)
    }
  }

  private def disableNotification(m: Message, user: String) {
    try {
      if (twitter.disableNotification(user) == null)
        m.reply("Oh Wise Master, I am no longer following that user.")
      else m.reply("Master! That user - it does not exist. I beg forgiveness.")
    } catch {
      case ex: TwitterException =>
        ex.printStackTrace()
        m.reply("Master, I must beg forgiveness. Those lackies at twitter have failed us. I was unable to cease following that user as a result. Error: " + ex.getMessage)
    }
  }

  private def tweetMessage(m: Message, message: String): Boolean = {
    try {
      twitter.updateStatus(m.getTrailing)
      true
    } catch {
      case ex: TwitterException =>
        ex.printStackTrace()
        m.reply("Some sort of problem with twitter: " + ex.getMessage)
        false
    }
  }

  //return true if we found one
  private def popTweetToChannel(m: Message, query: String): Boolean = {
    //find a matching tweet in cache
    searchResults.find(_._1 == query) match {
      case None =>
        false
      case Some(result) =>
        val tweet: Tweet = result._2.head
        searchResults = searchResults - result
        if (result._2.length > 1 && result._2.tail.length > 0)
          searchResults = searchResults + Tuple2(result._1, result._2.tail)
        m.reply(ageOfTweet(tweet) + " ago, " + BOLD + tweet.getFromUser + BOLD + ": " + unescapeHtml(tweet.getText).replaceAll("\n", ""))
        true
    }
  }


  private def ageOfTweet(tweet: Tweet): String = {
    return StringUtil.shortDurationString(System.currentTimeMillis - tweet.getCreatedAt.getTime)
  }

  //there has to be a better way to do this array munging ffs
  private def fetchFriendStatuses(twitter: Twitter): List[String] = {
    val statuses = twitter.getFriendsTimeline().toArray(new Array[Status](0))
    var strStatuses: List[String] = Nil
    for (status <- statuses)
      strStatuses = (BOLD + status.getUser.getName + NORMAL + ": " + status.getText) :: strStatuses
    strStatuses.reverse
  }

  private def refreshIdsToFollow() {
    followedIDs = twitter.getFriendsIDs(1l).getIDs
  }

  private def sendStatusToChan(status: Status, chan: String) {
    Message.createPrivmsg(chan, BOLD + status.getUser.getName + NORMAL + ": " + status.getText)
  }

  def processPrivateMessage(m: Message) {
    processChannelMessage(m)
  }

  def processChannelMessage(m: Message) {
    manageCache()
    m.getModCommand.toLowerCase() match {
      case "tweet" =>
        if (m.isAuthorised())
          tweet(m)
        else m.reply("You are " + BOLD + "not" + BOLD + " my Master.")
      case "tweetchannel" =>
        chan = m.getChanname
        Message.createPrivmsg(chan, "This channel is now the main channel for twitter following.").send()
      case "follow" =>
        if (m.isAuthorised())
          twitterActor ! (m, FOLLOW)
        else
          m.reply(m.getSender + ": You're no master of mine. Go and follow your own arsehole..")
      case "unfollow" =>
        if (m.isAuthorised())
          twitterActor ! (m, UNFOLLOW)
        else
          m.reply(m.getSender + ": You don't tell me what to do. I'll listen to who I like.")
      case "tweetsearch" | "twitsearch" | "twittersearch" | "inanity" | "t" =>
        if (sanitiseAndScold(m))
          if (!popTweetToChannel(m, m.getModTrailing.trim().toLowerCase)) {
            searchesMade += 1
            twitterActor ! (m, SEARCH)
          } else cacheHits += 1
      case "tweetstats" =>
        m.reply(m.getSender + ": Searches made:" + (searchesMade+cacheHits) + " Network hits:" + searchesMade + " Cache hits:" + cacheHits + " Cache size:" + cacheSize
                + " Last filter time:" + lastFilterTime + "ms"
                + " Avg. Filter Time:" + filterTimeAvg + "ms")
      case "trends" | "localtrends" =>
        twitterActor ! (m, TRENDS)
      case "tweetpurge" =>
        tweetpurge(m)
      case "tweetsearchsize" =>
        if (m.isAuthorised()) {
          //we expect one argument, a number
          val numString = m.getModTrailing.trim()
          try {
            searchSize = Integer.parseInt(numString)
            m.reply(m.getSender + ": I set search size to " + searchSize + ", as you commanded, my liege.")
          } catch {
            case ex: NumberFormatException =>
              m.reply(m.getSender + ": That's rubbish, try specifying a simple integer.")
          }
        } else m.reply(m.getSender + ": You are not as handsome, nor as intelligent, as I expect my master to be, so I will not do that.")
      case "trendsnotify" =>
        if( m.isAuthorised()) {
          if(m.getModTrailing.split(' ').size!=1) 
            m.reply("Master, your humble servant can only follow one channel at once. Forgive me.")
          else {
            val chan = m.getModTrailing().split(' ')(0)
            trendsNotifyActor ! (chan, TRENDSNOTIFY)
          }
        }
    }
  }

  private def sanitiseAndScold(m: Message): Boolean = {
    if (m.getModTrailing.trim.length == 0) {
      m.reply(m.getSender + ": Twitter might be inane, but you still need to tell me to search for *something*.")
      return false
    }
    true
  }

  private def tweetpurge(m: Message) {
    if (m.getModTrailing.split("\\s+").size > 1)
      m.reply(m.getSender + ": that doesn't look at all right. Try just specifying a number, then I will purge all tweets older than that many minutes.")
    else {
      var mins: Int = 0;
      try {
        mins = Integer.parseInt(m.getModTrailing.trim)
        m.reply(m.getSender + ": Purged " + purge(mins) + " tweets from cache.")
      } catch {
        case ex: NumberFormatException =>
          m.reply(m.getSender + ": that's no good at all. Try just specifying a number, then I will purge all tweets older than that many minutes.")
      }
    }
  }

  private def cacheSize(): Int = {
    searchResults.foldLeft(0)((sum, x) => sum + x._2.length)
  }

  //purge all the tweets older than age (age is in minutes), return the number purged
  private def purge(age: Int): Int = {
    val ageMillis: Long = age * MINUTE
    val now = System.currentTimeMillis
    val purged = searchResults.filter(x => (now - x._2.head.getCreatedAt.getTime) > ageMillis)
    searchResults = searchResults.filter(x => (now - x._2.head.getCreatedAt.getTime) < ageMillis)
    purged.foldLeft(0)((sum, x) => sum + x._2.length)
  }

  private def manageCache() {
    val now = System.currentTimeMillis
    if ((now - lastPurge) > purgePeriod * MINUTE) {
      purge(purgePeriod)
    }
    lastPurge = now
  }

  override def processOtherMessage(m: Message) {
    val now = System.currentTimeMillis
    if (m.getCommand == TOPIC && (now - lastTweet) > 10 * MINUTE) {
      //twitter.updateStatus(m.getTrailing)
      twitterActor ! (m, TWEET_TOPIC)
      lastTweet = now
    }
  }

  private def tweet(m: Message) {
    val now = System.currentTimeMillis
    if ((now - lastTweet) > HOUR || m.isAuthorised()) {
      //twitter.updateStatus(m.getModTrailing)
      twitterActor ! (m, TWEET)
      m.reply("Tweet!")
      lastTweet = now
    } else {
      m.reply("Don't ask me to be a blabbermouth. I tweeted only " + StringUtil.durationString(now - lastTweet) + " ago.")
    }
  }

  override def messageType = Module.WANT_COMMAND_MESSAGES

  def getCommands(): Array[String] = {
    Array("tweet", "tweetchannel", "follow", "unfollow", "tweetsearch", "twitsearch", "twittersearch", "inanity", "tweetstats", "trends","localtrends", "tweetpurge", "tweetsearchsize", "trendsnotify", "t")
  }

  private def filterIDs(ids: Array[Int]): Array[Int] = {
    var filteredIDs: List[Int] = Nil

    val idsList = followedIDs.toList

    for (id <- ids) {
      if (idsList.contains(id)) {
        filteredIDs = id :: filteredIDs
      }
    }

    filteredIDs.toArray: Array[Int] //ugly shit
  }

  private def isFollowed(id: Long): Boolean = {
    val idsList = followedIDs.toList
    if (idsList.contains(id)) {
      return true
    }
    false
  }

  class GoatStatusListener extends StatusListener {
    def onException(e: Exception) {
      //pass
      e.printStackTrace()
    }

    def onStatus(status: Status) {
      if (isFollowed(status.getUser.getId))
        sendStatusToChan(status, chan);
      println(status.getText)
    }

    def onDeletionNotice(statusDeletionNotice:StatusDeletionNotice) {
      //TODO ignore for now ; should really remove the deleted account
    }

    def onTrackLimitationNotice(numberOfLimitedStatuses:Int) {
      //ignore, now and forever
    }
    
    def onScrubGeo(userId:Long, upToStatusId:Long) {
      //who gives a shit
    }
  }
}

