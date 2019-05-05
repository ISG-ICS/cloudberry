/*
 * This module caches map results data of a query. The common services module communicates with
 * middle ware and the map result cache.
 */
'use strict';
angular.module('cloudberry.heatMapResult', ['cloudberry.common'])
    .service('heatMapResultCache', function () {

        // The key-value store that stores map results of a query.
        var store = [];
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
         * Retrieves map results data from the cache; ignores empty objects
         */
        this.getValues = function (timeInterval) {
            console.log(timeInterval)

            var resultArray = [];
            for (var j = 0; j < store.length; j++) {
                var day = new Date(store[j]["create_at"]);
                //console.log("day is ", day);
                if (day >= timeInterval.start && day <= timeInterval.end) {
                    resultArray.push(store[j]);
                }
            }
            //console.log(resultArray);
            return resultArray;
        };

        /**
         * Updates the store with map result each time the middleware responds to json request.
         */
        this.putValues = function (mapResult) {

            for (var i = 0; i < mapResult.length; i++){
                store.push(mapResult[i])
            }
            //console.log(store);
        };
    });