#===============================================================================
#
#          FILE: ingestElasticData.py
#
#         USAGE: used in elasticGeoTag.sh
#
#   DESCRIPTION: ingest data into Elasticsearch cluster
#
#       OPTIONS: ---
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Dayue Bai (dayueb@uci.edu), Baihao Wang (baihaow@uci.edu)
#  ORGANIZATION: ics.uci.edu
#       CREATED: 11/02/2019 21:29:00 PM PST
#      REVISION:  ---
#===============================================================================

import sys

print("[info]Checking Python interpreter version...\n[info]Make sure to use Python 3.0+")
if sys.version_info.major >= 3:
    print("[info]Passed!")
    from urllib import request
else:
    raise Exception("[error]Must be using Python 3.0+")

COUNTER = 0
BUFFER_SIZE_LIMIT = 40000
URL = "http://localhost:9200/twitter.ds_tweet/_doc/_bulk"
HEADERS = {"Content-type": "application/json"}
buffer = []

for tweet in sys.stdin:
    COUNTER += 1
    buffer.append(tweet)

    if COUNTER >= BUFFER_SIZE_LIMIT:
        COUNTER = 0
        data = ("".join(buffer)).encode("utf-8")
        buffer = []
        req = request.Request(URL, data=data, headers=HEADERS)
        res = request.urlopen(req)

if buffer:
    data = ("".join(buffer)).encode("utf-8")
    req = request.Request(URL, data=data, headers=HEADERS)
    res = request.urlopen(req)