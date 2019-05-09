angular.module("cloudberry.map")
  .controller("pinMapCtrl", function($scope, $http, cloudberry, cloudberryConfig,
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

    // Send pinmap query to cloudberry
    function sendPinmapQuery() {

      if (typeof(cloudberry.parameters.keywords) === "undefined"
        || cloudberry.parameters.keywords === null
        || cloudberry.parameters.keywords.length === 0) {
        return;
      }

      if ($scope.status.zoomLevel < $scope.currentKeywordMinZoomLevel) {
        $scope.currentKeywordMinZoomLevel = $scope.status.zoomLevel;
      }

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
          sliceMillis: cloudberryConfig.querySliceMills,
          returnDelta: true
        }
      };

      cloudberryClient.send(pinsJson, function(id, resultSet, resultTimeInterval){
        if(angular.isArray(resultSet)) {
          cloudberry.commonTweetResult = resultSet[0].slice(0, queryUtil.defaultSamplingSize - 1);
          cloudberry.pinmapMapResult = resultSet[0];
        }
      }, "pinMapResult");
    }

    // send time series query to Cloudberry
    function sendPinmapTimeQuery() {

      if (typeof(cloudberry.parameters.keywords) === "undefined"
        || cloudberry.parameters.keywords === null
        || cloudberry.parameters.keywords.length === 0) {
        return;
      }

      // For time-series histogram, get geoIds not in the time series cache.
      $scope.geoIdsNotInTimeSeriesCache = TimeSeriesCache.getGeoIdsNotInCache(cloudberry.parameters.keywords,
        cloudberry.parameters.timeInterval, cloudberry.parameters.geoIds, cloudberry.parameters.geoLevel);

      var pinsTimeJson = queryUtil.getTimeBarRequest(cloudberry.parameters, $scope.geoIdsNotInTimeSeriesCache);

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
      // if zoom in, send new query if points are not too many
      if (event.level > $scope.previousZoomLevel) {
        if ($scope.currentPointsCount < $scope.maxPointsCount) {
          sendPinmapQuery();
        }
      }
      // if zoom out, send new query if current keyword does not contain results of higher (vertical) level
      else if (event.level < $scope.previousZoomLevel) {
        if (event.level < $scope.currentKeywordMinZoomLevel) {
          sendPinmapQuery();
        }
      }
      $scope.previousZoomLevel = event.level;
      sendPinmapTimeQuery();
    }

    // Event handler for drag event
    function onDragPinmap(event) {
      if ($scope.currentPointsCount < $scope.maxPointsCount) {
        sendPinmapQuery();
      }
      sendPinmapTimeQuery();
    }

    // Event handler for search keyword event
    function onSearchKeyword(event) {
      drawPinMap([]);
      cleanPinmapMarker();
      $scope.currentKeywordMinZoomLevel = $scope.status.zoomLevel;
      sendPinmapQuery();
      sendPinmapTimeQuery();
    }

    // Event handler for time series range event
    function onTimeSeriesRange(event) {
      drawPinMap([]);
      sendPinmapQuery();
      sendPinmapTimeQuery();
    }

    function cleanPinmapLayer() {
      $scope.points = [];
      $scope.pointIDs = [];
      if($scope.pointsLayer != null) {
        $scope.map.removeLayer($scope.pointsLayer);
        $scope.pointsLayer = null;
      }
    }

    function cleanPinmapMarker() {
      if ($scope.currentMarker != null) {
        $scope.map.removeLayer($scope.currentMarker);
        $scope.currentMarker = null;
      }
    }

    function cleanPinmapTimer() {
      if ($scope.timer != null) {
        clearTimeout($scope.timer);
        $scope.timer = null;
      }
    }

    function cleanPinmapEventHandlers() {
      $scope.map.off("mousemove");

      // Unsubscribe to moduleManager's events
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, onZoomPinmap);
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, onDragPinmap);
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, onSearchKeyword);
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, onTimeSeriesRange);
    }

    // clean pinmap related data structures
    function cleanPinMap() {
      cleanPinmapLayer();
      cleanPinmapMarker();
      cleanPinmapTimer();
      cleanPinmapEventHandlers();
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
      moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, onDragPinmap);
      moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, onSearchKeyword);
      moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, onTimeSeriesRange);

      $scope.mouseOverPointID = -1;
      // number of points shown now
      $scope.currentPointsCount = 0;
      // maximum number of points can be shown, 10 million
      $scope.maxPointsCount = 10000000;
      // current keyword min zoom level
      $scope.currentKeywordMinZoomLevel = -1;
      // previous zoom level
      $scope.previousZoomLevel = $scope.status.zoomLevel;
    }
    
    // function for drawing pinmap
    function drawPinMap(result) {

      // initialize the points layer
      if (!$scope.pointsLayer) {
       
        $scope.pointsLayer = new WebGLPointLayer();
        $scope.pointsLayer.setPointSize(3);
        $scope.pointsLayer.setPointColor(0, 0, 255);

        $scope.map.addLayer($scope.pointsLayer);

        // register listener to "mousemove" event on map
        $scope.map.on("mousemove", onMapMouseMove);
        $scope.timer = null;
        // if user mouses over one place for 300ms, fire a "mouseintent" event.
        function onMapMouseMove(e) {
          $scope.currentMousePosition = e;
          if ($scope.timer != null) {
            clearTimeout($scope.timer);
            $scope.timer = null;
          }
          $scope.timer = setTimeout(L.Util.bind(function() {
            this.fire("mouseintent", e);
            $scope.timer = null;
          }, this), 300);
        }

        // translate individual tweet from JSON to html element
        function translateTweetDataToShow(tweetJSON) {
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
            if (tweetText == "" || null || undefined) {
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

        $scope.points = [];

        function onMapMouseIntent(e) {

          var pointID = $scope.pointsLayer.getCurrentPointID(e);

          // if mouse intent a new point, show the Popup Tweet!
          if (pointID > 0 && $scope.mouseOverPointID !== pointID) {
            $scope.mouseOverPointID = pointID;
            // (1) if previous Marker is not null, destroy it.
            cleanPinmapMarker();
            // (2) create a new Marker to highlight the point.
            $scope.currentMarker = L.circleMarker(e.latlng, {
              radius : 6,
              color : "#0d3e99",
              weight : 3,
              fillColor : "#b8e3ff",
              fillOpacity : 1.0
            }).addTo($scope.map);

            var pinJson = {
              dataset:"twitter.ds_tweet",
              filter: [{
                field: "id",
                relation: "=",
                values: "" + pointID
              }],
              select:{
                order: ["-create_at"],
                limit: 1,
                offset: 0,
                field: ["id","text","user.id","create_at","user.name","user.profile_image_url"]
              }
            };

            cloudberryClient.send(pinJson, function(id, resultSet, resultTimeInterval) {
              var tweetContent = translateTweetDataToShow(resultSet[0][0]);
              $scope.popUpTweet = L.popup({maxWidth:300, minWidth:300, maxHight:300});
              $scope.popUpTweet.setContent(tweetContent);
              // in case the results comes late, only show the popup if mouse position is still at the point
              if ($scope.currentMarker &&
                $scope.mouseOverPointID === $scope.pointsLayer.getCurrentPointID($scope.currentMousePosition)) {
                $scope.currentMarker.bindPopup($scope.popUpTweet).openPopup();
              }
            }, "pinResult");
          }
        }
      }

      //Update the points data
      if (result.length > 0) {
        $scope.currentPointsCount += result.length;
        $scope.points = [];
        for (var i = 0; i < result.length; i++) {
          if (result[i].hasOwnProperty("coordinate")) {
            $scope.points.push([result[i].coordinate[1], result[i].coordinate[0], result[i].id]);
          }
          else if (result[i].hasOwnProperty("place.bounding_box")) {
            $scope.points.push([$scope.rangeRandom(result[i].id, result[i]["place.bounding_box"][0][1], result[i]["place.bounding_box"][1][1]), $scope.rangeRandom(result[i].id + 79, result[i]["place.bounding_box"][0][0], result[i]["place.bounding_box"][1][0]), result[i].id]); // 79 is a magic number to avoid using the same seed for generating both the longitude and latitude.
          }
        }
        $scope.pointsLayer.appendData($scope.points);
      }
      else {
        $scope.points = [];
        $scope.pointsLayer.setData($scope.points);
      }
    }
    
    // initialize if the default map type is pinmap
    if (cloudberry.parameters.maptype === "pinmap") {
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
      else if (event.previousMapType === "pinmap") {
        cleanPinMap();
      }
    }

    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_MAP_TYPE, onMapTypeChange);

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
          }
        }
      }
    );

  });
