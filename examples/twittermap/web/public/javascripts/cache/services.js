/*Cache module stores user requested city polygons.When Users requests these same city polygon area again we have it in cache
 and provide it to the user without sending http request.*/
angular.module('cloudberry.cache', ['leaflet-directive', 'cloudberry.common'])
    .service('Cache', function ($window, $http, $compile, cloudberryConfig) {

        var cachedCityPolygonTree = rbush();
        var cachedRegion;
        var cacheSize = 0;
        var insertedTreeIDs = new Set();
        var cacheThreshold = cloudberryConfig.cacheThreshold; //hard limit
        var targetDeleteCount = 0;
        var deletedPolygonCount = 0;
        var preFetchDistance = 25; //25 miles
        var previousRequestCentroid;
        var currentReqMBR;
        var ratio; //slope of trend line
        var user_direction;
        var RequestPolygonWithPrefetch;
        var currentRequestPolygon;
        //Four Corners of Current request
        var UpperRightPoint;
        var UpperLeftPoint;
        var LowerLeftPoint;
        var LowerRightPoint;

        /* Map controller calls this function and this function checks whether a requested region is present in the cache or not. If not,
         it gets the requested region data from the middleware.*/
        this.getCityPolygonsFromCache = function city(rteBounds) {

            var deferred = new $.Deferred();
            var data_response;

            var bounds = rteBounds.split("/");
            var bounds_northEast_lat = parseFloat(bounds[1]);
            var bounds_southWest_lat = parseFloat(bounds[2]);
            var bounds_northEast_lng = parseFloat(bounds[3]);
            var bounds_southWest_lng = parseFloat(bounds[4]);
            currentRequestPolygon = turf.polygon([[
                [bounds_northEast_lng, bounds_northEast_lat],
                [bounds_northEast_lng, bounds_southWest_lat],
                [bounds_southWest_lng, bounds_southWest_lat],
                [bounds_southWest_lng, bounds_northEast_lat],
                [bounds_northEast_lng, bounds_northEast_lat]
            ]]);


            var bbox = turf.bbox(currentRequestPolygon);
            var extraBounds;
            currentReqMBR = bbox;
            // to search in Rbush Tree ,we need the MBR of the requested region.
            var item = {
                minX: bbox[0],
                minY: bbox[1],
                maxX: bbox[2],
                maxY: bbox[3]
            }


            if (typeof cachedRegion != "undefined" && typeof turf.difference(currentRequestPolygon, cachedRegion) == "undefined") {
                //cache HIT

                var result = cachedCityPolygonTree.search(item);
                data_response = turf.featureCollection(result);
                RequestPolygonWithPrefetch = currentRequestPolygon;
                console.log(data_response);
                deferred.resolve(data_response);
                return deferred.promise();

            } else {
                //cache MISS
                Hit = false;
                var centroidRequestPoly = turf.centroid(currentRequestPolygon);

                prefetch(currentRequestPolygon).done(function (newMBR) {

                    RequestPolygonWithPrefetch = turf.bboxPolygon(newMBR);
                    extraBounds = "city/" + newMBR[3] + "/" + newMBR[1] + "/" + newMBR[2] + "/" + newMBR[0];
                    $http.get(extraBounds).success(function (data) {

                        insertIntoTree(data.features, RequestPolygonWithPrefetch).done(function () {

                            data_response = data;

                            if (cachedRegion == undefined)
                                cachedRegion = currentRequestPolygon;
                            else
                                cachedRegion = turf.union(RequestPolygonWithPrefetch, cachedRegion);

                            previousRequestCentroid = centroidRequestPoly;
                            console.log(data_response);
                            deferred.resolve(data_response);
                        });
                    }).error(function (data) {
                        console.error("Load city data failure");
                    });
                });

                return deferred.promise();
            }
        }


//Find Angle between two points
        function getAngle(pt1, pt2) {

            var x1 = pt1["geometry"]["coordinates"][0];
            var x2 = pt2["geometry"]["coordinates"][0];
            var y1 = pt1["geometry"]["coordinates"][1];
            var y2 = pt2["geometry"]["coordinates"][1];
            var angle = Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI;
            ratio = (y2 - y1) / (x2 - x1);
            return angle;
        }


//Takes current Request Polygon and fives out new MBR with prefetched region
        function prefetch(currentRequestPolygon) {
            var deferred = new $.Deferred();
            var currentRequestCentroid = turf.centroid(currentRequestPolygon);
            ;
            var curReqBBox = turf.bbox(currentRequestPolygon);
            //No prefetch when there is no cached Region ,return just the MBR of original request without prefetch
            if (typeof cachedRegion == "undefined" || typeof previousRequestCentroid == "undefined") {
                deferred.resolve(curReqBBox);
                return deferred.promise();
            }


            var x1 = currentRequestCentroid["geometry"]["coordinates"][0];
            var y1 = currentRequestCentroid["geometry"]["coordinates"][1];
            var angle = getAngle(previousRequestCentroid, currentRequestCentroid);
            var y2, x2;

            //Finding Intersection of Trend Line and Request Polygon
            if (angle > 45 && angle <= 135) {
                //line1-TOP
                y2 = curReqBBox[3];
                x2 = ((y2 - y1) / ratio) + x1;

            }
            else if (angle > -45 && angle <= 45) {
                //line2-RIGHT
                x2 = curReqBBox[2];
                y2 = ratio * (x2 - x1) + y1;

            }
            else if (angle >= -135 && angle <= -45) {
                //line3-BOTTOM
                y2 = curReqBBox[1];
                x2 = ((y2 - y1) / ratio) + x1;

            }
            else if ((angle >= 135 && angle <= 180) || (angle >= -180 && angle <= -135)) {
                //line4-LEFT
                x2 = curReqBBox[0];
                y2 = ratio * (x2 - x1) + y1;

            }
            //Intersected Point
            var edgePoint = turf.point([x2, y2]);
            var bearing = turf.bearing(previousRequestCentroid, currentRequestCentroid);
            var units = 'miles';
            //New Point after prefetch Distance
            var newPoint = turf.destination(edgePoint, preFetchDistance, bearing, units);


            var newX = newPoint["geometry"]["coordinates"][0];
            var newY = newPoint["geometry"]["coordinates"][1];
            var deltaX = x2 - newX;
            var deltaY = y2 - newY;
            var maxDelta = Math.max(Math.abs(deltaX), Math.abs(deltaY));

            var newbbox;
            var minX = curReqBBox[0];
            var minY = curReqBBox[1];
            var maxX = curReqBBox[2];
            var maxY = curReqBBox[3];
            //Finding the new MBR
            if (angle > 0 && angle <= 90) {
                //NE

                var newmaxX = maxX + maxDelta;
                var newmaxY = maxY + maxDelta;
                newbbox = [minX, minY, newmaxX, newmaxY];
                user_direction = "NE";

            } else if (angle > -180 && angle <= -90) {
                //SW

                var newminX = minX - maxDelta;
                var newminY = minY - maxDelta;
                newbbox = [newminX, newminY, maxX, maxY];
                user_direction = "SW";

            } else if (angle > -90 && angle <= 0) {
                //SE

                var newmaxX = maxX + maxDelta;
                var newminY = minY - maxDelta;
                newbbox = [minX, newminY, newmaxX, maxY];
                user_direction = "SE";

            } else if (angle > 90 && angle <= 180) {
                //NW

                var newminX = minX - maxDelta;
                var newmaxY = maxY + maxDelta;
                newbbox = [newminX, minY, maxX, newmaxY];
                user_direction = "NW";

            }
            deferred.resolve(newbbox);

            return deferred.promise();
        }

//to insert polygons into the Rtree,features are the city polygons
        var insertIntoTree = function insertIntoTree(features, currentRequest) {

            var deferred = new $.Deferred();
            var nodes = [];
            var treeID;

            for (var id in features) {
                var box = turf.bbox(features[id]);
                features[id].minX = box[0];
                features[id].minY = box[1];
                features[id].maxX = box[2];
                features[id].maxY = box[3];
                features[id].properties["centerLog"] = (features[id].maxX + features[id].minX) / 2;
                features[id].properties["centerLat"] = (features[id].maxY + features[id].minY) / 2;
                treeID = box[0] + "/" + box[1] + "/" + box[2] + "/" + box[3];
                if (insertedTreeIDs.has(treeID) == false) {
                    nodes.push(features[id]);
                    insertedTreeIDs.add(treeID);
                }
            }
            //Checking Cache Overflow ,occurs when current polygons(nodes) to get inserted plus cachesize is greater than Cache Threshold
            if ((cacheSize + nodes.length) >= cacheThreshold) {

                targetDeleteCount = (cacheSize + nodes.length) - cacheThreshold;
                evict(currentRequest).done(function () {

                    cachedCityPolygonTree.load(nodes);
                    cacheSize += nodes.length;
                });
                deferred.resolve();
                return deferred.promise();

            } else {
                cacheSize += nodes.length;
                cachedCityPolygonTree.load(nodes);
                deferred.resolve();
                return deferred.promise();
            }
        }

        function findNewDirection(nearestPoint) {
            console.log("Finding  new direction");

            if (nearestPoint == UpperRightPoint) {
                return "SW";
            }
            if (nearestPoint == LowerRightPoint) {
                return "NW";
            }
            if (nearestPoint == UpperLeftPoint) {
                return "SE";
            }
            if (nearestPoint == LowerLeftPoint) {
                return "NE";
            }

        }
//Determines which part of the cached region needs to be evicted to reduce the cached city polygons to ensure staying within the cache budget.
        var evict = function Evict(currentRequest) {

            var deferred = new $.Deferred();
            var cache_bbox = turf.bbox(cachedRegion);
            var C_minX = cache_bbox[0];
            var C_minY = cache_bbox[1];
            var C_maxX = cache_bbox[2];
            var C_maxY = cache_bbox[3];

            var request_bbox = turf.bbox(currentRequest);
            var R_minX = request_bbox[0];
            var R_minY = request_bbox[1];
            var R_maxX = request_bbox[2];
            var R_maxY = request_bbox[3];

            var cachedRegionMBR = turf.bboxPolygon(cache_bbox);
            UpperRightPoint = turf.point([R_maxX, R_maxY]);
            UpperLeftPoint  = turf.point([R_minX, R_maxY]);
            LowerLeftPoint  = turf.point([R_minX, R_minY]);
            LowerRightPoint = turf.point([R_maxY, R_minY]);

            //Checks which part of requested region is overlapped with the cached region.
            var UpperRight = turf.inside(turf.point([R_maxX, R_maxY]), cachedRegionMBR);
            var UpperLeft  = turf.inside(turf.point([R_minX, R_maxY]), cachedRegionMBR);
            var LowerLeft  = turf.inside(turf.point([R_minX, R_minY]), cachedRegionMBR);
            var LowerRight = turf.inside(turf.point([R_maxY, R_minY]), cachedRegionMBR);

            var centroidCurrentRequest = turf.centroid(currentRequest);
            var points = [UpperRightPoint, UpperLeftPoint, LowerLeftPoint, LowerRightPoint];
            var pointCollection = turf.featureCollection(points);
            var nearestPoint = turf.nearest(centroidCurrentRequest, pointCollection);

            if (nearestPoint == UpperRightPoint && user_direction == "SW") {
                var newPointSet = [UpperLeftPoint, LowerRightPoint];
                var newPointCollection = turf.featureCollection(newPointSet);
                var newNearestPoint = turf.nearest(centroidCurrentRequest, newPointCollection);
                user_direction = findNewDirection(newNearestPoint);
            }
            if (nearestPoint == LowerRightPoint && user_direction == "NW") {
                var newPointSet = [LowerLeftPoint, UpperRightPoint];
                var newPointCollection = turf.featureCollection(newPointSet);
                var newNearestPoint = turf.nearest(centroidCurrentRequest, newPointCollection);
                user_direction = findNewDirection(newNearestPoint);
            }
            if (nearestPoint == UpperLeftPoint && user_direction == "SE") {
                var newPointSet = [LowerLeftPoint, UpperRightPoint];
                var newPointCollection = turf.featureCollection(newPointSet);
                var newNearestPoint = turf.nearest(centroidCurrentRequest, newPointCollection);
                user_direction = findNewDirection(newNearestPoint);
            }
            if (nearestPoint == LowerLeftPoint && user_direction == "NE") {
                var newPointSet = [UpperLeftPoint, LowerRightPoint];
                var newPointCollection = turf.featureCollection(newPointSet);
                var newNearestPoint = turf.nearest(centroidCurrentRequest, newPointCollection);
                user_direction = findNewDirection(newNearestPoint);
            }
            if (LowerRight || LowerLeft && !UpperRight && !UpperLeft) {
                //Y from bottom to top

                cutRegion(C_minX, C_minY, C_maxX, R_minY, user_direction).done(function () {
                    deferred.resolve();
                }).fail(function () {

                    clearCache().done();
                    deferred.resolve();
                })
                return deferred.promise();
            }
            else if (UpperRight || UpperLeft && !LowerLeft && !LowerRight) {

                cutRegion(C_minX, R_maxY, C_maxX, C_maxY, user_direction).done(function () {
                    //Y from top to bottom

                    deferred.resolve();
                }).fail(function () {

                    clearCache().done();
                    deferred.resolve();
                })
                return deferred.promise();

            }
            else if (UpperRight && LowerRight) {
                //X from left to right

                cutRegion(R_maxX, C_minY, C_maxX, C_maxY, user_direction).done(function () {

                    deferred.resolve();
                }).fail(function () {

                    clearCache().done();
                    deferred.resolve();
                })
                return deferred.promise();

            } else if (UpperLeft && LowerLeft) {
                //X from right to left

                cutRegion(C_minX, C_minY, R_minX, C_maxY, user_direction).done(function () {

                    deferred.resolve();
                }).fail(function () {

                    clearCache().done();
                    deferred.resolve();
                })
                return deferred.promise();

            } else if (!LowerRight && !LowerLeft && !UpperLeft && !UpperRight) {
                //NO Overlap"
                cutRegion(C_minX, C_minY, C_maxX, C_maxY, user_direction).done(function () {

                    deferred.resolve();
                }).fail(function () {

                    clearCache().done();
                    deferred.resolve();
                })
                return deferred.promise();
            }
        }

        /*Checks whether evicting some city polygons in a part of the cache region satisfies the target deletion count.
         If not, returns a failure message to the caller*/
        //Whether evicting the cached region vertically or horizontally. true - X axis (horizontally), false - Y axis (vertically)
        //TopTo Bottom = true
        //BottomToTop = false
        //LeftToRight = true
        //RightToLeft = false
        var cutRegion = function findCornerofEviction(minX, minY, maxX, maxY, Direction) {

            var deferred = new $.Deferred();
            var Xline, Yline;
            if (Direction == "NE") {
                Xline = turf.lineString([[minX, minY], [maxX, minY]]);
                Yline = turf.lineString([[minX, minY], [minX, maxY]]);
            }
            if (Direction == "SW") {
                Xline = turf.lineString([[maxX, maxY], [minX, maxY]]);
                Yline = turf.lineString([[maxX, maxY], [maxX, minY]]);
            }
            if (Direction == "NW") {
                Xline = turf.lineString([[maxX, minY], [minX, minY]]);
                Yline = turf.lineString([[maxX, minY], [maxX, maxY]]);
            }
            if (Direction == "SE") {
                Xline = turf.lineString([[minX, maxY], [maxX, maxY]]);
                Yline = turf.lineString([[minX, maxY], [minX, minY]]);
            }
            var distanceX = turf.lineDistance(Xline, 'miles');
            var distanceY = turf.lineDistance(Yline, 'miles');
            //Make 10 slices
            var ShiftX = distanceX / 10;
            var ShiftY = distanceY / 10;
            var start = 0;
            var stop = distanceX;
            var removeItems;
            var slicedXline, slicedYline, cutPointX, cutPointY, cutBbox, remove_search;
            var moveX = ShiftX;
            var moveY = ShiftY;
            var count = 0;
            while (deletedPolygonCount < targetDeleteCount) {


                slicedXline = turf.lineSliceAlong(Xline, start, moveX, 'miles');
                slicedYline = turf.lineSliceAlong(Yline, start, moveY, 'miles');
                cutPointX = slicedXline["geometry"]["coordinates"][1][0];
                cutPointY = slicedYline["geometry"]["coordinates"][1][1];
                if (Direction == "NE") {
                    cutBbox = [minX, minY, cutPointX, cutPointY];
                }
                if (Direction == "SW") {
                    cutBbox = [cutPointX, cutPointY, maxX, maxY];
                }
                if (Direction == "NW") {
                    cutBbox = [cutPointX, minY, maxX, cutPointY];
                }
                if (Direction == "SE") {
                    cutBbox = [minX, cutPointY, cutPointX, maxY];
                }

                remove_search = {
                    minX: cutBbox[0],
                    minY: cutBbox[1],
                    maxX: cutBbox[2],
                    maxY: cutBbox[3]
                }
                removeItems = cachedCityPolygonTree.search(remove_search);
                deletedPolygonCount = removeItems.length;
                moveX += ShiftX;
                moveY += ShiftY;
                count += 1;

                if (moveX > stop) {
                    break;
                }
            }

            deletion(removeItems).done(function () {
                var PolygonRegionRemovedFromCache = turf.bboxPolygon(cutBbox);
                cacheSize -= deletedPolygonCount;
                cachedRegion = turf.difference(cachedRegion, PolygonRegionRemovedFromCache);
                targetDeleteCount -= deletedPolygonCount;
                deletedPolygonCount = 0;
            });

            if (targetDeleteCount > 0) {
                // Unsucessful Delete
                deferred.reject();
                return deferred.promise();
            } else {
                // Sucessful Delete
                deferred.resolve();
                return deferred.promise();
            }
        }

// deleting polygons from Rtree
        var deletion = function deleteNodesfromTree(removeItems) {
            var deferred = new $.Deferred();
            for (var i = 0; i < removeItems.length; i++)
                cachedCityPolygonTree.remove(removeItems[i]);

            deferred.resolve();
            return deferred.promise();
        }

        var clearCache = function remove() {
            var deferred = new $.Deferred();
            cachedRegion = undefined;
            cacheSize = 0;
            cachedCityPolygonTree.clear();
            targetDeleteCount = 0;
            deletedPolygonCount = 0;
            insertedTreeIDs.clear();
            deferred.resolve();
            return deferred.promise();
        }


    })

