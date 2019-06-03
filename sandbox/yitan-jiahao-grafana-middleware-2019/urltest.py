
import urllib.parse as up
o = up.urlparse("http://localhost:8086/query?db=test&epoch=ms&q=SELECT+%22degrees%22+FROM+%22h2o_temperature%22+WHERE+time+%3E%3D+1546329600000ms+and+time+%3C%3D+1546329620000ms")

print(o)
query = o.query


p = up.parse_qsl(query)
print()
print(p)
print(1111111111)

query_string = up.parse_qsl(query)[-1][1]

print(query_string)


print(p[-1])

query = p[-1][1]
print(p[-1][1])

ql = query.split()
print(ql)

influx_url = "http://localhost:8086/query?db=test&epoch=ms&q=SELECT+%22degrees%22+FROM+%22h2o_temperature%22+WHERE+time+%3E%3D+1546329600000ms+and+time+%3C%3D+1546329620000ms"
print(influx_url.find("q="))
print(influx_url[45])
print(influx_url[:47])


print(up.urlencode(p))
import QueryInfo as qi

query_info = qi.QueryInfo(query_string)
lowerlimit = query_info.get_time_range()[0]
upperlimit = query_info.get_time_range()[1]


print(lowerlimit, upperlimit)
