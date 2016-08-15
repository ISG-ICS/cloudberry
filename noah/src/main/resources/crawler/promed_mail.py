import sys
import urllib2
import urllib
import datetime
import zlib
import re
import os

def getPages(query, headers) :
    url = "http://www.promedmail.org/ajax/runSearch.php?"
    request = urllib2.Request(url+query, headers = headers)
    response = urllib2.urlopen(request)
    return response

def getQueryForPage(start_date, end_date, pagenum, submit):
    query = {'kwby1':'summary', 'search':'zika', 'show_us':'1', 'feed_id':'1'}
    query['date1'] = start_date
    query['date2'] = end_date
    if (pagenum > 0):
        query['pagenum'] = pagenum
        query['submit'] = submit
    
    return query

def request(filename, query, headers):
    with open(filename, "wb") as writeFile:
        response = getPages(urllib.urlencode(query), headers)
        writeFile.write(zlib.decompress(response.read(), 16+zlib.MAX_WBITS))

def getIds(fileName):
    with open(fileName, 'rb') as f:
        file_content = f.read()
    nextExists = file_content.find('value=\\"next\\"') >= 0
      
    listId = re.findall(r"\D(\d{8})"+"."+"(\d{4,8})\D", file_content)
    return listId, nextExists

def getEachMail(archNo, headers, directoryName):
    
    url = "http://promedmail.org/ajax/getPost.php?alert_id="
    
            
    for id in archNo:
        fname = id +'.html'
        completeName = os.path.join(directoryName, fname)
        request = urllib2.Request(url+id, headers = headers)
        response = urllib2.urlopen(request)

        if not os.path.isfile(completeName):
            with open(completeName, 'wb') as f:
                f.write(zlib.decompress(response.read(), 16+zlib.MAX_WBITS))
        else:
            pass         

if __name__ == '__main__':
    pagenum = 0
    submit = "next"
    
    if len(sys.argv) != 2:
        sys.exit("Enter the directory name and number of days in arguments: \n Example: python promed_mail.py PromedResult 365")
    
    dirName = sys.argv[1]
               
    interval = sys.argv[2]
    
    archNo = set()
    headers = {}
    headers['User-Agent'] = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36"
    headers['X-Requested-With'] = 'XMLHttpRequest'
    headers['Referer'] = "http://promedmail.org/"
    headers['Accept'] = "application/json, text/javascript, */*"
    headers['Accept-Encoding'] = "gzip, deflate, sdch"
    headers['Accept-Language'] = "en-US,en;q=0.8"
    headers['Connection'] = "keep-alive"
    headers['Content-Type'] = "application/x-www-form-urlencoded"
    headers['Host'] = 'promedmail.org'
    
    end_date = datetime.date.today()
    start_date = end_date - datetime.timedelta(int(interval))
    end_date = end_date.strftime("%m/%d/%Y")
    start_date = start_date.strftime("%m/%d/%Y")
    filename = "promedsearchresult.html"
       
    if not os.path.exists(dirName):
        os.makedirs(dirName)
        
    completeName = os.path.join(dirName, filename)
    
    nextExists = False
    while( pagenum == 0 or nextExists):
        request(completeName, getQueryForPage(start_date, end_date, pagenum, submit), headers)
        (listId, nextExists) = getIds(completeName)
        for id in listId:
            concatId = id[1]
            if concatId not in archNo:
                archNo.add(concatId)              
        getEachMail(archNo, headers, dirName)
        pagenum += 1
        
    
		