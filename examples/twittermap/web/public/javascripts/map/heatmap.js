angular.module('cloudberry.map')
  .controller('heatMapCtrl', function($scope, $rootScope, $window, $http, $compile, cloudberry, leafletData, cloudberryConfig, Cache) {
    $rootScope.$on('maptypeChange', function (event, data) {
      if (cloudberry.parameters.maptype == 'heatmap') {
        $scope.resetPolygonLayers();
        //setInfoControlHeatMap();
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
    
    $scope.$watchCollection(
      function() {
        return {
          'mapResult': cloudberry.mapResult,
          //'pointsResult': cloudberry.pointsResult,
          'totalCount': cloudberry.totalCount,
          'doNormalization': $('#toggle-normalize').prop('checked'),
          'doSentiment': $('#toggle-sentiment').prop('checked')
        };
      },

      function(newResult, oldValue) {
        if (cloudberry.parameters.maptype == 'heatmap'){
          if (newResult['totalCount'] !== oldValue['totalCount']) {
            $scope.totalCount = newResult['totalCount'];
          }
        }
      }
    );
  });
