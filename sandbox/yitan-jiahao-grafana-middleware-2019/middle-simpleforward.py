"""
Simple Forwarding Middleware
    A simple middleware that forwards the query to influxdb,
    gets the data from db, and passes it back to grafana
"""

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
        
        print("\n----- Request Start ----->\n")
        print("Request path:", request_path)
        print("<----- Request End -----\n")
        
        influx_url = "http://localhost:8086"+request_path
        start = timeit.default_timer()
        r = requests.get(influx_url)
        stop = timeit.default_timer()
        print('Query Time: ', stop - start) 

## Sent header info to frontend
        self.send_response(200)
        for key, value in r.headers.items():
            self.send_header(key,value)
        self.end_headers()

## Compress and sent to frontend
        res = r.content
        gres = gzip.compress(res)
        self.wfile.write(gres)           
        
        doGet_end = timeit.default_timer()
        print("Total Time in middleware: ",doGet_end-doGet_start)
        
    
    do_PUT = do_POST
    do_DELETE = do_GET
        
def main():
## Listening on port 8080
    port = 8080
    print('Listening on localhost:%s' % port)
    server = HTTPServer(('', port), RequestHandler)
    server.serve_forever()

        
if __name__ == "__main__":
## Parser for command line options
    parser = OptionParser()
    parser.usage = ("Creates an http-server that will echo out any GET or POST parameters\n"
                    "Run:\n\n"
                    "   reflect")
    (options, args) = parser.parse_args()
    main()
