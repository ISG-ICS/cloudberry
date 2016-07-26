import scrapy
import ConfigParser
import time
import os
from scrapy.selector import Selector
from scrapy_splash import SplashRequest

class PahoSpider(scrapy.Spider):
	name = 'pahoSpider'
	allowed_domains = []
	domains = []
	directoryName = ""
	completePath = ""
	url1 = ""
	
	def config_param(self):
		config = ConfigParser.RawConfigParser()
		config.readfp(open(r'pahoconf.conf'))
		
		if(config.has_section('domains')):
			#Add the domains to the allowed_domains
			if(config.has_option('domains', 'allowed_domains')):
				self.domains = config.get('domains', 'allowed_domains')
				print self.domains
				
			if(config.has_option('domains', 'url1')):
				self.url1 = config.get('domains', 'url1')
				print self.url1
				
		if(config.has_section('result')):
			if(config.has_option('result', 'directoryName')):
				self.directoryName = config.get('result', 'directoryName')
				print self.directoryName
				
	def start_requests(self):
		self.config_param()
		if not os.path.exists(self.directoryName):
			os.makedirs(self.directoryName)
			self.completePath = os.path.abspath(self.directoryName)	
			url = self.url1
			yield SplashRequest(url, self.parse, args={'wait': 0.7})
		else:
			self.completePath = os.path.abspath(self.directoryName)	
			url = self.url1
			yield SplashRequest(url, self.parse, args={'wait': 0.7})

	def parse(self, response):
		html=response.body
		timestr = time.strftime("%Y%m%d-%H%M%S")
		#save the response in an html file
		filename = 'TimelinePaho-' + timestr +'.html'
		completeName = os.path.join(self.completePath, filename)
		if not os.path.isfile(completeName):
			with open(completeName, 'wb') as f:
				f.write(html)
		else:
			pass