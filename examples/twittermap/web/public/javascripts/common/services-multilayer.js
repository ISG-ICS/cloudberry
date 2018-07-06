angular.module('cloudberry.common')
    .service('createLayerService', function(multilayerHeatmap){
        var createLayerService = {
            heatmap: multilayerHeatmap.createLayer,
        };
        
        return createLayerService;
    });
