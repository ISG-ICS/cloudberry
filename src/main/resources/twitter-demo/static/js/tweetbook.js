/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
$(function () {

  // Add a format function to String
  if (!String.prototype.format) {
    String.prototype.format = function () {
      var args = arguments;
      return this.replace(/{(\d+)}/g, function (match, number) {
        return typeof args[number] != 'undefined'
          ? args[number]
          : match
          ;
      });
    };
  }


  // Initialize connection to AsterixDB. Just one connection is needed and contains
  // logic for connecting to each API endpoint. This object A is reused throughout the
  // code but does not store information about any individual API call.
  A = new AsterixDBConnection({

    // We will be using the geo dataverse, which we can configure either like this
    // or by following our AsterixDBConnection with a dataverse call, like so:
    // A = new AsterixDBConnection().dataverse("geo");
    "dataverse": "twitter",

    // Due to the setup of this demo using the Bottle server, it is necessary to change the
    // default endpoint of API calls. The proxy server handles the call to http://localhost:19002
    // for us, and we reconfigure this connection to connect to the proxy server.
    "endpoint_root": "/",

    // Finally, we want to make our error function nicer so that we show errors with a call to the
    // reportUserMessage function. Update the "error" property to do that.
    "error": function (data) {
      // For an error, data will look like this:
      // {
      //     "error-code" : [error-number, error-text]
      //     "stacktrace" : ...stack trace...
      //     "summary"    : ...summary of error...
      // }
      // We will report this as an Asterix REST API Error, an error code, and a reason message.
      // Note the method signature: reportUserMessage(message, isPositiveMessage, target). We will provide
      // an error message to display, a positivity value (false in this case, errors are bad), and a
      // target html element in which to report the message.
      var showErrorMessage = "Asterix Error #" + data["error-code"][0] + ": " + data["error-code"][1];
      var isPositive = false;
      var showReportAt = "report-message";

      reportUserMessage(showErrorMessage, isPositive, showReportAt);
    }
  });

  // This little bit of code manages period checks of the asynchronous query manager,
  // which holds onto handles asynchornously received. We can set the handle update
  // frequency using seconds, and it will let us know when it is ready.
  var intervalID = setInterval(
    function () {
      asynchronousQueryIntervalUpdate();
    },
    asynchronousQueryGetInterval()
  );

  // Legend Container
  // Create a rainbow from a pretty color scheme.
  // http://www.colourlovers.com/palette/292482/Terra
  rainbow = new Rainbow();
  rainbow.setSpectrum("blue", "aqua", "green", "yellow", "red");
  buildLegend();

  // Initialization of UI Tabas
  initDemoPrepareTabs();

  // UI Elements - Creates Map, Location Auto-Complete, Selection Rectangle
  var mapOptions = {
    center: new google.maps.LatLng(39.5, -96.35),
    zoom: 4,
    mapTypeId: google.maps.MapTypeId.ROADMAP,
    streetViewControl: false,
    draggable: true,
    mapTypeControl: false
  };
  map = new google.maps.Map(document.getElementById('map_canvas'), mapOptions);

  var input = document.getElementById('location-text-box');
  var autocomplete = new google.maps.places.Autocomplete(input);
  autocomplete.bindTo('bounds', map);

  google.maps.event.addListener(autocomplete, 'place_changed', function () {
    var place = autocomplete.getPlace();
    if (place.geometry.viewport) {
      map.fitBounds(place.geometry.viewport);
    } else {
      map.setCenter(place.geometry.location);
      map.setZoom(17);  // Why 17? Because it looks good.
    }
    var address = '';
    if (place.address_components) {
      address = [(place.address_components[0] && place.address_components[0].short_name || ''),
        (place.address_components[1] && place.address_components[1].short_name || ''),
        (place.address_components[2] && place.address_components[2].short_name || '')].join(' ');
    }
  });

  // Drawing Manager for selecting map regions. See documentation here:
  // https://developers.google.com/maps/documentation/javascript/reference#DrawingManager
  rectangleManager = new google.maps.drawing.DrawingManager({
    drawingMode: google.maps.drawing.OverlayType.RECTANGLE,
    drawingControl: false,
    rectangleOptions: {
      strokeWeight: 1,
      clickable: false,
      editable: true,
      strokeColor: "#2b3f8c",
      fillColor: "#2b3f8c",
      zIndex: 1
    }
  });
  rectangleManager.setMap(map);
  selectionRectangle = null;

  // Drawing Manager: Just one editable rectangle!
  google.maps.event.addListener(rectangleManager, 'rectanglecomplete', function (rectangle) {
    selectionRectangle = rectangle;
    rectangleManager.setDrawingMode(null);
  });

  // Initialize data structures
  APIqueryTracker = {};
  asyncQueryManager = {};
  map_cells = [];
  map_tweet_markers = [];
  label_marker = new MarkerWithLabel({
    position: new google.maps.LatLng(0, 0),
    draggable: false,
    raiseOnDrag: false,
    map: map,
    labelContent: "A",
    labelAnchor: new google.maps.Point(30, 20),
    labelClass: "labels", // the CSS class for the label
    labelStyle: {opacity: 1.0},
    icon: "http://placehold.it/1x1",
    visible: false
  });

  sample_marker = new MarkerWithLabel({
    position: new google.maps.LatLng(0, 0),
    draggable: false,
    raiseOnDrag: false,
    map: map,
    labelContent: "tweet",
    labelAnchor: new google.maps.Point(30, 20),
    labelClass: "tweet-labels", // the CSS class for the label
    labelStyle: {opacity: 1.0},
    icon: "http://placehold.it/3x3",
    visible: false
  });

  samples = {};
  review_mode_tweetbooks = [];

  initDemoUIButtonControls();

  google.maps.event.addListenerOnce(map, 'idle', function () {
    // Show tutorial tab only the first time the map is loaded
    // $('#mode-tabs a:first').tab('show');
  });

  county_polygons = {};
  state_polygons = {};
  city_polygons = {};
  drawCountyBoundry();
  drawStateBoundry();
});

