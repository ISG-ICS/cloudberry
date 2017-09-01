#!/usr/bin/env python
#===============================================================================
#
#          FILE: reconstructAdmSampleTweetToJson.sh
#
#         USAGE: ./reconstructAdmSampleTweetToJson.sh
#
#   DESCRIPTION: Reconstruct the adm format sample tweets to json format
#
#       OPTIONS:
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Haohan Zhang (), hh1680651@gmail.com
#  ORGANIZATION: ics.uci.edu
#       CREATED: 08/31/2017 20:49:01 PM PDT
#      REVISION:  ---
#===============================================================================

import re
import gzip

with gzip.open('./script/sample.adm.gz', 'rb') as file:
    f = file.readlines()
print 'total len:',len(f)
i = 0
for index in range(len(f)):
	sen = f[index]
	if i%1000 == 0:
		print i
	i += 1
	while 1:
	    mm = re.search('datetime\(".{10}T.{12}Z"\)', sen)
	    nn = re.search('date\(".{10}"\)',sen)
	    ls = re.search('rectangle\(".*,.* .*,.*"\)', sen)
	    fa = re.search(': false', sen)
	    tr = re.search(': true', sen)
	    hs = re.search('["hashtags"]["user_mentions"]: {{.*}}', sen)
	    cr = re.search('\"coordinate\": point\(\".*,.*\"\)', sen)
	    if mm:
	        mm = mm.group()
	        sen = sen.replace(mm, mm.replace("T", " ").replace('Z")', '"').replace("datetime(", ""))
	    elif nn:
	    	nn = nn.group()
	        sen = sen.replace(nn, nn.replace("date(", "").replace(")",""))
	    elif ls:
	    	ls = ls.group()
	    	sen = sen.replace(ls, ls.replace('rectangle("','"LINESTRING(').replace(",","$").replace(" ",",").replace("$"," ").replace('")',')"'))
	    elif fa:
	    	fa = fa.group()
	    	sen = sen.replace(fa, fa.replace("false","0"))
	    elif tr:
	    	tr = tr.group()
	    	sen = sen.replace(tr, tr.replace("true","1"))
	    elif hs:
	        hs = hs.group()
	        sen = sen.replace(hs, hs.replace("{{","[").replace("}}","]"))
	    elif cr:
	    	cr = cr.group()
	    	sen = sen.replace(cr, cr.replace('"coordinate": point("','"coordinate": "point(').replace('")',')"').replace(","," "))
	    else:
	    	f[index] = sen
	        break

text_file = open("./script/sample.json", "w")
text_file.write(''.join(f))
text_file.close()
