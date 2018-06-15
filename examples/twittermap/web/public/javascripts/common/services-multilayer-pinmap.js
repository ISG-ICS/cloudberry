angular.module('cloudberry.common')
    .service('multilayerPinmap', function($http, $timeout, $q, cloudberry, cloudberryConfig, leafletData){
        var defaultPinmapLimit = parseInt(config.pinmapSamplingLimit);
        
        function initPinMap(scope){
            var instance = this;
            
            this.layer = L.layerGroup();
            leafletData.getMap().then(function(map){
                instance.map = map;
            });
            this.mouseOverPointI = 0;
            
            var deferred = $q.defer();
            deferred.resolve();
            return deferred.promise;
        }
        
        // For randomize coordinates by bounding_box
        var randomizationSeed;

        // javascript does not provide API for setting seed for its random function, so we need to implement it ourselves.
        function CustomRandom() {
            var x = Math.sin(randomizationSeed++) * 10000;
            return x - Math.floor(x);
        }

        // return a random number with normal distribution
        function randomNorm(mean, stdev) {
            return mean + (((CustomRandom() + CustomRandom() + CustomRandom() + CustomRandom() + CustomRandom() + CustomRandom()) - 3) / 3) * stdev;
        }

        // randomize a pin coordinate for a tweet according to the bounding box (normally distributed within the bounding box) when the actual coordinate is not availalble.
        // by using the tweet id as the seed, the same tweet will always be randomized to the same coordinate.
        function rangeRandom(seed, minV, maxV){
            randomizationSeed = seed;
            var ret = randomNorm((minV + maxV) / 2, (maxV - minV) / 16);
            return ret;
        }
        
        function drawPinMap(result){
            var instance = this;
            
            if (this.currentMarker != null) {
                this.layer.removeLayer(this.currentMarker);
            }

            //To initialize the points layer
            if (!this.pointsLayer) {
           
                this.pointsLayer = new L.TileLayer.MaskCanvas(this.parameters.pinStyle);

                this.layer.addLayer(this.pointsLayer);

                //Create a new event called "mouseintent" by listening to "mousemove".
                var timer = null;
                //If user hang the mouse cursor for 300ms, fire a "mouseintent" event.
                function onMapMouseMove(e) {
                    var duration = 300;
                    if (timer !== null) {
                        clearTimeout(timer);
                        timer = null;
                    }
                    timer = setTimeout(L.Util.bind(function() {
                        this.fire("mouseintent", {
                            latlng : e.latlng,
                            layer : e.layer
                        });
                        timer = null;
                    }, this), duration);
                }
                this.map.on("mousemove", onMapMouseMove);

                this.currentBounds = null;
                this.scale_x = 0;
                this.scale_y = 0;

                //To generate Tweet Popup content from Twitter API (oembed.json?) response JSON
                function translateOembedTweet(tweetJSON) {
                    var userName = "";
                    try {
                        userName = tweetJSON.author_name;
                    }
                    catch (e){
                        console.log("author_name missing in this Tweet.:" + e.message);
                    }

                    var userLink = "";
                    try {
                        userLink = tweetJSON.author_url;
                    }
                    catch (e) {
                        console.log("author_url missing in this Tweet.:" + e.message);
                    }

                    var tweetLink = "";
                    try {
                        tweetLink = tweetJSON.url;
                    }
                    catch (e){
                        console.log("url missing in this Tweet.:" + e.message);
                    }

                    var tweetText = "";
                    try {
                        var tweetHtml = new DOMParser().parseFromString(tweetJSON.html, "text/html");
                        tweetText = tweetHtml.getElementsByTagName("p")[0].innerHTML;
                    }
                    catch (e){
                        console.log("html missing in this Tweet.:" + e.message);
                    }

                    var tweetTemplate = "\n"
                        + "<div class=\"tweet\">\n "
                        + "  <div class=\"tweet-body\">"
                        + "    <div class=\"user-info\"> "
                        + "      <span class=\"name\"> "
                        + "        <a href=\""
                        + userLink
                        + "        \"> "
                        + "@"
                        + userName
                        + "        </a>"
                        + "      </span> "
                        + "    </div>\n	"
                        + "    <div class=\"tweet-text\">"
                        + tweetText
                        + "\n &nbsp;&nbsp;<a href=\""
                        + tweetLink
                        + "      \"> "
                        + "[more]..."
                        + "      </a>"
                        + "    </div>\n	 "
                        + "  </div>\n	"
                        + "</div>\n";

                    return tweetTemplate;
                }

                this.map.on("mouseintent", onMapMouseIntent);

                this.currentMarker = null;
                this.points = [];
                this.pointIDs = [];

                function onMapMouseIntent(e) {
                    //make sure the scale metrics are updated
                    if (instance.currentBounds == null || instance.scale_x == 0 || instance.scale_y == 0) {
                        instance.currentBounds = instance.map.getBounds();
                        instance.scale_x = Math.abs(instance.currentBounds.getEast()
                            - instance.currentBounds.getWest());
                        instance.scale_y = Math.abs(instance.currentBounds.getNorth()
                            - instance.currentBounds.getSouth());
                    }

                    var i = isMouseOverAPoint(e.latlng.lat, e.latlng.lng);

                    //if mouse over a new point, show the Popup Tweet!
                    if (i >= 0 && instance.mouseOverPointI != i) {
                        instance.mouseOverPointI = i;
                        //(1) If previous Marker is not null, destroy it.
                        if (instance.currentMarker != null) {
                            instance.layer.removeLayer(instance.currentMarker);
                        }
                        //(2) Create a new Marker to highlight the point.
                        instance.currentMarker = L.circleMarker(e.latlng, instance.parameters.highlightPinStyle).addTo(instance.layer);
                        //(3) Send request to twitter.com for the oembed json tweet content.
                        var url = "https://api.twitter.com/1/statuses/oembed.json?callback=JSON_CALLBACK&id=" + instance.pointIDs[i];
                        $http.jsonp(url).success(function (data) {
                            var tweetContent = translateOembedTweet(data);
                            instance.popUpTweet = L.popup({maxWidth:300, minWidth:300, maxHight:300});
                            instance.popUpTweet.setContent(tweetContent);
                            instance.currentMarker.bindPopup(instance.popUpTweet).openPopup();
                        }).
                        error(function() {
                            var tweetContent = "Sorry! It seems the tweet with that ID has been deleted by the author.@_@";
                            instance.popUpTweet = L.popup({maxWidth:300, minWidth:300, maxHight:300});
                            instance.popUpTweet.setContent(tweetContent);
                            instance.currentMarker.bindPopup(instance.popUpTweet).openPopup();
                        });
                    }
                }

                function isMouseOverAPoint(x, y) {
                    for (var i = 0; i < instance.points.length; i += 1) {
                        var dist_x = Math.abs((instance.points[i][0] - x) / instance.scale_x);
                        var dist_y = Math.abs((instance.points[i][1] - y) / instance.scale_y);
                        if (dist_x <= 0.01 && dist_y <= 0.01) {
                            return i;
                        }
                    }
                    return -1;
                }
            }

            //Update the points data
            if (result.length > 0){
                this.points = [];
                this.pointIDs = [];
                for (var i = 0; i < result.length; i++) {
                    if (result[i].hasOwnProperty("coordinate")){
                        this.points.push([result[i].coordinate[1], result[i].coordinate[0]]);
                    }
                    else if (result[i].hasOwnProperty("place.bounding_box")){
                        this.points.push([rangeRandom(result[i].id, result[i]["place.bounding_box"][0][1], result[i]["place.bounding_box"][1][1]), rangeRandom(result[i].id + 79, result[i]["place.bounding_box"][0][0], result[i]["place.bounding_box"][1][0])]); // 79 is a magic number to avoid using the same seed for generating both the longitude and latitude.
                    }
                    this.pointIDs.push(result[i].id);
                }
                this.pointsLayer.setData(this.points);
            }
            else {
                this.points = [];
                this.pointIDs = [];
                this.pointsLayer.setData(this.points);
            }
        }
        
        function cleanPinMap(){
            this.layer = null;
        }
        
        function createPinQuery(filter){
            var pointsJson = (JSON.stringify({
                dataset: this.parameters.dataset,
                filter: filter,
                select: {
                    order: ["-create_at"],
                    limit: defaultPinmapLimit,
                    offset: 0,
                    field: ["id", "coordinate", "place.bounding_box", "create_at", "user.id"]
                },
                option: {
                    sliceMillis: cloudberryConfig.querySliceMills
                },
                transform: {
                    wrap: {
                        id: this.parameters.id,
                        category: this.parameters.id
                    }
                }
            }));
            
            return pointsJson;

            /*
            // for the time histogram
            var pointsTimeJson = (JSON.stringify({
                dataset: parameters.dataset,
                filter: getFilter(parameters, defaultNonSamplingDayRange, parameters.geoIds),
                group: {
                    by: [{
                        field: "create_at",
                        apply: {
                            name: "interval",
                            args: {
                                unit: parameters.timeBin
                            }
                        },
                        as: parameters.timeBin
                    }],
                    aggregate: [{
                        field: "*",
                        apply: {
                            name: "count"
                        },
                        as: "count"
                    }]
                },
                option: {
                    sliceMillis: cloudberryConfig.querySliceMills
                },
                transform: {
                    wrap: {
                        id: "pointsTime",
                        category: "pointsTime"
                    }
                }
            }));
            */
        }
        
        var watchVariables = {"pinmapMapResult":"cloudberry.pinmapMapResult"};
        
        var pinmapService = {
            createLayer: function(parameters){
                var deferred = $q.defer();
                deferred.resolve({
                    active: 0,
                    parameters: parameters,
                    layer: {},
                    init: initPinMap,
                    draw: drawPinMap,
                    clear: cleanPinMap,
                    createQuery: createPinQuery,
                    watchVariables: watchVariables
                });
                return deferred.promise;
            }
        }
        
        return pinmapService;
    });
