angular.module("cloudberry.map")
    .service("createLayerService", function(multilayerHeatmap,multilayerPolygon,multilayerCountmap){
        var createLayerService = {
            countmap:multilayerCountmap.createLayer,
            heatmap: multilayerHeatmap.createLayer,
            polygon: multilayerPolygon.createLayer,
        };
        
        return createLayerService;
    });
