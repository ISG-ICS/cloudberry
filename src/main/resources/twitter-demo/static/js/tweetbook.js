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
// =============================   Inilization  =================================
$(function () {

  // Add a format function to String
  if (!String.prototype.format) {
    String.prototype.format = function () {
      var args = arguments;
      return this.replace(/{(\d+)}/g, function (match, number) {
        return typeof args[number] != 'undefined' ? args[number] : match;
      });
    };
  }

  TempDSName = "temp_" + Math.random().toString(36).substr(2, 9);

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
      var showReportAt = "aql";

      reportUserMessage(showErrorMessage, isPositive, showReportAt);
    }
  });

  // create map and add controls
  map = L.map('map').setView([39.5, -96.35,], 4);
  L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
    maxZoom: 18,
    id: 'jeremyli.p6f712pj',
    accessToken: 'pk.eyJ1IjoiamVyZW15bGkiLCJhIjoiY2lrZ2U4MWI4MDA4bHVjajc1am1weTM2aSJ9.JHiBmawEKGsn3jiRK_d0Gw'
  }).addTo(map);

  // geojson  ploygons
  countyPolygons = null;
  statePolygons = null;
  //city_polygons = null;

  // status
  init = true;
  zoomLevel = 4;
  logicLevel = 'state';

  // const
  initialStartDate = "2012-02-17T00:00:00.000Z";
  initialEndDate = getCurrentDate() + "T23:59:59.000Z";

  // style
  stateStyle = {
    fillColor: '#f7f7f7',
    weight: 2,
    opacity: 1,
    color: '#92c5de',
    dashArray: '3',
    fillOpacity: 0.2
  };

  countyStyle = {
    fillColor: '#f7f7f7',
    weight: 1,
    opacity: 1,
    color: '#92c5de',
    fillOpacity: 0.2
  };

  hoverStyle = {
    weight: 5,
    color: '#666',
    dashArray: '',
    fillOpacity: 0.7
  };


  setInfoControl(map);

  // add  zoom event  listener
  map.on('zoomend', function () {
    zoomLevel = map.getZoom();
    if (zoomLevel > 5) {
      logicLevel = 'county';
      if (!init) {
        queryWrapper('zoom');
      }
      map.removeLayer(statePolygons);
      map.addLayer(countyPolygons);
    } else if (zoomLevel <= 5) {
      logicLevel = 'state';
      if (!init) {
        queryWrapper("zoom");
      }
      map.removeLayer(countyPolygons);
      map.addLayer(statePolygons);
    }
  });

  // add drag event listener
  map.on("dragend", function () {
    if (logicLevel != 'state')
      queryWrapper("drag");
  });

  // submit button
  $("#submit-button").on("click", function () {
    init = false;
    queryWrapper("submit");
  });

  $(document).keypress(function (e) {
    if (e.which == 13) {
      e.preventDefault();
      $("#submit-button").click();
    }
  });

  $('#sidebar').click(function(e){
    e.preventDefault();
    if($("#sidebar").attr("class")!="toggled")
      $("#sidebar").toggleClass("toggled");
    else{
      if($(e.target).attr("id")=="sidebar")
        $("#sidebar").toggleClass("toggled");
}
  });
});

function getCurrentDate() {
  var date = new Date();
  return toAQLDate(date);
}

function padding(integer) {
  return integer < 10 ? "0" + integer : integer;
}

function toAQLDate(date) {
  var month = date.getMonth() + 1
  var day = date.getDate();
  return date.getFullYear() + "-" + padding(month) + "-" + padding(day);
}

function toAQLTime(time) {
  var hours = time.getHours();
  var minus = time.getMinutes();
  var seconds = time.getSeconds();
  return padding(hours) + ":" + padding(minus) + ":" + padding(seconds);
}

function toAQLDateTime(dateTime) {
  return toAQLDate(dateTime) + "T" + toAQLTime(dateTime) + "Z";
}

