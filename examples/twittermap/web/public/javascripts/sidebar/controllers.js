angular.module('cloudberry.sidebar', ['cloudberry.common'])
  .controller('SidebarCtrl', function($scope, cloudberry) {
    cloudberry.parameters.isSampleTweetsOpen = false;
    cloudberry.parameters.isHashTagOpen = false;

    $scope.currentTab = "about";
    $scope.hashtag_currentKeywords = cloudberry.parameters.keywords;
    $scope.hashtag_currentGeoLevel = cloudberry.parameters.geoLevel;
    $scope.hashtag_currentTimeInterval = cloudberry.parameters.timeInterval;
    $scope.tweet_currentKeywords = cloudberry.parameters.keywords;
    $scope.tweet_currentGeoLevel = cloudberry.parameters.geoLevel;
    $scope.tweet_currentTimeInterval = cloudberry.parameters.timeInterval;

    $scope.showTab = function(tab) {

      if (tab != $scope.currentTab) {
        $scope.currentTab = tab;

        switch (tab) {
          case "hashtag":
            cloudberry.parameters.isHashTagOpen = true;
            cloudberry.parameters.isSampleTweetsOpen = false;

            if ($scope.hashtag_currentKeywords === cloudberry.parameters.keywords
              && $scope.hashtag_currentGeoLevel === cloudberry.parameters.geoLevel
              && $scope.hashtag_currentTimeInterval === cloudberry.parameters.timeInterval) {
              return;
            }

            $scope.hashtag_currentKeywords = cloudberry.parameters.keywords;
            $scope.hashtag_currentGeoLevel = cloudberry.parameters.geoLevel;
            $scope.hashtag_currentTimeInterval = cloudberry.parameters.timeInterval;

            cloudberry.querySidebar(cloudberry.parameters);
            break;
          case "tweet":
            cloudberry.parameters.isSampleTweetsOpen = true;
            cloudberry.parameters.isHashTagOpen = false;

            if ($scope.tweet_currentKeywords === cloudberry.parameters.keywords
              && $scope.tweet_currentGeoLevel === cloudberry.parameters.geoLevel
              && $scope.tweet_currentTimeInterval === cloudberry.parameters.timeInterval) {
              return;
            }

            $scope.tweet_currentKeywords = cloudberry.parameters.keywords;
            $scope.tweet_currentGeoLevel = cloudberry.parameters.geoLevel;
            $scope.tweet_currentTimeInterval = cloudberry.parameters.timeInterval;

            cloudberry.querySidebar(cloudberry.parameters);
            break;
          default:
            break;
        }
      }
    };

    $scope.showOrHideSidebar = function(click) {
      if (click === -1) {
        cloudberry.parameters.isSampleTweetsOpen = false;
        cloudberry.parameters.isHashTagOpen = false;
      }
      else {
        $scope.showTab($scope.currentTab);
      }
    };
  })
  .controller('HashTagCtrl', function ($scope, $window, cloudberry) {
    $scope.hashTagsList = null;
    $scope.$watch(
      function () {
        return cloudberry.commonHashTagResult;
      },
      function (newResult) {
        $scope.hashTagsList = newResult;

        $scope.hashtag_currentKeywords = cloudberry.parameters.keywords;
        $scope.hashtag_currentGeoLevel = cloudberry.parameters.geoLevel;
        $scope.hashtag_currentTimeInterval = cloudberry.parameters.timeInterval;
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
        '<tr ng-repeat="r in hashTagsList | orderBy:\'-count\'"><td># {{r.tag}}</td><br/><td>{{r.count}}</td></tr>',
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
        return cloudberry.commonTweetResult;
      },
      function (newResult) {
        $scope.results = newResult;
        drawTweets($scope.results);

        $scope.tweet_currentKeywords = cloudberry.parameters.keywords;
        $scope.tweet_currentGeoLevel = cloudberry.parameters.geoLevel;
        $scope.tweet_currentTimeInterval = cloudberry.parameters.timeInterval;
      }
    );
  })
  .directive('tweet', function () {
    return {
      restrict: 'E',
      controller: 'TweetCtrl'
    };
  })

    .controller('choosemap', function ($scope, $window, cloudberry, $rootScope, cloudberryConfig) {

        $scope.result = null;
        cloudberry.parameters.maptype = config.defaultMapType;

        var icon1 = document.getElementById('img1');
        var icon2 = document.getElementById('img2');
        var icon3 = document.getElementById('img3');
        
        switch (cloudberry.parameters.maptype){
          case "countmap":
            icon1.src = "/assets/images/aggregation_map.png";
            icon2.src = "/assets/images/heat_map_no_border.png";
            icon3.src = "/assets/images/point_map_no_border.png";
            break;
            
          case "heatmap":
            icon1.src = "/assets/images/aggregation_map_no_border.png";
            icon2.src = "/assets/images/heat_map.png";
            icon3.src = "/assets/images/point_map_no_border.png";
            break;
            
          case "pinmap":
            icon1.src = "/assets/images/aggregation_map_no_border.png";
            icon2.src = "/assets/images/heat_map_no_border.png";
            icon3.src = "/assets/images/point_map.png";
            break;
            
          default:
            break;
        }

        icon1.addEventListener("click", function () {

            if (cloudberry.parameters.maptype !== 'countmap') {
                var premaptype = cloudberry.parameters.maptype;
                cloudberry.parameters.maptype = 'countmap';
                icon1.src = "/assets/images/aggregation_map.png";
                icon2.src = "/assets/images/heat_map_no_border.png";
                icon3.src = "/assets/images/point_map_no_border.png";
                $rootScope.$emit("maptypeChange", [premaptype, cloudberry.parameters.maptype]);
            }

        });

        icon2.addEventListener("click", function () {

            if (cloudberry.parameters.maptype !== 'heatmap') {
                var premaptype = cloudberry.parameters.maptype;
                cloudberry.parameters.maptype = 'heatmap';
                icon1.src = "/assets/images/aggregation_map_no_border.png";
                icon2.src = "/assets/images/heat_map.png";
                icon3.src = "/assets/images/point_map_no_border.png";
                $rootScope.$emit("maptypeChange", [premaptype, cloudberry.parameters.maptype]);
            }

        });

        icon3.addEventListener("click", function () {

            if (cloudberry.parameters.maptype !== 'pinmap') {
                var premaptype = cloudberry.parameters.maptype;
                cloudberry.parameters.maptype = 'pinmap';
                icon1.src = "/assets/images/aggregation_map_no_border.png";
                icon2.src = "/assets/images/heat_map_no_border.png";
                icon3.src = "/assets/images/point_map.png";
                $rootScope.$emit("maptypeChange", [premaptype, cloudberry.parameters.maptype]);
            }

        });

    })

    .directive('mapchoose', function () {
        return {
            restrict: 'E',
            controller: 'choosemap'
        };
    });
