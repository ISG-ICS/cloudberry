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
import ConfigParser
import os
from scrapy_splash import SplashRequest

class PromedmailSpider(scrapy.Spider):

	name = 'promedSpider'
	allowed_domains = []
	domains = []
	s = set([])
	part_url = ""
	archNo = ""
	directoryName = ""
	completePath = ""	
	#This is the method called by Scrapy when the spider is opened for scraping when no particular URLs are specified.
	#If particular URLs are specified, the make_requests_from_url() is used instead to create the Requests.
	#This method is also called only once from Scrapy, so it is safe to implement it as a generator.
	
	def config_param(self):
		config = ConfigParser.RawConfigParser()
		config.readfp(open(r'promedconf.conf'))	
		
		if(config.has_section('domains')):
			#Add the domains to the allowed_domains
			if(config.has_option('domains', 'allowed_domains')):
				self.domains = config.get('domains', 'allowed_domains')
			
			#add the first archive number to the set.
			#set used to avoid duplicates
			if(config.has_option('domains', 'start_archive_number')):
				self.archNo = config.get('domains', 'start_archive_number')
				
			#Get the common part of the url 	
			if(config.has_option('domains', 'part_url')):
				self.part_url = config.get('domains', 'part_url')
				
		if(config.has_section('result')):
			if(config.has_option('result', 'directory')):
				self.directoryName = config.get('result', 'directory')
				
				
	def start_requests(self):
	
		self.config_param()
		self.s.add(self.archNo)
		if not os.path.exists(self.directoryName):
			os.makedirs(self.directoryName)
			self.completePath = os.path.abspath(self.directoryName)	
			url =  self.part_url + self.archNo
			yield SplashRequest(url, self.parse, args={'wait': 0.7})
		else:
			self.completePath = os.path.abspath(self.directoryName)	
			url =  self.part_url + self.archNo
			yield SplashRequest(url, self.parse, args={'wait': 0.7})
			
	def parse(self, response):
		html=response.body
		
		#save the response in an html file
		filename = response.url.split("/")[-1] + '.html'
		completeName = os.path.join(self.completePath, filename)
		if not os.path.isfile(completeName):
			with open(completeName, 'wb') as f:
				f.write(html)
		else:
			pass
			
		#regex to match the archive numbers
		listId = re.findall(r"\D(\d{8})"+"."+"(\d{5,8})\D", html)
		for id in listId:
			concatId = id[0]+"."+id[1]
			if concatId not in self.s:
				url = self.part_url + concatId
				self.s.add(concatId)
				yield SplashRequest(url, self.parse, args={'wait': 0.7})
				

#Scrapy creates scrapy.Request objects for each URL in the start_urls 
#attribute of the Spider, and assigns them the parse method of the 
#spider as their callback function.

#These Requests are scheduled, then executed, and scrapy.http.Response 
#objects are returned and then fed back to the spider, through the parse() method.