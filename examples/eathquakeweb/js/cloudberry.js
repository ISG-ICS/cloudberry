
var oraclequery='{\
  "dataset": "EARTHQUAKEDATA",\
  "filter": [\
    {\
      "field": "DEVID",\
      "relation": "matches",\
      "values": [\
        "T_DEVID"\
      ]\
    },\
    {\
      "field": "OBSDATE",\
      "relation": "inRange",\
      "values": [\
        "T_stardate",\
        "T_enddate"\
      ]\
    }\
  ],\
  "group": {\
    "by": [\
      {\
        "field": "OBSDATE",\
        "as": "dd",\
        "apply": {\
          "name": "interval",\
          "args": {\
            "unit": "T_interval"\
          }\
        }\
      }\
    ],\
    "aggregate": [\
      {\
        "field": "OBSVALUE",\
        "apply": {\
          "name": "T_aggregate"\
        },\
        "as": "v" \
      } \
    ] \
  },\
  "select" : {\
    "order" : [ "dd"],\
    "limit": T_number,\
    "offset" : 0\
  },\
	  "option" : { \
	    "sliceMillis": T_sliceMillis  \
	  }\
}\
'
	
	
     
//var cloudberry_url='http://128.195.52.124:9000/berry'
var cloudberry_url='http://localhost:9000/berry'
	
function getQueryJson(device,startdate,enddate,intervaltime,rowsnumber,aggregate,sliceMillis)
{
	var json;
	json =oraclequery.replace(/T_DEVID/, device)	  
	json =json.replace(/T_stardate/, startdate)
	json =json.replace(/T_enddate/, enddate)
	json =json.replace(/T_interval/, intervaltime)
	json =json.replace(/T_number/, rowsnumber)
	json =json.replace(/T_aggregate/, aggregate)
	json =json.replace(/T_sliceMillis/, sliceMillis)	
	return json;
}