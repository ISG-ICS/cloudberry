import io
import json
import urllib2
import urllib
import datetime
import gzip
import sets
import re

def getPages(query) :
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
	url = "http://www.promedmail.org/ajax/runSearch.php?"
	request = urllib2.Request(url+query, headers = headers)
	response = urllib2.urlopen(request)
	return response

def getQuery1Page1(start_date, end_date):
	query1 = {"kwby1":'summary', 'search':'zika', 'show_us':'1', 'feed_id':'1'}
	query1['date1'] = start_date
	query1['date2'] = end_date
	return query1
	
def getQuery2Page2(start_date, end_date):
	query2 = {"pagenum":"1", "kwby1":"summary", "search":"zika", "show_us":"1", "feed_id":"1", "submit":"next"}
	query2['date1'] = start_date
	query2['date2'] = end_date
	return query2
	
def getQuery3Page3(start_date, end_date):
	query3 = {"pagenum":"2", "kwby1":"summary", "search":"zika", "show_us":"1", "feed_id":"1", "submit":"next"}
	query3['date1'] = start_date
	query3['date2'] = end_date
	return query3
	
def getQuery4Page4(start_date, end_date):
	query4 = {"pagenum":"3", "kwby1":"summary", "search":"zika", "show_us":"1", "feed_id":"1", "submit":"next"}
	query4['date1'] = start_date
	query4['date2'] = end_date
	return query4

def request(filename, query):
	with io.open(filename, "wb") as writeFile:
		response = getPages(urllib.urlencode(query))
		writeFile.write(response.read())

def getArchiveNosAndPages(fileName):
	with gzip.open(fileName, 'rb') as f:
		file_content = f.read()
	listId = re.findall(r"\D(\d{8})"+"."+"(\d{5,8})\D", file_content)
	return listId

if __name__ == '__main__':
	
	archNo = set([])
	# get today's date
	end_date = datetime.date.today()
	start_date = datetime.datetime(2007, 01, 01)
	end_date = end_date.strftime("%m/%d/%Y")
	start_date = start_date.strftime("%m/%d/%Y")
	filename = "./promedsearchresult.html.gzip"
	request(filename, getQuery1Page1(start_date, end_date))
	listId = getArchiveNosAndPages(filename)
	for id in listId:
		concatId = id[0]+"."+id[1]
		if concatId not in archNo:
			archNo.add(concatId)
	print archNo
	print len(archNo)
		