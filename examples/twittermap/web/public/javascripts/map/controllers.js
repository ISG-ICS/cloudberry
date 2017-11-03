angular.module('cloudberry.map', ['leaflet-directive', 'cloudberry.common','cloudberry.cache'])
  .controller('MapCtrl', function($scope, $window, $http, $compile, cloudberry, leafletData, cloudberryConfig, Cache) {

    // add an alert bar of IE
    if (L.Browser.ie) {
      var alertDiv = document.getElementsByTagName("alert-bar")[0];
      var div = L.DomUtil.create('div', 'alert alert-warning alert-dismissible')
      div.innerHTML = [
        '<a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a>',
        '<strong>Warning! </strong> TwitterMap currently doesn\'t support IE.'
      ].join('');
      div.style.position = 'absolute';
      div.style.top = '0%';
      div.style.width = '100%';
      div.style.zIndex = '9999';
      div.style.fontSize = '23px';
      alertDiv.appendChild(div);
    }

    $scope.result = {};
    $scope.doNormalization = false;
    $scope.doSentiment = false;
    $scope.infoPromp = config.mapLegend;
    
    //chanki
    $scope.maptype = 1;

    // map setting
    angular.extend($scope, {
      tiles: {
        name: 'Mapbox',
        url: 'https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}',
        type: 'xyz',
        options: {
          accessToken: 'pk.eyJ1IjoiamVyZW15bGkiLCJhIjoiY2lrZ2U4MWI4MDA4bHVjajc1am1weTM2aSJ9.JHiBmawEKGsn3jiRK_d0Gw',
          id: 'jeremyli.p6f712pj'
        }
      },
      controls: {
        custom: []
      },
      geojsonData: {},
      polygons: {},
      status: {
        init: true,
        zoomLevel: 4,
        logicLevel: 'state'
      },
      styles: {
        initStyle: {
          weight: 1.5,
          fillOpacity: 0.5,
          color: 'white'
        },
        stateStyle: {
          fillColor: '#f7f7f7',
          weight: 1.5,
          opacity: 1,
          color: '#92d1e1',
          fillOpacity: 0.5
        },
        stateUpperStyle: {
          fillColor: '#f7f7f7',
          weight: 1.5,
          opacity: 1,
          color: '#92d1e1',
          fillOpacity: 0.5
        },
        countyStyle: {
          fillColor: '#f7f7f7',
          weight: 1.5,
          opacity: 1,
          color: '#92d1e1',
          fillOpacity: 0.5
        },
        countyUpperStyle: {
          fillColor: '#f7f7f7',
          weight: 1.5,
          opacity: 1,
          color: '#92d1e1',
          fillOpacity: 0.5
        },
        cityStyle: {
          fillColor: '#f7f7f7',
          weight: 1.5,
          opacity: 1,
          color: '#92d1e1',
          fillOpacity: 0.5
        },
        hoverStyle: {
          weight: 5,
          color: '#666',
          fillOpacity: 0.5
        },
        colors: [ '#ffffff', '#92d1e1', '#4393c3', '#2166ac', '#f4a582', '#d6604d', '#b2182b'],
        sentimentColors: ['#ff0000', '#C0C0C0', '#00ff00']
      }
    });

    function resetGeoIds(bounds, polygons, idTag) {
      cloudberry.parameters.geoIds = [];
        if (polygons != undefined) {
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

    function resetGeoInfo(level) {
      $scope.status.logicLevel = level;
      cloudberry.parameters.geoLevel = level;
      if ($scope.geojsonData[level])
        resetGeoIds($scope.bounds, $scope.geojsonData[level], level + 'ID');
    }


    // initialize
    $scope.init = function() {
      leafletData.getMap().then(function(map) {
        $scope.map = map;
        $scope.bounds = map.getBounds();
        //making attribution control to false to remove the default leaflet sign in the bottom of map
        map.attributionControl.setPrefix(false);
        map.setView([$scope.lat, $scope.lng],$scope.zoom);
      });

      //Reset Zoom Button
      var button = document.createElement("a");
      var text =  document.createTextNode("Reset");
      button.appendChild(text);
      button.title = "Reset";
      button.href = "#";
      button.style.position = 'inherit';
      button.style.top = '150%';
      button.style.left = '-53%';
      var body = document.getElementsByTagName("search-bar")[0];
      body.appendChild(button);
      button.addEventListener ("click", function() {
        $scope.map.setView([$scope.lat, $scope.lng], 4);
      });

      //Adjust Map to be County or State
      setInfoControl();
      
      //chanki add map choose icons here
      var img1 = document.createElement("img");
      img1.title = "Count Map";
      img1.id = "Count Map";
      img1.src = "/assets/images/aggregation_map.png";
      img1.width = "70";
      img1.height = "70";
      img1.style.position = 'inherit';
      img1.style.top = '200%';
      img1.style.left = '20%';
      body.appendChild(img1); //instrumental
       /* var imgbody = document.getElementById("ResetButton");
        imgbody.appendChild(img1); */   // this method isn't work.

      img1.addEventListener("click",function () {

          console.log('1111111111');
          $scope.maptype = 1;
          setInfoControl();
          cloudberry.parameters.maptype = $scope.maptype;
          cloudberry.query(cloudberry.parameters, cloudberry.queryType);


      });

        var img2 = document.createElement("img");
        img2.title = "heat Map";
        img2.id = "Heat Map";
        img2.src = "/assets/images/heat_map.png";
        img2.width = "60";
        img2.height = "60";
        img2.style.position = 'inherit';
        img2.style.top = '200%';
        img2.style.left= '35%';
        body.appendChild(img2); //instrumental

        img2.addEventListener("click",function () {

            //Interface with teyuc
            console.log('222222222');
            $scope.maptype = 2;
            setInfoControl();
            cloudberry.parameters.maptype = $scope.maptype;
            cloudberry.query(cloudberry.parameters, cloudberry.queryType);

        });

        var img3 = document.createElement("img");
        img3.title = "point Map";
        img3.id = "Point Map";
        img3.src = "/assets/images/point_map.png";
        img3.width = "65";
        img3.height = "55";
        img3.style.position = 'inherit';
        img3.style.top = '200%';
        img3.style.left = '50%';
        body.appendChild(img3); //instrumental

        img3.addEventListener("click",function () {

            console.log('333333333');
            $scope.maptype = 3;
            setInfoControl();
            cloudberry.parameters.maptype = $scope.maptype;
            cloudberry.query(cloudberry.parameters, cloudberry.queryType);


        });

    };


    function setInfoControl() {
      // Interaction function
      function highlightFeature(leafletEvent) {
        var layer = leafletEvent.target;
        layer.setStyle($scope.styles.hoverStyle);
        if (!L.Browser.ie && !L.Browser.opera) {
          layer.bringToFront();
        }
        $scope.selectedPlace = layer.feature;
      }

      function resetHighlight(leafletEvent) {
        var style;
        if (!$scope.status.init)
          style = {
            weight: 1.5,
            fillOpacity: 0.5,
            color: '#92d1e1'
          };
        else
          style = {
            weight: 1.5,
            fillOpacity: 0.5,
            color: '#92d1e1'
          };
        if (leafletEvent)
          leafletEvent.target.setStyle(style);
      }

      function zoomToFeature(leafletEvent) {
        if (leafletEvent)
          $scope.map.fitBounds(leafletEvent.target.getBounds());
      }

      function onEachFeature(feature, layer) {
        layer.on({
          mouseover: highlightFeature,
          mouseout: resetHighlight,
          click: zoomToFeature
        });
      }

      if ($scope.maptype == 1){
          // clear heat map
          if ($scope.heat){
            $scope.map.removeLayer($scope.heat);
            $scope.heat = null;
          }
          
          // add info control
          var info = L.control();

          info.onAdd = function() {
            this._div = L.DomUtil.create('div', 'info'); // create a div with a class "info"
            this._div.style.margin = '20% 0 0 0';
            this._div.innerHTML = [
              '<h4>{{ infoPromp }} by {{ status.logicLevel }}</h4>',
              '<b>{{ selectedPlace.properties.name || "No place selected" }}</b>',
              '<br/>',
              '{{ infoPromp }} {{ selectedPlace.properties.countText || "0" }}'
            ].join('');
            $compile(this._div)($scope);
            return this._div;
          };

          info.options = {
            position: 'topleft'
          };
          $scope.controls.custom.push(info);

          loadGeoJsonFiles(onEachFeature);
          
          if ($scope.zoomfunction){
            $scope.zoomfunction()
          }
          $scope.zoomfunction = $scope.$on("leafletDirectiveMap.zoomend", function() {
            if ($scope.map) {
              $scope.status.zoomLevel = $scope.map.getZoom();
              $scope.bounds = $scope.map.getBounds();
              if($scope.status.zoomLevel > 9) {
                resetGeoInfo("city");
                if ($scope.polygons.statePolygons) {
                  $scope.map.removeLayer($scope.polygons.statePolygons);
                }
                if ($scope.polygons.countyPolygons) {
                  $scope.map.removeLayer($scope.polygons.countyPolygons);
                }
                if ($scope.polygons.stateUpperPolygons) {
                  $scope.map.removeLayer($scope.polygons.stateUpperPolygons);
                }
                $scope.map.addLayer($scope.polygons.countyUpperPolygons);
                loadCityJsonByBound(onEachFeature);
              } else if ($scope.status.zoomLevel > 5) {
                resetGeoInfo("county");
                if (!$scope.status.init) {
                  cloudberry.query(cloudberry.parameters);
                }
                if($scope.polygons.statePolygons) {
                  $scope.map.removeLayer($scope.polygons.statePolygons);
                }
                if($scope.polygons.cityPolygons) {
                  $scope.map.removeLayer($scope.polygons.cityPolygons);
                }
                if($scope.polygons.countyUpperPolygons){
                  $scope.map.removeLayer($scope.polygons.countyUpperPolygons);
                }
                $scope.map.addLayer($scope.polygons.stateUpperPolygons);
                $scope.map.addLayer($scope.polygons.countyPolygons);
              } else if ($scope.status.zoomLevel <= 5) {
                resetGeoInfo("state");
                if (!$scope.status.init) {
                  cloudberry.query(cloudberry.parameters);
                }
                if($scope.polygons.countyPolygons) {
                  $scope.map.removeLayer($scope.polygons.countyPolygons);
                }
                if($scope.polygons.cityPolygons) {
                  $scope.map.removeLayer($scope.polygons.cityPolygons);
                }
                if ($scope.polygons.stateUpperPolygons) {
                  $scope.map.removeLayer($scope.polygons.stateUpperPolygons);
                }
                if($scope.polygons.countyUpperPolygons){
                  $scope.map.removeLayer($scope.polygons.countyUpperPolygons);
                }
                if ($scope.polygons.statePolygons) {
                    $scope.map.addLayer($scope.polygons.statePolygons);
                }
              }
            }
          });

          if ($scope.dragfunction){
            $scope.dragfunction()
          }
          $scope.dragfunction = $scope.$on("leafletDirectiveMap.dragend", function() {
            if (!$scope.status.init) {
              $scope.bounds = $scope.map.getBounds();
              var geoData;
              if ($scope.status.logicLevel === 'state') {
                geoData = $scope.geojsonData.state;
              } else if ($scope.status.logicLevel === 'county') {
                geoData = $scope.geojsonData.county;
              } else if ($scope.status.logicLevel === 'city') {
                geoData = $scope.geojsonData.city;
              } else {
                console.error("Error: Illegal value of logicLevel, set to default: state");
                $scope.status.logicLevel = 'state';
                geoData = $scope.geojsonData.state;
              }
            }
            if ($scope.status.logicLevel === 'city') {
              loadCityJsonByBound(onEachFeature);
            } else {
              resetGeoIds($scope.bounds, geoData, $scope.status.logicLevel + "ID");
              cloudberry.parameters.geoLevel = $scope.status.logicLevel;
              cloudberry.query(cloudberry.parameters);
            }
          });
      }
      else if ($scope.maptype == 2){
        if ($scope.polygons.statePolygons) {
          $scope.map.removeLayer($scope.polygons.statePolygons);
        }
        if($scope.polygons.countyPolygons) {
          $scope.map.removeLayer($scope.polygons.countyPolygons);
        }
        if($scope.polygons.cityPolygons) {
          $scope.map.removeLayer($scope.polygons.cityPolygons);
        }
        if ($scope.polygons.stateUpperPolygons) {
          $scope.map.removeLayer($scope.polygons.stateUpperPolygons);
        }
        if($scope.polygons.countyUpperPolygons){
          $scope.map.removeLayer($scope.polygons.countyUpperPolygons);
        }
        
        if ($scope.zoomfunction){
          $scope.zoomfunction()
        }
        $scope.zoomfunction = $scope.$on("leafletDirectiveMap.zoomend", function() {
            if ($scope.map) {
              $scope.status.zoomLevel = $scope.map.getZoom();
              console.log("zoom", $scope.status.zoomLevel);
              $scope.bounds = $scope.map.getBounds();
              if($scope.status.zoomLevel > 9) {
                loadCityJsonByBound(onEachFeature, false);
                resetGeoInfo("city");
                cloudberry.parameters.bounds = $scope.bounds;
                cloudberry.queryType = 'zoom';
                cloudberry.query(cloudberry.parameters, cloudberry.queryType);
              } else if ($scope.status.zoomLevel > 5) {
                resetGeoInfo("county");
                cloudberry.parameters.bounds = $scope.bounds;
                cloudberry.queryType = 'zoom';
                cloudberry.query(cloudberry.parameters, cloudberry.queryType);
              } else if ($scope.status.zoomLevel <= 5) {
                resetGeoInfo("state");
                cloudberry.parameters.bounds = $scope.bounds;
                cloudberry.queryType = 'zoom';
                cloudberry.query(cloudberry.parameters, cloudberry.queryType);
              }
            }
        });
        
        if ($scope.dragfunction){
          $scope.dragfunction()
        }
        $scope.dragfunction = $scope.$on("leafletDirectiveMap.dragend", function() {
            if (!$scope.status.init) {
              $scope.bounds = $scope.map.getBounds();
              var geoData;
              if ($scope.status.logicLevel === 'state') {
                geoData = $scope.geojsonData.state;
              } else if ($scope.status.logicLevel === 'county') {
                geoData = $scope.geojsonData.county;
              } else if ($scope.status.logicLevel === 'city') {
                geoData = $scope.geojsonData.city;
              } else {
                console.error("Error: Illegal value of logicLevel, set to default: state");
                $scope.status.logicLevel = 'state';
                geoData = $scope.geojsonData.state;
              }
            }
            if ($scope.status.logicLevel === 'city') {
              loadCityJsonByBound(onEachFeature);
            }
            else {
              resetGeoIds($scope.bounds, geoData, $scope.status.logicLevel + "ID");
              cloudberry.parameters.geoLevel = $scope.status.logicLevel;
              cloudberry.query(cloudberry.parameters);
            }
        });
        
        if (!$scope.heat){
            $scope.heat = L.heatLayer([]).addTo($scope.map);
        }
      }
    }

    function setCenterAndBoundry(features) {

      for(var id in features){
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
    // load geoJson
    function loadGeoJsonFiles(onEachFeature) {
      $http.get("assets/data/state.json")
        .success(function(data) {
          $scope.geojsonData.state = data;
          $scope.polygons.statePolygons = L.geoJson(data, {
            style: $scope.styles.stateStyle,
            onEachFeature: onEachFeature
          });
          $scope.polygons.stateUpperPolygons = L.geoJson(data, {
            style: $scope.styles.stateUpperStyle
          });
          setCenterAndBoundry($scope.geojsonData.state.features);
          if ($scope.status.logicLevel === 'state'){
            $scope.polygons.statePolygons.addTo($scope.map);
          }
        })
        .error(function(data) {
          console.error("Load state data failure");
        });
      $http.get("assets/data/county.json")
        .success(function(data) {
          $scope.geojsonData.county = data;
          $scope.polygons.countyPolygons = L.geoJson(data, {
            style: $scope.styles.countyStyle,
            onEachFeature: onEachFeature
          });
          $scope.polygons.countyUpperPolygons = L.geoJson(data, {
            style: $scope.styles.countyUpperStyle
          });
          setCenterAndBoundry($scope.geojsonData.county.features);
          if ($scope.status.logicLevel === 'county'){
            $scope.polygons.stateUpperPolygons.addTo($scope.map);
            $scope.polygons.countyPolygons.addTo($scope.map);
          }
        })
        .error(function(data) {
          console.error("Load county data failure");
        });
    }

    function loadCityJsonByBound(onEachFeature){

      var bounds = $scope.map.getBounds();
      var rteBounds = "city/" + bounds._northEast.lat + "/" + bounds._southWest.lat + "/" + bounds._northEast.lng + "/" + bounds._southWest.lng;

        // Caching feature only works when the given threshold is greater than zero.
        if (cloudberryConfig.cacheThreshold > 0) {
            Cache.getCityPolygonsFromCache(rteBounds).done(function(data) {
                $scope.geojsonData.city = data;

                if($scope.polygons.cityPolygons) {
                    $scope.map.removeLayer($scope.polygons.cityPolygons);
                }
                $scope.polygons.cityPolygons = L.geoJson(data, {
                    style: $scope.styles.cityStyle,
                    onEachFeature: onEachFeature
                });
                //set center and boundary done by Cache
                if (!$scope.status.init) {
                    resetGeoIds($scope.bounds, $scope.geojsonData.city, 'cityID');
                    cloudberry.parameters.geoLevel = 'city';
                    cloudberry.query(cloudberry.parameters);
                }
                resetGeoInfo("city");
                $scope.map.addLayer($scope.polygons.cityPolygons);
            });
        } else {
            // No caching used here.
            $http.get(rteBounds)
                .success(function (data) {
                    $scope.geojsonData.city = data;
                    if ($scope.polygons.cityPolygons) {
                        $scope.map.removeLayer($scope.polygons.cityPolygons);
                    }
                    $scope.polygons.cityPolygons = L.geoJson(data, {
                        style: $scope.styles.cityStyle,
                        onEachFeature: onEachFeature
                    });
                    setCenterAndBoundry($scope.geojsonData.city.features);
                    resetGeoInfo("city");
                    if (!$scope.status.init) {
                        cloudberry.query(cloudberry.parameters);
                    }
                    $scope.map.addLayer($scope.polygons.cityPolygons);
                })
                .error(function (data) {
                    console.error("Load city data failure");
                });
        }
    }


    /**
     * Update map based on a set of spatial query result cells
     * @param    result  =>  mapPlotData, an array of coordinate and weight objects
     */
    function drawMap(result) {

      var colors = $scope.styles.colors;
      var sentimentColors = $scope.styles.sentimentColors;
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
          if(d <= 0) // treat smaller counts the same as 0
            d = 0
        }
        d = Math.min(d, colors.length-1);
        return colors[d];
      }

      function getColor(d) {
        if($scope.doSentiment)  // 0 <= d <= 4
          return getSentimentColor(d);
        else if($scope.doNormalization)
          return getNormalizedCountColor(d);
        else
          return getUnnormalizedCountColor(d);
      }

      function style(feature) {
        if (!feature.properties.count || feature.properties.count === 0){
          return {
            fillColor: '#f7f7f7',
            weight: 1.5,
            opacity: 1,
            color: '#92d1e1',
            fillOpacity: 0.5
          };
        } else {
          return {
            fillColor: getColor(feature.properties.count),
            weight: 1.5,
            opacity: 1,
            color: '#92d1e1',
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
        if (geo['properties']['count'])
          geo['properties']['count'] = 0;
        if (geo['properties']['countText'])
          geo['properties']['countText'] = "";
      }

      function setNormalizedCount(geo, r){
        var normalizedCount = r['count'] / r['population'] * cloudberryConfig.normalizationUpscaleFactor;
        geo['properties']['count'] = normalizedCount;
        if(normalizedCount > normalizedCountMax)  // update max to enable dynamic legends
          normalizedCountMax = normalizedCount;
        setNormalizedCountText(geo);
      }

      function setUnnormalizedCount(geo ,r) {
        geo['properties']['count'] = r['count'];
        geo['properties']['countText'] = geo['properties']['count'].toString();
      }

      function updateTweetCountInGeojson(){
        var level = $scope.status.logicLevel;
        var geojsonData = $scope.geojsonData[level];
        if(geojsonData){
          angular.forEach(geojsonData['features'], function (geo) {
            resetCount(geo);
            angular.forEach(result, function (r) {
              if (r[level] === geo['properties'][level+"ID"]){
                if($scope.doSentiment){
                  // sentimentScore for all the tweets in the same polygon / number of tweets with the score
                  geo['properties']['count'] = r['sentimentScoreSum'] / r['sentimentScoreCount'];
                  geo["properties"]["countText"] = geo["properties"]["count"].toFixed(1);
                } else if ($scope.doNormalization) {
                  setNormalizedCount(geo, r);
                } else{
                  setUnnormalizedCount(geo, r);
                }
              }
            });
          });
          difference = normalizedCountMax - normalizedCountMin;  // to enable dynamic legend for normalization
          // draw
          $scope.polygons[level+"Polygons"].setStyle(style);
        }
      }

      if ($scope.maptype == 1){
        // Loop through each result and update its count information on its associated geo record
        updateTweetCountInGeojson();
      }
      else if ($scope.maptype = 2){
        function setHeatPoints(points) {
          $scope.heat.setLatLngs(points);
        }
        
        if (result.length > 0){
            var points = [];
            if ($scope.status.zoomLevel > 9){ // city
                var base_density = 1;
                if (result.length < 10000)
                    base_density = 10;
                
                for (var i = 0; i < result.length; i++) {
                    if (result[i].hasOwnProperty('coordinate')){
                        points.push([result[i].coordinate[1], result[i].coordinate[0], base_density]);
                    }
                    else {
                        points.push([rangeRandom(result[i].id, result[i]["place.bounding_box"][0][1], result[i]["place.bounding_box"][1][1]), rangeRandom(result[i].id + 79, result[i]["place.bounding_box"][0][0], result[i]["place.bounding_box"][1][0]), base_density]);
                    }
                }
            }
            else {
                var count = 0;
                var base_density = 1;
                for (var i = 0; i < result.length; i++)
                    count += result[i].count;
                if (count < 10000)
                    base_density = 10;
                
                for (var i = 0; i < result.length; i++) {
                    if (result[i].hasOwnProperty('coordinate')){
                        points.push([result[i].coordinate[1], result[i].coordinate[0], result[i].count * base_density]);
                    }
                    else {
                        points.push([(result[i]["place.bounding_box"][0][1] + result[i]["place.bounding_box"][1][1]) / 2, (result[i]["place.bounding_box"][0][0] + result[i]["place.bounding_box"][1][0]) / 2, result[i].count * base_density]);
                    }
                }
            }
            
            setHeatPoints(points);
        }
      }

      /**
       * add information control: legend, toggle
       * */

      function addMapControl(name, position, initDiv, initJS){
        var ctrlClass = $("."+name);
        if (ctrlClass) {
          ctrlClass.remove();
        }

        $scope[name]= L.control({
          position: position
        });

        $scope[name].onAdd = function() {
          var div = L.DomUtil.create('div', 'info ' + name);
          initDiv(div);
          return div;
        };
        if ($scope.map) {
          $scope[name].addTo($scope.map);
          if (initJS)
            initJS();
        }
      }

      function initNormalize(div) {
        if($scope.doNormalization)
          div.innerHTML = '<p>Normalize</p><input id="toggle-normalize" checked type="checkbox">';
        else
          div.innerHTML = '<p>Normalize</p><input id="toggle-normalize" type="checkbox">';
      }

      function initNormalizeToggle() {
        var toggle = $('#toggle-normalize');
        toggle.bootstrapToggle({
          on: "By Population"
        });
        if($scope.doSentiment){
          toggle.bootstrapToggle('off');
          toggle.bootstrapToggle('disable');
        }
      }

      function initSentiment(div) {
        if($scope.doSentiment)
          div.innerHTML = '<p>Sentiment Analysis</p><input id="toggle-sentiment" checked type="checkbox">';
        else
          div.innerHTML = '<p>Sentiment Analysis</p><input id="toggle-sentiment" type="checkbox">';
      }

      function initSentimentToggle() {
        $('#toggle-sentiment').bootstrapToggle({
          on: "By OpenNLP"
        });
      }

      function setSentimentLegend(div) {
        div.setAttribute("title", "Sentiment Score: Negative(0)-Positive(4)");  // add tool-tips for the legend
        div.innerHTML +=
          '<i style="background:' + getColor(1) + '"></i>Negative<br>';
        div.innerHTML +=
          '<i style="background:' + getColor(2) + '"></i>Neutral<br>';
        div.innerHTML +=
          '<i style="background:' + getColor(3) + '"></i>Positive<br>';
      }

      function setGrades(grades) {
        var i = 0;
        for(; i < grades.length; i++){
          if ($scope.doNormalization)
            grades[i] = normalizedCountMin + ((i * difference) / intervals);
          else
            grades[i] = Math.pow(10, i);
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
          if($scope.doNormalization)
            return returnText + cloudberryConfig.normalizationUpscaleText; //["1/M", "10/M", "100/M", "1K/M", "10K/M", "100K/M"];
          else
            return returnText; //["1", "10", "100", "1K", "10K", "100K"];
        });
      }

      function setCountLegend(div) {
        var grades = new Array(colors.length -1); //[1, 10, 100, 1000, 10000, 100000]
        setGrades(grades);
        var gName  = getGradesNames(grades);
        if($scope.doNormalization)
          div.setAttribute("title", "# of Tweets per Million People");  // add tool-tips for the legend to explain the meaning of "M"
        // loop through our density intervals and generate a label with a colored square for each interval
        i = 1;
        for (; i < grades.length; i++) {
          div.innerHTML +=
            '<i style="background:' + getColor(grades[i]) + '"></i>' + gName[i-1] + '&ndash;' + gName[i] + '<br>';
        }
        if ($scope.doNormalization)
          div.innerHTML += '<i style="background:' + getColor(grades[i-1] + ((difference) / intervals)) + '"></i> ' + gName[i-1] + '+';
        else
          div.innerHTML += '<i style="background:' + getColor(grades[i-1]*10) + '"></i> ' + gName[i-1] + '+';
      }

      function initLegend(div) {
        if($scope.doSentiment){
          setSentimentLegend(div);
        } else {
          setCountLegend(div);
        }
      }

      if ($scope.maptype == 1){
          // add legend
          addMapControl('legend', 'topleft', initLegend, null);

          // add toggle normalize
          addMapControl('normalize', 'topleft', initNormalize, initNormalizeToggle);

          // add toggle sentiment analysis
          if(cloudberryConfig.sentimentEnabled)
            addMapControl('sentiment', 'topleft', initSentiment, initSentimentToggle);
      }

    }

    $scope.$watchCollection(
      function() {
        return {
          'mapResult': cloudberry.mapResult,
          'totalCount': cloudberry.totalCount,
          'doNormalization': $('#toggle-normalize').prop('checked'),
          'doSentiment': $('#toggle-sentiment').prop('checked')
        };
      },

      function(newResult, oldValue) {
        if (newResult['mapResult'] !== oldValue['mapResult']) {
          $scope.result = newResult['mapResult'];
          if (Object.keys($scope.result).length !== 0) {
            $scope.status.init = false;
            drawMap($scope.result);
          } else {
            drawMap($scope.result);
          }
        }
        if (newResult['totalCount'] !== oldValue['totalCount']) {
          $scope.totalCount = newResult['totalCount'];
        }
        if(newResult['doNormalization'] !== oldValue['doNormalization']) {
          $scope.doNormalization = newResult['doNormalization'];
          drawMap($scope.result);
        }
        if(newResult['doSentiment'] !== oldValue['doSentiment']) {
          $scope.doSentiment = newResult['doSentiment'];
          if($scope.doSentiment) {
            $scope.infoPromp = "Score";  // change the info promp
          } else {
            $scope.infoPromp = config.mapLegend;
          }
          drawMap($scope.result);
        }
      }
    );
  })
  .directive("map", function () {
    return {
      restrict: 'E',
      scope: {
        lat: "=",
        lng: "=",
        zoom: "="
      },
      controller: 'MapCtrl',
      template:[
        '<leaflet lf-center="center" tiles="tiles" events="events" controls="controls" width="100%" height="100%" ng-init="init()"></leaflet>'
      ].join('')
    };
  });
