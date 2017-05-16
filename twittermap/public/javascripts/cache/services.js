
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
  var preFetchDistance = 25;//  Radius distance from corners of request


 /* Map controller calls this function and this function checks whether a requested region is present in the cache or not. If not,
 it gets the requested region data from the middleware.*/
  this.getCityPolygonsFromCache = function city(bounds){

       var deferred = new $.Deferred();
       var data_response;


       var rteBounds = "city/" + bounds._northEast.lat + "/" + bounds._southWest.lat + "/" + bounds._northEast.lng + "/" + bounds._southWest.lng;

       var currentRequestPolygon = turf.polygon([[
                                    [bounds._northEast.lng,bounds._northEast.lat],
                                    [bounds._northEast.lng,bounds._southWest.lat],
                                    [bounds._southWest.lng,bounds._southWest.lat],
                                    [bounds._southWest.lng,bounds._northEast.lat],
                                    [bounds._northEast.lng,bounds._northEast.lat]
                                ]]);

       var bbox = turf.bbox(currentRequestPolygon);
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
             deferred.resolve(data_response);

             return deferred.promise();

       }else{
                   //cache MISS

              var prefetchAdditionalRegion = turf.buffer(currentRequestPolygon, preFetchDistance , 'miles');
              var bboxBuffer = turf.bbox(prefetchAdditionalRegion);
                   //Pre Fetch
              var rteExtends = "city/" + bboxBuffer[3] + "/" + bboxBuffer[1] + "/" + bboxBuffer[2] + "/" + bboxBuffer[0];

              $http.get(rteExtends).success(function(data) {

                               var result_set = insertIntoTree(data.features,currentRequestPolygon).done(function(){

                                                var extendPoly = turf.bboxPolygon(bboxBuffer);

                                                if(cachedRegion == undefined)
                                                    {cachedRegion = extendPoly;}
                                                else
                                                    {cachedRegion = turf.union(extendPoly,cachedRegion);}

                                                var result = cachedCityPolygonTree.search(item);
                                                data_response = turf.featureCollection(result);

                                                deferred.resolve(data_response);


                      });
              }).error(function(data) {
                                    console.error("Load city data failure");
              });
              return deferred.promise();
       }
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
          return deferred.promise();

       }else{
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
            cutRegion(C_minX,C_minY,C_maxX,R_minY,false,true) .done(function(){
                    deferred.resolve();
            }).fail(function(){

                     clearCache().done();
                     deferred.resolve();
            })
            return deferred.promise();
       }
       else if(UpperRight || UpperLeft && !LowerLeft && !LowerRight){
            cutRegion(C_minX,R_maxY,C_maxX,C_maxY,false,false).done(function(){
            //Y from top to bottom
                   deferred.resolve();
            }).fail(function(){

                    clearCache().done();
                    deferred.resolve();
            })
            return deferred.promise();

       }
       else if(UpperRight && LowerRight){
            cutRegion(R_maxX,C_minY,C_maxX,C_maxY,true,true).done(function(){
            //X from left to right
                   deferred.resolve();
            }).fail(function(){

                    clearCache().done();
                    deferred.resolve();
            })
            return deferred.promise();

       }else if(UpperLeft && LowerLeft){
            cutRegion(C_minX,C_minY,R_minX,C_maxY,true,false).done(function(){
            //X from right to left
                   deferred.resolve();
            }).fail(function(){

                    clearCache().done();
                    deferred.resolve();
            })
            return deferred.promise();

       }else if(!LowerRight &&   !LowerLeft && !UpperLeft && !UpperRight){

              cutRegion(C_minX,C_minY,C_maxX,C_maxY,true,true).done(function(){
              //NO Overlap
                     deferred.resolve();
              }).fail(function(){

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
 var cutRegion =  function findCornerofEviction(minX,minY,maxX,maxY,isHorizontalEviction,Direction){

            var deferred = new $.Deferred();
            var line;
            if(isHorizontalEviction){
                if(Direction){
                    //Left to Right
                    line = turf.lineString([[minX,maxY],[maxX,maxY]]);
                }else{
                    //Right to Left
                    line = turf.lineString([[maxX,maxY],[minX,maxY]]);
                }
            }else{
                if(Direction){
                    //Top to Bottom
                    line = turf.lineString([[maxX,maxY],[maxX,minY]]);
                }else{
                    //Bottom toTop
                    line = turf.lineString([[maxX,minY],[maxX,maxY]]);
                }
            }

            var distance = turf.lineDistance(line, 'miles');
            //Make 10 slices
            var Move = distance/10;
            var start = 0;
            var stop= distance;
            var removeItems;
            var sliced,cutPoint,cutBbox,remove_search;

            while(deletedPolygonCount<targetDeleteCount){

                  sliced = turf.lineSliceAlong(line, start, Move, 'miles');
                  if(isHorizontalEviction){
                  cutPoint = sliced["geometry"]["coordinates"][1][0];
                  }else{
                  cutPoint = sliced["geometry"]["coordinates"][1][1];
                  }

                  cutBbox = [minX,minY,cutPoint,maxY];
                  remove_search = {
                                        minX: cutBbox[0],
                                        minY: cutBbox[1],
                                        maxX: cutBbox[2],
                                        maxY: cutBbox[3]
                                    }
                  removeItems = cachedCityPolygonTree.search(remove_search);
                  deletedPolygonCount = removeItems.length;
                  Move += Move;
                  if(Move>stop)
                  {break;}
            }

            deletion(removeItems).done(function(){
                    //Delete is complete
                    var PolygonRegionRemovedFromCache = turf.bboxPolygon(remove_search);
                    cacheSize -= deletedPolygonCount;
                    cachedRegion = turf.difference(cachedRegion,PolygonRegionRemovedFromCache);
                    targetDeleteCount -= deletedPolygonCount;
                    deletedPolygonCount = 0;
            });

            if(targetDeleteCount>0){

                 //UnSucessFul Delete
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



})

