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
  .directive('map', function () {
    return {
      restrict: "E",
      scope: {
        config: "="
      },
      link: function ($scope, $element, $attrs) {
        $scope.map = L.map('map').setView([36.5,-96.35],4);
        L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
          attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
          maxZoom: 18,
          id: 'jeremyli.p6f712pj',
          accessToken: 'pk.eyJ1IjoiamVyZW15bGkiLCJhIjoiY2lrZ2U4MWI4MDA4bHVjajc1am1weTM2aSJ9.JHiBmawEKGsn3jiRK_d0Gw'
        }).addTo($scope.map);
      }
    };
  });