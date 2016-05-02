var app = angular.module('cloudberry', ['cloudberry.map','cloudberry.timeseries','cloudberry.sidebar', 'cloudberry.util']);

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
    Asterix.queryType = 'search';
    Asterix.query(Asterix.parameters, Asterix.queryType);
  };
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
