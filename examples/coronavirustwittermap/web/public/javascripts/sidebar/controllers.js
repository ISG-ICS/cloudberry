angular.module("cloudberry.sidebar", ["cloudberry.common"])
  .controller("SidebarCtrl", function($scope, $rootScope, $timeout, cloudberry, moduleManager, cloudberryClient, queryUtil, cloudberryConfig, $http) {
    
    // Flag whether current result is outdated
    $scope.isHashTagOutdated = true;
    $scope.isSampleTweetsOutdated = false;
    // Flag whether sidebar tab is open
    $scope.isHashTagOpen = false;
    $scope.isSampleTweetsOpen = false;
    $scope.currentTab = "sampletweetTab";

    // Count the number of times that there's no sample tweets returned from API
    var noSampleTweetsCount = 0;
    // live tweets set
    var liveTweetSet = new Set();
    // live tweets queue
    var liveTweetsQueue = [];
    // queryInterval - Every how many seconds, we send a query to database to retrieve new tweets
    var queryInterval = config.liveTweetQueryInterval? config.liveTweetQueryInterval : 60;
    // query offset - Every time we query database the new tweets,
    //                  how many seconds before now is the query's end time condition
    var queryOffset = config.liveTweetQueryOffset? config.liveTweetQueryOffset : 30;
    // updateRate - Every how many seconds, we popup a tweet from queue and show it on page
    var updateRate = config.liveTweetUpdateRate? config.liveTweetUpdateRate : 2;
    // Store handle returned from window.setInterval function
    var queryLimit = parseInt(queryInterval / updateRate);
    $scope.liveTweetsProducer = {};
    $scope.liveTweetsConsumer = {};
    var timeZoneHoursOffset = ((new Date).getTimezoneOffset()) / 60;
    // Timer for sending query to check whether it can be solved by view
    $scope.timerCheckQuerySolvableByView = null;

    // queryID used to identify a query, which is sent by timer
    $scope.nowQueryID = null;

    //Function for the button for close the sidebar, and change the flags
    $scope.closeRightMenu = function() {
      document.getElementById("sidebar").style.left = "100%";
      $scope.showOrHideSidebar(-1);
    };

  
    $rootScope.$on("CallCloseMethod", function(){
         $scope.closeRightMenu();
    });

    // Function for the button that open the sidebar, and change the flags
    $scope.openRightMenu = function() {
      document.getElementById("sidebar").style.left = "76%";
      $scope.showOrHideSidebar(1);
    };


    function enableHashtagButton() {
      $("#Hashtag").removeClass("disableHashtag");
    }

    function disableHashtagButton() {
      $("#Hashtag").addClass("disableHashtag");
    }

    // Set a timer to sending query to check whether it is solvable, every one second
    $scope.setTimerToCheckQuery = function() {
      var queryToCheck = queryUtil.getHashTagRequest(cloudberry.parameters);

      // Add the queryID for a query in to request
      queryToCheck["transform"] = {
        wrap: {
          id: cloudberry.parameters.keywords.toString(),
          category: "checkQuerySolvableByView"
        }
      };
      $scope.nowQueryID = cloudberry.parameters.keywords.toString();
      $scope.timerCheckQuerySolvableByView = setInterval(function () {
        if (wsCheckQuerySolvableByView.readyState === wsCheckQuerySolvableByView.OPEN) {
          wsCheckQuerySolvableByView.send(JSON.stringify(queryToCheck));
        }
      }, 1000);
    };

    // A WebSocket that send query to Cloudberry, to check whether it is solvable by view
    var wsCheckQuerySolvableByView;
    cloudberryClient.newWebSocket(cloudberryConfig.ws + window.location.host + '/ws/checkQuerySolvableByView').done(function(pws) {
      wsCheckQuerySolvableByView = pws;

      moduleManager.publishEvent(moduleManager.EVENT.WS_CHECK_QUERY_SOLVABLE_BY_VIEW_READY, {});

      // When receiving messages from websocket, check its queryID and result.
      // If queryID is matched and result is true, enable the sidebar button and clear timer.
      wsCheckQuerySolvableByView.onmessage = function(event) {
        $timeout(function() {
          var result = JSON.parse(event.data);
          if (result.id === $scope.nowQueryID && result.value[0]) {
            clearInterval($scope.timerCheckQuerySolvableByView);
            enableHashtagButton();
          }
        });
      };
    });

    function sendHashTagQuery() {
      var hashtagRequest = queryUtil.getHashTagRequest(cloudberry.parameters);
      cloudberryClient.send(hashtagRequest, function(id, resultSet) {
        cloudberry.commonHashTagResult = resultSet[0];
      }, "hashtagRequest");
      $scope.isHashTagOutdated = false;
    }

    function drawTweets(message) {
      var url = "https://api.twitter.com/1/statuses/oembed.json?callback=JSON_CALLBACK&id=" + message["id"];
      $http.jsonp(url).success(function (data) {
        $(data.html).hide().prependTo("#tweet");
        window.setTimeout(function(){
          $("#tweet").children().filter("twitter-widget").first().removeClass("twitter-tweet").hide(0,function(){
            if ($("#loadingAnime").length !== 0) {
              $("#loadingAnime").remove();
            }
          }).slideDown(1000);
        },1000);
      });
    }

    // WebSocket for Live Tweets
    var LTSocket;
    cloudberryClient.newWebSocket(cloudberryConfig.ws + window.location.host + "/ws/liveTweets").done(function (pws) {
        LTSocket = pws;

        /* fetchTweetFromAPI sends a query to twittermap server through websocket
         * to fetch recent tweets for liveTweet feature
         * @param msg{Object}, msg is the query send to twittermap server
         */
        $scope.fetchTweetFromAPI = function (query) {
            if (LTSocket.readyState === LTSocket.OPEN) {
                LTSocket.send(query);
            }
      };

      moduleManager.publishEvent(moduleManager.EVENT.WS_LIVE_TWEETS_READY, {});

      LTSocket.onmessage = function(event){
        let tweets = JSON.parse(event.data);
        for (var i = 0 ; i<tweets.length; i++ )
        {
          if (!liveTweetSet.has(tweets[i]["id"])){
            liveTweetsQueue.push(tweets[i]);
            liveTweetSet.add(tweets[i]["id"]);
          }
        }
        if(liveTweetsQueue.length > 0){
          //draw a tweet immediately when there's new result
          drawTweets(liveTweetsQueue.pop());
        }
      };
    });
    
    function sendLiveTweetsQuery(sampleTweetSize) {
      var centerCoordinate = [cloudberry.parameters.bounds._southWest.lat,cloudberry.parameters.bounds._southWest.lng,cloudberry.parameters.bounds._northEast.lat,cloudberry.parameters.bounds._northEast.lng];
      // Construct time range condition for live tweets query
      var tempDateTime = (new Date(Date.now()));
      // 1. Get NOW considering time zone.
      tempDateTime.setHours(tempDateTime.getHours() - timeZoneHoursOffset);
      // 2. UpperBound = NOW - queryOffset
      tempDateTime.setSeconds(tempDateTime.getSeconds() - queryOffset);
      var timeUpperBound = tempDateTime.toISOString();
      // 3. LowerBound = UpperBound - queryInterval
      tempDateTime.setSeconds(tempDateTime.getSeconds()  - queryInterval);
      var timeLowerBound = tempDateTime.toISOString();
      var sampleTweetsRequest = queryUtil.getSampleTweetsRequest(cloudberry.parameters, timeLowerBound, timeUpperBound, sampleTweetSize);
      if (config.enableLiveTweet) {
        if (cloudberry.parameters.keywords.length === 1 && cloudberry.parameters.keywords[0] === "%") {
          var queryKeyword = config.liveTweetDefaultKeyword;
        }
        else {
          var queryKeyword = cloudberry.parameters.keywords.toString();
        }
        $scope.fetchTweetFromAPI(JSON.stringify({keyword: queryKeyword, location: centerCoordinate}));
        $scope.isSampleTweetsOutdated = false;
      }
      else {
        cloudberryClient.send(sampleTweetsRequest, function(id, resultSet) {
          // new tweets retrieved push back to live tweets queue
          liveTweetsQueue = liveTweetsQueue.concat(resultSet[0]);
          $scope.isSampleTweetsOutdated = false;
          if(liveTweetsQueue.length > 0){
            //draw a tweet immediately when there's new result
            drawTweets(liveTweetsQueue.pop());
          }
        }, "sampleTweetsRequest");
      }
    }
  
    // Constantly checking live tweets queue to draw tweet one by one
    function startLiveTweetsConsumer() {
      $scope.liveTweetsConsumer = window.setInterval(function() {
        if (liveTweetsQueue.length > 0){
          //reset the count since there is result
          noSampleTweetsCount = 0;
          var data = liveTweetsQueue.pop();
          drawTweets(data);
        }
        else {
          noSampleTweetsCount ++;
        }

        if($("#tweet").children().length > 20)
        {
          $("#tweet").children().last().remove();
        }

        if ($("#loadingAnime").length !== 0 && noSampleTweetsCount >= 10) {
          $("#loadingMsg").html("<p>Keyword may be too rare, expecting longer time to see sample tweets</p>")
        }

      }, updateRate * 1000);
    }
    
    function startLiveTweetsProducer() {
      sendLiveTweetsQuery(queryLimit);
      $scope.liveTweetsProducer = window.setInterval(function() {sendLiveTweetsQuery(queryLimit)}, queryInterval * 1000);
    }

    function stopLiveTweets() {
      window.clearInterval($scope.liveTweetsConsumer);
      window.clearInterval($scope.liveTweetsProducer);
    }

    function cleanLiveTweets() {
      liveTweetsQueue = [];
      $("#tweet").html("");
    };

    function handleSidebarQuery(){
      if ($scope.isHashTagOpen && $scope.isHashTagOutdated) {
        sendHashTagQuery();
      }
      if ($scope.isSampleTweetsOpen) {
        stopLiveTweets();
        if ($scope.isSampleTweetsOutdated) {
          cleanLiveTweets();
        }
        //Adding loading animation before the first tweet arrive
        if($("#loadingAnime").length==0) {
          $("#tweet").prepend("<div id='loadingAnime' class='lds-ring'><div></div><div></div><div></div><div></div><p id='loadingMsg'>Loading Tweets</p></div>");
        }
        //reset the count everytime a new event is fired.
        noSampleTweetsCount = 0;
        startLiveTweetsConsumer();
        startLiveTweetsProducer();
      }
    }
  
    $scope.showTab = function(tab) {

      if (tab !== "sampletweetTab") {
        stopLiveTweets();
      }

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
        case "aboutTab":
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
        $scope.isSampleTweetsOpen = false;
        $scope.isHashTagOpen = false;
        stopLiveTweets();
      }
      else {
        $scope.showTab($scope.currentTab);
      }
    };



    function eventHandler(event) {
      $scope.isHashTagOutdated = true;
      $scope.isSampleTweetsOutdated = true;
      liveTweetSet = new Set();
      handleSidebarQuery();
    }

    // When the keywords changed, we need to:
    // 1. clear previous timer 2. close and disable sidebar 3. set a new timer for new keywords
    function keywordsEventHandler(event) {
      if($scope.timerCheckQuerySolvableByView) {
        clearInterval($scope.timerCheckQuerySolvableByView);
      }

      $scope.setTimerToCheckQuery();
      disableHashtagButton();
      if (config.enableLiveTweet) {
        $scope.openRightMenu();
      }
    }


    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, eventHandler);
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, eventHandler);
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, keywordsEventHandler);
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, eventHandler);
  })
  .controller("HashTagCtrl", function ($scope, $window, cloudberry, queryUtil, cloudberryClient, chartUtil) {
    $scope.hashTagsList = null;
    $scope.selectedHashtag = null;

    // TODO - get rid of this watch by doing work inside the callback function in sendHashTagQuery()
    $scope.$watch(
      function () {
        return cloudberry.commonHashTagResult;
      },
      function (newResult) {
        $scope.hashTagsList = newResult;
      }
    );

    // send query of hashtag, and draw the line chart when collapse is expanded
    $("#AllCollapse").on("shown.bs.collapse", function(e) {
      $scope.selectedHashtag = e.target.firstChild.id.substring(7);
      if ($scope.selectedHashtag) {
        // send query to cloudberry
        var hashtagChartDataRequest = queryUtil.getHashTagChartDataRequest(cloudberry.parameters,$scope.selectedHashtag);
        cloudberryClient.send(hashtagChartDataRequest, function(id, resultSet) {
          if (angular.isArray(resultSet)) {
            chartUtil.drawChart(chartUtil.preProcessByMonthResult(resultSet[0]), "myChart" + $scope.selectedHashtag, false, "Tweets Count", false, "month");
          }
        }, "hashtagChartDataRequest");
      }
    });
  })
  .directive("hashtag", function () {
    return {
      restrict: "E",
      controller: "HashTagCtrl",
      template: [
        "<div id=\"AllCollapse\" class=\"hashtagDiv\">" +
        "<div ng-repeat=\"r in hashTagsList | orderBy:\'-count\'\" ng-class-odd=\"'striped'\" class=\"accordion-toggle hashtagEle\" aria-expanded=\"false\" data-toggle=\"collapse\"  data-target=\"#collapse{{r.tag}}\">" +
        "<div class=\"row\"><div class=\"col-xs-8\"><a><span class=\"glyphicon glyphicon-triangle-right\"></span><span class=\"glyphicon glyphicon-triangle-bottom\"></span></a># {{r.tag}}</div><div class=\"col-xs-4\">{{r.count}}</div></div> " +
        "<div id=\"collapse{{r.tag}}\" class=\"collapse hashtagChart\"><canvas id=\"myChart{{r.tag}}\" height=\"130\" ></canvas></div>"+
        "</div>" +
        "</div>"
      ].join('')
    };
  })
  .controller("choosemap", function ($scope, $window, cloudberry, moduleManager) {

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
