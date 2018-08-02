angular.module("cloudberry.map")
    .service("createLayerService", function(multilayerCountmap){
        var createLayerService = {
            countmap:multilayerCountmap.createLayer,
        };
        
        return createLayerService;
    });
