# Twitter Tool 

## About Twitter Stream Data 
* DataType

https://dev.twitter.com/streaming/overview/messages-types

* Tweets:
https://dev.twitter.com/overview/api/tweets

## The easiest way to run this demo on local machine
One time task to prepare the AsterixDB instance and the sample data
```
cd src/main/resources/script

# run the asterix instance in two docker container, one cc + one nc
./docker_run_asterix.sh
# load the sample data 
./upload_sample.sh
```
Then start the web server and query on!
```
# start the python web server
cd src/main/resources/twitter-demo
./run_tweetbook_demo.py
```
