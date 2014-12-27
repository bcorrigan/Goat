package goat.module

import goat.core.Module
import goat.core.Message
import goat.core.Users
import goat.core.Constants
import goat.util.CommandParser
import goat.util.BitcoinCharts
import goat.util.BitcoinAverage

import java.io._
import java.net._

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONException

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.htmlunit.HtmlUnitDriver

import java.text.NumberFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.text.ParseException
import java.util.ArrayList
import java.util.Date
import java.util.TimeZone

import scala.collection.JavaConversions._

class Bitcoin extends Module {

  val bitcoincharts = new BitcoinCharts()
  val bitcoinaverage = new BitcoinAverage()

  def isThreadSafe() = false

  def processPrivateMessage(m: Message) =
    processChannelMessage(m)

  def getCommands() =
    Array("bitcoin", "buttcoin")

  def processChannelMessage(m: Message) = {
    val parser = new CommandParser(m.getModTrailing)
    if(m.getModTrailing().startsWith("help"))
      m.reply("usage: bitcoin [help]  " +
        "[communism [period={day, week, month}]]" +
        "[column={volume, bid, high, currency_volume, ask, close, avg, low}]  " +
        "[currency={"+ bitcoincharts.currencies() +"}] " +
        "[symbol={see http://bitcoincharts.com/markets for list}]  " +
        "  If both currency and symbol are specified, symbol overrides currency. " +
        "results cached for 30 seconds")
    else if(m.getModTrailing().startsWith("communism")) {
      val period = if(parser.hasVar("period")) parser.get("period").toLowerCase else "24 hours"
      m.reply(communism(period))
    } else
      ircQuote(m, parser)
  }

  private def ircQuote(m: Message, parser: CommandParser) = {
    val userCurrency =
      if (parser.hasVar("currency") && bitcoincharts.hasCurrency(parser.get("currency")))
        parser.get("currency").toUpperCase()
      else if(Users.hasUser(m.getSender()))
        Users.getUser(m.getSender()).getCurrency()
      else
        "GBP"

    val tz =
      if(Users.hasUser(m.getSender()))
        Users.getUser(m.getSender()).getTimeZone()
      else
        TimeZone.getTimeZone("Zulu")

    val symbol = if (parser.hasVar("symbol")) parser.get("symbol") else "btcavg"

    try {
      if (symbol.startsWith("btcavg"))
        m.reply(bitcoinaverage.quote(tz, userCurrency))
      else {
        val column = if(parser.hasVar("column")) parser.get("column") else "close"
        m.reply(bitcoincharts.quote(symbol, column, tz));
      }
    } catch {
      case e: JSONException =>
        m.reply("Someone didn't program the JSON properly:  " + e.getMessage())
        e.printStackTrace()
      case e: SocketTimeoutException =>
        m.reply("I got bored waiting for the bitcoins to talk to me.")
      case e: MalformedURLException =>
        m.reply("My programmers don't know how to write a URL.")
        e.printStackTrace()
      case e: ProtocolException =>
        m.reply("Network buggery:  " + e.getMessage())
        e.printStackTrace()
      case e: IOException =>
        m.reply("I/O is hard, let's go shopping.  " + e.getMessage())
        e.printStackTrace();
    }
  }

  def communism(period: String) = {
    val volumes = fetchCurrencyVolumes
    val comPercent = communistPercent(volumes, period)
    val summary = f"Bitcoin was ${comPercent}%2.1f%% communist in the past " + period
    val change = period match {
      case "month" => "."
      case _ => f""", ${comPercent - communistPercent(volumes, "month")}%+3.1f%% compared to the past 30 days."""
    }
    val breakdown = volumes.map { case (sym, vols) =>
      sym + ": " + abbreviated(vols(period))
    }.mkString(", ")
    summary + change + s"  Volumes for past $period:  " + breakdown
  }

  def fetchCurrencyVolumes:Map[String, Map[String, Double]] = {
    val driver = new HtmlUnitDriver
    driver.get("https://bitcoinwisdom.com/")
    val currencyRows = driver.findElements(By.cssSelector(".overview .outer tbody.body tr")).filter{(we) => val id = we.getAttribute("id"); id.startsWith("o_btc") && id.length == 8}.toList
    currencyRows.foldLeft(Map[String, Map[String, Double]]()) { (m, we) =>
      val tdata = we.findElements(By.cssSelector("td")).map(_.getText.replaceAll(",","")).toList
      m + (tdata.head -> List("24 hours", "week", "month").zip(List(5, 7, 9).map(tdata(_).toDouble)).toMap)
    }
  }

  def totalVolume(volumes:Map[String, Map[String, Double]], period:String):Double = volumes.map(_._2(period)).fold(0.0)(_+_)
  def communistVolume(volumes:Map[String, Map[String, Double]], period:String):Double = volumes.filter{(p) => p._1 == "CNY" || p._1 == "RUR"}.map(_._2(period)).fold(0.0)(_+_)
  def capitalistVolume(volumes:Map[String, Map[String, Double]], period:String):Double = volumes.filter{(p) => p._1 != "CNY" && p._1 != "RUR"}.map(_._2(period)).fold(0.0)(_+_)
  def communistPercent(volumes:Map[String, Map[String, Double]], period:String):Double = 100 * communistVolume(volumes, period) / totalVolume(volumes, period)

  def abbreviated(num: Double):String = {
    num match {
      case n if n < 1 => f"$num%.2f"
      case n if n < 1000 => Math.round(num).toString
      case n if n < 10000 => f"${num/1000}%.1fK"
      case n if n < 1000000 => val k = Math.round(num/1000); f"$k%dK"
      case n if n < 10000000 => f"${num/1000000}%.1fM"
      case _ => val m = Math.round(num/1000000); f"$m%dM"
    }
  }
}
