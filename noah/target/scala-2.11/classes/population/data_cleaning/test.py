import json

file = open('allCityPopulation.json', 'r')
ref_file = open('data/city.json', 'r')
my_json = json.load(file)
ref_json = json.load(ref_file)

my_ids = set()

count = 0
for c in my_json:
    my_ids.add(c['cityID'])
    count += 1

print "my count=" + str(count)

count = 0
missed = 0
for c in ref_json['features']:
    prop = c['properties']
    if prop['cityID'] not in my_ids:
        print prop
        missed += 1
    count += 1

print "ref count=" + str(count)
print "missed=" + str(missed)