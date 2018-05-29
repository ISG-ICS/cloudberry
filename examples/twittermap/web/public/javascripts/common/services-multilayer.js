angular.module('cloudberry.common')
    .service('multilayerService', function(multilayerPolygon, multilayerCountmap, multilayerHeatmap){
        var multilayerService = {
            createPolygonLayer: multilayerPolygon.createLayer,
            createCountmapLayer: multilayerCountmap.createLayer,
            createHeatmapLayer: multilayerHeatmap.createLayer
        };
        
        return multilayerService;
    });
