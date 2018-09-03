val instanceName: String = "rinkeby";

import ammonite.ops._
val wd = pwd

import java.util.{Calendar, Date}
import java.text.SimpleDateFormat

val cal = Calendar.getInstance
cal.setTime(new Date)
val y = cal.get(Calendar.YEAR)
val sd:SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
val patternDate = "^.*\\[(\\d{2})-(\\d{2})\\|(\\d{2}:\\d{2}).*$"r 
val patternEvent = "^.*\\] (.*)$"r 
val patternParams = "(\\S*)=(\\S*)"r 


case class Param(field: String, func: String = "sum")
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


val lastFlushTime: Int = 0
// TODO: load lastFlushTime from the file

val _DATA = new HashMap[Long, Map[String,Long]]()
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
  if (!_DATA.containsKey(unixTime)) _DATA.put(unixTime, new HashMap[String, Long]());
  val mapMoment: Map[String, Long] = _DATA.get(unixTime)
  if(!mapMoment.containsKey(path)) {
    mapMoment.put(path, value);
  } else {
    mapMoment.put(path, combine(mapMoment.get(path), value, func));
  }
}

// in loop:
val line: String = read.lines(wd / instanceName / "docker.log").head
read.lines(wd / 'rinkeby / "docker.log").foreach(line => {
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

    // paramSeq.foreach( // split, and check if key is expected, if expected, then write into _DATA
    // TODO: foreach param: add(unixTime, a.path + "." + param.field, value=, func= param.func)
  })
})

// TODO: DATA can be saved into output
// TODO: flush all DATA (use AMPQ batches)
// TODO: save lastFlushTime

