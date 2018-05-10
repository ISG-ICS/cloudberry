angular.module('cloudberry.map')
  .controller('multiLayerCtrl', function($scope, $rootScope, $window, $http, $compile,cloudberryConfig,cloudberry,leafletData,Cache) {
    
        
    
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
        
        cloudberry.layer["tweets"].data = tweet_layer;
        return tweets_layer;
        
    }
    
    function clearTweetsLayer(){
        
        cloudberry.layer["tweets"].data.setData([]);
        
    }
    
    
    

    
    // initialize
    
    $rootScope.$on('multiLayer', function (event, data) {
        
            
            
        
            var layer_name = cloudberry.parameters.maptype;
            
        
            cloudberry.layer[layer_name].init();
        
             
            
            cloudberry.layer[layer_name].active = 1;    
            
            
            for (var key in cloudberry.layer) {
                if(key!=layer_name && cloudberry.layer[key]){
                    console.log(key); 
                    cloudberry.layer[key].clear();
                      
                }
            }    
        
            
    })
    
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
              cloudberry.layer[layer_name].draw($scope.result);
            } else {
              cloudberry.layer[layer_name].draw($scope.result);
            }
          }
          
          
          if(layer_name === "countmap" && count<5 )
          {
            count++;
            
            if(newResult['doNormalization'] !== oldValue['doNormalization']) {
              $scope.doNormalization = newResult['doNormalization'];
              cloudberry.layer["countmap"].draw($scope.result);
            }
            if(newResult['doSentiment'] !== oldValue['doSentiment']) {
              $scope.doSentiment = newResult['doSentiment'];
              if($scope.doSentiment) {
                $scope.infoPromp = "Score";  // change the info promp
              } else {
               $scope.infoPromp = config.mapLegend;
             }
              cloudberry.layer["countmap"].draw($scope.result);
            }
              
              
          }

        
      }
    );
    
    
    
    
  });
