package goat.module

import goat.core.Constants._
import goat.util.StringUtil
import goat.core.{KVStore, Module, Message, Users}
import goat.util.CommandParser
import goat.util.Passwords._
import goat.Goat
import goat.util.TranslateWrapper;

import scala.collection.immutable.HashSet
import scala.collection.mutable.Map
import scala.collection.JavaConversions._
import scala.util.Random

import java.util.Timer
import java.util.TimerTask
import java.lang.System

import twitter4j.auth.{Authorization, AccessToken}
import twitter4j.conf._
import twitter4j._

import org.apache.commons.lang.StringEscapeUtils.unescapeHtml

/*
 * Lets have the vapid outpourings of the digerati in goat.
 *
 * @author Barry Corrigan
 */
class TwitterModule extends Module {
  private var lastFilterTime: Long = 0 //these few vars are for some stats keeping
  private var filterTimeAvg: Long = 0
  private var filterCount: Int = 0

  private val MAX_FOLLOW_COUNT=20;
  
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
  
  private val tweetCountStore:KVStore[Integer] = getModuleStore("tweetCount")

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

  //HashSet of tuples (queries and tweets), we will use this as a local cache
  //should make a facility for this generic
  private var searchResults: Set[Tuple2[String, List[Status]]] = new HashSet

  private var followedIDs: List[Long] = null
  refreshIdsToFollow()
  
  //some random stats to see how effective the cache is
  private var searchesMade: Int = 0;
  private var cacheHits: Int = 0

  private val streamTwitter: TwitterStream = new TwitterStreamFactory().getInstance(twitter.getAuthorization())

  //streamTwitter.addListener( new GoatStatusListener() )
  streamTwitter.addListener( new GoatUserListener() )
  //we relay all @mentions to channel - this sets it up
  streamTwitter.user();

  //followIDs(followedIDs)

  private val trendsMap:Map[String,Int] = {
    var tLocs = twitter.getAvailableTrends().toList
    var tLocsMap:Map[String,Int] = Map()
    for(t <- tLocs) {
      tLocsMap += t.getName -> t.getWoeid
    }
    println("\n   TwitterModule:  loaded " + tLocsMap.size + " locations packed with inane tweeters")
    tLocsMap
  }

  private val trendsReverseMap:Map[Int,String] = {
    trendsMap.map(_.swap)
  }

  var trendsTimer:Option[Timer] = None
  // can't use this without concurrency of some sort
  private def trendsNotify(m:Message, chan:String, woeId:Int) {
    if(trendsTimer.isDefined)
      trendsTimer.get.cancel()

    val trendsTask = new TimerTask {
      val store = new KVStore[List[String]]("twitter.")
      var seenTrends:List[String] = if(store.has("trends")) store.get("trends") else Nil
      var timesAround=0

      def run() {
        try {
          timesAround+=1
          val trends = twitter.getPlaceTrends(woeId).getTrends.toList.map(_.getName)
          val newTrends = trends diff seenTrends
          if(!newTrends.isEmpty) {
            val msg = newTrends reduce ((t1,t2) => t1 + ", " + t2)
            m.reply(msg)
            seenTrends = (newTrends ++ seenTrends).take(1000)
            store.save("trends", seenTrends)
          }

          if(timesAround>36) {
            trendsTimer.get.cancel()
            trendsTimer=None
            m.reply(m.getSender + ": I'm stopping trends notification cos it has been three hours.")
          }
        } catch {
          case e:Exception =>
            val msg = if(e.getMessage!=null) {" with message " + e.getMessage} else ""
            m.reply(m.getSender + ": Stopping trends notifying - got " + e.getClass.getName + msg )
          throw e;
        }
      }
    }

    trendsTimer = Some( new Timer("trendsTimer") )
    trendsTimer.get.schedule(trendsTask, 0, 300000)
  }


  private def findWoeId(m:Message, search:String): Option[Int] = {
    val matchingTrends = trendsMap.filter(_._1.toLowerCase().contains(search.toLowerCase()))
    matchingTrends.size match {
      case 0 =>
        m.reply(m.getSender+": No matching trends found.")
        return None
      case 1 =>
        return Some(matchingTrends.head._2);
      case _ =>
        //if there's an exact match,return woeid for it
        if( matchingTrends.exists(_._1.toLowerCase().equals(search)) ) {
          return Some (matchingTrends.filter(_._1.toLowerCase().equals(search)).head._2 )
        } else {
          var replyStr = (
            if(search.equals("all") || search.equals("list"))
              trendsMap
            else
              matchingTrends).foldRight("")((x,y) => x._1 + ", " + y)
          replyStr=replyStr.substring(0,replyStr.length()-2)
          m.reply(m.getSender+": choose one of: " + replyStr)
          return None
        }
    }
  }

