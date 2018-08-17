angular.module('cloudberry.map')
  .controller('countMapCtrl', function($scope, $compile, cloudberry, cloudberryConfig, MapResultCache,
                                       TimeSeriesCache, moduleManager, cloudberryClient, queryUtil) {

    // Array to store the data for chart
    $scope.chartData=[];
    // Map to store the chart data for every polygon
    $scope.ChartDataMap = new HashMap();
    // The popup window shown now
    $scope.popUp = null;

    // return difference of two arrays
    function arr_diff (a1, a2) {
      var a = [], diff = [];
      for (var i = 0; i < a1.length; i++) {
        a[a1[i]] = true;
      }
      for (var j = 0; j < a2.length; j++) {
        if (a[a2[j]]) {
          delete a[a2[j]];
        } else {
          a[a2[j]] = true;
        }
      }
      for (var k in a) {
        diff.push(k);
      }
      return diff;
    }

    // Concat two hashmap result
    function concatHashmap(newMap, cacheMap) {
      if (cacheMap.count()===0) return newMap;

      var concatMap = new HashMap();
      newMap.forEach(function(value, key){
        var concatValue = [];
        var cacheValue = cacheMap.get(key);
        if(value !== 0) {
          if (cacheValue === undefined) concatValue=value;
          else concatValue = value.concat(cacheValue)
        }
        else if (cacheValue !== undefined) concatValue=cacheValue;

        concatMap.set(key,concatValue);
      });
      return concatMap;
    }

    function getPopupContent() {
      // get chart data for the polygon
      var geoIDChartData = $scope.ChartDataMap.get($scope.selectedGeoID);
      (geoIDChartData)? $scope.chartData = $scope.preProcess(geoIDChartData) : $scope.chartData = [];

      // get the count info of polygon
      var placeName = $scope.selectedPlace.properties.name;
      var infoPromp = $scope.infoPromp;
      var countText = '0';
      var logicLevel = $scope.status.logicLevel;
      if($scope.selectedPlace.properties.countText) {
        countText = $scope.selectedPlace.properties.countText;
      }

      // Generate the html in pop up window
      var content;
      if($scope.chartData.length===0) {
        content = '<div id="popup-info" style="margin-bottom: 0">' +
          '<div id="popup-statename">'+logicLevel+': '+placeName+'</div>' +
          '<div id="popup-count" style="margin-bottom: 0">'+infoPromp+'<b> '+countText+'</b></div>' +
          '</div>'+
          "<canvas id=\"myChart\" height=\"0\" ></canvas>";
      }else {
        content = '<div id="popup-info">' +
          '<div id="popup-statename">'+logicLevel+': '+placeName+'</div>' +
          '<div id="popup-count">'+infoPromp+'<b> '+countText+'</b></div>' +
          '</div>'+
          "<canvas id=\"myChart\"></canvas>";
      }
      return content;
    }

    // Add the event for popup window: when mouse out, close the popup window
    function addPopupEvent() {
      document.getElementsByClassName("leaflet-popup")[0].onmouseout=function (e) {
        var target = e.relatedTarget;
        if(target && target.className.toString()==="[object SVGAnimatedString]") {
          $scope.map.closePopup();
        }
      };
    }

    // If there are chartData, draw the line chart
    function drawLineChart(){
      if($scope.chartData.length !== 0 && document.getElementById("myChart")) {
        var ctx = document.getElementById("myChart").getContext('2d');
        var myChart = new Chart(ctx, {
          type: 'line',
          data:{
            datasets:[{
              lineTension: 0,
              data:$scope.chartData,
              borderColor:"#3e95cd",
              borderWidth: 0.8,
              pointRadius: 1.5
            }]
          },

          options: {
            legend: {
              display: false
            },
            scales: {
              xAxes: [{
                type: 'time',
                time: {
                  unit:'month'
                },
                scaleLabel: {
                  display: true,
                  labelString: 'Date'
                }
              }],
              yAxes: [{
                scaleLabel: {
                  display: true,
                  labelString: 'Count'
                },
                ticks: {
                  beginAtZero: true,
                  suggestedMax: 4
                }
              }]
            }
          }
        });
      }
    }

    // redraw popup window after ChartdataMap is updated
    function redrawPopup(){
      if($scope.popUp && $scope.popUp._isOpen){
        $scope.popUp.setContent(getPopupContent());
        addPopupEvent();
        drawLineChart();
      }
    }

    // Convert the array in chartDataMap to count result by month, which can be read by chart.js
    $scope.preProcess = function (result) {
      // group by year
      groups = result.reduce(function (r, o) {
        var m = o.day.split(('-'))[0];
        (r[m])? r[m].data.push(o) : r[m] = {year: m, data: [o]};
        return r;
      }, {});
      var resultByYear = Object.keys(groups).map(function(k){ return groups[k]; });
      // sum up the result for every month
      var resultByMonth = [];
      var hasCountMonth = [];
      for (var i=0; i<resultByYear.length;i++){
        groups = resultByYear[i].data.reduce(function (r, o) {
          var m = o.day.split(('-'))[1];
          if (r[m]){
            r[m].y += o.count;
          }else{
            var thisMonth = new Date(resultByYear[i].year,m-1);
            r[m] = { y: o.count, x: thisMonth};
            hasCountMonth.push(thisMonth);
          }
          return r;
        }, {});
        var resultByMonthOneYear = Object.keys(groups).map(function(k){ return groups[k]; });
        resultByMonth = resultByMonth.concat(resultByMonthOneYear);
      }
      // add empty data point
      var zeroCountMonth = [];
      var minDate = cloudberry.parameters.timeInterval.start;
      var maxDate = cloudberry.parameters.timeInterval.end;
      for (var m = new Date(minDate.getFullYear(),minDate.getMonth());m <= new Date(maxDate.getFullYear(),maxDate.getMonth()); m.setMonth(m.getMonth()+1)){
        zeroCountMonth.push(new Date(m.getTime()));
      }
      zeroCountMonth = arr_diff(hasCountMonth,zeroCountMonth);
      for (var j = 0; j < zeroCountMonth.length; j++) {
        resultByMonth.push({x: new Date(zeroCountMonth[j]), y:0});
      }
      // sort the date
      resultByMonth.sort(function(a,b){
        return a.x - b.x;
      });
      return resultByMonth;
    };

    // set map styles for countmap
    function setCountMapStyle() {
      $scope.setStyles({
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
      });
    }

    // Send query to cloudberry
    function sendCountmapQuery() {
      // For time-series histogram, get geoIds not in the time series cache.
      $scope.geoIdsNotInTimeSeriesCache = TimeSeriesCache.getGeoIdsNotInCache(cloudberry.parameters.keywords,
        cloudberry.parameters.timeInterval, cloudberry.parameters.geoIds, cloudberry.parameters.geoLevel);

      // Batch request without map result - used when the complete map result cache hit, partial time series result cache hit case
      var batchWithoutGeoRequest = cloudberryConfig.querySliceMills > 0 ? {
        batch: [queryUtil.byTimeRequest(cloudberry.parameters, $scope.geoIdsNotInTimeSeriesCache)],
        option: {
          sliceMillis: cloudberryConfig.querySliceMills
        }
      } : {
        batch: [queryUtil.byTimeRequest(cloudberry.parameters, $scope.geoIdsNotInTimeSeriesCache)]
      };

      // Gets the Geo IDs that are not in the map result cache.
      $scope.geoIdsNotInCache = MapResultCache.getGeoIdsNotInCache(cloudberry.parameters.keywords,
        cloudberry.parameters.timeInterval,
        cloudberry.parameters.geoIds, cloudberry.parameters.geoLevel);

      // Batch request without time series result - used when the complete time series cache hit, partial map result cache hit case
      var batchWithoutTimeRequest = cloudberryConfig.querySliceMills > 0 ? {
        batch: [queryUtil.byGeoRequest(cloudberry.parameters, $scope.geoIdsNotInCache)],
        option: {
          sliceMillis: cloudberryConfig.querySliceMills
        }
      } : {
        batch: [queryUtil.byGeoRequest(cloudberry.parameters, $scope.geoIdsNotInCache)]
      };

      // Batch request with only the geoIds whose map result or time series are not cached yet - partial map result cache hit case
      // This case also covers the complete cache miss case.
      var batchWithPartialRequest = cloudberryConfig.querySliceMills > 0 ? {
        batch: [queryUtil.byTimeRequest(cloudberry.parameters, $scope.geoIdsNotInTimeSeriesCache),
                queryUtil.byGeoRequest(cloudberry.parameters, $scope.geoIdsNotInCache)],
        option: {
          sliceMillis: cloudberryConfig.querySliceMills
        }
      } : {
        batch: [queryUtil.byTimeRequest(cloudberry.parameters, $scope.geoIdsNotInTimeSeriesCache),
                queryUtil.byGeoRequest(cloudberry.parameters, $scope.geoIdsNotInCache)]
      };

      // Complete map result cache and time series cache hit case
      if($scope.geoIdsNotInCache.length === 0 && $scope.geoIdsNotInTimeSeriesCache.length === 0)  {
        cloudberry.countmapMapResult = MapResultCache.getValues(cloudberry.parameters.geoIds,
          cloudberry.parameters.geoLevel);
        cloudberry.commonTimeSeriesResult = TimeSeriesCache.getTimeSeriesValues(cloudberry.parameters.geoIds,
          cloudberry.parameters.geoLevel, cloudberry.parameters.timeInterval);
      }
      // Complete map result cache hit case - exclude map result request
      else if($scope.geoIdsNotInCache.length === 0)  {
        cloudberry.countmapMapResult = MapResultCache.getValues(cloudberry.parameters.geoIds,
          cloudberry.parameters.geoLevel);

        cloudberryClient.send(batchWithoutGeoRequest, function(id, resultSet, resultTimeInterval){
          if(angular.isArray(resultSet)) {
            var requestTimeRange = {
              start: new Date(resultTimeInterval.start),
              end: new Date(resultTimeInterval.end)
            };
            // Since the middleware returns the query result in multiple steps,
            // cloudberryService.timeSeriesQueryResult stores the current intermediate result.
            cloudberry.timeSeriesQueryResult = resultSet[0];
            console.log("1");
            console.log(cloudberry.timeSeriesQueryResult);
            if(cloudberry.timeSeriesQueryResult.length!==0){
              console.log(cloudberry.timeSeriesQueryResult);
              console.log("!!!!!!");
            }

            // Avoid memory leak.
            resultSet[0] = [];
            $scope.ChartDataMap = concatHashmap(
              TimeSeriesCache.arrayToStore(cloudberry.parameters.geoIds,cloudberry.timeSeriesQueryResult,cloudberry.parameters.geoLevel),
              TimeSeriesCache.getInViewTimeSeriesStore(cloudberry.parameters.geoIds,cloudberry.parameters.timeInterval)
            );
            redrawPopup();

            cloudberry.commonTimeSeriesResult =
              TimeSeriesCache.getValuesFromResult(cloudberry.timeSeriesQueryResult).concat(
              TimeSeriesCache.getTimeSeriesValues(cloudberry.parameters.geoIds, cloudberry.parameters.geoLevel, requestTimeRange));
          }
          // When the query is executed completely, we update the time series cache.
          if((cloudberryConfig.querySliceMills > 0 && !angular.isArray(resultSet) &&
            resultSet['key'] === "done") || cloudberryConfig.querySliceMills <= 0) {
            redrawPopup();
            TimeSeriesCache.putTimeSeriesValues($scope.geoIdsNotInTimeSeriesCache,
              cloudberry.timeSeriesQueryResult, cloudberry.parameters.timeInterval);
          }
        }, "batchWithoutGeoRequest");
      }
      // Complete time series cache hit case - exclude time series request
      else if($scope.geoIdsNotInTimeSeriesCache.length === 0)  {
        cloudberry.countmapPartialMapResult = MapResultCache.getValues(cloudberry.parameters.geoIds,
          cloudberry.parameters.geoLevel);

        cloudberryClient.send(batchWithoutTimeRequest, function(id, resultSet, resultTimeInterval){
          if(angular.isArray(resultSet)) {
            var requestTimeRange = {
              start: new Date(resultTimeInterval.start),
              end: new Date(resultTimeInterval.end)
            };
            cloudberry.countmapMapResult = resultSet[0].concat(cloudberry.countmapPartialMapResult);
            cloudberry.commonTimeSeriesResult = TimeSeriesCache.getTimeSeriesValues(cloudberry.parameters.geoIds,
              cloudberry.parameters.geoLevel, requestTimeRange);
          }
          // When the query is executed completely, we update the map result cache.
          if((cloudberryConfig.querySliceMills > 0 && !angular.isArray(resultSet) &&
            resultSet['key'] === "done") || cloudberryConfig.querySliceMills <= 0) {
            MapResultCache.putValues($scope.geoIdsNotInCache, cloudberry.parameters.geoLevel,
              cloudberry.countmapMapResult);
          }
        }, "batchWithoutTimeRequest");
      }
      // Partial map result cache hit case
      else {
        cloudberry.countmapPartialMapResult = MapResultCache.getValues(cloudberry.parameters.geoIds,
          cloudberry.parameters.geoLevel);

        cloudberryClient.send(batchWithPartialRequest, function(id, resultSet, resultTimeInterval){
          if(angular.isArray(resultSet)) {
            var requestTimeRange = {
              start: new Date(resultTimeInterval.start),
              end: new Date(resultTimeInterval.end)
            };
            // Since the middleware returns the query result in multiple steps,
            // cloudberry.timeSeriesQueryResult stores the current intermediate result.
            cloudberry.timeSeriesQueryResult = resultSet[0];

            console.log("2");
            console.log(cloudberry.timeSeriesQueryResult);

            // Avoid memory leak.
            resultSet[0] = [];
            $scope.ChartDataMap = concatHashmap(
              TimeSeriesCache.arrayToStore(cloudberry.parameters.geoIds,cloudberry.timeSeriesQueryResult,cloudberry.parameters.geoLevel),
              TimeSeriesCache.getInViewTimeSeriesStore(cloudberry.parameters.geoIds,cloudberry.parameters.timeInterval)
            );
             redrawPopup();

            cloudberry.commonTimeSeriesResult = TimeSeriesCache.getValuesFromResult(cloudberry.timeSeriesQueryResult).concat(
              TimeSeriesCache.getTimeSeriesValues(cloudberry.parameters.geoIds, cloudberry.parameters.geoLevel, requestTimeRange));
            cloudberry.countmapMapResult = resultSet[1].concat(cloudberry.countmapPartialMapResult);
          }
          // When the query is executed completely, we update the map result cache and time series cache.
          if((cloudberryConfig.querySliceMills > 0 && !angular.isArray(resultSet) &&
            resultSet['key'] === "done") || cloudberryConfig.querySliceMills <= 0) {
            redrawPopup();
            MapResultCache.putValues($scope.geoIdsNotInCache, cloudberry.parameters.geoLevel,
              cloudberry.countmapMapResult);
            TimeSeriesCache.putTimeSeriesValues($scope.geoIdsNotInTimeSeriesCache,
              cloudberry.timeSeriesQueryResult, cloudberry.parameters.timeInterval);
          }
        }, "batchWithPartialRequest");
      }
    }

    // Common event handler for Countmap
    function countMapCommonEventHandler(event) {
      sendCountmapQuery();
    }
    
    // clear countmap specific data
    function cleanCountMap() {

      function removeMapControl(name){
        var ctrlClass = $("."+name);
        if (ctrlClass) {
          ctrlClass.remove();
        }
      }

      // remove CountMap controls
      removeMapControl('legend');
      removeMapControl('normalize');
      removeMapControl('sentiment');
      removeMapControl('info');

      // Unsubscribe to moduleManager's events
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, countMapCommonEventHandler);
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, countMapCommonEventHandler);
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, countMapCommonEventHandler);
      moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, countMapCommonEventHandler);
    }
    
    // initialize countmap
    function setInfoControlCountMap() {
    
      // Interaction function
      // highlight a polygon when the mouse is pointing at it, and popup a window
      function highlightPopupInfo(leafletEvent) {
        if (cloudberry.parameters.maptype == 'countmap'){
          // highlight a polygon
          var layer = leafletEvent.target;
          layer.setStyle($scope.styles.hoverStyle);
          if (!L.Browser.ie && !L.Browser.opera) {
            layer.bringToFront();
          }

          // get selected geoID for the polygon
          $scope.selectedPlace = layer.feature;
          $scope.selectedGeoID = $scope.selectedPlace.properties.cityID || $scope.selectedPlace.properties.countyID || $scope.selectedPlace.properties.stateID;

          // bind a pop up window
          $scope.popUp = L.popup();
          layer.bindPopup($scope.popUp).openPopup();
          $scope.popUp.setContent(getPopupContent()).setLatLng([$scope.selectedPlace.properties.popUpLat,$scope.selectedPlace.properties.popUpLog]);

          addPopupEvent();
          drawLineChart();

        }
      }

      // remove the highlight interaction function for the polygons， and close popup window
      function resetHighlight(leafletEvent) {
        if (cloudberry.parameters.maptype == 'countmap'){
          var style;
          if (!$scope.status.init){
            style = {
              weight: 1.5,
              fillOpacity: 0.5,
              color: '#92d1e1'
            };
          }
          else {
            style = {
              weight: 1.5,
              fillOpacity: 0.5,
              color: '#92d1e1'
            };
          }
          if (leafletEvent){
            leafletEvent.target.setStyle(style);
            var orginalTarget = leafletEvent.originalEvent.relatedTarget;
            if(orginalTarget && orginalTarget.toString()==="[object SVGSVGElement]") {
              $scope.map.closePopup();
            }
          }
        }
      }

      // add feature to each polygon
      // highlight a polygon when mouseover
      // remove the highlight when mouseout
      // zoom in to fit the polygon when the polygon is clicked
      function onEachFeature(feature, layer) {
        layer.on({
          mouseover: highlightPopupInfo,
          mouseout: resetHighlight,
          click: $scope.zoomToFeature
        });
      }

      // // add info control
      // var info = L.control();
      //
      // info.onAdd = function() {
      //   this._div = L.DomUtil.create('div', 'info'); // create a div with a class "info"
      //   this._div.style.margin = '20% 0 0 0';
      //   this._div.innerHTML = [
      //     '<h4><span ng-bind="infoPromp + \' by \' + status.logicLevel"></span></h4>',
      //     '<b><span ng-bind="selectedPlace.properties.name || \'No place selected\'"></span></b>',
      //     '<br/>',
      //     '<span ng-bind="infoPromp"></span> <span ng-bind="selectedPlace.properties.countText || \'0\'"></span>'
      //   ].join('');
      //   $compile(this._div)($scope);
      //   return this._div;
      // };
      //
      // info.options = {
      //   position: 'topleft'
      // };
      // if ($scope.map){
      //   info.addTo($scope.map);
      // }
      // else {
      //   $scope.controls.custom.push(info);
      // }

      $scope.loadGeoJsonFiles(onEachFeature);

      $scope.$parent.onEachFeature = onEachFeature;

      // Subscribe to moduleManager's events
      moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, countMapCommonEventHandler);
      moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_REGION_BY_DRAG, countMapCommonEventHandler);
      moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, countMapCommonEventHandler);
      moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_TIME_SERIES_RANGE, countMapCommonEventHandler);
    }
    
    /**
     * Update map based on a set of spatial query result cells
     * @param    result  =>  mapPlotData, an array of coordinate and weight objects
     */
    function drawCountMap(result) {
      var colors = $scope.styles.colors;
      var sentimentColors = $scope.styles.sentimentColors;
      var normalizedCountMax = 0,
          normalizedCountMin = 0,
          intervals = colors.length - 1,
          difference = 0;

      function getSentimentColor(d) {
        if( d < cloudberryConfig.sentimentUpperBound / 3) {    // 1/3
          return sentimentColors[0];
        } else if( d < 2 * cloudberryConfig.sentimentUpperBound / 3){    // 2/3
          return sentimentColors[1];
        } else{     // 3/3
          return sentimentColors[2];
        }
      }

      function getNormalizedCountColor(d) {
        var i = 1;
        for (; i <= intervals; i++){
          if ( d <= normalizedCountMin + ((i * difference) / intervals)){  // bound = min + (i / 6) * difference
            return colors[i];
          }
        }
        return colors[intervals]; // in case of max
      }

      function getUnnormalizedCountColor(d) {
        if(!d || d <= 0) {
          d = 0;
        } else if (d ===1 ){
          d = 1;
        } else {
          d = Math.ceil(Math.log10(d));
          if(d <= 0) // treat smaller counts the same as 0
            d = 0
        }
        d = Math.min(d, colors.length-1);
        return colors[d];
      }

      function getColor(d) {
        if($scope.doSentiment)  // 0 <= d <= 4
          return getSentimentColor(d);
        else if($scope.doNormalization)
          return getNormalizedCountColor(d);
        else
          return getUnnormalizedCountColor(d);
      }

      function style(feature) {
        if (!feature.properties.count || feature.properties.count === 0){
          return {
            fillColor: '#f7f7f7',
            weight: 1.5,
            opacity: 1,
            color: '#92d1e1',
            fillOpacity: 0.5
          };
        } else {
          return {
            fillColor: getColor(feature.properties.count),
            weight: 1.5,
            opacity: 1,
            color: '#92d1e1',
            fillOpacity: 0.5
          };
        }
      }

      function setNormalizedCountText(geo){
        // beautify 0.0000123 => 1.23e-5, 1.123 => 1.1
        if(geo["properties"]["count"] < 1){
          geo["properties"]["countText"] = geo["properties"]["count"].toExponential(1);
        } else{
          geo["properties"]["countText"] = geo["properties"]["count"].toFixed(1);
        }
        geo["properties"]["countText"] += cloudberryConfig.normalizationUpscaleText; // "/M"
      }

      function resetCount(geo) {
        if (geo['properties']['count'])
          geo['properties']['count'] = 0;
        if (geo['properties']['countText'])
          geo['properties']['countText'] = "";
      }

      function setNormalizedCount(geo, r){
        var normalizedCount = r['count'] / r['population'] * cloudberryConfig.normalizationUpscaleFactor;
        geo['properties']['count'] = normalizedCount;
        if(normalizedCount > normalizedCountMax)  // update max to enable dynamic legends
          normalizedCountMax = normalizedCount;
        setNormalizedCountText(geo);
      }

      function setUnnormalizedCount(geo ,r) {
        geo['properties']['count'] = r['count'];
        geo['properties']['countText'] = geo['properties']['count'].toString();
      }

      function updateTweetCountInGeojson(){
        var level = $scope.status.logicLevel;
        var geojsonData = $scope.geojsonData[level];
        if(geojsonData){
          angular.forEach(geojsonData['features'], function (geo) {
            resetCount(geo);
            angular.forEach(result, function (r) {
              if (r[level] === geo['properties'][level+"ID"]){
                if($scope.doSentiment){
                  // sentimentScore for all the tweets in the same polygon / number of tweets with the score
                  geo['properties']['count'] = r['sentimentScoreSum'] / r['sentimentScoreCount'];
                  geo["properties"]["countText"] = geo["properties"]["count"].toFixed(1);
                } else if ($scope.doNormalization) {
                  setNormalizedCount(geo, r);
                } else{
                  setUnnormalizedCount(geo, r);
                }
              }
            });
          });
          difference = normalizedCountMax - normalizedCountMin;  // to enable dynamic legend for normalization
          // draw
          $scope.polygons[level+"Polygons"].setStyle(style);
        }
      }

      // Loop through each result and update its count information on its associated geo record
      updateTweetCountInGeojson();

      /**
       * add information control: legend, toggle
       * */

      function addMapControl(name, position, initDiv, initJS){
        var ctrlClass = $("."+name);
        if (ctrlClass) {
          ctrlClass.remove();
        }

        $scope[name]= L.control({
          position: position
        });

        $scope[name].onAdd = function() {
          var div = L.DomUtil.create('div', 'info ' + name);
          initDiv(div);
          return div;
        };
        if ($scope.map) {
          $scope[name].addTo($scope.map);
          if (initJS)
            initJS();
        }
      }

      function initNormalize(div) {
        if($scope.doNormalization)
          div.innerHTML = '<p>Normalize</p><input id="toggle-normalize" checked type="checkbox">';
        else
          div.innerHTML = '<p>Normalize</p><input id="toggle-normalize" type="checkbox">';
      }

      function initNormalizeToggle() {
        var toggle = $('#toggle-normalize');
        toggle.bootstrapToggle({
          on: "By Population"
        });
        if($scope.doSentiment){
          toggle.bootstrapToggle('off');
          toggle.bootstrapToggle('disable');
        }
      }

      function initSentiment(div) {
        if($scope.doSentiment)
          div.innerHTML = '<p>Sentiment Analysis</p><input id="toggle-sentiment" checked type="checkbox">';
        else
          div.innerHTML = '<p>Sentiment Analysis</p><input id="toggle-sentiment" type="checkbox">';
      }

      function initSentimentToggle() {
        $('#toggle-sentiment').bootstrapToggle({
          on: "By OpenNLP"
        });
      }

      function setSentimentLegend(div) {
        div.setAttribute("title", "Sentiment Score: Negative(0)-Positive(4)");  // add tool-tips for the legend
        div.innerHTML +=
          '<i style="background:' + getColor(1) + '"></i>Negative<br>';
        div.innerHTML +=
          '<i style="background:' + getColor(2) + '"></i>Neutral<br>';
        div.innerHTML +=
          '<i style="background:' + getColor(3) + '"></i>Positive<br>';
      }

      function setGrades(grades) {
        var i = 0;
        for(; i < grades.length; i++){
          if ($scope.doNormalization)
            grades[i] = normalizedCountMin + ((i * difference) / intervals);
          else
            grades[i] = Math.pow(10, i);
        }
      }

      function getGradesNames(grades) {
        return grades.map( function(d) {
          var returnText = "";
          if (d < 1000){
            returnText = d.toFixed();
          } else if (d < 1000 * 1000) {
            returnText = (d / 1000).toFixed() + "K";
          } else if (d < 1000 * 1000 * 1000) {
            returnText = (d / 1000 / 1000).toFixed() + "M";
          } else{
            returnText = (d / 1000 / 1000).toFixed() + "M+";
          }
          if($scope.doNormalization)
            return returnText + cloudberryConfig.normalizationUpscaleText; //["1/M", "10/M", "100/M", "1K/M", "10K/M", "100K/M"];
          else
            return returnText; //["1", "10", "100", "1K", "10K", "100K"];
        });
      }

      function setCountLegend(div) {
        div.style.margin = '20% 0 0 0';
        var grades = new Array(colors.length -1); //[1, 10, 100, 1000, 10000, 100000]
        setGrades(grades);
        var gName  = getGradesNames(grades);
        if($scope.doNormalization)
          div.setAttribute("title", "# of Tweets per Million People");  // add tool-tips for the legend to explain the meaning of "M"
        // loop through our density intervals and generate a label with a colored square for each interval
        i = 1;
        for (; i < grades.length; i++) {
          div.innerHTML +=
            '<i style="background:' + getColor(grades[i]) + '"></i>' + gName[i-1] + '&ndash;' + gName[i] + '<br>';
        }
        if ($scope.doNormalization)
          div.innerHTML += '<i style="background:' + getColor(grades[i-1] + ((difference) / intervals)) + '"></i> ' + gName[i-1] + '+';
        else
          div.innerHTML += '<i style="background:' + getColor(grades[i-1]*10) + '"></i> ' + gName[i-1] + '+';
      }

      function initLegend(div) {
        if($scope.doSentiment){
          setSentimentLegend(div);
        } else {
          setCountLegend(div);
        }
      }

      // add legend
      addMapControl('legend', 'topleft', initLegend, null);

      // add toggle normalize
      addMapControl('normalize', 'topleft', initNormalize, initNormalizeToggle);

      // add toggle sentiment analysis
      if(cloudberryConfig.sentimentEnabled)
        addMapControl('sentiment', 'topleft', initSentiment, initSentimentToggle);

    }
    
    // initialize if the default map type is countmap
    if (cloudberry.parameters.maptype == 'countmap'){
      setCountMapStyle();
      $scope.resetPolygonLayers();
      setInfoControlCountMap();
    }
    
    // map type change handler
    // initialize the map (styles, zoom/drag handler, etc) when switch to this map
    // clear the map when switch to other map
    function onMapTypeChange(event) {
      if (event.currentMapType === "countmap") {
        setCountMapStyle();
        $scope.resetPolygonLayers();
        setInfoControlCountMap();
        sendCountmapQuery();
      }
      else if (event.previousMapType === "countmap"){
        cleanCountMap();
      }
    }

    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_MAP_TYPE, onMapTypeChange);
    
    // TODO - get rid of these variables watching by events subscribing and publishing
    // monitor the countmap related variables, update the countmap if necessary
    $scope.$watchCollection(
      function() {
        return {
          'countmapMapResult': cloudberry.countmapMapResult,
          'doNormalization': $('#toggle-normalize').prop('checked'),
          'doSentiment': $('#toggle-sentiment').prop('checked')
        };
      },

      function(newResult, oldValue) {
        if (cloudberry.parameters.maptype == 'countmap'){
          if (newResult['countmapMapResult'] !== oldValue['countmapMapResult']) {
            $scope.result = newResult['countmapMapResult'];
            if (Object.keys($scope.result).length !== 0) {
              $scope.status.init = false;
              drawCountMap($scope.result);
            } else {
              drawCountMap($scope.result);
            }
          }
          if(newResult['doNormalization'] !== oldValue['doNormalization']) {
            $scope.doNormalization = newResult['doNormalization'];
            drawCountMap($scope.result);
          }
          if(newResult['doSentiment'] !== oldValue['doSentiment']) {
            $scope.doSentiment = newResult['doSentiment'];
            if($scope.doSentiment) {
              $scope.infoPromp = "Score";  // change the info promp
            } else {
              $scope.infoPromp = config.mapLegend;
            }
            drawCountMap($scope.result);
          }
        }
      }
    );

  });
