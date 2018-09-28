angular.module("cloudberry.sidebar", ["cloudberry.common"])
  .controller("SidebarCtrl", function($scope, cloudberry, moduleManager, cloudberryClient, queryUtil, $http) {

    // Flag whether current result is outdated
    $scope.isHashTagOutdated = true;
    $scope.isSampleTweetsOutdated = true;
    $scope.sampleTweets = [];
    var timeRange = 3; // Set leng of time interval
    var sendQueryLoop = {}; //Store variable for window.setInterval function, keep only one setInterval function avtive at a time
    $scope.liveTweetsLoop = {};//Store variable for winddow.setInterval function, stop live tweets feeding when user specified a 
                            //time interval in time bar
    var timeSeriesEnd = new Date(cloudberry.parameters.timeInterval.end);// This date will be the latest date of tweets been ingested
    var timeZoneOffset = ((new Date).getTimezoneOffset())/60;
    timeSeriesEnd.setHours(timeSeriesEnd.getHours()-timeZoneOffset);//consider the timezone, in order to get live tweets work in any circumstance
    var timeUpperBound = timeSeriesEnd.toISOString(); //Upper time limit of live tweets, lower bound< create time of tweets < upper bound 
    var startDate = new Date(Date.now());
    startDate.setDate(startDate.getDate() - 1);
    var timeLowerBound = startDate.toISOString(); //lower bound of live tweets, the first lower bound will be current time - 1 day, to ensure there at least some contents

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

    function sendSampleTweetsQuery(timeLowerBound,timeUpperBound) {
      var parameters = cloudberry.parameters;
      var sampleTweetsRequest = queryUtil.getSampleTweetsRequest(cloudberry.parameters,timeLowerBound,timeUpperBound);
      cloudberryClient.send(sampleTweetsRequest, function(id, resultSet) {
          $scope.sampleTweets = $scope.sampleTweets.concat(resultSet[0]);//oldest tweet will be at front
      }, "sampleTweetsRequest");
      $scope.isSampleTweetsOutdated = false;
    }

    function handleSidebarQuery(){  

     
      var timeBarMin = new Date(cloudberry.parameters.timeInterval.start);//user specified time series start
      var timeBarMax = new Date(cloudberry.parameters.timeInterval.end);//user specified time series end
      
      
      if ($scope.isHashTagOpen && $scope.isHashTagOutdated) {
        sendHashTagQuery();
      }
      
      if ($scope.isSampleTweetsOpen && $scope.isSampleTweetsOutdated) {
        //Do traditional sample tweets,when user specifed time interval, and the end of time interval is older than latest tweet
        if(timeBarMax<timeSeriesEnd){
          //Clear both query and updating loop of live Tweets
          $scope.cleanLiveTweet();
          window.clearInterval(sendQueryLoop);
          sendSampleTweetsQuery(timeBarMin.toISOString(),timeBarMax.toISOString());
          $scope.$watch(function(){
            return $scope.sampleTweets;
          },function(result){
            $.each($scope.sampleTweets, function (i, d) {
            var url = "https://api.twitter.com/1/statuses/oembed.json?callback=JSON_CALLBACK&id=" + d.id;
            $http.jsonp(url).success(function (data) {
                $("#tweet").append(data.html);
              });
            });
          });
        }
        else{
          window.clearInterval(sendQueryLoop);
          $scope.sampleTweets = [];//Clean the queue for old event;
          var tempDateTime = (new Date(Date.now()));
          tempDateTime.setHours(tempDateTime.getHours()-timeZoneOffset);
          timeUpperBound = tempDateTime.toISOString();
          tempDateTime.setDate(tempDateTime.getDate()-1);//Send first query retrieve lastest 1 day tweets
          timeLowerBound = tempDateTime.toISOString();
          sendSampleTweetsQuery(timeLowerBound,timeUpperBound,10);
          sendQueryLoop = window.setInterval(function(){
            //Update time range of live tweets to avoid get repetitive tweets
            var tempDateTime = (new Date(Date.now()));
            tempDateTime.setHours(tempDateTime.getHours()-timeZoneOffset);
            timeUpperBound = tempDateTime.toISOString();
            tempDateTime.setSeconds(tempDateTime.getSeconds()-timeRange);
            timeLowerBound = tempDateTime.toISOString();
            sendSampleTweetsQuery(timeLowerBound,timeUpperBound,1);
          },timeRange*1000);//send query every second
          $scope.cleanLiveTweet();
          $scope.startLiveTweet();

        }
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
        window.clearInterval(sendQueryLoop); // Stop send query when sidebar is close
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
  })
  .directive("hashtag", function () {
    return {
      restrict: "E",
      controller: "HashTagCtrl",
      template: [
        '<table class="table" id="hashcount">',
        '<thead>',
        '<tr ng-repeat="r in hashTagsList | orderBy:\'-count\'"><td># {{r.tag}}</td><br/><td>{{r.count}}</td></tr>',
        '</thead>',
        '</table>'
      ].join('')
    };
  })
  .controller("TweetCtrl", function ($scope, $window, $http, cloudberry) {
    $scope.results = {};
    function drawTweets(message) {           
      var url = "https://api.twitter.com/1/statuses/oembed.json?callback=JSON_CALLBACK&id=" + message["id"];
      $http.jsonp(url).success(function (data) {
        var object = $(data.html);
        $("#tweet").prepend(data.html);
      });   
    }

    // TODO - get rid of this watch by doing work inside the callback function in sendSampleTweetsQuery()
  
    //Constantly checking local tweets queue to draw tweet one by one
    $scope.startLiveTweet = function startLiveTweet(){        
        $scope.liveTweetsLoop = window.setInterval(function(){
          if($scope.sampleTweets.length>0){
            var data = $scope.sampleTweets.pop();
            drawTweets(data);
          }
          if($("#tweet").children().length>9)
          {
            $("#tweet").children().last().remove();
          }
        },3000);
    };
    
    $scope.cleanLiveTweet = function cleanLiveTweet()
    {
      window.clearInterval($scope.liveTweetsLoop);
      $("#tweet").html("");//clean tweets in sidebar
      $scope.sampleTweets = [];//clean cached data
    };
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
