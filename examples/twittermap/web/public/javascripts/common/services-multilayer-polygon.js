angular.module('cloudberry.common')
    .service('multilayerPolygon', function($http, $timeout, $q, leafletData, cloudberry){
        function initPolygon(scope){
            var instance = this;
            this.scope = scope;
            
            var polygonStyle = {
                initStyle: {
                    weight: 0.5,
                    fillOpacity: 0,
                    color: "white"
                },
                stateStyle: {
                    fillColor: "#f7f7f7",
                    weight: 0.5,
                    opacity: 1,
                    color: "#92d1e1",
                    fillOpacity: 0
                },
                stateUpperStyle: {
                    fillColor: "#f7f7f7",
                    weight: 0.5,
                    opacity: 1,
                    color: "#92d1e1",
                    fillOpacity: 0
                },
                countyStyle: {
                    fillColor: "#f7f7f7",
                    weight: 0.5,
                    opacity: 1,
                    color: "#92d1e1",
                    fillOpacity: 0
                },
                countyUpperStyle: {
                    fillColor: "#f7f7f7",
                    weight: 0.5,
                    opacity: 1,
                    color: "#92d1e1",
                    fillOpacity: 0
                },
                cityStyle: {
                    fillColor: "#f7f7f7",
                    weight: 0.5,
                    opacity: 1,
                    color: "#92d1e1",
                    fillOpacity: 0
                },
                hoverStyle: {
                    weight: 0.7,
                    color: "#666",
                    fillOpacity: 0
                },
                colors: [ "#ffffff", "#92d1e1", "#4393c3", "#2166ac", "#f4a582", "#d6604d", "#b2182b"],
                sentimentColors: ["#ff0000", "#C0C0C0", "#00ff00"]
            };
            
            // update the center and the boundary of the visible area of the map
            function setCenterAndBoundry(features){
                for (var id in features){
                    var minLog = Number.POSITIVE_INFINITY;
                    var maxLog = Number.NEGATIVE_INFINITY;
                    var minLat = Number.POSITIVE_INFINITY;
                    var maxLat = Number.NEGATIVE_INFINITY;
                    if (features[id].geometry.type === "Polygon"){
                        features[id].geometry.coordinates[0].forEach(function(pair){
                            minLog = Math.min(minLog, pair[0]);
                            maxLog = Math.max(maxLog, pair[0]);
                            minLat = Math.min(minLat, pair[1]);
                            maxLat = Math.max(maxLat, pair[1]);
                        });
                    } else if( features[id].geometry.type === "MultiPolygon") {
                        features[id].geometry.coordinates.forEach(function(array){
                            array[0].forEach(function(pair){
                                minLog = Math.min(minLog, pair[0]);
                                maxLog = Math.max(maxLog, pair[0]);
                                minLat = Math.min(minLat, pair[1]);
                                maxLat = Math.max(maxLat, pair[1]);
                            });
                        });
                    }
                    features[id].properties["centerLog"] = (maxLog + minLog) / 2;
                    features[id].properties["centerLat"] = (maxLat + minLat) / 2;
                }
            }
            
            function zoomToFeature(leafletEvent){
                if (leafletEvent)
                    instance.map.fitBounds(leafletEvent.target.getBounds());
            }
            
            this.onEachFeature = function onEachFeature(feature, layer){
                layer.on({
                    click: zoomToFeature
                });
            }
            
            leafletData.getMap().then(function(map){
                instance.map = map;
            });
            
            var deferred = $q.defer();
            
            var statePolygonsReady = false;
            var countyPolygonsReady = false;
            
            // load geoJson to get state and county polygons
            if (!this.polygons.statePolygons){
                $http.get("assets/data/state.json")
                .success(function(data) {
                    instance.geojsonData.state = data;
                    instance.polygons.statePolygons = L.geoJson(data, {
                        style: polygonStyle.stateStyle,
                        onEachFeature: instance.onEachFeature
                    });
                    instance.polygons.stateUpperPolygons = L.geoJson(data, {
                        style: polygonStyle.stateUpperStyle
                    });
                    setCenterAndBoundry(instance.geojsonData.state.features);
                    instance.layer = instance.polygons.statePolygons;
                    if (countyPolygonsReady){
                        deferred.resolve();
                    }
                    else {
                        statePolygonsReady = true;
                    }
                })
                .error(function(data) {
                    console.error("Load state data failure");
                    if (countyPolygonsReady){
                        deferred.resolve();
                    }
                    else {
                        statePolygonsReady = true;
                    }
                });
            }
            if (!this.polygons.countyPolygons){
                $http.get("assets/data/county.json")
                .success(function(data) {
                    instance.geojsonData.county = data;
                    instance.polygons.countyPolygons = L.geoJson(data, {
                        style: polygonStyle.countyStyle,
                        onEachFeature: instance.onEachFeature
                    });
                    instance.polygons.countyUpperPolygons = L.geoJson(data, {
                        style: polygonStyle.countyUpperStyle
                    });
                    setCenterAndBoundry(instance.geojsonData.county.features);
                    if (statePolygonsReady){
                        deferred.resolve();
                    }
                    else {
                        countyPolygonsReady = true;
                    }
                })
                .error(function(data) {
                    console.error("Load county data failure");
                    if (statePolygonsReady){
                        deferred.resolve();
                    }
                    else {
                        countyPolygonsReady = true;
                    }
                });
            }
            
            return deferred.promise;
        }
        
        function drawPolygon(){}
        
        function cleanPolygon(){
            this.layer = null;
        }
        
        function resetGeoIds(bounds, polygons, idTag) {
            cloudberry.parameters.geoIds = [];
            if (polygons != undefined) {
                polygons.features.forEach(function (polygon) {
                    if (bounds._southWest.lat <= polygon.properties.centerLat &&
                        polygon.properties.centerLat <= bounds._northEast.lat &&
                        bounds._southWest.lng <= polygon.properties.centerLog &&
                        polygon.properties.centerLog <= bounds._northEast.lng) {
                        cloudberry.parameters.geoIds.push(polygon.properties[idTag]);
                    }
                });
            }
        }
        
        function zoomFunction(){
            var instance = this;
            
            function resetGeoInfo(level) {
                instance.status.logicLevel = level;
                cloudberry.parameters.geoLevel = level;
                if (instance.geojsonData[level]){
                    resetGeoIds(instance.bounds, instance.geojsonData[level], level + 'ID');
                }
            }
            
            if (this.map) {
                this.status.zoomLevel = this.map.getZoom();
                this.bounds = this.map.getBounds();
                if (this.status.zoomLevel > 9) {
                    resetGeoInfo("city");
                    if (this.polygons.statePolygons) {
                        this.map.removeLayer(this.polygons.statePolygons);
                    }
                    if (this.polygons.countyPolygons) {
                        this.map.removeLayer(this.polygons.countyPolygons);
                    }
                    if (this.polygons.stateUpperPolygons) {
                        this.map.removeLayer(this.polygons.stateUpperPolygons);
                    }
                    this.map.addLayer(this.polygons.countyUpperPolygons);
                    loadCityJsonByBound(this.onEachFeature);
                } else if (this.status.zoomLevel > 5) {
                    resetGeoInfo("county");
                    //if (!this.status.init) {
                        cloudberry.query(cloudberry.parameters);
                    //}
                    if (this.polygons.statePolygons) {
                        this.map.removeLayer(this.polygons.statePolygons);
                    }
                    if (this.polygons.cityPolygons) {
                        this.map.removeLayer(this.polygons.cityPolygons);
                    }
                    if (this.polygons.countyUpperPolygons) {
                        this.map.removeLayer(this.polygons.countyUpperPolygons);
                    }
                    this.map.addLayer(this.polygons.stateUpperPolygons);
                    this.map.addLayer(this.polygons.countyPolygons);
                } else if (this.status.zoomLevel <= 5) {
                    resetGeoInfo("state");
                    //if (!this.status.init) {
                        cloudberry.query(cloudberry.parameters);
                    //}
                    if (this.polygons.countyPolygons) {
                        this.map.removeLayer(this.polygons.countyPolygons);
                    }
                    if (this.polygons.cityPolygons) {
                        this.map.removeLayer(this.polygons.cityPolygons);
                    }
                    if (this.polygons.stateUpperPolygons) {
                        this.map.removeLayer(this.polygons.stateUpperPolygons);
                    }
                    if (this.polygons.countyUpperPolygons) {
                        this.map.removeLayer(this.polygons.countyUpperPolygons);
                    }
                    if (this.polygons.statePolygons) {
                        this.map.addLayer(this.polygons.statePolygons);
                    }
                }
            }
        }
        
        function dragFunction(){
            var instance = this;
            
            //if (!$scope.status.init) {
                this.bounds = this.map.getBounds();
                var geoData;
                if (this.status.logicLevel === 'state') {
                    geoData = this.geojsonData.state;
                } else if (this.status.logicLevel === 'county') {
                    geoData = this.geojsonData.county;
                } else if (this.status.logicLevel === 'city') {
                    geoData = this.geojsonData.city;
                } else {
                    console.error("Error: Illegal value of logicLevel, set to default: state");
                    this.status.logicLevel = 'state';
                    geoData = this.geojsonData.state;
                }
            //}
            if (this.status.logicLevel === 'city') {
                this.loadCityJsonByBound(this.onEachFeature);
            } else {
                resetGeoIds(this.bounds, geoData, this.status.logicLevel + "ID");
                cloudberry.parameters.geoLevel = this.status.logicLevel;
                cloudberry.query(cloudberry.parameters);
            }
        }
        
        var watchVariables = {};
        
        var polygonService = {
            createLayer: function(parameters){
                var deferred = $q.defer();
                deferred.resolve({
                    active: 0,
                    layer: {},
                    init: initPolygon,
                    draw: drawPolygon,
                    clear: cleanPolygon,
                    zoom: zoomFunction,
                    drag: dragFunction,
                    watchVariables: watchVariables,
                    map: null,
                    geojsonData: {},
                    polygons: {},
                    status: {}
                });
                return deferred.promise;
            }
        }
        
        return polygonService;
    });
