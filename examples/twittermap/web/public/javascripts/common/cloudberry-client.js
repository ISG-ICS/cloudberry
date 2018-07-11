/**
 * cloudberryClient - A service of AngularJS
 *  This service provides a common interface for modules to send a JSON query to Cloudberry.
 *  It provides 1 API function:
 *   - send(query, resultHandler, category, id)
 *  Example:
 *  In "heatmap.js":
 *      function sendHeatmapQuery() {
 *        var heatJson = { JSON query in Cloudberry's format };
 *        cloudberryClient.send(heatJson, function(id, resultSet){
 *          ...
 *          cloudberry.heatmapMapResult = resultSet[0];
 *        }, "heatMapResult");
 *        ...
 *      }
 */
angular.module("cloudberry.common")
  .service("cloudberryClient", function($timeout, cloudberryConfig) {

    var ws = new WebSocket(cloudberryConfig.ws);

    var cloudberryClient = {

      queryToResultHandlerMap: {},

      /**
       * send a query to Cloudberry
       *
       * @param query JSON object, query JSON for Cloudberry
       * @param resultHandler function, callback function(id, resultSet),
       *          if your query's slice option is ON, resultHandler can be called several times.
       * @param category String, not null, category of this query
       * @param id String, identification for this query, if null,
       *                   always use the same resultHandler for this category
       * @returns {boolean}
       */
      send(query, resultHandler, category, id) {

        if (ws.readyState !== ws.OPEN) {
          return false;
        }

        if (typeof category === "undefined" || category === null) {
          return false;
        }

        // The first time registering resultHandler for this category
        if (!(category in cloudberryClient.queryToResultHandlerMap)) {
          cloudberryClient.queryToResultHandlerMap[category] = {common: resultHandler};
        }

        // This query has unique resultHandler for this query id
        if(typeof id !== "undefined" && id !== null) {
          cloudberryClient.queryToResultHandlerMap[category][id] = resultHandler;
        }

        // Add "transform" attribute to the query JSON
        query["transform"] = {
          wrap: {
            id: id ? id : category,
            category: category
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
        if (id in cloudberryClient.queryToResultHandlerMap[category]) {
          cloudberryClient.queryToResultHandlerMap[category][id](id, result.value);
        }
        else {
          cloudberryClient.queryToResultHandlerMap[category]["common"](id, result.value);
        }
      });
    };

    return cloudberryClient;
  });