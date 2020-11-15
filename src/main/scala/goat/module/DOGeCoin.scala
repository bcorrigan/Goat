package goat.module

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONException
import goat.core.{IrcMessage, Module, User}
import goat.core.Constants._
import goat.util.StringUtil.{formatBigNumber, formatMoney, formatSmallNumber, scrub}
import goat.core.Users.hasUser
import goat.core.Users.getUser
import goat.util.CommandParser

import scala.math.round
import scala.jdk.CollectionConverters._

class DOGeCoin extends Module {
  override def messageType = Module.WANT_COMMAND_MESSAGES

  def getCommands(): Array[String] =
    Array("dog")

  def processPrivateMessage(m: Message) =
    processChannelMessage(m)

  def processChannelMessage(m: Message) =
    try {
      val cp = new CommandParser(m.getModTrailing)
      cp.command.toLowerCase match {
        case "ecoin" => m.reply(ticker(m))
        case "blocks" => m.reply(blocks(m))
        case "difficulty" => m.reply(difficulty(m))
        case "hashrate" | "hash" => m.reply(hashrate(m))
        case _       => m.reply("We are all going to learn so much from Dog eCoin.")
      }
    } catch {
      case je: JSONException =>
        m.reply("I had a problem with JSON:  " + je.getMessage())
    }

  val tickerFetcher = new goat.util.DOGeCoin

  def ticker(m: Message): String = {
    val json = tickerFetcher.insecureApiCall("")
    if(json.has("success"))
      formatTicker(json)
    else
      "I got a JSON from DOGeCoin.org, but I didn't understand it."
  }

  def formatTicker(json: JSONObject): String = {
    val jdata = json.getJSONObject("return").getJSONObject("markets").getJSONObject("DOGE")

    val dogusd = jdata.getDouble("lasttradeprice")
    val dogusdstr = formatSmallNumber(dogusd)
    val usddogstr = formatBigNumber(1.0 / dogusd)

    s"$$$dogusdstr  |  $$1.00 = $usddogstr dog eCoins"
  }

  val chainFetcher = new goat.util.DOGeChain

  def blocks(m: Message): String = {
    val lines: List[String] = chainFetcher.insecureApiCall("getblockcount").asScala.toList
    if(lines.head.startsWith("error:"))
      lines.head.substring(6).trim
    else
      formatBigNumber(lines(0).toDouble) + " blocks of DOG eCoin are currently premined"
  }

  def difficulty(m: Message): String = {
    val lines: List[String] = chainFetcher.insecureApiCall("getdifficulty").asScala.toList
    if(lines.head.startsWith("error:"))
      lines.head.substring(6).trim
    else
      "Making up a DOG eCoin currently requires " + formatMoney(lines(0).toDouble) + " maths."
  }

  def hashrate(m: Message): String = {
    val lines: List[String] = chainFetcher.insecureApiCall("nethash/60/-60").asScala.toList
    if(lines.head.startsWith("error:"))
      lines.head.substring(6).trim
    else {
      val fields = lines.dropWhile(_ != "START DATA").tail.head.split(",").map(_.toDouble)
      "The internet is currently wasting " + formatBigNumber(fields(7)) + " GPUs every second to produce one bag of DOG eCoins every " + formatBigNumber(fields(6)) + " seconds."
    }
  }

}
