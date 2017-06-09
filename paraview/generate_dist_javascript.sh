#!/bin/sh

npm install paraviewweb  --save
npm install kw-web-suite --save-dev
npm install font-awesome --save
npm install mout --save
npm install json-bigint --save
npm install http-server --save

./node_modules/.bin/webpack
