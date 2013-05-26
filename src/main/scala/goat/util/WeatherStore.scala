package goat.util

import goat.core.KVStore
import goat.core.Constants._
import scala.collection.immutable.List
import scala.math.Numeric
import goat.util.StringUtil.toDateStr

class WeatherStore {
    //hmnnn, this is piss poor
    val store:KVStore[List[WeatherReport]] = new KVStore[List[WeatherReport]]("weatherstore.");
    val records:KVStore[Int] = new KVStore[Int]("weatherstore.record.");
    val timeRecs:KVStore[Long] = new KVStore[Long]("weatherstore.record.");
    val reports:KVStore[String] = new KVStore[String]("weatherstore.record.")
    
    def getStatReport(userName:String, station:String):String = {
      val keyBase = userName+"."+station+"."
      val getMaxKey_ = getMaxKey(userName,station,_:String)
      val getMinKey_ = getMinKey(userName,station,_:String)
      val report:String = 
        (if(records.has(getMaxKey_("temp"))) "Max temp: " + records.get(getMaxKey_("temp")) + "C/" + records.get(getMaxKey_("tempf")) + "F " else "") +
        (if(records.has(getMinKey_("temp"))) "Min temp: " + records.get(getMinKey_("temp")) + "C/" + records.get(getMinKey_("tempf")) + "F " else "") +
        (if(records.has(getMaxKey_("wind"))) "Max wind: " + records.get(getMaxKey_("wind")) + " " else "") + 
        (if(records.has(getMaxKey_("gust"))) "Max gust: " + records.get(getMaxKey_("gust")) + " " else "") +
        getTopScores(userName, station)
      return report
    }
    
    private def getMaxKey(userName:String, station:String, attr:String):String = userName+"."+station+"."+attr+".max";
    private def getMinKey(userName:String, station:String, attr:String):String = userName+"."+station+"."+attr+".min";
    
    //maxwind, minwind etc - but not scores
    def getReport(userName:String, station:String, attr:String, typeStr:String, attrName:String):String = {
      val key = userName+"."+station+"."+attr+"."+typeStr
      if(records.has(key)) {
        if(reports.has(key+".report"))
          if(timeRecs.has(key+".timestamp")) 
            return "On " + toDateStr("dd/MM/yyyy", timeRecs.get(key+".timestamp")) + " " + userName + " had this " + typeStr + " " + attrName + " report: " + reports.get(key+".report")
          else 
            return "At an unknown date " + userName + " had this " + typeStr + " " + attrName + " report: " + reports.get(key+".report")
      }
      
      "No record found for " + userName + " at station " + station
    }
    
    def getScoreReport(userName:String, station:String, num:Int):String = {
      val key = userName+"."+station
      if(store.has(key)) {
        val recs = store.get(key).sortBy(_.score).reverse
        
        if(recs.size<num)
          return "User has no maxscore record at position " + num
        else {
          val rec = recs(num-1)
          return "On " + toDateStr("dd/MM/yyyy", rec.timestamp) + " " + userName + " had report: " + rec.report
        }
      } else 
        return "User has no weather records"
    }
    
    
    def getTopScores(userName:String, station:String):String = {
      val reports = store.get(userName+"."+station).sortBy(_.score).reverse
      //TODO localise based on user info
      return reports.zipWithIndex.foldLeft("")((report,record) =>
            report + BOLD + (record._2+1) + BOLD + ": " + "%2f".format(record._1.score) + " on " + toDateStr("dd/MM/yyyy",record._1.timestamp) + " "
        )
    }
    
    
    
    //returns 1 if record max, -1 if record min, 0 if neither
    def checkRecordAttribute(attr:String, value:Int, username:String, station:String, report:String):Int = {
      val maxKey = username+"."+station+"."+attr+".max";
      val minKey = username+"."+station+"."+attr+".min"
      if(records.has(maxKey)) {
        val record = records.get(maxKey)
        if(value>record) {
          records.save(maxKey,value)
          reports.save(maxKey+".report", report)
          timeRecs.save(maxKey+".timestamp", System.currentTimeMillis())
          return 1
        }
      } else {
        records.save(maxKey, value)
        reports.save(maxKey+".report", report)
        timeRecs.save(maxKey+".timestamp", System.currentTimeMillis())
      }
      
      if(records.has(minKey)) {
        val record = records.get(minKey)
        if(value<record) {
          records.save(minKey,value)
          reports.save(minKey+".report", report)
          timeRecs.save(minKey+".timestamp", System.currentTimeMillis())
          return -1
        }
      } else {
        records.save(minKey,value)
        reports.save(minKey+".report", report)
        timeRecs.save(minKey+".timestamp", System.currentTimeMillis())
      }
      
      0
    } 
    
    def checkSavedScores(score:Double, username:String, station:String, response:String):Boolean = {
      val key = username+"."+station
      if(!store.has(key)) {
        store.put(key,Nil);
      }
      
      val storedReports = store.get(key)
      val now = System.currentTimeMillis
      val report = new WeatherReport(station,username,score,response)
      
      if(storedReports.size<5) {
        if(hasRecentScore(storedReports,now)) {
          store.save(key, replaceRecentScore(storedReports,report,now))
        } else {
          store.save(key, report :: storedReports)
        }
      } else {
        if(storedReports.exists(rec => round2(rec.score)<round2(score))) {
          if(hasRecentScore(storedReports,now)) {
            store.save(key, replaceRecentScore(storedReports,report,now))
          } else {
            //replace smallest scoring report
            store.save(key, storedReports diff List(storedReports.minBy(_.score)))
          }
          return true
        }
      }
      
      false
    }
    
    def hasRecentScore(reports:List[WeatherReport], now:Long):Boolean = {
      reports.exists(now-_.timestamp<DAY)
    }
    
    def replaceRecentScore(reports:List[WeatherReport], replaceReport:WeatherReport, now:Long):List[WeatherReport] = {
      if( reports.filter(now-_.timestamp<DAY)
          .exists(rec => round2(rec.score)<round2(replaceReport.score)) )
        replaceReport :: reports.filter(now-_.timestamp>DAY)
        else reports
    }
    
    def round2(x:Double):Double = {
      "%2f".format(x).toDouble
    }
}

case class WeatherReport(station:String, username:String, score:Double, report:String, timestamp:Long=System.currentTimeMillis);
