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
            '<tr ng-repeat="r in result"><td># {{r.key}}</td><br/><td>{{r.count}}</td></tr>',
          '</thead>',
        '</table>'
      ].join('')
    };
  })
  .controller('TweetCtrl', function ($scope, $window, $http, Asterix) {
    $scope.results = {};

    function drawTweets(message) {
      $('#tweet').html('');
      $.each(message, function (i, d) {
        var url = "/tweet/" + d.tid;
        $.ajax({
          url: url,
          dataType: "json",
          success: function (data) {
            $('#tweet').append(data.html);
          },
          error: function(data) {
            // do nothing
          }
        });
      });
    }
    $scope.$watch(
      function() {
        return Asterix.tweetResult;
      },
      function(newResult) {
        $scope.results = newResult;
        if($scope.results && Object.keys($scope.results).length != 0)
          drawTweets($scope.results);
      }
    );
  })
  .directive('tweet', function () {
    return {
      restrict: 'E',
      controller: 'TweetCtrl'
    };
});