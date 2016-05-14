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
        if($scope.config.watch) {
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

              dc.renderAll();
          });
        }
        else {
          console.log($scope.config)
          linechart
            .renderArea($scope.config.renderArea)
            .width($scope.config.width - $scope.config.margin.left - $scope.config.margin.right)
            .height($scope.config.height - $scope.config.margin.bottom - $scope.config.margin.top)
            .margins($scope.config.margin)
            .dimension($scope.config.dimension)
            .group($scope.config.group)
            .x($scope.config.scale);

          dc.renderAll();
        }
      }
    };
  });