## Use sample() in the query to get sample data from influxdb

from http.server import HTTPServer, BaseHTTPRequestHandler
from optparse import OptionParser
import urllib.parse as up
import requests
import gzip
import json
import math
import QueryInfo as qi
import timeit

class RequestHandler(BaseHTTPRequestHandler):
    
    def do_GET(self):
        doGet_start = timeit.default_timer()
        
        request_path = self.path
        
        print("\n----- Request Start ----->\n")
        print("Request path:", request_path)
        print("<----- Request End -----\n")
        
        influx_url = "http://localhost:8086"+request_path

## sent query to influxdb

        urlquery = up.urlparse(influx_url).query
        tuple_list = up.parse_qsl(urlquery)
        query_string = tuple_list[-1][1]

## COUNT THE DATA by sending a query to infludb
        q = request_path.split('+')
        q[1] = ('count%28%2A%29')
        count = '+'.join(q)
        count_url = "http://localhost:8086"+count
        count_start = timeit.default_timer()
        count_query = requests.get(count_url)
        count_end = timeit.default_timer()
        print("count query time:", count_end - count_start)
        jdict = json.loads(count_query.content)
        count = jdict["results"][0]["series"][0]["values"][0][1]

## Sample the dataset with more than 2k points
        max_point = 2000
        if count <= max_point:
            print("DIDN'T MODIFY THE QUERY")
 
        else:
            query_info = qi.QueryInfo(query_string)
            lowerlimit = query_info.get_time_range()[0]
            upperlimit = query_info.get_time_range()[1]
            groupsize = math.floor((upperlimit-lowerlimit)/max_point)

            new_query = query_info.change_to_sample(max_point)
            
            new_tuple =("q",new_query)
            tuple_list[-1] = new_tuple

            parturl = up.urlencode(tuple_list)
            new_url = "http://localhost:8086/query?"+parturl
            influx_url = new_url
            
## Get sample data from influxdb
        start = timeit.default_timer()
        influx_resp = requests.get(influx_url)
        ## execute the query in influxdb
        stop = timeit.default_timer()
        print('Query Time: ', stop - start) 
 
        
        json_dict = json.loads(influx_resp.content)
        print("####################")

        data = json_dict["results"][0]["series"][0]["values"]        

## Forward infludb hearder respone to frontend
        self.send_response(200)
        for key, value in influx_resp.headers.items():
            self.send_header(key,value)
        self.end_headers()

## Compress the file and sent to frontend
        res = influx_resp.content
        gres = gzip.compress(res)
        self.wfile.write(gres)               
            
##        print("----received data END")
        doGet_end = timeit.default_timer()
        print("Total time in middleware: ",doGet_end-doGet_start)
        


        
    def do_POST(self):
        
        request_path = self.path
        
        print("\n----- Request Start ----->\n")
        print("Request path:", request_path)
        
        request_headers = self.headers
        content_length = request_headers.get('Content-Length')
        length = int(content_length) if content_length else 0
        
        print("Content Length:", length)
        print("Request headers:", request_headers)
        print("Request payload:", self.rfile.read(length))
        print("<----- Request End -----\n")
        
        self.send_response(200)
        self.end_headers()
    
    do_PUT = do_POST
    do_DELETE = do_GET
        
def main():

    port = 8080
    print('Listening on localhost:%s' % port)
    server = HTTPServer(('', port), RequestHandler)
    server.serve_forever()

        
if __name__ == "__main__":
    parser = OptionParser()
    parser.usage = ("Creates an http-server that will echo out any GET or POST parameters\n"
                    "Run:\n\n"
                    "   reflect")
    (options, args) = parser.parse_args()
    
    main()
