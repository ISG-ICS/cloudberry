/*
 * This module caches time-series histogram data. The common services module communicates with
 * middle ware and the time-series cache.
 */
'use strict';
angular.module('cloudberry.timeseriescache', [])
    .service('TimeSeriesCache', function () {

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
        // Cached time interval, intermediate or complete byTimeRequest result.
        var cachedTimeRange = {
            start: new Date(),
            end: endDate
        };
        // Time interval after byTimeRequest query is executed completely.
        var completeTimeRange = {
            start: new Date(),
            end: endDate
        };
        const INVALID_VALUE = 0;

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
            if (keywords.toString() != currentKeywords.toString() ||
                timeInterval.start < cachedTimeRange.start ||
                timeInterval.end > cachedTimeRange.end ||
                geoLevel != currentGeoLevel) {
                timeseriesStore.clear();
                currentKeywords = keywords.slice();
                currentGeoLevel = geoLevel;
                completeTimeRange.start = timeInterval.start;
                completeTimeRange.end = timeInterval.end;
                // Set default value which each result is >= start and <= end.
                cachedTimeRange.start = timeInterval.end;
                cachedTimeRange.end = timeInterval.start;

                return geoIds;
            }

            for (var i = 0; i < geoIds.length; i++) {
                if (!timeseriesStore.has(geoIds[i])) {
                    geoIdsNotInCache.push(geoIds[i]);
                }
            }

            return geoIdsNotInCache;
        };

        /**
         * Called when the byTimeRequest query is executed completely,
         * set cached time range to the complete request time range.
         */
        this.setCompleteTimeInterval = function () {
            if (completeTimeRange.start < cachedTimeRange.start) {
                cachedTimeRange.start = completeTimeRange.start; 
            }
            if (completeTimeRange.end > cachedTimeRange.end) {
                cachedTimeRange.end = completeTimeRange.end;
            }
        };

        /**
         * Retrieves time-series data from the cache; ignores empty objects
         */
        this.getTimeSeriesValues = function (geoIds, geoLevel, timeInterval) {
            var resultArray = [];

            for (var i = 0; i < geoIds.length; i++) {
                var values = timeseriesStore.get(geoIds[i]);
                if (values !== undefined && values !== INVALID_VALUE) {
                    for (var j = 0; j < values.length; j++) {
                        var currVal = values[j];
                        var day = new Date(currVal["day"]);
                        if (day >= timeInterval.start && day <= timeInterval.end) {
                            resultArray.push({"day":currVal["day"], "count":currVal["count"]});
                        }
                    }
                }
            }
            return resultArray;
        };

        /**
         * Updates the store with time-series result each time the middleware responds to the json request preloadRequest,
         * returns histogram data.
         */
        this.putTimeSeriesValues = function (geoIds, timeseriesResult) {
            var resultArray = [];
            // In case of cache miss.
            if (geoIds.length !== 0) {
                var store = new HashMap();
                var currentTimeRange = {
                    start: cachedTimeRange.start,
                    end: cachedTimeRange.end
                }
                var geoIdSet = new Set(geoIds);

                for (var i = 0; i < timeseriesResult.length; i++) {
                    var currVal = {day:timeseriesResult[i]["day"], count:timeseriesResult[i]["count"]};
                    resultArray.push(currVal);
                    // Update current time interval.
                    var currDate = new Date(currVal["day"]);
                    if (currDate < currentTimeRange.start) {
                        currentTimeRange.start = currDate;
                    }
                    if (currDate > currentTimeRange.end) {
                        currentTimeRange.end = currDate;
                    }
                    var values = store.get(timeseriesResult[i][currentGeoLevel]);
                    // First updates the store with geoIds that have results.
                    if (values !== undefined && values !== INVALID_VALUE) { // when one geoIds has more than one value
                        values.push(currVal);
                        store.set(timeseriesResult[i][currentGeoLevel], values);
                        geoIdSet.delete(timeseriesResult[i][currentGeoLevel]);
                    } else { // first value of current geoId
                        store.set(timeseriesResult[i][currentGeoLevel], [currVal]);
                        geoIdSet.delete(timeseriesResult[i][currentGeoLevel]);
                    }
                };
                // Mark other results as checked: these are geoIds with no results
                geoIdSet.forEach(function (value) {
                    store.set(value, INVALID_VALUE);
                });

                cachedTimeRange.start = currentTimeRange.start;
                cachedTimeRange.end = currentTimeRange.end;
                timeseriesStore = store;
            }
        
            return resultArray;
        };
    });
