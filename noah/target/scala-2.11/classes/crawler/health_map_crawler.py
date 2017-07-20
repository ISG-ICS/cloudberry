import urllib2
import urllib
import datetime
import io
import os
import json
import sys

#Check whether the arguments entered are valid
def argumentCheck():
    if len(sys.argv) != 4:
        sys.exit("Invalid number of Arguments... \n Enter the directory name, filename where json data needs to be stored and number of days in arguments: \n Example: python health_map_crawler.py HealthMapResult 365")  
    dirName = sys.argv[1]
    fileName = sys.argv[2]
    days = sys.argv[3]
    directoryCheck(dirName)
    return dirName, fileName, days

#Get the crawler stop condition.
def getDatesAndCondition():
    dateCounter = datetime.date.today()
    condition = (dateCounter - datetime.timedelta(int(days))).strftime("%m/%d/%Y")
    interval = datetime.timedelta(1)
    return dateCounter, condition, interval

#Calculate start and end date from current date and interval.
def getStartAndEndDate(dateCounter, interval):
    endDate = dateCounter.strftime("%m/%d/%Y")
    startDate = (dateCounter - interval).strftime("%m/%d/%Y")
    return endDate, startDate 
     
#get health map response object.
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
    
#Get the individual url to be crawled. Input are the query parameters and the output is the url.
def getEachUrl(alertid, queryParamTo, queryParamFr, pid):
    query = {}
    query['trto'] = queryParamTo
    query['trfr'] = queryParamFr
    url = "http://www.healthmap.org/ai.php?"+alertid+"&"+urllib.urlencode(query)+"&"+"pid"+pid
    return url

#Store the JSON result for each day   
def storeJsonResult(completeName, startDate, endDate, headers):
    with io.open(completeName + ".json", "w", encoding="utf-8") as writeFile:
            # get response from healthmap
            url = "http://www.healthmap.org/getAlerts.php?"
            response = getHealthMapResponse(url + urllib.urlencode(generateHealthMapQuery(startDate, endDate)), headers)
            #write to file
            writeFile.write(response.read().decode("utf-8"))
            
#Strore the HTML result for each alert
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

#Get teh query parameters for each alert. The input is html element of the json response and the output are the required parameters.        
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

#Create directory if it does not exist.    
def directoryCheck(dirName):
    if not os.path.exists(dirName):
        os.makedirs(dirName)
    
if __name__ == '__main__':
    headers = {}
    headers['User-Agent'] = "Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.27 Safari/537.17"
    (dirName, fileName, days) = argumentCheck()
    (dateCounter, condition, interval) =  getDatesAndCondition()  
    (endDate, startDate) = getStartAndEndDate(dateCounter, interval)
    while endDate != condition:
        newDirName = dirName+"/"+str(dateCounter)
        directoryCheck(newDirName)    
        completeName = os.path.join(newDirName, fileName)
        storeJsonResult(completeName, startDate, endDate, headers)   
        dateCounter -= interval
        (endDate, startDate) = getStartAndEndDate(dateCounter, interval)
        storeHtmlResult(completeName, headers, newDirName)                 
        