function drawCountyBoundry() {
  var geo_query_all_counties = 'for $x in dataset ds_us_county order by spatial-area($x.geometry) return { "county":$x.State-County, "geometry":$x.geometry};';
  var drawCountyHandler = drawBoundry('county', 'geometry', county_polygons, null);
  console.time("initial_polygon_aql_get_result");
  A.query(geo_query_all_counties, drawCountyHandler, "synchronous");
}

function drawStateBoundry() {
  var geo_query_all_counties = 'for $x in dataset ds_us_state order by spatial-area($x.geometry) return { "state":$x.abbr, "geometry":$x.geometry};';
  var drawCountyHandler = drawBoundry('state', 'geometry', state_polygons, map);
  console.time("initial_polygon_aql_get_result");
  A.query(geo_query_all_counties, drawCountyHandler, "synchronous");
}

function drawBoundry(key_name, geometry_name, polygonStores, targetMap) {

  return function (polygon_results) {
    console.timeEnd("initial_polygon_aql_get_result");
    console.time("initial_polygon_drawing");
    polygon_results = polygon_results.results[0];
    for (c = 0; c < polygon_results.length; c++) {
      var key = polygon_results[c][key_name];
      var county_geometry = polygon_results[c][geometry_name];

      var coordinate_list = [];
      for (var point = 0; point < county_geometry.length; point++) {
        var coordinate = {lat: county_geometry[point][1], lng: county_geometry[point][0]};
        coordinate_list.push(coordinate);
      }

      polygonStores[key] = new google.maps.Polygon({
        paths: coordinate_list,
        strokeColor: 'black',
        strokeOpacity: 0.8,
        strokeWeight: 0.5,
        fillColor: 'blue',
        fillOpacity: 0.4,
        level: key_name,
        key: key
      });

    }

    for (var k in polygonStores) {
      polygonStores[k].setMap(targetMap);
    }
    console.timeEnd("initial_polygon_drawing");
  }
}

