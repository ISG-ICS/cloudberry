'''
Python scripts to reformat the county_population.json to make it consistent with the existing
neo/public/data/county.json
Author: Shengjie Xu
Contact: shengjix12@gmail.com
'''

import json

'''
open json files and convert it to a Python dictionary
'''
county_file = open('data/county.json', 'r')
county_pop_file = open('population/county_population.json', 'r')
cleaned_county_pop_file = open('county_population_cleaned_final.json', 'w')
county_json = json.load(county_file)
county_pop_json = json.load(county_pop_file)

'''
clean population to make it as a int type
'''
for county in county_pop_json:
    county['county_population'] = int(county['county_population'].replace(',', ''))


'''
construct countyName => stateName => {population, duplicate} dictionary
'''
pop_dict = {}
for c in county_pop_json:
    name = c['county_name']
    population = c['county_population']
    stateName = c['state_name']

    state_level_dict = pop_dict.get(name)
    if state_level_dict is None:
        pop_dict[name] = {stateName: {'population': [population],
                                      'duplicate': 0}}
    else:
        data_level_dict = state_level_dict.get(stateName)
        if data_level_dict is None:
            state_level_dict[stateName] = {'population': [population],
                                           'duplicate': 0}
        else:
            data_level_dict['population'].append(population)
            data_level_dict['duplicate'] += 1
            print 'name: ' + name + ", state: " + stateName + \
                  ", dup: " + str(data_level_dict['duplicate']) + "[]: " + str(data_level_dict['population'])

'''
construct result json
[
    {
        'name': ..,
        'id': ..,
        ...
        'population': ..
    },
    ...
]
'''
result_list = []
county_list = county_json['features']
for c in county_list:
    data_dict = {}
    name = c['properties']['name']
    countyID = c['properties']['countyID']
    stateName = c['properties']['stateName']
    stateID = c['properties']['stateID']

    data_dict['name'] = name
    data_dict['countyID'] = countyID
    data_dict['stateName'] = stateName
    data_dict['stateID'] = stateID

    state_level_dict = pop_dict.get(name)
    if state_level_dict is None:
        data_dict['population'] = None
    else:
        data_level_dict = state_level_dict.get(stateName)
        if data_level_dict is None:
            data_dict['population'] = None
        else:
            data_dict['population'] = -1 if data_level_dict.get('duplicate') != 0 else data_level_dict.get('population')[0]
    result_list.append(data_dict)


'''
output as a json file
'''
json.dump(result_list, cleaned_county_pop_file)
