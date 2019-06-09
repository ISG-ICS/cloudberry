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

    $scope.sendTotalCountQuery = function() {
      cloudberryClient.send($scope.totalCountJson, function(id, resultSet, resultTimeInterval){
        $scope.totalCount = resultSet[0][0].count;
      }, "totalCountResult");
    };

    var onWSReady = function(event) {
      setInterval($scope.sendTotalCountQuery, 1000);
      moduleManager.unsubscribeEvent(moduleManager.EVENT.WS_READY, onWSReady);
    };

    moduleManager.subscribeEvent(moduleManager.EVENT.WS_READY, onWSReady);

  })
  .directive('timeSeries', function (cloudberry, moduleManager,$rootScope) {
    var onPlay = false;
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
              if (!onPlay) {
                ndx.remove();
                ndx.add($scope.empty);
                dc.redrawAll();
                ndx.add(newVal);
                dc.redrawAll();
              }
              return;
            }

            $scope.ndx = crossfilter(newVal);
            var timeDimension = $scope.ndx.dimension(function (d) {
              return d3.time.week(d.time);
            });
            var timeGroup = timeDimension.group().reduceSum(function (d) {
              return d.count;
            });

            var timeSeries = dc.lineChart(chart[0][0]);
            var timeBrush = timeSeries.brush();
            var resetClink = 0;

            var requestFunc = function(min, max) {
              cloudberry.parameters.timeInterval.start = min;
              cloudberry.parameters.timeInterval.end = max;
              moduleManager.publishEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, {min: min, max: max});
            };

            // This function is to remove the "blue color" highlight of line chart in selected time range
            // It happens when the time brush is moved by user
            var removeHighlight = function() {
              var panel = $(".chart-body")[0].firstChild;
              while (panel.childElementCount !== 1) {
                panel.removeChild(panel.lastChild);
              }
            };

            // This function is to highlight the line chart in selected time range
            // It happens when the chart has redrawn after the time brush in moved by user
            var highlightChart = function() {
              var chartBody = $(".chart-body")[0];
              var extent = $(".extent")[0];
              var panel = chartBody.firstChild;
              var oldPath = panel.firstChild;
              var newPath = oldPath.cloneNode(true);

              // If user clink the "reset" button, the whole line will be highlighted, the function return.
              if (resetClink === 1 || extent.getAttribute("width") === "0") {
                resetClink += 2;
                oldPath.setAttribute("stroke", "#1f77b4");
                return ;
              }

              if (panel.childElementCount !== 1) {
                removeHighlight();
              }
              var left = extent.getBoundingClientRect().left - chartBody.getBoundingClientRect().left;
              var right = chartBody.getBoundingClientRect().right - extent.getBoundingClientRect().right;

              // Dim the old line in chart by setting it to "grey color"
              oldPath.setAttribute("stroke", "#ccc");
              newPath.setAttribute("stroke", "#1f77b4");
              newPath.style.clipPath = "inset(0px "+right+"px 0px "+left+"px)";

              // Add the "blue color" highlight segment of line to the chart
              panel.appendChild(newPath);
            };

            var minDate = cloudberry.startDate;
            var maxDate = cloudberry.parameters.timeInterval.end;

            // Set the times of resetClink to 0 if the keyword is change
            moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, function(){
              resetClink = 0;
              
              if (playButton.text() === "Pause") {
                document.getElementById("play-button").click();
              }
              onPlay = false;
              requestFunc(brushInterval.start, brushInterval.end);
              // Move the handle to the start when keyword changed
              handle.attr("cx", x(brushInterval.start));
              currentValue = x(brushInterval.start);
            });
            
            moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_MAP_TYPE, function(event) {
              if (event.currentMapType !== "countmap") {
                if (playButton.text() === "Pause") {
                  document.getElementById("play-button").click();
                }
                onPlay = false;
                requestFunc(brushInterval.start, brushInterval.end);
                // Move the handle to the start when map type changed
                handle.attr("cx", x(brushInterval.start));
                currentValue = x(brushInterval.start);
              }
            });
            
            var brushInterval = {start: minDate, end: maxDate};

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
                .attr('href',"#")
                .on("click", function() { resetClink++; timeSeries.filterAll(); dc.redrawAll(); requestFunc(minDate, maxDate);})
                .style("position", "absolute")
                .style("bottom", "95%")
                .style("left", "1%");


            var startDate = (minDate.getFullYear()+"-"+(minDate.getMonth()+1));
            var endDate = (maxDate.getFullYear()+"-"+(maxDate.getMonth()+1));


            timeSeries
              .width(width)
              .height(height)
              .margins({top: margin.top, right: margin.right, bottom: margin.bottom, left: margin.left})
              .dimension(timeDimension)
              .group(timeGroup)
              .x(d3.time.scale().domain([minDate, maxDate]))
              .xUnits(d3.time.days)
              .xAxisLabel(startDate + "   to   " + endDate)
              .elasticY(true)
              .on("postRedraw", highlightChart)
              .on("filtered", removeHighlight);


            
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
                .select(function() { return this.parentNode.appendChild(this.cloneNode(true)); })
                .attr("class", "track-inset")
                .select(function() { return this.parentNode.appendChild(this.cloneNode(true)); })
                .attr("class", "track-overlay")
                .call(d3version4.drag()
                    .on("start.interrupt", function() { slider.interrupt(); })
                    .on("start drag", function() {
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
  
            function update(t) {
                handle.attr("cx", x(t));
                var newDate = new Date(t);
                newDate.setMonth(t.getMonth() + 1);
                if(newDate <= maxDate) {
                  requestFunc(t, newDate);
                } else {
                  requestFunc(t, maxDate);
                }               
            }

            var playButton = d3version4.select("#play-button");

            playButton
              .on("click", function() {
              var button = d3version4.select(this);
              onPlay = true;
              if (button.text() == "Pause") {
                clearInterval(timer);
                //Enable sidebar when time slider on "pause" mode
                document.getElementById("hamburgerButton").disabled = false;
                button.text("Play");
              } else {
                timer = setInterval(step, 800);
                //Disable sidebar when time slider on "play" mode
                document.getElementById("hamburgerButton").disabled = true;
                $rootScope.$emit("CallCloseMethod", {});
                button.text("Pause");
              }
            });

            function step() {
              update(x.invert(currentValue));
              // Determine the step (one step per month)
              var numberOfMonth = maxDate.getMonth() - minDate.getMonth() +
                (12 * (maxDate.getFullYear() - minDate.getFullYear()));
              currentValue = currentValue + (targetValue/numberOfMonth);
              if (x.invert(currentValue) >= brushInterval.end) {
                  //Enable sidebar when time slider done playing
                  document.getElementById("hamburgerButton").disabled = false;
                  onPlay = false;
                  currentValue = x(brushInterval.start);
                  clearInterval(timer);
                  playButton.text("Play");
                  handle.attr("cx", x(brushInterval.start));
                  requestFunc(brushInterval.start, brushInterval.end);
              }
              if (currentValue > targetValue) {
                  //Enable sidebar when time slider done playing
                  document.getElementById("hamburgerButton").disabled = false;
                  onPlay = false;
                  currentValue = 0;
                  clearInterval(timer);
                  playButton.text("Play");
                  handle.attr("cx", x(minDate));
                  requestFunc(minDate, maxDate);
                }
              }
   
          dc.renderAll();
          timeSeries.filter([minDate, maxDate]);
        })
      }
    };
});