  private def showTrends(m: Message) {
    try {
      //val parser = new CommandParser(m);
      //val query: Query = new Query(parser.remaining())
      val user = Users.getOrCreateUser(m.getSender)
      var woeId: Option[Int] = None
      var isNear = m.getModTrailing.trim.toLowerCase.startsWith("near");
      if(isNear) {
        val searchStr = m.getModTrailing.trim.toLowerCase.replaceFirst("near","").trim();
        if(searchStr==null || searchStr=="") {
          woeId = Some(user.getWoeId())
        } else {
          //look at the trends map and see what matches
          woeId = findWoeId(m,searchStr)
          if(woeId.isEmpty)
            return
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

      val prefix = reply
      var trendsStr=""
      var count = 1
      for (trend <- trends) {
        trendsStr += " " + BOLD + count + ":" + BOLD + trend.getName()
        count += 1
      }
      reply+=trendsStr
      addToLastTweets(m, new Trends(prefix,trendsStr))
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
      val user = Users.getOrCreateUser(m.getSender)
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
            val locat = new goat.util.Location(url)
            if(locat.isValid) {
              latitude=locat.getLatitude()
              longitude = locat.getLongitude()
            } else {
              m.reply(m.getSender + ": " + locat.error)
              return;
            }
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

  private def firstSimilarTweet(l: List[Status], t: Status): Option[Status] =
    l.find(similar(t, _))


  //this is all copied and pasted from elsewhere, frankly I can't be arsed generifying it just now
  private def stalk(m: Message) {
    val cp = new CommandParser(m)
    try {
      if(!cp.hasRemaining) {
        val num = if(cp.hasNumber) { cp.findNumber().toInt } else 1
        if (num < 1) {
          m.reply("You are a bad person.")
          return
        }
        lastTweets.get(m.getChanname()) match {
          case Some(l) =>
            if (l.isEmpty)
              m.reply("But there haven't been any tweets yet!")
            else if (l.length < num)
              m.reply("I only remember " + l.length + " tweets" +
                {if (l.length < maxLastTweets) " right now" else ""} + ".")
            else {
              l(num-1) match {
                case Right(_) => m.reply("Can't stalk trends, silly.")
                case Left(s) =>stalkUser(m, s.getUser.getScreenName)
              }
            }
          case None =>
            m.reply("I don't remember finding any tweets for " + m.getChanname())
        }
      } else {
        stalkUser(m,cp.remaining())
      }
    } catch {
      case nfe: NumberFormatException =>
        m.reply("I don't believe that's a number")
      case re: RuntimeException =>
        m.reply("Cannae stalk:  " + re.getMessage)
        re.printStackTrace
    }
  }

  //wrapper for UserResources.showUsers
  private def stalkUser(m: Message, userStr: String) {
    val userArg = if(userStr.startsWith("@")) {
      userStr.substring(1)
    } else userStr
    try {
      Option( twitter.showUser(userArg) ) match {
        case Some(user) =>
          var reply=m.getSender+": " + userArg + " "

          reply+= "has " + user.getFollowersCount + " followers & " + user.getFriendsCount + " friends. "

          reply+= " " + user.getStatusesCount + " tweets. "

          reply += "ArseFactor:" + "%.2f".format((user.getFriendsCount+0.0)/user.getFollowersCount) + " "

          reply += "Joined " + StringUtil.toDateStr("dd/MM/yyyy",user.getCreatedAt.getTime) + " "

          if(user.getName()!=null) {
            reply+="Name: " + user.getName + ". "
          }

          if(user.getLocation!=null) {
            reply+="Location: " + user.getLocation +". "
          }

          if(user.getLang!=null) {
            if(user.getLang!="en")
              reply+="Lang: " + user.getLang + " "
          }

          if(user.getTimeZone!=null) {
            reply+="Tz: " + user.getTimeZone + ". "
          }

          if(user.getOriginalProfileImageURL!=null) {
            reply+="Stalk pic: " + user.getOriginalProfileImageURL + " "
          }

          if(user.getURL!=null)
            reply += "URL: " + user.getURL + " "

          reply += "Stalk gallery: http://twitter.com/" + user.getScreenName + "/media/grid "

          if(user.getDescription!=null) {
            reply+="Description: " + user.getDescription
          }

          m.reply(reply)

        case None =>
          m.reply(m.getSender + ": no user found I'm afraid.")
      }
    } catch {
      case ex: TwitterException =>
        ex.printStackTrace()
        m.reply(m.getSender + ": can't stalk that user - got a TwitterException saying: " + ex.getMessage)
    }
  }

  private def enableNotification(m: Message, userStr: String) {
    try {
      val screenName = userStr.replaceAll("@","")
      val followedUser = twitter.createFriendship(screenName, true) 
      if (followedUser != null) {
        val user = Users.getUser(m.getSender().toLowerCase());
        user.addFollowing(screenName);
        m.reply("I am now following " + followedUser.getName() + " for their next " + MAX_FOLLOW_COUNT + " tweets.")
        followedIDs = followedUser.getId()::followedIDs
        tweetCountStore.save(followedUser.getScreenName, 0);
      } else m.reply("Looks like that user doesn't exist.")
    } catch {
      case ex: TwitterException =>
        ex.printStackTrace()
        m.reply("Those lackeys at twitter have failed us. I was unable to follow that user as a result. Error: " + ex.getMessage)
    }
  }

  private def disableNotification(m: Message, userStr: String) {
    try {
      val screenName = userStr.replaceAll("@","")
      val unfollowedUser = twitter.destroyFriendship(screenName);
      if (unfollowedUser != null) {
        val user = Users.getUser(m.getSender().toLowerCase());
        user.rmFollowing(screenName);
        m.reply("OK, I am no longer following " + unfollowedUser.getName + "." )
        followedIDs=followedIDs.filterNot(_==unfollowedUser.getId)
    } else m.reply("That user - we weren't following it.")
    } catch {
      case ex: TwitterException =>
        ex.printStackTrace()
        m.reply("Those lackeys at twitter have failed us. I was unable to cease following that user as a result. Error: " + ex.getMessage)
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

        m.reply(m.getSender() + ": tweet too long, goes over at …" + prefix + BOLD + remains.substring(0, remInd) + "…" )
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
  private var lastTweets: Map[String, List[Either[Status,Trends]]] = Map[String, List[Either[Status,Trends]]]()

  private def addToLastTweets(m: Message, tweet: Either[Status,Trends]): Unit = {
     lastTweets.get(m.getChanname()) match {
       case Some(l) =>
         lastTweets.put(m.getChanname(), (tweet :: l).take(maxLastTweets))
       case None =>
         lastTweets.put(m.getChanname(), List[Either[Status,Trends]](tweet))
     }
  }
  
  private def addToLastTweets(m:Message, tweet:Status): Unit = {
    addToLastTweets(m,Left(tweet))
  }
  
  private def addToLastTweets(m:Message, trends:Trends):Unit = {
    addToLastTweets(m, Right(trends))
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
      case re: RuntimeException =>
        m.reply("Lost in twanslation:  " + re.getMessage)
        re.printStackTrace
    }
  }

  private def twansLastTweet(m: Message, num: Int): String =
      lastTweets.get(m.getChanname()) match {
        case Some(l) =>
          twansTweetNum(m, l, num)
        case None =>
          "I don't remember finding any tweets for " + m.getChanname()
      }

  private def twansTweetNum(m: Message, l: List[Either[Status,Trends]], num: Int): String =
    if (num < 1)
      "You are a bad person."
    else if (l.isEmpty)
      "But there haven't been any tweets yet!"
    else if (l.length < num)
      "I only remember " + l.length + " tweets" +
      {if (l.length < maxLastTweets) " right now" else ""} + "."
    else
      twansTweet(m, l(num - 1))

  private def twansTweet(m: Message, tweet: Either[Status,Trends]): String = {
    val tweeText = unescapeHtml(tweet match { case Left(t) => t.getText() ; case Right(s) => s.trends ;})
    val lang = translator.detect(tweeText)
    if (lang.equals(translator.defaultLanguage))
      "As far as I can tell, that tweet was already in " + translator.defaultLanguage().name() + "."
    else {
      val langStr = "(from " + BOLD + lang.name + ")  ";
      tweet match {
      case Left(t) => formatTweet(t, langStr +
          NORMAL + translator.localize(m, translator.translate(tweeText, translator.defaultLanguage())));
      case Right(s) => s.prefix + " " + langStr + translator.localize(m, translator.translate(s.trends, translator.defaultLanguage()))
      }
    }
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

  private def fetchFriendStatuses(twitter: Twitter): List[String] =
    twitter.getHomeTimeline().toList.map((s) => BOLD + s.getUser.getName + NORMAL + ": " + s.getText).reverse

  private def refreshIdsToFollow() = {
    followedIDs = twitter.getFriendsIDs(-1l).getIDs.toList
  }

  private def sendStatusToChan(status: Status, chan: String):Unit = {
    val countStr =  if(tweetCountStore.has(status.getUser().getScreenName() )) {
      //increment
      tweetCountStore.save(status.getUser().getScreenName(), tweetCountStore.get(status.getUser().getScreenName())+1)
      tweetCountStore.get(status.getUser().getScreenName())
    } else ""
    
    Message.createPrivmsg(chan, REVERSE + RED + "*** " + countStr + NORMAL + BOLD +  status.getUser().getName() + " [@" + status.getUser().getScreenName() + "]" + BOLD + ": " + unescapeHtml(status.getText).replaceAll("\n", "")).send()
    
    if(tweetCountStore.has(status.getUser().getScreenName() )) {
      if(tweetCountStore.get(status.getUser().getScreenName())>=MAX_FOLLOW_COUNT) {
        Message.createPrivmsg(chan, "Seen " + MAX_FOLLOW_COUNT + " tweets from " + status.getUser().getScreenName() + " so unfollowing now.").send()
        disableNotification(Message.createPrivmsg(chan,""), status.getUser().getScreenName())
      }
    }
  }
    
  private def sanitiseAndScold(m: Message): Boolean =
    if (m.getModTrailing.trim.length == 0) {
      m.reply(m.getSender + ": Twitter might be inane, but you still need to tell me to search for *something*.")
      false
    } else true

  private def tweetpurge(m: Message) =
    m.reply(m.getSender + ": Purged " + purge(0) + " tweets from cache.")

  private def cacheSize(): Int =
    searchResults.foldLeft(0)((sum, x) => sum + x._2.length)

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
    if ((now - lastPurge) > purgePeriod * MINUTE)
      purge(purgePeriod)
    lastPurge = now
  }

  private def tweet(m: Message) {
    val now = System.currentTimeMillis
    if ((now - lastOutgoingTweetTime) > MINUTE ) {
      if(tweetMessage(m, m.getModTrailing))
        m.reply(tweetConfirmation)
    }
    else
      m.reply("Don't ask me to be a blabbermouth. I tweeted only " + StringUtil.durationString(now - lastOutgoingTweetTime) + " ago.")
  }

  private def tweetConfirmation: String =
    confirmationGripes(Random.nextInt(confirmationGripes.length))

  private val confirmationGripes = Array[String](
    "Your loneliness is now being used to deplete the world's energy supply.",
    "I will take that and shove it into my tweethole.",
    "Well, OK, but don't be surprised if nothing comes of it.",
    "I have increased the entropy of the universe a little for you. I think that's about all we can expect.",
    "Done; now we all get to see exactly how little the world cares for what you have to say.",
    "I have shat electrical impulses into the meaningless maw of tweeter.",
    "Oh, you're trying to troll? You know, none of you are as good at that as you used to be, but lets cross our fingers this time, eh?",
    "I have polluted the internet for you.")

  private def filterIDs(ids: Array[Int]): Array[Int] =
    ids.filter((id) => followedIDs.contains(id))

  private def isFollowed(id: Long): Boolean = {
    //what users are following the ID?
    //Are they active?
    
    //follow -> adds user.following.screenname
    //unfollow -> removes it
    
    followedIDs.contains(id)
  }

  private def isMention(status:Status):Boolean =
    status.getText().contains("@"+USER)



  // Goat.module.Module overrides

  override def messageType = Module.WANT_COMMAND_MESSAGES

  override def getCommands(): Array[String] = {
    Array("tweet", "tweetchannel", "follow", "unfollow", "tweetsearch", "twitsearch",
        "twittersearch", "inanity", "tweetstats", "trends","localtrends", "tweetpurge",
        "tweetsearchsize", "trendsnotify", "t", "twanslate", "twans", "stalk")
  }

  override def processPrivateMessage(m: Message) {
    processChannelMessage(m)
  }

  override def processChannelMessage(m: Message) {
    manageCache()
    (m.getModCommand.toLowerCase, m.isAuthorised) match {
      case ("tweet", true) =>
        tweetMessage(m, m.getModTrailing)
        m.reply("Most beneficant Master " + m.getSender + ", I have tweeted your wise words.")
      case ("tweet", false) =>
        tweet(m)
      case ("stalk", _) =>
        stalk(m)
      case ("tweetchannel", true) =>
        chan = m.getChanname
        Message.createPrivmsg(m.getChanname, "This channel is now the main channel for twitter following.").send()
      case ("tweetchannel", false) =>
        m.reply("You can't tell me where to send my tweeters")
      case ("follow", _) =>
        enableNotification(m, m.getModTrailing.trim())
      case ("unfollow", _) =>
        disableNotification(m, m.getModTrailing.trim())
      case ("tweetsearch" | "twitsearch" | "twittersearch" | "inanity" | "t", _) =>
        if (sanitiseAndScold(m))
          if (!popTweetToChannel(m, m.getModTrailing.trim().toLowerCase)) {
            searchesMade += 1
            queryTwitter(m, m.getModTrailing.trim().toLowerCase())
          } else cacheHits += 1
      case ("tweetstats", _) =>
        m.reply(m.getSender + ": Searches made:" + (searchesMade+cacheHits) + " Network hits:" + searchesMade + " Cache hits:" + cacheHits + " Cache size:" + cacheSize
                + " Last filter time:" + lastFilterTime + "ms"
                + " Avg. Filter Time:" + filterTimeAvg + "ms")
      case ("trends" | "localtrends", _) =>
        showTrends(m)
      case ("twanslate" | "twans", _) =>
        twanslate(m)
      case ("tweetpurge", _) =>
        tweetpurge(m)
      case ("tweetsearchsize", true) =>
        try {
          m.reply(m.getSender + ": I set search size to " + (new CommandParser(m)).findNumber + ", as you commanded, my liege.")
        } catch {
          case ex: NumberFormatException =>
            m.reply(m.getSender + ": That's rubbish, try specifying a simple integer.")
        }
      case ("tweetsearchsize", false) =>
        m.reply(m.getSender + ": You are not as handsome, nor as intelligent, as I expect my master to be, so I will not do that.")
      case ("trendsnotify", _) =>
        if(m.getModTrailing.trim.toLowerCase.equals("stop")) {
          trendsTimer match {
            case Some(t) =>
              t.cancel()
              m.reply(m.getSender + ": stopped trends notifying!")
              trendsTimer=None
            case None =>
              m.reply(m.getSender + ": But I am not trends notifying?")
          }
        } else {
          val woeId = if(m.getModTrailing.trim.length==0) {
            Some(1)
          } else {
            findWoeId(m,m.getModTrailing.trim.toLowerCase)
          }

          if(woeId.isDefined) {
            m.reply(m.getSender + ": Notifying for the next 3 hours for "+trendsReverseMap(woeId.get)+" trends.")
            trendsNotify(m,m.getChanname,woeId.get)
          }
        }
      case (_, _) =>
        m.reply("Looks like some dullard forgot to implement that command.")
    }
  }

  override def processOtherMessage(m: Message) {
    val now = System.currentTimeMillis
    if (m.getCommand == TOPIC && (now - lastOutgoingTweetTime) > 1 * MINUTE) {
      //twitter.updateStatus(m.getTrailing)
      tweetMessage(m, m.getTrailing)
      lastOutgoingTweetTime = now
    }
  }
  
  case class Trends(prefix:String, trends:String);

  class GoatUserListener extends UserStreamListener {
    def onException(e: Exception) {
      //pass
      e.printStackTrace()
    }

    def onStatus(status: Status) {
      if (isMention(status) || isFollowed(status.getUser.getId)) {
        sendStatusToChan(status, chan);
        println("****GOOD: " + status.getUser().getScreenName() + ": " + status.getText)
      } else println("JUNK: " + status.getUser().getScreenName() + ": " + status.getText)
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