function initDemoUIButtonControls() {
  $("#grid").jqGrid({
    datatype: "local",
    autowidth: true,
    shrinkToFit: false,
    height: 600,
    viewreords: true,
    rowNum: 60,
    pager: "#grid_pgr",
    colNames: ['User Name', 'Tweet Text'],
    colModel: [
      {
        name: 'uname', index: 'uname', width: 100, align: "left", cellattr: function (rowId, tv, rawObject, cm, rdata) {
        return 'style="white-space: normal;"'
      }
      },
      {
        name: 'tweet', index: 'tweet', width: 260, align: "left", cellattr: function (rowId, tv, rawObject, cm, rdata) {
        return 'style="white-space: normal;"'
      }
      }
    ]
  });
  $("#tweets-table").hide();

  // Explore Mode - Query Builder Date Pickers
  var dateOptions = {
    dateFormat: "yy-mm-dd",
    defaultDate: "2015-12-17",
    navigationAsDateFormat: true,
    constrainInput: true
  };
  var start_dp = $("#start-date").datepicker(dateOptions);
  start_dp.val(dateOptions.defaultDate);
  dateOptions['defaultDate'] = new Date().toJSON().slice(0, 10);
  var end_dp = $("#end-date").datepicker(dateOptions);
  end_dp.val(dateOptions.defaultDate);

  // Explore Mode: Toggle Selection/Location Search
  $('#selection-button').on('change', function (e) {
    $("#location-text-box").attr("disabled", "disabled");
    rectangleManager.setMap(map);
    if (selectionRectangle) {
      selectionRectangle.setMap(null);
      selectionRectangle = null;
    } else {
      rectangleManager.setDrawingMode(google.maps.drawing.OverlayType.RECTANGLE);
    }
  });
  $('#location-button').on('change', function (e) {
    $("#location-text-box").removeAttr("disabled");
    if (selectionRectangle) {
      selectionRectangle.setMap(map);
    }
    rectangleManager.setMap(null);
    rectangleManager.setDrawingMode(google.maps.drawing.OverlayType.RECTANGLE);
  });
  $("#location-button").trigger("click");

  // Review Mode: New Tweetbook
  $('#new-tweetbook-button').on('click', function (e) {
    onCreateNewTweetBook($('#new-tweetbook-entry').val());

    $('#new-tweetbook-entry').val("");
    $('#new-tweetbook-entry').attr("placeholder", "Name a new tweetbook");
  });

  $('#state-button').on('change', function (e) {
    $(this).data('clicked', true);
    $('#county-button').data('clicked', false);
    $('#city-button').data('clicked', false);
  });

  $('#county-button').on('change', function (e) {
    $(this).data('clicked', true);
    $('#state-button').data('clicked', false);
    $('#city-button').data('clicked', false);
  });

  $('#city-button').on('change', function (e) {
    $(this).data('clicked', true);
    $('#state-button').data('clicked', false);
    $('#county-button').data('clicked', false);
  });

  $('#state-button').trigger('click');

  // Explore Mode - Clear Button
  $("#clear-button").click(mapWidgetResetMap);

  // Explore Mode: Query Submission
  $("#submit-button").on("click", function () {
    $("#report-message").html('');
    $("#submit-button").attr("disabled", true);
    rectangleManager.setDrawingMode(null);

    var kwterm = $("#keyword-textbox").val();
    var startdp = $("#start-date").datepicker("getDate");
    var enddp = $("#end-date").datepicker("getDate");
    var startdt = $.datepicker.formatDate("yy-mm-dd", startdp) + "T00:00:00Z";
    var enddt = $.datepicker.formatDate("yy-mm-dd", enddp) + "T23:59:59Z";

    var level = 'state';
    if ($("#county-button").data('clicked')) {
      level = 'county';
    } else if ($("#city-button").data('clicked')) {
      level = 'city';
    }

    var formData = {
      "keyword": kwterm,
      "startdt": startdt,
      "enddt": enddt,
      "level": level
    };

    // Get Map Bounds
    var bounds;
    if ($('#selection-label').hasClass("active") && selectionRectangle) {
      bounds = selectionRectangle.getBounds();
    } else {
      bounds = map.getBounds();
    }

    var swLat = Math.abs(bounds.getSouthWest().lat());
    var swLng = Math.abs(bounds.getSouthWest().lng());
    var neLat = Math.abs(bounds.getNorthEast().lat());
    var neLng = Math.abs(bounds.getNorthEast().lng());

    formData["swLat"] = Math.min(swLat, neLat);
    formData["swLng"] = Math.max(swLng, neLng);
    formData["neLat"] = Math.max(swLat, neLat);
    formData["neLng"] = Math.min(swLng, neLng);

    var build_tweetbook_mode = "synchronous";
    if ($('#asbox').is(":checked")) {
      build_tweetbook_mode = "asynchronous";
    }

    var f = buildAQLQueryFromForm(formData);

    APIqueryTracker = {
      "query": "use dataverse " + A._properties['dataverse'] + ";\n" + f,
      "data": formData
    };


    reportUserMessage(APIqueryTracker.query, true, "report-query");

    if (selectionRectangle) {
      map.setCenter(selectionRectangle.getBounds().getCenter());
      switch (level) {
        case 'state':
          map.setZoom(4);
          break;
        case 'county':
          map.setZoom(6);
          break;
        case 'city':
          map.setZoom(8);
          break;
      }
    }
    if (build_tweetbook_mode == "synchronous") {
      console.time("query_aql_get_result");
      // Due to the feed + tmp dataset bug, we can not drop the dataset with the query.
      // However, we can excute it seprately.
      A.aql('drop dataset tmp_tweets if exists;', function () {
      }, build_tweetbook_mode);
      A.aql(f, tweetbookQuerySyncCallbackWithLevel(level), build_tweetbook_mode);
    } else {
      A.aql(f, tweetbookQueryAsyncCallback, build_tweetbook_mode);
    }

    mapWidgetClearMap();
    clearD3();
    // Clears selection rectangle on query execution, rather than waiting for another clear call.
    if (selectionRectangle) {
      selectionRectangle.setMap(null);
      selectionRectangle = null;
    }
  });
}

function declareRectangle(bounds) {
  return 'let $region := create-rectangle(create-point({0},{1}),\n create-point({2},{3}))'.format(bounds['sw']['lng'],
    bounds['sw']['lat'], bounds['ne']['lng'], bounds['ne']['lat'])
}


