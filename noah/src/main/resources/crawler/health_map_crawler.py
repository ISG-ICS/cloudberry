import urllib2
import urllib
import datetime
import io
import os
import json
import sys
 

#get health map response object
def get_health_map(url, headers):
    request = urllib2.Request(url, headers = headers)
    response = urllib2.urlopen(request)
    return response

# create the parameters, needs start_date and end_date in "mm/dd/yyyy" format
def generate_health_map_query(start_date, end_date):
    parameter_map = {"category":["1","2","29"],"diseases":["282"], "heatscore":"1", "partner":"hm"}
    parameter_map['sdate'] = start_date
    parameter_map['edate'] = end_date
    return parameter_map
    
    
def getUrl(alertid, trto, trfr, pid):
    query = {}
    query['trto'] = trto
    query['trfr'] = trfr
    url = "http://www.healthmap.org/ai.php?"+alertid+"&"+urllib.urlencode(query)+"&"+"pid"+pid
    return url

def storeHtml(response, fname, headers):
    request = urllib2.Request(url, headers = headers)
    response = urllib2.urlopen(request)
    with io.open(fname+".html", "w", encoding="utf-8") as writeFile:
        writeFile.write(response.read().decode("utf-8"))
        writeFile.write(u"\u000A")

if __name__ == '__main__':
    
    #headers
    headers = {}
    headers['User-Agent'] = "Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.27 Safari/537.17"
    
    dirName = sys.argv[1]
    fileName = sys.argv[2]
    days = sys.argv[3]
    
    if not os.path.exists(dirName):
        os.makedirs(dirName)
        
    # get today's date
    date_counter = datetime.date.today()
    condition = (date_counter - datetime.timedelta(int(days))).strftime("%m/%d/%Y")      
    # set interval to 1 day
    interval = datetime.timedelta(1)
    
    #get the end date and start date of 1 days interval
    end_date = date_counter.strftime("%m/%d/%Y")
    start_date = (date_counter - interval).strftime("%m/%d/%Y")
       
    while end_date != condition:
        newDirName = dirName+"\\"+str(date_counter)
        if not os.path.exists(newDirName):
            os.makedirs(newDirName)
            
        completeName = os.path.join(newDirName, fileName)
        with io.open(completeName + ".json", "w", encoding="utf-8") as writeFile:
            # get response from healthmap
            url = "http://www.healthmap.org/getAlerts.php?"
            response = get_health_map(url + urllib.urlencode(generate_health_map_query(start_date, end_date)), headers)
            #write to file
            writeFile.write(response.read().decode("utf-8"))
            writeFile.write(u"\u000A")
        date_counter -= interval
        end_date = date_counter.strftime("%m/%d/%Y")
        start_date = (date_counter - interval).strftime("%m/%d/%Y")
        
                    
        with io.open(completeName + ".json", "r", encoding="utf-8") as readFile:
            data = json.load(readFile)
            for id in data["markers"]:    
                html = id["html"]
                res = html.find("javascript:b(")
                start = res + len("javascript:b(")
                end = html.find(")", start)
                result = html[start:end]
                elements = result.split(',')
                alertid = elements[0]
                trto = elements[1].strip('\'')
                trfr = elements[2].strip('\'')
                pid = elements[3]
                url = getUrl(alertid, trto, trfr, pid)
                response = get_health_map(url, headers)
                fname = os.path.join(newDirName, alertid)           
                storeHtml(response, fname, headers)