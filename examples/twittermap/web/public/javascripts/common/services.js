angular.module('cloudberry.common', ['cloudberry.mapresultcache'])
  .factory('cloudberryConfig', function(){
    return {
      ws: "ws://" + location.host + "/ws",
      sentimentEnabled: config.sentimentEnabled,
      sentimentUDF: config.sentimentUDF,
      removeSearchBar: config.removeSearchBar,
      predefinedKeywords: config.predefinedKeywords,
      normalizationUpscaleFactor: 1000 * 1000,
      normalizationUpscaleText: "/M",
      sentimentUpperBound: 4,
      cacheThreshold: parseInt(config.cacheThreshold),
      querySliceMills: parseInt(config.querySliceMills),
      getPopulationTarget: function(parameters){
        switch (parameters.geoLevel) {
          case "state":
            return {
              joinKey: ["state"],
              dataset: "twitter.dsStatePopulation",
              lookupKey: ["stateID"],
              select: ["population"],
              as: ["population"]
            };
          case "county":
            return {
              joinKey: ["county"],
              dataset: "twitter.dsCountyPopulation",
              lookupKey: ["countyID"],
              select: ["population"],
              as: ["population"]
            };
          case "city":
            return {
              joinKey: ["city"],
              dataset: "twitter.dsCityPopulation",
              lookupKey: ["cityID"],
              select: ["population"],
              as: ["population"]
            };
        }
      }
    };
  })
  .service('cloudberry', function($timeout, cloudberryConfig, MapResultCache) {
    var startDate = config.startDate;
    var endDate = config.endDate;
    var defaultNonSamplingDayRange = 1500;
    var defaultSamplingDayRange = 1;
    var defaultSamplingSize = 10;
    var defaultHeatmapSamplingDayRange = parseInt(config.heatmapSamplingDayRange);
    var defaultHeatmapLimit = parseInt(config.heatmapSamplingLimit);
    var defaultPinmapSamplingDayRange = parseInt(config.pinmapSamplingDayRange);
    var defaultPinmapLimit = parseInt(config.pinmapSamplingLimit);
    var ws = new WebSocket(cloudberryConfig.ws);
    // The MapResultCache.getGeoIdsNotInCache() method returns the geoIds
    // not in the cache for the current query.
    var geoIdsNotInCache = [];

    var countRequest = JSON.stringify({
      dataset: "twitter.ds_tweet",
      global: {
        globalAggregate: {
          field: "*",
          apply: {
            name: "count"
          },
          as: "count"
        }},
      estimable : true,
      transform: {
        wrap: {
          id: "totalCount",
          category: "totalCount"
        }
      }
    });

    function requestLiveCounts() {
      if(ws.readyState === ws.OPEN){
        ws.send(countRequest);
      }
    }
    var myVar = setInterval(requestLiveCounts, 1000);

    function getLevel(level){
      switch(level){
        case "state" : return "stateID";
        case "county" : return "countyID";
        case "city" : return "cityID";
      }
    }

    function getFilter(parameters, maxDay, geoIds) {
      var spatialField = getLevel(parameters.geoLevel);
      var keywords = [];
      for(var i = 0; i < parameters.keywords.length; i++){
        keywords.push(parameters.keywords[i].replace("\"", "").trim());
      }
      var queryStartDate = new Date(parameters.timeInterval.end);
      queryStartDate.setDate(queryStartDate.getDate() - maxDay);
      queryStartDate = parameters.timeInterval.start > queryStartDate ? parameters.timeInterval.start : queryStartDate;

      var filter = [
        {
          field: "create_at",
          relation: "inRange",
          values: [queryStartDate.toISOString(), parameters.timeInterval.end.toISOString()]
        }, {
          field: "text",
          relation: "contains",
          values: keywords
        }
      ];
      if (geoIds.length <= 2000){
        filter.push(
          {
            field: "geo_tag." + spatialField,
            relation: "in",
            values: geoIds
          }
        );
      }
      return filter;
    }

    function byGeoRequest(parameters, geoIds) {
      if (cloudberryConfig.sentimentEnabled) {
        return {
          dataset: parameters.dataset,
          append: [{
            field: "text",
            definition: cloudberryConfig.sentimentUDF,
            type: "Number",
            as: "sentimentScore"
          }],
          filter: getFilter(parameters, defaultNonSamplingDayRange, geoIds),
          group: {
            by: [{
              field: "geo",
              apply: {
                name: "level",
                args: {
                  level: parameters.geoLevel
                }
              },
              as: parameters.geoLevel
            }],
            aggregate: [{
              field: "*",
              apply: {
                name: "count"
              },
              as: "count"
            }, {
              field: "sentimentScore",
              apply: {
                name: "sum"
              },
              as: "sentimentScoreSum"
            }, {
              field: "sentimentScore",
              apply: {
                name: "count"
              },
              as: "sentimentScoreCount"
            }],
            lookup: [
              cloudberryConfig.getPopulationTarget(parameters)
            ]
          }
        };
      } else {
        return {
          dataset: parameters.dataset,
          filter: getFilter(parameters, defaultNonSamplingDayRange, geoIds),
          group: {
            by: [{
              field: "geo",
              apply: {
                name: "level",
                args: {
                  level: parameters.geoLevel
                }
              },
              as: parameters.geoLevel
            }],
            aggregate: [{
              field: "*",
              apply: {
                name: "count"
              },
              as: "count"
            }],
            lookup: [
              cloudberryConfig.getPopulationTarget(parameters)
            ]
          }
        };
      }
    }

    function byTimeRequest(parameters) {
      return {
        dataset: parameters.dataset,
        filter: getFilter(parameters, defaultNonSamplingDayRange, parameters.geoIds),
        group: {
          by: [{
            field: "geo",
            apply: {
              name: "level",
              args: {
                level: parameters.geoLevel,
              }
            },
            as: parameters.geoLevel
          },
          {
            field: "create_at",
            apply: {
              name: "interval",
              args: {
                unit: parameters.timeBin
              }
            },
            as: parameters.timeBin
          }],
          aggregate: [{
            field: "*",
            apply: {
              name: "count"
            },
            as: "count"
          }]
        }
      };
    }

    // Handle byTimeRequest in heatmap and pinmap.
    function handleByTimeRequest(parameters, categoryName) {
      var byTimeRequestquery = byTimeRequest(parameters);
      byTimeRequestquery['option'] = {
        sliceMillis: cloudberryConfig.querySliceMills
      };
      byTimeRequestquery['transform'] = {
        wrap: {
          id: categoryName,
          category: categoryName
        }
      };
      var byTimeRequestJson = JSON.stringify(byTimeRequestquery);

      ws.send(byTimeRequestJson);
    }

    function byHashTagRequest(parameters) {
      return {
        dataset: parameters.dataset,
        filter: getFilter(parameters, defaultNonSamplingDayRange, parameters.geoIds),
        unnest: [{
          hashtags: "tag"
        }],
        group: {
          by: [{
            field: "tag"
          }],
          aggregate: [{
            field: "*",
            apply: {
              name: "count"
            },
            as: "count"
          }]
        },
        select: {
          order: ["-count"],
          limit: 50,
          offset: 0
        }
      };
    }

    // Retrieves time-series histogram result from the byTimeRequest result.
    function getTimeSeriesValues(byTimeRequestResult) {
      const INVALID_VALUE = 0;

      var timeSeriesStore = new HashMap();
      var geoIdSet = new Set(cloudberryService.parameters.geoIds);
      for (var i = 0; i < byTimeRequestResult.length; i++) {
        var currVal = byTimeRequestResult[i];
        var geoIds = currVal[cloudberryService.parameters.geoLevel];
        var values = timeSeriesStore.get(geoIds);
        // First updates the store with geoIds that have results.
        if (values !== undefined && values !== INVALID_VALUE) { // when one geoIds has more than one value
          values.push({day:currVal["day"], count:currVal["count"]});
          timeSeriesStore.set(geoIds, values);
          geoIdSet.delete(currVal[cloudberryService.parameters.geoLevel]);
        } else { // first value of current geoId
          timeSeriesStore.set(geoIds, [{day:currVal["day"], count:currVal["count"]}]);
          geoIdSet.delete(currVal[cloudberryService.parameters.geoLevel]);
        }
      }
      // Mark other results as checked: these are geoIds with no results
      geoIdSet.forEach(function (value) {
        timeSeriesStore.set(value, INVALID_VALUE);
      });

      var timeSeriesResultArray = [];
      for (var j = 0; j < cloudberryService.parameters.geoIds.length; j++) {
        var value = timeSeriesStore.get(cloudberryService.parameters.geoIds[j]);
        if (value !== undefined && value !== INVALID_VALUE) {
          timeSeriesResultArray = timeSeriesResultArray.concat(value);
        }
      }
      return timeSeriesResultArray;
    }

    var cloudberryService = {

      commonTotalCount: 0,
      startDate: startDate,
      parameters: {
        dataset: "twitter.ds_tweet",
        keywords: [],
        timeInterval: {
          start: startDate,
          end: endDate ?  endDate : new Date()
        },
        timeBin : "day",
        geoLevel: "state",
        geoIds : [37,51,24,11,10,34,42,9,44,48,35,4,40,6,20,32,8,49,12,22,28,1,13,45,5,47,21,29,54,17,18,39,19,55,26,27,31,56,41,46,16,30,53,38,25,36,50,33,23,2]
      },

      countmapMapResult: [],
      countmapPartialMapResult: [],
      commonTimeSeriesResult: [],
      commonHashTagResult: [],
      errorMessage: null,

      query: function(parameters) {
      
        if (ws.readyState !== ws.OPEN || typeof(parameters.keywords) === "undefined" || parameters.keywords == null || parameters.keywords.length == 0)
          return;

        // generate query based on map type
        switch (parameters.maptype) {
          case 'countmap':
            var sampleJson = (JSON.stringify({
              dataset: parameters.dataset,
              filter: getFilter(parameters, defaultSamplingDayRange, parameters.geoIds),
              select: {
                order: ["-create_at"],
                limit: defaultSamplingSize,
                offset: 0,
                field: ["create_at", "id", "user.id"]
              },
              transform: {
                wrap: {
                  id: "sample",
                  category: "sample"
                }
              }
            }));

            // Batch request without map result - used when the complete map result cache hit case
            var batchWithoutGeoRequest = cloudberryConfig.querySliceMills > 0 ? (JSON.stringify({
              batch: [byTimeRequest(parameters), byHashTagRequest(parameters)],
              option: {
                sliceMillis: cloudberryConfig.querySliceMills
              },
              transform: {
                wrap: {
                  id: "batchWithoutGeoRequest",
                  category: "batchWithoutGeoRequest"
                }
              }
            })) : (JSON.stringify({
                batch: [byTimeRequest(parameters), byHashTagRequest(parameters)],
                transform: {
                    wrap: {
                        id: "batchWithoutGeoRequest",
                        category: "batchWithoutGeoRequest"
                    }
                }
            }));

            // Gets the Geo IDs that are not in the map result cache.
            geoIdsNotInCache = MapResultCache.getGeoIdsNotInCache(cloudberryService.parameters.keywords,
              cloudberryService.parameters.timeInterval,
              cloudberryService.parameters.geoIds, cloudberryService.parameters.geoLevel);

            // Batch request with only the geoIds whose map result are not cached yet - partial map result cache hit case
            // This case also covers the complete cache miss case.
            var batchWithPartialGeoRequest = cloudberryConfig.querySliceMills > 0 ? (JSON.stringify({
              batch: [byTimeRequest(parameters), byGeoRequest(parameters, geoIdsNotInCache),
                byHashTagRequest(parameters)],
              option: {
                sliceMillis: cloudberryConfig.querySliceMills
              },
              transform: {
                wrap: {
                  id: "batchWithPartialGeoRequest",
                  category: "batchWithPartialGeoRequest"
                }
              }
            })) : (JSON.stringify({
                batch: [byTimeRequest(parameters), byGeoRequest(parameters, geoIdsNotInCache),
                  byHashTagRequest(parameters)],
                transform: {
                    wrap: {
                        id: "batchWithPartialGeoRequest",
                        category: "batchWithPartialGeoRequest"
                    }
                }
            }));

            // Complete map result cache hit case - exclude map result request
            if(geoIdsNotInCache.length === 0)  {
              cloudberryService.countmapMapResult = MapResultCache.getValues(cloudberryService.parameters.geoIds,
                cloudberryService.parameters.geoLevel);

              ws.send(sampleJson);
              ws.send(batchWithoutGeoRequest);
            }
            // Partial map result cache hit case
            else  {
              cloudberryService.countmapPartialMapResult = MapResultCache.getValues(cloudberryService.parameters.geoIds,
                    cloudberryService.parameters.geoLevel);

              ws.send(sampleJson);
              ws.send(batchWithPartialGeoRequest);
            }
            break;
            
          case 'heatmap':
            var heatJson = (JSON.stringify({
              dataset: parameters.dataset,
              filter: getFilter(parameters, defaultHeatmapSamplingDayRange, parameters.geoIds),
              select: {
                order: ["-create_at"],
                limit: defaultHeatmapLimit,
                offset: 0,
                field: ["id", "coordinate", "place.bounding_box", "create_at", "user.id"]
              },
              option: {
                sliceMillis: cloudberryConfig.querySliceMills
              },
              transform: {
                wrap: {
                  id: "heatMapResult",
                  category: "heatMapResult"
                }
              }
            }));

            var heatHashTagJson = JSON.stringify({
              dataset: parameters.dataset,
              filter: getFilter(parameters, defaultNonSamplingDayRange, parameters.geoIds),
              unnest: [{
                hashtags: "tag"
              }],
              group: {
                by: [{
                  field: "tag"
                }],
                aggregate: [{
                  field: "*",
                  apply: {
                    name: "count"
                  },
                  as: "count"
                }]
              },
              select: {
                order: ["-count"],
                limit: 50,
                offset: 0
              },
              transform: {
                wrap: {
                  id: "hashTags",
                  category: "hashTags"
                }
              }
            });

            handleByTimeRequest(parameters, "heatTime");
            ws.send(heatJson);
            ws.send(heatHashTagJson);
            break;

          case 'pinmap':

            var pointsJson = (JSON.stringify({
              dataset: parameters.dataset,
              filter: getFilter(parameters, defaultPinmapSamplingDayRange, parameters.geoIds),
              select: {
                order: ["-create_at"],
                limit: defaultPinmapLimit,
                offset: 0,
                field: ["id", "coordinate", "place.bounding_box", "create_at", "user.id"]
              },
              option: {
                sliceMillis: cloudberryConfig.querySliceMills
              },
              transform: {
                wrap: {
                  id: "points",
                  category: "points"
                }
              }
            }));

            var pinHashTagJson = JSON.stringify({
              dataset: parameters.dataset,
              filter: getFilter(parameters, defaultNonSamplingDayRange, parameters.geoIds),
              unnest: [{
                hashtags: "tag"
              }],
              group: {
                by: [{
                  field: "tag"
                }],
                aggregate: [{
                  field: "*",
                  apply: {
                    name: "count"
                  },
                  as: "count"
                }]
              },
              select: {
                order: ["-count"],
                limit: 50,
                offset: 0
              },
              transform: {
                wrap: {
                  id: "hashTags",
                  category: "hashTags"
                }
              }
            });

            handleByTimeRequest(parameters, "pointsTime");
            ws.send(pointsJson);
            ws.send(pinHashTagJson);
            break;
          
          default:
            // unrecognized map type
            break;
        }
      }
    };

    ws.onmessage = function(event) {
      $timeout(function() {
        var result = JSONbig.parse(event.data);

        switch (result.category) {

          case "sample":
            cloudberryService.commonTweetResult = result.value[0];
            break;
          // Complete cache hit case
          case "batchWithoutGeoRequest":
            if(angular.isArray(result.value)) {
              cloudberryService.commonTimeSeriesResult = getTimeSeriesValues(result.value[0]);
              cloudberryService.commonHashTagResult = result.value[1];
            }
            break;
          // Partial map result cache hit or complete cache miss case
          case "batchWithPartialGeoRequest":
            if(angular.isArray(result.value)) {
              cloudberryService.commonTimeSeriesResult = getTimeSeriesValues(result.value[0]);
              cloudberryService.countmapMapResult = result.value[1].concat(cloudberryService.countmapPartialMapResult);
              cloudberryService.commonHashTagResult = result.value[2];
            }
            // When the query is executed completely, we update the map result cache.
            if((cloudberryConfig.querySliceMills > 0 && !angular.isArray(result.value) &&
                result.value['key'] === "done") || cloudberryConfig.querySliceMills <= 0) {
              MapResultCache.putValues(geoIdsNotInCache, cloudberryService.parameters.geoLevel,
                cloudberryService.countmapMapResult);
            }
            break;
          case "batchHeatMapRequest":
            break;
          case "batchPinMapRequest":
            break;
          case "heatMapResult":
            if(angular.isArray(result.value)) {
              cloudberryService.commonTweetResult = result.value[0].slice(0, defaultSamplingSize - 1);
              cloudberryService.heatmapMapResult = result.value[0];
            }
            break;
          case "heatTime":
            if(angular.isArray(result.value)) {
              cloudberryService.commonTimeSeriesResult = getTimeSeriesValues(result.value[0]);
            }
            break;
          case "points":
            if(angular.isArray(result.value)) {
              cloudberryService.commonTweetResult = result.value[0].slice(0, defaultSamplingSize - 1);
              cloudberryService.pinmapMapResult = result.value[0];
            }
            break;
          case "pointsTime":
            if(angular.isArray(result.value)) {
              cloudberryService.commonTimeSeriesResult = getTimeSeriesValues(result.value[0]);
            }
            break;
          case "hashTags":
            if(angular.isArray(result.value)) {
              cloudberryService.commonHashTagResult = result.value[0];
            }
            break;
          case "totalCount":
            cloudberryService.commonTotalCount = result.value[0][0].count;
            break;
          case "error":
            console.error(result);
            cloudberryService.errorMessage = result.value;
            break;
          case "done":
            break;
          default:
            console.log("ws get unknown data: ", result);
            break;
        }
      });
    };

    return cloudberryService;
  });
