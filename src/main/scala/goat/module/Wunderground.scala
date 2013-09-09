package goat.module

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import goat.core.Module
import goat.core.Message
import goat.core.Constants._
import goat.util.StringUtil.scrub
import goat.core.Users.hasUser
import goat.core.Users.getUser
import goat.core.User
import goat.util.CommandParser

import scala.math.round

class Wunderground extends Module {
  override def messageType = Module.WANT_COMMAND_MESSAGES

  def getCommands(): Array[String] =
    Array("forecast",
          "hourlyforecast", "hforecast",
          "amateurweather", "amweather",
          "hurricaneporn", "hurrporn")

  def processPrivateMessage(m: Message) =
    processChannelMessage(m)

  def processChannelMessage(m: Message) =
    try {
      m.getModCommand.toLowerCase match {
        case "forecast" => m.reply(forecast(m, "forecast"))
        case "hourlyforecast" | "hforecast" => m.reply(forecast(m, "hourly"))
        case "amateurweather" | "amweather" => m.reply(amateurWeather(m))
        case "hurricaneporn" | "hurrporn" => m.reply(hurricane(m))
      }
    } catch {
      case je: JSONException =>
        m.reply("I had a problem with JSON:  " + je.getMessage())
    }

  val fetcher = new goat.util.Wunderground

  def forecast(m: Message, method: String): String = {
    val cp = new CommandParser(m)
    val query = getQuery(cp, m.getSender, false)
    val units = getUnits(cp, m.getSender)
    if(query.equals(""))
      "I don't know where you want me to forecast"
    else if(units.equals("bogus"))
      "Sorry, but \"" + cp.get("units") + "\" isn't real units."
    else {
      val json = fetcher.apiCall(method, query)
      if(json.has("forecast"))
        "for " + UNDERLINE + query + NORMAL + " " +
        formatForecast(json.getJSONObject("forecast"), units)
      else if(json.has("hourly_forecast"))
        "for " + UNDERLINE + query + NORMAL + ":  " +
        formatHourlyForecast(json.getJSONArray("hourly_forecast"), units, 3)
      else if(json.has("response"))
        formatOtherResponse(json.getJSONObject("response"), query)
      else {
        println(json.toString(2));
        "I got a JSON from Wunderground, but didn't understand it at all."
      }
    }
  }

  def amateurWeather(m: Message): String = {
    val cp = new CommandParser(m)
    val query = getQuery(cp, m.getSender, true)
    if(query.equals(""))
       "I don't know where you want me to look for a hobbyist weather observation."
    else {
      val json = fetcher.apiCall("conditions", query)
      if(json.has("current_observation"))
        formatObservation(json.getJSONObject("current_observation"))
      else if(json.has("response"))
        formatOtherResponse(json.getJSONObject("response"), query)
      else {
        println(json.toString(2));
        "I got a JSON from Wunderground, but didn't understand it at all."
      }
    }
  }

  def hurricane(m: Message): String = {
    val json = fetcher.apiCall("currenthurricane/view", "")
    if(json.has("currenthurricane"))
      formatHurricanes(json.getJSONArray("currenthurricane"))
    else if(json.has("response"))
      formatOtherResponse(json.getJSONObject("response"), "hurricane porn")
    else {
      println(json.toString(2));
      "I got a JSON from Wunderground, but didn't understand it at all."
    }
  }

  def getQuery(cp: CommandParser, user: String, ignoreUserStation: Boolean): String = {
    val remaining = scrub(cp.remaining)
    if(remaining.equals(""))
      if(cp.hasVar("user") && hasUser(cp.get("user")))
        getQueryFromUser(getUser(cp.get("user")), ignoreUserStation)
      else if(hasUser(user))
        getQueryFromUser(getUser(user), ignoreUserStation)
      else
        ""
    else
      remaining
  }

  def getQueryFromUser(user: User, ignoreUserStation: Boolean): String = {
    val weatherStation = Option(user.getWeatherStation)
    weatherStation match {
      case Some(icao) =>
        if(ignoreUserStation || icao.equals(""))
          getUserLatLon(user)
        else
          icao.toUpperCase
      case None =>
        getUserLatLon(user)
    }
  }

  def getUserLatLon(user: User): String = {
    val latitude = Option(user.getLatitude)
    val longitude = Option(user.getLongitude)
    latitude match {
      case Some(lat) =>
        longitude match {
          case Some(lon) => lat.toString + "," + lon.toString
          case None => ""
        }
      case None => ""
    }
  }

