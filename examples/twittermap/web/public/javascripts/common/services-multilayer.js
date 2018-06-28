angular.module('cloudberry.common')
    .service('createLayerService', function(multilayerHeatmap){
        var createLayerService = {
            polygon: {},
            countmap: {},
            heatmap: multilayerHeatmap.createLayer,
            pinmap: {}
        };
        
        return createLayerService;
    });
