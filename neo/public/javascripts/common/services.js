angular.module('cloudberry.common', [])
  .service('Asterix', function($http, $timeout, $location) {
    var startDate = new Date(2015, 10, 22, 0, 0, 0, 0);
    var ws = new WebSocket("ws://" + $location.host() + ":" + $location.port() + "/ws");
    var asterixService = {

      startDate: startDate,
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
            start:  Date.parse(parameters.timeInterval.start), //: Date.parse(startDate),
            end:  Date.parse(parameters.timeInterval.end) //: Date.parse(new Date())
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
            asterixService.tweetResult = result.value[0];
            break;
          case "batch":
            asterixService.timeResult = result.value[0];
            asterixService.mapResult = result.value[1];
            asterixService.hashTagResult = result.value[2];
            break;
          case "error":
            console.error(result);
            asterixService.errorMessage = result.value;
            break;
          case "done":
            break;
          default:
            console.error("ws get unknown data:" );
            console.error(result);
            break;
        }
      });
    };

    return asterixService;
  });
