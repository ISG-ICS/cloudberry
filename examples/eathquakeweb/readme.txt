3. Setup Cloudberry and Earthquake application:

Step 3.1: Install Oracle Database 10g+ on your machine and remember your server information and account information.

Step 3.2:  Start Oracle Database on your machine and create EARTHQUAKEDATA table and insert example data.
3.2.1 Start Oracle Database
	Execute the following command on your machine and a ¡°SQL>¡± prompt will be appear.
sqlplus "/as sysdba"
 
Then, execute command ¡°startup¡± to startup Oracle Database and quit the SQL PLUS environment with ¡°quit¡± command.

3.2.2 Start Oracle listener

lsnrctl start

3.2.3 Create EARTHQUAKEDATA table and insert example data.
Start SQL PLUS with the flowing command using your own password instead of YOURPASSWORD
sqlplus system/YOURPASSWORD

copy and past the script in file ¡°createsql.txt¡± to here

wait the command to finished and quit SQL PLUS with ¡°quit¡± command.
 

Step 3.3: Configure Cloudberry server.
 

Comment the statement ¡°asterixdb.lang = SQLPP¡±, and add the following statements:
oracledb.url = "jdbc:oracle:thin:system/PASSWORD@//SERVERIP:1521/INSTANCENAME"
asterixdb.lang = oracle

 

Step 3.4: Compile and run the Cloudberry server.

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


3.5 Open the browser to access cloudeberry/example/EarthquakeWeb/TestCloudberry.html to see the EathquakeWeb frontend.
If you want to view this web application in other computer, you can copy the forder ¡°EarthquakeWeb¡± to your computer and use notepad edit the file ¡°TestCloudberry.html¡±. To make sure ¡°COLUDBERRYURL¡± variable to point to your own cloudberry server.

 
3.6 You can change the search conditions and click button ¡°Search¡± to view the result.
