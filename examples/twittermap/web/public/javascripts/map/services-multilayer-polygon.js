angular.module('cloudberry.map')
    .service('multilayerPolygon', function($http, $timeout, $q, leafletData, cloudberry, moduleManager){
        
        var ly_style = {}
        var Scope;
        var instance;
        function initPolygon(scope){
            instance = this;
            Scope = scope;
            scope.$on("leafletDirectiveMap.zoomend",function(){
              if(cloudberry.parameters.maptype!="countmap")
              {
                zoomFunction();
              }
            });
            scope.$on("leafletDirectiveMap.dragend", function(){
              if(cloudberry.parameters.maptype!="countmap")
              {
                dragFunction();
              }
            });
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
            
            ly_style = polygonStyle;
            
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
            
            // reset the geo level (state, county, city)
            function resetGeoInfo(level) {
                instance.logicLevel = level;
                cloudberry.parameters.geoLevel = level;
                if (instance.geojsonData[level])
                    resetGeoIds(instance.bounds, instance.geojsonData[level], level + 'ID');
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
            function resetGeoInfo(level) {
                instance.status.logicLevel = level;
                cloudberry.parameters.geoLevel = level;
                if (instance.geojsonData[level]){
                    resetGeoIds(instance.bounds, instance.geojsonData[level], level + 'ID');
                }
            }
            
            if (instance.map) {
                instance.status.zoomLevel = instance.map.getZoom();
                instance.bounds = instance.map.getBounds();
                if (instance.status.zoomLevel > 9) {
                    resetGeoInfo("city");
                    if (instance.polygons.statePolygons) {
                        instance.map.removeLayer(instance.polygons.statePolygons);
                    }
                    if (instance.polygons.countyPolygons) {
                        instance.map.removeLayer(instance.polygons.countyPolygons);
                    }
                    if (instance.polygons.stateUpperPolygons) {
                        instance.map.removeLayer(instance.polygons.stateUpperPolygons);
                    }
                    instance.map.addLayer(instance.polygons.countyUpperPolygons);
                    loadCityJsonByBound(instance.onEachFeature);
                } else if (instance.status.zoomLevel > 5) {
                    resetGeoInfo("county");
                    //if (!instance.status.init) {
                        moduleManager.publishEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, {level: instance.map.getZoom(), bounds: instance.map.getBounds()});
                        //cloudberry.query(cloudberry.parameters);
                    //}
                    if (instance.polygons.statePolygons) {
                        instance.map.removeLayer(instance.polygons.statePolygons);
                    }
                    if (instance.polygons.cityPolygons) {
                        instance.map.removeLayer(instance.polygons.cityPolygons);
                    }
                    if (instance.polygons.countyUpperPolygons) {
                        instance.map.removeLayer(instance.polygons.countyUpperPolygons);
                    }
                    instance.map.addLayer(instance.polygons.stateUpperPolygons);
                    instance.map.addLayer(instance.polygons.countyPolygons);
                } else if (instance.status.zoomLevel <= 5) {
                    resetGeoInfo("state");
                    //if (!instance.status.init) {
                        moduleManager.publishEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, {level: instance.map.getZoom(), bounds: instance.map.getBounds()});
                        //cloudberry.query(cloudberry.parameters);
                    //}
                    if (instance.polygons.countyPolygons) {
                        instance.map.removeLayer(instance.polygons.countyPolygons);
                    }
                    if (instance.polygons.cityPolygons) {
                        instance.map.removeLayer(instance.polygons.cityPolygons);
                    }
                    if (instance.polygons.stateUpperPolygons) {
                        instance.map.removeLayer(instance.polygons.stateUpperPolygons);
                    }
                    if (instance.polygons.countyUpperPolygons) {
                        instance.map.removeLayer(instance.polygons.countyUpperPolygons);
                    }
                    if (instance.polygons.statePolygons) {
                        instance.map.addLayer(instance.polygons.statePolygons);
                    }
                }
            }
        }
        
        function dragFunction(){            
            //if (!$scope.status.init) {
                instance.bounds = instance.map.getBounds();
                var geoData;
                if (instance.status.logicLevel === 'state') {
                    geoData = instance.geojsonData.state;
                } else if (instance.status.logicLevel === 'county') {
                    geoData = instance.geojsonData.county;
                } else if (instance.status.logicLevel === 'city') {
                    geoData = instance.geojsonData.city;
                } else {
                    console.error("Error: Illegal value of logicLevel, set to default: state");
                    instance.status.logicLevel = 'state';
                    geoData = instance.geojsonData.state;
                }
            //}
            if (instance.status.logicLevel === 'city') {
                instance.loadCityJsonByBound(instance.onEachFeature);
            } else {
                resetGeoIds(instance.bounds, geoData, instance.status.logicLevel + "ID");
                cloudberry.parameters.geoLevel = instance.status.logicLevel;
                //cloudberry.query(cloudberry.parameters);
                moduleManager.publishEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, {bounds: instance.map.getBounds()});
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
                    onMapTypeChange: drawPolygon,
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
