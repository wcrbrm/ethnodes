val instanceName: String = "rinkeby";

import ammonite.ops._
val wd = pwd

import java.util.{Calendar, Date}
import java.text.SimpleDateFormat
import java.io._
import scala.collection.mutable.HashMap

val cal = Calendar.getInstance
cal.setTime(new Date)
val y = cal.get(Calendar.YEAR)
val sd:SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
val patternDate = "^.*\\[(\\d{2})-(\\d{2})\\|(\\d{2}:\\d{2}).*$"r 
val patternEvent = "^.*\\] (.*)$"r 
val patternParams = "(\\S*)=(\\S*)"r 


case class Param(val field: String, val func: String = "sum")
abstract class ImportAction(val title: String, val path:String, val aggregated: List[Param])

object ImportBlockHeaders extends ImportAction(
  "new block headers", "import.blockheaders",
  List(Param("count"), Param("number", "max"), Param("ignored"))
) 
object ImportBlockReceipts extends ImportAction(
  "new block receipts", "import.blockreceipts",
  List(Param("count"),Param("number", "max"),Param("ignored"))
)
object ImportStateEntries extends ImportAction(
  "new state entries", "import.stateentries",
  List(Param("count"),Param("processed", "max"), Param("pending"), Param("retry"),  Param("duplicate"), Param("unexpected"))
)
object RolledBackHeaders extends ImportAction (
  "Rolled back headers", "import.rolledback", List(Param("count"))
)

val actions:List[ImportAction] = List(ImportBlockHeaders, ImportBlockReceipts, ImportStateEntries, RolledBackHeaders)

var lastFlushTime: Int = 0
val lastFlushTimeFn = wd / "lastUpload.txt"
if (exists(lastFlushTimeFn)) {
   lastFlushTime = read.lines( lastFlushTimeFn )(0).trim.toInt
   println("Last Flush Time: " + lastFlushTime)
}

val _DATA = new HashMap[Long, HashMap[String,Long]]()
def combine(oldValue: Long, newValue: Long, func: String) = {
  if (func == "max") {
    if (oldValue > newValue) oldValue else newValue
  } else if (func == "sum") {
    oldValue + newValue
  } else {
    throw new Exception("Invalid function used for aggregation. Allowed: max, sum")
  }
}

def add(unixTime: Long, path: String, value: Long, func: String) = {
    // ignore time before last flush

  if (lastFlushTime < unixTime) {

    if (!_DATA.contains(unixTime)) {
      _DATA.put(unixTime, new HashMap[String, Long]());
    }
    val mapMoment: HashMap[String, Long] = _DATA.get(unixTime).get
    if(!mapMoment.contains(path)) {
      mapMoment.put(path, value);
    } else {
      mapMoment.put(path, combine(mapMoment.get(path).get, value, func));
    }
  }
}

// in loop:
val line: String = read.lines(wd / instanceName / "docker.log").head
read.lines(wd / 'rinkeby / "docker.log").foreach(line => {
  try {
    val patternDate(d, m, hi) = line
    val isoDate = y + "-" + m + "-" + d + " " + hi + ":00"
    val unixTime = sd.parse(isoDate).getTime / 1000L
  
    if (line.startsWith("INFO")) add(unixTime, "log.total.info", value = 1L, func ="sum")
    if (line.startsWith("WARN")) add(unixTime, "log.total.warn", value = 1L, func ="sum")

    // parse event
    val patternEvent(eventline) = line
    val actionMatch:List[ImportAction] = actions.filter(a => (eventline.contains(a.title)))
    
    actionMatch.foreach(a => {
      val path = a.path
      val expected: List[Param] = a.aggregated
      val paramSeq: Seq[String] = patternParams.findAllMatchIn(eventline).map(_.toString).toSeq 

      // 2. parse properly
      paramSeq.foreach(paramAndValue => {
        val parts = paramAndValue.split("=")
        val field = parts(0)
        val value = parts(1)
        val foundParam = expected.find(p => (p.field == field))
        // println(" field=" + field + ", value=" + value + " expected=" + expected);
        if (foundParam.isDefined) {
          // println(a.path + "." + field + "; " + value + "; " +  foundParam.get.func);
          add(unixTime, a.path + "." + field, value.toLong, foundParam.get.func)
        }
      })
    })
  } catch {
    case _: MatchError =>
  }
  // println("\n--\n")
})


val graphiteHost = "localhost"
case class DataChunk(metricPath: String, timestamp: Long, value: String) {
  def asString:String = s"${metricPath} ${value} ${timestamp}"
}
def sendData(chunks: List[DataChunk]) {
  val conn = new java.net.Socket(graphiteHost, 2003)
  val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(conn.getOutputStream)))
  val in = new BufferedReader(new InputStreamReader(conn.getInputStream))
  println(chunks.map(_.asString).mkString("\n") + "\n")
  out.write(chunks.map(_.asString).mkString("\n") + "\n")
  out.flush
  conn.close
}

_DATA.keys.map(time => {
  val mapEvents = _DATA.get(time).get
  val chunks = mapEvents.keys.map(key => DataChunk(instanceName + "." + key, time, mapEvents.get(key).get.toString)).toList
  // for every time frame
  //  println(time + ": " + chunks.toString)
  sendData(chunks)
  write.over( lastFlushTimeFn, time.toString )
})


