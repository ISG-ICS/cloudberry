angular.module('cloudberry.timeseries', ['cloudberry.common'])
  .controller('TimeSeriesCtrl', function ($scope, $rootScope, $window, $compile, cloudberry, cloudberryClient, moduleManager) {
    $scope.ndx = null;
    $scope.result = {};
    $scope.resultArray = [];
    $scope.d3 = $window.d3;
    $scope.dc = $window.dc;
    $scope.crossfilter = $window.crossfilter;
    $scope.empty = [];
    $scope.totalCount = 0;
    $scope.currentTweetCount = 0;
    // to store current tweet count in the dark before showing it
    $scope.currentTweetCountStage = 0;
    $scope.queried = false;
    $scope.sumText = config.sumText;
    //Used to control behavior of time bar, time bar should start to draw diagram when received second result,
    //Otherwise there will be a blink line.
    $scope.timeseriesState = 0;
    $scope.playButtonPaused = true;
    $scope.redrawTimeSeries = true;

    for (var date = new Date(); date >= cloudberry.startDate; date.setDate(date.getDate()-1)) {
      $scope.empty.push({'time': new Date(date), 'count': 0});
    }
    $scope.preProcess = function (result) {
      // TODO make the pattern can be changed by the returned result parameters
      var result_array = [];
      $scope.currentTweetCount = 0;
      $scope.currentTweetCountStage = 0;
      if (result && result[0]) {
        var granu = Object.keys(result[0])[0];
        angular.forEach(result, function (value, key) {
          key = new Date(value[granu]);
          value = +value.count;
          $scope.currentTweetCountStage += value;
          if ($scope.totalCount > 0) {
            $scope.currentTweetCount = Math.min($scope.currentTweetCountStage, $scope.totalCount);
          }
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

    var timeSlider = document.createElement("div");
    timeSlider.id = "time-slider";
    stats.appendChild(timeSlider);

    // TODO - get rid of this watch by doing work inside the callback function through cloudberryClient.send()
    $scope.$watch(
      function() {
        return cloudberry.commonTimeSeriesResult;
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

    // send total count request periodically
    $scope.totalCountJson = {
      dataset: "twitter.ds_tweet",
      global: {
        globalAggregate: {
          field: "*",
          apply: {
            name: "count"
          },
          as: "count"
        }},
      estimable : true,
      transform: {
        wrap: {
          id: "totalCount",
          category: "totalCount"
        }
      }
    };

    function eventHandler() {
      //reset time series state
      $scope.timeseriesState = 1;
      $scope.redrawTimeSeries = true;
    }

    function timeSeriesEventHandler() {
      $scope.timeseriesState = 1;
      $scope.redrawTimeSeries = false;
    }

    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, eventHandler);
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, eventHandler);
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, eventHandler);
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, timeSeriesEventHandler);

    $scope.sendTotalCountQuery = function() {
      cloudberryClient.send($scope.totalCountJson, function(id, resultSet, resultTimeInterval){
        $scope.totalCount = resultSet[0][0].count;
        // correct current tweet count if have not done so
        if ($scope.totalCount > 0 && $scope.currentTweetCount === 0) {
          $scope.currentTweetCount = Math.min($scope.currentTweetCountStage, $scope.totalCount);
        }
      }, "totalCountResult");
    };

    var onWSReady = function(event) {
      setInterval($scope.sendTotalCountQuery, 1000);
      moduleManager.unsubscribeEvent(moduleManager.EVENT.WS_READY, onWSReady);
    };

    moduleManager.subscribeEvent(moduleManager.EVENT.WS_READY, onWSReady);

  })
  .directive('timeSeries', function (cloudberry, cloudberryConfig, moduleManager,$rootScope) {
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
           if ($scope.timeseriesState >= 1) {
            $scope.queried = true;
            var ndx = $scope.ndx;
            if (ndx) {
              if ($scope.playButtonPaused && $scope.redrawTimeSeries) {
                ndx.remove();
                ndx.add($scope.empty);
                dc.redrawAll();
                ndx.add(newVal);
                dc.redrawAll();
              }
              return;
            }

            $scope.ndx = crossfilter(newVal);
            var timeDimension;
            switch (cloudberryConfig.timeSeriesGroupBy) {
              case "day":
                timeDimension = $scope.ndx.dimension(function (d) {
                  return d3.time.day(d.time);
                });
                break;
              case "week":
              default:
                timeDimension = $scope.ndx.dimension(function (d) {
                  return d3.time.week(d.time);
                });
                break;
            }

            var timeGroup = timeDimension.group().reduceSum(function (d) {
              return d.count;
            });

            var timeSeries;
            switch (cloudberryConfig.timeSeriesChartType) {
              case "bar":
                timeSeries = dc.barChart(chart[0][0]);
                break;
              case "line":
              default:
                timeSeries = dc.lineChart(chart[0][0]);
                break;
            }
            var timeBrush = timeSeries.brush();
            var resetClink = 0;

            var requestFunc = function (min, max) {
              cloudberry.parameters.timeInterval.start = min;
              cloudberry.parameters.timeInterval.end = max;
              moduleManager.publishEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, {min: min, max: max});
            };

            // This function is to remove the "blue color" highlight of line chart in selected time range
            // It happens when the time brush is moved by user
            var removeHighlight = function () {
              var panel = $(".chart-body")[0].firstChild;
              while (panel.childElementCount !== 1) {
                panel.removeChild(panel.lastChild);
              }
            };

            // This function is to highlight the line chart in selected time range
            // It happens when the chart has redrawn after the time brush in moved by user
            var highlightChart = function () {
              var chartBody = $(".chart-body")[0];
              var extent = $(".extent")[0];
              var panel = chartBody.firstChild;
              var oldPath = panel.firstChild;
              var newPath = oldPath.cloneNode(true);

              // If user clink the "reset" button, the whole line will be highlighted, the function return.
              if (resetClink === 1 || extent.getAttribute("width") === "0") {
                resetClink += 2;
                oldPath.setAttribute("stroke", "#1f77b4");
                return;
              }

              if (panel.childElementCount !== 1) {
                removeHighlight();
              }
              var left = extent.getBoundingClientRect().left - chartBody.getBoundingClientRect().left;
              var right = chartBody.getBoundingClientRect().right - extent.getBoundingClientRect().right;

              // Dim the old line in chart by setting it to "grey color"
              oldPath.setAttribute("stroke", "#ccc");
              newPath.setAttribute("stroke", "#1f77b4");
              newPath.style.clipPath = "inset(0px " + right + "px 0px " + left + "px)";

              // Add the "blue color" highlight segment of line to the chart
              panel.appendChild(newPath);
            };

            var minDate = cloudberry.startDate;
            var maxDate = cloudberry.parameters.timeInterval.end;
            var brushInterval = {start: minDate, end: maxDate};

            // Set the times of resetClink to 0 if the keyword is change
            moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, function() {
              resetClink = 0;
              moveSliderHandlerToFront();
            });
            moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, moveSliderHandlerToFront);
            moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, moveSliderHandlerToFront);

            moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_MAP_TYPE, function (event) {
              if (event.currentMapType !== "countmap") {
                if (!$scope.playButtonPaused) {
                  document.getElementById("play-button").click();
                };
                // Move the handle to the start when map type changed
                handle.attr("cx", x(brushInterval.start));
                currentValue = x(brushInterval.start);
              }
            });

            function moveSliderHandlerToFront() {
              if (!$scope.playButtonPaused) {
                document.getElementById("play-button").click();
              }
              handle.attr("cx", x(brushInterval.start));
              currentValue = x(brushInterval.start);
            };

            timeBrush.on('brushend', function (e) {
              var extent = timeBrush.extent();
              brushInterval.start = extent[0];
              brushInterval.end = extent[1];
              requestFunc(extent[0], extent[1]);
              // Move the handle to the beginning of the brush
              handle.attr("cx", x(extent[0]));
              currentValue = x(extent[0]);
            });

            chart.selectAll('a').remove();
            chart.append('a')
              .text('Reset')
              .attr('href', "#")
              .on("click", function () {
                resetClink++;
                timeSeries.filterAll();
                dc.redrawAll();
                requestFunc(minDate, maxDate);
                brushInterval.start = minDate;
                brushInterval.end = maxDate;
                moveSliderHandlerToFront();
              })
              .style("position", "absolute")
              .style("bottom", "95%")
              .style("left", "1%");


            var startDate = (minDate.getFullYear() + "-" + (minDate.getMonth() + 1));
            var endDate = (maxDate.getFullYear() + "-" + (maxDate.getMonth() + 1));

            switch (cloudberryConfig.timeSeriesChartType) {
              case "bar":
                timeSeries
                  .width(width)
                  .height(height)
                  .margins({top: margin.top, right: margin.right, bottom: margin.bottom, left: margin.left})
                  .dimension(timeDimension)
                  .group(timeGroup)
                  .x(d3.time.scale().domain([minDate, maxDate]))
                  .xUnits(d3.time.days)
                  .xAxisLabel(startDate + "   to   " + endDate)
                  .yAxisLabel("tweets")
                  .elasticY(true)
                  .yAxis().ticks(4);
                break;
              case "line":
              default:
                timeSeries
                  .width(width)
                  .height(height)
                  .margins({top: margin.top, right: margin.right, bottom: margin.bottom, left: margin.left})
                  .dimension(timeDimension)
                  .group(timeGroup)
                  .x(d3.time.scale().domain([minDate, maxDate]))
                  .xUnits(d3.time.days)
                  .xAxisLabel(startDate + "   to   " + endDate)
                  .yAxisLabel("tweets")
                  .elasticY(true)
                  .on("postRedraw", highlightChart)
                  .on("filtered", removeHighlight)
                  .yAxis().ticks(4);
                break;
            }

            // Time slider starts here
            var currentValue = 0;
            var targetValue = width - 85;
            var svg = d3version4.select("#time-slider").append("svg")
              .attr("width", width)
              .attr("height", height);
            var x = d3version4.scaleTime()
              .domain([minDate, maxDate])
              .range([0, targetValue])
              .clamp(true);

            var slider = svg.append("g")
              .attr("class", "slider")
              .attr("transform", "translate(" + 40 + "," + 10 + ")");

            slider.append("line")
              .attr("class", "track")
              .attr("x1", x.range()[0])
              .attr("x2", x.range()[1])
              .select(function () {
                return this.parentNode.appendChild(this.cloneNode(true));
              })
              .attr("class", "track-inset")
              .select(function () {
                return this.parentNode.appendChild(this.cloneNode(true));
              })
              .attr("class", "track-overlay")
              .call(d3version4.drag()
                .on("start.interrupt", function () {
                  slider.interrupt();
                })
                .on("start drag", function () {
                  currentValue = d3version4.event.x;
                  update(x.invert(currentValue));
                })
              );

            slider.insert("g", ".track-overlay")
              .attr("class", "ticks")
              .attr("transform", "translate(0," + 18 + ")")

            var handle = slider.insert("circle", ".track-overlay")
              .attr("class", "handle")
              .attr("r", 9);

            // hide the time slider when the default map type is not countmap
            if (cloudberry.parameters.maptype !== "countmap") {
              document.getElementById("time-slider").style.display = "none";
            }

            function update(t) {
              handle.attr("cx", x(t));
              var newDate = new Date(t);
              newDate.setMonth(t.getMonth() + 1);
              if (newDate <= maxDate) {
                requestFunc(t, newDate);
              } else {
                requestFunc(t, maxDate);
              }
            }

            var playButton = d3version4.select("#play-button");

            playButton
              .on("click", function () {
                playButton.html(
                  playButton.html() ==
                  '<i class="fa fa-pause-circle-o" aria-hidden="true"></i>' ?
                  '<i class="fa fa-play-circle-o" aria-hidden="true"></i>' :
                  '<i class="fa fa-pause-circle-o" aria-hidden="true"></i>');
                if (!$scope.playButtonPaused) {
                  clearInterval(timer);
                  // Enable sidebar when time slider on "pause" mode
                  document.getElementById("hamburgerButton").disabled = false;
                  $scope.playButtonPaused = true;
                } else {
                  timer = setInterval(step, 800);
                  // Disable sidebar when time slider on "play" mode
                  document.getElementById("hamburgerButton").disabled = true;
                  $rootScope.$emit("CallCloseMethod", {});
                  $scope.playButtonPaused = false;
                }
              });

            function step() {
              update(x.invert(currentValue));
              // Determine the step (one step every 5 days)
              var diffTime = Math.abs(maxDate - minDate);
              var diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)); 
              currentValue = currentValue + (targetValue / diffDays) * 5;
              if (x.invert(currentValue) >= brushInterval.end) {
                // Enable sidebar when time slider done playing
                document.getElementById("hamburgerButton").disabled = false;
                $scope.playButtonPaused = true;
                currentValue = x(brushInterval.start);
                clearInterval(timer);
                handle.attr("cx", x(brushInterval.start));
                playButton.html('<i class="fa fa-play-circle-o" aria-hidden="true"></i>');
                requestFunc(brushInterval.start, brushInterval.end);
              }
            }

            dc.renderAll();
            timeSeries.filter([minDate, maxDate]);
          }
          $scope.timeseriesState += 1;
        });
      }
    }
  });
