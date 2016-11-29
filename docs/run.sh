#!/bin/bash - 
#===============================================================================
#
#          FILE: run.sh
# 
#         USAGE: ./run.sh 
# 
#   DESCRIPTION: 
# 
#       OPTIONS: ---
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Jianfeng Jia (), jianfeng.jia@gmail.com
#  ORGANIZATION: ics.uci.edu
#       CREATED: 11/19/2016 07:59:19 PM PST
#      REVISION:  ---
#===============================================================================

set -o nounset                              # Treat unset variables as an error

docker run -p 4000:4000 -v `pwd`:/app mangar/jekyll:1.1 bash -c "bundle install; bundle exec jekyll serve --host 0.0.0.0"

