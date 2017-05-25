1) setup ParaviewWeb
    $ npm install paraviewweb  --save
    $ npm install kw-web-suite --save-dev

2) install the following packages
    font-awesome
    mout
    json-bigint
    http-server

3) generate a single js file
    $ ./node_modules/.bin/webpack

4) start cloudberry service
    $ sbt "project neo" "run"

5) start http server for the demo
    $ npm start

The demo should be available at http://localhost:8080/

Note: There is a cross origin request issue because of the query from the front end to the asterixDB. Since it is just for performance measurement purpose, I use a simple workaround to start chrom with the argument "--args --disable-web-security --user-data-dir" to enable cross origin request.
