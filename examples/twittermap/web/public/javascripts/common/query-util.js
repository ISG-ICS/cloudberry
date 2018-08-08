/**
 * queryUtil - A service of AngularJS
 *  This service provides several common functions for modules to construct a JSON query for Cloudberry.
 */
angular.module("cloudberry.common")
  .service("queryUtil", function(cloudberryConfig) {

    var queryUtil = {

      startDate: config.startDate,
      endDate: config.endDate,
      defaultNonSamplingDayRange: 1500,
      defaultSamplingDayRange: 1,
      defaultSamplingSize: 10,
      defaultHeatmapSamplingDayRange: parseInt(config.heatmapSamplingDayRange),
      defaultHeatmapLimit: parseInt(config.heatmapSamplingLimit),
      defaultPinmapSamplingDayRange: parseInt(config.pinmapSamplingDayRange),
      defaultPinmapLimit: parseInt(config.pinmapSamplingLimit),

      getLevel(level){
        switch(level){
          case "state" : return "stateID";
          case "county" : return "countyID";
          case "city" : return "cityID";
        }
      },

      getFilter(parameters, maxDay, geoIds) {
        var spatialField = queryUtil.getLevel(parameters.geoLevel);
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
      },

      byTimeRequest(parameters, geoIds) {
        return {
          dataset: parameters.dataset,
          filter: queryUtil.getFilter(parameters, queryUtil.defaultNonSamplingDayRange, geoIds),
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
            }, {
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
      },

      byGeoRequest(parameters, geoIds) {
        if (cloudberryConfig.sentimentEnabled) {
          return {
            dataset: parameters.dataset,
            append: [{
              field: "text",
              definition: cloudberryConfig.sentimentUDF,
              type: "Number",
              as: "sentimentScore"
            }],
            filter: queryUtil.getFilter(parameters, queryUtil.defaultNonSamplingDayRange, geoIds),
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
            filter: queryUtil.getFilter(parameters, queryUtil.defaultNonSamplingDayRange, geoIds),
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
      },

      getTimeBarRequest(parameters) {
        return {
          dataset: parameters.dataset,
          filter: queryUtil.getFilter(parameters, queryUtil.defaultNonSamplingDayRange, parameters.geoIds),
          group: {
            by: [{
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
          },
          option: {
            sliceMillis: cloudberryConfig.querySliceMills
          }
        };
      },

      // Generate top 50 hash tag JSON request
      getHashTagRequest(parameters) {
        return {
          dataset: parameters.dataset,
          filter: queryUtil.getFilter(parameters, queryUtil.defaultNonSamplingDayRange, parameters.geoIds),
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
      },

      // Generate latest 10 sample tweet JSON request
      getSampleTweetsRequest(parameters) {
        return {
          dataset: parameters.dataset,
          filter: queryUtil.getFilter(parameters, queryUtil.defaultSamplingDayRange, parameters.geoIds),
          select: {
            order: ["-create_at"],
            limit: queryUtil.defaultSamplingSize,
            offset: 0,
            field: ["create_at", "id", "user.id"]
          }
        };
      }
    };

    return queryUtil;
  });
