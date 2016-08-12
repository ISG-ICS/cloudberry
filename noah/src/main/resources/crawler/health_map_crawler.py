import urllib2
import urllib
import datetime
import io
import os
import json
import sys

def argumentCheck():
    if len(sys.argv) != 4:
        sys.exit("Invalid number of Arguments... \n Enter the directory name, filename where json data needs to be stored and number of days in arguments: \n Example: python health_map_crawler.py HealthMapResult 365")  
    dirName = sys.argv[1]
    fileName = sys.argv[2]
    days = sys.argv[3]
    directoryCheck(dirName)
    return dirName, fileName, days

def getDatesAndCondition():
    # get today's date
    dateCounter = datetime.date.today()
    #get the stop condition date
    condition = (dateCounter - datetime.timedelta(int(days))).strftime("%m/%d/%Y")
    # set interval to 1 day
    interval = datetime.timedelta(1)
    return dateCounter, condition, interval

def getStartAndEndDate(dateCounter, interval):
    #get the end date and start date of 1 days interval
    endDate = dateCounter.strftime("%m/%d/%Y")
    startDate = (dateCounter - interval).strftime("%m/%d/%Y")
    return endDate, startDate 
     
#get health map response object
def getHealthMapResponse(url, headers):
    request = urllib2.Request(url, headers = headers)
    response = urllib2.urlopen(request)
    return response

# create the parameters, needs start_date and end_date in "mm/dd/yyyy" format
def generateHealthMapQuery(start_date, end_date):
    parameter_map = {"category":["1","2","29"],"diseases":["282"], "heatscore":"1", "partner":"hm"}
    parameter_map['sdate'] = start_date
    parameter_map['edate'] = end_date
    return parameter_map   
    
def getEachUrl(alertid, queryParamTo, queryParamFr, pid):
    query = {}
    query['trto'] = queryParamTo
    query['trfr'] = queryParamFr
    url = "http://www.healthmap.org/ai.php?"+alertid+"&"+urllib.urlencode(query)+"&"+"pid"+pid
    return url
    
def storeJsonResult(completeName, startDate, endDate, headers):
    with io.open(completeName + ".json", "w", encoding="utf-8") as writeFile:
            # get response from healthmap
            url = "http://www.healthmap.org/getAlerts.php?"
            response = getHealthMapResponse(url + urllib.urlencode(generateHealthMapQuery(startDate, endDate)), headers)
            #write to file
            writeFile.write(response.read().decode("utf-8"))

def storeHtmlResult(completeName, headers, newDirName):
    with io.open(completeName + ".json", "r", encoding="utf-8") as readFile:
        data = json.load(readFile)
        for id in data["markers"]:    
            html = id["html"]
            (alertid, queryParamTo, queryParamFr, pid) = getQueryParam(html)
            url = getEachUrl(alertid, queryParamTo, queryParamFr, pid)
            response = getHealthMapResponse(url, headers)
            fname = os.path.join(newDirName, alertid)           
            request = urllib2.Request(url, headers = headers)
            response = urllib2.urlopen(request)
            with io.open(fname+".html", "w", encoding="utf-8") as writeFile:
                writeFile.write(response.read().decode("utf-8"))
        
def getQueryParam(html):
    res = html.find("javascript:b(")
    start = res + len("javascript:b(")
    end = html.find(")", start)
    result = html[start:end]
    elements = result.split(',')
    alertid = elements[0]
    queryParamTo = elements[1].strip('\'')
    queryParamFr = elements[2].strip('\'')
    pid = elements[3]
    return alertid, queryParamTo, queryParamFr, pid
    
def directoryCheck(dirName):
    if not os.path.exists(dirName):
        os.makedirs(dirName)
    
if __name__ == '__main__':
    #headers
    headers = {}
    headers['User-Agent'] = "Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.27 Safari/537.17"
    #check correctness of arguments and get arguments
    (dirName, fileName, days) = argumentCheck()
    #get dates and condition
    (dateCounter, condition, interval) =  getDatesAndCondition()   
    #get start date and end date
    (endDate, startDate) = getStartAndEndDate(dateCounter, interval)
    while endDate != condition:
        #Set new directory name to be the date
        newDirName = dirName+"\\"+str(dateCounter)
        #check directory exists or not
        directoryCheck(newDirName)
        #get complete name    
        completeName = os.path.join(newDirName, fileName)
        #store JSON result
        storeJsonResult(completeName, startDate, endDate, headers)
         #decrement date counter   
        dateCounter -= interval
        #edit start date and end date
        (endDate, startDate) = getStartAndEndDate(dateCounter, interval)
        #store HTML result
        storeHtmlResult(completeName, headers, newDirName)                 
        