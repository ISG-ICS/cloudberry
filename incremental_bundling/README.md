# Graph Visualization


Project is using [Play! Framework](https://www.playframework.com/), [SBT](https://www.scala-sbt.org/) and [PostgreSQL ](https://www.postgresql.org/download/)

## Build

### Prepare the PostgreSQL database
```
> cd graph-viz/data
> psql postgres
```
you need to save the shared datafile in the data folder
then execute the sql steps in the data file setup_postgres_reply_tweets_graph.sql

### To compile project
```
> cd graph-viz
> sbt compile
```

### Run demo

```
> cd graph-viz
> sbt run
```
You should see the demo on [http://localhost:9000](http://localhost:9000)