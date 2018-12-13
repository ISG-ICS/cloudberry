3. Setup Cloudberry and Earthquake:

Step 3.1: Install Oracle Database 10g+ on your machine and remember your server information and account information.



Step 3.1: create EARTHQUAKEDATA table and insert example data.
execute the following command:
sqlplus "/as sysdba"

Copy the script in "createsql.txt" and past on the terminal.




Step 3.1: Compile and run the Cloudberry server.

~/cloudberry> cd cloudberry
~/cloudberry> sbt compile
~/cloudberry> sbt "project neo" "run"
Wait until the shell prints the messages shown as following:

$ sbt "project neo" "run"
[info] Loading global plugins from /Users/white/.sbt/0.13/plugins
[info] Loading project definition from /Users/white/cloudberry/cloudberry/project
[info] Set current project to cloudberry (in build file:/Users/white/cloudberry/cloudberry/)
[info] Set current project to neo (in build file:/Users/white/cloudberry/cloudberry/)

--- (Running the application, auto-reloading is enabled) ---

[info] p.c.s.NettyServer - Listening for HTTP on /0:0:0:0:0:0:0:0:9000

(Server started, use Ctrl+D to stop and go back to the console...)
Step 2.2: Open another terminal window to ingest sample tweets (about 53K) and US population data into AsterixDB.

~/cloudberry> cd ../examples/twittermap
~/twittermap> ./script/ingestAllTwitterToLocalCluster.sh
When it finishes you should see the messages shown as following:

Socket 127.0.0.1:10005 - # of ingested records: 260000
Socket 127.0.0.1:10005 - # of total ingested records: 268497
>>> # of ingested records: 268497 Elapsed (s) : 2 (m) : 0 record/sec : 134248.5
>>> An ingestion process is done.
[success] Total time: 3 s, completed Nov 19, 2018 8:44:51 PM
Ingested city population dataset.
Step 2.3: Start the TwitterMap Web server (in port 9001) by running the following command in another shell:

~/twittermap> sbt "project web" "run 9001"
Wait until the shell prints the messages shown as following:

$ sbt "project web" "run 9001"
[info] Loading global plugins from /Users/white/.sbt/0.13/plugins
...
--- (Running the application, auto-reloading is enabled) ---

[info] p.c.s.NettyServer - Listening for HTTP on /0:0:0:0:0:0:0:0:9001

(Server started, use Ctrl+D to stop and go back to the console...)
Step 2.4: Open a browser to access http://localhost:9001 to see the TwitterMap frontend. The first time you open the page, it could take up to several minutes (depending on your machine¡¯s speed) to show the following Web page: