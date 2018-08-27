angular.module("cloudberry.map")
  .service("multilayerCountmap", function($http, $q, $compile, cloudberry, cloudberryConfig, leafletData,
                                          moduleManager, MapResultCache, cloudberryClient, queryUtil, Cache,
                                          TimeSeriesCache){
    
    function setCenterAndBoundry(features) {
      for(var id in features){
        if ({}.hasOwnProperty.call(features, id)) {
          var minLog = Number.POSITIVE_INFINITY;
          var maxLog = Number.NEGATIVE_INFINITY;
          var minLat = Number.POSITIVE_INFINITY;
          var maxLat = Number.NEGATIVE_INFINITY;
          if(features[id].geometry.type === "Polygon") {
            features[id].geometry.coordinates[0].forEach(function(pair) {
              minLog = Math.min(minLog, pair[0]);
              maxLog = Math.max(maxLog, pair[0]);
              minLat = Math.min(minLat, pair[1]);
              maxLat = Math.max(maxLat, pair[1]);
            });
          } else if( features[id].geometry.type === "MultiPolygon") {
            features[id].geometry.coordinates.forEach(function(array){
              array[0].forEach(function(pair){
                minLog = Math.min(minLog, pair[0]);
                maxLog = Math.max(maxLog, pair[0]);
                minLat = Math.min(minLat, pair[1]);
                maxLat = Math.max(maxLat, pair[1]);
              });
            });
          }
          features[id].properties["centerLog"] = (maxLog + minLog) / 2;
          features[id].properties["centerLat"] = (maxLat + minLat) / 2;
        }
      }
    }
  
    function loadCityJsonByBound(onEachFeature,instance){
      var scope = instance.scope;
      var bounds = scope.map.getBounds();
      var rteBounds = "city/" + bounds._northEast.lat + "/" + bounds._southWest.lat + "/" + bounds._northEast.lng + "/" + bounds._southWest.lng;

        // Caching feature only works when the given threshold is greater than zero.
        if (cloudberryConfig.cacheThreshold > 0) {
          Cache.getCityPolygonsFromCache(rteBounds).done(function(data) {

            //set center and boundary done by Cache
            if (!instance.status.init) {
              resetGeoIds(instance.bounds, data, "cityID");
              cloudberry.parameters.geoLevel = "city";
              // TODO Publish zoom/drag event to moduleManager
              //moduleManager.publishEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, {level: scope.map.getZoom(), bounds: scope.map.getBounds()});
            }

            cloudberry.parameters.geoLevel = "city";

            // initializes the instance.geojsonData.city and instance.cityIdSet when first time zoom in
            if(typeof instance.polygons.cityPolygons === "undefined"){
              instance.geojsonData.city = data;
              instance.polygons.cityPolygons = L.geoJson(data, {
                style: instance.styles.cityStyle,
                onEachFeature
              });
              
              for (var i = 0; i < instance.geojsonData.city.features.length; i++) {
                scope.cityIdSet.add(instance.geojsonData.city.features[i].properties.cityID);
              }
            } 
            else {
              // compares the current region"s cityIds with previously stored cityIds
              // stores the new delta cities" ID and polygon info
              // add the new polygons as GeoJson objects incrementally on the layer
              
              for (var i = 0; i < data.features.length; i++) {
                if (!scope.cityIdSet.has(data.features[i].properties.cityID)) {
                  instance.geojsonData.city.features.push(data.features[i]);
                  scope.cityIdSet.add(data.features[i].properties.cityID);
                  instance.polygons.cityPolygons.addData(data.features[i]);
                }
              }
            }

            // To add the city level map only when it doesn"t exit
           
            instance.layer.addLayer(instance.polygons.cityPolygons);
            
          });
        } else {
          // No caching used here.
          $http.get(rteBounds)
            .success(function (data) {
              instance.geojsonData.city = data;
              if (instance.polygons.cityPolygons) {
                instance.layer.removeLayer(instance.polygons.cityPolygons);
              }
              instance.polygons.cityPolygons = L.geoJson(data, {
                style: instance.styles.cityStyle,
                onEachFeature
              });
              setCenterAndBoundry(instance.geojsonData.city.features);
              resetGeoInfo("city");
              if (!instance.status.init) {
                //TODO Publish zoom/drag event to moduleManager
                //moduleManager.publishEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, {level: instance.map.getZoom(), bounds: instance.map.getBounds()});
              }
              instance.layer.addLayer(instance.polygons.cityPolygons);
            })
            .error(function (data) {
              console.error("Load city data failure");
            });
        }
    }
    
    function drawCountMap(result,instance){
      var colors = instance.styles.colors;
      var sentimentColors = instance.styles.sentimentColors;
      var normalizedCountMax = 0,
          normalizedCountMin = 0,
          intervals = colors.length - 1,
          difference = 0;

      function getSentimentColor(d) {
        if( d < cloudberryConfig.sentimentUpperBound / 3) {    // 1/3
          return sentimentColors[0];
        } else if( d < 2 * cloudberryConfig.sentimentUpperBound / 3){    // 2/3
          return sentimentColors[1];
        } else{     // 3/3
          return sentimentColors[2];
        }
      }

      function getNormalizedCountColor(d) {
        var i = 1;
        for (; i <= intervals; i++){
          if ( d <= normalizedCountMin + ((i * difference) / intervals)){  // bound = min + (i / 6) * difference
            return colors[i];
          }
        }
        return colors[intervals]; // in case of max
      }

      function getUnnormalizedCountColor(d) {
        if(!d || d <= 0) {
          d = 0;
        } else if (d ===1 ){
          d = 1;
        } else {
          d = Math.ceil(Math.log10(d));
          if(d <= 0){ // treat smaller counts the same as 0
            d = 0;
          }
        }
        d = Math.min(d, colors.length-1);
        return colors[d];
      }

      function getColor(d) {
        if(instance.doSentiment){  // 0 <= d <= 4
          return getSentimentColor(d);
        }
        else if(instance.doNormalization){
          return getNormalizedCountColor(d);
        }
        else{
          return getUnnormalizedCountColor(d);
        }
      }

      function style(feature) {
        if (!feature.properties.count || feature.properties.count === 0){
          return {
            fillColor: "#f7f7f7",
            weight: 1.5,
            opacity: 1,
            color: "#92d1e1",
            fillOpacity: 0.5
          };
        } else {
          return {
            fillColor: getColor(feature.properties.count),
            weight: 1.5,
            opacity: 1,
            color: "#92d1e1",
            fillOpacity: 0.5
          };
        }
      }

      function setNormalizedCountText(geo){
        // beautify 0.0000123 => 1.23e-5, 1.123 => 1.1
        if(geo["properties"]["count"] < 1){
          geo["properties"]["countText"] = geo["properties"]["count"].toExponential(1);
        } else{
          geo["properties"]["countText"] = geo["properties"]["count"].toFixed(1);
        }
        geo["properties"]["countText"] += cloudberryConfig.normalizationUpscaleText; // "/M"
      }

      function resetCount(geo) {
        if (geo["properties"]["count"]){
          geo["properties"]["count"] = 0;
        }
        if (geo["properties"]["countText"]){
          geo["properties"]["countText"] = "";
        }
      }

      function setNormalizedCount(geo, r){
        var normalizedCount = r["count"] / r["population"] * cloudberryConfig.normalizationUpscaleFactor;
        geo["properties"]["count"] = normalizedCount;
        if(normalizedCount > normalizedCountMax){  // update max to enable dynamic legends
          normalizedCountMax = normalizedCount;
        }
        setNormalizedCountText(geo);
      }

      function setUnnormalizedCount(geo ,r) {
        geo["properties"]["count"] = r["count"];
        geo["properties"]["countText"] = geo["properties"]["count"].toString();
      }

      function updateTweetCountInGeojson(){
        var level = cloudberry.parameters.geoLevel;
        var geojsonData = instance.geojsonData[level];
        if(geojsonData){
          angular.forEach(geojsonData["features"], function (geo) {
            resetCount(geo);
            angular.forEach(result, function (r) {
              if (r[level] === geo["properties"][level+"ID"]){
                if(instance.doSentiment){
                  // sentimentScore for all the tweets in the same polygon / number of tweets with the score
                  geo["properties"]["count"] = r["sentimentScoreSum"] / r["sentimentScoreCount"];
                  geo["properties"]["countText"] = geo["properties"]["count"].toFixed(1);
                } else if (instance.doNormalization) {
                  setNormalizedCount(geo, r);
                } else{
                  setUnnormalizedCount(geo, r);
                }
              }
            });
          });
          difference = normalizedCountMax - normalizedCountMin;  // to enable dynamic legend for normalization
          // draw
          instance.polygons[level+"Polygons"].setStyle(style);
        }
      }

      // Loop through each result and update its count information on its associated geo record
      updateTweetCountInGeojson();

      /**
           * add information control: legend, toggle
           * */

      function addMapControl(name, position, initDiv, initJS){
        var ctrlClass = $("."+name);
        if (ctrlClass) {
          ctrlClass.remove();
        }

        instance[name]= L.control({
          position
        });

        instance[name].onAdd = function() {
          var div = L.DomUtil.create("div", "info " + name);
          initDiv(div);
          return div;
        };


        if (instance.map) {   
          instance[name].addTo(instance.map);
          if (initJS){
            initJS();
          }
        }
      }

      function initNormalize(div) {

        if(instance.doNormalization){
          div.innerHTML = "<p>Normalize</p><input id=\"toggle-normalize\" checked type=\"checkbox\">";
        }
        else{
          div.innerHTML = "<p>Normalize</p><input id=\"toggle-normalize\" type=\"checkbox\">";
        }
      }

      function initNormalizeToggle() {
        var toggle = $("#toggle-normalize");
        toggle.bootstrapToggle({
          on: "By Population"
        });
        if(instance.doSentiment){
          toggle.bootstrapToggle("off");
          toggle.bootstrapToggle("disable");
        }
      }

      function initSentiment(div) {
        if(instance.doSentiment){
          div.innerHTML = "<p>Sentiment Analysis</p><input id=\"toggle-sentiment\" checked type=\"checkbox\">";
        }
        else{
          div.innerHTML = "<p>Sentiment Analysis</p><input id=\"toggle-sentiment\" type=\"checkbox\">";
        }
      }

      function initSentimentToggle() {
        $("#toggle-sentiment").bootstrapToggle({
          on: "By OpenNLP"
        });
      }

      function setSentimentLegend(div) {
        div.setAttribute("title", "Sentiment Score: Negative(0)-Positive(4)");  // add tool-tips for the legend
        div.innerHTML +=
          "<i style='background:" + getColor(1) + "'></i>Negative<br>";
        div.innerHTML +=
          "<i style='background:" + getColor(2) + "'></i>Neutral<br>";
        div.innerHTML +=
          "<i style='background:" + getColor(3) + "'></i>Positive<br>";
      }

      function setGrades(grades) {
        var i = 0;
        for(; i < grades.length; i++){
          if (instance.doNormalization){
            grades[i] = normalizedCountMin + ((i * difference) / intervals);
          }
          else{
            grades[i] = Math.pow(10, i);
          }
        }
      }

      function getGradesNames(grades) {
        return grades.map( function(d) {
          var returnText = "";
          if (d < 1000){
            returnText = d.toFixed();
          } else if (d < 1000 * 1000) {
            returnText = (d / 1000).toFixed() + "K";
          } else if (d < 1000 * 1000 * 1000) {
            returnText = (d / 1000 / 1000).toFixed() + "M";
          } else{
            returnText = (d / 1000 / 1000).toFixed() + "M+";
          }
          if(instance.doNormalization){
            return returnText + cloudberryConfig.normalizationUpscaleText; //["1/M", "10/M", "100/M", "1K/M", "10K/M", "100K/M"];
          }
          else{
            return returnText; //["1", "10", "100", "1K", "10K", "100K"];
          }
        });
      }

      function setCountLegend(div) {
        var grades = new Array(colors.length -1); //[1, 10, 100, 1000, 10000, 100000]
        setGrades(grades);
        var gName  = getGradesNames(grades);
        if(instance.doNormalization){
          div.setAttribute("title", "# of Tweets per Million People");  // add tool-tips for the legend to explain the meaning of "M"
        }
        // loop through our density intervals and generate a label with a colored square for each interval
        
        var i = 1;
        for (; i < grades.length; i++) {
          div.innerHTML +=
            "<i style='background:" + getColor(grades[i]) + "'></i>" + gName[i-1] + "&ndash;" + gName[i] + "<br>";
        }
        if (instance.doNormalization){
          div.innerHTML += "<i style='background:" + getColor(grades[i-1] + ((difference) / intervals)) + "'></i> " + gName[i-1] + "+";
        }
        else{
          div.innerHTML += "<i style='background:" + getColor(grades[i-1]*10) + "'></i> " + gName[i-1] + "+";
        }
      }

      function initLegend(div) {
        if(instance.doSentiment){
          setSentimentLegend(div);
        } else {
          setCountLegend(div);
        }
      }


      if(cloudberry.parameters.maptype==="countmap"){
        // add legend
        // add toggle normalize
        addMapControl("legend", "topleft", initLegend, null);
        addMapControl("normalize", "topleft", initNormalize, initNormalizeToggle);
      }
      

      instance.normalize =  $("#toggle-normalize").prop("checked");  
    }
  
    // Send query to cloudberry
    function sendCountmapQuery(instance) {
      var scope = instance.scope;
      // For time-series histogram, get geoIds not in the time series cache.
      scope.geoIdsNotInTimeSeriesCache = TimeSeriesCache.getGeoIdsNotInCache(cloudberry.parameters.keywords,
        cloudberry.parameters.timeInterval, cloudberry.parameters.geoIds, cloudberry.parameters.geoLevel);

      // Batch request without map result - used when the complete map result cache hit,
      // partial time series result cache hit case
      var batchWithoutGeoRequest = cloudberryConfig.querySliceMills > 0 ? {
        batch: [queryUtil.byTimeRequest(cloudberry.parameters, scope.geoIdsNotInTimeSeriesCache)],
        option: {
          sliceMillis: cloudberryConfig.querySliceMills
        }
      } : {
        batch: [queryUtil.byTimeRequest(cloudberry.parameters, scope.geoIdsNotInTimeSeriesCache)]
      };

      // Gets the Geo IDs that are not in the map result cache.
      scope.geoIdsNotInCache = MapResultCache.getGeoIdsNotInCache(cloudberry.parameters.keywords,
        cloudberry.parameters.timeInterval,
        cloudberry.parameters.geoIds, cloudberry.parameters.geoLevel);

      // Batch request without time series result - used when the complete time series cache hit, partial map result cache hit case
      var batchWithoutTimeRequest = cloudberryConfig.querySliceMills > 0 ? {
        batch: [queryUtil.byGeoRequest(cloudberry.parameters, scope.geoIdsNotInCache)],
        option: {
          sliceMillis: cloudberryConfig.querySliceMills
        }
      } : {
        batch: [queryUtil.byGeoRequest(cloudberry.parameters, scope.geoIdsNotInCache)]
      };

      // Batch request with only the geoIds whose map result or time series are not cached yet - partial map result cache hit case
      // This case also covers the complete cache miss case.
      var batchWithPartialRequest = cloudberryConfig.querySliceMills > 0 ? {
        batch: [queryUtil.byTimeRequest(cloudberry.parameters, scope.geoIdsNotInTimeSeriesCache),
          queryUtil.byGeoRequest(cloudberry.parameters, scope.geoIdsNotInCache)],
        option: {
          sliceMillis: cloudberryConfig.querySliceMills
        }
      } : {
        batch: [queryUtil.byTimeRequest(cloudberry.parameters, scope.geoIdsNotInTimeSeriesCache),
          queryUtil.byGeoRequest(cloudberry.parameters, scope.geoIdsNotInCache)]
      };

      // Complete map result cache and time series cache hit case
      if(scope.geoIdsNotInCache.length === 0 && scope.geoIdsNotInTimeSeriesCache.length === 0)  {
        cloudberry.countmapMapResult = MapResultCache.getValues(cloudberry.parameters.geoIds,
          cloudberry.parameters.geoLevel);
        cloudberry.commonTimeSeriesResult = TimeSeriesCache.getTimeSeriesValues(cloudberry.parameters.geoIds,
          cloudberry.parameters.geoLevel, cloudberry.parameters.timeInterval);
        drawCountMap(cloudberry.countmapMapResult,instance);
      }
      // Complete map result cache hit case - exclude map result request
      else if(scope.geoIdsNotInCache.length === 0)  {
        cloudberry.countmapMapResult = MapResultCache.getValues(cloudberry.parameters.geoIds,
          cloudberry.parameters.geoLevel);
        drawCountMap(cloudberry.countmapMapResult,instance);

        cloudberryClient.send(batchWithoutGeoRequest, function(id, resultSet, resultTimeInterval){
          if(angular.isArray(resultSet)) {
            var requestTimeRange = {
              start: new Date(resultTimeInterval.start),
              end: new Date(resultTimeInterval.end)
            };
            // Since the middleware returns the query result in multiple steps,
            // cloudberryService.timeSeriesQueryResult stores the current intermediate result.
            cloudberry.timeSeriesQueryResult = resultSet[0];
            // Avoid memory leak.
            resultSet[0] = [];
            cloudberry.commonTimeSeriesResult =
              TimeSeriesCache.getValuesFromResult(cloudberry.timeSeriesQueryResult).concat(
                TimeSeriesCache.getTimeSeriesValues(cloudberry.parameters.geoIds, cloudberry.parameters.geoLevel, requestTimeRange));
          }
          // When the query is executed completely, we update the time series cache.
          if((cloudberryConfig.querySliceMills > 0 && !angular.isArray(resultSet) &&
            resultSet["key"] === "done") || cloudberryConfig.querySliceMills <= 0) {
            TimeSeriesCache.putTimeSeriesValues(scope.geoIdsNotInTimeSeriesCache,
              cloudberry.timeSeriesQueryResult, cloudberry.parameters.timeInterval);
          }
        }, "batchWithoutGeoRequest");
      }
      // Complete time series cache hit case - exclude time series request
      else if(scope.geoIdsNotInTimeSeriesCache.length === 0)  {
        cloudberry.countmapPartialMapResult = MapResultCache.getValues(cloudberry.parameters.geoIds,
          cloudberry.parameters.geoLevel);

        cloudberryClient.send(batchWithoutTimeRequest, function(id, resultSet, resultTimeInterval){
          if(angular.isArray(resultSet)) {
            var requestTimeRange = {
              start: new Date(resultTimeInterval.start),
              end: new Date(resultTimeInterval.end)
            };
            cloudberry.countmapMapResult = resultSet[0].concat(cloudberry.countmapPartialMapResult);
            drawCountMap(cloudberry.countmapMapResult,instance);
            cloudberry.commonTimeSeriesResult = TimeSeriesCache.getTimeSeriesValues(cloudberry.parameters.geoIds,
              cloudberry.parameters.geoLevel, requestTimeRange);
          }
          // When the query is executed completely, we update the map result cache.
          if((cloudberryConfig.querySliceMills > 0 && !angular.isArray(resultSet) &&
            resultSet["key"] === "done") || cloudberryConfig.querySliceMills <= 0) {
            MapResultCache.putValues(scope.geoIdsNotInCache, cloudberry.parameters.geoLevel,
              cloudberry.countmapMapResult);
          }
        }, "batchWithoutTimeRequest");
      }
      // Partial map result cache hit case
      else {
        cloudberry.countmapPartialMapResult = MapResultCache.getValues(cloudberry.parameters.geoIds,
          cloudberry.parameters.geoLevel);

        cloudberryClient.send(batchWithPartialRequest, function(id, resultSet, resultTimeInterval){
          if(angular.isArray(resultSet)) {
            var requestTimeRange = {
              start: new Date(resultTimeInterval.start),
              end: new Date(resultTimeInterval.end)
            };
            // Since the middleware returns the query result in multiple steps,
            // cloudberry.timeSeriesQueryResult stores the current intermediate result.
            cloudberry.timeSeriesQueryResult = resultSet[0];
            // Avoid memory leak.
            resultSet[0] = [];
            cloudberry.commonTimeSeriesResult = TimeSeriesCache.getValuesFromResult(cloudberry.timeSeriesQueryResult).concat(
              TimeSeriesCache.getTimeSeriesValues(cloudberry.parameters.geoIds, cloudberry.parameters.geoLevel, requestTimeRange));
            cloudberry.countmapMapResult = resultSet[1].concat(cloudberry.countmapPartialMapResult);
            drawCountMap(cloudberry.countmapMapResult,instance);
          }
          // When the query is executed completely, we update the map result cache and time series cache.
          if((cloudberryConfig.querySliceMills > 0 && !angular.isArray(resultSet) &&
            resultSet["key"] === "done") || cloudberryConfig.querySliceMills <= 0) {
            MapResultCache.putValues(scope.geoIdsNotInCache, cloudberry.parameters.geoLevel,
              cloudberry.countmapMapResult);
            TimeSeriesCache.putTimeSeriesValues(scope.geoIdsNotInTimeSeriesCache,
              cloudberry.timeSeriesQueryResult, cloudberry.parameters.timeInterval);
          }
        }, "batchWithPartialRequest");
      }
    }
    
    function countmapHandler(instance){
      sendCountmapQuery(instance);
    }
  
    function onMapTypeChangeHandler(instance)
    {
        // add info control
        instance.info.addTo(instance.map);
      
        countmapHandler(instance);
    }
    
    function cleanCountMap(){

      function removeMapControl(name){
        var ctrlClass = $("."+name);
        if (ctrlClass) {
          ctrlClass.remove();
        }
      }

      // remove CountMap controls
      removeMapControl("info");
      removeMapControl("legend");
      removeMapControl("normalize");
    }

    function resetGeoIds(bounds, polygons, idTag) {
      cloudberry.parameters.geoIds = [];
      if (polygons !== undefined) {
        polygons.features.forEach(function (polygon) {
          if (bounds._southWest.lat <= polygon.properties.centerLat &&
              polygon.properties.centerLat <= bounds._northEast.lat &&
              bounds._southWest.lng <= polygon.properties.centerLog &&
              polygon.properties.centerLog <= bounds._northEast.lng) {
            cloudberry.parameters.geoIds.push(polygon.properties[idTag]);
          }
        });
      }
    }

    function zoomFunction(instance){
      var scope = instance.scope;
      function resetGeoInfo(level) {
        cloudberry.parameters.geoLevel = level;
        cloudberry.parameters.geoLevel = level;
        if (instance.geojsonData[level]){
          resetGeoIds(instance.bounds, instance.geojsonData[level], level + "ID");
        }
      }
      if (instance.map) {
        instance.status.zoomLevel = instance.map.getZoom();
        instance.bounds = instance.map.getBounds();
        if (instance.status.zoomLevel > 9) {
          resetGeoInfo("city");
          if (instance.polygons.statePolygons) {
            instance.layer.removeLayer(instance.polygons.statePolygons);
          }
          if (instance.polygons.countyPolygons) {
            instance.layer.removeLayer(instance.polygons.countyPolygons);
          }
          if (instance.polygons.stateUpperPolygons) {
            instance.layer.removeLayer(instance.polygons.stateUpperPolygons);
          }
          instance.layer.addLayer(instance.polygons.countyUpperPolygons);
          loadCityJsonByBound(instance.onEachFeature,instance);
        } else if (instance.status.zoomLevel > 5) {
          resetGeoInfo("county");
          scope.resetGeoInfo("county");
          //TODO publish events
          //moduleManager.publishEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, {level: instance.map.getZoom(), bounds: instance.map.getBounds()});
          if (instance.polygons.statePolygons) {
            instance.layer.removeLayer(instance.polygons.statePolygons);
          }
          if (instance.polygons.cityPolygons) {
            instance.layer.removeLayer(instance.polygons.cityPolygons);
          }
          if (instance.polygons.countyUpperPolygons) {
            instance.layer.removeLayer(instance.polygons.countyUpperPolygons);
          }
          instance.layer.addLayer(instance.polygons.stateUpperPolygons);
          instance.layer.addLayer(instance.polygons.countyPolygons);
        } else if (instance.status.zoomLevel <= 5) {
          resetGeoInfo("state");
          if(!instance.status.init){
            //TODO publish events
            //moduleManager.publishEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, {level: instance.map.getZoom(), bounds: instance.map.getBounds()});
          }
          if (!instance.status.init) {
          cloudberry.query(cloudberry.parameters);
          }
          if (instance.polygons.countyPolygons) {
            instance.layer.removeLayer(instance.polygons.countyPolygons);
          }
          if (instance.polygons.cityPolygons) {
            instance.layer.removeLayer(instance.polygons.cityPolygons);
          }
          if (instance.polygons.stateUpperPolygons) {
            instance.layer.removeLayer(instance.polygons.stateUpperPolygons);
          }
          if (instance.polygons.countyUpperPolygons) {
            instance.layer.removeLayer(instance.polygons.countyUpperPolygons);
          }
          if (instance.polygons.statePolygons) {
            instance.layer.addLayer(instance.polygons.statePolygons);
          }
        }
      }
    }

    function dragFunction(instance){
      instance.bounds = instance.map.getBounds();
      var geoData;
      if (cloudberry.parameters.geoLevel === "state") {
        geoData = instance.geojsonData.state;
      } else if (cloudberry.parameters.geoLevel === "county") {
        geoData = instance.geojsonData.county;
      } else if (cloudberry.parameters.geoLevel === "city") {
        geoData = instance.geojsonData.city;
      } else {
        console.error("Error: Illegal value of logicLevel, set to default: state");
        cloudberry.parameters.geoLevel = "state";
        geoData = instance.geojsonData.state;
      }  
      if (cloudberry.parameters.geoLevel === "city") {
          loadCityJsonByBound(instance.onEachFeature,instance);
      } else {
        resetGeoIds(instance.bounds, geoData, cloudberry.parameters.geoLevel + "ID");
        if(!instance.status.init){
          cloudberry.parameters.geoLevel = cloudberry.parameters.geoLevel;
        }
        //TODO publish events
        //moduleManager.publishEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, {bounds: instance.map.getBounds()});
      }
    }

    function initCountMap(scope,instance){
      this.scope = scope;
      this.doNormalization = false;
      this.doSentiment = false;
      instance.selectedPlace = "no place selected";
      instance.countText = "0";
      this.layer = L.layerGroup();
      instance.normalize = null;
      
      scope.$on("leafletDirectiveMap.zoomend",function(){       
          zoomFunction(instance);   
      });
      scope.$on("leafletDirectiveMap.dragend", function(){     
          dragFunction(instance);       
      });
      var countmapStyle = {
        initStyle: {
          weight: 1.5,
          fillOpacity: 0.5,
          color: "white"
        },
        stateStyle: {
          fillColor: "#f7f7f7",
          weight: 1.5,
          opacity: 1,
          color: "#92d1e1",
          fillOpacity: 0.5
        },
        stateUpperStyle: {
          fillColor: "#f7f7f7",
          weight: 1.5,
          opacity: 1,
          color: "#92d1e1",
          fillOpacity: 0.5
        },
        countyStyle: {
          fillColor: "#f7f7f7",
          weight: 1.5,
          opacity: 1,
          color: "#92d1e1",
          fillOpacity: 0.5
        },
        countyUpperStyle: {
          fillColor: "#f7f7f7",
          weight: 1.5,
          opacity: 1,
          color: "#92d1e1",
          fillOpacity: 0.5
        },
        cityStyle: {
          fillColor: "#f7f7f7",
          weight: 1.5,
          opacity: 1,
          color: "#92d1e1",
          fillOpacity: 0.5
        },
        hoverStyle: {
          weight: 5,
          color: "#666",
          fillOpacity: 0.5
        },
        colors: [ "#ffffff", "#92d1e1", "#4393c3", "#2166ac", "#f4a582", "#d6604d", "#b2182b"],
        sentimentColors: ["#ff0000", "#C0C0C0", "#00ff00"]
      };

      this.styles = countmapStyle;

      function highlightFeature(leafletEvent) {
        var layer = leafletEvent.target;
        layer.setStyle(countmapStyle.hoverStyle);
        if (!L.Browser.ie && !L.Browser.opera) {
          layer.bringToFront();
        }

        instance.selectedPlace = layer.feature.properties.name;
        instance.countText = layer.feature.properties.countText;
      }

      // remove the highlight interaction function for the polygons
      function resetHighlight(leafletEvent) {
        var style = {
          weight: 1.5,
          fillOpacity: 0.5,
          color: "#92d1e1"
        };
        if (leafletEvent){
          leafletEvent.target.setStyle(style);
        }
      }

      // add info control
      var info = L.control();
      var placeName = "No place selected";
      var countText = "0";
      var infoDiv;
      
      info.onAdd = function() {
        this._div = L.DomUtil.create("div", "info"); // create a div with a class "info"
        this._div.style.margin = "20% 0 0 0";
        infoDiv = this._div;
        instance.infoDiv = infoDiv;
        this._div.innerHTML = [
          "<h4>Count: by "+cloudberry.parameters.geoLevel+"</h4>",
          "<b>"+placeName+"</b>",
          "<br/>Count: "+countText,
          ""
        ].join("");
        $compile(this._div)(this);
        return this._div;
      };     

      info.options = {
        position: "topleft"
      };
      
      instance.info = info;
      
      instance.scope.$watch(function(){
        return instance.map;
      },function(result){
        if(cloudberry.parameters.maptype==="countmap"){
          info.addTo(instance.map);
        }
      });

      //watch variable for left up corner"s info control
      scope.$watchCollection(function(){
        return { "selectedPalace":instance.selectedPlace,
                 "geoLevel":cloudberry.parameters.geoLevel};

      },function(oldResult,newResult){

        if(!instance.countText){
          instance.countText = "0";
        }

          infoDiv.innerHTML = innerHTML = [
          "<h4>Count: by "+cloudberry.parameters.geoLevel+"</h4>",
          "<b>"+instance.selectedPlace+"</b>",
          "<br/>Count: "+instance.countText,
          ""].join("");


      });
      
      // add feature to each polygon
      // highlight a polygon when mouseover
      // remove the highlight when mouseout
      // zoom in to fit the polygon when the polygon is clicked
      this.onEachFeature = function onEachFeature(feature, layer) {
        layer.on({
          mouseover: highlightFeature,
          mouseout: resetHighlight,
          click:scope.zoomToFeature
        });
      };

      leafletData.getMap().then(function(map){
        instance.map = map;
      });
      
      //watch normalize switch and redraw map, when switch is on.
      scope.$watch(function(){
        return $("#toggle-normalize").prop("checked");
      },function(resultN){
        scope.doNormalization = resultN;
        instance.doNormalization = resultN;
        if(cloudberry.parameters.maptype==="countmap")
        {
          drawCountMap(cloudberry.countmapMapResult,instance);    
        }
      });
      
      scope.loadGeoJsonFiles(this.onEachFeature);
      
      //Change the init to be false, otherwise map/controllers.js will not publish zoom/drag events
      scope.status.init = false;

      // update the center and the boundary of the visible area of the map
      function setCenterAndBoundry(features){
        for (var id in features){
          var minLog = Number.POSITIVE_INFINITY;
          var maxLog = Number.NEGATIVE_INFINITY;
          var minLat = Number.POSITIVE_INFINITY;
          var maxLat = Number.NEGATIVE_INFINITY;
          if (features[id].geometry.type === "Polygon"){
            features[id].geometry.coordinates[0].forEach(function(pair){
              minLog = Math.min(minLog, pair[0]);
              maxLog = Math.max(maxLog, pair[0]);
              minLat = Math.min(minLat, pair[1]);
              maxLat = Math.max(maxLat, pair[1]);
            });
          } else if( features[id].geometry.type === "MultiPolygon") {
            features[id].geometry.coordinates.forEach(function(array){
              array[0].forEach(function(pair){
                minLog = Math.min(minLog, pair[0]);
                maxLog = Math.max(maxLog, pair[0]);
                minLat = Math.min(minLat, pair[1]);
                maxLat = Math.max(maxLat, pair[1]);
              });
            });
          }
          features[id].properties["centerLog"] = (maxLog + minLog) / 2;
          features[id].properties["centerLat"] = (maxLat + minLat) / 2;
        }
      }

      // reset the geo level (state, county, city)
      function resetGeoInfo(level) {
        cloudberry.parameters.geoLevel = level;
        cloudberry.parameters.geoLevel = level;
        if (instance.geojsonData[level]){
          resetGeoIds(instance.bounds, instance.geojsonData[level], level + "ID");
        }
      }

      var deferred = $q.defer();

      var statePolygonsReady = false;
      var countyPolygonsReady = false;
      resetGeoInfo("state");

      // load geoJson to get state and county polygons
      if (!this.polygons.statePolygons){
        $http.get("assets/data/state.json")
          .success(function(data) {
          instance.geojsonData.state = data;
          instance.polygons.statePolygons = L.geoJson(data, {
            style: countmapStyle.stateStyle,
            onEachFeature: instance.onEachFeature
          });
          instance.polygons.stateUpperPolygons = L.geoJson(data, {
            style: countmapStyle.stateUpperStyle
          });
          setCenterAndBoundry(instance.geojsonData.state.features);
          instance.layer.addLayer(instance.polygons.statePolygons);
          if (countyPolygonsReady){
            deferred.resolve();
          }
          else {
            statePolygonsReady = true;
          }
        })
          .error(function(data) {
          console.error("Load state data failure");
          if (countyPolygonsReady){
            deferred.resolve();
          }
          else {
            statePolygonsReady = true;
          }
        });
      }
      if (!this.polygons.countyPolygons){
        $http.get("assets/data/county.json")
          .success(function(data) {
          instance.geojsonData.county = data;
          instance.polygons.countyPolygons = L.geoJson(data, {
            style: countmapStyle.countyStyle,
            onEachFeature: instance.onEachFeature
          });
          instance.polygons.countyUpperPolygons = L.geoJson(data, {
            style: countmapStyle.countyUpperStyle
          });
          setCenterAndBoundry(instance.geojsonData.county.features);
          if (statePolygonsReady){
            deferred.resolve();
          }
          else {
            countyPolygonsReady = true;
          }
        })
          .error(function(data) {
          console.error("Load county data failure");
          if (statePolygonsReady){
            deferred.resolve();
          }
          else {
            countyPolygonsReady = true;
          }
        });
      }

      return deferred.promise;
    }

    var countmapService = {
      createLayer: function(parameters){
        var deferred = $q.defer();
        deferred.resolve({
          active: 0,
          layer: {},
          init: initCountMap,
          onMapTypeChange: onMapTypeChangeHandler,
          onChangeSearchKeyword:countmapHandler,
          onChangeTimeSeriesRange:countmapHandler,
          onZoom: countmapHandler,
          onDrag: countmapHandler,
          clear: cleanCountMap,
          map: null,
          geojsonData: {},
          polygons: {},
          status: {}
        });
        return deferred.promise;
      }
    };

    return countmapService;
  });
