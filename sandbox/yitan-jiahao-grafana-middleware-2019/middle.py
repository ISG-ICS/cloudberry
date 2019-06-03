## Get all the data point requested from influxdb and sent part of 
## data to the frontend. 

from http.server import HTTPServer, BaseHTTPRequestHandler
from optparse import OptionParser
import requests
import gzip
import json
import math
import timeit


class RequestHandler(BaseHTTPRequestHandler):
    
    def do_GET(self):
        doGet_start = timeit.default_timer()
        request_path = self.path
        
##        print("\n----- Request Start ----->\n")
##        print("Request path:", request_path)
##        print("Request headers:", self.headers)
##        print("<----- Request End -----\n")
        
        influx_url = "http://localhost:8086"+request_path
##        print("----received data END")

## Sent the query from frontend to the backend.
        start = timeit.default_timer()
        r = requests.get(influx_url)
        stop = timeit.default_timer()
        print('Query Time: ', stop - start) 
        

## COUNT THE DATA with another query (quicker than count locally)
        q = request_path.split('+')
        q[1] = ('count%28%2A%29')
        count = '+'.join(q)
        count_url = "http://localhost:8086"+count
        count_query = requests.get(count_url)
        jdict = json.loads(count_query.content)
        count = jdict["results"][0]["series"][0]["values"][0][1]
##        print("COUNT", jdict["results"][0]["series"][0]["values"][0][1])

##        print("----received data")
##        print("r.header")
##        print(r.headers)      
        json_dict = json.loads(r.content)        
        data = json_dict["results"][0]["series"][0]["values"]
        
## Sent the header
        self.send_response(200)
        for key, value in r.headers.items():
            self.send_header(key,value)
        self.end_headers()

## Check if the size of data hit the limit. If so, extract data evenly from
##        each time interval. Else, do nothing on the data.
        max_point = 2000
        if count <= max_point:
            res = r.content
            gres = gzip.compress(res)
 
        else:
            batchsize = math.floor(count/max_point)
            newlist = []
            for i in range(0,len(data),batchsize):
                newlist.append(data[i])

##            print("SIZE OF THE NEW LIST ",len(newlist))

            json_dict["results"][0]["series"][0]["values"] = newlist

            string_dict = json.dumps(json_dict)
            byte_dict = string_dict.encode()
                
            gres = gzip.compress(byte_dict)

        self.wfile.write(gres)           
##        print("----received data END")
        doGet_end = timeit.default_timer()
        print("doGet Time: ",doGet_end-doGet_start)
        
        
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
## command line parser
    parser = OptionParser()
    parser.usage = ("Creates an http-server that will echo out any GET or POST parameters\n"
                    "Run:\n\n"
                    "   reflect")
    (options, args) = parser.parse_args()
    
    main()
