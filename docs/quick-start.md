---
layout: page
title: Quick Start
---

## Setup TwitterMap locally

This page includes instructions on how to setup a small instance of the
[TwitterMap](http://cloudberry.ics.uci.edu/demos/twittermap/) on a local machine.

System requirements:

 - Linux or Mac
 - At least 4GB memory

**Step 1**: Install `sbt` by following the instructions on this [`page`](http://www.scala-sbt.org/release/docs/Setup.html).

**Step 2**: Clone the codebase.

```
shell> git clone https://github.com/ISG-ICS/cloudberry.git
```

Suppose the repostory is cloned to the folder `~/cloudberry`.

**Step 3**: Use the following steps to install an AsterixDB cluster on the local machine in order to run the Cloudberry middleware.  

   1. Install [Docker](https://www.docker.com/products/docker) (version at least 1.10) on the local machine;
   2. Run the following commands to create an AsterixDB cluster locally:

```
~> cd cloudberry
~/cloudberry> ./script/dockerRunAsterixDB.sh
```
This command will download and run a prebuilt AsterixDB docker image from [here](https://hub.docker.com/r/jianfeng/asterixdb/). This step may take 5-10 minutes or even longer, depending on your network speed.
After it finishes, you should see the messages as shown in the following screenshot:
![docker][docker]

**Step 4**: Run the following command to ingest sample tweets (about 324K) and US population data into AsterixDB.


```
~/cloudberry> ./script/ingestAllTwitterToLocalCluster.sh
```

This step is downloading about 70MB of data, and it may take 5 minutes, again, depending on your network speed.  This step is successful after you see a message "Data ingestion completed!" in the shell.
After it finishes, you should see the messages as shown in the following screenshot:
![ingestion][ingestion]

**Step 5**: Compile and run the Cloudberry server.

```
~/cloudberry> sbt compile
~/cloudberry> sbt "project neo" "run"
```

Wait until the shell prints the messages as shown in the following screenshot:
![neo][neo]

**Step 6**: Start the TwitterMap frontend by running the following command in another shell:

```
~/cloudberry> sbt "project twittermap" "run 9001"
```

Wait until the shell prints the messages as shown in the following screenshot:
![twittermap][twittermap]


**Step 7**: Open a browser to access [http://localhost:9001](http://localhost:9001) to see the TwitterMap frontend.  Notice that the first time you open the page, it could take up to several minutes (depending on your machine) to show the following webpage:
![web][web]


**Congratulations!** You have successfully set up TwitterMap using AsterixDB and Cloudberry!



### Run your own front-end server

TwitterMap is our homemade front-end that shows how to use Cloudberry server. You can implement own front-end service
and let it talk to Cloudberry to achieve the same interactive user experience.

[docker]: /img/docker.png
{: width="800px"}
[ingestion]: /img/ingestion.png
{: width="800px"}
[neo]: /img/neo.png
{: width="800px"}
[twittermap]: /img/twittermap.png
{: width="800px"}
[web]: /img/web.png
{: width="800px"}
