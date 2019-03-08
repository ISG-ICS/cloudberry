angular.module("cloudberry.map")
  .controller("pinMapCtrl", function($rootScope, $scope, $http, cloudberry, cloudberryConfig,
                                     TimeSeriesCache, moduleManager, cloudberryClient, queryUtil) {
    // set map styles for pinmap
    function setPinMapStyle() {
      $scope.setStyles({
        initStyle: {
          weight: 0.5,
          fillOpacity: 0,
          color: "white"
        },
        stateStyle: {
          fillColor: "#f7f7f7",
          weight: 0.5,
          opacity: 1,
          color: "#92d1e1",
          fillOpacity: 0
        },
        stateUpperStyle: {
          fillColor: "#f7f7f7",
          weight: 0.5,
          opacity: 1,
          color: "#92d1e1",
          fillOpacity: 0
        },
        countyStyle: {
          fillColor: "#f7f7f7",
          weight: 0.5,
          opacity: 1,
          color: "#92d1e1",
          fillOpacity: 0
        },
        countyUpperStyle: {
          fillColor: "#f7f7f7",
          weight: 0.5,
          opacity: 1,
          color: "#92d1e1",
          fillOpacity: 0
        },
        cityStyle: {
          fillColor: "#f7f7f7",
          weight: 0.5,
          opacity: 1,
          color: "#92d1e1",
          fillOpacity: 0
        },
        hoverStyle: {
          weight: 0.7,
          color: "#666",
          fillOpacity: 0
        },
        colors: [ "#ffffff", "#92d1e1", "#4393c3", "#2166ac", "#f4a582", "#d6604d", "#b2182b"],
        sentimentColors: ["#ff0000", "#C0C0C0", "#00ff00"]
      });
    }

    $scope.livePinLayer = L.layerGroup();

    // Send query to cloudberry
    function sendPinmapQuery() {

      if (typeof(cloudberry.parameters.keywords) === "undefined"
        || cloudberry.parameters.keywords === null
        || cloudberry.parameters.keywords.length === 0) {
        return;
      }

      // For time-series histogram, get geoIds not in the time series cache.
      $scope.geoIdsNotInTimeSeriesCache = TimeSeriesCache.getGeoIdsNotInCache(cloudberry.parameters.keywords,
        cloudberry.parameters.timeInterval, cloudberry.parameters.geoIds, cloudberry.parameters.geoLevel);

      var pinsJson = {
        dataset: cloudberry.parameters.dataset,
        filter: queryUtil.getFilter(cloudberry.parameters, queryUtil.defaultPinmapSamplingDayRange, cloudberry.parameters.geoIds),
        select: {
          order: ["-create_at"],
          limit: queryUtil.defaultPinmapLimit,
          offset: 0,
          field: ["id", "coordinate", "place.bounding_box", "create_at", "user.id"]
        },
        option: {
          sliceMillis: cloudberryConfig.querySliceMills
        }
      };

      var pinsTimeJson = queryUtil.getTimeBarRequest(cloudberry.parameters, $scope.geoIdsNotInTimeSeriesCache);

      cloudberryClient.send(pinsJson, function(id, resultSet, resultTimeInterval){
        if(angular.isArray(resultSet)) {
          cloudberry.commonTweetResult = resultSet[0].slice(0, queryUtil.defaultSamplingSize - 1);
          cloudberry.pinmapMapResult = resultSet[0];
        }
      }, "pinMapResult");

      // Complete time series cache hit case - exclude time series request
      if($scope.geoIdsNotInTimeSeriesCache.length === 0) {
        cloudberry.commonTimeSeriesResult = TimeSeriesCache.getTimeSeriesValues(cloudberry.parameters.geoIds, cloudberry.parameters.geoLevel, cloudberry.parameters.timeInterval);
      }
      // Partial time series cache hit case
      else {
        cloudberryClient.send(pinsTimeJson, function(id, resultSet, resultTimeInterval){
          if(angular.isArray(resultSet)) {
            var requestTimeRange = {
              start: new Date(resultTimeInterval.start),
              end: new Date(resultTimeInterval.end)
            };
            // Since the middleware returns the query result in multiple steps,
            // cloudberry.timeSeriesQueryResult stores the current intermediate result.
            cloudberry.timeSeriesQueryResult = resultSet[0];
            // Avoid memory leak.
            resultSet[0] = [];
            cloudberry.commonTimeSeriesResult = TimeSeriesCache.getValuesFromResult(cloudberry.timeSeriesQueryResult).concat(
              TimeSeriesCache.getTimeSeriesValues(cloudberry.parameters.geoIds, cloudberry.parameters.geoLevel, requestTimeRange));
          }
          // When the query is executed completely, we update the time series cache.
          if((cloudberryConfig.querySliceMills > 0 && !angular.isArray(resultSet) &&
            resultSet["key"] === "done") || cloudberryConfig.querySliceMills <= 0) {
            TimeSeriesCache.putTimeSeriesValues($scope.geoIdsNotInTimeSeriesCache,
              cloudberry.timeSeriesQueryResult, cloudberry.parameters.timeInterval);
          }
        }, "pinTime");
      }
    }

    // Event handler for zoom event
    function onZoomPinmap(event) {
      sendPinmapQuery();
    }

    // Common event handler for Countmap
    function pinMapCommonEventHandler(event) {
        $scope.livePinLayer.eachLayer(m=>{
          $scope.map.removeLayer(m);
        })
        sendPinmapQuery();
    }

    // clear pinmap specific data
    function cleanPinMap() {
      $scope.points = [];
      $scope.pointIDs = [];
      if($scope.pointsLayer != null) {
        $scope.map.removeLayer($scope.pointsLayer);
        $scope.pointsLayer = null;
      }
      if ($scope.currentMarker != null) {
        $scope.map.removeLayer($scope.currentMarker);
        $scope.currentMarker = null;
      }

      $scope.livePinLayer.eachLayer((m) => {
        $scope.map.removeLayer(m);
      });
      // Unsubscribe to moduleManager's events
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, onZoomPinmap);
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, pinMapCommonEventHandler);
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, pinMapCommonEventHandler);
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, pinMapCommonEventHandler);
    }

    // initialize pinmap
    function setInfoControlPinMap() {

      // add feature to each polygon
      // when a user click on a polygon, the map will zoom in to fit that polygon in the view
      function onEachFeature(feature, layer) {
        layer.on({
          click: $scope.zoomToFeature
        });
      }

      $scope.loadGeoJsonFiles(onEachFeature);

      $scope.$parent.onEachFeature = onEachFeature;

      // Subscribe to moduleManager's events
      moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, onZoomPinmap);
      moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, pinMapCommonEventHandler);
      moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, pinMapCommonEventHandler);
      moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, pinMapCommonEventHandler);

      $scope.mouseOverPointI = 0;
    }
    
    // function for drawing pinmap
    function drawPinMap(result) {

      if ($scope.currentMarker != null) {
        $scope.map.removeLayer($scope.currentMarker);
      }

      //To initialize the points layer
      if (!$scope.pointsLayer) {
       
        $scope.pointsLayer = new WebGLPointLayer();
        //$scope.pointsLayer.setPointSize(10);
        $scope.pointsLayer.setPointSize(3);
        $scope.pointsLayer.setPointColor(0, 0, 255);

        $scope.map.addLayer($scope.pointsLayer);

        //Create a new event called "mouseintent" by listening to "mousemove".
        $scope.map.on("mousemove", onMapMouseMove);
        var timer = null;
        //If user hang the mouse cursor for 300ms, fire a "mouseintent" event.
        function onMapMouseMove(e) {
          var duration = 300;
          if (timer !== null) {
            clearTimeout(timer);
            timer = null;
          }
          timer = setTimeout(L.Util.bind(function() {
            this.fire("mouseintent", e);
            timer = null;
          }, this), duration);
        }

        //shows each point's info in Front-end.
        $scope.translateTweetDataToShow = function (tweetJSON) {
            var tweetid = "";
            try {
                tweetid = tweetJSON["id"];
            }
            catch (e){
                //tweetid missing in this Tweet.
            }

            var userName = "";
            try {
                userName = tweetJSON["user.name"];
            }
            catch (e){
                //userName missing in this Tweet.
            }

            var userPhotoUrl = "";
            try {
                userPhotoUrl = tweetJSON["user.profile_image_url"];
            }
            catch (e){
                //user.profile_image_url missing in this Tweet.
            }

            var tweetText = "";
            try {
                tweetText = tweetJSON.text;
            }
            catch (e){
                //Text missing in this Tweet.
            }

            var tweetTime = "";
            try {
                var createdAt = new Date(tweetJSON["create_at"]);
                tweetTime = createdAt.toISOString();
            }
            catch (e){
                //Time missing in this Tweet.
            }

            var tweetLink = "";
            try {
                tweetLink = "https://twitter.com/" + userName + "/status/" + tweetid;
            }
            catch (e){
                //tweetLink missing in this Tweet.
            }

            var tweetTemplate;

            //handles exceptions:
            if(tweetText == "" || null || undefined){
                tweetTemplate = "\n"
                + "<div>"
                + "Fail to get Tweets data."
                + "</div>\n";
            }
            else {
                //presents all the information.
                tweetTemplate = "\n"
                    + "<div class=\"tweet\">\n "
                    + "  <div class=\"tweet-body\">"
                    + "    <div class=\"user-info\"> "
                    + "      <img src=\""
                    + userPhotoUrl
                    + "\" onerror=\" this.src='/assets/images/default_pinicon.png'\" style=\"width: 32px; display: inline; \">\n"
                    + "      <span class=\"name\" style='color: #0e90d2; font-weight: bold'> "
                    + userName
                    + "      </span> "
                    + "    </div>\n	"
                    + "    <span class=\"tweet-time\" style='color: darkgray'>"
                    + tweetTime
                    + "    <br></span>\n	 "
                    + "    <span class=\"tweet-text\" style='color: #0f0f0f'>"
                    + tweetText
                    + "    </span><br>\n	 "
                    + "\n <a href=\""
                    + tweetLink
                    + "\"> "
                    + tweetLink
                    + "</a>"
                    + "  </div>\n	"
                    + "</div>\n";
            }

            return tweetTemplate;
        }

        $scope.map.on("mouseintent", onMapMouseIntent);

        $scope.currentMarker = null;
        $scope.points = [];

        function onMapMouseIntent(e) {

          var pointID = $scope.pointsLayer.getCurrentPointID(e);

          //if mouse over a new point, show the Popup Tweet!
          if (pointID > 0 && $scope.mouseOverPointID !== pointID) {
            $scope.mouseOverPointID = pointID;
            //(1) If previous Marker is not null, destroy it.
            if ($scope.currentMarker != null) {
              $scope.map.removeLayer($scope.currentMarker);
            }
            //(2) Create a new Marker to highlight the point.
            $scope.currentMarker = L.circleMarker(e.latlng, {
              radius : 6,
              color : "#0d3e99",
              weight : 3,
              fillColor : "#b8e3ff",
              fillOpacity : 1.0
            }).addTo($scope.map);

            //send the query to cloudberry using string format.
            var passID = "" + pointID;
            cloudberry.pinMapOneTweetLookUpQuery(passID);
          }
        }
        //monitors and receives the result with updating content of each pin tweet.
        $scope.$watch(function () {
           return cloudberryConfig.pinMapOneTweetLookUpResult;
        }, function (newVal) {
           var tweetContent = $scope.translateTweetDataToShow(newVal);
           $scope.popUpTweet = L.popup({maxWidth:300, minWidth:300, maxHight:300});
           $scope.popUpTweet.setContent(tweetContent);
           if($scope.currentMarker === null)
           {
               //pass
           }
           else {
               $scope.currentMarker.bindPopup($scope.popUpTweet).openPopup();
           }
        });
      }

      //Update the points data
      if (result.length > 0){
        $scope.points = [];
        for (var i = 0; i < result.length; i++) {
          if (result[i].hasOwnProperty("coordinate")){
            $scope.points.push([result[i].coordinate[1], result[i].coordinate[0], result[i].id]);
          }
          else if (result[i].hasOwnProperty("place.bounding_box")){
            $scope.points.push([$scope.rangeRandom(result[i].id, result[i]["place.bounding_box"][0][1], result[i]["place.bounding_box"][1][1]), $scope.rangeRandom(result[i].id + 79, result[i]["place.bounding_box"][0][0], result[i]["place.bounding_box"][1][0]), result[i].id]); // 79 is a magic number to avoid using the same seed for generating both the longitude and latitude.
          }
        }
        $scope.pointsLayer.setData($scope.points);
      }
      else {
        $scope.points = [];
        $scope.pointsLayer.setData($scope.points);
      }
    }
    
    // initialize if the default map type is pinmap
    if (cloudberry.parameters.maptype === "pinmap"){
      setPinMapStyle();
      $scope.resetPolygonLayers();
      setInfoControlPinMap();
    }
    
    // map type change handler
    // initialize the map (styles, zoom/drag handler, etc) when switch to this map
    // clear the map when switch to other map
    function onMapTypeChange(event) {
      if (event.currentMapType === "pinmap") {
        setPinMapStyle();
        $scope.resetPolygonLayers();
        setInfoControlPinMap();
        sendPinmapQuery();
      }
      else if (event.previousMapType === "pinmap"){
        cleanPinMap();
      }
    }

    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_MAP_TYPE, onMapTypeChange);



    //Dynamic generating points
    var firefoxIcon = L.icon({
        iconUrl: "/assets/images/pinmap_new_tweet_animation.gif",
        iconSize: [20, 20], // size of the icon
        popupAnchor: [0,-15]
    });

    $scope.dynamicMapAnimation = function (result){
      var coordinates = result;
      var markList = [];
      let pinStyle = {
        radius: 1,//80,
        useAbsoluteRadius: false,//true,
        color: "#623cfc",//"#0084b4"
        noMask: true,
        lineColor: "#623cfc"//"#00aced"
      }
      function transition(tweet)
      {
          var coordinate = tweet["coordinate"];
          var mark = L.marker([coordinate[0], coordinate[1]], {icon: firefoxIcon});
          mark.addTo($scope.map);
          markList.push(mark);
          var tweetContent = $scope.translateTweetDataToShow(tweet);
          $scope.popUpTweet = L.popup({maxWidth:300, minWidth:300, maxHight:300});
          $scope.popUpTweet.setContent(tweetContent);
          var mark2 = L.circleMarker([coordinate[0], coordinate[1]], pinStyle).bindPopup($scope.popUpTweet);

          $scope.livePinLayer.addLayer(mark2).addTo($scope.map);
          setTimeout(function()
          {
              markList.forEach( (m) => $scope.map.removeLayer(m));
          },10000);

      }

      coordinates.forEach( (x) => transition(x));

    };

    $rootScope.$on("drawLivePin", function(event,result){
        $scope.dynamicMapAnimation(result["data"]);
    });




    // TODO - get rid of this watch by doing work inside the callback function in sendPinmapQuery()
    // monitor the pinmap related variables, update the pinmap if necessary
    $scope.$watch(
      function() {
        return cloudberry.pinmapMapResult;
      },

      function(newResult) {
        if (cloudberry.parameters.maptype === "pinmap"){
          $scope.result = newResult;
          if (Object.keys($scope.result).length !== 0) {
            $scope.status.init = false;
            drawPinMap($scope.result);
          } else {
            drawPinMap($scope.result);
          }
        }
      }
    );

  });
