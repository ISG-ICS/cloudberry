/*
 * This module caches map results data of a query. The common services module communicates with
 * middle ware and the map result cache.
 */
'use strict';
angular.module('cloudberry.mapresultcache', ['cloudberry.common'])
    .service('MapResultCache', function () {

        // The key-value store that stores map results of a query.
        var store = new HashMap();
        // To check if keyword in query is changed (new query?)
        var currentKeywords = [""];
        // To check if time range in query changed (new query?)
        var endDate = new Date();
        // Deducts -1 to create a default value that can't exist
        endDate.setDate(endDate.getDate() - 1);
        var currentTimeRange = {
            start: new Date(),
            end: endDate
        };
        // Prefix for geoIds to make each key unique
        var prefix = Object.freeze({
            state: 'S',
            county: 'C',
            city: 'I'
        });
        const INVALID_VALUE = 0;

        /**
         * Checks keyword, time range and the cache store and returns the geoIds that
         * are not already cached.
         */
        this.getGeoIdsNotInCache = function (keywords, timeInterval, geoIds, geoLevel) {
            // The length of geoIdsNotInCache is 0 in case of complete cache hit,
            // same length as the geoIds parameter in case of complete cache miss,
            // otherwise in range (0, geoIds.length)
            var geoIdsNotInCache = [];

            // New query case
            if (keywords.toString() != currentKeywords.toString() ||
                !angular.equals(currentTimeRange, timeInterval)) {
                store.clear();
                currentKeywords = keywords.slice();
                currentTimeRange.start = timeInterval.start;
                currentTimeRange.end = timeInterval.end;
            }

            for (var i = 0; i < geoIds.length; i++) {
                if (!store.has(prefix[geoLevel] + geoIds[i])) {
                    geoIdsNotInCache.push(geoIds[i]);
                }
            }

            return geoIdsNotInCache;
        };

        /**
         * Retrieves map results data from the cache; ignores empty objects
         */
        this.getValues = function (geoIds, geoLevel) {
            var resultArray = [];

            for (var i = 0; i < geoIds.length; i++) {
                var value = store.get(prefix[geoLevel] + geoIds[i]);
                if (value !==  undefined && value !== INVALID_VALUE) {
                    resultArray.push(value);
                }
            }

            return resultArray;
        };

        /**
         * Updates the store with map result each time the middleware responds to json request.
         */
        this.putValues = function (geoIds, geoLevel, mapResult) {
            var geoIdSet = new Set(geoIds);

            // First updates the store with geoIds that have results.
            for (var i = 0; i < mapResult.length; i++) {
                store.set(prefix[geoLevel] + mapResult[i][geoLevel], mapResult[i]);
                geoIdSet.delete(mapResult[i][geoLevel]);
            }
            // Mark other results as checked: these are geoIds with no results
            geoIdSet.forEach(function (value) {
                store.set(prefix[geoLevel] + value, INVALID_VALUE);
            });
        };
    });