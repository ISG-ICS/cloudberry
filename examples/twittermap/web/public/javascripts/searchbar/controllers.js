angular.module('cloudberry.util', ['cloudberry.common'])
  .controller('SearchCtrl', function($scope, $window, cloudberry, cloudberryConfig) {
    $scope.search = function() {
      if ($scope.keyword && $scope.keyword.trim().length > 0) {
        cloudberry.parameters.keywords = $scope.keyword.trim().split(/\s+/);
        // skip the empty query for now.

          //stopword filtering feature
          String.prototype.removeStopWords = function() {
              var x;
              var y;
              var word;
              var stop_word;
              var regex_str;
              var regex;
              var cleansed_string = this.valueOf();
              var stop_words = external_stop_words();

              // Split out all the individual words in the phrase
              var words = cleansed_string.match(/[^\s]+|\s+[^\s+]$/g);

              // Review all the words
              for(x=0; x < words.length; x++) {
                  // For each word, check all the stop words
                  for(y=0; y < stop_words.length; y++) {
                      // Get the current word
                      word = words[x].replace(/\s+|[^a-z]+/ig, "");   // Trim the word and remove non-alpha

                      // Get the stop word
                      stop_word = stop_words[y];

                      // If the word matches the stop word, remove it from the keywords
                      if(word.toLowerCase() === stop_word) {
                          // Build the regex
                          regex_str = "^\\s*"+stop_word+"\\s*$";      // Only word
                          regex_str += "|^\\s*"+stop_word+"\\s+";     // First word
                          regex_str += "|\\s+"+stop_word+"\\s*$";     // Last word
                          regex_str += "|\\s+"+stop_word+"\\s+";      // Word somewhere in the middle
                          regex = new RegExp(regex_str, "ig");

                          // Remove the word from the keywords
                          cleansed_string = cleansed_string.replace(regex, " ");
                      }
                  }
              }
              return cleansed_string.replace(/^\s+|\s+$/g, "");
          };

          var rawData = $scope.keyword;

          var myData = rawData.removeStopWords();

          if (myData.valueOf() === "" || null) {

              alert("Invalid input! Please enter again.");

          }
          else {

              cloudberry.parameters.keywords = myData;
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
