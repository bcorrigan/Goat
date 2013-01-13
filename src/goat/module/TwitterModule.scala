package goat.module

import goat.core.Constants._
import goat.util.StringUtil
import goat.core.Module
import goat.core.Message
import goat.util.Profile._
import goat.util.CommandParser
import goat.util.Passwords._
import goat.Goat
import goat.util.TranslateWrapper;

import scala.actors.Actor._
import scala.collection.immutable.HashSet
import scala.collection.mutable.Map
import scala.collection.JavaConversions._

import java.lang.System

import twitter4j.auth.{Authorization, AccessToken}
import twitter4j.conf._
import twitter4j._

import org.apache.commons.lang.StringEscapeUtils.unescapeHtml

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

  private var pwds = getPasswords() 
  private val consumerKey = pwds.getProperty("twitter.consumerKey")
  private val consumerSecret = pwds.getProperty("twitter.consumerSecret")
  private val accessToken = pwds.getProperty("twitter.accessToken")
  private val accessTokenSecret = pwds.getProperty("twitter.accessTokenSecret")
  
  pwds = null // lame, yes. but it's Good Housekeeping; the object contains many
              // other passwords, and shouldn't be kept in memory.

  private var searchSize = 50 //how many tweets should a request retrieve at once

  private var lastOutgoingTweetTime: Long = 0
  private var lastPurge: Long = System.currentTimeMillis
  private val purgePeriod: Int = 10 //interval in minutes when we garbage collect
  
  private val translator = new TranslateWrapper()

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
  private var chan = "#jism" //BotStats.getInstance().getChannels()(0)
  //for messaging actor
  private val TWEET = "TWEET"
  private val TWEET_TOPIC = "TWEET_TOPIC"
  private val MAINCHANNEL = "MAINCHANNEL"
  private val FOLLOW = "FOLLOW"
  private val UNFOLLOW = "UNFOLLOW"
  private val SEARCH = "SEARCH"
  private val TRANSLATE = "TRANSLATE"
  private val TRENDS = "TRENDS"
  private val TRENDSNOTIFY = "TRENDSNOTIFY"

  //HashSet of tuples (queries and tweets), we will use this as a local cache
  //should make a facility for this generic
  private var searchResults: Set[Tuple2[String, List[Status]]] = new HashSet

  private var followedIDs: Array[Long] = null
  refreshIdsToFollow()

  //some random stats to see how effective the cache is
  private var searchesMade: Int = 0;
  private var cacheHits: Int = 0

  private val streamTwitter: TwitterStream = new TwitterStreamFactory().getInstance(twitter.getAuthorization())

  //streamTwitter.addListener( new GoatStatusListener() )
  streamTwitter.addListener( new GoatUserListener() )
  //we relay all @mentions to channel - this sets it up 
  streamTwitter.user();

  followIDs(followedIDs)

  private val trendsMap:Map[String,Int] = {
    var tLocs = twitter.getAvailableTrends().toList
    var tLocsMap:Map[String,Int] = Map()
    for(t <- tLocs) {
      tLocsMap += t.getName -> t.getWoeid
    }
    println("\n   TwitterModule:  loaded " + tLocsMap.size + " locations packed with inane tweeters")
    tLocsMap
  }

  private def refreshTwitterStream() {
    println("refreshing the tweetstream...")
    refreshIdsToFollow()
    followIDs(followedIDs)
  }

  private def followIDs(followIDs:Array[Long]) {
    if (followIDs.nonEmpty) {
      val query = new FilterQuery(followIDs)
      streamTwitter.filter(query)
    }
  }
  


  //this actor will send tweets, do searches, etc - we can fire up several of these
  private val twitterActor = actor {
    //val twitter: Twitter = new Twitter("goatbot", "slashnet")
    while (true) {
      receive {
        case (msg: Message, TWEET) =>
          if (tweetMessage(msg, msg.getModTrailing))
            if(msg.isAuthorised())
              msg.reply("Most beneficant Master " + msg.getSender + ", I have tweeted your wise words.")
            else 
              msg.reply(msg.getSender + ", I have tweeted your rash words.")
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
        case (msg: Message, TRANSLATE) =>
          twanslate(msg)
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
	    // woeid 1 is "world"
	    val trends = twitter.getPlaceTrends(1).getTrends.toList.map(_.getName)
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
	    //val parser = new CommandParser(m);
	    //val query: Query = new Query(parser.remaining())
	    val user = Goat.getUsers().getOrCreateUser(m.getSender)
	    var woeId: Option[Int] = None
	    var isNear = m.getModTrailing.trim.toLowerCase.startsWith("near");
	    if(isNear) {
	      val searchStr = m.getModTrailing.trim.toLowerCase.replaceFirst("near","").trim();
	      if(searchStr==null || searchStr=="") {
	        woeId = Some(user.getWoeId())
	      } else {
	        //look at the trends map and see what matches
	        val matchingTrends = trendsMap.filter(_._1.toLowerCase().contains(searchStr.toLowerCase()))
	        if(matchingTrends.size==0) {
	          m.reply(m.getSender+": No matching trends found.")
	          return
	        } else if(matchingTrends.size>1) {
	          //if there's an exact match, display trends for it
	          if( matchingTrends.exists(_._1.toLowerCase().equals(searchStr)) ) {
	            woeId = Some (matchingTrends.filter(_._1.toLowerCase().equals(searchStr)).head._2 )
	          } else {
	        	var replyStr = (if(searchStr.equals("all") || searchStr.equals("list")) {
	        	  trendsMap
	        	} else matchingTrends).foldRight("")((x,y) => x._1 + ", " + y)
	            replyStr=replyStr.substring(0,replyStr.length()-2)
	          	m.reply(m.getSender+": choose one of: " + replyStr)
	          	return
	          }
	        } else {
	          woeId=Some(matchingTrends.head._2);
	        }
	      }
	    }

	    var reply = "Trends of the moment"

	    val trends: List[Trend] = if (woeId.isEmpty) {
	      reply += ": "
	      // woeid 1 is "world"
	      twitter.getPlaceTrends(1).getTrends().toList
	    } else {
	      val locTrends = twitter.getPlaceTrends(woeId.get)
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
      if (parser.hasVar("radius") || parser.hasVar("location")) {
        //parse radius
        var radius: Double = 50 //50km by default
        if (parser.hasVar("radius")) {
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
        if (parser.hasVar("location")) {
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

      //query.setRpp(searchSize)
      query.setCount(searchSize)
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
    val tweets: List[Status] = filterSimilarTweets(tweetsArr.toList.asInstanceOf[List[Status]])
    searchResults = searchResults + Tuple2(query, tweets) //Tuple2 needed or just for eclipse?
  }

  private def filterSimilarTweets(tweets: List[Status]): List[Status] = {
    val start = System.currentTimeMillis
    //a map of the tweets we have already seen, against the count of similar tweets (which we throw away)
    var filtCount: Map[Status, Int] = Map()

    for (t <- tweets) {
      val simTweet = firstSimilarTweet(filtCount.keys.toList, t)
      if (simTweet.isEmpty)
        filtCount += t -> 1
      else
        filtCount += simTweet.get -> (filtCount.get(simTweet.get).get + 1)
    }

    var filtTweetsPaired: List[Tuple2[Status, Int]] = filtCount.toList
    var sortedTweetsPaired: List[Tuple2[Status, Int]] = Nil
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
  private def similar(t1: Status, t2: Status): Boolean = {
    val distLimit = t1.getText.length / 2
    var dist = 0
    //levlim is expensive call, guard just says don't call it for most obviously different cases based on string length
    if (t2.getText().length > distLimit && t2.getText().length < (distLimit * 3))
      dist = StringUtil.levlim(t1.getText, t2.getText, distLimit)
    else return false;
    return dist < distLimit
  }

  private def firstSimilarTweet(l: List[Status], t: Status): Option[Status] = {
    l.find(similar(t, _))
    //l.filter(similar(t,_))
  }

  private def enableNotification(m: Message, user: String) {
    try {
      if (twitter.updateFriendship(user, true,false) == null)
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
      if (twitter.updateFriendship(user, false, false) == null)
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
      if(message.length()<=140) {
        lastOutgoingTweetTime=System.currentTimeMillis()
    	twitter.updateStatus(message)
    	return true
      } else {
    	val remains=message.substring(139);
    	var remInd=0;
    	if(remains.length()<10)
    	  remInd=remains.length();
    	else remInd=9;
    	
    	val prefix = message.substring(124, 139) + "_"
    	
    	m.reply(m.getSender() + ": tweet too long, goes over at …" + prefix + remains.substring(0, remInd) + "…" )
    	false
      }
    } catch {
      case ex: TwitterException =>
        ex.printStackTrace()
        m.reply("Some sort of problem with twitter: " + ex.getMessage)
        false
    }
  }

  private val maxLastTweets: Int = 32
  private var lastTweets: Map[String, List[Status]] = Map[String, List[Status]]()
  
  private def addToLastTweets(m: Message, tweet: Status): Unit = {    
     lastTweets.get(m.getChanname()) match {
       case Some(l) => 
         lastTweets.put(m.getChanname(), (tweet :: l).take(maxLastTweets))
       case None =>
         lastTweets.put(m.getChanname(), List[Status](tweet))
     }
  }
  
  private def twanslate(m: Message): Unit = {
    val cp = new CommandParser(m)
    try {
      val num = cp.findNumber().toInt
      m.reply(twansLastTweet(m, num))
    } catch {
      case nfe: NumberFormatException =>
        if(cp.hasNumber)
          m.reply("I don't believe that's a number")
        else
          m.reply(twansLastTweet(m, 1))
      case e: Exception =>
        m.reply("Something went wrong with my translator")
    }
  }

  private def twansLastTweet(m: Message, num: Int): String = 
      lastTweets.get(m.getChanname()) match {
        case Some(l) =>
          twansTweetNum(l, num)
        case None =>
          "I don't remember finding any tweets for " + m.getChanname()
      }
  
  private def twansTweetNum(l: List[Status], num: Int): String =
    if (num < 1)
      "You are a bad person."
    else if (l.isEmpty)
      "But there haven't been any tweets yet!"
    else if (l.length < num)
      "I only remember " + l.length + " tweets" +
      {if (l.length < maxLastTweets) " right now" else ""} + "."
    else
      twansTweet(l(num - 1))
  
  private def twansTweet(tweet: Status): String = {
    val tweeText = unescapeHtml(tweet.getText())
    val lang = translator.detect(tweeText)
    if (lang.equals(translator.defaultLanguage))
      "As far as I can tell, that tweet was already in " + translator.defaultLanguage().name() + "."
    else
      formatTweet(tweet, "(from " + BOLD + lang.name + ")  " +
          NORMAL + translator.translate(tweeText, translator.defaultLanguage()))
  }

  //return true if we found one
  private def popTweetToChannel(m: Message, query: String): Boolean = {
    //find a matching tweet in cache
    searchResults.find(_._1 == query) match {
      case None =>
        false
      case Some(result) =>
        val tweet: Status = result._2.head
        searchResults = searchResults - result
        if (result._2.length > 1 && result._2.tail.length > 0)
          searchResults = searchResults + Tuple2(result._1, result._2.tail)
        m.reply(formatTweet(tweet))
        addToLastTweets(m, tweet)
        true
    }
  }

  private def formatTweet(tweet: Status): String =
    formatTweet(tweet, unescapeHtml(tweet.getText))
  
  private def formatTweet(tweet: Status, body: String): String =
    ageOfTweet(tweet) + " ago, " + 
    BOLD + tweet.getUser().getName() + 
    " [@" + tweet.getUser().getScreenName() + "]" + 
    NORMAL + ": " + body.replaceAll("\\s+", " ")

  private def ageOfTweet(tweet: Status): String = {
    return StringUtil.shortDurationString(System.currentTimeMillis - tweet.getCreatedAt.getTime)
  }

  //there has to be a better way to do this array munging ffs
  private def fetchFriendStatuses(twitter: Twitter): List[String] = {
    val statuses = twitter.getHomeTimeline().toArray(new Array[Status](0))
    var strStatuses: List[String] = Nil
    for (status <- statuses)
      strStatuses = (BOLD + status.getUser.getName + NORMAL + ": " + status.getText) :: strStatuses
    strStatuses.reverse
  }

  private def refreshIdsToFollow() {
    followedIDs = twitter.getFriendsIDs(1l).getIDs
  }

  private def sendStatusToChan(status: Status, chan: String) {
    println("got here, chan:" + chan);
    Message.createPrivmsg(chan, BOLD + status.getUser().getName() + " [@" + status.getUser().getScreenName() + "]" + BOLD + ": " + unescapeHtml(status.getText).replaceAll("\n", "")).send()
  }

  def processPrivateMessage(m: Message) {
    processChannelMessage(m)
  }

  def processChannelMessage(m: Message) {
    manageCache()
    m.getModCommand.toLowerCase() match {
      case "tweet" =>
        tweet(m)
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
      case "twanslate" | "twans" =>
        twitterActor ! (m, TRANSLATE)
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
    m.reply(m.getSender + ": Purged " + purge(0) + " tweets from cache.")
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
    if (m.getCommand == TOPIC && (now - lastOutgoingTweetTime) > 10 * MINUTE) {
      //twitter.updateStatus(m.getTrailing)
      twitterActor ! (m, TWEET_TOPIC)
      lastOutgoingTweetTime = now
    }
  }

  private def tweet(m: Message) {
    val now = System.currentTimeMillis
    if ((now - lastOutgoingTweetTime) > MINUTE ) {
      twitterActor ! (m, TWEET)
    } else {
      m.reply("Don't ask me to be a blabbermouth. I tweeted only " + StringUtil.durationString(now - lastOutgoingTweetTime) + " ago.")
    }
  }

  override def messageType = Module.WANT_COMMAND_MESSAGES

  def getCommands(): Array[String] = {
    Array("tweet", "tweetchannel", "follow", "unfollow", "tweetsearch", "twitsearch", 
        "twittersearch", "inanity", "tweetstats", "trends","localtrends", "tweetpurge", 
        "tweetsearchsize", "trendsnotify", "t", "twanslate", "twans")
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
  
  private def isMention(status:Status):Boolean = {
    println("sn:"+status.getInReplyToScreenName() + ":USER:" + USER) 
    println("getText():" + status.getText())
    status.getText().contains("@"+USER)
  }

  /*class GoatStatusListener extends StatusListener {
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
    
    def onStallWarning(sw:StallWarning) {
      //pass
    }
  }*/
  
  class GoatUserListener extends UserStreamListener {
    def onException(e: Exception) {
      //pass
      e.printStackTrace()
    }

    def onStatus(status: Status) {
      if (isMention(status) || isFollowed(status.getUser.getId))
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
    
    def onStallWarning(sw:StallWarning) {
      //pass
    }
    
    def onBlock(source:User, blockedUser:User) { }
    def onDeletionNotice(directMessageId:Long, userId:Long) { }  
    def onDirectMessage(directMessage:DirectMessage) {
      //TODO
    }  
    def onFavorite(source:User, target:User, favoritedStatus:Status) { }  
    def onFollow(source:User, followedUser:User) { }  
    def onFriendList(friendIds:Array[Long]) { }  
    def onUnblock(source:User, unblockedUser:User) { }  
    def onUnfavorite(source:User, target:User, unfavoritedStatus:Status) { }  
    def onUserListCreation(listOwner:User, list:UserList) { }  
    def onUserListDeletion(listOwner:User, list:UserList) { }  
    def onUserListMemberAddition(addedMember:User, listOwner:User, list:UserList) { } 
    def onUserListMemberDeletion(deletedMember:User, listOwner:User, list:UserList) { } 
    def onUserListSubscription(subscriber:User, listOwner:User, list:UserList) { }  
    def onUserListUnsubscription(subscriber:User, listOwner:User, list:UserList) { }  
    def onUserListUpdate(listOwner:User, list:UserList) { }  
    def onUserProfileUpdate(updatedUser:User) { } 
  }
}

