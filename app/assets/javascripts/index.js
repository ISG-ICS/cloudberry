var app = angular.module('cloudberry', []);

app.factory('Asterix', function($http, $timeout) {
  var ws = new WebSocket("ws://localhost:9000/ws");
  var asterixService = {

    level: "state",

    mapBoundary: {},
    timeRange: {},
    keyword: null,

    mapResult: {},
    timeResult: {},
    hashTagResult: {},

    query: function(keyword, mapBoundary, timeRange) {
      //TODO compose the query
      var query = null;
      ws.send(JSON.stringify({
        keyword: keyword,
        mapBoundary: mapBoundary,
        timeRange: timeRange
      }));
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
        case "hashTag":
          asterixService.hashTagResult = result.result;
          break;
        default:
          console.log("ws get unknown data: " + result);
          break;
      }
    });
  };

  return asterixService;
});

app.controller('SearchCtrl', function($scope, $window, Asterix) {
  $scope.search = function() {
    //    Asterix.keyword = $scope.query.trim().split(/\s+/)
    Asterix.keyword = $scope.query
    Asterix.query(Asterix.keyword, Asterix.mapBoundary, Asterix.timeRange);
  };
});

app.controller('MapCtrl', function($scope, $window, Asterix) {
  $scope.result = {};
  $scope.$watch(
    function() {
      return Asterix.mapResult;
    },

    function(newResult) {
      $scope.result = newResult;
    }
  );
});

app.controller('TimeCtrl', function($scope, $window, Asterix) {
  $scope.result = {};
  $scope.$watch(
    function() {
      return Asterix.timeResult;
    },

    function(newResult) {
      $scope.result = newResult;
    }
  );
});

app.controller('D3Ctrl', function($scope, $http, $timeout, Asterix) {

});