function setInfoControl(map) {
  // Interaction function
  function highlightFeature(e) {
    var layer = e.target;

    layer.setStyle(hoverStyle);

    if (!L.Browser.ie && !L.Browser.opera) {
      layer.bringToFront();
    }
    info.update(layer.feature.properties);
  }

  function resetHighlight(e) {
    var style;
    if (!init)
      style = {
        weight: 2,
        fillOpacity: 0.5,
        color: 'white'
      };
    else
      style = {
        weight: 1,
        fillOpacity: 0.2,
        color: '#92c5de'
      }
    if (logicLevel == "state")
      statePolygons.setStyle(style);
    else
      countyPolygons.setStyle(style);
    info.update();
  }

  function zoomToFeature(e) {
    map.fitBounds(e.target.getBounds());
  }

  function onEachFeature(feature, layer) {
    layer.on({
      mouseover: highlightFeature,
      mouseout: resetHighlight,
      click: zoomToFeature
    });
  }

  // add info control
  var info = L.control();

  info.onAdd = function (map) {
    this._div = L.DomUtil.create('div', 'info'); // create a div with a class "info"
    this.update();
    return this._div;
  };

  // method that we will use to update the control based on feature properties passed
  info.update = function (props) {
    this._div.innerHTML = '<h4>Count by ' + logicLevel + '</h4>' + (props ?
      '<b>' + props.NAME + '</b><br />' + 'Count: ' + (props.count ? props.count : 0) : 'Hover over a state');
  };

  info.options = { position: 'topleft' };

  info.addTo(map);

  loadGeoJsonFiles(onEachFeature);
}

function loadGeoJsonFiles(onEachFeature) {
  state_hash = {}
  $.getJSON('static/data/states_hash.json', function (data) {
    state_hash = data;
  });

  state = {}
  $.getJSON('static/data/state.shape.20m.json', function (data) {
    state = data;
    statePolygons = L.geoJson(data, {
      style: stateStyle,
      onEachFeature: onEachFeature
    });
    statePolygons.addTo(map);
  });

  county = {}
  $.getJSON('static/data/county.shape.20m.json', function (data) {
    county = data;
    countyPolygons = L.geoJson(data, {
      style: countyStyle,
      onEachFeature: onEachFeature
    });
  });

}
// =============================   Query Builder Functions  =================================
/**
 * buildAQL helper function
 **/
function declareRectangle(bounds) {
  return 'let $region := create-rectangle(create-point({0},{1}),\n create-point({2},{3}))'.format(bounds['sw']['lng'],
    bounds['sw']['lat'], bounds['ne']['lng'], bounds['ne']['lat'])
}

/**
 * buildAQL helper function
 **/
