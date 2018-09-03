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
  def asString:String = s"""("${metricpath}", (${timestamp}, ${value}))"""
}
def sendData(chunks: List[DataChunk]) {
  val conn = new java.net.Socket("localhost", 2004)
  val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(conn.getOutputStream)))
  val in = new BufferedReader(new InputStreamReader(conn.getInputStream))

  val payload: String = "[" + chunks.map(_.asString).mkString(",") + "]"
  println(payload)
  val len:Int = payload.length
  println(len)
  out.write(Array[Char]((len & 0xFF).toChar, (len >> 8).toChar, 0, 0))
  out.write(payload)
  out.flush

  Stream.continually(in.readLine()).takeWhile(_ != -1).map(println)
  conn.close
}

val tm: Long = (%%date("+%s")).out.string.trim.toLong

sendData(List(
  DataChunk("local.random.dice", tm, (Math.random * 10).toString),
  DataChunk("local.random.roll", tm, (Math.random * 10).toString)
))

// sendPlainData(path, new java.util.Date().getTime() / 1000L, (Math.random * 3).toString)

