'''
Python scripts to clean nulls and -1 in county_population_cleaned_final.json
Author: Shengjie Xu
Contact: shengjix12@gmail.com
'''

import json

'''
open json files and convert it to a Python dictionary
'''

state_pop_file = open('state_population_cleaned_final.json', 'r')
county_pop_file = open('county_population_cleaned_final.json', 'r')
target = open('allCountyPopulation.json', 'w')
state_pop_json= json.load(state_pop_file)
county_pop_json = json.load(county_pop_file)

'''
count number of counties under each state
stateID => count
'''
count_dict = {}
for c in county_pop_json:
    stateID = c['stateID']
    if stateID not in count_dict:
        count_dict[stateID] = {'count': 1}
    else:
        count_dict[stateID]['count'] += 1


'''
calculate average county population under each state
'''
avg_dict = {}
for s in state_pop_json:
    avg_dict[s['stateID']] = s['population'] / count_dict[s['stateID']]['count']

count = 0
for c in county_pop_json:
    if c['population'] is None or c['population'] == -1:
        c['population'] = avg_dict[c['stateID']]
        count += 1

print "null cleaned: " + str(count)

'''
output as a json file
'''
json.dump(county_pop_json, target)
