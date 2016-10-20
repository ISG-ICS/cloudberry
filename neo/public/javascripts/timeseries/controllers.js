angular.module('cloudberry.timeseries', ['cloudberry.common'])
  .controller('TimeSeriesCtrl', function ($scope, $window, Asterix) {
    $scope.result = {};
    $scope.resultArray = [];
    $scope.d3 = $window.d3;
    $scope.dc = $window.dc;
    $scope.crossfilter = $window.crossfilter;
    $scope.preProcess = function (result) {
      // TODO make the pattern can be changed by the returned result parameters
      var result_array = [];
      if (result && result[0]) {
        var granu = Object.keys(result[0])[0];
        angular.forEach(result, function (value, key) {
          key = new Date(value[granu]);
          value = +value.count;
          result_array.push({'time': key, 'count': value});
        });
      }
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
        } else if(Asterix.queryType != 'time'){
          $scope.result = {};
          $scope.resultArray = [];
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
          var isNew = false;
          $scope.$watch('resultArray', function (newVal, oldVal) {

            if(oldVal.length == 0)
            {
                isNew = true;
                if(newVal.length == 0)
                  return;
            }

            var ndx = crossfilter(newVal);
            var timeDimension = ndx.dimension(function (d) {
              if (d.time != null)
                return d.time;
            });
            var timeGroup = timeDimension.group().reduceSum(function (d) {
              return d.count;
            });

            if (!isNew) {
              dc.redrawAll();
              return;
            }

            var timeSeries = dc.barChart(chart[0][0]);
            var timeBrush = timeSeries.brush();

            var requestFunc = function(min, max) {
              Asterix.parameters.timeInterval.start = min;
              Asterix.parameters.timeInterval.end = max;
              Asterix.queryType = 'time';
              Asterix.query(Asterix.parameters, Asterix.queryType);
            };

            timeBrush.on('brushend', function (e) {
              var extent = timeBrush.extent();
              requestFunc(extent[0], extent[1])
            });

            var minDate = Asterix.startDate;
            var maxDate = new Date();
            chart.selectAll('a').remove();
            chart.append('a')
                .text('Reset')
                .attr('href',"#")
                .on("click", function() { timeSeries.filterAll(); dc.redrawAll(); requestFunc(minDate, maxDate);})
                .style("position", "inherit")
                .style("bottom", "90%")
                .style("left", "3%");


            var startDate = (minDate.getFullYear()+"-"+(minDate.getMonth()+1));
            var endDate = (maxDate.getFullYear()+"-"+(maxDate.getMonth()+1));

            timeSeries
              .width(width)
              .height(height)
              .margins({top: margin.top, right: margin.right, bottom: margin.bottom, left: margin.left})
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
