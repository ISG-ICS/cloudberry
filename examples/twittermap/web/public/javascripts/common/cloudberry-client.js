/**
 * cloudberryClient - A service of AngularJS
 *  This service provides a common interface for modules to send a JSON query to Cloudberry.
 *  It provides 1 API function:
 *   - send(query, resultHandler, category, id)
 *  Example:
 *  In "heatmap.js":
 *      function sendHeatmapQuery() {
 *        var heatJson = { JSON query in Cloudberry's format };
 *        cloudberryClient.send(heatJson, function(id, resultSet, timeInterval){
 *          ...
 *          cloudberry.heatmapMapResult = resultSet[0];
 *        }, "heatMapResult");
 *        ...
 *      }
 */
angular.module("cloudberry.common")
  .service("cloudberryClient", function($timeout, cloudberry, cloudberryConfig) {

    var ws = new WebSocket(cloudberryConfig.ws);

    var cloudberryClient = {

      queryToResultHandlerMap: {},

      /**
       * send a query to Cloudberry
       *
       * @param query JSON object, query JSON for Cloudberry
       * @param resultHandler function, callback function(id, resultSet, timeInterval),
       *          if your query's slice option is ON, resultHandler can be called several times.
       * @param category String, not null, category of this query
       * @param id String, identification for this query, if null,
       *                   always use the same resultHandler for this category with id assigned "defaultID"
       *           * NOTE: resultHandler for one unique id can be registered only once at the first time
       *                 you call this send function.
       * @returns {boolean}
       */
      send(query, resultHandler, category, id) {

        var queryCategory = category;
        var queryID = "defaultID";

        if (ws.readyState !== ws.OPEN) {
          return false;
        }

        if (typeof queryCategory === "undefined" || queryCategory === null) {
          return false;
        }

        // This query has unique resultHandler for this queryID
        if(typeof id !== "undefined" && id !== null) {
          queryID = id;
        }

        // The first time registering queryCategory
        if (!(queryCategory in cloudberryClient.queryToResultHandlerMap)) {
          cloudberryClient.queryToResultHandlerMap[queryCategory] = {};
        }
        
        // The first time registering queryID inside queryCategory
        if (!(queryID in cloudberryClient.queryToResultHandlerMap[queryCategory])) {
          cloudberryClient.queryToResultHandlerMap[queryCategory][queryID] = resultHandler;
        }

        // Add "transform" attribute to the query JSON
        query["transform"] = {
          wrap: {
            id: queryID,
            category: queryCategory
          }
        };

        var request = JSON.stringify(query);

        ws.send(request);

        return true;
      }

    };

    ws.onmessage = function(event) {
      $timeout(function() {
        var result = JSONbig.parse(event.data);
        var category = result.category;
        var id = result.id;
        var timeInterval = JSON.stringify({
          start: new Date(cloudberry.parameters.timeInterval.start.getTime()),
          end: new Date(cloudberry.parameters.timeInterval.end.getTime())
        });
        if (typeof result.timeInterval !== "undefined" && result.timeInterval !== null) {
          timeInterval = result.timeInterval;
        }
        cloudberryClient.queryToResultHandlerMap[category][id](id, result.value, timeInterval);
      });
    };

    return cloudberryClient;
  });
