angular.module('cloudberry.map')
  .controller('heatMapCtrl', function($scope, $rootScope, $window, $http, $compile, cloudberry, leafletData, cloudberryConfig, Cache) {
    $rootScope.$on('maptypeChange', function (event, data) {
      if (cloudberry.parameters.maptype == 'heatmap') {
        $scope.resetPolygonLayers();
        cloudberry.query(cloudberry.parameters, cloudberry.queryType);
      }
      else {
        cleanHeatMap();
      }
    })
    
    function setHeatMapStyle() {
    }
    
    function cleanHeatMap() {
    }
    
    function setInfoControlHeatMap() {
      //TODO For HeatMap use later.
    }
    
    function drawHeatMap(result) {
    }
    
    // initialize
    setHeatMapStyle();
    setInfoControlHeatMap();
    
    $scope.$watch(
      function() {
        return cloudberry.heatmapMapResult;
      },

      function(newResult) {
        if (cloudberry.parameters.maptype == 'heatmap'){
        }
      }
    );
  });
