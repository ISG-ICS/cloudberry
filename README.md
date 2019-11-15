# Cloudberry - Big Data Visualization

[Cloudberry](http://cloudberry.ics.uci.edu) is heavily designed and implemented by the [Actor model](http://doc.akka.io/docs/akka/current/scala/actors.html) using the [Play! Framework](https://www.playframework.com/).

[![Build Status](https://travis-ci.org/ISG-ICS/cloudberry.svg?branch=master)](https://travis-ci.org/ISG-ICS/cloudberry) 
[![code style: prettier](https://img.shields.io/badge/code_style-prettier-ff69b4.svg?style=flat-square)](https://github.com/prettier/prettier)

# About
**Cloudberry** is a general-purpose middleware system to support visualization on large amounts of data. It communicates with backend data management systems via adapters. It supports various frontend interfaces by providing a RESTful interface.

![cloudberry architecture](http://cloudberry.ics.uci.edu/wp-content/uploads/2019/08/cloudberry-overall-architecture.png)

**Twittermap** ([live demo](http://cloudberry.ics.uci.edu/apps/twittermap)) is an application that utilizes Cloudberry's RESTful API to support interactive analytics and visualization on more than **1.6 billion tweets (2TB)** with new data continuously being ingested.

![twittermap demo](https://github.com/ISG-ICS/cloudberry/blob/master/docs/Twittermap%20demo.gif)

# More information
* [Quick Start](https://github.com/ISG-ICS/cloudberry/wiki/quick-start) (Run Twittermap on your own computer!)
* Documentation (Build your own application using Cloudberry!)
  * [Prepare Database](https://github.com/ISG-ICS/cloudberry/wiki/prepare-database)
  * [Register Dataset](https://github.com/ISG-ICS/cloudberry/wiki/register-dataset)
  * [Query Cloudberry](https://github.com/ISG-ICS/cloudberry/wiki/query-cloudberry)
  * [Deregister Dataset](https://github.com/ISG-ICS/cloudberry/wiki/deregister-dataset)
* Advanced topics
  * Database Adapters (Replace backend database!)
    * [Elasticsearch](https://github.com/ISG-ICS/cloudberry/wiki/Elasticsearch-Adapter)
    * [Oracle](https://github.com/ISG-ICS/cloudberry/wiki/Oracle-Adapter)
    * [MySQL](https://github.com/ISG-ICS/cloudberry/wiki/MySQL-Adapter)
    * [PostgreSQL](https://github.com/ISG-ICS/cloudberry/wiki/PostgreSQL-Adapter)
    * [Develop New Adapters](https://github.com/ISG-ICS/cloudberry/wiki/Develop-New-Adapters)
  * [Enable Sidebar Live Tweets](https://github.com/ISG-ICS/cloudberry/wiki/Enable-Sidebar-Live-Tweets)
  * [Realtime Tweets' Ingestion](https://github.com/ISG-ICS/cloudberry/wiki/Start-realtime-twitter-stream-ingestion-into-local-AsterixDB)
* How to Contribute
  * [Deploy Environment](https://github.com/ISG-ICS/cloudberry/wiki/Setting-up-the-development-environment)
  * [Technical Details of Cloudberry Middleware](https://github.com/ISG-ICS/cloudberry/wiki/Cloudberry-Middleware)
  * [Technical Details of TwitterMap Application](https://github.com/ISG-ICS/cloudberry/wiki/TwitterMap-documentation)
* [Research](https://github.com/ISG-ICS/cloudberry/wiki/research)

Online public discussion: [![Join the chat at https://gitter.im/ISG-ICS/cloudberry](https://badges.gitter.im/ISG-ICS/cloudberry.svg)](https://gitter.im/ISG-ICS/cloudberry?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
