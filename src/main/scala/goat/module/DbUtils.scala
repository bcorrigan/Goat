package goat.module

import goat.core.{IrcMessage, KVStore, Module}
import goat.util.CommandParser
import java.io.IOException

import scala.jdk.CollectionConverters._
import scala.collection.TraversableOnce

class DbUtils extends Module {

  val store = new KVStore[AnyRef]()
  
  override def getCommands(): Array[String] = {
    Array("db")
  }
  
  override def processPrivateMessage(m:Message):Unit = {
    processChannelMessage(m)
  }
  
  /**
   * db read key=keyName
   * db del key=keyName
   * db set key=keyName value=val
   * db find key=keyName | value=val
   */
  override def processChannelMessage(m:Message):Unit = {
    val parser = new CommandParser(m)
 
    if(validReq(parser,m)) {
      (parser.remaining,m.isAuthorised()) match {
        case ("read",true) =>  read(m,parser)
        case ("set",true) =>  set(m,parser)
        case ("del",true) => del(m,parser)
        case ("find",true) => find(m,parser)
        case ("dump",true) => dump(m,parser)
        case ("load",true) => load(m,parser)
        case ("compact",true) => compact(m)
        case ("stats",_) => stats(m)
        case ("help",_) => m.reply("db load <file=optional>| dump <file=optional> | compact | read key=required | set key=required value=required | del key=required | find key=required")
        case (_,true) => m.reply("Not familiar with that one.")
        case (_,false) => m.reply("You're not allowed to mind meld with me, pillock.")
      }
    }
  }
  
  def validReq(parser:CommandParser, m:Message):Boolean = {
    val subcmd = parser.remaining
    subcmd match {
      case "read" | "del" | "set" | "find" =>
        if(parser.hasVar("key")) {
          subcmd match {
            case "read" | "del" => return true
            case "find" =>
              if(parser.hasVar("value")) {
                m.reply("Master, you should only supply a key or a value to find, not both at once.")
                return false
              } else return true
            case "set" => 
              if(parser.hasVar("value"))
                return true
              else {
                m.reply("Master, you need to supply a value to set.")
                return false
              }
          }
        } else {
          subcmd match {
            case "read" | "del" =>
              m.reply("Sorry my master, you need to supply a key - eg key=somekeyName")
              return false
            case "find" =>
              if(parser.hasVar("value")) {
                m.reply(m.getSender+": Master, you should really only specify a key or a value to find, not both.")
                return false
              } else return true
            case "set" =>
              if(parser.hasVar("value")) {
                m.reply(m.getSender + ": Oh master, you must supply a key to set the value for. Forgive my confusion.")
                return false
              } else return true
          }
        }
      case "dump" | "load" | "compact" | "help" | "stats" =>
        return true;
      case _ => 
        m.reply("wat:" + subcmd + ":")
        return false
    }
  }
  
  def stats(m:Message):Unit = {
    m.reply("My brain has " + store.size() + " keys in it." )
  }
  
  /*
   * Read a key and spew it into channel.
   */
  def read(m:Message,parser:CommandParser):Unit = {
    val key = parser.get("key")
    if(store.has(key)) {
      val value = store.get(key)
      val valueStr = if(value.isInstanceOf[Array[Any]]) {
        //this used to be deep() so might now miss deeply nested keys
        //probably need ot invent some kind of arbitrarily nested magical flattening function
        value.asInstanceOf[Array[Any]].mkString(",")
      } else value.toString()
      
      m.reply(m.getSender+": " + key + "=>" + value.getClass().getName() + ": " + valueStr )
    } else {
      m.reply(m.getSender+": dinnae ken yon key :(")
    }
  }
  
  def set(m:Message,parser:CommandParser):Unit = {
    val key = parser.get("key")
    val value = parser.get("value")
    store.save(key,value);
    m.reply(m.getSender+": Set!")
  }
  
  def del(m:Message, parser:CommandParser):Unit = {
    val key = parser.get("key")
    if(store.has(key)) {
      store.remove(key)
      m.reply(m.getSender+": zapped " + key)
    } else {
      m.reply(m.getSender+": I'm afraid " + key + " doesn't exist, so can't be deleted.")
    }
  }
  
  def find(m:Message,parser:CommandParser):Unit = {
    if(parser.hasVar("key")) {
      val keys  = KVStore.findKeys(".*"+parser.get("key")+".*").asScala
      if(keys.length>1) {
        m.reply(m.getSender + ": Found several keys: " + keys.reduceLeft((r,l) => r + ", " + l))
      } else if(keys.length==1) {
        m.reply(m.getSender + ": One matching key: " + keys.head)
      } else {
        m.reply(m.getSender + ": No keys found to match that.")
      }
    }
  }

  private def getFile(parser:CommandParser):String = {
    if(parser.hasVar("file")) {
      parser.get("file")
    } else "goatdb_dump.txt";
  } 
  
  def dump(m:Message, parser:CommandParser):Unit = {
    val file=getFile(parser);
    try {
        store.dump(file);
        m.reply("Master, successfully dumped DB to " + file)
    } catch {
      case e:IOException =>
        m.reply("IO Error dumping DB: " + e.getMessage())
    }
  }
  
  def load(m:Message, parser:CommandParser):Unit = {
    val file=getFile(parser);
    
    try {
      val duffObjects = store.load(file);
      if(duffObjects.length>0) {
        m.reply("My mind has been replaced, but these objects couldn't be deserialised (I just ploughed on and ignored errors anyway) :" + duffObjects.toString)
      } else {
        m.reply("My mind has been replaced, and with no problems or errors with any props at all. Yay!")
      }
    } catch {
      case e:IOException =>
        m.reply("Oh dear, there was an unrecoverable IO error. I rolled back DB though, whew! Error was:" + e.getMessage)
    }
  }
  
  def compact(m:Message):Unit = {
    store.compact();
    m.reply(m.getSender() + ": Compacted store.")
  }
}