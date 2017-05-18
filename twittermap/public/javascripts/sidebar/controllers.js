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
    $scope.predefinedSearch = function (keyword) {
      $scope.keyword = keyword;
      if ($scope.keyword && $scope.keyword.trim().length > 0) {
        cloudberry.parameters.keywords = $scope.keyword.trim().split(/\s+/);
        // skip the empty query for now.
        cloudberry.queryType = 'search';
        cloudberry.query(cloudberry.parameters, cloudberry.queryType);
      } else {
        cloudberry.parameters.keywords = [];
      }
    }
  })
  .directive('predefinedKeywords', function (cloudberryConfig) {
    if(cloudberryConfig.removeSearchBar) {
      return {
        restrict: 'E',
        controller: 'PredefinedKeywordsCtrl',
        template: [
          '<table class="table" id="keyword-list">',
          '<thead>',
          '<tr ng-repeat="keyword in predefinedKeywords"><td class="predefined-keywords"><a ng-click="predefinedSearch(keyword)">{{keyword}}</a></td></tr>',
          '</thead>',
          '</table>'
        ].join('')
      };
    }
    else{
      return{
      };
    }
  });