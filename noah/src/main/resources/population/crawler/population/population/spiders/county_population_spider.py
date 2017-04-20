'''
A crawler to extract the city population in a given state by census years.

Base url:
https://www.citypopulation.de/php/usa-census-***.php (swap *** with the state name)
Example URL:
https://www.citypopulation.de/php/usa-census-alabama.php

Output ==> json:
1. county name
2. county population
3. county's administration (state)

Author: Shengjie Xu
Contact: shengjix12@gmail.com
'''

import scrapy


class CountyPopulationSpider(scrapy.Spider):
    '''
    A customized crawler inherented from the scrapy.Spider base class.
    To run this crawler, install scrapy https://scrapy.org/, and run:
        scrapy crawl county_population -o county_population.json
    '''
    name = "county_population"
    def start_requests(self):
        state_list = ['alabama', 'alaska', 'arizona', 'arkansas', 'california',
                      'colorado', 'connecticut', 'delaware', 'districtofcolumbia',
                      'florida', 'georgia', 'hawaii', 'idaho', 'illinois', 'indiana',
                      'iowa', 'kansas', 'kentucky', 'louisiana', 'maine', 'maryland',
                      'massachusetts', 'michigan', 'minnesota', 'mississippi', 'missouri',
                      'montana', 'nebraska', 'nevada', 'newhampshire', 'newjersey', 'newmexico',
                      'newyork', 'northcarolina', 'northdakota', 'ohio', 'oklahoma', 'oregon',
                      'pennsylvania', 'rhodeisland', 'southcarolina', 'southdakota',
                      'tennessee', 'texas', 'utah', 'vermont', 'virginia', 'washington',
                      'westvirginia', 'wisconsin', 'wyoming']
        base_url = 'https://www.citypopulation.de/php/usa-census-'
        url_list = []
        for state in state_list:
            url_list.append(base_url + state + '.php')
        for url in url_list:
            yield scrapy.Request(url, self.parse)

    def parse(self, response):
        state_name = response.css('header.citypage h1 span.smalltext::text').extract_first()
        for county in response.css('article#table section#adminareas table#tl tbody tr'):
            yield {
                'county_name': county.css('td.rname span a::text').extract_first(),
                'county_population': county.css('td.rpop.prio1::text').extract_first(),
                'state_name': state_name
            }
