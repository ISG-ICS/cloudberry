angular.module("cloudberry.map")
  .controller("heatMapCtrl", function($scope, $rootScope, $window, $http, $compile, $timeout, cloudberry, leafletData, cloudberryConfig, Cache) {
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
      
      $scope.resetZoomFunction(onEachFeature);
      $scope.resetDragFunction(onEachFeature);

      if (!$scope.heat){
        $scope.heat = L.heatLayer([]);
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
        
      var points = [];
      for (var i = 0; i < result.length; i++) {
        if (result[i].hasOwnProperty("coordinate")){
          points.push([result[i].coordinate[1], result[i].coordinate[0]]);
        }
        else {
          points.push([$scope.rangeRandom(result[i].id, result[i]["place.bounding_box"][0][1], result[i]["place.bounding_box"][1][1]), $scope.rangeRandom(result[i].id + 79, result[i]["place.bounding_box"][0][0], result[i]["place.bounding_box"][1][0])]); // 79 is a magic number to avoid using the same seed for generating both the longitude and latitude.
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
    
    function initheatMap(){
        setHeatMapStyle();
        $scope.resetPolygonLayers();
        setInfoControlHeatMap();
        cloudberry.query(cloudberry.parameters, cloudberry.queryType);
    }
    
    var watchVariables = {"heatmapMapResult":"cloudberry.heatmapMapResult"};
    
    var l = {
            active:0,
            init:initheatMap,
            data:"",
            draw:drawHeatMap,
            clear:cleanHeatMap,
            watchVariables:watchVariables
    }
    
    /*
    $timeout(function() {
        $rootScope.$emit("registerLayer", ["heatmap", l]);
    })
    */
        
    //cloudberry.layer["heatmap"] = l;

    //$scope.watchVariables["heatmapMapResult"] = "cloudberry.heatmapMapResult";
       
    

    
    
    
    
    
    $rootScope.$on("maptypeChange", function (event, data) {
      if (cloudberry.parameters.maptype === "heatmap") {
        setHeatMapStyle();
        $scope.resetPolygonLayers();
        setInfoControlHeatMap();
        cloudberry.query(cloudberry.parameters, cloudberry.queryType);
      }
      else if (data[0] === "heatmap"){
        cleanHeatMap();
      }
    })
    
    /*$scope.$watch(
      function() {
        return cloudberry.heatmapMapResult;
      },        
      function(newResult) {
        
        if(cloudberry.layer["heatmap"])
            cloudberry.layer["heatmap"].data = newResult; 
          
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
    );*/
  });
