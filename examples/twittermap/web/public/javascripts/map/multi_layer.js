angular.module('cloudberry.map')
  .controller('multiLayerCtrl', function($timeout, $scope, $rootScope, $window, $http, $compile, cloudberryConfig, cloudberry, leafletData, Cache, createLayerService) {
    
    cloudberry.parameters.layers = {};
    $scope.watchVariables = {};
    
    // initialize
    
    $rootScope.$on('multiLayer', function (event, data) {
        var layer_name = cloudberry.parameters.maptype;
    
        
        
        if(layer_name!='heatmap'){

                    $scope.map.removeLayer(cloudberry.parameters.layers['heatmap'].layer);
                    
                    
                    cloudberry.parameters.layers['heatmap'].active = 0;
        
        }
        if (layer_name==='heatmap' && cloudberry.parameters.layers[layer_name].active == 0 ){
            
            if (typeof cloudberry.parameters.layers[layer_name].activate === "function"){c
                cloudberry.parameters.layers[layer_name].activate();
            }
            cloudberry.parameters.layers[layer_name].active = 1;
            $scope.map.addLayer(cloudberry.parameters.layers[layer_name].layer);
        }
        

        
        cloudberry.query(cloudberry.parameters);
    })
    
    function addLayer(layerID, active, parameters){
        createLayerService[layerID](parameters).then(function(layer){
            cloudberry.parameters.layers[layerID] = layer;
            cloudberry.parameters.layers[layerID].init($scope).then(function(){
                cloudberry.parameters.layers[layerID].active = active;
                for (var key in layer.watchVariables){
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
    }
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
            var obj = {}
        
            for (var key in $scope.watchVariables){
                obj[key] = eval($scope.watchVariables[key]);
            }
          
            return obj;
        },
        function(newResult, oldValue) {
            var layer_name = cloudberry.parameters.maptype;  
            var result_name = layer_name + "MapResult";  
        
            if (newResult[result_name] !== oldValue[result_name]) {
                $scope.result = newResult[result_name];
                if (Object.keys($scope.result).length !== 0) {
                    $scope.status.init = false;
                    cloudberry.parameters.layers[layer_name].draw($scope.result);
                } else {
                    cloudberry.parameters.layers[layer_name].draw($scope.result);
                }
            }
        }
    );
    
    
    
    
  });
