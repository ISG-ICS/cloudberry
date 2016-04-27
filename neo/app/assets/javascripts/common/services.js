angular.module('cloudberry.common', [])
  .service('Asterix', function($http, $timeout) {
    var ws = new WebSocket("ws://localhost:9000/ws");
    var asterixService = {

      parameters: {
        dataset: "twitter",
        keyword: null,
        area: {
          swLog: -46.23046874999999,
          swLat: 53.85252660044951,
          neLog: -146.42578125,
          neLat: 21.453068633086783
        },
        time: {
          start: new Date(2012, 1, 1, 0, 0, 0, 0),
          end: new Date()
        },
        level: "state",
        repeatDuration: 0
      },

      mapResult: {},
      timeResult: {},
      hashTagResult: {},
      errorMessage: null,

      query: function(parameters) {
        var json = (JSON.stringify({
          dataset: parameters.dataset,
          keyword: parameters.keyword,
          area: parameters.area,
          timeRange: {
            start: Date.parse(parameters.time.start),
            end: Date.parse(parameters.time.end)
          },
          level: parameters.level,
          repeatDuration: parameters.repeatDuration
        }));
        ws.send(json);
      }
    };

    ws.onmessage = function(event) {
      $timeout(function() {
        console.log(event.data);
        var result = JSON.parse(event.data);
        switch (result.aggType) {
          case "map":
            asterixService.mapResult = result.result;
            break;
          case "time":
            asterixService.timeResult = result.result;
            break;
          case "hashtag":
            asterixService.hashTagResult = result.result;
            break;
          case "error":
            asterixService.errorMessage = result.errorMessage;
            break;
          default:
            console.log("ws get unknown data: " + result);
            break;
        }
      });
    };

    return asterixService;
  });
