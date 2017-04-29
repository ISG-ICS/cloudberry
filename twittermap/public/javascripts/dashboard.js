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
      {name: "President Candidate 1", type: "P", party: "R", date: "12/27/2012", state: 1, count: 10},
      {name: "President Candidate 2", type: "P", party: "D", date: "12/27/2012", state: 3, count: 20},
      {name: "President Candidate 3", type: "P", party: "D", date: "12/27/2012", state: 11, count: 7},
      {name: "President Candidate 1", type: "P", party: "R", date: "12/28/2012", state: 21, count: 1},
      {name: "President Candidate 2", type: "P", party: "D", date: "12/28/2012", state: 19, count: 2},
      {name: "President Candidate 3", type: "P", party: "D", date: "12/28/2012", state: 7, count: 3},
      {name: "President Candidate 1", type: "P", party: "R", date: "12/29/2012", state: 8, count: 8},
      {name: "President Candidate 2", type: "P", party: "D", date: "12/29/2012", state: 18, count: 5},
      {name: "President Candidate 3", type: "P", party: "D", date: "12/29/2012", state: 32, count: 31},
      {name: "Senator 1", type: "S", party: "D", date: "12/27/2012", state: 29, count: 12},
      {name: "Senator 2", type: "S", party: "R", date: "12/27/2012", state: 13, count: 22},
      {name: "Senator 3", type: "S", party: "R", date: "12/27/2012", state: 12, count: 37},
      {name: "Senator 4", type: "S", party: "D", date: "12/27/2012", state: 27, count: 24},
      {name: "Senator 5", type: "S", party: "D", date: "12/27/2012", state: 22, count: 8},
      {name: "Senator 6", type: "S", party: "R", date: "12/27/2012", state: 4, count: 19},
      {name: "Senator 7", type: "S", party: "D", date: "12/27/2012", state: 2, count: 31},
      {name: "Senator 8", type: "S", party: "R", date: "12/27/2012", state: 9, count: 24},
      {name: "Senator 9", type: "S", party: "R", date: "12/27/2012", state: 15, count: 7},
      {name: "Senator 10", type: "S", party: "D", date: "12/27/2012", state: 17, count: 4},
      {name: "Senator 1", type: "S", party: "D", date: "12/28/2012", state: 33, count: 2},
      {name: "Senator 2", type: "S", party: "R", date: "12/28/2012", state: 24, count: 2},
      {name: "Senator 3", type: "S", party: "R", date: "12/28/2012", state: 22, count: 7},
      {name: "Senator 4", type: "S", party: "D", date: "12/28/2012", state: 11, count: 4},
      {name: "Senator 5", type: "S", party: "D", date: "12/28/2012", state: 4, count: 1},
      {name: "Senator 6", type: "S", party: "R", date: "12/28/2012", state: 2, count: 9},
      {name: "Senator 7", type: "S", party: "D", date: "12/28/2012", state: 1, count: 1},
      {name: "Senator 8", type: "S", party: "R", date: "12/28/2012", state: 40, count: 4},
      {name: "Senator 9", type: "S", party: "R", date: "12/28/2012", state: 19, count: 2},
      {name: "Senator 10", type: "S", party: "D", date: "12/28/2012", state: 18, count: 3},
      {name: "Senator 1", type: "S", party: "D", date: "12/29/2012", state: 7, count: 11},
      {name: "Senator 2", type: "S", party: "R", date: "12/29/2012", state: 25, count: 17},
      {name: "Senator 3", type: "S", party: "R", date: "12/29/2012", state: 25, count: 9},
      {name: "Senator 4", type: "S", party: "D", date: "12/29/2012", state: 14, count: 10},
      {name: "Senator 5", type: "S", party: "D", date: "12/29/2012", state: 19, count: 25},
      {name: "Senator 6", type: "S", party: "R", date: "12/29/2012", state: 31, count: 28},
      {name: "Senator 7", type: "S", party: "D", date: "12/29/2012", state: 34, count: 31},
      {name: "Senator 8", type: "S", party: "R", date: "12/29/2012", state: 1, count: 28},
      {name: "Senator 9", type: "S", party: "R", date: "12/29/2012", state: 2, count: 17},
      {name: "Senator 10", type: "S", party: "D", date: "12/29/2012", state: 2, count: 4}
    ];

    $scope.globalConf = {
      rowHeight: [350,200],
      grids: [[2,2,8],[6,6]]
    };

    var ndx = crossfilter(data);
    var parseDate = d3.time.format("%m/%d/%Y").parse;
    data.forEach(function(d) {
      d.date = parseDate(d.date);
    });
    var dateDim = ndx.dimension(function(d) {return d.date;});
    var dateTotal = dateDim.group().reduceSum(function(d) {return d.count;});
    var minDate = dateDim.bottom(1)[0].date;
    var maxDate = dateDim.top(1)[0].date;

    $scope.linechartConf = {
      data: data,
      margin:
      {
        top: 10,
        right: 25,
        bottom: 30,
        left: 10
      },
      height: $scope.globalConf.rowHeight[1],
      grid: $scope.globalConf.grids[1][1],
      dimension: dateDim,
      group: dateTotal,
      scale: d3.time.scale().domain([minDate,maxDate]),
      renderArea: true
    };

    var partyDim  = ndx.dimension(function(d) {return d.party;});
    var partyTotal = partyDim.group().reduceSum(function(d) {return d.count;});

    $scope.piechartConf = {
      data: data,
      height: $scope.globalConf.rowHeight[1],
      margin: 30,
      grid: $scope.globalConf.grids[0][1],
      dimension: partyDim,
      group: partyTotal,
      innerRadius: 30
    };

    var nameDim  = ndx.dimension(function(d) {return d.name;});
    var nameTotal = nameDim.group().reduceSum(function(d) {return d.count;});
    $scope.rowchartConf = {
      data: data,
      height: $scope.globalConf.rowHeight[0],
      grid: $scope.globalConf.grids[1][0],
      dimension: nameDim,
      group: nameTotal
    };

    var geoDim = ndx.dimension(function (d) { return d.state });
    var geoTotal = geoDim.group().reduceSum(function (d) { return d.count; });

    $scope.mapConf = {
      data: data,
      height: $scope.globalConf.rowHeight[0],
      grid: $scope.globalConf.grids[1][0],
      dimension: geoDim,
      group: geoTotal
    };

  })
  ;