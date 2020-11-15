package goat.module

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONException
import goat.core.{IrcMessage, Module, User}
import goat.core.Constants._
import goat.util.StringUtil.scrub
import goat.core.Users.hasUser
import goat.core.Users.getUser
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
        case "hourlyforecast" | "hforecast" => m.reply(forecast(m, "hourly/astronomy"))
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
      else if(json.has("hourly_forecast")) {
        "for " + UNDERLINE + query + NORMAL + ":  " +
        formatHourlyForecast(json, units, getInterval(cp,3), getIcons(cp, m.getSender))
      }
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
      if(Set("metric","imperial","english").contains(cp.get("units")))
        getUser(user).setUnits(cp.get("units"))
      else
        "bogus"
    else
      getUser(user).getUnits

  def getIcons(cp: CommandParser, user: String): Map[String, String] =
    if(cp.hasVar("icons") && iconsets.contains(cp.get("icons")))
      iconsets(cp.get("icons"))
    else
      iconsets("consolas")

  def getInterval(cp: CommandParser, default: Int): Int = {
    val ret: Int =
      if(cp.hasVar("interval"))
        cp.getInt("interval", default)
      else if(cp.hasVar("i"))
        cp.getInt("i", default)
      else
        default
    if(ret < 1 || ret > 12)
      default
    else
      ret
  }

  def formatForecast(json: JSONObject, units: String): String = {
    val txt_forecast = json.getJSONObject("txt_forecast")
    val date = txt_forecast.getString("date")
    val forecastday = txt_forecast.getJSONArray("forecastday")
    val days = (0 until forecastday.length).map(forecastday.getJSONObject(_))
    days.map(formatDay(_, units)).foldLeft("(" + date + ") ")(_ + BLUE + " \u2022 " + NORMAL + _)
  }

  def formatDay(day: JSONObject, units: String): String = {
    val unitKey: String = if (units == "imperial" || units == "english") "fcttext"
                          else "fcttext_metric"
    BOLD + day.getString("title") + NORMAL + " " + day.getString(unitKey)
  }

  def formatHourlyForecast(json: JSONObject, units: String, interval: Int, icons: Map[String, String]): String = {
    val hfJsonArr = json.getJSONArray("hourly_forecast")
    val hours = (0 until hfJsonArr.length).map(hfJsonArr.getJSONObject(_)).filter(_.getJSONObject("FCTTIME").getString("hour").toInt % interval == 0)
    hours.map(formatHour(_, SunMoon(json, icons), units)).reduceLeft(_ + ", " + _)
  }

  def formatHour(json: JSONObject, sunMoon: SunMoon, units: String): String = {
    val fcttime = json.getJSONObject("FCTTIME")
    val hour = fcttime.getString("hour").toInt
    val day = if(hour == 0)
      MAGENTA + fcttime.getString("weekday_name_abbrev") + NORMAL + " "
              else ""
    val time = (hour % 12).toString.replaceAll("\\b0", "12") +
      fcttime.getString("ampm").toLowerCase
    val tempjson = json.getJSONObject("temp")
    val temp =
      if(units == "metric" || units == "english")
          tempjson.getString("metric") + "C"
      else if(units.equals("imperial"))
        tempjson.getString("english") + "F"
      else
        tempjson.getString("english") + "F/" + tempjson.getString("metric") + "C"
    val pop = json.getString("pop").toInt
    val fctcode = json.getString("fctcode").toInt

    List(codeIcon(fctcode, sunMoon, hour), temp, windString(json, units), rainChanceString(pop, fctcode, sunMoon.icons))
      .filter(! _.equals(""))
      .foldLeft(day + BOLD + time + NORMAL)(_ + " " + _)
  }

  def rainChanceString(pop: Integer, code: Integer, icons: Map[String, String]): String =
    if(pop > 0)
      precipIcon(pop, code, icons) + " " + pop + "%"
    else
      ""

  def windString(json: JSONObject, units: String): String = {
    val direction = json.getJSONObject("wdir").getString("dir").filter("NSEW".contains(_))
    val speed =
      if(units == "english" || units == "imperial")
        json.getJSONObject("wspd").getString("english") + "mph"
      else
        json.getJSONObject("wspd").getString("metric") + "kph"

    if(speed.equals("") || json.getJSONObject("wspd").getString("english").toInt < 6)
      ""
    else if(direction.equals(""))
      speed.head + "\u0362" + speed.tail
    else
      direction.head + "\u0362" + direction.tail + " " + speed
  }

  def codeIcon(code: Int, sunMoon: SunMoon, hour: Int): String = {
    val icons = sunMoon.icons
    val daytime: Boolean = sunMoon.isDaylightHour(hour)
    val bg = if(daytime) ",02" else ",01"
    // background color for haze, mostly cloudy
    val bg2 = if(daytime) ",12" else ",14"
    val suncolor = if(daytime) "08" else "00"

    code match {
      case 1 =>
        if (!daytime && icons("setName") == "emoji")
          // sadly, unicode emoji moons only make sense on a light background
          "\u000301,00" + sunMoon.moonIcon + NORMAL
        else
          "\u0003"+suncolor+bg + sunMoon.icon(hour) + NORMAL
      case 2 =>
        if (icons.contains("partlyCloudy") && icons("partlyCloudy") != "")
          "\u000300"+bg + icons("partlyCloudy") + NORMAL
        else
          "\u000300"+bg + sunMoon.icon(hour) + icons("partlyCloudyModifier") + NORMAL
      case 3 =>
        if (icons.contains("mostlyCloudy") && icons("mostlyCloudy") != "")
          "\u000300"+bg2 + icons("mostlyCloudy") + NORMAL
        else
          "\u000300"+bg2 + sunMoon.icon(hour) + icons("mostlyCloudyModifier") + NORMAL
      case 4 => "\u000300"+bg2 + icons("cloudy") + NORMAL
      case 5 =>
        if (!daytime && icons("setName") == "emoji")
          "\u000315,14" + sunMoon.moonIcon + NORMAL // emoji moons
        else
          "\u0003"+suncolor+bg2 + sunMoon.icon(hour) + NORMAL //haze
      case 6 => "\u000314,15" + icons("foggy") + NORMAL
      case 7 => "\u000304" + bg + icons("veryHot") + NORMAL
      case 8 => "\u000311" + bg + icons("veryCold") + NORMAL
      case 14 | 15 => "\u000308" + bg2 + icons("thunderstorm") + NORMAL
      case _ => ""
    }
  }

  def precipIcon(pop: Integer, code: Integer, icons: Map[String,String]) =
    if(isSnowCode(code))
      WHITE + icons("snow") + NORMAL
    else if(pop < 35)
      TEAL + icons("chanceOfShowers") +
        (if (icons.contains("partlyCloudyModifier"))
          icons("partlyCloudyModifier")
         else "") + NORMAL
    else if(pop < 75)
      BLUE + icons("showers") +
        (if (icons.contains("mostlyCloudyModifier"))
          icons("mostlyCloudyModifier")
         else "") + NORMAL
    else
      DARK_BLUE + icons("rain") +
        (if (icons.contains("mostlyCloudyModifier"))
          icons("mostlyCloudyModifier")
         else "") + NORMAL

  def isSnowCode(code: Integer): Boolean =
    List[Integer](9, 16, 18, 19, 20, 21, 24).contains(code)

  // This is meant to mimic the output of WeatherModule.java for observations
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
    val temp_f = json.getDouble("temp_f")
    val temp_c = json.getDouble("temp_c")
    f"$temp_f%.1fF/$temp_c%.1fC" +
      conditions + windStringMph(json) + " " + humidity +
      "  Reported " + minutes_ago + " minutes ago at " +
      stationIdString(json) + " (" +
      json.getJSONObject("observation_location").getString("full") + ")"
  }

  def stationIdString(json: JSONObject) =
    if(json.getString("station_id").matches("\\b[A-Z]{8}\\d+\\b"))
      "pws:" + json.getString("station_id")
    else
      json.getString("station_id")

  def windStringMph(json: JSONObject): String = {
    val dir = json.getString("wind_dir")
    val mph = json.getDouble("wind_mph")
    val gust_mph = json.getDouble("wind_gust_mph")
    val gust =
      if(gust_mph.equals("") || gust_mph.equals("0") || gust_mph.equals("0.0"))
        ""
      else
        " gusting to " + round(gust_mph) + "mph"
    if(mph.equals("") || mph.equals("0") || mph.equals("0.0"))
      "No Wind."
    else if (dir.equals(""))
      "Wind " + round(mph) + "mph" + gust + "."
    else
      "Wind " + dir + " " + round(mph) + "mph" + "."
  }

  def formatHurricanes(json: JSONArray): String = {
    val hurricanes: String = (0 until json.length)
      .map(json.getJSONObject(_))
      .filter(isSeriousHurricane(_))
      .map(hurrIcon + " " + formatHurricane(_))
      .reduceLeft(_ + " " + _)
    if(hurricanes.equals(""))
      "No hurricanes."
    else
      hurricanes
  }

  val hurrIcon = CYAN + ",02" + CYCLONE + " " + NORMAL

  //FIXME: this should look at forecasted strength, too
  def isSeriousHurricane(json: JSONObject): Boolean =
    json.getJSONObject("Current").getInt("SaffirSimpsonCategory") >= -2

  def formatHurricane(json: JSONObject): String = {
    val current = json.getJSONObject("Current")
    val stormInfo = json.getJSONObject("stormInfo")
    val lat = current.getDouble("lat")
    val lon = current.getDouble("lon")
    stormInfo.getString("stormName_Nice") + " (cat. " +
      current.getInt("SaffirSimpsonCategory").toString + ") " +
      f"$lat%.2f, $lon%.2f. " +
      "Wind " + hurrWindString(current) + ". "
  }

  def hurrWindString(json: JSONObject): String = {
    val sustained = json.getJSONObject("WindSpeed").getDouble("Kts")
    val gustOpt = Option(json.getJSONObject("WindGust").getDouble("Kts"))
    gustOpt match {
      case Some(speed) => f"$sustained%.1fkt gusting to $speed%.1fkt"
      case None => f"$sustained%.1fkt"
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

  val iconsets: Map[String,Map[String,String]] =
    Map(
      "emoji" -> Map(
        "setName" -> "emoji",
        "sun" -> (BLACK_SUN_WITH_RAYS + " "),
        "partlyCloudy" -> (SUN_BEHIND_CLOUD + " "),
        // "partlyCloudyModifier" -> "",
        "mostlyCloudy" -> (SUN_BEHIND_CLOUD + " "),
        // "mostlyCloudyModifier" -> "",
        "cloudy" -> (CLOUD + " "),
        "hazy" -> (WHITE_SUN_WITH_RAYS + " "),
        "foggy" -> (FOGGY + " "),
        "veryHot" -> (BLACK_SUN_WITH_RAYS + " "),
        "veryCold" -> (SNOWMAN_WITHOUT_SNOW + " "),
        // "blowingSnow" -> "",
        "chanceOfShowers" -> (CLOSED_UMBRELLA + " "),
        "showers" -> (UMBRELLA + " "),
        // "chanceOfRain" -> "",
        "rain" -> (UMBRELLA_WITH_RAIN_DROPS + " "),
        // "chanceOfThunderstorm" -> "",
        "thunderstorm" -> (THUNDER_CLOUD_AND_RAIN + " "),
        // "chanceOfSnowShowers" -> "",
        // "snowShowers" -> "",
        // "chanceOfSnow" -> "",
        "snow" -> (SNOWFLAKE + " "),
        // "chanceOfIcePellets" -> "",
        // "icePellets" -> "",
        // "blizzard" -> "",
        "newMoon" -> (NEW_MOON_SYMBOL + " "),
        "waxingCrescentMoon" -> (WAXING_CRESCENT_MOON_SYMBOL + " "),
        "waxingQuarterMoon" -> (FIRST_QUARTER_MOON_SYMBOL + " "),
        "waxingGibbousMoon" -> (WAXING_GIBBOUS_MOON_SYMBOL + " "),
        "fullMoon" -> (FULL_MOON_SYMBOL + " "),
        "waningGibbousMoon" -> (WANING_GIBBOUS_MOON_SYMBOL + " "),
        "waningQuarterMoon" -> (LAST_QUARTER_MOON_SYMBOL + " "),
        "waningCrescentMoon" -> (WANING_CRESCENT_MOON_SYMBOL + " ")
      ),
      // call this one consolas because that's the font I stared at to make it
      "consolas" -> Map(
        "setName" -> "consolas",
        "sun" -> WHITE_SUN_WITH_RAYS,
        // "partlyCloudy" -> (SUN_BEHIND_CLOUD + " "),
        "partlyCloudyModifier" -> "\u0303",
        //"mostlyCloudy" -> (SUN_BEHIND_CLOUD + " "),
        "mostlyCloudyModifier" -> "\u034c",
        "cloudy" -> "\u2248",
        "hazy" -> WHITE_SUN_WITH_RAYS ,
        "foggy" -> "\u2592",
        "veryHot" -> (WHITE_SUN_WITH_RAYS + "\u333e"),
        "veryCold" -> "*\u02df\u0359",
        // "blowingSnow" -> "",
        "chanceOfShowers" -> ":",
        "showers" -> ":",
        // "chanceOfRain" -> "",
        "rain" -> "\u205e",
        // "chanceOfThunderstorm" -> "",
        "thunderstorm" -> (":\u034c" + YELLOW + "\u0003"),
        // "chanceOfSnowShowers" -> "",
        // "snowShowers" -> "",
        // "chanceOfSnow" -> "",
        "snow" -> "*\u02df\u0359",
        // "chanceOfIcePellets" -> "",
        // "icePellets" -> "",
        // "blizzard" -> "",
        "newMoon" -> "\u25cc",
        "waxingCrescentMoon" -> "\u208e",
        "waxingQuarterMoon" -> "\u037b",
        "waxingGibbousMoon" -> "\u037d",
        "fullMoon" -> "\u25cf",
        "waningGibbousMoon" -> "\u001f",
        "waningQuarterMoon" -> "\u1d12", //boo, the only other choice is 'c'
        "waningCrescentMoon" -> "\u208d"
      ))

  case class SunMoon(sunriseHour: Int, sunriseMinute: Int, sunsetHour: Int, sunsetMinute: Int, moonPercent: Int, moonAge: Int, icons: Map[String,String]) {

    def sunrise(): Double = sunriseHour + sunriseMinute / 60.0

    def sunset(): Double = sunsetHour + sunsetMinute / 60.0

    def isDaylightHour(hour: Int): Boolean =
      hour > sunrise().round && hour < sunset().round

    def moonDescription: String =
      if(moonPercent > 94) "full"
      else if(moonPercent < 6) "new"
      else if(moonPercent < 44) moonDirection + "Crescent"
      else if (moonPercent > 56) moonDirection + "Gibbous"
      else moonDirection + "Quarter"

    def moonDirection: String =
      if (moonAge < 14) "waxing"
      else "waning"

    def moonIcon: String = icons(moonDescription + "Moon")

    def icon(hour: Int): String =
      if (isDaylightHour(hour))
        icons("sun")
      else
        moonIcon
  }

  object SunMoon {
    // this is tied pretty closely to the Wunderground API
    def apply(json: JSONObject, icons: Map[String, String]) = {
      val moonPhase = json.getJSONObject("moon_phase")
      val sunPhase = json.getJSONObject("sun_phase")
      val sunrise = sunPhase.getJSONObject("sunrise")
      val sunset = sunPhase.getJSONObject("sunset")
      new SunMoon(sunrise.getString("hour").toInt,
                  sunrise.getString("minute").toInt,
                  sunset.getString("hour").toInt,
                  sunset.getString("minute").toInt,
                  moonPhase.getString("percentIlluminated").toInt,
                  moonPhase.getString("ageOfMoon").toInt,
                  icons)
    }
  }

  // end SunMoon Class + Object
}
