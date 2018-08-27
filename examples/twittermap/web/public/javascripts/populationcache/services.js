/*
 * This module caches geo regions' population histogram data.
 */
'use strict';
angular.module('cloudberry.populationcache', ['cloudberry.timeseriescache'])
    .service('PopulationCache', ['$http', 'TimeSeriesCache', function ($http, TimeSeriesCache) {
        const INVALID_VALUE = 0;

        // When popCached.state or county is true, all state or county level population is preloaded/cached;
        // when popCached.city is true, it is partically loaded/cached.
        var popCached = {
            state: false,
            county: false,
            city: false
        }
        var popStore = {
            state: new HashMap(),
            county: new HashMap(),
            city: new HashMap()
        }

         this.statePopulationCached = function(){return popCached.state;};
         this.countyPopulationCached = function(){return popCached.county;};
         //this.cityPopulationCached = function(){return popCached.city;};

        /*
         * Put result in population store in {geoID, population} form.
         */
        this.putPopValues = function (data, geoLevel) {
            if (data !== undefined) {
                var store = popStore[geoLevel];
                for (var i = 0; i < data.length; i++) {
                  store.set(data[i][geoLevel+'ID'], data[i]['population']);
                }
                    popStore[geoLevel] = store;
                    popCached[geoLevel] = true;
            }
        };

        /*
         * Load and store cities in cityIds' population.
         */
        this.loadCityPopulationToCache = function(cityIds) {
            // Distinguish outer scope with http scope.
            var self = this;

            $http.get("cityPopulation/" + cityIds).success(function (data) {
                // Cache return results in popStore.city.
                self.putPopValues(data, "city");
            }).error(function (data) {
                console.error("Load city population failure");
            });
        };

        /*
         * Accumulate and return map result data {geoID, count, population} from
         * byGeoTimeRequest sliced result (timeseriesPartialStore) and byGeoTimeRequest cache {day, count}.
         */
        this.getCountMapValues = function (geoIds, geoLevel, timeInterval, timeseriesPartialStore) {
             var resultArray = [];

            for (var i = 0; i < geoIds.length; i++) {
                // Cache hit case: geoID's byGeoTimeRequest results in time series cache case.
                var {values, count} = TimeSeriesCache.getGeoRegionValues(geoIds[i], timeInterval);
                // Cache miss case: geoID's byGeoTimeRequest results in new request sliced result case.
                if (values === INVALID_VALUE && timeseriesPartialStore.has(geoIds[i])) {
                    values = timeseriesPartialStore.get(geoIds[i]);
                    count = this.getCurrentSum(values, timeInterval);
                }
                if (values !== undefined && values !== INVALID_VALUE) {
                    var population = popStore[geoLevel].get(geoIds[i]);
                    switch (geoLevel){
                      case "state":
                        resultArray.push({"state":geoIds[i], "count":count, "population":population});
                      case "county":
                        resultArray.push({"county":geoIds[i], "count":count, "population":population});
                      case "city":
                        resultArray.push({"city":geoIds[i], "count":count, "population":population});
                    }
                }
            }

            return resultArray;
        };

        /*
         * Return sum all "count" in values with variables {day, count}.
         */
        this.getCurrentSum = function(values, timeInterval) {
            var sum = 0;
            for (var j = 0; j < values.length; j++) {
                var currVal = values[j];
                var day = new Date(currVal["day"]);
                if (day >= timeInterval.start && day <= timeInterval.end) {
                    sum += currVal["count"];
                }
            }

            return sum;
        };
    }]);
