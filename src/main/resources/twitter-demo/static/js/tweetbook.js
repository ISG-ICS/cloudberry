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
  var geo_query_all_counties = 'for $x in dataset ds_us_county return { "county":$x.State-County, "geometry":$x.geometry};';
  var drawCountyHandler = drawBoundry('county', 'geometry', county_polygons, null);
  console.time("initial_polygon_aql_get_result");
  A.query(geo_query_all_counties, drawCountyHandler, "synchronous");
}

function drawStateBoundry() {
  var geo_query_all_counties = 'for $x in dataset ds_us_state return { "state":$x.abbr, "geometry":$x.geometry};';
  var drawCountyHandler = drawBoundry('state', 'geometry', state_polygons, map);
  console.time("initial_polygon_aql_get_result");
  A.query(geo_query_all_counties, drawCountyHandler, "synchronous");
}

function drawBoundry(key_name, geometry_name, polygonStores, targetMap) {

  return function (polygon_results) {
    console.timeEnd("initial_polygon_aql_get_result");
    console.time("initial_polygon_drawing");
    polygon_results = polygon_results.results;
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
        fillOpacity: 0.4
      });

      polygonStores[key].setMap(targetMap);
    }
    console.timeEnd("initial_polygon_drawing");
  }
}

function initDemoUIButtonControls() {

  // Explore Mode - Update Sliders
  //var updateSliderDisplay = function (event, ui) {
  //    if (event.target.id == "grid-lat-slider") {
  //        $("#gridlat").text("" + ui.value);
  //    } else {
  //        $("#gridlng").text("" + ui.value);
  //    }
  //};
  //sliderOptions = {
  //    max: 10,
  //    min: 2.0,
  //    step: .1,
  //    value: 3.0,
  //    slidechange: updateSliderDisplay,
  //    slide: updateSliderDisplay,
  //    start: updateSliderDisplay,
  //    stop: updateSliderDisplay
  //};
  //$("#gridlat").text("" + sliderOptions.value);
  //$("#gridlng").text("" + sliderOptions.value);
  //$(".grid-slider").slider(sliderOptions);

  // Explore Mode - Query Builder Date Pickers
  var dateOptions = {
    dateFormat: "yy-mm-dd",
    defaultDate: "2015-11-11",
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

    if (build_tweetbook_mode == "synchronous") {
      console.time("query_aql_get_result");
      A.query(f, tweetbookQuerySyncCallbackWithLevel(level), build_tweetbook_mode);
    } else {
      A.query(f, tweetbookQueryAsyncCallback, build_tweetbook_mode);
    }

    // Clears selection rectangle on query execution, rather than waiting for another clear call.
    if (selectionRectangle) {
      selectionRectangle.setMap(null);
      selectionRectangle = null;
    }
  });
}

/**
 * Builds AsterixDB REST Query from explore mode form.
 */
