# -*- coding: utf-8 -*-
import os
os.environ['NLS_LANG'] = 'ENGLISH_AMERICA.UTF8'
import cx_Oracle
import json
con = cx_Oracle.connect('berry/orcl@localhost:1521/orcl')
var = con.version.split(".")
print(var)
cur = con.cursor()
cityquery = """CREATE TABLE "twitter.dsCityPopulation"(
"name" varchar (255),
"population" number,
"cityID" number,
"countyName" varchar(255),
"countyID" number,
"stateName" varchar (255),
"stateID" number,
PRIMARY KEY("cityID")
)
"""
countyquery = """CREATE TABLE "twitter.dsCountyPopulation" (
"name" varchar (255),
"population" number,
"countyID" number,
"stateName" varchar (255),
"stateID" number,
PRIMARY KEY ("countyID")

)
"""
statequery = """

CREATE TABLE "twitter.dsStatePopulation"(
"name" varchar (255),
"population" number,
"stateID" number,
PRIMARY KEY("stateID")
)
"""
try:
    cur.execute("drop table \"twitter.dsCityPopulation\"")

except cx_Oracle.DatabaseError as e:
    print(e)
    print("\"dsCityPopulation\" table does not exists")
    print("Starting to create table \"dsCityPopulation\"")

print(cityquery)
cur.execute(cityquery)


try:
    cur.execute("drop table \"twitter.dsCountyPopulation\"")
except cx_Oracle.DatabaseError as e:
    print(e)
    print("\"dsCountyPopulation\" table does not exists")
    print("Starting to create table \"dsCountyPopulation\"")

print(countyquery)
cur.execute(countyquery)

try:
    cur.execute("drop table \"twitter.dsStatePopulation\"")
except cx_Oracle.DatabaseError as e:
    print(e)
    print("\"dsStatePopulation\" table does not exists")
    print("Starting to create table \"dsStatePopulation\"")

print(statequery)
cur.execute(statequery)


print("Population Tables Created...")
print("Start Ingesting City Population")



def load_json_multiple(segments):
    chunk = ""
    for segment in segments:
        chunk += segment
        try:
            yield json.loads(chunk)
            chunk = ""
        except ValueError:
            pass

population = open("populationingets.sql","w")

with open("./noah/src/main/resources/population/adm/allCityPopulation.adm") as cityf:
    count = 0
    for cityjson in load_json_multiple(cityf):
        if count % 1000==0:
            print("send record: {}".format(count))
        count += 1
        cityName = "'"+cityjson['name'].replace("'","''")+"'"
        cityPopulation = cityjson['population']
        cityID = cityjson['cityID']
        cityCountyName = "'"+cityjson['countyName'].replace("'","''")+"'"
        cityCountyID = cityjson['countyID']
        cityStateName ="'"+ cityjson['stateName'].replace("'","''")+"'"
        cityStateID = cityjson['stateID']

        insertCityQuery = """
        INSERT INTO "twitter.dsCityPopulation"("name","population","cityID","countyName","countyID","stateName","stateID") VALUES ({},{},{},{},{},{},{})
        """ .format(cityName,cityPopulation,cityID,cityCountyName,cityCountyID,cityStateName,cityStateID)
        if count == 1:
            print(insertCityQuery)
        try:
            cur.execute(insertCityQuery)
        except cx_Oracle.DatabaseError as e:
            print(e)



print("Ingesting of City Population Completed...")
print("Start Ingesting County Population")

with open("./noah/src/main/resources/population/adm/allCountyPopulation.adm") as countyf:
    count = 0
    for countyjson in load_json_multiple(countyf):
        if count % 1000==0:
            print("send record: {}".format(count))
        count += 1
        countyName = "'"+countyjson['name'].replace("'","''")+"'"
        countyPopulation = countyjson['population']
        countyID = countyjson['countyID']
        countyStateName ="'"+ countyjson['stateName'].replace("'","''")+"'"
        countyStateID = countyjson['stateID']

        insertCountyQuery = """
        INSERT INTO "twitter.dsCountyPopulation"("name","population","countyID","stateName","stateID") VALUES ({},{},{},{},{})
        """ .format(countyName,countyPopulation,countyID,countyStateName,countyStateID)
        if count == 1:
            print(insertCountyQuery)
        try:
            cur.execute(insertCountyQuery)
        except cx_Oracle.DatabaseError as e:
            print(e)


print("Ingesting of County Population Completed...")
print("Start Ingesting State Population...")

with open("./noah/src/main/resources/population/adm/allStatePopulation.adm") as statef:
    count = 0
    for statejson in load_json_multiple(statef):
        count += 1
        name = "'"+statejson['name'].replace("'","''") + "'"
        statePopulation = statejson['population']
        stateID = statejson['stateID']

        insertStateQuery = """
        INSERT INTO "twitter.dsStatePopulation" ("name", "population","stateID") VALUES ({},{},{})
        """.format(name,statePopulation,stateID)
        if count == 1:
            print(insertStateQuery)
        try:
            cur.execute(insertStateQuery)
        except cx_Oracle.DatabaseError as e:
            print(e)

con.commit()





