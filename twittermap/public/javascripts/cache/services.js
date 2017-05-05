 angular.module('cloudberry.cache', ['leaflet-directive', 'cloudberry.common' ])
 .service('Cache', function( $window, $http, $compile){

  var cachedCityPolygonTree = rbush();
  var cachedRegion ;
  var cacheSize = 0;
  var insertedTreeIDs = new Set();
  var cacheThreshold = 7500;//hard limit
  var DeleteTarget = 0;
  var DeletedCount = 0;
  var preFetchDistance = 25;


  /*Call will happen from map controller and this functions sees whether a required data is present in cache or not
  if not gets from midleware*/
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
        // to search in Rbush Tree we need this in this format
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

              var buffered = turf.buffer(currentRequestPolygon, preFetchDistance , 'miles');
              var bboxBuffer = turf.bbox(buffered);
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
//to insert polygons into the Rtree
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
                   cacheSize = cacheSize + 1;

           }

       }

       if(cacheSize >= cacheThreshold){

             DeleteTarget = cacheSize - cacheThreshold;
             evict(currentRequest).done(function(){

                   cachedCityPolygonTree.load(nodes);
                   cacheSize = nodes.length;
                   console.log(" Size:",cacheSize);
              });
          deferred.resolve();
          return deferred.promise();

       }else{
               cachedCityPolygonTree.load(nodes);
               console.log(" Size:",cacheSize);
               deferred.resolve();
               return deferred.promise();
       }

 }

//X = true Y = false
//TopTo Bottom = true
//BottomToTop = false
//LeftToRight = True
//RightToLeft = False
//determine which region of cached region to cut to satisfy Target,if first region couldn't satisfy the Target go to new region
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

       var CacheMBR = turf.bboxPolygon(cache_bbox);
       //Four Corners of Request to see which is inside cache_region
       var UpperRight = turf.inside(turf.point([R_maxX,R_maxY]),CacheMBR);
       var UpperLeft  = turf.inside(turf.point([R_minX,R_maxY]),CacheMBR);
       var LowerLeft  = turf.inside(turf.point([R_minX,R_minY]),CacheMBR);
       var LowerRight = turf.inside(turf.point([R_maxY,R_minY]),CacheMBR);


       if(LowerRight || LowerLeft && !UpperRight && !UpperLeft){
            //Y from bottom to top
            cutRegion(C_minX,C_minY,C_maxX,R_minY,false,true) .done(function(){
                    deferred.resolve();
            }).fail(function(){
//                     cacheThreshold += DeleteTarget;
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
//                    cacheThreshold += DeleteTarget;
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
//                    cacheThreshold += DeleteTarget;
                    clearCache().done();
                    deferred.resolve();
            })
            return deferred.promise();

       }else if(UpperLeft && LowerLeft){
            cutRegion(C_minX,C_minY,R_minX,C_maxY,true,false).done(function(){
            //X from right to left
                   deferred.resolve();
            }).fail(function(){
//                    cacheThreshold += DeleteTarget;
                    clearCache().done();
                    deferred.resolve();
            })
            return deferred.promise();

       }else if(!LowerRight &&   !LowerLeft && !UpperLeft && !UpperRight){

              cutRegion(C_minX,C_minY,C_maxX,C_maxY,true,true).done(function(){
              //NO Overlap
                     deferred.resolve();
              }).fail(function(){
//                      cacheThreshold += DeleteTarget;
                      clearCache().done();
                      deferred.resolve();
              })
              return deferred.promise();
       }
 }
//sees whether cutting the region can satisfy the target ,If not sends a failed message to evict function
//X = true Y = false
//TopTo Bottom = true
//BottomToTop = false
//LeftToRight = true
//RightToLeft = false
 var cutRegion =  function findCornerofEviction(minX,minY,maxX,maxY,XorY,Direction){

            var deferred = new $.Deferred();
            var line;
            if(XorY){
                if(Direction){
                    //Left to Right
                    line = turf.lineString([[minX,maxY],[maxX,maxY]]);
                }else{
                    //Right to Left
                    line = turf.lineString([[maxX,maxY],[minX,maxY]]);
                }
            }else{
                if(Direction){
                    //TopToBottom
                    line = turf.lineString([[maxX,maxY],[maxX,minY]]);
                }else{
                    //BottomToTop
                    line = turf.lineString([[maxX,minY],[maxX,maxY]]);
                }
            }

            var distance = turf.lineDistance(line, 'miles');
            var Move = distance/10;
            var start = 0;
            var stop= distance;
            var removeItems;
            var sliced,cutPoint,cutBbox,remove_search;

            while(DeletedCount<DeleteTarget){

                  sliced = turf.lineSliceAlong(line, start, Move, 'miles');
                  if(XorY){
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
                  DeletedCount = removeItems.length;
                  Move += Move;
                  if(Move>stop)
                  {break;}
            }

            deletion(removeItems).done(function(){
                    //Delete is complete
                    var PolygonRegionRemovedFromCache = turf.bboxPolygon(remove_search);
                    cacheSize -= DeletedCount;
                    cachedRegion = turf.difference(cachedRegion,PolygonRegionRemovedFromCache);
                    DeleteTarget -= DeletedCount;
                    DeletedCount = 0;
            });

            if(DeleteTarget>0){

                 //UnSucessFul Delete
                  deferred.reject();
                  return deferred.promise();
            }else{
                  // SucessFul Delete
                  deferred.resolve();
                  return deferred.promise();
            }
 }

//wher deletion from tree occurs
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
    DeleteTarget = 0;
    DeletedCount = 0;
    insertedTreeIDs.clear();
    deferred.resolve();
    console.log("cache_cleared");
    return deferred.promise();
}



})

