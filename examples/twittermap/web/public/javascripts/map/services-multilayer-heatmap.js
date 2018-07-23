angular.module("cloudberry.map")
  .service("multilayerHeatmap", function($timeout, $q, cloudberry, cloudberryConfig, cloudberryClient, queryUtil){
    var defaultHeatmapLimit = parseInt(config.heatmapSamplingLimit);
    var defaultHeatmapSamplingDayRange = parseInt(config.heatmapSamplingDayRange);
    var defaultNonSamplingDayRange = 1500;
    var instance;
    function initheatMap(scope){
      var unitRadius = parseInt(config.heatmapUnitRadius); // getting the default radius for a tweet
      this.layer = L.heatLayer([], {radius: unitRadius});
      instance = this;
      var deferred = $q.defer();
      deferred.resolve();
      return deferred.promise;            
    }
  
    //This function handle all events for heatmap
    function heatMapHandler(){
      
      var heatJson = {
        dataset: cloudberry.parameters.dataset,
        filter: queryUtil.getFilter(cloudberry.parameters, queryUtil.defaultHeatmapSamplingDayRange, cloudberry.parameters.geoIds),
        select: {
          order: ["-create_at"],
          limit: queryUtil.defaultHeatmapLimit,
          offset: 0,
          field: ["id", "coordinate", "place.bounding_box", "create_at", "user.id"]
        },
        option: {
          sliceMillis: cloudberryConfig.querySliceMills
        }
      };
      
      cloudberryClient.send(heatJson,function(id,resultSet){
        if(angular.isArray(resultSet[0])){
          drawHeatMap(resultSet[0]);
          cloudberry.commonTweetResult = resultSet[0].slice(0, queryUtil.defaultSamplingSize - 1);
          cloudberry.heatmapMapResult = resultSet[0];
        }
      },"heatMapResult");
      
      var heatTimeJson = queryUtil.getTimeBarRequest(cloudberry.parameters);
      
      cloudberryClient.send(heatTimeJson,function(id,resultSet){
        if(angular.isArray(resultSet[0])){
          cloudberry.commonTimeSeriesResult = resultSet[0];
        }
      },"heatTime");
    }
  
    // For randomize coordinates by bounding_box
    var randomizationSeed;
    
    // javascript does not provide API for setting seed for its random function, so we need to implement it ourselves.
    function customRandom() {
      var x = Math.sin(randomizationSeed++) * 10000;
      return x - Math.floor(x);
    }

    // return a random number with normal distribution
    function randomNorm(mean, stdev) {
      return mean + (((customRandom() + customRandom() + customRandom() + customRandom() + customRandom() + customRandom()) - 3) / 3) * stdev;
    }

    // randomize a pin coordinate for a tweet according to the bounding box (normally distributed within the bounding box) when the actual coordinate is not availalble.
    // by using the tweet id as the seed, the same tweet will always be randomized to the same coordinate.
    function rangeRandom(seed, minV, maxV){
      randomizationSeed = seed;
      var ret = randomNorm((minV + maxV) / 2, (maxV - minV) / 16);
      return ret;
    }

    function drawHeatMap(result,ref){
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

    var heatmapService = {
      createLayer(parameters){             
        var deferred = $q.defer();
        deferred.resolve({
          active: 0,
          parameters,
          layer: {},
          init: initheatMap,
          clear: cleanHeatMap,
          onMapTypeChange:heatMapHandler,
          onChangeSearchKeyword:heatMapHandler,
          onChangeTimeSeriesRange:heatMapHandler,
          onZoom:heatMapHandler,
          onDrag:heatMapHandler
        });
        return deferred.promise;
      }
    };

    return heatmapService;
  });
