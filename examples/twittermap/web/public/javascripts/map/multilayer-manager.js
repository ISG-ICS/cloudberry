angular.module("cloudberry.map")
  .controller("multiLayerManager", function($scope, $rootScope, cloudberry, createLayerService, moduleManager, cloudberryClient) {

    cloudberry.parameters.layers = {};

    // This function checks MAP_TYPE_CHANGE event for all layers 
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_MAP_TYPE, function(event){
      var layerName = cloudberry.parameters.maptype;

      for(var key in cloudberry.parameters.layers)
      {   
        if(key!==layerName)
        {       
          $scope.map.removeLayer(cloudberry.parameters.layers[key].layer);
          cloudberry.parameters.layers[key].active = 0;
          if(typeof(cloudberry.parameters.layers[key].clear) === "function"){
            cloudberry.parameters.layers[key].clear();
          }
        }
        else if(key === layerName && cloudberry.parameters.layers[key].active === 0)
        {
          if(typeof cloudberry.parameters.layers[key].activate === "function"){
            cloudberry.parameters.layers[key].activate();
          }
          cloudberry.parameters.layers[key].active = 1;
          $scope.map.addLayer(cloudberry.parameters.layers[key].layer);
          cloudberry.parameters.layers[key].onMapTypeChange();
        }
      }

    });
    
    //This function checks CHANGE_SEARCH_KEYWORD event for all layers
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD,function(event){
      var layerName = cloudberry.parameters.maptype;
      
      for(var key in cloudberry.parameters.layers)
      {   
        if(key === layerName && cloudberry.parameters.layers[key].active === 1)
        {
          cloudberry.parameters.layers[key].onChangeSearchKeyword();
        }
      }
    });
    
    //This function checks CHANGE_TIME_SERIES_RANGE event for all layers
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, function(event){
      var layerName = cloudberry.parameters.maptype;
      
      for(var key in cloudberry.parameters.layers)
      {   
        if(key === layerName && cloudberry.parameters.layers[key].active === 1)
        {
          cloudberry.parameters.layers[key].onChangeTimeSeriesRange();
        }
      }
      
    });
  
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, function(event){
      var layerName = cloudberry.parameters.maptype;
      
      for(var key in cloudberry.parameters.layers)
      {   
        if(key === layerName && cloudberry.parameters.layers[key].active === 1)
        {
          cloudberry.parameters.layers[key].onZoom();
        }
      }
    });
    
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, function(event){
      var layerName = cloudberry.parameters.maptype;
      
      for(var key in cloudberry.parameters.layers)
      {   
        if(key === layerName && cloudberry.parameters.layers[key].active === 1)
        {
          cloudberry.parameters.layers[key].onDrag();
        }
      }  
    });
    
    var heatmapParameters = {
        id: "heatmap",
        dataset: "twitter.ds_tweet",
    }
    addLayer("heatmap", 0, heatmapParameters);
    addLayer("polygon", 1,{})
    addLayer("countmap",1,{})
    
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
  
  });
