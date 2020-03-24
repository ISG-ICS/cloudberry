import scrapy
from selenium import webdriver
from selenium.common.exceptions import TimeoutException, NoSuchElementException
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.webdriver.common.action_chains import ActionChains
import json
import csv
import datetime
from os import path
import os

def read_ids_from_json(target='state'):
    with open(os.path.join('raw_id_jsons', f'{target}.json'), 'rb') as file:
        all_states = json.load(file)
    ids = {}
    for feature in all_states['features']:
        id_ = int(feature['properties'][f'{target}ID'])
        if target == 'state':
            name = feature['properties']['name'].lower()
        else:
            name = feature['properties']['name'].lower() + ', ' + feature['properties']['stateName'].lower()
        ids[name] = id_
    return ids

class CoronavirusSpider(scrapy.Spider):
    name = "coronavirus"

    def __init__(self):
        self.driver = webdriver.Firefox()
        self.start_urls = ['https://coronavirus.1point3acres.com/']

    def start_requests(self):
        headers= {'User-Agent': 'Mozilla/5.0 (X11; Linux i686; rv:64.0) Gecko/20100101 Firefox/64.0'}
        for url in self.start_urls:
            yield scrapy.Request(url=url, headers=headers, callback=self.parse)
    
    def parse(self, response):
        self.driver.get(response.url)
        state_ids = read_ids_from_json('state')
        county_ids = read_ids_from_json('county')
        
        data = {}
        time_series = []
        
        if path.exists('data.txt'):
            with open('data.txt', 'r') as f:
                data = json.load(f)
        if path.exists('time_series.txt'):
            with open('time_series.txt', 'r') as f:
                time_series = json.load(f)
        
        now = datetime.date.today()
        dt_string = now.strftime('%Y-%m-%dT%H:%M:%SZ')
        if dt_string not in time_series:
            time_series.append(dt_string)
        
        i = 1
        while True:
            try:
                latest = self.driver.find_element_by_xpath('//*[@id="map"]/div[2]/div[1]/div[4]/div[{}]'.format(i))
                self.driver.execute_script("arguments[0].scrollIntoView();", latest)
                element = WebDriverWait(self.driver, 10).until(EC.element_to_be_clickable((By.XPATH, '//*[@id="map"]/div[2]/div[1]/div[4]/div[{}]/div[1]'.format(i+1))))
                element.click()
                WebDriverWait(self.driver, 10).until(EC.visibility_of_element_located((By.XPATH, '//*[@id="map"]/div[2]/div[1]/div[4]/div[{}]/div[2]'.format(i+1))))
                state = self.driver.find_element_by_xpath('//*[@id="map"]/div[2]/div[1]/div[4]/div[{}]/div[1]/span[1]'.format(i+1)).text
                if state not in data.keys():
                    data[state] = {}
                state_confirmed = self.driver.find_element_by_xpath('//*[@id="map"]/div[2]/div[1]/div[4]/div[{}]/div[1]/span[2]'.format(i+1)).text
                state_deaths = self.driver.find_element_by_xpath('//*[@id="map"]/div[2]/div[1]/div[4]/div[{}]/div[1]/span[3]'.format(i+1)).text
                state_fr = self.driver.find_element_by_xpath('//*[@id="map"]/div[2]/div[1]/div[4]/div[{}]/div[1]/span[4]'.format(i+1)).text
                state_confirmed_list = state_confirmed.split('\n')
                state_deaths_list = state_deaths.split('\n')
                
                if 'new_confirmed' not in data[state].keys():
                    data[state]['new_confirmed'] = {}
                    for t in time_series[:-1]:
                        data[state]['new_confirmed'][t] = 0
                if 'confirmed' not in data[state].keys():
                    data[state]['confirmed'] = {}
                    for t in time_series[:-1]:
                        data[state]['confirmed'][t] = 0
                if 'new_deaths' not in data[state].keys():
                    data[state]['new_deaths'] = {}
                    for t in time_series[:-1]:
                        data[state]['new_deaths'][t] = 0
                if 'deaths' not in data[state].keys():
                    data[state]['deaths'] = {}
                    for t in time_series[:-1]:
                        data[state]['deaths'][t] = 0
                if 'fatality_rate' not in data[state].keys():
                    data[state]['fatality_rate'] = {}
                    for t in time_series[:-1]:
                        data[state]['fatality_rate'][t] = 0.0
                if len(state_confirmed_list) == 1:
                    data[state]['new_confirmed'][dt_string] = 0
                    data[state]['confirmed'][dt_string] = int(state_confirmed_list[0])
                else:
                    data[state]['new_confirmed'][dt_string] = int(state_confirmed_list[0])
                    data[state]['confirmed'][dt_string] = int(state_confirmed_list[1])
                if len(state_deaths_list) == 1:
                    data[state]['new_deaths'][dt_string] = 0
                    data[state]['deaths'][dt_string] = int(state_deaths_list[0])
                else:
                    data[state]['new_deaths'][dt_string] = int(state_deaths_list[0])
                    data[state]['deaths'][dt_string] = int(state_deaths_list[1])
                data[state]['fatality_rate'][dt_string] = round(float(state_fr.strip('%')) / 100, 3)
                
                if 'county' not in data[state].keys():
                    data[state]['county'] = {}
                
                j = 1
                while True:
                    try:
                        county = self.driver.find_element_by_xpath('//*[@id="map"]/div[2]/div[1]/div[4]/div[{}]/div[2]/div[{}]/span[1]'.format(i+1,j)).text
                        confirmed = self.driver.find_element_by_xpath('//*[@id="map"]/div[2]/div[1]/div[4]/div[{}]/div[2]/div[{}]/span[2]'.format(i+1,j)).text
                        deaths = self.driver.find_element_by_xpath('//*[@id="map"]/div[2]/div[1]/div[4]/div[{}]/div[2]/div[{}]/span[3]'.format(i+1,j)).text
                        fr = self.driver.find_element_by_xpath('//*[@id="map"]/div[2]/div[1]/div[4]/div[{}]/div[2]/div[{}]/span[4]'.format(i+1,j)).text
                        confirmed_list = confirmed.split('\n')
                        deaths_list = deaths.split('\n')
                        
                        if county not in data[state]['county'].keys():
                            data[state]['county'][county] = {}
                        if 'new_confirmed' not in data[state]['county'][county].keys():
                            data[state]['county'][county]['new_confirmed'] = {}
                            for t in time_series[:-1]:
                                data[state]['county'][county]['new_confirmed'][t] = 0
                        if 'confirmed' not in data[state]['county'][county].keys():
                            data[state]['county'][county]['confirmed'] = {}
                            for t in time_series[:-1]:
                                data[state]['county'][county]['confirmed'][t] = 0
                        if 'new_deaths' not in data[state]['county'][county].keys():
                            data[state]['county'][county]['new_deaths'] = {}
                            for t in time_series[:-1]:
                                data[state]['county'][county]['new_deaths'][t] = 0
                        if 'deaths' not in data[state]['county'][county].keys():
                            data[state]['county'][county]['deaths'] = {}
                            for t in time_series[:-1]:
                                data[state]['county'][county]['deaths'][t] = 0
                        if 'fatality_rate' not in data[state]['county'][county].keys():
                            data[state]['county'][county]['fatality_rate'] = {}
                            for t in time_series[:-1]:
                                data[state]['county'][county]['fatality_rate'][t] = 0.0
                        if len(confirmed_list) == 1:
                            data[state]['county'][county]['new_confirmed'][dt_string] = 0
                            data[state]['county'][county]['confirmed'][dt_string] = int(confirmed_list[0])
                        else:
                            data[state]['county'][county]['new_confirmed'][dt_string] = int(confirmed_list[0])
                            data[state]['county'][county]['confirmed'][dt_string] = int(confirmed_list[1])
                        if len(deaths_list) == 1:
                            data[state]['county'][county]['new_deaths'][dt_string] = 0
                            data[state]['county'][county]['deaths'][dt_string] = int(deaths_list[0])
                        else:
                            data[state]['county'][county]['new_deaths'][dt_string] = int(deaths_list[0])
                            data[state]['county'][county]['deaths'][dt_string] = int(deaths_list[1])
                        data[state]['county'][county]['fatality_rate'][dt_string] = round(float(fr.strip('%')) / 100, 3)
                        
                    except NoSuchElementException:
                        break
                    j += 1
                    
                element = WebDriverWait(self.driver, 10).until(EC.element_to_be_clickable((By.XPATH, '//*[@id="map"]/div[2]/div[1]/div[4]/div[{}]/div[1]'.format(i+1))))
                element.click()
                
            except (NoSuchElementException, TimeoutException):
                break
            i += 1
        
        self.driver.close()
        
        with open('data.txt', 'w') as f:
            json.dump(data, f)
        
        with open('time_series.txt', 'w') as f:
            json.dump(time_series, f)

        with open('state_cases.csv', 'w', newline='') as csvfile:
            fieldnames = ['state_id', 'last_update', 'confirmed', 'new_confirmed', 'deaths', 'new_deaths', 'fatality_rate']
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            for state in data.keys():
                state_string = state.lower()
                id_ = None
                if state_string in state_ids.keys():
                    id_ = state_ids[state_string]
                d = {'state_id': id_}
                for t in time_series:
                    d['last_update'] = t
                    d['confirmed'] = data[state]['confirmed'][t]
                    d['new_confirmed'] = data[state]['new_confirmed'][t]
                    d['deaths'] = data[state]['deaths'][t]
                    d['new_deaths'] = data[state]['new_deaths'][t]
                    d['fatality_rate'] = data[state]['fatality_rate'][t]
                    writer.writerow(d)
        
        with open('county_cases.csv', 'w', newline='') as csvfile:
            fieldnames = ['county_id', 'last_update', 'confirmed', 'new_confirmed', 'deaths', 'new_deaths', 'fatality_rate']
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            for state in data.keys():
                for county in data[state]['county'].keys():
                    county_string = county.lower() + ', ' + state.lower()
                    id_ = None
                    if county_string in county_ids.keys():
                        id_ = county_ids[county_string]
                    d = {'county_id': id_}
                    for t in time_series:
                        d['last_update'] = t
                        d['confirmed'] = data[state]['county'][county]['confirmed'][t]
                        d['new_confirmed'] = data[state]['county'][county]['new_confirmed'][t]
                        d['deaths'] = data[state]['county'][county]['deaths'][t]
                        d['new_deaths'] = data[state]['county'][county]['new_deaths'][t]
                        d['fatality_rate'] = data[state]['county'][county]['fatality_rate'][t]
                        writer.writerow(d)