function buildTemporaryDataset(parameters) {

  var bounds = {
    "ne": {
      "lat": parameters["neLat"],
      "lng": -1 * parameters["neLng"]
    },
    "sw": {
      "lat": parameters["swLat"],
      "lng": -1 * parameters["swLng"]
    }
  };

  var aql = [];
  aql.push('drop dataset {0} if exists;'.format(TempDSName));
  aql.push('create temporary dataset {0}(type_tweet) primary key id; '.format(TempDSName));
  aql.push('insert into dataset {0} ('.format(TempDSName));
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
      ds_predicate += 'and similarity-jaccard(word-tokens($t.text_msg), word-tokens($keyword{0})) > 0.0 \n'.format(i);
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

/**
 * Builds AsterixDB REST Query from the form.
 */
function buildAQLQueryFromForm(parameters, type) {
  var aql;
  if (type != "time")
    aql = buildTemporaryDataset(parameters);
  else
    aql = [];

  aql.push(buildSpatial(type, parameters));
  aql.push(buildTimeGroupby(type, parameters));
  aql.push(buildHashTagCountQuery(type, parameters));
  aql.push(buildTweetSample(type, parameters));
  return aql.join('\n');
}

function buildSpatial(type, parameters) {
  var aql = [];
  aql.push('for $t in dataset {0}'.format(TempDSName));
  var level = parameters["level"];

  if (type == "time") {
    aql.push('let $ts_start := datetime("{0}")'.format(parameters['startdt']));
    aql.push('let $ts_end := datetime("{0}")'.format(parameters['enddt']));
    aql.push('where $t.create_at >= $ts_start and $t.create_at < $ts_end');
  }
  if (level === 'county') {
    aql.push('group by $c := $t.county with $t \
      let $count := count($t) \n\
      order by $count desc \n\
    return { "cell" : $c, "count" : $count};');
  } else if (level === "state") {
    aql.push('group by $c := substring-after($t.place.full_name, ", ") with $t');
    aql.push('let $count := count($t)');
    aql.push('order by $count desc');
    aql.push('return { "cell":$c, "count" : $count };');
  } else if (level === "city") {
    aql.push('group by $c := $t.place.full_name with $t');
    aql.push('let $count := count($t)');
    aql.push('order by $count desc');
    aql.push('return {"cell":$c, "count" : $count, "area": $t[0].place.bounding_box };');
  }
  return aql.join('\n');
}
/**
 * query tmp date set to get result group by time
 * @ param string  query type
 * @parameters    query parameters
 **/
function buildTimeGroupby(type, parameters) {
  var aql = [];
  aql.push('for $t in dataset {0}'.format(TempDSName));
  if (type == "time") {
    aql.push('let $ts_start := datetime("{0}")'.format(parameters['startdt']));
    aql.push('let $ts_end := datetime("{0}")'.format(parameters['enddt']));
    aql.push('where $t.create_at >= $ts_start and $t.create_at < $ts_end');
  }
  aql.push('group by $c := print-datetime($t.create_at, "YYYY-MM-DD hh") with $t ');
  aql.push('let $count := count($t)');
  aql.push('order by $c ');
  aql.push('return {"slice":$c, "count" : $count };\n');
  return aql.join('\n');
}

/**
 * query tmp date set to get result group by hashtag
 * @ param string  query type
 * @parameters    query parameters
 **/
function buildHashTagCountQuery(type, parameters) {
  var aql = [];

  aql.push('for $t in dataset {0}'.format(TempDSName));
  if (type == "time") {
    aql.push('let $ts_start := datetime("{0}")'.format(parameters['startdt']));
    aql.push('let $ts_end := datetime("{0}")'.format(parameters['enddt']));
  }
  aql.push('where not(is-null($t.hashtags))');
  if (type == "time") {
    aql.push('and $t.create_at >= $ts_start and $t.create_at < $ts_end');
  }
  aql.push('for $h in $t.hashtags');
  aql.push('group by $tag := $h with $h');
  aql.push('let $c := count($h) ');
  aql.push('order by $c desc ');
  aql.push('limit 50 ');
  aql.push('return { "tag": $tag, "count": $c};\n');
  return aql.join('\n');
}

/**
 * query tmp date set to get tweets
 * @ param string  query type
 * @parameters    query parameters
 **/
function buildTweetSample(type, parameters) {
  var aql = [];
  aql.push('for $t in dataset {0}'.format(TempDSName));
  if (type == "time") {
    aql.push('let $ts_start := datetime("{0}")'.format(parameters['startdt']));
    aql.push('let $ts_end := datetime("{0}")'.format(parameters['enddt']));
    aql.push('where $t.create_at >= $ts_start and $t.create_at < $ts_end');
  }
  aql.push('limit 10');
  aql.push('return {"uname": $t.user.screen_name, "tweet":$t.text_msg, "id":string($t.id)};\n')
  return aql.join('\n');
}

// =============================   Query Functions  =================================

/**
 * A wrapper function for query
 * @ param string  query type: {"submit", "zoom", "time", "drag"}
 **/
function queryWrapper(type) {
  $("#aql").html('');

  // build form data
  var kwterm = $("#keyword-textbox").val();
  if (kwterm.trim().length < 1) {
    alert("please provide at least one keyword")
    return;
  }
  var formData = {
    "keyword": kwterm,
    "level": logicLevel
  };

  // Set time
  if (type == "time") {
    formData.startdt = brush_start;
    formData.enddt = brush_end;
  } else {
    formData.startdt = initialStartDate;
    formData.enddt = initialEndDate;
  }

  //Get Map Bounds
  var bounds = map.getBounds();

  var swLat = Math.abs(bounds.getSouthWest().lat);
  var swLng = Math.abs(bounds.getSouthWest().lng);
  var neLat = Math.abs(bounds.getNorthEast().lat);
  var neLng = Math.abs(bounds.getNorthEast().lng);

  formData["swLat"] = Math.min(swLat, neLat);
  formData["swLng"] = Math.max(swLng, neLng);
  formData["neLat"] = Math.max(swLat, neLat);
  formData["neLng"] = Math.min(swLng, neLng);

  // build aql
  var aql = buildAQLQueryFromForm(formData, type);

  APIqueryTracker = {
    "query": "use dataverse " + A._properties['dataverse'] + ";\n" + aql,
    "data": formData
  };

  console.log(APIqueryTracker.query)
  reportUserMessage(APIqueryTracker.query, true, "aql");
  // query

  A.aql(aql, queryCallbackWrapper(type), "synchronous");
}

/**
 * query callback function
 * @param string query type
 **/
function queryCallbackWrapper(type) {
  /**
   * A spatial data cleaning and mapping call
   * @param    {Object}    res, a result object from a tweetbook geospatial query
   */
  return function queryCallback(res) {
    // First, we check if any results came back in.
    // If they didn't, return.
    console.timeEnd("query_aql_get_result");
    if (!res.hasOwnProperty("results")) {
      reportUserMessage("Oops, no results found for those parameters.", false, "aql");
      return;
    }
    console.log(res)
    // update map
    if (res.results[0] && res.results[0].length>0)
      drawMap(res.results[0]);
    // update time series
    if (res.results[1] && type != "time" && res.results[1].length>0) {
      drawTimeSerialBrush(res.results[1]);
    }
    // update hashtag
    if (res.results[2] && res.results[2].length>0) {
      drawHashtag(res.results[2]);
    }
    // update tweet table
    if (res.results[3] && res.results[3].length>0) {
      drawTweets(res.results[3]);
    }
  }
}

// =============================   Draw Functions  =================================

/**
 * Update map based on a set of spatial query result cells
 * @param    [Array]     mapPlotData, an array of coordinate and weight objects
 */
function drawMap(mapPlotData) {
  /** Clear anything currently on the map **/
  console.time("query_aql_draw");
  var maxWeight = 0;
  var minWeight = Number.MAX_VALUE;

  // find max/min weight
  $.each(mapPlotData, function (i, data) {
    if (data.count) {
      maxWeight = Math.max(data.count, maxWeight);
      minWeight = Math.min(data.count, minWeight);
    }
  });

  var range = maxWeight - minWeight;
  if (range < 0) {
    range = 0
    maxWeight = 0
    minWeight = 0
  }

  colors = ['#053061', '#2166ac', '#4393c3', '#92c5de', '#d1e5f0', '#f7f7f7', '#fddbc7', '#f4a582', '#d6604d', '#b2182b', '#67001f'];
  // style function
  function getColor(d) {
    return d > minWeight + range * 0.9 ? colors[10] :
      d > minWeight + range * 0.8 ? colors[9] :
        d > minWeight + range * 0.7 ? colors[8] :
          d > minWeight + range * 0.6 ? colors[7] :
            d > minWeight + range * 0.5 ? colors[6] :
              d > minWeight + range * 0.4 ? colors[5] :
                d > minWeight + range * 0.3 ? colors[4] :
                  d > minWeight + range * 0.2 ? colors[3] :
                    d > minWeight + range * 0.1 ? colors[2] :
                      d > minWeight ? colors[1] :
                        colors[0];
  }

  function style(feature) {
    return {
      fillColor: getColor(feature.properties.count),
      weight: 2,
      opacity: 1,
      color: 'white',
      dashArray: '3',
      fillOpacity: 0.5
    };
  }

  // draw geojson polygons
  if (logicLevel == "state") {
    // transfer to geohash
    $.each(mapPlotData, function (m, data) {
      for (var hash in state_hash) {
        if (state_hash.hasOwnProperty(hash)) {
          if (hash == mapPlotData[m].cell) {
            var val = state_hash[hash];
            mapPlotData[m].cell = val;
          }
        }
      }
    });

    // update states count
    $.each(state.features, function (i, d) {
      if (d.properties.count)
        d.properties.count = 0;
      for (var m in mapPlotData) {
        if (mapPlotData[m].cell == d.properties.NAME)
          d.properties.count = mapPlotData[m].count;
      }
    });

    // draw state polygons
    statePolygons.setStyle(style);
  } else if (logicLevel == "county") {
    // update county's count
    $.each(county.features, function (i, d) {
      if (d.properties.count)
        d.properties.count = 0;
      for (var m in mapPlotData) {
        if (mapPlotData[m].cell && mapPlotData[m].cell.slice(3, mapPlotData[m].cell.length) == d.properties.NAME)
          d.properties.count = mapPlotData[m].count;
      }
    });

    // draw county polygons
    countyPolygons.setStyle(style);
  }
  // add legend
  if ($('.legend'))
    $('.legend').remove();

  var legend = L.control({
    position: 'topleft'
  });

  legend.onAdd = function (map) {
    var div = L.DomUtil.create('div', 'info legend'),
      grades = [0],
      labels = [];

    for (var i = 0; i < 10; i++) {
      var value = Math.floor((i * 1.0 / 10) * range + minWeight);
      if (value > grades[i]) {
        grades.push(value);
      }
    }

    // loop through our density intervals and generate a label with a colored square for each interval
    for (var i = 0; i < grades.length; i++) {
      div.innerHTML +=
        '<i style="background:' + getColor(grades[i]) + '"></i> ' +
        grades[i] + (grades[i + 1] ? '&ndash;' + grades[i + 1] + '<br>' : '+');
    }

    return div;
  };

  legend.addTo(map);

  console.timeEnd("query_aql_draw");
}

/**
 * draw timeseris
 * @ param {object}  time series query results
 **/
function drawTimeSerialBrush(slice_count) {

  var margin = {
      top: 10,
      right: 10,
      bottom: 30,
      left: 20
    },
    width = 962 - margin.left - margin.right,
    height = 150 - margin.top - margin.bottom;


  timeSeries = dc.lineChart("#time-series");
  timeBrush = timeSeries.brush();
  timeBrush.on('brushend', function (e) {
    var extent = timeBrush.extent();

    brush_start = toAQLDateTime(extent[0]);
    brush_end = toAQLDateTime(extent[1]);
    queryWrapper('time');
  });

  var parseDate = d3.time.format("%Y-%m-%d %H").parse;

  slice_count.forEach(function (d) {
    d.slice = parseDate(d.slice);
    d.count = +d.count;
  });
  var ndx = crossfilter(slice_count);
  var timeDimension = ndx.dimension(function (d) {
    if (d.slice != null) return d.slice;
  })
  var timeGroup = timeDimension.group().reduceSum(function (d) {
    return d.count;
  });

  var minDate = timeDimension.bottom(1)[0].slice;
  var maxDate = timeDimension.top(1)[0].slice;

  timeSeries
    .renderArea(true)
    .width(width)
    .height(height)
    .margins(margin)
    .dimension(timeDimension)
    .group(timeGroup)
    .x(d3.time.scale().domain([minDate, maxDate]));

  dc.renderAll();

  console.log('finished refining query');
}

/**
 * draw hashtag
 * @ param {object}  hashtag query results
 **/
function drawHashtag(tag_count) {
  $('#hashcount tr').html('');
  $.each(tag_count, function (i, d) {
    $('#hashcount tr:last').after('<tr><td>' + "#" + d.tag + '<br/>' + d.count + '</td></tr>');
  });
}

/**
 * draw tweet table
 * @ param {object}  tweets query results
 **/
function drawTweets(message) {
  $('#tweet').html('');
  $.each(message, function (i, d) {
    var url = "query/tweet/" + d.id;
    $.ajax({
      url: url,
      dataType: "json",
      success: function (data) {
        $('#tweet').append(data.html);
      },
      error: function(data) {
        // do nothing
      }
    });
  });
}

// =============================   Utility Functions =================================

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

  // Append the appropriate message
  $('<div/>')
    .attr("class", "alert " + alertType)
    .html('<button type="button" class="close" data-dismiss="alert">&times;</button>' + message)
    .appendTo('#' + target);
}

