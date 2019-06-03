# Ingest random data into influxdb

from influxdb import InfluxDBClient
from time import time,sleep
import datetime
import random

client = InfluxDBClient(host='localhost', port=8086)
client.switch_database('test')

while True:
    time = datetime.datetime.now() + datetime.timedelta(hours=8)
    json =     [{
        "measurement": "h2o_temperature",
        "time": "{}".format(time),
        "fields": {
            "degrees": float(random.randint(60,80))
        }
    }]

    client.write_points(json)



    sleep(2)
    print("write temp: {}".format(json[0]['fields']['degrees']))
