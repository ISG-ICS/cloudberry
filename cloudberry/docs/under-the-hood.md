---
layout: page
---

## What can Cloudberry do ? 
A client application is highly customized for one specific domain. The types of queries sent to the database are limited 
and the consecutive queries usually have certain semantic relations. On the other hand, database systems are general 
purpose systems that each incoming query is treated independently. The relation between queries is difficult to 
capture if the application directly talks with a database system.

Cloudberry is a middleware service that connects the general purpose backend database system and the client application. 
It can understand the semantic of queries, automatically store and maintain the query result, analyse the relation between the new query and the existing 
results, and then only send the necessary queries to the database system. In this way the client application can achieve 
the fast query performance and don't have to handle the complex view design and maintenance logic itself.


## Cloudberry Architecture

The main component of the Cloudberry is the `Query Planner` and the `View Manager`. The `Query Planner` is responsible 
for query rewriting depending on the given views information. If there is an appropriate view, the original query will be
split into multiple queries to ask different datasets. After all results come back, the `Query Planner` will merge 
the results from all queries and return to the client. The `View Manager` is responsible for view maintenance. Each view
is generated, maintained, and removed automatically.

![twittermap-artitecture][architecture]

The following figures shows an example how Cloudberry answer the request using the existing query result. 
![view-cache-example][view-cache]
The client sends a query to ask the *total count of the tweets mentioning "zika" in the "Twitter" dataset*. 
By parsing the request and checking the "view information" Cloudberry knows there is already a view that contains all
"zika" related tweets till Mar 12. Then it can split the query to ask the count of the tweets till Mar 12 against the 
"zika" dataset and only asks the original "Twitter" for the count of tweets published after Mar 12. 

Since the view normally is very small compare to the original dataset, the view result returned in step 6 is very fast.
On the original dataset "Twitter" side, because we give it a very selective query range (from Mar 12 to now), the query 
performance is also very fast. Thus, the query performance is highly improved comparing to directly send the request
to the database. 

After the query has been answered, the View Manager will create or update the view based on the requirement. E.g., in 
our example, it will send the update query to the database to append the new record to the existing "zika" view to 
keep it up to date. We also defined several rules to decide if we need to store the current query result and what should
be in the view to speedup the future queries.


## Query Slicing

Not every query can find an appropriate view, especially at the beginning when the system just started. In this case, 
instead of waiting for the entire query result, Cloudberry can return a serials of partial results as a streaming fashion 
in a steady pace.

Under the hood of the system, Cloudberry splits the query into a serials of mini-queries. The selectivity 
of each mini-query is adapted based on the query performance so that each mini-query is guaranteed to finish within 
a short time limit. 




[architecture]: https://www.lucidchart.com/publicSegments/view/c810ca78-6690-4505-9b67-a27c4f2bcc6a/image.png
{: width="800px"}
[view-cache]: https://www.lucidchart.com/publicSegments/view/24b3c182-a055-4ba0-a966-5916033e7ae5/image.png
{: width="800px"}


