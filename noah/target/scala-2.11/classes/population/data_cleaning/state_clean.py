'''
Python scripts to reformat the state_population.json to make it consistent with the existing
neo/public/data/state.json
Author: Shengjie Xu
Contact: shengjix12@gmail.com
'''

import json

'''
open json files and convert it to a Python dictionary
'''
state_file = open('data/state.json', 'r')
state_pop_file = open('population/state_population.json', 'r')
cleaned_state_pop_file = open('population/output/state_population_cleaned_final.json', 'w')
state_json = json.load(state_file)
state_pop_json = json.load(state_pop_file)

'''
clean population to make it as a int type
'''
for state in state_pop_json:
    state['state_population'] = int(state['state_population'].replace(',', ''))

'''
construct name => population dictionary
'''
pop_dict = {}
for state in state_pop_json:
    pop_dict[state['state_name']] = state['state_population']


'''
construct result json
[
    {
        'name': ..,
        'id': ..,
        'population': ..
    },
    ...
]
'''
result_list = []
state_list = state_json['features']
for state in state_list:
    data_dict = {}
    name = state['properties']['name']
    stateID = state['properties']['stateID']

    data_dict['name'] = name
    data_dict['stateID'] = stateID
    data_dict['population'] = pop_dict.get(name)
    result_list.append(data_dict)

'''
output as a json file
'''
json.dump(result_list, cleaned_state_pop_file)
