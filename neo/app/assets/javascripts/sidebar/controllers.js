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
  .directive('hashtag', function () {
    return {
      restrict: 'E',
      controller: 'HashTagCtrl',
      template: [
        '<table class="table" id="hashcount">',
          '<thead>',
            '<tr ng-repeat="(k,v) in result"><td># {{k}}</td><br/><td>{{v}}</td></tr>',
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