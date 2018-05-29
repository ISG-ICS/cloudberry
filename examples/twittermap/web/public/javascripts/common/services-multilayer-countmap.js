angular.module('cloudberry.common')
    .service('multilayerCountmap', function($http, $timeout, $q, $compile, cloudberryConfig, leafletData){
        function initCountMap(){
            var instance = this;
            
            this.layer = L.layerGroup();
            
            countmapStyle = {
                initStyle: {
                    weight: 1.5,
                    fillOpacity: 0.5,
                    color: 'white'
                },
                stateStyle: {
                    fillColor: '#f7f7f7',
                    weight: 1.5,
                    opacity: 1,
                    color: '#92d1e1',
                    fillOpacity: 0.5
                },
                stateUpperStyle: {
                    fillColor: '#f7f7f7',
                    weight: 1.5,
                    opacity: 1,
                    color: '#92d1e1',
                    fillOpacity: 0.5
                },
                countyStyle: {
                    fillColor: '#f7f7f7',
                    weight: 1.5,
                    opacity: 1,
                    color: '#92d1e1',
                    fillOpacity: 0.5
                },
                countyUpperStyle: {
                    fillColor: '#f7f7f7',
                    weight: 1.5,
                    opacity: 1,
                    color: '#92d1e1',
                    fillOpacity: 0.5
                },
                cityStyle: {
                    fillColor: '#f7f7f7',
                    weight: 1.5,
                    opacity: 1,
                    color: '#92d1e1',
                    fillOpacity: 0.5
                },
                hoverStyle: {
                    weight: 5,
                    color: '#666',
                    fillOpacity: 0.5
                },
                colors: [ '#ffffff', '#92d1e1', '#4393c3', '#2166ac', '#f4a582', '#d6604d', '#b2182b'],
                sentimentColors: ['#ff0000', '#C0C0C0', '#00ff00']
            }
            
            function highlightFeature(leafletEvent) {
                var layer = leafletEvent.target;
                layer.setStyle(countmapStyle.hoverStyle);
                if (!L.Browser.ie && !L.Browser.opera) {
                    layer.bringToFront();
                }
                instance.selectedPlace = layer.feature;
            }

            // remove the highlight interaction function for the polygons
            function resetHighlight(leafletEvent) {
                var style = {
                    weight: 1.5,
                    fillOpacity: 0.5,
                    color: '#92d1e1'
                };
                if (leafletEvent){
                    leafletEvent.target.setStyle(style);
                }
            }

            // add feature to each polygon
            // highlight a polygon when mouseover
            // remove the highlight when mouseout
            // zoom in to fit the polygon when the polygon is clicked
            this.onEachFeature = function onEachFeature(feature, layer) {
                layer.on({
                    mouseover: highlightFeature,
                    mouseout: resetHighlight,
                });
            }

            // add info control
            /*
            var info = L.control();

            info.onAdd = function() {
                this._div = L.DomUtil.create('div', 'info'); // create a div with a class "info"
                this._div.style.margin = '20% 0 0 0';
                this._div.innerHTML = [
                    '<h4>{{ infoPromp }} by {{ status.logicLevel }}</h4>',
                    '<b>{{ selectedPlace.properties.name || "No place selected" }}</b>',
                    '<br/>',
                    '{{ infoPromp }} {{ selectedPlace.properties.countText || "0" }}'
                ].join('');
                $compile(this._div)(this);
                return this._div;
            };

            info.options = {
                position: 'topleft'
            };
            this.layer.addLayer(info);
            */
            //$scope.controls.custom.push(info);
            
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
                        style: countmapStyle.stateStyle,
                        onEachFeature: instance.onEachFeature
                    });
                    instance.polygons.stateUpperPolygons = L.geoJson(data, {
                        style: countmapStyle.stateUpperStyle
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
                        style: countmapStyle.countyStyle,
                        onEachFeature: instance.onEachFeature
                    });
                    instance.polygons.countyUpperPolygons = L.geoJson(data, {
                        style: countmapStyle.countyUpperStyle
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
        
        function drawCountMap(result){
        
        }
        
        function cleanCountMap(){
            this.layer = null;
        }
        
        var watchVariables = {"countmapMapResult":"cloudberry.countmapMapResult",
                              "doNormalization":"$('#toggle-normalize').prop('checked')",
                              "doSentiment":"$('#toggle-sentiment').prop('checked')"};
        
        var countmapService = {
            createLayer: function(parameters){
                var deferred = $q.defer();
                deferred.resolve({
                    active: 0,
                    layer: {},
                    init: initCountMap,
                    draw: drawCountMap,
                    clear: cleanCountMap,
                    watchVariables: watchVariables,
                    map: null,
                    geojsonData: {},
                    polygons: {},
                    status: {}
                });
                return deferred.promise;
            }
        }
        
        return countmapService;
    });
