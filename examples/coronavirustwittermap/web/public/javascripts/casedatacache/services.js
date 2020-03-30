/*
 * This module caches geo regions' case data.
 */
'use strict';
angular.module('cloudberry.casedatacache', [])
  .service('caseDataCache', [function () {

    var caseDataCached = {
      state: false,
      county: false
    };

    /**
     * Case data store
     *
     * store hash map of geoID --> [[confirmed], [recovered], [death]].
     * each sub-array contains a list of daily case numbers,
     * e.g. [confirmed] = [{day: "1/22/20", count: 0}, {day: "1/23/20", count: 2}, ...]
     *
     */
    var caseDataStore = {
      state: new HashMap(),
      county: new HashMap()
    };

    this.stateCaseDataCached = function () {
      return caseDataCached.state;
    };
    this.countyCaseDataCached = function () {
      return caseDataCached.county;
    };

    /**
     * Return geoId's case data.
     */
    this.getGeoIdCaseData = function (geoLevel, geoId, start, end) {
      if (caseDataCached[geoLevel]) {
        var cases = caseDataStore[geoLevel].get(geoId);
        // filter the data with start and end
        var result = [[], [], []];
        for (var k = 0; k < 3; k++) {
          var data = cases[k];
          for (var i = 0; i < data.length; i++) {
            if (data[i].day.getDate() >= start.getDate() && data[i].day.getDate() <= end.getDate()) {
              result[k].push(data[i]);
            }
          }
        }
        return result;
      }
      else {
        return undefined;
      }
    };

    this.getDailyTotalCaseCount = function (geoLevel, date) {
      var result = [0, 0];

      if (caseDataCached[geoLevel]) {
        for (let geoId of caseDataStore[geoLevel].keys()) {
          var cases = caseDataStore[geoLevel].get(geoId);
          for (var i = 0; i < cases[0].length; i++) {
            if (cases[0][i].day.getDate() === date.getDate()) {
              result[0] += cases[0][i].count;
              result[1] += cases[1][i].count;
            }
          }
        }
      }
      return result;
    };

    /**
     * Parse csv to case data store
     * @param csv - [[state_id, last_update, confirmed, recovered, death]]
     * @param geoLevel - state / county
     */
    this.loadCsvToCaseDataStore = function (csv, geoLevel) {
      if (csv !== undefined) {
        var data = $.csv.toArrays(csv);
        // skip header
        for (var i = 1; i < data.length; i ++) {
          var tuple = data[i];
          // skip lines without first column
          if (tuple[0] === null || tuple[0] === "") continue;
          var geoId = Number(tuple[0]);
          var last_update = new Date(tuple[1]);
          last_update = new Date(last_update.getTime() + (last_update.getTimezoneOffset() * 60000));
          var confirmed = Number(tuple[2]);
          var recovered = Number(tuple[3]);
          var death = Number(tuple[4]);
          var cases = caseDataStore[geoLevel].get(geoId);
          // cases array of this geoId exists
          if (cases) {
            cases[0].push({day: last_update, count: confirmed});
            cases[1].push({day: last_update, count: recovered});
            cases[2].push({day: last_update, count: death});
          }
          else {
            cases = [
              [{day: last_update, count: confirmed}],
              [{day: last_update, count: recovered}],
              [{day: last_update, count: death}]
            ];
            caseDataStore[geoLevel].set(geoId, cases);
          }
        }
        caseDataCached[geoLevel] = true;
      }
    };
  }]);
