angular.module('cloudberry.dashboard', [])
  .directive('lineChart', function () {
    return {
      restrict: "E",
      scope: {
        config: "="
      },
      template: [
        '<div class="col-md-{{$scope.config.grid}}">',
        '</div>'
      ].join(''),
      link: function ($scope, $element, $attrs) {
        var chart = d3.select($element[0]);
        var linechart = dc.lineChart(chart[0][0]);
        $scope.$watch('config', function (newVal, oldVal) {
          chart.selectAll('*').remove();

          linechart
            .renderArea($scope.config.renderArea)
            .width($(window).width()*$scope.config.grid/12 - $scope.config.margin.left - $scope.config.margin.right)
            .height($scope.config.height - $scope.config.margin.bottom - $scope.config.margin.top)
            .margins($scope.config.margin)
            .dimension($scope.config.dimension)
            .group($scope.config.group)
            .x($scope.config.scale);

          linechart.render();
        });
      }
    };
  })
  .directive('pieChart', function () {
    return {
      restrict: "E",
      scope: {
        config: "="
      },
      template: [
        '<div class="col-md-{{$scope.config.grid}}">',
        '</div>'
      ].join(''),
      link: function ($scope, $element, $attrs) {
        var chart = d3.select($element[0]);
        var piechart = dc.pieChart(chart[0][0]);
        $scope.$watch('config', function (newVal, oldVal) {
          if (newVal.length == 0)
            return;
          chart.selectAll('*').remove();

          piechart
            .width($(window).width()*$scope.config.grid/12-2*$scope.config.margin)
            .height($scope.config.height-2*$scope.config.margin)
            .dimension($scope.config.dimension)
            .group($scope.config.group)
            .innerRadius($scope.config.innerRadius);

          piechart.render();
        });
      }
    };
  })
  .directive('rowChart', function () {
    return {
      restrict: "E",
      scope: {
        config: "="
      },
      template: [
        '<div class="col-md-{{$scope.config.grid}}">',
        '</div>'
      ].join(''),
      link: function ($scope, $element, $attrs) {
        var chart = d3.select($element[0]);
        var rowchart = dc.rowChart(chart[0][0]);
        $scope.$watch('config', function (newVal, oldVal) {
          if (newVal.length == 0)
            return;
          chart.selectAll('*').remove();

          rowchart
            .width($(window).width()*$scope.config.grid/12)
            .height($scope.config.height)
            .dimension($scope.config.dimension)
            .group($scope.config.group)
            .elasticX(true);

          rowchart.render();
        });
      }
    };
  })
  .directive('map', function ($http) {
    return {
      restrict: "E",
      scope: {
        config: "="
      },
      link: function ($scope, $element, $attrs) {
        $http.get("assets/data/state.json")
          .success(function(data) {
            angular.forEach(data.features, function(d) {
              if (d.properties.count)
                d.properties.count = 0;
              for (var k in $scope.config.data) {
                if ($scope.config.data[k].state == d.properties.stateID)
                  d.properties.count += $scope.config.data[k].count;
              }
            });
            draw(data);
          })
          .error(function(data) {
            console.log("Load state data failure");
          });

        function draw(geojson) {
          var chart = d3.select($element[0]);
          var leafletChoroplethChart = dc.leafletChoroplethChart(chart[0][0]);

          leafletChoroplethChart
            .width($(window).width()*$scope.config.grid/12)
            .height($scope.config.height)
            .dimension($scope.config.dimension)
            .group($scope.config.group)
            .center([36.5, -96.35])
            .zoom(4)
            .geojson(geojson)
            .colors(['#053061', '#2166ac', '#4393c3', '#92c5de', '#d1e5f0', '#f7f7f7', '#fddbc7', '#f4a582', '#d6604d', '#b2182b', '#67001f'])
            .colorDomain(function() {
              return [dc.utils.groupMin(this.group(), this.valueAccessor()),
                dc.utils.groupMax(this.group(), this.valueAccessor())];
            })
            .colorAccessor(function(d,i) {
              return d.value;
            })
            .featureKeyAccessor(function(feature) {
              return feature.properties.stateID;
            });

          dc.renderAll();

        }
      }
    };
  });