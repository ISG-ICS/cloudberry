angular.module("cloudberry.map")
  .controller("multiLayerManager", function($scope, $rootScope, cloudberry, createLayerService, moduleManager, cloudberryClient) {

    cloudberry.parameters.layers = {};

    // This function checks MAP_TYPE_CHANGE event for all layers 
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_MAP_TYPE, function(event){
      var layerName = cloudberry.parameters.maptype;

      for(var key in cloudberry.parameters.layers)
      {   
        if(key !== layerName)
        { 
          //We do not remove polygon layer, for 2 reason
          //(1) we do not have polygon maptype, so once the layer been removed it will not be redraw
          //(2) we need polygon layer whenever the map type is not countmap, for 
          //    efficiency consideration, it's wasting to redraw polygon layer when user switch between maps.
          if(cloudberry.parameters.layers[key].layer && key !== "polygon"){
            $scope.map.removeLayer(cloudberry.parameters.layers[key].layer);
          }
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
          if(cloudberry.parameters.layers[key].layer){
            $scope.map.addLayer(cloudberry.parameters.layers[key].layer);
          }
          //Pass the instance back to handler function
          cloudberry.parameters.layers[key].onMapTypeChange(cloudberry.parameters.layers[key]);
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
          cloudberry.parameters.layers[key].onChangeSearchKeyword(cloudberry.parameters.layers[key]);
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
          cloudberry.parameters.layers[key].onChangeTimeSeriesRange(cloudberry.parameters.layers[key]);
        }
      }
      
    });
  
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, function(event){
      var layerName = cloudberry.parameters.maptype;
      
      for(var key in cloudberry.parameters.layers)
      {   
        if(key === layerName && cloudberry.parameters.layers[key].active === 1)
        {
          cloudberry.parameters.layers[key].onZoom(cloudberry.parameters.layers[key]);
        }
      }
    });
    
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, function(event){
      var layerName = cloudberry.parameters.maptype;
      
      for(var key in cloudberry.parameters.layers)
      {   
        if(key === layerName && cloudberry.parameters.layers[key].active === 1)
        {
          cloudberry.parameters.layers[key].onDrag(cloudberry.parameters.layers[key]);
        }
      }  
    });
  
    //This function register layer to layer manager 
    function addLayer(layerID, active, parameters){
      createLayerService[layerID](parameters).then(function(layer){
        cloudberry.parameters.layers[layerID] = layer;
        cloudberry.parameters.layers[layerID].init($scope,layer).then(function(){
          cloudberry.parameters.layers[layerID].active = active;
          if (cloudberry.parameters.layers[layerID].active){
            $scope.map.addLayer(cloudberry.parameters.layers[layerID].layer);
          }
        });
      });
    }
    
    addLayer("countmap",1,{});
  });
