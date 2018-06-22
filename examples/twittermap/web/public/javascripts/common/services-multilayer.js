angular.module('cloudberry.common')
    .service('createLayerService', function(multilayerPolygon, multilayerCountmap, multilayerPinmap, multilayerHeatmap){
        var createLayerService = {
            polygon: multilayerPolygon.createLayer,
            countmap: multilayerCountmap.createLayer,
            heatmap: multilayerHeatmap.createLayer,
            pinmap: multilayerPinmap.createLayer
        };
        
        return createLayerService;
    });
