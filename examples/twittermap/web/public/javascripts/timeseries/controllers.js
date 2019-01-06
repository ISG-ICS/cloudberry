angular.module('cloudberry.timeseries', ['cloudberry.common'])
  .controller('TimeSeriesCtrl', function ($scope, $window, $compile, cloudberry, moduleManager) {
    $scope.ndx = null;
    $scope.resultArray = [];
    $scope.crossfilter = $window.crossfilter;
    $scope.empty = [];
    $scope.totalCount = 0;
    $scope.currentTweetCount = 0;
    $scope.queried = false;
    $scope.sumText = config.sumText;
    $scope.drawTimeSereis = true;
    const initialTimeBarStart = new Date(cloudberry.startDate);
    initialTimeBarStart.setMonth(initialTimeBarStart.getMonth()-1);
    const initialTimeBarEnd = new Date(cloudberry.parameters.timeInterval.end);
    initialTimeBarEnd.setMonth(initialTimeBarEnd.getMonth()+1);

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

    // This function will redraw timebar whenever new event has been triggered
    var enableTimebarChange = function(){
        var min = new Date(initialTimeBarStart);
        var max = new Date(initialTimeBarEnd);
        if(!$scope.drawTimeSereis) {
            $scope.drawTimeSereis = true;
            requestFunc(min,max);
        }
    };

    var requestFunc = function(min, max) {
      cloudberry.parameters.timeInterval.start = min;
      cloudberry.parameters.timeInterval.end = max;
      moduleManager.publishEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, {min, max});
    };

    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL,enableTimebarChange);
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG,enableTimebarChange);
    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD,enableTimebarChange);


    // TODO - get rid of this watch by doing work inside the callback function through cloudberryClient.send()
    $scope.$watch(
      function() {
        return cloudberry.commonTimeSeriesResult;
      },
      function(newResult) {
        if(newResult) {
          $scope.resultArray = $scope.preProcess(newResult);
            // created a new dict type to store the day and the count of data
            $scope.newResultDict = {};
            $scope.isDuplicated = [];
            for (var i = 0; i < newResult.length; i++) {
                if (!$scope.isDuplicated.includes(newResult[i].day.substring(0, 7))) {
                    $scope.isDuplicated[i] = newResult[i].day.substring(0, 7);
                    $scope.newResultDict[newResult[i].day.substring(0, 7)] = newResult[i].count;
                }
                else {
                    $scope.newResultDict[newResult[i].day.substring(0, 7)] += newResult[i].count;
                }
            }

            var temp = {};
            Object.keys($scope.newResultDict).sort().forEach(function(key) {
                temp[key] = $scope.newResultDict[key];
            });


            $scope.newResultDict = temp;

            if($scope.drawTimeSereis) {
                $scope.newResultsCount = Object.values($scope.newResultDict);
                $scope.newResultsDay = Object.keys($scope.newResultDict).map(x=>x.replace(/-/g,'/'));
                $scope.drawCharts($scope.newResultsDay, $scope.newResultsCount);
            }


        } else {
          $scope.resultArray = [];
        }
      }
    );
      var margin = {
          top: 10,
          right: 30,
          bottom: 40,
          left: 40
      };
      // set the initial width of the timeline equal to the initial width of the browser window
      var width = $(window).width() * 0.6 - margin.left - margin.right;
      var height = 160 - margin.top - margin.bottom;
      var minDate = cloudberry.startDate;
      var maxDate = cloudberry.parameters.timeInterval.end;

      var startDate = (minDate.getFullYear()+"-"+(minDate.getMonth()+1));
      var endDate = (maxDate.getFullYear()+"-"+(maxDate.getMonth()+1));



      $scope.drawCharts = function (day, count) {

          var resetZoomButton = {
              position:{
                  y : 30
              }
          };
          var chart = {
              type: 'area',
              resetZoomButton,
              zoomType: 'x',
              width: width,
              height: height,
              margin: [margin.top, margin.right, margin.bottom, margin.left],
              backgroundColor: null,
              events: {
                  selection:function(event){
                      if( !event.resetSelection ) {
                          selectedMin = event.xAxis[0].min;
                          selectedMax = event.xAxis[0].max;
                          var left = Date.parse(cloudberry.parameters.timeInterval.start);
                          var right = Date.parse(cloudberry.parameters.timeInterval.end);
                          var timeBarRange  = event.xAxis[0].axis.getExtremes().max - event.xAxis[0].axis.getExtremes().min;
                          var difference = right - left;
                          var minRatio = selectedMin / (timeBarRange);
                          var maxRatio = selectedMax / (timeBarRange);
                          var min = new Date(left + minRatio * difference);
                          var max = new Date(left + maxRatio * difference);
                          $scope.drawTimeSereis = false;
                          requestFunc(min, max);
                      }
                      else{
                          enableTimebarChange();
                      }

                  }

              }

          };
          // Set chart title to be empty
          var title = {
              text: ''
          };
          var xAxis = {
              tickmarkPlacement: 'on',
              title: {
                  text: startDate + "   to   " + endDate
              },
              categories: day,
              type: 'datetime'
          };

          var tooltip = {
              crosshairs: true,
              shared: true,
              valueSuffix: ''
          };
          var rangeSelector = {
              enabled: false
          };
          var plotOptions = {
              area: {
                  stacking: 'normal',
                  lineColor: '#92d1e1',
                  lineWidth: 1,

                  marker: {
                      lineWidth: 1,
                      lineColor: '#92d1e1'
                  }
              }
          };
          var credits = {     // watermark
              enabled: false
          };
          var series = [
              {
                  showInLegend: false,
                  name: 'count',
                  data: count
              }
          ];



          var options = {
              chart,
              title,
              xAxis,
              tooltip,
              rangeSelector,
              plotOptions,
              credits,
              series,
          };
          $('#chart').highcharts(options);
      }

    // TODO - get rid of this watch by doing work inside the callback function through cloudberryClient.send()
    $scope.$watch(
      function () {
        return cloudberry.commonTotalCount;
      },
      function (newCount) {
        if(newCount) {
          $scope.totalCount = newCount;
        }
      }
    );

  })
  .directive('timeSeries', function (cloudberry, moduleManager) {
      return {
        restrict: "E",
        controller: 'TimeSeriesCtrl',
        link: function ($scope, $element, $attrs) {
          $scope.$watch('resultArray', function (newVal, oldVal) {

            if(oldVal.length == 0)
            {
                if(newVal.length == 0)
                  return;
            }
            $scope.queried = true;

          })
        }
      };
  });
