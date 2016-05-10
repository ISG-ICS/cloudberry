angular.module('cloudberry.util', ['cloudberry.common'])
  .controller('SearchCtrl', function($scope, $window, Asterix) {
    $scope.search = function() {
      if ($scope.keyword)
        Asterix.parameters.keyword = $scope.keyword;
      Asterix.queryType = 'search';
      Asterix.query(Asterix.parameters, Asterix.queryType);
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
            '<input type="text" style="width: 97%" class="form-control " id="keyword-textbox" placeholder="Keywords" ng-model="keyword"/>',
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
