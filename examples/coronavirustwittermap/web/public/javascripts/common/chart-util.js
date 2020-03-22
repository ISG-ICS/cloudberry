/*
 * This service is for drawing tendency line chart.
 * It provides function for preprocessing chart data, and using chart.js to draw chart.
 * It is used by popup window and hash tag module now.
 */
angular.module("cloudberry.common")
  .service("chartUtil", function (cloudberry) {

    var chartUtil = {
      // Return difference of two arrays, the arrays must has no duplicate
      arrayDiff(newArray, oldArray) {
        var diffArray = [], difference = [];
        for (var i = 0; i < newArray.length; i++) {
          diffArray[newArray[i]] = true;
        }
        for (var j = 0; j < oldArray.length; j++) {
          if (diffArray[oldArray[j]]) {
            delete diffArray[oldArray[j]];
          } else {
            diffArray[oldArray[j]] = true;
          }
        }
        for (var key in diffArray) {
          difference.push(key);
        }
        return difference;
      },


      // Complement chart data by adding empty data point and then sort it
      complementData(chartData, hasCountMonth) {
        var zeroCountMonth = [];
        var minDate = cloudberry.parameters.timeInterval.start;
        var maxDate = cloudberry.parameters.timeInterval.end;

        // add empty data point
        for (var m = new Date(minDate.getFullYear(), minDate.getMonth()); m <= new Date(maxDate.getFullYear(), maxDate.getMonth()); m.setMonth(m.getMonth() + 1)) {
          zeroCountMonth.push(new Date(m.getTime()));
        }
        zeroCountMonth = this.arrayDiff(hasCountMonth, zeroCountMonth);
        for (var j = 0; j < zeroCountMonth.length; j++) {
          chartData.push({x: new Date(zeroCountMonth[j]), y: 0});
        }

        // sort the date
        chartData.sort(function (previousVal, currentVal) {
          return previousVal.x - currentVal.x;
        });
        return chartData;
      },


      // Preprocess query result to chart data could be used by chart.js
      // The `queryResult` is group by month, after prepocess it is still group by month.
      preProcessByMonthResult(queryResult) {
        var chartData = [];
        var hasCountMonth = [];
        for (var i = 0; i < queryResult.length; i++) {
          var thisMonth = new Date(queryResult[i].month.split(("-"))[0], queryResult[i].month.split(("-"))[1] - 1);
          hasCountMonth.push(thisMonth);
          chartData.push({x: thisMonth, y: queryResult[i].count});
        }
        return this.complementData(chartData, hasCountMonth);
      },


      // Preprocess query result to chart data could be used by chart.js
      // The `queryResult` is group by day, pre-process it into given groupBy result.
      preProcessByDayResult(queryResult, groupBy) {
        switch (groupBy) {
          case "day":
            // replace key name "day" with "x" and value name "count" with "y"
            let resultByDay = [];
            queryResult.forEach(function (element) {
              resultByDay.push({x: new Date(element.day), y: element.count});
            });
            // sort the date
            resultByDay.sort(function (previousVal, currentVal) {
              return previousVal.x - currentVal.x;
            });
            return resultByDay;
          case "week":
          // TODO
          case "month":
          default:
            // group by year
            var groupsByYear = queryResult.reduce(function (previousVal, currentVal) {
              var yearNum = currentVal.day.split(("-"))[0];
              (previousVal[yearNum]) ? previousVal[yearNum].data.push(currentVal) : previousVal[yearNum] = {
                year: yearNum,
                data: [currentVal]
              };
              return previousVal;
            }, {});
            var resultByYear = Object.keys(groupsByYear).map(function (k) {
              return groupsByYear[k];
            });

            // sum up the result for every month
            var resultByMonth = [];
            var hasCountMonth = [];
            for (var i = 0; i < resultByYear.length; i++) {
              var groupsByMonthOneYear = resultByYear[i].data.reduce(function (previousVal, currentVal) {
                var monthNum = currentVal.day.split(("-"))[1];
                if (previousVal[monthNum]) {
                  previousVal[monthNum].y += currentVal.count;
                } else {
                  var thisMonth = new Date(resultByYear[i].year, monthNum - 1);
                  previousVal[monthNum] = {y: currentVal.count, x: thisMonth};
                  hasCountMonth.push(thisMonth);
                }
                return previousVal;
              }, {});
              var resultByMonthOneYear = Object.keys(groupsByMonthOneYear).map(function (key) {
                return groupsByMonthOneYear[key];
              });
              resultByMonth = resultByMonth.concat(resultByMonthOneYear);
            }
            return this.complementData(resultByMonth, hasCountMonth);
        }
      },


      // Configure the chart: whether show the lable/grid or not in chart.
      chartConfig(chartData, displayLable, yLabel, displayGrid, groupBy) {
        return {
          type: "line",
          data: {
            datasets: [{
              lineTension: 0,
              data: chartData,
              borderColor: "#3e95cd",
              borderWidth: 0.8,
              pointRadius: 1.5
            }]
          },
          options: {
            legend: {
              display: false
            },
            scales: {
              xAxes: [{
                type: "time",
                time: {
                  unit: groupBy,
                  round: groupBy
                },
                scaleLabel: {
                  display: displayLable,
                  labelString: "Time by " + groupBy
                },
                gridLines: {
                  display: displayGrid
                }
              }],
              yAxes: [{
                scaleLabel: {
                  display: displayLable,
                  labelString: yLabel
                },
                ticks: {
                  beginAtZero: true,
                  suggestedMax: 4
                },
                gridLines: {
                  display: displayGrid
                }
              }]
            }
          }
        };
      },


      //draw tendency chart using chart.js
      drawChart(chartData, chartElementId, displayLabel, yLabel, displayGrid, groupBy) {
        if (chartData.length !== 0 && document.getElementById(chartElementId)) {
          var ctx = document.getElementById(chartElementId).getContext("2d");
          new Chart(ctx, chartUtil.chartConfig(chartData, displayLabel, yLabel, displayGrid, groupBy));
        }
      },

      // Configure the multi-line chart: whether show the lable/grid or not in chart.
      multiLineChartConfig(chartData, chartDataColors, displayLable, yLabel, displayGrid, groupBy) {
        var datasets = [];
        for (var i = 0; i < chartData.length; i ++) {
          datasets.push({
            lineTension: 0,
            data: chartData[i],
            borderColor: chartDataColors[i],
            borderWidth: 0.8,
            pointRadius: 1.5
          })
        }
        return {
          type: "line",
          data: {
            datasets: datasets
          },
          options: {
            legend: {
              display: false
            },
            scales: {
              xAxes: [{
                type: "time",
                time: {
                  unit: groupBy,
                  round: groupBy
                },
                scaleLabel: {
                  display: displayLable,
                  labelString: "Time by " + groupBy
                },
                gridLines: {
                  display: displayGrid
                }
              }],
              yAxes: [{
                scaleLabel: {
                  display: displayLable,
                  labelString: yLabel
                },
                ticks: {
                  beginAtZero: true,
                  suggestedMax: 4
                },
                gridLines: {
                  display: displayGrid
                }
              }]
            }
          }
        };
      },

      // draw multi-line tendency chart using chart.js
      drawMultiLineChart(chartData, chartDataColors, chartElementId, displayLabel, yLabel, displayGrid, groupBy) {
        if (chartData.length !== 0 && document.getElementById(chartElementId)) {
          var ctx = document.getElementById(chartElementId).getContext("2d");
          new Chart(ctx, chartUtil.multiLineChartConfig(chartData, chartDataColors, displayLabel, yLabel, displayGrid, groupBy));
        }
      }
    };

    return chartUtil;


  });
