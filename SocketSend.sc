import java.io._

def sendPlainData(metricPath: String, tm: Long, value: String) {
  val conn = new java.net.Socket("localhost", 2003)
  val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(conn.getOutputStream)))
  val in = new BufferedReader(new InputStreamReader(conn.getInputStream))
  out.write(metricPath + " " + value + " " + tm + "\n")
  out.flush
  conn.close
}

case class DataChunk(metricPath: String, timestamp: Long, value: String) {
  def asString:String = s"${metricPath} ${value} ${timestamp}"
}
def sendData(chunks: List[DataChunk]) {
  val conn = new java.net.Socket("localhost", 2003)
  val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(conn.getOutputStream)))
  val in = new BufferedReader(new InputStreamReader(conn.getInputStream))
  println(chunks.map(_.asString).mkString("\n") + "\n")
  out.write(chunks.map(_.asString).mkString("\n") + "\n")
  out.flush
  conn.close
}

val tm: Long = (%%date("+%s")).out.string.trim.toLong

sendData(List(
  DataChunk("local.random.dice", tm, (Math.random * 10).toString),
  DataChunk("local.random.roll", tm, (Math.random * 10).toString)
))

// sendPlainData(path, new java.util.Date().getTime() / 1000L, (Math.random * 3).toString)

