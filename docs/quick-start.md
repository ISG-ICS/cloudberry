---
layout: page
---

## Setup TwitterMap Using Cloudberry and AsterixDB

This page includes instructions on how to use Cloudberry and AsterixDB to setup a small instance of the
[TwitterMap](http://cloudberry.ics.uci.edu/demos/twittermap/) on a local machine. 
The following diagram illustrates its architecture: ![architecture][architecture]

System requirements:

 - Linux or Mac
 - At least 4GB memory

**Step 1**: Follow the instructions on this [`page`](http://www.scala-sbt.org/release/docs/Setup.html) to install `sbt`.

**Step 2**: Clone the Cloudberry codebase from github.

```
~> git clone https://github.com/ISG-ICS/cloudberry.git
```

Suppose the repostory is cloned to the folder `~/cloudberry`.

**Step 3**: Use the following steps to start a Docker container that has a pre-built AsterixDB cluster.

   1. Install [Docker](https://www.docker.com/products/docker) (version at least 1.10) on the local machine;
   2. Run the following commands:

```
~> cd cloudberry
~/cloudberry> ./script/dockerRunAsterixDB.sh
```
The second command will download and run a prebuilt AsterixDB docker container from [here](https://hub.docker.com/r/jianfeng/asterixdb/). This step may take 5-10 minutes or even longer, depending on your network speed.
After it finishes, you should see the messages as shown in the following screenshot:
![docker][docker]

**Step 4**: Run the following command to ingest sample tweets (about 324K) and US population data into AsterixDB.


```
~/cloudberry> ./script/ingestAllTwitterToLocalCluster.sh
```

This step is downloading about 70MB of data, and it may take 5 minutes, again, depending on your network speed. You should see the messages as shown in the following screenshot:
![ingestion][ingestion]

**Step 5**: Compile and run the Cloudberry server.

```
~/cloudberry> sbt compile
~/cloudberry> sbt "project neo" "run"
```

Wait until the shell prints the messages shown in the following screenshot:
![neo][neo]

**Step 6**: Start the TwitterMap Web server (in port 9001) by running the following command in another shell:

```
~/cloudberry> sbt "project twittermap" "run 9001"
```

Wait until the shell prints the messages shown in the following screenshot:
![twittermap][twittermap]


**Step 7**: Open a browser to access [http://localhost:9001](http://localhost:9001) to see the TwitterMap frontend.  The first time you open the page, it could take up to several minutes (depending on your machine's speed) to show the following Web page:
![web][web]


**Congratulations!** You have successfully set up TwitterMap using Cloudberry and AsterixDB!


## Setup your own AsterixDB cluster 

The instructions above assume that we use an AsterixDB instance in a Docker container. If you want to setup your AsterixDB cluster, please use the following steps.


**Step 8**: Follow the instructions on the [AsterixDB Installation Guide](https://ci.apache.org/projects/asterixdb/index.html) to install an AsterixDB cluster.  Select your preferred installation option. 

**Step 9**: Ingest twitter data.

**Step 10**: Change the Cloudberry middleware configuration to connect to this new AsterixDB cluster. You can modify the AsterixDB hostname in the configuration file `neo/conf/application.conf` and change the `asterixdb.url` value to the AsterixDB hostname.

```
asterixdb.url = "http://YourAsterixDBHostName:19002/query/service"
```

## Build your own application

For more information about Cloudberry, please read its [documentation](/documentation).

[architecture]: /img/quick-start-architecture.png
{: width="800px"}
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
