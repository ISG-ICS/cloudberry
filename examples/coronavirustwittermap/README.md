# Coronavirus TwitterMap Setup

Follow the quick-start instructions here: [(II) Setup Twittermap on your Local Machine](https://github.com/ISG-ICS/cloudberry/wiki/quick-start#ii-setup-twittermap-on-your-local-machine)

The different steps are as follows.

1) In **Step 2.3: Download and ingest the synthetic sample tweets (about 100K) data into AsterixDB.**

Replace **(1) Download the synthetic sample tweets (about 100K) data:** with the following commands,
```bash
cd ~/quick-start/cloudberry/examples/twittermap/script/
wget http://cloudberry.ics.uci.edu/img/coronavirus-tweets/sample.adm.gz
```
**_Note: this will download coronavirus replated sample tweets._**

2) In **Step 2.4: Start the TwitterMap Web server (in port 9001):**

Use the following command to start CoronavirusTwitterMap,
```bash
cd ~/quick-start/cloudberry/examples/coronavirustwittermap/
sbt "project web" "run 9001"
```

Now you should be able to see Coronavirus TwitterMap frontend by visiting: [http://localhost:9001](http://localhost:9001).

**_Note: all other information and trouble shooting should be the same with quick-start here:_** [(II) Setup Twittermap on your Local Machine](https://github.com/ISG-ICS/cloudberry/wiki/quick-start#ii-setup-twittermap-on-your-local-machine) 