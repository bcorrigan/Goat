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

class Wunderground extends Module {
  override def messageType = Module.WANT_COMMAND_MESSAGES

  def getCommands(): Array[String] =
    Array("forecast", "hourlyforecast", "hforecast", "amateurweather", "aweather")

  def processPrivateMessage(m: Message) =
    processChannelMessage(m)

  def processChannelMessage(m: Message) =
    try {
      m.getModCommand.toLowerCase match {
        case "forecast" => m.reply(forecast(m, "forecast"))
        case "hourlyforecast" | "hforecast" => m.reply(forecast(m, "hourly"))
        case "amateurweather" | "aweather" => m.reply(amateurWeather(m))
      }
    } catch {
      case je: JSONException =>
        m.reply("I had a problem with JSON:  " + je.getMessage())
    }

  val fetcher = new goat.util.Wunderground

  def forecast(m: Message, method: String): String = {
    val query = getQuery(m, false)
    if(query.equals(""))
       "I don't know where you want me to forecast"
    else {
      val json = fetcher.apiCall(method, query)
      if(json.has("forecast"))
        "for " + UNDERLINE + query + NORMAL + " " +
        formatForecast(json.getJSONObject("forecast"))
      else if(json.has("hourly_forecast"))
        "for " + UNDERLINE + query + NORMAL + ":  " +
        formatHourlyForecast(json.getJSONArray("hourly_forecast"))
      else if(json.has("response"))
        formatOtherResponse(json.getJSONObject("response"), query)
      else {
        println(json.toString(2));
        "I got a JSON from Wunderground, but didn't understand it at all."
      }
    }
  }

  def amateurWeather(m: Message): String = {
    val query = getQuery(m, true)
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

  def getQuery(m: Message, ignoreUserStation: Boolean): String = {
    val trailing = scrub(m.getModTrailing)
    if(trailing.equals(""))
      getQueryFromUser(m.getSender, ignoreUserStation)
    else
      trailing
  }

  def getQueryFromUser(name: String, ignoreUserStation: Boolean): String = {
    if(hasUser(name)) {
      val user = getUser(name)
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
    } else
      "" // user unknown
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

  def formatForecast(json: JSONObject): String = {
    val txt_forecast = json.getJSONObject("txt_forecast")
    val date = txt_forecast.getString("date")
    val forecastday = txt_forecast.getJSONArray("forecastday")
    val days = (0 until forecastday.length).map(forecastday.getJSONObject(_))
    "(" + date + ")  " + days.map(formatDay(_)).reduceLeft(_ + BLUE + " \u2022 " + NORMAL + _)
  }

  def formatDay(day: JSONObject): String =
    BOLD + day.getString("title") + NORMAL + " " + day.getString("fcttext_metric")

  def formatHourlyForecast(json: JSONArray): String = {
    val hours = (0 until json.length).map(json.getJSONObject(_))
    hours.map(formatHour(_)).reduceLeft(_ + "  " + _)
  }

  def formatHour(json: JSONObject): String = {
    val fcttime = json.getJSONObject("FCTTIME")
    val hour = fcttime.getString("hour").toInt
    val day = if(hour == 0)
      MAGENTA + fcttime.getString("weekday_name_abbrev") + NORMAL + " "
              else ""
    val time = (hour % 12).toString.replaceAll("\\b0", "12") +
      fcttime.getString("ampm").toLowerCase
    val tempjson = json.getJSONObject("temp")
    val temp = tempjson.getString("metric") + "/" +
      tempjson.getString("english") + "F"
    val pop = json.getString("pop").toInt
    val fctcode = json.getString("fctcode").toInt
    // Fixme: add qpf/snow if available
    day + BOLD + time + NORMAL + " " +  temp + " " + rainChanceString(pop, fctcode)
  }

  def rainChanceString(pop: Integer, code: Integer): String =
    if(pop > 0)
      precipIcon(pop, code) + " " + pop + "% "
    else
      ""

  def precipIcon(pop: Integer, code: Integer) =
    if(isSnowCode(code))
      WHITE + "\u2744" + NORMAL // snowflake
    else if(pop < 25)
      DARK_BLUE + CLOSED_UMBRELLA + NORMAL // closed umbrella
    else if(pop < 50)
      BLUE + "\u2602" + NORMAL // open umbrella
    else
      TEAL + "\u2614" + NORMAL // open umbrella with rain

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
        "Humidity " + json.getString("relative_humidity") + ". "
    val minutes_ago =
      (System.currentTimeMillis/1000L - json.getString("observation_epoch").toLong) / 60L
    json.getString("temp_c") + "/" + json.getString("temp_f") +
      "F" + conditions + windString(json) + " " + humidity +
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
      if(gust_mph.equals("") || gust_mph.equals("0"))
        ""
      else
        " gusting to " + gust_mph + "mph"
    if(mph.equals("") || mph.equals("0"))
      "No Wind."
    else if (dir.equals(""))
      "Wind " + mph + "mph" + gust + "."
    else
      "Wind " + dir + " " + mph + "mph" + "."
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
    (0 until json.length).map(json.getJSONObject(_)).map(formatSearchResult(_)).reduceLeft(_ + " \u2022 " + _)

  def formatSearchResult(json: JSONObject): String = {
    val state = if(! json.getString("state").equals(""))
                  ", " + json.getString("state")
                else
                  ""
    json.getString("city") + state + " " + json.getString("country") + " (zmw:" +
      json.getString("zmw") + ")"
  }


}
