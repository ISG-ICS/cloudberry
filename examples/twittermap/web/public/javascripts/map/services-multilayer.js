angular.module("cloudberry.map")
    .service("createLayerService", function(multilayerHeatmap,multilayerPolygon,multilayerCountmap){
        var createLayerService = {
            heatmap: multilayerHeatmap.createLayer,
            polygon: multilayerPolygon.createLayer,
            countmap:multilayerCountmap.createLayer,
        };
        
        return createLayerService;
    });
