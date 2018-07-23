angular.module("cloudberry.map")
    .service("createLayerService", function(multilayerHeatmap,multilayerPolygon){
        var createLayerService = {
            heatmap: multilayerHeatmap.createLayer,
            polygon: multilayerPolygon.createLayer
        };
        
        return createLayerService;
    });
