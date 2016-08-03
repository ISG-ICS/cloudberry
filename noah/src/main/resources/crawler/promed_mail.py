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

def getQueryForPage(start_date, end_date):
    query = {'kwby1':'summary', 'search':'zika', 'show_us':'1', 'feed_id':'1'}
    query['date1'] = start_date
    query['date2'] = end_date
    return query

def requestPage(filename, query, headers):
    
    with open(filename, "wb") as writeFile:
        response = getPages(urllib.urlencode(query), headers)
        writeFile.write(zlib.decompress(response.read(), 16+zlib.MAX_WBITS))

def getIds(fileName):
    with open(fileName, 'rb') as f:
        file_content = f.read()
    listId = re.findall(r"\D(\d{8})"+"."+"(\d{4,8})\D", file_content)
    return listId

def getEachMail(archNo, headers, directoryName):
    
    url = "http://promedmail.org/ajax/getPost.php?alert_id="
    directoryName = 'PromedResult'
    
    if not os.path.exists(directoryName):
        os.makedirs(directoryName)
        completePath = os.path.abspath(directoryName)
    else:
        completePath = os.path.abspath(directoryName)
        
    for id in archNo:
        fname = id +'.html'
        completeName = os.path.join(completePath, fname)
        request = urllib2.Request(url+id, headers = headers)
        response = urllib2.urlopen(request)

        if not os.path.isfile(completeName):
            with open(completeName, 'wb') as f:
                f.write(zlib.decompress(response.read(), 16+zlib.MAX_WBITS))
        else:
            pass         

if __name__ == '__main__':
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
    start_date = end_date - datetime.timedelta(7)
    end_date = end_date.strftime("%m/%d/%Y")
    start_date = start_date.strftime("%m/%d/%Y")
    filename = "promedsearchresult.html"
    dirName = "PromedResult"
       
    if not os.path.exists(dirName):
        os.makedirs(dirName)
        completePath = os.path.abspath(dirName)
    else:
        completePath = os.path.abspath(dirName)
        
    completeName = os.path.join(completePath, filename)
    requestPage(completeName, getQueryForPage(start_date, end_date), headers)

    listId = getIds(completeName)
    
    for id in listId:
        concatId = id[1]
        if concatId not in archNo:
            archNo.add(concatId)
            
    getEachMail(archNo, headers, dirName)
    
		