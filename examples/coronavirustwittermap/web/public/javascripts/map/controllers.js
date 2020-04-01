angular.module('cloudberry.map', ['leaflet-directive', 'cloudberry.common','cloudberry.cache', 'cloudberry.populationcache', 'cloudberry.casedatacache'])
  .controller('MapCtrl', function($scope, $http, cloudberry, leafletData,
                                  cloudberryConfig, Cache, PopulationCache, caseDataCache, moduleManager) {

    cloudberry.parameters.maptype = config.defaultMapType;
  
    // add an alert window for gecko-based browsers like Firefox
    // refer to Leaflet documentation: https://leafletjs.com/reference-1.0.2.html#browser
    // L.Browser.gecko: true for gecko-based browsers like Firefox.
    // L.Browser.gecko3d: true for gecko-based browsers supporting CSS transforms.
    if (L.Browser.gecko || L.Browser.gecko3d) {
      var alertDiv = document.getElementsByTagName("alert-bar")[0];
      var div = L.DomUtil.create('div', 'alert alert-warning alert-dismissible')
      div.innerHTML = [
        '<a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a>',
        '<p>TwitterMap currently doesn\'t support time series chart on Firefox.</p>',
        '<p>To enable live tweets, make sure to go to <a href="about:config?filter=privacy.trackingprotection.enabled">about:config</a> and change the value of <code>privacy.trackingprotection.enabled</code> to false.</p>'
      ].join('');
      div.style.position = 'absolute';
      div.style.top = '0%';
      div.style.width = '100%';
      div.style.zIndex = '9999';
      div.style.fontSize = '23px';
      alertDiv.appendChild(div);
    }
  
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
    $scope.cityIdSet = new Set();

    // setting default map styles, zoom level, etc.
    angular.extend($scope, {
      tiles: {
        name: 'Mapbox',
        url: 'https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}',
        type: 'xyz',
        options: {
          accessToken: 'pk.eyJ1IjoiamVyZW15bGkiLCJhIjoiY2lrZ2U4MWI4MDA4bHVjajc1am1weTM2aSJ9.JHiBmawEKGsn3jiRK_d0Gw',
          id: 'jeremyli.p6f712pj',
          minZoom: 2,
          maxZoom: 16
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
    
    // set map styles
    $scope.setStyles = function setStyles(styles) {
      $scope.styles = styles;
    };

    // find the geoIds of the polygons that are within a given bounding box
    $scope.resetGeoIds = function resetGeoIds(bounds, polygons, idTag) {
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
    };

    // reset the geo level (state, county, city)
    $scope.resetGeoInfo = function resetGeoInfo(level) {
      $scope.status.logicLevel = level;
      cloudberry.parameters.geoLevel = level;
      if ($scope.geojsonData[level])
        $scope.resetGeoIds($scope.bounds, $scope.geojsonData[level], level + 'ID');
    };


    // initialize the leaflet map
    $scope.init = function() {
      leafletData.getMap().then(function(map) {
        $scope.map = map;
        $scope.bounds = map.getBounds();
        cloudberry.parameters.bounds = $scope.bounds;
        //making attribution control to false to remove the default leaflet sign in the bottom of map
        map.attributionControl.setPrefix(false);
        map.setView([$scope.lat, $scope.lng],$scope.zoom);
        if ($(window).width() <= 500) {
          map.setView([$scope.lat, $scope.lng], 3);
        }
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
        if ($(window).width() > 500) {
          $scope.map.setView([$scope.lat, $scope.lng], 4);
        } else {
          $scope.map.setView([$scope.lat, $scope.lng], 3);
        }
      });

      $scope.resetGeoInfo("state");
    };
    
    // redraw the polygons with the new map styles
    $scope.resetPolygonLayers = function resetPolygonLayers() {
      if ($scope.polygons.statePolygons) {
        $scope.polygons.statePolygons.setStyle($scope.styles.stateStyle);
      }
      if ($scope.polygons.countyPolygons) {
        $scope.polygons.countyPolygons.setStyle($scope.styles.countyStyle);
      }
      if ($scope.polygons.cityPolygons) {
        $scope.polygons.cityPolygons.setStyle($scope.styles.cityStyle);
      }
      if ($scope.polygons.stateUpperPolygons) {
        $scope.polygons.stateUpperPolygons.setStyle($scope.styles.stateUpperStyle);
      }
      if ($scope.polygons.countyUpperPolygons) {
        $scope.polygons.countyUpperPolygons.setStyle($scope.styles.countyUpperStyle);
      }
    };

    $scope.deletePolygonLayers = function deletePolygonLayers() {
      if ($scope.polygons.statePolygons && $scope.map.hasLayer($scope.polygons.statePolygons)) {
        $scope.map.removeLayer($scope.polygons.statePolygons);
      }
      if ($scope.polygons.countyPolygons && $scope.map.hasLayer($scope.polygons.countyPolygons)) {
        $scope.map.removeLayer($scope.polygons.countyPolygons);
      }
      if ($scope.polygons.cityPolygons && $scope.map.hasLayer($scope.polygons.cityPolygons)) {
        $scope.map.removeLayer($scope.polygons.cityPolygons);
      }
      if ($scope.polygons.stateUpperPolygons && $scope.map.hasLayer($scope.polygons.stateUpperPolygons)) {
        $scope.map.removeLayer($scope.polygons.stateUpperPolygons);
      }
      if ($scope.polygons.countyUpperPolygons && $scope.map.hasLayer($scope.polygons.countyUpperPolygons)) {
        $scope.map.removeLayer($scope.polygons.countyUpperPolygons);
      }
    };

    // update the center and the boundary of the visible area of the map
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
        features[id].properties["popUpLog"] = (maxLog + minLog) / 2;
        features[id].properties["popUpLat"] = maxLat;

        // Set the position of popup window for special case, eg. "Alaska" State
        if( maxLog > 0 && minLog < 0) {
          minLog = Number.POSITIVE_INFINITY;
          maxLog = Number.NEGATIVE_INFINITY;
          if(features[id].geometry.type === "Polygon") {
            features[id].geometry.coordinates[0].forEach(function(pair) {
              if (pair[0] < 0) {
                minLog = Math.min(minLog, pair[0]);
                maxLog = Math.max(maxLog, pair[0]);
              }
            });
          } else if( features[id].geometry.type === "MultiPolygon") {
            features[id].geometry.coordinates.forEach(function(array){
              array[0].forEach(function(pair){
                if (pair[0] < 0) {
                  minLog = Math.min(minLog, pair[0]);
                  maxLog = Math.max(maxLog, pair[0]);
                }
              });
            });
          }
          features[id].properties["popUpLog"] = (maxLog + minLog) / 2;
          features[id].properties["popUpLat"] = (maxLat + minLat) / 2;
        }
      }
    }
    
    // load geoJson to get state and county polygons
    $scope.loadGeoJsonFiles = function loadGeoJsonFiles(onEachFeature) {
      if (typeof($scope.polygons.statePolygons) === "undefined" || $scope.polygons.statePolygons == null){
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
          $scope.polygons.statePolygons.addTo($scope.map);
        })
        .error(function(data) {
          console.error("Load state data failure");
        });
      }
      else {
        $scope.polygons.statePolygons.onEachFeature = onEachFeature;
        $scope.polygons.statePolygons.eachLayer(function(layer) {onEachFeature(null, layer)});
      }
      if (typeof($scope.polygons.countyPolygons) === "undefined" || $scope.polygons.countyPolygons == null){
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
        })
        .error(function(data) {
          console.error("Load county data failure");
        });
      }
      else {
        $scope.polygons.countyPolygons.onEachFeature = onEachFeature;
        $scope.polygons.countyPolygons.eachLayer(function(layer) {onEachFeature(null, layer)});
      }
    };

    // load geoJson to get city polygons
    $scope.loadCityJsonByBound = function loadCityJsonByBound(onEachFeature, fromEventName, fromEvent){

      var bounds = $scope.map.getBounds();
      var rteBounds = "city/" + bounds._northEast.lat + "/" + bounds._southWest.lat + "/" + bounds._northEast.lng + "/" + bounds._southWest.lng;

        // Caching feature only works when the given threshold is greater than zero.
        if (cloudberryConfig.cacheThreshold > 0) {
          Cache.getCityPolygonsFromCache(rteBounds).done(function(data) {

            //set center and boundary done by Cache
            if (!$scope.status.init) {
              $scope.resetGeoIds($scope.bounds, data, 'cityID');
              cloudberry.parameters.geoLevel = 'city';
              // Publish zoom/drag event to moduleManager
              moduleManager.publishEvent(fromEventName, fromEvent);
            }

            $scope.status.logicLevel = 'city';

            // initializes the $scope.geojsonData.city and $scope.cityIdSet when first time zoom in
            if(typeof $scope.polygons.cityPolygons === 'undefined'){
              $scope.geojsonData.city = data;
              $scope.polygons.cityPolygons = L.geoJson(data, {
                style: $scope.styles.cityStyle,
                onEachFeature: onEachFeature
              });
              for (i = 0; i < $scope.geojsonData.city.features.length; i++) {
                $scope.cityIdSet.add($scope.geojsonData.city.features[i].properties.cityID);
              }
            } else {
              // compares the current region's cityIds with previously stored cityIds
              // stores the new delta cities' ID and polygon info
              // add the new polygons as GeoJson objects incrementally on the layer

              for (i = 0; i < data.features.length; i++) {
                if (!$scope.cityIdSet.has(data.features[i].properties.cityID)) {
                  $scope.geojsonData.city.features.push(data.features[i]);
                  $scope.cityIdSet.add(data.features[i].properties.cityID);
                  $scope.polygons.cityPolygons.addData(data.features[i]);
                }
              }
            }

            // To add the city level map only when it doesn't exit
            if(!$scope.map.hasLayer($scope.polygons.cityPolygons)){
              $scope.map.addLayer($scope.polygons.cityPolygons);
            }
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
              $scope.resetGeoInfo("city");
              if (!$scope.status.init) {
                // Publish zoom/drag event to moduleManager
                moduleManager.publishEvent(fromEventName, fromEvent);
              }
              $scope.map.addLayer($scope.polygons.cityPolygons);
            })
            .error(function (data) {
              console.error("Load city data failure");
            });
        }
    };

    // load population Json to get state and county populations, store in population cache.
    $scope.loadPopJsonFiles = function loadPopJsonFiles() {
      $scope.popjsonData = {};
      if (PopulationCache.statePopulationCached() === false){
        $http.get("assets/data/allStatePopulation.json")
        .success(function(data) {
          PopulationCache.putPopValues(data, "state");
        })
        .error(function(data) {
          console.error("Load state population data failure");
        });
      }
      if (PopulationCache.countyPopulationCached() === false){
        $http.get("assets/data/allCountyPopulation.json")
        .success(function(data) {
          PopulationCache.putPopValues(data, "county");
        })
        .error(function(data) {
          console.error("Load county population data failure");
        });
      }
    };

    // load case data csv to get state and county case data, store in case data cache.
    $scope.loadCaseCsvFiles = function () {
      if (caseDataCache.stateCaseDataCached() === false) {
        $http.get("/stateCases")
          .success(function(csv) {
            caseDataCache.loadCsvToCaseDataStore(csv, "state");
          })
          .error(function(csv) {
            console.error("Loading stateCases.csv data failed.");
          });
      }

      if (caseDataCache.countyCaseDataCached() === false) {
        $http.get("/countyCases")
          .success(function(csv) {
            console.log(csv)
            caseDataCache.loadCsvToCaseDataStore(csv, "county");
          })
          .error(function(csv) {
            console.error("Loading countyCases.csv data failed.");
          });
      }
    };

    // zoom in to fit the selected polygon
    $scope.zoomToFeature = function zoomToFeature(leafletEvent) {
      if (leafletEvent){
        $scope.map.closePopup();
        $scope.map.fitBounds(leafletEvent.target.getBounds(), {animate: true});
      }

    };
    
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
    $scope.rangeRandom = function rangeRandom(seed, minV, maxV){
      randomizationSeed = seed;
      var ret = randomNorm((minV + maxV) / 2, (maxV - minV) / 16);
      return ret;
    };

    $scope.onEachFeature = null;

    // Listens to Leaflet's zoomend event and publish it to moduleManager
    $scope.$on("leafletDirectiveMap.zoomend", function() {

      // Original operations on zoomend event
      if ($scope.map) {
        $scope.status.zoomLevel = $scope.map.getZoom();
        $scope.bounds = $scope.map.getBounds();
        cloudberry.parameters.bounds = $scope.bounds;
        if ($scope.status.zoomLevel > 9) {
          $scope.resetGeoInfo("city");
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
          $scope.loadCityJsonByBound($scope.onEachFeature, moduleManager.EVENT.CHANGE_ZOOM_LEVEL,
            {level: $scope.map.getZoom(), bounds: $scope.map.getBounds()});
        } else if ($scope.status.zoomLevel > 5) {
          $scope.resetGeoInfo("county");
          if (!$scope.status.init) {
            // Publish zoom event to moduleManager
            moduleManager.publishEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, {level: $scope.map.getZoom(), bounds: $scope.map.getBounds()});
          }
          if ($scope.polygons.statePolygons) {
            $scope.map.removeLayer($scope.polygons.statePolygons);
          }
          if ($scope.polygons.cityPolygons) {
            $scope.map.removeLayer($scope.polygons.cityPolygons);
          }
          if ($scope.polygons.countyUpperPolygons) {
            $scope.map.removeLayer($scope.polygons.countyUpperPolygons);
          }
          $scope.map.addLayer($scope.polygons.stateUpperPolygons);
          $scope.map.addLayer($scope.polygons.countyPolygons);
        } else if ($scope.status.zoomLevel <= 5) {
          $scope.resetGeoInfo("state");
          if (!$scope.status.init) {
            // Publish zoom event to moduleManager
            moduleManager.publishEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, {level: $scope.map.getZoom(), bounds: $scope.map.getBounds()});
          }
          if ($scope.polygons.countyPolygons) {
            $scope.map.removeLayer($scope.polygons.countyPolygons);
          }
          if ($scope.polygons.cityPolygons) {
            $scope.map.removeLayer($scope.polygons.cityPolygons);
          }
          if ($scope.polygons.stateUpperPolygons) {
            $scope.map.removeLayer($scope.polygons.stateUpperPolygons);
          }
          if ($scope.polygons.countyUpperPolygons) {
            $scope.map.removeLayer($scope.polygons.countyUpperPolygons);
          }
          if ($scope.polygons.statePolygons) {
            $scope.map.addLayer($scope.polygons.statePolygons);
          }
        }
      }
    });

    // Listens to Leaflet's dragend event and publish it to moduleManager
    $scope.$on("leafletDirectiveMap.dragend", function() {

      // Original operations on dragend event
      if (!$scope.status.init) {
        $scope.bounds = $scope.map.getBounds();
        cloudberry.parameters.bounds = $scope.bounds;
        var geoData;
        if ($scope.status.logicLevel === "state") {
          geoData = $scope.geojsonData.state;
        } else if ($scope.status.logicLevel === "county") {
          geoData = $scope.geojsonData.county;
        } else if ($scope.status.logicLevel === "city") {
          geoData = $scope.geojsonData.city;
        } else {
          console.error("Error: Illegal value of logicLevel, set to default: state");
          $scope.status.logicLevel = "state";
          geoData = $scope.geojsonData.state;
        }
      }
      if ($scope.status.logicLevel === "city") {
        $scope.loadCityJsonByBound($scope.onEachFeature, moduleManager.EVENT.CHANGE_REGION_BY_DRAG,
          {bounds: $scope.map.getBounds()});
      } else {
        $scope.resetGeoIds($scope.bounds, geoData, $scope.status.logicLevel + "ID");
        cloudberry.parameters.geoLevel = $scope.status.logicLevel;
        // Publish drag event to moduleManager
        moduleManager.publishEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, {bounds: $scope.map.getBounds()});
      }
    });

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
        '<leaflet lf-center="center" tiles="tiles" events="events" controls="controls" width="100%" height="100%" ng-init="init()"></leaflet><div ng-controller="countMapCtrl"></div><div ng-controller="pinMapCtrl"></div><div ng-controller="heatMapCtrl"></div>'
      ].join('')
    };
  });
