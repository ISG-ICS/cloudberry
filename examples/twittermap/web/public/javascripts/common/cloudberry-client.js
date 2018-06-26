angular.module('cloudberry.common')
  .service('cloudberryClient', function($timeout, cloudberryConfig) {

    var ws = new WebSocket(cloudberryConfig.ws);

    var cloudberryClient = {

      queryToResultHandlerMap: {},

      /**
       * send a query to Cloudberry
       *
       * @param query JSON object, query JSON for Cloudberry
       * @param resultHandler function, callback function(id, resultSet)
       * @param category String, not null, category of this query
       * @param id String, identification for this query, if null,
       *                   always use the same resultHandler for this category
       * @returns {boolean}
       */
      send(query, resultHandler, category, id) {
        if (typeof category === "undefined") {
          return false;
        }

        // The first time registering resultHandler for this category
        if (!category in this.queryToResultHandlerMap) {
          this.queryToResultHandlerMap[category] = {common: resultHandler};
        }

        // This query has unique resultHandler for this query id
        if(id instanceof String) {
          this.queryToResultHandlerMap[category][id] = resultHandler;
        }

        // Add "transform" attribute to the query JSON
        query["transform"] = {
          wrap: {
            id: id,
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
        if (category in cloudberryClient.queryToResultHandlerMap) {
          if (id in cloudberryClient.queryToResultHandlerMap[category]) {
            cloudberryClient.queryToResultHandlerMap[category][id](id, result.value);
          }
          else {
            cloudberryClient.queryToResultHandlerMap[category]["common"](id, result.value);
          }
        }
      })
    };

    return cloudberryClient;
  });