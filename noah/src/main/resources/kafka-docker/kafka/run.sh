docker build . -t kafka
sudo mkdir /home/kafka
docker run --detach -v /home/kafka/:/opt/kafka/data -p 9092:9092 kafka