function buildTemporaryDataset(parameters) {

  var bounds = {
    "ne": {"lat": parameters["neLat"], "lng": -1 * parameters["neLng"]},
    "sw": {"lat": parameters["swLat"], "lng": -1 * parameters["swLng"]}
  };

  var aql = [];

  aql.push('create temporary dataset tmp_tweets(type_tweet) primary key id; ');
  aql.push('insert into dataset tmp_tweets (');
  aql.push(declareRectangle(bounds));
  aql.push('let $ts_start := datetime("{0}")'.format(parameters['startdt']));
  aql.push('let $ts_end := datetime("{0}")'.format(parameters['enddt']));

  var ds_for = 'for $t in dataset ds_tweets ';
  var ds_predicate = 'where $t.place.country = "United States" and $t.place.place_type = "city" \n' +
    'and spatial-area($t.place.bounding_box) < 5 \n' +
    'and $t.create_at >= $ts_start and $t.create_at < $ts_end \n' +
    'and spatial-intersect($t.place.bounding_box, $region) \n';
  if (parameters["keyword"].length > 0) {
    var tokens = parameters["keyword"].split(/\s+/g);
    for (var i = 0; i < tokens.length; i++) {
      ds_predicate = 'let $keyword{0} := "{1}"\n'.format(i, tokens[i]) + ds_predicate;
    }
    for (var i = 0; i < tokens.length; i++) {
      ds_predicate += 'and contains($t.text_msg, $keyword{0}) \n'.format(i);
    }
  }
  aql.push(ds_for);
  aql.push(ds_predicate);
  aql.push('return { \
      "create_at" : $t.create_at,\
      "id": $t.id,\
      "text_msg" : $t.text_msg,\
      "in_reply_to_status" : $t.in_reply_to_status,\
      "in_reply_to_user" : $t.in_reply_to_user,\
      "favorite_count" : $t.favorite_count,\
      "geo_location" : $t.geo_location,\
      "retweet_count" : $t.retweet_count,\
      "lang" : $t.lang,\
      "is_retweet": $t.is_retweet,\
      "hashtags" : $t.hashtags,\
      "user_mentions" : $t.user_mentions ,\
      "user" : $t.user,\
      "place" : $t.place,\
      "county": (for $city in dataset ds_zip\
      where substring-before($t.place.full_name, ",") = $city.city \
      and substring-after($t.place.full_name, ", ") = $city.state \
      and not(is-null($city.county))\
      return string-concat([$city.state, "-", $city.county]) )[0]}\n');
  aql.push(')'); // end of insert
  return aql;
}

function selectAreaByPolygon(polygon) {
  switch (polygon.level) {
    case 'state':
      return 'substring-after($t.place.full_name, ", ") = "{0}" '.format(polygon.key);
    case 'county':
      return '$t.county = "{0}"'.format(polygon.key);
    case 'city':
      return '$t.place.full_name = "{0}" '.format(polygon.key);
    default:
      alert('unknown polygon level');
  }
}

function buildHashTagCountQuery(polygon) {
  var aql = [];
  aql.push('for $t in dataset tmp_tweets ');
  aql.push('where not(is-null($t.hashtags))');
  if (polygon) {
    aql.push('and ' + selectAreaByPolygon(polygon));
  }
  aql.push('for $h in $t.hashtags');
  aql.push('group by $tag := $h with $h');
  aql.push('let $c := count($h) ');
  aql.push('order by $c desc ');
  aql.push('return { "tag": $tag, "count": $c};\n');
  return aql.join('\n');
}

function buildTimeGroupby(polygon) {
  var aql = [];
  aql.push('for $t in dataset tmp_tweets ');
  if (polygon) {
    aql.push('where ' + selectAreaByPolygon(polygon));
  }
  aql.push('group by $c := print-datetime($t.create_at, "YYYY-MM-DD hh") with $t ');
  aql.push('let $count := count($t)');
  aql.push('order by $c ');
  aql.push('return {"slice":$c, "count" : $count };\n');
  return aql.join('\n');
}

function buildTweetSample(polygon) {
  var aql = [];
  aql.push('for $t in dataset tmp_tweets ');
  if (polygon) {
    aql.push('where ' + selectAreaByPolygon(polygon));
  }
  aql.push('limit 100');
  aql.push('return {"uname": $t.user.screen_name, "tweet":$t.text_msg};\n')
  return aql.join('\n');
}

/**
 * Builds AsterixDB REST Query from explore mode form.
 */
function buildAQLQueryFromForm(parameters) {

  var level = parameters["level"];

  var aql = buildTemporaryDataset(parameters);

  var sample = '';
  for (var i = 0; i < 1; i++) {
    if (i > 0) {
      sample += ',';
    }
    sample += ' "s{0}":$t[{1}].text_msg'.format(i, i);
  }

  var ds_for = 'for $t in dataset tmp_tweets ';

  aql.push(ds_for);
  if (level === 'county') {
    aql.push('group by $c := $t.county with $t \
      let $count := count($t) \n\
      order by $count desc \n\
    return { "cell" : $c, "count" : $count};');
  } else {
    if (parameters['level'] === "state") {
      aql.push('group by $c := substring-after($t.place.full_name, ", ") with $t');
      aql.push('let $count := count($t)');
      aql.push('order by $count desc');
      aql.push('return { "cell":$c, "count" : $count };');
    } else if (parameters['level'] === "city") {
      aql.push('group by $c := $t.place.full_name with $t');
      aql.push('let $count := count($t)');
      aql.push('order by $count desc');
      aql.push('return {"cell":$c, "count" : $count, "area": $t[0].place.bounding_box };');
    }
  }

  aql.push(buildTimeGroupby());
  aql.push(buildHashTagCountQuery());
  aql.push(buildTweetSample());
  return aql.join('\n');
}

/**
 * Checks through each asynchronous query to see if they are ready yet
 */
function asynchronousQueryIntervalUpdate() {
  for (var handle_key in asyncQueryManager) {
    if (!asyncQueryManager[handle_key].hasOwnProperty("ready")) {
      asynchronousQueryGetAPIQueryStatus(asyncQueryManager[handle_key]["handle"], handle_key);
    }
  }
}

