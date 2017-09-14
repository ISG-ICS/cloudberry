/*
 * Module to cache map result data. Common services module communicates with
 * middleware and updates resultcache module.
 */
'use strict';
angular.module('cloudberry.mapresultcache', ['cloudberry.common'])
  .service('MapResultCache', function () {

    // The key-value store that stores mapResults
    var store = new HashMap();
    // To check if keyword in query changed
    var currentKeywords = [""];
    // To check if time range in query changed
    var date = new Date();
    date.setDate(date.getDate() - 1);
    var currentTimeRange = {
      start: new Date(),
      end: date
    };
    // Prefix for geoIds to make key unique
    var prefix = Object.freeze({
      state: 'S',
      county: 'C',
      city: 'I'
    });
    const INVALID_VALUE = 0;

    // Check keyword, time range and the cache store
    // Return the geoIds not in store
    this.getGeoIdsNotInCache = function (keywords, timeInterval, geoIds, geoLevel) {
      // Length of geoIdsNotInCache is 0 in case of complete cache hit,
      // same length as geoIds in case of complete cache miss,
      // otherwise in range (0, geoIds.length)
      var geoIdsNotInCache = [];

      if(keywords.toString() != currentKeywords.toString() ||
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

    // Retrieve mapResults from cache; ignore empty objects
    this.getValues = function (geoIds, geoLevel) {
      var resultArray = [];

      for (var i = 0; i < geoIds.length; i++) {
        var value = store.get(prefix[geoLevel] + geoIds[i]);
        if(value !== INVALID_VALUE) {
            resultArray.push(value);
        }
      }

      return resultArray;
    };

    // Update store with mapResult each time middleware responds to batchJson
    // or batchWithPartialGeoRequest query
    this.putValues = function (geoIds, geoLevel, mapResult) {
      var geoIdSet = new Set(geoIds);

      // First update store with geoIds that have results
      for (var i = 0; i < mapResult.length; i++) {
        store.set(prefix[geoLevel] + mapResult[i][geoLevel], mapResult[i]);
        geoIdSet.delete(mapResult[i][geoLevel]);
      }
      // Mark other results as checked: these are geoIds with no results
      geoIdSet.forEach(function(value)  {
        store.set(prefix[geoLevel] + value, INVALID_VALUE);
      });
    };
  });