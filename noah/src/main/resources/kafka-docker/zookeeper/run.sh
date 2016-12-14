#!/bin/bash

VOLUME_DIR=/home/zookeeper
sudo mkdir $VOLUME_DIR
docker run --detach -v $VOLUME_DIR:/opt/zookeeper/data/ -p 2181:2181 xizzz/zookeeper
