
/*Cache module stores user requested city polygons.When Users requests these same city polygon area again we have it in cache
and provide it to the user without sending http request.*/
angular.module('cloudberry.cache', ['leaflet-directive', 'cloudberry.common' ])
.service('Cache', function( $window, $http, $compile){

var cachedCityPolygonTree = rbush();
var cachedRegion ;
var cacheSize = 0;
var insertedTreeIDs = new Set();
var cacheThreshold = 7500;//hard limit
var targetDeleteCount = 0;
var deletedPolygonCount = 0;
var preFetchDistance = 25;
var previousRequestsData = [];
var movementHistory = [];
var noOfHistories = 5;
var previousRequestCentroid;
var cacheHitCount = 0;
var cacheMissCount = 0;
var currentReqMBR;
var Hit = false;
var currentReqCentroid;
var weights = [0.2,0.3,0.4,0.5,0.8,1.0];
var requestCounts = 0;
var ratio ;
var user_direction;
var displayCacheRegion;
var evictCount = 0;
var RequestPolygonWithPrefetch;
var cacheResetCount;
var currentRequestPolygon;
/* Map controller calls this function and this function checks whether a requested region is present in the cache or not. If not,
it gets the requested region data from the middleware.*/
this.getCityPolygonsFromCache = function city(rteBounds){

   var deferred = new $.Deferred();
   var data_response;

  var bounds = rteBounds.split("/");
  var bounds_northEast_lat = parseFloat(bounds[1]);
  var bounds_southWest_lat = parseFloat(bounds[2]);
  var bounds_northEast_lng = parseFloat(bounds[3]);
  var bounds_southWest_lng = parseFloat(bounds[4]);
  currentRequestPolygon = turf.polygon([[
      [bounds_northEast_lng,bounds_northEast_lat],
      [bounds_northEast_lng,bounds_southWest_lat],
      [bounds_southWest_lng,bounds_southWest_lat],
      [bounds_southWest_lng,bounds_northEast_lat],
      [bounds_northEast_lng,bounds_northEast_lat]
  ]]);

    requestCounts += 1;


    var bbox = turf.bbox(currentRequestPolygon);
    var extraBounds;
   currentReqMBR = bbox;
    // to search in Rbush Tree ,we need the MBR of the requested region.
   var item = {
       minX : bbox[0],
       minY : bbox[1],
       maxX : bbox[2],
       maxY : bbox[3]
   }


   if(typeof cachedRegion != "undefined" && typeof turf.difference(currentRequestPolygon,cachedRegion) == "undefined") {
          //cache HIT

         var result = cachedCityPolygonTree.search(item);
         data_response = turf.featureCollection(result);
         cacheHitCount += 1;
         Hit = true;
         RequestPolygonWithPrefetch = currentRequestPolygon;
         deferred.resolve(data_response);
         return deferred.promise();

   }else{
               //cache MISS
            Hit = false;
            var centroidRequestPoly = turf.centroid(currentRequestPolygon);

             prefetch(centroidRequestPoly,bbox).done(function(newMBR){
                 //console.log("new bbox",newMBR,"and ",requestCounts);
                 RequestPolygonWithPrefetch = turf.bboxPolygon(newMBR);
                 extraBounds = "city/"+newMBR[3]+"/"+newMBR[1]+"/"+newMBR[2]+"/"+newMBR[0];
                 $http.get(extraBounds).success(function(data) {

                     insertIntoTree(data.features,RequestPolygonWithPrefetch).done(function(){

                         data_response = data;

                         if(cachedRegion == undefined)
                         {cachedRegion = currentRequestPolygon;
                             displayCacheRegion = cachedRegion;}
                         else {
                             displayCacheRegion = cachedRegion;
                             cachedRegion = turf.union(RequestPolygonWithPrefetch,cachedRegion);
                         }
                         previousRequestCentroid = centroidRequestPoly;
                         var previousRequest = {};
                         previousRequest["x"] = centroidRequestPoly["geometry"]["coordinates"][0] ;
                         previousRequest["y"] = centroidRequestPoly["geometry"]["coordinates"][1] ;
                         previousRequestsData.push(previousRequest);
                         currentReqCentroid = previousRequest;
                         cacheMissCount += 1;
                         deferred.resolve(data_response);
                     });
                 }).error(function(data) {
                     console.error("Load city data failure");
                 });
             });

             return deferred.promise();
          }
}

/*Moved Right is true,Moved left is false
Moved Up is true,Moved Down is false*/

function getAngle(pt1,pt2){

   var  x1 = pt1["geometry"]["coordinates"][0];
   var  x2 = pt2["geometry"]["coordinates"][0];
   var y1 =  pt1["geometry"]["coordinates"][1];
   var y2 =  pt2["geometry"]["coordinates"][1];
   var angle = Math.atan2(y2-y1,x2-x1)*180/Math.PI;
   ratio = (y2-y1)/(x2-x1);
   console.log("angle",angle);
   return angle;
}





function prefetch(currentRequestCentroid,curReqBBox){
    var deferred = new $.Deferred();
    if(typeof cachedRegion == "undefined" || typeof previousRequestCentroid == "undefined")
    {
        //console.log("preFtech",curReqBBox);
        deferred.resolve(curReqBBox);
        return deferred.promise();
    }

    //console.log(previousRequestCentroid,"centroid");
    var x1 = currentRequestCentroid["geometry"]["coordinates"][0];
    var y1 = currentRequestCentroid["geometry"]["coordinates"][1];
    var angle = getAngle(previousRequestCentroid,currentRequestCentroid);
    var y2,x2;

    //Finding Intersection of Trend Line and Request Polygon
    if(angle>45 && angle<=135){
        //line1-TOP
         y2 = curReqBBox[3];
         x2 = ((y2-y1)/ratio)+x1;

    }
    else if(angle>-45 && angle<=45){
        //line2-RIGHT
         x2 = curReqBBox[2];
         y2 = ratio*(x2-x1)+y1;

    }
    else if(angle>=-135 && angle<=-45){
        //line3-DOWN
         y2 = curReqBBox[1];
         x2 = ((y2-y1)/ratio)+x1;

    }
    else if((angle>=135 && angle<=180) || (angle>=-180 && angle<=-135)){
        //line4-LEFT
         x2 = curReqBBox[0];
         y2 = ratio*(x2-x1)+y1;

    }
    //Intersected Point
    var edgePoint = turf.point([x2,y2]);
    var bearing = turf.bearing(previousRequestCentroid,currentRequestCentroid);
    var units = 'miles';
    //New Point after prefetch Distance
    var newPoint = turf.destination(edgePoint,preFetchDistance,bearing,units);


    var newX = newPoint["geometry"]["coordinates"][0];
    var newY = newPoint["geometry"]["coordinates"][1];
    var deltaX = x2-newX;
    var deltaY = y2-newY;
    var maxDelta = Math.max(Math.abs(deltaX),Math.abs(deltaY));

    var newbbox;
    var minX = curReqBBox[0];
    var minY = curReqBBox[1];
    var maxX = curReqBBox[2];
    var maxY = curReqBBox[3];
    //Finding the new MBR
    if(angle>0 && angle<=90){
        //NE

        var newmaxX = maxX + maxDelta;
        var newmaxY = maxY + maxDelta;
        newbbox = [minX,minY,newmaxX,newmaxY];
        user_direction = "NE";

    }else if(angle>-180 && angle<=-90){
        //SW

        var newminX = minX - maxDelta;
        var newminY = minY - maxDelta;
        newbbox = [newminX,newminY,maxX,maxY];
        user_direction = "SW";

    }else if(angle>-90 && angle<=0){
        //SE

        var newmaxX = maxX+maxDelta;
        var newminY = minY-maxDelta;
        newbbox = [minX,newminY,newmaxX,maxY];
        user_direction = "SE";

    }else if( angle>90 && angle<=180) {
        //NW

        var newminX = minX - maxDelta;
        var newmaxY = maxY + maxDelta;
        if(newminX>minX){
            newminX = minX;
            console.log("faultX");
        }
        if(newmaxY<maxY){
            newmaxY = maxY;
            console.log("faultY");
        }
        newbbox = [newminX, minY, maxX, newmaxY];
        user_direction = "NW";

    }
    deferred.resolve(newbbox);

    return deferred.promise();
}

//to insert polygons into the Rtree,features are the city polygons
var insertIntoTree = function insertIntoTree(features,currentRequest){

   var deferred = new $.Deferred();
   var nodes = [];
   var treeID;

   for(var id in features){
       var box = turf.bbox(features[id]);
       features[id].minX = box[0];
       features[id].minY = box[1];
       features[id].maxX = box[2];
       features[id].maxY = box[3];
       features[id].properties["centerLog"] = (features[id].maxX + features[id].minX) / 2;
       features[id].properties["centerLat"] = (features[id].maxY + features[id].minY) / 2;
       treeID = box[0]+"/"+box[1]+"/"+box[2]+"/"+box[3];
       if( insertedTreeIDs.has(treeID) == false){
               nodes.push(features[id]);
               insertedTreeIDs.add(treeID);
       }
   }
   //Checking Cache Overflow ,occurs when current polygons(nodes) to get inserted plus cachesize is greater than Cache Threshold
   if((cacheSize+nodes.length) >= cacheThreshold){

         targetDeleteCount = (cacheSize+nodes.length) - cacheThreshold;
         evict(currentRequest).done(function(){

               cachedCityPolygonTree.load(nodes);
               cacheSize += nodes.length;
          });
      deferred.resolve();
      evictCount += 1;
      return deferred.promise();

   }else{
           cacheSize += nodes.length;
           cachedCityPolygonTree.load(nodes);
           deferred.resolve();
           return deferred.promise();
   }
}

//Determines which part of the cached region needs to be evicted to reduce the cached city polygons to ensure staying within the cache budget.
var evict = function Evict(currentRequest){

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
   //Checks which part of requested region is overlapped with the cached region.
   var UpperRight = turf.inside(turf.point([R_maxX,R_maxY]),cachedRegionMBR);
   var UpperLeft  = turf.inside(turf.point([R_minX,R_maxY]),cachedRegionMBR);
   var LowerLeft  = turf.inside(turf.point([R_minX,R_minY]),cachedRegionMBR);
   var LowerRight = turf.inside(turf.point([R_maxY,R_minY]),cachedRegionMBR);


   if(LowerRight || LowerLeft && !UpperRight && !UpperLeft){
        //Y from bottom to top

        cutRegion(C_minX,C_minY,C_maxX,R_minY,user_direction) .done(function(){
                deferred.resolve();
        }).fail(function(){
                 ////console.log("Y from bottom to top order minX,minY,maxX,maxY: ",R_minX,R_minY,R_maxX,R_maxY);
                 clearCache().done();
                 deferred.resolve();
        })
        return deferred.promise();
   }
   else if(UpperRight || UpperLeft && !LowerLeft && !LowerRight){

        cutRegion(C_minX,R_maxY,C_maxX,C_maxY,user_direction).done(function(){
        //Y from top to bottom

               deferred.resolve();
        }).fail(function(){
                ////console.log("Y from top to bottom order minX,minY,maxX,maxY: ",R_minX,R_minY,R_maxX,R_maxY);
                clearCache().done();
                deferred.resolve();
        })
        return deferred.promise();

   }
   else if(UpperRight && LowerRight){
       //X from left to right

        cutRegion(R_maxX,C_minY,C_maxX,C_maxY,user_direction).done(function(){

               deferred.resolve();
        }).fail(function(){
                ////console.log("X from left to right order minX,minY,maxX,maxY: ",R_minX,R_minY,R_maxX,R_maxY);
                clearCache().done();
                deferred.resolve();
        })
        return deferred.promise();

   }else if(UpperLeft && LowerLeft){
       //X from right to left

        cutRegion(C_minX,C_minY,R_minX,C_maxY,user_direction).done(function(){

                           deferred.resolve();
        }).fail(function(){
                ////console.log("X from left to right order minX,minY,maxX,maxY: ",R_minX,R_minY,R_maxX,R_maxY);
                clearCache().done();
                deferred.resolve();
        })
        return deferred.promise();

   }else if(!LowerRight &&   !LowerLeft && !UpperLeft && !UpperRight){
          //NO Overlap"
          cutRegion(C_minX,C_minY,C_maxX,C_maxY,user_direction).done(function(){


                 deferred.resolve();
          }).fail(function(){
                  ////console.log("NO OVERLAP .PROBLEM!");
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
var cutRegion =  function findCornerofEviction(minX,minY,maxX,maxY,Direction){

        var deferred = new $.Deferred();
        var Xline,Yline;
        if(Direction == "NE"){
            Xline = turf.lineString([[minX,minY],[maxX,minY]]);
            Yline = turf.lineString([[minX,minY],[minX,maxY]]);
        }
        if(Direction == "SW"){
            Xline =  turf.lineString([[maxX,maxY],[minX,maxY]]);
            Yline =  turf.lineString([[maxX,maxY],[maxX,minY]]);
        }
        if(Direction == "NW"){
            Xline = turf.lineString([[maxX,minY],[minX,minY]]);
            Yline = turf.lineString([[maxX,minY],[maxX,maxY]]);
        }
        if(Direction == "SE"){
            Xline =  turf.lineString([[minX,maxY],[maxX,maxY]]);
            Yline =  turf.lineString([[minX,maxY],[minX,minY]]);
        }
        var distanceX = turf.lineDistance(Xline, 'miles');
        var distanceY = turf.lineDistance(Yline,'miles');
        //Make 10 slices
        var ShiftX = distanceX/10;
        var ShiftY = distanceY/10;
        var start = 0;
        var stop= distanceX;
        var removeItems;
        var slicedXline,slicedYline,cutPointX,cutPointY,cutBbox,remove_search;
        var moveX = ShiftX;
        var moveY = ShiftY;
        var ti = performance.now();
        var count = 0;
        while(deletedPolygonCount<targetDeleteCount){


            slicedXline = turf.lineSliceAlong(Xline, start, moveX, 'miles');
            slicedYline = turf.lineSliceAlong(Yline, start, moveY, 'miles');
            cutPointX = slicedXline["geometry"]["coordinates"][1][0];
            cutPointY = slicedYline["geometry"]["coordinates"][1][1];
            if(Direction == "NE"){
                cutBbox = [minX,minY,cutPointX,cutPointY];
            }
            if(Direction == "SW"){
                cutBbox = [cutPointX,cutPointY,maxX,maxY];
            }
            if(Direction == "NW"){
                cutBbox = [cutPointX,minY,maxX,cutPointY];
            }
            if(Direction == "SE"){
                cutBbox = [minX,cutPointY,cutPointX,maxY];
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

              if(moveX>stop)
              {break;}
        }
        ////console.log("time:",performance.now()-ti,"count:",count);
        deletion(removeItems).done(function(){
                //Delete is complete
                //console.log("Polygon search",remove_search);
                var PolygonRegionRemovedFromCache = turf.bboxPolygon(cutBbox);
                //console.log("remove",PolygonRegionRemovedFromCache);
                cacheSize -= deletedPolygonCount;
                //console.log("before delete:",cachedRegion,"removing ",PolygonRegionRemovedFromCache);

                cachedRegion = turf.difference(cachedRegion,PolygonRegionRemovedFromCache);
                //console.log("after Delete:",cachedRegion);
                targetDeleteCount -= deletedPolygonCount;
                deletedPolygonCount = 0;
        });

        if(targetDeleteCount>0){

             //UnSucessFul Delete
              ////console.log("Cache Size:",cacheSize);
              ////console.log("Deleted Count:",deletedPolygonCount);
              ////console.log("Target",targetDeleteCount);
              ////console.log("removed region",remove_search);

              deferred.reject();
              return deferred.promise();
        }else{
              // SucessFul Delete
              deferred.resolve();
              return deferred.promise();
        }
}

// deleting polygons from Rtree
var deletion = function deleteNodesfromTree(removeItems){
  var deferred = new $.Deferred();
  for (var i = 0;i<removeItems.length;i++)
        cachedCityPolygonTree.remove(removeItems[i]);

  deferred.resolve();
  return deferred.promise();
}

var clearCache = function remove(){
    var deferred = new $.Deferred();
    cachedRegion = undefined ;
    cacheSize = 0;
    cachedCityPolygonTree.clear();
    targetDeleteCount = 0;
    deletedPolygonCount = 0;
    insertedTreeIDs.clear();
    deferred.resolve();
    return deferred.promise();
}

this.getCacheSize = function getSize(){
    return cacheSize;
}
this.getCacheHitCount = function getHit(){
        return cacheHitCount;
}
this.getCacheMissCount = function getMiss(){
    return cacheMissCount;
}

this.whetherOverflow = function(){
    return (cacheThreshold>cacheSize);
}

this.getRequest = function(){
    return currentReqMBR;
}

this.getHitorMiss= function(){
    return Hit;
}

this.getRequestCentroid = function(){
    return currentReqCentroid;
}

this.getCachedRegion = function(){
    if(Hit)
        return cachedRegion;
    else
        return displayCacheRegion;
}

this.getRequestsCount = function(){
    return requestCounts;
}

this.getCacheThreshold = function(){
    return cacheThreshold;
}

this.getEvictCount = function(){
    return evictCount;
}

this.getPrefetchedRegion = function(){
    return  RequestPolygonWithPrefetch;
}

this.getCurrentRequest = function(){
    return currentRequestPolygon;
}

this.getCache = function(){
    if(typeof cachedRegion != "undefined") {
        var bbox = turf.bbox(cachedRegion);
        console.log(bbox);
        return bbox;
    }
    else
    return;
}
this.getCachePolygons = function(){
    var cityPolygons = cachedCityPolygonTree.all();
    console.log(cityPolygons);
    var city_data = turf.featureCollection(cityPolygons);
    console.log(city_data);
    return city_data;
}
})

