angular.module('cloudberry.sidebar', ['cloudberry.common'])
  .controller('HashTagCtrl', function ($scope, $window, Asterix) {
    $scope.result = null;
    $scope.$watch(
      function () {
        return Asterix.hashTagResult;
      },
      function (newResult) {
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
        '<tr ng-repeat="r in result | orderBy:\'-count\'"><td># {{r.tag}}</td><br/><td>{{r.count}}</td></tr>',
        '</thead>',
        '</table>'
      ].join('')
    };
  })
  .controller('TweetCtrl', function ($scope, $window, $http, Asterix) {
    $scope.results = {};

    function drawTweets(message) {
      $('#tweet').html('');
      if (message) {
        $.each(message, function (i, d) {
          var url = "https://api.twitter.com/1/statuses/oembed.json?callback=JSON_CALLBACK&id=" + d.id;
          $http.jsonp(url).success(function (data) {
            $('#tweet').append(data.html);
          });
        });
      }
    }

    $scope.$watch(
      function () {
        return Asterix.tweetResult;
      },
      function (newResult) {
        $scope.results = newResult;
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
