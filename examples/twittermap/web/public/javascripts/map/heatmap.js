angular.module("cloudberry.map")
  .controller("heatMapCtrl", function($scope, cloudberry, cloudberryConfig,
                                      moduleManager, cloudberryClient, queryUtil) {
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

    // Send query to cloudberry
    function sendHeatmapQuery() {
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
          sliceMillis: cloudberryConfig.querySliceMills
        }
      };

      var heatTimeJson = queryUtil.getTimeBarRequest(cloudberry.parameters);

      cloudberryClient.send(heatJson, function(id, resultSet){
        if(angular.isArray(resultSet)) {
          cloudberry.commonTweetResult = resultSet[0].slice(0, queryUtil.defaultSamplingSize - 1);
          cloudberry.heatmapMapResult = resultSet[0];
        }
      }, "heatMapResult");

      cloudberryClient.send(heatTimeJson, function(id, resultSet){
        if(angular.isArray(resultSet)) {
          cloudberry.commonTimeSeriesResult = resultSet[0];
        }
      }, "heatTime");
    }

    // Common event handler for Heatmap
    function heatMapCommonEventHandler(event) {
        sendHeatmapQuery();
    }

    function cleanHeatMap() {
      if ($scope.heatMapLayer){
        $scope.map.removeLayer($scope.heatMapLayer);
        $scope.heatMapLayer = null;
      }

    }
    
    function setInfoControlHeatMap() {
      function onEachFeature(feature, layer) {
        layer.on({
          click: $scope.zoomToFeature
        });
      }

      $scope.loadGeoJsonFiles(onEachFeature);

      $scope.$parent.onEachFeature = onEachFeature;

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
      var points = [];
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
        setHeatMapStyle();
        $scope.resetPolygonLayers();
        setInfoControlHeatMap();
      }
      else if (event.previousMapType === "heatmap"){
        cleanHeatMap();
      }
    }

    //moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_MAP_TYPE, onMapTypeChange);
  });
