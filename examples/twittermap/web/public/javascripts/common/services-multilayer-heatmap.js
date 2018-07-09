angular.module('cloudberry.common')
    .service('multilayerHeatmap', function($timeout, $q, cloudberry, cloudberryConfig){
        var defaultHeatmapLimit = parseInt(config.heatmapSamplingLimit);
        var defaultHeatmapSamplingDayRange = parseInt(config.heatmapSamplingDayRange);
        var defaultNonSamplingDayRange = 1500;
        
        function initheatMap(scope){
            var unitRadius = parseInt(config.heatmapUnitRadius); // getting the default radius for a tweet
            this.layer = L.heatLayer([], {radius: unitRadius});
            var instance = this;
            
            var deferred = $q.defer();
            deferred.resolve();
            return deferred.promise;
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
        
        function zoomFunction(){
        }
        
        function createHeatmapQuery(){
            var heatJson = (JSON.stringify({
                dataset: this.parameters.dataset,
                filter: cloudberry.getFilter(cloudberry.parameters, defaultHeatmapSamplingDayRange, cloudberry.parameters.geoIds),
                select: {
                    order: ["-create_at"],
                    limit: defaultHeatmapLimit,
                    offset: 0,
                    field: ["id", "coordinate", "place.bounding_box", "create_at", "user.id"]
                },
                option: {
                    sliceMillis: cloudberryConfig.querySliceMills
                },
                transform: {
                    wrap: {
                        id: this.parameters.id,
                        category: this.parameters.id
                    }
                }
            }));
            
            var heatTimeJson = (JSON.stringify({
                dataset: this.parameters.dataset,
                filter: cloudberry.getFilter(cloudberry.parameters, defaultNonSamplingDayRange, cloudberry.parameters.geoIds),
                group: {
                    by: [{
                        field: "create_at",
                        apply: {
                            name: "interval",
                            args: {
                                unit: cloudberry.parameters.timeBin
                            }
                        },
                        as: cloudberry.parameters.timeBin
                    }],
                    aggregate: [{
                        field: "*",
                        apply: {
                            name: "count"
                        },
                        as: "count"
                    }]
                },
                option: {
                    sliceMillis: cloudberryConfig.querySliceMills
                },
                transform: {
                    wrap: {
                        id: "timeSeries",
                        category: "timeSeries"
                    }
                }
            }));
            
            return [heatJson, heatTimeJson];
        }
        
        var watchVariables = {"heatmapMapResult":"cloudberry.heatmapMapResult"};
        
        var heatmapService = {
            createLayer: function(parameters){
                var deferred = $q.defer();
                deferred.resolve({
                    active: 0,
                    parameters: parameters,
                    layer: {},
                    init: initheatMap,
                    draw: drawHeatMap,
                    clear: cleanHeatMap,
                    zoom: zoomFunction,
                    createQuery: createHeatmapQuery,
                    watchVariables: watchVariables
                });
                return deferred.promise;
            }
        };
        
        return heatmapService;
    });
