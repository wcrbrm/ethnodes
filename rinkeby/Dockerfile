FROM ubuntu
RUN apt-get -y update \
  && apt-get install -y software-properties-common \
  && add-apt-repository -y ppa:ethereum/ethereum \
  && apt-get -y update \
  && apt-get install -y ethereum wget
EXPOSE 8547 30306 30306/udp
VOLUME /mnt/rinkeby
CMD wget --no-check-certificate https://www.rinkeby.io/rinkeby.json -O ~/rinkeby.json \
  && geth --datadir /mnt/rinkeby init ~/rinkeby.json
CMD geth --datadir=/mnt/rinkeby --networkid=4  --rinkeby \
 --rpc --rpcaddr='0.0.0.0' --rpcport=8547 --wsport=30306 --rpcapi='personal,eth,network,web3' \
  --rpccorsdomain "http://localhost:3000,http://localhost:3001,http://localhost:3002,http://test.thistoken.rocks,https://thistoken.rocks" 