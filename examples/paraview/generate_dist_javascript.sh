#!/bin/sh

npm install paraviewweb  --save       # install the ParaviewWeb npm package
npm install kw-web-suite --save-dev   # install the development packages required by ParaviewWeb
npm install font-awesome --save       # a package required by ParaviewWeb but is missing in kw-web-suite
npm install mout --save               # a package required by ParaviewWeb but is missing in kw-web-suite
npm install json-bigint --save        # a package required by ParaviewWeb but is missing in kw-web-suite

./node_modules/.bin/webpack           # generate a combine JavaScript file in the 'dist' directory
