var app = angular.module('cloudberry', ['leaflet-directive']);

app.factory('Asterix', function($http, $timeout) {
  var ws = new WebSocket("ws://localhost:9000/ws");
  var asterixService = {

    parameters: {
      dataset: "twitter",
      keyword: null,
      area: {
        swLog: -146,
        swLat: 8,
        neLog: -100,
        neLat: 50
      },
      time: {
        start: new Date(2012, 1, 1, 0, 0, 0, 0),
        end: new Date()
      },
      level: "state",
      repeatDuration: 0
    },

    mapResult: {},
    timeResult: {},
    hashTagResult: {},
    errorMessage: null,

    query: function(parameters) {
      var json = (JSON.stringify({
        dataset: parameters.dataset,
        keyword: parameters.keyword,
        area: parameters.area,
        timeRange: {
          start: Date.parse(parameters.time.start),
          end: Date.parse(parameters.time.end)
        },
        level: parameters.level,
        repeatDuration : parameters.repeatDuration
      }));
      ws.send(json);
    }
  };

  ws.onmessage = function(event) {
    $timeout(function() {
      console.log(event.data);
      var result = JSON.parse(event.data);
      switch (result.aggType) {
        case "map":
          asterixService.mapResult = result.result;
          break;
        case "time":
          asterixService.timeResult = result.result;
          break;
        case "hashtag":
          asterixService.hashTagResult = result.result;
          break;
        case "error":
          asterixService.errorMessage = result.errorMessage;
          break;
        default:
          console.log("ws get unknown data: " + result);
          break;
      }
    });
  };

  return asterixService;
});

app.controller('SearchCtrl', function($scope, $window, Asterix) {
  $scope.init = function(){
    $scope.time = Asterix.parameters.time;
    $scope.area = Asterix.parameters.area;
    $scope.level = Asterix.parameters.level;
  };
  $scope.search = function() {
    if ($scope.dataset)
      Asterix.parameters.dataset = $scope.dataset;
    if ($scope.keyword)
      Asterix.parameters.keyword = $scope.keyword;
    if ($scope.area)
      Asterix.parameters.area = $scope.area;
    if ($scope.time)
      Asterix.parameters.time = $scope.time;
    if ($scope.level)
      Asterix.parameters.level = $scope.level;
    Asterix.query(Asterix.parameters);
  };
});

app.controller('MapCtrl', function($scope, $window, $http, Asterix) {
  $scope.result = {};
  // map setting
  angular.extend($scope, {
    center: {
      lat:  39.5,
      lng:  -96.35,
      zoom: 4
    },
    tiles: {
      name: 'Mapbox',
      url: 'https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}',
      type: 'xyz',
      options: {
        accessToken: 'pk.eyJ1IjoiamVyZW15bGkiLCJhIjoiY2lrZ2U4MWI4MDA4bHVjajc1am1weTM2aSJ9.JHiBmawEKGsn3jiRK_d0Gw',
        id: 'jeremyli.p6f712pj'
      }
    },
    geojson: {},
    legend: {
      colors: ['#053061', '#2166ac', '#4393c3', '#92c5de', '#d1e5f0', '#f7f7f7', '#fddbc7', '#f4a582', '#d6604d', '#b2182b', '#67001f'],
      labels: []
    },
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
      }
    }
  });

  // initialize
  $scope.init = function () {
    loadGeoJsonFiles();
  };

  // load geoJson
  function loadGeoJsonFiles() {
    $http.get("assets/data/state.json")
      .success(function (data) {
        $scope.geojson.state = {
          data: data,
          style: $scope.styles.stateStyle
        };
      })
      .error(function (data) {
        console.log("Load state data failure");
      });
    // $http.get("../../../public/data/county.json")
    //   .success(function (data) {
    //     $scope.geojson.county = data;
    //   })
    //   .error(function (data) {
    //     console.log("Load county data failure");
    //   });
    // $http.get("../../../public/data/city.json")
    //   .success(function (data) {
    //     $scope.geojson.city = data;
    //   })
    //   .error(function (data) {
    //     console.log("Load city data failure");
    //   });

  }

  /**
   * Update map based on a set of spatial query result cells
   * @param    [Array]     mapPlotData, an array of coordinate and weight objects
   */
  function drawMap(result) {
    var maxWeight = 10;
    var minWeight = 0;

    // find max/min weight
    angular.forEach(result, function (value, key) {
      maxWeight = Math.max(maxWeight, value);
    });

    var range = maxWeight - minWeight;
    if (range < 0) {
      range = 0
      maxWeight = 0
      minWeight = 0
    }
    if (range < 10){
      range = 10
    }

    var colors = $scope.legend.colors;

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

    // update states count
    if($scope.geojson.hasOwnProperty('state')) {
      angular.forEach($scope.geojson.state.data.features, function (d) {
        if (d.properties.count)
          d.properties.count = 0;
        for (var k in result) {
          if (k == d.properties.name)
            d.properties.count = result[k];
        }
      });

      // draw
      $scope.geojson.state.style = style;
    }
  }

  $scope.$watch(
    function() {
      return Asterix.mapResult;
    },

    function(newResult) {
      $scope.result = newResult;
      if(Object.keys($scope.result).length != 0)
        drawMap($scope.result);
    }
  );
});

app.controller('TimeCtrl', function($scope, $window, Asterix) {
  $scope.result = {};
  $scope.$watch(
    function() {
      return Asterix.timeResult;
    },

    function(newResult) {
      $scope.result = newResult;
    }
  );
});

app.controller('HashTagCtrl', function($scope, $window, Asterix) {
  $scope.result = {};
  $scope.$watch(
    function() {
      return Asterix.hashTagResult;
    },

    function(newResult) {
      $scope.result = newResult;
    }
  );
});

app.controller('ExceptionCtrl', function($scope, $window, Asterix) {
  $scope.$watch(
    function() {
      return Asterix.errorMessage;
    },

    function(newMsg) {
      if (newMsg) $window.alert(newMsg);
      Asterix.errorMessage = null;
    }
  );
});

app.controller('D3Ctrl', function($scope, $http, $timeout, Asterix) {

});
