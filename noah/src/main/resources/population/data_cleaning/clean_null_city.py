'''
Python scripts to clean nulls and -1 in city_population_cleaned_final.json
Author: Shengjie Xu
Contact: shengjix12@gmail.com
'''

import json

'''
open json files and convert it to a Python dictionary
'''

county_pop_file = open('allCountyPopulation.json', 'r')
city_pop_file = open('city_population_cleaned_final.json', 'r')
target = open('allCityPopulation.json', 'w')
county_pop_json = json.load(county_pop_file)
city_pop_json = json.load(city_pop_file)

'''
count number of cities under each county
countyID => count
'''
count_dict = {}
for c in city_pop_json:
    countyID = c['countyID']
    if countyID not in count_dict:
        count_dict[countyID] = {'count': 1}
    else:
        count_dict[countyID]['count'] += 1

'''
calculate average city population under each county
'''
avg_dict = {}
for c in county_pop_json:
    if c['countyID'] in count_dict:
        avg_dict[c['countyID']] = c['population'] / count_dict[c['countyID']]['count']


count = 0
for c in city_pop_json:
    if c['population'] is None or c['population'] == -1:
        if c['countyID'] not in avg_dict:
            c['population'] = 100
        else:
            c['population'] = avg_dict[c['countyID']]
        count += 1

print "null cleaned: " + str(count)

'''
output as a json file
'''
json.dump(city_pop_json, target)