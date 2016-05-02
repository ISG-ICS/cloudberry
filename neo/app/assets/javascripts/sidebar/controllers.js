angular.module('cloudberry.sidebar', ['cloudberry.common'])
  .controller('HashTagCtrl', function ($scope, $window, Asterix) {
    $scope.result = {};
    $scope.$watch(
      function() {
        return Asterix.hashTagResult;
      },
      function(newResult) {
        $scope.result = newResult;
      }
    );
  })
  .directive('hashTag', function () {
    return {
      restrict: 'E',
      template: [
        '<table class="table" id="hashcount" ng-repeat="(k,v) in result">',
          '<thead>',
            '<tr><td># {{k}}</td><br/><td>{{v}}</td></tr>',
          '</thead>',
        '</table>'
      ].join('')
    };
  })
  .controller('TweetCtrl', function ($scope, $window, Asterix) {
    // TODO
  })
  .directive('tweet', function () {
    // TODO
});