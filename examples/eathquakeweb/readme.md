# Earthquake Web App

## Overview

This article gives a quick start guide to deploy an example earthquake web app on top of Cloudberry and Oracle database.

## Requirement

**Oracle 10g+ database**

If you don't have an Oracle database installed already, these are example links to install Oracle  database on Mac OS and Linux:

[How to install Oracle Database on Mac OS Sierra 10.12 or above](https://medium.com/@mfofana/how-to-install-oracle-database-on-mac-os-sierra-10-12-or-above-c0b350fd2f2c)

[How to Install Oracle Database 12c on CentOS 7](https://www.howtoforge.com/tutorial/how-to-install-oracle-database-12c-on-centos-7/)

[Install Oracle Database 12c on Ubuntu 16.04](https://medium.com/venturenxt/install-oracle-database-12c-on-ubuntu-16-04-c081d51c0f9d)

**Suppose your Oracle Database information are as follows:**

- Username: berry
- Password: orcl
- Port: 1521
- SID: orcl

## Create Tables and generate synthetic data

#### Start SQLPlus command line tool
```plsql
$ sqlplus
```

#### Connect to Oracle database
```plsql
SQL> connect berry@orcl
```

#### Run `earhtquake-data.sql` **(it will take 2-3 mins)**
```plsql
SQL> @/home/oracle/earthquake-data.sql
```

## Configure and start Cloudberry

#### Configure Cloudberry to connect to Oracle

Modify `cloudberry/cloudberry/neo/conf/application.conf`

```bash
#asterixdb.lang = SQLPP # comment this line
asterixdb.lang = oracle # uncomment this line
oracledb.url = "jdbc:oracle:thin:berry/orcl@localhost:1521:orcl" # uncomment this line with url of your oracle
```

(*Note: please replace username, password, hostname, port and sid with your information*)

#### Start Cloudberry

```bash
$ cd cloudberry/cloudberry
$ sbt compile
$ sbt "project neo" "run"
```

## Register Dataset to Cloudberry

```bash
$ curl -X POST -H "Content-Type: application/json" -d @register-data.json http://localhost:9000/admin/register
```

## Start earthquake web application

Open a browser (e.g. Chrome) to access `cloudeberry/example/EarthquakeWeb/EarthquakeApp.html` to see the EathquakeWeb frontend.





