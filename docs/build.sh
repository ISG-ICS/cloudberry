#!/bin/bash - 
#===============================================================================
#
#          FILE: build.sh
# 
#         USAGE: ./build.sh 
# 
#   DESCRIPTION: 
# 
#       OPTIONS: ---
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Jianfeng Jia (), jianfeng.jia@gmail.com
#  ORGANIZATION: ics.uci.edu
#       CREATED: 11/29/2016 12:31:27 AM PST
#      REVISION:  ---
#===============================================================================

set -o nounset                              # Treat unset variables as an error

docker run -v `pwd`:/app mangar/jekyll:1.1 bash -c "bundle install; bundle exec jekyll build"
sudo cp -rf _site/* /var/www/html/

