'''
A crawler to extract the population of all cities, towns and unincorporated places
in the United States of America with more than 100,000 inhabitants according to census
results and latest official estimates. In other words, major cities in the USA.

URL:
https://www.citypopulation.de/USA-Cities.html

Output ==> json:
1. city name
2. city population
3. city's administration (county)

Author: Shengjie Xu
Contact: shengjix12@gmail.com
'''

import scrapy


class MajorCityPopulationSpider(scrapy.Spider):
    '''
    A customized crawler inherented from the scrapy.Spider base class.
    To run this crawler, install scrapy https://scrapy.org/, and run:
        scrapy crawl major_city_population -o major_city_population.json
    '''
    name = "major_city_population"
    start_urls = [
        'https://www.citypopulation.de/USA-Cities.html'
    ]

    def parse(self, response):
        for city in response.css('section#citysection table#ts tbody tr'):
            yield {
                'city_name': city.css('td.rname a span::text').extract_first(),
                'city_population': city.css('td.rpop.prio1::text').extract_first(),
                'city_admin': city.css('td.radm a::text').extract_first()
            }
