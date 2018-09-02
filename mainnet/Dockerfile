FROM ubuntu
RUN apt-get -y update \
  && apt-get install -y software-properties-common \
  && add-apt-repository -y ppa:ethereum/ethereum \
  && apt-get -y update \
  && apt-get install -y ethereum wget
EXPOSE 8545 30303 30303/udp
VOLUME /mnt/mainnet
CMD geth --datadir=/mnt/mainnet --networkid=0 \
  --rpc --rpcaddr '208.76.51.220' --rpcport=8545 --wsport=30303 --rpcapi='personal,eth,network,web3' \
  --rpccorsdomain "http://localhost:3000,http://localhost:3001,http://localhost:3002,http://test.thistoken.rocks,https://thistoken.rocks" 