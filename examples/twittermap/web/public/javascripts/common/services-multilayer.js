angular.module("cloudberry.map")
    .service("createLayerService", function(multilayerHeatmap){
        var createLayerService = {
            heatmap: multilayerHeatmap.createLayer
        };
        
        return createLayerService;
    });
