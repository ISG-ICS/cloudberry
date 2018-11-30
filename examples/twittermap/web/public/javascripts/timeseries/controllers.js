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

        // TODO - get rid of this watch by doing work inside the callback function through cloudberryClient.send()
        $scope.$watch(
            function() {
                return cloudberry.commonTimeSeriesResult;
            },

            function(newResult) {
                if(newResult) {
                    $scope.result = newResult;
                    $scope.resultArray = $scope.preProcess(newResult);
                    console.log("newResult: ", newResult);
                    //console.log("resultArray above: ", $scope.resultArray);

                    $scope.newResultsCount = new Array();
                    $scope.newResultsDay = new Array();

                    // created a new dict type to store the day and the count of data
                    $scope.newResultDict = new Object();
                    $scope.isDuplicated = [];

                    for (var i = 0; i < newResult.length; i++) {
                        if (!$scope.isDuplicated.includes(newResult[i].day.substring(0, 7))) {
                            $scope.isDuplicated[i] = newResult[i].day.substring(0, 7);
                            $scope.newResultDict[newResult[i].day.substring(0, 7)] = newResult[i].count;
                        }
                        else {
                            console.log("day: ", newResult[i].day.substring(0, 7));
                            console.log("there's this date already: ", $scope.newResultDict[newResult[i].day.substring(0, 7)]);
                            $scope.newResultDict[newResult[i].day.substring(0, 7)] += newResult[i].count;
                        }
                    }

                    function sortOnKeys(dict) {

                        var sorted = [];
                        for(var key in dict) {
                            sorted[sorted.length] = key;
                        }
                        sorted.sort();

                        var tempDict = {};
                        for(var i = 0; i < sorted.length; i++) {
                            tempDict[sorted[i]] = dict[sorted[i]];
                        }

                        return tempDict;
                    }

                    $scope.newResultDict = sortOnKeys($scope.newResultDict);

                    console.log("duplicatedArr: ", $scope.isDuplicated);
                    console.log("dict: ", $scope.newResultDict);
                    console.log("d_length: ", Object.keys($scope.newResultDict).length);

                    $scope.newResultsDay = Object.keys($scope.newResultDict);
                    for (var j = 0; j < Object.keys($scope.newResultDict).length; j++) {
                        // $scope.newResultsDay = Object.keys($scope.newResultDict);
                        $scope.newResultsDay[j] = $scope.newResultsDay[j].replace(/-/g, '/');
                        $scope.newResultsCount = Object.values($scope.newResultDict);
                    }

                    // console.log("dict: ", $scope.newResultDict);
                    console.log("day: ", $scope.newResultsDay);
                    console.log("count: ", $scope.newResultsCount);
                    $scope.drawCharts($scope.newResultsDay, $scope.newResultsCount);

                } else {
                    $scope.result = {};
                    $scope.resultArray = [];
                }
            }
        );

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
        var margin = {
            top: 10,
            right: 30,
            bottom: 40,
            left: 40
        };
        // set the initial width of the timeline equal to the initial width of the browser window
        var width = $(window).width() * 0.6 - margin.left - margin.right;
        var height = 150 - margin.top - margin.bottom;
        return {
            restrict: "E",
            controller: 'TimeSeriesCtrl',
            link: function ($scope, $element, $attrs) {
                $scope.$watch('resultArray', function (newVal, oldVal) {
                    var minDate = cloudberry.startDate;
                    console.log("minDate: ", minDate);
                    var maxDate = cloudberry.parameters.timeInterval.end;
                    console.log("maxDate: ", maxDate);

                    var startDate = (minDate.getFullYear()+"-"+(minDate.getMonth()+1));
                    var endDate = (maxDate.getFullYear()+"-"+(maxDate.getMonth()+1));
                    //var startDateForAxis = startDate.split("-");
                    //var endDateForAxis = endDate.split("-");

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
                        ndx.add(newVal);
                        return;
                    }
                    $scope.ndx = crossfilter(newVal);

                    var timeDimension = $scope.ndx.dimension(function (d) {
                        return d3.time.week(d.time);
                    });
                    var timeGroup = timeDimension.group().reduceSum(function (d) {
                        return d.count;
                    });

                    var requestFunc = function(min, max) {
                        cloudberry.parameters.timeInterval.start = min;
                        cloudberry.parameters.timeInterval.end = max;
                        moduleManager.publishEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, {min: min, max: max});
                    };

                    $scope.drawCharts = function (day, count) {
                        var chart = {
                            type: 'area',
                            zoomType: 'x', // 이 한줄로 zoom 가능
                            width: width,
                            height: height,
                            margin: [margin.top, margin.right, margin.bottom, margin.left],
                            backgroundColor: null,
                            events: {
                                selection: function(event) {

                                }
                            }
                        };
                        var title = {
                            text: ''
                        }
                        var subtitle = {
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
                        var yAxis = {
                            title: {
                                text: ''
                            }
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

                        var json = {};
                        json.chart = chart;
                        json.title = title;
                        json.subtitle = subtitle;
                        json.xAxis = xAxis;
                        json.yAxis = yAxis;
                        json.tooltip = tooltip;
                        json.rangeSelector = rangeSelector;
                        json.plotOptions = plotOptions;
                        json.credits = credits;
                        json.series = series;
                        $('#chart').highcharts(json);
                    }
                });
            }
        }
    });