/**
 * Returns current time interval to check for asynchronous query readiness
 * @returns  {number}    milliseconds between asychronous query checks
 */
function asynchronousQueryGetInterval() {
  var seconds = 10;
  return seconds * 1000;
}

/**
 * Retrieves status of an asynchronous query, using an opaque result handle from API
 * @param    {Object}    handle, an object previously returned from an async call
 * @param    {number}    handle_id, the integer ID parsed from the handle object
 */
function asynchronousQueryGetAPIQueryStatus(handle, handle_id) {

  A.query_status(
    {
      "handle": JSON.stringify(handle)
    },
    function (res) {
      if (res["status"] == "SUCCESS") {
        // We don't need to check if this one is ready again, it's not going anywhere...
        // Unless the life cycle of handles has changed drastically
        asyncQueryManager[handle_id]["ready"] = true;

        // Indicate success.
        $('#handle_' + handle_id).removeClass("btn-disabled").prop('disabled', false).addClass("btn-success");
      }
    }
  );
}

function tweetbookQuerySyncCallbackWithLevel(level) {

  /**
   * A spatial data cleaning and mapping call
   * @param    {Object}    res, a result object from a tweetbook geospatial query
   */
  return function tweetbookQuerySyncCallback(res) {
    // First, we check if any results came back in.
    // If they didn't, return.

    console.timeEnd("query_aql_get_result");
    if (!res.hasOwnProperty("results")) {
      reportUserMessage("Oops, no results found for those parameters.", false, "report-message");
      return;
    }

    var maxWeight = 0;
    var minWeight = Number.MAX_VALUE;

    // Parse resulting JSON objects. Here is an example record:
    // { "cell": rectangle("21.5,-98.5 24.5,-95.5"), "count": 78i64 }
    $.each(res.results[0], function (i, data) {

      // We need to clean the JSON a bit to parse it properly in javascript

      // We track the minimum and maximum weight to support our legend.
      maxWeight = Math.max(data.count, maxWeight);
      minWeight = Math.min(data.count, minWeight);

    });

    var polygons = state_polygons;
    if (level === 'county') {
      polygons = county_polygons;
    } else if (level === 'city') {
      polygons = city_polygons;
    }
    triggerUIUpdate(res.results[0], maxWeight, minWeight, polygons, level);
    if (res.results[1]) {
      drawTimeSerialBrush(res.results[1]);
    }
    if (res.results[2]) {
      drawWordCloud(res.results[2]);
    }
    if (res.results[3]) {
      drawTable(res.results[3]);
    }
  }
}

/**
 * Triggers a map update based on a set of spatial query result cells
 * @param    [Array]     mapPlotData, an array of coordinate and weight objects
 * @param    [Array]     plotWeights, a list of weights of the spatial cells - e.g., number of tweets
 */
function triggerUIUpdate(mapPlotData, maxWeight, minWeight, polygons, level) {
  /** Clear anything currently on the map **/
  console.time("query_aql_draw");
  mapWidgetClearMap();

  $.each(mapPlotData, function (m) {


    var cp = polygons[mapPlotData[m].cell];
    if (!cp) {
      if (level === "city") {
        var coordinate_list = [];
        var rectangle = mapPlotData[m].area;
        if (rectangle) {
          coordinate_list.push({lat: rectangle[0][1], lng: rectangle[0][0]});
          coordinate_list.push({lat: rectangle[1][1], lng: rectangle[0][0]});
          coordinate_list.push({lat: rectangle[1][1], lng: rectangle[1][0]});
          coordinate_list.push({lat: rectangle[0][1], lng: rectangle[1][0]});
          coordinate_list.push({lat: rectangle[0][1], lng: rectangle[0][0]});
        }

        polygons[mapPlotData[m].cell] = new google.maps.Polygon({
          paths: coordinate_list,
          strokeColor: 'black',
          strokeOpacity: 0.8,
          strokeWeight: 0.5,
          fillColor: 'blue',
          fillOpacity: 0.4,
          level: "city",
          key: mapPlotData[m].cell
        });
        cp = polygons[mapPlotData[m].cell];
      }
    }

    if (cp) {
      cp.fillColor = "#" + rainbow.colourAt(Math.ceil(100 * (mapPlotData[m].count / maxWeight)));
      cp.fillOpacity = 0.8;


      // Clicking on a circle drills down map to that value, hovering over it displays a count
      // of tweets at that location.
      //google.maps.event.addListener(cp, 'click', function (event) {
      //$.each(markers, function (i) {
      //    markers[i].close();
      //});
      //onMapPointDrillDown(map_circle.val);
      //});

      google.maps.event.addListener(cp, 'mousemove', function (event) {
        label_marker.setPosition(event.latLng);
        label_marker.labelContent = mapPlotData[m].cell + ":" + mapPlotData[m].count + " tweets";
        label_marker.label.draw();
        label_marker.setVisible(true);

      });

      google.maps.event.addListener(cp, 'mouseout', function (event) {
        label_marker.setVisible(false);
        //sample_marker.setVisible(false);
      });

      google.maps.event.addListener(cp, 'click', function (event) {
        clearD3();
        var sample = '';
        for (var i = 0; i < 1; i++) {
          sample += mapPlotData[m]['s{0}'.format(i)] + '\n';
        }

        var aql = buildTimeGroupby(cp) +
          buildHashTagCountQuery(cp) +
          buildTweetSample(cp);

        A.aql(aql, function (res) {
          if (res.results) {
            if (res.results[0]) {
              drawTimeSerialBrush(res.results[0]);
            }
            if (res.results[1]) {
              drawWordCloud(res.results[1]);
            }
            if (res.results[2]) {
              drawTable(res.results[2]);
            }
          }
          return;
        }, "synchronous");

        reportUserMessage("use dataverse " + A._properties['dataverse'] + ";\n" + aql, true, 'report-query');
      });

      polygons[mapPlotData[m].cell] = cp;
      //polygons[mapPlotData[m].cell].setMap(null);
      polygons[mapPlotData[m].cell].setMap(map);

      // Show legend
      $("#legend-min").html(minWeight);
      $("#legend-max").html(maxWeight);
      $("#rainbow-legend-container").show();
    }
  });

  console.timeEnd("query_aql_draw");

  updateDashBoard(mapPlotData.slice(0, 50));
}

