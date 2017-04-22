 angular.module('cloudberry.cache', ['leaflet-directive', 'cloudberry.common' ])
 .service('Cache', function( $window, $http, $compile, Asterix){


  var cachedCityPolygonTree = rbush();
  var cachedRegion ;
  var cacheSize = 0;
  var insertedTreeIDs = new Set();
  var cacheThreshold = 5500;//soft limit
  var DeleteTarget = 0;
  var DeletedCount = 0;
  var preFetchDistance = 25;



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


       if(cachedRegion != undefined && turf.difference(currentRequestPolygon,cachedRegion) == undefined) {
              //cache HIT
//              var t0 = performance.now();
             var result = cachedCityPolygonTree.search(item);
             data_response = turf.featureCollection(result);
             deferred.resolve(data_response);
//              console.log("Cache HIT took",performance.now()-t0);
             return deferred.promise();

       }else{
                   //cache MISS
//               var MISSt0 = performance.now();
              var buffered = turf.buffer(currentRequestPolygon, preFetchDistance , 'miles');
              var bboxBuffer = turf.bbox(buffered);
                   //Pre Fetch
              var rteExtends = "city/" + bboxBuffer[3] + "/" + bboxBuffer[1] + "/" + bboxBuffer[2] + "/" + bboxBuffer[0];

              $http.get(rteExtends).success(function(data) {

                      var result_set = insertIntoTree(data.features,currentRequestPolygon).done(function(){
                               console.log("done");
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
//               console.log("Cache MISS took",performance.now()-MISSt0);
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
             evict(currentRequest).then(function(){
                   cachedCityPolygonTree.load(nodes);

              });
          deferred.resolve();
          return deferred.promise();

       }else{
               cachedCityPolygonTree.load(nodes);
               deferred.resolve();
               return deferred.promise();
       }
  
 }


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

       //Four Corners of Request to see which is inside cache_region
       var UpperRightCorner = turf.point([R_maxX,R_maxY]);
       var UpperLeftCorner  = turf.point([R_minX,R_maxY]);
       var LowerLeftCorner  = turf.point([R_minX,R_minY]);
       var LowerRightCorner = turf.point([R_maxY,R_minY]);

//       console.log("evict function called");
       var CacheMBR = turf.bboxPolygon(cache_bbox);
       if(turf.inside(LowerLeftCorner,CacheMBR)&&turf.inside(LowerRightCorner,CacheMBR)){

           cutRegion(R_maxX,C_minY,C_maxX,C_maxY).done(function(){
                    deferred.resolve();
            }).fail(function(){
                        cutRegion(C_minX,C_minY,R_minX,C_maxY).done(function(){
                            deferred.resolve();
                        }).fail(function(){
                              cutRegion(R_minX,C_minY,R_maxY,R_minY).done(function(){
                                deferred.resolve();
                              }).fail(function(){
                                  cacheThreshold += DeleteTarget;
                                  deferred.resolve();
                              })
                       })
            });
             return deferred.promise();

       }else if(turf.inside(LowerLeftCorner,CacheMBR) && !turf.inside(LowerRightCorner,CacheMBR) && !turf.inside(UpperRightCorner,CacheMBR) &&!turf.inside(UpperLeftCorner,CacheMBR)){

            cutRegion(C_minX,C_minY,R_minX,C_maxY).done(function(){
                        deferred.resolve();
            }).fail(function(){
                        cutRegion(R_minX,C_minY,C_maxX,R_minY).done(function(){
                            deferred.resolve();
                        }).fail(function(){
                                cacheThreshold += DeleteTarget;
                                deferred.resolve();
                        })
             });
             return deferred.promise();

       }else if(turf.inside(LowerLeftCorner,CacheMBR) &&    turf.inside(UpperLeftCorner,CacheMBR) ){

           cutRegion(C_minX,C_minY,R_minX,C_maxY).done(function(){
                  deferred.resolve();
           }).fail(function(){

              cutRegion(R_minX,R_maxY,C_maxX,C_maxY).done(function(){
                       deferred.resolve();
                   }).fail(function(){
                                cutRegion(R_minX,C_minY,C_maxX,R_maxY).done(function(){
                                        deferred.resolve();
                                }).fail(function(){
                                    cacheThreshold += DeleteTarget;
                                    deferred.resolve();
                               })
                   })
           });
           return deferred.promise();

       }else if(turf.inside(UpperLeftCorner,CacheMBR) && !turf.inside(UpperRightCorner,CacheMBR) && !turf.inside(LowerLeftCorner ,CacheMBR) && !turf.inside(LowerRightCorner,CacheMBR)){

            cutRegion(C_minX,C_minY,R_minX,C_maxY).done(function(){

                deferred.resolve();

            }).fail(function(){
                   cutRegion(R_minX,R_maxY,C_maxX,C_maxY).done(function(){
                          deferred.resolve();
                      }).fail(function(){
                          cacheThreshold += DeleteTarget;
                          deferred.resolve();
                      })
             });
            return deferred.promise();

       }else if(turf.inside(UpperLeftCorner,CacheMBR) && turf.inside(UpperRightCorner,CacheMBR)){

               cutRegion(C_minX,C_minY,R_minX,R_maxY).done(function(){
                     deferred.resolve();
               }).fail(function(){
                     cutRegion(R_maxX,C_minY,C_maxX,C_maxY).done(function(){
                               deferred.resolve();
                     }).fail(function(){
                                     cutRegion(R_minX,R_maxY,R_maxX,C_maxY).done(function(){
                                          deferred.resolve();
                                     }).fail(function(){
                                          cacheThreshold += DeleteTarget;
                                          deferred.resolve();
                                     })
                     })
               });
               return deferred.promise();

       }else if(turf.inside(UpperRightCorner,CacheMBR) && !turf.inside(LowerLeftCorner,CacheMBR) && !turf.inside(UpperLeftCorner,CacheMBR) && !turf.inside(LowerRightCorner,CacheMBR)){

               cutRegion(R_maxX,C_minY,C_maxX,C_maxY).done(function(){
                          deferred.resolve();
               }).fail(function(){
                              cutRegion(C_minX,R_maxY,R_maxX,C_maxY).done(function(){
                                      deferred.resolve();
                              }).fail(function(){
                                  cacheThreshold += DeleteTarget;
                                  deferred.resolve();
                          })
               });
               return deferred.promise();

       }else if(turf.inside(UpperRightCorner,CacheMBR) &&   turf.inside(LowerRightCorner,CacheMBR)){

             cutRegion(R_maxX,C_minY,C_maxX,C_maxY).done(function(){
                          deferred.resolve();
             }).fail(function(){

                          cutRegion(C_minX,R_maxY,R_maxX,C_maxY).done(function(){
                                   deferred.resolve();
                          }).fail(function(){
                                  cutRegion(C_minX,C_minY,R_maxX,R_minY).done(function(){
                                           deferred.resolve();
                                  }).fail(function(){
                                           cacheThreshold += DeleteTarget;
                                           deferred.resolve();
                                  })
                          })
             });
             return deferred.promise();

       }else if(turf.inside(LowerRightCorner,CacheMBR) &&   !turf.inside(LowerLeftCorner,CacheMBR) && !turf.inside(UpperLeftCorner,CacheMBR) && !turf.inside(UpperRightCorner,CacheMBR)){


               cutRegion(R_maxX,C_minY,C_maxX,C_maxY).done(function(){
                       deferred.resolve();
               }).fail(function(){
                          cutRegion(C_minX,C_minY,R_maxX,R_minY).done(function(){
                              deferred.resolve();

                          }).fail(function(){
                              cacheThreshold += DeleteTarget;
                              deferred.resolve();
                          })
               });
               return deferred.promise();

       }else if(!turf.inside(LowerRightCorner,CacheMBR) &&   !turf.inside(LowerLeftCorner,CacheMBR) && !turf.inside(UpperLeftCorner,CacheMBR) && !turf.inside(UpperRightCorner,CacheMBR)){

              cutRegion(C_minX,C_minY,C_maxX,C_maxY).done(function(){
                     deferred.resolve();
              }).fail(function(){
                      cacheThreshold += DeleteTarget;
                      deferred.resolve();
              })
              return deferred.promise();
       }else{
           deferred.resolve();
           return deferred.promise();
       }
 }
//sees whether cutting the region can satisfy the target ,If not sends a failed message to evict function
 var cutRegion =  function findCornerofEviction(minX,minY,maxX,maxY){
            var deferred = new $.Deferred();
            var line = turf.lineString([[minX,maxY],[maxX,maxY]]);
            var distance = turf.lineDistance(line, 'miles');
            var Move = distance/10;
            var start = 0;
            var stop= distance;
            var removeItems;
            var sliced,cutPoint,cutBbox,remove_search;

            while(DeletedCount<DeleteTarget){

                  sliced = turf.lineSliceAlong(line, start, Move, 'miles');
                  cutPoint = sliced["geometry"]["coordinates"][1][0];
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
//                    console.log("Delete is complete");
                    var PolygonRegionRemovedFromCache = turf.bboxPolygon(remove_search);
                    cacheSize -= DeletedCount;
                    cachedRegion = turf.difference(cachedRegion,PolygonRegionRemovedFromCache);
                    DeleteTarget -= DeletedCount;
                    DeletedCount = 0;
            });

            if(DeleteTarget>0){

//                  console.log("UnSucessFul Delete");
                  deferred.reject();
                  return deferred.promise();
            }else{
//                  console.log("SucessFul Delete");
                  deferred.resolve();
                  return deferred.promise();
            }
 }
//wher real deletion from tree occurs
 var deletion = function deleteNodesfromTree(removeItems){
      var deferred = new $.Deferred();
      for (var i = 0;i<removeItems.length;i++)
            cachedCityPolygonTree.remove(removeItems[i]);

      deferred.resolve();
      return deferred.promise();
  }




})
