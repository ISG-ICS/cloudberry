angular.module("cloudberry.sidebar", ["cloudberry.common"])
  .controller("SidebarCtrl", function($scope, cloudberry, moduleManager, cloudberryClient, queryUtil) {

    // Flag whether current result is outdated
    $scope.isHashTagOutdated = true;
    $scope.isSampleTweetsOutdated = true;
    $scope.sampleTweets = [];
    var sendQueryLoop = {}; //Store variable for window.setInterval function, keep only one setInterval function avtive at a time
    // Flag whether sidebar tab is open
    $scope.isHashTagOpen = false;
    $scope.isSampleTweetsOpen = false;

    $scope.currentTab = "aboutTab";
    
    // Using this function to get current date time in cloudberry query format
    function getCurrentDateTime(reduceDays=0){
      var d = new Date();
      var month = "";
      var day = "";
      var hours = "";
      var minutes = "";
      var seconds = "";
      
      if(d.getHours()<10){
        hours = "0"+d.getHours();
      }
      else{
        hours = d.getHours();
      }
      
      if(d.getMinutes()<10){
        minutes = "0"+d.getMinutes();
      }
      else{
        minutes = d.getMinutes();
      }
      
      if(d.getSeconds()<10){
        seconds = "0"+d.getSeconds();
      }
      else{
        seconds = d.getSeconds();
      }
      
      if((d.getMonth()+1)<10){
        month = "0"+(d.getMonth()+1);
      }
      else{
        moth = d.getMonth();
      }
      
      if(d.getDate()<10){
        day = "0"+(d.getDate()-reduceDays);
      }
      else{
        day = (d.getDate()-reduceDays);
      }

      return d.getFullYear()+"-"+month+"-"+day+"T"+hours+":"+minutes+":"+seconds+"."+d.getMilliseconds()+"Z";
    }
    
    var timeLowerBound = getCurrentDateTime(7); //lower bound of live tweets, the first lower bound will be current time - 7days, to ensure there at least some contents
    var timeUpperBound = getCurrentDateTime(); //Upper time limit of live tweets, lower bound< create time of tweets < upper bound 
        
    function sendHashTagQuery() {
      var hashtagRequest = queryUtil.getHashTagRequest(cloudberry.parameters);
      cloudberryClient.send(hashtagRequest, function(id, resultSet) {
        cloudberry.commonHashTagResult = resultSet[0];
      }, "hashtagRequest");
      $scope.isHashTagOutdated = false;
    }

    function sendSampleTweetsQuery(offset=0) {
      var parameters = cloudberry.parameters;
      var sampleTweetsRequest = {
          dataset: parameters.dataset,
          filter: [{
            "field": "text",
            "relation": "contains",
            "values": [$scope.keyword]
          },
          {
            "field": "create_at",
            "relation": "inRange",
            "values": [ timeLowerBound, timeUpperBound]
          }
          ],
          select: {
            order: ["-create_at"],
            limit: 10,
            offset: 0,
            field: ["create_at", "id", "user.id"]
          }
        };
      cloudberryClient.send(sampleTweetsRequest, function(id, resultSet) {
        $scope.sampleTweets = $scope.sampleTweets.concat(resultSet[0]);//oldest tweet will be at front
      }, "sampleTweetsRequest");
      $scope.isSampleTweetsOutdated = false;
    }

    function handleSidebarQuery(){
      if ($scope.isHashTagOpen && $scope.isHashTagOutdated) {
        sendHashTagQuery();
      }
    }
  
    $scope.$watch(function(){
        return $scope.keyword;
    },function(newResult){ 
      if ($scope.isSampleTweetsOpen && $scope.isSampleTweetsOutdated) {
        window.clearInterval(sendQueryLoop);
        $scope.sampleTweets = [];//Clean the queue for old keyword;
        sendSampleTweetsQuery();
        sendQueryLoop = window.setInterval(function(){
          //Update time range of live tweets to avoid get repetitive tweets
          timeLowerBound = timeUpperBound;
          timeUpperBound = getCurrentDateTime();
          sendSampleTweetsQuery();
        },30000);
      }  
    });

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
    function startLiveTweet(){        
        window.setInterval(function(){
          if($scope.sampleTweets.length>0){
            var data = $scope.sampleTweets.pop();
            drawTweets(data);
          }
          if($('#tweet').children().length>9)
          {
            $('#tweet').children().last().remove();
          }
        },3000);
    }
  
    startLiveTweet();
    
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
