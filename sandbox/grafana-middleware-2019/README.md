# Grafana-InfluxDB Middleware
A simple middleware between Grafana and Influxdb that has the following functions:  
- simple forwarding
- optimize queries, accelerate the frontend rendering speed.
- intelligently decide the sample size

### Setup
##### Install Grafana:
To install Grafana, you may use Homebrew:  
`
Brew update`      
`Brew install grafana      
`   
OR Install from binary tar file:     
Download the latest [tar.gz](https://grafana.com/get) file and extract it.      

##### Install InfluxDB:  
To install InfluxDB:     
`brew update`      
`brew install influxdb        
`   
Other methods are available [here](https://portal.influxdata.com/downloads/).  

##### To run grafana and influxdb:   
`brew services start grafana`  
`brew services start influxdb`     
###### To stop grafana and influxdb:  
`brew services stop grafana`      
`brew services stop influxdb`  
### Getting started with InfluxDB:  
1. create a database:     
`create database NOAA_water_database`      

2. Ingest data by using influxDB command `influx -import -path=NOAA_data.txt -precision=s -database=NOAA_water_database` OR by running our script `IngestRandomData.py` stored in the `data` directory.      

More InfluxDB commands can be found in [here](https://docs.influxdata.com/influxdb/v1.7/introduction/getting-started/).  

### Getting started with Grafana:  
To run Grafana open your browser and go to http://localhost:3000/. 3000 is the default HTTP port that Grafana listens to.  
- First, add a data source for Grafana. Find the Configuration on the side menu and click Data sources. If the side menu is not visible click the Grafana icon in the upper left corner.  
- Click "Add Data Source" and you will see settings page of your new data source and choose InfluxDB as a data source type.   
- The default InfluxDB port is 8086. You need to specify the database you created in InfluxDB and the user.   
- After setting this up, click "Save & Test". If everything works well, it will show “Data source is working”.      
- Find "Create" on the side menu and click "Dashboard".Click “Graph” to create a graph panel.    
- Click "edit" to edit the details of the panel.    
- After choosing a data source, you can write your desired query to InfluxDB. 

A detailed Grafana instruction can be found in [here](https://grafana.com/docs/grafana/latest/guides/getting_started/).

### How to run our code:   
In the `middlewares` directory, we wrote three middlewares that connects the frontend Grafana and the backend InfluxDB database.         
- `SimpleForwardMiddleware.py` is a simple middleware that forwards the query to influxdb, gets the data from db, and passes it back to grafana.       
- `PartialMiddleware.py` is a middleware that fetches all the data points requested from influxdb, but only send partial(i.e. 2000) data to Grafana.   
- `QueryOptMiddleware.py` is a middleware that can optimize queries using random sampling when data is large. Files in `smart-sampling` demonstrates how to intelligently decide a sample ratio.       
   
You can start up our middleware by running each middleware files. After the middleware is started, Grafana will communicate the database through the middleware, and you will find that Grafana will visualize the data much faster than before when the target data is large.      

A detailed report can be found in [Grafana-Final-Report.pdf](https://github.com/heyhalcyon/cloudberry/blob/master/sandbox/grafana-middleware-2019/Grafana-Final-Report.pdf). 



 






