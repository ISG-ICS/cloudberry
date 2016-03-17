var app = angular.module('cloudberry', []);

app.factory('Asterix', function ($http, $timeout) {
    var ws = new WebSocket("ws://localhost:9000/ws");
    var asterixService = {
        tweets: [],
        query: function (query) {
            ws.send(JSON.stringify({query: query}));
        }
    };

    ws.onmessage = function (event) {
        $timeout(function () {
            asterixService.tweets = JSON.parse(event.data);
        });
    };

    return asterixService;
});

app.controller('SearchCtrl', function ($scope, $http, $timeout, Asterix) {
    $scope.search = function () {
        Asterix.query($scope.query);
    };
});

app.controller('TweetsCtrl', function ($scope, $http, $timeout, Asterix) {
    $scope.tweets = [];

    $scope.$watch(
        function () {
            return Asterix.tweets;
        },

        function (tweets) {
            $scope.tweets = tweets;
        }
    );
});

