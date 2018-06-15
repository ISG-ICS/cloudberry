angular.module('cloudberry.common')
    .service('multilayerService', function(multilayerPolygon, multilayerCountmap, multilayerPinmap, multilayerHeatmap){
        var multilayerService = {
            createPolygonLayer: multilayerPolygon.createLayer,
            createCountmapLayer: multilayerCountmap.createLayer,
            createHeatmapLayer: multilayerHeatmap.createLayer,
            createPinmapLayer: multilayerPinmap.createLayer
        };
        
        return multilayerService;
    });