function updateDashBoard(mapPlotData) {
  drawChart(mapPlotData);
  drawPie(mapPlotData);
}

function drawChart(cell_count) {
  var margin = {top: 20, right: 20, bottom: 30, left: 50},
    width = 800 - margin.left - margin.right,
    height = 300 - margin.top - margin.bottom;

  var x = d3.scale.ordinal()
    .rangeRoundBands([0, width], .1);

  var y = d3.scale.linear()
    .range([height, 0]);

  var xAxis = d3.svg.axis()
    .scale(x)
    .orient("bottom");

  var yAxis = d3.svg.axis()
    .scale(y)
    .orient("left");

  var area = d3.svg.area()
    .x(function (d) {
      return x(d.cell);
    })
    .y0(height)
    .y1(function (d) {
      return y(d.count);
    });

  d3.select("#svg-chart").remove();
  var svg = d3.select("#chart").append("svg")
    .attr("id", "svg-chart")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
    .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

  x.domain(cell_count.map(function (d) {
    return d.cell;
  }));
  y.domain([0, d3.max(cell_count, function (d) {
    return d.count;
  })]);

  var tip = d3.tip()
    .attr('class', 'd3-tip')
    .offset([-10, 0])
    .html(function (d) {
      return "<span style='color:red'>" + d.cell + ":" + d.count + "</span>";
    });

  svg.append("g")
    .attr("class", "x axis")
    .attr("transform", "translate(0," + height + ")")
    .call(xAxis)
    .selectAll("text")   // rotate tab
    .style("text-anchor", "end")
    .attr("dx", "-.8em")
    .attr("dy", ".15em")
    .attr("transform", "rotate(-65)" );

  svg.append("g")
    .attr("class", "y axis")
    .call(yAxis)
    .append("text")
    .attr("transform", "rotate(-90)")
    .attr("y", 6)
    .attr("dy", ".71em")
    .style("text-anchor", "end")
    .text("Count");

  svg.call(tip);

  var bars = svg.selectAll(".bar")
    .data(cell_count)
    .enter().append("g")
    .attr("class", "bar");

  bars.append("rect")
    .attr("x", function (d) {
      return x(d.cell);
    })
    .attr("width", x.rangeBand())
    .attr("y", function (d) {
      return y(d.count);
    })
    .attr("height", function (d) {
      return height - y(d.count);
    })
    .on('mouseover', tip.show)
    .on('mouseout', tip.hide);

}


function drawPie(cell_count) {
  var sum = d3.sum(cell_count, function (d) {
    return d.count
  });
  var margin = {top: 20, right: 20, bottom: 30, left: 50},
    width = 800 - margin.left - margin.right,
    height = 300 - margin.top - margin.bottom,
    radius = Math.min(width, height) / 2;

  var color = d3.scale.ordinal()
    .range(["#98abc5", "#8a89a6", "#7b6888", "#6b486b", "#a05d56", "#d0743c", "#ff8c00"]);

  var arc = d3.svg.arc()
    .outerRadius(radius - 10)
    .innerRadius(0);

  var labelArc = d3.svg.arc()
    .outerRadius(radius - 40)
    .innerRadius(radius - 40);

  var pie = d3.layout.pie()
    .sort(null)
    .value(function (d) {
      return d.count;
    });

  var tip = d3.tip()
    .attr('class', 'd3-tip')
    .offset([-10, 0])
    .html(function (d) {
      return "<span style='color:red'>" + d.data.cell + ":" + (100 * d.data.count / sum).toFixed(2) + "%" + "</span>";
    });

  d3.select("#svg-pie").remove();
  var svg = d3.select("#pie").append("svg")
    .attr("id", "svg-pie")
    .attr("width", width)
    .attr("height", height)
    .append("g")
    .attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");

  svg.call(tip);

  var g = svg.selectAll(".arc")
    .data(pie(cell_count))
    .enter().append("g")
    .attr("class", "arc")

  g.append("path")
    .attr("d", arc)
    .style("fill", function (d) {
      return color(d.data.cell);
    })
    .on('mouseover', tip.show)
    .on('mouseout', tip.hide);

  g.append("text")
    .attr("transform", function (d) {
      return "translate(" + labelArc.centroid(d) + ")";
    })
    .attr("dy", ".35em")
    .text(function (d) {
      return "";
      //return d.data.cell + ((100 * d.data.count)/sum).toFixed(2) + "%";
    });
}

