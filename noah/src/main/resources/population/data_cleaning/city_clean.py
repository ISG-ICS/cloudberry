'''
Python scripts to reformat the city_population.json to make it consistent with the existing 
neo/public/data/city.json
Author: Shengjie Xu
Contact: shengjix12@gmail.com
'''

import json

'''
open json files and convert it to a Python dictionary
'''
city_file = open('data/city.json', 'r')
city_pop_file = open('population/city_population.json', 'r')
cleaned_city_pop_file = open('city_population_cleaned_final.json', 'w')
city_json = json.load(city_file)
city_pop_json = json.load(city_pop_file)

'''
clean population to make it as a int type
'''
for city in city_pop_json:
    city['city_population'] = int(city['city_population'].replace(',', ''))


'''
construct cityName => countyName => stateName => {population, duplicate} dictionary
'''
pop_dict = {}
for c in city_pop_json:
    name = c['city_name']
    population = c['city_population']
    countyName = c['county_name']
    stateName = c['state_name']

    if "/" in countyName:
        countyNameList = countyName.split("/")
        for n in countyNameList:
            countyName = n.strip()
            county_level_dict = pop_dict.get(name)
            if county_level_dict is None:
                pop_dict[name] = {countyName: {stateName: {'population': [population], 'duplicate': 0}}}
            else:
                state_level_dict = county_level_dict.get(countyName)
                if state_level_dict is None:
                    county_level_dict[countyName] = {stateName: {'population': [population], 'duplicate': 0}}
                else:
                    data_level_dict = state_level_dict.get(stateName)
                    if data_level_dict is None:
                        state_level_dict[stateName] = {'population': [population],
                                                       'duplicate': 0}
                    else:
                        data_level_dict['population'].append(population)
                        data_level_dict['duplicate'] += 1
                        print 'name: ' + name + ", county: " + countyName + ", state: " + stateName + \
                              ", dup: " + str(data_level_dict['duplicate']) + "[]: " + str(
                            data_level_dict['population'])
    else:
        county_level_dict = pop_dict.get(name)
        if county_level_dict is None:
            pop_dict[name] = {countyName: {stateName: {'population': [population], 'duplicate': 0}}}
        else:
            state_level_dict = county_level_dict.get(countyName)
            if state_level_dict is None:
                county_level_dict[countyName] = {stateName: {'population': [population], 'duplicate': 0}}
            else:
                data_level_dict = state_level_dict.get(stateName)
                if data_level_dict is None:
                    state_level_dict[stateName] = {'population': [population],
                                                   'duplicate': 0}
                else:
                    data_level_dict['population'].append(population)
                    data_level_dict['duplicate'] += 1
                    print 'name: ' + name + ", county: " + countyName + ", state: " + stateName + \
                          ", dup: "+str(data_level_dict['duplicate'])+ "[]: " + str(data_level_dict['population'])


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
county_list = city_json['features']
for c in county_list:
    data_dict = {}
    name = c['properties']['name']
    cityID = c['properties']['cityID']
    countyName = c['properties']['countyName']
    countyID = c['properties']['countyID']
    stateName = c['properties']['stateName']
    stateID = c['properties']['stateID']

    data_dict['name'] = name
    data_dict['cityID'] = cityID
    data_dict['countyName'] = countyName
    data_dict['countyID'] = countyID
    data_dict['stateName'] = stateName
    data_dict['stateID'] = stateID

    county_level_dict = pop_dict.get(name)
    if county_level_dict is None:
        data_dict['population'] = None
    else:
        state_level_dict = county_level_dict.get(countyName)
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
json.dump(result_list, cleaned_city_pop_file)
