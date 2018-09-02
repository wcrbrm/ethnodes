import ammonite.ops._

// generate Docker file
class GethInstance(net:String = "mainnet", httpPort: Int = 8545, wsPort: Int = 30306, networkId:Int = 1) {
  def build = {
    val apis = "personal,eth,network,web3";
    val corsAllowed = List(
       "http://localhost:3000",
       "http://localhost:3001",
       "http://localhost:3002",
       "http://test.thistoken.rocks",
       "https://thistoken.rocks"
    );

    val bind: String = "0.0.0.0"
    val boot = "enode://a24ac7c5484ef4ed0c5eb2d36620ba4e4aa13b8c84684e1b4aab0cebea2ae45cb4d375b77eab56516d34bfbd3c1a833fc51296ff084b770b94fb9028c4d25ccf@52.169.42.101:30303"
    val args: String = if (networkId == 4) " --rinkeby" else ""

    val initCommands: List[String] = if (networkId == 4) {
        List(
            s"CMD wget --no-check-certificate https://www.rinkeby.io/rinkeby.json -O ~/$net.json \\",
            s"  && geth --datadir /mnt/$net init ~/$net.json"
        )
    } else List()
 
    val dockerLines: List[String] = List(
       "FROM ubuntu",
       "RUN apt-get -y update \\",
           "  && apt-get install -y software-properties-common \\",
           "  && add-apt-repository -y ppa:ethereum/ethereum \\",
           "  && apt-get -y update \\",
           "  && apt-get install -y ethereum wget",
       s"EXPOSE ${httpPort} ${wsPort} ${wsPort}/udp",
       s"VOLUME /mnt/${net}") ++ initCommands ++ List(
       s"CMD geth --datadir=/mnt/$net --networkid=${networkId} ${args} \\",
           s" --rpc --rpcaddr='0.0.0.0' --rpcport=$httpPort --wsport=$wsPort --rpcapi='$apis' \\",
           "  --rpccorsdomain \"" + corsAllowed.mkString(",") + "\" "
       );

    write.over( wd / net / "Dockerfile", dockerLines.mkString("\n"))
    %docker("build", "--tag", "wcrbrm/geth-" + net, "./" + net)
    this
  }

  def run = {
    %docker("run", "-d",
      "--name", s"eth_${net}", 
     // ports:
      "-p", s"${wsPort}:${wsPort}",
      "-p", s"${httpPort}:${httpPort}",
       // volumes:
      "-v", s"/mnt/${net}:/mnt/${net}",
      s"wcrbrm/geth-${net}"
    )
    this
  }

  def stop = {
    println(s"Stopping eth_$net")
    %docker("stop", s"eth_${net}")
    this
  }

  def logs = {
    %docker("logs", s"eth_${net}")
  }

  def remove = {
    println(s"Removing eth_$net container")
    %docker("container", "rm", s"eth_$net")
    this
  }

}

object Mainnet extends GethInstance(net = "mainnet", httpPort = 8545, wsPort = 30303)
object Rinkeby extends GethInstance(net = "rinkeby", httpPort = 8547, wsPort = 30306, networkId = 4)

// Mainnet.stop.remove.build.run.logs
// Rinkeby.stop.remove.build.run.logs
