
/*
keys to be used:
7WE4Z7AQ6RUWPGZCJFFHQMSWSXQYGBRER8
WXUUWKYP731JJYX4HHAXQJIBXR29AB39SY
T2W7GWYSVR6THTGDMTZXWADHV5IQEWZRTK

parse hex from "result"
https://api.etherscan.io/api?module=proxy&action=eth_blockNumber&apikey=7WE4Z7AQ6RUWPGZCJFFHQMSWSXQYGBRER8
https://api-rinkeby.etherscan.io/api?module=proxy&action=eth_blockNumber&apikey=YourApiKeyToken
*/

import $ivy.`org.scalaj::scalaj-http:2.3.0`
import $ivy.`com.lihaoyi::ujson:0.6.6`
import scalaj.http._
import ujson.Js
import ammonite.ops._

def getRemoteBlockNumber(host:String = "https://api.etherscan.io"): Int = {
  try {
    val apiKey:String = "7WE4Z7AQ6RUWPGZCJFFHQMSWSXQYGBRER8"
    val json: String = Http(host + "/api?module=proxy&action=eth_blockNumber&apikey=" + apiKey)
        .header("Content-Type", "application/json")
        .option(HttpOptions.readTimeout(20000))
        .asString.body
    val data = ujson.read(json);
    Integer.parseInt(data("result").str.replace("0x", ""), 16)
  } catch {
    case e => {
	println( "ERROR " + host + ", " + e.toString() )
	0
    }
  }
}

def getLocalBlockNumber(port:Int = 8545): Int = {
  try {
    val json = Http("http://localhost:" + port + "/")
      .header("Content-Type", "application/json")
      .postData("{\"method\":\"eth_blockNumber\",\"params\":[],\"jsonrpc\":\"2.0\"}")
      .option(HttpOptions.readTimeout(3000))
      .asString.body
    val data = ujson.read(json);
    Integer.parseInt(data("result").str.replace("0x", ""), 16)
  } catch {
    case e => { 
	println( "ERROR localhost:" + port + ", " + e.toString() )
	0
    }
  }
}

def flushLocalAndRemote(net: String = "mainnet", remoteHost: String, localPort: Int = 8545) = {
  val blockNumbers = List(getRemoteBlockNumber(remoteHost), getLocalBlockNumber(localPort));
  println(net + "\t" + blockNumbers.mkString(";"));
  val fn = s"eth-${net}"
  write.over(root / 'var / 'log / fn, blockNumbers.mkString("\n"));
}

flushLocalAndRemote(net="mainnet", remoteHost="https://api.etherscan.io", 8545)
flushLocalAndRemote(net="rinkeby", remoteHost="https://api-rinkeby.etherscan.io", 8547)

//println("REMOTE BLOCK #" + getRemoteBlockNumber())
//println("LOCAL BLOCK #" + getLocalBlockNumber())