function buildAQLQueryFromForm(parameters) {

  var level = parameters["level"];

  var bounds = {
    "ne": {"lat": parameters["neLat"], "lng": -1 * parameters["neLng"]},
    "sw": {"lat": parameters["swLat"], "lng": -1 * parameters["swLng"]}
  };

  var aql = [];
  aql.push('let $region := create-rectangle(create-point({0},{1}),\n create-point({2},{3}))'.format(bounds['sw']['lng'],
    bounds['sw']['lat'], bounds['ne']['lng'], bounds['ne']['lat']));
  aql.push('let $ts_start := datetime("{0}")'.format(parameters['startdt']));
  aql.push('let $ts_end := datetime("{0}")'.format(parameters['enddt']));

  var ds_for = 'for $t in dataset ds_tweets ';
  var ds_predicate = 'where $t.place.country = "United States" and $t.place.place_type = "city" \n' +
    'and spatial-area($t.place.bounding_box) < 5 \n' +
    'and $t.create_at >= $ts_start and $t.create_at < $ts_end \n' +
    'and spatial-intersect($t.place.bounding_box, $region) \n';

  if (parameters["keyword"].length > 0) {
    ds_predicate = 'let $keyword := "{0}"\n'.format(parameters['keyword']) + ds_predicate +
      'and contains($t.text_msg, $keyword) \n';
  }

  var sample = '';
  for (var i = 0; i < 1; i++) {
    if (i > 0) {
      sample += ',';
    }
    sample += ' "s{0}":$t[{1}].text_msg'.format(i, i);
  }

  if (level === 'county') {
    aql.push('let $join := {0}'.format(ds_for));
    aql.push(ds_predicate);
    aql.push('return { "text_msg": $t.text_msg,\n\
      "county": (for $city in dataset ds_zip\n\
      where substring-before($t.place.full_name, ",") = $city.city \n\
      and substring-after($t.place.full_name, ", ") = $city.state \n\
      and not(is-null($city.county))\n\
      return string-concat([$city.state, "-", $city.county]) )[0]}\n\
    for $t in $join\n\
      group by $c := $t.county with $t \n\
      let $count := count($t) \n\
      order by $count desc \n\
    return { "cell" : $c, "count" : $count, {0} }'.format(sample));
  } else {
    aql.push(ds_for);
    aql.push(ds_predicate);
    if (parameters['level'] === "state") {
      aql.push('group by $c := substring-after($t.place.full_name, ", ") with $t');
      aql.push('let $count := count($t)');
      aql.push('order by $count desc');
      aql.push('return { "cell":$c, "count" : count($t), {0} };'.format(sample));
    } else if (parameters['level'] === "city") {
      aql.push('group by $c := $t.place.full_name with $t');
      aql.push('let $count := count($t)');
      aql.push('order by $count desc');
      aql.push('return {"cell":$c, "count" : count($t), "area": $t[0].place.bounding_box, {0} };'.format(sample));
    }
  }

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

/**
 * On-success callback after async API query
 * @param    {object}    res, a result object containing an opaque result handle to Asterix
 */
function tweetbookQueryAsyncCallback(res) {

  // Parse handle, handle id and query from async call result
  var handle_query = APIqueryTracker["query"];
  var handle = res;
  var handle_id = res["handle"].toString().split(',')[0];

  // Add to stored map of existing handles
  asyncQueryManager[handle_id] = {
    "handle": handle,
    "query": handle_query, // This will show up when query control button is clicked.
    "data": APIqueryTracker["data"]
  };

  // Create a container for this async query handle
  $('<div/>')
    .css("margin-left", "1em")
    .css("margin-bottom", "1em")
    .css("display", "block")
    .attr({
      "class": "btn-group",
      "id": "async_container_" + handle_id
    })
    .appendTo("#async-handle-controls");

  // Adds the main button for this async handle
  var handle_action_button = '<button class="btn btn-disabled" id="handle_' + handle_id + '">Handle ' + handle_id + '</button>';
  $('#async_container_' + handle_id).append(handle_action_button);
  $('#handle_' + handle_id).prop('disabled', true);
  $('#handle_' + handle_id).on('click', function (e) {

    // make sure query is ready to be run
    if (asyncQueryManager[handle_id]["ready"]) {

      APIqueryTracker = {
        "query": asyncQueryManager[handle_id]["query"],
        "data": asyncQueryManager[handle_id]["data"]
      };

      if (!asyncQueryManager[handle_id].hasOwnProperty("result")) {
        // Generate new Asterix Core API Query
        A.query_result(
          {"handle": JSON.stringify(asyncQueryManager[handle_id]["handle"])},
          function (res) {
            asyncQueryManager[handle_id]["result"] = res;

            var resultTransform = {
              "results": res.results
            };

            tweetbookQuerySyncCallback(resultTransform);
          }
        );
      } else {

        var resultTransform = {
          "results": asyncQueryManager[handle_id]["result"].results
        };

        tweetbookQuerySyncCallback(resultTransform);
      }
    }
  });

  // Adds a removal button for this async handle
  var asyncDeleteButton = addDeleteButton(
    "trashhandle_" + handle_id,
    "async_container_" + handle_id,
    function (e) {
      $('#async_container_' + handle_id).remove();
      delete asyncQueryManager[handle_id];
    }
  );

  $('#async_container_' + handle_id).append('<br/>');

  $("#submit-button").attr("disabled", false);
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
    $.each(res.results, function (i, data) {

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
    triggerUIUpdate(res.results, maxWeight, minWeight, polygons, level);
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
          fillOpacity: 0.4
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
        var sample = '';
        for (var i = 0; i < 1; i++) {
          sample += mapPlotData[m]['s{0}'.format(i)] + '\n';
        }
        var bounds = map.getBounds();
        var lat = bounds.getNorthEast().lat();
        var lng = bounds.getSouthWest().lng();
        var lat_range = Math.abs(bounds.getNorthEast().lat() - bounds.getSouthWest().lat());
        var lng_range = Math.abs(bounds.getNorthEast().lng() - bounds.getSouthWest().lng());
        lat = lat - lat_range / 10;
        lng = lng + lng_range / 10;

        var boundary = 100;
        if (sample.length > boundary) {
          sample = sample.substr(0, boundary) + "<br>" + sample.substr(boundary);
        }
        sample_marker.setPosition({lat: lat, lng: lng});
        //sample_marker.setPosition(map.getCenter());
        sample_marker.labelContent = sample;
        sample_marker.label.draw();
        sample_marker.setVisible(true);
        reportUserMessage(sample, true, 'report-sample');
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
  var svg = d3.select("#dashboard").append("svg")
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
    .call(xAxis);

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
  var svg = d3.select("#dashboard").append("svg")
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
/**
 * prepares an Asterix API query to drill down in a rectangular spatial zone
 *
 * @params {object} marker_borders a set of bounds for a region from a previous api result
 */
function onMapPointDrillDown(marker_borders) {

  var zoneData = APIqueryTracker["data"];

  var zswBounds = new google.maps.LatLng(marker_borders.latSW, marker_borders.lngSW);
  var zneBounds = new google.maps.LatLng(marker_borders.latNE, marker_borders.lngNE);

  var zoneBounds = new google.maps.LatLngBounds(zswBounds, zneBounds);
  zoneData["swLat"] = zoneBounds.getSouthWest().lat();
  zoneData["swLng"] = zoneBounds.getSouthWest().lng();
  zoneData["neLat"] = zoneBounds.getNorthEast().lat();
  zoneData["neLng"] = zoneBounds.getNorthEast().lng();
  var zB = {
    "sw": {
      "lat": zoneBounds.getSouthWest().lat(),
      "lng": zoneBounds.getSouthWest().lng()
    },
    "ne": {
      "lat": zoneBounds.getNorthEast().lat(),
      "lng": zoneBounds.getNorthEast().lng()
    }
  };

  mapWidgetClearMap();

  var customBounds = new google.maps.LatLngBounds();
  var zoomSWBounds = new google.maps.LatLng(zoneData["swLat"], zoneData["swLng"]);
  var zoomNEBounds = new google.maps.LatLng(zoneData["neLat"], zoneData["neLng"]);
  customBounds.extend(zoomSWBounds);
  customBounds.extend(zoomNEBounds);
  map.fitBounds(customBounds);

  var df = getDrillDownQuery(zoneData, zB);

  APIqueryTracker = {
    "query_string": "use dataverse geo;\n" + df.val(),
    "marker_path": "static/img/mobile2.png"
  };

  A.query(df.val(), onTweetbookQuerySuccessPlot);
}

/**
 * Generates an aql query for zooming on a spatial cell and obtaining tweets contained therein.
 * @param parameters, the original query parameters
 * @param bounds, the bounds of the zone to zoom in on.
 */
function getDrillDownQuery(parameters, bounds) {

  var zoomRectangle = new FunctionExpression("create-rectangle",
    new FunctionExpression("create-point", bounds["sw"]["lat"], bounds["sw"]["lng"]),
    new FunctionExpression("create-point", bounds["ne"]["lat"], bounds["ne"]["lng"]));

  var drillDown = new FLWOGRExpression()
    .ForClause("$t", new AExpression("dataset TweetsAddress"))
    .LetClause("$region", zoomRectangle);

  if (parameters["keyword"].length == 0) {
    drillDown = drillDown
      .WhereClause().and(
        new AExpression().set('$t.place.country = "United States"'),
        new AExpression().set('$t.place.place_type = "city"'),
        new FunctionExpression('spatial-intersect', '$t.place.bounding_box', '$region'),
        new AExpression().set('$t.create_at > datetime("' + parameters["startdt"] + '")'),
        new AExpression().set('$t.create_at < datetime("' + parameters["enddt"] + '")')
      );
  } else {
    drillDown = drillDown
      .LetClause("$keyword", new AExpression('"' + parameters["keyword"] + '"'))
      .WhereClause().and(
        new AExpression().set('$t.place.country = "United States"'),
        new AExpression().set('$t.place.place_type = "city"'),
        new FunctionExpression('spatial-intersect', '$t.place.bounding_box', '$region'),
        new AExpression().set('$t.create_at > datetime("' + parameters["startdt"] + '")'),
        new AExpression().set('$t.create_at < datetime("' + parameters["enddt"] + '")'),
        new FunctionExpression('contains', '$t.text_msg', '$keyword')
      );
  }

  drillDown = drillDown
    .ReturnClause({
      "tweetId": "$t.tid",
      "tweetText": "$t.message-text",
      "tweetLoc": "$t.sender-location"
    });

  return drillDown;
}

/**
 * Given a location where a tweet exists, opens a modal to examine or update a tweet's content.
 * @param t0, a tweetobject that has a location, text, id, and optionally a comment.
 */
function onDrillDownAtLocation(tO) {

  var tweetId = tO["tweetEntryId"];
  var tweetText = tO["tweetText"];

  // First, set tweet in drilldown modal to be this tweet's text
  $('#modal-body-tweet').html('Tweet #' + tweetId + ": " + tweetText);

  // Next, empty any leftover tweetbook comments or error/success messages
  $("#modal-body-add-to").val('');
  $("#modal-body-add-note").val('');
  $("#modal-body-message-holder").html("");

  // Next, if there is an existing tweetcomment reported, show it.
  if (tO.hasOwnProperty("tweetComment")) {

    // Show correct panel
    $("#modal-existing-note").show();
    $("#modal-save-tweet-panel").hide();

    // Fill in existing tweet comment
    $("#modal-body-tweet-note").val(tO["tweetComment"]);

    // Change Tweetbook Badge
    $("#modal-current-tweetbook").val(APIqueryTracker["active_tweetbook"]);

    // Add deletion functionality
    $("#modal-body-trash-icon").on('click', function () {
      // Send comment deletion to asterix
      var deleteTweetCommentOnId = '"' + tweetId + '"';
      var toDelete = new DeleteStatement(
        "$mt",
        APIqueryTracker["active_tweetbook"],
        new AExpression("$mt.tid = " + deleteTweetCommentOnId.toString())
      );
      A.update(
        toDelete.val()
      );

      // Hide comment from map
      $('#drilldown_modal').modal('hide');

      // Replot tweetbook
      onPlotTweetbook(APIqueryTracker["active_tweetbook"]);
    });

  } else {
    // Show correct panel
    $("#modal-existing-note").hide();
    $("#modal-save-tweet-panel").show();

    // Now, when adding a comment on an available tweet to a tweetbook
    $('#save-comment-tweetbook-modal').unbind('click');
    $("#save-comment-tweetbook-modal").on('click', function (e) {

      // Stuff to save about new comment
      var save_metacomment_target_tweetbook = $("#modal-body-add-to").val();
      var save_metacomment_target_comment = '"' + $("#modal-body-add-note").val() + '"';
      var save_metacomment_target_tweet = '"' + tweetId + '"';

      // Make sure content is entered, and then save this comment.
      if ($("#modal-body-add-note").val() == "") {

        reportUserMessage("Please enter a comment about the tweet", false, "report-message");

      } else if ($("#modal-body-add-to").val() == "") {

        reportUserMessage("Please enter a tweetbook.", false, "report-message");

      } else {

        // Check if tweetbook exists. If not, create it.
        if (!(existsTweetbook(save_metacomment_target_tweetbook))) {
          onCreateNewTweetBook(save_metacomment_target_tweetbook);
        }

        var toInsert = new InsertStatement(
          save_metacomment_target_tweetbook,
          {
            "tid": save_metacomment_target_tweet.toString(),
            "comment-text": save_metacomment_target_comment
          }
        );

        A.update(toInsert.val(), function () {
          var successMessage = "Saved comment on <b>Tweet #" + tweetId +
            "</b> in dataset <b>" + save_metacomment_target_tweetbook + "</b>.";
          reportUserMessage(successMessage, true, "report-message");

          $("#modal-body-add-to").val('');
          $("#modal-body-add-note").val('');
          $('#save-comment-tweetbook-modal').unbind('click');

          // Close modal
          $('#drilldown_modal').modal('hide');
        });
      }
    });
  }
}

/**
 * Adds a new tweetbook entry to the menu and creates a dataset of type TweetbookEntry.
 */
function onCreateNewTweetBook(tweetbook_title) {

  var tweetbook_title = tweetbook_title.split(' ').join('_');

  A.ddl(
    "create dataset " + tweetbook_title + "(TweetbookEntry) primary key tid;",
    function () {
    }
  );

  if (!(existsTweetbook(tweetbook_title))) {
    review_mode_tweetbooks.push(tweetbook_title);
    addTweetBookDropdownItem(tweetbook_title);
  }
}

/**
 * Removes a tweetbook from both demo and from
 * dataverse metadata.
 */
function onDropTweetBook(tweetbook_title) {

  // AQL Call
  A.ddl(
    "drop dataset " + tweetbook_title + " if exists;",
    function () {
    }
  );

  // Removes tweetbook from review_mode_tweetbooks
  var remove_position = $.inArray(tweetbook_title, review_mode_tweetbooks);
  if (remove_position >= 0) review_mode_tweetbooks.splice(remove_position, 1);

  // Clear UI with review tweetbook titles
  $('#review-tweetbook-titles').html('');
  for (r in review_mode_tweetbooks) {
    addTweetBookDropdownItem(review_mode_tweetbooks[r]);
  }
}

/**
 * Adds a tweetbook action button to the dropdown box in review mode.
 * @param tweetbook, a string representing a tweetbook
 */
function addTweetBookDropdownItem(tweetbook) {
  // Add placeholder for this tweetbook
  $('<div/>')
    .attr({
      "class": "btn-group",
      "id": "rm_holder_" + tweetbook
    }).appendTo("#review-tweetbook-titles");

  // Add plotting button for this tweetbook
  var plot_button = '<button class="btn btn-default" id="rm_plotbook_' + tweetbook + '">' + tweetbook + '</button>';
  $("#rm_holder_" + tweetbook).append(plot_button);
  $("#rm_plotbook_" + tweetbook).width("200px");
  $("#rm_plotbook_" + tweetbook).on('click', function (e) {
    onPlotTweetbook(tweetbook);
  });

  // Add trash button for this tweetbook
  var onTrashTweetbookButton = addDeleteButton(
    "rm_trashbook_" + tweetbook,
    "rm_holder_" + tweetbook,
    function (e) {
      onDropTweetBook(tweetbook);
    }
  );
}

/**
 * Generates AsterixDB query to plot existing tweetbook commnets
 * @param tweetbook, a string representing a tweetbook
 */
function onPlotTweetbook(tweetbook) {

  // Clear map for this one
  mapWidgetClearMap();

  var plotTweetQuery = new FLWOGRExpression()
    .ForClause("$t", new AExpression("dataset TweetsAddress"))
    .ForClause("$m", new AExpression("dataset " + tweetbook))
    .WhereClause(new AExpression("$m.tid = $t.tid"))
    .ReturnClause({
      "tweetId": "$m.tid",
      "tweetText": "$t.message-text",
      "tweetCom": "$m.comment-text",
      "tweetLoc": "$t.sender-location"
    });

  APIqueryTracker = {
    "query_string": "use dataverse geo;\n" + plotTweetQuery.val(),
    "marker_path": "static/img/mobile_green2.png",
    "active_tweetbook": tweetbook
  };

  A.query(plotTweetQuery.val(), onTweetbookQuerySuccessPlot);
}

/**
 * Given an output response set of tweet data,
 * prepares markers on map to represent individual tweets.
 * @param res, a JSON Object
 */
function onTweetbookQuerySuccessPlot(res) {

  // First, we check if any results came back in.
  // If they didn't, return.
  if (!res.hasOwnProperty("results")) {
    reportUserMessage("Oops, no data matches this query.", false, "report-message");
    return;
  }

  // Parse out tweet Ids, texts, and locations
  var tweets = [];
  var al = 1;

  $.each(res.results, function (i, data) {

    // First, clean up the data
    //{ "tweetId": "100293", "tweetText": " like at&t the touch-screen is amazing", "tweetLoc": ["31.59,-84.23"]) }
    // We need to turn the point object at the end into a string

    // Now, we construct a tweet object
    var tweetData = {
      "tweetEntryId": parseInt(data.tweetId),
      "tweetText": data.tweetText,
      "tweetLat": data.tweetLoc[0],
      "tweetLng": data.tweetLoc[1]
    };

    // If we are parsing out tweetbook data with comments, we need to check
    // for those here as well.
    if (data.hasOwnProperty("tweetCom")) {
      tweetData["tweetComment"] = data.tweetCom;
    }

    tweets.push(tweetData)
  });

  // Create a marker for each tweet
  $.each(tweets, function (i, t) {
    // Create a phone marker at tweet's position
    var map_tweet_m = new google.maps.Marker({
      position: new google.maps.LatLng(tweets[i]["tweetLat"], tweets[i]["tweetLng"]),
      map: map,
      icon: APIqueryTracker["marker_path"],
      clickable: true,
    });
    map_tweet_m["test"] = t;

    // Open Tweet exploration window on click
    google.maps.event.addListener(map_tweet_m, 'click', function (event) {
      onClickTweetbookMapMarker(map_tweet_markers[i]["test"]);
    });

    // Add marker to index of tweets
    map_tweet_markers.push(map_tweet_m);
  });

}

/**
 * Checks if a tweetbook exists
 * @param tweetbook, a String
 */
function existsTweetbook(tweetbook) {
  if (parseInt($.inArray(tweetbook, review_mode_tweetbooks)) == -1) {
    return false;
  } else {
    return true;
  }
}

/**
 * When a marker is clicked on in the tweetbook, it will launch a modal
 * view to examine or edit the appropriate tweet
 */
function onClickTweetbookMapMarker(t) {
  onDrillDownAtLocation(t)
  $('#drilldown_modal').modal();
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
    e.preventDefault()
    $(this).tab('show')
  })

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
 * Creates a delete icon button using default trash icon
 * @param    {String}    id, id for this element
 * @param    {String}    attachTo, id string of an element to which I can attach this button.
 * @param    {Function}  onClick, a function to fire when this icon is clicked
 */
function addDeleteButton(iconId, attachTo, onClick) {

  var trashIcon = '<button class="btn btn-default" id="' + iconId + '"><span class="glyphicon glyphicon-trash"></span></button>';
  $('#' + attachTo).append(trashIcon);

  // When this trash button is clicked, the function is called.
  $('#' + iconId).on('click', onClick);
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

  // Reset map center and zoom
  map.setCenter(new google.maps.LatLng(39.5, -100.35));
  map.setZoom(4);

  // Selection button
  $("#selection-button").trigger("click");
  $("#state-button").trigger("click");
  rectangleManager.setMap(map);
  rectangleManager.setDrawingMode(google.maps.drawing.OverlayType.RECTANGLE);
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
