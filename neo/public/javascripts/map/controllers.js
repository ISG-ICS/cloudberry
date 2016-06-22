angular.module('cloudberry.map', ['leaflet-directive', 'cloudberry.common'])
  .controller('MapCtrl', function($scope, $window, $http, $compile, Asterix, leafletData) {
    $scope.result = {};
    // map setting
    angular.extend($scope, {
      // TODO make this center and level as parameters to make it general
      // center: {
      //   lat: 39.5,
      //   lng: -96.35,
      //   zoom: 4
      // },
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
        countyStyle: {
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
        colors: ['#053061', '#2166ac', '#4393c3', '#92c5de', '#d1e5f0', '#f7f7f7', '#fddbc7', '#f4a582', '#d6604d', '#b2182b', '#67001f'],
      },

    });

    // initialize
    $scope.init = function() {
      leafletData.getMap().then(function(map) {
        $scope.map = map;
        $scope.bounds = map.getBounds();
        map.setView([$scope.lat, $scope.lng],$scope.zoom);
      });

      setInfoControl();
      $scope.$on("leafletDirectiveMap.zoomend", function() {
        if ($scope.map) {
          $scope.status.zoomLevel = $scope.map.getZoom();
          $scope.bounds = $scope.map.getBounds();
          if ($scope.status.zoomLevel > 5) {
            $scope.status.logicLevel = 'county';
            if (!$scope.status.init) {
              Asterix.parameters.area.swLat = $scope.bounds._southWest.lat;
              Asterix.parameters.area.swLog = $scope.bounds._southWest.lng;
              Asterix.parameters.area.neLat = $scope.bounds._northEast.lat;
              Asterix.parameters.area.neLog = $scope.bounds._northEast.lng;
              Asterix.parameters.level = 'county';
              Asterix.queryType = 'zoom';
              Asterix.query(Asterix.parameters, Asterix.queryType);
            }
            if($scope.polygons.statePolygons) {
              $scope.map.removeLayer($scope.polygons.statePolygons);
              $scope.map.addLayer($scope.polygons.countyPolygons);
            }
          } else if ($scope.status.zoomLevel <= 5) {
            $scope.status.logicLevel = 'state';
            if (!$scope.status.init) {
              Asterix.parameters.area.swLat = $scope.bounds._southWest.lat;
              Asterix.parameters.area.swLog = $scope.bounds._southWest.lng;
              Asterix.parameters.area.neLat = $scope.bounds._northEast.lat;
              Asterix.parameters.area.neLog = $scope.bounds._northEast.lng;
              Asterix.parameters.level = 'state';
              Asterix.queryType = 'zoom';
              Asterix.query(Asterix.parameters, Asterix.queryType);
            }
            if($scope.polygons.countyPolygons) {
              $scope.map.removeLayer($scope.polygons.countyPolygons);
              $scope.map.addLayer($scope.polygons.statePolygons);
            }
          }
        }
      });

      $scope.$on("leafletDirectiveMap.dragend", function() {
        if (!$scope.status.init) {
          $scope.bounds = $scope.map.getBounds();
          Asterix.parameters.area.swLat = $scope.bounds._southWest.lat;
          Asterix.parameters.area.swLog = $scope.bounds._southWest.lng;
          Asterix.parameters.area.neLat = $scope.bounds._northEast.lat;
          Asterix.parameters.area.neLog = $scope.bounds._northEast.lng;
          Asterix.parameters.level = $scope.status.logicLevel;
          Asterix.queryType = 'drag';
          Asterix.query(Asterix.parameters, Asterix.queryType);
        }
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
            weight: 2,
            fillOpacity: 0.5,
            color: 'white'
          };
        else
          style = {
            weight: 1,
            fillOpacity: 0.2,
            color: '#92c5de'
          }
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
        this._div.innerHTML = [
          '<h4>Count by {{ status.logicLevel }}</h4>',
          '<b>{{ selectedPlace.properties.name || "No place selected" }}</b>',
          '<br/>',
          'Count: {{ selectedPlace.properties.count || "0" }}'
        ].join('');
        $compile(this._div)($scope);
        return this._div;
      };

      info.options = {
        position: 'topleft'
      };
      $scope.controls.custom.push(info);

      loadGeoJsonFiles(onEachFeature);

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
          $scope.polygons.statePolygons.addTo($scope.map);
        })
        .error(function(data) {
          console.log("Load state data failure");
        });
      $http.get("assets/data/county.json")
        .success(function(data) {
          $scope.geojsonData.county = data;
          $scope.polygons.countyPolygons = L.geoJson(data, {
            style: $scope.styles.countyStyle,
            onEachFeature: onEachFeature
          });
        })
        .error(function(data) {
          console.log("Load county data failure");
        });

    }

    /**
     * Update map based on a set of spatial query result cells
     * @param    [Array]     mapPlotData, an array of coordinate and weight objects
     */
    function drawMap(result) {
      var maxWeight = 10;
      var minWeight = 0;

      // find max/min weight
      angular.forEach(result, function(value, key) {
        maxWeight = Math.max(maxWeight, value.count);
      });

      var range = maxWeight - minWeight;
      if (range < 0) {
        range = 0
        maxWeight = 0
        minWeight = 0
      }
      if (range < 10) {
        range = 10
      }

      var colors = $scope.styles.colors;

      function getColor(d) {
        return d > minWeight + range * 0.9 ? colors[10] :
          d > minWeight + range * 0.8 ? colors[9] :
          d > minWeight + range * 0.7 ? colors[8] :
          d > minWeight + range * 0.6 ? colors[7] :
          d > minWeight + range * 0.5 ? colors[6] :
          d > minWeight + range * 0.4 ? colors[5] :
          d > minWeight + range * 0.3 ? colors[4] :
          d > minWeight + range * 0.2 ? colors[3] :
          d > minWeight + range * 0.1 ? colors[2] :
          d > minWeight ? colors[1] :
          colors[0];
      }

      function style(feature) {
        return {
          fillColor: getColor(feature.properties.count),
          weight: 2,
          opacity: 1,
          color: 'white',
          dashArray: '3',
          fillOpacity: 0.5
        };
      }

      // update count
      if ($scope.status.logicLevel == "state" && $scope.geojsonData.state) {
        angular.forEach($scope.geojsonData.state.features, function(d) {
          if (d.properties.count)
            d.properties.count = 0;
          for (var k in result) {
          //TODO make a hash map from ID to make it faster
            if (result[k].key == d.properties.stateID)
              d.properties.count = result[k].count;
          }
        });

        // draw
        $scope.polygons.statePolygons.setStyle(style);
      } else if ($scope.status.logicLevel == "county" && $scope.geojsonData.county) {
        angular.forEach($scope.geojsonData.county.features, function(d) {
          if (d.properties.count)
            d.properties.count = 0;
          for (var k in result) {
          //TODO make a hash map from ID to make it faster
            if (result[k].key == d.properties.countyID)
              d.properties.count = result[k].count;
          }
        });

        // draw
        $scope.polygons.countyPolygons.setStyle(style);
      }
      // add legend
      if ($('.legend'))
        $('.legend').remove();

      $scope.legend = L.control({
        position: 'topleft'
      });

      $scope.legend.onAdd = function(map) {
        var div = L.DomUtil.create('div', 'info legend'),
          grades = [0]

        for (var i = 1; i < 10; i++) {
          var value = Math.floor((i * 1.0 / 10) * range + minWeight);
          if (value > grades[i - 1]) {
            grades.push(value);
          }
        }

        // loop through our density intervals and generate a label with a colored square for each interval
        for (var i = 0; i < grades.length; i++) {
          div.innerHTML +=
            '<i style="background:' + getColor(grades[i]) + '"></i> ' +
            grades[i] + (grades[i + 1] ? '&ndash;' + grades[i + 1] + '<br>' : '+');
        }

        return div;
      };
      if ($scope.map)
        $scope.legend.addTo($scope.map);
    }

    $scope.$watch(
      function() {
        return Asterix.mapResult;
      },

      function(newResult) {
        $scope.result = newResult;
        if (Object.keys($scope.result).length != 0) {
          $scope.status.init = false;
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
