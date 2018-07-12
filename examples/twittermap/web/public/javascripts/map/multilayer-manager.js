angular.module("cloudberry.map")
  .controller("multiLayerManager", function($timeout, $scope, $rootScope, $window, $http, $compile, cloudberryConfig, cloudberry, leafletData, Cache, createLayerService) {
    
    cloudberry.parameters.layers = {};
    
    // This function will watch all maptypeChange events for all layers 
    $rootScope.$on("maptypeChange", function (event, data) {
        var layerName = cloudberry.parameters.maptype;
        
        for(var key in cloudberry.parameters.layers)
        {   
            if(key!==layerName)
            {       
                    $scope.map.removeLayer(cloudberry.parameters.layers[key].layer);
                    cloudberry.parameters.layers[key].active = 0;
            }
            else if(key === layerName && cloudberry.parameters.layers[key].active === 0)
            {
                if(typeof cloudberry.parameters.layers[key].activate === "function"){
                    cloudberry.parameters.layers[key].activate();
                 }
                cloudberry.parameters.layers[key].active = 1;
                $scope.map.addLayer(cloudberry.parameters.layers[key].layer);
            }
        }

        cloudberry.query(cloudberry.parameters);
    });
    //This function register layer to layer manager 
    function addLayer(layerID, active, parameters){ 
        createLayerService[layerID](parameters).then(function(layer){
            cloudberry.parameters.layers[layerID] = layer;
            cloudberry.parameters.layers[layerID].init($scope).then(function(){
                cloudberry.parameters.layers[layerID].active = active;
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
  });
