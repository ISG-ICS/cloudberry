#!/bin/bash - 
#===============================================================================
#
#          FILE: prepare_ddl.sh
# 
#         USAGE: ./prepare_ddl.sh
# 
#   DESCRIPTION: 
# 
#       OPTIONS: ---
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Jianfeng Jia (), jianfeng.jia@gmail.com
#  ORGANIZATION: ics.uci.edu
#       CREATED: 01/21/2016 12:17:46 AM PST
#      REVISION:  ---
#===============================================================================


host="http://localhost:19002"
[ ! -z $1 ] && host=$1

set -o nounset                              # Treat unset variables as an error

ncip=`docker inspect nc-master-1 | grep IPAddress | grep -o '[0-9.]*' | head -1`

cat <<-EOF | curl -XPOST --data-binary @- $host/aql  
use dataverse twitter;

load dataset ds_tweets
using localfs
(("path"="$ncip:///data/sample.adm"),("format"="adm"));

EOF

