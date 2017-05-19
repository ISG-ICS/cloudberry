angular.module('cloudberry.util', ['cloudberry.common'])
  .controller('SearchCtrl', function($scope, $window, cloudberry) {
    $scope.search = function() {
      if ($scope.keyword && $scope.keyword.trim().length > 0) {
        cloudberry.parameters.keywords = $scope.keyword.trim().split(/\s+/);
        // skip the empty query for now.
        cloudberry.queryType = 'search';
        cloudberry.query(cloudberry.parameters, cloudberry.queryType);
      } else {
        cloudberry.parameters.keywords = [];
      }
    };
  })
  .directive('searchBar', function (cloudberryConfig) {
    if(cloudberryConfig.removeSearchBar)
      return{
      };
    else {
      return {
        restrict: "E",
        controller: 'SearchCtrl',
        template: [
          '<form class="form-inline" id="input-form" ng-submit="search()" >',
          '<div class="input-group col-lg-12">',
          '<label class="sr-only">Keywords</label>',
          '<input type="text" style="width: 97%" class="form-control " id="keyword-textbox" placeholder="Search keywords, e.g. zika" ng-model="keyword" required/>',
          '<span class="input-group-btn">',
          '<button type="submit" class="btn btn-primary" id="submit-button">Submit</button>',
          '</span>',
          '</div>',
          '</form>'
        ].join('')
      };
    }
  })
  .controller('ExceptionCtrl', function($scope, $window, cloudberry) {
    $scope.$watch(
      function() {
        return cloudberry.errorMessage;
      },
  
      function(newMsg) {
        if (newMsg) $window.alert(newMsg);
        cloudberry.errorMessage = null;
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
  .controller('D3Ctrl', function($scope, $http, $timeout, cloudberry) {

  })
  .controller('PredefinedKeywordsCtrl', function ($scope, cloudberry, cloudberryConfig) {
    $scope.predefinedKeywords = cloudberryConfig.predefinedKeywords;
    $scope.predefinedSearch = function (keyword) {
      $scope.keyword = keyword;
      if ($scope.keyword && $scope.keyword.trim().length > 0) {
        cloudberry.parameters.keywords = $scope.keyword.trim().split(/\s+/);
        // skip the empty query for now.
        cloudberry.queryType = 'search';
        cloudberry.query(cloudberry.parameters, cloudberry.queryType);
      } else {
        cloudberry.parameters.keywords = [];
      }
    }
  })
  .directive('predefinedKeywords', function (cloudberryConfig) {
    if(cloudberryConfig.removeSearchBar) {
      return {
        restrict: 'E',
        controller: 'PredefinedKeywordsCtrl',
        template: [
          '<div class="btn-group btn-group-justified" role="group" aria-label="predefined-keywords-list">',
            '<div ng-repeat="keyword in predefinedKeywords" class="btn-group" role="group">',
              '<button type="button" class="btn btn-default predefined-keywords" ng-click="predefinedSearch(keyword)">{{ keyword }}</button>',
            '</div>',
          '</div>'
        ].join('')
      };
    }
    else{
      return{
      };
    }
  });
