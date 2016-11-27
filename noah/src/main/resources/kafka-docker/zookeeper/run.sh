docker build . -t zookeeper
sudo mkdir /home/zookeeper
docker run --detach -v /home/zookeeper/:/opt/zookeeper/data/ -p 2181:2181 zookeeper