  def getUnits(cp: CommandParser, user: String):String =
    if(cp.hasVar("units"))
      if(cp.get("units").toLowerCase.equals("english"))
        "english"
      else if(cp.get("units").toLowerCase.equals("metric"))
        "metric"
      else
        "bogus"
    else
      "default"

  def formatForecast(json: JSONObject, units: String): String = {
    val txt_forecast = json.getJSONObject("txt_forecast")
    val date = txt_forecast.getString("date")
    val forecastday = txt_forecast.getJSONArray("forecastday")
    val days = (0 until forecastday.length).map(forecastday.getJSONObject(_))
    days.map(formatDay(_, units)).foldLeft("(" + date + ") ")(_ + BLUE + " \u2022 " + NORMAL + _)
  }

  def formatDay(day: JSONObject, units: String): String = {
    val unitKey: String = if (units.equals("english")) "fcttext"
                          else "fcttext_metric"
    BOLD + day.getString("title") + NORMAL + " " + day.getString(unitKey)
  }

  def formatHourlyForecast(json: JSONArray, units: String, interval: Int): String = {
    val hours = (0 until json.length).map(json.getJSONObject(_)).filter(_.getJSONObject("FCTTIME").getString("hour").toInt % interval == 0)
    hours.map(formatHour(_, units)).reduceLeft(_ + ", " + _)
  }

  def formatHour(json: JSONObject, units: String): String = {
    val fcttime = json.getJSONObject("FCTTIME")
    val hour = fcttime.getString("hour").toInt
    val day = if(hour == 0)
      MAGENTA + fcttime.getString("weekday_name_abbrev") + NORMAL + " "
              else ""
    val time = (hour % 12).toString.replaceAll("\\b0", "12") +
      fcttime.getString("ampm").toLowerCase
    val tempjson = json.getJSONObject("temp")
    val temp =
      if(units.equals("metric"))
          tempjson.getString("metric") + "C"
      else if(units.equals("english"))
        tempjson.getString("english") + "F"
      else
        tempjson.getString("english") + "F/" + tempjson.getString("metric") + "C"
    val pop = json.getString("pop").toInt
    val fctcode = json.getString("fctcode").toInt

    List(codeIcon(fctcode), temp, windString(json, units), rainChanceString(pop, fctcode))
      .filter(! _.equals(""))
      .foldLeft(day + BOLD + time + NORMAL)(_ + " " + _)
  }

  def rainChanceString(pop: Integer, code: Integer): String =
    if(pop > 0)
      precipIcon(pop, code) + " " + pop + "% "
    else
      ""
  def windString(json: JSONObject, units: String): String = {
    val direction = json.getJSONObject("wdir").getString("dir").filter("NSEW".contains(_))
    val speed =
      if(units.equals("english"))
        json.getJSONObject("wspd").getString("english") + "mph"
      else
        json.getJSONObject("wspd").getString("metric") + "kph"

    if(speed.equals("") || json.getJSONObject("wspd").getString("english").toInt < 6)
      ""
    else if(direction.equals(""))
      windIcon + speed
    else
      windIcon + direction + " " + speed
  }

  val windIcon = DASH_SYMBOL + " "

  def codeIcon(code: Int): String =
    code match {
      case 1 => "\u000308,02" + BLACK_SUN_WITH_RAYS + " " + NORMAL
      case 2 => "\u000300,02" + SUN_BEHIND_CLOUD + " " + NORMAL
      case 3 => "\u000300,12" + SUN_BEHIND_CLOUD + " " + NORMAL
      case 4 => "\u000300,14" + CLOUD + " " + NORMAL
      case 5 => "\u000308,14" + WHITE_SUN_WITH_RAYS + " " + NORMAL
      case 6 => "\u000314,15" + FOGGY + " " + NORMAL
      case 7 => "\u000304,02" + BLACK_SUN_WITH_RAYS + " " + NORMAL
      case 8 => "\u000311,12" + SNOWMAN_WITHOUT_SNOW + " " + NORMAL
      case 14 | 15 => "\u000308,15" + THUNDER_CLOUD_AND_RAIN + " " + NORMAL
      case _ => ""
    }

  def precipIcon(pop: Integer, code: Integer) =
    if(isSnowCode(code))
      WHITE + SNOWFLAKE + NORMAL
    else if(pop < 25)
      DARK_BLUE + CLOSED_UMBRELLA + NORMAL
    else if(pop < 50)
      BLUE + UMBRELLA + NORMAL // open umbrella
    else
      TEAL + UMBRELLA_WITH_RAIN_DROPS + NORMAL // open umbrella with rain

