var app = angular.module('cloudberry', []);

app.factory('Asterix', function($http, $timeout) {
  var ws = new WebSocket("ws://localhost:9000/ws");
  var asterixService = {

    level: "state",

    mapBoundary: {},
    timeRange: {},
    keywords: [],

    mapResult: {},
    timeResult: {},
    hashTagResult: {},

    query: function() {
      //TODO compose the query
      var query = null;
      ws.send(JSON.stringify({
        query: "state"
      }));
    }
  };

  ws.onmessage = function(event) {
    $timeout(function() {
      var result = JSON.parse(event.data);
      switch (result.type) {
        case "map":
          asterixService.mapResult = result.result
          break;
        case "time":
          asterixService.timeResult = result.result
          break;
        case "hashTag":
          asterixService.hashTagResult = result.result
          break;
        default:
          console.log("ws get unknown data: " + result)
          break
      }
    });
  };

  return asterixService;
});

app.controller('SearchCtrl', function($scope, $window, Asterix) {
  $scope.search = function() {
    Asterix.keywords = $scope.query.trim().split(/\s+/)
    Asterix.query();
  };
});

app.controller('TweetsCtrl', function($scope, $window, Asterix) {
  $scope.result = {};
});

app.controller('MapCtrl', function($scope, $window, $q, $http, Asterix) {
  // style
  var stateStyle = {
    fillColor: '#f7f7f7',
    weight: 2,
    opacity: 1,
    color: '#92c5de',
    dashArray: '3',
    fillOpacity: 0.2
  };

  var countyStyle = {
    fillColor: '#f7f7f7',
    weight: 1,
    opacity: 1,
    color: '#92c5de',
    fillOpacity: 0.2
  };

  var hoverStyle = {
    weight: 5,
    color: '#666',
    dashArray: '',
    fillOpacity: 0.7
  };

  function setLayer(data, style, onEachFeature) {
    return $window.L.geoJson(data, {
      style: style,
      onEachFeature: onEachFeature
    });
  }

  $q.all([
    $http.get('/assets/data/states_hash.json'),
    $http.get('/assets/data/state.shape.20m.json'),
    $http.get('/assets/data/county.shape.20m.json')
  ]).then(function(data) {
    var state_hash = data[0].data;
    var state = data[1].data;
    var county = data[2].data;

    var map = $window.L.map('map').setView([39.5, -96.35, ], 4);
    $window.L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
      attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
      maxZoom: 18,
      id: 'jeremyli.p6f712pj',
      accessToken: 'pk.eyJ1IjoiamVyZW15bGkiLCJhIjoiY2lrZ2U4MWI4MDA4bHVjajc1am1weTM2aSJ9.JHiBmawEKGsn3jiRK_d0Gw'
    }).addTo(map);

    $window.L.Util.requestAnimFrame(map.invalidateSize, map, !1, map._container);
    
    var statePolygons = null;
    var countyPolygons = null;

    var info = $window.L.control();

    function highlightFeature(e) {
      var layer = e.target;
      layer.setStyle(hoverStyle);
      if (!$window.L.Browser.ie && !$window.L.Browser.opera) {
        layer.bringToFront();
      }
      info.update(layer.feature.properties);
    }

    function resetHighlight(e) {
      var style = {
        weight: 1,
        fillOpacity: 0.2,
        color: '#92c5de'
      }
      if (Asterix.level == "state")
        statePolygons.setStyle(style);
      else
        countyPolygons.setStyle(style);
      info.update();
    }

    function zoomToFeature(e) {
      map.fitBounds(e.target.getBounds());
    }

    function onEachFeature(feature, layer) {
      layer.on({
        mouseover: highlightFeature,
        mouseout: resetHighlight,
        click: zoomToFeature
      });
    }

    statePolygons = setLayer(state, stateStyle, onEachFeature);
    countyPolygons = setLayer(county, countyStyle, onEachFeature);
    statePolygons.addTo(map);
    countyPolygons.addTo(map);

    info.onAdd = function(map) {
      this._div = $window.L.DomUtil.create('div', 'info'); // create a div with a class "info"
      this.update();
      return this._div;
    };

    // method that we will use to update the control based on feature properties passed
    info.update = function(props) {
      this._div.innerHTML = '<h4>Count by ' + Asterix.level + '</h4>' + (props ?
        '<b>' + props.NAME + '</b><br />' + 'Count: ' + (props.count ? props.count : 0) : 'Hover over a state');
    };

    info.addTo(map);

    // add  zoom event  listener
    map.on('zoomend', function() {
      var zoomLevel = map.getZoom();
      if (zoomLevel > 5) {
        Asterix.level = 'county';
        Asterix.mapBoundary = map.getBounds();
        Asterix.query
        map.removeLayer(statePolygons);
        map.addLayer(countyPolygons);
      } else if (zoomLevel <= 5) {
        Asterix.level = 'state';
        Asterix.mapBoundary = map.getBounds();
        Asterix.query
        map.removeLayer(countyPolygons);
        map.addLayer(statePolygons);
      }
    });

    // add drag event listener
    map.on("dragend", function() {
      Asterix.mapBoundary = map.getBounds();
      Asterix.query
    });


    //// watch result

    $scope.$watch(
      function() {
        return Asterix.mapResult;
      },

      function(mapPlotData) {
        var maxWeight = 0;
        var minWeight = Number.MAX_VALUE;

        // find max/min weight
        angular.forEach(mapPlotData, function(data, i) {
          if (data.count) {
            maxWeight = Math.max(data.count, maxWeight);
            minWeight = Math.min(data.count, minWeight);
          }
        });

        var range = maxWeight - minWeight;
        if (range < 0) {
          range = 0
          maxWeight = 0
          minWeight = 0
        }

        colors = ['#053061', '#2166ac', '#4393c3', '#92c5de', '#d1e5f0', '#f7f7f7', '#fddbc7', '#f4a582', '#d6604d', '#b2182b', '#67001f'];
        // style function
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

        // draw geojson polygons
        if (Asterix.level == "state") {
          // transfer to geohash
          angular.forEach(mapPlotData, function(m, data) {
            for (var hash in state_hash) {
              if (state_hash.hasOwnProperty(hash)) {
                if (hash == mapPlotData[m].cell) {
                  var val = state_hash[hash];
                  mapPlotData[m].cell = val;
                }
              }
            }
          });

          // update states count
          angular.forEach(state.features, function(d, i) {
            if (d.properties.count)
              d.properties.count = 0;
            for (var m in mapPlotData) {
              if (mapPlotData[m].cell == d.properties.NAME)
                d.properties.count = mapPlotData[m].count;
            }
          });

          // draw state polygons
          statePolygons.setStyle(style);
        } else if (Asterix.level == "county") {
          // update county's count
          angular.forEach(county.features, function(d, i) {
            if (d.properties.count)
              d.properties.count = 0;
            for (var m in mapPlotData) {
              if (mapPlotData[m].cell && mapPlotData[m].cell.slice(3, mapPlotData[m].cell.length) == d.properties.NAME)
                d.properties.count = mapPlotData[m].count;
            }
          });

          // draw county polygons
          countyPolygons.setStyle(style);
        }
        var legend = $window.L.control({
          position: 'bottomright'
        });

        legend.onAdd = function(map) {
          var div = $window.L.DomUtil.create('div', 'info legend'),
            grades = [0],
            labels = [];

          for (var i = 0; i < 10; i++) {
            var value = Math.floor((i * 1.0 / 10) * range + minWeight);
            if (value > grades[i]) {
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

        legend.addTo(map);
      }
    );
  });
});

app.controller('D3Ctrl', function($scope, $http, $timeout, Asterix) {

});

app.controller('TimeCtrl', function($scope, $http, $timeout, Asterix) {

  var initialStartDate = "2012-01-01T00:00:00.000Z";

});
