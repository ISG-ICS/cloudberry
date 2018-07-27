angular.module("cloudberry.sidebar", ["cloudberry.common"])
  .controller("SidebarCtrl", function($scope, cloudberry, moduleManager, cloudberryClient, queryUtil) {

    // Flag whether current result is outdated
    $scope.isHashTagOutdated = true;
    $scope.isSampleTweetsOutdated = true;

    // Flag whether sidebar tab is open
    $scope.isHashTagOpen = false;
    $scope.isSampleTweetsOpen = false;

    $scope.currentTab = "aboutTab";

    function sendHashTagQuery() {
      var hashtagRequest = queryUtil.getHashTagRequest(cloudberry.parameters);
      cloudberryClient.send(hashtagRequest, function(id, resultSet) {
        cloudberry.commonHashTagResult = resultSet[0];
      }, "hashtagRequest");
      $scope.isHashTagOutdated = false;
    }

    function sendSampleTweetsQuery() {
      var sampleTweetsRequest = queryUtil.getSampleTweetsRequest(cloudberry.parameters);
      cloudberryClient.send(sampleTweetsRequest, function(id, resultSet) {
        cloudberry.commonTweetResult = resultSet[0];
      }, "sampleTweetsRequest");
      $scope.isSampleTweetsOutdated = false;
    }

    function handleSidebarQuery() {

      if ($scope.isHashTagOpen && $scope.isHashTagOutdated) {
        sendHashTagQuery();
      }

      if ($scope.isSampleTweetsOpen && $scope.isSampleTweetsOutdated) {
        sendSampleTweetsQuery();
      }
    }

    $scope.showTab = function(tab) {

      if (tab !== $scope.currentTab) {
        $scope.currentTab = tab;
      }

      switch (tab) {
        case "hashtagTab":
          $scope.isHashTagOpen = true;
          $scope.isSampleTweetsOpen = false;
          break;
        case "sampletweetTab":
          $scope.isSampleTweetsOpen = true;
          $scope.isHashTagOpen = false;
          break;
        case "about":
          $scope.isHashTagOpen = false;
          $scope.isSampleTweetsOpen = false;
          break;
        default:
          break;
      }

      handleSidebarQuery();
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

    function eventHandler(event) {
      $scope.isHashTagOutdated = true;
      $scope.isSampleTweetsOutdated = true;
      handleSidebarQuery();
    }

    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, eventHandler);
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, eventHandler);
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, eventHandler);
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, eventHandler);
  })
  .controller("HashTagCtrl", function ($scope, $window, cloudberry) {
    $scope.hashTagsList = null;
    // TODO - get rid of this watch by doing work inside the callback function in sendHashTagQuery()
    $scope.$watch(
      function () {
        return cloudberry.commonHashTagResult;
      },
      function (newResult) {
        $scope.hashTagsList = newResult;
      }
    );

    // draw the line chart when collapse is expanded
    $('#AllCollapse').on('shown.bs.collapse', function(e) {
      var chartTarget = e.target.firstChild;
      var hashtagName = chartTarget.id.substring(7);
      if(hashtagName){
        console.log(hashtagName);
        var ctx = chartTarget.getContext('2d');
        var myChart = new Chart(ctx, {
          type: 'line',
          data: {
            labels: [1750,1800,1850,1900,1950,1999,2050],
            datasets: [{
              data: [106,107,111,133,221,783,2478],
              label: "Africa",
              borderColor: "#3e95cd",
              fill: false
            }
            ]
          },
          options: {
          }
        });
      }

    });
  })
  .directive("hashtag", function () {
    return {
      restrict: "E",
      controller: "HashTagCtrl",
      template: [
        '<div id="AllCollapse" class="hashtagDiv">' +
        '<div ng-repeat="r in hashTagsList | orderBy:\'-count\'" class="accordion-toggle hashtagEle"  data-toggle="collapse"  data-target="#collapse{{r.tag}}">' +
        '<div class="row"><div class="col-xs-8"># {{r.tag}}</div><div class="col-xs-4">{{r.count}}</div></div> ' +
        '<div id="collapse{{r.tag}}" class="collapse hashtagChart"><canvas id="myChart{{r.tag}}" ></canvas></div>'+
        '</div>' +
        '</div>'
      ].join('')
    };
  })
  .controller("TweetCtrl", function ($scope, $window, $http, cloudberry) {
    $scope.results = {};

    function drawTweets(message) {
      $('#tweet').html("");
      if (message) {
        $.each(message, function (i, d) {
          var url = "https://api.twitter.com/1/statuses/oembed.json?callback=JSON_CALLBACK&id=" + d.id;
          $http.jsonp(url).success(function (data) {
            $("#tweet").append(data.html);
          });
        });
      }
    }

    // TODO - get rid of this watch by doing work inside the callback function in sendSampleTweetsQuery()
    $scope.$watch(
      function () {
        return cloudberry.commonTweetResult;
      },
      function (newResult) {
        $scope.results = newResult;
        drawTweets($scope.results);
      }
    );
  })
  .directive("tweet", function () {
    return {
      restrict: "E",
      controller: "TweetCtrl"
    };
  })

  .controller("choosemap", function ($scope, $window, cloudberry, $rootScope, moduleManager) {

    $scope.result = null;
    cloudberry.parameters.maptype = config.defaultMapType;

    var icon1 = document.getElementById("img1");
    var icon2 = document.getElementById("img2");
    var icon3 = document.getElementById("img3");

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

      if (cloudberry.parameters.maptype !== "countmap") {
        var premaptype = cloudberry.parameters.maptype;
        cloudberry.parameters.maptype = "countmap";
        icon1.src = "/assets/images/aggregation_map.png";
        icon2.src = "/assets/images/heat_map_no_border.png";
        icon3.src = "/assets/images/point_map_no_border.png";
        moduleManager.publishEvent(moduleManager.EVENT.CHANGE_MAP_TYPE,
          {previousMapType: premaptype, currentMapType: cloudberry.parameters.maptype});
      }

    });

    icon2.addEventListener("click", function () {

      if (cloudberry.parameters.maptype !== "heatmap") {
        var premaptype = cloudberry.parameters.maptype;
        cloudberry.parameters.maptype = "heatmap";
        icon1.src = "/assets/images/aggregation_map_no_border.png";
        icon2.src = "/assets/images/heat_map.png";
        icon3.src = "/assets/images/point_map_no_border.png";
        moduleManager.publishEvent(moduleManager.EVENT.CHANGE_MAP_TYPE,
          {previousMapType: premaptype, currentMapType: cloudberry.parameters.maptype});
      }

    });

    icon3.addEventListener("click", function () {

      if (cloudberry.parameters.maptype !== "pinmap") {
        var premaptype = cloudberry.parameters.maptype;
        cloudberry.parameters.maptype = "pinmap";
        icon1.src = "/assets/images/aggregation_map_no_border.png";
        icon2.src = "/assets/images/heat_map_no_border.png";
        icon3.src = "/assets/images/point_map.png";
        moduleManager.publishEvent(moduleManager.EVENT.CHANGE_MAP_TYPE,
          {previousMapType: premaptype, currentMapType: cloudberry.parameters.maptype});
      }

    });

  })

  .directive("mapchoose", function () {
      return {
          restrict: "E",
          controller: "choosemap"
      };
  });
