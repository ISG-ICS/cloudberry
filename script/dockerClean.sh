#!/usr/bin/env bash
#clean up the existing images
docker stop -f cc nc1
docker rm -f cc nc1
docker volume rm dbstore
# remove the local image to fetch the newest remote version
docker rmi jianfeng/asterixdb


