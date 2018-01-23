angular.module('cloudberry.util', ['cloudberry.common'])
  .controller('SearchCtrl', function($scope, $window, cloudberry, cloudberryConfig) {
    $scope.search = function() {
      if ($scope.keyword && $scope.keyword.trim().length > 0) {

          //stopword filtering feature
          String.prototype.removeStopWords = function() {
              var x;
              var word;
              var cleansed_string = this.valueOf();
              var Hstop_words = external_stop_words();

              //Split out all the individual words in the phrase
              var words = cleansed_string.split(" ");

              //Review each token
              for(x=0; x<words.length; x++){
                  word = words[x];

                  //If matches, remove it from the keywords
                  if(Hstop_words.has(word.toLowerCase()))
                  {
                      // Remove the word from the keywords
                      cleansed_string = cleansed_string.replace(word, "");
                  }
              }
              return cleansed_string.replace(/^\s+|\s+$/g, "");
          };

          var rawData = $scope.keyword;
          var newData = rawData.removeStopWords();

          if (newData.valueOf() === "" || null) {

              alert("Your query contains stopwords. Please re-enter your query.");
          }
          else {
              cloudberry.parameters.keywords = newData;
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
