angular.module('cloudberry.common')
    .service('multilayerHeatmap', function($timeout, cloudberryConfig){
        var heatmapService = {
            createLayer: function(parameters){
                function initheatMap(result) {
                }
                function drawHeatMap(result) {
                }
                
                var layer = {
                    active:0,
                    init:initheatMap,
                    data:"",
                    draw:drawHeatMap,
                    clear:cleanHeatMap,
                    watchVariables:watchVariables
                }
                return layer;
            }
        }
        
        return heatmapService;
    });
