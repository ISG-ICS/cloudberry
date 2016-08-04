angular.module('cloudberry.util', ['cloudberry.common'])
  .controller('SearchCtrl', function($scope, $window, Asterix) {
    $scope.search = function() {
      if ($scope.keyword && $scope.keyword.trim().length > 0) {
        Asterix.parameters.keywords = $scope.keyword.trim().split(/\s+/);
        Asterix.queryType = 'search';
        Asterix.query(Asterix.parameters, Asterix.queryType);
      } else {
        Asterix.parameters.keywords = [];
      }
    };
  })
  .directive('searchBar', function () {
    return {
      restrict: "E",
      controller: 'SearchCtrl',
      template: [
        '<form class="form-inline" id="input-form" ng-submit="search()" >',
          '<div class="form-group" style="width: 80%">',
            '<label class="sr-only">Keywords</label>',
            '<input type="text" style="width: 97%" class="form-control " id="keyword-textbox" placeholder="Search keywords, e.g. zika" ng-model="keyword" required/>',
          '</div>',
          '<button type="submit" class="btn btn-primary" id="submit-button">Submit</button>',
        '</form>'
      ].join('')
    };
  })
  .controller('ExceptionCtrl', function($scope, $window, Asterix) {
    $scope.$watch(
      function() {
        return Asterix.errorMessage;
      },
  
      function(newMsg) {
        if (newMsg) $window.alert(newMsg);
        Asterix.errorMessage = null;
      }
    );
  })
  .directive('exceptionBar', function () {
    return {
      restrict: "E",
      controller: 'ExceptionCtrl',
      template: [
        '<p> {{ newMsg }}</p>'
      ].join('')
    }
  })
  .controller('D3Ctrl', function($scope, $http, $timeout, Asterix) {

  });
