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
          }
        ];

        // if keywords only contains wildcard %, get rid of text filter
        if (keywords.length === 1 && keywords[0] === "%") {
        }
        else {
          filter.push(
            {
              field: "text",
              relation: "contains",
              values: keywords
            }
          );
        }

        if (geoIds.length <= 2000) {
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

      byGeoTimeRequest(parameters, geoIds) {
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

      getTimeBarRequest(parameters, geoIds) {
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

      // Get the tendency chart data for a specific hash tag
      getHashTagChartDataRequest(parameters, hashtagName) {
        var filter = queryUtil.getFilter(parameters, queryUtil.defaultNonSamplingDayRange, parameters.geoIds);
        filter.push({
          field: "tag",
          relation: "matches",
          values: [hashtagName]
        });

        return {
          dataset: parameters.dataset,
          filter: filter,
          unnest: [{
            hashtags: "tag"
          }],
          group: {
            by: [
              {
              field: "create_at",
              apply: {
                name: "interval",
                args: {
                  unit: "month"
                }
              },
              as: "month"
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

      // Generate latest 10 sample tweet JSON request
      getSampleTweetsRequest(parameters, timeLowerBound, timeUpperBound, sampleSize = queryUtil.defaultSamplingSize) {
        var spatialField = queryUtil.getLevel(parameters.geoLevel);
        var keywords = [];
        for(var i = 0; i < parameters.keywords.length; i++){
          keywords.push(parameters.keywords[i].replace("\"", "").trim());
        }
        var geoIds = parameters.geoIds;
        var filter = [
          {
            field: "create_at",
            relation: "inRange",
            values: [timeLowerBound, timeUpperBound]
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
        return {
          dataset: parameters.dataset,
          filter,
          select: {
            order: ["-create_at"],
            limit: sampleSize,
            offset: 0,
            field: ["create_at", "id", "user.id"]
          }
        };
      }
    };

    return queryUtil;
  });
