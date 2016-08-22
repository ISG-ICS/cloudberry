angular.module('cloudberry.timeseries', ['cloudberry.common'])
  .controller('TimeSeriesCtrl', function ($scope, $window, Asterix) {
    $scope.result = {};
    $scope.resultArray = [];
    $scope.d3 = $window.d3;
    $scope.dc = $window.dc;
    $scope.crossfilter = $window.crossfilter;
    $scope.preProcess = function (result) {
      // TODO make the pattern can be changed by the returned result parameters
      var parseDate = d3.time.format("%Y-%m-%d").parse;
      var result_array = [];
      angular.forEach(result, function (value, key) {
        key = parseDate(value.key);
        value = +value.count;
        result_array.push({'time':key, 'count':value});
      });
      return result_array;
    };
    $scope.$watch(
      function() {
        return Asterix.timeResult;
      },

      function(newResult) {
        if(newResult && Asterix.queryType != 'time') {
          $scope.result = newResult;
          $scope.resultArray = $scope.preProcess(newResult);
        }
      }
    );
  })
  .directive('timeSeries', function (Asterix) {
    var margin = {
      top: 10,
      right: 30,
      bottom: 30,
      left: 40
    };
    var width = 962 - margin.left - margin.right;
    var height = 150 - margin.top - margin.bottom;
      return {
        restrict: "E",
        controller: 'TimeSeriesCtrl',
        link: function ($scope, $element, $attrs) {
          var chart = d3.select($element[0]);
          $scope.$watch('resultArray', function (newVal, oldVal) {

            if(oldVal.length == 0)
            {
                if(newVal.length == 0)
                  return;
            }

            chart.selectAll('*').remove();

            var timeSeries = dc.barChart(chart[0][0]);
            var timeBrush = timeSeries.brush();
            timeBrush.on('brushend', function (e) {
              var extent = timeBrush.extent();
              Asterix.parameters.time.start = extent[0];
              Asterix.parameters.time.end = extent[1];
              Asterix.queryType = 'time';
              Asterix.query(Asterix.parameters, Asterix.queryType);
            });

            var ndx = crossfilter(newVal);
            var timeDimension = ndx.dimension(function (d) {
              if (d.time != null)
                return d.time;
            })
            var timeGroup = timeDimension.group().reduceSum(function (d) {
              return d.count;
            });

            var minDate = timeDimension.bottom(1)[0].time;
            var maxDate = timeDimension.top(1)[0].time;

            chart.append('a')
                .text('Reset')
                .attr('href',"#")
                .on("click", function() { timeSeries.filterAll(); dc.redrawAll();})
                .style("position", "inherit")
                .style("bottom", "90%")
                .style("left", "3%");


            var startDate = (minDate.getFullYear()+"-"+(minDate.getMonth()+1));
            var endDate = (maxDate.getFullYear()+"-"+(maxDate.getMonth()+1));

            timeSeries
              .width(width)
              .height(height)
              .dimension(timeDimension)
              .group(timeGroup)
              .centerBar(true)
              .x(d3.time.scale().domain([minDate, maxDate]))
              .xUnits(d3.time.days)
              .gap(1)
              .xAxisLabel(startDate + "   to   " + endDate);



            dc.renderAll();

          })
        }
      };
  });