function drawTimeSerialBrush(slice_count) {

  var margin = {top: 10, right: 10, bottom: 100, left: 40},
    margin2 = {top: 430, right: 10, bottom: 20, left: 40},
    width = 800 - margin.left - margin.right,
    height = 500 - margin.top - margin.bottom,
    height2 = 500 - margin2.top - margin2.bottom;

  var parseDate = d3.time.format("%Y-%m-%d %H").parse;

  var x = d3.time.scale().range([0, width]),
    x2 = d3.time.scale().range([0, width]),
    y = d3.scale.linear().range([height, 0]),
    y2 = d3.scale.linear().range([height2, 0]);

  var xAxis = d3.svg.axis().scale(x).orient("bottom"),
    xAxis2 = d3.svg.axis().scale(x2).orient("bottom"),
    yAxis = d3.svg.axis().scale(y).orient("left");

  var brush = d3.svg.brush()
    .x(x2)
    .on("brush", brushed);

  var area = d3.svg.area()
    .interpolate("monotone")
    .x(function (d) {
      return x(d.slice);
    })
    .y0(height)
    .y1(function (d) {
      return y(d.count);
    });

  var area2 = d3.svg.area()
    .interpolate("monotone")
    .x(function (d) {
      return x2(d.slice);
    })
    .y0(height2)
    .y1(function (d) {
      return y2(d.count);
    });

  d3.select("#svg-time").remove();
  var svg = d3.select("#timeseries").append("svg")
    .attr("id", "svg-time")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom);

  svg.append("defs").append("clipPath")
    .attr("id", "clip")
    .append("rect")
    .attr("width", width)
    .attr("height", height);

  var focus = svg.append("g")
    .attr("class", "focus")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

  var context = svg.append("g")
    .attr("class", "context")
    .attr("transform", "translate(" + margin2.left + "," + margin2.top + ")");

  slice_count.forEach(type);
  x.domain(d3.extent(slice_count.map(function (d) {
    return d.slice;
  })));
  y.domain([0, d3.max(slice_count.map(function (d) {
    return d.count;
  }))]);
  x2.domain(x.domain());
  y2.domain(y.domain());

  focus.append("path")
    .datum(slice_count)
    .attr("class", "area")
    .attr("d", area);

  focus.append("g")
    .attr("class", "x axis")
    .attr("transform", "translate(0," + height + ")")
    .call(xAxis);

  focus.append("g")
    .attr("class", "y axis")
    .call(yAxis);

  context.append("path")
    .datum(slice_count)
    .attr("class", "area")
    .attr("d", area2);

  context.append("g")
    .attr("class", "x axis")
    .attr("transform", "translate(0," + height2 + ")")
    .call(xAxis2);

  context.append("g")
    .attr("class", "x brush")
    .call(brush)
    .selectAll("rect")
    .attr("y", -6)
    .attr("height", height2 + 7);

  function brushed() {
    x.domain(brush.empty() ? x2.domain() : brush.extent());
    focus.select(".area").attr("d", area);
    focus.select(".x.axis").call(xAxis);
  }

  function type(d) {
    d.slice = parseDate(d.slice);
    d.count = +d.count;
    return d;
  }

  console.log('finished refining query');
}

function drawWordCloud(tag_count) {

  tag_count = tag_count.slice(0, 50);
  var color = d3.scale.linear()
    .domain([0, 1, 2, 3, 4, 5, 6, 10, 15, 20, 100])
    .range(["#ddd", "#ccc", "#bbb", "#aaa", "#999", "#888", "#777", "#666", "#555", "#444", "#333", "#222"]);

  var size = d3.scale.linear()
    .domain([0, d3.max(tag_count.map(function (t) {
      return t.count
    }))])
    .range([10, 60]);

  d3.layout.cloud().size([850, 550])
    .words(tag_count)
    .rotate(0)
    .fontSize(function (d) {
      return size(d.count);
    })
    .on("end", draw)
    .start();

  function draw(words) {
    d3.select("#svg-word-cloud").remove();
    d3.select("#wordcloud").append("svg")
      .attr("id", "svg-word-cloud")
      .attr("width", 850)
      .attr("height", 550)
      .attr("class", "wordcloud")
      .append("g")
      // without the transform, words words would get cutoff to the left and top, they would
      // appear outside of the SVG area
      .attr("transform", "translate(350,200)")
      .selectAll("text")
      .data(words)
      .enter().append("text")
      .style("font-size", function (d) {
        return size(d.count) + "px";
      })
      .style("fill", function (d, i) {
        return color(i);
      })
      .attr("transform", function (d) {
        return "translate(" + [d.x, d.y] + ")rotate(" + d.rotate + ")";
      })
      .text(function (d) {
        return d.tag;
      });
  }
}

