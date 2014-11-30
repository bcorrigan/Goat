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

  def processChannelMessage(m: Message) =
    if(m.getModTrailing().startsWith("help"))
      m.reply("usage: bitcoin [help]  " +
        "[column={volume, bid, high, currency_volume, ask, close, avg, low}]  " +
        "[currency={"+ bitcoincharts.currencies() +"}] " +
        "[symbol={see http://bitcoincharts.com/markets for list}]  " +
        "  If both currency and symbol are specified, symbol overrides currency. " +
        "results cached for 30 seconds")
    else
      ircQuote(m)

  def ircQuote(m: Message) = {
    val parser = new CommandParser(m.getModTrailing())

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
}
