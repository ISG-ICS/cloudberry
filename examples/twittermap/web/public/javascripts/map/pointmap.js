angular.module('cloudberry.map')
  .controller('pointMapCtrl', function($scope, $rootScope, $window, $http, $compile, cloudberry, leafletData, cloudberryConfig, Cache) {
    // set map styles for pointmap
    function setPointMapStyle() {
      $scope.setStyles({
        initStyle: {
          weight: 0.5,
          fillOpacity: 0,
          color: 'white'
        },
        stateStyle: {
          fillColor: '#f7f7f7',
          weight: 0.5,
          opacity: 1,
          color: '#92d1e1',
          fillOpacity: 0
        },
        stateUpperStyle: {
          fillColor: '#f7f7f7',
          weight: 0.5,
          opacity: 1,
          color: '#92d1e1',
          fillOpacity: 0
        },
        countyStyle: {
          fillColor: '#f7f7f7',
          weight: 0.5,
          opacity: 1,
          color: '#92d1e1',
          fillOpacity: 0
        },
        countyUpperStyle: {
          fillColor: '#f7f7f7',
          weight: 0.5,
          opacity: 1,
          color: '#92d1e1',
          fillOpacity: 0
        },
        cityStyle: {
          fillColor: '#f7f7f7',
          weight: 0.5,
          opacity: 1,
          color: '#92d1e1',
          fillOpacity: 0
        },
        hoverStyle: {
          weight: 0.7,
          color: '#666',
          fillOpacity: 0
        },
        colors: [ '#ffffff', '#92d1e1', '#4393c3', '#2166ac', '#f4a582', '#d6604d', '#b2182b'],
        sentimentColors: ['#ff0000', '#C0C0C0', '#00ff00']
      });
    }
    
    // clear pointmap specific data
    function cleanPointMap() {
      $scope.points = [];
      $scope.pointIDs = [];
      if($scope.pointsLayer != null) {
        $scope.map.removeLayer($scope.pointsLayer);
        $scope.pointsLayer = null;
      }
      if ($scope.currentMarker != null) {
        $scope.map.removeLayer($scope.currentMarker);
        $scope.currentMarker = null;
      }
    }
    
    // additional operations required by pinmap for zoom event
    // update the map boundary and x/y axis scale
    function zoomPostProcess() {
      //For rescaling the metric of distance between points and mouse cursor.
      $scope.currentBounds = $scope.map.getBounds();
      $scope.scale_x = Math.abs($scope.currentBounds.getEast() - $scope.currentBounds.getWest());
      $scope.scale_y = Math.abs($scope.currentBounds.getNorth() - $scope.currentBounds.getSouth());
    }

    // initialize pointmap
    function setInfoControlPointMap() {

      // add feature to each polygon
      // when a user click on a polygon, the map will zoom in to fit that polygon in the view
      function onEachFeature(feature, layer) {
        layer.on({
          click: $scope.zoomToFeature
        });
      }

      $scope.loadGeoJsonFiles(onEachFeature);
      
      $scope.resetZoomFunction(onEachFeature, zoomPostProcess);
      $scope.resetDragFunction(onEachFeature);

      $scope.mouseOverPointI = 0;
    }
    
    // function for drawing pointmap
    function drawPointMap(result) {

      if ($scope.currentMarker != null) {
        $scope.map.removeLayer($scope.currentMarker);
      }

      //For randomize coordinates by bounding_box
      //TODO Should be reused by HeatMap in HeatMap PR.
      var gseed;

      function CustomRandom() {
        var x = Math.sin(gseed++) * 10000;
        return x - Math.floor(x);
      }

      function randomNorm(mean, stdev) {
        return mean + (((CustomRandom() + CustomRandom() + CustomRandom() + CustomRandom() + CustomRandom() + CustomRandom()) - 3) / 3) * stdev;
      }

      function rangeRandom(seed, minV, maxV){
        gseed = seed;
        var ret = randomNorm((minV + maxV) / 2, (maxV - minV) / 16);
        return ret;
      }

      //To initialize the points layer
      if (!$scope.pointsLayer) {
       
        $scope.pointsLayer = new L.TileLayer.MaskCanvas({
          opacity: 0.8,
          radius: 2,//80,
          useAbsoluteRadius: false,//true,
          color: '#17c6fc',//'#0084b4'
          noMask: true,
          lineColor: '#0d3e99'//'#00aced'
        });

        $scope.map.addLayer($scope.pointsLayer);

        //Create a new event called 'mouseintent' by listening to 'mousemove'.
        $scope.map.on('mousemove', onMapMouseMove);
        var timer = null;
        //If user hang the mouse cursor for 300ms, fire a 'mouseintent' event.
        function onMapMouseMove(e) {
          var duration = 300;
          if (timer !== null) {
            clearTimeout(timer);
            timer = null;
          }
          timer = setTimeout(L.Util.bind(function() {
            this.fire('mouseintent', {
              latlng : e.latlng,
              layer : e.layer
            });
            timer = null;
          }, this), duration);
        }

        $scope.currentBounds = null;
        $scope.scale_x = 0;
        $scope.scale_y = 0;

        //shows each point's info in Front-end.
        function translateTweetdatatoShow(tweetJSON) {
            var userName = "";
            try {
                userName = tweetJSON['user.name'];
            }
            catch (e){
                console.log("author_name missing in this Tweet. :" + e.message);
            }

            var userPhotoUrl = "";
            try {
                userPhotoUrl= tweetJSON['user.profile_image_url'];
                console.log("userPhotoUrl = " + userPhotoUrl);
            }
            catch (e){
                console.log("user.profile_image_url missing in this Tweet.:");
            }

            var tweetText = "";
            try {
                tweetText = tweetJSON.text;
            }
            catch (e){
                console.log("Text missing in this Tweet. :" + e.message);
            }
            if (tweetText === "" || null || undefined)
            {
                tweetText = "Fail to get Tweets data."
            }

            var tweetTime = "";
            try {
                var created_at = new Date(tweetJSON['create_at']);
                tweetTime = created_at.toTimeString() + "\\t" + created_at.toDateString();
                console.log("tweetTime = " + tweetTime);

            }
            catch (e){
                console.log("Time missing in this Tweet. :" + e.message);
            }

            var tweetTemplate = "\n"
                + "<div class=\"tweet\">\n "
                + "  <div class=\"tweet-body\">"
                + "    <div class=\"user-info\"> "
                + "      <span class=\"name\"> "
                + userName
                + "      </span> "
                + "      <img src=\""
                + userPhotoUrl
                + "\" style=\"width: 32px; display: inline;\">\n"
                + "    </div>\n	"
                + "    <span class=\"tweet-time\">"
                + tweetTime
                + "    </span>\n	 "
                + "    <span class=\"tweet-text\">"
                + tweetText
                + "    </span>\n	 "
                + "  </div>\n	"
                + "</div>\n";

            return tweetTemplate;
        }

        $scope.map.on('mouseintent', onMapMouseIntent);

        $scope.currentMarker = null;
        $scope.points = [];
        $scope.pointIDs = [];

        function onMapMouseIntent(e) {
          //make sure the scale metrics are updated
          if ($scope.currentBounds == null || $scope.scale_x == 0 || $scope.scale_y == 0) {
            $scope.currentBounds = $scope.map.getBounds();
            $scope.scale_x = Math.abs($scope.currentBounds.getEast()
              - $scope.currentBounds.getWest());
            $scope.scale_y = Math.abs($scope.currentBounds.getNorth()
              - $scope.currentBounds.getSouth());
          }

          var i = isMouseOverAPoint(e.latlng.lat, e.latlng.lng);

          //if mouse over a new point, show the Popup Tweet!
          if (i >= 0 && $scope.mouseOverPointI != i) {
            $scope.mouseOverPointI = i;
            //(1) If previous Marker is not null, destroy it.
            if ($scope.currentMarker != null) {
              $scope.map.removeLayer($scope.currentMarker);
            }
            //(2) Create a new Marker to highlight the point.
            $scope.currentMarker = L.circleMarker(e.latlng, {
              radius : 6,
              color : '#0d3e99',
              weight : 1.5,
              fillColor : '#b8e3ff',
              fillOpacity : 1.0
            }).addTo($scope.map);

            //send the query to cloudberry.
            var passID = "" + $scope.pointIDs[i];
            cloudberry.querypin(passID);

            //receives the result.
            $rootScope.$on("TweetData",function (event, args){
                //cloudberryConfig.tweetDBResult is a json object
                var tweetContent = translateTweetdatatoShow(cloudberryConfig.tweetDBresult);
                $scope.popUpTweet = L.popup({maxWidth:300, minWidth:300, maxHight:300});
                $scope.popUpTweet.setContent(tweetContent);
                $scope.currentMarker.bindPopup($scope.popUpTweet).openPopup();
            });
          }
        }

        function isMouseOverAPoint(x, y) {
          for (var i = 0; i < $scope.points.length; i += 1) {
            var dist_x = Math.abs(($scope.points[i][0] - x) / $scope.scale_x);
            var dist_y = Math.abs(($scope.points[i][1] - y) / $scope.scale_y);
            if (dist_x <= 0.01 && dist_y <= 0.01) {
              return i;
            }
          }
          return -1;
        }
      }

      //Update the points data
      if (result.length > 0){
        $scope.points = [];
        $scope.pointIDs = [];
        for (var i = 0; i < result.length; i++) {
          if (result[i].hasOwnProperty('coordinate')){
            $scope.points.push([result[i].coordinate[1], result[i].coordinate[0]]);
          }
          else if (result[i].hasOwnProperty('place.bounding_box')){
            $scope.points.push([rangeRandom(result[i].id, result[i]["place.bounding_box"][0][1], result[i]["place.bounding_box"][1][1]), rangeRandom(result[i].id + 79, result[i]["place.bounding_box"][0][0], result[i]["place.bounding_box"][1][0])]);
          }
          $scope.pointIDs.push(result[i].id);
        }
        $scope.pointsLayer.setData($scope.points);
      }
      else {
        $scope.points = [];
        $scope.pointIDs = [];
        $scope.pointsLayer.setData($scope.points);
      }
    }
    
    // initialize if the default map type is pointmap
    if (cloudberry.parameters.maptype == 'pointmap'){
      setPointMapStyle();
      $scope.resetPolygonLayers();
      setInfoControlPointMap();
    }
    
    // map type change handler
    // initialize the map (styles, zoom/drag handler, etc) when switch to this map
    // clear the map when switch to other map
    $rootScope.$on('maptypeChange', function (event, data) {
      if (cloudberry.parameters.maptype == 'pointmap') {
        setPointMapStyle();
        $scope.resetPolygonLayers();
        setInfoControlPointMap();
        cloudberry.query(cloudberry.parameters, cloudberry.queryType);
      }
      else if (data[0] == 'pointmap'){
        cleanPointMap();
      }
    })
    
    // monitor the pointmap related variables, update the pointmap if necessary
    $scope.$watch(
      function() {
        return cloudberry.pointmapMapResult;
      },

      function(newResult) {
        if (cloudberry.parameters.maptype == 'pointmap'){
          $scope.result = newResult;
          if (Object.keys($scope.result).length !== 0) {
            $scope.status.init = false;
            drawPointMap($scope.result);
          } else {
            drawPointMap($scope.result);
          }
        }
      }
    );
  });
