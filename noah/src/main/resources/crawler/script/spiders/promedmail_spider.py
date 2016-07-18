#The spider

#name: identifies the Spider. It must be unique, 
#that is, you cannot set the same name for different Spiders.

#start_urls: a list of URLs where the Spider will begin to crawl 
#from. The first pages downloaded will be those listed here. 
#The subsequent URLs will be generated successively from data 
#contained in the start URLs.

#parse(): a method of the spider, which will be called with the downloaded

#Response: object of each start URL. The response is passed to the 
#method as the first and only argument.
#This method is responsible for parsing the response data and 
#extracting scraped data (as scraped items) and more URLs to follow.

#The parse() method is in charge of processing the response and 
#returning scraped data (as Item objects) and more URLs to follow 
#(as Request objects).

import scrapy
import sets
import re
from scrapy_splash import SplashRequest
from scrapy.selector import Selector

class PromedmailSpider(scrapy.Spider):
	name = 'promedSpider'
	allowed_domains = ['promedmail.org']
	
	#add the first archive number to the set.
	#set used to avoid duplicates
	
	
	#This is the method called by Scrapy when the spider is opened for scraping when no particular URLs are specified.
	#If particular URLs are specified, the make_requests_from_url() is used instead to create the Requests.
	#This method is also called only once from Scrapy, so it is safe to implement it as a generator.
	
	def start_requests(self):
		self.archNo = raw_input("Enter Archive number of starting url: ")
		self.s = set([str(self.archNo)])
		url = "http://www.promedmail.org/post/"+str(self.archNo)
		yield SplashRequest(url, self.parse, args={'wait': 0.5})
			
	def parse(self, response):
		html=response.body
		
		#save the response in an html file
		filename = response.url.split("/")[-1] + '.html'
		with open(filename, 'wb') as f:
			f.write(html)
			
		#regex to match the archive numbers
		listId = re.findall(r"\D(\d{8})"+"."+"(\d{5,8})\D", html)
		print listId
		for id in listId:
			concatId = id[0]+"."+id[1]
			url = "http://www.promedmail.org/post/" + concatId
			if concatId not in self.s:
				self.s.add(concatId)
				yield SplashRequest(url, self.parse, args={'wait': 0.5})
				
#Scrapy creates scrapy.Request objects for each URL in the start_urls 
#attribute of the Spider, and assigns them the parse method of the 
#spider as their callback function.

#These Requests are scheduled, then executed, and scrapy.http.Response 
#objects are returned and then fed back to the spider, through the parse() method.