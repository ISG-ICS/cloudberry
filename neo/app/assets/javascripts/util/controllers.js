angular.module('cloudberry.util', ['cloudberry.common'])
  .controller('SearchCtrl', function($scope, $window, Asterix) {
    $scope.keyword = "cao";
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
      scope: {
        keyword: "=",
        search: "="
      },
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
  });
