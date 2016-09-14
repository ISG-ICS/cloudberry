angular.module('cloudberry.common', [])
  .service('Asterix', function($http, $timeout, $location) {
    var startDate = new Date(2016, 5, 30, 0, 0, 0, 0);
    var ws = new WebSocket("ws://" + $location.host() + ":" + $location.port() + "/ws");
    var asterixService = {

      parameters: {
        dataset: "twitter.ds_tweet",
        keywords: [],
        timeInterval: {
          start: startDate,
          end: new Date()
        },
        timeBin : "day",
        geoLevel: "state",
        geoIds : [37,51,24,11,10,34,42,9,44,48,35,4,40,6,20,32,8,49,12,22,28,1,13,45,5,47,21,29,54,17,18,39,19,55,26,27,31,56,41,46,16,30,53,38,25,36,50,33,23,2]
      },

      queryType: "search",

      mapResult: [],
      timeResult: [],
      hashTagResult: [],
      errorMessage: null,

      query: function(parameters, queryType) {
        var json = (JSON.stringify({
          dataset: parameters.dataset,
          keywords: parameters.keywords,
          timeInterval: {
            start: queryType==='time' ? Date.parse(parameters.timeInterval.start) : Date.parse(startDate),
            end: queryType==='time' ? Date.parse(parameters.timeInterval.end) : Date.parse(new Date())
          },
          timeBin : parameters.timeBin,
          geoLevel: parameters.geoLevel,
          geoIds : parameters.geoIds
        }));
        ws.send(json);
      }
    };

    ws.onmessage = function(event) {
      $timeout(function() {
        var result = JSONbig.parse(event.data);
        switch (result.key) {
          case "byPlace":
            asterixService.mapResult = result.value;
            break;
          case "byTime":
            asterixService.timeResult = result.value;
            break;
          case "byHashTag":
            asterixService.hashTagResult = result.value;
            break;
          case "sample":
            asterixService.tweetResult = result.value;
            break;
          case "error":
            asterixService.errorMessage = result.value;
            break;
          default:
            console.error("ws get unknown data: " + result);
            break;
        }
      });
    };

    return asterixService;
  });
