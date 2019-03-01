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
  .service("cloudberryClient", function($timeout, cloudberry, cloudberryConfig, moduleManager) {

    var ws;

    var queryToResultHandlerMap = {};

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
    this.send = function(query, resultHandler, category, id) {

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
      if (!(queryCategory in queryToResultHandlerMap)) {
        queryToResultHandlerMap[queryCategory] = {};
      }

      // The first time registering queryID inside queryCategory
      if (!(queryID in queryToResultHandlerMap[queryCategory])) {
        queryToResultHandlerMap[queryCategory][queryID] = resultHandler;
      }

      // Add "transform" attribute to the query JSON
      query["transform"] = {
        wrap: {
          id: queryID,
          category: queryCategory
        }
      };

      var request = JSON.stringify(query);

      // Special operation only for individual pin look up query,
      // change id from string to int64
      if (category === "pinResult") {
        request = request.replace(/"(\d+)"/,"[$1]");
      }

      ws.send(request);

      return true;
    };

    this.connectWS = function(url) {

      var deferred = new $.Deferred();
      var lws = null;

      function connect(url) {

        lws = new WebSocket(url);

        lws.onopen = function () {
          console.log("[cloudberry-client] connecting to [" + url + "] succeed!");
          deferred.resolve(lws);
        };

        lws.onerror = function (err) {
          console.log("[cloudberry-client] connecting to [" + url + "] fail!");
          lws.close();
        };

        lws.onclose = function (e) {
          console.log("[cloudberry-client] will reconnect to [" + url + "] in 500 ms.");
          setTimeout(function () {
            console.log("[cloudberry-client] reconnecting to [" + url + "] ...");
            connect(url);
          }, 500);
        };
      }

      connect(url);

      return deferred.promise();
    };

    var wsConnection = this.connectWS(cloudberryConfig.ws);

    wsConnection.done(function (pws) {
      ws = pws;

      console.log("connection done.  ws = " + ws);
      moduleManager.publishEvent(moduleManager.EVENT.WS_READY, {});

      ws.onmessage = function (event) {
        $timeout(function () {
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
          queryToResultHandlerMap[category][id](id, result.value, timeInterval);
        });
      };
    });

  });
