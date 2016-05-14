angular.module('cloudberry.dashboard', [])
  .directive('lineChart', function () {
    return {
      restrict: "E",
      scope: {
        config: "="
      },
      link: function ($scope, $element, $attrs) {
        var chart = d3.select($element[0]);
        var linechart = dc.lineChart(chart[0][0]);
        $scope.$watch('config', function (newVal, oldVal) {
          if (newVal.length == 0)
            return;
          chart.selectAll('*').remove();

          linechart
            .renderArea($scope.config.renderArea)
            .width($scope.config.width - $scope.config.margin.left - $scope.config.margin.right)
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
      link: function ($scope, $element, $attrs) {
        var chart = d3.select($element[0]);
        var piechart = dc.pieChart(chart[0][0]);
        $scope.$watch('config', function (newVal, oldVal) {
          if (newVal.length == 0)
            return;
          chart.selectAll('*').remove();

          piechart
            .width($scope.config.width)
            .height($scope.config.height)
            .dimension($scope.config.dimension)
            .group($scope.config.group)
            .innerRadius($scope.config.innerRadius);

          piechart.render();
        });
      }
    };
  });