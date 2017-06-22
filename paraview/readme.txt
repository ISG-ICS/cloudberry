1) setup ParaviewWeb
    $ npm install paraviewweb  --save
    $ npm install kw-web-suite --save-dev

2) install the following packages
    font-awesome
    mout
    json-bigint

3) generate a single js file
    $ ./node_modules/.bin/webpack

4) start cloudberry service
    $ sbt "project neo" "run"

5) start a php server for the demo under "cloudberry/paraview/dist" directory
    $ php -S localhost:8888

The demo should be available at http://localhost:8888/


To change the Cloudberry servcie address, you need to modify the "cloudberryHost" variable in "paraview/src/CloudberryDemo.js", and the URL in "paraview/dist/proxy.php".

To change the AsterixDB service address, you need to modify the URL in "paraview/dist/proxy.php".


Note: The cross origin request issue is solved by relaying the query to the AsterixDB from the php server.
