## This file is used to generate random time series data, which can be
##   imported by influxdb:
##     influx -import -path=FILE_NAME -precision=s -database=DB_NAME


import random
import math

db_name = "sinedb" # the db to create
print("# DDL")

print("CREATE DATABASE {}".format(db_name)) 

print("# DML")

print("# CONTEXT-DATABASE: {}".format(db_name))

## The range is the time range using unix time stamp.
## The function in format may change to the desired pattern.
for i in range(1546300800, 1546948800):
	print("h2o_temperature,location=santa_monica degrees={} {}"\
              .format(10*math.sin(i/10000) + random.randint(-3,3),i))


