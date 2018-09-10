/*
 * This module caches time-series histogram data. The common services module communicates with
 * middle ware and the time-series cache.
 */
'use strict';
angular.module('cloudberry.timeseriescache', ['cloudberry.populationcache'])
    .service('TimeSeriesCache', ['PopulationCache', function (PopulationCache) {

        // The key-value stores time series results of a query.
        var timeseriesStore = new HashMap();
        // To check if keyword in query is changed (new query?)
        var currentKeywords = [""];
        // To check if geo level in query is changed.
        var currentGeoLevel = "";
        // To check if time range in query changed (new query?)
        var endDate = new Date();
        // Deducts -1 to create a default value that can't exist
        endDate.setDate(endDate.getDate() - 1);
        // Cached time interval of complete byTimeRequest result.
        var cachedTimeRange = {
            start: new Date(),
            end: endDate
        };
        const INVALID_VALUE = 0;
        // Maximum geoIds in cache, to avoid reaching mamximum browser memory capacity.
        const MAX_GEOIDS = 3222;

        /**
         * Checks keyword, time range and the cache store, and returns the geoIds that
         * are not already cached.
         */
        this.getGeoIdsNotInCache = function (keywords, timeInterval, geoIds, geoLevel) {
            // The length of geoIdsNotInCache is 0 in case of complete cache hit,
            // same length as the geoIds parameter in case of complete cache miss,
            // otherwise in range (0, geoIds.length)
            var geoIdsNotInCache = [];
            // New query case
            if (keywords.toString() !== currentKeywords.toString() ||
                timeInterval.start < cachedTimeRange.start ||
                timeInterval.end > cachedTimeRange.end ||
                geoLevel !== currentGeoLevel) {
                timeseriesStore.clear();
                currentKeywords = keywords.slice();
                currentGeoLevel = geoLevel;
                cachedTimeRange.start = new Date(timeInterval.start.getTime());
                cachedTimeRange.end = new Date(timeInterval.end.getTime());

                return geoIds;
            } else {
              for (var i = 0; i < geoIds.length; i++) {
                if (!timeseriesStore.has(geoIds[i])) {
                    geoIdsNotInCache.push(geoIds[i]);
                }
              }
            }
            // Clear storage if caching the new query will exceed MAX_GEOIDS limit.
            if (timeseriesStore.count() + geoIdsNotInCache.length > MAX_GEOIDS) {
                timeseriesStore.clear();
                return geoIds;
            } else {
              return geoIdsNotInCache;
            }
        };

        /**
         * Retrieves time-series data from the cache; ignores empty objects.
         */
        this.getTimeSeriesValues = function (geoIds, geoLevel, timeInterval) {
            var resultArray = [];

            for (var i = 0; i < geoIds.length; i++) {
                var values = timeseriesStore.get(geoIds[i]);
                for (var j = 0; (values !== undefined && values !== INVALID_VALUE) && j < values.length; j++) {
                    var day = new Date(values[j]["day"]);
                    if (day >= timeInterval.start && day <= timeInterval.end) {
                        resultArray.push({"day":values[j]["day"], "count":values[j]["count"]});
                    }
                }
            }

            return resultArray;
        };

        /**
         * Return subset of time-series store in the current map view.
         */
        this.getInViewTimeSeriesStore = function (geoIds, timeInterval) {
            var store = new HashMap();

            for (var i = 0; i < geoIds.length; i++) {
                var values = timeseriesStore.get(geoIds[i]);
                var inRangeValues = [];
                for (var j = 0; (values !== undefined && values !== INVALID_VALUE) && j < values.length; j++) {
                    var day = new Date(values[j]["day"]);
                    if (day >= timeInterval.start && day <= timeInterval.end) {
                        inRangeValues.push({"day":values[j]["day"], "count":values[j]["count"]});
                    }
                }
                store.set(geoIds[i], inRangeValues);
            }

            return store;
        };

        /**
         * Return time-series histogram data from byTimeRequest result array.
         */
        this.getValuesFromResult = function (timeseriesResult) {
            var resultArray = [];
            for (var i = 0; i < timeseriesResult.length; i++) {
                var currVal = {day:timeseriesResult[i]["day"], count:timeseriesResult[i]["count"]};
                resultArray.push(currVal);
            }

            return resultArray;
        };

        /**
         * Convert byTimeSeries result array to timeseriesStore HashMap format.
         */
        this.arrayToStore = function (geoIds, timeseriesResult, geoLevel) {
            var store = new HashMap();
            var geoIdSet = new Set(geoIds);

            for (var i = 0; i < timeseriesResult.length; i++) {
                var currVal = {day:timeseriesResult[i]["day"], count:timeseriesResult[i]["count"]};
                var values = store.get(timeseriesResult[i][geoLevel]);
                // First updates the store with geoIds that have results.
                if (values !== undefined && values !== INVALID_VALUE) { // when one geoIds has more than one value
                    values.push(currVal);
                    store.set(timeseriesResult[i][geoLevel], values);
                    geoIdSet.delete(timeseriesResult[i][geoLevel]);
                } else { // first value of current geoId
                    store.set(timeseriesResult[i][geoLevel], [currVal]);
                    geoIdSet.delete(timeseriesResult[i][geoLevel]);
                }
            }
            // Mark other results as checked: these are geoIds with no results
            geoIdSet.forEach(function (value) {
                store.set(value, INVALID_VALUE);
            });

            return store;
        };

        /**
         * Updates the store with time-series result each time the middleware responds to the json request preloadRequest,
         * returns histogram data.
         */
        // TODO: combine geoIds and timeInterval dimensions in the time-series and map-result cache modules.
        this.putTimeSeriesValues = function (geoIds, timeseriesResult, timeInterval) {
            var store = this.arrayToStore(geoIds, timeseriesResult, currentGeoLevel);
            if (timeseriesStore.count() === 0) {
                timeseriesStore = store;
            } else if (timeInterval.start.getTime() === cachedTimeRange.start.getTime() &&
                       timeInterval.end.getTime() === cachedTimeRange.end.getTime()) {
                // Add to cache.
                store.forEach(function(value, key) {
                    timeseriesStore.set(key, value);
                });
            } else {
                // Result is not added to cache because it has a shorter time interval than older cached results.
            }
        };

        /*
         * Accumulate and return map result data {geoID, count, population} from byGeoTimeRequest sliced result.
         */
        this.getCountMapValues = function (geoIds, geoLevel, timeInterval, timeseriesPartialStore) {
            var resultArray = [];

            for (var i = 0; i < geoIds.length; i++) {
                // Cache hit case: geoID's byGeoTimeRequest results in time series cache case.
                var {values, count} = this.getGeoRegionValues(geoIds[i], timeInterval);
                // Cache miss case: geoID's byGeoTimeRequest results in new request sliced result case.
                if (values === INVALID_VALUE && timeseriesPartialStore.has(geoIds[i])) {
                    values = timeseriesPartialStore.get(geoIds[i]);
                    count = this.getCurrentSum(values, timeInterval);
                }
                if (values !== undefined && values !== INVALID_VALUE) {
                    var population = PopulationCache.getGeoIdPop(geoLevel, geoIds[i])
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

        /**
         * For other service to obtain some geoId's values in timeInterval from timeSeriesStore.
         */
        this.getGeoRegionValues = function(geoId, timeInterval) {
            var inRangeValues = [];
            var sum = 0;

            if (timeseriesStore.has(geoId)) {
                var values = timeseriesStore.get(geoId);
                for (var i = 0; (values !== undefined && values !== INVALID_VALUE) && i < values.length; i++) {
                    var day = new Date(values[i]["day"]);
                    if (day >= timeInterval.start && day <= timeInterval.end) {
                        inRangeValues.push(values[i]);
                        sum += values[i]["count"];
                    }
                }

                return {values: inRangeValues, count: sum};
            }
            return {values: INVALID_VALUE, count: sum};
        };

        /**
         * Return sum all "count" in values with variables {day, count}.
         */
        this.getCurrentSum = function(values, timeInterval) {
            var sum = 0;

            if (values === INVALID_VALUE) {
              return 0;
            }
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
