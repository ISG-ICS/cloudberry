angular.module('cloudberry.common', ['cloudberry.mapresultcache', 'cloudberry.timeseriescache'])
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
  .service('cloudberry', function($timeout, cloudberryConfig, MapResultCache, TimeSeriesCache) {
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
    // The TimeSeriesCache.getGeoIdsNotInCache() method returns the geoIds
    // not in the cache for the current query.
    var geoIdsNotInTimeSeriesCache = [];

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

    function byTimeRequest(parameters, geoIds) {
      return {
        dataset: parameters.dataset,
        filter: getFilter(parameters, defaultNonSamplingDayRange, geoIds),
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
      var byTimeRequestquery = byTimeRequest(parameters, geoIdsNotInTimeSeriesCache);
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
      // storage for the subset of time-series result in TimeSeriesCache, with format {day, count}.
      timeSeriesPartialResult: [],
      // storage for the newest byTimeSeries query result, with format {geoId, day, count}.
      timeSeriesQueryResult: [],
      commonHashTagResult: [],
      errorMessage: null,

      query: function(parameters) {
      
        if (ws.readyState !== ws.OPEN || typeof(parameters.keywords) === "undefined" || parameters.keywords == null || parameters.keywords.length == 0)
          return;

        // For time-series histogram, get geoIds not in the TimeSeriesCache,
        // and retrieve cached time-series data from the cache for the current user request.
        geoIdsNotInTimeSeriesCache = TimeSeriesCache.getGeoIdsNotInCache(parameters.keywords,
          parameters.timeInterval, parameters.geoIds, parameters.geoLevel);
        cloudberryService.timeSeriesPartialResult = TimeSeriesCache.getTimeSeriesValues(parameters.geoIds,
            parameters.geoLevel, parameters.timeInterval);

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
              batch: [byTimeRequest(parameters, geoIdsNotInTimeSeriesCache), byHashTagRequest(parameters)],
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
                batch: [byTimeRequest(parameters, geoIdsNotInTimeSeriesCache), byHashTagRequest(parameters)],
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
              batch: [byTimeRequest(parameters, geoIdsNotInTimeSeriesCache), byGeoRequest(parameters, geoIdsNotInCache),
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
                batch: [byTimeRequest(parameters, geoIdsNotInTimeSeriesCache), byGeoRequest(parameters, geoIdsNotInCache),
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
              // Since the middleware returns the query result in multiple steps,
              // cloudberryService.timeSeriesQueryResult stores the current intermediate result.
              cloudberryService.timeSeriesQueryResult = result.value[0];
              // Avoid memory leak.
              result.value[0] = [];
              cloudberryService.commonTimeSeriesResult = TimeSeriesCache.getValuesFromResult(cloudberryService.timeSeriesQueryResult).concat(cloudberryService.timeSeriesPartialResult);
              cloudberryService.commonHashTagResult = result.value[1];
            }
            // When the query is executed completely, we update the time-series cache's time interval.
            if((cloudberryConfig.querySliceMills > 0 && !angular.isArray(result.value) &&
                result.value['key'] === "done") || cloudberryConfig.querySliceMills <= 0) {
              TimeSeriesCache.putTimeSeriesValues(geoIdsNotInTimeSeriesCache, cloudberryService.timeSeriesQueryResult, cloudberryService.parameters.timeInterval);
            }
            break;
          // Partial map result cache hit or complete cache miss case
          case "batchWithPartialGeoRequest":
            if(angular.isArray(result.value)) {
              // Since the middleware returns the query result in multiple steps,
              // cloudberryService.timeSeriesQueryResult stores the current intermediate result.
              cloudberryService.timeSeriesQueryResult = result.value[0];
              // Avoid memory leak.
              result.value[0] = [];
              cloudberryService.commonTimeSeriesResult = TimeSeriesCache.getValuesFromResult(cloudberryService.timeSeriesQueryResult).concat(cloudberryService.timeSeriesPartialResult);
              cloudberryService.countmapMapResult = result.value[1].concat(cloudberryService.countmapPartialMapResult);
              cloudberryService.commonHashTagResult = result.value[2];
            }
            // When the query is executed completely, we update the map result cache and time-series cache's time interval.
            if((cloudberryConfig.querySliceMills > 0 && !angular.isArray(result.value) &&
                result.value['key'] === "done") || cloudberryConfig.querySliceMills <= 0) {
              MapResultCache.putValues(geoIdsNotInCache, cloudberryService.parameters.geoLevel,
                cloudberryService.countmapMapResult);
                TimeSeriesCache.putTimeSeriesValues(geoIdsNotInTimeSeriesCache, cloudberryService.timeSeriesQueryResult, cloudberryService.parameters.timeInterval);
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
              // Since the middleware returns the query result in multiple steps,
              // cloudberryService.timeSeriesQueryResult stores the current intermediate result.
              cloudberryService.timeSeriesQueryResult = result.value[0];
              // Avoid memory leak.
              result.value[0] = [];
              cloudberryService.commonTimeSeriesResult = TimeSeriesCache.getValuesFromResult(cloudberryService.timeSeriesQueryResult).concat(cloudberryService.timeSeriesPartialResult);
            }
            // When the query is executed completely, we update the time-series cache's time interval.
            if((cloudberryConfig.querySliceMills > 0 && !angular.isArray(result.value) &&
                result.value['key'] === "done") || cloudberryConfig.querySliceMills <= 0) {
              TimeSeriesCache.putTimeSeriesValues(geoIdsNotInTimeSeriesCache, cloudberryService.timeSeriesQueryResult, cloudberryService.parameters.timeInterval);
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
              // Since the middleware returns the query result in multiple steps,
              // cloudberryService.timeSeriesQueryResult stores the current intermediate result.
              cloudberryService.timeSeriesQueryResult = result.value[0];
              // Avoid memory leak.
              result.value[0] = [];
              cloudberryService.commonTimeSeriesResult = TimeSeriesCache.getValuesFromResult(cloudberryService.timeSeriesQueryResult).concat(cloudberryService.timeSeriesPartialResult);
            }
            // When the query is executed completely, we update the time-series cache's time interval.
            if((cloudberryConfig.querySliceMills > 0 && !angular.isArray(result.value) &&
                result.value['key'] === "done") || cloudberryConfig.querySliceMills <= 0) {
              TimeSeriesCache.putTimeSeriesValues(geoIdsNotInTimeSeriesCache, cloudberryService.timeSeriesQueryResult, cloudberryService.parameters.timeInterval);
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
