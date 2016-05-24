/**
 * Created by Pro on 5/9/16.
 */
angular.module('dashboard', ['cloudberry.dashboard'])
  // .config(function(dashboardProvider){
  //
  //   // dashboardProvider
  //   //   .structure('6-6', {
  //   //     rows: [{
  //   //       columns: [{
  //   //         styleClass: 'col-md-6'
  //   //       }, {
  //   //         styleClass: 'col-md-6'
  //   //       }]
  //   //     }]
  //   //   });
  // })
  .controller('dashboardCtrl', function ($scope, $window) {
    var data = [
      {name: "President Candidate 1", party: "R", date: "12/27/2012", state: 1, count: 10},
      {name: "President Candidate 2", party: "D", date: "12/27/2012", state: 1, count: 20},
      {name: "President Candidate 3", party: "D", date: "12/27/2012", state: 1, count: 7},
      {name: "President Candidate 1", party: "R", date: "12/29/2012", state: 2, count: 8},
      {name: "President Candidate 2", party: "D", date: "12/29/2012", state: 2, count: 5},
      {name: "President Candidate 3", party: "D", date: "12/29/2012", state: 2, count: 31},
      {name: "Senator 1", party: "D", date: "12/27/2012", state: 1, count: 12},
      {name: "Senator 2", party: "R", date: "12/27/2012", state: 1, count: 22},
      {name: "Senator 3", party: "R", date: "12/27/2012", state: 1, count: 37},
      {name: "Senator 4", party: "D", date: "12/27/2012", state: 1, count: 24},
      {name: "Senator 5", party: "D", date: "12/27/2012", state: 1, count: 8},
      {name: "Senator 6", party: "R", date: "12/27/2012", state: 1, count: 19},
      {name: "Senator 7", party: "D", date: "12/27/2012", state: 1, count: 31},
      {name: "Senator 8", party: "R", date: "12/27/2012", state: 1, count: 24},
      {name: "Senator 9", party: "R", date: "12/27/2012", state: 1, count: 7},
      {name: "Senator 10", party: "D", date: "12/27/2012", state: 1, count: 4},
      {name: "Senator 1", party: "D", date: "12/29/2012", state: 2, count: 11},
      {name: "Senator 2", party: "R", date: "12/29/2012", state: 2, count: 17},
      {name: "Senator 3", party: "R", date: "12/29/2012", state: 2, count: 9},
      {name: "Senator 4", party: "D", date: "12/29/2012", state: 2, count: 10},
      {name: "Senator 5", party: "D", date: "12/29/2012", state: 2, count: 25},
      {name: "Senator 6", party: "R", date: "12/29/2012", state: 2, count: 28},
      {name: "Senator 7", party: "D", date: "12/29/2012", state: 2, count: 31},
      {name: "Senator 8", party: "R", date: "12/29/2012", state: 2, count: 28},
      {name: "Senator 9", party: "R", date: "12/29/2012", state: 2, count: 17},
      {name: "Senator 10", party: "D", date: "12/29/2012", state: 2, count: 4}
    ];

    $scope.globalConf = {
      rowHeight: 200,
      grids: [4,4,4]
    };

    var ndx = crossfilter(data);
    var parseDate = d3.time.format("%m/%d/%Y").parse;
    data.forEach(function(d) {
      d.date = parseDate(d.date);
      d.total= d.http_404+d.http_200+d.http_302;
      d.Year=d.date.getFullYear();
    });
    var dateDim = ndx.dimension(function(d) {return d.date;});
    var hits = dateDim.group().reduceSum(function(d) {return d.total;});
    var minDate = dateDim.bottom(1)[0].date;
    var maxDate = dateDim.top(1)[0].date;

    $scope.linechartConf = {
      data: data,
      margin:
      {
        top: 10,
        right: 10,
        bottom: 30,
        left: 30
      },
      height: $scope.globalConf.rowHeight,
      grid: $scope.globalConf.grids[0],
      dimension: dateDim,
      group: hits,
      scale: d3.time.scale().domain([minDate,maxDate]),
      renderArea: true
    };

    var yearDim  = ndx.dimension(function(d) {return +d.Year;});
    var year_total = yearDim.group().reduceSum(function(d) {return d.http_200+d.http_302;});

    $scope.piechartConf = {
      data: data,
      height: $scope.globalConf.rowHeight,
      grid: $scope.globalConf.grids[1],
      dimension: yearDim,
      group: year_total,
      innerRadius: 30
    };
  })
  ;