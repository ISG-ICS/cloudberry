var app = angular.module('cloudberry', []);

app.factory('Asterix', function($http, $timeout) {
  var ws = new WebSocket("ws://localhost:9000/ws");
  var asterixService = {

    parameters: {
      dataset: "twitter",
      keyword: null,
      area: {
        swLog: -146,
        swLat: 8,
        neLog: -100,
        neLat: 50
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
        repeatDuration : parameters.repeatDuration
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

app.controller('SearchCtrl', function($scope, $window, Asterix) {
  $scope.init = function(){
    $scope.time = Asterix.parameters.time;
    $scope.area = Asterix.parameters.area;
    $scope.level = Asterix.parameters.level;
  };
  $scope.search = function() {
    if ($scope.dataset)
      Asterix.parameters.dataset = $scope.dataset;
    if ($scope.keyword)
      Asterix.parameters.keyword = $scope.keyword;
    if ($scope.area)
      Asterix.parameters.area = $scope.area;
    if ($scope.time)
      Asterix.parameters.time = $scope.time;
    if ($scope.level)
      Asterix.parameters.level = $scope.level;
    Asterix.query(Asterix.parameters);
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

app.controller('HashTagCtrl', function($scope, $window, Asterix) {
  $scope.result = {};
  $scope.$watch(
    function() {
      return Asterix.hashTagResult;
    },

    function(newResult) {
      $scope.result = newResult;
    }
  );
});

app.controller('ExceptionCtrl', function($scope, $window, Asterix) {
  $scope.$watch(
    function() {
      return Asterix.errorMessage;
    },

    function(newMsg) {
      if (newMsg) $window.alert(newMsg);
      Asterix.errorMessage = null;
    }
  );
});

app.controller('D3Ctrl', function($scope, $http, $timeout, Asterix) {

});
