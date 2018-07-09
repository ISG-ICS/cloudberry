angular.module("cloudberry.map")
  .controller("multiLayerCtrl", function($timeout, $scope, $rootScope, $window, $http, $compile, cloudberryConfig, cloudberry, leafletData, Cache, createLayerService) {
    
    cloudberry.parameters.layers = {};
    $scope.watchVariables = {};
    
    // This function will watch all maptypeChange events for all layers 
    $rootScope.$on("maptypeChange", function (event, data) {
        var layerName = cloudberry.parameters.maptype;
        
        if(layerName!=="heatmap"){
                    $scope.map.removeLayer(cloudberry.parameters.layers["heatmap"].layer);
                    cloudberry.parameters.layers["heatmap"].active = 0;
        }
        if (layerName==="heatmap" && cloudberry.parameters.layers[layerName].active === 0 ){
            
            if (typeof cloudberry.parameters.layers[layerName].activate === "function"){
                cloudberry.parameters.layers[layerName].activate();
            }
            cloudberry.parameters.layers[layerName].active = 1;
            $scope.map.addLayer(cloudberry.parameters.layers[layerName].layer);
        }
        
        cloudberry.query(cloudberry.parameters);
    });
    //This function register layer to layer manager 
    function addLayer(layerID, active, parameters){ 
        createLayerService[layerID](parameters).then(function(layer){
            cloudberry.parameters.layers[layerID] = layer;
            cloudberry.parameters.layers[layerID].init($scope).then(function(){
                cloudberry.parameters.layers[layerID].active = active;
                for (var key in layer.watchVariables){
                    if({}.hasOwnProperty.call(layer.watchVariables,key))
                        $scope.watchVariables[key] = layer.watchVariables[key];
                }
                if (cloudberry.parameters.layers[layerID].active){
                    $scope.map.addLayer(cloudberry.parameters.layers[layerID].layer);
                }
            });
        });
    }     
    
    var heatmapParameters = {
        id: "heatmap",
        dataset: "twitter.ds_tweet",
    };
    addLayer("heatmap", 0, heatmapParameters); 
    
    $scope.$on("leafletDirectiveMap.zoomend", function() {
        for (var key in cloudberry.parameters.layers) {
            if (cloudberry.parameters.layers[key].active && typeof cloudberry.parameters.layers[key].zoom === "function"){
                cloudberry.parameters.layers[key].zoom();
            }
        }
    });
    
    $scope.$on("leafletDirectiveMap.dragend", function() {
        for (var key in cloudberry.parameters.layers) {
            if (cloudberry.parameters.layers[key].active && typeof cloudberry.parameters.layers[key].drag === "function"){
                cloudberry.parameters.layers[key].drag();
            }
        }
    });
    
    $scope.$watchCollection(
        function() {
            var obj = {};
        
            for (var key in $scope.watchVariables){
                if({}.hasOwnProperty.call($scope.watchVariables,key))
                    obj[key] = eval($scope.watchVariables[key]);
            }
          
            return obj;
        },
        function(newResult, oldValue) {
            var layerName = cloudberry.parameters.maptype;  
            var resultName = layerName + "MapResult";  
        
            if (newResult[resultName] !== oldValue[resultName]) {
                $scope.result = newResult[resultName];
                if (Object.keys($scope.result).length !== 0) {
                    $scope.status.init = false;
                    cloudberry.parameters.layers[layerName].draw($scope.result);
                } else {
                    cloudberry.parameters.layers[layerName].draw($scope.result);
                }
            }
        }
    );
    
  });
