#!/bin/bash
docker build . -t kafka
sudo mkdir /home/kafka-data
docker run --detach -v /home/kafka-data/:/opt/kafka/data -p 9092:9092 kafka
