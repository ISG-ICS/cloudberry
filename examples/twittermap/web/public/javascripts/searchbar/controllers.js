angular.module('cloudberry.util', ['cloudberry.common'])
  .controller('SearchCtrl', function($scope, $window, $location, cloudberry, cloudberryClient, cloudberryConfig, moduleManager) {
    var stopwordsMap = buildStopwordsMap();
    //When user input keyword we will first send query to and get result to render auto-complete menu
    //The reason we do not use keydown, we wanna send query after user finish the input rather than start the input
    $("#keyword-textbox").autocomplete({source:[],disabled:true,delay:200});

    var ACSocket;
    cloudberryClient.newWebSocket("ws://"+window.location.host+"/autoComplete").done(function(pws) {
      ACSocket = pws;

      ACSocket.onmessage = function(event){
        var suggestion = [];
        var data = JSON.parse(event.data);
        var topics = JSON.parse(data).topics;
        for (var i = 0; i < topics.length; i ++) {
          var value = String(topics[i].topic);
          //Exclude hashtag topic and repetitive topic
          if (value[0] !== "#" && !suggestion.includes(value)) {
            suggestion.push(value);
          }
        }

        $("#keyword-textbox").autocomplete({source:suggestion});
        $("#keyword-textbox").autocomplete("enable");
      };
    });

    $("#keyword-textbox").on("keyup", function(event) {
      if (event.key !== "Enter") {
        $("#keyword-textbox").autocomplete("enable");
      }
      else {
        $("#keyword-textbox").autocomplete("close");
        $("#keyword-textbox").autocomplete("disable");
      }
      var autoCompleteQuery = {
        "keyword": $scope.keyword
      };

      if (ACSocket.readyState === ACSocket.OPEN && $scope.keyword) {
        ACSocket.send(JSON.stringify(autoCompleteQuery));
      }
    });

    //If keyword been selected and user pushed enter,then we perform search directly
    $( "#keyword-textbox" ).on( "autocompleteselect", function( event, ui ){
      $scope.keyword = ui.item.value;
      $("#keyword-textbox").autocomplete("close");  
      $scope.search();
      $scope.updateSearchBox($scope.keyword);
    });

    $scope.search = function() {
      $("#keyword-textbox").autocomplete( "close" );
      if ($scope.keyword && $scope.keyword.trim().length > 0) {
        //Splits out all individual words in the query keyword.
        var keywords = $scope.keyword.trim().split(/\s+/);
        var newKeywords = new Array();

        //Adds the stopword filtering feature and checks each token.
        for(var x=0; x<keywords.length; x++){
          //If matches, remove it from the keywords
          if(!stopwordsMap.has(keywords[x].toLowerCase()))
          {
              //creates the final keyword.
              newKeywords.push(keywords[x]);
          }
        }

        if (newKeywords.length === 0) {
          //All the words are stopwords.
          cloudberry.parameters.keywords = [];
          alert("Your query only contains stopwords. Please re-enter your query.");
        }
        else {
          cloudberry.parameters.keywords = newKeywords;
          moduleManager.publishEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, {keywords: newKeywords});
        }
      }
      else {
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

    // If url indicates keyword, search it immediately.
    // e.g. url = http://localhost:9001/#?keyword=hurricane
    var defaultKeyword = $location.search().keyword;
    if (defaultKeyword) {
      var onWSReady = function(event) {
        $scope.predefinedSearch(defaultKeyword);
        moduleManager.unsubscribeEvent(moduleManager.EVENT.WS_READY, onWSReady);
      };
      moduleManager.subscribeEvent(moduleManager.EVENT.WS_READY, onWSReady);
    }
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
    // TODO - get rid of this variable watching by events subscribing and publishing
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
