angular.module('cloudberry.util', ['cloudberry.common'])
  .controller('SearchCtrl', function($scope, $window, cloudberry, cloudberryConfig) {
      var stopwordsMap = buildStopwordsMap();

    $scope.search = function() {
      if ($scope.keyword && $scope.keyword.trim().length > 0) {
          //Split out all the individual words in the phrase
          cloudberry.parameters.keywords = $scope.keyword.trim().split(/\s+/);
          console.log("1.The keywords are " + cloudberry.parameters.keywords);
          var newKeywords = new Array();

          //stopword filtering feature
              //Review each token
              for(var x=0; x<cloudberry.parameters.keywords.length; x++){
                  //If matches, remove it from the keywords
                  if(!stopwordsMap.has(cloudberry.parameters.keywords[x].toLowerCase()))
                  {
                      //create the final keyword.
                      newKeywords.push(cloudberry.parameters.keywords[x]);
                  }
              }

          console.log("2.After remove: " + newKeywords);

          if (newKeywords.length === 0) {
              //all the words are stopwords.
              cloudberry.parameters.keywords = [];
              alert("Your query only contains stopwords. Please re-enter your query.");
          }
          else {
              cloudberry.parameters.keywords = newKeywords;
              cloudberry.query(cloudberry.parameters);
          }

      } else {
        cloudberry.parameters.keywords = [];
      }
    };
    $scope.predefinedKeywords = cloudberryConfig.predefinedKeywords;
    $scope.updateSearchBox = function (keyword) {
      $('.search-keyword-btn').html(keyword + ' <span class="caret"></span>');
    };
    $scope.predefinedSearch = function (keyword) {
      $scope.keyword = keyword;
      $scope.search();
      $scope.updateSearchBox(keyword);
    };
  })
  .directive('searchBar', function (cloudberryConfig) {
    if(cloudberryConfig.removeSearchBar) {
      return {
        restrict: "E",
        controller: "SearchCtrl",
        template: [
          '<div class="btn-group search-keyword-btn-group col-lg-12">',
            '<button type="button" data-toggle="dropdown" class="btn btn-primary search-keyword-btn dropdown-toggle">Keywords <span class="caret"></span></button>',
            '<ul class="dropdown-menu" aria-labelledby="dropdownMenu1">',
              '<li ng-repeat="keyword in predefinedKeywords"><a href="#" ng-click="predefinedSearch(keyword)">{{ keyword }}</a></li>',
            '</ul>',
          '</div>'
        ].join('')
      };
    } else {
      return {
        restrict: "E",
        controller: "SearchCtrl",
        template: [
          '<form class="form-inline" id="input-form" ng-submit="search()" >',
            '<div class="input-group col-lg-12">',
              '<label class="sr-only">Keywords</label>',
              '<input type="text" style="width: 97%" class="form-control " id="keyword-textbox" placeholder="Search keywords, e.g. hurricane" ng-model="keyword" required/>',
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

  });
