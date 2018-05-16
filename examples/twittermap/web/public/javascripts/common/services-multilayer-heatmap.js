angular.module('cloudberry.common')
    .service('multilayerHeatmap', function($timeout, $q, cloudberryConfig){
        var heatmapStyle = {
            initStyle: {
                weight: 0.5,
                fillOpacity: 0,
                color: "white"
            },
            stateStyle: {
                fillColor: "#f7f7f7",
                weight: 0.5,
                opacity: 1,
                color: "#92d1e1",
                fillOpacity: 0
            },
            stateUpperStyle: {
                fillColor: "#f7f7f7",
                weight: 0.5,
                opacity: 1,
                color: "#92d1e1",
                fillOpacity: 0
            },
            countyStyle: {
                fillColor: "#f7f7f7",
                weight: 0.5,
                opacity: 1,
                color: "#92d1e1",
                fillOpacity: 0
            },
            countyUpperStyle: {
                fillColor: "#f7f7f7",
                weight: 0.5,
                opacity: 1,
                color: "#92d1e1",
                fillOpacity: 0
            },
            cityStyle: {
                fillColor: "#f7f7f7",
                weight: 0.5,
                opacity: 1,
                color: "#92d1e1",
                fillOpacity: 0
            },
            hoverStyle: {
                weight: 0.7,
                color: "#666",
                fillOpacity: 0
            },
            colors: [ "#ffffff", "#92d1e1", "#4393c3", "#2166ac", "#f4a582", "#d6604d", "#b2182b"],
            sentimentColors: ["#ff0000", "#C0C0C0", "#00ff00"]
        };
        
        function initheatMap(){
            var unitRadius = parseInt(config.heatmapUnitRadius); // getting the default radius for a tweet
            this.layer = L.heatLayer([], {radius: unitRadius});
        }
        
        // For randomize coordinates by bounding_box
        var randomizationSeed;

        // javascript does not provide API for setting seed for its random function, so we need to implement it ourselves.
        function CustomRandom() {
            var x = Math.sin(randomizationSeed++) * 10000;
            return x - Math.floor(x);
        }

        // return a random number with normal distribution
        function randomNorm(mean, stdev) {
            return mean + (((CustomRandom() + CustomRandom() + CustomRandom() + CustomRandom() + CustomRandom() + CustomRandom()) - 3) / 3) * stdev;
        }

        // randomize a pin coordinate for a tweet according to the bounding box (normally distributed within the bounding box) when the actual coordinate is not availalble.
        // by using the tweet id as the seed, the same tweet will always be randomized to the same coordinate.
        function rangeRandom(seed, minV, maxV){
            randomizationSeed = seed;
            var ret = randomNorm((minV + maxV) / 2, (maxV - minV) / 16);
            return ret;
        }
        
        function drawHeatMap(result){
            var instance = this;
            function setHeatMapPoints(points) {
                instance.layer.setLatLngs(points);
                instance.layer.redraw();
            }
            
            var unitIntensity = parseInt(config.heatmapUnitIntensity); // getting the default intensity for a tweet
            var points = [];
            for (var i = 0; i < result.length; i++) {
                if (result[i].hasOwnProperty("coordinate")){
                    points.push([result[i].coordinate[1], result[i].coordinate[0], unitIntensity]);
                }
                else {
                    points.push([rangeRandom(result[i].id, result[i]["place.bounding_box"][0][1], result[i]["place.bounding_box"][1][1]), rangeRandom(result[i].id + 79, result[i]["place.bounding_box"][0][0], result[i]["place.bounding_box"][1][0]), unitIntensity]); // 79 is a magic number to avoid using the same seed for generating both the longitude and latitude.
                }
            }
            setHeatMapPoints(points);
        }
        
        function cleanHeatMap(){
            this.layer = null;
        }
        
        var watchVariables = {"heatmapMapResult":"cloudberry.heatmapMapResult"};
        
        var heatmapService = {
            createLayer: function(parameters){
                var deferred = $q.defer();
                deferred.resolve({
                    active: 0,
                    layer: {},
                    init: initheatMap,
                    draw: drawHeatMap,
                    clear: cleanHeatMap,
                    watchVariables: watchVariables
                });
                return deferred.promise;
            }
        }
        
        return heatmapService;
    });
