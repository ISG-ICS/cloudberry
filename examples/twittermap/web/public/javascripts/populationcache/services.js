/*
 * This module caches geo regions' population histogram data.
 */
'use strict';
angular.module('cloudberry.populationcache', [])
    .service('PopulationCache', ['$http', function ($http) {
        const INVALID_VALUE = 0;

        // When popCached.state or county is true, all state or county level population is preloaded/cached;
        // when popCached.city is true when the city level population is partically cached
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

        /**
         * Return geoId's population.
         */
        this.getGeoIdPop = function(geoLevel, geoId) {
          return popStore[geoLevel].get(geoId);
        };

        /**
         * Returns the cities' geoIds that are not already cached.
         */
        this.getCitiesNotInCache = function (geoLevel, geoIds) {
            // The length of geoIdsNotInCache is 0 in case of complete cache hit,
            // same length as the geoIds parameter in case of complete cache miss,
            // otherwise in range (0, geoIds.length)
            var geoIdsNotInCache = [];
            if (geoLevel !== "city") {
              return geoIdsNotInCache;
            }
            for (var i = 0; i < geoIds.length; i++) {
                if (!popStore["city"].has(geoIds[i])) {
                    geoIdsNotInCache.push(geoIds[i]);
                }
              }

            return geoIdsNotInCache;
        };

        /**
         * Load and store cities in cityIds' population.
         */
        this.loadCityPopulationToCache = function(cityIds) {
            // Distinguish outer scope with http scope.
            var self = this;
            var deferred = new $.Deferred();

            $http.get("cityPopulation/" + cityIds).success(function (data) {
                // Cache return results in popStore.city.
                self.putPopValues(data, "city");
                deferred.resolve(data);
            }).error(function (data) {
                console.error("Load city population failure");
            });

            return deferred.promise();
        };

        /**
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
    }]);