  def isSnowCode(code: Integer): Boolean =
    List[Integer](9, 16, 18, 19, 20, 21, 24).contains(code)

  def formatObservation(json: JSONObject) = {
    val conditions =
      if (json.getString("weather").equals(""))
        " "
      else
        ", " + json.getString("weather").toLowerCase + ". "
    val humidity =
      if (json.getString("relative_humidity").equals(""))
        ""
      else
        "Humidity " + json.getString("relative_humidity") + "."
    val minutes_ago =
      (System.currentTimeMillis/1000L - json.getString("observation_epoch").toLong) / 60L
    json.getString("temp_f") + "F/" + json.getString("temp_c") +
      "C" + conditions + windString(json) + " " + humidity +
      "  Reported " + minutes_ago + " minutes ago at " +
      stationIdString(json) + " (" +
      json.getJSONObject("observation_location").getString("full") + ")"
  }

  def stationIdString(json: JSONObject) =
    if(json.getString("station_id").matches("\\b[A-Z]{8}\\d+\\b"))
      "pws:" + json.getString("station_id")
    else
      json.getString("station_id")

  def windString(json: JSONObject): String = {
    val dir = json.getString("wind_dir")
    val mph = json.getString("wind_mph")
    val gust_mph = json.getString("wind_gust_mph")
    val gust =
      if(gust_mph.equals("") || gust_mph.equals("0") || gust_mph.equals("0.0"))
        ""
      else
        " gusting to " + round(gust_mph.toFloat) + "mph"
    if(mph.equals("") || mph.equals("0") || mph.equals("0.0"))
      "No Wind."
    else if (dir.equals(""))
      "Wind " + round(mph.toFloat) + "mph" + gust + "."
    else
      "Wind " + dir + " " + round(mph.toFloat) + "mph" + "."
  }

  def formatHurricanes(json: JSONArray): String = {
    val hurricanes: String = (0 until json.length).map(json.getJSONObject(_)).filter(isSeriousHurricane(_)).map(formatHurricane(_)).foldLeft("")(_ + " " + hurrIcon + " " + _)
    if(hurricanes.equals(""))
      "No hurricanes."
    else
      hurrIcon + " " + hurricanes
  }

  val hurrIcon = CYAN + ",02" + CYCLONE + " " + NORMAL

  //FIXME: this should look at forecasted strength, too
  def isSeriousHurricane(json: JSONObject): Boolean =
    json.getJSONObject("Current").getString("SaffirSimpsonCategory").toInt >= -2

  def formatHurricane(json: JSONObject): String = {
    val current = json.getJSONObject("Current")
    val stormInfo = json.getJSONObject("stormInfo")
    stormInfo.getString("stormName_Nice") + " (cat. " +
      current.getString("SaffirSimpsonCategory") + ") " +
      current.getString("lat") + ", " + current.getString("lon") + ".  " +
      "Wind " + hurrWindString(current) + ". "
  }

  def hurrWindString(json: JSONObject): String = {
    val sustained = json.getJSONObject("WindSpeed").getString("Kts")
    val gustOpt = Option(json.getJSONObject("WindGust").getString("Kts"))
    gustOpt match {
      case Some(str) => sustained + "kt gusting to " + str
      case None => sustained + "kt"
    }
  }

  def formatOtherResponse(response: JSONObject, query: String): String =
    if(response.has("results"))
      "You need to be more specific.  I found:  " + formatSearchResults(response.getJSONArray("results"))
    else if (response.has("error")) {
      val error = response.getJSONObject("error")
      if(error.getString("type").equals("querynotfound"))
        "I couldn't find anything for \"" + query + "\""
      else
        "Wunderground had a problem.  type: " + error.getString("type") + ".  description: " + error.getString("description")
    } else {
      println("Problematic wunderground response JSON:")
      println(response.toString(2));
      "I got a JSON from Wunderground with a response in it, but I didn't know what to do with it."
    }

  def formatSearchResults(json: JSONArray): String =
    (0 until json.length).map(json.getJSONObject(_)).map(formatSearchResult(_)).foldLeft("")(_ + " \u2022 " + _)

  def formatSearchResult(json: JSONObject): String = {
    val state = if(! json.getString("state").equals(""))
                  ", " + json.getString("state")
                else
                  ""
    json.getString("city") + state + " " + json.getString("country") + " (zmw:" +
      json.getString("zmw") + ")"
  }


}
