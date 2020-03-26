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
     * store hash map of geoID --> [[confirmed], [death]].
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
        if (!cases) {
          cases = [[], []]
        }
        // filter the data with start and end
        var result = [[], []];
        for (var k = 0; k < 2; k++) {
          var data = cases[k];
          for (var i = 0; i < data.length; i++) {
            if (data[i].day >= start && data[i].day <= end) {
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

    /**
     * Parse csv to case data store
     * @param csv - [[state_id, last_update, confirmed, new_confirmed, deaths, new_deaths, fatality_rate]]
     * @param geoLevel - state / county
     */
    this.loadCsvToCaseDataStore = function (csv, geoLevel) {
      if (csv) {
        var data = $.csv.toArrays(csv);
        // skip header
        for (var i = 1; i < data.length; i ++) {
          var tuple = data[i];
          // skip lines without first column
          if (tuple[0] === null || tuple[0] === "") continue;
          var geoId = Number(tuple[0]);
          var last_update = new Date(tuple[1]);
          var confirmed = Number(tuple[2]);
          var death = Number(tuple[4]);
          var cases = caseDataStore[geoLevel].get(geoId);
          // cases array of this geoId exists
          if (cases) {
            cases[0].push({day: last_update, count: confirmed});
            cases[1].push({day: last_update, count: death});
          }
          else {
            cases = [
              [{day: last_update, count: confirmed}],
              [{day: last_update, count: death}]
            ];
            caseDataStore[geoLevel].set(geoId, cases);
          }
        }
        caseDataCached[geoLevel] = true;
      }
    };
  }]);
