# Cloudberry Matrix 


[Cloudberry](http://cloudberry.ics.uci.edu) is now using the [Play! Framework](https://www.playframework.com/) and [Angular JS](https://angular.io/)

[![Build Status](https://travis-ci.org/ISG-ICS/cloudberry.svg?branch=master)](https://travis-ci.org/ISG-ICS/cloudberry)
[![codecov](https://codecov.io/gh/ISG-ICS/cloudberry/branch/master/graph/badge.svg)](https://codecov.io/gh/ISG-ICS/cloudberry)

Users are welcome to join our online chat forum :[![Join the chat at https://gitter.im/ISG-ICS/cloudberry](https://badges.gitter.im/ISG-ICS/cloudberry.svg)](https://gitter.im/ISG-ICS/cloudberry?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

For developers please join our [slack group](https://cloudberry-uci.slack.com/)

## Build

Prerequisites: scala, sbt, [AsterixDB](http://asterixdb.apache.org)

### Prepare the AsterixDB cluster
Option 1: Follow the official [documentation](https://ci.apache.org/projects/asterixdb/install.html) to setup a fully functional cluster.

Option 2: Use the prebuilt AsterixDB docker image to run a small test cluster locally. 
This approach serves the debug purpose. 

Assume you've already had docker(>1.10) installed on your local machine, 
you can simply run the following command to create a two nc AsterixDB cluster locally. 

```
./script/dockerRunAsterixDB.sh && sleep 2s && ./script/ingestTwitterToLocalCluster.sh
```

### To compile projects
```
 sbt compile
```

### Run front-end demo 
You will need to give the AsterixDB cluster link to `neo` by change the `asterixdb.url` configuration in `neo/conf/application.conf` file.
The default value points to the localhost docker cluster
```
 sbt "project neo" "run"
```
