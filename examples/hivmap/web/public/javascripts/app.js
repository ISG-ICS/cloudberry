var app = angular.module('cloudberry', ['cloudberry.map','cloudberry.timeseries', 'cloudberry.sidebar',
                          'cloudberry.util','cloudberry.cache', 'cloudberry.mapresultcache']);

app.controller("AppCtrl", function ($scope, cloudberry, $rootScope) {
  //When websocket is ready, send the initial default query.
  $rootScope.$on('ws-ready', function (event, data) {
    cloudberry.parameters.maptype = 'countmap';
    cloudberry.parameters.keywords = ['hiv'];
    cloudberry.query(cloudberry.parameters);
    console.log('Initial query sent.');
  });
});
