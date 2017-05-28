'''
A crawler to extract the population of the U.S. states according to census
results and latest official estimates.

URL:
https://www.citypopulation.de/USA-Cities.html

Output ==> json:
1. state name
2. state population
3. state abbreviation

Author: Shengjie Xu
Contact: shengjix12@gmail.com
'''

import scrapy


class StatePopulationSpider(scrapy.Spider):
    '''
    A customized crawler inherented from the scrapy.Spider base class.
    To run this crawler, install scrapy https://scrapy.org/, and run:
        scrapy crawl state_population -o state_population.json
    '''
    name = "state_population"
    start_urls = [
        'https://www.citypopulation.de/USA-Cities.html'
    ]

    def parse(self, response):
        for state in response.css('section#adminareas table#tl tbody tr'):
            yield {
                'state_name': state.css('td.rname a span::text').extract_first(),
                'state_abbr': state.css('td.rabbr::text').extract_first(),
                'state_population': state.css('td.rpop.prio1::text').extract_first(),
            }
