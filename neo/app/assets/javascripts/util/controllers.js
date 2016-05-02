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
      scope: false,
      template: [
        '<form class="form-inline" id="input-form" ng-submit="search()" >',
          '<div class="form-group" id="input-group">',
            '<label class="sr-only">Keywords</label>',
            '<input type="text" class="form-control " id="keyword-textbox" placeholder="Keywords" ng-model="keyword" required/>',
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
      directive: false,
      template: [
        '<p> {{ newMsg }}</p>'
      ].join('')
    }
  })
  .controller('D3Ctrl', function($scope, $http, $timeout, Asterix) {

  });
