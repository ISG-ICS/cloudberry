angular.module('cloudberry.map', ['leaflet-directive', 'cloudberry.common'])
  .controller('MapCtrl', function($scope, $window, $http, $compile, cloudberry, leafletData, cloudberryConfig) {
    $scope.result = {};
    $scope.doNormalization = false;
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
          weight: 2,
          fillOpacity: 0.5,
          color: 'white'
        },
        stateStyle: {
          fillColor: '#f7f7f7',
          weight: 2,
          opacity: 1,
          color: '#92c5de',
          dashArray: '3',
          fillOpacity: 0.2
        },
        stateUpperStyle: {
          fillColor: '#f7f7f7',
          weight: 3,
          opacity: 1,
          color: '#ffc125',
          fillOpacity: 0.2
        },
        countyStyle: {
          fillColor: '#f7f7f7',
          weight: 1,
          opacity: 1,
          color: '#92c5de',
          fillOpacity: 0.2
        },
        countyUpperStyle: {
          fillColor: '#f7f7f7',
          weight: 2,
          opacity: 1,
          color: '#ffc125',
          fillOpacity: 0.2
        },
        cityStyle: {
          fillColor: '#f7f7f7',
          weight: 1,
          opacity: 1,
          color: '#92c5de',
          fillOpacity: 0.2
        },
        hoverStyle: {
          weight: 5,
          color: '#666',
          dashArray: '',
          fillOpacity: 0.7
        },
        colors: [ '#f7f7f7', '#92c5de', '#4393c3', '#2166ac', '#f4a582', '#d6604d', '#b2182b']
      }
    });

    function resetGeoIds(bounds, polygons, idTag) {
      cloudberry.parameters.geoIds = [];
      polygons.features.forEach(function(polygon){
        if (bounds._southWest.lat <= polygon.properties.centerLat &&
              polygon.properties.centerLat <= bounds._northEast.lat &&
              bounds._southWest.lng <= polygon.properties.centerLog &&
              polygon.properties.centerLog <= bounds._northEast.lng) {
            cloudberry.parameters.geoIds.push(polygon.properties[idTag]);
        }
      });
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
            weight: 2,
            fillOpacity: 0.5,
            color: 'white'
          };
        else
          style = {
            weight: 1,
            fillOpacity: 0.2,
            color: '#92c5de'
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

      // add info control
      var info = L.control();

      info.onAdd = function() {
        this._div = L.DomUtil.create('div', 'info'); // create a div with a class "info"
        this._div.style.margin = '20% 0 0 0';
        this._div.innerHTML = [
          '<h4>Count by {{ status.logicLevel }}</h4>',
          '<b>{{ selectedPlace.properties.name || "No place selected" }}</b>',
          '<br/>',
          'Count: {{ selectedPlace.properties.countText || "0" }}'
        ].join('');
        $compile(this._div)($scope);
        return this._div;
      };

      info.options = {
        position: 'topleft'
      };
      $scope.controls.custom.push(info);

      loadGeoJsonFiles(onEachFeature);

      $scope.$on("leafletDirectiveMap.zoomend", function() {
        if ($scope.map) {
          $scope.status.zoomLevel = $scope.map.getZoom();
          $scope.bounds = $scope.map.getBounds();
          if($scope.status.zoomLevel > 7) {
            $scope.status.logicLevel = 'city';
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
            $scope.status.logicLevel = 'county';
            if (!$scope.status.init) {
              resetGeoIds($scope.bounds, $scope.geojsonData.county, 'countyID');
              cloudberry.parameters.geoLevel = 'county';
              cloudberry.queryType = 'zoom';
              cloudberry.query(cloudberry.parameters, cloudberry.queryType);
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
            $scope.status.logicLevel = 'state';
            if (!$scope.status.init) {
              resetGeoIds($scope.bounds, $scope.geojsonData.state, 'stateID');
              cloudberry.parameters.geoLevel = 'state';
              cloudberry.queryType = 'zoom';
              cloudberry.query(cloudberry.parameters, cloudberry.queryType);
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
            $scope.map.addLayer($scope.polygons.statePolygons);
          }
        }
      });

      $scope.$on("leafletDirectiveMap.dragend", function() {
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
            console.error("Error: Illegal value of logicLevel, set to default: state")
            $scope.status.logicLevel = 'state'
            geoData = $scope.geojsonData.state;
          }
        }
        if ($scope.status.logicLevel === 'city') {
          loadCityJsonByBound(onEachFeature);
        }
        resetGeoIds($scope.bounds, geoData, $scope.status.logicLevel + "ID");
        cloudberry.parameters.geoLevel = $scope.status.logicLevel;
        cloudberry.queryType = 'drag';
        cloudberry.query(cloudberry.parameters, cloudberry.queryType);
      });

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
          $scope.polygons.statePolygons.addTo($scope.map);
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
        })
        .error(function(data) {
          console.error("Load county data failure");
        });
    }

    function loadCityJsonByBound(onEachFeature){
      var bounds = $scope.map.getBounds();
      var rteBounds = "city/" + bounds._northEast.lat + "/" + bounds._southWest.lat + "/" + bounds._northEast.lng + "/" + bounds._southWest.lng;
      $http.get(rteBounds)
        .success(function(data) {
          $scope.geojsonData.city = data;
          if($scope.polygons.cityPolygons) {
            $scope.map.removeLayer($scope.polygons.cityPolygons);
          }
          $scope.polygons.cityPolygons = L.geoJson(data, {
            style: $scope.styles.cityStyle,
            onEachFeature: onEachFeature
          });
          setCenterAndBoundry($scope.geojsonData.city.features);
          if (!$scope.status.init) {
            resetGeoIds($scope.bounds, $scope.geojsonData.city, 'cityID');
            cloudberry.parameters.geoLevel = 'city';
            cloudberry.queryType = 'zoom';
            cloudberry.query(cloudberry.parameters, cloudberry.queryType);
          }
          $scope.map.addLayer($scope.polygons.cityPolygons);
        })
        .error(function(data) {
          console.error("Load city data failure");
        });
    }


    /**
     * Update map based on a set of spatial query result cells
     * @param    [Array]     mapPlotData, an array of coordinate and weight objects
     */
    function drawMap(result) {

      var colors = $scope.styles.colors;

      function getColor(d) {
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

      function style(feature) {
        if (!feature.properties.count || feature.properties.count == 0){
          return {
            fillColor: '#f7f7f7',
            weight: 2,
            opacity: 1,
            color: '#92c5de',
            dashArray: '3',
            fillOpacity: 0.2
          };
        } else {
          return {
            fillColor: getColor(feature.properties.count),
            weight: 2,
            opacity: 1,
            color: 'white',
            dashArray: '3',
            fillOpacity: 0.5
          };
        }
      }

      function setNormalizedCountText(geo){
        // beautify 0.0000123 => 1.23e-5, 1.123 => 1.1
        if(geo["properties"]["count"] < 1){
          geo["properties"]["countText"] = geo["properties"]["count"].toExponential(1);
        }
        else{
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
        geo['properties']['count'] = r['count'] / r['population'] * cloudberryConfig.normalizationUpscaleFactor;
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
                if ($scope.doNormalization)
                  setNormalizedCount(geo, r);
                else
                  setUnnormalizedCount(geo, r);
              }
            });
          });

          // draw
          $scope.polygons[level+"Polygons"].setStyle(style);
        }
      }

      // Loop through each result and update its count information on its associated geo record
      updateTweetCountInGeojson();

      // add legend
      var legend = $('.legend');
      if (legend)
        legend.remove();

      $scope.legend = L.control({
        position: 'topleft'
      });

      $scope.legend.onAdd = function(map) {
        var div = L.DomUtil.create('div', 'info legend');
        var grades = new Array(colors.length -1); //[1, 10, 100, 1000, 10000, 100000];

        for(var i = 0; i < grades.length; i++){
          grades[i] = Math.pow(10, i);
        }

        var gName  = grades.map( function(d) {
          var returnText = "";
          if (d < 1000){
            returnText = d.toString();
          } else if (d < 1000 * 1000) {
            returnText = (d / 1000).toString() + "K";
          } else if (d < 1000 * 1000 * 1000) {
            returnText = (d / 1000 / 1000).toString() + "M";
          } else{
            returnText = (d / 1000 / 1000).toString() + "M+";
          }

          if($scope.doNormalization){
            return returnText + "/" + cloudberryConfig.normalizationUpscaleText; //["1/M", "10/M", "100/M", "1K/M", "10K/M", "100K/M"];
          }
          else{
            return returnText; //["1", "10", "100", "1K", "10K", "100K"];
          }
        });

        // loop through our density intervals and generate a label with a colored square for each interval
        var i = 1;
        for (; i < grades.length; i++) {
          div.innerHTML +=
            '<i style="background:' + getColor(grades[i]) + '"></i>' + gName[i-1] + '&ndash;' + gName[i] + '<br>';
        }
        div.innerHTML += '<i style="background:' + getColor(grades[i-1]*10) + '"></i> ' + gName[i-1] + '+';

        return div;
      };
      if ($scope.map)
        $scope.legend.addTo($scope.map);

      // add toggle normalize
      var normalize = $('.normalize');
      if (normalize) {
        normalize.remove();
      }

      $scope.normalize= L.control({
        position: 'topleft'
      });

      $scope.normalize.onAdd = function() {
        var div = L.DomUtil.create('div', 'info normalize');
        if($scope.doNormalization)
          div.innerHTML = '<p>Normalize</p><input id="toggle-normalize" checked type="checkbox">';
        else
          div.innerHTML = '<p>Normalize</p><input id="toggle-normalize" type="checkbox">';
        return div;
      };
      if ($scope.map) {
        $scope.normalize.addTo($scope.map);
        $('#toggle-normalize').bootstrapToggle();
      }

    }

    $scope.$watchCollection(
      function() {
        return {
          'mapResult': cloudberry.mapResult,
          'totalCount': cloudberry.totalCount,
          'doNormalization': $('#toggle-normalize').prop('checked')
        };
      },

      function(newResult, oldValue) {
        if (newResult['mapResult'] != oldValue['mapResult']) {
            $scope.result = newResult['mapResult'];
            if (Object.keys($scope.result).length != 0) {
                $scope.status.init = false;
                drawMap($scope.result);
            }
            else {
                drawMap($scope.result);
            }
        }
        if (newResult['totalCount'] != oldValue['totalCount']) {
            $scope.totalCount = newResult['totalCount'];
        }
        if(newResult['doNormalization'] != oldValue['doNormalization']) {
          $scope.doNormalization = newResult['doNormalization'];
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
