angular.module("cloudberry.map")
  .controller("heatMapCtrl", function($scope, cloudberry, cloudberryConfig,
                                      TimeSeriesCache, heatMapResultCache, moduleManager, cloudberryClient, queryUtil) {
    function setHeatMapStyle() {
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
    var previousKeywords = [];
    var points = [];
    // Send query to cloudberry
    function sendHeatmapQuery() {

      if (typeof(cloudberry.parameters.keywords) === "undefined"
        || cloudberry.parameters.keywords === null
        || cloudberry.parameters.keywords.length === 0) {
        return;
      }

      // For time-series histogram, get geoIds not in the time series cache.
      $scope.geoIdsNotInTimeSeriesCache = TimeSeriesCache.getGeoIdsNotInCache(cloudberry.parameters.keywords,
        cloudberry.parameters.timeInterval, cloudberry.parameters.geoIds, cloudberry.parameters.geoLevel);

      var heatJson = {
        dataset: cloudberry.parameters.dataset,
        filter: queryUtil.getFilter(cloudberry.parameters, queryUtil.defaultHeatmapSamplingDayRange, cloudberry.parameters.geoIds),
        select: {
          order: ["-create_at"],
          limit: queryUtil.defaultHeatmapLimit,
          offset: 0,
          field: ["id", "coordinate", "place.bounding_box", "create_at", "user.id"]
        },
        option: {
          sliceMillis: cloudberryConfig.querySliceMills,
          returnDelta: true
        }
      };


      // Complete time series cache hit case - exclude time series request
      if($scope.geoIdsNotInTimeSeriesCache.length === 0) {
        cloudberry.commonTimeSeriesResult = TimeSeriesCache.getTimeSeriesValues(cloudberry.parameters.geoIds, cloudberry.parameters.geoLevel, cloudberry.parameters.timeInterval);

        // Partial heatmap cache hit case
        if (!heatMapResultCache.cacheIsDone(cloudberry.parameters.timeInterval)) {
          cloudberryClient.send(heatJson, function(id, resultSet, resultTimeInterval){
            if(angular.isArray(resultSet)) {
              cloudberry.commonTweetResult = resultSet[0].slice(0, queryUtil.defaultSamplingSize - 1);
              cloudberry.heatmapMapResult = resultSet[0];
              heatMapResultCache.putValues(cloudberry.heatmapMapResult, resultTimeInterval);
              cloudberry.heatMapMinDate = cloudberry.heatmapMapResult[cloudberry.heatmapMapResult.length-1]["create_at"];
            }
          }, "heatMapResult");
        }
        
        // Complete heatmap cache hit case
        else {
          cloudberry.heatmapMapResult = heatMapResultCache.getValues(cloudberry.parameters.timeInterval);
        }
      }
    
      // Partial time series cache hit case
      else {
        var heatTimeJson = queryUtil.getTimeBarRequest(cloudberry.parameters, $scope.geoIdsNotInTimeSeriesCache);

        // Partial heatmap cache hit case
        if (!heatMapResultCache.cacheIsDone(cloudberry.parameters.timeInterval)) {   
          cloudberryClient.send(heatJson, function(id, resultSet, resultTimeInterval) {
            if(angular.isArray(resultSet)) {
              cloudberry.commonTweetResult = resultSet[0].slice(0, queryUtil.defaultSamplingSize - 1);
              cloudberry.heatmapMapResult = resultSet[0];
              heatMapResultCache.putValues(cloudberry.heatmapMapResult, resultTimeInterval);
              cloudberry.heatMapMinDate = cloudberry.heatmapMapResult[cloudberry.heatmapMapResult.length-1]["create_at"];
            }
          }, "heatMapResult");
        }
        else {
          // Complete heatmap cache hit case
          cloudberry.heatmapMapResult = heatMapResultCache.getValues(cloudberry.parameters.timeInterval);
        }
        
        cloudberryClient.send(heatTimeJson, function(id, resultSet, resultTimeInterval){
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
        }, "heatTime");
      }
    }

    // Common event handler for Heatmap
    function heatMapCommonEventHandler(event) {
        sendHeatmapQuery();
        points = [];
    }

    function heatMapKeywordEventHandler(event) {
      heatMapCommonEventHandler(event);
      heatMapResultCache.emptyStore();
    }

    function cleanHeatMap() {
      if ($scope.heatMapLayer){
        $scope.map.removeLayer($scope.heatMapLayer);
        $scope.heatMapLayer = null;
      }

      // Unsubscribe to moduleManager's events
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, heatMapCommonEventHandler);
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, heatMapCommonEventHandler);
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, heatMapKeywordEventHandler);
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, heatMapCommonEventHandler);
    }
    
    function setInfoControlHeatMap() {
      function onEachFeature(feature, layer) {
        layer.on({
          click: $scope.zoomToFeature
        });
      }

      $scope.loadGeoJsonFiles(onEachFeature);

      $scope.$parent.onEachFeature = onEachFeature;

      // Subscribe to moduleManager's events
      moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, heatMapCommonEventHandler);
      moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, heatMapCommonEventHandler);
      moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, heatMapKeywordEventHandler);
      moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, heatMapCommonEventHandler);

      if (!$scope.heat){
        var unitRadius = parseInt(config.heatmapUnitRadius); // getting the default radius for a tweet
        $scope.heat = L.heatLayer([], {radius: unitRadius});
      }
    }
    
    function drawHeatMap(result) {
      function setHeatMapPoints(points) {
        $scope.heat.setLatLngs(points);
        $scope.heat.redraw();
      }
      
      if (!$scope.heatMapLayer){
        $scope.heatMapLayer = $scope.heat;
        $scope.map.addLayer($scope.heatMapLayer);
      }
      
      var unitIntensity = parseInt(config.heatmapUnitIntensity); // getting the default intensity for a tweet
      for (var i = 0; i < result.length; i++) {
        if (result[i].hasOwnProperty("coordinate")){
          points.push([result[i].coordinate[1], result[i].coordinate[0], unitIntensity]);
        }
        else {
          points.push([$scope.rangeRandom(result[i].id, result[i]["place.bounding_box"][0][1], result[i]["place.bounding_box"][1][1]), $scope.rangeRandom(result[i].id + 79, result[i]["place.bounding_box"][0][0], result[i]["place.bounding_box"][1][0]), unitIntensity]); // 79 is a magic number to avoid using the same seed for generating both the longitude and latitude.
        }
      }
      setHeatMapPoints(points);
    }
    
    // initialize
    if (cloudberry.parameters.maptype === "heatmap"){
      setHeatMapStyle();
      $scope.resetPolygonLayers();
      setInfoControlHeatMap();
    }

    // map type change handler
    // initialize the map (styles, zoom/drag handler, etc) when switch to this map
    // clear the map when switch to other map
    function onMapTypeChange(event) {
      if (event.currentMapType === "heatmap") {
        document.getElementById("time-slider").style.display = "none";
        document.getElementById("play-button").style.display = "none";
        points = [];
        if (cloudberry.parameters.keywords != previousKeywords){
          heatMapResultCache.emptyStore();
        };
        setHeatMapStyle();
        $scope.resetPolygonLayers();
        setInfoControlHeatMap();
        sendHeatmapQuery();
      }
      else if (event.previousMapType === "heatmap"){
        points = [];
        previousKeywords = cloudberry.parameters.keywords;
        cleanHeatMap();
      }
    }

    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_MAP_TYPE, onMapTypeChange);

    // TODO - get rid of this watch by doing work inside the callback function in sendHeatmapQuery()
    $scope.$watch(
      function() {
        return cloudberry.heatmapMapResult;
      },

      function(newResult) {
        if (cloudberry.parameters.maptype === "heatmap"){
          $scope.result = newResult;
          if (Object.keys($scope.result).length !== 0) {
            $scope.status.init = false;
            drawHeatMap($scope.result);
          } else {
            drawHeatMap($scope.result);
          }
        }
      }
    );

  });