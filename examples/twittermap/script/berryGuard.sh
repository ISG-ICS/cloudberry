#!/usr/bin/env bash
#===============================================================================
#
#          FILE: berryGuard.sh
#
#         USAGE: ./berryGuard.sh [*DB host:port] [CB host:port] [TM host:port]
#                [Run as deamon] 
#                  !NOTE: crontab or nohup will not work properly because,
#                         when being called by another backgroud command,
#                         the 'wscat' command -w will not work at all.
#                  Please use 'screen' command to run it as daemon:
#                  $ screen
#                  $ ./berryGuard.sh |& tee -a berryGuard.log
#
#   DESCRIPTION: Touch AsterixDB, Cloudberry and Twittermap each once every hour
#                to see whether they are alive, if any of them is not
#                responding properly, send an e-mail to administrators.
#
#       OPTIONS: 
#  REQUIREMENTS: (1) Python 2.7
#                (2) npm:
#                  ubuntu: sudo apt-get update
#                          sudo apt-get install nodejs
#                          sudo apt-get install npm
#                  CentOS: sudo yum install epel-release
#                          sudo yum install nodejs
#                          sudo yum install npm
#                (3) wscat2:
#                  sudo npm i -g npm
#                  sudo npm install -g wscat@2.1.1
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Qiushi Bai (), baiqiushi@gmail.com
#  ORGANIZATION: ics.uci.edu
#       CREATED: 4/7/2018 11:06:01 PM PST
#      REVISION:  ---
#===============================================================================

set -o nounset                              # Treat unset variables as an error
asterixDBHostPort=${1:-"localhost:19002"}
cloudberryHostPort=${2:-"localhost:9000"}
twittermapHostPort=${3:-"localhost:9001"}

# Add more administrators here.
# !!! Must put your e-mail address here !!!
adminEmails=("your@email.com")

# parameter $1: subject
# parameter $2: content
sendEmail() 
{ 
	echo "Sending e-mail to administrators..."
	local emailTimeStamp=$(date)
	local emailSubject=$1
	local emailContent=$2
	echo "Time: " ${emailTimeStamp}
	echo "subject : " ${emailSubject}
	echo "Content :" ${emailContent}
	for addr in ${adminEmails[@]}
	do
		emailRes=$(mail -s "[Cloudberry Alert] ! ${emailSubject}" ${addr} <<< "[${emailTimeStamp}] ${emailSubject} ${emailContent}")
		echo "email sending result: " ${emailRes}
	done
}

touchAsterixDB()
{
	echo "[1] Touching AsterixDB with \"select count(*) from berry.meta\" ..."
	local result=$(curl -s --max-time 900 --data-urlencode "statement=select count(*) from berry.meta;" http://${asterixDBHostPort}/query/service)
	echo "Result is :" ${result}
	local status=$(echo $result | python -c "import sys, json; print json.load(sys.stdin)['status']" 2>/dev/null)
	echo "Status = " ${status}
	if [ "${status}" = "success" ] ; then
		echo "[Good!] AsterixDB is alive."
	else
		echo "[Bad!] AsterixDB maybe down."
		sendEmail "AsterixDB maybe down." "Please check here: http://${asterixDBHostPort}/admin/cluster"
	fi
}

touchCloudberry()
{
	echo "[2] Touching Cloudberry with \"{\"dataset\":\"twitter.ds_tweet\",\"global\":{\"globalAggregate\":{\"field\":\"*\",\"apply\":{\"name\":\"count\"},\"as\":\"count\"}},\"estimable\":true}\" ..."
	local result=$(wscat -x '{"dataset":"twitter.ds_tweet","global":{"globalAggregate":{"field":"*","apply":{"name":"count"},"as":"count"}},"estimable":true}' -w 5 -c ws://${cloudberryHostPort}/ws)
	echo "Result is :" ${result}
	result=$(echo ${result} | tr -d "[]")
	echo "Result is :" ${result}
	local cnt=$(echo $result | python -c "import sys, json; print json.load(sys.stdin)['count']")
	echo "cnt = " ${cnt}
	if [ "${cnt}" -ge "0" ] ; then
		echo "[Good!] Cloudberry is working properly."
		return
	fi
	echo "[Bad!] Cloudberry is NOT working properly."
	sendEmail "Cloudberry is NOT working properly." "The total count of ds_tweet retrieved is ${cnt}."
}

touchTwittermap()
{
	echo "[3] Touching Twittermap with http request ..."
	local httpCode=$(curl --max-time 900 -s -o /dev/null -w "%{http_code}" http://${twittermapHostPort})
	echo "httpCode is :" ${httpCode}
	if [ "${httpCode}" = "200" ] ; then
		echo "[Good!] Twittermap is accessible."
	else
		echo "[Bad!] Twittermap is NOT accessible."
		sendEmail "Twittermap is NOT accessible." "Please check here: http://${twittermapHostPort}"
	fi
}

while true
do
    touchAsterixDB
    touchCloudberry
    touchTwittermap
    sleep 3600 # every hour
done