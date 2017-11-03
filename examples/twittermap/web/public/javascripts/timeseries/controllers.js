angular.module('cloudberry.timeseries', ['cloudberry.common'])
  .controller('TimeSeriesCtrl', function ($scope, $window, $compile, cloudberry) {
    $scope.ndx = null;
    $scope.result = {};
    $scope.resultArray = [];
    $scope.d3 = $window.d3;
    $scope.dc = $window.dc;
    $scope.crossfilter = $window.crossfilter;
    $scope.empty = [];
    $scope.totalCount = 0;
    $scope.currentTweetCount = 0;
    $scope.queried = false;
    $scope.sumText = config.sumText;
    for (var date = new Date(); date >= cloudberry.startDate; date.setDate(date.getDate()-1)) {
      $scope.empty.push({'time': new Date(date), 'count': 0});
    }
    $scope.preProcess = function (result) {
      // TODO make the pattern can be changed by the returned result parameters
      var result_array = [];
      $scope.currentTweetCount = 0;
      if (result && result[0]) {
        var granu = Object.keys(result[0])[0];
        angular.forEach(result, function (value, key) {
          key = new Date(value[granu]);
          value = +value.count;
          $scope.currentTweetCount += value;
          result_array.push({'time': key, 'count': value});
        });

      }
      return result_array;
    };

    // add information about the count of tweets
    var countDiv = document.createElement("div");
    countDiv.id = "count-div";
    countDiv.title = "Display the count information of Tweets";
    countDiv.innerHTML = [
      "<div ng-if='queried'><p id='count'>{{ currentTweetCount | number:0 }}<span id='count-text'>&nbsp;&nbsp;of&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span></p></div>",
      "<p id='count'>{{ totalCount | number:0 }}<span id='count-text'>&nbsp;&nbsp;{{sumText}}</span></p>",
    ].join("");
    var stats = document.getElementsByClassName("stats")[0];
    $compile(countDiv)($scope);
    stats.appendChild(countDiv);


    $scope.$watch(
      function() {
        return cloudberry.timeResult;
      },

      function(newResult) {
        if(newResult) {
          $scope.result = newResult;
          $scope.resultArray = $scope.preProcess(newResult);
        } else {
          $scope.result = {};
          $scope.resultArray = [];
        }
      }
    );

    $scope.$watch(
      function () {
        return cloudberry.totalCount;
      },

      function (newCount) {
        if(newCount) {
          $scope.totalCount = newCount;
        }
      }
    );

  })
  .directive('timeSeries', function (cloudberry) {
    var margin = {
      top: 10,
      right: 30,
      bottom: 30,
      left: 40
    };
    // set the initial width of the timeline equal to the initial width of the browser window
    var width = $(window).width() * 0.6 - margin.left - margin.right;
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
            
            $scope.queried = true;
            var ndx = $scope.ndx;
            if (ndx) {
              ndx.remove();
              ndx.add($scope.empty);
              dc.redrawAll();
              ndx.add(newVal);
              dc.redrawAll();
              return;
            }

            $scope.ndx = crossfilter(newVal);
            var timeDimension = $scope.ndx.dimension(function (d) {
              return d3.time.day(d.time);
            });
            var timeGroup = timeDimension.group().reduceSum(function (d) {
              return d.count;
            });

            var timeSeries = dc.barChart(chart[0][0]);
            var timeBrush = timeSeries.brush();

            var requestFunc = function(min, max) {
              cloudberry.parameters.timeInterval.start = min;
              cloudberry.parameters.timeInterval.end = max;
              cloudberry.query(cloudberry.parameters);
            };

            timeBrush.on('brushend', function (e) {
              var extent = timeBrush.extent();
              requestFunc(extent[0], extent[1])
            });

            var minDate = cloudberry.startDate;
            var maxDate = cloudberry.parameters.timeInterval.end;
            chart.selectAll('a').remove();
            chart.append('a')
                .text('Reset')
                .attr('href',"#")
                .on("click", function() { timeSeries.filterAll(); dc.redrawAll(); requestFunc(minDate, maxDate);})
                .style("position", "absolute")
                .style("bottom", "90%")
                .style("left", "5%");


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
              .xAxisLabel(startDate + "   to   " + endDate)
              .elasticY(true);



            dc.renderAll();

          })
        }
      };
  });
