# Cloudberry - Big Data Visualization

[Cloudberry](http://cloudberry.ics.uci.edu) is heavily designed and implemented by the [Actor model](http://doc.akka.io/docs/akka/current/scala/actors.html) using the [Play! Framework](https://www.playframework.com/).

[![Build Status](https://travis-ci.org/ISG-ICS/cloudberry.svg?branch=master)](https://travis-ci.org/ISG-ICS/cloudberry) 
[![code style: prettier](https://img.shields.io/badge/code_style-prettier-ff69b4.svg?style=flat-square)](https://github.com/prettier/prettier)

![twittermap demo](https://github.com/ISG-ICS/cloudberry/blob/master/docs/Twittermap%20demo.gif)
[live demo](http://cloudberry.ics.uci.edu/apps/twittermap)

# About
Cloudberry is a general-purpose middleware system to support visualization on large amounts of data. It communicates with backend data management systems via adapters. It supports various frontend interfaces by providing a RESTful interface.

![software architecture](https://docs.google.com/drawings/d/e/2PACX-1vT0SZxo6i5eIvtBOYmKUkZyrK5dawUy4mYcHHE4G4PjLeFRVdg5_PI-wgHJHb0S0VTWdDN-2vUE2OrQ/pub?w=960&h=720)

Twittermap ([live demo](http://cloudberry.ics.uci.edu/apps/twittermap)) is an application that utilizes Cloudberry's RESTful API to support interactive analytics and visualization on more than one billion tweets with new data continuously being ingested.

# More information
* [Quick Start](https://github.com/ISG-ICS/cloudberry/wiki/quick-start) (Run Twittermap on your own computer!)
* Documentation (Build your own application using Cloudberry!)
  * [Prepare Database](https://github.com/ISG-ICS/cloudberry/wiki/prepare-database)
  * [Register Dataset](https://github.com/ISG-ICS/cloudberry/wiki/register-dataset)
  * [Query Cloudberry](https://github.com/ISG-ICS/cloudberry/wiki/query-cloudberry)
  * [Deregister Dataset](https://github.com/ISG-ICS/cloudberry/wiki/deregister-dataset)
* Advanced topics
  * Database Adapters (Replace backend database!)
    * [Elasticsearch](https://github.com/ISG-ICS/cloudberry/wiki/Elasticsearch-Adapter-Quick-Start-Guide)
    * [Oracle](https://github.com/ISG-ICS/cloudberry/pull/617)
    * [PostgreSQL]()
    * [MySQL](https://github.com/ISG-ICS/cloudberry/wiki/Documentation-for-Cloudberry-(Using-SQL-Database))
    * [SparkSQL](https://github.com/ISG-ICS/cloudberry/wiki/Connect-to-SparkSQL)
  * [Realtime Tweets' Ingestion](https://github.com/ISG-ICS/cloudberry/wiki/Start-realtime-twitter-stream-ingestion-into-local-AsterixDB) 
* How to Contribute
  * [Deploy Environment](https://github.com/ISG-ICS/cloudberry/wiki/Setting-up-the-development-environment)
  * [Technical Details of Cloudberry Middleware](https://github.com/ISG-ICS/cloudberry/wiki/Cloudberry-Middleware)
  * [Technical Details of TwitterMap Application](https://github.com/ISG-ICS/cloudberry/wiki/TwitterMap-documentation)
* [Research](https://github.com/ISG-ICS/cloudberry/wiki/research)

Online public discussion: [![Join the chat at https://gitter.im/ISG-ICS/cloudberry](https://badges.gitter.im/ISG-ICS/cloudberry.svg)](https://gitter.im/ISG-ICS/cloudberry?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
