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

    .controller('choosemap', function ($scope, $window, cloudberry, $rootScope) {

        $scope.result = null;
        $scope.maptype = 'countmap';

        var icon1 = document.getElementById('img1');
        var icon2 = document.getElementById('img2');
        var icon3 = document.getElementById('img3');

        icon1.addEventListener("click", function () {

            $rootScope.maptype = 'countmap';
            $rootScope.$emit("maptypeChange", $scope.maptype);

        });

        icon2.addEventListener("click", function () {

            $rootScope.maptype = 'heatmap';
            $rootScope.$emit("maptypeChange", $scope.maptype);

        });

        icon3.addEventListener("click", function () {

            $rootScope.maptype = 'pointmap';
            $rootScope.$emit("maptypeChange", $scope.maptype);

        });

    })

    .directive('mapchoose', function () {
        return {
            restrict: 'E',
            controller: 'choosemap'
        };
    });
