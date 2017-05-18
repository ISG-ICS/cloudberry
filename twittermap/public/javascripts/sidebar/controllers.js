angular.module('cloudberry.sidebar', ['cloudberry.common'])
  .controller('HashTagCtrl', function ($scope, $window, cloudberry) {
    $scope.result = null;
    $scope.$watch(
      function () {
        return cloudberry.hashTagResult;
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
  .controller('TweetCtrl', function ($scope, $window, $http, cloudberry) {
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
        return cloudberry.tweetResult;
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
  })
  .controller('PredefinedKeywordsCtrl', function ($scope, cloudberry, cloudberryConfig) {
    $scope.predefinedKeywords = cloudberryConfig.predefinedKeywords;
  })
  .directive('predefinedKeywords', function () {
    return {
      restrict: 'E',
      controller: 'PredefinedKeywordsCtrl',
      template: [
        '<table class="table" id="keyword-list">',
        '<thead>',
        '<tr ng-repeat="keyword in predefinedKeywords"><td>{{keyword}}</td></tr>',
        '</thead>',
        '</table>'
      ].join('')
    };
  });
