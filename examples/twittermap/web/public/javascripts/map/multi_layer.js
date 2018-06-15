angular.module('cloudberry.map')
  .controller('multiLayerCtrl', function($timeout, $scope, $rootScope, $window, $http, $compile,cloudberryConfig,cloudberry,leafletData,Cache,multilayerService) {
    
    $scope.layer = {};
    cloudberry.parameters.layers = {};
    
    function drawTweetsLayer(result)
    {
        
        
       var tweets_layer = L.TileLayer.maskCanvas({
       radius: 10,  // radius in pixels or in meters (see useAbsoluteRadius)
       useAbsoluteRadius: true,  // true: r in meters, false: r in pixels
       color: '#000',  // the color of the layer
       opacity: 0.5,  // opacity of the not covered area
       noMask: true,  // true results in normal (filled) circled, instead masked circles
       lineColor: 'rgb(56,175,243)'   // color of the circle outline if noMask is true
       });
        
        
        var obj= JSON.stringify({
        "dataset": "twitter.ds_tweet",
        "filter": [
          {
            "field": "text",
            "relation": "contains",
            "values": [ "shit"]
          }
          ],
        "select" : {
        "order" : [ "-create_at"],
        
        "offset" : 0,
        "limit":100000,
        "field": ["create_at", "coordinate","id"]
        }
        });
        
        var ws = new WebSocket("ws://localhost:9000/ws");
        console.log(ws.readyState);
        // Connection opened
        ws.addEventListener('open', function (event) {
            console.log("1111");
            ws.send(obj);
        });
        
        
        
        ws.addEventListener('message', function (event) {
            console.log("2222");

            console.log(typeof(event.data));

            var result = event.data.slice(1,event.data.length-1);


            var big = $.parseJSON(result);

            var tweets = [];

            console.log("Raw data length");
            console.log(big.length);
           

            for(let i=0;i<big.length;i++)
            {
                if(big[i].hasOwnProperty('coordinate'))
                {    var coor  = big[i].coordinate;
                     var lat = [coor[1],coor[0]];
                    tweets.push(lat);
                }
            }

            console.log("Twitter data length");
            console.log(tweets.length);
            tweets_layer.setData(tweets);

        });
        
        $scope.layer["tweets"].data = tweet_layer;
        return tweets_layer;
        
    }
    
    function clearTweetsLayer(){
        
        $scope.layer["tweets"].data.setData([]);
        
    }
    
    
    

    
    // initialize
    
    $rootScope.$on('multiLayer', function (event, data) {
        
            
            
        
            var layer_name = cloudberry.parameters.maptype;
            
            //if (cloudberry.
            
        
            if ($scope.layer[layer_name].active == 0){
                //$scope.layer[layer_name].init($scope);
                $scope.layer[layer_name].active = 1;
                $scope.map.addLayer($scope.layer[layer_name].layer);
            }
            
            
            for (var key in $scope.layer) {
                if(key!=layer_name && $scope.layer[key]){
                    if(key != "polygon"){
                        $scope.map.removeLayer($scope.layer[key].layer);
                        //$scope.layer[key].clear();
                        $scope.layer[key].active = 0;
                    }
                }
            }    
        
            
    })
    
    multilayerService.createPolygonLayer().then(function(polygonLayer){
        $scope.layer["polygon"] = polygonLayer;
        $scope.layer["polygon"].init().then(function(){
            $scope.layer["polygon"].active = 1;
            $scope.map.addLayer($scope.layer["polygon"].layer);
            for (var key in polygonLayer.watchVariables){
                $scope.watchVariables[key] = polygonLayer.watchVariables[key];
            }
        });
    });
    
    multilayerService.createCountmapLayer().then(function(countmapLayer){
        $scope.layer["countmap"] = countmapLayer;
        $scope.layer["countmap"].init($scope).then(function(){
            $scope.layer["countmap"].active = 1;
            $scope.map.addLayer($scope.layer["countmap"].layer);
            for (var key in countmapLayer.watchVariables){
                $scope.watchVariables[key] = countmapLayer.watchVariables[key];
            }
        });
    });
    
    multilayerService.createHeatmapLayer().then(function(heatmapLayer){
        $scope.layer["heatmap"] = heatmapLayer;
        $scope.layer["heatmap"].init($scope).then(function(){
            $scope.layer["heatmap"].active = 0;
            for (var key in heatmapLayer.watchVariables){
                $scope.watchVariables[key] = heatmapLayer.watchVariables[key];
            }
        });
    });
    
    var parameters = {
        id: "pinmap",
        dataset: "twitter.ds_tweet",
        pinStyle: {
            opacity: 0.8,
            radius: 1.2,//80,
            useAbsoluteRadius: false,//true,
            color: "#00aced",//"#0084b4"
            noMask: true,
            lineColor: "#00aced"//"#00aced"
        },
        highlightPinStyle: {
            radius : 6,
            color : "#0d3e99",
            weight : 3,
            fillColor : "#b8e3ff",
            fillOpacity : 1.0
        }
    }
    
    multilayerService.createPinmapLayer(parameters).then(function(pinmapLayer){
        $scope.layer["pinmap"] = pinmapLayer;
        $scope.layer["pinmap"].init($scope).then(function(){
            $scope.layer["pinmap"].active = 0;
            for (var key in pinmapLayer.watchVariables){
                $scope.watchVariables[key] = pinmapLayer.watchVariables[key];
            }
            cloudberry.parameters.layers["pinmap"] = pinmapLayer;
            cloudberry.query(cloudberry.parameters);
        });
    });
    
    /*
    $rootScope.$on('registerLayer', function (event, data) {
        var layer_name = data[0];
        var layer = data[1];
        $scope.layer[layer_name] = layer;
        for (var key in layer.watchVariables){
            $scope.watchVariables[key] = layer.watchVariables[key];
        }
    })
    */
    
    var count = 0;
    
    if($scope.watchVariables){}
    else{
        console.log("create variable in ml module");
        $scope.watchVariables = {
            'pinmapMapResult': "cloudberry.pinmapMapResult",
            'countmapMapResult': "cloudberry.countmapMapResult",
            'doNormalization': "$('#toggle-normalize').prop('checked')",
            'doSentiment': "$('#toggle-sentiment').prop('checked')"
          
        }
    }
    
    $scope.$on("leafletDirectiveMap.zoomend", function() {
        for (var key in $scope.layer) {
            if ($scope.layer[key].active && typeof $scope.layer[key].zoom === "function"){
                $scope.layer[key].zoom();
            }
        }
    });
    
    $scope.$on("leafletDirectiveMap.dragend", function() {
        for (var key in $scope.layer) {
            if ($scope.layer[key].active && typeof $scope.layer[key].drag === "function"){
                $scope.layer[key].drag();
            }
        }
    });
    
    $scope.$watchCollection(
      function() {
          
        var obj = {}
        
        for (var key in $scope.watchVariables)
        {
            obj[key] = eval($scope.watchVariables[key]);
            
        }
          
        return obj;
      },

      function(newResult, oldValue) {
          
        
        
        var layer_name = cloudberry.parameters.maptype;  
        var result_name = layer_name + "MapResult";  
        
          if (newResult[result_name] !== oldValue[result_name]) {
            $scope.result = newResult[result_name];
            if (Object.keys($scope.result).length !== 0) {
              $scope.status.init = false;
              $scope.layer[layer_name].draw($scope.result);
            } else {
              $scope.layer[layer_name].draw($scope.result);
            }
          }
          
          /*
          if(layer_name === "countmap" && count<5 )
          {
            count++;
            
            if(newResult['doNormalization'] !== oldValue['doNormalization']) {
              $scope.doNormalization = newResult['doNormalization'];
              $scope.layer["countmap"].draw($scope.result);
            }
            if(newResult['doSentiment'] !== oldValue['doSentiment']) {
              $scope.doSentiment = newResult['doSentiment'];
              if($scope.doSentiment) {
                $scope.infoPromp = "Score";  // change the info promp
              } else {
               $scope.infoPromp = config.mapLegend;
             }
              $scope.layer["countmap"].draw($scope.result);
            }
              
              
          }
          */

        
      }
    );
    
    
    
    
  });
