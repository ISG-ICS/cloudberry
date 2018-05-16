angular.module('cloudberry.common')
    .service('multilayerService', function(multilayerHeatmap){
        var multilayerService = {
            createHeatmapLayer: multilayerHeatmap.createLayer
        };
        
        return multilayerService;
    });