function drawTable(message) {
  $("#tweets-table").show();
  $("#grid").jqGrid('clearGridData');
  for (var i = 0; i < message.length; i++) {
    $("#grid").jqGrid('addRowData', undefined , message[i]);
  }
}

/**
 * Explore mode: Initial map creation and screen alignment
 */
function onOpenExploreMap() {
  var explore_column_height = $('#explore-well').height();
  var right_column_width = $('#right-col').width();
  $('#map_canvas').height(explore_column_height + "px");
  $('#map_canvas').width(right_column_width + "px");

  $('#review-well').height(explore_column_height + "px");
  $('#review-well').css('max-height', explore_column_height + "px");
  $('#right-col').height(explore_column_height + "px");
}

/**
 * initializes demo - adds some extra events when review/explore
 * mode are clicked, initializes tabs, aligns map box, moves
 * active tab to about tab
 */
function initDemoPrepareTabs() {

  // Tab behavior for About, Explore, and Demo
  $('#mode-tabs a').click(function (e) {
    e.preventDefault();
    $(this).tab('show')
  });

  // Explore mode should show explore-mode query-builder UI
  $('#explore-mode').click(function (e) {
    $('#review-well').hide();
    $('#explore-well').show();
    rectangleManager.setMap(map);
    rectangleManager.setDrawingMode(google.maps.drawing.OverlayType.RECTANGLE);
    mapWidgetResetMap();
  });

  // Review mode should show review well and hide explore well
  $('#review-mode').click(function (e) {
    $('#explore-well').hide();
    $('#review-well').show();
    mapWidgetResetMap();
    rectangleManager.setMap(null);
    rectangleManager.setDrawingMode(null);
  });

  // Does some alignment necessary for the map canvas
  onOpenExploreMap();
}

/**
 * Creates a message and attaches it to data management area.
 * @param    {String}    message, a message to post
 * @param    {Boolean}   isPositiveMessage, whether or not this is a positive message.
 * @param    {String}    target, the target div to attach this message.
 */
function reportUserMessage(message, isPositiveMessage, target) {
  // Clear out any existing messages
  $('#' + target).html('');

  message = message.replace(/\r\n?|\n/g, '<br />');
  // Select appropriate alert-type
  var alertType = "alert-success";
  if (!isPositiveMessage) {
    alertType = "alert-danger";
  }

  console.log(message);
  // Append the appropriate message
  $('<div/>')
    .attr("class", "alert " + alertType)
    .html('<button type="button" class="close" data-dismiss="alert">&times;</button>' + message)
    .appendTo('#' + target);
}

/**
 * mapWidgetResetMap
 *
 * [No Parameters]
 *
 * Clears ALL map elements - plotted items, overlays, then resets position
 */
function mapWidgetResetMap() {

  mapWidgetClearMap();
  clearReport();
  clearD3();

  // Reset map center and zoom
  map.setCenter(new google.maps.LatLng(39.5, -96.35));
  map.setZoom(4);

  // Selection button
  $("#selection-button").trigger("click");
  $("#state-button").trigger("click");
  rectangleManager.setMap(map);
  rectangleManager.setDrawingMode(google.maps.drawing.OverlayType.RECTANGLE);
}

function clearReport() {
  $('#report-query').html('');
  $('#report-message').html('');
}

function clearD3() {
  $("#chart").html('');
  $("#pie").html('');
  $("#timeseries").html('');
  $("#wordcloud").html('');
  $("#grid").html('');
}


/**
 * mapWidgetClearMap
 * Removes data/markers
 */
function mapWidgetClearMap() {

  // Remove previously plotted data/markers
  for (c in map_cells) {
    map_cells[c].setMap(null);
  }
  map_cells = [];

  label_marker.setVisible(false);

  for (m in map_tweet_markers) {
    map_tweet_markers[m].setMap(null);
  }
  map_tweet_markers = [];

  // Hide legend
  $("#rainbow-legend-container").hide();

  // Reenable submit button
  $("#submit-button").attr("disabled", false);

  // Hide selection rectangle
  if (selectionRectangle) {
    selectionRectangle.setMap(null);
    selectionRectangle = null;
  }

  for (var cName in county_polygons) {
    county_polygons[cName].fillColor = 'blue';
    county_polygons[cName].fillOpacity = 0.4;
    county_polygons[cName].setMap(null);
  }

  for (var cName in state_polygons) {
    state_polygons[cName].fillColor = 'blue';
    state_polygons[cName].fillOpacity = 0.4;
    state_polygons[cName].setMap(null);
  }
  for (var poly in city_polygons) {
    city_polygons[poly].setMap(null);
  }
  $("#tweets-table").hide();
}

/**
 * buildLegend
 *
 * Generates gradient, button action for legend bar
 */
function buildLegend() {

  // Fill in legend area with colors
  var gradientColor;

  for (i = 0; i < 100; i++) {
    //$("#rainbow-legend-container").append("" + rainbow.colourAt(i));
    $("#legend-gradient").append('<div style="display:inline-block; max-width:2px; background-color:#' + rainbow.colourAt(i) + ';">&nbsp;</div>');
  }
}
