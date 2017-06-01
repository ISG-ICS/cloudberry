/******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};

/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {

/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId])
/******/ 			return installedModules[moduleId].exports;

/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			exports: {},
/******/ 			id: moduleId,
/******/ 			loaded: false
/******/ 		};

/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);

/******/ 		// Flag the module as loaded
/******/ 		module.loaded = true;

/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}


/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;

/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;

/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";

/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(0);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(global) {module.exports = global["CloudberryDemo"] = __webpack_require__(1);
	/* WEBPACK VAR INJECTION */}.call(exports, (function() { return this; }())))

/***/ },
/* 1 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	var _CompositeClosureHelper = __webpack_require__(2);

	var _CompositeClosureHelper2 = _interopRequireDefault(_CompositeClosureHelper);

	var _FieldProvider = __webpack_require__(5);

	var _FieldProvider2 = _interopRequireDefault(_FieldProvider);

	var _LegendProvider = __webpack_require__(6);

	var _LegendProvider2 = _interopRequireDefault(_LegendProvider);

	var _Histogram1DProvider = __webpack_require__(21);

	var _Histogram1DProvider2 = _interopRequireDefault(_Histogram1DProvider);

	var _HistogramBinHoverProvider = __webpack_require__(22);

	var _HistogramBinHoverProvider2 = _interopRequireDefault(_HistogramBinHoverProvider);

	var _ScoresProvider = __webpack_require__(23);

	var _ScoresProvider2 = _interopRequireDefault(_ScoresProvider);

	var _SelectionProvider = __webpack_require__(24);

	var _SelectionProvider2 = _interopRequireDefault(_SelectionProvider);

	var _HistogramSelector = __webpack_require__(28);

	var _HistogramSelector2 = _interopRequireDefault(_HistogramSelector);

	var _FieldSelector = __webpack_require__(63);

	var _FieldSelector2 = _interopRequireDefault(_FieldSelector);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	var JSONbig = __webpack_require__(67); /* global document */


	var httpProxy = "proxy.php";
	var cloudberryHost = "ws://localhost:9000/ws";
	var dataModel = { fields: { byTimeResult: {}, byGeoResult: {} }, histogram1D_storage: { 32: { byTimeResult: {}, byGeoResult: {} } }, dirty: true };
	var xhttpState = "initializing";
	var WSServer;
	var xhttp = new XMLHttpRequest();

	// timestamp for performance measurement
	var t0;
	var t1;

	// select a start date for the query
	var startDate = new Date(2015, 10, 22, 0, 0, 0, 0);

	var defaultNonSamplingDayRange = 1500;
	var defaultSamplingDayRange = 1500;
	var defaultSamplingSize = 10;

	var bodyElt = document.querySelector('body');
	// '100vh' is 100% of the current screen height
	var defaultHeight = '100vh';

	var histogramSelectorContainer = document.createElement('div');
	histogramSelectorContainer.style.position = 'relative';
	histogramSelectorContainer.style.width = '52%';
	histogramSelectorContainer.style.height = defaultHeight;
	histogramSelectorContainer.style.float = 'left';
	bodyElt.appendChild(histogramSelectorContainer);

	var fieldSelectorContainer = document.createElement('div');
	fieldSelectorContainer.style.position = 'relative';
	fieldSelectorContainer.style.width = '48%';
	fieldSelectorContainer.style.height = defaultHeight;
	fieldSelectorContainer.style.float = 'left';
	fieldSelectorContainer.style['font-size'] = '10pt';
	bodyElt.appendChild(fieldSelectorContainer);

	var countRequest = JSON.stringify({
	    dataset: 'twitter.ds_tweet',
	    global: {
	        globalAggregate: {
	            field: '*',
	            apply: {
	                name: 'count'
	            },
	            as: 'count'
	        }
	    },
	    estimable: true,
	    transform: {
	        wrap: {
	            key: 'totalCount'
	        }
	    }
	});

	function getLevel(level) {
	    switch (level) {
	        case 'state':
	            return 'stateID';
	        case 'county':
	            return 'countyID';
	        case 'city':
	            return 'cityID';
	        default:
	            break;
	    }
	    return 'good';
	}

	function mkString(array, delimiter) {
	    var s = '';
	    function myconcat(item) {
	        s += item.toString() + delimiter;
	    }
	    array.forEach(myconcat);
	    return s.substring(0, s.length - 1);
	}

	function getFilter(parameters, maxDay) {
	    var spatialField = getLevel(parameters.geoLevel);
	    var keywords = [];
	    var queryStartDate = new Date(parameters.timeInterval.end);
	    var i;
	    for (i = 0; i < parameters.keywords.length; i++) {
	        keywords.push(parameters.keywords[i].replace('\'', '').trim());
	    }
	    queryStartDate.setDate(queryStartDate.getDate() - maxDay);
	    queryStartDate = parameters.timeInterval.start > queryStartDate ? parameters.timeInterval.start : queryStartDate;

	    return [{
	        field: 'geo_tag.'.concat(spatialField),
	        relation: 'in',
	        values: parameters.geoIds
	    }, {
	        field: 'create_at',
	        relation: 'inRange',
	        values: [queryStartDate.toISOString(), parameters.timeInterval.end.toISOString()]
	    }, {
	        field: 'text',
	        relation: 'contains',
	        values: [mkString(keywords, ',')]
	    }];
	}

	function byGeoRequest(parameters) {
	    return {
	        dataset: parameters.dataset,
	        filter: getFilter(parameters, defaultNonSamplingDayRange),
	        group: {
	            by: [{
	                field: 'geo',
	                apply: {
	                    name: 'level',
	                    args: {
	                        level: parameters.geoLevel
	                    }
	                },
	                as: parameters.geoLevel
	            }],
	            aggregate: [{
	                field: '*',
	                apply: {
	                    name: 'count'
	                },
	                as: 'count'
	            }]
	        }
	    };
	}

	function byTimeRequest(parameters) {
	    return {
	        dataset: parameters.dataset,
	        filter: getFilter(parameters, defaultNonSamplingDayRange),
	        group: {
	            by: [{
	                field: 'create_at',
	                apply: {
	                    name: 'interval',
	                    args: {
	                        unit: parameters.timeBin
	                    }
	                },
	                as: parameters.timeBin
	            }],
	            aggregate: [{
	                field: '*',
	                apply: {
	                    name: 'count'
	                },
	                as: 'count'
	            }]
	        }
	    };
	}

	function dateDiffInDays(a, b) {
	    var timeDiff = Math.abs(b.getTime() - a.getTime());
	    var diffDays = Math.ceil(timeDiff / (1000 * 3600 * 24));
	    return diffDays;
	}

	function register_dataset() {
	    var TwitterMapDDL = '{' + '"dataset":"twitter.ds_tweet",' + '"schema":{' + '"typeName":"twitter.typeTweet",' + '"dimension":[' + '{"name":"create_at","isOptional":false,"datatype":"Time"},' + '{"name":"id","isOptional":false,"datatype":"Number"},' + '{"name":"coordinate","isOptional":false,"datatype":"Point"},' + '{"name":"lang","isOptional":false,"datatype":"String"},' + '{"name":"is_retweet","isOptional":false,"datatype":"Boolean"},' + '{"name":"hashtags","isOptional":true,"datatype":"Bag","innerType":"String"},' + '{"name":"user_mentions","isOptional":true,"datatype":"Bag","innerType":"Number"},' + '{"name":"user.id","isOptional":false,"datatype":"Number"},' + '{"name":"geo_tag.stateID","isOptional":false,"datatype":"Number"},' + '{"name":"geo_tag.countyID","isOptional":false,"datatype":"Number"},' + '{"name":"geo_tag.cityID","isOptional":false,"datatype":"Number"},' + '{"name":"geo","isOptional":false,"datatype":"Hierarchy","innerType":"Number",' + '"levels":[' + '{"level":"state","field":"geo_tag.stateID"},' + '{"level":"county","field":"geo_tag.countyID"},' + '{"level":"city","field":"geo_tag.cityID"}' + ']' + '}' + '],' + '"measurement":[' + '{"name":"text","isOptional":false,"datatype":"Text"},' + '{"name":"in_reply_to_status","isOptional":false,"datatype":"Number"},' + '{"name":"in_reply_to_user","isOptional":false,"datatype":"Number"},' + '{"name":"favorite_count","isOptional":false,"datatype":"Number"},' + '{"name":"retweet_count","isOptional":false,"datatype":"Number"},' + '{"name":"user.status_count","isOptional":false,"datatype":"Number"}' + '],' + '"primaryKey":["id"],' + '"timeField":"create_at"' + '}' + '}';

	    xhttp.open("POST", httpProxy, true);
	    xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	    xhttp.send("cloudberry=" + TwitterMapDDL);
	}

	// process query result from asterixDB
	xhttp.onreadystatechange = function () {
	    if (this.readyState == 4 && this.status == 200) {
	        if (xhttpState == "initializing") {
	            xhttpState = "waiting";
	        } else if (xhttpState == "query_pending") {
	            // transform the query result into dataModel required by paraviewweb
	            var response = JSONbig.parse(xhttp.responseText);
	            var tweetCount = 0;
	            var width = dateDiffInDays(startDate, new Date()) + 1;
	            var valuesTime = new Array(width);
	            valuesTime.fill(0.0001);
	            for (var i = 0; i < response['results'].length; i++) {
	                valuesTime[dateDiffInDays(startDate, new Date(response['results'][i].day))] = response['results'][i].count;
	                tweetCount += response['results'][i].count;
	            }
	            dataModel.fields.byTimeResult = { name: 'byTimeResult', range: [0.0, width], active: true, id: 0 };
	            dataModel.histogram1D_storage['32'].byTimeResult = { max: width, counts: valuesTime, name: 'byTimeResult', min: 0.0 };

	            var valuesGeo = new Array(52);
	            valuesGeo.fill(0.0001);
	            for (var i = 0; i < response['results-0'].length; i++) {
	                valuesGeo[response['results-0'][i].stateID] = response['results-0'][i].count;
	            }
	            dataModel.fields.byGeoResult = { name: 'byGeoResult', range: [0, 52], active: true, id: 1 };
	            dataModel.histogram1D_storage['32'].byGeoResult = { max: 52, counts: valuesGeo, name: 'byGeoResult', min: 0 };

	            updateHistogram(dataModel);

	            // update the performance measurement
	            document.getElementById('queryCount').innerHTML = tweetCount;
	            t1 = performance.now();
	            document.getElementById('queryTime').innerHTML = t1 - t0 + " milliseconds.";

	            xhttpState = "waiting";
	        } else {
	            console.log(xhttp.responseText);
	        }
	    }
	};

	function prepareSendButton() {
	    document.getElementById('cntBtn').onclick = function send() {
	        var element = document.getElementById("datasource");
	        var dataSource = element.options[element.selectedIndex].value;
	        if (dataSource == "cloudberry") {
	            // prepare the query for cloudberry
	            var parameters = {
	                dataset: 'twitter.ds_tweet',
	                keywords: document.getElementById('query').value.trim().split(/\s+/),
	                timeInterval: {
	                    start: startDate,
	                    end: new Date()
	                },
	                timeBin: 'day',
	                geoLevel: 'state',
	                geoIds: [37, 51, 24, 11, 10, 34, 42, 9, 44, 48, 35, 4, 40, 6, 20, 32, 8, 49, 12, 22, 28, 1, 13, 45, 5, 47, 21, 29, 54, 17, 18, 39, 19, 55, 26, 27, 31, 56, 41, 46, 16, 30, 53, 38, 25, 36, 50, 33, 23, 2]
	            };

	            var batchJson = JSON.stringify({
	                batch: [byTimeRequest(parameters), byGeoRequest(parameters)],
	                transform: {
	                    wrap: {
	                        key: 'batch'
	                    }
	                }
	            });

	            // send the query and start the timer
	            WSServer.send(batchJson);
	            t0 = performance.now();
	        } else if (dataSource == "asterixdb") {
	            if (xhttpState == "waiting") {
	                // prepare the query for asterixDB
	                xhttpState = "query_pending";
	                var keyword = document.getElementById('query').value.trim().split(/\s+/);

	                var query = [];
	                query[0] = "select day, count(*) as count from twitter.ds_tweet t where ftcontains(t.text, ['" + keyword + "']) group by get_interval_start_datetime(interval_bin(t.create_at, datetime('1990-01-01T00:00:00.000Z'), day_time_duration('P1D') )) as day;";
	                query[1] = "select stateID, count(*) as count from twitter.ds_tweet t where ftcontains(t.text, ['" + keyword + "']) group by geo_tag.stateID as stateID;";
	                var params = "statement=";
	                for (var i = 0; i < query.length; i++) {
	                    params += query[i];
	                }

	                // send the query and start the timer
	                xhttp.open("POST", httpProxy, true);
	                xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	                xhttp.send("asterixdb=" + params);
	                t0 = performance.now();
	            }
	        }
	    };
	}

	function onload_process() {
	    prepareSendButton();
	    register_dataset();
	}

	window.onload = onload_process();

	// draw histogram using paraviewweb
	function updateHistogram(dataModel) {
	    var provider = _CompositeClosureHelper2.default.newInstance(function (publicAPI, model) {
	        var initialValues = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : {};

	        Object.assign(model, initialValues);
	        _FieldProvider2.default.extend(publicAPI, model, initialValues);
	        _Histogram1DProvider2.default.extend(publicAPI, model, initialValues);
	        _HistogramBinHoverProvider2.default.extend(publicAPI, model);
	        _LegendProvider2.default.extend(publicAPI, model, initialValues);
	        _ScoresProvider2.default.extend(publicAPI, model, initialValues);
	        _SelectionProvider2.default.extend(publicAPI, model, initialValues);
	    })(dataModel);

	    // set provider behaviors
	    provider.setFieldsSorted(true);
	    provider.getFieldNames().forEach(function (name) {
	        provider.addLegendEntry(name);
	    });
	    provider.assignLegend(['colors', 'shapes']);

	    // activate scoring gui
	    var scores = [{ name: 'No', color: '#FDAE61', value: -1 }, { name: 'Maybe', color: '#FFFFBF', value: 0 }, { name: 'Yes', color: '#A6D96A', value: 1 }];
	    provider.setScores(scores);
	    provider.setDefaultScore(1);

	    // Create histogram selector
	    var histogramSelector = _HistogramSelector2.default.newInstance({
	        provider: provider,
	        container: histogramSelectorContainer
	    });
	    // set a target number per row.
	    histogramSelector.requestNumBoxesPerRow(4);
	    // Or show a single variable as the focus, possibly disabling switching to other vars.
	    // histogramSelector.displaySingleHistogram(provider.getFieldNames()[5], true);
	    // and maybe set a scoring annotation:
	    // histogramSelector.setDefaultScorePartition(provider.getFieldNames()[5]);
	    // test reset:
	    // window.setTimeout(() => {
	    //   histogramSelector.requestNumBoxesPerRow(4);
	    // }, 5000);

	    // Create field selector
	    var fieldSelector = _FieldSelector2.default.newInstance({ provider: provider, container: fieldSelectorContainer });

	    histogramSelector.resize();
	    fieldSelector.resize();
	}

	// set up websocket connection and event handlers to cloudberry 
	function connect() {
	    WSServer = new WebSocket(cloudberryHost);

	    function requestLiveCounts() {
	        if (WSServer.readyState === WSServer.OPEN) {
	            WSServer.send(countRequest);
	        }
	    }
	    setInterval(requestLiveCounts, 1000);

	    // event handelr for incoming messages
	    WSServer.onmessage = function msghandler(event) {
	        var result = JSONbig.parse(event.data);
	        var width = dateDiffInDays(startDate, new Date()) + 1;
	        var valuesTime = new Array(width);
	        var valuesGeo = new Array(52);
	        valuesTime.fill(0.0001);
	        valuesGeo.fill(0.0001);

	        switch (result.key) {
	            case 'batch':
	                // transform the query result into dataModel required by paraviewweb
	                var tweetCount = 0;
	                for (var i = 0; i < result.value[0].length; i++) {
	                    valuesTime[dateDiffInDays(startDate, new Date(result.value[0][i].day))] = parseInt(result.value[0][i].count, 10);
	                    tweetCount += parseInt(result.value[0][i].count, 10);
	                }
	                dataModel.fields.byTimeResult = { name: 'byTimeResult', range: [0.0, width], active: true, id: 0 };
	                dataModel.histogram1D_storage['32'].byTimeResult = { max: width, counts: valuesTime, name: 'byTimeResult', min: 0.0 };

	                for (var i = 0; i < result.value[1].length; i++) {
	                    valuesGeo[parseInt(result.value[1][i].state, 10)] = parseInt(result.value[1][i].count, 10);
	                }
	                dataModel.fields.byGeoResult = { name: 'byGeoResult', range: [0, 52], active: true, id: 1 };
	                dataModel.histogram1D_storage['32'].byGeoResult = { max: 52, counts: valuesGeo, name: 'byGeoResult', min: 0 };

	                updateHistogram(dataModel);

	                // update the performance measurement
	                document.getElementById('queryCount').innerHTML = tweetCount;
	                t1 = performance.now();
	                document.getElementById('queryTime').innerHTML = t1 - t0 + " milliseconds.";

	                break;
	            case 'totalCount':
	                document.getElementById('totalCount').innerHTML = result.value[0][0].count;
	                break;
	            case 'error':
	                // console.error(result);
	                break;
	            case 'done':
	                break;
	            default:
	                console.error('ws get unknown data: ', result);
	                break;
	        }
	    };
	}

	connect();

/***/ },
/* 2 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(setImmediate) {'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	exports.capitalize = capitalize;
	// ----------------------------------------------------------------------------
	// capitalize provided string
	// ----------------------------------------------------------------------------

	function capitalize(str) {
	  return str.charAt(0).toUpperCase() + str.slice(1);
	}

	// ----------------------------------------------------------------------------
	// Add isA function and register your class name
	// ----------------------------------------------------------------------------

	function isA(publicAPI) {
	  var model = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};
	  var name = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : null;

	  if (!model.isA) {
	    model.isA = [];
	  }

	  if (name) {
	    model.isA.push(name);
	  }

	  if (!publicAPI.isA) {
	    publicAPI.isA = function (className) {
	      return model.isA.indexOf(className) !== -1;
	    };
	  }
	}

	// ----------------------------------------------------------------------------
	// Basic setter
	// ----------------------------------------------------------------------------

	function set(publicAPI) {
	  var model = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};
	  var names = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : [];

	  names.forEach(function (name) {
	    publicAPI['set' + capitalize(name)] = function (value) {
	      model[name] = value;
	    };
	  });
	}

	// ----------------------------------------------------------------------------
	// Basic getter
	// ----------------------------------------------------------------------------

	function get(publicAPI) {
	  var model = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};
	  var names = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : [];

	  names.forEach(function (name) {
	    publicAPI['get' + capitalize(name)] = function () {
	      return model[name];
	    };
	  });
	}

	// ----------------------------------------------------------------------------
	// Add destroy function
	// ----------------------------------------------------------------------------

	function destroy(publicAPI) {
	  var model = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};

	  var previousDestroy = publicAPI.destroy;

	  if (!model.subscriptions) {
	    model.subscriptions = [];
	  }

	  publicAPI.destroy = function () {
	    if (previousDestroy) {
	      previousDestroy();
	    }
	    while (model.subscriptions && model.subscriptions.length) {
	      model.subscriptions.pop().unsubscribe();
	    }
	    Object.keys(model).forEach(function (field) {
	      delete model[field];
	    });

	    // Flag the instance beeing deleted
	    model.deleted = true;
	  };
	}

	// ----------------------------------------------------------------------------
	// Event handling: onXXX(callback), fireXXX(args...)
	// ----------------------------------------------------------------------------

	function event(publicAPI, model, eventName) {
	  var asynchrounous = arguments.length > 3 && arguments[3] !== undefined ? arguments[3] : true;

	  var callbacks = [];
	  var previousDestroy = publicAPI.destroy;

	  function off(index) {
	    callbacks[index] = null;
	  }

	  function on(index) {
	    function unsubscribe() {
	      off(index);
	    }
	    return Object.freeze({ unsubscribe: unsubscribe });
	  }

	  publicAPI['fire' + capitalize(eventName)] = function () {
	    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
	      args[_key] = arguments[_key];
	    }

	    if (model.deleted) {
	      console.log('instance deleted - can not call any method');
	      return;
	    }

	    function processCallbacks() {
	      callbacks.forEach(function (callback) {
	        if (callback) {
	          try {
	            callback.apply(publicAPI, args);
	          } catch (errObj) {
	            console.log('Error event:', eventName, errObj);
	          }
	        }
	      });
	    }

	    if (asynchrounous) {
	      setImmediate(processCallbacks);
	    } else {
	      processCallbacks();
	    }
	  };

	  publicAPI['on' + capitalize(eventName)] = function (callback) {
	    if (model.deleted) {
	      console.log('instance deleted - can not call any method');
	      return null;
	    }

	    var index = callbacks.length;
	    callbacks.push(callback);
	    return on(index);
	  };

	  publicAPI.destroy = function () {
	    previousDestroy();
	    callbacks.forEach(function (el, index) {
	      return off(index);
	    });
	  };
	}

	// ----------------------------------------------------------------------------
	// Fetch handling: setXXXFetchCallback / return { addRequest }
	// ----------------------------------------------------------------------------
	function fetch(publicAPI, model, name) {
	  var fetchCallback = null;
	  var requestQueue = [];

	  publicAPI['set' + capitalize(name) + 'FetchCallback'] = function (fetchMethod) {
	    if (requestQueue.length) {
	      fetchMethod(requestQueue);
	    }
	    fetchCallback = fetchMethod;
	  };

	  return {
	    addRequest: function addRequest(request) {
	      requestQueue.push(request);
	      if (fetchCallback) {
	        fetchCallback(requestQueue);
	      }
	    },
	    resetRequests: function resetRequests(requestList) {
	      while (requestQueue.length) {
	        requestQueue.pop();
	      }
	      if (requestList) {
	        // Rebuild request list
	        requestList.forEach(function (req) {
	          requestQueue.push(req);
	        });
	        // Also trigger a request
	        if (fetchCallback) {
	          fetchCallback(requestQueue);
	        }
	      }
	    }
	  };
	}

	// ----------------------------------------------------------------------------
	// Dynamic array handler
	//   - add${xxx}(item)
	//   - remove${xxx}(item)
	//   - get${xxx}() => [items...]
	//   - removeAll${xxx}()
	// ----------------------------------------------------------------------------

	function dynamicArray(publicAPI, model, name) {
	  if (!model[name]) {
	    model[name] = [];
	  }

	  publicAPI['set' + capitalize(name)] = function (items) {
	    model[name] = [].concat(items);
	  };

	  publicAPI['add' + capitalize(name)] = function (item) {
	    model[name].push(item);
	  };

	  publicAPI['remove' + capitalize(name)] = function (item) {
	    var index = model[name].indexOf(item);
	    model[name].splice(index, 1);
	  };

	  publicAPI['get' + capitalize(name)] = function () {
	    return model[name];
	  };

	  publicAPI['removeAll' + capitalize(name)] = function () {
	    return model[name] = [];
	  };
	}

	// ----------------------------------------------------------------------------
	// Chain function calls
	// ----------------------------------------------------------------------------

	function chain() {
	  for (var _len2 = arguments.length, fn = Array(_len2), _key2 = 0; _key2 < _len2; _key2++) {
	    fn[_key2] = arguments[_key2];
	  }

	  return function () {
	    for (var _len3 = arguments.length, args = Array(_len3), _key3 = 0; _key3 < _len3; _key3++) {
	      args[_key3] = arguments[_key3];
	    }

	    return fn.filter(function (i) {
	      return !!i;
	    }).forEach(function (i) {
	      return i.apply(undefined, args);
	    });
	  };
	}

	// ----------------------------------------------------------------------------
	// Data Subscription
	//   => dataHandler = {
	//         // Set of default values you would expect in your metadata
	//         defaultMetadata: {
	//            numberOfBins: 32,
	//         },
	//
	//         // Method used internally to store the data
	//         set(model, data) { return !!sameAsBefore; }, // Return true if nothing has changed
	//
	//         // Method used internally to extract the data from the cache based on a given subscription
	//         // This should return null/undefined if the data is not available (yet).
	//         get(model, request, dataChanged) {},
	//      }
	// ----------------------------------------------------------------------------
	// Methods generated with dataName = 'mutualInformation'
	// => publicAPI
	//     - onMutualInformationSubscriptionChange(callback) => subscription[unsubscribe() + update(variables = [], metadata = {})]
	//     - fireMutualInformationSubscriptionChange(request)
	//     - subscribeToMutualInformation(onDataReady, variables = [], metadata = {})
	//     - setMutualInformation(data)
	//     - destroy()
	// ----------------------------------------------------------------------------

	function dataSubscriber(publicAPI, model, dataName, dataHandler) {
	  // Private members
	  var dataSubscriptions = [];
	  var forceFlushRequests = 0;
	  var eventName = dataName + 'SubscriptionChange';
	  var fireMethodName = 'fire' + capitalize(eventName);
	  var dataContainerName = dataName + '_storage';

	  // Add data container to model if not exist
	  if (!model[dataContainerName]) {
	    model[dataContainerName] = {};
	  }

	  // Add event handling methods
	  event(publicAPI, model, eventName);

	  function off() {
	    var count = dataSubscriptions.length;
	    while (count) {
	      count -= 1;
	      dataSubscriptions[count] = null;
	    }
	  }

	  // Internal function that will notify any subscriber with its data in a synchronous manner
	  function flushDataToListener(dataListener, dataChanged) {
	    try {
	      if (dataListener) {
	        var dataToForward = dataHandler.get(model[dataContainerName], dataListener.request, dataChanged);
	        if (dataToForward && (JSON.stringify(dataToForward) !== dataListener.request.lastPush || dataListener.request.metadata.forceFlush)) {
	          dataListener.request.lastPush = JSON.stringify(dataToForward);
	          dataListener.onDataReady(dataToForward);
	        }
	      }
	    } catch (err) {
	      console.log('flush ' + dataName + ' error caught:', err);
	    }
	  }

	  // onDataReady function will be called each time the setXXX method will be called and
	  // when the actual subscription correspond to the data that has been set.
	  // This is performed synchronously.
	  // The default behavior is to avoid pushing data to subscribers if nothing has changed
	  // since the last push.  However, by providing "forceFlush: true" in the metadata,
	  // subscribers can indicate that they want data pushed to them even if there has been
	  // no change since the last push.
	  publicAPI['subscribeTo' + capitalize(dataName)] = function (onDataReady) {
	    var variables = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : [];
	    var metadata = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : {};

	    var id = dataSubscriptions.length;
	    var request = {
	      id: id,
	      variables: variables,
	      metadata: Object.assign({}, dataHandler.defaultMetadata, metadata)
	    };
	    if (request.metadata.forceFlush) {
	      forceFlushRequests += 1;
	    }
	    var dataListener = { onDataReady: onDataReady, request: request };
	    dataSubscriptions.push(dataListener);
	    publicAPI[fireMethodName](request);
	    flushDataToListener(dataListener, null);
	    return {
	      unsubscribe: function unsubscribe() {
	        request.action = 'unsubscribe';
	        if (request.metadata.forceFlush) {
	          forceFlushRequests -= 1;
	        }
	        publicAPI[fireMethodName](request);
	        dataSubscriptions[id] = null;
	      },
	      update: function update(vars, meta) {
	        request.variables = [].concat(vars);
	        if (meta && meta.forceFlush !== request.metadata.forceFlush) {
	          forceFlushRequests += meta.forceFlush ? 1 : -1;
	        }
	        request.metadata = Object.assign({}, request.metadata, meta);
	        publicAPI[fireMethodName](request);
	        flushDataToListener(dataListener, null);
	      }
	    };
	  };

	  // Method use to store data
	  publicAPI['set' + capitalize(dataName)] = function (data) {
	    // Process all subscription to see if we can trigger a notification
	    if (!dataHandler.set(model[dataContainerName], data) || forceFlushRequests > 0) {
	      dataSubscriptions.forEach(function (dataListener) {
	        return flushDataToListener(dataListener, data);
	      });
	    }
	  };

	  publicAPI.destroy = chain(off, publicAPI.destroy);
	}

	// ----------------------------------------------------------------------------
	// newInstance
	// ----------------------------------------------------------------------------

	function newInstance(extend) {
	  return function () {
	    var initialValues = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {};

	    var model = {};
	    var publicAPI = {};
	    extend(publicAPI, model, initialValues);
	    return Object.freeze(publicAPI);
	  };
	}

	exports.default = {
	  chain: chain,
	  dataSubscriber: dataSubscriber,
	  destroy: destroy,
	  dynamicArray: dynamicArray,
	  event: event,
	  fetch: fetch,
	  get: get,
	  isA: isA,
	  newInstance: newInstance,
	  set: set
	};
	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(3).setImmediate))

/***/ },
/* 3 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(setImmediate, clearImmediate) {var nextTick = __webpack_require__(4).nextTick;
	var apply = Function.prototype.apply;
	var slice = Array.prototype.slice;
	var immediateIds = {};
	var nextImmediateId = 0;

	// DOM APIs, for completeness

	exports.setTimeout = function() {
	  return new Timeout(apply.call(setTimeout, window, arguments), clearTimeout);
	};
	exports.setInterval = function() {
	  return new Timeout(apply.call(setInterval, window, arguments), clearInterval);
	};
	exports.clearTimeout =
	exports.clearInterval = function(timeout) { timeout.close(); };

	function Timeout(id, clearFn) {
	  this._id = id;
	  this._clearFn = clearFn;
	}
	Timeout.prototype.unref = Timeout.prototype.ref = function() {};
	Timeout.prototype.close = function() {
	  this._clearFn.call(window, this._id);
	};

	// Does not start the time, just sets up the members needed.
	exports.enroll = function(item, msecs) {
	  clearTimeout(item._idleTimeoutId);
	  item._idleTimeout = msecs;
	};

	exports.unenroll = function(item) {
	  clearTimeout(item._idleTimeoutId);
	  item._idleTimeout = -1;
	};

	exports._unrefActive = exports.active = function(item) {
	  clearTimeout(item._idleTimeoutId);

	  var msecs = item._idleTimeout;
	  if (msecs >= 0) {
	    item._idleTimeoutId = setTimeout(function onTimeout() {
	      if (item._onTimeout)
	        item._onTimeout();
	    }, msecs);
	  }
	};

	// That's not how node.js implements it but the exposed api is the same.
	exports.setImmediate = typeof setImmediate === "function" ? setImmediate : function(fn) {
	  var id = nextImmediateId++;
	  var args = arguments.length < 2 ? false : slice.call(arguments, 1);

	  immediateIds[id] = true;

	  nextTick(function onNextTick() {
	    if (immediateIds[id]) {
	      // fn.call() is faster so we optimize for the common use-case
	      // @see http://jsperf.com/call-apply-segu
	      if (args) {
	        fn.apply(null, args);
	      } else {
	        fn.call(null);
	      }
	      // Prevent ids from leaking
	      exports.clearImmediate(id);
	    }
	  });

	  return id;
	};

	exports.clearImmediate = typeof clearImmediate === "function" ? clearImmediate : function(id) {
	  delete immediateIds[id];
	};
	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(3).setImmediate, __webpack_require__(3).clearImmediate))

/***/ },
/* 4 */
/***/ function(module, exports) {

	// shim for using process in browser
	var process = module.exports = {};

	// cached from whatever global is present so that test runners that stub it
	// don't break things.  But we need to wrap it in a try catch in case it is
	// wrapped in strict mode code which doesn't define any globals.  It's inside a
	// function because try/catches deoptimize in certain engines.

	var cachedSetTimeout;
	var cachedClearTimeout;

	function defaultSetTimout() {
	    throw new Error('setTimeout has not been defined');
	}
	function defaultClearTimeout () {
	    throw new Error('clearTimeout has not been defined');
	}
	(function () {
	    try {
	        if (typeof setTimeout === 'function') {
	            cachedSetTimeout = setTimeout;
	        } else {
	            cachedSetTimeout = defaultSetTimout;
	        }
	    } catch (e) {
	        cachedSetTimeout = defaultSetTimout;
	    }
	    try {
	        if (typeof clearTimeout === 'function') {
	            cachedClearTimeout = clearTimeout;
	        } else {
	            cachedClearTimeout = defaultClearTimeout;
	        }
	    } catch (e) {
	        cachedClearTimeout = defaultClearTimeout;
	    }
	} ())
	function runTimeout(fun) {
	    if (cachedSetTimeout === setTimeout) {
	        //normal enviroments in sane situations
	        return setTimeout(fun, 0);
	    }
	    // if setTimeout wasn't available but was latter defined
	    if ((cachedSetTimeout === defaultSetTimout || !cachedSetTimeout) && setTimeout) {
	        cachedSetTimeout = setTimeout;
	        return setTimeout(fun, 0);
	    }
	    try {
	        // when when somebody has screwed with setTimeout but no I.E. maddness
	        return cachedSetTimeout(fun, 0);
	    } catch(e){
	        try {
	            // When we are in I.E. but the script has been evaled so I.E. doesn't trust the global object when called normally
	            return cachedSetTimeout.call(null, fun, 0);
	        } catch(e){
	            // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error
	            return cachedSetTimeout.call(this, fun, 0);
	        }
	    }


	}
	function runClearTimeout(marker) {
	    if (cachedClearTimeout === clearTimeout) {
	        //normal enviroments in sane situations
	        return clearTimeout(marker);
	    }
	    // if clearTimeout wasn't available but was latter defined
	    if ((cachedClearTimeout === defaultClearTimeout || !cachedClearTimeout) && clearTimeout) {
	        cachedClearTimeout = clearTimeout;
	        return clearTimeout(marker);
	    }
	    try {
	        // when when somebody has screwed with setTimeout but no I.E. maddness
	        return cachedClearTimeout(marker);
	    } catch (e){
	        try {
	            // When we are in I.E. but the script has been evaled so I.E. doesn't  trust the global object when called normally
	            return cachedClearTimeout.call(null, marker);
	        } catch (e){
	            // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error.
	            // Some versions of I.E. have different rules for clearTimeout vs setTimeout
	            return cachedClearTimeout.call(this, marker);
	        }
	    }



	}
	var queue = [];
	var draining = false;
	var currentQueue;
	var queueIndex = -1;

	function cleanUpNextTick() {
	    if (!draining || !currentQueue) {
	        return;
	    }
	    draining = false;
	    if (currentQueue.length) {
	        queue = currentQueue.concat(queue);
	    } else {
	        queueIndex = -1;
	    }
	    if (queue.length) {
	        drainQueue();
	    }
	}

	function drainQueue() {
	    if (draining) {
	        return;
	    }
	    var timeout = runTimeout(cleanUpNextTick);
	    draining = true;

	    var len = queue.length;
	    while(len) {
	        currentQueue = queue;
	        queue = [];
	        while (++queueIndex < len) {
	            if (currentQueue) {
	                currentQueue[queueIndex].run();
	            }
	        }
	        queueIndex = -1;
	        len = queue.length;
	    }
	    currentQueue = null;
	    draining = false;
	    runClearTimeout(timeout);
	}

	process.nextTick = function (fun) {
	    var args = new Array(arguments.length - 1);
	    if (arguments.length > 1) {
	        for (var i = 1; i < arguments.length; i++) {
	            args[i - 1] = arguments[i];
	        }
	    }
	    queue.push(new Item(fun, args));
	    if (queue.length === 1 && !draining) {
	        runTimeout(drainQueue);
	    }
	};

	// v8 likes predictible objects
	function Item(fun, array) {
	    this.fun = fun;
	    this.array = array;
	}
	Item.prototype.run = function () {
	    this.fun.apply(null, this.array);
	};
	process.title = 'browser';
	process.browser = true;
	process.env = {};
	process.argv = [];
	process.version = ''; // empty string to avoid regexp issues
	process.versions = {};

	function noop() {}

	process.on = noop;
	process.addListener = noop;
	process.once = noop;
	process.off = noop;
	process.removeListener = noop;
	process.removeAllListeners = noop;
	process.emit = noop;
	process.prependListener = noop;
	process.prependOnceListener = noop;

	process.listeners = function (name) { return [] }

	process.binding = function (name) {
	    throw new Error('process.binding is not supported');
	};

	process.cwd = function () { return '/' };
	process.chdir = function (dir) {
	    throw new Error('process.chdir is not supported');
	};
	process.umask = function() { return 0; };


/***/ },
/* 5 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	exports.newInstance = undefined;
	exports.extend = extend;

	var _CompositeClosureHelper = __webpack_require__(2);

	var _CompositeClosureHelper2 = _interopRequireDefault(_CompositeClosureHelper);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	var DEFAULT_FIELD_STATE = {
	  range: [0, 1],
	  active: false
	};

	var PROVIDER_NAME = 'FieldProvider';

	// ----------------------------------------------------------------------------
	// Field Provider
	// ----------------------------------------------------------------------------

	function fieldProvider(publicAPI, model) {
	  if (!model.fields) {
	    model.fields = {};
	  }

	  var triggerFieldChange = function triggerFieldChange(field) {
	    if (publicAPI.isA('PersistentStateProvider')) {
	      publicAPI.setPersistentState(PROVIDER_NAME, model.fields);
	    }
	    publicAPI.fireFieldChange(field);
	  };

	  publicAPI.loadFieldsFromState = function () {
	    var count = 0;
	    if (publicAPI.isA('PersistentStateProvider')) {
	      var storageItems = publicAPI.getPersistentState(PROVIDER_NAME);
	      Object.keys(storageItems).forEach(function (storeKey) {
	        publicAPI.updateField(storeKey, storageItems[storeKey]);
	        count += 1;
	      });
	    }
	    return count;
	  };

	  publicAPI.getFieldNames = function () {
	    var val = Object.keys(model.fields);
	    if (model.fieldsSorted) val.sort();
	    return val;
	  };

	  publicAPI.getActiveFieldNames = function () {
	    var val = Object.keys(model.fields).filter(function (name) {
	      return model.fields[name].active;
	    });
	    if (model.fieldsSorted) val.sort();
	    return val;
	  };

	  publicAPI.addField = function (name) {
	    var initialState = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};

	    var field = Object.assign({}, DEFAULT_FIELD_STATE, initialState, { name: name });
	    field.range = [].concat(field.range); // Make sure we copy the array
	    model.fields[name] = field;
	    triggerFieldChange(field);
	  };

	  publicAPI.removeField = function (name) {
	    delete model.fields[name];
	    triggerFieldChange();
	  };

	  publicAPI.getField = function (name) {
	    return model.fields[name];
	  };

	  publicAPI.updateField = function (name) {
	    var changeSet = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};

	    var field = model.fields[name] || {};
	    var hasChange = false;

	    Object.keys(changeSet).forEach(function (key) {
	      hasChange = hasChange || JSON.stringify(field[key]) !== JSON.stringify(changeSet[key]);
	      // Set changes
	      field[key] = changeSet[key];
	    });

	    if (hasChange) {
	      field.name = name; // Just in case
	      model.fields[name] = field;
	      triggerFieldChange(field);
	    }
	  };

	  publicAPI.toggleFieldSelection = function (name) {
	    model.fields[name].active = !model.fields[name].active;
	    triggerFieldChange(model.fields[name]);
	  };

	  publicAPI.removeAllFields = function () {
	    model.fields = {};
	    triggerFieldChange();
	  };
	}

	// ----------------------------------------------------------------------------
	// Object factory
	// ----------------------------------------------------------------------------

	var DEFAULT_VALUES = {
	  fields: null,
	  fieldsSorted: true
	};

	// ----------------------------------------------------------------------------

	function extend(publicAPI, model) {
	  var initialValues = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : {};

	  Object.assign(model, DEFAULT_VALUES, initialValues);

	  _CompositeClosureHelper2.default.destroy(publicAPI, model);
	  _CompositeClosureHelper2.default.isA(publicAPI, model, PROVIDER_NAME);
	  _CompositeClosureHelper2.default.event(publicAPI, model, 'FieldChange');
	  _CompositeClosureHelper2.default.get(publicAPI, model, ['fieldsSorted']);
	  _CompositeClosureHelper2.default.set(publicAPI, model, ['fieldsSorted']);

	  fieldProvider(publicAPI, model);
	}

	// ----------------------------------------------------------------------------

	var newInstance = exports.newInstance = _CompositeClosureHelper2.default.newInstance(extend);

	// ----------------------------------------------------------------------------

	exports.default = { newInstance: newInstance, extend: extend };

/***/ },
/* 6 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	exports.newInstance = exports.STATIC = undefined;
	exports.createSortedIterator = createSortedIterator;
	exports.extend = extend;

	var _CompositeClosureHelper = __webpack_require__(2);

	var _CompositeClosureHelper2 = _interopRequireDefault(_CompositeClosureHelper);

	var _shapes = __webpack_require__(7);

	var _shapes2 = _interopRequireDefault(_shapes);

	var _ColorPalettes = __webpack_require__(20);

	var _ColorPalettes2 = _interopRequireDefault(_ColorPalettes);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	// ----------------------------------------------------------------------------
	// Global
	// ----------------------------------------------------------------------------

	function convert(item, model) {
	  var result = { color: item.colors };
	  result.shape = model.legendShapes[item.shapes];
	  return result;
	}

	function createSortedIterator(priorityOrder, propertyChoices, defaultValues) {
	  var propertyKeys = Object.keys(propertyChoices);

	  var prioritySizes = priorityOrder.map(function (name) {
	    return propertyChoices[name].length;
	  });
	  var priorityIndex = prioritySizes.map(function (i) {
	    return 0;
	  });

	  var get = function get() {
	    var item = {};
	    propertyKeys.forEach(function (name) {
	      var idx = priorityOrder.indexOf(name);
	      if (idx === -1) {
	        item[name] = defaultValues[name];
	      } else {
	        item[name] = propertyChoices[name][priorityIndex[idx]];
	      }
	    });
	    return item;
	  };

	  var next = function next() {
	    var overflowIdx = 0;
	    priorityIndex[overflowIdx] += 1;
	    while (priorityIndex[overflowIdx] === prioritySizes[overflowIdx]) {
	      // Handle overflow
	      priorityIndex[overflowIdx] = 0;
	      if (overflowIdx < priorityIndex.length) {
	        overflowIdx += 1;
	        priorityIndex[overflowIdx] += 1;
	      }
	    }
	  };

	  return { get: get, next: next };
	}

	// ----------------------------------------------------------------------------
	// Static API
	// ----------------------------------------------------------------------------

	var STATIC = exports.STATIC = {
	  shapes: _shapes2.default,
	  palettes: _ColorPalettes2.default
	};

	// ----------------------------------------------------------------------------
	// Legend Provider
	// ----------------------------------------------------------------------------

	function legendProvider(publicAPI, model) {
	  publicAPI.addLegendEntry = function (name) {
	    if (model.legendEntries.indexOf(name) === -1 && name) {
	      model.legendEntries.push(name);
	      model.legendDirty = true;
	    }
	  };

	  publicAPI.removeLegendEntry = function (name) {
	    if (model.legendEntries.indexOf(name) !== -1 && name) {
	      model.legendEntries.splice(model.legendEntries.indexOf(name), 1);
	      model.legendDirty = true;
	    }
	  };
	  publicAPI.removeAllLegendEntry = function () {
	    model.legendEntries = [];
	    model.legendDirty = true;
	  };

	  publicAPI.assignLegend = function () {
	    var newPriority = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : null;

	    if (newPriority) {
	      model.legendPriorities = newPriority;
	      model.legendDirty = true;
	    }
	    if (model.legendDirty) {
	      var shapesArray = Object.keys(model.legendShapes);
	      model.legendDirty = false;
	      model.legendMapping = {};

	      if (model.legendPriorities && model.legendPriorities.length) {
	        var defaultColor = model.legendColors[0];
	        var defaultShape = shapesArray[0];

	        var iterator = createSortedIterator(model.legendPriorities, { colors: model.legendColors, shapes: shapesArray }, { colors: defaultColor, shapes: defaultShape });

	        model.legendEntries.forEach(function (name) {
	          model.legendMapping[name] = convert(iterator.get(), model);
	          iterator.next();
	        });
	      } else {
	        model.legendEntries.forEach(function (name, idx) {
	          model.legendMapping[name] = {
	            color: model.legendColors[idx % model.legendColors.length],
	            shape: model.legendShapes[shapesArray[idx % shapesArray.length]]
	          };
	        });
	      }
	    }
	  };

	  publicAPI.useLegendPalette = function (name) {
	    var colorSet = _ColorPalettes2.default[name];
	    if (colorSet) {
	      model.legendColors = [].concat(colorSet);
	      model.legendDirty = true;
	    }
	  };

	  publicAPI.updateLegendSettings = function (settings) {
	    ['legendShapes', 'legendColors', 'legendEntries', 'legendPriorities'].forEach(function (key) {
	      if (settings[key]) {
	        model[key] = [].concat(settings.key);
	        model.legendDirty = true;
	      }
	    });
	  };

	  publicAPI.listLegendColorPalettes = function () {
	    return Object.keys(_ColorPalettes2.default);
	  };

	  publicAPI.getLegend = function (name) {
	    if (model.legendDirty) {
	      publicAPI.assignLegend();
	    }
	    return model.legendMapping[name];
	  };
	}

	// ----------------------------------------------------------------------------
	// Object factory
	// ----------------------------------------------------------------------------

	var DEFAULT_VALUES = {
	  legendShapes: _shapes2.default,
	  legendColors: [].concat(_ColorPalettes2.default.Paired),
	  legendEntries: [],
	  legendPriorities: ['shapes', 'colors'],
	  legendMapping: {},
	  legendDirty: true
	};

	// ----------------------------------------------------------------------------

	function extend(publicAPI, model) {
	  var initialValues = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : {};

	  Object.assign(model, DEFAULT_VALUES, initialValues);

	  _CompositeClosureHelper2.default.destroy(publicAPI, model);
	  _CompositeClosureHelper2.default.isA(publicAPI, model, 'LegendProvider');

	  legendProvider(publicAPI, model);
	}

	// ----------------------------------------------------------------------------

	var newInstance = exports.newInstance = _CompositeClosureHelper2.default.newInstance(extend);

	// ----------------------------------------------------------------------------

	exports.default = Object.assign({ newInstance: newInstance, extend: extend }, STATIC);

/***/ },
/* 7 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});

	var _Circle = __webpack_require__(8);

	var _Circle2 = _interopRequireDefault(_Circle);

	var _Square = __webpack_require__(12);

	var _Square2 = _interopRequireDefault(_Square);

	var _Triangle = __webpack_require__(13);

	var _Triangle2 = _interopRequireDefault(_Triangle);

	var _Diamond = __webpack_require__(14);

	var _Diamond2 = _interopRequireDefault(_Diamond);

	var _X = __webpack_require__(15);

	var _X2 = _interopRequireDefault(_X);

	var _Pentagon = __webpack_require__(16);

	var _Pentagon2 = _interopRequireDefault(_Pentagon);

	var _InvertedTriangle = __webpack_require__(17);

	var _InvertedTriangle2 = _interopRequireDefault(_InvertedTriangle);

	var _Star = __webpack_require__(18);

	var _Star2 = _interopRequireDefault(_Star);

	var _Plus = __webpack_require__(19);

	var _Plus2 = _interopRequireDefault(_Plus);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	exports.default = {
	  Circle: _Circle2.default,
	  Square: _Square2.default,
	  Triangle: _Triangle2.default,
	  Diamond: _Diamond2.default,
	  X: _X2.default,
	  Pentagon: _Pentagon2.default,
	  InvertedTriangle: _InvertedTriangle2.default,
	  Star: _Star2.default,
	  Plus: _Plus2.default
	};

/***/ },
/* 8 */
/***/ function(module, exports, __webpack_require__) {

	
	var sprite = __webpack_require__(9);
	var image = "<symbol viewBox=\"0 0 15 15\" id=\"Circle\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"> <g> <circle cx=\"7.5\" cy=\"7.5\" r=\"6\"/> </g> </symbol>";
	module.exports = sprite.add(image, "Circle");

/***/ },
/* 9 */
/***/ function(module, exports, __webpack_require__) {

	var Sprite = __webpack_require__(10);
	var globalSprite = new Sprite();

	if (document.body) {
	  globalSprite.elem = globalSprite.render(document.body);
	} else {
	  document.addEventListener('DOMContentLoaded', function () {
	    globalSprite.elem = globalSprite.render(document.body);
	  }, false);
	}

	module.exports = globalSprite;


/***/ },
/* 10 */
/***/ function(module, exports, __webpack_require__) {

	var Sniffr = __webpack_require__(11);

	/**
	 * List of SVG attributes to fix url target in them
	 * @type {string[]}
	 */
	var fixAttributes = [
	  'clipPath',
	  'colorProfile',
	  'src',
	  'cursor',
	  'fill',
	  'filter',
	  'marker',
	  'markerStart',
	  'markerMid',
	  'markerEnd',
	  'mask',
	  'stroke'
	];

	/**
	 * Query to find'em
	 * @type {string}
	 */
	var fixAttributesQuery = '[' + fixAttributes.join('],[') + ']';
	/**
	 * @type {RegExp}
	 */
	var URI_FUNC_REGEX = /^url\((.*)\)$/;

	/**
	 * Convert array-like to array
	 * @param {Object} arrayLike
	 * @returns {Array.<*>}
	 */
	function arrayFrom(arrayLike) {
	  return Array.prototype.slice.call(arrayLike, 0);
	}

	/**
	 * Handles forbidden symbols which cannot be directly used inside attributes with url(...) content.
	 * Adds leading slash for the brackets
	 * @param {string} url
	 * @return {string} encoded url
	 */
	function encodeUrlForEmbedding(url) {
	  return url.replace(/\(|\)/g, "\\$&");
	}

	/**
	 * Replaces prefix in `url()` functions
	 * @param {Element} svg
	 * @param {string} currentUrlPrefix
	 * @param {string} newUrlPrefix
	 */
	function baseUrlWorkAround(svg, currentUrlPrefix, newUrlPrefix) {
	  var nodes = svg.querySelectorAll(fixAttributesQuery);

	  if (!nodes) {
	    return;
	  }

	  arrayFrom(nodes).forEach(function (node) {
	    if (!node.attributes) {
	      return;
	    }

	    arrayFrom(node.attributes).forEach(function (attribute) {
	      var attributeName = attribute.localName.toLowerCase();

	      if (fixAttributes.indexOf(attributeName) !== -1) {
	        var match = URI_FUNC_REGEX.exec(node.getAttribute(attributeName));

	        // Do not touch urls with unexpected prefix
	        if (match && match[1].indexOf(currentUrlPrefix) === 0) {
	          var referenceUrl = encodeUrlForEmbedding(newUrlPrefix + match[1].split(currentUrlPrefix)[1]);
	          node.setAttribute(attributeName, 'url(' + referenceUrl + ')');
	        }
	      }
	    });
	  });
	}

	/**
	 * Because of Firefox bug #353575 gradients and patterns don't work if they are within a symbol.
	 * To workaround this we move the gradient definition outside the symbol element
	 * @see https://bugzilla.mozilla.org/show_bug.cgi?id=353575
	 * @param {Element} svg
	 */
	var FirefoxSymbolBugWorkaround = function (svg) {
	  var defs = svg.querySelector('defs');

	  var moveToDefsElems = svg.querySelectorAll('symbol linearGradient, symbol radialGradient, symbol pattern');
	  for (var i = 0, len = moveToDefsElems.length; i < len; i++) {
	    defs.appendChild(moveToDefsElems[i]);
	  }
	};

	/**
	 * @type {string}
	 */
	var DEFAULT_URI_PREFIX = '#';

	/**
	 * @type {string}
	 */
	var xLinkHref = 'xlink:href';
	/**
	 * @type {string}
	 */
	var xLinkNS = 'http://www.w3.org/1999/xlink';
	/**
	 * @type {string}
	 */
	var svgOpening = '<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="' + xLinkNS + '"';
	/**
	 * @type {string}
	 */
	var svgClosing = '</svg>';
	/**
	 * @type {string}
	 */
	var contentPlaceHolder = '{content}';

	/**
	 * Representation of SVG sprite
	 * @constructor
	 */
	function Sprite() {
	  var baseElement = document.getElementsByTagName('base')[0];
	  var currentUrl = window.location.href.split('#')[0];
	  var baseUrl = baseElement && baseElement.href;
	  this.urlPrefix = baseUrl && baseUrl !== currentUrl ? currentUrl + DEFAULT_URI_PREFIX : DEFAULT_URI_PREFIX;

	  var sniffr = new Sniffr();
	  sniffr.sniff();
	  this.browser = sniffr.browser;
	  this.content = [];

	  if (this.browser.name !== 'ie' && baseUrl) {
	    window.addEventListener('spriteLoaderLocationUpdated', function (e) {
	      var currentPrefix = this.urlPrefix;
	      var newUrlPrefix = e.detail.newUrl.split(DEFAULT_URI_PREFIX)[0] + DEFAULT_URI_PREFIX;
	      baseUrlWorkAround(this.svg, currentPrefix, newUrlPrefix);
	      this.urlPrefix = newUrlPrefix;

	      if (this.browser.name === 'firefox' || this.browser.name === 'edge' || this.browser.name === 'chrome' && this.browser.version[0] >= 49) {
	        var nodes = arrayFrom(document.querySelectorAll('use[*|href]'));
	        nodes.forEach(function (node) {
	          var href = node.getAttribute(xLinkHref);
	          if (href && href.indexOf(currentPrefix) === 0) {
	            node.setAttributeNS(xLinkNS, xLinkHref, newUrlPrefix + href.split(DEFAULT_URI_PREFIX)[1]);
	          }
	        });
	      }
	    }.bind(this));
	  }
	}

	Sprite.styles = ['position:absolute', 'width:0', 'height:0', 'visibility:hidden'];

	Sprite.spriteTemplate = function(){ return svgOpening + ' style="'+ Sprite.styles.join(';') +'"><defs>' + contentPlaceHolder + '</defs>' + svgClosing; }
	Sprite.symbolTemplate = function() { return svgOpening + '>' + contentPlaceHolder + svgClosing; }

	/**
	 * @type {Array<String>}
	 */
	Sprite.prototype.content = null;

	/**
	 * @param {String} content
	 * @param {String} id
	 */
	Sprite.prototype.add = function (content, id) {
	  if (this.svg) {
	    this.appendSymbol(content);
	  }

	  this.content.push(content);

	  return DEFAULT_URI_PREFIX + id;
	};

	/**
	 *
	 * @param content
	 * @param template
	 * @returns {Element}
	 */
	Sprite.prototype.wrapSVG = function (content, template) {
	  var svgString = template.replace(contentPlaceHolder, content);

	  var svg = new DOMParser().parseFromString(svgString, 'image/svg+xml').documentElement;

	  /**
	   * Fix for browser (IE, maybe other too) which are throwing 'WrongDocumentError'
	   * if you insert an element which is not part of the document
	   * @see http://stackoverflow.com/questions/7981100/how-do-i-dynamically-insert-an-svg-image-into-html#7986519
	   */
	  if (document.importNode) {
	    svg = document.importNode(svg, true);
	  }

	  if (this.browser.name !== 'ie' && this.urlPrefix) {
	    baseUrlWorkAround(svg, DEFAULT_URI_PREFIX, this.urlPrefix);
	  }

	  return svg;
	};

	Sprite.prototype.appendSymbol = function (content) {
	  var symbol = this.wrapSVG(content, Sprite.symbolTemplate()).childNodes[0];

	  this.svg.querySelector('defs').appendChild(symbol);
	  if (this.browser.name === 'firefox') {
	    FirefoxSymbolBugWorkaround(this.svg);
	  }
	};

	/**
	 * @returns {String}
	 */
	Sprite.prototype.toString = function () {
	  var wrapper = document.createElement('div');
	  wrapper.appendChild(this.render());
	  return wrapper.innerHTML;
	};

	/**
	 * @param {HTMLElement} [target]
	 * @param {Boolean} [prepend=true]
	 * @returns {HTMLElement} Rendered sprite node
	 */
	Sprite.prototype.render = function (target, prepend) {
	  target = target || null;
	  prepend = typeof prepend === 'boolean' ? prepend : true;

	  var svg = this.wrapSVG(this.content.join(''), Sprite.spriteTemplate());

	  if (this.browser.name === 'firefox') {
	    FirefoxSymbolBugWorkaround(svg);
	  }

	  if (target) {
	    if (prepend && target.childNodes[0]) {
	      target.insertBefore(svg, target.childNodes[0]);
	    } else {
	      target.appendChild(svg);
	    }
	  }

	  this.svg = svg;

	  return svg;
	};

	module.exports = Sprite;


/***/ },
/* 11 */
/***/ function(module, exports) {

	(function(host) {

	  var properties = {
	    browser: [
	      [/msie ([\.\_\d]+)/, "ie"],
	      [/trident\/.*?rv:([\.\_\d]+)/, "ie"],
	      [/firefox\/([\.\_\d]+)/, "firefox"],
	      [/chrome\/([\.\_\d]+)/, "chrome"],
	      [/version\/([\.\_\d]+).*?safari/, "safari"],
	      [/mobile safari ([\.\_\d]+)/, "safari"],
	      [/android.*?version\/([\.\_\d]+).*?safari/, "com.android.browser"],
	      [/crios\/([\.\_\d]+).*?safari/, "chrome"],
	      [/opera/, "opera"],
	      [/opera\/([\.\_\d]+)/, "opera"],
	      [/opera ([\.\_\d]+)/, "opera"],
	      [/opera mini.*?version\/([\.\_\d]+)/, "opera.mini"],
	      [/opios\/([a-z\.\_\d]+)/, "opera"],
	      [/blackberry/, "blackberry"],
	      [/blackberry.*?version\/([\.\_\d]+)/, "blackberry"],
	      [/bb\d+.*?version\/([\.\_\d]+)/, "blackberry"],
	      [/rim.*?version\/([\.\_\d]+)/, "blackberry"],
	      [/iceweasel\/([\.\_\d]+)/, "iceweasel"],
	      [/edge\/([\.\d]+)/, "edge"]
	    ],
	    os: [
	      [/linux ()([a-z\.\_\d]+)/, "linux"],
	      [/mac os x/, "macos"],
	      [/mac os x.*?([\.\_\d]+)/, "macos"],
	      [/os ([\.\_\d]+) like mac os/, "ios"],
	      [/openbsd ()([a-z\.\_\d]+)/, "openbsd"],
	      [/android/, "android"],
	      [/android ([a-z\.\_\d]+);/, "android"],
	      [/mozilla\/[a-z\.\_\d]+ \((?:mobile)|(?:tablet)/, "firefoxos"],
	      [/windows\s*(?:nt)?\s*([\.\_\d]+)/, "windows"],
	      [/windows phone.*?([\.\_\d]+)/, "windows.phone"],
	      [/windows mobile/, "windows.mobile"],
	      [/blackberry/, "blackberryos"],
	      [/bb\d+/, "blackberryos"],
	      [/rim.*?os\s*([\.\_\d]+)/, "blackberryos"]
	    ],
	    device: [
	      [/ipad/, "ipad"],
	      [/iphone/, "iphone"],
	      [/lumia/, "lumia"],
	      [/htc/, "htc"],
	      [/nexus/, "nexus"],
	      [/galaxy nexus/, "galaxy.nexus"],
	      [/nokia/, "nokia"],
	      [/ gt\-/, "galaxy"],
	      [/ sm\-/, "galaxy"],
	      [/xbox/, "xbox"],
	      [/(?:bb\d+)|(?:blackberry)|(?: rim )/, "blackberry"]
	    ]
	  };

	  var UNKNOWN = "Unknown";

	  var propertyNames = Object.keys(properties);

	  function Sniffr() {
	    var self = this;

	    propertyNames.forEach(function(propertyName) {
	      self[propertyName] = {
	        name: UNKNOWN,
	        version: [],
	        versionString: UNKNOWN
	      };
	    });
	  }

	  function determineProperty(self, propertyName, userAgent) {
	    properties[propertyName].forEach(function(propertyMatcher) {
	      var propertyRegex = propertyMatcher[0];
	      var propertyValue = propertyMatcher[1];

	      var match = userAgent.match(propertyRegex);

	      if (match) {
	        self[propertyName].name = propertyValue;

	        if (match[2]) {
	          self[propertyName].versionString = match[2];
	          self[propertyName].version = [];
	        } else if (match[1]) {
	          self[propertyName].versionString = match[1].replace(/_/g, ".");
	          self[propertyName].version = parseVersion(match[1]);
	        } else {
	          self[propertyName].versionString = UNKNOWN;
	          self[propertyName].version = [];
	        }
	      }
	    });
	  }

	  function parseVersion(versionString) {
	    return versionString.split(/[\._]/).map(function(versionPart) {
	      return parseInt(versionPart);
	    });
	  }

	  Sniffr.prototype.sniff = function(userAgentString) {
	    var self = this;
	    var userAgent = (userAgentString || navigator.userAgent || "").toLowerCase();

	    propertyNames.forEach(function(propertyName) {
	      determineProperty(self, propertyName, userAgent);
	    });
	  };


	  if (typeof module !== 'undefined' && module.exports) {
	    module.exports = Sniffr;
	  } else {
	    host.Sniffr = new Sniffr();
	    host.Sniffr.sniff(navigator.userAgent);
	  }
	})(this);


/***/ },
/* 12 */
/***/ function(module, exports, __webpack_require__) {

	
	var sprite = __webpack_require__(9);
	var image = "<symbol viewBox=\"0 0 15 15\" id=\"Square\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"> <g> <rect x=\"2\" y=\"2\" width=\"12\" height=\"12\"/> </g> </symbol>";
	module.exports = sprite.add(image, "Square");

/***/ },
/* 13 */
/***/ function(module, exports, __webpack_require__) {

	
	var sprite = __webpack_require__(9);
	var image = "<symbol viewBox=\"0 0 15 15\" id=\"Triangle\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"> <g> <polygon points=\"7.5,1 14,14 1,14\"/> </g> </symbol>";
	module.exports = sprite.add(image, "Triangle");

/***/ },
/* 14 */
/***/ function(module, exports, __webpack_require__) {

	
	var sprite = __webpack_require__(9);
	var image = "<symbol viewBox=\"0 0 15 15\" id=\"Diamond\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"> <g> <polygon points=\"1,7.5 7.5,1 14,7.5 7.5,14\"/> </g> </symbol>";
	module.exports = sprite.add(image, "Diamond");

/***/ },
/* 15 */
/***/ function(module, exports, __webpack_require__) {

	
	var sprite = __webpack_require__(9);
	var image = "<symbol viewBox=\"0 0 15 15\" id=\"X\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"> <g> <polygon points=\"4.0,1.0 7.5,4.5 11.0,1.0 14.0,4.0 10.5,7.5 14.0,11.0 11.0,14.0 7.5,10.5 4.0,14.0 1.0,11.0 4.5,7.5 1.0,4.0\"/> </g> </symbol>";
	module.exports = sprite.add(image, "X");

/***/ },
/* 16 */
/***/ function(module, exports, __webpack_require__) {

	
	var sprite = __webpack_require__(9);
	var image = "<symbol viewBox=\"0 0 15 15\" id=\"Pentagon\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"> <g> <polygon points=\"11.03,12.35 13.21,5.65 7.50,1.50 1.79,5.65 3.97,12.35\"/> </g> </symbol>";
	module.exports = sprite.add(image, "Pentagon");

/***/ },
/* 17 */
/***/ function(module, exports, __webpack_require__) {

	
	var sprite = __webpack_require__(9);
	var image = "<symbol viewBox=\"0 0 15 15\" id=\"InvertedTriangle\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"> <g> <polygon points=\"1,1 14,1 7.5,14\"/> </g> </symbol>";
	module.exports = sprite.add(image, "InvertedTriangle");

/***/ },
/* 18 */
/***/ function(module, exports, __webpack_require__) {

	
	var sprite = __webpack_require__(9);
	var image = "<symbol viewBox=\"0 0 15 15\" id=\"Star\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"> <g> <polygon points=\"11.03,12.35 9.78,8.24 13.21,5.65 8.91,5.56 7.50,1.50 6.09,5.56 1.79,5.65 5.22,8.24 3.97,12.35 7.50,9.90\"/> </g> </symbol>";
	module.exports = sprite.add(image, "Star");

/***/ },
/* 19 */
/***/ function(module, exports, __webpack_require__) {

	
	var sprite = __webpack_require__(9);
	var image = "<symbol viewBox=\"0 0 15 15\" id=\"Plus\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"> <g> <polygon points=\"5.5,1.0 9.5,1.0 9.5,5.5 14.0,5.5 14.0,9.5 9.5,9.5 9.5,14.0 5.5,14.0 5.5,9.5 1,9.5 1,5.5 5.5,5.5\"/> </g> </symbol>";
	module.exports = sprite.add(image, "Plus");

/***/ },
/* 20 */
/***/ function(module, exports) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	var YlGn = ['rgb(255, 255, 229)', 'rgb(247, 252, 185)', 'rgb(217, 240, 163)', 'rgb(173, 221, 142)', 'rgb(120, 198, 121)', 'rgb(65, 171, 93)', 'rgb(35, 132, 67)', 'rgb(0, 104, 55)', 'rgb(0, 69, 41)'];

	var YlGnBu = ['rgb(255, 255, 217)', 'rgb(237, 248, 177)', 'rgb(199, 233, 180)', 'rgb(127, 205, 187)', 'rgb(65, 182, 196)', 'rgb(29, 145, 192)', 'rgb(34, 94, 168)', 'rgb(37, 52, 148)', 'rgb(8, 29, 88)'];

	var GnBu = ['rgb(247, 252, 240)', 'rgb(224, 243, 219)', 'rgb(204, 235, 197)', 'rgb(168, 221, 181)', 'rgb(123, 204, 196)', 'rgb(78, 179, 211)', 'rgb(43, 140, 190)', 'rgb(8, 104, 172)', 'rgb(8, 64, 129)'];

	var BuGn = ['rgb(247, 252, 253)', 'rgb(229, 245, 249)', 'rgb(204, 236, 230)', 'rgb(153, 216, 201)', 'rgb(102, 194, 164)', 'rgb(65, 174, 118)', 'rgb(35, 139, 69)', 'rgb(0, 109, 44)', 'rgb(0, 68, 27)'];

	var PuBuGn = ['rgb(255, 247, 251)', 'rgb(236, 226, 240)', 'rgb(208, 209, 230)', 'rgb(166, 189, 219)', 'rgb(103, 169, 207)', 'rgb(54, 144, 192)', 'rgb(2, 129, 138)', 'rgb(1, 108, 89)', 'rgb(1, 70, 54)'];

	var PuBu = ['rgb(255, 247, 251)', 'rgb(236, 231, 242)', 'rgb(208, 209, 230)', 'rgb(166, 189, 219)', 'rgb(116, 169, 207)', 'rgb(54, 144, 192)', 'rgb(5, 112, 176)', 'rgb(4, 90, 141)', 'rgb(2, 56, 88)'];

	var BuPu = ['rgb(247, 252, 253)', 'rgb(224, 236, 244)', 'rgb(191, 211, 230)', 'rgb(158, 188, 218)', 'rgb(140, 150, 198)', 'rgb(140, 107, 177)', 'rgb(136, 65, 157)', 'rgb(129, 15, 124)', 'rgb(77, 0, 75)'];

	var RdPu = ['rgb(255, 247, 243)', 'rgb(253, 224, 221)', 'rgb(252, 197, 192)', 'rgb(250, 159, 181)', 'rgb(247, 104, 161)', 'rgb(221, 52, 151)', 'rgb(174, 1, 126)', 'rgb(122, 1, 119)', 'rgb(73, 0, 106)'];

	var PuRd = ['rgb(247, 244, 249)', 'rgb(231, 225, 239)', 'rgb(212, 185, 218)', 'rgb(201, 148, 199)', 'rgb(223, 101, 176)', 'rgb(231, 41, 138)', 'rgb(206, 18, 86)', 'rgb(152, 0, 67)', 'rgb(103, 0, 31)'];

	var OrRd = ['rgb(255, 247, 236)', 'rgb(254, 232, 200)', 'rgb(253, 212, 158)', 'rgb(253, 187, 132)', 'rgb(252, 141, 89)', 'rgb(239, 101, 72)', 'rgb(215, 48, 31)', 'rgb(179, 0, 0)', 'rgb(127, 0, 0)'];

	var YlOrRd = ['rgb(255, 255, 204)', 'rgb(255, 237, 160)', 'rgb(254, 217, 118)', 'rgb(254, 178, 76)', 'rgb(253, 141, 60)', 'rgb(252, 78, 42)', 'rgb(227, 26, 28)', 'rgb(189, 0, 38)', 'rgb(128, 0, 38)'];

	var YlOrBr = ['rgb(255, 255, 229)', 'rgb(255, 247, 188)', 'rgb(254, 227, 145)', 'rgb(254, 196, 79)', 'rgb(254, 153, 41)', 'rgb(236, 112, 20)', 'rgb(204, 76, 2)', 'rgb(153, 52, 4)', 'rgb(102, 37, 6)'];

	var Purples = ['rgb(252, 251, 253)', 'rgb(239, 237, 245)', 'rgb(218, 218, 235)', 'rgb(188, 189, 220)', 'rgb(158, 154, 200)', 'rgb(128, 125, 186)', 'rgb(106, 81, 163)', 'rgb(84, 39, 143)', 'rgb(63, 0, 125)'];

	var Blues = ['rgb(247, 251, 255)', 'rgb(222, 235, 247)', 'rgb(198, 219, 239)', 'rgb(158, 202, 225)', 'rgb(107, 174, 214)', 'rgb(66, 146, 198)', 'rgb(33, 113, 181)', 'rgb(8, 81, 156)', 'rgb(8, 48, 107)'];

	var Greens = ['rgb(247, 252, 245)', 'rgb(229, 245, 224)', 'rgb(199, 233, 192)', 'rgb(161, 217, 155)', 'rgb(116, 196, 118)', 'rgb(65, 171, 93)', 'rgb(35, 139, 69)', 'rgb(0, 109, 44)', 'rgb(0, 68, 27)'];

	var Oranges = ['rgb(255, 245, 235)', 'rgb(254, 230, 206)', 'rgb(253, 208, 162)', 'rgb(253, 174, 107)', 'rgb(253, 141, 60)', 'rgb(241, 105, 19)', 'rgb(217, 72, 1)', 'rgb(166, 54, 3)', 'rgb(127, 39, 4)'];

	var Reds = ['rgb(255, 245, 240)', 'rgb(254, 224, 210)', 'rgb(252, 187, 161)', 'rgb(252, 146, 114)', 'rgb(251, 106, 74)', 'rgb(239, 59, 44)', 'rgb(203, 24, 29)', 'rgb(165, 15, 21)', 'rgb(103, 0, 13)'];

	var Greys = ['rgb(255, 255, 255)', 'rgb(240, 240, 240)', 'rgb(217, 217, 217)', 'rgb(189, 189, 189)', 'rgb(150, 150, 150)', 'rgb(115, 115, 115)', 'rgb(82, 82, 82)', 'rgb(37, 37, 37)', 'rgb(0, 0, 0)'];

	var PuOr = ['rgb(127, 59, 8)', 'rgb(179, 88, 6)', 'rgb(224, 130, 20)', 'rgb(253, 184, 99)', 'rgb(254, 224, 182)', 'rgb(247, 247, 247)', 'rgb(216, 218, 235)', 'rgb(178, 171, 210)', 'rgb(128, 115, 172)', 'rgb(84, 39, 136)', 'rgb(45, 0, 75)'];

	var BrBG = ['rgb(84, 48, 5)', 'rgb(140, 81, 10)', 'rgb(191, 129, 45)', 'rgb(223, 194, 125)', 'rgb(246, 232, 195)', 'rgb(245, 245, 245)', 'rgb(199, 234, 229)', 'rgb(128, 205, 193)', 'rgb(53, 151, 143)', 'rgb(1, 102, 94)', 'rgb(0, 60, 48)'];

	var PRGn = ['rgb(64, 0, 75)', 'rgb(118, 42, 131)', 'rgb(153, 112, 171)', 'rgb(194, 165, 207)', 'rgb(231, 212, 232)', 'rgb(247, 247, 247)', 'rgb(217, 240, 211)', 'rgb(166, 219, 160)', 'rgb(90, 174, 97)', 'rgb(27, 120, 55)', 'rgb(0, 68, 27)'];

	var PiYG = ['rgb(142, 1, 82)', 'rgb(197, 27, 125)', 'rgb(222, 119, 174)', 'rgb(241, 182, 218)', 'rgb(253, 224, 239)', 'rgb(247, 247, 247)', 'rgb(230, 245, 208)', 'rgb(184, 225, 134)', 'rgb(127, 188, 65)', 'rgb(77, 146, 33)', 'rgb(39, 100, 25)'];

	var RdBu = ['rgb(103, 0, 31)', 'rgb(178, 24, 43)', 'rgb(214, 96, 77)', 'rgb(244, 165, 130)', 'rgb(253, 219, 199)', 'rgb(247, 247, 247)', 'rgb(209, 229, 240)', 'rgb(146, 197, 222)', 'rgb(67, 147, 195)', 'rgb(33, 102, 172)', 'rgb(5, 48, 97)'];

	var RdGy = ['rgb(103, 0, 31)', 'rgb(178, 24, 43)', 'rgb(214, 96, 77)', 'rgb(244, 165, 130)', 'rgb(253, 219, 199)', 'rgb(255, 255, 255)', 'rgb(224, 224, 224)', 'rgb(186, 186, 186)', 'rgb(135, 135, 135)', 'rgb(77, 77, 77)', 'rgb(26, 26, 26)'];

	var RdYlBu = ['rgb(165, 0, 38)', 'rgb(215, 48, 39)', 'rgb(244, 109, 67)', 'rgb(253, 174, 97)', 'rgb(254, 224, 144)', 'rgb(255, 255, 191)', 'rgb(224, 243, 248)', 'rgb(171, 217, 233)', 'rgb(116, 173, 209)', 'rgb(69, 117, 180)', 'rgb(49, 54, 149)'];

	var Spectral = ['rgb(158, 1, 66)', 'rgb(213, 62, 79)', 'rgb(244, 109, 67)', 'rgb(253, 174, 97)', 'rgb(254, 224, 139)', 'rgb(255, 255, 191)', 'rgb(230, 245, 152)', 'rgb(171, 221, 164)', 'rgb(102, 194, 165)', 'rgb(50, 136, 189)', 'rgb(94, 79, 162)'];

	var RdYlGn = ['rgb(165, 0, 38)', 'rgb(215, 48, 39)', 'rgb(244, 109, 67)', 'rgb(253, 174, 97)', 'rgb(254, 224, 139)', 'rgb(255, 255, 191)', 'rgb(217, 239, 139)', 'rgb(166, 217, 106)', 'rgb(102, 189, 99)', 'rgb(26, 152, 80)', 'rgb(0, 104, 55)'];

	var Accent = ['rgb(127, 201, 127)', 'rgb(190, 174, 212)', 'rgb(253, 192, 134)', 'rgb(255, 255, 153)', 'rgb(56, 108, 176)', 'rgb(240, 2, 127)', 'rgb(191, 91, 23)', 'rgb(102, 102, 102)'];

	var Dark2 = ['rgb(27, 158, 119)', 'rgb(217, 95, 2)', 'rgb(117, 112, 179)', 'rgb(231, 41, 138)', 'rgb(102, 166, 30)', 'rgb(230, 171, 2)', 'rgb(166, 118, 29)', 'rgb(102, 102, 102)'];

	var Paired = ['rgb(166, 206, 227)', 'rgb(31, 120, 180)', 'rgb(178, 223, 138)', 'rgb(51, 160, 44)', 'rgb(251, 154, 153)', 'rgb(227, 26, 28)', 'rgb(253, 191, 111)', 'rgb(255, 127, 0)', 'rgb(202, 178, 214)', 'rgb(106, 61, 154)', 'rgb(255, 255, 153)', 'rgb(177, 89, 40)'];

	var Pastel1 = ['rgb(251, 180, 174)', 'rgb(179, 205, 227)', 'rgb(204, 235, 197)', 'rgb(222, 203, 228)', 'rgb(254, 217, 166)', 'rgb(255, 255, 204)', 'rgb(229, 216, 189)', 'rgb(253, 218, 236)', 'rgb(242, 242, 242)'];

	var Pastel2 = ['rgb(179, 226, 205)', 'rgb(253, 205, 172)', 'rgb(203, 213, 232)', 'rgb(244, 202, 228)', 'rgb(230, 245, 201)', 'rgb(255, 242, 174)', 'rgb(241, 226, 204)', 'rgb(204, 204, 204)'];

	var Set1 = ['rgb(228, 26, 28)', 'rgb(55, 126, 184)', 'rgb(77, 175, 74)', 'rgb(152, 78, 163)', 'rgb(255, 127, 0)', 'rgb(255, 255, 51)', 'rgb(166, 86, 40)', 'rgb(247, 129, 191)', 'rgb(153, 153, 153)'];

	var Set2 = ['rgb(102, 194, 165)', 'rgb(252, 141, 98)', 'rgb(141, 160, 203)', 'rgb(231, 138, 195)', 'rgb(166, 216, 84)', 'rgb(255, 217, 47)', 'rgb(229, 196, 148)', 'rgb(179, 179, 179)'];

	var Set3 = ['rgb(141, 211, 199)', 'rgb(255, 255, 179)', 'rgb(190, 186, 218)', 'rgb(251, 128, 114)', 'rgb(128, 177, 211)', 'rgb(253, 180, 98)', 'rgb(179, 222, 105)', 'rgb(252, 205, 229)', 'rgb(217, 217, 217)', 'rgb(188, 128, 189)', 'rgb(204, 235, 197)', 'rgb(255, 237, 111)'];

	exports.default = {
	  YlGn: YlGn,
	  YlGnBu: YlGnBu,
	  GnBu: GnBu,
	  BuGn: BuGn,
	  PuBuGn: PuBuGn,
	  PuBu: PuBu,
	  BuPu: BuPu,
	  RdPu: RdPu,
	  PuRd: PuRd,
	  OrRd: OrRd,
	  YlOrRd: YlOrRd,
	  YlOrBr: YlOrBr,
	  Purples: Purples,
	  Blues: Blues,
	  Greens: Greens,
	  Oranges: Oranges,
	  Reds: Reds,
	  Greys: Greys,
	  PuOr: PuOr,
	  BrBG: BrBG,
	  PRGn: PRGn,
	  PiYG: PiYG,
	  RdBu: RdBu,
	  RdGy: RdGy,
	  RdYlBu: RdYlBu,
	  Spectral: Spectral,
	  RdYlGn: RdYlGn,
	  Accent: Accent,
	  Dark2: Dark2,
	  Paired: Paired,
	  Pastel1: Pastel1,
	  Pastel2: Pastel2,
	  Set1: Set1,
	  Set2: Set2,
	  Set3: Set3
	};

/***/ },
/* 21 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	exports.newInstance = undefined;
	exports.extend = extend;

	var _CompositeClosureHelper = __webpack_require__(2);

	var _CompositeClosureHelper2 = _interopRequireDefault(_CompositeClosureHelper);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	// ----------------------------------------------------------------------------
	// Histogram 1D Provider
	// ----------------------------------------------------------------------------

	/*
	  Data Format: Below is an example of the expected histogram 1D data format

	  {
	    "name": "points per game",
	    "min": 0,
	    "max": 32,
	    "counts": [10, 4, 0, 0, 13, ... ]
	  }
	*/

	function extend(publicAPI, model) {
	  var initialValues = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : {};

	  Object.assign(model, initialValues);

	  _CompositeClosureHelper2.default.destroy(publicAPI, model);
	  _CompositeClosureHelper2.default.isA(publicAPI, model, 'Histogram1DProvider');
	  _CompositeClosureHelper2.default.dataSubscriber(publicAPI, model, 'histogram1D', {
	    defaultMetadata: {
	      numberOfBins: 32,
	      partial: true
	    },
	    set: function set(storage, data) {
	      var numberOfBins = data.counts.length;
	      if (!storage[numberOfBins]) {
	        storage[numberOfBins] = {};
	      }
	      var binStorage = storage[numberOfBins];

	      // Ensure that empty range histogram to only fill the first bin
	      if (data.min === data.max) {
	        var totalCount = data.counts.reduce(function (a, b) {
	          return a + b;
	        }, 0);
	        data.counts = data.counts.map(function (v, i) {
	          return i ? 0 : totalCount;
	        });
	      }

	      var sameAsBefore = JSON.stringify(data) === JSON.stringify(binStorage[data.name]);
	      binStorage[data.name] = data;

	      return sameAsBefore;
	    },
	    get: function get(storage, request, dataChanged) {
	      var numberOfBins = request.metadata.numberOfBins;

	      var binStorage = storage[numberOfBins];
	      var returnedData = {};
	      var count = 0;
	      request.variables.forEach(function (name) {
	        if (binStorage && binStorage[name]) {
	          count += 1;
	          returnedData[name] = binStorage[name];
	        }
	      });
	      if (count === request.variables.length || request.metadata.partial && count > 0) {
	        return returnedData;
	      }
	      return null;
	    }
	  });
	}

	// ----------------------------------------------------------------------------

	var newInstance = exports.newInstance = _CompositeClosureHelper2.default.newInstance(extend);

	// ----------------------------------------------------------------------------

	exports.default = { newInstance: newInstance, extend: extend };

/***/ },
/* 22 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	exports.newInstance = undefined;
	exports.extend = extend;

	var _CompositeClosureHelper = __webpack_require__(2);

	var _CompositeClosureHelper2 = _interopRequireDefault(_CompositeClosureHelper);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	// ----------------------------------------------------------------------------
	// Histogram Bin Hover Provider
	// ----------------------------------------------------------------------------

	function histogramBinHoverProvider(publicAPI, model) {
	  if (!model.hoverState) {
	    model.hoverState = {};
	  }

	  publicAPI.setHoverState = function (hoverState) {
	    model.hoverState = hoverState;
	    publicAPI.fireHoverBinChange(model.hoverState);
	  };
	}

	// ----------------------------------------------------------------------------
	// Object factory
	// ----------------------------------------------------------------------------

	var DEFAULT_VALUES = {};

	// ----------------------------------------------------------------------------

	function extend(publicAPI, model) {
	  var initialValues = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : {};

	  Object.assign(model, DEFAULT_VALUES, initialValues);

	  _CompositeClosureHelper2.default.destroy(publicAPI, model);
	  _CompositeClosureHelper2.default.isA(publicAPI, model, 'HistogramBinHoverProvider');
	  _CompositeClosureHelper2.default.event(publicAPI, model, 'HoverBinChange');

	  histogramBinHoverProvider(publicAPI, model);
	}

	// ----------------------------------------------------------------------------

	var newInstance = exports.newInstance = _CompositeClosureHelper2.default.newInstance(extend);

	// ----------------------------------------------------------------------------

	exports.default = { newInstance: newInstance, extend: extend };

/***/ },
/* 23 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	exports.newInstance = undefined;
	exports.extend = extend;

	var _CompositeClosureHelper = __webpack_require__(2);

	var _CompositeClosureHelper2 = _interopRequireDefault(_CompositeClosureHelper);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	// ----------------------------------------------------------------------------
	// Partition Provider
	// ----------------------------------------------------------------------------

	function scoresProvider(publicAPI, model) {
	  publicAPI.setScores = function (scores) {
	    model.scores = [].concat(scores);
	    var scoreMapByValue = {};
	    model.scores.forEach(function (score) {
	      scoreMapByValue[score.value] = score;
	    });
	    model.scoreMapByValue = scoreMapByValue;
	    publicAPI.fireScoresChange(model.scores);
	  };

	  publicAPI.getScoreColor = function (value) {
	    var score = model.scoreMapByValue[value];
	    return score ? score.color : undefined;
	  };

	  publicAPI.getScoreName = function (value) {
	    var score = model.scoreMapByValue[value];
	    return score ? score.name : undefined;
	  };
	  publicAPI.getDefaultScore = function () {
	    if (model.scores) {
	      var index = model.scores.findIndex(function (score) {
	        return !!score.isDefault;
	      });
	      return index === -1 ? 0 : index;
	    }
	    return 0;
	  };
	  publicAPI.setDefaultScore = function (value) {
	    if (model.scores) {
	      model.scores[publicAPI.getDefaultScore()].isDefault = false;
	      model.scores[value].isDefault = true;
	    }
	  };
	}

	// ----------------------------------------------------------------------------
	// Object factory
	// ----------------------------------------------------------------------------

	var DEFAULT_VALUES = {
	  // scores: null,
	};

	// ----------------------------------------------------------------------------

	function extend(publicAPI, model) {
	  var initialValues = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : {};

	  Object.assign(model, DEFAULT_VALUES, initialValues);

	  _CompositeClosureHelper2.default.destroy(publicAPI, model);
	  _CompositeClosureHelper2.default.isA(publicAPI, model, 'ScoresProvider');
	  _CompositeClosureHelper2.default.event(publicAPI, model, 'scoresChange', false);
	  _CompositeClosureHelper2.default.get(publicAPI, model, ['scores']);

	  scoresProvider(publicAPI, model);
	}

	// ----------------------------------------------------------------------------

	var newInstance = exports.newInstance = _CompositeClosureHelper2.default.newInstance(extend);

	// ----------------------------------------------------------------------------

	exports.default = { newInstance: newInstance, extend: extend };

/***/ },
/* 24 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	exports.newInstance = undefined;
	exports.extend = extend;

	var _CompositeClosureHelper = __webpack_require__(2);

	var _CompositeClosureHelper2 = _interopRequireDefault(_CompositeClosureHelper);

	var _dataHelper = __webpack_require__(25);

	var _dataHelper2 = _interopRequireDefault(_dataHelper);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	// ----------------------------------------------------------------------------
	// Selection Provider
	// ----------------------------------------------------------------------------

	function selectionProvider(publicAPI, model) {
	  var dataSubscriptions = [];

	  if (!model.selectionData) {
	    model.selectionData = {};
	  }
	  if (!model.selectionMetaData) {
	    model.selectionMetaData = {};
	  }

	  function off() {
	    var count = dataSubscriptions.length;
	    while (count) {
	      count -= 1;
	      dataSubscriptions[count] = null;
	    }
	  }

	  function flushDataToListener(dataListener, dataChanged) {
	    try {
	      if (dataListener) {
	        var event = _dataHelper2.default.getNotificationData(model.selectionData, dataListener.request);
	        if (event) {
	          if (dataChanged && dataChanged.type === dataListener.request.type) {
	            dataListener.onDataReady(event);
	          } else if (!dataChanged) {
	            dataListener.onDataReady(event);
	          }
	        }
	      }
	    } catch (err) {
	      console.log('flushDataToListener error caught:', err);
	    }
	  }

	  // Method use to store received data
	  publicAPI.setSelectionData = function (data) {
	    _dataHelper2.default.set(model.selectionData, data);

	    // Process all subscription to see if we can trigger a notification
	    dataSubscriptions.forEach(function (listener) {
	      return flushDataToListener(listener, data);
	    });
	  };

	  // Method use to access cached data. Will return undefined if not available
	  publicAPI.getSelectionData = function (query) {
	    return _dataHelper2.default.get(model.selectionData, query);
	  };

	  // Use to extend data subscription
	  publicAPI.updateSelectionMetadata = function (addon) {
	    model.selectionMetaData[addon.type] = Object.assign({}, model.selectionMetaData[addon.type], addon.metadata);
	  };

	  // Get metadata for a given data type
	  publicAPI.getSelectionMetadata = function (type) {
	    return model.selectionMetaData[type];
	  };

	  // --------------------------------

	  publicAPI.setSelection = function (selection) {
	    model.selection = selection;
	    publicAPI.fireSelectionChange(selection);
	  };

	  // --------------------------------

	  // annotation = {
	  //    selection: {...},
	  //    score: [0],
	  //    weight: 1,
	  //    rationale: 'why not...',
	  // }

	  publicAPI.setAnnotation = function (annotation) {
	    model.annotation = annotation;
	    if (annotation.selection) {
	      publicAPI.setSelection(annotation.selection);
	    } else {
	      annotation.selection = model.selection;
	    }
	    model.shouldCreateNewAnnotation = false;
	    publicAPI.fireAnnotationChange(annotation);
	  };

	  // --------------------------------

	  publicAPI.shouldCreateNewAnnotation = function () {
	    return model.shouldCreateNewAnnotation;
	  };
	  publicAPI.setCreateNewAnnotationFlag = function (shouldCreate) {
	    return model.shouldCreateNewAnnotation = shouldCreate;
	  };

	  // --------------------------------
	  // When a new selection is made, data dependent on that selection will be pushed
	  // to subscribers.
	  // A subscriber should save the return value and call update() when they need to
	  // change the variables or meta data which is pushed to them.
	  publicAPI.subscribeToDataSelection = function (type, onDataReady) {
	    var variables = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : [];
	    var metadata = arguments.length > 3 && arguments[3] !== undefined ? arguments[3] : {};

	    var id = dataSubscriptions.length;
	    var request = { id: id, type: type, variables: variables, metadata: metadata };
	    var dataListener = { onDataReady: onDataReady, request: request };
	    dataSubscriptions.push(dataListener);
	    publicAPI.fireDataSelectionSubscriptionChange(request);
	    flushDataToListener(dataListener, null);
	    return {
	      unsubscribe: function unsubscribe() {
	        request.action = 'unsubscribe';
	        publicAPI.fireDataSelectionSubscriptionChange(request);
	        dataSubscriptions[id] = null;
	      },
	      update: function update(vars, meta) {
	        request.variables = [].concat(vars);
	        request.metadata = Object.assign({}, request.metadata, meta);
	        publicAPI.fireDataSelectionSubscriptionChange(request);
	        flushDataToListener(dataListener, null);
	      }
	    };
	  };

	  publicAPI.destroy = _CompositeClosureHelper2.default.chain(off, publicAPI.destroy);
	}

	// ----------------------------------------------------------------------------
	// Object factory
	// ----------------------------------------------------------------------------

	var DEFAULT_VALUES = {
	  // selection: null,
	  // selectionData: null,
	  // selectionMetaData: null,
	  shouldCreateNewAnnotation: false
	};

	// ----------------------------------------------------------------------------

	function extend(publicAPI, model) {
	  var initialValues = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : {};

	  Object.assign(model, DEFAULT_VALUES, initialValues);

	  _CompositeClosureHelper2.default.destroy(publicAPI, model);
	  _CompositeClosureHelper2.default.isA(publicAPI, model, 'SelectionProvider');
	  _CompositeClosureHelper2.default.get(publicAPI, model, ['selection', 'annotation']);
	  _CompositeClosureHelper2.default.event(publicAPI, model, 'selectionChange');
	  _CompositeClosureHelper2.default.event(publicAPI, model, 'annotationChange');
	  _CompositeClosureHelper2.default.event(publicAPI, model, 'dataSelectionSubscriptionChange');

	  selectionProvider(publicAPI, model);
	}

	// ----------------------------------------------------------------------------

	var newInstance = exports.newInstance = _CompositeClosureHelper2.default.newInstance(extend);

	// ----------------------------------------------------------------------------

	exports.default = { newInstance: newInstance, extend: extend };

/***/ },
/* 25 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});

	var _histogram2d = __webpack_require__(26);

	var _histogram2d2 = _interopRequireDefault(_histogram2d);

	var _counts = __webpack_require__(27);

	var _counts2 = _interopRequireDefault(_counts);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	var dataMapping = {
	  histogram2d: _histogram2d2.default,
	  counts: _counts2.default
	};

	// ----------------------------------------------------------------------------

	function getHandler(type) {
	  var handler = dataMapping[type];
	  if (handler) {
	    return handler;
	  }

	  throw new Error('No set handler for ' + type);
	}

	function set(model, data) {
	  return getHandler(data.type).set(model, data);
	}

	function get(model, data) {
	  return getHandler(data.type).get(model, data);
	}

	function getNotificationData(model, request) {
	  return getHandler(request.type).getNotificationData(model, request);
	}

	exports.default = {
	  set: set,
	  get: get,
	  getNotificationData: getNotificationData
	};

/***/ },
/* 26 */
/***/ function(module, exports) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	exports.set = set;
	// ----------------------------------------------------------------------------
	// Histogram2d
	// ----------------------------------------------------------------------------
	//
	// ===> SET
	//
	//  const payload = {
	//    type: 'histogram2d',
	//    filter: '7c1ce0b1-4ecd-4415-80d3-00364c1f7f8b',
	//    data: {
	//      x: 'temperature',
	//      y: 'pressure',
	//      bins: [...],
	//      annotationInfo: {
	//        annotationGeneration: 2,
	//        selectionGeneration: 5,
	//      },
	//      role: {
	//        score: 0,
	//        selected: true,
	//      },
	//    },
	//  }
	//
	// ===> GET
	//
	//  const query = {
	//    type: 'histogram2d',
	//    axes: ['temperature', 'pressure'],
	//    score: [0, 2],
	//  }
	//
	// const response = [
	//   {
	//     x: 'temperature',
	//     y: 'pressure',
	//     bins: [],
	//     annotationInfo: {
	//       annotationGeneration: 2,
	//       selectionGeneration: 5,
	//     },
	//     role: {
	//       score: 0,
	//       selected: true,
	//     },
	//     maxCount: 3534,
	//   }, {
	//     x: 'temperature',
	//     y: 'pressure',
	//     bins: [],
	//     annotationInfo: {
	//       annotationGeneration: 2,
	//       selectionGeneration: 5,
	//     },
	//     role: {
	//       score: 0,
	//       selected: true,
	//     },
	//     maxCount: 3534,
	//   },
	// ];
	//
	// ===> NOTIFICATION
	//
	// request = {
	//   type: 'histogram2d',
	//   variables: [
	//     ['temperature', 'pressure'],
	//     ['pressure', 'velocity'],
	//     ['velocity', 'abcd'],
	//   ],
	//   metadata: {
	//     partitionScore: [0, 2],
	//   },
	// }
	//
	// const notification = {
	//   temperature: {
	//     pressure: [
	//       {
	//         x: 'temperature',
	//         y: 'pressure',
	//         bins: [],
	//         annotationInfo: {
	//           annotationGeneration: 2,
	//           selectionGeneration: 5,
	//         },
	//         role: {
	//           score: 0,
	//           selected: true,
	//         },
	//         maxCount: 3534,
	//       }, {
	//         x: 'temperature',
	//         y: 'pressure',
	//         bins: [],
	//         annotationInfo: {
	//           annotationGeneration: 2,
	//           selectionGeneration: 5,
	//         },
	//         role: {
	//           score: 2,
	//           selected: true,
	//         },
	//         maxCount: 3534,
	//       },
	//     ],
	//   },
	//   pressure: {
	//     velocity: [
	//       {
	//         x: 'pressure',
	//         y: 'velocity',
	//         bins: [],
	//         annotationInfo: {
	//           annotationGeneration: 2,
	//           selectionGeneration: 5,
	//         },
	//         role: {
	//           score: 0,
	//           selected: true,
	//         },
	//         maxCount: 3534,
	//       }, {
	//         x: 'pressure',
	//         y: 'velocity',
	//         bins: [],
	//         annotationInfo: {
	//           annotationGeneration: 2,
	//           selectionGeneration: 5,
	//         },
	//         role: {
	//           score: 2,
	//           selected: true,
	//         },
	//         maxCount: 3534,
	//       },
	//     ],
	//   },
	//   velocity: {
	//     abcd: [
	//       {
	//         x: 'velocity',
	//         y: 'abcd',
	//         bins: [],
	//         annotationInfo: {
	//           annotationGeneration: 2,
	//           selectionGeneration: 5,
	//         },
	//         role: {
	//           score: 0,
	//           selected: true,
	//         },
	//         maxCount: 3534,
	//       }, {
	//         x: 'velocity',
	//         y: 'abcd',
	//         bins: [],
	//         annotationInfo: {
	//           annotationGeneration: 2,
	//           selectionGeneration: 5,
	//         },
	//         role: {
	//           score: 2,
	//           selected: true,
	//         },
	//         maxCount: 3534,
	//       },
	//     ],
	//   },
	// };
	//
	// ----------------------------------------------------------------------------

	// function flipHistogram(histo2d) {
	//   const newHisto2d = {
	//     bins: histo2d.bins.map(bin => {
	//       const { x, y, count } = bin;
	//       return {
	//         x: y,
	//         y: x,
	//         count,
	//       };
	//     }),
	//     x: histo2d.y,
	//     y: histo2d.x };

	//   return newHisto2d;
	// }

	// ----------------------------------------------------------------------------

	function set(model, payload) {
	  if (!model.histogram2d) {
	    model.histogram2d = {};
	  }
	  var _payload$data = payload.data,
	      x = _payload$data.x,
	      y = _payload$data.y,
	      annotationInfo = _payload$data.annotationInfo,
	      role = _payload$data.role;

	  if (!model.histogram2d[x.name]) {
	    model.histogram2d[x.name] = {};
	  }
	  if (!model.histogram2d[x.name][y.name]) {
	    model.histogram2d[x.name][y.name] = [];
	  }

	  model.histogram2d[x.name][y.name] = [].concat(payload.data, model.histogram2d[x.name][y.name].filter(function (hist) {
	    return hist.annotationInfo.annotationGeneration === annotationInfo.annotationGeneration && hist.role.score !== role.score;
	  }));

	  // Attach max count
	  var count = 0;
	  payload.data.bins.forEach(function (item) {
	    count = count < item.count ? item.count : count;
	  });
	  payload.data.maxCount = count;

	  // Create flipped histogram?
	  // FIXME
	}

	// ----------------------------------------------------------------------------

	function get(model, query) {
	  if (model.histogram2d && model.histogram2d[query.axes[0]] && model.histogram2d[query.axes[0]][query.axes[1]]) {
	    if (query.score) {
	      return model.histogram2d[query.axes[0]][query.axes[1]].filter(function (hist) {
	        return query.score.indexOf(hist.role.score) !== -1;
	      });
	    }
	    return model.histogram2d[query.axes[0]][query.axes[1]];
	  }
	  return null;
	}

	// ----------------------------------------------------------------------------

	function getNotificationData(model, request) {
	  var result = {};
	  var missingData = false;
	  var generationNumbers = [];

	  request.variables.forEach(function (axes) {
	    var histograms = get(model, { axes: axes });
	    if (histograms && histograms.length) {
	      if (!result[axes[0]]) {
	        result[axes[0]] = {};
	      }
	      result[axes[0]][axes[1]] = histograms;
	      histograms.forEach(function (hist) {
	        return generationNumbers.push(hist.annotationInfo.annotationGeneration);
	      });
	    } else {
	      missingData = true;
	    }
	  });

	  // Prevent generation mix in result
	  generationNumbers.sort();
	  var generation = generationNumbers.shift();
	  if (generationNumbers.length && generation !== generationNumbers.pop()) {
	    return null;
	  }

	  result['##annotationGeneration##'] = generation;

	  return missingData ? null : result;
	}

	// ----------------------------------------------------------------------------

	exports.default = {
	  set: set,
	  get: get,
	  getNotificationData: getNotificationData
	};

/***/ },
/* 27 */
/***/ function(module, exports) {

	"use strict";

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	exports.set = set;
	// ----------------------------------------------------------------------------
	// count
	// ----------------------------------------------------------------------------
	//
	// ===> SET
	//
	//  const payload = {
	//    type: 'count',
	//    data: {
	//       annotationInfo: {
	//         annotationGeneration: 1,
	//         selectionGeneration: 1,
	//       },
	//       count: 20,
	//       role: {
	//         selected: true,
	//         score: 0,
	//       },
	//    },
	//  }
	//
	// ===> GET
	//
	//  const query = {
	//    type: 'count',
	//  }
	//
	// const response = [
	//   {
	//   },
	// ];
	//
	// ===> NOTIFICATION
	//
	// request = {
	//   type: 'count',
	//   variables: [],
	//   metadata: {},
	// }
	//
	// const notification = {
	// };
	//
	// ----------------------------------------------------------------------------

	function keep(id) {
	  return function (item) {
	    return item.annotationInfo.annotationGeneration === id;
	  };
	}

	function sortByScore(a, b) {
	  return a.role.score - b.role.score;
	}

	function set(model, payload) {
	  var annotationGeneration = payload.data.annotationInfo.annotationGeneration;

	  model.count = (model.count || []).filter(keep(annotationGeneration)).concat(payload.data);
	  model.count.sort(sortByScore);
	  model.count = model.count.filter(function (item, idx, array) {
	    return !idx || array[idx - 1].role.score !== item.role.score;
	  });
	}

	// ----------------------------------------------------------------------------

	function get(model, query) {
	  return model.count;
	}

	// ----------------------------------------------------------------------------

	function getNotificationData(model, request) {
	  return get(model);
	}

	// ----------------------------------------------------------------------------

	exports.default = {
	  set: set,
	  get: get,
	  getNotificationData: getNotificationData
	};

/***/ },
/* 28 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(setImmediate) {'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	exports.newInstance = undefined;

	var _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"]) _i["return"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError("Invalid attempt to destructure non-iterable instance"); } }; }(); /* global document */

	exports.extend = extend;

	var _d2 = __webpack_require__(29);

	var _d3 = _interopRequireDefault(_d2);

	var _HistogramSelector = __webpack_require__(30);

	var _HistogramSelector2 = _interopRequireDefault(_HistogramSelector);

	var _CompositeClosureHelper = __webpack_require__(2);

	var _CompositeClosureHelper2 = _interopRequireDefault(_CompositeClosureHelper);

	var _D3MultiClick = __webpack_require__(41);

	var _D3MultiClick2 = _interopRequireDefault(_D3MultiClick);

	var _score = __webpack_require__(42);

	var _score2 = _interopRequireDefault(_score);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	// ----------------------------------------------------------------------------
	// Histogram Selector
	// ----------------------------------------------------------------------------
	//
	// This component is designed to display histograms in a grid and support
	// user selection of histograms. The idea being to allow the user to view
	// histograms for a large number of parameters and then select some of
	// those parameters to use in further visualizations.
	//
	// Due to the large number of DOM elements a histogram can have, we modify
	// the standard D3 graph approach to reuse DOM elements as the histograms
	// scroll offscreen.  This way we can support thousands of histograms
	// while only creating enough DOM elements to fill the screen.
	//
	// A Transform is used to reposition existing DOM elements as they
	// are reused. Supposedly this is a fast operation. The idea comes from
	// http://bl.ocks.org/gmaclennan/11130600 among other examples.
	// Reuse happens at the row level.
	//
	// The minBoxSize variable controls the smallest width that a box
	// (histogram) will use. This code will fill its container with the
	// smallest size histogram it can that does not exceed this limit and
	// provides an integral number of histograms across the container's width.
	//

	function histogramSelector(publicAPI, model) {
	  // in contact-sheet mode, specify the smallest width a histogram can shrink
	  // to before fewer histograms are created to fill the container's width
	  var minBoxSize = 200;
	  // smallest we'll let it go. Limits boxesPerRow in header GUI.
	  var minBoxSizeLimit = 125;
	  var legendSize = 15;
	  var iconSize = 24.3;
	  // hard coded because I did not figure out how to
	  // properly query this value from our container.
	  var borderSize = 6;
	  // 8? for linux/firefox, 16 for win10/chrome (hi-res screen)
	  var scrollbarWidth = 16;

	  var displayOnlySelected = false;

	  var scoreHelper = (0, _score2.default)(publicAPI, model);

	  // This function modifies the Transform property
	  // of the rows of the grid. Instead of creating new
	  // rows filled with DOM elements. Inside histogramSelector()
	  // to make sure document.head/body exists.
	  var transformCSSProp = function tcssp(property) {
	    var prefixes = ['webkit', 'ms', 'Moz', 'O'];
	    var i = -1;
	    var n = prefixes.length;
	    var s = document.head ? document.head.style : document.body ? document.body.style : null;

	    if (s === null || property.toLowerCase() in s) {
	      return property.toLowerCase();
	    }

	    /* eslint-disable no-plusplus */
	    while (++i < n) {
	      if (prefixes[i] + property in s) {
	        return '-' + prefixes[i].toLowerCase() + property.replace(/([A-Z])/g, '-$1').toLowerCase();
	      }
	    }
	    /* eslint-enable no-plusplus */

	    return false;
	  }('Transform');

	  // Apply our desired attributes to the grid rows
	  function styleRows(selection, self) {
	    selection.classed(_HistogramSelector2.default.row, true).style('height', self.rowHeight + 'px').style(transformCSSProp, function (d, i) {
	      return 'translate3d(0,' + d.key * self.rowHeight + 'px,0)';
	    });
	  }

	  // apply our desired attributes to the boxes of a row
	  function styleBoxes(selection, self) {
	    selection.style('width', self.boxWidth + 'px').style('height', self.boxHeight + 'px')
	    // .style('margin', `${self.boxMargin / 2}px`)
	    ;
	  }

	  publicAPI.svgWidth = function () {
	    return model.histWidth + model.histMargin.left + model.histMargin.right;
	  };
	  publicAPI.svgHeight = function () {
	    return model.histHeight + model.histMargin.top + model.histMargin.bottom;
	  };

	  function getClientArea() {
	    var clientRect = model.listContainer.getBoundingClientRect();
	    return [clientRect.width - borderSize - scrollbarWidth, clientRect.height - borderSize];
	  }

	  function updateSizeInformation(singleMode) {
	    var updateBoxPerRow = false;

	    var boxMargin = 3; // outside the box dimensions
	    var boxBorder = 3; // included in the box dimensions, visible border

	    // Get the client area size
	    var dimensions = getClientArea();

	    // compute key values based on our new size
	    var boxesPerRow = singleMode ? 1 : Math.max(1, Math.floor(dimensions[0] / minBoxSize));
	    model.boxWidth = Math.floor(dimensions[0] / boxesPerRow) - 2 * boxMargin;
	    if (boxesPerRow === 1) {
	      // use 3 / 4 to make a single hist wider than it is tall.
	      model.boxHeight = Math.min(Math.floor((model.boxWidth + 2 * boxMargin) * (3 / 4) - 2 * boxMargin), Math.floor(dimensions[1] - 2 * boxMargin));
	    } else {
	      model.boxHeight = model.boxWidth;
	    }
	    model.rowHeight = model.boxHeight + 2 * boxMargin;
	    model.rowsPerPage = Math.ceil(dimensions[1] / model.rowHeight);

	    if (boxesPerRow !== model.boxesPerRow) {
	      updateBoxPerRow = true;
	      model.boxesPerRow = boxesPerRow;
	    }

	    model.histWidth = model.boxWidth - boxBorder * 2 - model.histMargin.left - model.histMargin.right;
	    // other row size, probably a way to query for this
	    var otherRowHeight = 23;
	    model.histHeight = model.boxHeight - boxBorder * 2 - otherRowHeight - model.histMargin.top - model.histMargin.bottom;

	    return updateBoxPerRow;
	  }

	  // which row of model.nest does this field name reside in?
	  function getFieldRow(name) {
	    if (model.nest === null) return 0;
	    var foundRow = model.nest.reduce(function (prev, item, i) {
	      var val = item.value.filter(function (def) {
	        return def.name === name;
	      });
	      if (val.length > 0) {
	        return item.key;
	      }
	      return prev;
	    }, 0);
	    return foundRow;
	  }

	  function getCurrentFieldNames() {
	    var fieldNames = [];
	    // Initialize fields
	    if (model.provider.isA('FieldProvider')) {
	      fieldNames = !displayOnlySelected ? model.provider.getFieldNames() : model.provider.getActiveFieldNames();
	    }
	    fieldNames = scoreHelper.filterFieldNames(fieldNames);
	    return fieldNames;
	  }

	  var fieldHeaderClick = function fieldHeaderClick(d) {
	    displayOnlySelected = !displayOnlySelected;
	    publicAPI.render();
	  };

	  function incrNumBoxes(amount) {
	    if (model.singleModeName !== null) return;
	    // Get the client area size
	    var dimensions = getClientArea();
	    var maxNumBoxes = Math.floor(dimensions[0] / minBoxSizeLimit);
	    var newBoxesPerRow = Math.min(maxNumBoxes, Math.max(1, model.boxesPerRow + amount));

	    // if we actually changed, re-render, letting updateSizeInformation actually change dimensions.
	    if (newBoxesPerRow !== model.boxesPerRow) {
	      // compute a reasonable new minimum for box size based on the current container dimensions.
	      // Midway between desired and next larger number of boxes, except at limit.
	      var newMinBoxSize = newBoxesPerRow === maxNumBoxes ? minBoxSizeLimit : Math.floor(0.5 * (dimensions[0] / newBoxesPerRow + dimensions[0] / (newBoxesPerRow + 1)));
	      minBoxSize = newMinBoxSize;
	      publicAPI.render();
	    }
	  }

	  // let the caller set a specific number of boxes/row, within our normal size constraints.
	  publicAPI.requestNumBoxesPerRow = function (count) {
	    model.singleModeName = null;
	    model.singleModeSticky = false;
	    incrNumBoxes(count - model.boxesPerRow);
	  };

	  function changeSingleField(direction) {
	    if (model.singleModeName === null) return;
	    var fieldNames = getCurrentFieldNames();
	    if (fieldNames.length === 0) return;

	    var index = fieldNames.indexOf(model.singleModeName);
	    if (index === -1) index = 0;else index = (index + direction) % fieldNames.length;
	    if (index < 0) index = fieldNames.length - 1;

	    model.singleModeName = fieldNames[index];
	    publicAPI.render();
	  }

	  function createHeader(divSel) {
	    var header = divSel.append('div').classed(_HistogramSelector2.default.header, true).style('height', model.headerSize + 'px').style('line-height', model.headerSize + 'px');
	    header.append('span').on('click', fieldHeaderClick).append('i').classed(_HistogramSelector2.default.jsFieldsIcon, true);
	    header.append('span').classed(_HistogramSelector2.default.jsHeaderLabel, true).text('Only Selected').on('click', fieldHeaderClick);

	    scoreHelper.createHeader(header);

	    var numBoxesSpan = header.append('span').classed(_HistogramSelector2.default.headerBoxes, true);
	    numBoxesSpan.append('i').classed(_HistogramSelector2.default.headerBoxesMinus, true).on('click', function () {
	      return incrNumBoxes(-1);
	    });
	    numBoxesSpan.append('span').classed(_HistogramSelector2.default.jsHeaderBoxesNum, true).text(model.boxesPerRow);
	    numBoxesSpan.append('i').classed(_HistogramSelector2.default.headerBoxesPlus, true).on('click', function () {
	      return incrNumBoxes(1);
	    });

	    var singleSpan = header.append('span').classed(_HistogramSelector2.default.headerSingle, true);
	    singleSpan.append('i').classed(_HistogramSelector2.default.headerSinglePrev, true).on('click', function () {
	      return changeSingleField(-1);
	    });
	    singleSpan.append('span').classed(_HistogramSelector2.default.jsHeaderSingleField, true).text('');
	    singleSpan.append('i').classed(_HistogramSelector2.default.headerSingleNext, true).on('click', function () {
	      return changeSingleField(1);
	    });
	  }

	  function updateHeader(dataLength) {
	    if (model.singleModeSticky) {
	      // header isn't useful for a single histogram.
	      _d3.default.select(model.container).select('.' + _HistogramSelector2.default.jsHeader).style('display', 'none');
	      return;
	    }
	    _d3.default.select(model.container).select('.' + _HistogramSelector2.default.jsHeader).style('display', null);
	    _d3.default.select(model.container).select('.' + _HistogramSelector2.default.jsFieldsIcon)
	    // apply class - 'false' should come first to not remove common base class.
	    .classed(displayOnlySelected ? _HistogramSelector2.default.allFieldsIcon : _HistogramSelector2.default.selectedFieldsIcon, false).classed(!displayOnlySelected ? _HistogramSelector2.default.allFieldsIcon : _HistogramSelector2.default.selectedFieldsIcon, true);
	    scoreHelper.updateHeader();

	    _d3.default.select(model.container).select('.' + _HistogramSelector2.default.jsHeaderBoxes).style('display', model.singleModeName === null ? null : 'none');
	    _d3.default.select(model.container).select('.' + _HistogramSelector2.default.jsHeaderBoxesNum).text(model.boxesPerRow + ' /row');

	    _d3.default.select(model.container).select('.' + _HistogramSelector2.default.jsHeaderSingle).style('display', model.singleModeName === null ? 'none' : null);

	    if (model.provider.isA('LegendProvider') && model.singleModeName) {
	      var _model$provider$getLe = model.provider.getLegend(model.singleModeName),
	          color = _model$provider$getLe.color,
	          shape = _model$provider$getLe.shape;

	      _d3.default.select(model.container).select('.' + _HistogramSelector2.default.jsHeaderSingleField).html('<svg class=\'' + _HistogramSelector2.default.legendSvg + '\' width=\'' + legendSize + '\' height=\'' + legendSize + '\'\n                fill=\'' + color + '\' stroke=\'black\'><use xlink:href=\'' + shape + '\'/></svg>');
	    } else {
	      _d3.default.select(model.container).select('.' + _HistogramSelector2.default.jsHeaderSingleField).text(function () {
	        var name = model.singleModeName;
	        if (!name) return '';
	        if (name.length > 10) {
	          name = name.slice(0, 9) + '...';
	        }
	        return name;
	      });
	    }
	  }

	  publicAPI.getMouseCoords = function (tdsl) {
	    // y-coordinate is not handled correctly for svg or svgGr or overlay inside scrolling container.
	    var coord = _d3.default.mouse(tdsl.node());
	    return [coord[0] - model.histMargin.left, coord[1] - model.histMargin.top];
	  };

	  publicAPI.resize = function () {
	    if (!model.container) return;

	    var clientRect = model.container.getBoundingClientRect();
	    var deltaHeader = model.singleModeSticky ? 0 : model.headerSize;
	    if (clientRect.width !== 0 && clientRect.height > deltaHeader) {
	      model.containerHidden = false;
	      _d3.default.select(model.listContainer).style('height', clientRect.height - deltaHeader + 'px');
	      // scrollbarWidth = model.listContainer.offsetWidth - clientRect.width;
	      publicAPI.render();
	    } else {
	      model.containerHidden = true;
	    }
	  };

	  function toggleSingleModeEvt(d) {
	    if (!model.singleModeSticky) {
	      if (model.singleModeName === null) {
	        model.singleModeName = d.name;
	      } else {
	        model.singleModeName = null;
	      }
	      model.scrollToName = d.name;
	      publicAPI.render();
	    }
	    if (_d3.default.event) _d3.default.event.stopPropagation();
	  }

	  // Display a single histogram. If disableSwitch is true, switching to
	  // other histograms in the fields list is disabled.
	  // Calling requestNumBoxesPerRow() re-enables switching.
	  publicAPI.displaySingleHistogram = function (fieldName, disableSwitch) {
	    model.singleModeName = fieldName;
	    model.scrollToName = fieldName;
	    if (model.singleModeName && disableSwitch) {
	      model.singleModeSticky = true;
	    } else {
	      model.singleModeSticky = false;
	    }
	    publicAPI.resize();
	  };

	  publicAPI.disableFieldActions = function (fieldName, actionNames) {
	    if (!model.disabledFieldsActions) {
	      model.disabledFieldsActions = {};
	    }
	    if (!model.disabledFieldsActions[fieldName]) {
	      model.disabledFieldsActions[fieldName] = [];
	    }
	    var disableActionList = model.disabledFieldsActions[fieldName];
	    [].concat(actionNames).forEach(function (action) {
	      if (disableActionList.indexOf(action) === -1) {
	        disableActionList.push(action);
	      }
	    });
	  };

	  publicAPI.enableFieldActions = function (fieldName, actionNames) {
	    if (!model.disabledFieldsActions) {
	      return;
	    }
	    if (!model.disabledFieldsActions[fieldName]) {
	      return;
	    }
	    var disableActionList = model.disabledFieldsActions[fieldName];
	    [].concat(actionNames).forEach(function (action) {
	      var idx = disableActionList.indexOf(action);
	      if (idx !== -1) {
	        disableActionList.splice(idx, 1);
	      }
	    });
	  };

	  publicAPI.isFieldActionDisabled = function (fieldName, actionName) {
	    if (!model.disabledFieldsActions) {
	      return false;
	    }
	    if (!model.disabledFieldsActions[fieldName]) {
	      return false;
	    }
	    var disableActionList = model.disabledFieldsActions[fieldName];
	    return disableActionList.indexOf(actionName) !== -1;
	  };

	  publicAPI.render = function () {
	    var onlyFieldName = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : null;

	    if (!model.fieldData || onlyFieldName !== null && !model.fieldData[onlyFieldName]) {
	      return;
	    }

	    if (!model.container || model.container.offsetParent === null) return;
	    if (!model.listContainer) return;
	    if (model.containerHidden) {
	      publicAPI.resize();
	      return;
	    }

	    updateSizeInformation(model.singleModeName !== null);
	    var fieldNames = getCurrentFieldNames();

	    updateHeader(fieldNames.length);
	    if (model.singleModeName !== null) {
	      // display only one histogram at a time.
	      fieldNames = [model.singleModeName];
	    }

	    // If we find down the road that it's too expensive to re-populate the nest
	    // all the time, we can try to come up with the proper guards that make sure
	    // we do whenever we need it, but not more.  For now, we just make sure we
	    // always get the updates we need.
	    if (fieldNames.length > 0) {
	      // get the data and put it into the nest based on the
	      // number of boxesPerRow
	      var mungedData = fieldNames.filter(function (name) {
	        return model.fieldData[name];
	      }).map(function (name) {
	        var d = model.fieldData[name];
	        return d;
	      });

	      model.nest = mungedData.reduce(function (prev, item, i) {
	        var group = Math.floor(i / model.boxesPerRow);
	        if (prev[group]) {
	          prev[group].value.push(item);
	        } else {
	          prev.push({
	            key: group,
	            value: [item]
	          });
	        }
	        return prev;
	      }, []);
	    }

	    // resize the div area to be tall enough to hold all our
	    // boxes even though most are 'virtual' and lack DOM
	    var newHeight = Math.ceil(model.nest.length * model.rowHeight) + 'px';
	    model.parameterList.style('height', newHeight);

	    if (!model.nest) return;

	    // if we've changed view modes, single <==> contact sheet,
	    // we need to re-scroll.
	    if (model.scrollToName !== null) {
	      var topRow = getFieldRow(model.scrollToName);
	      model.listContainer.scrollTop = topRow * model.rowHeight;
	      model.scrollToName = null;
	    }

	    // scroll distance, in pixels.
	    var scrollY = model.listContainer.scrollTop;
	    // convert scroll from pixels to rows, get one row above (-1)
	    var offset = Math.max(0, Math.floor(scrollY / model.rowHeight) - 1);

	    // extract the visible graphs from the data based on how many rows
	    // we have scrolled down plus one above and one below (+2)
	    var count = model.rowsPerPage + 2;
	    var dataSlice = model.nest.slice(offset, offset + count);

	    // attach our slice of data to the rows
	    var rows = model.parameterList.selectAll('div').data(dataSlice, function (d) {
	      return d.key;
	    });

	    // here is the code that reuses the exit nodes to fill entry
	    // nodes. If there are not enough exit nodes then additional ones
	    // will be created as needed. The boxes start off as hidden and
	    // later have the class removed when their data is ready
	    var exitNodes = rows.exit();
	    rows.enter().append(function () {
	      var reusableNode = 0;
	      for (var i = 0; i < exitNodes[0].length; i++) {
	        reusableNode = exitNodes[0][i];
	        if (reusableNode) {
	          exitNodes[0][i] = undefined;
	          _d3.default.select(reusableNode).selectAll('table').classed(_HistogramSelector2.default.hiddenBox, true);
	          return reusableNode;
	        }
	      }
	      return document.createElement('div');
	    });
	    rows.call(styleRows, model);

	    // if there are exit rows remaining that we
	    // do not need we can delete them
	    rows.exit().remove();

	    // now put the data into the boxes
	    var boxes = rows.selectAll('table').data(function (d) {
	      return d.value;
	    });
	    boxes.enter().append('table').classed(_HistogramSelector2.default.hiddenBox, true);

	    // free up any extra boxes
	    boxes.exit().remove();

	    // scoring interface - create floating controls to set scores, values, when needed.
	    scoreHelper.createPopups();

	    // for every item that has data, create all the sub-elements
	    // and size them correctly based on our data
	    function prepareItem(def, idx) {
	      // updateData is called in response to UI events; it tells
	      // the dataProvider to update the data to match the UI.
	      //
	      // updateData must be inside prepareItem() since it uses idx;
	      // d3's listener method cannot guarantee the index passed to
	      // updateData will be correct:
	      function updateData(data) {
	        // data.selectedGen++;
	        // model.provider.updateField(data.name, { active: data.selected });
	        model.provider.toggleFieldSelection(data.name);
	      }

	      // get existing sub elements
	      var ttab = _d3.default.select(this);
	      var trow1 = ttab.select('tr.' + _HistogramSelector2.default.jsLegendRow);
	      var trow2 = ttab.select('tr.' + _HistogramSelector2.default.jsTr2);
	      var tdsl = trow2.select('td.' + _HistogramSelector2.default.jsSparkline);
	      var legendCell = trow1.select('.' + _HistogramSelector2.default.jsLegend);
	      var fieldCell = trow1.select('.' + _HistogramSelector2.default.jsFieldName);
	      var iconCell = trow1.select('.' + _HistogramSelector2.default.jsLegendIcons);
	      var iconCellViz = iconCell.select('.' + _HistogramSelector2.default.jsLegendIconsViz);
	      var svgGr = tdsl.select('svg').select('.' + _HistogramSelector2.default.jsGHist);
	      // let svgOverlay = svgGr.select(`.${style.jsOverlay}`);

	      // if they are not created yet then create them
	      if (trow1.empty()) {
	        trow1 = ttab.append('tr').classed(_HistogramSelector2.default.legendRow, true).on('click', (0, _D3MultiClick2.default)([function singleClick(d, i) {
	          // single click handler
	          // const overCoords = d3.mouse(model.listContainer);
	          updateData(d);
	        },
	        // double click handler
	        toggleSingleModeEvt]));
	        trow2 = ttab.append('tr').classed(_HistogramSelector2.default.jsTr2, true);
	        tdsl = trow2.append('td').classed(_HistogramSelector2.default.sparkline, true).attr('colspan', '3');
	        legendCell = trow1.append('td').classed(_HistogramSelector2.default.legend, true);

	        fieldCell = trow1.append('td').classed(_HistogramSelector2.default.fieldName, true);
	        iconCell = trow1.append('td').classed(_HistogramSelector2.default.legendIcons, true);
	        iconCellViz = iconCell.append('span').classed(_HistogramSelector2.default.legendIconsViz, true);
	        scoreHelper.createScoreIcons(iconCellViz);
	        iconCellViz.append('i').classed(_HistogramSelector2.default.expandIcon, true).on('click', toggleSingleModeEvt);

	        // Create SVG, and main group created inside the margins for use by axes, title, etc.
	        svgGr = tdsl.append('svg').classed(_HistogramSelector2.default.sparklineSvg, true).append('g').classed(_HistogramSelector2.default.jsGHist, true).attr('transform', 'translate( ' + model.histMargin.left + ', ' + model.histMargin.top + ' )');
	        // nested groups inside main group
	        svgGr.append('g').classed(_HistogramSelector2.default.axis, true);
	        svgGr.append('g').classed(_HistogramSelector2.default.jsGRect, true);
	        // scoring interface
	        scoreHelper.createGroups(svgGr);
	        svgGr.append('rect').classed(_HistogramSelector2.default.overlay, true).style('cursor', 'default');
	      }
	      var dataActive = def.active;
	      // Apply legend
	      if (model.provider.isA('LegendProvider')) {
	        var _model$provider$getLe2 = model.provider.getLegend(def.name),
	            color = _model$provider$getLe2.color,
	            shape = _model$provider$getLe2.shape;

	        legendCell.html('<svg class=\'' + _HistogramSelector2.default.legendSvg + '\' width=\'' + legendSize + '\' height=\'' + legendSize + '\'\n                  fill=\'' + color + '\' stroke=\'black\'><use xlink:href=\'' + shape + '\'/></svg>');
	      } else {
	        legendCell.html('<i></i>').select('i');
	      }
	      trow1.classed(!dataActive ? _HistogramSelector2.default.selectedLegendRow : _HistogramSelector2.default.unselectedLegendRow, false).classed(dataActive ? _HistogramSelector2.default.selectedLegendRow : _HistogramSelector2.default.unselectedLegendRow, true);
	      // selection outline
	      ttab.classed(_HistogramSelector2.default.hiddenBox, false).classed(!dataActive ? _HistogramSelector2.default.selectedBox : _HistogramSelector2.default.unselectedBox, false).classed(dataActive ? _HistogramSelector2.default.selectedBox : _HistogramSelector2.default.unselectedBox, true);

	      // Change interaction icons based on state.
	      // scoreHelper has save icon and score icon.
	      var numIcons = (model.singleModeSticky ? 0 : 1) + scoreHelper.numScoreIcons(def);
	      iconCell.style('width', numIcons * iconSize + 2 + 'px');
	      scoreHelper.updateScoreIcons(iconCellViz, def);
	      iconCellViz.select('.' + _HistogramSelector2.default.jsExpandIcon).attr('class', model.singleModeName === null ? _HistogramSelector2.default.expandIcon : _HistogramSelector2.default.shrinkIcon).style('display', model.singleModeSticky ? 'none' : null);
	      // + 2 accounts for internal padding.
	      var allIconsWidth = Math.ceil(iconCellViz.node().getBoundingClientRect().width) + 2;
	      // reset to the actual width used.
	      iconCell.style('width', allIconsWidth + 'px');
	      // Apply field name
	      fieldCell.style('width', model.boxWidth - (10 + legendSize + 6 + allIconsWidth) + 'px').text(def.name);

	      // adjust some settings based on current size
	      tdsl.select('svg').attr('width', publicAPI.svgWidth()).attr('height', publicAPI.svgHeight());

	      // get the histogram data and rebuild the histogram based on the results
	      var hobj = def.hobj;
	      if (hobj) {
	        var cmax = 1.0 * _d3.default.max(hobj.counts);
	        var hsize = hobj.counts.length;
	        var hdata = svgGr.select('.' + _HistogramSelector2.default.jsGRect).selectAll('.' + _HistogramSelector2.default.jsHistRect).data(hobj.counts);

	        hdata.enter().append('rect');
	        // changes apply to both enter and update data join:
	        hdata.attr('class', function (d, i) {
	          return i % 2 === 0 ? _HistogramSelector2.default.histRectEven : _HistogramSelector2.default.histRectOdd;
	        }).attr('pname', def.name).attr('y', function (d) {
	          return model.histHeight * (1.0 - d / cmax);
	        }).attr('x', function (d, i) {
	          return model.histWidth / hsize * i;
	        }).attr('height', function (d) {
	          return model.histHeight * (d / cmax);
	        }).attr('width', Math.ceil(model.histWidth / hsize));

	        hdata.exit().remove();

	        var svgOverlay = svgGr.select('.' + _HistogramSelector2.default.jsOverlay);
	        svgOverlay.attr('x', -model.histMargin.left).attr('y', -model.histMargin.top).attr('width', publicAPI.svgWidth()).attr('height', publicAPI.svgHeight()); // allow clicks inside x-axis.

	        if (!scoreHelper.editingScore(def)) {
	          if (model.provider.isA('HistogramBinHoverProvider')) {
	            svgOverlay.on('mousemove.hs', function (d, i) {
	              var mCoords = publicAPI.getMouseCoords(tdsl);
	              var binNum = Math.floor(mCoords[0] / model.histWidth * hsize);
	              var state = {};
	              state[def.name] = [binNum];
	              model.provider.setHoverState({ state: state });
	            }).on('mouseout.hs', function (d, i) {
	              var state = {};
	              state[def.name] = [-1];
	              model.provider.setHoverState({ state: state });
	            });
	          }
	          svgOverlay.on('click.hs', function (d) {
	            var overCoords = publicAPI.getMouseCoords(tdsl);
	            if (overCoords[1] <= model.histHeight) {
	              updateData(d);
	            }
	          });
	        } else {
	          // disable when score editing is happening - it's distracting.
	          // Note we still respond to hovers over other components.
	          svgOverlay.on('.hs', null);
	        }

	        // Show an x-axis with just min/max displayed.
	        // Attach scale, axis objects to this box's
	        // data (the 'def' object) to allow persistence when scrolled.
	        if (typeof def.xScale === 'undefined') {
	          def.xScale = _d3.default.scale.linear();
	        }

	        var _scoreHelper$getHistR = scoreHelper.getHistRange(def),
	            _scoreHelper$getHistR2 = _slicedToArray(_scoreHelper$getHistR, 2),
	            minRange = _scoreHelper$getHistR2[0],
	            maxRange = _scoreHelper$getHistR2[1];

	        def.xScale.rangeRound([0, model.histWidth]).domain([minRange, maxRange]);

	        if (typeof def.xAxis === 'undefined') {
	          var formatter = _d3.default.format('.3s');
	          def.xAxis = _d3.default.svg.axis().tickFormat(formatter).orient('bottom');
	        }
	        def.xAxis.scale(def.xScale);
	        var numTicks = 2;
	        if (model.histWidth >= model.moreTicksSize) {
	          numTicks = 5;
	          // using .ticks() results in skipping min/max values,
	          // if they aren't 'nice'. Make exactly 5 ticks.
	          var myTicks = _d3.default.range(numTicks).map(function (d) {
	            return minRange + d / (numTicks - 1) * (maxRange - minRange);
	          });
	          def.xAxis.tickValues(myTicks);
	        } else {
	          def.xAxis.tickValues(def.xScale.domain());
	        }
	        // nested group for the x-axis min/max display.
	        var gAxis = svgGr.select('.' + _HistogramSelector2.default.jsAxis);
	        gAxis.attr('transform', 'translate(0, ' + model.histHeight + ')').call(def.xAxis);
	        var tickLabels = gAxis.selectAll('text').classed(_HistogramSelector2.default.axisText, true);
	        numTicks = tickLabels.size();
	        tickLabels.style('text-anchor', function (d, i) {
	          return i === 0 ? 'start' : i === numTicks - 1 ? 'end' : 'middle';
	        });
	        gAxis.selectAll('line').classed(_HistogramSelector2.default.axisLine, true);
	        gAxis.selectAll('path').classed(_HistogramSelector2.default.axisPath, true);

	        scoreHelper.prepareItem(def, idx, svgGr, tdsl);
	      }
	    }

	    // make sure all the elements are created
	    // and updated
	    if (onlyFieldName === null) {
	      boxes.each(prepareItem);
	      boxes.call(styleBoxes, model);
	    } else {
	      boxes.filter(function (def) {
	        return def.name === onlyFieldName;
	      }).each(prepareItem);
	    }
	  };

	  publicAPI.setContainer = function (element) {
	    if (model.container) {
	      while (model.container.firstChild) {
	        model.container.removeChild(model.container.firstChild);
	      }
	      model.container = null;
	    }

	    model.container = element;

	    if (model.container) {
	      var cSel = _d3.default.select(model.container);
	      createHeader(cSel);
	      // wrapper height is set insize resize()
	      var wrapper = cSel.append('div').style('overflow-y', 'auto').style('overflow-x', 'hidden').on('scroll', function () {
	        publicAPI.render();
	      });

	      model.listContainer = wrapper.node();
	      model.parameterList = wrapper.append('div').classed(_HistogramSelector2.default.histogramSelector, true);

	      model.parameterList.append('span').classed(_HistogramSelector2.default.parameterScrollFix, true);
	      publicAPI.resize();

	      setImmediate(scoreHelper.updateFieldAnnotations);
	    }
	  };

	  function handleHoverUpdate(data) {
	    var everything = _d3.default.select(model.container);
	    Object.keys(data.state).forEach(function (pName) {
	      var binList = data.state[pName];
	      everything.selectAll('rect[pname=\'' + pName + '\']').classed(_HistogramSelector2.default.binHilite, function (d, i) {
	        return binList.indexOf(i) >= 0;
	      });
	    });
	  }

	  function createFieldData(fieldName) {
	    return Object.assign(model.fieldData[fieldName] || {}, model.provider.getField(fieldName), scoreHelper.defaultFieldData());
	  }

	  // Auto unmount on destroy
	  model.subscriptions.push({ unsubscribe: publicAPI.setContainer });

	  if (model.provider.isA('FieldProvider')) {
	    if (!model.fieldData) {
	      model.fieldData = {};
	    }

	    model.provider.getFieldNames().forEach(function (name) {
	      model.fieldData[name] = createFieldData(name);
	    });

	    model.subscriptions.push(model.provider.onFieldChange(function (field) {
	      if (field && model.fieldData[field.name]) {
	        Object.assign(model.fieldData[field.name], field);
	        publicAPI.render();
	      } else {
	        var fieldNames = model.provider.getFieldNames();
	        if (field) {
	          model.fieldData[field.name] = createFieldData(field.name);
	        } else {
	          // check for deleted field. Delete our fieldData if so. Ensures subscription remains up-to-date.
	          Object.keys(model.fieldData).forEach(function (name) {
	            if (fieldNames.indexOf(name) === -1) {
	              delete model.fieldData[name];
	            }
	          });
	        }
	        model.histogram1DDataSubscription.update(fieldNames, {
	          numberOfBins: model.numberOfBins,
	          partial: true
	        });
	      }
	    }));
	  }

	  if (model.provider.isA('HistogramBinHoverProvider')) {
	    model.subscriptions.push(model.provider.onHoverBinChange(handleHoverUpdate));
	  }

	  if (model.provider.isA('Histogram1DProvider')) {
	    model.histogram1DDataSubscription = model.provider.subscribeToHistogram1D(function (data) {
	      // Below, we're asking for partial updates, so we just update our
	      // cache with anything that came in.
	      Object.keys(data).forEach(function (name) {
	        if (!model.fieldData[name]) model.fieldData[name] = {};
	        if (model.fieldData[name].hobj) {
	          var oldRangeMin = model.fieldData[name].hobj.min;
	          var oldRangeMax = model.fieldData[name].hobj.max;
	          model.fieldData[name].hobj = data[name];
	          scoreHelper.rescaleDividers(name, oldRangeMin, oldRangeMax);
	        } else {
	          model.fieldData[name].hobj = data[name];
	        }
	      });

	      publicAPI.render();
	    }, Object.keys(model.fieldData), {
	      numberOfBins: model.numberOfBins,
	      partial: true
	    });

	    model.subscriptions.push(model.histogram1DDataSubscription);
	  }

	  if (model.provider.isA('AnnotationStoreProvider')) {
	    // Preload annotation from store
	    var partitionSelectionToLoad = {};
	    var annotations = model.provider.getStoredAnnotations();
	    Object.keys(annotations).forEach(function (id) {
	      var annotation = annotations[id];
	      if (annotation && annotation.selection.type === 'partition') {
	        partitionSelectionToLoad[annotation.selection.partition.variable] = annotation;
	      }
	    });
	    if (Object.keys(partitionSelectionToLoad).length) {
	      scoreHelper.updateFieldAnnotations(partitionSelectionToLoad);
	    }

	    model.subscriptions.push(model.provider.onStoreAnnotationChange(function (event) {
	      if (event.action === 'delete' && event.annotation) {
	        var annotation = event.annotation;
	        if (annotation.selection.type === 'partition') {
	          var fieldName = annotation.selection.partition.variable;
	          if (model.fieldData[fieldName]) {
	            scoreHelper.clearFieldAnnotation(fieldName);
	            publicAPI.render(fieldName);
	          }
	        }
	      }
	    }));
	  }

	  // scoring interface
	  scoreHelper.addSubscriptions();

	  // Make sure default values get applied
	  publicAPI.setContainer(model.container);

	  // Expose update fields partitions
	  publicAPI.updateFieldAnnotations = scoreHelper.updateFieldAnnotations;

	  publicAPI.getAnnotationForField = function (fieldName) {
	    return model.fieldData[fieldName].annotation;
	  };
	}

	// ----------------------------------------------------------------------------
	// Object factory
	// ----------------------------------------------------------------------------

	var DEFAULT_VALUES = {
	  container: null,
	  provider: null,
	  listContainer: null,
	  needData: true,
	  containerHidden: false,

	  parameterList: null,
	  nest: null, // nested aray of data nest[rows][boxes]
	  boxesPerRow: 0,
	  rowsPerPage: 0,
	  boxWidth: 120,
	  boxHeight: 120,
	  // show 1 per row?
	  singleModeName: null,
	  singleModeSticky: false,
	  scrollToName: null,
	  // margins inside the SVG element.
	  histMargin: { top: 6, right: 12, bottom: 23, left: 12 },
	  histWidth: 90,
	  histHeight: 70,
	  // what's the smallest histogram size that shows 5 ticks, instead of min/max? in pixels
	  moreTicksSize: 300,
	  lastOffset: -1,
	  headerSize: 25,
	  // scoring interface activated by passing in 'scores' array externally.
	  // scores: [{ name: 'Yes', color: '#00C900' }, ... ],
	  defaultScore: 0,
	  dragMargin: 8,
	  selectedDef: null,

	  numberOfBins: 32
	};

	// ----------------------------------------------------------------------------

	function extend(publicAPI, model) {
	  var initialValues = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : {};

	  Object.assign(model, DEFAULT_VALUES, initialValues);

	  _CompositeClosureHelper2.default.destroy(publicAPI, model);
	  _CompositeClosureHelper2.default.isA(publicAPI, model, 'VizComponent');
	  _CompositeClosureHelper2.default.get(publicAPI, model, ['provider', 'container', 'numberOfBins']);
	  _CompositeClosureHelper2.default.set(publicAPI, model, ['numberOfBins']);
	  _CompositeClosureHelper2.default.dynamicArray(publicAPI, model, 'readOnlyFields');

	  histogramSelector(publicAPI, model);
	}

	// ----------------------------------------------------------------------------

	var newInstance = exports.newInstance = _CompositeClosureHelper2.default.newInstance(extend);

	// ----------------------------------------------------------------------------

	exports.default = { newInstance: newInstance, extend: extend };
	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(3).setImmediate))

/***/ },
/* 29 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_FACTORY__, __WEBPACK_AMD_DEFINE_RESULT__;!function() {
	  var d3 = {
	    version: "3.5.17"
	  };
	  var d3_arraySlice = [].slice, d3_array = function(list) {
	    return d3_arraySlice.call(list);
	  };
	  var d3_document = this.document;
	  function d3_documentElement(node) {
	    return node && (node.ownerDocument || node.document || node).documentElement;
	  }
	  function d3_window(node) {
	    return node && (node.ownerDocument && node.ownerDocument.defaultView || node.document && node || node.defaultView);
	  }
	  if (d3_document) {
	    try {
	      d3_array(d3_document.documentElement.childNodes)[0].nodeType;
	    } catch (e) {
	      d3_array = function(list) {
	        var i = list.length, array = new Array(i);
	        while (i--) array[i] = list[i];
	        return array;
	      };
	    }
	  }
	  if (!Date.now) Date.now = function() {
	    return +new Date();
	  };
	  if (d3_document) {
	    try {
	      d3_document.createElement("DIV").style.setProperty("opacity", 0, "");
	    } catch (error) {
	      var d3_element_prototype = this.Element.prototype, d3_element_setAttribute = d3_element_prototype.setAttribute, d3_element_setAttributeNS = d3_element_prototype.setAttributeNS, d3_style_prototype = this.CSSStyleDeclaration.prototype, d3_style_setProperty = d3_style_prototype.setProperty;
	      d3_element_prototype.setAttribute = function(name, value) {
	        d3_element_setAttribute.call(this, name, value + "");
	      };
	      d3_element_prototype.setAttributeNS = function(space, local, value) {
	        d3_element_setAttributeNS.call(this, space, local, value + "");
	      };
	      d3_style_prototype.setProperty = function(name, value, priority) {
	        d3_style_setProperty.call(this, name, value + "", priority);
	      };
	    }
	  }
	  d3.ascending = d3_ascending;
	  function d3_ascending(a, b) {
	    return a < b ? -1 : a > b ? 1 : a >= b ? 0 : NaN;
	  }
	  d3.descending = function(a, b) {
	    return b < a ? -1 : b > a ? 1 : b >= a ? 0 : NaN;
	  };
	  d3.min = function(array, f) {
	    var i = -1, n = array.length, a, b;
	    if (arguments.length === 1) {
	      while (++i < n) if ((b = array[i]) != null && b >= b) {
	        a = b;
	        break;
	      }
	      while (++i < n) if ((b = array[i]) != null && a > b) a = b;
	    } else {
	      while (++i < n) if ((b = f.call(array, array[i], i)) != null && b >= b) {
	        a = b;
	        break;
	      }
	      while (++i < n) if ((b = f.call(array, array[i], i)) != null && a > b) a = b;
	    }
	    return a;
	  };
	  d3.max = function(array, f) {
	    var i = -1, n = array.length, a, b;
	    if (arguments.length === 1) {
	      while (++i < n) if ((b = array[i]) != null && b >= b) {
	        a = b;
	        break;
	      }
	      while (++i < n) if ((b = array[i]) != null && b > a) a = b;
	    } else {
	      while (++i < n) if ((b = f.call(array, array[i], i)) != null && b >= b) {
	        a = b;
	        break;
	      }
	      while (++i < n) if ((b = f.call(array, array[i], i)) != null && b > a) a = b;
	    }
	    return a;
	  };
	  d3.extent = function(array, f) {
	    var i = -1, n = array.length, a, b, c;
	    if (arguments.length === 1) {
	      while (++i < n) if ((b = array[i]) != null && b >= b) {
	        a = c = b;
	        break;
	      }
	      while (++i < n) if ((b = array[i]) != null) {
	        if (a > b) a = b;
	        if (c < b) c = b;
	      }
	    } else {
	      while (++i < n) if ((b = f.call(array, array[i], i)) != null && b >= b) {
	        a = c = b;
	        break;
	      }
	      while (++i < n) if ((b = f.call(array, array[i], i)) != null) {
	        if (a > b) a = b;
	        if (c < b) c = b;
	      }
	    }
	    return [ a, c ];
	  };
	  function d3_number(x) {
	    return x === null ? NaN : +x;
	  }
	  function d3_numeric(x) {
	    return !isNaN(x);
	  }
	  d3.sum = function(array, f) {
	    var s = 0, n = array.length, a, i = -1;
	    if (arguments.length === 1) {
	      while (++i < n) if (d3_numeric(a = +array[i])) s += a;
	    } else {
	      while (++i < n) if (d3_numeric(a = +f.call(array, array[i], i))) s += a;
	    }
	    return s;
	  };
	  d3.mean = function(array, f) {
	    var s = 0, n = array.length, a, i = -1, j = n;
	    if (arguments.length === 1) {
	      while (++i < n) if (d3_numeric(a = d3_number(array[i]))) s += a; else --j;
	    } else {
	      while (++i < n) if (d3_numeric(a = d3_number(f.call(array, array[i], i)))) s += a; else --j;
	    }
	    if (j) return s / j;
	  };
	  d3.quantile = function(values, p) {
	    var H = (values.length - 1) * p + 1, h = Math.floor(H), v = +values[h - 1], e = H - h;
	    return e ? v + e * (values[h] - v) : v;
	  };
	  d3.median = function(array, f) {
	    var numbers = [], n = array.length, a, i = -1;
	    if (arguments.length === 1) {
	      while (++i < n) if (d3_numeric(a = d3_number(array[i]))) numbers.push(a);
	    } else {
	      while (++i < n) if (d3_numeric(a = d3_number(f.call(array, array[i], i)))) numbers.push(a);
	    }
	    if (numbers.length) return d3.quantile(numbers.sort(d3_ascending), .5);
	  };
	  d3.variance = function(array, f) {
	    var n = array.length, m = 0, a, d, s = 0, i = -1, j = 0;
	    if (arguments.length === 1) {
	      while (++i < n) {
	        if (d3_numeric(a = d3_number(array[i]))) {
	          d = a - m;
	          m += d / ++j;
	          s += d * (a - m);
	        }
	      }
	    } else {
	      while (++i < n) {
	        if (d3_numeric(a = d3_number(f.call(array, array[i], i)))) {
	          d = a - m;
	          m += d / ++j;
	          s += d * (a - m);
	        }
	      }
	    }
	    if (j > 1) return s / (j - 1);
	  };
	  d3.deviation = function() {
	    var v = d3.variance.apply(this, arguments);
	    return v ? Math.sqrt(v) : v;
	  };
	  function d3_bisector(compare) {
	    return {
	      left: function(a, x, lo, hi) {
	        if (arguments.length < 3) lo = 0;
	        if (arguments.length < 4) hi = a.length;
	        while (lo < hi) {
	          var mid = lo + hi >>> 1;
	          if (compare(a[mid], x) < 0) lo = mid + 1; else hi = mid;
	        }
	        return lo;
	      },
	      right: function(a, x, lo, hi) {
	        if (arguments.length < 3) lo = 0;
	        if (arguments.length < 4) hi = a.length;
	        while (lo < hi) {
	          var mid = lo + hi >>> 1;
	          if (compare(a[mid], x) > 0) hi = mid; else lo = mid + 1;
	        }
	        return lo;
	      }
	    };
	  }
	  var d3_bisect = d3_bisector(d3_ascending);
	  d3.bisectLeft = d3_bisect.left;
	  d3.bisect = d3.bisectRight = d3_bisect.right;
	  d3.bisector = function(f) {
	    return d3_bisector(f.length === 1 ? function(d, x) {
	      return d3_ascending(f(d), x);
	    } : f);
	  };
	  d3.shuffle = function(array, i0, i1) {
	    if ((m = arguments.length) < 3) {
	      i1 = array.length;
	      if (m < 2) i0 = 0;
	    }
	    var m = i1 - i0, t, i;
	    while (m) {
	      i = Math.random() * m-- | 0;
	      t = array[m + i0], array[m + i0] = array[i + i0], array[i + i0] = t;
	    }
	    return array;
	  };
	  d3.permute = function(array, indexes) {
	    var i = indexes.length, permutes = new Array(i);
	    while (i--) permutes[i] = array[indexes[i]];
	    return permutes;
	  };
	  d3.pairs = function(array) {
	    var i = 0, n = array.length - 1, p0, p1 = array[0], pairs = new Array(n < 0 ? 0 : n);
	    while (i < n) pairs[i] = [ p0 = p1, p1 = array[++i] ];
	    return pairs;
	  };
	  d3.transpose = function(matrix) {
	    if (!(n = matrix.length)) return [];
	    for (var i = -1, m = d3.min(matrix, d3_transposeLength), transpose = new Array(m); ++i < m; ) {
	      for (var j = -1, n, row = transpose[i] = new Array(n); ++j < n; ) {
	        row[j] = matrix[j][i];
	      }
	    }
	    return transpose;
	  };
	  function d3_transposeLength(d) {
	    return d.length;
	  }
	  d3.zip = function() {
	    return d3.transpose(arguments);
	  };
	  d3.keys = function(map) {
	    var keys = [];
	    for (var key in map) keys.push(key);
	    return keys;
	  };
	  d3.values = function(map) {
	    var values = [];
	    for (var key in map) values.push(map[key]);
	    return values;
	  };
	  d3.entries = function(map) {
	    var entries = [];
	    for (var key in map) entries.push({
	      key: key,
	      value: map[key]
	    });
	    return entries;
	  };
	  d3.merge = function(arrays) {
	    var n = arrays.length, m, i = -1, j = 0, merged, array;
	    while (++i < n) j += arrays[i].length;
	    merged = new Array(j);
	    while (--n >= 0) {
	      array = arrays[n];
	      m = array.length;
	      while (--m >= 0) {
	        merged[--j] = array[m];
	      }
	    }
	    return merged;
	  };
	  var abs = Math.abs;
	  d3.range = function(start, stop, step) {
	    if (arguments.length < 3) {
	      step = 1;
	      if (arguments.length < 2) {
	        stop = start;
	        start = 0;
	      }
	    }
	    if ((stop - start) / step === Infinity) throw new Error("infinite range");
	    var range = [], k = d3_range_integerScale(abs(step)), i = -1, j;
	    start *= k, stop *= k, step *= k;
	    if (step < 0) while ((j = start + step * ++i) > stop) range.push(j / k); else while ((j = start + step * ++i) < stop) range.push(j / k);
	    return range;
	  };
	  function d3_range_integerScale(x) {
	    var k = 1;
	    while (x * k % 1) k *= 10;
	    return k;
	  }
	  function d3_class(ctor, properties) {
	    for (var key in properties) {
	      Object.defineProperty(ctor.prototype, key, {
	        value: properties[key],
	        enumerable: false
	      });
	    }
	  }
	  d3.map = function(object, f) {
	    var map = new d3_Map();
	    if (object instanceof d3_Map) {
	      object.forEach(function(key, value) {
	        map.set(key, value);
	      });
	    } else if (Array.isArray(object)) {
	      var i = -1, n = object.length, o;
	      if (arguments.length === 1) while (++i < n) map.set(i, object[i]); else while (++i < n) map.set(f.call(object, o = object[i], i), o);
	    } else {
	      for (var key in object) map.set(key, object[key]);
	    }
	    return map;
	  };
	  function d3_Map() {
	    this._ = Object.create(null);
	  }
	  var d3_map_proto = "__proto__", d3_map_zero = "\x00";
	  d3_class(d3_Map, {
	    has: d3_map_has,
	    get: function(key) {
	      return this._[d3_map_escape(key)];
	    },
	    set: function(key, value) {
	      return this._[d3_map_escape(key)] = value;
	    },
	    remove: d3_map_remove,
	    keys: d3_map_keys,
	    values: function() {
	      var values = [];
	      for (var key in this._) values.push(this._[key]);
	      return values;
	    },
	    entries: function() {
	      var entries = [];
	      for (var key in this._) entries.push({
	        key: d3_map_unescape(key),
	        value: this._[key]
	      });
	      return entries;
	    },
	    size: d3_map_size,
	    empty: d3_map_empty,
	    forEach: function(f) {
	      for (var key in this._) f.call(this, d3_map_unescape(key), this._[key]);
	    }
	  });
	  function d3_map_escape(key) {
	    return (key += "") === d3_map_proto || key[0] === d3_map_zero ? d3_map_zero + key : key;
	  }
	  function d3_map_unescape(key) {
	    return (key += "")[0] === d3_map_zero ? key.slice(1) : key;
	  }
	  function d3_map_has(key) {
	    return d3_map_escape(key) in this._;
	  }
	  function d3_map_remove(key) {
	    return (key = d3_map_escape(key)) in this._ && delete this._[key];
	  }
	  function d3_map_keys() {
	    var keys = [];
	    for (var key in this._) keys.push(d3_map_unescape(key));
	    return keys;
	  }
	  function d3_map_size() {
	    var size = 0;
	    for (var key in this._) ++size;
	    return size;
	  }
	  function d3_map_empty() {
	    for (var key in this._) return false;
	    return true;
	  }
	  d3.nest = function() {
	    var nest = {}, keys = [], sortKeys = [], sortValues, rollup;
	    function map(mapType, array, depth) {
	      if (depth >= keys.length) return rollup ? rollup.call(nest, array) : sortValues ? array.sort(sortValues) : array;
	      var i = -1, n = array.length, key = keys[depth++], keyValue, object, setter, valuesByKey = new d3_Map(), values;
	      while (++i < n) {
	        if (values = valuesByKey.get(keyValue = key(object = array[i]))) {
	          values.push(object);
	        } else {
	          valuesByKey.set(keyValue, [ object ]);
	        }
	      }
	      if (mapType) {
	        object = mapType();
	        setter = function(keyValue, values) {
	          object.set(keyValue, map(mapType, values, depth));
	        };
	      } else {
	        object = {};
	        setter = function(keyValue, values) {
	          object[keyValue] = map(mapType, values, depth);
	        };
	      }
	      valuesByKey.forEach(setter);
	      return object;
	    }
	    function entries(map, depth) {
	      if (depth >= keys.length) return map;
	      var array = [], sortKey = sortKeys[depth++];
	      map.forEach(function(key, keyMap) {
	        array.push({
	          key: key,
	          values: entries(keyMap, depth)
	        });
	      });
	      return sortKey ? array.sort(function(a, b) {
	        return sortKey(a.key, b.key);
	      }) : array;
	    }
	    nest.map = function(array, mapType) {
	      return map(mapType, array, 0);
	    };
	    nest.entries = function(array) {
	      return entries(map(d3.map, array, 0), 0);
	    };
	    nest.key = function(d) {
	      keys.push(d);
	      return nest;
	    };
	    nest.sortKeys = function(order) {
	      sortKeys[keys.length - 1] = order;
	      return nest;
	    };
	    nest.sortValues = function(order) {
	      sortValues = order;
	      return nest;
	    };
	    nest.rollup = function(f) {
	      rollup = f;
	      return nest;
	    };
	    return nest;
	  };
	  d3.set = function(array) {
	    var set = new d3_Set();
	    if (array) for (var i = 0, n = array.length; i < n; ++i) set.add(array[i]);
	    return set;
	  };
	  function d3_Set() {
	    this._ = Object.create(null);
	  }
	  d3_class(d3_Set, {
	    has: d3_map_has,
	    add: function(key) {
	      this._[d3_map_escape(key += "")] = true;
	      return key;
	    },
	    remove: d3_map_remove,
	    values: d3_map_keys,
	    size: d3_map_size,
	    empty: d3_map_empty,
	    forEach: function(f) {
	      for (var key in this._) f.call(this, d3_map_unescape(key));
	    }
	  });
	  d3.behavior = {};
	  function d3_identity(d) {
	    return d;
	  }
	  d3.rebind = function(target, source) {
	    var i = 1, n = arguments.length, method;
	    while (++i < n) target[method = arguments[i]] = d3_rebind(target, source, source[method]);
	    return target;
	  };
	  function d3_rebind(target, source, method) {
	    return function() {
	      var value = method.apply(source, arguments);
	      return value === source ? target : value;
	    };
	  }
	  function d3_vendorSymbol(object, name) {
	    if (name in object) return name;
	    name = name.charAt(0).toUpperCase() + name.slice(1);
	    for (var i = 0, n = d3_vendorPrefixes.length; i < n; ++i) {
	      var prefixName = d3_vendorPrefixes[i] + name;
	      if (prefixName in object) return prefixName;
	    }
	  }
	  var d3_vendorPrefixes = [ "webkit", "ms", "moz", "Moz", "o", "O" ];
	  function d3_noop() {}
	  d3.dispatch = function() {
	    var dispatch = new d3_dispatch(), i = -1, n = arguments.length;
	    while (++i < n) dispatch[arguments[i]] = d3_dispatch_event(dispatch);
	    return dispatch;
	  };
	  function d3_dispatch() {}
	  d3_dispatch.prototype.on = function(type, listener) {
	    var i = type.indexOf("."), name = "";
	    if (i >= 0) {
	      name = type.slice(i + 1);
	      type = type.slice(0, i);
	    }
	    if (type) return arguments.length < 2 ? this[type].on(name) : this[type].on(name, listener);
	    if (arguments.length === 2) {
	      if (listener == null) for (type in this) {
	        if (this.hasOwnProperty(type)) this[type].on(name, null);
	      }
	      return this;
	    }
	  };
	  function d3_dispatch_event(dispatch) {
	    var listeners = [], listenerByName = new d3_Map();
	    function event() {
	      var z = listeners, i = -1, n = z.length, l;
	      while (++i < n) if (l = z[i].on) l.apply(this, arguments);
	      return dispatch;
	    }
	    event.on = function(name, listener) {
	      var l = listenerByName.get(name), i;
	      if (arguments.length < 2) return l && l.on;
	      if (l) {
	        l.on = null;
	        listeners = listeners.slice(0, i = listeners.indexOf(l)).concat(listeners.slice(i + 1));
	        listenerByName.remove(name);
	      }
	      if (listener) listeners.push(listenerByName.set(name, {
	        on: listener
	      }));
	      return dispatch;
	    };
	    return event;
	  }
	  d3.event = null;
	  function d3_eventPreventDefault() {
	    d3.event.preventDefault();
	  }
	  function d3_eventSource() {
	    var e = d3.event, s;
	    while (s = e.sourceEvent) e = s;
	    return e;
	  }
	  function d3_eventDispatch(target) {
	    var dispatch = new d3_dispatch(), i = 0, n = arguments.length;
	    while (++i < n) dispatch[arguments[i]] = d3_dispatch_event(dispatch);
	    dispatch.of = function(thiz, argumentz) {
	      return function(e1) {
	        try {
	          var e0 = e1.sourceEvent = d3.event;
	          e1.target = target;
	          d3.event = e1;
	          dispatch[e1.type].apply(thiz, argumentz);
	        } finally {
	          d3.event = e0;
	        }
	      };
	    };
	    return dispatch;
	  }
	  d3.requote = function(s) {
	    return s.replace(d3_requote_re, "\\$&");
	  };
	  var d3_requote_re = /[\\\^\$\*\+\?\|\[\]\(\)\.\{\}]/g;
	  var d3_subclass = {}.__proto__ ? function(object, prototype) {
	    object.__proto__ = prototype;
	  } : function(object, prototype) {
	    for (var property in prototype) object[property] = prototype[property];
	  };
	  function d3_selection(groups) {
	    d3_subclass(groups, d3_selectionPrototype);
	    return groups;
	  }
	  var d3_select = function(s, n) {
	    return n.querySelector(s);
	  }, d3_selectAll = function(s, n) {
	    return n.querySelectorAll(s);
	  }, d3_selectMatches = function(n, s) {
	    var d3_selectMatcher = n.matches || n[d3_vendorSymbol(n, "matchesSelector")];
	    d3_selectMatches = function(n, s) {
	      return d3_selectMatcher.call(n, s);
	    };
	    return d3_selectMatches(n, s);
	  };
	  if (typeof Sizzle === "function") {
	    d3_select = function(s, n) {
	      return Sizzle(s, n)[0] || null;
	    };
	    d3_selectAll = Sizzle;
	    d3_selectMatches = Sizzle.matchesSelector;
	  }
	  d3.selection = function() {
	    return d3.select(d3_document.documentElement);
	  };
	  var d3_selectionPrototype = d3.selection.prototype = [];
	  d3_selectionPrototype.select = function(selector) {
	    var subgroups = [], subgroup, subnode, group, node;
	    selector = d3_selection_selector(selector);
	    for (var j = -1, m = this.length; ++j < m; ) {
	      subgroups.push(subgroup = []);
	      subgroup.parentNode = (group = this[j]).parentNode;
	      for (var i = -1, n = group.length; ++i < n; ) {
	        if (node = group[i]) {
	          subgroup.push(subnode = selector.call(node, node.__data__, i, j));
	          if (subnode && "__data__" in node) subnode.__data__ = node.__data__;
	        } else {
	          subgroup.push(null);
	        }
	      }
	    }
	    return d3_selection(subgroups);
	  };
	  function d3_selection_selector(selector) {
	    return typeof selector === "function" ? selector : function() {
	      return d3_select(selector, this);
	    };
	  }
	  d3_selectionPrototype.selectAll = function(selector) {
	    var subgroups = [], subgroup, node;
	    selector = d3_selection_selectorAll(selector);
	    for (var j = -1, m = this.length; ++j < m; ) {
	      for (var group = this[j], i = -1, n = group.length; ++i < n; ) {
	        if (node = group[i]) {
	          subgroups.push(subgroup = d3_array(selector.call(node, node.__data__, i, j)));
	          subgroup.parentNode = node;
	        }
	      }
	    }
	    return d3_selection(subgroups);
	  };
	  function d3_selection_selectorAll(selector) {
	    return typeof selector === "function" ? selector : function() {
	      return d3_selectAll(selector, this);
	    };
	  }
	  var d3_nsXhtml = "http://www.w3.org/1999/xhtml";
	  var d3_nsPrefix = {
	    svg: "http://www.w3.org/2000/svg",
	    xhtml: d3_nsXhtml,
	    xlink: "http://www.w3.org/1999/xlink",
	    xml: "http://www.w3.org/XML/1998/namespace",
	    xmlns: "http://www.w3.org/2000/xmlns/"
	  };
	  d3.ns = {
	    prefix: d3_nsPrefix,
	    qualify: function(name) {
	      var i = name.indexOf(":"), prefix = name;
	      if (i >= 0 && (prefix = name.slice(0, i)) !== "xmlns") name = name.slice(i + 1);
	      return d3_nsPrefix.hasOwnProperty(prefix) ? {
	        space: d3_nsPrefix[prefix],
	        local: name
	      } : name;
	    }
	  };
	  d3_selectionPrototype.attr = function(name, value) {
	    if (arguments.length < 2) {
	      if (typeof name === "string") {
	        var node = this.node();
	        name = d3.ns.qualify(name);
	        return name.local ? node.getAttributeNS(name.space, name.local) : node.getAttribute(name);
	      }
	      for (value in name) this.each(d3_selection_attr(value, name[value]));
	      return this;
	    }
	    return this.each(d3_selection_attr(name, value));
	  };
	  function d3_selection_attr(name, value) {
	    name = d3.ns.qualify(name);
	    function attrNull() {
	      this.removeAttribute(name);
	    }
	    function attrNullNS() {
	      this.removeAttributeNS(name.space, name.local);
	    }
	    function attrConstant() {
	      this.setAttribute(name, value);
	    }
	    function attrConstantNS() {
	      this.setAttributeNS(name.space, name.local, value);
	    }
	    function attrFunction() {
	      var x = value.apply(this, arguments);
	      if (x == null) this.removeAttribute(name); else this.setAttribute(name, x);
	    }
	    function attrFunctionNS() {
	      var x = value.apply(this, arguments);
	      if (x == null) this.removeAttributeNS(name.space, name.local); else this.setAttributeNS(name.space, name.local, x);
	    }
	    return value == null ? name.local ? attrNullNS : attrNull : typeof value === "function" ? name.local ? attrFunctionNS : attrFunction : name.local ? attrConstantNS : attrConstant;
	  }
	  function d3_collapse(s) {
	    return s.trim().replace(/\s+/g, " ");
	  }
	  d3_selectionPrototype.classed = function(name, value) {
	    if (arguments.length < 2) {
	      if (typeof name === "string") {
	        var node = this.node(), n = (name = d3_selection_classes(name)).length, i = -1;
	        if (value = node.classList) {
	          while (++i < n) if (!value.contains(name[i])) return false;
	        } else {
	          value = node.getAttribute("class");
	          while (++i < n) if (!d3_selection_classedRe(name[i]).test(value)) return false;
	        }
	        return true;
	      }
	      for (value in name) this.each(d3_selection_classed(value, name[value]));
	      return this;
	    }
	    return this.each(d3_selection_classed(name, value));
	  };
	  function d3_selection_classedRe(name) {
	    return new RegExp("(?:^|\\s+)" + d3.requote(name) + "(?:\\s+|$)", "g");
	  }
	  function d3_selection_classes(name) {
	    return (name + "").trim().split(/^|\s+/);
	  }
	  function d3_selection_classed(name, value) {
	    name = d3_selection_classes(name).map(d3_selection_classedName);
	    var n = name.length;
	    function classedConstant() {
	      var i = -1;
	      while (++i < n) name[i](this, value);
	    }
	    function classedFunction() {
	      var i = -1, x = value.apply(this, arguments);
	      while (++i < n) name[i](this, x);
	    }
	    return typeof value === "function" ? classedFunction : classedConstant;
	  }
	  function d3_selection_classedName(name) {
	    var re = d3_selection_classedRe(name);
	    return function(node, value) {
	      if (c = node.classList) return value ? c.add(name) : c.remove(name);
	      var c = node.getAttribute("class") || "";
	      if (value) {
	        re.lastIndex = 0;
	        if (!re.test(c)) node.setAttribute("class", d3_collapse(c + " " + name));
	      } else {
	        node.setAttribute("class", d3_collapse(c.replace(re, " ")));
	      }
	    };
	  }
	  d3_selectionPrototype.style = function(name, value, priority) {
	    var n = arguments.length;
	    if (n < 3) {
	      if (typeof name !== "string") {
	        if (n < 2) value = "";
	        for (priority in name) this.each(d3_selection_style(priority, name[priority], value));
	        return this;
	      }
	      if (n < 2) {
	        var node = this.node();
	        return d3_window(node).getComputedStyle(node, null).getPropertyValue(name);
	      }
	      priority = "";
	    }
	    return this.each(d3_selection_style(name, value, priority));
	  };
	  function d3_selection_style(name, value, priority) {
	    function styleNull() {
	      this.style.removeProperty(name);
	    }
	    function styleConstant() {
	      this.style.setProperty(name, value, priority);
	    }
	    function styleFunction() {
	      var x = value.apply(this, arguments);
	      if (x == null) this.style.removeProperty(name); else this.style.setProperty(name, x, priority);
	    }
	    return value == null ? styleNull : typeof value === "function" ? styleFunction : styleConstant;
	  }
	  d3_selectionPrototype.property = function(name, value) {
	    if (arguments.length < 2) {
	      if (typeof name === "string") return this.node()[name];
	      for (value in name) this.each(d3_selection_property(value, name[value]));
	      return this;
	    }
	    return this.each(d3_selection_property(name, value));
	  };
	  function d3_selection_property(name, value) {
	    function propertyNull() {
	      delete this[name];
	    }
	    function propertyConstant() {
	      this[name] = value;
	    }
	    function propertyFunction() {
	      var x = value.apply(this, arguments);
	      if (x == null) delete this[name]; else this[name] = x;
	    }
	    return value == null ? propertyNull : typeof value === "function" ? propertyFunction : propertyConstant;
	  }
	  d3_selectionPrototype.text = function(value) {
	    return arguments.length ? this.each(typeof value === "function" ? function() {
	      var v = value.apply(this, arguments);
	      this.textContent = v == null ? "" : v;
	    } : value == null ? function() {
	      this.textContent = "";
	    } : function() {
	      this.textContent = value;
	    }) : this.node().textContent;
	  };
	  d3_selectionPrototype.html = function(value) {
	    return arguments.length ? this.each(typeof value === "function" ? function() {
	      var v = value.apply(this, arguments);
	      this.innerHTML = v == null ? "" : v;
	    } : value == null ? function() {
	      this.innerHTML = "";
	    } : function() {
	      this.innerHTML = value;
	    }) : this.node().innerHTML;
	  };
	  d3_selectionPrototype.append = function(name) {
	    name = d3_selection_creator(name);
	    return this.select(function() {
	      return this.appendChild(name.apply(this, arguments));
	    });
	  };
	  function d3_selection_creator(name) {
	    function create() {
	      var document = this.ownerDocument, namespace = this.namespaceURI;
	      return namespace === d3_nsXhtml && document.documentElement.namespaceURI === d3_nsXhtml ? document.createElement(name) : document.createElementNS(namespace, name);
	    }
	    function createNS() {
	      return this.ownerDocument.createElementNS(name.space, name.local);
	    }
	    return typeof name === "function" ? name : (name = d3.ns.qualify(name)).local ? createNS : create;
	  }
	  d3_selectionPrototype.insert = function(name, before) {
	    name = d3_selection_creator(name);
	    before = d3_selection_selector(before);
	    return this.select(function() {
	      return this.insertBefore(name.apply(this, arguments), before.apply(this, arguments) || null);
	    });
	  };
	  d3_selectionPrototype.remove = function() {
	    return this.each(d3_selectionRemove);
	  };
	  function d3_selectionRemove() {
	    var parent = this.parentNode;
	    if (parent) parent.removeChild(this);
	  }
	  d3_selectionPrototype.data = function(value, key) {
	    var i = -1, n = this.length, group, node;
	    if (!arguments.length) {
	      value = new Array(n = (group = this[0]).length);
	      while (++i < n) {
	        if (node = group[i]) {
	          value[i] = node.__data__;
	        }
	      }
	      return value;
	    }
	    function bind(group, groupData) {
	      var i, n = group.length, m = groupData.length, n0 = Math.min(n, m), updateNodes = new Array(m), enterNodes = new Array(m), exitNodes = new Array(n), node, nodeData;
	      if (key) {
	        var nodeByKeyValue = new d3_Map(), keyValues = new Array(n), keyValue;
	        for (i = -1; ++i < n; ) {
	          if (node = group[i]) {
	            if (nodeByKeyValue.has(keyValue = key.call(node, node.__data__, i))) {
	              exitNodes[i] = node;
	            } else {
	              nodeByKeyValue.set(keyValue, node);
	            }
	            keyValues[i] = keyValue;
	          }
	        }
	        for (i = -1; ++i < m; ) {
	          if (!(node = nodeByKeyValue.get(keyValue = key.call(groupData, nodeData = groupData[i], i)))) {
	            enterNodes[i] = d3_selection_dataNode(nodeData);
	          } else if (node !== true) {
	            updateNodes[i] = node;
	            node.__data__ = nodeData;
	          }
	          nodeByKeyValue.set(keyValue, true);
	        }
	        for (i = -1; ++i < n; ) {
	          if (i in keyValues && nodeByKeyValue.get(keyValues[i]) !== true) {
	            exitNodes[i] = group[i];
	          }
	        }
	      } else {
	        for (i = -1; ++i < n0; ) {
	          node = group[i];
	          nodeData = groupData[i];
	          if (node) {
	            node.__data__ = nodeData;
	            updateNodes[i] = node;
	          } else {
	            enterNodes[i] = d3_selection_dataNode(nodeData);
	          }
	        }
	        for (;i < m; ++i) {
	          enterNodes[i] = d3_selection_dataNode(groupData[i]);
	        }
	        for (;i < n; ++i) {
	          exitNodes[i] = group[i];
	        }
	      }
	      enterNodes.update = updateNodes;
	      enterNodes.parentNode = updateNodes.parentNode = exitNodes.parentNode = group.parentNode;
	      enter.push(enterNodes);
	      update.push(updateNodes);
	      exit.push(exitNodes);
	    }
	    var enter = d3_selection_enter([]), update = d3_selection([]), exit = d3_selection([]);
	    if (typeof value === "function") {
	      while (++i < n) {
	        bind(group = this[i], value.call(group, group.parentNode.__data__, i));
	      }
	    } else {
	      while (++i < n) {
	        bind(group = this[i], value);
	      }
	    }
	    update.enter = function() {
	      return enter;
	    };
	    update.exit = function() {
	      return exit;
	    };
	    return update;
	  };
	  function d3_selection_dataNode(data) {
	    return {
	      __data__: data
	    };
	  }
	  d3_selectionPrototype.datum = function(value) {
	    return arguments.length ? this.property("__data__", value) : this.property("__data__");
	  };
	  d3_selectionPrototype.filter = function(filter) {
	    var subgroups = [], subgroup, group, node;
	    if (typeof filter !== "function") filter = d3_selection_filter(filter);
	    for (var j = 0, m = this.length; j < m; j++) {
	      subgroups.push(subgroup = []);
	      subgroup.parentNode = (group = this[j]).parentNode;
	      for (var i = 0, n = group.length; i < n; i++) {
	        if ((node = group[i]) && filter.call(node, node.__data__, i, j)) {
	          subgroup.push(node);
	        }
	      }
	    }
	    return d3_selection(subgroups);
	  };
	  function d3_selection_filter(selector) {
	    return function() {
	      return d3_selectMatches(this, selector);
	    };
	  }
	  d3_selectionPrototype.order = function() {
	    for (var j = -1, m = this.length; ++j < m; ) {
	      for (var group = this[j], i = group.length - 1, next = group[i], node; --i >= 0; ) {
	        if (node = group[i]) {
	          if (next && next !== node.nextSibling) next.parentNode.insertBefore(node, next);
	          next = node;
	        }
	      }
	    }
	    return this;
	  };
	  d3_selectionPrototype.sort = function(comparator) {
	    comparator = d3_selection_sortComparator.apply(this, arguments);
	    for (var j = -1, m = this.length; ++j < m; ) this[j].sort(comparator);
	    return this.order();
	  };
	  function d3_selection_sortComparator(comparator) {
	    if (!arguments.length) comparator = d3_ascending;
	    return function(a, b) {
	      return a && b ? comparator(a.__data__, b.__data__) : !a - !b;
	    };
	  }
	  d3_selectionPrototype.each = function(callback) {
	    return d3_selection_each(this, function(node, i, j) {
	      callback.call(node, node.__data__, i, j);
	    });
	  };
	  function d3_selection_each(groups, callback) {
	    for (var j = 0, m = groups.length; j < m; j++) {
	      for (var group = groups[j], i = 0, n = group.length, node; i < n; i++) {
	        if (node = group[i]) callback(node, i, j);
	      }
	    }
	    return groups;
	  }
	  d3_selectionPrototype.call = function(callback) {
	    var args = d3_array(arguments);
	    callback.apply(args[0] = this, args);
	    return this;
	  };
	  d3_selectionPrototype.empty = function() {
	    return !this.node();
	  };
	  d3_selectionPrototype.node = function() {
	    for (var j = 0, m = this.length; j < m; j++) {
	      for (var group = this[j], i = 0, n = group.length; i < n; i++) {
	        var node = group[i];
	        if (node) return node;
	      }
	    }
	    return null;
	  };
	  d3_selectionPrototype.size = function() {
	    var n = 0;
	    d3_selection_each(this, function() {
	      ++n;
	    });
	    return n;
	  };
	  function d3_selection_enter(selection) {
	    d3_subclass(selection, d3_selection_enterPrototype);
	    return selection;
	  }
	  var d3_selection_enterPrototype = [];
	  d3.selection.enter = d3_selection_enter;
	  d3.selection.enter.prototype = d3_selection_enterPrototype;
	  d3_selection_enterPrototype.append = d3_selectionPrototype.append;
	  d3_selection_enterPrototype.empty = d3_selectionPrototype.empty;
	  d3_selection_enterPrototype.node = d3_selectionPrototype.node;
	  d3_selection_enterPrototype.call = d3_selectionPrototype.call;
	  d3_selection_enterPrototype.size = d3_selectionPrototype.size;
	  d3_selection_enterPrototype.select = function(selector) {
	    var subgroups = [], subgroup, subnode, upgroup, group, node;
	    for (var j = -1, m = this.length; ++j < m; ) {
	      upgroup = (group = this[j]).update;
	      subgroups.push(subgroup = []);
	      subgroup.parentNode = group.parentNode;
	      for (var i = -1, n = group.length; ++i < n; ) {
	        if (node = group[i]) {
	          subgroup.push(upgroup[i] = subnode = selector.call(group.parentNode, node.__data__, i, j));
	          subnode.__data__ = node.__data__;
	        } else {
	          subgroup.push(null);
	        }
	      }
	    }
	    return d3_selection(subgroups);
	  };
	  d3_selection_enterPrototype.insert = function(name, before) {
	    if (arguments.length < 2) before = d3_selection_enterInsertBefore(this);
	    return d3_selectionPrototype.insert.call(this, name, before);
	  };
	  function d3_selection_enterInsertBefore(enter) {
	    var i0, j0;
	    return function(d, i, j) {
	      var group = enter[j].update, n = group.length, node;
	      if (j != j0) j0 = j, i0 = 0;
	      if (i >= i0) i0 = i + 1;
	      while (!(node = group[i0]) && ++i0 < n) ;
	      return node;
	    };
	  }
	  d3.select = function(node) {
	    var group;
	    if (typeof node === "string") {
	      group = [ d3_select(node, d3_document) ];
	      group.parentNode = d3_document.documentElement;
	    } else {
	      group = [ node ];
	      group.parentNode = d3_documentElement(node);
	    }
	    return d3_selection([ group ]);
	  };
	  d3.selectAll = function(nodes) {
	    var group;
	    if (typeof nodes === "string") {
	      group = d3_array(d3_selectAll(nodes, d3_document));
	      group.parentNode = d3_document.documentElement;
	    } else {
	      group = d3_array(nodes);
	      group.parentNode = null;
	    }
	    return d3_selection([ group ]);
	  };
	  d3_selectionPrototype.on = function(type, listener, capture) {
	    var n = arguments.length;
	    if (n < 3) {
	      if (typeof type !== "string") {
	        if (n < 2) listener = false;
	        for (capture in type) this.each(d3_selection_on(capture, type[capture], listener));
	        return this;
	      }
	      if (n < 2) return (n = this.node()["__on" + type]) && n._;
	      capture = false;
	    }
	    return this.each(d3_selection_on(type, listener, capture));
	  };
	  function d3_selection_on(type, listener, capture) {
	    var name = "__on" + type, i = type.indexOf("."), wrap = d3_selection_onListener;
	    if (i > 0) type = type.slice(0, i);
	    var filter = d3_selection_onFilters.get(type);
	    if (filter) type = filter, wrap = d3_selection_onFilter;
	    function onRemove() {
	      var l = this[name];
	      if (l) {
	        this.removeEventListener(type, l, l.$);
	        delete this[name];
	      }
	    }
	    function onAdd() {
	      var l = wrap(listener, d3_array(arguments));
	      onRemove.call(this);
	      this.addEventListener(type, this[name] = l, l.$ = capture);
	      l._ = listener;
	    }
	    function removeAll() {
	      var re = new RegExp("^__on([^.]+)" + d3.requote(type) + "$"), match;
	      for (var name in this) {
	        if (match = name.match(re)) {
	          var l = this[name];
	          this.removeEventListener(match[1], l, l.$);
	          delete this[name];
	        }
	      }
	    }
	    return i ? listener ? onAdd : onRemove : listener ? d3_noop : removeAll;
	  }
	  var d3_selection_onFilters = d3.map({
	    mouseenter: "mouseover",
	    mouseleave: "mouseout"
	  });
	  if (d3_document) {
	    d3_selection_onFilters.forEach(function(k) {
	      if ("on" + k in d3_document) d3_selection_onFilters.remove(k);
	    });
	  }
	  function d3_selection_onListener(listener, argumentz) {
	    return function(e) {
	      var o = d3.event;
	      d3.event = e;
	      argumentz[0] = this.__data__;
	      try {
	        listener.apply(this, argumentz);
	      } finally {
	        d3.event = o;
	      }
	    };
	  }
	  function d3_selection_onFilter(listener, argumentz) {
	    var l = d3_selection_onListener(listener, argumentz);
	    return function(e) {
	      var target = this, related = e.relatedTarget;
	      if (!related || related !== target && !(related.compareDocumentPosition(target) & 8)) {
	        l.call(target, e);
	      }
	    };
	  }
	  var d3_event_dragSelect, d3_event_dragId = 0;
	  function d3_event_dragSuppress(node) {
	    var name = ".dragsuppress-" + ++d3_event_dragId, click = "click" + name, w = d3.select(d3_window(node)).on("touchmove" + name, d3_eventPreventDefault).on("dragstart" + name, d3_eventPreventDefault).on("selectstart" + name, d3_eventPreventDefault);
	    if (d3_event_dragSelect == null) {
	      d3_event_dragSelect = "onselectstart" in node ? false : d3_vendorSymbol(node.style, "userSelect");
	    }
	    if (d3_event_dragSelect) {
	      var style = d3_documentElement(node).style, select = style[d3_event_dragSelect];
	      style[d3_event_dragSelect] = "none";
	    }
	    return function(suppressClick) {
	      w.on(name, null);
	      if (d3_event_dragSelect) style[d3_event_dragSelect] = select;
	      if (suppressClick) {
	        var off = function() {
	          w.on(click, null);
	        };
	        w.on(click, function() {
	          d3_eventPreventDefault();
	          off();
	        }, true);
	        setTimeout(off, 0);
	      }
	    };
	  }
	  d3.mouse = function(container) {
	    return d3_mousePoint(container, d3_eventSource());
	  };
	  var d3_mouse_bug44083 = this.navigator && /WebKit/.test(this.navigator.userAgent) ? -1 : 0;
	  function d3_mousePoint(container, e) {
	    if (e.changedTouches) e = e.changedTouches[0];
	    var svg = container.ownerSVGElement || container;
	    if (svg.createSVGPoint) {
	      var point = svg.createSVGPoint();
	      if (d3_mouse_bug44083 < 0) {
	        var window = d3_window(container);
	        if (window.scrollX || window.scrollY) {
	          svg = d3.select("body").append("svg").style({
	            position: "absolute",
	            top: 0,
	            left: 0,
	            margin: 0,
	            padding: 0,
	            border: "none"
	          }, "important");
	          var ctm = svg[0][0].getScreenCTM();
	          d3_mouse_bug44083 = !(ctm.f || ctm.e);
	          svg.remove();
	        }
	      }
	      if (d3_mouse_bug44083) point.x = e.pageX, point.y = e.pageY; else point.x = e.clientX, 
	      point.y = e.clientY;
	      point = point.matrixTransform(container.getScreenCTM().inverse());
	      return [ point.x, point.y ];
	    }
	    var rect = container.getBoundingClientRect();
	    return [ e.clientX - rect.left - container.clientLeft, e.clientY - rect.top - container.clientTop ];
	  }
	  d3.touch = function(container, touches, identifier) {
	    if (arguments.length < 3) identifier = touches, touches = d3_eventSource().changedTouches;
	    if (touches) for (var i = 0, n = touches.length, touch; i < n; ++i) {
	      if ((touch = touches[i]).identifier === identifier) {
	        return d3_mousePoint(container, touch);
	      }
	    }
	  };
	  d3.behavior.drag = function() {
	    var event = d3_eventDispatch(drag, "drag", "dragstart", "dragend"), origin = null, mousedown = dragstart(d3_noop, d3.mouse, d3_window, "mousemove", "mouseup"), touchstart = dragstart(d3_behavior_dragTouchId, d3.touch, d3_identity, "touchmove", "touchend");
	    function drag() {
	      this.on("mousedown.drag", mousedown).on("touchstart.drag", touchstart);
	    }
	    function dragstart(id, position, subject, move, end) {
	      return function() {
	        var that = this, target = d3.event.target.correspondingElement || d3.event.target, parent = that.parentNode, dispatch = event.of(that, arguments), dragged = 0, dragId = id(), dragName = ".drag" + (dragId == null ? "" : "-" + dragId), dragOffset, dragSubject = d3.select(subject(target)).on(move + dragName, moved).on(end + dragName, ended), dragRestore = d3_event_dragSuppress(target), position0 = position(parent, dragId);
	        if (origin) {
	          dragOffset = origin.apply(that, arguments);
	          dragOffset = [ dragOffset.x - position0[0], dragOffset.y - position0[1] ];
	        } else {
	          dragOffset = [ 0, 0 ];
	        }
	        dispatch({
	          type: "dragstart"
	        });
	        function moved() {
	          var position1 = position(parent, dragId), dx, dy;
	          if (!position1) return;
	          dx = position1[0] - position0[0];
	          dy = position1[1] - position0[1];
	          dragged |= dx | dy;
	          position0 = position1;
	          dispatch({
	            type: "drag",
	            x: position1[0] + dragOffset[0],
	            y: position1[1] + dragOffset[1],
	            dx: dx,
	            dy: dy
	          });
	        }
	        function ended() {
	          if (!position(parent, dragId)) return;
	          dragSubject.on(move + dragName, null).on(end + dragName, null);
	          dragRestore(dragged);
	          dispatch({
	            type: "dragend"
	          });
	        }
	      };
	    }
	    drag.origin = function(x) {
	      if (!arguments.length) return origin;
	      origin = x;
	      return drag;
	    };
	    return d3.rebind(drag, event, "on");
	  };
	  function d3_behavior_dragTouchId() {
	    return d3.event.changedTouches[0].identifier;
	  }
	  d3.touches = function(container, touches) {
	    if (arguments.length < 2) touches = d3_eventSource().touches;
	    return touches ? d3_array(touches).map(function(touch) {
	      var point = d3_mousePoint(container, touch);
	      point.identifier = touch.identifier;
	      return point;
	    }) : [];
	  };
	  var ε = 1e-6, ε2 = ε * ε, π = Math.PI, τ = 2 * π, τε = τ - ε, halfπ = π / 2, d3_radians = π / 180, d3_degrees = 180 / π;
	  function d3_sgn(x) {
	    return x > 0 ? 1 : x < 0 ? -1 : 0;
	  }
	  function d3_cross2d(a, b, c) {
	    return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
	  }
	  function d3_acos(x) {
	    return x > 1 ? 0 : x < -1 ? π : Math.acos(x);
	  }
	  function d3_asin(x) {
	    return x > 1 ? halfπ : x < -1 ? -halfπ : Math.asin(x);
	  }
	  function d3_sinh(x) {
	    return ((x = Math.exp(x)) - 1 / x) / 2;
	  }
	  function d3_cosh(x) {
	    return ((x = Math.exp(x)) + 1 / x) / 2;
	  }
	  function d3_tanh(x) {
	    return ((x = Math.exp(2 * x)) - 1) / (x + 1);
	  }
	  function d3_haversin(x) {
	    return (x = Math.sin(x / 2)) * x;
	  }
	  var ρ = Math.SQRT2, ρ2 = 2, ρ4 = 4;
	  d3.interpolateZoom = function(p0, p1) {
	    var ux0 = p0[0], uy0 = p0[1], w0 = p0[2], ux1 = p1[0], uy1 = p1[1], w1 = p1[2], dx = ux1 - ux0, dy = uy1 - uy0, d2 = dx * dx + dy * dy, i, S;
	    if (d2 < ε2) {
	      S = Math.log(w1 / w0) / ρ;
	      i = function(t) {
	        return [ ux0 + t * dx, uy0 + t * dy, w0 * Math.exp(ρ * t * S) ];
	      };
	    } else {
	      var d1 = Math.sqrt(d2), b0 = (w1 * w1 - w0 * w0 + ρ4 * d2) / (2 * w0 * ρ2 * d1), b1 = (w1 * w1 - w0 * w0 - ρ4 * d2) / (2 * w1 * ρ2 * d1), r0 = Math.log(Math.sqrt(b0 * b0 + 1) - b0), r1 = Math.log(Math.sqrt(b1 * b1 + 1) - b1);
	      S = (r1 - r0) / ρ;
	      i = function(t) {
	        var s = t * S, coshr0 = d3_cosh(r0), u = w0 / (ρ2 * d1) * (coshr0 * d3_tanh(ρ * s + r0) - d3_sinh(r0));
	        return [ ux0 + u * dx, uy0 + u * dy, w0 * coshr0 / d3_cosh(ρ * s + r0) ];
	      };
	    }
	    i.duration = S * 1e3;
	    return i;
	  };
	  d3.behavior.zoom = function() {
	    var view = {
	      x: 0,
	      y: 0,
	      k: 1
	    }, translate0, center0, center, size = [ 960, 500 ], scaleExtent = d3_behavior_zoomInfinity, duration = 250, zooming = 0, mousedown = "mousedown.zoom", mousemove = "mousemove.zoom", mouseup = "mouseup.zoom", mousewheelTimer, touchstart = "touchstart.zoom", touchtime, event = d3_eventDispatch(zoom, "zoomstart", "zoom", "zoomend"), x0, x1, y0, y1;
	    if (!d3_behavior_zoomWheel) {
	      d3_behavior_zoomWheel = "onwheel" in d3_document ? (d3_behavior_zoomDelta = function() {
	        return -d3.event.deltaY * (d3.event.deltaMode ? 120 : 1);
	      }, "wheel") : "onmousewheel" in d3_document ? (d3_behavior_zoomDelta = function() {
	        return d3.event.wheelDelta;
	      }, "mousewheel") : (d3_behavior_zoomDelta = function() {
	        return -d3.event.detail;
	      }, "MozMousePixelScroll");
	    }
	    function zoom(g) {
	      g.on(mousedown, mousedowned).on(d3_behavior_zoomWheel + ".zoom", mousewheeled).on("dblclick.zoom", dblclicked).on(touchstart, touchstarted);
	    }
	    zoom.event = function(g) {
	      g.each(function() {
	        var dispatch = event.of(this, arguments), view1 = view;
	        if (d3_transitionInheritId) {
	          d3.select(this).transition().each("start.zoom", function() {
	            view = this.__chart__ || {
	              x: 0,
	              y: 0,
	              k: 1
	            };
	            zoomstarted(dispatch);
	          }).tween("zoom:zoom", function() {
	            var dx = size[0], dy = size[1], cx = center0 ? center0[0] : dx / 2, cy = center0 ? center0[1] : dy / 2, i = d3.interpolateZoom([ (cx - view.x) / view.k, (cy - view.y) / view.k, dx / view.k ], [ (cx - view1.x) / view1.k, (cy - view1.y) / view1.k, dx / view1.k ]);
	            return function(t) {
	              var l = i(t), k = dx / l[2];
	              this.__chart__ = view = {
	                x: cx - l[0] * k,
	                y: cy - l[1] * k,
	                k: k
	              };
	              zoomed(dispatch);
	            };
	          }).each("interrupt.zoom", function() {
	            zoomended(dispatch);
	          }).each("end.zoom", function() {
	            zoomended(dispatch);
	          });
	        } else {
	          this.__chart__ = view;
	          zoomstarted(dispatch);
	          zoomed(dispatch);
	          zoomended(dispatch);
	        }
	      });
	    };
	    zoom.translate = function(_) {
	      if (!arguments.length) return [ view.x, view.y ];
	      view = {
	        x: +_[0],
	        y: +_[1],
	        k: view.k
	      };
	      rescale();
	      return zoom;
	    };
	    zoom.scale = function(_) {
	      if (!arguments.length) return view.k;
	      view = {
	        x: view.x,
	        y: view.y,
	        k: null
	      };
	      scaleTo(+_);
	      rescale();
	      return zoom;
	    };
	    zoom.scaleExtent = function(_) {
	      if (!arguments.length) return scaleExtent;
	      scaleExtent = _ == null ? d3_behavior_zoomInfinity : [ +_[0], +_[1] ];
	      return zoom;
	    };
	    zoom.center = function(_) {
	      if (!arguments.length) return center;
	      center = _ && [ +_[0], +_[1] ];
	      return zoom;
	    };
	    zoom.size = function(_) {
	      if (!arguments.length) return size;
	      size = _ && [ +_[0], +_[1] ];
	      return zoom;
	    };
	    zoom.duration = function(_) {
	      if (!arguments.length) return duration;
	      duration = +_;
	      return zoom;
	    };
	    zoom.x = function(z) {
	      if (!arguments.length) return x1;
	      x1 = z;
	      x0 = z.copy();
	      view = {
	        x: 0,
	        y: 0,
	        k: 1
	      };
	      return zoom;
	    };
	    zoom.y = function(z) {
	      if (!arguments.length) return y1;
	      y1 = z;
	      y0 = z.copy();
	      view = {
	        x: 0,
	        y: 0,
	        k: 1
	      };
	      return zoom;
	    };
	    function location(p) {
	      return [ (p[0] - view.x) / view.k, (p[1] - view.y) / view.k ];
	    }
	    function point(l) {
	      return [ l[0] * view.k + view.x, l[1] * view.k + view.y ];
	    }
	    function scaleTo(s) {
	      view.k = Math.max(scaleExtent[0], Math.min(scaleExtent[1], s));
	    }
	    function translateTo(p, l) {
	      l = point(l);
	      view.x += p[0] - l[0];
	      view.y += p[1] - l[1];
	    }
	    function zoomTo(that, p, l, k) {
	      that.__chart__ = {
	        x: view.x,
	        y: view.y,
	        k: view.k
	      };
	      scaleTo(Math.pow(2, k));
	      translateTo(center0 = p, l);
	      that = d3.select(that);
	      if (duration > 0) that = that.transition().duration(duration);
	      that.call(zoom.event);
	    }
	    function rescale() {
	      if (x1) x1.domain(x0.range().map(function(x) {
	        return (x - view.x) / view.k;
	      }).map(x0.invert));
	      if (y1) y1.domain(y0.range().map(function(y) {
	        return (y - view.y) / view.k;
	      }).map(y0.invert));
	    }
	    function zoomstarted(dispatch) {
	      if (!zooming++) dispatch({
	        type: "zoomstart"
	      });
	    }
	    function zoomed(dispatch) {
	      rescale();
	      dispatch({
	        type: "zoom",
	        scale: view.k,
	        translate: [ view.x, view.y ]
	      });
	    }
	    function zoomended(dispatch) {
	      if (!--zooming) dispatch({
	        type: "zoomend"
	      }), center0 = null;
	    }
	    function mousedowned() {
	      var that = this, dispatch = event.of(that, arguments), dragged = 0, subject = d3.select(d3_window(that)).on(mousemove, moved).on(mouseup, ended), location0 = location(d3.mouse(that)), dragRestore = d3_event_dragSuppress(that);
	      d3_selection_interrupt.call(that);
	      zoomstarted(dispatch);
	      function moved() {
	        dragged = 1;
	        translateTo(d3.mouse(that), location0);
	        zoomed(dispatch);
	      }
	      function ended() {
	        subject.on(mousemove, null).on(mouseup, null);
	        dragRestore(dragged);
	        zoomended(dispatch);
	      }
	    }
	    function touchstarted() {
	      var that = this, dispatch = event.of(that, arguments), locations0 = {}, distance0 = 0, scale0, zoomName = ".zoom-" + d3.event.changedTouches[0].identifier, touchmove = "touchmove" + zoomName, touchend = "touchend" + zoomName, targets = [], subject = d3.select(that), dragRestore = d3_event_dragSuppress(that);
	      started();
	      zoomstarted(dispatch);
	      subject.on(mousedown, null).on(touchstart, started);
	      function relocate() {
	        var touches = d3.touches(that);
	        scale0 = view.k;
	        touches.forEach(function(t) {
	          if (t.identifier in locations0) locations0[t.identifier] = location(t);
	        });
	        return touches;
	      }
	      function started() {
	        var target = d3.event.target;
	        d3.select(target).on(touchmove, moved).on(touchend, ended);
	        targets.push(target);
	        var changed = d3.event.changedTouches;
	        for (var i = 0, n = changed.length; i < n; ++i) {
	          locations0[changed[i].identifier] = null;
	        }
	        var touches = relocate(), now = Date.now();
	        if (touches.length === 1) {
	          if (now - touchtime < 500) {
	            var p = touches[0];
	            zoomTo(that, p, locations0[p.identifier], Math.floor(Math.log(view.k) / Math.LN2) + 1);
	            d3_eventPreventDefault();
	          }
	          touchtime = now;
	        } else if (touches.length > 1) {
	          var p = touches[0], q = touches[1], dx = p[0] - q[0], dy = p[1] - q[1];
	          distance0 = dx * dx + dy * dy;
	        }
	      }
	      function moved() {
	        var touches = d3.touches(that), p0, l0, p1, l1;
	        d3_selection_interrupt.call(that);
	        for (var i = 0, n = touches.length; i < n; ++i, l1 = null) {
	          p1 = touches[i];
	          if (l1 = locations0[p1.identifier]) {
	            if (l0) break;
	            p0 = p1, l0 = l1;
	          }
	        }
	        if (l1) {
	          var distance1 = (distance1 = p1[0] - p0[0]) * distance1 + (distance1 = p1[1] - p0[1]) * distance1, scale1 = distance0 && Math.sqrt(distance1 / distance0);
	          p0 = [ (p0[0] + p1[0]) / 2, (p0[1] + p1[1]) / 2 ];
	          l0 = [ (l0[0] + l1[0]) / 2, (l0[1] + l1[1]) / 2 ];
	          scaleTo(scale1 * scale0);
	        }
	        touchtime = null;
	        translateTo(p0, l0);
	        zoomed(dispatch);
	      }
	      function ended() {
	        if (d3.event.touches.length) {
	          var changed = d3.event.changedTouches;
	          for (var i = 0, n = changed.length; i < n; ++i) {
	            delete locations0[changed[i].identifier];
	          }
	          for (var identifier in locations0) {
	            return void relocate();
	          }
	        }
	        d3.selectAll(targets).on(zoomName, null);
	        subject.on(mousedown, mousedowned).on(touchstart, touchstarted);
	        dragRestore();
	        zoomended(dispatch);
	      }
	    }
	    function mousewheeled() {
	      var dispatch = event.of(this, arguments);
	      if (mousewheelTimer) clearTimeout(mousewheelTimer); else d3_selection_interrupt.call(this), 
	      translate0 = location(center0 = center || d3.mouse(this)), zoomstarted(dispatch);
	      mousewheelTimer = setTimeout(function() {
	        mousewheelTimer = null;
	        zoomended(dispatch);
	      }, 50);
	      d3_eventPreventDefault();
	      scaleTo(Math.pow(2, d3_behavior_zoomDelta() * .002) * view.k);
	      translateTo(center0, translate0);
	      zoomed(dispatch);
	    }
	    function dblclicked() {
	      var p = d3.mouse(this), k = Math.log(view.k) / Math.LN2;
	      zoomTo(this, p, location(p), d3.event.shiftKey ? Math.ceil(k) - 1 : Math.floor(k) + 1);
	    }
	    return d3.rebind(zoom, event, "on");
	  };
	  var d3_behavior_zoomInfinity = [ 0, Infinity ], d3_behavior_zoomDelta, d3_behavior_zoomWheel;
	  d3.color = d3_color;
	  function d3_color() {}
	  d3_color.prototype.toString = function() {
	    return this.rgb() + "";
	  };
	  d3.hsl = d3_hsl;
	  function d3_hsl(h, s, l) {
	    return this instanceof d3_hsl ? void (this.h = +h, this.s = +s, this.l = +l) : arguments.length < 2 ? h instanceof d3_hsl ? new d3_hsl(h.h, h.s, h.l) : d3_rgb_parse("" + h, d3_rgb_hsl, d3_hsl) : new d3_hsl(h, s, l);
	  }
	  var d3_hslPrototype = d3_hsl.prototype = new d3_color();
	  d3_hslPrototype.brighter = function(k) {
	    k = Math.pow(.7, arguments.length ? k : 1);
	    return new d3_hsl(this.h, this.s, this.l / k);
	  };
	  d3_hslPrototype.darker = function(k) {
	    k = Math.pow(.7, arguments.length ? k : 1);
	    return new d3_hsl(this.h, this.s, k * this.l);
	  };
	  d3_hslPrototype.rgb = function() {
	    return d3_hsl_rgb(this.h, this.s, this.l);
	  };
	  function d3_hsl_rgb(h, s, l) {
	    var m1, m2;
	    h = isNaN(h) ? 0 : (h %= 360) < 0 ? h + 360 : h;
	    s = isNaN(s) ? 0 : s < 0 ? 0 : s > 1 ? 1 : s;
	    l = l < 0 ? 0 : l > 1 ? 1 : l;
	    m2 = l <= .5 ? l * (1 + s) : l + s - l * s;
	    m1 = 2 * l - m2;
	    function v(h) {
	      if (h > 360) h -= 360; else if (h < 0) h += 360;
	      if (h < 60) return m1 + (m2 - m1) * h / 60;
	      if (h < 180) return m2;
	      if (h < 240) return m1 + (m2 - m1) * (240 - h) / 60;
	      return m1;
	    }
	    function vv(h) {
	      return Math.round(v(h) * 255);
	    }
	    return new d3_rgb(vv(h + 120), vv(h), vv(h - 120));
	  }
	  d3.hcl = d3_hcl;
	  function d3_hcl(h, c, l) {
	    return this instanceof d3_hcl ? void (this.h = +h, this.c = +c, this.l = +l) : arguments.length < 2 ? h instanceof d3_hcl ? new d3_hcl(h.h, h.c, h.l) : h instanceof d3_lab ? d3_lab_hcl(h.l, h.a, h.b) : d3_lab_hcl((h = d3_rgb_lab((h = d3.rgb(h)).r, h.g, h.b)).l, h.a, h.b) : new d3_hcl(h, c, l);
	  }
	  var d3_hclPrototype = d3_hcl.prototype = new d3_color();
	  d3_hclPrototype.brighter = function(k) {
	    return new d3_hcl(this.h, this.c, Math.min(100, this.l + d3_lab_K * (arguments.length ? k : 1)));
	  };
	  d3_hclPrototype.darker = function(k) {
	    return new d3_hcl(this.h, this.c, Math.max(0, this.l - d3_lab_K * (arguments.length ? k : 1)));
	  };
	  d3_hclPrototype.rgb = function() {
	    return d3_hcl_lab(this.h, this.c, this.l).rgb();
	  };
	  function d3_hcl_lab(h, c, l) {
	    if (isNaN(h)) h = 0;
	    if (isNaN(c)) c = 0;
	    return new d3_lab(l, Math.cos(h *= d3_radians) * c, Math.sin(h) * c);
	  }
	  d3.lab = d3_lab;
	  function d3_lab(l, a, b) {
	    return this instanceof d3_lab ? void (this.l = +l, this.a = +a, this.b = +b) : arguments.length < 2 ? l instanceof d3_lab ? new d3_lab(l.l, l.a, l.b) : l instanceof d3_hcl ? d3_hcl_lab(l.h, l.c, l.l) : d3_rgb_lab((l = d3_rgb(l)).r, l.g, l.b) : new d3_lab(l, a, b);
	  }
	  var d3_lab_K = 18;
	  var d3_lab_X = .95047, d3_lab_Y = 1, d3_lab_Z = 1.08883;
	  var d3_labPrototype = d3_lab.prototype = new d3_color();
	  d3_labPrototype.brighter = function(k) {
	    return new d3_lab(Math.min(100, this.l + d3_lab_K * (arguments.length ? k : 1)), this.a, this.b);
	  };
	  d3_labPrototype.darker = function(k) {
	    return new d3_lab(Math.max(0, this.l - d3_lab_K * (arguments.length ? k : 1)), this.a, this.b);
	  };
	  d3_labPrototype.rgb = function() {
	    return d3_lab_rgb(this.l, this.a, this.b);
	  };
	  function d3_lab_rgb(l, a, b) {
	    var y = (l + 16) / 116, x = y + a / 500, z = y - b / 200;
	    x = d3_lab_xyz(x) * d3_lab_X;
	    y = d3_lab_xyz(y) * d3_lab_Y;
	    z = d3_lab_xyz(z) * d3_lab_Z;
	    return new d3_rgb(d3_xyz_rgb(3.2404542 * x - 1.5371385 * y - .4985314 * z), d3_xyz_rgb(-.969266 * x + 1.8760108 * y + .041556 * z), d3_xyz_rgb(.0556434 * x - .2040259 * y + 1.0572252 * z));
	  }
	  function d3_lab_hcl(l, a, b) {
	    return l > 0 ? new d3_hcl(Math.atan2(b, a) * d3_degrees, Math.sqrt(a * a + b * b), l) : new d3_hcl(NaN, NaN, l);
	  }
	  function d3_lab_xyz(x) {
	    return x > .206893034 ? x * x * x : (x - 4 / 29) / 7.787037;
	  }
	  function d3_xyz_lab(x) {
	    return x > .008856 ? Math.pow(x, 1 / 3) : 7.787037 * x + 4 / 29;
	  }
	  function d3_xyz_rgb(r) {
	    return Math.round(255 * (r <= .00304 ? 12.92 * r : 1.055 * Math.pow(r, 1 / 2.4) - .055));
	  }
	  d3.rgb = d3_rgb;
	  function d3_rgb(r, g, b) {
	    return this instanceof d3_rgb ? void (this.r = ~~r, this.g = ~~g, this.b = ~~b) : arguments.length < 2 ? r instanceof d3_rgb ? new d3_rgb(r.r, r.g, r.b) : d3_rgb_parse("" + r, d3_rgb, d3_hsl_rgb) : new d3_rgb(r, g, b);
	  }
	  function d3_rgbNumber(value) {
	    return new d3_rgb(value >> 16, value >> 8 & 255, value & 255);
	  }
	  function d3_rgbString(value) {
	    return d3_rgbNumber(value) + "";
	  }
	  var d3_rgbPrototype = d3_rgb.prototype = new d3_color();
	  d3_rgbPrototype.brighter = function(k) {
	    k = Math.pow(.7, arguments.length ? k : 1);
	    var r = this.r, g = this.g, b = this.b, i = 30;
	    if (!r && !g && !b) return new d3_rgb(i, i, i);
	    if (r && r < i) r = i;
	    if (g && g < i) g = i;
	    if (b && b < i) b = i;
	    return new d3_rgb(Math.min(255, r / k), Math.min(255, g / k), Math.min(255, b / k));
	  };
	  d3_rgbPrototype.darker = function(k) {
	    k = Math.pow(.7, arguments.length ? k : 1);
	    return new d3_rgb(k * this.r, k * this.g, k * this.b);
	  };
	  d3_rgbPrototype.hsl = function() {
	    return d3_rgb_hsl(this.r, this.g, this.b);
	  };
	  d3_rgbPrototype.toString = function() {
	    return "#" + d3_rgb_hex(this.r) + d3_rgb_hex(this.g) + d3_rgb_hex(this.b);
	  };
	  function d3_rgb_hex(v) {
	    return v < 16 ? "0" + Math.max(0, v).toString(16) : Math.min(255, v).toString(16);
	  }
	  function d3_rgb_parse(format, rgb, hsl) {
	    var r = 0, g = 0, b = 0, m1, m2, color;
	    m1 = /([a-z]+)\((.*)\)/.exec(format = format.toLowerCase());
	    if (m1) {
	      m2 = m1[2].split(",");
	      switch (m1[1]) {
	       case "hsl":
	        {
	          return hsl(parseFloat(m2[0]), parseFloat(m2[1]) / 100, parseFloat(m2[2]) / 100);
	        }

	       case "rgb":
	        {
	          return rgb(d3_rgb_parseNumber(m2[0]), d3_rgb_parseNumber(m2[1]), d3_rgb_parseNumber(m2[2]));
	        }
	      }
	    }
	    if (color = d3_rgb_names.get(format)) {
	      return rgb(color.r, color.g, color.b);
	    }
	    if (format != null && format.charAt(0) === "#" && !isNaN(color = parseInt(format.slice(1), 16))) {
	      if (format.length === 4) {
	        r = (color & 3840) >> 4;
	        r = r >> 4 | r;
	        g = color & 240;
	        g = g >> 4 | g;
	        b = color & 15;
	        b = b << 4 | b;
	      } else if (format.length === 7) {
	        r = (color & 16711680) >> 16;
	        g = (color & 65280) >> 8;
	        b = color & 255;
	      }
	    }
	    return rgb(r, g, b);
	  }
	  function d3_rgb_hsl(r, g, b) {
	    var min = Math.min(r /= 255, g /= 255, b /= 255), max = Math.max(r, g, b), d = max - min, h, s, l = (max + min) / 2;
	    if (d) {
	      s = l < .5 ? d / (max + min) : d / (2 - max - min);
	      if (r == max) h = (g - b) / d + (g < b ? 6 : 0); else if (g == max) h = (b - r) / d + 2; else h = (r - g) / d + 4;
	      h *= 60;
	    } else {
	      h = NaN;
	      s = l > 0 && l < 1 ? 0 : h;
	    }
	    return new d3_hsl(h, s, l);
	  }
	  function d3_rgb_lab(r, g, b) {
	    r = d3_rgb_xyz(r);
	    g = d3_rgb_xyz(g);
	    b = d3_rgb_xyz(b);
	    var x = d3_xyz_lab((.4124564 * r + .3575761 * g + .1804375 * b) / d3_lab_X), y = d3_xyz_lab((.2126729 * r + .7151522 * g + .072175 * b) / d3_lab_Y), z = d3_xyz_lab((.0193339 * r + .119192 * g + .9503041 * b) / d3_lab_Z);
	    return d3_lab(116 * y - 16, 500 * (x - y), 200 * (y - z));
	  }
	  function d3_rgb_xyz(r) {
	    return (r /= 255) <= .04045 ? r / 12.92 : Math.pow((r + .055) / 1.055, 2.4);
	  }
	  function d3_rgb_parseNumber(c) {
	    var f = parseFloat(c);
	    return c.charAt(c.length - 1) === "%" ? Math.round(f * 2.55) : f;
	  }
	  var d3_rgb_names = d3.map({
	    aliceblue: 15792383,
	    antiquewhite: 16444375,
	    aqua: 65535,
	    aquamarine: 8388564,
	    azure: 15794175,
	    beige: 16119260,
	    bisque: 16770244,
	    black: 0,
	    blanchedalmond: 16772045,
	    blue: 255,
	    blueviolet: 9055202,
	    brown: 10824234,
	    burlywood: 14596231,
	    cadetblue: 6266528,
	    chartreuse: 8388352,
	    chocolate: 13789470,
	    coral: 16744272,
	    cornflowerblue: 6591981,
	    cornsilk: 16775388,
	    crimson: 14423100,
	    cyan: 65535,
	    darkblue: 139,
	    darkcyan: 35723,
	    darkgoldenrod: 12092939,
	    darkgray: 11119017,
	    darkgreen: 25600,
	    darkgrey: 11119017,
	    darkkhaki: 12433259,
	    darkmagenta: 9109643,
	    darkolivegreen: 5597999,
	    darkorange: 16747520,
	    darkorchid: 10040012,
	    darkred: 9109504,
	    darksalmon: 15308410,
	    darkseagreen: 9419919,
	    darkslateblue: 4734347,
	    darkslategray: 3100495,
	    darkslategrey: 3100495,
	    darkturquoise: 52945,
	    darkviolet: 9699539,
	    deeppink: 16716947,
	    deepskyblue: 49151,
	    dimgray: 6908265,
	    dimgrey: 6908265,
	    dodgerblue: 2003199,
	    firebrick: 11674146,
	    floralwhite: 16775920,
	    forestgreen: 2263842,
	    fuchsia: 16711935,
	    gainsboro: 14474460,
	    ghostwhite: 16316671,
	    gold: 16766720,
	    goldenrod: 14329120,
	    gray: 8421504,
	    green: 32768,
	    greenyellow: 11403055,
	    grey: 8421504,
	    honeydew: 15794160,
	    hotpink: 16738740,
	    indianred: 13458524,
	    indigo: 4915330,
	    ivory: 16777200,
	    khaki: 15787660,
	    lavender: 15132410,
	    lavenderblush: 16773365,
	    lawngreen: 8190976,
	    lemonchiffon: 16775885,
	    lightblue: 11393254,
	    lightcoral: 15761536,
	    lightcyan: 14745599,
	    lightgoldenrodyellow: 16448210,
	    lightgray: 13882323,
	    lightgreen: 9498256,
	    lightgrey: 13882323,
	    lightpink: 16758465,
	    lightsalmon: 16752762,
	    lightseagreen: 2142890,
	    lightskyblue: 8900346,
	    lightslategray: 7833753,
	    lightslategrey: 7833753,
	    lightsteelblue: 11584734,
	    lightyellow: 16777184,
	    lime: 65280,
	    limegreen: 3329330,
	    linen: 16445670,
	    magenta: 16711935,
	    maroon: 8388608,
	    mediumaquamarine: 6737322,
	    mediumblue: 205,
	    mediumorchid: 12211667,
	    mediumpurple: 9662683,
	    mediumseagreen: 3978097,
	    mediumslateblue: 8087790,
	    mediumspringgreen: 64154,
	    mediumturquoise: 4772300,
	    mediumvioletred: 13047173,
	    midnightblue: 1644912,
	    mintcream: 16121850,
	    mistyrose: 16770273,
	    moccasin: 16770229,
	    navajowhite: 16768685,
	    navy: 128,
	    oldlace: 16643558,
	    olive: 8421376,
	    olivedrab: 7048739,
	    orange: 16753920,
	    orangered: 16729344,
	    orchid: 14315734,
	    palegoldenrod: 15657130,
	    palegreen: 10025880,
	    paleturquoise: 11529966,
	    palevioletred: 14381203,
	    papayawhip: 16773077,
	    peachpuff: 16767673,
	    peru: 13468991,
	    pink: 16761035,
	    plum: 14524637,
	    powderblue: 11591910,
	    purple: 8388736,
	    rebeccapurple: 6697881,
	    red: 16711680,
	    rosybrown: 12357519,
	    royalblue: 4286945,
	    saddlebrown: 9127187,
	    salmon: 16416882,
	    sandybrown: 16032864,
	    seagreen: 3050327,
	    seashell: 16774638,
	    sienna: 10506797,
	    silver: 12632256,
	    skyblue: 8900331,
	    slateblue: 6970061,
	    slategray: 7372944,
	    slategrey: 7372944,
	    snow: 16775930,
	    springgreen: 65407,
	    steelblue: 4620980,
	    tan: 13808780,
	    teal: 32896,
	    thistle: 14204888,
	    tomato: 16737095,
	    turquoise: 4251856,
	    violet: 15631086,
	    wheat: 16113331,
	    white: 16777215,
	    whitesmoke: 16119285,
	    yellow: 16776960,
	    yellowgreen: 10145074
	  });
	  d3_rgb_names.forEach(function(key, value) {
	    d3_rgb_names.set(key, d3_rgbNumber(value));
	  });
	  function d3_functor(v) {
	    return typeof v === "function" ? v : function() {
	      return v;
	    };
	  }
	  d3.functor = d3_functor;
	  d3.xhr = d3_xhrType(d3_identity);
	  function d3_xhrType(response) {
	    return function(url, mimeType, callback) {
	      if (arguments.length === 2 && typeof mimeType === "function") callback = mimeType, 
	      mimeType = null;
	      return d3_xhr(url, mimeType, response, callback);
	    };
	  }
	  function d3_xhr(url, mimeType, response, callback) {
	    var xhr = {}, dispatch = d3.dispatch("beforesend", "progress", "load", "error"), headers = {}, request = new XMLHttpRequest(), responseType = null;
	    if (this.XDomainRequest && !("withCredentials" in request) && /^(http(s)?:)?\/\//.test(url)) request = new XDomainRequest();
	    "onload" in request ? request.onload = request.onerror = respond : request.onreadystatechange = function() {
	      request.readyState > 3 && respond();
	    };
	    function respond() {
	      var status = request.status, result;
	      if (!status && d3_xhrHasResponse(request) || status >= 200 && status < 300 || status === 304) {
	        try {
	          result = response.call(xhr, request);
	        } catch (e) {
	          dispatch.error.call(xhr, e);
	          return;
	        }
	        dispatch.load.call(xhr, result);
	      } else {
	        dispatch.error.call(xhr, request);
	      }
	    }
	    request.onprogress = function(event) {
	      var o = d3.event;
	      d3.event = event;
	      try {
	        dispatch.progress.call(xhr, request);
	      } finally {
	        d3.event = o;
	      }
	    };
	    xhr.header = function(name, value) {
	      name = (name + "").toLowerCase();
	      if (arguments.length < 2) return headers[name];
	      if (value == null) delete headers[name]; else headers[name] = value + "";
	      return xhr;
	    };
	    xhr.mimeType = function(value) {
	      if (!arguments.length) return mimeType;
	      mimeType = value == null ? null : value + "";
	      return xhr;
	    };
	    xhr.responseType = function(value) {
	      if (!arguments.length) return responseType;
	      responseType = value;
	      return xhr;
	    };
	    xhr.response = function(value) {
	      response = value;
	      return xhr;
	    };
	    [ "get", "post" ].forEach(function(method) {
	      xhr[method] = function() {
	        return xhr.send.apply(xhr, [ method ].concat(d3_array(arguments)));
	      };
	    });
	    xhr.send = function(method, data, callback) {
	      if (arguments.length === 2 && typeof data === "function") callback = data, data = null;
	      request.open(method, url, true);
	      if (mimeType != null && !("accept" in headers)) headers["accept"] = mimeType + ",*/*";
	      if (request.setRequestHeader) for (var name in headers) request.setRequestHeader(name, headers[name]);
	      if (mimeType != null && request.overrideMimeType) request.overrideMimeType(mimeType);
	      if (responseType != null) request.responseType = responseType;
	      if (callback != null) xhr.on("error", callback).on("load", function(request) {
	        callback(null, request);
	      });
	      dispatch.beforesend.call(xhr, request);
	      request.send(data == null ? null : data);
	      return xhr;
	    };
	    xhr.abort = function() {
	      request.abort();
	      return xhr;
	    };
	    d3.rebind(xhr, dispatch, "on");
	    return callback == null ? xhr : xhr.get(d3_xhr_fixCallback(callback));
	  }
	  function d3_xhr_fixCallback(callback) {
	    return callback.length === 1 ? function(error, request) {
	      callback(error == null ? request : null);
	    } : callback;
	  }
	  function d3_xhrHasResponse(request) {
	    var type = request.responseType;
	    return type && type !== "text" ? request.response : request.responseText;
	  }
	  d3.dsv = function(delimiter, mimeType) {
	    var reFormat = new RegExp('["' + delimiter + "\n]"), delimiterCode = delimiter.charCodeAt(0);
	    function dsv(url, row, callback) {
	      if (arguments.length < 3) callback = row, row = null;
	      var xhr = d3_xhr(url, mimeType, row == null ? response : typedResponse(row), callback);
	      xhr.row = function(_) {
	        return arguments.length ? xhr.response((row = _) == null ? response : typedResponse(_)) : row;
	      };
	      return xhr;
	    }
	    function response(request) {
	      return dsv.parse(request.responseText);
	    }
	    function typedResponse(f) {
	      return function(request) {
	        return dsv.parse(request.responseText, f);
	      };
	    }
	    dsv.parse = function(text, f) {
	      var o;
	      return dsv.parseRows(text, function(row, i) {
	        if (o) return o(row, i - 1);
	        var a = new Function("d", "return {" + row.map(function(name, i) {
	          return JSON.stringify(name) + ": d[" + i + "]";
	        }).join(",") + "}");
	        o = f ? function(row, i) {
	          return f(a(row), i);
	        } : a;
	      });
	    };
	    dsv.parseRows = function(text, f) {
	      var EOL = {}, EOF = {}, rows = [], N = text.length, I = 0, n = 0, t, eol;
	      function token() {
	        if (I >= N) return EOF;
	        if (eol) return eol = false, EOL;
	        var j = I;
	        if (text.charCodeAt(j) === 34) {
	          var i = j;
	          while (i++ < N) {
	            if (text.charCodeAt(i) === 34) {
	              if (text.charCodeAt(i + 1) !== 34) break;
	              ++i;
	            }
	          }
	          I = i + 2;
	          var c = text.charCodeAt(i + 1);
	          if (c === 13) {
	            eol = true;
	            if (text.charCodeAt(i + 2) === 10) ++I;
	          } else if (c === 10) {
	            eol = true;
	          }
	          return text.slice(j + 1, i).replace(/""/g, '"');
	        }
	        while (I < N) {
	          var c = text.charCodeAt(I++), k = 1;
	          if (c === 10) eol = true; else if (c === 13) {
	            eol = true;
	            if (text.charCodeAt(I) === 10) ++I, ++k;
	          } else if (c !== delimiterCode) continue;
	          return text.slice(j, I - k);
	        }
	        return text.slice(j);
	      }
	      while ((t = token()) !== EOF) {
	        var a = [];
	        while (t !== EOL && t !== EOF) {
	          a.push(t);
	          t = token();
	        }
	        if (f && (a = f(a, n++)) == null) continue;
	        rows.push(a);
	      }
	      return rows;
	    };
	    dsv.format = function(rows) {
	      if (Array.isArray(rows[0])) return dsv.formatRows(rows);
	      var fieldSet = new d3_Set(), fields = [];
	      rows.forEach(function(row) {
	        for (var field in row) {
	          if (!fieldSet.has(field)) {
	            fields.push(fieldSet.add(field));
	          }
	        }
	      });
	      return [ fields.map(formatValue).join(delimiter) ].concat(rows.map(function(row) {
	        return fields.map(function(field) {
	          return formatValue(row[field]);
	        }).join(delimiter);
	      })).join("\n");
	    };
	    dsv.formatRows = function(rows) {
	      return rows.map(formatRow).join("\n");
	    };
	    function formatRow(row) {
	      return row.map(formatValue).join(delimiter);
	    }
	    function formatValue(text) {
	      return reFormat.test(text) ? '"' + text.replace(/\"/g, '""') + '"' : text;
	    }
	    return dsv;
	  };
	  d3.csv = d3.dsv(",", "text/csv");
	  d3.tsv = d3.dsv("	", "text/tab-separated-values");
	  var d3_timer_queueHead, d3_timer_queueTail, d3_timer_interval, d3_timer_timeout, d3_timer_frame = this[d3_vendorSymbol(this, "requestAnimationFrame")] || function(callback) {
	    setTimeout(callback, 17);
	  };
	  d3.timer = function() {
	    d3_timer.apply(this, arguments);
	  };
	  function d3_timer(callback, delay, then) {
	    var n = arguments.length;
	    if (n < 2) delay = 0;
	    if (n < 3) then = Date.now();
	    var time = then + delay, timer = {
	      c: callback,
	      t: time,
	      n: null
	    };
	    if (d3_timer_queueTail) d3_timer_queueTail.n = timer; else d3_timer_queueHead = timer;
	    d3_timer_queueTail = timer;
	    if (!d3_timer_interval) {
	      d3_timer_timeout = clearTimeout(d3_timer_timeout);
	      d3_timer_interval = 1;
	      d3_timer_frame(d3_timer_step);
	    }
	    return timer;
	  }
	  function d3_timer_step() {
	    var now = d3_timer_mark(), delay = d3_timer_sweep() - now;
	    if (delay > 24) {
	      if (isFinite(delay)) {
	        clearTimeout(d3_timer_timeout);
	        d3_timer_timeout = setTimeout(d3_timer_step, delay);
	      }
	      d3_timer_interval = 0;
	    } else {
	      d3_timer_interval = 1;
	      d3_timer_frame(d3_timer_step);
	    }
	  }
	  d3.timer.flush = function() {
	    d3_timer_mark();
	    d3_timer_sweep();
	  };
	  function d3_timer_mark() {
	    var now = Date.now(), timer = d3_timer_queueHead;
	    while (timer) {
	      if (now >= timer.t && timer.c(now - timer.t)) timer.c = null;
	      timer = timer.n;
	    }
	    return now;
	  }
	  function d3_timer_sweep() {
	    var t0, t1 = d3_timer_queueHead, time = Infinity;
	    while (t1) {
	      if (t1.c) {
	        if (t1.t < time) time = t1.t;
	        t1 = (t0 = t1).n;
	      } else {
	        t1 = t0 ? t0.n = t1.n : d3_timer_queueHead = t1.n;
	      }
	    }
	    d3_timer_queueTail = t0;
	    return time;
	  }
	  function d3_format_precision(x, p) {
	    return p - (x ? Math.ceil(Math.log(x) / Math.LN10) : 1);
	  }
	  d3.round = function(x, n) {
	    return n ? Math.round(x * (n = Math.pow(10, n))) / n : Math.round(x);
	  };
	  var d3_formatPrefixes = [ "y", "z", "a", "f", "p", "n", "µ", "m", "", "k", "M", "G", "T", "P", "E", "Z", "Y" ].map(d3_formatPrefix);
	  d3.formatPrefix = function(value, precision) {
	    var i = 0;
	    if (value = +value) {
	      if (value < 0) value *= -1;
	      if (precision) value = d3.round(value, d3_format_precision(value, precision));
	      i = 1 + Math.floor(1e-12 + Math.log(value) / Math.LN10);
	      i = Math.max(-24, Math.min(24, Math.floor((i - 1) / 3) * 3));
	    }
	    return d3_formatPrefixes[8 + i / 3];
	  };
	  function d3_formatPrefix(d, i) {
	    var k = Math.pow(10, abs(8 - i) * 3);
	    return {
	      scale: i > 8 ? function(d) {
	        return d / k;
	      } : function(d) {
	        return d * k;
	      },
	      symbol: d
	    };
	  }
	  function d3_locale_numberFormat(locale) {
	    var locale_decimal = locale.decimal, locale_thousands = locale.thousands, locale_grouping = locale.grouping, locale_currency = locale.currency, formatGroup = locale_grouping && locale_thousands ? function(value, width) {
	      var i = value.length, t = [], j = 0, g = locale_grouping[0], length = 0;
	      while (i > 0 && g > 0) {
	        if (length + g + 1 > width) g = Math.max(1, width - length);
	        t.push(value.substring(i -= g, i + g));
	        if ((length += g + 1) > width) break;
	        g = locale_grouping[j = (j + 1) % locale_grouping.length];
	      }
	      return t.reverse().join(locale_thousands);
	    } : d3_identity;
	    return function(specifier) {
	      var match = d3_format_re.exec(specifier), fill = match[1] || " ", align = match[2] || ">", sign = match[3] || "-", symbol = match[4] || "", zfill = match[5], width = +match[6], comma = match[7], precision = match[8], type = match[9], scale = 1, prefix = "", suffix = "", integer = false, exponent = true;
	      if (precision) precision = +precision.substring(1);
	      if (zfill || fill === "0" && align === "=") {
	        zfill = fill = "0";
	        align = "=";
	      }
	      switch (type) {
	       case "n":
	        comma = true;
	        type = "g";
	        break;

	       case "%":
	        scale = 100;
	        suffix = "%";
	        type = "f";
	        break;

	       case "p":
	        scale = 100;
	        suffix = "%";
	        type = "r";
	        break;

	       case "b":
	       case "o":
	       case "x":
	       case "X":
	        if (symbol === "#") prefix = "0" + type.toLowerCase();

	       case "c":
	        exponent = false;

	       case "d":
	        integer = true;
	        precision = 0;
	        break;

	       case "s":
	        scale = -1;
	        type = "r";
	        break;
	      }
	      if (symbol === "$") prefix = locale_currency[0], suffix = locale_currency[1];
	      if (type == "r" && !precision) type = "g";
	      if (precision != null) {
	        if (type == "g") precision = Math.max(1, Math.min(21, precision)); else if (type == "e" || type == "f") precision = Math.max(0, Math.min(20, precision));
	      }
	      type = d3_format_types.get(type) || d3_format_typeDefault;
	      var zcomma = zfill && comma;
	      return function(value) {
	        var fullSuffix = suffix;
	        if (integer && value % 1) return "";
	        var negative = value < 0 || value === 0 && 1 / value < 0 ? (value = -value, "-") : sign === "-" ? "" : sign;
	        if (scale < 0) {
	          var unit = d3.formatPrefix(value, precision);
	          value = unit.scale(value);
	          fullSuffix = unit.symbol + suffix;
	        } else {
	          value *= scale;
	        }
	        value = type(value, precision);
	        var i = value.lastIndexOf("."), before, after;
	        if (i < 0) {
	          var j = exponent ? value.lastIndexOf("e") : -1;
	          if (j < 0) before = value, after = ""; else before = value.substring(0, j), after = value.substring(j);
	        } else {
	          before = value.substring(0, i);
	          after = locale_decimal + value.substring(i + 1);
	        }
	        if (!zfill && comma) before = formatGroup(before, Infinity);
	        var length = prefix.length + before.length + after.length + (zcomma ? 0 : negative.length), padding = length < width ? new Array(length = width - length + 1).join(fill) : "";
	        if (zcomma) before = formatGroup(padding + before, padding.length ? width - after.length : Infinity);
	        negative += prefix;
	        value = before + after;
	        return (align === "<" ? negative + value + padding : align === ">" ? padding + negative + value : align === "^" ? padding.substring(0, length >>= 1) + negative + value + padding.substring(length) : negative + (zcomma ? value : padding + value)) + fullSuffix;
	      };
	    };
	  }
	  var d3_format_re = /(?:([^{])?([<>=^]))?([+\- ])?([$#])?(0)?(\d+)?(,)?(\.-?\d+)?([a-z%])?/i;
	  var d3_format_types = d3.map({
	    b: function(x) {
	      return x.toString(2);
	    },
	    c: function(x) {
	      return String.fromCharCode(x);
	    },
	    o: function(x) {
	      return x.toString(8);
	    },
	    x: function(x) {
	      return x.toString(16);
	    },
	    X: function(x) {
	      return x.toString(16).toUpperCase();
	    },
	    g: function(x, p) {
	      return x.toPrecision(p);
	    },
	    e: function(x, p) {
	      return x.toExponential(p);
	    },
	    f: function(x, p) {
	      return x.toFixed(p);
	    },
	    r: function(x, p) {
	      return (x = d3.round(x, d3_format_precision(x, p))).toFixed(Math.max(0, Math.min(20, d3_format_precision(x * (1 + 1e-15), p))));
	    }
	  });
	  function d3_format_typeDefault(x) {
	    return x + "";
	  }
	  var d3_time = d3.time = {}, d3_date = Date;
	  function d3_date_utc() {
	    this._ = new Date(arguments.length > 1 ? Date.UTC.apply(this, arguments) : arguments[0]);
	  }
	  d3_date_utc.prototype = {
	    getDate: function() {
	      return this._.getUTCDate();
	    },
	    getDay: function() {
	      return this._.getUTCDay();
	    },
	    getFullYear: function() {
	      return this._.getUTCFullYear();
	    },
	    getHours: function() {
	      return this._.getUTCHours();
	    },
	    getMilliseconds: function() {
	      return this._.getUTCMilliseconds();
	    },
	    getMinutes: function() {
	      return this._.getUTCMinutes();
	    },
	    getMonth: function() {
	      return this._.getUTCMonth();
	    },
	    getSeconds: function() {
	      return this._.getUTCSeconds();
	    },
	    getTime: function() {
	      return this._.getTime();
	    },
	    getTimezoneOffset: function() {
	      return 0;
	    },
	    valueOf: function() {
	      return this._.valueOf();
	    },
	    setDate: function() {
	      d3_time_prototype.setUTCDate.apply(this._, arguments);
	    },
	    setDay: function() {
	      d3_time_prototype.setUTCDay.apply(this._, arguments);
	    },
	    setFullYear: function() {
	      d3_time_prototype.setUTCFullYear.apply(this._, arguments);
	    },
	    setHours: function() {
	      d3_time_prototype.setUTCHours.apply(this._, arguments);
	    },
	    setMilliseconds: function() {
	      d3_time_prototype.setUTCMilliseconds.apply(this._, arguments);
	    },
	    setMinutes: function() {
	      d3_time_prototype.setUTCMinutes.apply(this._, arguments);
	    },
	    setMonth: function() {
	      d3_time_prototype.setUTCMonth.apply(this._, arguments);
	    },
	    setSeconds: function() {
	      d3_time_prototype.setUTCSeconds.apply(this._, arguments);
	    },
	    setTime: function() {
	      d3_time_prototype.setTime.apply(this._, arguments);
	    }
	  };
	  var d3_time_prototype = Date.prototype;
	  function d3_time_interval(local, step, number) {
	    function round(date) {
	      var d0 = local(date), d1 = offset(d0, 1);
	      return date - d0 < d1 - date ? d0 : d1;
	    }
	    function ceil(date) {
	      step(date = local(new d3_date(date - 1)), 1);
	      return date;
	    }
	    function offset(date, k) {
	      step(date = new d3_date(+date), k);
	      return date;
	    }
	    function range(t0, t1, dt) {
	      var time = ceil(t0), times = [];
	      if (dt > 1) {
	        while (time < t1) {
	          if (!(number(time) % dt)) times.push(new Date(+time));
	          step(time, 1);
	        }
	      } else {
	        while (time < t1) times.push(new Date(+time)), step(time, 1);
	      }
	      return times;
	    }
	    function range_utc(t0, t1, dt) {
	      try {
	        d3_date = d3_date_utc;
	        var utc = new d3_date_utc();
	        utc._ = t0;
	        return range(utc, t1, dt);
	      } finally {
	        d3_date = Date;
	      }
	    }
	    local.floor = local;
	    local.round = round;
	    local.ceil = ceil;
	    local.offset = offset;
	    local.range = range;
	    var utc = local.utc = d3_time_interval_utc(local);
	    utc.floor = utc;
	    utc.round = d3_time_interval_utc(round);
	    utc.ceil = d3_time_interval_utc(ceil);
	    utc.offset = d3_time_interval_utc(offset);
	    utc.range = range_utc;
	    return local;
	  }
	  function d3_time_interval_utc(method) {
	    return function(date, k) {
	      try {
	        d3_date = d3_date_utc;
	        var utc = new d3_date_utc();
	        utc._ = date;
	        return method(utc, k)._;
	      } finally {
	        d3_date = Date;
	      }
	    };
	  }
	  d3_time.year = d3_time_interval(function(date) {
	    date = d3_time.day(date);
	    date.setMonth(0, 1);
	    return date;
	  }, function(date, offset) {
	    date.setFullYear(date.getFullYear() + offset);
	  }, function(date) {
	    return date.getFullYear();
	  });
	  d3_time.years = d3_time.year.range;
	  d3_time.years.utc = d3_time.year.utc.range;
	  d3_time.day = d3_time_interval(function(date) {
	    var day = new d3_date(2e3, 0);
	    day.setFullYear(date.getFullYear(), date.getMonth(), date.getDate());
	    return day;
	  }, function(date, offset) {
	    date.setDate(date.getDate() + offset);
	  }, function(date) {
	    return date.getDate() - 1;
	  });
	  d3_time.days = d3_time.day.range;
	  d3_time.days.utc = d3_time.day.utc.range;
	  d3_time.dayOfYear = function(date) {
	    var year = d3_time.year(date);
	    return Math.floor((date - year - (date.getTimezoneOffset() - year.getTimezoneOffset()) * 6e4) / 864e5);
	  };
	  [ "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday" ].forEach(function(day, i) {
	    i = 7 - i;
	    var interval = d3_time[day] = d3_time_interval(function(date) {
	      (date = d3_time.day(date)).setDate(date.getDate() - (date.getDay() + i) % 7);
	      return date;
	    }, function(date, offset) {
	      date.setDate(date.getDate() + Math.floor(offset) * 7);
	    }, function(date) {
	      var day = d3_time.year(date).getDay();
	      return Math.floor((d3_time.dayOfYear(date) + (day + i) % 7) / 7) - (day !== i);
	    });
	    d3_time[day + "s"] = interval.range;
	    d3_time[day + "s"].utc = interval.utc.range;
	    d3_time[day + "OfYear"] = function(date) {
	      var day = d3_time.year(date).getDay();
	      return Math.floor((d3_time.dayOfYear(date) + (day + i) % 7) / 7);
	    };
	  });
	  d3_time.week = d3_time.sunday;
	  d3_time.weeks = d3_time.sunday.range;
	  d3_time.weeks.utc = d3_time.sunday.utc.range;
	  d3_time.weekOfYear = d3_time.sundayOfYear;
	  function d3_locale_timeFormat(locale) {
	    var locale_dateTime = locale.dateTime, locale_date = locale.date, locale_time = locale.time, locale_periods = locale.periods, locale_days = locale.days, locale_shortDays = locale.shortDays, locale_months = locale.months, locale_shortMonths = locale.shortMonths;
	    function d3_time_format(template) {
	      var n = template.length;
	      function format(date) {
	        var string = [], i = -1, j = 0, c, p, f;
	        while (++i < n) {
	          if (template.charCodeAt(i) === 37) {
	            string.push(template.slice(j, i));
	            if ((p = d3_time_formatPads[c = template.charAt(++i)]) != null) c = template.charAt(++i);
	            if (f = d3_time_formats[c]) c = f(date, p == null ? c === "e" ? " " : "0" : p);
	            string.push(c);
	            j = i + 1;
	          }
	        }
	        string.push(template.slice(j, i));
	        return string.join("");
	      }
	      format.parse = function(string) {
	        var d = {
	          y: 1900,
	          m: 0,
	          d: 1,
	          H: 0,
	          M: 0,
	          S: 0,
	          L: 0,
	          Z: null
	        }, i = d3_time_parse(d, template, string, 0);
	        if (i != string.length) return null;
	        if ("p" in d) d.H = d.H % 12 + d.p * 12;
	        var localZ = d.Z != null && d3_date !== d3_date_utc, date = new (localZ ? d3_date_utc : d3_date)();
	        if ("j" in d) date.setFullYear(d.y, 0, d.j); else if ("W" in d || "U" in d) {
	          if (!("w" in d)) d.w = "W" in d ? 1 : 0;
	          date.setFullYear(d.y, 0, 1);
	          date.setFullYear(d.y, 0, "W" in d ? (d.w + 6) % 7 + d.W * 7 - (date.getDay() + 5) % 7 : d.w + d.U * 7 - (date.getDay() + 6) % 7);
	        } else date.setFullYear(d.y, d.m, d.d);
	        date.setHours(d.H + (d.Z / 100 | 0), d.M + d.Z % 100, d.S, d.L);
	        return localZ ? date._ : date;
	      };
	      format.toString = function() {
	        return template;
	      };
	      return format;
	    }
	    function d3_time_parse(date, template, string, j) {
	      var c, p, t, i = 0, n = template.length, m = string.length;
	      while (i < n) {
	        if (j >= m) return -1;
	        c = template.charCodeAt(i++);
	        if (c === 37) {
	          t = template.charAt(i++);
	          p = d3_time_parsers[t in d3_time_formatPads ? template.charAt(i++) : t];
	          if (!p || (j = p(date, string, j)) < 0) return -1;
	        } else if (c != string.charCodeAt(j++)) {
	          return -1;
	        }
	      }
	      return j;
	    }
	    d3_time_format.utc = function(template) {
	      var local = d3_time_format(template);
	      function format(date) {
	        try {
	          d3_date = d3_date_utc;
	          var utc = new d3_date();
	          utc._ = date;
	          return local(utc);
	        } finally {
	          d3_date = Date;
	        }
	      }
	      format.parse = function(string) {
	        try {
	          d3_date = d3_date_utc;
	          var date = local.parse(string);
	          return date && date._;
	        } finally {
	          d3_date = Date;
	        }
	      };
	      format.toString = local.toString;
	      return format;
	    };
	    d3_time_format.multi = d3_time_format.utc.multi = d3_time_formatMulti;
	    var d3_time_periodLookup = d3.map(), d3_time_dayRe = d3_time_formatRe(locale_days), d3_time_dayLookup = d3_time_formatLookup(locale_days), d3_time_dayAbbrevRe = d3_time_formatRe(locale_shortDays), d3_time_dayAbbrevLookup = d3_time_formatLookup(locale_shortDays), d3_time_monthRe = d3_time_formatRe(locale_months), d3_time_monthLookup = d3_time_formatLookup(locale_months), d3_time_monthAbbrevRe = d3_time_formatRe(locale_shortMonths), d3_time_monthAbbrevLookup = d3_time_formatLookup(locale_shortMonths);
	    locale_periods.forEach(function(p, i) {
	      d3_time_periodLookup.set(p.toLowerCase(), i);
	    });
	    var d3_time_formats = {
	      a: function(d) {
	        return locale_shortDays[d.getDay()];
	      },
	      A: function(d) {
	        return locale_days[d.getDay()];
	      },
	      b: function(d) {
	        return locale_shortMonths[d.getMonth()];
	      },
	      B: function(d) {
	        return locale_months[d.getMonth()];
	      },
	      c: d3_time_format(locale_dateTime),
	      d: function(d, p) {
	        return d3_time_formatPad(d.getDate(), p, 2);
	      },
	      e: function(d, p) {
	        return d3_time_formatPad(d.getDate(), p, 2);
	      },
	      H: function(d, p) {
	        return d3_time_formatPad(d.getHours(), p, 2);
	      },
	      I: function(d, p) {
	        return d3_time_formatPad(d.getHours() % 12 || 12, p, 2);
	      },
	      j: function(d, p) {
	        return d3_time_formatPad(1 + d3_time.dayOfYear(d), p, 3);
	      },
	      L: function(d, p) {
	        return d3_time_formatPad(d.getMilliseconds(), p, 3);
	      },
	      m: function(d, p) {
	        return d3_time_formatPad(d.getMonth() + 1, p, 2);
	      },
	      M: function(d, p) {
	        return d3_time_formatPad(d.getMinutes(), p, 2);
	      },
	      p: function(d) {
	        return locale_periods[+(d.getHours() >= 12)];
	      },
	      S: function(d, p) {
	        return d3_time_formatPad(d.getSeconds(), p, 2);
	      },
	      U: function(d, p) {
	        return d3_time_formatPad(d3_time.sundayOfYear(d), p, 2);
	      },
	      w: function(d) {
	        return d.getDay();
	      },
	      W: function(d, p) {
	        return d3_time_formatPad(d3_time.mondayOfYear(d), p, 2);
	      },
	      x: d3_time_format(locale_date),
	      X: d3_time_format(locale_time),
	      y: function(d, p) {
	        return d3_time_formatPad(d.getFullYear() % 100, p, 2);
	      },
	      Y: function(d, p) {
	        return d3_time_formatPad(d.getFullYear() % 1e4, p, 4);
	      },
	      Z: d3_time_zone,
	      "%": function() {
	        return "%";
	      }
	    };
	    var d3_time_parsers = {
	      a: d3_time_parseWeekdayAbbrev,
	      A: d3_time_parseWeekday,
	      b: d3_time_parseMonthAbbrev,
	      B: d3_time_parseMonth,
	      c: d3_time_parseLocaleFull,
	      d: d3_time_parseDay,
	      e: d3_time_parseDay,
	      H: d3_time_parseHour24,
	      I: d3_time_parseHour24,
	      j: d3_time_parseDayOfYear,
	      L: d3_time_parseMilliseconds,
	      m: d3_time_parseMonthNumber,
	      M: d3_time_parseMinutes,
	      p: d3_time_parseAmPm,
	      S: d3_time_parseSeconds,
	      U: d3_time_parseWeekNumberSunday,
	      w: d3_time_parseWeekdayNumber,
	      W: d3_time_parseWeekNumberMonday,
	      x: d3_time_parseLocaleDate,
	      X: d3_time_parseLocaleTime,
	      y: d3_time_parseYear,
	      Y: d3_time_parseFullYear,
	      Z: d3_time_parseZone,
	      "%": d3_time_parseLiteralPercent
	    };
	    function d3_time_parseWeekdayAbbrev(date, string, i) {
	      d3_time_dayAbbrevRe.lastIndex = 0;
	      var n = d3_time_dayAbbrevRe.exec(string.slice(i));
	      return n ? (date.w = d3_time_dayAbbrevLookup.get(n[0].toLowerCase()), i + n[0].length) : -1;
	    }
	    function d3_time_parseWeekday(date, string, i) {
	      d3_time_dayRe.lastIndex = 0;
	      var n = d3_time_dayRe.exec(string.slice(i));
	      return n ? (date.w = d3_time_dayLookup.get(n[0].toLowerCase()), i + n[0].length) : -1;
	    }
	    function d3_time_parseMonthAbbrev(date, string, i) {
	      d3_time_monthAbbrevRe.lastIndex = 0;
	      var n = d3_time_monthAbbrevRe.exec(string.slice(i));
	      return n ? (date.m = d3_time_monthAbbrevLookup.get(n[0].toLowerCase()), i + n[0].length) : -1;
	    }
	    function d3_time_parseMonth(date, string, i) {
	      d3_time_monthRe.lastIndex = 0;
	      var n = d3_time_monthRe.exec(string.slice(i));
	      return n ? (date.m = d3_time_monthLookup.get(n[0].toLowerCase()), i + n[0].length) : -1;
	    }
	    function d3_time_parseLocaleFull(date, string, i) {
	      return d3_time_parse(date, d3_time_formats.c.toString(), string, i);
	    }
	    function d3_time_parseLocaleDate(date, string, i) {
	      return d3_time_parse(date, d3_time_formats.x.toString(), string, i);
	    }
	    function d3_time_parseLocaleTime(date, string, i) {
	      return d3_time_parse(date, d3_time_formats.X.toString(), string, i);
	    }
	    function d3_time_parseAmPm(date, string, i) {
	      var n = d3_time_periodLookup.get(string.slice(i, i += 2).toLowerCase());
	      return n == null ? -1 : (date.p = n, i);
	    }
	    return d3_time_format;
	  }
	  var d3_time_formatPads = {
	    "-": "",
	    _: " ",
	    "0": "0"
	  }, d3_time_numberRe = /^\s*\d+/, d3_time_percentRe = /^%/;
	  function d3_time_formatPad(value, fill, width) {
	    var sign = value < 0 ? "-" : "", string = (sign ? -value : value) + "", length = string.length;
	    return sign + (length < width ? new Array(width - length + 1).join(fill) + string : string);
	  }
	  function d3_time_formatRe(names) {
	    return new RegExp("^(?:" + names.map(d3.requote).join("|") + ")", "i");
	  }
	  function d3_time_formatLookup(names) {
	    var map = new d3_Map(), i = -1, n = names.length;
	    while (++i < n) map.set(names[i].toLowerCase(), i);
	    return map;
	  }
	  function d3_time_parseWeekdayNumber(date, string, i) {
	    d3_time_numberRe.lastIndex = 0;
	    var n = d3_time_numberRe.exec(string.slice(i, i + 1));
	    return n ? (date.w = +n[0], i + n[0].length) : -1;
	  }
	  function d3_time_parseWeekNumberSunday(date, string, i) {
	    d3_time_numberRe.lastIndex = 0;
	    var n = d3_time_numberRe.exec(string.slice(i));
	    return n ? (date.U = +n[0], i + n[0].length) : -1;
	  }
	  function d3_time_parseWeekNumberMonday(date, string, i) {
	    d3_time_numberRe.lastIndex = 0;
	    var n = d3_time_numberRe.exec(string.slice(i));
	    return n ? (date.W = +n[0], i + n[0].length) : -1;
	  }
	  function d3_time_parseFullYear(date, string, i) {
	    d3_time_numberRe.lastIndex = 0;
	    var n = d3_time_numberRe.exec(string.slice(i, i + 4));
	    return n ? (date.y = +n[0], i + n[0].length) : -1;
	  }
	  function d3_time_parseYear(date, string, i) {
	    d3_time_numberRe.lastIndex = 0;
	    var n = d3_time_numberRe.exec(string.slice(i, i + 2));
	    return n ? (date.y = d3_time_expandYear(+n[0]), i + n[0].length) : -1;
	  }
	  function d3_time_parseZone(date, string, i) {
	    return /^[+-]\d{4}$/.test(string = string.slice(i, i + 5)) ? (date.Z = -string, 
	    i + 5) : -1;
	  }
	  function d3_time_expandYear(d) {
	    return d + (d > 68 ? 1900 : 2e3);
	  }
	  function d3_time_parseMonthNumber(date, string, i) {
	    d3_time_numberRe.lastIndex = 0;
	    var n = d3_time_numberRe.exec(string.slice(i, i + 2));
	    return n ? (date.m = n[0] - 1, i + n[0].length) : -1;
	  }
	  function d3_time_parseDay(date, string, i) {
	    d3_time_numberRe.lastIndex = 0;
	    var n = d3_time_numberRe.exec(string.slice(i, i + 2));
	    return n ? (date.d = +n[0], i + n[0].length) : -1;
	  }
	  function d3_time_parseDayOfYear(date, string, i) {
	    d3_time_numberRe.lastIndex = 0;
	    var n = d3_time_numberRe.exec(string.slice(i, i + 3));
	    return n ? (date.j = +n[0], i + n[0].length) : -1;
	  }
	  function d3_time_parseHour24(date, string, i) {
	    d3_time_numberRe.lastIndex = 0;
	    var n = d3_time_numberRe.exec(string.slice(i, i + 2));
	    return n ? (date.H = +n[0], i + n[0].length) : -1;
	  }
	  function d3_time_parseMinutes(date, string, i) {
	    d3_time_numberRe.lastIndex = 0;
	    var n = d3_time_numberRe.exec(string.slice(i, i + 2));
	    return n ? (date.M = +n[0], i + n[0].length) : -1;
	  }
	  function d3_time_parseSeconds(date, string, i) {
	    d3_time_numberRe.lastIndex = 0;
	    var n = d3_time_numberRe.exec(string.slice(i, i + 2));
	    return n ? (date.S = +n[0], i + n[0].length) : -1;
	  }
	  function d3_time_parseMilliseconds(date, string, i) {
	    d3_time_numberRe.lastIndex = 0;
	    var n = d3_time_numberRe.exec(string.slice(i, i + 3));
	    return n ? (date.L = +n[0], i + n[0].length) : -1;
	  }
	  function d3_time_zone(d) {
	    var z = d.getTimezoneOffset(), zs = z > 0 ? "-" : "+", zh = abs(z) / 60 | 0, zm = abs(z) % 60;
	    return zs + d3_time_formatPad(zh, "0", 2) + d3_time_formatPad(zm, "0", 2);
	  }
	  function d3_time_parseLiteralPercent(date, string, i) {
	    d3_time_percentRe.lastIndex = 0;
	    var n = d3_time_percentRe.exec(string.slice(i, i + 1));
	    return n ? i + n[0].length : -1;
	  }
	  function d3_time_formatMulti(formats) {
	    var n = formats.length, i = -1;
	    while (++i < n) formats[i][0] = this(formats[i][0]);
	    return function(date) {
	      var i = 0, f = formats[i];
	      while (!f[1](date)) f = formats[++i];
	      return f[0](date);
	    };
	  }
	  d3.locale = function(locale) {
	    return {
	      numberFormat: d3_locale_numberFormat(locale),
	      timeFormat: d3_locale_timeFormat(locale)
	    };
	  };
	  var d3_locale_enUS = d3.locale({
	    decimal: ".",
	    thousands: ",",
	    grouping: [ 3 ],
	    currency: [ "$", "" ],
	    dateTime: "%a %b %e %X %Y",
	    date: "%m/%d/%Y",
	    time: "%H:%M:%S",
	    periods: [ "AM", "PM" ],
	    days: [ "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" ],
	    shortDays: [ "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" ],
	    months: [ "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" ],
	    shortMonths: [ "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" ]
	  });
	  d3.format = d3_locale_enUS.numberFormat;
	  d3.geo = {};
	  function d3_adder() {}
	  d3_adder.prototype = {
	    s: 0,
	    t: 0,
	    add: function(y) {
	      d3_adderSum(y, this.t, d3_adderTemp);
	      d3_adderSum(d3_adderTemp.s, this.s, this);
	      if (this.s) this.t += d3_adderTemp.t; else this.s = d3_adderTemp.t;
	    },
	    reset: function() {
	      this.s = this.t = 0;
	    },
	    valueOf: function() {
	      return this.s;
	    }
	  };
	  var d3_adderTemp = new d3_adder();
	  function d3_adderSum(a, b, o) {
	    var x = o.s = a + b, bv = x - a, av = x - bv;
	    o.t = a - av + (b - bv);
	  }
	  d3.geo.stream = function(object, listener) {
	    if (object && d3_geo_streamObjectType.hasOwnProperty(object.type)) {
	      d3_geo_streamObjectType[object.type](object, listener);
	    } else {
	      d3_geo_streamGeometry(object, listener);
	    }
	  };
	  function d3_geo_streamGeometry(geometry, listener) {
	    if (geometry && d3_geo_streamGeometryType.hasOwnProperty(geometry.type)) {
	      d3_geo_streamGeometryType[geometry.type](geometry, listener);
	    }
	  }
	  var d3_geo_streamObjectType = {
	    Feature: function(feature, listener) {
	      d3_geo_streamGeometry(feature.geometry, listener);
	    },
	    FeatureCollection: function(object, listener) {
	      var features = object.features, i = -1, n = features.length;
	      while (++i < n) d3_geo_streamGeometry(features[i].geometry, listener);
	    }
	  };
	  var d3_geo_streamGeometryType = {
	    Sphere: function(object, listener) {
	      listener.sphere();
	    },
	    Point: function(object, listener) {
	      object = object.coordinates;
	      listener.point(object[0], object[1], object[2]);
	    },
	    MultiPoint: function(object, listener) {
	      var coordinates = object.coordinates, i = -1, n = coordinates.length;
	      while (++i < n) object = coordinates[i], listener.point(object[0], object[1], object[2]);
	    },
	    LineString: function(object, listener) {
	      d3_geo_streamLine(object.coordinates, listener, 0);
	    },
	    MultiLineString: function(object, listener) {
	      var coordinates = object.coordinates, i = -1, n = coordinates.length;
	      while (++i < n) d3_geo_streamLine(coordinates[i], listener, 0);
	    },
	    Polygon: function(object, listener) {
	      d3_geo_streamPolygon(object.coordinates, listener);
	    },
	    MultiPolygon: function(object, listener) {
	      var coordinates = object.coordinates, i = -1, n = coordinates.length;
	      while (++i < n) d3_geo_streamPolygon(coordinates[i], listener);
	    },
	    GeometryCollection: function(object, listener) {
	      var geometries = object.geometries, i = -1, n = geometries.length;
	      while (++i < n) d3_geo_streamGeometry(geometries[i], listener);
	    }
	  };
	  function d3_geo_streamLine(coordinates, listener, closed) {
	    var i = -1, n = coordinates.length - closed, coordinate;
	    listener.lineStart();
	    while (++i < n) coordinate = coordinates[i], listener.point(coordinate[0], coordinate[1], coordinate[2]);
	    listener.lineEnd();
	  }
	  function d3_geo_streamPolygon(coordinates, listener) {
	    var i = -1, n = coordinates.length;
	    listener.polygonStart();
	    while (++i < n) d3_geo_streamLine(coordinates[i], listener, 1);
	    listener.polygonEnd();
	  }
	  d3.geo.area = function(object) {
	    d3_geo_areaSum = 0;
	    d3.geo.stream(object, d3_geo_area);
	    return d3_geo_areaSum;
	  };
	  var d3_geo_areaSum, d3_geo_areaRingSum = new d3_adder();
	  var d3_geo_area = {
	    sphere: function() {
	      d3_geo_areaSum += 4 * π;
	    },
	    point: d3_noop,
	    lineStart: d3_noop,
	    lineEnd: d3_noop,
	    polygonStart: function() {
	      d3_geo_areaRingSum.reset();
	      d3_geo_area.lineStart = d3_geo_areaRingStart;
	    },
	    polygonEnd: function() {
	      var area = 2 * d3_geo_areaRingSum;
	      d3_geo_areaSum += area < 0 ? 4 * π + area : area;
	      d3_geo_area.lineStart = d3_geo_area.lineEnd = d3_geo_area.point = d3_noop;
	    }
	  };
	  function d3_geo_areaRingStart() {
	    var λ00, φ00, λ0, cosφ0, sinφ0;
	    d3_geo_area.point = function(λ, φ) {
	      d3_geo_area.point = nextPoint;
	      λ0 = (λ00 = λ) * d3_radians, cosφ0 = Math.cos(φ = (φ00 = φ) * d3_radians / 2 + π / 4), 
	      sinφ0 = Math.sin(φ);
	    };
	    function nextPoint(λ, φ) {
	      λ *= d3_radians;
	      φ = φ * d3_radians / 2 + π / 4;
	      var dλ = λ - λ0, sdλ = dλ >= 0 ? 1 : -1, adλ = sdλ * dλ, cosφ = Math.cos(φ), sinφ = Math.sin(φ), k = sinφ0 * sinφ, u = cosφ0 * cosφ + k * Math.cos(adλ), v = k * sdλ * Math.sin(adλ);
	      d3_geo_areaRingSum.add(Math.atan2(v, u));
	      λ0 = λ, cosφ0 = cosφ, sinφ0 = sinφ;
	    }
	    d3_geo_area.lineEnd = function() {
	      nextPoint(λ00, φ00);
	    };
	  }
	  function d3_geo_cartesian(spherical) {
	    var λ = spherical[0], φ = spherical[1], cosφ = Math.cos(φ);
	    return [ cosφ * Math.cos(λ), cosφ * Math.sin(λ), Math.sin(φ) ];
	  }
	  function d3_geo_cartesianDot(a, b) {
	    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
	  }
	  function d3_geo_cartesianCross(a, b) {
	    return [ a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] ];
	  }
	  function d3_geo_cartesianAdd(a, b) {
	    a[0] += b[0];
	    a[1] += b[1];
	    a[2] += b[2];
	  }
	  function d3_geo_cartesianScale(vector, k) {
	    return [ vector[0] * k, vector[1] * k, vector[2] * k ];
	  }
	  function d3_geo_cartesianNormalize(d) {
	    var l = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
	    d[0] /= l;
	    d[1] /= l;
	    d[2] /= l;
	  }
	  function d3_geo_spherical(cartesian) {
	    return [ Math.atan2(cartesian[1], cartesian[0]), d3_asin(cartesian[2]) ];
	  }
	  function d3_geo_sphericalEqual(a, b) {
	    return abs(a[0] - b[0]) < ε && abs(a[1] - b[1]) < ε;
	  }
	  d3.geo.bounds = function() {
	    var λ0, φ0, λ1, φ1, λ_, λ__, φ__, p0, dλSum, ranges, range;
	    var bound = {
	      point: point,
	      lineStart: lineStart,
	      lineEnd: lineEnd,
	      polygonStart: function() {
	        bound.point = ringPoint;
	        bound.lineStart = ringStart;
	        bound.lineEnd = ringEnd;
	        dλSum = 0;
	        d3_geo_area.polygonStart();
	      },
	      polygonEnd: function() {
	        d3_geo_area.polygonEnd();
	        bound.point = point;
	        bound.lineStart = lineStart;
	        bound.lineEnd = lineEnd;
	        if (d3_geo_areaRingSum < 0) λ0 = -(λ1 = 180), φ0 = -(φ1 = 90); else if (dλSum > ε) φ1 = 90; else if (dλSum < -ε) φ0 = -90;
	        range[0] = λ0, range[1] = λ1;
	      }
	    };
	    function point(λ, φ) {
	      ranges.push(range = [ λ0 = λ, λ1 = λ ]);
	      if (φ < φ0) φ0 = φ;
	      if (φ > φ1) φ1 = φ;
	    }
	    function linePoint(λ, φ) {
	      var p = d3_geo_cartesian([ λ * d3_radians, φ * d3_radians ]);
	      if (p0) {
	        var normal = d3_geo_cartesianCross(p0, p), equatorial = [ normal[1], -normal[0], 0 ], inflection = d3_geo_cartesianCross(equatorial, normal);
	        d3_geo_cartesianNormalize(inflection);
	        inflection = d3_geo_spherical(inflection);
	        var dλ = λ - λ_, s = dλ > 0 ? 1 : -1, λi = inflection[0] * d3_degrees * s, antimeridian = abs(dλ) > 180;
	        if (antimeridian ^ (s * λ_ < λi && λi < s * λ)) {
	          var φi = inflection[1] * d3_degrees;
	          if (φi > φ1) φ1 = φi;
	        } else if (λi = (λi + 360) % 360 - 180, antimeridian ^ (s * λ_ < λi && λi < s * λ)) {
	          var φi = -inflection[1] * d3_degrees;
	          if (φi < φ0) φ0 = φi;
	        } else {
	          if (φ < φ0) φ0 = φ;
	          if (φ > φ1) φ1 = φ;
	        }
	        if (antimeridian) {
	          if (λ < λ_) {
	            if (angle(λ0, λ) > angle(λ0, λ1)) λ1 = λ;
	          } else {
	            if (angle(λ, λ1) > angle(λ0, λ1)) λ0 = λ;
	          }
	        } else {
	          if (λ1 >= λ0) {
	            if (λ < λ0) λ0 = λ;
	            if (λ > λ1) λ1 = λ;
	          } else {
	            if (λ > λ_) {
	              if (angle(λ0, λ) > angle(λ0, λ1)) λ1 = λ;
	            } else {
	              if (angle(λ, λ1) > angle(λ0, λ1)) λ0 = λ;
	            }
	          }
	        }
	      } else {
	        point(λ, φ);
	      }
	      p0 = p, λ_ = λ;
	    }
	    function lineStart() {
	      bound.point = linePoint;
	    }
	    function lineEnd() {
	      range[0] = λ0, range[1] = λ1;
	      bound.point = point;
	      p0 = null;
	    }
	    function ringPoint(λ, φ) {
	      if (p0) {
	        var dλ = λ - λ_;
	        dλSum += abs(dλ) > 180 ? dλ + (dλ > 0 ? 360 : -360) : dλ;
	      } else λ__ = λ, φ__ = φ;
	      d3_geo_area.point(λ, φ);
	      linePoint(λ, φ);
	    }
	    function ringStart() {
	      d3_geo_area.lineStart();
	    }
	    function ringEnd() {
	      ringPoint(λ__, φ__);
	      d3_geo_area.lineEnd();
	      if (abs(dλSum) > ε) λ0 = -(λ1 = 180);
	      range[0] = λ0, range[1] = λ1;
	      p0 = null;
	    }
	    function angle(λ0, λ1) {
	      return (λ1 -= λ0) < 0 ? λ1 + 360 : λ1;
	    }
	    function compareRanges(a, b) {
	      return a[0] - b[0];
	    }
	    function withinRange(x, range) {
	      return range[0] <= range[1] ? range[0] <= x && x <= range[1] : x < range[0] || range[1] < x;
	    }
	    return function(feature) {
	      φ1 = λ1 = -(λ0 = φ0 = Infinity);
	      ranges = [];
	      d3.geo.stream(feature, bound);
	      var n = ranges.length;
	      if (n) {
	        ranges.sort(compareRanges);
	        for (var i = 1, a = ranges[0], b, merged = [ a ]; i < n; ++i) {
	          b = ranges[i];
	          if (withinRange(b[0], a) || withinRange(b[1], a)) {
	            if (angle(a[0], b[1]) > angle(a[0], a[1])) a[1] = b[1];
	            if (angle(b[0], a[1]) > angle(a[0], a[1])) a[0] = b[0];
	          } else {
	            merged.push(a = b);
	          }
	        }
	        var best = -Infinity, dλ;
	        for (var n = merged.length - 1, i = 0, a = merged[n], b; i <= n; a = b, ++i) {
	          b = merged[i];
	          if ((dλ = angle(a[1], b[0])) > best) best = dλ, λ0 = b[0], λ1 = a[1];
	        }
	      }
	      ranges = range = null;
	      return λ0 === Infinity || φ0 === Infinity ? [ [ NaN, NaN ], [ NaN, NaN ] ] : [ [ λ0, φ0 ], [ λ1, φ1 ] ];
	    };
	  }();
	  d3.geo.centroid = function(object) {
	    d3_geo_centroidW0 = d3_geo_centroidW1 = d3_geo_centroidX0 = d3_geo_centroidY0 = d3_geo_centroidZ0 = d3_geo_centroidX1 = d3_geo_centroidY1 = d3_geo_centroidZ1 = d3_geo_centroidX2 = d3_geo_centroidY2 = d3_geo_centroidZ2 = 0;
	    d3.geo.stream(object, d3_geo_centroid);
	    var x = d3_geo_centroidX2, y = d3_geo_centroidY2, z = d3_geo_centroidZ2, m = x * x + y * y + z * z;
	    if (m < ε2) {
	      x = d3_geo_centroidX1, y = d3_geo_centroidY1, z = d3_geo_centroidZ1;
	      if (d3_geo_centroidW1 < ε) x = d3_geo_centroidX0, y = d3_geo_centroidY0, z = d3_geo_centroidZ0;
	      m = x * x + y * y + z * z;
	      if (m < ε2) return [ NaN, NaN ];
	    }
	    return [ Math.atan2(y, x) * d3_degrees, d3_asin(z / Math.sqrt(m)) * d3_degrees ];
	  };
	  var d3_geo_centroidW0, d3_geo_centroidW1, d3_geo_centroidX0, d3_geo_centroidY0, d3_geo_centroidZ0, d3_geo_centroidX1, d3_geo_centroidY1, d3_geo_centroidZ1, d3_geo_centroidX2, d3_geo_centroidY2, d3_geo_centroidZ2;
	  var d3_geo_centroid = {
	    sphere: d3_noop,
	    point: d3_geo_centroidPoint,
	    lineStart: d3_geo_centroidLineStart,
	    lineEnd: d3_geo_centroidLineEnd,
	    polygonStart: function() {
	      d3_geo_centroid.lineStart = d3_geo_centroidRingStart;
	    },
	    polygonEnd: function() {
	      d3_geo_centroid.lineStart = d3_geo_centroidLineStart;
	    }
	  };
	  function d3_geo_centroidPoint(λ, φ) {
	    λ *= d3_radians;
	    var cosφ = Math.cos(φ *= d3_radians);
	    d3_geo_centroidPointXYZ(cosφ * Math.cos(λ), cosφ * Math.sin(λ), Math.sin(φ));
	  }
	  function d3_geo_centroidPointXYZ(x, y, z) {
	    ++d3_geo_centroidW0;
	    d3_geo_centroidX0 += (x - d3_geo_centroidX0) / d3_geo_centroidW0;
	    d3_geo_centroidY0 += (y - d3_geo_centroidY0) / d3_geo_centroidW0;
	    d3_geo_centroidZ0 += (z - d3_geo_centroidZ0) / d3_geo_centroidW0;
	  }
	  function d3_geo_centroidLineStart() {
	    var x0, y0, z0;
	    d3_geo_centroid.point = function(λ, φ) {
	      λ *= d3_radians;
	      var cosφ = Math.cos(φ *= d3_radians);
	      x0 = cosφ * Math.cos(λ);
	      y0 = cosφ * Math.sin(λ);
	      z0 = Math.sin(φ);
	      d3_geo_centroid.point = nextPoint;
	      d3_geo_centroidPointXYZ(x0, y0, z0);
	    };
	    function nextPoint(λ, φ) {
	      λ *= d3_radians;
	      var cosφ = Math.cos(φ *= d3_radians), x = cosφ * Math.cos(λ), y = cosφ * Math.sin(λ), z = Math.sin(φ), w = Math.atan2(Math.sqrt((w = y0 * z - z0 * y) * w + (w = z0 * x - x0 * z) * w + (w = x0 * y - y0 * x) * w), x0 * x + y0 * y + z0 * z);
	      d3_geo_centroidW1 += w;
	      d3_geo_centroidX1 += w * (x0 + (x0 = x));
	      d3_geo_centroidY1 += w * (y0 + (y0 = y));
	      d3_geo_centroidZ1 += w * (z0 + (z0 = z));
	      d3_geo_centroidPointXYZ(x0, y0, z0);
	    }
	  }
	  function d3_geo_centroidLineEnd() {
	    d3_geo_centroid.point = d3_geo_centroidPoint;
	  }
	  function d3_geo_centroidRingStart() {
	    var λ00, φ00, x0, y0, z0;
	    d3_geo_centroid.point = function(λ, φ) {
	      λ00 = λ, φ00 = φ;
	      d3_geo_centroid.point = nextPoint;
	      λ *= d3_radians;
	      var cosφ = Math.cos(φ *= d3_radians);
	      x0 = cosφ * Math.cos(λ);
	      y0 = cosφ * Math.sin(λ);
	      z0 = Math.sin(φ);
	      d3_geo_centroidPointXYZ(x0, y0, z0);
	    };
	    d3_geo_centroid.lineEnd = function() {
	      nextPoint(λ00, φ00);
	      d3_geo_centroid.lineEnd = d3_geo_centroidLineEnd;
	      d3_geo_centroid.point = d3_geo_centroidPoint;
	    };
	    function nextPoint(λ, φ) {
	      λ *= d3_radians;
	      var cosφ = Math.cos(φ *= d3_radians), x = cosφ * Math.cos(λ), y = cosφ * Math.sin(λ), z = Math.sin(φ), cx = y0 * z - z0 * y, cy = z0 * x - x0 * z, cz = x0 * y - y0 * x, m = Math.sqrt(cx * cx + cy * cy + cz * cz), u = x0 * x + y0 * y + z0 * z, v = m && -d3_acos(u) / m, w = Math.atan2(m, u);
	      d3_geo_centroidX2 += v * cx;
	      d3_geo_centroidY2 += v * cy;
	      d3_geo_centroidZ2 += v * cz;
	      d3_geo_centroidW1 += w;
	      d3_geo_centroidX1 += w * (x0 + (x0 = x));
	      d3_geo_centroidY1 += w * (y0 + (y0 = y));
	      d3_geo_centroidZ1 += w * (z0 + (z0 = z));
	      d3_geo_centroidPointXYZ(x0, y0, z0);
	    }
	  }
	  function d3_geo_compose(a, b) {
	    function compose(x, y) {
	      return x = a(x, y), b(x[0], x[1]);
	    }
	    if (a.invert && b.invert) compose.invert = function(x, y) {
	      return x = b.invert(x, y), x && a.invert(x[0], x[1]);
	    };
	    return compose;
	  }
	  function d3_true() {
	    return true;
	  }
	  function d3_geo_clipPolygon(segments, compare, clipStartInside, interpolate, listener) {
	    var subject = [], clip = [];
	    segments.forEach(function(segment) {
	      if ((n = segment.length - 1) <= 0) return;
	      var n, p0 = segment[0], p1 = segment[n];
	      if (d3_geo_sphericalEqual(p0, p1)) {
	        listener.lineStart();
	        for (var i = 0; i < n; ++i) listener.point((p0 = segment[i])[0], p0[1]);
	        listener.lineEnd();
	        return;
	      }
	      var a = new d3_geo_clipPolygonIntersection(p0, segment, null, true), b = new d3_geo_clipPolygonIntersection(p0, null, a, false);
	      a.o = b;
	      subject.push(a);
	      clip.push(b);
	      a = new d3_geo_clipPolygonIntersection(p1, segment, null, false);
	      b = new d3_geo_clipPolygonIntersection(p1, null, a, true);
	      a.o = b;
	      subject.push(a);
	      clip.push(b);
	    });
	    clip.sort(compare);
	    d3_geo_clipPolygonLinkCircular(subject);
	    d3_geo_clipPolygonLinkCircular(clip);
	    if (!subject.length) return;
	    for (var i = 0, entry = clipStartInside, n = clip.length; i < n; ++i) {
	      clip[i].e = entry = !entry;
	    }
	    var start = subject[0], points, point;
	    while (1) {
	      var current = start, isSubject = true;
	      while (current.v) if ((current = current.n) === start) return;
	      points = current.z;
	      listener.lineStart();
	      do {
	        current.v = current.o.v = true;
	        if (current.e) {
	          if (isSubject) {
	            for (var i = 0, n = points.length; i < n; ++i) listener.point((point = points[i])[0], point[1]);
	          } else {
	            interpolate(current.x, current.n.x, 1, listener);
	          }
	          current = current.n;
	        } else {
	          if (isSubject) {
	            points = current.p.z;
	            for (var i = points.length - 1; i >= 0; --i) listener.point((point = points[i])[0], point[1]);
	          } else {
	            interpolate(current.x, current.p.x, -1, listener);
	          }
	          current = current.p;
	        }
	        current = current.o;
	        points = current.z;
	        isSubject = !isSubject;
	      } while (!current.v);
	      listener.lineEnd();
	    }
	  }
	  function d3_geo_clipPolygonLinkCircular(array) {
	    if (!(n = array.length)) return;
	    var n, i = 0, a = array[0], b;
	    while (++i < n) {
	      a.n = b = array[i];
	      b.p = a;
	      a = b;
	    }
	    a.n = b = array[0];
	    b.p = a;
	  }
	  function d3_geo_clipPolygonIntersection(point, points, other, entry) {
	    this.x = point;
	    this.z = points;
	    this.o = other;
	    this.e = entry;
	    this.v = false;
	    this.n = this.p = null;
	  }
	  function d3_geo_clip(pointVisible, clipLine, interpolate, clipStart) {
	    return function(rotate, listener) {
	      var line = clipLine(listener), rotatedClipStart = rotate.invert(clipStart[0], clipStart[1]);
	      var clip = {
	        point: point,
	        lineStart: lineStart,
	        lineEnd: lineEnd,
	        polygonStart: function() {
	          clip.point = pointRing;
	          clip.lineStart = ringStart;
	          clip.lineEnd = ringEnd;
	          segments = [];
	          polygon = [];
	        },
	        polygonEnd: function() {
	          clip.point = point;
	          clip.lineStart = lineStart;
	          clip.lineEnd = lineEnd;
	          segments = d3.merge(segments);
	          var clipStartInside = d3_geo_pointInPolygon(rotatedClipStart, polygon);
	          if (segments.length) {
	            if (!polygonStarted) listener.polygonStart(), polygonStarted = true;
	            d3_geo_clipPolygon(segments, d3_geo_clipSort, clipStartInside, interpolate, listener);
	          } else if (clipStartInside) {
	            if (!polygonStarted) listener.polygonStart(), polygonStarted = true;
	            listener.lineStart();
	            interpolate(null, null, 1, listener);
	            listener.lineEnd();
	          }
	          if (polygonStarted) listener.polygonEnd(), polygonStarted = false;
	          segments = polygon = null;
	        },
	        sphere: function() {
	          listener.polygonStart();
	          listener.lineStart();
	          interpolate(null, null, 1, listener);
	          listener.lineEnd();
	          listener.polygonEnd();
	        }
	      };
	      function point(λ, φ) {
	        var point = rotate(λ, φ);
	        if (pointVisible(λ = point[0], φ = point[1])) listener.point(λ, φ);
	      }
	      function pointLine(λ, φ) {
	        var point = rotate(λ, φ);
	        line.point(point[0], point[1]);
	      }
	      function lineStart() {
	        clip.point = pointLine;
	        line.lineStart();
	      }
	      function lineEnd() {
	        clip.point = point;
	        line.lineEnd();
	      }
	      var segments;
	      var buffer = d3_geo_clipBufferListener(), ringListener = clipLine(buffer), polygonStarted = false, polygon, ring;
	      function pointRing(λ, φ) {
	        ring.push([ λ, φ ]);
	        var point = rotate(λ, φ);
	        ringListener.point(point[0], point[1]);
	      }
	      function ringStart() {
	        ringListener.lineStart();
	        ring = [];
	      }
	      function ringEnd() {
	        pointRing(ring[0][0], ring[0][1]);
	        ringListener.lineEnd();
	        var clean = ringListener.clean(), ringSegments = buffer.buffer(), segment, n = ringSegments.length;
	        ring.pop();
	        polygon.push(ring);
	        ring = null;
	        if (!n) return;
	        if (clean & 1) {
	          segment = ringSegments[0];
	          var n = segment.length - 1, i = -1, point;
	          if (n > 0) {
	            if (!polygonStarted) listener.polygonStart(), polygonStarted = true;
	            listener.lineStart();
	            while (++i < n) listener.point((point = segment[i])[0], point[1]);
	            listener.lineEnd();
	          }
	          return;
	        }
	        if (n > 1 && clean & 2) ringSegments.push(ringSegments.pop().concat(ringSegments.shift()));
	        segments.push(ringSegments.filter(d3_geo_clipSegmentLength1));
	      }
	      return clip;
	    };
	  }
	  function d3_geo_clipSegmentLength1(segment) {
	    return segment.length > 1;
	  }
	  function d3_geo_clipBufferListener() {
	    var lines = [], line;
	    return {
	      lineStart: function() {
	        lines.push(line = []);
	      },
	      point: function(λ, φ) {
	        line.push([ λ, φ ]);
	      },
	      lineEnd: d3_noop,
	      buffer: function() {
	        var buffer = lines;
	        lines = [];
	        line = null;
	        return buffer;
	      },
	      rejoin: function() {
	        if (lines.length > 1) lines.push(lines.pop().concat(lines.shift()));
	      }
	    };
	  }
	  function d3_geo_clipSort(a, b) {
	    return ((a = a.x)[0] < 0 ? a[1] - halfπ - ε : halfπ - a[1]) - ((b = b.x)[0] < 0 ? b[1] - halfπ - ε : halfπ - b[1]);
	  }
	  var d3_geo_clipAntimeridian = d3_geo_clip(d3_true, d3_geo_clipAntimeridianLine, d3_geo_clipAntimeridianInterpolate, [ -π, -π / 2 ]);
	  function d3_geo_clipAntimeridianLine(listener) {
	    var λ0 = NaN, φ0 = NaN, sλ0 = NaN, clean;
	    return {
	      lineStart: function() {
	        listener.lineStart();
	        clean = 1;
	      },
	      point: function(λ1, φ1) {
	        var sλ1 = λ1 > 0 ? π : -π, dλ = abs(λ1 - λ0);
	        if (abs(dλ - π) < ε) {
	          listener.point(λ0, φ0 = (φ0 + φ1) / 2 > 0 ? halfπ : -halfπ);
	          listener.point(sλ0, φ0);
	          listener.lineEnd();
	          listener.lineStart();
	          listener.point(sλ1, φ0);
	          listener.point(λ1, φ0);
	          clean = 0;
	        } else if (sλ0 !== sλ1 && dλ >= π) {
	          if (abs(λ0 - sλ0) < ε) λ0 -= sλ0 * ε;
	          if (abs(λ1 - sλ1) < ε) λ1 -= sλ1 * ε;
	          φ0 = d3_geo_clipAntimeridianIntersect(λ0, φ0, λ1, φ1);
	          listener.point(sλ0, φ0);
	          listener.lineEnd();
	          listener.lineStart();
	          listener.point(sλ1, φ0);
	          clean = 0;
	        }
	        listener.point(λ0 = λ1, φ0 = φ1);
	        sλ0 = sλ1;
	      },
	      lineEnd: function() {
	        listener.lineEnd();
	        λ0 = φ0 = NaN;
	      },
	      clean: function() {
	        return 2 - clean;
	      }
	    };
	  }
	  function d3_geo_clipAntimeridianIntersect(λ0, φ0, λ1, φ1) {
	    var cosφ0, cosφ1, sinλ0_λ1 = Math.sin(λ0 - λ1);
	    return abs(sinλ0_λ1) > ε ? Math.atan((Math.sin(φ0) * (cosφ1 = Math.cos(φ1)) * Math.sin(λ1) - Math.sin(φ1) * (cosφ0 = Math.cos(φ0)) * Math.sin(λ0)) / (cosφ0 * cosφ1 * sinλ0_λ1)) : (φ0 + φ1) / 2;
	  }
	  function d3_geo_clipAntimeridianInterpolate(from, to, direction, listener) {
	    var φ;
	    if (from == null) {
	      φ = direction * halfπ;
	      listener.point(-π, φ);
	      listener.point(0, φ);
	      listener.point(π, φ);
	      listener.point(π, 0);
	      listener.point(π, -φ);
	      listener.point(0, -φ);
	      listener.point(-π, -φ);
	      listener.point(-π, 0);
	      listener.point(-π, φ);
	    } else if (abs(from[0] - to[0]) > ε) {
	      var s = from[0] < to[0] ? π : -π;
	      φ = direction * s / 2;
	      listener.point(-s, φ);
	      listener.point(0, φ);
	      listener.point(s, φ);
	    } else {
	      listener.point(to[0], to[1]);
	    }
	  }
	  function d3_geo_pointInPolygon(point, polygon) {
	    var meridian = point[0], parallel = point[1], meridianNormal = [ Math.sin(meridian), -Math.cos(meridian), 0 ], polarAngle = 0, winding = 0;
	    d3_geo_areaRingSum.reset();
	    for (var i = 0, n = polygon.length; i < n; ++i) {
	      var ring = polygon[i], m = ring.length;
	      if (!m) continue;
	      var point0 = ring[0], λ0 = point0[0], φ0 = point0[1] / 2 + π / 4, sinφ0 = Math.sin(φ0), cosφ0 = Math.cos(φ0), j = 1;
	      while (true) {
	        if (j === m) j = 0;
	        point = ring[j];
	        var λ = point[0], φ = point[1] / 2 + π / 4, sinφ = Math.sin(φ), cosφ = Math.cos(φ), dλ = λ - λ0, sdλ = dλ >= 0 ? 1 : -1, adλ = sdλ * dλ, antimeridian = adλ > π, k = sinφ0 * sinφ;
	        d3_geo_areaRingSum.add(Math.atan2(k * sdλ * Math.sin(adλ), cosφ0 * cosφ + k * Math.cos(adλ)));
	        polarAngle += antimeridian ? dλ + sdλ * τ : dλ;
	        if (antimeridian ^ λ0 >= meridian ^ λ >= meridian) {
	          var arc = d3_geo_cartesianCross(d3_geo_cartesian(point0), d3_geo_cartesian(point));
	          d3_geo_cartesianNormalize(arc);
	          var intersection = d3_geo_cartesianCross(meridianNormal, arc);
	          d3_geo_cartesianNormalize(intersection);
	          var φarc = (antimeridian ^ dλ >= 0 ? -1 : 1) * d3_asin(intersection[2]);
	          if (parallel > φarc || parallel === φarc && (arc[0] || arc[1])) {
	            winding += antimeridian ^ dλ >= 0 ? 1 : -1;
	          }
	        }
	        if (!j++) break;
	        λ0 = λ, sinφ0 = sinφ, cosφ0 = cosφ, point0 = point;
	      }
	    }
	    return (polarAngle < -ε || polarAngle < ε && d3_geo_areaRingSum < -ε) ^ winding & 1;
	  }
	  function d3_geo_clipCircle(radius) {
	    var cr = Math.cos(radius), smallRadius = cr > 0, notHemisphere = abs(cr) > ε, interpolate = d3_geo_circleInterpolate(radius, 6 * d3_radians);
	    return d3_geo_clip(visible, clipLine, interpolate, smallRadius ? [ 0, -radius ] : [ -π, radius - π ]);
	    function visible(λ, φ) {
	      return Math.cos(λ) * Math.cos(φ) > cr;
	    }
	    function clipLine(listener) {
	      var point0, c0, v0, v00, clean;
	      return {
	        lineStart: function() {
	          v00 = v0 = false;
	          clean = 1;
	        },
	        point: function(λ, φ) {
	          var point1 = [ λ, φ ], point2, v = visible(λ, φ), c = smallRadius ? v ? 0 : code(λ, φ) : v ? code(λ + (λ < 0 ? π : -π), φ) : 0;
	          if (!point0 && (v00 = v0 = v)) listener.lineStart();
	          if (v !== v0) {
	            point2 = intersect(point0, point1);
	            if (d3_geo_sphericalEqual(point0, point2) || d3_geo_sphericalEqual(point1, point2)) {
	              point1[0] += ε;
	              point1[1] += ε;
	              v = visible(point1[0], point1[1]);
	            }
	          }
	          if (v !== v0) {
	            clean = 0;
	            if (v) {
	              listener.lineStart();
	              point2 = intersect(point1, point0);
	              listener.point(point2[0], point2[1]);
	            } else {
	              point2 = intersect(point0, point1);
	              listener.point(point2[0], point2[1]);
	              listener.lineEnd();
	            }
	            point0 = point2;
	          } else if (notHemisphere && point0 && smallRadius ^ v) {
	            var t;
	            if (!(c & c0) && (t = intersect(point1, point0, true))) {
	              clean = 0;
	              if (smallRadius) {
	                listener.lineStart();
	                listener.point(t[0][0], t[0][1]);
	                listener.point(t[1][0], t[1][1]);
	                listener.lineEnd();
	              } else {
	                listener.point(t[1][0], t[1][1]);
	                listener.lineEnd();
	                listener.lineStart();
	                listener.point(t[0][0], t[0][1]);
	              }
	            }
	          }
	          if (v && (!point0 || !d3_geo_sphericalEqual(point0, point1))) {
	            listener.point(point1[0], point1[1]);
	          }
	          point0 = point1, v0 = v, c0 = c;
	        },
	        lineEnd: function() {
	          if (v0) listener.lineEnd();
	          point0 = null;
	        },
	        clean: function() {
	          return clean | (v00 && v0) << 1;
	        }
	      };
	    }
	    function intersect(a, b, two) {
	      var pa = d3_geo_cartesian(a), pb = d3_geo_cartesian(b);
	      var n1 = [ 1, 0, 0 ], n2 = d3_geo_cartesianCross(pa, pb), n2n2 = d3_geo_cartesianDot(n2, n2), n1n2 = n2[0], determinant = n2n2 - n1n2 * n1n2;
	      if (!determinant) return !two && a;
	      var c1 = cr * n2n2 / determinant, c2 = -cr * n1n2 / determinant, n1xn2 = d3_geo_cartesianCross(n1, n2), A = d3_geo_cartesianScale(n1, c1), B = d3_geo_cartesianScale(n2, c2);
	      d3_geo_cartesianAdd(A, B);
	      var u = n1xn2, w = d3_geo_cartesianDot(A, u), uu = d3_geo_cartesianDot(u, u), t2 = w * w - uu * (d3_geo_cartesianDot(A, A) - 1);
	      if (t2 < 0) return;
	      var t = Math.sqrt(t2), q = d3_geo_cartesianScale(u, (-w - t) / uu);
	      d3_geo_cartesianAdd(q, A);
	      q = d3_geo_spherical(q);
	      if (!two) return q;
	      var λ0 = a[0], λ1 = b[0], φ0 = a[1], φ1 = b[1], z;
	      if (λ1 < λ0) z = λ0, λ0 = λ1, λ1 = z;
	      var δλ = λ1 - λ0, polar = abs(δλ - π) < ε, meridian = polar || δλ < ε;
	      if (!polar && φ1 < φ0) z = φ0, φ0 = φ1, φ1 = z;
	      if (meridian ? polar ? φ0 + φ1 > 0 ^ q[1] < (abs(q[0] - λ0) < ε ? φ0 : φ1) : φ0 <= q[1] && q[1] <= φ1 : δλ > π ^ (λ0 <= q[0] && q[0] <= λ1)) {
	        var q1 = d3_geo_cartesianScale(u, (-w + t) / uu);
	        d3_geo_cartesianAdd(q1, A);
	        return [ q, d3_geo_spherical(q1) ];
	      }
	    }
	    function code(λ, φ) {
	      var r = smallRadius ? radius : π - radius, code = 0;
	      if (λ < -r) code |= 1; else if (λ > r) code |= 2;
	      if (φ < -r) code |= 4; else if (φ > r) code |= 8;
	      return code;
	    }
	  }
	  function d3_geom_clipLine(x0, y0, x1, y1) {
	    return function(line) {
	      var a = line.a, b = line.b, ax = a.x, ay = a.y, bx = b.x, by = b.y, t0 = 0, t1 = 1, dx = bx - ax, dy = by - ay, r;
	      r = x0 - ax;
	      if (!dx && r > 0) return;
	      r /= dx;
	      if (dx < 0) {
	        if (r < t0) return;
	        if (r < t1) t1 = r;
	      } else if (dx > 0) {
	        if (r > t1) return;
	        if (r > t0) t0 = r;
	      }
	      r = x1 - ax;
	      if (!dx && r < 0) return;
	      r /= dx;
	      if (dx < 0) {
	        if (r > t1) return;
	        if (r > t0) t0 = r;
	      } else if (dx > 0) {
	        if (r < t0) return;
	        if (r < t1) t1 = r;
	      }
	      r = y0 - ay;
	      if (!dy && r > 0) return;
	      r /= dy;
	      if (dy < 0) {
	        if (r < t0) return;
	        if (r < t1) t1 = r;
	      } else if (dy > 0) {
	        if (r > t1) return;
	        if (r > t0) t0 = r;
	      }
	      r = y1 - ay;
	      if (!dy && r < 0) return;
	      r /= dy;
	      if (dy < 0) {
	        if (r > t1) return;
	        if (r > t0) t0 = r;
	      } else if (dy > 0) {
	        if (r < t0) return;
	        if (r < t1) t1 = r;
	      }
	      if (t0 > 0) line.a = {
	        x: ax + t0 * dx,
	        y: ay + t0 * dy
	      };
	      if (t1 < 1) line.b = {
	        x: ax + t1 * dx,
	        y: ay + t1 * dy
	      };
	      return line;
	    };
	  }
	  var d3_geo_clipExtentMAX = 1e9;
	  d3.geo.clipExtent = function() {
	    var x0, y0, x1, y1, stream, clip, clipExtent = {
	      stream: function(output) {
	        if (stream) stream.valid = false;
	        stream = clip(output);
	        stream.valid = true;
	        return stream;
	      },
	      extent: function(_) {
	        if (!arguments.length) return [ [ x0, y0 ], [ x1, y1 ] ];
	        clip = d3_geo_clipExtent(x0 = +_[0][0], y0 = +_[0][1], x1 = +_[1][0], y1 = +_[1][1]);
	        if (stream) stream.valid = false, stream = null;
	        return clipExtent;
	      }
	    };
	    return clipExtent.extent([ [ 0, 0 ], [ 960, 500 ] ]);
	  };
	  function d3_geo_clipExtent(x0, y0, x1, y1) {
	    return function(listener) {
	      var listener_ = listener, bufferListener = d3_geo_clipBufferListener(), clipLine = d3_geom_clipLine(x0, y0, x1, y1), segments, polygon, ring;
	      var clip = {
	        point: point,
	        lineStart: lineStart,
	        lineEnd: lineEnd,
	        polygonStart: function() {
	          listener = bufferListener;
	          segments = [];
	          polygon = [];
	          clean = true;
	        },
	        polygonEnd: function() {
	          listener = listener_;
	          segments = d3.merge(segments);
	          var clipStartInside = insidePolygon([ x0, y1 ]), inside = clean && clipStartInside, visible = segments.length;
	          if (inside || visible) {
	            listener.polygonStart();
	            if (inside) {
	              listener.lineStart();
	              interpolate(null, null, 1, listener);
	              listener.lineEnd();
	            }
	            if (visible) {
	              d3_geo_clipPolygon(segments, compare, clipStartInside, interpolate, listener);
	            }
	            listener.polygonEnd();
	          }
	          segments = polygon = ring = null;
	        }
	      };
	      function insidePolygon(p) {
	        var wn = 0, n = polygon.length, y = p[1];
	        for (var i = 0; i < n; ++i) {
	          for (var j = 1, v = polygon[i], m = v.length, a = v[0], b; j < m; ++j) {
	            b = v[j];
	            if (a[1] <= y) {
	              if (b[1] > y && d3_cross2d(a, b, p) > 0) ++wn;
	            } else {
	              if (b[1] <= y && d3_cross2d(a, b, p) < 0) --wn;
	            }
	            a = b;
	          }
	        }
	        return wn !== 0;
	      }
	      function interpolate(from, to, direction, listener) {
	        var a = 0, a1 = 0;
	        if (from == null || (a = corner(from, direction)) !== (a1 = corner(to, direction)) || comparePoints(from, to) < 0 ^ direction > 0) {
	          do {
	            listener.point(a === 0 || a === 3 ? x0 : x1, a > 1 ? y1 : y0);
	          } while ((a = (a + direction + 4) % 4) !== a1);
	        } else {
	          listener.point(to[0], to[1]);
	        }
	      }
	      function pointVisible(x, y) {
	        return x0 <= x && x <= x1 && y0 <= y && y <= y1;
	      }
	      function point(x, y) {
	        if (pointVisible(x, y)) listener.point(x, y);
	      }
	      var x__, y__, v__, x_, y_, v_, first, clean;
	      function lineStart() {
	        clip.point = linePoint;
	        if (polygon) polygon.push(ring = []);
	        first = true;
	        v_ = false;
	        x_ = y_ = NaN;
	      }
	      function lineEnd() {
	        if (segments) {
	          linePoint(x__, y__);
	          if (v__ && v_) bufferListener.rejoin();
	          segments.push(bufferListener.buffer());
	        }
	        clip.point = point;
	        if (v_) listener.lineEnd();
	      }
	      function linePoint(x, y) {
	        x = Math.max(-d3_geo_clipExtentMAX, Math.min(d3_geo_clipExtentMAX, x));
	        y = Math.max(-d3_geo_clipExtentMAX, Math.min(d3_geo_clipExtentMAX, y));
	        var v = pointVisible(x, y);
	        if (polygon) ring.push([ x, y ]);
	        if (first) {
	          x__ = x, y__ = y, v__ = v;
	          first = false;
	          if (v) {
	            listener.lineStart();
	            listener.point(x, y);
	          }
	        } else {
	          if (v && v_) listener.point(x, y); else {
	            var l = {
	              a: {
	                x: x_,
	                y: y_
	              },
	              b: {
	                x: x,
	                y: y
	              }
	            };
	            if (clipLine(l)) {
	              if (!v_) {
	                listener.lineStart();
	                listener.point(l.a.x, l.a.y);
	              }
	              listener.point(l.b.x, l.b.y);
	              if (!v) listener.lineEnd();
	              clean = false;
	            } else if (v) {
	              listener.lineStart();
	              listener.point(x, y);
	              clean = false;
	            }
	          }
	        }
	        x_ = x, y_ = y, v_ = v;
	      }
	      return clip;
	    };
	    function corner(p, direction) {
	      return abs(p[0] - x0) < ε ? direction > 0 ? 0 : 3 : abs(p[0] - x1) < ε ? direction > 0 ? 2 : 1 : abs(p[1] - y0) < ε ? direction > 0 ? 1 : 0 : direction > 0 ? 3 : 2;
	    }
	    function compare(a, b) {
	      return comparePoints(a.x, b.x);
	    }
	    function comparePoints(a, b) {
	      var ca = corner(a, 1), cb = corner(b, 1);
	      return ca !== cb ? ca - cb : ca === 0 ? b[1] - a[1] : ca === 1 ? a[0] - b[0] : ca === 2 ? a[1] - b[1] : b[0] - a[0];
	    }
	  }
	  function d3_geo_conic(projectAt) {
	    var φ0 = 0, φ1 = π / 3, m = d3_geo_projectionMutator(projectAt), p = m(φ0, φ1);
	    p.parallels = function(_) {
	      if (!arguments.length) return [ φ0 / π * 180, φ1 / π * 180 ];
	      return m(φ0 = _[0] * π / 180, φ1 = _[1] * π / 180);
	    };
	    return p;
	  }
	  function d3_geo_conicEqualArea(φ0, φ1) {
	    var sinφ0 = Math.sin(φ0), n = (sinφ0 + Math.sin(φ1)) / 2, C = 1 + sinφ0 * (2 * n - sinφ0), ρ0 = Math.sqrt(C) / n;
	    function forward(λ, φ) {
	      var ρ = Math.sqrt(C - 2 * n * Math.sin(φ)) / n;
	      return [ ρ * Math.sin(λ *= n), ρ0 - ρ * Math.cos(λ) ];
	    }
	    forward.invert = function(x, y) {
	      var ρ0_y = ρ0 - y;
	      return [ Math.atan2(x, ρ0_y) / n, d3_asin((C - (x * x + ρ0_y * ρ0_y) * n * n) / (2 * n)) ];
	    };
	    return forward;
	  }
	  (d3.geo.conicEqualArea = function() {
	    return d3_geo_conic(d3_geo_conicEqualArea);
	  }).raw = d3_geo_conicEqualArea;
	  d3.geo.albers = function() {
	    return d3.geo.conicEqualArea().rotate([ 96, 0 ]).center([ -.6, 38.7 ]).parallels([ 29.5, 45.5 ]).scale(1070);
	  };
	  d3.geo.albersUsa = function() {
	    var lower48 = d3.geo.albers();
	    var alaska = d3.geo.conicEqualArea().rotate([ 154, 0 ]).center([ -2, 58.5 ]).parallels([ 55, 65 ]);
	    var hawaii = d3.geo.conicEqualArea().rotate([ 157, 0 ]).center([ -3, 19.9 ]).parallels([ 8, 18 ]);
	    var point, pointStream = {
	      point: function(x, y) {
	        point = [ x, y ];
	      }
	    }, lower48Point, alaskaPoint, hawaiiPoint;
	    function albersUsa(coordinates) {
	      var x = coordinates[0], y = coordinates[1];
	      point = null;
	      (lower48Point(x, y), point) || (alaskaPoint(x, y), point) || hawaiiPoint(x, y);
	      return point;
	    }
	    albersUsa.invert = function(coordinates) {
	      var k = lower48.scale(), t = lower48.translate(), x = (coordinates[0] - t[0]) / k, y = (coordinates[1] - t[1]) / k;
	      return (y >= .12 && y < .234 && x >= -.425 && x < -.214 ? alaska : y >= .166 && y < .234 && x >= -.214 && x < -.115 ? hawaii : lower48).invert(coordinates);
	    };
	    albersUsa.stream = function(stream) {
	      var lower48Stream = lower48.stream(stream), alaskaStream = alaska.stream(stream), hawaiiStream = hawaii.stream(stream);
	      return {
	        point: function(x, y) {
	          lower48Stream.point(x, y);
	          alaskaStream.point(x, y);
	          hawaiiStream.point(x, y);
	        },
	        sphere: function() {
	          lower48Stream.sphere();
	          alaskaStream.sphere();
	          hawaiiStream.sphere();
	        },
	        lineStart: function() {
	          lower48Stream.lineStart();
	          alaskaStream.lineStart();
	          hawaiiStream.lineStart();
	        },
	        lineEnd: function() {
	          lower48Stream.lineEnd();
	          alaskaStream.lineEnd();
	          hawaiiStream.lineEnd();
	        },
	        polygonStart: function() {
	          lower48Stream.polygonStart();
	          alaskaStream.polygonStart();
	          hawaiiStream.polygonStart();
	        },
	        polygonEnd: function() {
	          lower48Stream.polygonEnd();
	          alaskaStream.polygonEnd();
	          hawaiiStream.polygonEnd();
	        }
	      };
	    };
	    albersUsa.precision = function(_) {
	      if (!arguments.length) return lower48.precision();
	      lower48.precision(_);
	      alaska.precision(_);
	      hawaii.precision(_);
	      return albersUsa;
	    };
	    albersUsa.scale = function(_) {
	      if (!arguments.length) return lower48.scale();
	      lower48.scale(_);
	      alaska.scale(_ * .35);
	      hawaii.scale(_);
	      return albersUsa.translate(lower48.translate());
	    };
	    albersUsa.translate = function(_) {
	      if (!arguments.length) return lower48.translate();
	      var k = lower48.scale(), x = +_[0], y = +_[1];
	      lower48Point = lower48.translate(_).clipExtent([ [ x - .455 * k, y - .238 * k ], [ x + .455 * k, y + .238 * k ] ]).stream(pointStream).point;
	      alaskaPoint = alaska.translate([ x - .307 * k, y + .201 * k ]).clipExtent([ [ x - .425 * k + ε, y + .12 * k + ε ], [ x - .214 * k - ε, y + .234 * k - ε ] ]).stream(pointStream).point;
	      hawaiiPoint = hawaii.translate([ x - .205 * k, y + .212 * k ]).clipExtent([ [ x - .214 * k + ε, y + .166 * k + ε ], [ x - .115 * k - ε, y + .234 * k - ε ] ]).stream(pointStream).point;
	      return albersUsa;
	    };
	    return albersUsa.scale(1070);
	  };
	  var d3_geo_pathAreaSum, d3_geo_pathAreaPolygon, d3_geo_pathArea = {
	    point: d3_noop,
	    lineStart: d3_noop,
	    lineEnd: d3_noop,
	    polygonStart: function() {
	      d3_geo_pathAreaPolygon = 0;
	      d3_geo_pathArea.lineStart = d3_geo_pathAreaRingStart;
	    },
	    polygonEnd: function() {
	      d3_geo_pathArea.lineStart = d3_geo_pathArea.lineEnd = d3_geo_pathArea.point = d3_noop;
	      d3_geo_pathAreaSum += abs(d3_geo_pathAreaPolygon / 2);
	    }
	  };
	  function d3_geo_pathAreaRingStart() {
	    var x00, y00, x0, y0;
	    d3_geo_pathArea.point = function(x, y) {
	      d3_geo_pathArea.point = nextPoint;
	      x00 = x0 = x, y00 = y0 = y;
	    };
	    function nextPoint(x, y) {
	      d3_geo_pathAreaPolygon += y0 * x - x0 * y;
	      x0 = x, y0 = y;
	    }
	    d3_geo_pathArea.lineEnd = function() {
	      nextPoint(x00, y00);
	    };
	  }
	  var d3_geo_pathBoundsX0, d3_geo_pathBoundsY0, d3_geo_pathBoundsX1, d3_geo_pathBoundsY1;
	  var d3_geo_pathBounds = {
	    point: d3_geo_pathBoundsPoint,
	    lineStart: d3_noop,
	    lineEnd: d3_noop,
	    polygonStart: d3_noop,
	    polygonEnd: d3_noop
	  };
	  function d3_geo_pathBoundsPoint(x, y) {
	    if (x < d3_geo_pathBoundsX0) d3_geo_pathBoundsX0 = x;
	    if (x > d3_geo_pathBoundsX1) d3_geo_pathBoundsX1 = x;
	    if (y < d3_geo_pathBoundsY0) d3_geo_pathBoundsY0 = y;
	    if (y > d3_geo_pathBoundsY1) d3_geo_pathBoundsY1 = y;
	  }
	  function d3_geo_pathBuffer() {
	    var pointCircle = d3_geo_pathBufferCircle(4.5), buffer = [];
	    var stream = {
	      point: point,
	      lineStart: function() {
	        stream.point = pointLineStart;
	      },
	      lineEnd: lineEnd,
	      polygonStart: function() {
	        stream.lineEnd = lineEndPolygon;
	      },
	      polygonEnd: function() {
	        stream.lineEnd = lineEnd;
	        stream.point = point;
	      },
	      pointRadius: function(_) {
	        pointCircle = d3_geo_pathBufferCircle(_);
	        return stream;
	      },
	      result: function() {
	        if (buffer.length) {
	          var result = buffer.join("");
	          buffer = [];
	          return result;
	        }
	      }
	    };
	    function point(x, y) {
	      buffer.push("M", x, ",", y, pointCircle);
	    }
	    function pointLineStart(x, y) {
	      buffer.push("M", x, ",", y);
	      stream.point = pointLine;
	    }
	    function pointLine(x, y) {
	      buffer.push("L", x, ",", y);
	    }
	    function lineEnd() {
	      stream.point = point;
	    }
	    function lineEndPolygon() {
	      buffer.push("Z");
	    }
	    return stream;
	  }
	  function d3_geo_pathBufferCircle(radius) {
	    return "m0," + radius + "a" + radius + "," + radius + " 0 1,1 0," + -2 * radius + "a" + radius + "," + radius + " 0 1,1 0," + 2 * radius + "z";
	  }
	  var d3_geo_pathCentroid = {
	    point: d3_geo_pathCentroidPoint,
	    lineStart: d3_geo_pathCentroidLineStart,
	    lineEnd: d3_geo_pathCentroidLineEnd,
	    polygonStart: function() {
	      d3_geo_pathCentroid.lineStart = d3_geo_pathCentroidRingStart;
	    },
	    polygonEnd: function() {
	      d3_geo_pathCentroid.point = d3_geo_pathCentroidPoint;
	      d3_geo_pathCentroid.lineStart = d3_geo_pathCentroidLineStart;
	      d3_geo_pathCentroid.lineEnd = d3_geo_pathCentroidLineEnd;
	    }
	  };
	  function d3_geo_pathCentroidPoint(x, y) {
	    d3_geo_centroidX0 += x;
	    d3_geo_centroidY0 += y;
	    ++d3_geo_centroidZ0;
	  }
	  function d3_geo_pathCentroidLineStart() {
	    var x0, y0;
	    d3_geo_pathCentroid.point = function(x, y) {
	      d3_geo_pathCentroid.point = nextPoint;
	      d3_geo_pathCentroidPoint(x0 = x, y0 = y);
	    };
	    function nextPoint(x, y) {
	      var dx = x - x0, dy = y - y0, z = Math.sqrt(dx * dx + dy * dy);
	      d3_geo_centroidX1 += z * (x0 + x) / 2;
	      d3_geo_centroidY1 += z * (y0 + y) / 2;
	      d3_geo_centroidZ1 += z;
	      d3_geo_pathCentroidPoint(x0 = x, y0 = y);
	    }
	  }
	  function d3_geo_pathCentroidLineEnd() {
	    d3_geo_pathCentroid.point = d3_geo_pathCentroidPoint;
	  }
	  function d3_geo_pathCentroidRingStart() {
	    var x00, y00, x0, y0;
	    d3_geo_pathCentroid.point = function(x, y) {
	      d3_geo_pathCentroid.point = nextPoint;
	      d3_geo_pathCentroidPoint(x00 = x0 = x, y00 = y0 = y);
	    };
	    function nextPoint(x, y) {
	      var dx = x - x0, dy = y - y0, z = Math.sqrt(dx * dx + dy * dy);
	      d3_geo_centroidX1 += z * (x0 + x) / 2;
	      d3_geo_centroidY1 += z * (y0 + y) / 2;
	      d3_geo_centroidZ1 += z;
	      z = y0 * x - x0 * y;
	      d3_geo_centroidX2 += z * (x0 + x);
	      d3_geo_centroidY2 += z * (y0 + y);
	      d3_geo_centroidZ2 += z * 3;
	      d3_geo_pathCentroidPoint(x0 = x, y0 = y);
	    }
	    d3_geo_pathCentroid.lineEnd = function() {
	      nextPoint(x00, y00);
	    };
	  }
	  function d3_geo_pathContext(context) {
	    var pointRadius = 4.5;
	    var stream = {
	      point: point,
	      lineStart: function() {
	        stream.point = pointLineStart;
	      },
	      lineEnd: lineEnd,
	      polygonStart: function() {
	        stream.lineEnd = lineEndPolygon;
	      },
	      polygonEnd: function() {
	        stream.lineEnd = lineEnd;
	        stream.point = point;
	      },
	      pointRadius: function(_) {
	        pointRadius = _;
	        return stream;
	      },
	      result: d3_noop
	    };
	    function point(x, y) {
	      context.moveTo(x + pointRadius, y);
	      context.arc(x, y, pointRadius, 0, τ);
	    }
	    function pointLineStart(x, y) {
	      context.moveTo(x, y);
	      stream.point = pointLine;
	    }
	    function pointLine(x, y) {
	      context.lineTo(x, y);
	    }
	    function lineEnd() {
	      stream.point = point;
	    }
	    function lineEndPolygon() {
	      context.closePath();
	    }
	    return stream;
	  }
	  function d3_geo_resample(project) {
	    var δ2 = .5, cosMinDistance = Math.cos(30 * d3_radians), maxDepth = 16;
	    function resample(stream) {
	      return (maxDepth ? resampleRecursive : resampleNone)(stream);
	    }
	    function resampleNone(stream) {
	      return d3_geo_transformPoint(stream, function(x, y) {
	        x = project(x, y);
	        stream.point(x[0], x[1]);
	      });
	    }
	    function resampleRecursive(stream) {
	      var λ00, φ00, x00, y00, a00, b00, c00, λ0, x0, y0, a0, b0, c0;
	      var resample = {
	        point: point,
	        lineStart: lineStart,
	        lineEnd: lineEnd,
	        polygonStart: function() {
	          stream.polygonStart();
	          resample.lineStart = ringStart;
	        },
	        polygonEnd: function() {
	          stream.polygonEnd();
	          resample.lineStart = lineStart;
	        }
	      };
	      function point(x, y) {
	        x = project(x, y);
	        stream.point(x[0], x[1]);
	      }
	      function lineStart() {
	        x0 = NaN;
	        resample.point = linePoint;
	        stream.lineStart();
	      }
	      function linePoint(λ, φ) {
	        var c = d3_geo_cartesian([ λ, φ ]), p = project(λ, φ);
	        resampleLineTo(x0, y0, λ0, a0, b0, c0, x0 = p[0], y0 = p[1], λ0 = λ, a0 = c[0], b0 = c[1], c0 = c[2], maxDepth, stream);
	        stream.point(x0, y0);
	      }
	      function lineEnd() {
	        resample.point = point;
	        stream.lineEnd();
	      }
	      function ringStart() {
	        lineStart();
	        resample.point = ringPoint;
	        resample.lineEnd = ringEnd;
	      }
	      function ringPoint(λ, φ) {
	        linePoint(λ00 = λ, φ00 = φ), x00 = x0, y00 = y0, a00 = a0, b00 = b0, c00 = c0;
	        resample.point = linePoint;
	      }
	      function ringEnd() {
	        resampleLineTo(x0, y0, λ0, a0, b0, c0, x00, y00, λ00, a00, b00, c00, maxDepth, stream);
	        resample.lineEnd = lineEnd;
	        lineEnd();
	      }
	      return resample;
	    }
	    function resampleLineTo(x0, y0, λ0, a0, b0, c0, x1, y1, λ1, a1, b1, c1, depth, stream) {
	      var dx = x1 - x0, dy = y1 - y0, d2 = dx * dx + dy * dy;
	      if (d2 > 4 * δ2 && depth--) {
	        var a = a0 + a1, b = b0 + b1, c = c0 + c1, m = Math.sqrt(a * a + b * b + c * c), φ2 = Math.asin(c /= m), λ2 = abs(abs(c) - 1) < ε || abs(λ0 - λ1) < ε ? (λ0 + λ1) / 2 : Math.atan2(b, a), p = project(λ2, φ2), x2 = p[0], y2 = p[1], dx2 = x2 - x0, dy2 = y2 - y0, dz = dy * dx2 - dx * dy2;
	        if (dz * dz / d2 > δ2 || abs((dx * dx2 + dy * dy2) / d2 - .5) > .3 || a0 * a1 + b0 * b1 + c0 * c1 < cosMinDistance) {
	          resampleLineTo(x0, y0, λ0, a0, b0, c0, x2, y2, λ2, a /= m, b /= m, c, depth, stream);
	          stream.point(x2, y2);
	          resampleLineTo(x2, y2, λ2, a, b, c, x1, y1, λ1, a1, b1, c1, depth, stream);
	        }
	      }
	    }
	    resample.precision = function(_) {
	      if (!arguments.length) return Math.sqrt(δ2);
	      maxDepth = (δ2 = _ * _) > 0 && 16;
	      return resample;
	    };
	    return resample;
	  }
	  d3.geo.path = function() {
	    var pointRadius = 4.5, projection, context, projectStream, contextStream, cacheStream;
	    function path(object) {
	      if (object) {
	        if (typeof pointRadius === "function") contextStream.pointRadius(+pointRadius.apply(this, arguments));
	        if (!cacheStream || !cacheStream.valid) cacheStream = projectStream(contextStream);
	        d3.geo.stream(object, cacheStream);
	      }
	      return contextStream.result();
	    }
	    path.area = function(object) {
	      d3_geo_pathAreaSum = 0;
	      d3.geo.stream(object, projectStream(d3_geo_pathArea));
	      return d3_geo_pathAreaSum;
	    };
	    path.centroid = function(object) {
	      d3_geo_centroidX0 = d3_geo_centroidY0 = d3_geo_centroidZ0 = d3_geo_centroidX1 = d3_geo_centroidY1 = d3_geo_centroidZ1 = d3_geo_centroidX2 = d3_geo_centroidY2 = d3_geo_centroidZ2 = 0;
	      d3.geo.stream(object, projectStream(d3_geo_pathCentroid));
	      return d3_geo_centroidZ2 ? [ d3_geo_centroidX2 / d3_geo_centroidZ2, d3_geo_centroidY2 / d3_geo_centroidZ2 ] : d3_geo_centroidZ1 ? [ d3_geo_centroidX1 / d3_geo_centroidZ1, d3_geo_centroidY1 / d3_geo_centroidZ1 ] : d3_geo_centroidZ0 ? [ d3_geo_centroidX0 / d3_geo_centroidZ0, d3_geo_centroidY0 / d3_geo_centroidZ0 ] : [ NaN, NaN ];
	    };
	    path.bounds = function(object) {
	      d3_geo_pathBoundsX1 = d3_geo_pathBoundsY1 = -(d3_geo_pathBoundsX0 = d3_geo_pathBoundsY0 = Infinity);
	      d3.geo.stream(object, projectStream(d3_geo_pathBounds));
	      return [ [ d3_geo_pathBoundsX0, d3_geo_pathBoundsY0 ], [ d3_geo_pathBoundsX1, d3_geo_pathBoundsY1 ] ];
	    };
	    path.projection = function(_) {
	      if (!arguments.length) return projection;
	      projectStream = (projection = _) ? _.stream || d3_geo_pathProjectStream(_) : d3_identity;
	      return reset();
	    };
	    path.context = function(_) {
	      if (!arguments.length) return context;
	      contextStream = (context = _) == null ? new d3_geo_pathBuffer() : new d3_geo_pathContext(_);
	      if (typeof pointRadius !== "function") contextStream.pointRadius(pointRadius);
	      return reset();
	    };
	    path.pointRadius = function(_) {
	      if (!arguments.length) return pointRadius;
	      pointRadius = typeof _ === "function" ? _ : (contextStream.pointRadius(+_), +_);
	      return path;
	    };
	    function reset() {
	      cacheStream = null;
	      return path;
	    }
	    return path.projection(d3.geo.albersUsa()).context(null);
	  };
	  function d3_geo_pathProjectStream(project) {
	    var resample = d3_geo_resample(function(x, y) {
	      return project([ x * d3_degrees, y * d3_degrees ]);
	    });
	    return function(stream) {
	      return d3_geo_projectionRadians(resample(stream));
	    };
	  }
	  d3.geo.transform = function(methods) {
	    return {
	      stream: function(stream) {
	        var transform = new d3_geo_transform(stream);
	        for (var k in methods) transform[k] = methods[k];
	        return transform;
	      }
	    };
	  };
	  function d3_geo_transform(stream) {
	    this.stream = stream;
	  }
	  d3_geo_transform.prototype = {
	    point: function(x, y) {
	      this.stream.point(x, y);
	    },
	    sphere: function() {
	      this.stream.sphere();
	    },
	    lineStart: function() {
	      this.stream.lineStart();
	    },
	    lineEnd: function() {
	      this.stream.lineEnd();
	    },
	    polygonStart: function() {
	      this.stream.polygonStart();
	    },
	    polygonEnd: function() {
	      this.stream.polygonEnd();
	    }
	  };
	  function d3_geo_transformPoint(stream, point) {
	    return {
	      point: point,
	      sphere: function() {
	        stream.sphere();
	      },
	      lineStart: function() {
	        stream.lineStart();
	      },
	      lineEnd: function() {
	        stream.lineEnd();
	      },
	      polygonStart: function() {
	        stream.polygonStart();
	      },
	      polygonEnd: function() {
	        stream.polygonEnd();
	      }
	    };
	  }
	  d3.geo.projection = d3_geo_projection;
	  d3.geo.projectionMutator = d3_geo_projectionMutator;
	  function d3_geo_projection(project) {
	    return d3_geo_projectionMutator(function() {
	      return project;
	    })();
	  }
	  function d3_geo_projectionMutator(projectAt) {
	    var project, rotate, projectRotate, projectResample = d3_geo_resample(function(x, y) {
	      x = project(x, y);
	      return [ x[0] * k + δx, δy - x[1] * k ];
	    }), k = 150, x = 480, y = 250, λ = 0, φ = 0, δλ = 0, δφ = 0, δγ = 0, δx, δy, preclip = d3_geo_clipAntimeridian, postclip = d3_identity, clipAngle = null, clipExtent = null, stream;
	    function projection(point) {
	      point = projectRotate(point[0] * d3_radians, point[1] * d3_radians);
	      return [ point[0] * k + δx, δy - point[1] * k ];
	    }
	    function invert(point) {
	      point = projectRotate.invert((point[0] - δx) / k, (δy - point[1]) / k);
	      return point && [ point[0] * d3_degrees, point[1] * d3_degrees ];
	    }
	    projection.stream = function(output) {
	      if (stream) stream.valid = false;
	      stream = d3_geo_projectionRadians(preclip(rotate, projectResample(postclip(output))));
	      stream.valid = true;
	      return stream;
	    };
	    projection.clipAngle = function(_) {
	      if (!arguments.length) return clipAngle;
	      preclip = _ == null ? (clipAngle = _, d3_geo_clipAntimeridian) : d3_geo_clipCircle((clipAngle = +_) * d3_radians);
	      return invalidate();
	    };
	    projection.clipExtent = function(_) {
	      if (!arguments.length) return clipExtent;
	      clipExtent = _;
	      postclip = _ ? d3_geo_clipExtent(_[0][0], _[0][1], _[1][0], _[1][1]) : d3_identity;
	      return invalidate();
	    };
	    projection.scale = function(_) {
	      if (!arguments.length) return k;
	      k = +_;
	      return reset();
	    };
	    projection.translate = function(_) {
	      if (!arguments.length) return [ x, y ];
	      x = +_[0];
	      y = +_[1];
	      return reset();
	    };
	    projection.center = function(_) {
	      if (!arguments.length) return [ λ * d3_degrees, φ * d3_degrees ];
	      λ = _[0] % 360 * d3_radians;
	      φ = _[1] % 360 * d3_radians;
	      return reset();
	    };
	    projection.rotate = function(_) {
	      if (!arguments.length) return [ δλ * d3_degrees, δφ * d3_degrees, δγ * d3_degrees ];
	      δλ = _[0] % 360 * d3_radians;
	      δφ = _[1] % 360 * d3_radians;
	      δγ = _.length > 2 ? _[2] % 360 * d3_radians : 0;
	      return reset();
	    };
	    d3.rebind(projection, projectResample, "precision");
	    function reset() {
	      projectRotate = d3_geo_compose(rotate = d3_geo_rotation(δλ, δφ, δγ), project);
	      var center = project(λ, φ);
	      δx = x - center[0] * k;
	      δy = y + center[1] * k;
	      return invalidate();
	    }
	    function invalidate() {
	      if (stream) stream.valid = false, stream = null;
	      return projection;
	    }
	    return function() {
	      project = projectAt.apply(this, arguments);
	      projection.invert = project.invert && invert;
	      return reset();
	    };
	  }
	  function d3_geo_projectionRadians(stream) {
	    return d3_geo_transformPoint(stream, function(x, y) {
	      stream.point(x * d3_radians, y * d3_radians);
	    });
	  }
	  function d3_geo_equirectangular(λ, φ) {
	    return [ λ, φ ];
	  }
	  (d3.geo.equirectangular = function() {
	    return d3_geo_projection(d3_geo_equirectangular);
	  }).raw = d3_geo_equirectangular.invert = d3_geo_equirectangular;
	  d3.geo.rotation = function(rotate) {
	    rotate = d3_geo_rotation(rotate[0] % 360 * d3_radians, rotate[1] * d3_radians, rotate.length > 2 ? rotate[2] * d3_radians : 0);
	    function forward(coordinates) {
	      coordinates = rotate(coordinates[0] * d3_radians, coordinates[1] * d3_radians);
	      return coordinates[0] *= d3_degrees, coordinates[1] *= d3_degrees, coordinates;
	    }
	    forward.invert = function(coordinates) {
	      coordinates = rotate.invert(coordinates[0] * d3_radians, coordinates[1] * d3_radians);
	      return coordinates[0] *= d3_degrees, coordinates[1] *= d3_degrees, coordinates;
	    };
	    return forward;
	  };
	  function d3_geo_identityRotation(λ, φ) {
	    return [ λ > π ? λ - τ : λ < -π ? λ + τ : λ, φ ];
	  }
	  d3_geo_identityRotation.invert = d3_geo_equirectangular;
	  function d3_geo_rotation(δλ, δφ, δγ) {
	    return δλ ? δφ || δγ ? d3_geo_compose(d3_geo_rotationλ(δλ), d3_geo_rotationφγ(δφ, δγ)) : d3_geo_rotationλ(δλ) : δφ || δγ ? d3_geo_rotationφγ(δφ, δγ) : d3_geo_identityRotation;
	  }
	  function d3_geo_forwardRotationλ(δλ) {
	    return function(λ, φ) {
	      return λ += δλ, [ λ > π ? λ - τ : λ < -π ? λ + τ : λ, φ ];
	    };
	  }
	  function d3_geo_rotationλ(δλ) {
	    var rotation = d3_geo_forwardRotationλ(δλ);
	    rotation.invert = d3_geo_forwardRotationλ(-δλ);
	    return rotation;
	  }
	  function d3_geo_rotationφγ(δφ, δγ) {
	    var cosδφ = Math.cos(δφ), sinδφ = Math.sin(δφ), cosδγ = Math.cos(δγ), sinδγ = Math.sin(δγ);
	    function rotation(λ, φ) {
	      var cosφ = Math.cos(φ), x = Math.cos(λ) * cosφ, y = Math.sin(λ) * cosφ, z = Math.sin(φ), k = z * cosδφ + x * sinδφ;
	      return [ Math.atan2(y * cosδγ - k * sinδγ, x * cosδφ - z * sinδφ), d3_asin(k * cosδγ + y * sinδγ) ];
	    }
	    rotation.invert = function(λ, φ) {
	      var cosφ = Math.cos(φ), x = Math.cos(λ) * cosφ, y = Math.sin(λ) * cosφ, z = Math.sin(φ), k = z * cosδγ - y * sinδγ;
	      return [ Math.atan2(y * cosδγ + z * sinδγ, x * cosδφ + k * sinδφ), d3_asin(k * cosδφ - x * sinδφ) ];
	    };
	    return rotation;
	  }
	  d3.geo.circle = function() {
	    var origin = [ 0, 0 ], angle, precision = 6, interpolate;
	    function circle() {
	      var center = typeof origin === "function" ? origin.apply(this, arguments) : origin, rotate = d3_geo_rotation(-center[0] * d3_radians, -center[1] * d3_radians, 0).invert, ring = [];
	      interpolate(null, null, 1, {
	        point: function(x, y) {
	          ring.push(x = rotate(x, y));
	          x[0] *= d3_degrees, x[1] *= d3_degrees;
	        }
	      });
	      return {
	        type: "Polygon",
	        coordinates: [ ring ]
	      };
	    }
	    circle.origin = function(x) {
	      if (!arguments.length) return origin;
	      origin = x;
	      return circle;
	    };
	    circle.angle = function(x) {
	      if (!arguments.length) return angle;
	      interpolate = d3_geo_circleInterpolate((angle = +x) * d3_radians, precision * d3_radians);
	      return circle;
	    };
	    circle.precision = function(_) {
	      if (!arguments.length) return precision;
	      interpolate = d3_geo_circleInterpolate(angle * d3_radians, (precision = +_) * d3_radians);
	      return circle;
	    };
	    return circle.angle(90);
	  };
	  function d3_geo_circleInterpolate(radius, precision) {
	    var cr = Math.cos(radius), sr = Math.sin(radius);
	    return function(from, to, direction, listener) {
	      var step = direction * precision;
	      if (from != null) {
	        from = d3_geo_circleAngle(cr, from);
	        to = d3_geo_circleAngle(cr, to);
	        if (direction > 0 ? from < to : from > to) from += direction * τ;
	      } else {
	        from = radius + direction * τ;
	        to = radius - .5 * step;
	      }
	      for (var point, t = from; direction > 0 ? t > to : t < to; t -= step) {
	        listener.point((point = d3_geo_spherical([ cr, -sr * Math.cos(t), -sr * Math.sin(t) ]))[0], point[1]);
	      }
	    };
	  }
	  function d3_geo_circleAngle(cr, point) {
	    var a = d3_geo_cartesian(point);
	    a[0] -= cr;
	    d3_geo_cartesianNormalize(a);
	    var angle = d3_acos(-a[1]);
	    return ((-a[2] < 0 ? -angle : angle) + 2 * Math.PI - ε) % (2 * Math.PI);
	  }
	  d3.geo.distance = function(a, b) {
	    var Δλ = (b[0] - a[0]) * d3_radians, φ0 = a[1] * d3_radians, φ1 = b[1] * d3_radians, sinΔλ = Math.sin(Δλ), cosΔλ = Math.cos(Δλ), sinφ0 = Math.sin(φ0), cosφ0 = Math.cos(φ0), sinφ1 = Math.sin(φ1), cosφ1 = Math.cos(φ1), t;
	    return Math.atan2(Math.sqrt((t = cosφ1 * sinΔλ) * t + (t = cosφ0 * sinφ1 - sinφ0 * cosφ1 * cosΔλ) * t), sinφ0 * sinφ1 + cosφ0 * cosφ1 * cosΔλ);
	  };
	  d3.geo.graticule = function() {
	    var x1, x0, X1, X0, y1, y0, Y1, Y0, dx = 10, dy = dx, DX = 90, DY = 360, x, y, X, Y, precision = 2.5;
	    function graticule() {
	      return {
	        type: "MultiLineString",
	        coordinates: lines()
	      };
	    }
	    function lines() {
	      return d3.range(Math.ceil(X0 / DX) * DX, X1, DX).map(X).concat(d3.range(Math.ceil(Y0 / DY) * DY, Y1, DY).map(Y)).concat(d3.range(Math.ceil(x0 / dx) * dx, x1, dx).filter(function(x) {
	        return abs(x % DX) > ε;
	      }).map(x)).concat(d3.range(Math.ceil(y0 / dy) * dy, y1, dy).filter(function(y) {
	        return abs(y % DY) > ε;
	      }).map(y));
	    }
	    graticule.lines = function() {
	      return lines().map(function(coordinates) {
	        return {
	          type: "LineString",
	          coordinates: coordinates
	        };
	      });
	    };
	    graticule.outline = function() {
	      return {
	        type: "Polygon",
	        coordinates: [ X(X0).concat(Y(Y1).slice(1), X(X1).reverse().slice(1), Y(Y0).reverse().slice(1)) ]
	      };
	    };
	    graticule.extent = function(_) {
	      if (!arguments.length) return graticule.minorExtent();
	      return graticule.majorExtent(_).minorExtent(_);
	    };
	    graticule.majorExtent = function(_) {
	      if (!arguments.length) return [ [ X0, Y0 ], [ X1, Y1 ] ];
	      X0 = +_[0][0], X1 = +_[1][0];
	      Y0 = +_[0][1], Y1 = +_[1][1];
	      if (X0 > X1) _ = X0, X0 = X1, X1 = _;
	      if (Y0 > Y1) _ = Y0, Y0 = Y1, Y1 = _;
	      return graticule.precision(precision);
	    };
	    graticule.minorExtent = function(_) {
	      if (!arguments.length) return [ [ x0, y0 ], [ x1, y1 ] ];
	      x0 = +_[0][0], x1 = +_[1][0];
	      y0 = +_[0][1], y1 = +_[1][1];
	      if (x0 > x1) _ = x0, x0 = x1, x1 = _;
	      if (y0 > y1) _ = y0, y0 = y1, y1 = _;
	      return graticule.precision(precision);
	    };
	    graticule.step = function(_) {
	      if (!arguments.length) return graticule.minorStep();
	      return graticule.majorStep(_).minorStep(_);
	    };
	    graticule.majorStep = function(_) {
	      if (!arguments.length) return [ DX, DY ];
	      DX = +_[0], DY = +_[1];
	      return graticule;
	    };
	    graticule.minorStep = function(_) {
	      if (!arguments.length) return [ dx, dy ];
	      dx = +_[0], dy = +_[1];
	      return graticule;
	    };
	    graticule.precision = function(_) {
	      if (!arguments.length) return precision;
	      precision = +_;
	      x = d3_geo_graticuleX(y0, y1, 90);
	      y = d3_geo_graticuleY(x0, x1, precision);
	      X = d3_geo_graticuleX(Y0, Y1, 90);
	      Y = d3_geo_graticuleY(X0, X1, precision);
	      return graticule;
	    };
	    return graticule.majorExtent([ [ -180, -90 + ε ], [ 180, 90 - ε ] ]).minorExtent([ [ -180, -80 - ε ], [ 180, 80 + ε ] ]);
	  };
	  function d3_geo_graticuleX(y0, y1, dy) {
	    var y = d3.range(y0, y1 - ε, dy).concat(y1);
	    return function(x) {
	      return y.map(function(y) {
	        return [ x, y ];
	      });
	    };
	  }
	  function d3_geo_graticuleY(x0, x1, dx) {
	    var x = d3.range(x0, x1 - ε, dx).concat(x1);
	    return function(y) {
	      return x.map(function(x) {
	        return [ x, y ];
	      });
	    };
	  }
	  function d3_source(d) {
	    return d.source;
	  }
	  function d3_target(d) {
	    return d.target;
	  }
	  d3.geo.greatArc = function() {
	    var source = d3_source, source_, target = d3_target, target_;
	    function greatArc() {
	      return {
	        type: "LineString",
	        coordinates: [ source_ || source.apply(this, arguments), target_ || target.apply(this, arguments) ]
	      };
	    }
	    greatArc.distance = function() {
	      return d3.geo.distance(source_ || source.apply(this, arguments), target_ || target.apply(this, arguments));
	    };
	    greatArc.source = function(_) {
	      if (!arguments.length) return source;
	      source = _, source_ = typeof _ === "function" ? null : _;
	      return greatArc;
	    };
	    greatArc.target = function(_) {
	      if (!arguments.length) return target;
	      target = _, target_ = typeof _ === "function" ? null : _;
	      return greatArc;
	    };
	    greatArc.precision = function() {
	      return arguments.length ? greatArc : 0;
	    };
	    return greatArc;
	  };
	  d3.geo.interpolate = function(source, target) {
	    return d3_geo_interpolate(source[0] * d3_radians, source[1] * d3_radians, target[0] * d3_radians, target[1] * d3_radians);
	  };
	  function d3_geo_interpolate(x0, y0, x1, y1) {
	    var cy0 = Math.cos(y0), sy0 = Math.sin(y0), cy1 = Math.cos(y1), sy1 = Math.sin(y1), kx0 = cy0 * Math.cos(x0), ky0 = cy0 * Math.sin(x0), kx1 = cy1 * Math.cos(x1), ky1 = cy1 * Math.sin(x1), d = 2 * Math.asin(Math.sqrt(d3_haversin(y1 - y0) + cy0 * cy1 * d3_haversin(x1 - x0))), k = 1 / Math.sin(d);
	    var interpolate = d ? function(t) {
	      var B = Math.sin(t *= d) * k, A = Math.sin(d - t) * k, x = A * kx0 + B * kx1, y = A * ky0 + B * ky1, z = A * sy0 + B * sy1;
	      return [ Math.atan2(y, x) * d3_degrees, Math.atan2(z, Math.sqrt(x * x + y * y)) * d3_degrees ];
	    } : function() {
	      return [ x0 * d3_degrees, y0 * d3_degrees ];
	    };
	    interpolate.distance = d;
	    return interpolate;
	  }
	  d3.geo.length = function(object) {
	    d3_geo_lengthSum = 0;
	    d3.geo.stream(object, d3_geo_length);
	    return d3_geo_lengthSum;
	  };
	  var d3_geo_lengthSum;
	  var d3_geo_length = {
	    sphere: d3_noop,
	    point: d3_noop,
	    lineStart: d3_geo_lengthLineStart,
	    lineEnd: d3_noop,
	    polygonStart: d3_noop,
	    polygonEnd: d3_noop
	  };
	  function d3_geo_lengthLineStart() {
	    var λ0, sinφ0, cosφ0;
	    d3_geo_length.point = function(λ, φ) {
	      λ0 = λ * d3_radians, sinφ0 = Math.sin(φ *= d3_radians), cosφ0 = Math.cos(φ);
	      d3_geo_length.point = nextPoint;
	    };
	    d3_geo_length.lineEnd = function() {
	      d3_geo_length.point = d3_geo_length.lineEnd = d3_noop;
	    };
	    function nextPoint(λ, φ) {
	      var sinφ = Math.sin(φ *= d3_radians), cosφ = Math.cos(φ), t = abs((λ *= d3_radians) - λ0), cosΔλ = Math.cos(t);
	      d3_geo_lengthSum += Math.atan2(Math.sqrt((t = cosφ * Math.sin(t)) * t + (t = cosφ0 * sinφ - sinφ0 * cosφ * cosΔλ) * t), sinφ0 * sinφ + cosφ0 * cosφ * cosΔλ);
	      λ0 = λ, sinφ0 = sinφ, cosφ0 = cosφ;
	    }
	  }
	  function d3_geo_azimuthal(scale, angle) {
	    function azimuthal(λ, φ) {
	      var cosλ = Math.cos(λ), cosφ = Math.cos(φ), k = scale(cosλ * cosφ);
	      return [ k * cosφ * Math.sin(λ), k * Math.sin(φ) ];
	    }
	    azimuthal.invert = function(x, y) {
	      var ρ = Math.sqrt(x * x + y * y), c = angle(ρ), sinc = Math.sin(c), cosc = Math.cos(c);
	      return [ Math.atan2(x * sinc, ρ * cosc), Math.asin(ρ && y * sinc / ρ) ];
	    };
	    return azimuthal;
	  }
	  var d3_geo_azimuthalEqualArea = d3_geo_azimuthal(function(cosλcosφ) {
	    return Math.sqrt(2 / (1 + cosλcosφ));
	  }, function(ρ) {
	    return 2 * Math.asin(ρ / 2);
	  });
	  (d3.geo.azimuthalEqualArea = function() {
	    return d3_geo_projection(d3_geo_azimuthalEqualArea);
	  }).raw = d3_geo_azimuthalEqualArea;
	  var d3_geo_azimuthalEquidistant = d3_geo_azimuthal(function(cosλcosφ) {
	    var c = Math.acos(cosλcosφ);
	    return c && c / Math.sin(c);
	  }, d3_identity);
	  (d3.geo.azimuthalEquidistant = function() {
	    return d3_geo_projection(d3_geo_azimuthalEquidistant);
	  }).raw = d3_geo_azimuthalEquidistant;
	  function d3_geo_conicConformal(φ0, φ1) {
	    var cosφ0 = Math.cos(φ0), t = function(φ) {
	      return Math.tan(π / 4 + φ / 2);
	    }, n = φ0 === φ1 ? Math.sin(φ0) : Math.log(cosφ0 / Math.cos(φ1)) / Math.log(t(φ1) / t(φ0)), F = cosφ0 * Math.pow(t(φ0), n) / n;
	    if (!n) return d3_geo_mercator;
	    function forward(λ, φ) {
	      if (F > 0) {
	        if (φ < -halfπ + ε) φ = -halfπ + ε;
	      } else {
	        if (φ > halfπ - ε) φ = halfπ - ε;
	      }
	      var ρ = F / Math.pow(t(φ), n);
	      return [ ρ * Math.sin(n * λ), F - ρ * Math.cos(n * λ) ];
	    }
	    forward.invert = function(x, y) {
	      var ρ0_y = F - y, ρ = d3_sgn(n) * Math.sqrt(x * x + ρ0_y * ρ0_y);
	      return [ Math.atan2(x, ρ0_y) / n, 2 * Math.atan(Math.pow(F / ρ, 1 / n)) - halfπ ];
	    };
	    return forward;
	  }
	  (d3.geo.conicConformal = function() {
	    return d3_geo_conic(d3_geo_conicConformal);
	  }).raw = d3_geo_conicConformal;
	  function d3_geo_conicEquidistant(φ0, φ1) {
	    var cosφ0 = Math.cos(φ0), n = φ0 === φ1 ? Math.sin(φ0) : (cosφ0 - Math.cos(φ1)) / (φ1 - φ0), G = cosφ0 / n + φ0;
	    if (abs(n) < ε) return d3_geo_equirectangular;
	    function forward(λ, φ) {
	      var ρ = G - φ;
	      return [ ρ * Math.sin(n * λ), G - ρ * Math.cos(n * λ) ];
	    }
	    forward.invert = function(x, y) {
	      var ρ0_y = G - y;
	      return [ Math.atan2(x, ρ0_y) / n, G - d3_sgn(n) * Math.sqrt(x * x + ρ0_y * ρ0_y) ];
	    };
	    return forward;
	  }
	  (d3.geo.conicEquidistant = function() {
	    return d3_geo_conic(d3_geo_conicEquidistant);
	  }).raw = d3_geo_conicEquidistant;
	  var d3_geo_gnomonic = d3_geo_azimuthal(function(cosλcosφ) {
	    return 1 / cosλcosφ;
	  }, Math.atan);
	  (d3.geo.gnomonic = function() {
	    return d3_geo_projection(d3_geo_gnomonic);
	  }).raw = d3_geo_gnomonic;
	  function d3_geo_mercator(λ, φ) {
	    return [ λ, Math.log(Math.tan(π / 4 + φ / 2)) ];
	  }
	  d3_geo_mercator.invert = function(x, y) {
	    return [ x, 2 * Math.atan(Math.exp(y)) - halfπ ];
	  };
	  function d3_geo_mercatorProjection(project) {
	    var m = d3_geo_projection(project), scale = m.scale, translate = m.translate, clipExtent = m.clipExtent, clipAuto;
	    m.scale = function() {
	      var v = scale.apply(m, arguments);
	      return v === m ? clipAuto ? m.clipExtent(null) : m : v;
	    };
	    m.translate = function() {
	      var v = translate.apply(m, arguments);
	      return v === m ? clipAuto ? m.clipExtent(null) : m : v;
	    };
	    m.clipExtent = function(_) {
	      var v = clipExtent.apply(m, arguments);
	      if (v === m) {
	        if (clipAuto = _ == null) {
	          var k = π * scale(), t = translate();
	          clipExtent([ [ t[0] - k, t[1] - k ], [ t[0] + k, t[1] + k ] ]);
	        }
	      } else if (clipAuto) {
	        v = null;
	      }
	      return v;
	    };
	    return m.clipExtent(null);
	  }
	  (d3.geo.mercator = function() {
	    return d3_geo_mercatorProjection(d3_geo_mercator);
	  }).raw = d3_geo_mercator;
	  var d3_geo_orthographic = d3_geo_azimuthal(function() {
	    return 1;
	  }, Math.asin);
	  (d3.geo.orthographic = function() {
	    return d3_geo_projection(d3_geo_orthographic);
	  }).raw = d3_geo_orthographic;
	  var d3_geo_stereographic = d3_geo_azimuthal(function(cosλcosφ) {
	    return 1 / (1 + cosλcosφ);
	  }, function(ρ) {
	    return 2 * Math.atan(ρ);
	  });
	  (d3.geo.stereographic = function() {
	    return d3_geo_projection(d3_geo_stereographic);
	  }).raw = d3_geo_stereographic;
	  function d3_geo_transverseMercator(λ, φ) {
	    return [ Math.log(Math.tan(π / 4 + φ / 2)), -λ ];
	  }
	  d3_geo_transverseMercator.invert = function(x, y) {
	    return [ -y, 2 * Math.atan(Math.exp(x)) - halfπ ];
	  };
	  (d3.geo.transverseMercator = function() {
	    var projection = d3_geo_mercatorProjection(d3_geo_transverseMercator), center = projection.center, rotate = projection.rotate;
	    projection.center = function(_) {
	      return _ ? center([ -_[1], _[0] ]) : (_ = center(), [ _[1], -_[0] ]);
	    };
	    projection.rotate = function(_) {
	      return _ ? rotate([ _[0], _[1], _.length > 2 ? _[2] + 90 : 90 ]) : (_ = rotate(), 
	      [ _[0], _[1], _[2] - 90 ]);
	    };
	    return rotate([ 0, 0, 90 ]);
	  }).raw = d3_geo_transverseMercator;
	  d3.geom = {};
	  function d3_geom_pointX(d) {
	    return d[0];
	  }
	  function d3_geom_pointY(d) {
	    return d[1];
	  }
	  d3.geom.hull = function(vertices) {
	    var x = d3_geom_pointX, y = d3_geom_pointY;
	    if (arguments.length) return hull(vertices);
	    function hull(data) {
	      if (data.length < 3) return [];
	      var fx = d3_functor(x), fy = d3_functor(y), i, n = data.length, points = [], flippedPoints = [];
	      for (i = 0; i < n; i++) {
	        points.push([ +fx.call(this, data[i], i), +fy.call(this, data[i], i), i ]);
	      }
	      points.sort(d3_geom_hullOrder);
	      for (i = 0; i < n; i++) flippedPoints.push([ points[i][0], -points[i][1] ]);
	      var upper = d3_geom_hullUpper(points), lower = d3_geom_hullUpper(flippedPoints);
	      var skipLeft = lower[0] === upper[0], skipRight = lower[lower.length - 1] === upper[upper.length - 1], polygon = [];
	      for (i = upper.length - 1; i >= 0; --i) polygon.push(data[points[upper[i]][2]]);
	      for (i = +skipLeft; i < lower.length - skipRight; ++i) polygon.push(data[points[lower[i]][2]]);
	      return polygon;
	    }
	    hull.x = function(_) {
	      return arguments.length ? (x = _, hull) : x;
	    };
	    hull.y = function(_) {
	      return arguments.length ? (y = _, hull) : y;
	    };
	    return hull;
	  };
	  function d3_geom_hullUpper(points) {
	    var n = points.length, hull = [ 0, 1 ], hs = 2;
	    for (var i = 2; i < n; i++) {
	      while (hs > 1 && d3_cross2d(points[hull[hs - 2]], points[hull[hs - 1]], points[i]) <= 0) --hs;
	      hull[hs++] = i;
	    }
	    return hull.slice(0, hs);
	  }
	  function d3_geom_hullOrder(a, b) {
	    return a[0] - b[0] || a[1] - b[1];
	  }
	  d3.geom.polygon = function(coordinates) {
	    d3_subclass(coordinates, d3_geom_polygonPrototype);
	    return coordinates;
	  };
	  var d3_geom_polygonPrototype = d3.geom.polygon.prototype = [];
	  d3_geom_polygonPrototype.area = function() {
	    var i = -1, n = this.length, a, b = this[n - 1], area = 0;
	    while (++i < n) {
	      a = b;
	      b = this[i];
	      area += a[1] * b[0] - a[0] * b[1];
	    }
	    return area * .5;
	  };
	  d3_geom_polygonPrototype.centroid = function(k) {
	    var i = -1, n = this.length, x = 0, y = 0, a, b = this[n - 1], c;
	    if (!arguments.length) k = -1 / (6 * this.area());
	    while (++i < n) {
	      a = b;
	      b = this[i];
	      c = a[0] * b[1] - b[0] * a[1];
	      x += (a[0] + b[0]) * c;
	      y += (a[1] + b[1]) * c;
	    }
	    return [ x * k, y * k ];
	  };
	  d3_geom_polygonPrototype.clip = function(subject) {
	    var input, closed = d3_geom_polygonClosed(subject), i = -1, n = this.length - d3_geom_polygonClosed(this), j, m, a = this[n - 1], b, c, d;
	    while (++i < n) {
	      input = subject.slice();
	      subject.length = 0;
	      b = this[i];
	      c = input[(m = input.length - closed) - 1];
	      j = -1;
	      while (++j < m) {
	        d = input[j];
	        if (d3_geom_polygonInside(d, a, b)) {
	          if (!d3_geom_polygonInside(c, a, b)) {
	            subject.push(d3_geom_polygonIntersect(c, d, a, b));
	          }
	          subject.push(d);
	        } else if (d3_geom_polygonInside(c, a, b)) {
	          subject.push(d3_geom_polygonIntersect(c, d, a, b));
	        }
	        c = d;
	      }
	      if (closed) subject.push(subject[0]);
	      a = b;
	    }
	    return subject;
	  };
	  function d3_geom_polygonInside(p, a, b) {
	    return (b[0] - a[0]) * (p[1] - a[1]) < (b[1] - a[1]) * (p[0] - a[0]);
	  }
	  function d3_geom_polygonIntersect(c, d, a, b) {
	    var x1 = c[0], x3 = a[0], x21 = d[0] - x1, x43 = b[0] - x3, y1 = c[1], y3 = a[1], y21 = d[1] - y1, y43 = b[1] - y3, ua = (x43 * (y1 - y3) - y43 * (x1 - x3)) / (y43 * x21 - x43 * y21);
	    return [ x1 + ua * x21, y1 + ua * y21 ];
	  }
	  function d3_geom_polygonClosed(coordinates) {
	    var a = coordinates[0], b = coordinates[coordinates.length - 1];
	    return !(a[0] - b[0] || a[1] - b[1]);
	  }
	  var d3_geom_voronoiEdges, d3_geom_voronoiCells, d3_geom_voronoiBeaches, d3_geom_voronoiBeachPool = [], d3_geom_voronoiFirstCircle, d3_geom_voronoiCircles, d3_geom_voronoiCirclePool = [];
	  function d3_geom_voronoiBeach() {
	    d3_geom_voronoiRedBlackNode(this);
	    this.edge = this.site = this.circle = null;
	  }
	  function d3_geom_voronoiCreateBeach(site) {
	    var beach = d3_geom_voronoiBeachPool.pop() || new d3_geom_voronoiBeach();
	    beach.site = site;
	    return beach;
	  }
	  function d3_geom_voronoiDetachBeach(beach) {
	    d3_geom_voronoiDetachCircle(beach);
	    d3_geom_voronoiBeaches.remove(beach);
	    d3_geom_voronoiBeachPool.push(beach);
	    d3_geom_voronoiRedBlackNode(beach);
	  }
	  function d3_geom_voronoiRemoveBeach(beach) {
	    var circle = beach.circle, x = circle.x, y = circle.cy, vertex = {
	      x: x,
	      y: y
	    }, previous = beach.P, next = beach.N, disappearing = [ beach ];
	    d3_geom_voronoiDetachBeach(beach);
	    var lArc = previous;
	    while (lArc.circle && abs(x - lArc.circle.x) < ε && abs(y - lArc.circle.cy) < ε) {
	      previous = lArc.P;
	      disappearing.unshift(lArc);
	      d3_geom_voronoiDetachBeach(lArc);
	      lArc = previous;
	    }
	    disappearing.unshift(lArc);
	    d3_geom_voronoiDetachCircle(lArc);
	    var rArc = next;
	    while (rArc.circle && abs(x - rArc.circle.x) < ε && abs(y - rArc.circle.cy) < ε) {
	      next = rArc.N;
	      disappearing.push(rArc);
	      d3_geom_voronoiDetachBeach(rArc);
	      rArc = next;
	    }
	    disappearing.push(rArc);
	    d3_geom_voronoiDetachCircle(rArc);
	    var nArcs = disappearing.length, iArc;
	    for (iArc = 1; iArc < nArcs; ++iArc) {
	      rArc = disappearing[iArc];
	      lArc = disappearing[iArc - 1];
	      d3_geom_voronoiSetEdgeEnd(rArc.edge, lArc.site, rArc.site, vertex);
	    }
	    lArc = disappearing[0];
	    rArc = disappearing[nArcs - 1];
	    rArc.edge = d3_geom_voronoiCreateEdge(lArc.site, rArc.site, null, vertex);
	    d3_geom_voronoiAttachCircle(lArc);
	    d3_geom_voronoiAttachCircle(rArc);
	  }
	  function d3_geom_voronoiAddBeach(site) {
	    var x = site.x, directrix = site.y, lArc, rArc, dxl, dxr, node = d3_geom_voronoiBeaches._;
	    while (node) {
	      dxl = d3_geom_voronoiLeftBreakPoint(node, directrix) - x;
	      if (dxl > ε) node = node.L; else {
	        dxr = x - d3_geom_voronoiRightBreakPoint(node, directrix);
	        if (dxr > ε) {
	          if (!node.R) {
	            lArc = node;
	            break;
	          }
	          node = node.R;
	        } else {
	          if (dxl > -ε) {
	            lArc = node.P;
	            rArc = node;
	          } else if (dxr > -ε) {
	            lArc = node;
	            rArc = node.N;
	          } else {
	            lArc = rArc = node;
	          }
	          break;
	        }
	      }
	    }
	    var newArc = d3_geom_voronoiCreateBeach(site);
	    d3_geom_voronoiBeaches.insert(lArc, newArc);
	    if (!lArc && !rArc) return;
	    if (lArc === rArc) {
	      d3_geom_voronoiDetachCircle(lArc);
	      rArc = d3_geom_voronoiCreateBeach(lArc.site);
	      d3_geom_voronoiBeaches.insert(newArc, rArc);
	      newArc.edge = rArc.edge = d3_geom_voronoiCreateEdge(lArc.site, newArc.site);
	      d3_geom_voronoiAttachCircle(lArc);
	      d3_geom_voronoiAttachCircle(rArc);
	      return;
	    }
	    if (!rArc) {
	      newArc.edge = d3_geom_voronoiCreateEdge(lArc.site, newArc.site);
	      return;
	    }
	    d3_geom_voronoiDetachCircle(lArc);
	    d3_geom_voronoiDetachCircle(rArc);
	    var lSite = lArc.site, ax = lSite.x, ay = lSite.y, bx = site.x - ax, by = site.y - ay, rSite = rArc.site, cx = rSite.x - ax, cy = rSite.y - ay, d = 2 * (bx * cy - by * cx), hb = bx * bx + by * by, hc = cx * cx + cy * cy, vertex = {
	      x: (cy * hb - by * hc) / d + ax,
	      y: (bx * hc - cx * hb) / d + ay
	    };
	    d3_geom_voronoiSetEdgeEnd(rArc.edge, lSite, rSite, vertex);
	    newArc.edge = d3_geom_voronoiCreateEdge(lSite, site, null, vertex);
	    rArc.edge = d3_geom_voronoiCreateEdge(site, rSite, null, vertex);
	    d3_geom_voronoiAttachCircle(lArc);
	    d3_geom_voronoiAttachCircle(rArc);
	  }
	  function d3_geom_voronoiLeftBreakPoint(arc, directrix) {
	    var site = arc.site, rfocx = site.x, rfocy = site.y, pby2 = rfocy - directrix;
	    if (!pby2) return rfocx;
	    var lArc = arc.P;
	    if (!lArc) return -Infinity;
	    site = lArc.site;
	    var lfocx = site.x, lfocy = site.y, plby2 = lfocy - directrix;
	    if (!plby2) return lfocx;
	    var hl = lfocx - rfocx, aby2 = 1 / pby2 - 1 / plby2, b = hl / plby2;
	    if (aby2) return (-b + Math.sqrt(b * b - 2 * aby2 * (hl * hl / (-2 * plby2) - lfocy + plby2 / 2 + rfocy - pby2 / 2))) / aby2 + rfocx;
	    return (rfocx + lfocx) / 2;
	  }
	  function d3_geom_voronoiRightBreakPoint(arc, directrix) {
	    var rArc = arc.N;
	    if (rArc) return d3_geom_voronoiLeftBreakPoint(rArc, directrix);
	    var site = arc.site;
	    return site.y === directrix ? site.x : Infinity;
	  }
	  function d3_geom_voronoiCell(site) {
	    this.site = site;
	    this.edges = [];
	  }
	  d3_geom_voronoiCell.prototype.prepare = function() {
	    var halfEdges = this.edges, iHalfEdge = halfEdges.length, edge;
	    while (iHalfEdge--) {
	      edge = halfEdges[iHalfEdge].edge;
	      if (!edge.b || !edge.a) halfEdges.splice(iHalfEdge, 1);
	    }
	    halfEdges.sort(d3_geom_voronoiHalfEdgeOrder);
	    return halfEdges.length;
	  };
	  function d3_geom_voronoiCloseCells(extent) {
	    var x0 = extent[0][0], x1 = extent[1][0], y0 = extent[0][1], y1 = extent[1][1], x2, y2, x3, y3, cells = d3_geom_voronoiCells, iCell = cells.length, cell, iHalfEdge, halfEdges, nHalfEdges, start, end;
	    while (iCell--) {
	      cell = cells[iCell];
	      if (!cell || !cell.prepare()) continue;
	      halfEdges = cell.edges;
	      nHalfEdges = halfEdges.length;
	      iHalfEdge = 0;
	      while (iHalfEdge < nHalfEdges) {
	        end = halfEdges[iHalfEdge].end(), x3 = end.x, y3 = end.y;
	        start = halfEdges[++iHalfEdge % nHalfEdges].start(), x2 = start.x, y2 = start.y;
	        if (abs(x3 - x2) > ε || abs(y3 - y2) > ε) {
	          halfEdges.splice(iHalfEdge, 0, new d3_geom_voronoiHalfEdge(d3_geom_voronoiCreateBorderEdge(cell.site, end, abs(x3 - x0) < ε && y1 - y3 > ε ? {
	            x: x0,
	            y: abs(x2 - x0) < ε ? y2 : y1
	          } : abs(y3 - y1) < ε && x1 - x3 > ε ? {
	            x: abs(y2 - y1) < ε ? x2 : x1,
	            y: y1
	          } : abs(x3 - x1) < ε && y3 - y0 > ε ? {
	            x: x1,
	            y: abs(x2 - x1) < ε ? y2 : y0
	          } : abs(y3 - y0) < ε && x3 - x0 > ε ? {
	            x: abs(y2 - y0) < ε ? x2 : x0,
	            y: y0
	          } : null), cell.site, null));
	          ++nHalfEdges;
	        }
	      }
	    }
	  }
	  function d3_geom_voronoiHalfEdgeOrder(a, b) {
	    return b.angle - a.angle;
	  }
	  function d3_geom_voronoiCircle() {
	    d3_geom_voronoiRedBlackNode(this);
	    this.x = this.y = this.arc = this.site = this.cy = null;
	  }
	  function d3_geom_voronoiAttachCircle(arc) {
	    var lArc = arc.P, rArc = arc.N;
	    if (!lArc || !rArc) return;
	    var lSite = lArc.site, cSite = arc.site, rSite = rArc.site;
	    if (lSite === rSite) return;
	    var bx = cSite.x, by = cSite.y, ax = lSite.x - bx, ay = lSite.y - by, cx = rSite.x - bx, cy = rSite.y - by;
	    var d = 2 * (ax * cy - ay * cx);
	    if (d >= -ε2) return;
	    var ha = ax * ax + ay * ay, hc = cx * cx + cy * cy, x = (cy * ha - ay * hc) / d, y = (ax * hc - cx * ha) / d, cy = y + by;
	    var circle = d3_geom_voronoiCirclePool.pop() || new d3_geom_voronoiCircle();
	    circle.arc = arc;
	    circle.site = cSite;
	    circle.x = x + bx;
	    circle.y = cy + Math.sqrt(x * x + y * y);
	    circle.cy = cy;
	    arc.circle = circle;
	    var before = null, node = d3_geom_voronoiCircles._;
	    while (node) {
	      if (circle.y < node.y || circle.y === node.y && circle.x <= node.x) {
	        if (node.L) node = node.L; else {
	          before = node.P;
	          break;
	        }
	      } else {
	        if (node.R) node = node.R; else {
	          before = node;
	          break;
	        }
	      }
	    }
	    d3_geom_voronoiCircles.insert(before, circle);
	    if (!before) d3_geom_voronoiFirstCircle = circle;
	  }
	  function d3_geom_voronoiDetachCircle(arc) {
	    var circle = arc.circle;
	    if (circle) {
	      if (!circle.P) d3_geom_voronoiFirstCircle = circle.N;
	      d3_geom_voronoiCircles.remove(circle);
	      d3_geom_voronoiCirclePool.push(circle);
	      d3_geom_voronoiRedBlackNode(circle);
	      arc.circle = null;
	    }
	  }
	  function d3_geom_voronoiClipEdges(extent) {
	    var edges = d3_geom_voronoiEdges, clip = d3_geom_clipLine(extent[0][0], extent[0][1], extent[1][0], extent[1][1]), i = edges.length, e;
	    while (i--) {
	      e = edges[i];
	      if (!d3_geom_voronoiConnectEdge(e, extent) || !clip(e) || abs(e.a.x - e.b.x) < ε && abs(e.a.y - e.b.y) < ε) {
	        e.a = e.b = null;
	        edges.splice(i, 1);
	      }
	    }
	  }
	  function d3_geom_voronoiConnectEdge(edge, extent) {
	    var vb = edge.b;
	    if (vb) return true;
	    var va = edge.a, x0 = extent[0][0], x1 = extent[1][0], y0 = extent[0][1], y1 = extent[1][1], lSite = edge.l, rSite = edge.r, lx = lSite.x, ly = lSite.y, rx = rSite.x, ry = rSite.y, fx = (lx + rx) / 2, fy = (ly + ry) / 2, fm, fb;
	    if (ry === ly) {
	      if (fx < x0 || fx >= x1) return;
	      if (lx > rx) {
	        if (!va) va = {
	          x: fx,
	          y: y0
	        }; else if (va.y >= y1) return;
	        vb = {
	          x: fx,
	          y: y1
	        };
	      } else {
	        if (!va) va = {
	          x: fx,
	          y: y1
	        }; else if (va.y < y0) return;
	        vb = {
	          x: fx,
	          y: y0
	        };
	      }
	    } else {
	      fm = (lx - rx) / (ry - ly);
	      fb = fy - fm * fx;
	      if (fm < -1 || fm > 1) {
	        if (lx > rx) {
	          if (!va) va = {
	            x: (y0 - fb) / fm,
	            y: y0
	          }; else if (va.y >= y1) return;
	          vb = {
	            x: (y1 - fb) / fm,
	            y: y1
	          };
	        } else {
	          if (!va) va = {
	            x: (y1 - fb) / fm,
	            y: y1
	          }; else if (va.y < y0) return;
	          vb = {
	            x: (y0 - fb) / fm,
	            y: y0
	          };
	        }
	      } else {
	        if (ly < ry) {
	          if (!va) va = {
	            x: x0,
	            y: fm * x0 + fb
	          }; else if (va.x >= x1) return;
	          vb = {
	            x: x1,
	            y: fm * x1 + fb
	          };
	        } else {
	          if (!va) va = {
	            x: x1,
	            y: fm * x1 + fb
	          }; else if (va.x < x0) return;
	          vb = {
	            x: x0,
	            y: fm * x0 + fb
	          };
	        }
	      }
	    }
	    edge.a = va;
	    edge.b = vb;
	    return true;
	  }
	  function d3_geom_voronoiEdge(lSite, rSite) {
	    this.l = lSite;
	    this.r = rSite;
	    this.a = this.b = null;
	  }
	  function d3_geom_voronoiCreateEdge(lSite, rSite, va, vb) {
	    var edge = new d3_geom_voronoiEdge(lSite, rSite);
	    d3_geom_voronoiEdges.push(edge);
	    if (va) d3_geom_voronoiSetEdgeEnd(edge, lSite, rSite, va);
	    if (vb) d3_geom_voronoiSetEdgeEnd(edge, rSite, lSite, vb);
	    d3_geom_voronoiCells[lSite.i].edges.push(new d3_geom_voronoiHalfEdge(edge, lSite, rSite));
	    d3_geom_voronoiCells[rSite.i].edges.push(new d3_geom_voronoiHalfEdge(edge, rSite, lSite));
	    return edge;
	  }
	  function d3_geom_voronoiCreateBorderEdge(lSite, va, vb) {
	    var edge = new d3_geom_voronoiEdge(lSite, null);
	    edge.a = va;
	    edge.b = vb;
	    d3_geom_voronoiEdges.push(edge);
	    return edge;
	  }
	  function d3_geom_voronoiSetEdgeEnd(edge, lSite, rSite, vertex) {
	    if (!edge.a && !edge.b) {
	      edge.a = vertex;
	      edge.l = lSite;
	      edge.r = rSite;
	    } else if (edge.l === rSite) {
	      edge.b = vertex;
	    } else {
	      edge.a = vertex;
	    }
	  }
	  function d3_geom_voronoiHalfEdge(edge, lSite, rSite) {
	    var va = edge.a, vb = edge.b;
	    this.edge = edge;
	    this.site = lSite;
	    this.angle = rSite ? Math.atan2(rSite.y - lSite.y, rSite.x - lSite.x) : edge.l === lSite ? Math.atan2(vb.x - va.x, va.y - vb.y) : Math.atan2(va.x - vb.x, vb.y - va.y);
	  }
	  d3_geom_voronoiHalfEdge.prototype = {
	    start: function() {
	      return this.edge.l === this.site ? this.edge.a : this.edge.b;
	    },
	    end: function() {
	      return this.edge.l === this.site ? this.edge.b : this.edge.a;
	    }
	  };
	  function d3_geom_voronoiRedBlackTree() {
	    this._ = null;
	  }
	  function d3_geom_voronoiRedBlackNode(node) {
	    node.U = node.C = node.L = node.R = node.P = node.N = null;
	  }
	  d3_geom_voronoiRedBlackTree.prototype = {
	    insert: function(after, node) {
	      var parent, grandpa, uncle;
	      if (after) {
	        node.P = after;
	        node.N = after.N;
	        if (after.N) after.N.P = node;
	        after.N = node;
	        if (after.R) {
	          after = after.R;
	          while (after.L) after = after.L;
	          after.L = node;
	        } else {
	          after.R = node;
	        }
	        parent = after;
	      } else if (this._) {
	        after = d3_geom_voronoiRedBlackFirst(this._);
	        node.P = null;
	        node.N = after;
	        after.P = after.L = node;
	        parent = after;
	      } else {
	        node.P = node.N = null;
	        this._ = node;
	        parent = null;
	      }
	      node.L = node.R = null;
	      node.U = parent;
	      node.C = true;
	      after = node;
	      while (parent && parent.C) {
	        grandpa = parent.U;
	        if (parent === grandpa.L) {
	          uncle = grandpa.R;
	          if (uncle && uncle.C) {
	            parent.C = uncle.C = false;
	            grandpa.C = true;
	            after = grandpa;
	          } else {
	            if (after === parent.R) {
	              d3_geom_voronoiRedBlackRotateLeft(this, parent);
	              after = parent;
	              parent = after.U;
	            }
	            parent.C = false;
	            grandpa.C = true;
	            d3_geom_voronoiRedBlackRotateRight(this, grandpa);
	          }
	        } else {
	          uncle = grandpa.L;
	          if (uncle && uncle.C) {
	            parent.C = uncle.C = false;
	            grandpa.C = true;
	            after = grandpa;
	          } else {
	            if (after === parent.L) {
	              d3_geom_voronoiRedBlackRotateRight(this, parent);
	              after = parent;
	              parent = after.U;
	            }
	            parent.C = false;
	            grandpa.C = true;
	            d3_geom_voronoiRedBlackRotateLeft(this, grandpa);
	          }
	        }
	        parent = after.U;
	      }
	      this._.C = false;
	    },
	    remove: function(node) {
	      if (node.N) node.N.P = node.P;
	      if (node.P) node.P.N = node.N;
	      node.N = node.P = null;
	      var parent = node.U, sibling, left = node.L, right = node.R, next, red;
	      if (!left) next = right; else if (!right) next = left; else next = d3_geom_voronoiRedBlackFirst(right);
	      if (parent) {
	        if (parent.L === node) parent.L = next; else parent.R = next;
	      } else {
	        this._ = next;
	      }
	      if (left && right) {
	        red = next.C;
	        next.C = node.C;
	        next.L = left;
	        left.U = next;
	        if (next !== right) {
	          parent = next.U;
	          next.U = node.U;
	          node = next.R;
	          parent.L = node;
	          next.R = right;
	          right.U = next;
	        } else {
	          next.U = parent;
	          parent = next;
	          node = next.R;
	        }
	      } else {
	        red = node.C;
	        node = next;
	      }
	      if (node) node.U = parent;
	      if (red) return;
	      if (node && node.C) {
	        node.C = false;
	        return;
	      }
	      do {
	        if (node === this._) break;
	        if (node === parent.L) {
	          sibling = parent.R;
	          if (sibling.C) {
	            sibling.C = false;
	            parent.C = true;
	            d3_geom_voronoiRedBlackRotateLeft(this, parent);
	            sibling = parent.R;
	          }
	          if (sibling.L && sibling.L.C || sibling.R && sibling.R.C) {
	            if (!sibling.R || !sibling.R.C) {
	              sibling.L.C = false;
	              sibling.C = true;
	              d3_geom_voronoiRedBlackRotateRight(this, sibling);
	              sibling = parent.R;
	            }
	            sibling.C = parent.C;
	            parent.C = sibling.R.C = false;
	            d3_geom_voronoiRedBlackRotateLeft(this, parent);
	            node = this._;
	            break;
	          }
	        } else {
	          sibling = parent.L;
	          if (sibling.C) {
	            sibling.C = false;
	            parent.C = true;
	            d3_geom_voronoiRedBlackRotateRight(this, parent);
	            sibling = parent.L;
	          }
	          if (sibling.L && sibling.L.C || sibling.R && sibling.R.C) {
	            if (!sibling.L || !sibling.L.C) {
	              sibling.R.C = false;
	              sibling.C = true;
	              d3_geom_voronoiRedBlackRotateLeft(this, sibling);
	              sibling = parent.L;
	            }
	            sibling.C = parent.C;
	            parent.C = sibling.L.C = false;
	            d3_geom_voronoiRedBlackRotateRight(this, parent);
	            node = this._;
	            break;
	          }
	        }
	        sibling.C = true;
	        node = parent;
	        parent = parent.U;
	      } while (!node.C);
	      if (node) node.C = false;
	    }
	  };
	  function d3_geom_voronoiRedBlackRotateLeft(tree, node) {
	    var p = node, q = node.R, parent = p.U;
	    if (parent) {
	      if (parent.L === p) parent.L = q; else parent.R = q;
	    } else {
	      tree._ = q;
	    }
	    q.U = parent;
	    p.U = q;
	    p.R = q.L;
	    if (p.R) p.R.U = p;
	    q.L = p;
	  }
	  function d3_geom_voronoiRedBlackRotateRight(tree, node) {
	    var p = node, q = node.L, parent = p.U;
	    if (parent) {
	      if (parent.L === p) parent.L = q; else parent.R = q;
	    } else {
	      tree._ = q;
	    }
	    q.U = parent;
	    p.U = q;
	    p.L = q.R;
	    if (p.L) p.L.U = p;
	    q.R = p;
	  }
	  function d3_geom_voronoiRedBlackFirst(node) {
	    while (node.L) node = node.L;
	    return node;
	  }
	  function d3_geom_voronoi(sites, bbox) {
	    var site = sites.sort(d3_geom_voronoiVertexOrder).pop(), x0, y0, circle;
	    d3_geom_voronoiEdges = [];
	    d3_geom_voronoiCells = new Array(sites.length);
	    d3_geom_voronoiBeaches = new d3_geom_voronoiRedBlackTree();
	    d3_geom_voronoiCircles = new d3_geom_voronoiRedBlackTree();
	    while (true) {
	      circle = d3_geom_voronoiFirstCircle;
	      if (site && (!circle || site.y < circle.y || site.y === circle.y && site.x < circle.x)) {
	        if (site.x !== x0 || site.y !== y0) {
	          d3_geom_voronoiCells[site.i] = new d3_geom_voronoiCell(site);
	          d3_geom_voronoiAddBeach(site);
	          x0 = site.x, y0 = site.y;
	        }
	        site = sites.pop();
	      } else if (circle) {
	        d3_geom_voronoiRemoveBeach(circle.arc);
	      } else {
	        break;
	      }
	    }
	    if (bbox) d3_geom_voronoiClipEdges(bbox), d3_geom_voronoiCloseCells(bbox);
	    var diagram = {
	      cells: d3_geom_voronoiCells,
	      edges: d3_geom_voronoiEdges
	    };
	    d3_geom_voronoiBeaches = d3_geom_voronoiCircles = d3_geom_voronoiEdges = d3_geom_voronoiCells = null;
	    return diagram;
	  }
	  function d3_geom_voronoiVertexOrder(a, b) {
	    return b.y - a.y || b.x - a.x;
	  }
	  d3.geom.voronoi = function(points) {
	    var x = d3_geom_pointX, y = d3_geom_pointY, fx = x, fy = y, clipExtent = d3_geom_voronoiClipExtent;
	    if (points) return voronoi(points);
	    function voronoi(data) {
	      var polygons = new Array(data.length), x0 = clipExtent[0][0], y0 = clipExtent[0][1], x1 = clipExtent[1][0], y1 = clipExtent[1][1];
	      d3_geom_voronoi(sites(data), clipExtent).cells.forEach(function(cell, i) {
	        var edges = cell.edges, site = cell.site, polygon = polygons[i] = edges.length ? edges.map(function(e) {
	          var s = e.start();
	          return [ s.x, s.y ];
	        }) : site.x >= x0 && site.x <= x1 && site.y >= y0 && site.y <= y1 ? [ [ x0, y1 ], [ x1, y1 ], [ x1, y0 ], [ x0, y0 ] ] : [];
	        polygon.point = data[i];
	      });
	      return polygons;
	    }
	    function sites(data) {
	      return data.map(function(d, i) {
	        return {
	          x: Math.round(fx(d, i) / ε) * ε,
	          y: Math.round(fy(d, i) / ε) * ε,
	          i: i
	        };
	      });
	    }
	    voronoi.links = function(data) {
	      return d3_geom_voronoi(sites(data)).edges.filter(function(edge) {
	        return edge.l && edge.r;
	      }).map(function(edge) {
	        return {
	          source: data[edge.l.i],
	          target: data[edge.r.i]
	        };
	      });
	    };
	    voronoi.triangles = function(data) {
	      var triangles = [];
	      d3_geom_voronoi(sites(data)).cells.forEach(function(cell, i) {
	        var site = cell.site, edges = cell.edges.sort(d3_geom_voronoiHalfEdgeOrder), j = -1, m = edges.length, e0, s0, e1 = edges[m - 1].edge, s1 = e1.l === site ? e1.r : e1.l;
	        while (++j < m) {
	          e0 = e1;
	          s0 = s1;
	          e1 = edges[j].edge;
	          s1 = e1.l === site ? e1.r : e1.l;
	          if (i < s0.i && i < s1.i && d3_geom_voronoiTriangleArea(site, s0, s1) < 0) {
	            triangles.push([ data[i], data[s0.i], data[s1.i] ]);
	          }
	        }
	      });
	      return triangles;
	    };
	    voronoi.x = function(_) {
	      return arguments.length ? (fx = d3_functor(x = _), voronoi) : x;
	    };
	    voronoi.y = function(_) {
	      return arguments.length ? (fy = d3_functor(y = _), voronoi) : y;
	    };
	    voronoi.clipExtent = function(_) {
	      if (!arguments.length) return clipExtent === d3_geom_voronoiClipExtent ? null : clipExtent;
	      clipExtent = _ == null ? d3_geom_voronoiClipExtent : _;
	      return voronoi;
	    };
	    voronoi.size = function(_) {
	      if (!arguments.length) return clipExtent === d3_geom_voronoiClipExtent ? null : clipExtent && clipExtent[1];
	      return voronoi.clipExtent(_ && [ [ 0, 0 ], _ ]);
	    };
	    return voronoi;
	  };
	  var d3_geom_voronoiClipExtent = [ [ -1e6, -1e6 ], [ 1e6, 1e6 ] ];
	  function d3_geom_voronoiTriangleArea(a, b, c) {
	    return (a.x - c.x) * (b.y - a.y) - (a.x - b.x) * (c.y - a.y);
	  }
	  d3.geom.delaunay = function(vertices) {
	    return d3.geom.voronoi().triangles(vertices);
	  };
	  d3.geom.quadtree = function(points, x1, y1, x2, y2) {
	    var x = d3_geom_pointX, y = d3_geom_pointY, compat;
	    if (compat = arguments.length) {
	      x = d3_geom_quadtreeCompatX;
	      y = d3_geom_quadtreeCompatY;
	      if (compat === 3) {
	        y2 = y1;
	        x2 = x1;
	        y1 = x1 = 0;
	      }
	      return quadtree(points);
	    }
	    function quadtree(data) {
	      var d, fx = d3_functor(x), fy = d3_functor(y), xs, ys, i, n, x1_, y1_, x2_, y2_;
	      if (x1 != null) {
	        x1_ = x1, y1_ = y1, x2_ = x2, y2_ = y2;
	      } else {
	        x2_ = y2_ = -(x1_ = y1_ = Infinity);
	        xs = [], ys = [];
	        n = data.length;
	        if (compat) for (i = 0; i < n; ++i) {
	          d = data[i];
	          if (d.x < x1_) x1_ = d.x;
	          if (d.y < y1_) y1_ = d.y;
	          if (d.x > x2_) x2_ = d.x;
	          if (d.y > y2_) y2_ = d.y;
	          xs.push(d.x);
	          ys.push(d.y);
	        } else for (i = 0; i < n; ++i) {
	          var x_ = +fx(d = data[i], i), y_ = +fy(d, i);
	          if (x_ < x1_) x1_ = x_;
	          if (y_ < y1_) y1_ = y_;
	          if (x_ > x2_) x2_ = x_;
	          if (y_ > y2_) y2_ = y_;
	          xs.push(x_);
	          ys.push(y_);
	        }
	      }
	      var dx = x2_ - x1_, dy = y2_ - y1_;
	      if (dx > dy) y2_ = y1_ + dx; else x2_ = x1_ + dy;
	      function insert(n, d, x, y, x1, y1, x2, y2) {
	        if (isNaN(x) || isNaN(y)) return;
	        if (n.leaf) {
	          var nx = n.x, ny = n.y;
	          if (nx != null) {
	            if (abs(nx - x) + abs(ny - y) < .01) {
	              insertChild(n, d, x, y, x1, y1, x2, y2);
	            } else {
	              var nPoint = n.point;
	              n.x = n.y = n.point = null;
	              insertChild(n, nPoint, nx, ny, x1, y1, x2, y2);
	              insertChild(n, d, x, y, x1, y1, x2, y2);
	            }
	          } else {
	            n.x = x, n.y = y, n.point = d;
	          }
	        } else {
	          insertChild(n, d, x, y, x1, y1, x2, y2);
	        }
	      }
	      function insertChild(n, d, x, y, x1, y1, x2, y2) {
	        var xm = (x1 + x2) * .5, ym = (y1 + y2) * .5, right = x >= xm, below = y >= ym, i = below << 1 | right;
	        n.leaf = false;
	        n = n.nodes[i] || (n.nodes[i] = d3_geom_quadtreeNode());
	        if (right) x1 = xm; else x2 = xm;
	        if (below) y1 = ym; else y2 = ym;
	        insert(n, d, x, y, x1, y1, x2, y2);
	      }
	      var root = d3_geom_quadtreeNode();
	      root.add = function(d) {
	        insert(root, d, +fx(d, ++i), +fy(d, i), x1_, y1_, x2_, y2_);
	      };
	      root.visit = function(f) {
	        d3_geom_quadtreeVisit(f, root, x1_, y1_, x2_, y2_);
	      };
	      root.find = function(point) {
	        return d3_geom_quadtreeFind(root, point[0], point[1], x1_, y1_, x2_, y2_);
	      };
	      i = -1;
	      if (x1 == null) {
	        while (++i < n) {
	          insert(root, data[i], xs[i], ys[i], x1_, y1_, x2_, y2_);
	        }
	        --i;
	      } else data.forEach(root.add);
	      xs = ys = data = d = null;
	      return root;
	    }
	    quadtree.x = function(_) {
	      return arguments.length ? (x = _, quadtree) : x;
	    };
	    quadtree.y = function(_) {
	      return arguments.length ? (y = _, quadtree) : y;
	    };
	    quadtree.extent = function(_) {
	      if (!arguments.length) return x1 == null ? null : [ [ x1, y1 ], [ x2, y2 ] ];
	      if (_ == null) x1 = y1 = x2 = y2 = null; else x1 = +_[0][0], y1 = +_[0][1], x2 = +_[1][0], 
	      y2 = +_[1][1];
	      return quadtree;
	    };
	    quadtree.size = function(_) {
	      if (!arguments.length) return x1 == null ? null : [ x2 - x1, y2 - y1 ];
	      if (_ == null) x1 = y1 = x2 = y2 = null; else x1 = y1 = 0, x2 = +_[0], y2 = +_[1];
	      return quadtree;
	    };
	    return quadtree;
	  };
	  function d3_geom_quadtreeCompatX(d) {
	    return d.x;
	  }
	  function d3_geom_quadtreeCompatY(d) {
	    return d.y;
	  }
	  function d3_geom_quadtreeNode() {
	    return {
	      leaf: true,
	      nodes: [],
	      point: null,
	      x: null,
	      y: null
	    };
	  }
	  function d3_geom_quadtreeVisit(f, node, x1, y1, x2, y2) {
	    if (!f(node, x1, y1, x2, y2)) {
	      var sx = (x1 + x2) * .5, sy = (y1 + y2) * .5, children = node.nodes;
	      if (children[0]) d3_geom_quadtreeVisit(f, children[0], x1, y1, sx, sy);
	      if (children[1]) d3_geom_quadtreeVisit(f, children[1], sx, y1, x2, sy);
	      if (children[2]) d3_geom_quadtreeVisit(f, children[2], x1, sy, sx, y2);
	      if (children[3]) d3_geom_quadtreeVisit(f, children[3], sx, sy, x2, y2);
	    }
	  }
	  function d3_geom_quadtreeFind(root, x, y, x0, y0, x3, y3) {
	    var minDistance2 = Infinity, closestPoint;
	    (function find(node, x1, y1, x2, y2) {
	      if (x1 > x3 || y1 > y3 || x2 < x0 || y2 < y0) return;
	      if (point = node.point) {
	        var point, dx = x - node.x, dy = y - node.y, distance2 = dx * dx + dy * dy;
	        if (distance2 < minDistance2) {
	          var distance = Math.sqrt(minDistance2 = distance2);
	          x0 = x - distance, y0 = y - distance;
	          x3 = x + distance, y3 = y + distance;
	          closestPoint = point;
	        }
	      }
	      var children = node.nodes, xm = (x1 + x2) * .5, ym = (y1 + y2) * .5, right = x >= xm, below = y >= ym;
	      for (var i = below << 1 | right, j = i + 4; i < j; ++i) {
	        if (node = children[i & 3]) switch (i & 3) {
	         case 0:
	          find(node, x1, y1, xm, ym);
	          break;

	         case 1:
	          find(node, xm, y1, x2, ym);
	          break;

	         case 2:
	          find(node, x1, ym, xm, y2);
	          break;

	         case 3:
	          find(node, xm, ym, x2, y2);
	          break;
	        }
	      }
	    })(root, x0, y0, x3, y3);
	    return closestPoint;
	  }
	  d3.interpolateRgb = d3_interpolateRgb;
	  function d3_interpolateRgb(a, b) {
	    a = d3.rgb(a);
	    b = d3.rgb(b);
	    var ar = a.r, ag = a.g, ab = a.b, br = b.r - ar, bg = b.g - ag, bb = b.b - ab;
	    return function(t) {
	      return "#" + d3_rgb_hex(Math.round(ar + br * t)) + d3_rgb_hex(Math.round(ag + bg * t)) + d3_rgb_hex(Math.round(ab + bb * t));
	    };
	  }
	  d3.interpolateObject = d3_interpolateObject;
	  function d3_interpolateObject(a, b) {
	    var i = {}, c = {}, k;
	    for (k in a) {
	      if (k in b) {
	        i[k] = d3_interpolate(a[k], b[k]);
	      } else {
	        c[k] = a[k];
	      }
	    }
	    for (k in b) {
	      if (!(k in a)) {
	        c[k] = b[k];
	      }
	    }
	    return function(t) {
	      for (k in i) c[k] = i[k](t);
	      return c;
	    };
	  }
	  d3.interpolateNumber = d3_interpolateNumber;
	  function d3_interpolateNumber(a, b) {
	    a = +a, b = +b;
	    return function(t) {
	      return a * (1 - t) + b * t;
	    };
	  }
	  d3.interpolateString = d3_interpolateString;
	  function d3_interpolateString(a, b) {
	    var bi = d3_interpolate_numberA.lastIndex = d3_interpolate_numberB.lastIndex = 0, am, bm, bs, i = -1, s = [], q = [];
	    a = a + "", b = b + "";
	    while ((am = d3_interpolate_numberA.exec(a)) && (bm = d3_interpolate_numberB.exec(b))) {
	      if ((bs = bm.index) > bi) {
	        bs = b.slice(bi, bs);
	        if (s[i]) s[i] += bs; else s[++i] = bs;
	      }
	      if ((am = am[0]) === (bm = bm[0])) {
	        if (s[i]) s[i] += bm; else s[++i] = bm;
	      } else {
	        s[++i] = null;
	        q.push({
	          i: i,
	          x: d3_interpolateNumber(am, bm)
	        });
	      }
	      bi = d3_interpolate_numberB.lastIndex;
	    }
	    if (bi < b.length) {
	      bs = b.slice(bi);
	      if (s[i]) s[i] += bs; else s[++i] = bs;
	    }
	    return s.length < 2 ? q[0] ? (b = q[0].x, function(t) {
	      return b(t) + "";
	    }) : function() {
	      return b;
	    } : (b = q.length, function(t) {
	      for (var i = 0, o; i < b; ++i) s[(o = q[i]).i] = o.x(t);
	      return s.join("");
	    });
	  }
	  var d3_interpolate_numberA = /[-+]?(?:\d+\.?\d*|\.?\d+)(?:[eE][-+]?\d+)?/g, d3_interpolate_numberB = new RegExp(d3_interpolate_numberA.source, "g");
	  d3.interpolate = d3_interpolate;
	  function d3_interpolate(a, b) {
	    var i = d3.interpolators.length, f;
	    while (--i >= 0 && !(f = d3.interpolators[i](a, b))) ;
	    return f;
	  }
	  d3.interpolators = [ function(a, b) {
	    var t = typeof b;
	    return (t === "string" ? d3_rgb_names.has(b.toLowerCase()) || /^(#|rgb\(|hsl\()/i.test(b) ? d3_interpolateRgb : d3_interpolateString : b instanceof d3_color ? d3_interpolateRgb : Array.isArray(b) ? d3_interpolateArray : t === "object" && isNaN(b) ? d3_interpolateObject : d3_interpolateNumber)(a, b);
	  } ];
	  d3.interpolateArray = d3_interpolateArray;
	  function d3_interpolateArray(a, b) {
	    var x = [], c = [], na = a.length, nb = b.length, n0 = Math.min(a.length, b.length), i;
	    for (i = 0; i < n0; ++i) x.push(d3_interpolate(a[i], b[i]));
	    for (;i < na; ++i) c[i] = a[i];
	    for (;i < nb; ++i) c[i] = b[i];
	    return function(t) {
	      for (i = 0; i < n0; ++i) c[i] = x[i](t);
	      return c;
	    };
	  }
	  var d3_ease_default = function() {
	    return d3_identity;
	  };
	  var d3_ease = d3.map({
	    linear: d3_ease_default,
	    poly: d3_ease_poly,
	    quad: function() {
	      return d3_ease_quad;
	    },
	    cubic: function() {
	      return d3_ease_cubic;
	    },
	    sin: function() {
	      return d3_ease_sin;
	    },
	    exp: function() {
	      return d3_ease_exp;
	    },
	    circle: function() {
	      return d3_ease_circle;
	    },
	    elastic: d3_ease_elastic,
	    back: d3_ease_back,
	    bounce: function() {
	      return d3_ease_bounce;
	    }
	  });
	  var d3_ease_mode = d3.map({
	    "in": d3_identity,
	    out: d3_ease_reverse,
	    "in-out": d3_ease_reflect,
	    "out-in": function(f) {
	      return d3_ease_reflect(d3_ease_reverse(f));
	    }
	  });
	  d3.ease = function(name) {
	    var i = name.indexOf("-"), t = i >= 0 ? name.slice(0, i) : name, m = i >= 0 ? name.slice(i + 1) : "in";
	    t = d3_ease.get(t) || d3_ease_default;
	    m = d3_ease_mode.get(m) || d3_identity;
	    return d3_ease_clamp(m(t.apply(null, d3_arraySlice.call(arguments, 1))));
	  };
	  function d3_ease_clamp(f) {
	    return function(t) {
	      return t <= 0 ? 0 : t >= 1 ? 1 : f(t);
	    };
	  }
	  function d3_ease_reverse(f) {
	    return function(t) {
	      return 1 - f(1 - t);
	    };
	  }
	  function d3_ease_reflect(f) {
	    return function(t) {
	      return .5 * (t < .5 ? f(2 * t) : 2 - f(2 - 2 * t));
	    };
	  }
	  function d3_ease_quad(t) {
	    return t * t;
	  }
	  function d3_ease_cubic(t) {
	    return t * t * t;
	  }
	  function d3_ease_cubicInOut(t) {
	    if (t <= 0) return 0;
	    if (t >= 1) return 1;
	    var t2 = t * t, t3 = t2 * t;
	    return 4 * (t < .5 ? t3 : 3 * (t - t2) + t3 - .75);
	  }
	  function d3_ease_poly(e) {
	    return function(t) {
	      return Math.pow(t, e);
	    };
	  }
	  function d3_ease_sin(t) {
	    return 1 - Math.cos(t * halfπ);
	  }
	  function d3_ease_exp(t) {
	    return Math.pow(2, 10 * (t - 1));
	  }
	  function d3_ease_circle(t) {
	    return 1 - Math.sqrt(1 - t * t);
	  }
	  function d3_ease_elastic(a, p) {
	    var s;
	    if (arguments.length < 2) p = .45;
	    if (arguments.length) s = p / τ * Math.asin(1 / a); else a = 1, s = p / 4;
	    return function(t) {
	      return 1 + a * Math.pow(2, -10 * t) * Math.sin((t - s) * τ / p);
	    };
	  }
	  function d3_ease_back(s) {
	    if (!s) s = 1.70158;
	    return function(t) {
	      return t * t * ((s + 1) * t - s);
	    };
	  }
	  function d3_ease_bounce(t) {
	    return t < 1 / 2.75 ? 7.5625 * t * t : t < 2 / 2.75 ? 7.5625 * (t -= 1.5 / 2.75) * t + .75 : t < 2.5 / 2.75 ? 7.5625 * (t -= 2.25 / 2.75) * t + .9375 : 7.5625 * (t -= 2.625 / 2.75) * t + .984375;
	  }
	  d3.interpolateHcl = d3_interpolateHcl;
	  function d3_interpolateHcl(a, b) {
	    a = d3.hcl(a);
	    b = d3.hcl(b);
	    var ah = a.h, ac = a.c, al = a.l, bh = b.h - ah, bc = b.c - ac, bl = b.l - al;
	    if (isNaN(bc)) bc = 0, ac = isNaN(ac) ? b.c : ac;
	    if (isNaN(bh)) bh = 0, ah = isNaN(ah) ? b.h : ah; else if (bh > 180) bh -= 360; else if (bh < -180) bh += 360;
	    return function(t) {
	      return d3_hcl_lab(ah + bh * t, ac + bc * t, al + bl * t) + "";
	    };
	  }
	  d3.interpolateHsl = d3_interpolateHsl;
	  function d3_interpolateHsl(a, b) {
	    a = d3.hsl(a);
	    b = d3.hsl(b);
	    var ah = a.h, as = a.s, al = a.l, bh = b.h - ah, bs = b.s - as, bl = b.l - al;
	    if (isNaN(bs)) bs = 0, as = isNaN(as) ? b.s : as;
	    if (isNaN(bh)) bh = 0, ah = isNaN(ah) ? b.h : ah; else if (bh > 180) bh -= 360; else if (bh < -180) bh += 360;
	    return function(t) {
	      return d3_hsl_rgb(ah + bh * t, as + bs * t, al + bl * t) + "";
	    };
	  }
	  d3.interpolateLab = d3_interpolateLab;
	  function d3_interpolateLab(a, b) {
	    a = d3.lab(a);
	    b = d3.lab(b);
	    var al = a.l, aa = a.a, ab = a.b, bl = b.l - al, ba = b.a - aa, bb = b.b - ab;
	    return function(t) {
	      return d3_lab_rgb(al + bl * t, aa + ba * t, ab + bb * t) + "";
	    };
	  }
	  d3.interpolateRound = d3_interpolateRound;
	  function d3_interpolateRound(a, b) {
	    b -= a;
	    return function(t) {
	      return Math.round(a + b * t);
	    };
	  }
	  d3.transform = function(string) {
	    var g = d3_document.createElementNS(d3.ns.prefix.svg, "g");
	    return (d3.transform = function(string) {
	      if (string != null) {
	        g.setAttribute("transform", string);
	        var t = g.transform.baseVal.consolidate();
	      }
	      return new d3_transform(t ? t.matrix : d3_transformIdentity);
	    })(string);
	  };
	  function d3_transform(m) {
	    var r0 = [ m.a, m.b ], r1 = [ m.c, m.d ], kx = d3_transformNormalize(r0), kz = d3_transformDot(r0, r1), ky = d3_transformNormalize(d3_transformCombine(r1, r0, -kz)) || 0;
	    if (r0[0] * r1[1] < r1[0] * r0[1]) {
	      r0[0] *= -1;
	      r0[1] *= -1;
	      kx *= -1;
	      kz *= -1;
	    }
	    this.rotate = (kx ? Math.atan2(r0[1], r0[0]) : Math.atan2(-r1[0], r1[1])) * d3_degrees;
	    this.translate = [ m.e, m.f ];
	    this.scale = [ kx, ky ];
	    this.skew = ky ? Math.atan2(kz, ky) * d3_degrees : 0;
	  }
	  d3_transform.prototype.toString = function() {
	    return "translate(" + this.translate + ")rotate(" + this.rotate + ")skewX(" + this.skew + ")scale(" + this.scale + ")";
	  };
	  function d3_transformDot(a, b) {
	    return a[0] * b[0] + a[1] * b[1];
	  }
	  function d3_transformNormalize(a) {
	    var k = Math.sqrt(d3_transformDot(a, a));
	    if (k) {
	      a[0] /= k;
	      a[1] /= k;
	    }
	    return k;
	  }
	  function d3_transformCombine(a, b, k) {
	    a[0] += k * b[0];
	    a[1] += k * b[1];
	    return a;
	  }
	  var d3_transformIdentity = {
	    a: 1,
	    b: 0,
	    c: 0,
	    d: 1,
	    e: 0,
	    f: 0
	  };
	  d3.interpolateTransform = d3_interpolateTransform;
	  function d3_interpolateTransformPop(s) {
	    return s.length ? s.pop() + "," : "";
	  }
	  function d3_interpolateTranslate(ta, tb, s, q) {
	    if (ta[0] !== tb[0] || ta[1] !== tb[1]) {
	      var i = s.push("translate(", null, ",", null, ")");
	      q.push({
	        i: i - 4,
	        x: d3_interpolateNumber(ta[0], tb[0])
	      }, {
	        i: i - 2,
	        x: d3_interpolateNumber(ta[1], tb[1])
	      });
	    } else if (tb[0] || tb[1]) {
	      s.push("translate(" + tb + ")");
	    }
	  }
	  function d3_interpolateRotate(ra, rb, s, q) {
	    if (ra !== rb) {
	      if (ra - rb > 180) rb += 360; else if (rb - ra > 180) ra += 360;
	      q.push({
	        i: s.push(d3_interpolateTransformPop(s) + "rotate(", null, ")") - 2,
	        x: d3_interpolateNumber(ra, rb)
	      });
	    } else if (rb) {
	      s.push(d3_interpolateTransformPop(s) + "rotate(" + rb + ")");
	    }
	  }
	  function d3_interpolateSkew(wa, wb, s, q) {
	    if (wa !== wb) {
	      q.push({
	        i: s.push(d3_interpolateTransformPop(s) + "skewX(", null, ")") - 2,
	        x: d3_interpolateNumber(wa, wb)
	      });
	    } else if (wb) {
	      s.push(d3_interpolateTransformPop(s) + "skewX(" + wb + ")");
	    }
	  }
	  function d3_interpolateScale(ka, kb, s, q) {
	    if (ka[0] !== kb[0] || ka[1] !== kb[1]) {
	      var i = s.push(d3_interpolateTransformPop(s) + "scale(", null, ",", null, ")");
	      q.push({
	        i: i - 4,
	        x: d3_interpolateNumber(ka[0], kb[0])
	      }, {
	        i: i - 2,
	        x: d3_interpolateNumber(ka[1], kb[1])
	      });
	    } else if (kb[0] !== 1 || kb[1] !== 1) {
	      s.push(d3_interpolateTransformPop(s) + "scale(" + kb + ")");
	    }
	  }
	  function d3_interpolateTransform(a, b) {
	    var s = [], q = [];
	    a = d3.transform(a), b = d3.transform(b);
	    d3_interpolateTranslate(a.translate, b.translate, s, q);
	    d3_interpolateRotate(a.rotate, b.rotate, s, q);
	    d3_interpolateSkew(a.skew, b.skew, s, q);
	    d3_interpolateScale(a.scale, b.scale, s, q);
	    a = b = null;
	    return function(t) {
	      var i = -1, n = q.length, o;
	      while (++i < n) s[(o = q[i]).i] = o.x(t);
	      return s.join("");
	    };
	  }
	  function d3_uninterpolateNumber(a, b) {
	    b = (b -= a = +a) || 1 / b;
	    return function(x) {
	      return (x - a) / b;
	    };
	  }
	  function d3_uninterpolateClamp(a, b) {
	    b = (b -= a = +a) || 1 / b;
	    return function(x) {
	      return Math.max(0, Math.min(1, (x - a) / b));
	    };
	  }
	  d3.layout = {};
	  d3.layout.bundle = function() {
	    return function(links) {
	      var paths = [], i = -1, n = links.length;
	      while (++i < n) paths.push(d3_layout_bundlePath(links[i]));
	      return paths;
	    };
	  };
	  function d3_layout_bundlePath(link) {
	    var start = link.source, end = link.target, lca = d3_layout_bundleLeastCommonAncestor(start, end), points = [ start ];
	    while (start !== lca) {
	      start = start.parent;
	      points.push(start);
	    }
	    var k = points.length;
	    while (end !== lca) {
	      points.splice(k, 0, end);
	      end = end.parent;
	    }
	    return points;
	  }
	  function d3_layout_bundleAncestors(node) {
	    var ancestors = [], parent = node.parent;
	    while (parent != null) {
	      ancestors.push(node);
	      node = parent;
	      parent = parent.parent;
	    }
	    ancestors.push(node);
	    return ancestors;
	  }
	  function d3_layout_bundleLeastCommonAncestor(a, b) {
	    if (a === b) return a;
	    var aNodes = d3_layout_bundleAncestors(a), bNodes = d3_layout_bundleAncestors(b), aNode = aNodes.pop(), bNode = bNodes.pop(), sharedNode = null;
	    while (aNode === bNode) {
	      sharedNode = aNode;
	      aNode = aNodes.pop();
	      bNode = bNodes.pop();
	    }
	    return sharedNode;
	  }
	  d3.layout.chord = function() {
	    var chord = {}, chords, groups, matrix, n, padding = 0, sortGroups, sortSubgroups, sortChords;
	    function relayout() {
	      var subgroups = {}, groupSums = [], groupIndex = d3.range(n), subgroupIndex = [], k, x, x0, i, j;
	      chords = [];
	      groups = [];
	      k = 0, i = -1;
	      while (++i < n) {
	        x = 0, j = -1;
	        while (++j < n) {
	          x += matrix[i][j];
	        }
	        groupSums.push(x);
	        subgroupIndex.push(d3.range(n));
	        k += x;
	      }
	      if (sortGroups) {
	        groupIndex.sort(function(a, b) {
	          return sortGroups(groupSums[a], groupSums[b]);
	        });
	      }
	      if (sortSubgroups) {
	        subgroupIndex.forEach(function(d, i) {
	          d.sort(function(a, b) {
	            return sortSubgroups(matrix[i][a], matrix[i][b]);
	          });
	        });
	      }
	      k = (τ - padding * n) / k;
	      x = 0, i = -1;
	      while (++i < n) {
	        x0 = x, j = -1;
	        while (++j < n) {
	          var di = groupIndex[i], dj = subgroupIndex[di][j], v = matrix[di][dj], a0 = x, a1 = x += v * k;
	          subgroups[di + "-" + dj] = {
	            index: di,
	            subindex: dj,
	            startAngle: a0,
	            endAngle: a1,
	            value: v
	          };
	        }
	        groups[di] = {
	          index: di,
	          startAngle: x0,
	          endAngle: x,
	          value: groupSums[di]
	        };
	        x += padding;
	      }
	      i = -1;
	      while (++i < n) {
	        j = i - 1;
	        while (++j < n) {
	          var source = subgroups[i + "-" + j], target = subgroups[j + "-" + i];
	          if (source.value || target.value) {
	            chords.push(source.value < target.value ? {
	              source: target,
	              target: source
	            } : {
	              source: source,
	              target: target
	            });
	          }
	        }
	      }
	      if (sortChords) resort();
	    }
	    function resort() {
	      chords.sort(function(a, b) {
	        return sortChords((a.source.value + a.target.value) / 2, (b.source.value + b.target.value) / 2);
	      });
	    }
	    chord.matrix = function(x) {
	      if (!arguments.length) return matrix;
	      n = (matrix = x) && matrix.length;
	      chords = groups = null;
	      return chord;
	    };
	    chord.padding = function(x) {
	      if (!arguments.length) return padding;
	      padding = x;
	      chords = groups = null;
	      return chord;
	    };
	    chord.sortGroups = function(x) {
	      if (!arguments.length) return sortGroups;
	      sortGroups = x;
	      chords = groups = null;
	      return chord;
	    };
	    chord.sortSubgroups = function(x) {
	      if (!arguments.length) return sortSubgroups;
	      sortSubgroups = x;
	      chords = null;
	      return chord;
	    };
	    chord.sortChords = function(x) {
	      if (!arguments.length) return sortChords;
	      sortChords = x;
	      if (chords) resort();
	      return chord;
	    };
	    chord.chords = function() {
	      if (!chords) relayout();
	      return chords;
	    };
	    chord.groups = function() {
	      if (!groups) relayout();
	      return groups;
	    };
	    return chord;
	  };
	  d3.layout.force = function() {
	    var force = {}, event = d3.dispatch("start", "tick", "end"), timer, size = [ 1, 1 ], drag, alpha, friction = .9, linkDistance = d3_layout_forceLinkDistance, linkStrength = d3_layout_forceLinkStrength, charge = -30, chargeDistance2 = d3_layout_forceChargeDistance2, gravity = .1, theta2 = .64, nodes = [], links = [], distances, strengths, charges;
	    function repulse(node) {
	      return function(quad, x1, _, x2) {
	        if (quad.point !== node) {
	          var dx = quad.cx - node.x, dy = quad.cy - node.y, dw = x2 - x1, dn = dx * dx + dy * dy;
	          if (dw * dw / theta2 < dn) {
	            if (dn < chargeDistance2) {
	              var k = quad.charge / dn;
	              node.px -= dx * k;
	              node.py -= dy * k;
	            }
	            return true;
	          }
	          if (quad.point && dn && dn < chargeDistance2) {
	            var k = quad.pointCharge / dn;
	            node.px -= dx * k;
	            node.py -= dy * k;
	          }
	        }
	        return !quad.charge;
	      };
	    }
	    force.tick = function() {
	      if ((alpha *= .99) < .005) {
	        timer = null;
	        event.end({
	          type: "end",
	          alpha: alpha = 0
	        });
	        return true;
	      }
	      var n = nodes.length, m = links.length, q, i, o, s, t, l, k, x, y;
	      for (i = 0; i < m; ++i) {
	        o = links[i];
	        s = o.source;
	        t = o.target;
	        x = t.x - s.x;
	        y = t.y - s.y;
	        if (l = x * x + y * y) {
	          l = alpha * strengths[i] * ((l = Math.sqrt(l)) - distances[i]) / l;
	          x *= l;
	          y *= l;
	          t.x -= x * (k = s.weight + t.weight ? s.weight / (s.weight + t.weight) : .5);
	          t.y -= y * k;
	          s.x += x * (k = 1 - k);
	          s.y += y * k;
	        }
	      }
	      if (k = alpha * gravity) {
	        x = size[0] / 2;
	        y = size[1] / 2;
	        i = -1;
	        if (k) while (++i < n) {
	          o = nodes[i];
	          o.x += (x - o.x) * k;
	          o.y += (y - o.y) * k;
	        }
	      }
	      if (charge) {
	        d3_layout_forceAccumulate(q = d3.geom.quadtree(nodes), alpha, charges);
	        i = -1;
	        while (++i < n) {
	          if (!(o = nodes[i]).fixed) {
	            q.visit(repulse(o));
	          }
	        }
	      }
	      i = -1;
	      while (++i < n) {
	        o = nodes[i];
	        if (o.fixed) {
	          o.x = o.px;
	          o.y = o.py;
	        } else {
	          o.x -= (o.px - (o.px = o.x)) * friction;
	          o.y -= (o.py - (o.py = o.y)) * friction;
	        }
	      }
	      event.tick({
	        type: "tick",
	        alpha: alpha
	      });
	    };
	    force.nodes = function(x) {
	      if (!arguments.length) return nodes;
	      nodes = x;
	      return force;
	    };
	    force.links = function(x) {
	      if (!arguments.length) return links;
	      links = x;
	      return force;
	    };
	    force.size = function(x) {
	      if (!arguments.length) return size;
	      size = x;
	      return force;
	    };
	    force.linkDistance = function(x) {
	      if (!arguments.length) return linkDistance;
	      linkDistance = typeof x === "function" ? x : +x;
	      return force;
	    };
	    force.distance = force.linkDistance;
	    force.linkStrength = function(x) {
	      if (!arguments.length) return linkStrength;
	      linkStrength = typeof x === "function" ? x : +x;
	      return force;
	    };
	    force.friction = function(x) {
	      if (!arguments.length) return friction;
	      friction = +x;
	      return force;
	    };
	    force.charge = function(x) {
	      if (!arguments.length) return charge;
	      charge = typeof x === "function" ? x : +x;
	      return force;
	    };
	    force.chargeDistance = function(x) {
	      if (!arguments.length) return Math.sqrt(chargeDistance2);
	      chargeDistance2 = x * x;
	      return force;
	    };
	    force.gravity = function(x) {
	      if (!arguments.length) return gravity;
	      gravity = +x;
	      return force;
	    };
	    force.theta = function(x) {
	      if (!arguments.length) return Math.sqrt(theta2);
	      theta2 = x * x;
	      return force;
	    };
	    force.alpha = function(x) {
	      if (!arguments.length) return alpha;
	      x = +x;
	      if (alpha) {
	        if (x > 0) {
	          alpha = x;
	        } else {
	          timer.c = null, timer.t = NaN, timer = null;
	          event.end({
	            type: "end",
	            alpha: alpha = 0
	          });
	        }
	      } else if (x > 0) {
	        event.start({
	          type: "start",
	          alpha: alpha = x
	        });
	        timer = d3_timer(force.tick);
	      }
	      return force;
	    };
	    force.start = function() {
	      var i, n = nodes.length, m = links.length, w = size[0], h = size[1], neighbors, o;
	      for (i = 0; i < n; ++i) {
	        (o = nodes[i]).index = i;
	        o.weight = 0;
	      }
	      for (i = 0; i < m; ++i) {
	        o = links[i];
	        if (typeof o.source == "number") o.source = nodes[o.source];
	        if (typeof o.target == "number") o.target = nodes[o.target];
	        ++o.source.weight;
	        ++o.target.weight;
	      }
	      for (i = 0; i < n; ++i) {
	        o = nodes[i];
	        if (isNaN(o.x)) o.x = position("x", w);
	        if (isNaN(o.y)) o.y = position("y", h);
	        if (isNaN(o.px)) o.px = o.x;
	        if (isNaN(o.py)) o.py = o.y;
	      }
	      distances = [];
	      if (typeof linkDistance === "function") for (i = 0; i < m; ++i) distances[i] = +linkDistance.call(this, links[i], i); else for (i = 0; i < m; ++i) distances[i] = linkDistance;
	      strengths = [];
	      if (typeof linkStrength === "function") for (i = 0; i < m; ++i) strengths[i] = +linkStrength.call(this, links[i], i); else for (i = 0; i < m; ++i) strengths[i] = linkStrength;
	      charges = [];
	      if (typeof charge === "function") for (i = 0; i < n; ++i) charges[i] = +charge.call(this, nodes[i], i); else for (i = 0; i < n; ++i) charges[i] = charge;
	      function position(dimension, size) {
	        if (!neighbors) {
	          neighbors = new Array(n);
	          for (j = 0; j < n; ++j) {
	            neighbors[j] = [];
	          }
	          for (j = 0; j < m; ++j) {
	            var o = links[j];
	            neighbors[o.source.index].push(o.target);
	            neighbors[o.target.index].push(o.source);
	          }
	        }
	        var candidates = neighbors[i], j = -1, l = candidates.length, x;
	        while (++j < l) if (!isNaN(x = candidates[j][dimension])) return x;
	        return Math.random() * size;
	      }
	      return force.resume();
	    };
	    force.resume = function() {
	      return force.alpha(.1);
	    };
	    force.stop = function() {
	      return force.alpha(0);
	    };
	    force.drag = function() {
	      if (!drag) drag = d3.behavior.drag().origin(d3_identity).on("dragstart.force", d3_layout_forceDragstart).on("drag.force", dragmove).on("dragend.force", d3_layout_forceDragend);
	      if (!arguments.length) return drag;
	      this.on("mouseover.force", d3_layout_forceMouseover).on("mouseout.force", d3_layout_forceMouseout).call(drag);
	    };
	    function dragmove(d) {
	      d.px = d3.event.x, d.py = d3.event.y;
	      force.resume();
	    }
	    return d3.rebind(force, event, "on");
	  };
	  function d3_layout_forceDragstart(d) {
	    d.fixed |= 2;
	  }
	  function d3_layout_forceDragend(d) {
	    d.fixed &= ~6;
	  }
	  function d3_layout_forceMouseover(d) {
	    d.fixed |= 4;
	    d.px = d.x, d.py = d.y;
	  }
	  function d3_layout_forceMouseout(d) {
	    d.fixed &= ~4;
	  }
	  function d3_layout_forceAccumulate(quad, alpha, charges) {
	    var cx = 0, cy = 0;
	    quad.charge = 0;
	    if (!quad.leaf) {
	      var nodes = quad.nodes, n = nodes.length, i = -1, c;
	      while (++i < n) {
	        c = nodes[i];
	        if (c == null) continue;
	        d3_layout_forceAccumulate(c, alpha, charges);
	        quad.charge += c.charge;
	        cx += c.charge * c.cx;
	        cy += c.charge * c.cy;
	      }
	    }
	    if (quad.point) {
	      if (!quad.leaf) {
	        quad.point.x += Math.random() - .5;
	        quad.point.y += Math.random() - .5;
	      }
	      var k = alpha * charges[quad.point.index];
	      quad.charge += quad.pointCharge = k;
	      cx += k * quad.point.x;
	      cy += k * quad.point.y;
	    }
	    quad.cx = cx / quad.charge;
	    quad.cy = cy / quad.charge;
	  }
	  var d3_layout_forceLinkDistance = 20, d3_layout_forceLinkStrength = 1, d3_layout_forceChargeDistance2 = Infinity;
	  d3.layout.hierarchy = function() {
	    var sort = d3_layout_hierarchySort, children = d3_layout_hierarchyChildren, value = d3_layout_hierarchyValue;
	    function hierarchy(root) {
	      var stack = [ root ], nodes = [], node;
	      root.depth = 0;
	      while ((node = stack.pop()) != null) {
	        nodes.push(node);
	        if ((childs = children.call(hierarchy, node, node.depth)) && (n = childs.length)) {
	          var n, childs, child;
	          while (--n >= 0) {
	            stack.push(child = childs[n]);
	            child.parent = node;
	            child.depth = node.depth + 1;
	          }
	          if (value) node.value = 0;
	          node.children = childs;
	        } else {
	          if (value) node.value = +value.call(hierarchy, node, node.depth) || 0;
	          delete node.children;
	        }
	      }
	      d3_layout_hierarchyVisitAfter(root, function(node) {
	        var childs, parent;
	        if (sort && (childs = node.children)) childs.sort(sort);
	        if (value && (parent = node.parent)) parent.value += node.value;
	      });
	      return nodes;
	    }
	    hierarchy.sort = function(x) {
	      if (!arguments.length) return sort;
	      sort = x;
	      return hierarchy;
	    };
	    hierarchy.children = function(x) {
	      if (!arguments.length) return children;
	      children = x;
	      return hierarchy;
	    };
	    hierarchy.value = function(x) {
	      if (!arguments.length) return value;
	      value = x;
	      return hierarchy;
	    };
	    hierarchy.revalue = function(root) {
	      if (value) {
	        d3_layout_hierarchyVisitBefore(root, function(node) {
	          if (node.children) node.value = 0;
	        });
	        d3_layout_hierarchyVisitAfter(root, function(node) {
	          var parent;
	          if (!node.children) node.value = +value.call(hierarchy, node, node.depth) || 0;
	          if (parent = node.parent) parent.value += node.value;
	        });
	      }
	      return root;
	    };
	    return hierarchy;
	  };
	  function d3_layout_hierarchyRebind(object, hierarchy) {
	    d3.rebind(object, hierarchy, "sort", "children", "value");
	    object.nodes = object;
	    object.links = d3_layout_hierarchyLinks;
	    return object;
	  }
	  function d3_layout_hierarchyVisitBefore(node, callback) {
	    var nodes = [ node ];
	    while ((node = nodes.pop()) != null) {
	      callback(node);
	      if ((children = node.children) && (n = children.length)) {
	        var n, children;
	        while (--n >= 0) nodes.push(children[n]);
	      }
	    }
	  }
	  function d3_layout_hierarchyVisitAfter(node, callback) {
	    var nodes = [ node ], nodes2 = [];
	    while ((node = nodes.pop()) != null) {
	      nodes2.push(node);
	      if ((children = node.children) && (n = children.length)) {
	        var i = -1, n, children;
	        while (++i < n) nodes.push(children[i]);
	      }
	    }
	    while ((node = nodes2.pop()) != null) {
	      callback(node);
	    }
	  }
	  function d3_layout_hierarchyChildren(d) {
	    return d.children;
	  }
	  function d3_layout_hierarchyValue(d) {
	    return d.value;
	  }
	  function d3_layout_hierarchySort(a, b) {
	    return b.value - a.value;
	  }
	  function d3_layout_hierarchyLinks(nodes) {
	    return d3.merge(nodes.map(function(parent) {
	      return (parent.children || []).map(function(child) {
	        return {
	          source: parent,
	          target: child
	        };
	      });
	    }));
	  }
	  d3.layout.partition = function() {
	    var hierarchy = d3.layout.hierarchy(), size = [ 1, 1 ];
	    function position(node, x, dx, dy) {
	      var children = node.children;
	      node.x = x;
	      node.y = node.depth * dy;
	      node.dx = dx;
	      node.dy = dy;
	      if (children && (n = children.length)) {
	        var i = -1, n, c, d;
	        dx = node.value ? dx / node.value : 0;
	        while (++i < n) {
	          position(c = children[i], x, d = c.value * dx, dy);
	          x += d;
	        }
	      }
	    }
	    function depth(node) {
	      var children = node.children, d = 0;
	      if (children && (n = children.length)) {
	        var i = -1, n;
	        while (++i < n) d = Math.max(d, depth(children[i]));
	      }
	      return 1 + d;
	    }
	    function partition(d, i) {
	      var nodes = hierarchy.call(this, d, i);
	      position(nodes[0], 0, size[0], size[1] / depth(nodes[0]));
	      return nodes;
	    }
	    partition.size = function(x) {
	      if (!arguments.length) return size;
	      size = x;
	      return partition;
	    };
	    return d3_layout_hierarchyRebind(partition, hierarchy);
	  };
	  d3.layout.pie = function() {
	    var value = Number, sort = d3_layout_pieSortByValue, startAngle = 0, endAngle = τ, padAngle = 0;
	    function pie(data) {
	      var n = data.length, values = data.map(function(d, i) {
	        return +value.call(pie, d, i);
	      }), a = +(typeof startAngle === "function" ? startAngle.apply(this, arguments) : startAngle), da = (typeof endAngle === "function" ? endAngle.apply(this, arguments) : endAngle) - a, p = Math.min(Math.abs(da) / n, +(typeof padAngle === "function" ? padAngle.apply(this, arguments) : padAngle)), pa = p * (da < 0 ? -1 : 1), sum = d3.sum(values), k = sum ? (da - n * pa) / sum : 0, index = d3.range(n), arcs = [], v;
	      if (sort != null) index.sort(sort === d3_layout_pieSortByValue ? function(i, j) {
	        return values[j] - values[i];
	      } : function(i, j) {
	        return sort(data[i], data[j]);
	      });
	      index.forEach(function(i) {
	        arcs[i] = {
	          data: data[i],
	          value: v = values[i],
	          startAngle: a,
	          endAngle: a += v * k + pa,
	          padAngle: p
	        };
	      });
	      return arcs;
	    }
	    pie.value = function(_) {
	      if (!arguments.length) return value;
	      value = _;
	      return pie;
	    };
	    pie.sort = function(_) {
	      if (!arguments.length) return sort;
	      sort = _;
	      return pie;
	    };
	    pie.startAngle = function(_) {
	      if (!arguments.length) return startAngle;
	      startAngle = _;
	      return pie;
	    };
	    pie.endAngle = function(_) {
	      if (!arguments.length) return endAngle;
	      endAngle = _;
	      return pie;
	    };
	    pie.padAngle = function(_) {
	      if (!arguments.length) return padAngle;
	      padAngle = _;
	      return pie;
	    };
	    return pie;
	  };
	  var d3_layout_pieSortByValue = {};
	  d3.layout.stack = function() {
	    var values = d3_identity, order = d3_layout_stackOrderDefault, offset = d3_layout_stackOffsetZero, out = d3_layout_stackOut, x = d3_layout_stackX, y = d3_layout_stackY;
	    function stack(data, index) {
	      if (!(n = data.length)) return data;
	      var series = data.map(function(d, i) {
	        return values.call(stack, d, i);
	      });
	      var points = series.map(function(d) {
	        return d.map(function(v, i) {
	          return [ x.call(stack, v, i), y.call(stack, v, i) ];
	        });
	      });
	      var orders = order.call(stack, points, index);
	      series = d3.permute(series, orders);
	      points = d3.permute(points, orders);
	      var offsets = offset.call(stack, points, index);
	      var m = series[0].length, n, i, j, o;
	      for (j = 0; j < m; ++j) {
	        out.call(stack, series[0][j], o = offsets[j], points[0][j][1]);
	        for (i = 1; i < n; ++i) {
	          out.call(stack, series[i][j], o += points[i - 1][j][1], points[i][j][1]);
	        }
	      }
	      return data;
	    }
	    stack.values = function(x) {
	      if (!arguments.length) return values;
	      values = x;
	      return stack;
	    };
	    stack.order = function(x) {
	      if (!arguments.length) return order;
	      order = typeof x === "function" ? x : d3_layout_stackOrders.get(x) || d3_layout_stackOrderDefault;
	      return stack;
	    };
	    stack.offset = function(x) {
	      if (!arguments.length) return offset;
	      offset = typeof x === "function" ? x : d3_layout_stackOffsets.get(x) || d3_layout_stackOffsetZero;
	      return stack;
	    };
	    stack.x = function(z) {
	      if (!arguments.length) return x;
	      x = z;
	      return stack;
	    };
	    stack.y = function(z) {
	      if (!arguments.length) return y;
	      y = z;
	      return stack;
	    };
	    stack.out = function(z) {
	      if (!arguments.length) return out;
	      out = z;
	      return stack;
	    };
	    return stack;
	  };
	  function d3_layout_stackX(d) {
	    return d.x;
	  }
	  function d3_layout_stackY(d) {
	    return d.y;
	  }
	  function d3_layout_stackOut(d, y0, y) {
	    d.y0 = y0;
	    d.y = y;
	  }
	  var d3_layout_stackOrders = d3.map({
	    "inside-out": function(data) {
	      var n = data.length, i, j, max = data.map(d3_layout_stackMaxIndex), sums = data.map(d3_layout_stackReduceSum), index = d3.range(n).sort(function(a, b) {
	        return max[a] - max[b];
	      }), top = 0, bottom = 0, tops = [], bottoms = [];
	      for (i = 0; i < n; ++i) {
	        j = index[i];
	        if (top < bottom) {
	          top += sums[j];
	          tops.push(j);
	        } else {
	          bottom += sums[j];
	          bottoms.push(j);
	        }
	      }
	      return bottoms.reverse().concat(tops);
	    },
	    reverse: function(data) {
	      return d3.range(data.length).reverse();
	    },
	    "default": d3_layout_stackOrderDefault
	  });
	  var d3_layout_stackOffsets = d3.map({
	    silhouette: function(data) {
	      var n = data.length, m = data[0].length, sums = [], max = 0, i, j, o, y0 = [];
	      for (j = 0; j < m; ++j) {
	        for (i = 0, o = 0; i < n; i++) o += data[i][j][1];
	        if (o > max) max = o;
	        sums.push(o);
	      }
	      for (j = 0; j < m; ++j) {
	        y0[j] = (max - sums[j]) / 2;
	      }
	      return y0;
	    },
	    wiggle: function(data) {
	      var n = data.length, x = data[0], m = x.length, i, j, k, s1, s2, s3, dx, o, o0, y0 = [];
	      y0[0] = o = o0 = 0;
	      for (j = 1; j < m; ++j) {
	        for (i = 0, s1 = 0; i < n; ++i) s1 += data[i][j][1];
	        for (i = 0, s2 = 0, dx = x[j][0] - x[j - 1][0]; i < n; ++i) {
	          for (k = 0, s3 = (data[i][j][1] - data[i][j - 1][1]) / (2 * dx); k < i; ++k) {
	            s3 += (data[k][j][1] - data[k][j - 1][1]) / dx;
	          }
	          s2 += s3 * data[i][j][1];
	        }
	        y0[j] = o -= s1 ? s2 / s1 * dx : 0;
	        if (o < o0) o0 = o;
	      }
	      for (j = 0; j < m; ++j) y0[j] -= o0;
	      return y0;
	    },
	    expand: function(data) {
	      var n = data.length, m = data[0].length, k = 1 / n, i, j, o, y0 = [];
	      for (j = 0; j < m; ++j) {
	        for (i = 0, o = 0; i < n; i++) o += data[i][j][1];
	        if (o) for (i = 0; i < n; i++) data[i][j][1] /= o; else for (i = 0; i < n; i++) data[i][j][1] = k;
	      }
	      for (j = 0; j < m; ++j) y0[j] = 0;
	      return y0;
	    },
	    zero: d3_layout_stackOffsetZero
	  });
	  function d3_layout_stackOrderDefault(data) {
	    return d3.range(data.length);
	  }
	  function d3_layout_stackOffsetZero(data) {
	    var j = -1, m = data[0].length, y0 = [];
	    while (++j < m) y0[j] = 0;
	    return y0;
	  }
	  function d3_layout_stackMaxIndex(array) {
	    var i = 1, j = 0, v = array[0][1], k, n = array.length;
	    for (;i < n; ++i) {
	      if ((k = array[i][1]) > v) {
	        j = i;
	        v = k;
	      }
	    }
	    return j;
	  }
	  function d3_layout_stackReduceSum(d) {
	    return d.reduce(d3_layout_stackSum, 0);
	  }
	  function d3_layout_stackSum(p, d) {
	    return p + d[1];
	  }
	  d3.layout.histogram = function() {
	    var frequency = true, valuer = Number, ranger = d3_layout_histogramRange, binner = d3_layout_histogramBinSturges;
	    function histogram(data, i) {
	      var bins = [], values = data.map(valuer, this), range = ranger.call(this, values, i), thresholds = binner.call(this, range, values, i), bin, i = -1, n = values.length, m = thresholds.length - 1, k = frequency ? 1 : 1 / n, x;
	      while (++i < m) {
	        bin = bins[i] = [];
	        bin.dx = thresholds[i + 1] - (bin.x = thresholds[i]);
	        bin.y = 0;
	      }
	      if (m > 0) {
	        i = -1;
	        while (++i < n) {
	          x = values[i];
	          if (x >= range[0] && x <= range[1]) {
	            bin = bins[d3.bisect(thresholds, x, 1, m) - 1];
	            bin.y += k;
	            bin.push(data[i]);
	          }
	        }
	      }
	      return bins;
	    }
	    histogram.value = function(x) {
	      if (!arguments.length) return valuer;
	      valuer = x;
	      return histogram;
	    };
	    histogram.range = function(x) {
	      if (!arguments.length) return ranger;
	      ranger = d3_functor(x);
	      return histogram;
	    };
	    histogram.bins = function(x) {
	      if (!arguments.length) return binner;
	      binner = typeof x === "number" ? function(range) {
	        return d3_layout_histogramBinFixed(range, x);
	      } : d3_functor(x);
	      return histogram;
	    };
	    histogram.frequency = function(x) {
	      if (!arguments.length) return frequency;
	      frequency = !!x;
	      return histogram;
	    };
	    return histogram;
	  };
	  function d3_layout_histogramBinSturges(range, values) {
	    return d3_layout_histogramBinFixed(range, Math.ceil(Math.log(values.length) / Math.LN2 + 1));
	  }
	  function d3_layout_histogramBinFixed(range, n) {
	    var x = -1, b = +range[0], m = (range[1] - b) / n, f = [];
	    while (++x <= n) f[x] = m * x + b;
	    return f;
	  }
	  function d3_layout_histogramRange(values) {
	    return [ d3.min(values), d3.max(values) ];
	  }
	  d3.layout.pack = function() {
	    var hierarchy = d3.layout.hierarchy().sort(d3_layout_packSort), padding = 0, size = [ 1, 1 ], radius;
	    function pack(d, i) {
	      var nodes = hierarchy.call(this, d, i), root = nodes[0], w = size[0], h = size[1], r = radius == null ? Math.sqrt : typeof radius === "function" ? radius : function() {
	        return radius;
	      };
	      root.x = root.y = 0;
	      d3_layout_hierarchyVisitAfter(root, function(d) {
	        d.r = +r(d.value);
	      });
	      d3_layout_hierarchyVisitAfter(root, d3_layout_packSiblings);
	      if (padding) {
	        var dr = padding * (radius ? 1 : Math.max(2 * root.r / w, 2 * root.r / h)) / 2;
	        d3_layout_hierarchyVisitAfter(root, function(d) {
	          d.r += dr;
	        });
	        d3_layout_hierarchyVisitAfter(root, d3_layout_packSiblings);
	        d3_layout_hierarchyVisitAfter(root, function(d) {
	          d.r -= dr;
	        });
	      }
	      d3_layout_packTransform(root, w / 2, h / 2, radius ? 1 : 1 / Math.max(2 * root.r / w, 2 * root.r / h));
	      return nodes;
	    }
	    pack.size = function(_) {
	      if (!arguments.length) return size;
	      size = _;
	      return pack;
	    };
	    pack.radius = function(_) {
	      if (!arguments.length) return radius;
	      radius = _ == null || typeof _ === "function" ? _ : +_;
	      return pack;
	    };
	    pack.padding = function(_) {
	      if (!arguments.length) return padding;
	      padding = +_;
	      return pack;
	    };
	    return d3_layout_hierarchyRebind(pack, hierarchy);
	  };
	  function d3_layout_packSort(a, b) {
	    return a.value - b.value;
	  }
	  function d3_layout_packInsert(a, b) {
	    var c = a._pack_next;
	    a._pack_next = b;
	    b._pack_prev = a;
	    b._pack_next = c;
	    c._pack_prev = b;
	  }
	  function d3_layout_packSplice(a, b) {
	    a._pack_next = b;
	    b._pack_prev = a;
	  }
	  function d3_layout_packIntersects(a, b) {
	    var dx = b.x - a.x, dy = b.y - a.y, dr = a.r + b.r;
	    return .999 * dr * dr > dx * dx + dy * dy;
	  }
	  function d3_layout_packSiblings(node) {
	    if (!(nodes = node.children) || !(n = nodes.length)) return;
	    var nodes, xMin = Infinity, xMax = -Infinity, yMin = Infinity, yMax = -Infinity, a, b, c, i, j, k, n;
	    function bound(node) {
	      xMin = Math.min(node.x - node.r, xMin);
	      xMax = Math.max(node.x + node.r, xMax);
	      yMin = Math.min(node.y - node.r, yMin);
	      yMax = Math.max(node.y + node.r, yMax);
	    }
	    nodes.forEach(d3_layout_packLink);
	    a = nodes[0];
	    a.x = -a.r;
	    a.y = 0;
	    bound(a);
	    if (n > 1) {
	      b = nodes[1];
	      b.x = b.r;
	      b.y = 0;
	      bound(b);
	      if (n > 2) {
	        c = nodes[2];
	        d3_layout_packPlace(a, b, c);
	        bound(c);
	        d3_layout_packInsert(a, c);
	        a._pack_prev = c;
	        d3_layout_packInsert(c, b);
	        b = a._pack_next;
	        for (i = 3; i < n; i++) {
	          d3_layout_packPlace(a, b, c = nodes[i]);
	          var isect = 0, s1 = 1, s2 = 1;
	          for (j = b._pack_next; j !== b; j = j._pack_next, s1++) {
	            if (d3_layout_packIntersects(j, c)) {
	              isect = 1;
	              break;
	            }
	          }
	          if (isect == 1) {
	            for (k = a._pack_prev; k !== j._pack_prev; k = k._pack_prev, s2++) {
	              if (d3_layout_packIntersects(k, c)) {
	                break;
	              }
	            }
	          }
	          if (isect) {
	            if (s1 < s2 || s1 == s2 && b.r < a.r) d3_layout_packSplice(a, b = j); else d3_layout_packSplice(a = k, b);
	            i--;
	          } else {
	            d3_layout_packInsert(a, c);
	            b = c;
	            bound(c);
	          }
	        }
	      }
	    }
	    var cx = (xMin + xMax) / 2, cy = (yMin + yMax) / 2, cr = 0;
	    for (i = 0; i < n; i++) {
	      c = nodes[i];
	      c.x -= cx;
	      c.y -= cy;
	      cr = Math.max(cr, c.r + Math.sqrt(c.x * c.x + c.y * c.y));
	    }
	    node.r = cr;
	    nodes.forEach(d3_layout_packUnlink);
	  }
	  function d3_layout_packLink(node) {
	    node._pack_next = node._pack_prev = node;
	  }
	  function d3_layout_packUnlink(node) {
	    delete node._pack_next;
	    delete node._pack_prev;
	  }
	  function d3_layout_packTransform(node, x, y, k) {
	    var children = node.children;
	    node.x = x += k * node.x;
	    node.y = y += k * node.y;
	    node.r *= k;
	    if (children) {
	      var i = -1, n = children.length;
	      while (++i < n) d3_layout_packTransform(children[i], x, y, k);
	    }
	  }
	  function d3_layout_packPlace(a, b, c) {
	    var db = a.r + c.r, dx = b.x - a.x, dy = b.y - a.y;
	    if (db && (dx || dy)) {
	      var da = b.r + c.r, dc = dx * dx + dy * dy;
	      da *= da;
	      db *= db;
	      var x = .5 + (db - da) / (2 * dc), y = Math.sqrt(Math.max(0, 2 * da * (db + dc) - (db -= dc) * db - da * da)) / (2 * dc);
	      c.x = a.x + x * dx + y * dy;
	      c.y = a.y + x * dy - y * dx;
	    } else {
	      c.x = a.x + db;
	      c.y = a.y;
	    }
	  }
	  d3.layout.tree = function() {
	    var hierarchy = d3.layout.hierarchy().sort(null).value(null), separation = d3_layout_treeSeparation, size = [ 1, 1 ], nodeSize = null;
	    function tree(d, i) {
	      var nodes = hierarchy.call(this, d, i), root0 = nodes[0], root1 = wrapTree(root0);
	      d3_layout_hierarchyVisitAfter(root1, firstWalk), root1.parent.m = -root1.z;
	      d3_layout_hierarchyVisitBefore(root1, secondWalk);
	      if (nodeSize) d3_layout_hierarchyVisitBefore(root0, sizeNode); else {
	        var left = root0, right = root0, bottom = root0;
	        d3_layout_hierarchyVisitBefore(root0, function(node) {
	          if (node.x < left.x) left = node;
	          if (node.x > right.x) right = node;
	          if (node.depth > bottom.depth) bottom = node;
	        });
	        var tx = separation(left, right) / 2 - left.x, kx = size[0] / (right.x + separation(right, left) / 2 + tx), ky = size[1] / (bottom.depth || 1);
	        d3_layout_hierarchyVisitBefore(root0, function(node) {
	          node.x = (node.x + tx) * kx;
	          node.y = node.depth * ky;
	        });
	      }
	      return nodes;
	    }
	    function wrapTree(root0) {
	      var root1 = {
	        A: null,
	        children: [ root0 ]
	      }, queue = [ root1 ], node1;
	      while ((node1 = queue.pop()) != null) {
	        for (var children = node1.children, child, i = 0, n = children.length; i < n; ++i) {
	          queue.push((children[i] = child = {
	            _: children[i],
	            parent: node1,
	            children: (child = children[i].children) && child.slice() || [],
	            A: null,
	            a: null,
	            z: 0,
	            m: 0,
	            c: 0,
	            s: 0,
	            t: null,
	            i: i
	          }).a = child);
	        }
	      }
	      return root1.children[0];
	    }
	    function firstWalk(v) {
	      var children = v.children, siblings = v.parent.children, w = v.i ? siblings[v.i - 1] : null;
	      if (children.length) {
	        d3_layout_treeShift(v);
	        var midpoint = (children[0].z + children[children.length - 1].z) / 2;
	        if (w) {
	          v.z = w.z + separation(v._, w._);
	          v.m = v.z - midpoint;
	        } else {
	          v.z = midpoint;
	        }
	      } else if (w) {
	        v.z = w.z + separation(v._, w._);
	      }
	      v.parent.A = apportion(v, w, v.parent.A || siblings[0]);
	    }
	    function secondWalk(v) {
	      v._.x = v.z + v.parent.m;
	      v.m += v.parent.m;
	    }
	    function apportion(v, w, ancestor) {
	      if (w) {
	        var vip = v, vop = v, vim = w, vom = vip.parent.children[0], sip = vip.m, sop = vop.m, sim = vim.m, som = vom.m, shift;
	        while (vim = d3_layout_treeRight(vim), vip = d3_layout_treeLeft(vip), vim && vip) {
	          vom = d3_layout_treeLeft(vom);
	          vop = d3_layout_treeRight(vop);
	          vop.a = v;
	          shift = vim.z + sim - vip.z - sip + separation(vim._, vip._);
	          if (shift > 0) {
	            d3_layout_treeMove(d3_layout_treeAncestor(vim, v, ancestor), v, shift);
	            sip += shift;
	            sop += shift;
	          }
	          sim += vim.m;
	          sip += vip.m;
	          som += vom.m;
	          sop += vop.m;
	        }
	        if (vim && !d3_layout_treeRight(vop)) {
	          vop.t = vim;
	          vop.m += sim - sop;
	        }
	        if (vip && !d3_layout_treeLeft(vom)) {
	          vom.t = vip;
	          vom.m += sip - som;
	          ancestor = v;
	        }
	      }
	      return ancestor;
	    }
	    function sizeNode(node) {
	      node.x *= size[0];
	      node.y = node.depth * size[1];
	    }
	    tree.separation = function(x) {
	      if (!arguments.length) return separation;
	      separation = x;
	      return tree;
	    };
	    tree.size = function(x) {
	      if (!arguments.length) return nodeSize ? null : size;
	      nodeSize = (size = x) == null ? sizeNode : null;
	      return tree;
	    };
	    tree.nodeSize = function(x) {
	      if (!arguments.length) return nodeSize ? size : null;
	      nodeSize = (size = x) == null ? null : sizeNode;
	      return tree;
	    };
	    return d3_layout_hierarchyRebind(tree, hierarchy);
	  };
	  function d3_layout_treeSeparation(a, b) {
	    return a.parent == b.parent ? 1 : 2;
	  }
	  function d3_layout_treeLeft(v) {
	    var children = v.children;
	    return children.length ? children[0] : v.t;
	  }
	  function d3_layout_treeRight(v) {
	    var children = v.children, n;
	    return (n = children.length) ? children[n - 1] : v.t;
	  }
	  function d3_layout_treeMove(wm, wp, shift) {
	    var change = shift / (wp.i - wm.i);
	    wp.c -= change;
	    wp.s += shift;
	    wm.c += change;
	    wp.z += shift;
	    wp.m += shift;
	  }
	  function d3_layout_treeShift(v) {
	    var shift = 0, change = 0, children = v.children, i = children.length, w;
	    while (--i >= 0) {
	      w = children[i];
	      w.z += shift;
	      w.m += shift;
	      shift += w.s + (change += w.c);
	    }
	  }
	  function d3_layout_treeAncestor(vim, v, ancestor) {
	    return vim.a.parent === v.parent ? vim.a : ancestor;
	  }
	  d3.layout.cluster = function() {
	    var hierarchy = d3.layout.hierarchy().sort(null).value(null), separation = d3_layout_treeSeparation, size = [ 1, 1 ], nodeSize = false;
	    function cluster(d, i) {
	      var nodes = hierarchy.call(this, d, i), root = nodes[0], previousNode, x = 0;
	      d3_layout_hierarchyVisitAfter(root, function(node) {
	        var children = node.children;
	        if (children && children.length) {
	          node.x = d3_layout_clusterX(children);
	          node.y = d3_layout_clusterY(children);
	        } else {
	          node.x = previousNode ? x += separation(node, previousNode) : 0;
	          node.y = 0;
	          previousNode = node;
	        }
	      });
	      var left = d3_layout_clusterLeft(root), right = d3_layout_clusterRight(root), x0 = left.x - separation(left, right) / 2, x1 = right.x + separation(right, left) / 2;
	      d3_layout_hierarchyVisitAfter(root, nodeSize ? function(node) {
	        node.x = (node.x - root.x) * size[0];
	        node.y = (root.y - node.y) * size[1];
	      } : function(node) {
	        node.x = (node.x - x0) / (x1 - x0) * size[0];
	        node.y = (1 - (root.y ? node.y / root.y : 1)) * size[1];
	      });
	      return nodes;
	    }
	    cluster.separation = function(x) {
	      if (!arguments.length) return separation;
	      separation = x;
	      return cluster;
	    };
	    cluster.size = function(x) {
	      if (!arguments.length) return nodeSize ? null : size;
	      nodeSize = (size = x) == null;
	      return cluster;
	    };
	    cluster.nodeSize = function(x) {
	      if (!arguments.length) return nodeSize ? size : null;
	      nodeSize = (size = x) != null;
	      return cluster;
	    };
	    return d3_layout_hierarchyRebind(cluster, hierarchy);
	  };
	  function d3_layout_clusterY(children) {
	    return 1 + d3.max(children, function(child) {
	      return child.y;
	    });
	  }
	  function d3_layout_clusterX(children) {
	    return children.reduce(function(x, child) {
	      return x + child.x;
	    }, 0) / children.length;
	  }
	  function d3_layout_clusterLeft(node) {
	    var children = node.children;
	    return children && children.length ? d3_layout_clusterLeft(children[0]) : node;
	  }
	  function d3_layout_clusterRight(node) {
	    var children = node.children, n;
	    return children && (n = children.length) ? d3_layout_clusterRight(children[n - 1]) : node;
	  }
	  d3.layout.treemap = function() {
	    var hierarchy = d3.layout.hierarchy(), round = Math.round, size = [ 1, 1 ], padding = null, pad = d3_layout_treemapPadNull, sticky = false, stickies, mode = "squarify", ratio = .5 * (1 + Math.sqrt(5));
	    function scale(children, k) {
	      var i = -1, n = children.length, child, area;
	      while (++i < n) {
	        area = (child = children[i]).value * (k < 0 ? 0 : k);
	        child.area = isNaN(area) || area <= 0 ? 0 : area;
	      }
	    }
	    function squarify(node) {
	      var children = node.children;
	      if (children && children.length) {
	        var rect = pad(node), row = [], remaining = children.slice(), child, best = Infinity, score, u = mode === "slice" ? rect.dx : mode === "dice" ? rect.dy : mode === "slice-dice" ? node.depth & 1 ? rect.dy : rect.dx : Math.min(rect.dx, rect.dy), n;
	        scale(remaining, rect.dx * rect.dy / node.value);
	        row.area = 0;
	        while ((n = remaining.length) > 0) {
	          row.push(child = remaining[n - 1]);
	          row.area += child.area;
	          if (mode !== "squarify" || (score = worst(row, u)) <= best) {
	            remaining.pop();
	            best = score;
	          } else {
	            row.area -= row.pop().area;
	            position(row, u, rect, false);
	            u = Math.min(rect.dx, rect.dy);
	            row.length = row.area = 0;
	            best = Infinity;
	          }
	        }
	        if (row.length) {
	          position(row, u, rect, true);
	          row.length = row.area = 0;
	        }
	        children.forEach(squarify);
	      }
	    }
	    function stickify(node) {
	      var children = node.children;
	      if (children && children.length) {
	        var rect = pad(node), remaining = children.slice(), child, row = [];
	        scale(remaining, rect.dx * rect.dy / node.value);
	        row.area = 0;
	        while (child = remaining.pop()) {
	          row.push(child);
	          row.area += child.area;
	          if (child.z != null) {
	            position(row, child.z ? rect.dx : rect.dy, rect, !remaining.length);
	            row.length = row.area = 0;
	          }
	        }
	        children.forEach(stickify);
	      }
	    }
	    function worst(row, u) {
	      var s = row.area, r, rmax = 0, rmin = Infinity, i = -1, n = row.length;
	      while (++i < n) {
	        if (!(r = row[i].area)) continue;
	        if (r < rmin) rmin = r;
	        if (r > rmax) rmax = r;
	      }
	      s *= s;
	      u *= u;
	      return s ? Math.max(u * rmax * ratio / s, s / (u * rmin * ratio)) : Infinity;
	    }
	    function position(row, u, rect, flush) {
	      var i = -1, n = row.length, x = rect.x, y = rect.y, v = u ? round(row.area / u) : 0, o;
	      if (u == rect.dx) {
	        if (flush || v > rect.dy) v = rect.dy;
	        while (++i < n) {
	          o = row[i];
	          o.x = x;
	          o.y = y;
	          o.dy = v;
	          x += o.dx = Math.min(rect.x + rect.dx - x, v ? round(o.area / v) : 0);
	        }
	        o.z = true;
	        o.dx += rect.x + rect.dx - x;
	        rect.y += v;
	        rect.dy -= v;
	      } else {
	        if (flush || v > rect.dx) v = rect.dx;
	        while (++i < n) {
	          o = row[i];
	          o.x = x;
	          o.y = y;
	          o.dx = v;
	          y += o.dy = Math.min(rect.y + rect.dy - y, v ? round(o.area / v) : 0);
	        }
	        o.z = false;
	        o.dy += rect.y + rect.dy - y;
	        rect.x += v;
	        rect.dx -= v;
	      }
	    }
	    function treemap(d) {
	      var nodes = stickies || hierarchy(d), root = nodes[0];
	      root.x = root.y = 0;
	      if (root.value) root.dx = size[0], root.dy = size[1]; else root.dx = root.dy = 0;
	      if (stickies) hierarchy.revalue(root);
	      scale([ root ], root.dx * root.dy / root.value);
	      (stickies ? stickify : squarify)(root);
	      if (sticky) stickies = nodes;
	      return nodes;
	    }
	    treemap.size = function(x) {
	      if (!arguments.length) return size;
	      size = x;
	      return treemap;
	    };
	    treemap.padding = function(x) {
	      if (!arguments.length) return padding;
	      function padFunction(node) {
	        var p = x.call(treemap, node, node.depth);
	        return p == null ? d3_layout_treemapPadNull(node) : d3_layout_treemapPad(node, typeof p === "number" ? [ p, p, p, p ] : p);
	      }
	      function padConstant(node) {
	        return d3_layout_treemapPad(node, x);
	      }
	      var type;
	      pad = (padding = x) == null ? d3_layout_treemapPadNull : (type = typeof x) === "function" ? padFunction : type === "number" ? (x = [ x, x, x, x ], 
	      padConstant) : padConstant;
	      return treemap;
	    };
	    treemap.round = function(x) {
	      if (!arguments.length) return round != Number;
	      round = x ? Math.round : Number;
	      return treemap;
	    };
	    treemap.sticky = function(x) {
	      if (!arguments.length) return sticky;
	      sticky = x;
	      stickies = null;
	      return treemap;
	    };
	    treemap.ratio = function(x) {
	      if (!arguments.length) return ratio;
	      ratio = x;
	      return treemap;
	    };
	    treemap.mode = function(x) {
	      if (!arguments.length) return mode;
	      mode = x + "";
	      return treemap;
	    };
	    return d3_layout_hierarchyRebind(treemap, hierarchy);
	  };
	  function d3_layout_treemapPadNull(node) {
	    return {
	      x: node.x,
	      y: node.y,
	      dx: node.dx,
	      dy: node.dy
	    };
	  }
	  function d3_layout_treemapPad(node, padding) {
	    var x = node.x + padding[3], y = node.y + padding[0], dx = node.dx - padding[1] - padding[3], dy = node.dy - padding[0] - padding[2];
	    if (dx < 0) {
	      x += dx / 2;
	      dx = 0;
	    }
	    if (dy < 0) {
	      y += dy / 2;
	      dy = 0;
	    }
	    return {
	      x: x,
	      y: y,
	      dx: dx,
	      dy: dy
	    };
	  }
	  d3.random = {
	    normal: function(µ, σ) {
	      var n = arguments.length;
	      if (n < 2) σ = 1;
	      if (n < 1) µ = 0;
	      return function() {
	        var x, y, r;
	        do {
	          x = Math.random() * 2 - 1;
	          y = Math.random() * 2 - 1;
	          r = x * x + y * y;
	        } while (!r || r > 1);
	        return µ + σ * x * Math.sqrt(-2 * Math.log(r) / r);
	      };
	    },
	    logNormal: function() {
	      var random = d3.random.normal.apply(d3, arguments);
	      return function() {
	        return Math.exp(random());
	      };
	    },
	    bates: function(m) {
	      var random = d3.random.irwinHall(m);
	      return function() {
	        return random() / m;
	      };
	    },
	    irwinHall: function(m) {
	      return function() {
	        for (var s = 0, j = 0; j < m; j++) s += Math.random();
	        return s;
	      };
	    }
	  };
	  d3.scale = {};
	  function d3_scaleExtent(domain) {
	    var start = domain[0], stop = domain[domain.length - 1];
	    return start < stop ? [ start, stop ] : [ stop, start ];
	  }
	  function d3_scaleRange(scale) {
	    return scale.rangeExtent ? scale.rangeExtent() : d3_scaleExtent(scale.range());
	  }
	  function d3_scale_bilinear(domain, range, uninterpolate, interpolate) {
	    var u = uninterpolate(domain[0], domain[1]), i = interpolate(range[0], range[1]);
	    return function(x) {
	      return i(u(x));
	    };
	  }
	  function d3_scale_nice(domain, nice) {
	    var i0 = 0, i1 = domain.length - 1, x0 = domain[i0], x1 = domain[i1], dx;
	    if (x1 < x0) {
	      dx = i0, i0 = i1, i1 = dx;
	      dx = x0, x0 = x1, x1 = dx;
	    }
	    domain[i0] = nice.floor(x0);
	    domain[i1] = nice.ceil(x1);
	    return domain;
	  }
	  function d3_scale_niceStep(step) {
	    return step ? {
	      floor: function(x) {
	        return Math.floor(x / step) * step;
	      },
	      ceil: function(x) {
	        return Math.ceil(x / step) * step;
	      }
	    } : d3_scale_niceIdentity;
	  }
	  var d3_scale_niceIdentity = {
	    floor: d3_identity,
	    ceil: d3_identity
	  };
	  function d3_scale_polylinear(domain, range, uninterpolate, interpolate) {
	    var u = [], i = [], j = 0, k = Math.min(domain.length, range.length) - 1;
	    if (domain[k] < domain[0]) {
	      domain = domain.slice().reverse();
	      range = range.slice().reverse();
	    }
	    while (++j <= k) {
	      u.push(uninterpolate(domain[j - 1], domain[j]));
	      i.push(interpolate(range[j - 1], range[j]));
	    }
	    return function(x) {
	      var j = d3.bisect(domain, x, 1, k) - 1;
	      return i[j](u[j](x));
	    };
	  }
	  d3.scale.linear = function() {
	    return d3_scale_linear([ 0, 1 ], [ 0, 1 ], d3_interpolate, false);
	  };
	  function d3_scale_linear(domain, range, interpolate, clamp) {
	    var output, input;
	    function rescale() {
	      var linear = Math.min(domain.length, range.length) > 2 ? d3_scale_polylinear : d3_scale_bilinear, uninterpolate = clamp ? d3_uninterpolateClamp : d3_uninterpolateNumber;
	      output = linear(domain, range, uninterpolate, interpolate);
	      input = linear(range, domain, uninterpolate, d3_interpolate);
	      return scale;
	    }
	    function scale(x) {
	      return output(x);
	    }
	    scale.invert = function(y) {
	      return input(y);
	    };
	    scale.domain = function(x) {
	      if (!arguments.length) return domain;
	      domain = x.map(Number);
	      return rescale();
	    };
	    scale.range = function(x) {
	      if (!arguments.length) return range;
	      range = x;
	      return rescale();
	    };
	    scale.rangeRound = function(x) {
	      return scale.range(x).interpolate(d3_interpolateRound);
	    };
	    scale.clamp = function(x) {
	      if (!arguments.length) return clamp;
	      clamp = x;
	      return rescale();
	    };
	    scale.interpolate = function(x) {
	      if (!arguments.length) return interpolate;
	      interpolate = x;
	      return rescale();
	    };
	    scale.ticks = function(m) {
	      return d3_scale_linearTicks(domain, m);
	    };
	    scale.tickFormat = function(m, format) {
	      return d3_scale_linearTickFormat(domain, m, format);
	    };
	    scale.nice = function(m) {
	      d3_scale_linearNice(domain, m);
	      return rescale();
	    };
	    scale.copy = function() {
	      return d3_scale_linear(domain, range, interpolate, clamp);
	    };
	    return rescale();
	  }
	  function d3_scale_linearRebind(scale, linear) {
	    return d3.rebind(scale, linear, "range", "rangeRound", "interpolate", "clamp");
	  }
	  function d3_scale_linearNice(domain, m) {
	    d3_scale_nice(domain, d3_scale_niceStep(d3_scale_linearTickRange(domain, m)[2]));
	    d3_scale_nice(domain, d3_scale_niceStep(d3_scale_linearTickRange(domain, m)[2]));
	    return domain;
	  }
	  function d3_scale_linearTickRange(domain, m) {
	    if (m == null) m = 10;
	    var extent = d3_scaleExtent(domain), span = extent[1] - extent[0], step = Math.pow(10, Math.floor(Math.log(span / m) / Math.LN10)), err = m / span * step;
	    if (err <= .15) step *= 10; else if (err <= .35) step *= 5; else if (err <= .75) step *= 2;
	    extent[0] = Math.ceil(extent[0] / step) * step;
	    extent[1] = Math.floor(extent[1] / step) * step + step * .5;
	    extent[2] = step;
	    return extent;
	  }
	  function d3_scale_linearTicks(domain, m) {
	    return d3.range.apply(d3, d3_scale_linearTickRange(domain, m));
	  }
	  function d3_scale_linearTickFormat(domain, m, format) {
	    var range = d3_scale_linearTickRange(domain, m);
	    if (format) {
	      var match = d3_format_re.exec(format);
	      match.shift();
	      if (match[8] === "s") {
	        var prefix = d3.formatPrefix(Math.max(abs(range[0]), abs(range[1])));
	        if (!match[7]) match[7] = "." + d3_scale_linearPrecision(prefix.scale(range[2]));
	        match[8] = "f";
	        format = d3.format(match.join(""));
	        return function(d) {
	          return format(prefix.scale(d)) + prefix.symbol;
	        };
	      }
	      if (!match[7]) match[7] = "." + d3_scale_linearFormatPrecision(match[8], range);
	      format = match.join("");
	    } else {
	      format = ",." + d3_scale_linearPrecision(range[2]) + "f";
	    }
	    return d3.format(format);
	  }
	  var d3_scale_linearFormatSignificant = {
	    s: 1,
	    g: 1,
	    p: 1,
	    r: 1,
	    e: 1
	  };
	  function d3_scale_linearPrecision(value) {
	    return -Math.floor(Math.log(value) / Math.LN10 + .01);
	  }
	  function d3_scale_linearFormatPrecision(type, range) {
	    var p = d3_scale_linearPrecision(range[2]);
	    return type in d3_scale_linearFormatSignificant ? Math.abs(p - d3_scale_linearPrecision(Math.max(abs(range[0]), abs(range[1])))) + +(type !== "e") : p - (type === "%") * 2;
	  }
	  d3.scale.log = function() {
	    return d3_scale_log(d3.scale.linear().domain([ 0, 1 ]), 10, true, [ 1, 10 ]);
	  };
	  function d3_scale_log(linear, base, positive, domain) {
	    function log(x) {
	      return (positive ? Math.log(x < 0 ? 0 : x) : -Math.log(x > 0 ? 0 : -x)) / Math.log(base);
	    }
	    function pow(x) {
	      return positive ? Math.pow(base, x) : -Math.pow(base, -x);
	    }
	    function scale(x) {
	      return linear(log(x));
	    }
	    scale.invert = function(x) {
	      return pow(linear.invert(x));
	    };
	    scale.domain = function(x) {
	      if (!arguments.length) return domain;
	      positive = x[0] >= 0;
	      linear.domain((domain = x.map(Number)).map(log));
	      return scale;
	    };
	    scale.base = function(_) {
	      if (!arguments.length) return base;
	      base = +_;
	      linear.domain(domain.map(log));
	      return scale;
	    };
	    scale.nice = function() {
	      var niced = d3_scale_nice(domain.map(log), positive ? Math : d3_scale_logNiceNegative);
	      linear.domain(niced);
	      domain = niced.map(pow);
	      return scale;
	    };
	    scale.ticks = function() {
	      var extent = d3_scaleExtent(domain), ticks = [], u = extent[0], v = extent[1], i = Math.floor(log(u)), j = Math.ceil(log(v)), n = base % 1 ? 2 : base;
	      if (isFinite(j - i)) {
	        if (positive) {
	          for (;i < j; i++) for (var k = 1; k < n; k++) ticks.push(pow(i) * k);
	          ticks.push(pow(i));
	        } else {
	          ticks.push(pow(i));
	          for (;i++ < j; ) for (var k = n - 1; k > 0; k--) ticks.push(pow(i) * k);
	        }
	        for (i = 0; ticks[i] < u; i++) {}
	        for (j = ticks.length; ticks[j - 1] > v; j--) {}
	        ticks = ticks.slice(i, j);
	      }
	      return ticks;
	    };
	    scale.tickFormat = function(n, format) {
	      if (!arguments.length) return d3_scale_logFormat;
	      if (arguments.length < 2) format = d3_scale_logFormat; else if (typeof format !== "function") format = d3.format(format);
	      var k = Math.max(1, base * n / scale.ticks().length);
	      return function(d) {
	        var i = d / pow(Math.round(log(d)));
	        if (i * base < base - .5) i *= base;
	        return i <= k ? format(d) : "";
	      };
	    };
	    scale.copy = function() {
	      return d3_scale_log(linear.copy(), base, positive, domain);
	    };
	    return d3_scale_linearRebind(scale, linear);
	  }
	  var d3_scale_logFormat = d3.format(".0e"), d3_scale_logNiceNegative = {
	    floor: function(x) {
	      return -Math.ceil(-x);
	    },
	    ceil: function(x) {
	      return -Math.floor(-x);
	    }
	  };
	  d3.scale.pow = function() {
	    return d3_scale_pow(d3.scale.linear(), 1, [ 0, 1 ]);
	  };
	  function d3_scale_pow(linear, exponent, domain) {
	    var powp = d3_scale_powPow(exponent), powb = d3_scale_powPow(1 / exponent);
	    function scale(x) {
	      return linear(powp(x));
	    }
	    scale.invert = function(x) {
	      return powb(linear.invert(x));
	    };
	    scale.domain = function(x) {
	      if (!arguments.length) return domain;
	      linear.domain((domain = x.map(Number)).map(powp));
	      return scale;
	    };
	    scale.ticks = function(m) {
	      return d3_scale_linearTicks(domain, m);
	    };
	    scale.tickFormat = function(m, format) {
	      return d3_scale_linearTickFormat(domain, m, format);
	    };
	    scale.nice = function(m) {
	      return scale.domain(d3_scale_linearNice(domain, m));
	    };
	    scale.exponent = function(x) {
	      if (!arguments.length) return exponent;
	      powp = d3_scale_powPow(exponent = x);
	      powb = d3_scale_powPow(1 / exponent);
	      linear.domain(domain.map(powp));
	      return scale;
	    };
	    scale.copy = function() {
	      return d3_scale_pow(linear.copy(), exponent, domain);
	    };
	    return d3_scale_linearRebind(scale, linear);
	  }
	  function d3_scale_powPow(e) {
	    return function(x) {
	      return x < 0 ? -Math.pow(-x, e) : Math.pow(x, e);
	    };
	  }
	  d3.scale.sqrt = function() {
	    return d3.scale.pow().exponent(.5);
	  };
	  d3.scale.ordinal = function() {
	    return d3_scale_ordinal([], {
	      t: "range",
	      a: [ [] ]
	    });
	  };
	  function d3_scale_ordinal(domain, ranger) {
	    var index, range, rangeBand;
	    function scale(x) {
	      return range[((index.get(x) || (ranger.t === "range" ? index.set(x, domain.push(x)) : NaN)) - 1) % range.length];
	    }
	    function steps(start, step) {
	      return d3.range(domain.length).map(function(i) {
	        return start + step * i;
	      });
	    }
	    scale.domain = function(x) {
	      if (!arguments.length) return domain;
	      domain = [];
	      index = new d3_Map();
	      var i = -1, n = x.length, xi;
	      while (++i < n) if (!index.has(xi = x[i])) index.set(xi, domain.push(xi));
	      return scale[ranger.t].apply(scale, ranger.a);
	    };
	    scale.range = function(x) {
	      if (!arguments.length) return range;
	      range = x;
	      rangeBand = 0;
	      ranger = {
	        t: "range",
	        a: arguments
	      };
	      return scale;
	    };
	    scale.rangePoints = function(x, padding) {
	      if (arguments.length < 2) padding = 0;
	      var start = x[0], stop = x[1], step = domain.length < 2 ? (start = (start + stop) / 2, 
	      0) : (stop - start) / (domain.length - 1 + padding);
	      range = steps(start + step * padding / 2, step);
	      rangeBand = 0;
	      ranger = {
	        t: "rangePoints",
	        a: arguments
	      };
	      return scale;
	    };
	    scale.rangeRoundPoints = function(x, padding) {
	      if (arguments.length < 2) padding = 0;
	      var start = x[0], stop = x[1], step = domain.length < 2 ? (start = stop = Math.round((start + stop) / 2), 
	      0) : (stop - start) / (domain.length - 1 + padding) | 0;
	      range = steps(start + Math.round(step * padding / 2 + (stop - start - (domain.length - 1 + padding) * step) / 2), step);
	      rangeBand = 0;
	      ranger = {
	        t: "rangeRoundPoints",
	        a: arguments
	      };
	      return scale;
	    };
	    scale.rangeBands = function(x, padding, outerPadding) {
	      if (arguments.length < 2) padding = 0;
	      if (arguments.length < 3) outerPadding = padding;
	      var reverse = x[1] < x[0], start = x[reverse - 0], stop = x[1 - reverse], step = (stop - start) / (domain.length - padding + 2 * outerPadding);
	      range = steps(start + step * outerPadding, step);
	      if (reverse) range.reverse();
	      rangeBand = step * (1 - padding);
	      ranger = {
	        t: "rangeBands",
	        a: arguments
	      };
	      return scale;
	    };
	    scale.rangeRoundBands = function(x, padding, outerPadding) {
	      if (arguments.length < 2) padding = 0;
	      if (arguments.length < 3) outerPadding = padding;
	      var reverse = x[1] < x[0], start = x[reverse - 0], stop = x[1 - reverse], step = Math.floor((stop - start) / (domain.length - padding + 2 * outerPadding));
	      range = steps(start + Math.round((stop - start - (domain.length - padding) * step) / 2), step);
	      if (reverse) range.reverse();
	      rangeBand = Math.round(step * (1 - padding));
	      ranger = {
	        t: "rangeRoundBands",
	        a: arguments
	      };
	      return scale;
	    };
	    scale.rangeBand = function() {
	      return rangeBand;
	    };
	    scale.rangeExtent = function() {
	      return d3_scaleExtent(ranger.a[0]);
	    };
	    scale.copy = function() {
	      return d3_scale_ordinal(domain, ranger);
	    };
	    return scale.domain(domain);
	  }
	  d3.scale.category10 = function() {
	    return d3.scale.ordinal().range(d3_category10);
	  };
	  d3.scale.category20 = function() {
	    return d3.scale.ordinal().range(d3_category20);
	  };
	  d3.scale.category20b = function() {
	    return d3.scale.ordinal().range(d3_category20b);
	  };
	  d3.scale.category20c = function() {
	    return d3.scale.ordinal().range(d3_category20c);
	  };
	  var d3_category10 = [ 2062260, 16744206, 2924588, 14034728, 9725885, 9197131, 14907330, 8355711, 12369186, 1556175 ].map(d3_rgbString);
	  var d3_category20 = [ 2062260, 11454440, 16744206, 16759672, 2924588, 10018698, 14034728, 16750742, 9725885, 12955861, 9197131, 12885140, 14907330, 16234194, 8355711, 13092807, 12369186, 14408589, 1556175, 10410725 ].map(d3_rgbString);
	  var d3_category20b = [ 3750777, 5395619, 7040719, 10264286, 6519097, 9216594, 11915115, 13556636, 9202993, 12426809, 15186514, 15190932, 8666169, 11356490, 14049643, 15177372, 8077683, 10834324, 13528509, 14589654 ].map(d3_rgbString);
	  var d3_category20c = [ 3244733, 7057110, 10406625, 13032431, 15095053, 16616764, 16625259, 16634018, 3253076, 7652470, 10607003, 13101504, 7695281, 10394312, 12369372, 14342891, 6513507, 9868950, 12434877, 14277081 ].map(d3_rgbString);
	  d3.scale.quantile = function() {
	    return d3_scale_quantile([], []);
	  };
	  function d3_scale_quantile(domain, range) {
	    var thresholds;
	    function rescale() {
	      var k = 0, q = range.length;
	      thresholds = [];
	      while (++k < q) thresholds[k - 1] = d3.quantile(domain, k / q);
	      return scale;
	    }
	    function scale(x) {
	      if (!isNaN(x = +x)) return range[d3.bisect(thresholds, x)];
	    }
	    scale.domain = function(x) {
	      if (!arguments.length) return domain;
	      domain = x.map(d3_number).filter(d3_numeric).sort(d3_ascending);
	      return rescale();
	    };
	    scale.range = function(x) {
	      if (!arguments.length) return range;
	      range = x;
	      return rescale();
	    };
	    scale.quantiles = function() {
	      return thresholds;
	    };
	    scale.invertExtent = function(y) {
	      y = range.indexOf(y);
	      return y < 0 ? [ NaN, NaN ] : [ y > 0 ? thresholds[y - 1] : domain[0], y < thresholds.length ? thresholds[y] : domain[domain.length - 1] ];
	    };
	    scale.copy = function() {
	      return d3_scale_quantile(domain, range);
	    };
	    return rescale();
	  }
	  d3.scale.quantize = function() {
	    return d3_scale_quantize(0, 1, [ 0, 1 ]);
	  };
	  function d3_scale_quantize(x0, x1, range) {
	    var kx, i;
	    function scale(x) {
	      return range[Math.max(0, Math.min(i, Math.floor(kx * (x - x0))))];
	    }
	    function rescale() {
	      kx = range.length / (x1 - x0);
	      i = range.length - 1;
	      return scale;
	    }
	    scale.domain = function(x) {
	      if (!arguments.length) return [ x0, x1 ];
	      x0 = +x[0];
	      x1 = +x[x.length - 1];
	      return rescale();
	    };
	    scale.range = function(x) {
	      if (!arguments.length) return range;
	      range = x;
	      return rescale();
	    };
	    scale.invertExtent = function(y) {
	      y = range.indexOf(y);
	      y = y < 0 ? NaN : y / kx + x0;
	      return [ y, y + 1 / kx ];
	    };
	    scale.copy = function() {
	      return d3_scale_quantize(x0, x1, range);
	    };
	    return rescale();
	  }
	  d3.scale.threshold = function() {
	    return d3_scale_threshold([ .5 ], [ 0, 1 ]);
	  };
	  function d3_scale_threshold(domain, range) {
	    function scale(x) {
	      if (x <= x) return range[d3.bisect(domain, x)];
	    }
	    scale.domain = function(_) {
	      if (!arguments.length) return domain;
	      domain = _;
	      return scale;
	    };
	    scale.range = function(_) {
	      if (!arguments.length) return range;
	      range = _;
	      return scale;
	    };
	    scale.invertExtent = function(y) {
	      y = range.indexOf(y);
	      return [ domain[y - 1], domain[y] ];
	    };
	    scale.copy = function() {
	      return d3_scale_threshold(domain, range);
	    };
	    return scale;
	  }
	  d3.scale.identity = function() {
	    return d3_scale_identity([ 0, 1 ]);
	  };
	  function d3_scale_identity(domain) {
	    function identity(x) {
	      return +x;
	    }
	    identity.invert = identity;
	    identity.domain = identity.range = function(x) {
	      if (!arguments.length) return domain;
	      domain = x.map(identity);
	      return identity;
	    };
	    identity.ticks = function(m) {
	      return d3_scale_linearTicks(domain, m);
	    };
	    identity.tickFormat = function(m, format) {
	      return d3_scale_linearTickFormat(domain, m, format);
	    };
	    identity.copy = function() {
	      return d3_scale_identity(domain);
	    };
	    return identity;
	  }
	  d3.svg = {};
	  function d3_zero() {
	    return 0;
	  }
	  d3.svg.arc = function() {
	    var innerRadius = d3_svg_arcInnerRadius, outerRadius = d3_svg_arcOuterRadius, cornerRadius = d3_zero, padRadius = d3_svg_arcAuto, startAngle = d3_svg_arcStartAngle, endAngle = d3_svg_arcEndAngle, padAngle = d3_svg_arcPadAngle;
	    function arc() {
	      var r0 = Math.max(0, +innerRadius.apply(this, arguments)), r1 = Math.max(0, +outerRadius.apply(this, arguments)), a0 = startAngle.apply(this, arguments) - halfπ, a1 = endAngle.apply(this, arguments) - halfπ, da = Math.abs(a1 - a0), cw = a0 > a1 ? 0 : 1;
	      if (r1 < r0) rc = r1, r1 = r0, r0 = rc;
	      if (da >= τε) return circleSegment(r1, cw) + (r0 ? circleSegment(r0, 1 - cw) : "") + "Z";
	      var rc, cr, rp, ap, p0 = 0, p1 = 0, x0, y0, x1, y1, x2, y2, x3, y3, path = [];
	      if (ap = (+padAngle.apply(this, arguments) || 0) / 2) {
	        rp = padRadius === d3_svg_arcAuto ? Math.sqrt(r0 * r0 + r1 * r1) : +padRadius.apply(this, arguments);
	        if (!cw) p1 *= -1;
	        if (r1) p1 = d3_asin(rp / r1 * Math.sin(ap));
	        if (r0) p0 = d3_asin(rp / r0 * Math.sin(ap));
	      }
	      if (r1) {
	        x0 = r1 * Math.cos(a0 + p1);
	        y0 = r1 * Math.sin(a0 + p1);
	        x1 = r1 * Math.cos(a1 - p1);
	        y1 = r1 * Math.sin(a1 - p1);
	        var l1 = Math.abs(a1 - a0 - 2 * p1) <= π ? 0 : 1;
	        if (p1 && d3_svg_arcSweep(x0, y0, x1, y1) === cw ^ l1) {
	          var h1 = (a0 + a1) / 2;
	          x0 = r1 * Math.cos(h1);
	          y0 = r1 * Math.sin(h1);
	          x1 = y1 = null;
	        }
	      } else {
	        x0 = y0 = 0;
	      }
	      if (r0) {
	        x2 = r0 * Math.cos(a1 - p0);
	        y2 = r0 * Math.sin(a1 - p0);
	        x3 = r0 * Math.cos(a0 + p0);
	        y3 = r0 * Math.sin(a0 + p0);
	        var l0 = Math.abs(a0 - a1 + 2 * p0) <= π ? 0 : 1;
	        if (p0 && d3_svg_arcSweep(x2, y2, x3, y3) === 1 - cw ^ l0) {
	          var h0 = (a0 + a1) / 2;
	          x2 = r0 * Math.cos(h0);
	          y2 = r0 * Math.sin(h0);
	          x3 = y3 = null;
	        }
	      } else {
	        x2 = y2 = 0;
	      }
	      if (da > ε && (rc = Math.min(Math.abs(r1 - r0) / 2, +cornerRadius.apply(this, arguments))) > .001) {
	        cr = r0 < r1 ^ cw ? 0 : 1;
	        var rc1 = rc, rc0 = rc;
	        if (da < π) {
	          var oc = x3 == null ? [ x2, y2 ] : x1 == null ? [ x0, y0 ] : d3_geom_polygonIntersect([ x0, y0 ], [ x3, y3 ], [ x1, y1 ], [ x2, y2 ]), ax = x0 - oc[0], ay = y0 - oc[1], bx = x1 - oc[0], by = y1 - oc[1], kc = 1 / Math.sin(Math.acos((ax * bx + ay * by) / (Math.sqrt(ax * ax + ay * ay) * Math.sqrt(bx * bx + by * by))) / 2), lc = Math.sqrt(oc[0] * oc[0] + oc[1] * oc[1]);
	          rc0 = Math.min(rc, (r0 - lc) / (kc - 1));
	          rc1 = Math.min(rc, (r1 - lc) / (kc + 1));
	        }
	        if (x1 != null) {
	          var t30 = d3_svg_arcCornerTangents(x3 == null ? [ x2, y2 ] : [ x3, y3 ], [ x0, y0 ], r1, rc1, cw), t12 = d3_svg_arcCornerTangents([ x1, y1 ], [ x2, y2 ], r1, rc1, cw);
	          if (rc === rc1) {
	            path.push("M", t30[0], "A", rc1, ",", rc1, " 0 0,", cr, " ", t30[1], "A", r1, ",", r1, " 0 ", 1 - cw ^ d3_svg_arcSweep(t30[1][0], t30[1][1], t12[1][0], t12[1][1]), ",", cw, " ", t12[1], "A", rc1, ",", rc1, " 0 0,", cr, " ", t12[0]);
	          } else {
	            path.push("M", t30[0], "A", rc1, ",", rc1, " 0 1,", cr, " ", t12[0]);
	          }
	        } else {
	          path.push("M", x0, ",", y0);
	        }
	        if (x3 != null) {
	          var t03 = d3_svg_arcCornerTangents([ x0, y0 ], [ x3, y3 ], r0, -rc0, cw), t21 = d3_svg_arcCornerTangents([ x2, y2 ], x1 == null ? [ x0, y0 ] : [ x1, y1 ], r0, -rc0, cw);
	          if (rc === rc0) {
	            path.push("L", t21[0], "A", rc0, ",", rc0, " 0 0,", cr, " ", t21[1], "A", r0, ",", r0, " 0 ", cw ^ d3_svg_arcSweep(t21[1][0], t21[1][1], t03[1][0], t03[1][1]), ",", 1 - cw, " ", t03[1], "A", rc0, ",", rc0, " 0 0,", cr, " ", t03[0]);
	          } else {
	            path.push("L", t21[0], "A", rc0, ",", rc0, " 0 0,", cr, " ", t03[0]);
	          }
	        } else {
	          path.push("L", x2, ",", y2);
	        }
	      } else {
	        path.push("M", x0, ",", y0);
	        if (x1 != null) path.push("A", r1, ",", r1, " 0 ", l1, ",", cw, " ", x1, ",", y1);
	        path.push("L", x2, ",", y2);
	        if (x3 != null) path.push("A", r0, ",", r0, " 0 ", l0, ",", 1 - cw, " ", x3, ",", y3);
	      }
	      path.push("Z");
	      return path.join("");
	    }
	    function circleSegment(r1, cw) {
	      return "M0," + r1 + "A" + r1 + "," + r1 + " 0 1," + cw + " 0," + -r1 + "A" + r1 + "," + r1 + " 0 1," + cw + " 0," + r1;
	    }
	    arc.innerRadius = function(v) {
	      if (!arguments.length) return innerRadius;
	      innerRadius = d3_functor(v);
	      return arc;
	    };
	    arc.outerRadius = function(v) {
	      if (!arguments.length) return outerRadius;
	      outerRadius = d3_functor(v);
	      return arc;
	    };
	    arc.cornerRadius = function(v) {
	      if (!arguments.length) return cornerRadius;
	      cornerRadius = d3_functor(v);
	      return arc;
	    };
	    arc.padRadius = function(v) {
	      if (!arguments.length) return padRadius;
	      padRadius = v == d3_svg_arcAuto ? d3_svg_arcAuto : d3_functor(v);
	      return arc;
	    };
	    arc.startAngle = function(v) {
	      if (!arguments.length) return startAngle;
	      startAngle = d3_functor(v);
	      return arc;
	    };
	    arc.endAngle = function(v) {
	      if (!arguments.length) return endAngle;
	      endAngle = d3_functor(v);
	      return arc;
	    };
	    arc.padAngle = function(v) {
	      if (!arguments.length) return padAngle;
	      padAngle = d3_functor(v);
	      return arc;
	    };
	    arc.centroid = function() {
	      var r = (+innerRadius.apply(this, arguments) + +outerRadius.apply(this, arguments)) / 2, a = (+startAngle.apply(this, arguments) + +endAngle.apply(this, arguments)) / 2 - halfπ;
	      return [ Math.cos(a) * r, Math.sin(a) * r ];
	    };
	    return arc;
	  };
	  var d3_svg_arcAuto = "auto";
	  function d3_svg_arcInnerRadius(d) {
	    return d.innerRadius;
	  }
	  function d3_svg_arcOuterRadius(d) {
	    return d.outerRadius;
	  }
	  function d3_svg_arcStartAngle(d) {
	    return d.startAngle;
	  }
	  function d3_svg_arcEndAngle(d) {
	    return d.endAngle;
	  }
	  function d3_svg_arcPadAngle(d) {
	    return d && d.padAngle;
	  }
	  function d3_svg_arcSweep(x0, y0, x1, y1) {
	    return (x0 - x1) * y0 - (y0 - y1) * x0 > 0 ? 0 : 1;
	  }
	  function d3_svg_arcCornerTangents(p0, p1, r1, rc, cw) {
	    var x01 = p0[0] - p1[0], y01 = p0[1] - p1[1], lo = (cw ? rc : -rc) / Math.sqrt(x01 * x01 + y01 * y01), ox = lo * y01, oy = -lo * x01, x1 = p0[0] + ox, y1 = p0[1] + oy, x2 = p1[0] + ox, y2 = p1[1] + oy, x3 = (x1 + x2) / 2, y3 = (y1 + y2) / 2, dx = x2 - x1, dy = y2 - y1, d2 = dx * dx + dy * dy, r = r1 - rc, D = x1 * y2 - x2 * y1, d = (dy < 0 ? -1 : 1) * Math.sqrt(Math.max(0, r * r * d2 - D * D)), cx0 = (D * dy - dx * d) / d2, cy0 = (-D * dx - dy * d) / d2, cx1 = (D * dy + dx * d) / d2, cy1 = (-D * dx + dy * d) / d2, dx0 = cx0 - x3, dy0 = cy0 - y3, dx1 = cx1 - x3, dy1 = cy1 - y3;
	    if (dx0 * dx0 + dy0 * dy0 > dx1 * dx1 + dy1 * dy1) cx0 = cx1, cy0 = cy1;
	    return [ [ cx0 - ox, cy0 - oy ], [ cx0 * r1 / r, cy0 * r1 / r ] ];
	  }
	  function d3_svg_line(projection) {
	    var x = d3_geom_pointX, y = d3_geom_pointY, defined = d3_true, interpolate = d3_svg_lineLinear, interpolateKey = interpolate.key, tension = .7;
	    function line(data) {
	      var segments = [], points = [], i = -1, n = data.length, d, fx = d3_functor(x), fy = d3_functor(y);
	      function segment() {
	        segments.push("M", interpolate(projection(points), tension));
	      }
	      while (++i < n) {
	        if (defined.call(this, d = data[i], i)) {
	          points.push([ +fx.call(this, d, i), +fy.call(this, d, i) ]);
	        } else if (points.length) {
	          segment();
	          points = [];
	        }
	      }
	      if (points.length) segment();
	      return segments.length ? segments.join("") : null;
	    }
	    line.x = function(_) {
	      if (!arguments.length) return x;
	      x = _;
	      return line;
	    };
	    line.y = function(_) {
	      if (!arguments.length) return y;
	      y = _;
	      return line;
	    };
	    line.defined = function(_) {
	      if (!arguments.length) return defined;
	      defined = _;
	      return line;
	    };
	    line.interpolate = function(_) {
	      if (!arguments.length) return interpolateKey;
	      if (typeof _ === "function") interpolateKey = interpolate = _; else interpolateKey = (interpolate = d3_svg_lineInterpolators.get(_) || d3_svg_lineLinear).key;
	      return line;
	    };
	    line.tension = function(_) {
	      if (!arguments.length) return tension;
	      tension = _;
	      return line;
	    };
	    return line;
	  }
	  d3.svg.line = function() {
	    return d3_svg_line(d3_identity);
	  };
	  var d3_svg_lineInterpolators = d3.map({
	    linear: d3_svg_lineLinear,
	    "linear-closed": d3_svg_lineLinearClosed,
	    step: d3_svg_lineStep,
	    "step-before": d3_svg_lineStepBefore,
	    "step-after": d3_svg_lineStepAfter,
	    basis: d3_svg_lineBasis,
	    "basis-open": d3_svg_lineBasisOpen,
	    "basis-closed": d3_svg_lineBasisClosed,
	    bundle: d3_svg_lineBundle,
	    cardinal: d3_svg_lineCardinal,
	    "cardinal-open": d3_svg_lineCardinalOpen,
	    "cardinal-closed": d3_svg_lineCardinalClosed,
	    monotone: d3_svg_lineMonotone
	  });
	  d3_svg_lineInterpolators.forEach(function(key, value) {
	    value.key = key;
	    value.closed = /-closed$/.test(key);
	  });
	  function d3_svg_lineLinear(points) {
	    return points.length > 1 ? points.join("L") : points + "Z";
	  }
	  function d3_svg_lineLinearClosed(points) {
	    return points.join("L") + "Z";
	  }
	  function d3_svg_lineStep(points) {
	    var i = 0, n = points.length, p = points[0], path = [ p[0], ",", p[1] ];
	    while (++i < n) path.push("H", (p[0] + (p = points[i])[0]) / 2, "V", p[1]);
	    if (n > 1) path.push("H", p[0]);
	    return path.join("");
	  }
	  function d3_svg_lineStepBefore(points) {
	    var i = 0, n = points.length, p = points[0], path = [ p[0], ",", p[1] ];
	    while (++i < n) path.push("V", (p = points[i])[1], "H", p[0]);
	    return path.join("");
	  }
	  function d3_svg_lineStepAfter(points) {
	    var i = 0, n = points.length, p = points[0], path = [ p[0], ",", p[1] ];
	    while (++i < n) path.push("H", (p = points[i])[0], "V", p[1]);
	    return path.join("");
	  }
	  function d3_svg_lineCardinalOpen(points, tension) {
	    return points.length < 4 ? d3_svg_lineLinear(points) : points[1] + d3_svg_lineHermite(points.slice(1, -1), d3_svg_lineCardinalTangents(points, tension));
	  }
	  function d3_svg_lineCardinalClosed(points, tension) {
	    return points.length < 3 ? d3_svg_lineLinearClosed(points) : points[0] + d3_svg_lineHermite((points.push(points[0]), 
	    points), d3_svg_lineCardinalTangents([ points[points.length - 2] ].concat(points, [ points[1] ]), tension));
	  }
	  function d3_svg_lineCardinal(points, tension) {
	    return points.length < 3 ? d3_svg_lineLinear(points) : points[0] + d3_svg_lineHermite(points, d3_svg_lineCardinalTangents(points, tension));
	  }
	  function d3_svg_lineHermite(points, tangents) {
	    if (tangents.length < 1 || points.length != tangents.length && points.length != tangents.length + 2) {
	      return d3_svg_lineLinear(points);
	    }
	    var quad = points.length != tangents.length, path = "", p0 = points[0], p = points[1], t0 = tangents[0], t = t0, pi = 1;
	    if (quad) {
	      path += "Q" + (p[0] - t0[0] * 2 / 3) + "," + (p[1] - t0[1] * 2 / 3) + "," + p[0] + "," + p[1];
	      p0 = points[1];
	      pi = 2;
	    }
	    if (tangents.length > 1) {
	      t = tangents[1];
	      p = points[pi];
	      pi++;
	      path += "C" + (p0[0] + t0[0]) + "," + (p0[1] + t0[1]) + "," + (p[0] - t[0]) + "," + (p[1] - t[1]) + "," + p[0] + "," + p[1];
	      for (var i = 2; i < tangents.length; i++, pi++) {
	        p = points[pi];
	        t = tangents[i];
	        path += "S" + (p[0] - t[0]) + "," + (p[1] - t[1]) + "," + p[0] + "," + p[1];
	      }
	    }
	    if (quad) {
	      var lp = points[pi];
	      path += "Q" + (p[0] + t[0] * 2 / 3) + "," + (p[1] + t[1] * 2 / 3) + "," + lp[0] + "," + lp[1];
	    }
	    return path;
	  }
	  function d3_svg_lineCardinalTangents(points, tension) {
	    var tangents = [], a = (1 - tension) / 2, p0, p1 = points[0], p2 = points[1], i = 1, n = points.length;
	    while (++i < n) {
	      p0 = p1;
	      p1 = p2;
	      p2 = points[i];
	      tangents.push([ a * (p2[0] - p0[0]), a * (p2[1] - p0[1]) ]);
	    }
	    return tangents;
	  }
	  function d3_svg_lineBasis(points) {
	    if (points.length < 3) return d3_svg_lineLinear(points);
	    var i = 1, n = points.length, pi = points[0], x0 = pi[0], y0 = pi[1], px = [ x0, x0, x0, (pi = points[1])[0] ], py = [ y0, y0, y0, pi[1] ], path = [ x0, ",", y0, "L", d3_svg_lineDot4(d3_svg_lineBasisBezier3, px), ",", d3_svg_lineDot4(d3_svg_lineBasisBezier3, py) ];
	    points.push(points[n - 1]);
	    while (++i <= n) {
	      pi = points[i];
	      px.shift();
	      px.push(pi[0]);
	      py.shift();
	      py.push(pi[1]);
	      d3_svg_lineBasisBezier(path, px, py);
	    }
	    points.pop();
	    path.push("L", pi);
	    return path.join("");
	  }
	  function d3_svg_lineBasisOpen(points) {
	    if (points.length < 4) return d3_svg_lineLinear(points);
	    var path = [], i = -1, n = points.length, pi, px = [ 0 ], py = [ 0 ];
	    while (++i < 3) {
	      pi = points[i];
	      px.push(pi[0]);
	      py.push(pi[1]);
	    }
	    path.push(d3_svg_lineDot4(d3_svg_lineBasisBezier3, px) + "," + d3_svg_lineDot4(d3_svg_lineBasisBezier3, py));
	    --i;
	    while (++i < n) {
	      pi = points[i];
	      px.shift();
	      px.push(pi[0]);
	      py.shift();
	      py.push(pi[1]);
	      d3_svg_lineBasisBezier(path, px, py);
	    }
	    return path.join("");
	  }
	  function d3_svg_lineBasisClosed(points) {
	    var path, i = -1, n = points.length, m = n + 4, pi, px = [], py = [];
	    while (++i < 4) {
	      pi = points[i % n];
	      px.push(pi[0]);
	      py.push(pi[1]);
	    }
	    path = [ d3_svg_lineDot4(d3_svg_lineBasisBezier3, px), ",", d3_svg_lineDot4(d3_svg_lineBasisBezier3, py) ];
	    --i;
	    while (++i < m) {
	      pi = points[i % n];
	      px.shift();
	      px.push(pi[0]);
	      py.shift();
	      py.push(pi[1]);
	      d3_svg_lineBasisBezier(path, px, py);
	    }
	    return path.join("");
	  }
	  function d3_svg_lineBundle(points, tension) {
	    var n = points.length - 1;
	    if (n) {
	      var x0 = points[0][0], y0 = points[0][1], dx = points[n][0] - x0, dy = points[n][1] - y0, i = -1, p, t;
	      while (++i <= n) {
	        p = points[i];
	        t = i / n;
	        p[0] = tension * p[0] + (1 - tension) * (x0 + t * dx);
	        p[1] = tension * p[1] + (1 - tension) * (y0 + t * dy);
	      }
	    }
	    return d3_svg_lineBasis(points);
	  }
	  function d3_svg_lineDot4(a, b) {
	    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3];
	  }
	  var d3_svg_lineBasisBezier1 = [ 0, 2 / 3, 1 / 3, 0 ], d3_svg_lineBasisBezier2 = [ 0, 1 / 3, 2 / 3, 0 ], d3_svg_lineBasisBezier3 = [ 0, 1 / 6, 2 / 3, 1 / 6 ];
	  function d3_svg_lineBasisBezier(path, x, y) {
	    path.push("C", d3_svg_lineDot4(d3_svg_lineBasisBezier1, x), ",", d3_svg_lineDot4(d3_svg_lineBasisBezier1, y), ",", d3_svg_lineDot4(d3_svg_lineBasisBezier2, x), ",", d3_svg_lineDot4(d3_svg_lineBasisBezier2, y), ",", d3_svg_lineDot4(d3_svg_lineBasisBezier3, x), ",", d3_svg_lineDot4(d3_svg_lineBasisBezier3, y));
	  }
	  function d3_svg_lineSlope(p0, p1) {
	    return (p1[1] - p0[1]) / (p1[0] - p0[0]);
	  }
	  function d3_svg_lineFiniteDifferences(points) {
	    var i = 0, j = points.length - 1, m = [], p0 = points[0], p1 = points[1], d = m[0] = d3_svg_lineSlope(p0, p1);
	    while (++i < j) {
	      m[i] = (d + (d = d3_svg_lineSlope(p0 = p1, p1 = points[i + 1]))) / 2;
	    }
	    m[i] = d;
	    return m;
	  }
	  function d3_svg_lineMonotoneTangents(points) {
	    var tangents = [], d, a, b, s, m = d3_svg_lineFiniteDifferences(points), i = -1, j = points.length - 1;
	    while (++i < j) {
	      d = d3_svg_lineSlope(points[i], points[i + 1]);
	      if (abs(d) < ε) {
	        m[i] = m[i + 1] = 0;
	      } else {
	        a = m[i] / d;
	        b = m[i + 1] / d;
	        s = a * a + b * b;
	        if (s > 9) {
	          s = d * 3 / Math.sqrt(s);
	          m[i] = s * a;
	          m[i + 1] = s * b;
	        }
	      }
	    }
	    i = -1;
	    while (++i <= j) {
	      s = (points[Math.min(j, i + 1)][0] - points[Math.max(0, i - 1)][0]) / (6 * (1 + m[i] * m[i]));
	      tangents.push([ s || 0, m[i] * s || 0 ]);
	    }
	    return tangents;
	  }
	  function d3_svg_lineMonotone(points) {
	    return points.length < 3 ? d3_svg_lineLinear(points) : points[0] + d3_svg_lineHermite(points, d3_svg_lineMonotoneTangents(points));
	  }
	  d3.svg.line.radial = function() {
	    var line = d3_svg_line(d3_svg_lineRadial);
	    line.radius = line.x, delete line.x;
	    line.angle = line.y, delete line.y;
	    return line;
	  };
	  function d3_svg_lineRadial(points) {
	    var point, i = -1, n = points.length, r, a;
	    while (++i < n) {
	      point = points[i];
	      r = point[0];
	      a = point[1] - halfπ;
	      point[0] = r * Math.cos(a);
	      point[1] = r * Math.sin(a);
	    }
	    return points;
	  }
	  function d3_svg_area(projection) {
	    var x0 = d3_geom_pointX, x1 = d3_geom_pointX, y0 = 0, y1 = d3_geom_pointY, defined = d3_true, interpolate = d3_svg_lineLinear, interpolateKey = interpolate.key, interpolateReverse = interpolate, L = "L", tension = .7;
	    function area(data) {
	      var segments = [], points0 = [], points1 = [], i = -1, n = data.length, d, fx0 = d3_functor(x0), fy0 = d3_functor(y0), fx1 = x0 === x1 ? function() {
	        return x;
	      } : d3_functor(x1), fy1 = y0 === y1 ? function() {
	        return y;
	      } : d3_functor(y1), x, y;
	      function segment() {
	        segments.push("M", interpolate(projection(points1), tension), L, interpolateReverse(projection(points0.reverse()), tension), "Z");
	      }
	      while (++i < n) {
	        if (defined.call(this, d = data[i], i)) {
	          points0.push([ x = +fx0.call(this, d, i), y = +fy0.call(this, d, i) ]);
	          points1.push([ +fx1.call(this, d, i), +fy1.call(this, d, i) ]);
	        } else if (points0.length) {
	          segment();
	          points0 = [];
	          points1 = [];
	        }
	      }
	      if (points0.length) segment();
	      return segments.length ? segments.join("") : null;
	    }
	    area.x = function(_) {
	      if (!arguments.length) return x1;
	      x0 = x1 = _;
	      return area;
	    };
	    area.x0 = function(_) {
	      if (!arguments.length) return x0;
	      x0 = _;
	      return area;
	    };
	    area.x1 = function(_) {
	      if (!arguments.length) return x1;
	      x1 = _;
	      return area;
	    };
	    area.y = function(_) {
	      if (!arguments.length) return y1;
	      y0 = y1 = _;
	      return area;
	    };
	    area.y0 = function(_) {
	      if (!arguments.length) return y0;
	      y0 = _;
	      return area;
	    };
	    area.y1 = function(_) {
	      if (!arguments.length) return y1;
	      y1 = _;
	      return area;
	    };
	    area.defined = function(_) {
	      if (!arguments.length) return defined;
	      defined = _;
	      return area;
	    };
	    area.interpolate = function(_) {
	      if (!arguments.length) return interpolateKey;
	      if (typeof _ === "function") interpolateKey = interpolate = _; else interpolateKey = (interpolate = d3_svg_lineInterpolators.get(_) || d3_svg_lineLinear).key;
	      interpolateReverse = interpolate.reverse || interpolate;
	      L = interpolate.closed ? "M" : "L";
	      return area;
	    };
	    area.tension = function(_) {
	      if (!arguments.length) return tension;
	      tension = _;
	      return area;
	    };
	    return area;
	  }
	  d3_svg_lineStepBefore.reverse = d3_svg_lineStepAfter;
	  d3_svg_lineStepAfter.reverse = d3_svg_lineStepBefore;
	  d3.svg.area = function() {
	    return d3_svg_area(d3_identity);
	  };
	  d3.svg.area.radial = function() {
	    var area = d3_svg_area(d3_svg_lineRadial);
	    area.radius = area.x, delete area.x;
	    area.innerRadius = area.x0, delete area.x0;
	    area.outerRadius = area.x1, delete area.x1;
	    area.angle = area.y, delete area.y;
	    area.startAngle = area.y0, delete area.y0;
	    area.endAngle = area.y1, delete area.y1;
	    return area;
	  };
	  d3.svg.chord = function() {
	    var source = d3_source, target = d3_target, radius = d3_svg_chordRadius, startAngle = d3_svg_arcStartAngle, endAngle = d3_svg_arcEndAngle;
	    function chord(d, i) {
	      var s = subgroup(this, source, d, i), t = subgroup(this, target, d, i);
	      return "M" + s.p0 + arc(s.r, s.p1, s.a1 - s.a0) + (equals(s, t) ? curve(s.r, s.p1, s.r, s.p0) : curve(s.r, s.p1, t.r, t.p0) + arc(t.r, t.p1, t.a1 - t.a0) + curve(t.r, t.p1, s.r, s.p0)) + "Z";
	    }
	    function subgroup(self, f, d, i) {
	      var subgroup = f.call(self, d, i), r = radius.call(self, subgroup, i), a0 = startAngle.call(self, subgroup, i) - halfπ, a1 = endAngle.call(self, subgroup, i) - halfπ;
	      return {
	        r: r,
	        a0: a0,
	        a1: a1,
	        p0: [ r * Math.cos(a0), r * Math.sin(a0) ],
	        p1: [ r * Math.cos(a1), r * Math.sin(a1) ]
	      };
	    }
	    function equals(a, b) {
	      return a.a0 == b.a0 && a.a1 == b.a1;
	    }
	    function arc(r, p, a) {
	      return "A" + r + "," + r + " 0 " + +(a > π) + ",1 " + p;
	    }
	    function curve(r0, p0, r1, p1) {
	      return "Q 0,0 " + p1;
	    }
	    chord.radius = function(v) {
	      if (!arguments.length) return radius;
	      radius = d3_functor(v);
	      return chord;
	    };
	    chord.source = function(v) {
	      if (!arguments.length) return source;
	      source = d3_functor(v);
	      return chord;
	    };
	    chord.target = function(v) {
	      if (!arguments.length) return target;
	      target = d3_functor(v);
	      return chord;
	    };
	    chord.startAngle = function(v) {
	      if (!arguments.length) return startAngle;
	      startAngle = d3_functor(v);
	      return chord;
	    };
	    chord.endAngle = function(v) {
	      if (!arguments.length) return endAngle;
	      endAngle = d3_functor(v);
	      return chord;
	    };
	    return chord;
	  };
	  function d3_svg_chordRadius(d) {
	    return d.radius;
	  }
	  d3.svg.diagonal = function() {
	    var source = d3_source, target = d3_target, projection = d3_svg_diagonalProjection;
	    function diagonal(d, i) {
	      var p0 = source.call(this, d, i), p3 = target.call(this, d, i), m = (p0.y + p3.y) / 2, p = [ p0, {
	        x: p0.x,
	        y: m
	      }, {
	        x: p3.x,
	        y: m
	      }, p3 ];
	      p = p.map(projection);
	      return "M" + p[0] + "C" + p[1] + " " + p[2] + " " + p[3];
	    }
	    diagonal.source = function(x) {
	      if (!arguments.length) return source;
	      source = d3_functor(x);
	      return diagonal;
	    };
	    diagonal.target = function(x) {
	      if (!arguments.length) return target;
	      target = d3_functor(x);
	      return diagonal;
	    };
	    diagonal.projection = function(x) {
	      if (!arguments.length) return projection;
	      projection = x;
	      return diagonal;
	    };
	    return diagonal;
	  };
	  function d3_svg_diagonalProjection(d) {
	    return [ d.x, d.y ];
	  }
	  d3.svg.diagonal.radial = function() {
	    var diagonal = d3.svg.diagonal(), projection = d3_svg_diagonalProjection, projection_ = diagonal.projection;
	    diagonal.projection = function(x) {
	      return arguments.length ? projection_(d3_svg_diagonalRadialProjection(projection = x)) : projection;
	    };
	    return diagonal;
	  };
	  function d3_svg_diagonalRadialProjection(projection) {
	    return function() {
	      var d = projection.apply(this, arguments), r = d[0], a = d[1] - halfπ;
	      return [ r * Math.cos(a), r * Math.sin(a) ];
	    };
	  }
	  d3.svg.symbol = function() {
	    var type = d3_svg_symbolType, size = d3_svg_symbolSize;
	    function symbol(d, i) {
	      return (d3_svg_symbols.get(type.call(this, d, i)) || d3_svg_symbolCircle)(size.call(this, d, i));
	    }
	    symbol.type = function(x) {
	      if (!arguments.length) return type;
	      type = d3_functor(x);
	      return symbol;
	    };
	    symbol.size = function(x) {
	      if (!arguments.length) return size;
	      size = d3_functor(x);
	      return symbol;
	    };
	    return symbol;
	  };
	  function d3_svg_symbolSize() {
	    return 64;
	  }
	  function d3_svg_symbolType() {
	    return "circle";
	  }
	  function d3_svg_symbolCircle(size) {
	    var r = Math.sqrt(size / π);
	    return "M0," + r + "A" + r + "," + r + " 0 1,1 0," + -r + "A" + r + "," + r + " 0 1,1 0," + r + "Z";
	  }
	  var d3_svg_symbols = d3.map({
	    circle: d3_svg_symbolCircle,
	    cross: function(size) {
	      var r = Math.sqrt(size / 5) / 2;
	      return "M" + -3 * r + "," + -r + "H" + -r + "V" + -3 * r + "H" + r + "V" + -r + "H" + 3 * r + "V" + r + "H" + r + "V" + 3 * r + "H" + -r + "V" + r + "H" + -3 * r + "Z";
	    },
	    diamond: function(size) {
	      var ry = Math.sqrt(size / (2 * d3_svg_symbolTan30)), rx = ry * d3_svg_symbolTan30;
	      return "M0," + -ry + "L" + rx + ",0" + " 0," + ry + " " + -rx + ",0" + "Z";
	    },
	    square: function(size) {
	      var r = Math.sqrt(size) / 2;
	      return "M" + -r + "," + -r + "L" + r + "," + -r + " " + r + "," + r + " " + -r + "," + r + "Z";
	    },
	    "triangle-down": function(size) {
	      var rx = Math.sqrt(size / d3_svg_symbolSqrt3), ry = rx * d3_svg_symbolSqrt3 / 2;
	      return "M0," + ry + "L" + rx + "," + -ry + " " + -rx + "," + -ry + "Z";
	    },
	    "triangle-up": function(size) {
	      var rx = Math.sqrt(size / d3_svg_symbolSqrt3), ry = rx * d3_svg_symbolSqrt3 / 2;
	      return "M0," + -ry + "L" + rx + "," + ry + " " + -rx + "," + ry + "Z";
	    }
	  });
	  d3.svg.symbolTypes = d3_svg_symbols.keys();
	  var d3_svg_symbolSqrt3 = Math.sqrt(3), d3_svg_symbolTan30 = Math.tan(30 * d3_radians);
	  d3_selectionPrototype.transition = function(name) {
	    var id = d3_transitionInheritId || ++d3_transitionId, ns = d3_transitionNamespace(name), subgroups = [], subgroup, node, transition = d3_transitionInherit || {
	      time: Date.now(),
	      ease: d3_ease_cubicInOut,
	      delay: 0,
	      duration: 250
	    };
	    for (var j = -1, m = this.length; ++j < m; ) {
	      subgroups.push(subgroup = []);
	      for (var group = this[j], i = -1, n = group.length; ++i < n; ) {
	        if (node = group[i]) d3_transitionNode(node, i, ns, id, transition);
	        subgroup.push(node);
	      }
	    }
	    return d3_transition(subgroups, ns, id);
	  };
	  d3_selectionPrototype.interrupt = function(name) {
	    return this.each(name == null ? d3_selection_interrupt : d3_selection_interruptNS(d3_transitionNamespace(name)));
	  };
	  var d3_selection_interrupt = d3_selection_interruptNS(d3_transitionNamespace());
	  function d3_selection_interruptNS(ns) {
	    return function() {
	      var lock, activeId, active;
	      if ((lock = this[ns]) && (active = lock[activeId = lock.active])) {
	        active.timer.c = null;
	        active.timer.t = NaN;
	        if (--lock.count) delete lock[activeId]; else delete this[ns];
	        lock.active += .5;
	        active.event && active.event.interrupt.call(this, this.__data__, active.index);
	      }
	    };
	  }
	  function d3_transition(groups, ns, id) {
	    d3_subclass(groups, d3_transitionPrototype);
	    groups.namespace = ns;
	    groups.id = id;
	    return groups;
	  }
	  var d3_transitionPrototype = [], d3_transitionId = 0, d3_transitionInheritId, d3_transitionInherit;
	  d3_transitionPrototype.call = d3_selectionPrototype.call;
	  d3_transitionPrototype.empty = d3_selectionPrototype.empty;
	  d3_transitionPrototype.node = d3_selectionPrototype.node;
	  d3_transitionPrototype.size = d3_selectionPrototype.size;
	  d3.transition = function(selection, name) {
	    return selection && selection.transition ? d3_transitionInheritId ? selection.transition(name) : selection : d3.selection().transition(selection);
	  };
	  d3.transition.prototype = d3_transitionPrototype;
	  d3_transitionPrototype.select = function(selector) {
	    var id = this.id, ns = this.namespace, subgroups = [], subgroup, subnode, node;
	    selector = d3_selection_selector(selector);
	    for (var j = -1, m = this.length; ++j < m; ) {
	      subgroups.push(subgroup = []);
	      for (var group = this[j], i = -1, n = group.length; ++i < n; ) {
	        if ((node = group[i]) && (subnode = selector.call(node, node.__data__, i, j))) {
	          if ("__data__" in node) subnode.__data__ = node.__data__;
	          d3_transitionNode(subnode, i, ns, id, node[ns][id]);
	          subgroup.push(subnode);
	        } else {
	          subgroup.push(null);
	        }
	      }
	    }
	    return d3_transition(subgroups, ns, id);
	  };
	  d3_transitionPrototype.selectAll = function(selector) {
	    var id = this.id, ns = this.namespace, subgroups = [], subgroup, subnodes, node, subnode, transition;
	    selector = d3_selection_selectorAll(selector);
	    for (var j = -1, m = this.length; ++j < m; ) {
	      for (var group = this[j], i = -1, n = group.length; ++i < n; ) {
	        if (node = group[i]) {
	          transition = node[ns][id];
	          subnodes = selector.call(node, node.__data__, i, j);
	          subgroups.push(subgroup = []);
	          for (var k = -1, o = subnodes.length; ++k < o; ) {
	            if (subnode = subnodes[k]) d3_transitionNode(subnode, k, ns, id, transition);
	            subgroup.push(subnode);
	          }
	        }
	      }
	    }
	    return d3_transition(subgroups, ns, id);
	  };
	  d3_transitionPrototype.filter = function(filter) {
	    var subgroups = [], subgroup, group, node;
	    if (typeof filter !== "function") filter = d3_selection_filter(filter);
	    for (var j = 0, m = this.length; j < m; j++) {
	      subgroups.push(subgroup = []);
	      for (var group = this[j], i = 0, n = group.length; i < n; i++) {
	        if ((node = group[i]) && filter.call(node, node.__data__, i, j)) {
	          subgroup.push(node);
	        }
	      }
	    }
	    return d3_transition(subgroups, this.namespace, this.id);
	  };
	  d3_transitionPrototype.tween = function(name, tween) {
	    var id = this.id, ns = this.namespace;
	    if (arguments.length < 2) return this.node()[ns][id].tween.get(name);
	    return d3_selection_each(this, tween == null ? function(node) {
	      node[ns][id].tween.remove(name);
	    } : function(node) {
	      node[ns][id].tween.set(name, tween);
	    });
	  };
	  function d3_transition_tween(groups, name, value, tween) {
	    var id = groups.id, ns = groups.namespace;
	    return d3_selection_each(groups, typeof value === "function" ? function(node, i, j) {
	      node[ns][id].tween.set(name, tween(value.call(node, node.__data__, i, j)));
	    } : (value = tween(value), function(node) {
	      node[ns][id].tween.set(name, value);
	    }));
	  }
	  d3_transitionPrototype.attr = function(nameNS, value) {
	    if (arguments.length < 2) {
	      for (value in nameNS) this.attr(value, nameNS[value]);
	      return this;
	    }
	    var interpolate = nameNS == "transform" ? d3_interpolateTransform : d3_interpolate, name = d3.ns.qualify(nameNS);
	    function attrNull() {
	      this.removeAttribute(name);
	    }
	    function attrNullNS() {
	      this.removeAttributeNS(name.space, name.local);
	    }
	    function attrTween(b) {
	      return b == null ? attrNull : (b += "", function() {
	        var a = this.getAttribute(name), i;
	        return a !== b && (i = interpolate(a, b), function(t) {
	          this.setAttribute(name, i(t));
	        });
	      });
	    }
	    function attrTweenNS(b) {
	      return b == null ? attrNullNS : (b += "", function() {
	        var a = this.getAttributeNS(name.space, name.local), i;
	        return a !== b && (i = interpolate(a, b), function(t) {
	          this.setAttributeNS(name.space, name.local, i(t));
	        });
	      });
	    }
	    return d3_transition_tween(this, "attr." + nameNS, value, name.local ? attrTweenNS : attrTween);
	  };
	  d3_transitionPrototype.attrTween = function(nameNS, tween) {
	    var name = d3.ns.qualify(nameNS);
	    function attrTween(d, i) {
	      var f = tween.call(this, d, i, this.getAttribute(name));
	      return f && function(t) {
	        this.setAttribute(name, f(t));
	      };
	    }
	    function attrTweenNS(d, i) {
	      var f = tween.call(this, d, i, this.getAttributeNS(name.space, name.local));
	      return f && function(t) {
	        this.setAttributeNS(name.space, name.local, f(t));
	      };
	    }
	    return this.tween("attr." + nameNS, name.local ? attrTweenNS : attrTween);
	  };
	  d3_transitionPrototype.style = function(name, value, priority) {
	    var n = arguments.length;
	    if (n < 3) {
	      if (typeof name !== "string") {
	        if (n < 2) value = "";
	        for (priority in name) this.style(priority, name[priority], value);
	        return this;
	      }
	      priority = "";
	    }
	    function styleNull() {
	      this.style.removeProperty(name);
	    }
	    function styleString(b) {
	      return b == null ? styleNull : (b += "", function() {
	        var a = d3_window(this).getComputedStyle(this, null).getPropertyValue(name), i;
	        return a !== b && (i = d3_interpolate(a, b), function(t) {
	          this.style.setProperty(name, i(t), priority);
	        });
	      });
	    }
	    return d3_transition_tween(this, "style." + name, value, styleString);
	  };
	  d3_transitionPrototype.styleTween = function(name, tween, priority) {
	    if (arguments.length < 3) priority = "";
	    function styleTween(d, i) {
	      var f = tween.call(this, d, i, d3_window(this).getComputedStyle(this, null).getPropertyValue(name));
	      return f && function(t) {
	        this.style.setProperty(name, f(t), priority);
	      };
	    }
	    return this.tween("style." + name, styleTween);
	  };
	  d3_transitionPrototype.text = function(value) {
	    return d3_transition_tween(this, "text", value, d3_transition_text);
	  };
	  function d3_transition_text(b) {
	    if (b == null) b = "";
	    return function() {
	      this.textContent = b;
	    };
	  }
	  d3_transitionPrototype.remove = function() {
	    var ns = this.namespace;
	    return this.each("end.transition", function() {
	      var p;
	      if (this[ns].count < 2 && (p = this.parentNode)) p.removeChild(this);
	    });
	  };
	  d3_transitionPrototype.ease = function(value) {
	    var id = this.id, ns = this.namespace;
	    if (arguments.length < 1) return this.node()[ns][id].ease;
	    if (typeof value !== "function") value = d3.ease.apply(d3, arguments);
	    return d3_selection_each(this, function(node) {
	      node[ns][id].ease = value;
	    });
	  };
	  d3_transitionPrototype.delay = function(value) {
	    var id = this.id, ns = this.namespace;
	    if (arguments.length < 1) return this.node()[ns][id].delay;
	    return d3_selection_each(this, typeof value === "function" ? function(node, i, j) {
	      node[ns][id].delay = +value.call(node, node.__data__, i, j);
	    } : (value = +value, function(node) {
	      node[ns][id].delay = value;
	    }));
	  };
	  d3_transitionPrototype.duration = function(value) {
	    var id = this.id, ns = this.namespace;
	    if (arguments.length < 1) return this.node()[ns][id].duration;
	    return d3_selection_each(this, typeof value === "function" ? function(node, i, j) {
	      node[ns][id].duration = Math.max(1, value.call(node, node.__data__, i, j));
	    } : (value = Math.max(1, value), function(node) {
	      node[ns][id].duration = value;
	    }));
	  };
	  d3_transitionPrototype.each = function(type, listener) {
	    var id = this.id, ns = this.namespace;
	    if (arguments.length < 2) {
	      var inherit = d3_transitionInherit, inheritId = d3_transitionInheritId;
	      try {
	        d3_transitionInheritId = id;
	        d3_selection_each(this, function(node, i, j) {
	          d3_transitionInherit = node[ns][id];
	          type.call(node, node.__data__, i, j);
	        });
	      } finally {
	        d3_transitionInherit = inherit;
	        d3_transitionInheritId = inheritId;
	      }
	    } else {
	      d3_selection_each(this, function(node) {
	        var transition = node[ns][id];
	        (transition.event || (transition.event = d3.dispatch("start", "end", "interrupt"))).on(type, listener);
	      });
	    }
	    return this;
	  };
	  d3_transitionPrototype.transition = function() {
	    var id0 = this.id, id1 = ++d3_transitionId, ns = this.namespace, subgroups = [], subgroup, group, node, transition;
	    for (var j = 0, m = this.length; j < m; j++) {
	      subgroups.push(subgroup = []);
	      for (var group = this[j], i = 0, n = group.length; i < n; i++) {
	        if (node = group[i]) {
	          transition = node[ns][id0];
	          d3_transitionNode(node, i, ns, id1, {
	            time: transition.time,
	            ease: transition.ease,
	            delay: transition.delay + transition.duration,
	            duration: transition.duration
	          });
	        }
	        subgroup.push(node);
	      }
	    }
	    return d3_transition(subgroups, ns, id1);
	  };
	  function d3_transitionNamespace(name) {
	    return name == null ? "__transition__" : "__transition_" + name + "__";
	  }
	  function d3_transitionNode(node, i, ns, id, inherit) {
	    var lock = node[ns] || (node[ns] = {
	      active: 0,
	      count: 0
	    }), transition = lock[id], time, timer, duration, ease, tweens;
	    function schedule(elapsed) {
	      var delay = transition.delay;
	      timer.t = delay + time;
	      if (delay <= elapsed) return start(elapsed - delay);
	      timer.c = start;
	    }
	    function start(elapsed) {
	      var activeId = lock.active, active = lock[activeId];
	      if (active) {
	        active.timer.c = null;
	        active.timer.t = NaN;
	        --lock.count;
	        delete lock[activeId];
	        active.event && active.event.interrupt.call(node, node.__data__, active.index);
	      }
	      for (var cancelId in lock) {
	        if (+cancelId < id) {
	          var cancel = lock[cancelId];
	          cancel.timer.c = null;
	          cancel.timer.t = NaN;
	          --lock.count;
	          delete lock[cancelId];
	        }
	      }
	      timer.c = tick;
	      d3_timer(function() {
	        if (timer.c && tick(elapsed || 1)) {
	          timer.c = null;
	          timer.t = NaN;
	        }
	        return 1;
	      }, 0, time);
	      lock.active = id;
	      transition.event && transition.event.start.call(node, node.__data__, i);
	      tweens = [];
	      transition.tween.forEach(function(key, value) {
	        if (value = value.call(node, node.__data__, i)) {
	          tweens.push(value);
	        }
	      });
	      ease = transition.ease;
	      duration = transition.duration;
	    }
	    function tick(elapsed) {
	      var t = elapsed / duration, e = ease(t), n = tweens.length;
	      while (n > 0) {
	        tweens[--n].call(node, e);
	      }
	      if (t >= 1) {
	        transition.event && transition.event.end.call(node, node.__data__, i);
	        if (--lock.count) delete lock[id]; else delete node[ns];
	        return 1;
	      }
	    }
	    if (!transition) {
	      time = inherit.time;
	      timer = d3_timer(schedule, 0, time);
	      transition = lock[id] = {
	        tween: new d3_Map(),
	        time: time,
	        timer: timer,
	        delay: inherit.delay,
	        duration: inherit.duration,
	        ease: inherit.ease,
	        index: i
	      };
	      inherit = null;
	      ++lock.count;
	    }
	  }
	  d3.svg.axis = function() {
	    var scale = d3.scale.linear(), orient = d3_svg_axisDefaultOrient, innerTickSize = 6, outerTickSize = 6, tickPadding = 3, tickArguments_ = [ 10 ], tickValues = null, tickFormat_;
	    function axis(g) {
	      g.each(function() {
	        var g = d3.select(this);
	        var scale0 = this.__chart__ || scale, scale1 = this.__chart__ = scale.copy();
	        var ticks = tickValues == null ? scale1.ticks ? scale1.ticks.apply(scale1, tickArguments_) : scale1.domain() : tickValues, tickFormat = tickFormat_ == null ? scale1.tickFormat ? scale1.tickFormat.apply(scale1, tickArguments_) : d3_identity : tickFormat_, tick = g.selectAll(".tick").data(ticks, scale1), tickEnter = tick.enter().insert("g", ".domain").attr("class", "tick").style("opacity", ε), tickExit = d3.transition(tick.exit()).style("opacity", ε).remove(), tickUpdate = d3.transition(tick.order()).style("opacity", 1), tickSpacing = Math.max(innerTickSize, 0) + tickPadding, tickTransform;
	        var range = d3_scaleRange(scale1), path = g.selectAll(".domain").data([ 0 ]), pathUpdate = (path.enter().append("path").attr("class", "domain"), 
	        d3.transition(path));
	        tickEnter.append("line");
	        tickEnter.append("text");
	        var lineEnter = tickEnter.select("line"), lineUpdate = tickUpdate.select("line"), text = tick.select("text").text(tickFormat), textEnter = tickEnter.select("text"), textUpdate = tickUpdate.select("text"), sign = orient === "top" || orient === "left" ? -1 : 1, x1, x2, y1, y2;
	        if (orient === "bottom" || orient === "top") {
	          tickTransform = d3_svg_axisX, x1 = "x", y1 = "y", x2 = "x2", y2 = "y2";
	          text.attr("dy", sign < 0 ? "0em" : ".71em").style("text-anchor", "middle");
	          pathUpdate.attr("d", "M" + range[0] + "," + sign * outerTickSize + "V0H" + range[1] + "V" + sign * outerTickSize);
	        } else {
	          tickTransform = d3_svg_axisY, x1 = "y", y1 = "x", x2 = "y2", y2 = "x2";
	          text.attr("dy", ".32em").style("text-anchor", sign < 0 ? "end" : "start");
	          pathUpdate.attr("d", "M" + sign * outerTickSize + "," + range[0] + "H0V" + range[1] + "H" + sign * outerTickSize);
	        }
	        lineEnter.attr(y2, sign * innerTickSize);
	        textEnter.attr(y1, sign * tickSpacing);
	        lineUpdate.attr(x2, 0).attr(y2, sign * innerTickSize);
	        textUpdate.attr(x1, 0).attr(y1, sign * tickSpacing);
	        if (scale1.rangeBand) {
	          var x = scale1, dx = x.rangeBand() / 2;
	          scale0 = scale1 = function(d) {
	            return x(d) + dx;
	          };
	        } else if (scale0.rangeBand) {
	          scale0 = scale1;
	        } else {
	          tickExit.call(tickTransform, scale1, scale0);
	        }
	        tickEnter.call(tickTransform, scale0, scale1);
	        tickUpdate.call(tickTransform, scale1, scale1);
	      });
	    }
	    axis.scale = function(x) {
	      if (!arguments.length) return scale;
	      scale = x;
	      return axis;
	    };
	    axis.orient = function(x) {
	      if (!arguments.length) return orient;
	      orient = x in d3_svg_axisOrients ? x + "" : d3_svg_axisDefaultOrient;
	      return axis;
	    };
	    axis.ticks = function() {
	      if (!arguments.length) return tickArguments_;
	      tickArguments_ = d3_array(arguments);
	      return axis;
	    };
	    axis.tickValues = function(x) {
	      if (!arguments.length) return tickValues;
	      tickValues = x;
	      return axis;
	    };
	    axis.tickFormat = function(x) {
	      if (!arguments.length) return tickFormat_;
	      tickFormat_ = x;
	      return axis;
	    };
	    axis.tickSize = function(x) {
	      var n = arguments.length;
	      if (!n) return innerTickSize;
	      innerTickSize = +x;
	      outerTickSize = +arguments[n - 1];
	      return axis;
	    };
	    axis.innerTickSize = function(x) {
	      if (!arguments.length) return innerTickSize;
	      innerTickSize = +x;
	      return axis;
	    };
	    axis.outerTickSize = function(x) {
	      if (!arguments.length) return outerTickSize;
	      outerTickSize = +x;
	      return axis;
	    };
	    axis.tickPadding = function(x) {
	      if (!arguments.length) return tickPadding;
	      tickPadding = +x;
	      return axis;
	    };
	    axis.tickSubdivide = function() {
	      return arguments.length && axis;
	    };
	    return axis;
	  };
	  var d3_svg_axisDefaultOrient = "bottom", d3_svg_axisOrients = {
	    top: 1,
	    right: 1,
	    bottom: 1,
	    left: 1
	  };
	  function d3_svg_axisX(selection, x0, x1) {
	    selection.attr("transform", function(d) {
	      var v0 = x0(d);
	      return "translate(" + (isFinite(v0) ? v0 : x1(d)) + ",0)";
	    });
	  }
	  function d3_svg_axisY(selection, y0, y1) {
	    selection.attr("transform", function(d) {
	      var v0 = y0(d);
	      return "translate(0," + (isFinite(v0) ? v0 : y1(d)) + ")";
	    });
	  }
	  d3.svg.brush = function() {
	    var event = d3_eventDispatch(brush, "brushstart", "brush", "brushend"), x = null, y = null, xExtent = [ 0, 0 ], yExtent = [ 0, 0 ], xExtentDomain, yExtentDomain, xClamp = true, yClamp = true, resizes = d3_svg_brushResizes[0];
	    function brush(g) {
	      g.each(function() {
	        var g = d3.select(this).style("pointer-events", "all").style("-webkit-tap-highlight-color", "rgba(0,0,0,0)").on("mousedown.brush", brushstart).on("touchstart.brush", brushstart);
	        var background = g.selectAll(".background").data([ 0 ]);
	        background.enter().append("rect").attr("class", "background").style("visibility", "hidden").style("cursor", "crosshair");
	        g.selectAll(".extent").data([ 0 ]).enter().append("rect").attr("class", "extent").style("cursor", "move");
	        var resize = g.selectAll(".resize").data(resizes, d3_identity);
	        resize.exit().remove();
	        resize.enter().append("g").attr("class", function(d) {
	          return "resize " + d;
	        }).style("cursor", function(d) {
	          return d3_svg_brushCursor[d];
	        }).append("rect").attr("x", function(d) {
	          return /[ew]$/.test(d) ? -3 : null;
	        }).attr("y", function(d) {
	          return /^[ns]/.test(d) ? -3 : null;
	        }).attr("width", 6).attr("height", 6).style("visibility", "hidden");
	        resize.style("display", brush.empty() ? "none" : null);
	        var gUpdate = d3.transition(g), backgroundUpdate = d3.transition(background), range;
	        if (x) {
	          range = d3_scaleRange(x);
	          backgroundUpdate.attr("x", range[0]).attr("width", range[1] - range[0]);
	          redrawX(gUpdate);
	        }
	        if (y) {
	          range = d3_scaleRange(y);
	          backgroundUpdate.attr("y", range[0]).attr("height", range[1] - range[0]);
	          redrawY(gUpdate);
	        }
	        redraw(gUpdate);
	      });
	    }
	    brush.event = function(g) {
	      g.each(function() {
	        var event_ = event.of(this, arguments), extent1 = {
	          x: xExtent,
	          y: yExtent,
	          i: xExtentDomain,
	          j: yExtentDomain
	        }, extent0 = this.__chart__ || extent1;
	        this.__chart__ = extent1;
	        if (d3_transitionInheritId) {
	          d3.select(this).transition().each("start.brush", function() {
	            xExtentDomain = extent0.i;
	            yExtentDomain = extent0.j;
	            xExtent = extent0.x;
	            yExtent = extent0.y;
	            event_({
	              type: "brushstart"
	            });
	          }).tween("brush:brush", function() {
	            var xi = d3_interpolateArray(xExtent, extent1.x), yi = d3_interpolateArray(yExtent, extent1.y);
	            xExtentDomain = yExtentDomain = null;
	            return function(t) {
	              xExtent = extent1.x = xi(t);
	              yExtent = extent1.y = yi(t);
	              event_({
	                type: "brush",
	                mode: "resize"
	              });
	            };
	          }).each("end.brush", function() {
	            xExtentDomain = extent1.i;
	            yExtentDomain = extent1.j;
	            event_({
	              type: "brush",
	              mode: "resize"
	            });
	            event_({
	              type: "brushend"
	            });
	          });
	        } else {
	          event_({
	            type: "brushstart"
	          });
	          event_({
	            type: "brush",
	            mode: "resize"
	          });
	          event_({
	            type: "brushend"
	          });
	        }
	      });
	    };
	    function redraw(g) {
	      g.selectAll(".resize").attr("transform", function(d) {
	        return "translate(" + xExtent[+/e$/.test(d)] + "," + yExtent[+/^s/.test(d)] + ")";
	      });
	    }
	    function redrawX(g) {
	      g.select(".extent").attr("x", xExtent[0]);
	      g.selectAll(".extent,.n>rect,.s>rect").attr("width", xExtent[1] - xExtent[0]);
	    }
	    function redrawY(g) {
	      g.select(".extent").attr("y", yExtent[0]);
	      g.selectAll(".extent,.e>rect,.w>rect").attr("height", yExtent[1] - yExtent[0]);
	    }
	    function brushstart() {
	      var target = this, eventTarget = d3.select(d3.event.target), event_ = event.of(target, arguments), g = d3.select(target), resizing = eventTarget.datum(), resizingX = !/^(n|s)$/.test(resizing) && x, resizingY = !/^(e|w)$/.test(resizing) && y, dragging = eventTarget.classed("extent"), dragRestore = d3_event_dragSuppress(target), center, origin = d3.mouse(target), offset;
	      var w = d3.select(d3_window(target)).on("keydown.brush", keydown).on("keyup.brush", keyup);
	      if (d3.event.changedTouches) {
	        w.on("touchmove.brush", brushmove).on("touchend.brush", brushend);
	      } else {
	        w.on("mousemove.brush", brushmove).on("mouseup.brush", brushend);
	      }
	      g.interrupt().selectAll("*").interrupt();
	      if (dragging) {
	        origin[0] = xExtent[0] - origin[0];
	        origin[1] = yExtent[0] - origin[1];
	      } else if (resizing) {
	        var ex = +/w$/.test(resizing), ey = +/^n/.test(resizing);
	        offset = [ xExtent[1 - ex] - origin[0], yExtent[1 - ey] - origin[1] ];
	        origin[0] = xExtent[ex];
	        origin[1] = yExtent[ey];
	      } else if (d3.event.altKey) center = origin.slice();
	      g.style("pointer-events", "none").selectAll(".resize").style("display", null);
	      d3.select("body").style("cursor", eventTarget.style("cursor"));
	      event_({
	        type: "brushstart"
	      });
	      brushmove();
	      function keydown() {
	        if (d3.event.keyCode == 32) {
	          if (!dragging) {
	            center = null;
	            origin[0] -= xExtent[1];
	            origin[1] -= yExtent[1];
	            dragging = 2;
	          }
	          d3_eventPreventDefault();
	        }
	      }
	      function keyup() {
	        if (d3.event.keyCode == 32 && dragging == 2) {
	          origin[0] += xExtent[1];
	          origin[1] += yExtent[1];
	          dragging = 0;
	          d3_eventPreventDefault();
	        }
	      }
	      function brushmove() {
	        var point = d3.mouse(target), moved = false;
	        if (offset) {
	          point[0] += offset[0];
	          point[1] += offset[1];
	        }
	        if (!dragging) {
	          if (d3.event.altKey) {
	            if (!center) center = [ (xExtent[0] + xExtent[1]) / 2, (yExtent[0] + yExtent[1]) / 2 ];
	            origin[0] = xExtent[+(point[0] < center[0])];
	            origin[1] = yExtent[+(point[1] < center[1])];
	          } else center = null;
	        }
	        if (resizingX && move1(point, x, 0)) {
	          redrawX(g);
	          moved = true;
	        }
	        if (resizingY && move1(point, y, 1)) {
	          redrawY(g);
	          moved = true;
	        }
	        if (moved) {
	          redraw(g);
	          event_({
	            type: "brush",
	            mode: dragging ? "move" : "resize"
	          });
	        }
	      }
	      function move1(point, scale, i) {
	        var range = d3_scaleRange(scale), r0 = range[0], r1 = range[1], position = origin[i], extent = i ? yExtent : xExtent, size = extent[1] - extent[0], min, max;
	        if (dragging) {
	          r0 -= position;
	          r1 -= size + position;
	        }
	        min = (i ? yClamp : xClamp) ? Math.max(r0, Math.min(r1, point[i])) : point[i];
	        if (dragging) {
	          max = (min += position) + size;
	        } else {
	          if (center) position = Math.max(r0, Math.min(r1, 2 * center[i] - min));
	          if (position < min) {
	            max = min;
	            min = position;
	          } else {
	            max = position;
	          }
	        }
	        if (extent[0] != min || extent[1] != max) {
	          if (i) yExtentDomain = null; else xExtentDomain = null;
	          extent[0] = min;
	          extent[1] = max;
	          return true;
	        }
	      }
	      function brushend() {
	        brushmove();
	        g.style("pointer-events", "all").selectAll(".resize").style("display", brush.empty() ? "none" : null);
	        d3.select("body").style("cursor", null);
	        w.on("mousemove.brush", null).on("mouseup.brush", null).on("touchmove.brush", null).on("touchend.brush", null).on("keydown.brush", null).on("keyup.brush", null);
	        dragRestore();
	        event_({
	          type: "brushend"
	        });
	      }
	    }
	    brush.x = function(z) {
	      if (!arguments.length) return x;
	      x = z;
	      resizes = d3_svg_brushResizes[!x << 1 | !y];
	      return brush;
	    };
	    brush.y = function(z) {
	      if (!arguments.length) return y;
	      y = z;
	      resizes = d3_svg_brushResizes[!x << 1 | !y];
	      return brush;
	    };
	    brush.clamp = function(z) {
	      if (!arguments.length) return x && y ? [ xClamp, yClamp ] : x ? xClamp : y ? yClamp : null;
	      if (x && y) xClamp = !!z[0], yClamp = !!z[1]; else if (x) xClamp = !!z; else if (y) yClamp = !!z;
	      return brush;
	    };
	    brush.extent = function(z) {
	      var x0, x1, y0, y1, t;
	      if (!arguments.length) {
	        if (x) {
	          if (xExtentDomain) {
	            x0 = xExtentDomain[0], x1 = xExtentDomain[1];
	          } else {
	            x0 = xExtent[0], x1 = xExtent[1];
	            if (x.invert) x0 = x.invert(x0), x1 = x.invert(x1);
	            if (x1 < x0) t = x0, x0 = x1, x1 = t;
	          }
	        }
	        if (y) {
	          if (yExtentDomain) {
	            y0 = yExtentDomain[0], y1 = yExtentDomain[1];
	          } else {
	            y0 = yExtent[0], y1 = yExtent[1];
	            if (y.invert) y0 = y.invert(y0), y1 = y.invert(y1);
	            if (y1 < y0) t = y0, y0 = y1, y1 = t;
	          }
	        }
	        return x && y ? [ [ x0, y0 ], [ x1, y1 ] ] : x ? [ x0, x1 ] : y && [ y0, y1 ];
	      }
	      if (x) {
	        x0 = z[0], x1 = z[1];
	        if (y) x0 = x0[0], x1 = x1[0];
	        xExtentDomain = [ x0, x1 ];
	        if (x.invert) x0 = x(x0), x1 = x(x1);
	        if (x1 < x0) t = x0, x0 = x1, x1 = t;
	        if (x0 != xExtent[0] || x1 != xExtent[1]) xExtent = [ x0, x1 ];
	      }
	      if (y) {
	        y0 = z[0], y1 = z[1];
	        if (x) y0 = y0[1], y1 = y1[1];
	        yExtentDomain = [ y0, y1 ];
	        if (y.invert) y0 = y(y0), y1 = y(y1);
	        if (y1 < y0) t = y0, y0 = y1, y1 = t;
	        if (y0 != yExtent[0] || y1 != yExtent[1]) yExtent = [ y0, y1 ];
	      }
	      return brush;
	    };
	    brush.clear = function() {
	      if (!brush.empty()) {
	        xExtent = [ 0, 0 ], yExtent = [ 0, 0 ];
	        xExtentDomain = yExtentDomain = null;
	      }
	      return brush;
	    };
	    brush.empty = function() {
	      return !!x && xExtent[0] == xExtent[1] || !!y && yExtent[0] == yExtent[1];
	    };
	    return d3.rebind(brush, event, "on");
	  };
	  var d3_svg_brushCursor = {
	    n: "ns-resize",
	    e: "ew-resize",
	    s: "ns-resize",
	    w: "ew-resize",
	    nw: "nwse-resize",
	    ne: "nesw-resize",
	    se: "nwse-resize",
	    sw: "nesw-resize"
	  };
	  var d3_svg_brushResizes = [ [ "n", "e", "s", "w", "nw", "ne", "se", "sw" ], [ "e", "w" ], [ "n", "s" ], [] ];
	  var d3_time_format = d3_time.format = d3_locale_enUS.timeFormat;
	  var d3_time_formatUtc = d3_time_format.utc;
	  var d3_time_formatIso = d3_time_formatUtc("%Y-%m-%dT%H:%M:%S.%LZ");
	  d3_time_format.iso = Date.prototype.toISOString && +new Date("2000-01-01T00:00:00.000Z") ? d3_time_formatIsoNative : d3_time_formatIso;
	  function d3_time_formatIsoNative(date) {
	    return date.toISOString();
	  }
	  d3_time_formatIsoNative.parse = function(string) {
	    var date = new Date(string);
	    return isNaN(date) ? null : date;
	  };
	  d3_time_formatIsoNative.toString = d3_time_formatIso.toString;
	  d3_time.second = d3_time_interval(function(date) {
	    return new d3_date(Math.floor(date / 1e3) * 1e3);
	  }, function(date, offset) {
	    date.setTime(date.getTime() + Math.floor(offset) * 1e3);
	  }, function(date) {
	    return date.getSeconds();
	  });
	  d3_time.seconds = d3_time.second.range;
	  d3_time.seconds.utc = d3_time.second.utc.range;
	  d3_time.minute = d3_time_interval(function(date) {
	    return new d3_date(Math.floor(date / 6e4) * 6e4);
	  }, function(date, offset) {
	    date.setTime(date.getTime() + Math.floor(offset) * 6e4);
	  }, function(date) {
	    return date.getMinutes();
	  });
	  d3_time.minutes = d3_time.minute.range;
	  d3_time.minutes.utc = d3_time.minute.utc.range;
	  d3_time.hour = d3_time_interval(function(date) {
	    var timezone = date.getTimezoneOffset() / 60;
	    return new d3_date((Math.floor(date / 36e5 - timezone) + timezone) * 36e5);
	  }, function(date, offset) {
	    date.setTime(date.getTime() + Math.floor(offset) * 36e5);
	  }, function(date) {
	    return date.getHours();
	  });
	  d3_time.hours = d3_time.hour.range;
	  d3_time.hours.utc = d3_time.hour.utc.range;
	  d3_time.month = d3_time_interval(function(date) {
	    date = d3_time.day(date);
	    date.setDate(1);
	    return date;
	  }, function(date, offset) {
	    date.setMonth(date.getMonth() + offset);
	  }, function(date) {
	    return date.getMonth();
	  });
	  d3_time.months = d3_time.month.range;
	  d3_time.months.utc = d3_time.month.utc.range;
	  function d3_time_scale(linear, methods, format) {
	    function scale(x) {
	      return linear(x);
	    }
	    scale.invert = function(x) {
	      return d3_time_scaleDate(linear.invert(x));
	    };
	    scale.domain = function(x) {
	      if (!arguments.length) return linear.domain().map(d3_time_scaleDate);
	      linear.domain(x);
	      return scale;
	    };
	    function tickMethod(extent, count) {
	      var span = extent[1] - extent[0], target = span / count, i = d3.bisect(d3_time_scaleSteps, target);
	      return i == d3_time_scaleSteps.length ? [ methods.year, d3_scale_linearTickRange(extent.map(function(d) {
	        return d / 31536e6;
	      }), count)[2] ] : !i ? [ d3_time_scaleMilliseconds, d3_scale_linearTickRange(extent, count)[2] ] : methods[target / d3_time_scaleSteps[i - 1] < d3_time_scaleSteps[i] / target ? i - 1 : i];
	    }
	    scale.nice = function(interval, skip) {
	      var domain = scale.domain(), extent = d3_scaleExtent(domain), method = interval == null ? tickMethod(extent, 10) : typeof interval === "number" && tickMethod(extent, interval);
	      if (method) interval = method[0], skip = method[1];
	      function skipped(date) {
	        return !isNaN(date) && !interval.range(date, d3_time_scaleDate(+date + 1), skip).length;
	      }
	      return scale.domain(d3_scale_nice(domain, skip > 1 ? {
	        floor: function(date) {
	          while (skipped(date = interval.floor(date))) date = d3_time_scaleDate(date - 1);
	          return date;
	        },
	        ceil: function(date) {
	          while (skipped(date = interval.ceil(date))) date = d3_time_scaleDate(+date + 1);
	          return date;
	        }
	      } : interval));
	    };
	    scale.ticks = function(interval, skip) {
	      var extent = d3_scaleExtent(scale.domain()), method = interval == null ? tickMethod(extent, 10) : typeof interval === "number" ? tickMethod(extent, interval) : !interval.range && [ {
	        range: interval
	      }, skip ];
	      if (method) interval = method[0], skip = method[1];
	      return interval.range(extent[0], d3_time_scaleDate(+extent[1] + 1), skip < 1 ? 1 : skip);
	    };
	    scale.tickFormat = function() {
	      return format;
	    };
	    scale.copy = function() {
	      return d3_time_scale(linear.copy(), methods, format);
	    };
	    return d3_scale_linearRebind(scale, linear);
	  }
	  function d3_time_scaleDate(t) {
	    return new Date(t);
	  }
	  var d3_time_scaleSteps = [ 1e3, 5e3, 15e3, 3e4, 6e4, 3e5, 9e5, 18e5, 36e5, 108e5, 216e5, 432e5, 864e5, 1728e5, 6048e5, 2592e6, 7776e6, 31536e6 ];
	  var d3_time_scaleLocalMethods = [ [ d3_time.second, 1 ], [ d3_time.second, 5 ], [ d3_time.second, 15 ], [ d3_time.second, 30 ], [ d3_time.minute, 1 ], [ d3_time.minute, 5 ], [ d3_time.minute, 15 ], [ d3_time.minute, 30 ], [ d3_time.hour, 1 ], [ d3_time.hour, 3 ], [ d3_time.hour, 6 ], [ d3_time.hour, 12 ], [ d3_time.day, 1 ], [ d3_time.day, 2 ], [ d3_time.week, 1 ], [ d3_time.month, 1 ], [ d3_time.month, 3 ], [ d3_time.year, 1 ] ];
	  var d3_time_scaleLocalFormat = d3_time_format.multi([ [ ".%L", function(d) {
	    return d.getMilliseconds();
	  } ], [ ":%S", function(d) {
	    return d.getSeconds();
	  } ], [ "%I:%M", function(d) {
	    return d.getMinutes();
	  } ], [ "%I %p", function(d) {
	    return d.getHours();
	  } ], [ "%a %d", function(d) {
	    return d.getDay() && d.getDate() != 1;
	  } ], [ "%b %d", function(d) {
	    return d.getDate() != 1;
	  } ], [ "%B", function(d) {
	    return d.getMonth();
	  } ], [ "%Y", d3_true ] ]);
	  var d3_time_scaleMilliseconds = {
	    range: function(start, stop, step) {
	      return d3.range(Math.ceil(start / step) * step, +stop, step).map(d3_time_scaleDate);
	    },
	    floor: d3_identity,
	    ceil: d3_identity
	  };
	  d3_time_scaleLocalMethods.year = d3_time.year;
	  d3_time.scale = function() {
	    return d3_time_scale(d3.scale.linear(), d3_time_scaleLocalMethods, d3_time_scaleLocalFormat);
	  };
	  var d3_time_scaleUtcMethods = d3_time_scaleLocalMethods.map(function(m) {
	    return [ m[0].utc, m[1] ];
	  });
	  var d3_time_scaleUtcFormat = d3_time_formatUtc.multi([ [ ".%L", function(d) {
	    return d.getUTCMilliseconds();
	  } ], [ ":%S", function(d) {
	    return d.getUTCSeconds();
	  } ], [ "%I:%M", function(d) {
	    return d.getUTCMinutes();
	  } ], [ "%I %p", function(d) {
	    return d.getUTCHours();
	  } ], [ "%a %d", function(d) {
	    return d.getUTCDay() && d.getUTCDate() != 1;
	  } ], [ "%b %d", function(d) {
	    return d.getUTCDate() != 1;
	  } ], [ "%B", function(d) {
	    return d.getUTCMonth();
	  } ], [ "%Y", d3_true ] ]);
	  d3_time_scaleUtcMethods.year = d3_time.year.utc;
	  d3_time.scale.utc = function() {
	    return d3_time_scale(d3.scale.linear(), d3_time_scaleUtcMethods, d3_time_scaleUtcFormat);
	  };
	  d3.text = d3_xhrType(function(request) {
	    return request.responseText;
	  });
	  d3.json = function(url, callback) {
	    return d3_xhr(url, "application/json", d3_json, callback);
	  };
	  function d3_json(request) {
	    return JSON.parse(request.responseText);
	  }
	  d3.html = function(url, callback) {
	    return d3_xhr(url, "text/html", d3_html, callback);
	  };
	  function d3_html(request) {
	    var range = d3_document.createRange();
	    range.selectNode(d3_document.body);
	    return range.createContextualFragment(request.responseText);
	  }
	  d3.xml = d3_xhrType(function(request) {
	    return request.responseXML;
	  });
	  if (true) this.d3 = d3, !(__WEBPACK_AMD_DEFINE_FACTORY__ = (d3), __WEBPACK_AMD_DEFINE_RESULT__ = (typeof __WEBPACK_AMD_DEFINE_FACTORY__ === 'function' ? (__WEBPACK_AMD_DEFINE_FACTORY__.call(exports, __webpack_require__, exports, module)) : __WEBPACK_AMD_DEFINE_FACTORY__), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__)); else if (typeof module === "object" && module.exports) module.exports = d3; else this.d3 = d3;
	}();

/***/ },
/* 30 */
/***/ function(module, exports, __webpack_require__) {

	// style-loader: Adds some css to the DOM by adding a <style> tag

	// load the styles
	var content = __webpack_require__(31);
	if(typeof content === 'string') content = [[module.id, content, '']];
	// add the styles to the DOM
	var update = __webpack_require__(40)(content, {});
	if(content.locals) module.exports = content.locals;
	// Hot Module Replacement
	if(false) {
		// When the styles change, update the <style> tags
		if(!content.locals) {
			module.hot.accept("!!../../../css-loader/index.js?modules&importLoaders=1&localIdentName=[name]_[local]_[hash:base64:5]!../../../postcss-loader/index.js!./HistogramSelector.mcss", function() {
				var newContent = require("!!../../../css-loader/index.js?modules&importLoaders=1&localIdentName=[name]_[local]_[hash:base64:5]!../../../postcss-loader/index.js!./HistogramSelector.mcss");
				if(typeof newContent === 'string') newContent = [[module.id, newContent, '']];
				update(newContent);
			});
		}
		// When the module is disposed, remove the <style> tags
		module.hot.dispose(function() { update(); });
	}

/***/ },
/* 31 */
/***/ function(module, exports, __webpack_require__) {

	exports = module.exports = __webpack_require__(32)();
	// imports
	exports.i(__webpack_require__(33), undefined);

	// module
	exports.push([module.id, "/*empty styles allow for d3 selection in javascript*/\n.HistogramSelector_jsAxis_1pGZQ,\n.HistogramSelector_jsBox_1vGt3,\n.HistogramSelector_jsBrush_1eNJZ,\n.HistogramSelector_jsDividerPopup_L8pF0,\n.HistogramSelector_jsDividerUncertaintyInput_1ZXZw,\n.HistogramSelector_jsDividerValueInput_1fQL4,\n.HistogramSelector_jsDividerValuePopup_9Y9Hn,\n.HistogramSelector_jsExpandIcon_2txtV,\n.HistogramSelector_jsFieldName_OPMVu,\n.HistogramSelector_jsFieldsIcon_3Kl1G,\n.HistogramSelector_jsGHist_1ehp9,\n.HistogramSelector_jsGRect_dSYnn,\n.HistogramSelector_jsHeader_3Koz7,\n.HistogramSelector_jsHeaderBoxes_3ETEq,\n.HistogramSelector_jsHeaderBoxesNum_2rd2E,\n.HistogramSelector_jsHeaderLabel_2MECq,\n.HistogramSelector_jsHeaderSingle_2DfIQ,\n.HistogramSelector_jsHeaderSingleField_328Ln,\n.HistogramSelector_jsHistRect_2PVAN,\n.HistogramSelector_jsLegend_3hNgG,\n.HistogramSelector_jsLegendIcons_1zY13,\n.HistogramSelector_jsLegendIconsViz_2UF04,\n.HistogramSelector_jsLegendRow_2iIRX,\n.HistogramSelector_jsOverlay_1TdL7,\n.HistogramSelector_jsScore_2xbb4,\n.HistogramSelector_jsScoreBackground_nzITI,\n.HistogramSelector_jsScoreChoice_zMVVX,\n.HistogramSelector_jsScoreDivLabel_3pIsD,\n.HistogramSelector_jsScoredHeader_Ap2nr,\n.HistogramSelector_jsScoreIcon_13I3g,\n.HistogramSelector_jsSaveIcon_KjcLz,\n.HistogramSelector_jsScoreLabel_3JsXU,\n.HistogramSelector_jsScorePopup_1W48_,\n.HistogramSelector_jsScoreRect_277qI,\n.HistogramSelector_jsScoreUncertainty_pz16k,\n.HistogramSelector_jsShowScoredIcon_K0plJ,\n.HistogramSelector_jsSparkline_1Zv-Q,\n.HistogramSelector_jsTr2_1jUw9 {\n\n}\n\n.HistogramSelector_histogramSelector_2BGDQ {\n  font-family: \"Optima\", \"Linux Biolinum\", \"URW Classico\", sans;\n  font-size: 10pt;\n  position: relative;\n  top: 0px;\n  bottom: 0px;\n  left: 0px;\n  right: 0px;\n  box-sizing: border-box;\n}\n\n/* fix a chrome bug with scrolling - must be rendered, full-height */\n.HistogramSelector_parameterScrollFix_1MpVK {\n  position: absolute;\n  top: 0px;\n  bottom: 0px;\n  width: 1px;\n}\n\n.HistogramSelector_header_2R4nn {\n  font-family: \"Optima\", \"Linux Biolinum\", \"URW Classico\", sans;\n  font-size: 11pt;\n  font-weight: bold;\n  -webkit-user-select: none;\n     -moz-user-select: none;\n      -ms-user-select: none;\n          user-select: none;\n  cursor: pointer;\n}\n\n.HistogramSelector_hidden_ajRQ1 {\n  opacity: 0;\n}\n\n.HistogramSelector_icon_1bFhf {\n  -webkit-user-select: none;\n     -moz-user-select: none;\n      -ms-user-select: none;\n          user-select: none;\n  cursor: pointer;\n  padding: 1px 5px;\n}\n\n.HistogramSelector_selectedIcon_d6fpx {\n}\n\n.HistogramSelector_allIcon_TZOcZ {\n}\n\n.HistogramSelector_selectedFieldsIcon_U5UaG {\n}\n.HistogramSelector_allFieldsIcon_PXP1O {\n}\n.HistogramSelector_onlyScoredIcon_1UbxO {\n}\n.HistogramSelector_allScoredIcon_2XiRW {\n}\n\n.HistogramSelector_headerIcon_1n4DA {\n  vertical-align: middle;\n}\n\n.HistogramSelector_headerBoxesPlus_1vxrx {\n  padding-right: 2px;\n}\n.HistogramSelector_headerBoxesMinus_276L_ {\n  padding-left: 2px;\n}\n\n.HistogramSelector_headerBoxes_Cr0NF {\n  padding-left: 10px;\n  padding-right: 10px;\n}\n\n.HistogramSelector_headerSingleIcon_omcfx {\n}\n.HistogramSelector_headerSingleNext_2FYhF {\n}\n.HistogramSelector_headerSinglePrev_2ZvTw {\n}\n.HistogramSelector_headerSingle_MhoJ9 {\n  padding-left: 10px;\n  padding-right: 10px;\n}\n\n.HistogramSelector_legendIcon_11azX {\n  vertical-align: middle;\n  padding: 1px 1px;\n}\n.HistogramSelector_expandIcon_aB_gx {\n  }\n.HistogramSelector_shrinkIcon_1-H3j {\n}\n\n.HistogramSelector_scoreStartIcon_3_Fu7 {\n}\n\n.HistogramSelector_scoreEndIcon_lPEwV {\n}\n\n.HistogramSelector_hideScoreIcon_3qSR- {\n  display: none;\n}\n\n.HistogramSelector_saveIcon_2Euan {\n}\n.HistogramSelector_noSaveIcon_3t4kV {\n  display: none;\n}\n.HistogramSelector_unchangedSaveIcon_1C7og {\n}\n.HistogramSelector_unchangedActiveSaveIcon_1fubr {\n}\n.HistogramSelector_modifiedSaveIcon_Uu1N_ {\n}\n.HistogramSelector_newSaveIcon_1ti1c {\n}\n\n.HistogramSelector_histogramSelectorCell_oPDdR {\n  padding: 0px;\n}\n\n.HistogramSelector_baseLegend_2tDFp {\n  text-align: center;\n  border-bottom: 1px solid #fff;\n  width: 19px;\n  -webkit-user-select: none;\n     -moz-user-select: none;\n      -ms-user-select: none;\n          user-select: none;\n}\n.HistogramSelector_legend_3GheZ {\n}\n.HistogramSelector_legendSvg_3lqDb {\n  margin: 3px 3px 2px 3px;\n  vertical-align: middle;\n}\n\n.HistogramSelector_baseFieldName_3z46Q {\n  width: auto;\n  white-space: nowrap;\n  overflow: hidden;\n  text-align: left;\n  text-overflow: ellipsis;\n  border-bottom: 1px solid #fff;\n  padding: 2px;\n  -webkit-user-select: none;\n     -moz-user-select: none;\n      -ms-user-select: none;\n          user-select: none;\n}\n.HistogramSelector_fieldName_1kc9i {\n}\n\n.HistogramSelector_row_2XWV2 {\n  position: absolute;\n  width: 100%;\n}\n\n.HistogramSelector_baseLegendRow_1J7Jl {\n  -webkit-user-select: none;\n     -moz-user-select: none;\n      -ms-user-select: none;\n          user-select: none;\n  cursor: pointer;\n}\n.HistogramSelector_legendRow_3B9CI {\n}\n\n.HistogramSelector_unselectedLegendRow_20lua {\n  opacity: 0.5;\n}\n\n.HistogramSelector_selectedLegendRow_1kV6t {\n  background-color: #ccd;\n  opacity: 1;\n}\n\n.HistogramSelector_legendIcons_3k1CP {\n  padding-right: 1px;\n  padding-left: 0px;\n  text-align: right;\n  border-bottom: 1px solid #fff;\n  overflow: hidden;\n}\n\n.HistogramSelector_legendIconsViz_1Ip34 {\n  visibility: hidden;\n  padding: 0;\n  margin: 0;\n}\n\n.HistogramSelector_baseLegendRow_1J7Jl:hover {\n  background-color: #ccd;\n}\n.HistogramSelector_jsLegendRow_2iIRX:hover .HistogramSelector_baseLegend_2tDFp, .HistogramSelector_jsLegendRow_2iIRX:hover .HistogramSelector_baseFieldName_3z46Q, .HistogramSelector_jsLegendRow_2iIRX:hover .HistogramSelector_legendIcons_3k1CP {\n  border-bottom: 1px solid #000;\n}\n\n.HistogramSelector_jsLegendRow_2iIRX:hover .HistogramSelector_legendIconsViz_1Ip34 {\n  visibility: visible;\n}\n.HistogramSelector_sparkline_1CB3c {\n}\n.HistogramSelector_sparklineSvg_1rYHz {\n  vertical-align: middle;\n}\n\n.HistogramSelector_box_1xpw0 {\n  -webkit-user-select: none;\n     -moz-user-select: none;\n      -ms-user-select: none;\n          user-select: none;\n  /*cursor: crosshair;*/\n  padding: 0px;\n  margin: 3px;\n  border-width: 3px;\n  border-spacing: 0;\n  border-collapse: separate;\n  border-radius: 6px;\n  border-style: solid;\n  background-color: #fff;\n  float: left;\n  table-layout: fixed;\n}\n.HistogramSelector_unselectedBox_35q11 {\n  border-color: #bbb;\n}\n.HistogramSelector_selectedBox_1LOEC {\n  border-color: #222;\n}\n.HistogramSelector_hiddenBox_3GDat {\n}\n.HistogramSelector_jsBox_1vGt3 td, .HistogramSelector_jsBox_1vGt3 tr {\n  padding: 1px;\n}\n\n/* When hovering over the box, set the legendRow's styles */\n.HistogramSelector_jsBox_1vGt3:hover .HistogramSelector_baseLegendRow_1J7Jl {\n  background-color: #ccd;\n}\n.HistogramSelector_jsBox_1vGt3:hover .HistogramSelector_baseLegend_2tDFp, .HistogramSelector_jsBox_1vGt3:hover .HistogramSelector_baseFieldName_3z46Q, .HistogramSelector_jsBox_1vGt3:hover .HistogramSelector_legendIcons_3k1CP {\n  border-bottom: 1px solid #000;\n}\n.HistogramSelector_jsBox_1vGt3:hover .HistogramSelector_jsLegendIconsViz_2UF04 {\n  visibility: visible;\n}\n\n.HistogramSelector_histRect_1UEwr {\n  stroke: none;\n  shape-rendering: crispEdges;\n}\n.HistogramSelector_histRectEven_3FutJ {\n  fill: #8089B8;\n}\n.HistogramSelector_histRectOdd_4RfIA {\n  fill: #7780AB;\n}\n\n.HistogramSelector_hmax_3wTiP {\n  text-align: right;\n  -webkit-user-select: none;\n     -moz-user-select: none;\n      -ms-user-select: none;\n          user-select: none;\n}\n.HistogramSelector_hmin_16nnF {\n  text-align: left;\n  -webkit-user-select: none;\n     -moz-user-select: none;\n      -ms-user-select: none;\n          user-select: none;\n}\n\n.HistogramSelector_axis_3OqVv {\n}\n.HistogramSelector_axisPath_qIldS,\n.HistogramSelector_axisLine_19AQv {\n  fill: none;\n  stroke: #000;\n  shape-rendering: crispEdges;\n}\n.HistogramSelector_axisText_2P5dU {\n  cursor: default;\n}\n\n.HistogramSelector_overlay_x6P8f {\n  fill: none;\n  pointer-events: all;\n}\n\n.HistogramSelector_binHilite_1BUiw {\n  fill: #001EB8;\n}\n\n/* Scoring gui */\n.HistogramSelector_score_3lBn1 {\n  stroke: #fff;\n  shape-rendering: crispEdges;\n}\n\n.HistogramSelector_scoreRegionBg_3pdKz {\n\n}\n\n.HistogramSelector_jsBox_1vGt3:hover .HistogramSelector_scoreRegion_3uyve {\n  opacity: 0.2;\n}\n\n.HistogramSelector_scoreRegionFg_3JuA0 {\n}\n\n.HistogramSelector_popup_35N9Q {\n  position: absolute;\n  background-color: #fff;\n  border: 1px #ccc solid;\n  border-radius: 6px;\n  padding: 3px;\n  font-size: 8pt;\n}\n.HistogramSelector_scorePopup_2VcRU {\n}\n.HistogramSelector_dividerPopup_N4rgx {\n}\n.HistogramSelector_dividerValuePopup_cdXvt {\n}\n\n.HistogramSelector_popupCell_KO5FK {\n  padding: 2px;\n}\n\n.HistogramSelector_scoreChoice_c8xi4 {\n  float: left;\n  display: none;\n}\n\n.HistogramSelector_scoreLabel_1Wt_3 {\n  display: block;\n  border-radius: 5px;\n  padding: 4px;\n  margin: 2px;\n  line-height: 16px;\n}\n\n.HistogramSelector_scoreSwatch_1LwQi {\n  float: left;\n  width: 14px;\n  height: 14px;\n  margin-right: 8px;\n  border: 1px #707070 solid;\n  border-radius: 3px;\n}\n\n.HistogramSelector_scoreButton_1I3w9 {\n  border: 1px #4C4CA3 solid;\n  border-radius: 5px;\n  padding: 4px;\n  margin: 2px;\n}\n\n.HistogramSelector_scoreDashSpacer_ec3ZO {\n  border-bottom: 1px #bbb solid;\n  width: 95%;\n  height: 1px;\n  margin: 2px auto 3px auto;\n}\n", ""]);

	// exports
	exports.locals = {
		"jsAxis": "HistogramSelector_jsAxis_1pGZQ",
		"jsBox": "HistogramSelector_jsBox_1vGt3",
		"jsBrush": "HistogramSelector_jsBrush_1eNJZ",
		"jsDividerPopup": "HistogramSelector_jsDividerPopup_L8pF0",
		"jsDividerUncertaintyInput": "HistogramSelector_jsDividerUncertaintyInput_1ZXZw",
		"jsDividerValueInput": "HistogramSelector_jsDividerValueInput_1fQL4",
		"jsDividerValuePopup": "HistogramSelector_jsDividerValuePopup_9Y9Hn",
		"jsExpandIcon": "HistogramSelector_jsExpandIcon_2txtV",
		"jsFieldName": "HistogramSelector_jsFieldName_OPMVu",
		"jsFieldsIcon": "HistogramSelector_jsFieldsIcon_3Kl1G",
		"jsGHist": "HistogramSelector_jsGHist_1ehp9",
		"jsGRect": "HistogramSelector_jsGRect_dSYnn",
		"jsHeader": "HistogramSelector_jsHeader_3Koz7",
		"jsHeaderBoxes": "HistogramSelector_jsHeaderBoxes_3ETEq",
		"jsHeaderBoxesNum": "HistogramSelector_jsHeaderBoxesNum_2rd2E",
		"jsHeaderLabel": "HistogramSelector_jsHeaderLabel_2MECq",
		"jsHeaderSingle": "HistogramSelector_jsHeaderSingle_2DfIQ",
		"jsHeaderSingleField": "HistogramSelector_jsHeaderSingleField_328Ln",
		"jsHistRect": "HistogramSelector_jsHistRect_2PVAN",
		"jsLegend": "HistogramSelector_jsLegend_3hNgG",
		"jsLegendIcons": "HistogramSelector_jsLegendIcons_1zY13",
		"jsLegendIconsViz": "HistogramSelector_jsLegendIconsViz_2UF04",
		"jsLegendRow": "HistogramSelector_jsLegendRow_2iIRX",
		"jsOverlay": "HistogramSelector_jsOverlay_1TdL7",
		"jsScore": "HistogramSelector_jsScore_2xbb4",
		"jsScoreBackground": "HistogramSelector_jsScoreBackground_nzITI",
		"jsScoreChoice": "HistogramSelector_jsScoreChoice_zMVVX",
		"jsScoreDivLabel": "HistogramSelector_jsScoreDivLabel_3pIsD",
		"jsScoredHeader": "HistogramSelector_jsScoredHeader_Ap2nr",
		"jsScoreIcon": "HistogramSelector_jsScoreIcon_13I3g",
		"jsSaveIcon": "HistogramSelector_jsSaveIcon_KjcLz",
		"jsScoreLabel": "HistogramSelector_jsScoreLabel_3JsXU",
		"jsScorePopup": "HistogramSelector_jsScorePopup_1W48_",
		"jsScoreRect": "HistogramSelector_jsScoreRect_277qI",
		"jsScoreUncertainty": "HistogramSelector_jsScoreUncertainty_pz16k",
		"jsShowScoredIcon": "HistogramSelector_jsShowScoredIcon_K0plJ",
		"jsSparkline": "HistogramSelector_jsSparkline_1Zv-Q",
		"jsTr2": "HistogramSelector_jsTr2_1jUw9",
		"histogramSelector": "HistogramSelector_histogramSelector_2BGDQ",
		"parameterScrollFix": "HistogramSelector_parameterScrollFix_1MpVK",
		"header": "HistogramSelector_header_2R4nn HistogramSelector_jsHeader_3Koz7",
		"hidden": "HistogramSelector_hidden_ajRQ1",
		"icon": "HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + "",
		"selectedIcon": "HistogramSelector_selectedIcon_d6fpx HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-check-square-o"] + "",
		"allIcon": "HistogramSelector_allIcon_TZOcZ HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-square-o"] + "",
		"selectedFieldsIcon": "HistogramSelector_selectedFieldsIcon_U5UaG HistogramSelector_jsFieldsIcon_3Kl1G HistogramSelector_selectedIcon_d6fpx HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-check-square-o"] + "",
		"allFieldsIcon": "HistogramSelector_allFieldsIcon_PXP1O HistogramSelector_jsFieldsIcon_3Kl1G HistogramSelector_allIcon_TZOcZ HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-square-o"] + "",
		"onlyScoredIcon": "HistogramSelector_onlyScoredIcon_1UbxO HistogramSelector_jsShowScoredIcon_K0plJ HistogramSelector_selectedIcon_d6fpx HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-check-square-o"] + "",
		"allScoredIcon": "HistogramSelector_allScoredIcon_2XiRW HistogramSelector_jsShowScoredIcon_K0plJ HistogramSelector_allIcon_TZOcZ HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-square-o"] + "",
		"headerIcon": "HistogramSelector_headerIcon_1n4DA HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-lg"] + "",
		"headerBoxesPlus": "HistogramSelector_headerBoxesPlus_1vxrx HistogramSelector_headerIcon_1n4DA HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-lg"] + " " + __webpack_require__(33).locals["fa-plus-square-o"] + "",
		"headerBoxesMinus": "HistogramSelector_headerBoxesMinus_276L_ HistogramSelector_headerIcon_1n4DA HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-lg"] + " " + __webpack_require__(33).locals["fa-minus-square-o"] + "",
		"headerBoxes": "HistogramSelector_headerBoxes_Cr0NF HistogramSelector_jsHeaderBoxes_3ETEq",
		"headerSingleIcon": "HistogramSelector_headerSingleIcon_omcfx HistogramSelector_headerIcon_1n4DA HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-lg"] + "",
		"headerSingleNext": "HistogramSelector_headerSingleNext_2FYhF HistogramSelector_headerSingleIcon_omcfx HistogramSelector_headerIcon_1n4DA HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-lg"] + " " + __webpack_require__(33).locals["fa-caret-right"] + "",
		"headerSinglePrev": "HistogramSelector_headerSinglePrev_2ZvTw HistogramSelector_headerSingleIcon_omcfx HistogramSelector_headerIcon_1n4DA HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-lg"] + " " + __webpack_require__(33).locals["fa-caret-left"] + "",
		"headerSingle": "HistogramSelector_headerSingle_MhoJ9 HistogramSelector_jsHeaderSingle_2DfIQ",
		"legendIcon": "HistogramSelector_legendIcon_11azX " + __webpack_require__(33).locals["fa-lg"] + "",
		"expandIcon": "HistogramSelector_expandIcon_aB_gx HistogramSelector_jsExpandIcon_2txtV HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-expand"] + " HistogramSelector_legendIcon_11azX " + __webpack_require__(33).locals["fa-lg"] + "",
		"shrinkIcon": "HistogramSelector_shrinkIcon_1-H3j HistogramSelector_jsExpandIcon_2txtV HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-th-large"] + " HistogramSelector_legendIcon_11azX " + __webpack_require__(33).locals["fa-lg"] + "",
		"scoreStartIcon": "HistogramSelector_scoreStartIcon_3_Fu7 HistogramSelector_jsScoreIcon_13I3g HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-star-o"] + " HistogramSelector_legendIcon_11azX " + __webpack_require__(33).locals["fa-lg"] + "",
		"scoreEndIcon": "HistogramSelector_scoreEndIcon_lPEwV HistogramSelector_jsScoreIcon_13I3g HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-star"] + " HistogramSelector_legendIcon_11azX " + __webpack_require__(33).locals["fa-lg"] + "",
		"hideScoreIcon": "HistogramSelector_hideScoreIcon_3qSR- HistogramSelector_jsScoreIcon_13I3g",
		"saveIcon": "HistogramSelector_saveIcon_2Euan HistogramSelector_jsSaveIcon_KjcLz HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " HistogramSelector_legendIcon_11azX " + __webpack_require__(33).locals["fa-lg"] + "",
		"noSaveIcon": "HistogramSelector_noSaveIcon_3t4kV HistogramSelector_saveIcon_2Euan HistogramSelector_jsSaveIcon_KjcLz HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " HistogramSelector_legendIcon_11azX " + __webpack_require__(33).locals["fa-lg"] + "",
		"unchangedSaveIcon": "HistogramSelector_unchangedSaveIcon_1C7og HistogramSelector_saveIcon_2Euan HistogramSelector_jsSaveIcon_KjcLz HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " HistogramSelector_legendIcon_11azX " + __webpack_require__(33).locals["fa-lg"] + " " + __webpack_require__(33).locals["fa-bookmark-o"] + "",
		"unchangedActiveSaveIcon": "HistogramSelector_unchangedActiveSaveIcon_1fubr HistogramSelector_saveIcon_2Euan HistogramSelector_jsSaveIcon_KjcLz HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " HistogramSelector_legendIcon_11azX " + __webpack_require__(33).locals["fa-lg"] + " " + __webpack_require__(33).locals["fa-bookmark"] + "",
		"modifiedSaveIcon": "HistogramSelector_modifiedSaveIcon_Uu1N_ HistogramSelector_saveIcon_2Euan HistogramSelector_jsSaveIcon_KjcLz HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " HistogramSelector_legendIcon_11azX " + __webpack_require__(33).locals["fa-lg"] + " " + __webpack_require__(33).locals["fa-floppy-o"] + "",
		"newSaveIcon": "HistogramSelector_newSaveIcon_1ti1c HistogramSelector_saveIcon_2Euan HistogramSelector_jsSaveIcon_KjcLz HistogramSelector_icon_1bFhf " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " HistogramSelector_legendIcon_11azX " + __webpack_require__(33).locals["fa-lg"] + " " + __webpack_require__(33).locals["fa-plus"] + "",
		"histogramSelectorCell": "HistogramSelector_histogramSelectorCell_oPDdR",
		"baseLegend": "HistogramSelector_baseLegend_2tDFp",
		"legend": "HistogramSelector_legend_3GheZ HistogramSelector_jsLegend_3hNgG HistogramSelector_histogramSelectorCell_oPDdR HistogramSelector_baseLegend_2tDFp",
		"legendSvg": "HistogramSelector_legendSvg_3lqDb",
		"baseFieldName": "HistogramSelector_baseFieldName_3z46Q",
		"fieldName": "HistogramSelector_fieldName_1kc9i HistogramSelector_jsFieldName_OPMVu HistogramSelector_histogramSelectorCell_oPDdR HistogramSelector_baseFieldName_3z46Q",
		"row": "HistogramSelector_row_2XWV2",
		"baseLegendRow": "HistogramSelector_baseLegendRow_1J7Jl",
		"legendRow": "HistogramSelector_legendRow_3B9CI HistogramSelector_jsLegendRow_2iIRX HistogramSelector_baseLegendRow_1J7Jl",
		"unselectedLegendRow": "HistogramSelector_unselectedLegendRow_20lua HistogramSelector_legendRow_3B9CI HistogramSelector_jsLegendRow_2iIRX HistogramSelector_baseLegendRow_1J7Jl",
		"selectedLegendRow": "HistogramSelector_selectedLegendRow_1kV6t HistogramSelector_legendRow_3B9CI HistogramSelector_jsLegendRow_2iIRX HistogramSelector_baseLegendRow_1J7Jl",
		"legendIcons": "HistogramSelector_legendIcons_3k1CP HistogramSelector_jsLegendIcons_1zY13",
		"legendIconsViz": "HistogramSelector_legendIconsViz_1Ip34 HistogramSelector_jsLegendIconsViz_2UF04",
		"sparkline": "HistogramSelector_sparkline_1CB3c HistogramSelector_jsSparkline_1Zv-Q HistogramSelector_histogramSelectorCell_oPDdR",
		"sparklineSvg": "HistogramSelector_sparklineSvg_1rYHz",
		"box": "HistogramSelector_box_1xpw0 HistogramSelector_jsBox_1vGt3",
		"unselectedBox": "HistogramSelector_unselectedBox_35q11 HistogramSelector_box_1xpw0 HistogramSelector_jsBox_1vGt3",
		"selectedBox": "HistogramSelector_selectedBox_1LOEC HistogramSelector_box_1xpw0 HistogramSelector_jsBox_1vGt3",
		"hiddenBox": "HistogramSelector_hiddenBox_3GDat HistogramSelector_box_1xpw0 HistogramSelector_jsBox_1vGt3 HistogramSelector_hidden_ajRQ1",
		"histRect": "HistogramSelector_histRect_1UEwr HistogramSelector_jsHistRect_2PVAN",
		"histRectEven": "HistogramSelector_histRectEven_3FutJ HistogramSelector_histRect_1UEwr HistogramSelector_jsHistRect_2PVAN",
		"histRectOdd": "HistogramSelector_histRectOdd_4RfIA HistogramSelector_histRect_1UEwr HistogramSelector_jsHistRect_2PVAN",
		"hmax": "HistogramSelector_hmax_3wTiP HistogramSelector_histogramSelectorCell_oPDdR",
		"hmin": "HistogramSelector_hmin_16nnF HistogramSelector_histogramSelectorCell_oPDdR",
		"axis": "HistogramSelector_axis_3OqVv HistogramSelector_jsAxis_1pGZQ",
		"axisPath": "HistogramSelector_axisPath_qIldS",
		"axisLine": "HistogramSelector_axisLine_19AQv",
		"axisText": "HistogramSelector_axisText_2P5dU",
		"overlay": "HistogramSelector_overlay_x6P8f HistogramSelector_jsOverlay_1TdL7",
		"binHilite": "HistogramSelector_binHilite_1BUiw",
		"score": "HistogramSelector_score_3lBn1 HistogramSelector_jsScore_2xbb4",
		"scoreRegionBg": "HistogramSelector_scoreRegionBg_3pdKz",
		"scoreRegion": "HistogramSelector_scoreRegion_3uyve",
		"scoreRegionFg": "HistogramSelector_scoreRegionFg_3JuA0 HistogramSelector_jsScoreRect_277qI HistogramSelector_scoreRegion_3uyve",
		"popup": "HistogramSelector_popup_35N9Q",
		"scorePopup": "HistogramSelector_scorePopup_2VcRU HistogramSelector_jsScorePopup_1W48_ HistogramSelector_popup_35N9Q",
		"dividerPopup": "HistogramSelector_dividerPopup_N4rgx HistogramSelector_jsDividerPopup_L8pF0 HistogramSelector_popup_35N9Q",
		"dividerValuePopup": "HistogramSelector_dividerValuePopup_cdXvt HistogramSelector_jsDividerValuePopup_9Y9Hn HistogramSelector_popup_35N9Q",
		"popupCell": "HistogramSelector_popupCell_KO5FK",
		"scoreChoice": "HistogramSelector_scoreChoice_c8xi4 HistogramSelector_jsScoreChoice_zMVVX",
		"scoreLabel": "HistogramSelector_scoreLabel_1Wt_3 HistogramSelector_jsScoreLabel_3JsXU",
		"scoreSwatch": "HistogramSelector_scoreSwatch_1LwQi",
		"scoreButton": "HistogramSelector_scoreButton_1I3w9",
		"scoreDashSpacer": "HistogramSelector_scoreDashSpacer_ec3ZO"
	};

/***/ },
/* 32 */
/***/ function(module, exports) {

	/*
		MIT License http://www.opensource.org/licenses/mit-license.php
		Author Tobias Koppers @sokra
	*/
	// css base code, injected by the css-loader
	module.exports = function() {
		var list = [];

		// return the list of modules as css string
		list.toString = function toString() {
			var result = [];
			for(var i = 0; i < this.length; i++) {
				var item = this[i];
				if(item[2]) {
					result.push("@media " + item[2] + "{" + item[1] + "}");
				} else {
					result.push(item[1]);
				}
			}
			return result.join("");
		};

		// import a list of modules into the list
		list.i = function(modules, mediaQuery) {
			if(typeof modules === "string")
				modules = [[null, modules, ""]];
			var alreadyImportedModules = {};
			for(var i = 0; i < this.length; i++) {
				var id = this[i][0];
				if(typeof id === "number")
					alreadyImportedModules[id] = true;
			}
			for(i = 0; i < modules.length; i++) {
				var item = modules[i];
				// skip already imported module
				// this implementation is not 100% perfect for weird media query combinations
				//  when a module is imported multiple times with different media queries.
				//  I hope this will never occur (Hey this way we have smaller bundles)
				if(typeof item[0] !== "number" || !alreadyImportedModules[item[0]]) {
					if(mediaQuery && !item[2]) {
						item[2] = mediaQuery;
					} else if(mediaQuery) {
						item[2] = "(" + item[2] + ") and (" + mediaQuery + ")";
					}
					list.push(item);
				}
			}
		};
		return list;
	};


/***/ },
/* 33 */
/***/ function(module, exports, __webpack_require__) {

	exports = module.exports = __webpack_require__(32)();
	// imports


	// module
	exports.push([module.id, "/*!\n *  Font Awesome 4.7.0 by @davegandy - http://fontawesome.io - @fontawesome\n *  License - http://fontawesome.io/license (Font: SIL OFL 1.1, CSS: MIT License)\n */\n/* FONT PATH\n * -------------------------- */\n@font-face {\n  font-family: 'FontAwesome';\n  src: url(" + __webpack_require__(34) + ");\n  src: url(" + __webpack_require__(35) + "?#iefix&v=4.7.0) format('embedded-opentype'), url(" + __webpack_require__(36) + ") format('woff2'), url(" + __webpack_require__(37) + ") format('woff'), url(" + __webpack_require__(38) + ") format('truetype'), url(" + __webpack_require__(39) + "#fontawesomeregular) format('svg');\n  font-weight: normal;\n  font-style: normal;\n}\n.font-awesome_fa_2otTb {\n  display: inline-block;\n  font: normal normal normal 14px/1 FontAwesome;\n  font-size: inherit;\n  text-rendering: auto;\n  -webkit-font-smoothing: antialiased;\n  -moz-osx-font-smoothing: grayscale;\n}\n/* makes the font 33% larger relative to the icon container */\n.font-awesome_fa-lg_2-2uP {\n  font-size: 1.33333333em;\n  line-height: 0.75em;\n  vertical-align: -15%;\n}\n.font-awesome_fa-2x_2Mgjx {\n  font-size: 2em;\n}\n.font-awesome_fa-3x_1gdsS {\n  font-size: 3em;\n}\n.font-awesome_fa-4x_2VkGW {\n  font-size: 4em;\n}\n.font-awesome_fa-5x_QKikc {\n  font-size: 5em;\n}\n.font-awesome_fa-fw_1FdA5 {\n  width: 1.28571429em;\n  text-align: center;\n}\n.font-awesome_fa-ul_2XTDQ {\n  padding-left: 0;\n  margin-left: 2.14285714em;\n  list-style-type: none;\n}\n.font-awesome_fa-ul_2XTDQ > li {\n  position: relative;\n}\n.font-awesome_fa-li_1vepp {\n  position: absolute;\n  left: -2.14285714em;\n  width: 2.14285714em;\n  top: 0.14285714em;\n  text-align: center;\n}\n.font-awesome_fa-li_1vepp.font-awesome_fa-lg_2-2uP {\n  left: -1.85714286em;\n}\n.font-awesome_fa-border_6EUMg {\n  padding: .2em .25em .15em;\n  border: solid 0.08em #eeeeee;\n  border-radius: .1em;\n}\n.font-awesome_fa-pull-left_3jHfw {\n  float: left;\n}\n.font-awesome_fa-pull-right_20ZAt {\n  float: right;\n}\n.font-awesome_fa_2otTb.font-awesome_fa-pull-left_3jHfw {\n  margin-right: .3em;\n}\n.font-awesome_fa_2otTb.font-awesome_fa-pull-right_20ZAt {\n  margin-left: .3em;\n}\n/* Deprecated as of 4.4.0 */\n.font-awesome_pull-right_1Mb60 {\n  float: right;\n}\n.font-awesome_pull-left_30vXl {\n  float: left;\n}\n.font-awesome_fa_2otTb.font-awesome_pull-left_30vXl {\n  margin-right: .3em;\n}\n.font-awesome_fa_2otTb.font-awesome_pull-right_1Mb60 {\n  margin-left: .3em;\n}\n.font-awesome_fa-spin_NsqCr {\n  animation: font-awesome_fa-spin_NsqCr 2s infinite linear;\n}\n.font-awesome_fa-pulse_1Vv2f {\n  animation: font-awesome_fa-spin_NsqCr 1s infinite steps(8);\n}\n@keyframes font-awesome_fa-spin_NsqCr {\n  0% {\n    transform: rotate(0deg);\n  }\n  100% {\n    transform: rotate(359deg);\n  }\n}\n.font-awesome_fa-rotate-90_1snKw {\n  -ms-filter: \"progid:DXImageTransform.Microsoft.BasicImage(rotation=1)\";\n  transform: rotate(90deg);\n}\n.font-awesome_fa-rotate-180_2hMM8 {\n  -ms-filter: \"progid:DXImageTransform.Microsoft.BasicImage(rotation=2)\";\n  transform: rotate(180deg);\n}\n.font-awesome_fa-rotate-270_3eBDG {\n  -ms-filter: \"progid:DXImageTransform.Microsoft.BasicImage(rotation=3)\";\n  transform: rotate(270deg);\n}\n.font-awesome_fa-flip-horizontal_33SUC {\n  -ms-filter: \"progid:DXImageTransform.Microsoft.BasicImage(rotation=0, mirror=1)\";\n  transform: scale(-1, 1);\n}\n.font-awesome_fa-flip-vertical_klOOu {\n  -ms-filter: \"progid:DXImageTransform.Microsoft.BasicImage(rotation=2, mirror=1)\";\n  transform: scale(1, -1);\n}\n:root .font-awesome_fa-rotate-90_1snKw,\n:root .font-awesome_fa-rotate-180_2hMM8,\n:root .font-awesome_fa-rotate-270_3eBDG,\n:root .font-awesome_fa-flip-horizontal_33SUC,\n:root .font-awesome_fa-flip-vertical_klOOu {\n  filter: none;\n}\n.font-awesome_fa-stack_3fqsM {\n  position: relative;\n  display: inline-block;\n  width: 2em;\n  height: 2em;\n  line-height: 2em;\n  vertical-align: middle;\n}\n.font-awesome_fa-stack-1x_14Vb0,\n.font-awesome_fa-stack-2x_It5yP {\n  position: absolute;\n  left: 0;\n  width: 100%;\n  text-align: center;\n}\n.font-awesome_fa-stack-1x_14Vb0 {\n  line-height: inherit;\n}\n.font-awesome_fa-stack-2x_It5yP {\n  font-size: 2em;\n}\n.font-awesome_fa-inverse_1e1EX {\n  color: #ffffff;\n}\n/* Font Awesome uses the Unicode Private Use Area (PUA) to ensure screen\n   readers do not read off random characters that represent icons */\n.font-awesome_fa-glass_34uit:before {\n  content: \"\\F000\";\n}\n.font-awesome_fa-music_3f2s5:before {\n  content: \"\\F001\";\n}\n.font-awesome_fa-search_1C7GK:before {\n  content: \"\\F002\";\n}\n.font-awesome_fa-envelope-o_3EWEI:before {\n  content: \"\\F003\";\n}\n.font-awesome_fa-heart_3DHYT:before {\n  content: \"\\F004\";\n}\n.font-awesome_fa-star_2g4Ye:before {\n  content: \"\\F005\";\n}\n.font-awesome_fa-star-o_3ty_o:before {\n  content: \"\\F006\";\n}\n.font-awesome_fa-user_N3puO:before {\n  content: \"\\F007\";\n}\n.font-awesome_fa-film_2qmKe:before {\n  content: \"\\F008\";\n}\n.font-awesome_fa-th-large_2N4P8:before {\n  content: \"\\F009\";\n}\n.font-awesome_fa-th_3f0mR:before {\n  content: \"\\F00A\";\n}\n.font-awesome_fa-th-list_3qelJ:before {\n  content: \"\\F00B\";\n}\n.font-awesome_fa-check_3DXVm:before {\n  content: \"\\F00C\";\n}\n.font-awesome_fa-remove_1MIYz:before,\n.font-awesome_fa-close_1zysR:before,\n.font-awesome_fa-times_1Y-Cs:before {\n  content: \"\\F00D\";\n}\n.font-awesome_fa-search-plus_9OGuc:before {\n  content: \"\\F00E\";\n}\n.font-awesome_fa-search-minus_1j_Aj:before {\n  content: \"\\F010\";\n}\n.font-awesome_fa-power-off_vPefe:before {\n  content: \"\\F011\";\n}\n.font-awesome_fa-signal_1VxWh:before {\n  content: \"\\F012\";\n}\n.font-awesome_fa-gear_yfzjv:before,\n.font-awesome_fa-cog_30mdw:before {\n  content: \"\\F013\";\n}\n.font-awesome_fa-trash-o_2uFKh:before {\n  content: \"\\F014\";\n}\n.font-awesome_fa-home_3Fr6e:before {\n  content: \"\\F015\";\n}\n.font-awesome_fa-file-o_208AJ:before {\n  content: \"\\F016\";\n}\n.font-awesome_fa-clock-o_3vfig:before {\n  content: \"\\F017\";\n}\n.font-awesome_fa-road_2017v:before {\n  content: \"\\F018\";\n}\n.font-awesome_fa-download_1TDS9:before {\n  content: \"\\F019\";\n}\n.font-awesome_fa-arrow-circle-o-down_2M97h:before {\n  content: \"\\F01A\";\n}\n.font-awesome_fa-arrow-circle-o-up_2aqY-:before {\n  content: \"\\F01B\";\n}\n.font-awesome_fa-inbox_3bWnM:before {\n  content: \"\\F01C\";\n}\n.font-awesome_fa-play-circle-o_3vU6r:before {\n  content: \"\\F01D\";\n}\n.font-awesome_fa-rotate-right_1E_3J:before,\n.font-awesome_fa-repeat_27E0b:before {\n  content: \"\\F01E\";\n}\n.font-awesome_fa-refresh_2AOlD:before {\n  content: \"\\F021\";\n}\n.font-awesome_fa-list-alt_3nS4v:before {\n  content: \"\\F022\";\n}\n.font-awesome_fa-lock_inyGT:before {\n  content: \"\\F023\";\n}\n.font-awesome_fa-flag_1qWlx:before {\n  content: \"\\F024\";\n}\n.font-awesome_fa-headphones_13olw:before {\n  content: \"\\F025\";\n}\n.font-awesome_fa-volume-off_1llC2:before {\n  content: \"\\F026\";\n}\n.font-awesome_fa-volume-down_1jTgZ:before {\n  content: \"\\F027\";\n}\n.font-awesome_fa-volume-up_2XIXx:before {\n  content: \"\\F028\";\n}\n.font-awesome_fa-qrcode_17ZaI:before {\n  content: \"\\F029\";\n}\n.font-awesome_fa-barcode_1al4-:before {\n  content: \"\\F02A\";\n}\n.font-awesome_fa-tag_2CMhy:before {\n  content: \"\\F02B\";\n}\n.font-awesome_fa-tags_3kYb4:before {\n  content: \"\\F02C\";\n}\n.font-awesome_fa-book_32JVT:before {\n  content: \"\\F02D\";\n}\n.font-awesome_fa-bookmark_1s2Fl:before {\n  content: \"\\F02E\";\n}\n.font-awesome_fa-print_y2Ezw:before {\n  content: \"\\F02F\";\n}\n.font-awesome_fa-camera_Ls8dv:before {\n  content: \"\\F030\";\n}\n.font-awesome_fa-font_1VH0X:before {\n  content: \"\\F031\";\n}\n.font-awesome_fa-bold_11qyx:before {\n  content: \"\\F032\";\n}\n.font-awesome_fa-italic_1Gtc3:before {\n  content: \"\\F033\";\n}\n.font-awesome_fa-text-height_3db67:before {\n  content: \"\\F034\";\n}\n.font-awesome_fa-text-width_2yBeb:before {\n  content: \"\\F035\";\n}\n.font-awesome_fa-align-left_3DuVK:before {\n  content: \"\\F036\";\n}\n.font-awesome_fa-align-center_M9xyY:before {\n  content: \"\\F037\";\n}\n.font-awesome_fa-align-right_3Icru:before {\n  content: \"\\F038\";\n}\n.font-awesome_fa-align-justify_maoNA:before {\n  content: \"\\F039\";\n}\n.font-awesome_fa-list_3CT1m:before {\n  content: \"\\F03A\";\n}\n.font-awesome_fa-dedent_3p5N-:before,\n.font-awesome_fa-outdent_34S6p:before {\n  content: \"\\F03B\";\n}\n.font-awesome_fa-indent_2Y6xl:before {\n  content: \"\\F03C\";\n}\n.font-awesome_fa-video-camera_2Tfna:before {\n  content: \"\\F03D\";\n}\n.font-awesome_fa-photo_2f_lI:before,\n.font-awesome_fa-image_l6mTT:before,\n.font-awesome_fa-picture-o_3srts:before {\n  content: \"\\F03E\";\n}\n.font-awesome_fa-pencil_3o0Fh:before {\n  content: \"\\F040\";\n}\n.font-awesome_fa-map-marker_1Lc7q:before {\n  content: \"\\F041\";\n}\n.font-awesome_fa-adjust_1uk96:before {\n  content: \"\\F042\";\n}\n.font-awesome_fa-tint_23wIx:before {\n  content: \"\\F043\";\n}\n.font-awesome_fa-edit_2ITK3:before,\n.font-awesome_fa-pencil-square-o_1jFx_:before {\n  content: \"\\F044\";\n}\n.font-awesome_fa-share-square-o_1bC_y:before {\n  content: \"\\F045\";\n}\n.font-awesome_fa-check-square-o_2eIdJ:before {\n  content: \"\\F046\";\n}\n.font-awesome_fa-arrows_2tovc:before {\n  content: \"\\F047\";\n}\n.font-awesome_fa-step-backward_1aJ3J:before {\n  content: \"\\F048\";\n}\n.font-awesome_fa-fast-backward_rW1JQ:before {\n  content: \"\\F049\";\n}\n.font-awesome_fa-backward_WWKjR:before {\n  content: \"\\F04A\";\n}\n.font-awesome_fa-play_1vQTN:before {\n  content: \"\\F04B\";\n}\n.font-awesome_fa-pause_33hRm:before {\n  content: \"\\F04C\";\n}\n.font-awesome_fa-stop_1l1v_:before {\n  content: \"\\F04D\";\n}\n.font-awesome_fa-forward_3jJNW:before {\n  content: \"\\F04E\";\n}\n.font-awesome_fa-fast-forward_2JwE2:before {\n  content: \"\\F050\";\n}\n.font-awesome_fa-step-forward_3NyZe:before {\n  content: \"\\F051\";\n}\n.font-awesome_fa-eject_GA_Jm:before {\n  content: \"\\F052\";\n}\n.font-awesome_fa-chevron-left_1EwAm:before {\n  content: \"\\F053\";\n}\n.font-awesome_fa-chevron-right_3RxN_:before {\n  content: \"\\F054\";\n}\n.font-awesome_fa-plus-circle_1SAMg:before {\n  content: \"\\F055\";\n}\n.font-awesome_fa-minus-circle_Th8wG:before {\n  content: \"\\F056\";\n}\n.font-awesome_fa-times-circle_JfW7D:before {\n  content: \"\\F057\";\n}\n.font-awesome_fa-check-circle_PBRDH:before {\n  content: \"\\F058\";\n}\n.font-awesome_fa-question-circle_33ykP:before {\n  content: \"\\F059\";\n}\n.font-awesome_fa-info-circle_2ZdWr:before {\n  content: \"\\F05A\";\n}\n.font-awesome_fa-crosshairs_18oYo:before {\n  content: \"\\F05B\";\n}\n.font-awesome_fa-times-circle-o_3LLKK:before {\n  content: \"\\F05C\";\n}\n.font-awesome_fa-check-circle-o_1CW2E:before {\n  content: \"\\F05D\";\n}\n.font-awesome_fa-ban_1LOdy:before {\n  content: \"\\F05E\";\n}\n.font-awesome_fa-arrow-left_2G_P0:before {\n  content: \"\\F060\";\n}\n.font-awesome_fa-arrow-right_15DTA:before {\n  content: \"\\F061\";\n}\n.font-awesome_fa-arrow-up_12gpU:before {\n  content: \"\\F062\";\n}\n.font-awesome_fa-arrow-down_2zqoH:before {\n  content: \"\\F063\";\n}\n.font-awesome_fa-mail-forward_2tQrw:before,\n.font-awesome_fa-share_3O8Dc:before {\n  content: \"\\F064\";\n}\n.font-awesome_fa-expand_34Ihf:before {\n  content: \"\\F065\";\n}\n.font-awesome_fa-compress_1JDdS:before {\n  content: \"\\F066\";\n}\n.font-awesome_fa-plus_WEb-k:before {\n  content: \"\\F067\";\n}\n.font-awesome_fa-minus_1WAd4:before {\n  content: \"\\F068\";\n}\n.font-awesome_fa-asterisk_w7w6r:before {\n  content: \"\\F069\";\n}\n.font-awesome_fa-exclamation-circle_2bbrU:before {\n  content: \"\\F06A\";\n}\n.font-awesome_fa-gift_3bKvI:before {\n  content: \"\\F06B\";\n}\n.font-awesome_fa-leaf_2B5Uf:before {\n  content: \"\\F06C\";\n}\n.font-awesome_fa-fire_1qsDr:before {\n  content: \"\\F06D\";\n}\n.font-awesome_fa-eye_3XRn0:before {\n  content: \"\\F06E\";\n}\n.font-awesome_fa-eye-slash_18NEx:before {\n  content: \"\\F070\";\n}\n.font-awesome_fa-warning_32nGg:before,\n.font-awesome_fa-exclamation-triangle_ttuT-:before {\n  content: \"\\F071\";\n}\n.font-awesome_fa-plane_3L5mD:before {\n  content: \"\\F072\";\n}\n.font-awesome_fa-calendar_1niuw:before {\n  content: \"\\F073\";\n}\n.font-awesome_fa-random_2RH42:before {\n  content: \"\\F074\";\n}\n.font-awesome_fa-comment_2koYW:before {\n  content: \"\\F075\";\n}\n.font-awesome_fa-magnet_33k7m:before {\n  content: \"\\F076\";\n}\n.font-awesome_fa-chevron-up_2R5R_:before {\n  content: \"\\F077\";\n}\n.font-awesome_fa-chevron-down_746nC:before {\n  content: \"\\F078\";\n}\n.font-awesome_fa-retweet_2ma5b:before {\n  content: \"\\F079\";\n}\n.font-awesome_fa-shopping-cart_276KU:before {\n  content: \"\\F07A\";\n}\n.font-awesome_fa-folder_2MMW6:before {\n  content: \"\\F07B\";\n}\n.font-awesome_fa-folder-open_1a3bX:before {\n  content: \"\\F07C\";\n}\n.font-awesome_fa-arrows-v_27J04:before {\n  content: \"\\F07D\";\n}\n.font-awesome_fa-arrows-h_3EAQ6:before {\n  content: \"\\F07E\";\n}\n.font-awesome_fa-bar-chart-o_BMSPQ:before,\n.font-awesome_fa-bar-chart_3LGib:before {\n  content: \"\\F080\";\n}\n.font-awesome_fa-twitter-square_146CY:before {\n  content: \"\\F081\";\n}\n.font-awesome_fa-facebook-square_3IbRT:before {\n  content: \"\\F082\";\n}\n.font-awesome_fa-camera-retro_oM_mn:before {\n  content: \"\\F083\";\n}\n.font-awesome_fa-key_3bV7M:before {\n  content: \"\\F084\";\n}\n.font-awesome_fa-gears_3cjY1:before,\n.font-awesome_fa-cogs_CqXH5:before {\n  content: \"\\F085\";\n}\n.font-awesome_fa-comments_2lUtO:before {\n  content: \"\\F086\";\n}\n.font-awesome_fa-thumbs-o-up_3cD9j:before {\n  content: \"\\F087\";\n}\n.font-awesome_fa-thumbs-o-down_3AeCO:before {\n  content: \"\\F088\";\n}\n.font-awesome_fa-star-half_2zxdp:before {\n  content: \"\\F089\";\n}\n.font-awesome_fa-heart-o_QI-Zl:before {\n  content: \"\\F08A\";\n}\n.font-awesome_fa-sign-out_2IOU5:before {\n  content: \"\\F08B\";\n}\n.font-awesome_fa-linkedin-square_3HkV4:before {\n  content: \"\\F08C\";\n}\n.font-awesome_fa-thumb-tack_2gcw0:before {\n  content: \"\\F08D\";\n}\n.font-awesome_fa-external-link_1ku_O:before {\n  content: \"\\F08E\";\n}\n.font-awesome_fa-sign-in_1MYT-:before {\n  content: \"\\F090\";\n}\n.font-awesome_fa-trophy_3CyBM:before {\n  content: \"\\F091\";\n}\n.font-awesome_fa-github-square_1xm6W:before {\n  content: \"\\F092\";\n}\n.font-awesome_fa-upload_wVRel:before {\n  content: \"\\F093\";\n}\n.font-awesome_fa-lemon-o_2v3hR:before {\n  content: \"\\F094\";\n}\n.font-awesome_fa-phone_1EiFR:before {\n  content: \"\\F095\";\n}\n.font-awesome_fa-square-o_WbQ8x:before {\n  content: \"\\F096\";\n}\n.font-awesome_fa-bookmark-o_1R5xe:before {\n  content: \"\\F097\";\n}\n.font-awesome_fa-phone-square_3GkD1:before {\n  content: \"\\F098\";\n}\n.font-awesome_fa-twitter_cyUBg:before {\n  content: \"\\F099\";\n}\n.font-awesome_fa-facebook-f_3r4VF:before,\n.font-awesome_fa-facebook_f3EUw:before {\n  content: \"\\F09A\";\n}\n.font-awesome_fa-github_MdgBC:before {\n  content: \"\\F09B\";\n}\n.font-awesome_fa-unlock_XTSXp:before {\n  content: \"\\F09C\";\n}\n.font-awesome_fa-credit-card_28S4q:before {\n  content: \"\\F09D\";\n}\n.font-awesome_fa-feed_3tLbf:before,\n.font-awesome_fa-rss_3_EzS:before {\n  content: \"\\F09E\";\n}\n.font-awesome_fa-hdd-o_3ZoO6:before {\n  content: \"\\F0A0\";\n}\n.font-awesome_fa-bullhorn_3o7hz:before {\n  content: \"\\F0A1\";\n}\n.font-awesome_fa-bell_26AZW:before {\n  content: \"\\F0F3\";\n}\n.font-awesome_fa-certificate_11sLt:before {\n  content: \"\\F0A3\";\n}\n.font-awesome_fa-hand-o-right_2G1w_:before {\n  content: \"\\F0A4\";\n}\n.font-awesome_fa-hand-o-left_2KTOL:before {\n  content: \"\\F0A5\";\n}\n.font-awesome_fa-hand-o-up_3xrkS:before {\n  content: \"\\F0A6\";\n}\n.font-awesome_fa-hand-o-down_3cWAN:before {\n  content: \"\\F0A7\";\n}\n.font-awesome_fa-arrow-circle-left_2CgFw:before {\n  content: \"\\F0A8\";\n}\n.font-awesome_fa-arrow-circle-right_35XcE:before {\n  content: \"\\F0A9\";\n}\n.font-awesome_fa-arrow-circle-up_FHcwE:before {\n  content: \"\\F0AA\";\n}\n.font-awesome_fa-arrow-circle-down_1NJKi:before {\n  content: \"\\F0AB\";\n}\n.font-awesome_fa-globe_2fYFX:before {\n  content: \"\\F0AC\";\n}\n.font-awesome_fa-wrench_3snDo:before {\n  content: \"\\F0AD\";\n}\n.font-awesome_fa-tasks_2_oS8:before {\n  content: \"\\F0AE\";\n}\n.font-awesome_fa-filter_1q5k8:before {\n  content: \"\\F0B0\";\n}\n.font-awesome_fa-briefcase_aikwY:before {\n  content: \"\\F0B1\";\n}\n.font-awesome_fa-arrows-alt_1vqY9:before {\n  content: \"\\F0B2\";\n}\n.font-awesome_fa-group_XbMo9:before,\n.font-awesome_fa-users_1PfY8:before {\n  content: \"\\F0C0\";\n}\n.font-awesome_fa-chain_2QCgS:before,\n.font-awesome_fa-link_3kFkN:before {\n  content: \"\\F0C1\";\n}\n.font-awesome_fa-cloud_2l8rd:before {\n  content: \"\\F0C2\";\n}\n.font-awesome_fa-flask_3iTak:before {\n  content: \"\\F0C3\";\n}\n.font-awesome_fa-cut_17wpt:before,\n.font-awesome_fa-scissors_1xAHX:before {\n  content: \"\\F0C4\";\n}\n.font-awesome_fa-copy_a2GP3:before,\n.font-awesome_fa-files-o_2pUmI:before {\n  content: \"\\F0C5\";\n}\n.font-awesome_fa-paperclip_d4foW:before {\n  content: \"\\F0C6\";\n}\n.font-awesome_fa-save_10fTV:before,\n.font-awesome_fa-floppy-o_1MBo6:before {\n  content: \"\\F0C7\";\n}\n.font-awesome_fa-square_N1IJZ:before {\n  content: \"\\F0C8\";\n}\n.font-awesome_fa-navicon_3anpJ:before,\n.font-awesome_fa-reorder_2ukY7:before,\n.font-awesome_fa-bars_3WARK:before {\n  content: \"\\F0C9\";\n}\n.font-awesome_fa-list-ul_3s6_2:before {\n  content: \"\\F0CA\";\n}\n.font-awesome_fa-list-ol_AP-DO:before {\n  content: \"\\F0CB\";\n}\n.font-awesome_fa-strikethrough_h0-a_:before {\n  content: \"\\F0CC\";\n}\n.font-awesome_fa-underline_2PIFp:before {\n  content: \"\\F0CD\";\n}\n.font-awesome_fa-table_2mEeT:before {\n  content: \"\\F0CE\";\n}\n.font-awesome_fa-magic_qWQg_:before {\n  content: \"\\F0D0\";\n}\n.font-awesome_fa-truck_1AsFs:before {\n  content: \"\\F0D1\";\n}\n.font-awesome_fa-pinterest_1xKnl:before {\n  content: \"\\F0D2\";\n}\n.font-awesome_fa-pinterest-square_3Yhwf:before {\n  content: \"\\F0D3\";\n}\n.font-awesome_fa-google-plus-square_90VGD:before {\n  content: \"\\F0D4\";\n}\n.font-awesome_fa-google-plus_1Tp-z:before {\n  content: \"\\F0D5\";\n}\n.font-awesome_fa-money_32Lir:before {\n  content: \"\\F0D6\";\n}\n.font-awesome_fa-caret-down_1crEO:before {\n  content: \"\\F0D7\";\n}\n.font-awesome_fa-caret-up_2TwZv:before {\n  content: \"\\F0D8\";\n}\n.font-awesome_fa-caret-left_39lOf:before {\n  content: \"\\F0D9\";\n}\n.font-awesome_fa-caret-right_3p0nW:before {\n  content: \"\\F0DA\";\n}\n.font-awesome_fa-columns_nToc3:before {\n  content: \"\\F0DB\";\n}\n.font-awesome_fa-unsorted_2nhbR:before,\n.font-awesome_fa-sort_F3dcY:before {\n  content: \"\\F0DC\";\n}\n.font-awesome_fa-sort-down_3wTbK:before,\n.font-awesome_fa-sort-desc_3CQ5e:before {\n  content: \"\\F0DD\";\n}\n.font-awesome_fa-sort-up_Ad_bv:before,\n.font-awesome_fa-sort-asc_3MlT5:before {\n  content: \"\\F0DE\";\n}\n.font-awesome_fa-envelope_3xnLD:before {\n  content: \"\\F0E0\";\n}\n.font-awesome_fa-linkedin_25eMJ:before {\n  content: \"\\F0E1\";\n}\n.font-awesome_fa-rotate-left_3mzU5:before,\n.font-awesome_fa-undo_hNldt:before {\n  content: \"\\F0E2\";\n}\n.font-awesome_fa-legal_1C_3g:before,\n.font-awesome_fa-gavel_2ttLP:before {\n  content: \"\\F0E3\";\n}\n.font-awesome_fa-dashboard_3bEM7:before,\n.font-awesome_fa-tachometer_3R5zx:before {\n  content: \"\\F0E4\";\n}\n.font-awesome_fa-comment-o_2pEPg:before {\n  content: \"\\F0E5\";\n}\n.font-awesome_fa-comments-o_hQJKS:before {\n  content: \"\\F0E6\";\n}\n.font-awesome_fa-flash_1DU_v:before,\n.font-awesome_fa-bolt_3iT3l:before {\n  content: \"\\F0E7\";\n}\n.font-awesome_fa-sitemap_QKmtm:before {\n  content: \"\\F0E8\";\n}\n.font-awesome_fa-umbrella_3fE2k:before {\n  content: \"\\F0E9\";\n}\n.font-awesome_fa-paste_3RUtK:before,\n.font-awesome_fa-clipboard_1Wx9E:before {\n  content: \"\\F0EA\";\n}\n.font-awesome_fa-lightbulb-o_3MZxy:before {\n  content: \"\\F0EB\";\n}\n.font-awesome_fa-exchange_1cgNj:before {\n  content: \"\\F0EC\";\n}\n.font-awesome_fa-cloud-download_2fd-7:before {\n  content: \"\\F0ED\";\n}\n.font-awesome_fa-cloud-upload_BCKnV:before {\n  content: \"\\F0EE\";\n}\n.font-awesome_fa-user-md_3Unw6:before {\n  content: \"\\F0F0\";\n}\n.font-awesome_fa-stethoscope_3TPjy:before {\n  content: \"\\F0F1\";\n}\n.font-awesome_fa-suitcase_2ZK-F:before {\n  content: \"\\F0F2\";\n}\n.font-awesome_fa-bell-o_3iuFm:before {\n  content: \"\\F0A2\";\n}\n.font-awesome_fa-coffee_2tZxb:before {\n  content: \"\\F0F4\";\n}\n.font-awesome_fa-cutlery_2dZZ2:before {\n  content: \"\\F0F5\";\n}\n.font-awesome_fa-file-text-o_3vkBr:before {\n  content: \"\\F0F6\";\n}\n.font-awesome_fa-building-o_1ML8l:before {\n  content: \"\\F0F7\";\n}\n.font-awesome_fa-hospital-o_2dZPM:before {\n  content: \"\\F0F8\";\n}\n.font-awesome_fa-ambulance_3oMTO:before {\n  content: \"\\F0F9\";\n}\n.font-awesome_fa-medkit_3TuAD:before {\n  content: \"\\F0FA\";\n}\n.font-awesome_fa-fighter-jet_2EPG4:before {\n  content: \"\\F0FB\";\n}\n.font-awesome_fa-beer_25HMG:before {\n  content: \"\\F0FC\";\n}\n.font-awesome_fa-h-square_iRMP3:before {\n  content: \"\\F0FD\";\n}\n.font-awesome_fa-plus-square_28zW8:before {\n  content: \"\\F0FE\";\n}\n.font-awesome_fa-angle-double-left_3Q7bL:before {\n  content: \"\\F100\";\n}\n.font-awesome_fa-angle-double-right_2R24L:before {\n  content: \"\\F101\";\n}\n.font-awesome_fa-angle-double-up_2GMJK:before {\n  content: \"\\F102\";\n}\n.font-awesome_fa-angle-double-down_IlK-a:before {\n  content: \"\\F103\";\n}\n.font-awesome_fa-angle-left_7b-ty:before {\n  content: \"\\F104\";\n}\n.font-awesome_fa-angle-right_RfvDx:before {\n  content: \"\\F105\";\n}\n.font-awesome_fa-angle-up_2xGkU:before {\n  content: \"\\F106\";\n}\n.font-awesome_fa-angle-down_3nIhI:before {\n  content: \"\\F107\";\n}\n.font-awesome_fa-desktop_7pHFF:before {\n  content: \"\\F108\";\n}\n.font-awesome_fa-laptop_2QHxL:before {\n  content: \"\\F109\";\n}\n.font-awesome_fa-tablet_eRAwh:before {\n  content: \"\\F10A\";\n}\n.font-awesome_fa-mobile-phone_3tGZx:before,\n.font-awesome_fa-mobile_ry_56:before {\n  content: \"\\F10B\";\n}\n.font-awesome_fa-circle-o_We1QB:before {\n  content: \"\\F10C\";\n}\n.font-awesome_fa-quote-left_tgvF3:before {\n  content: \"\\F10D\";\n}\n.font-awesome_fa-quote-right_2LbYu:before {\n  content: \"\\F10E\";\n}\n.font-awesome_fa-spinner_1FgdF:before {\n  content: \"\\F110\";\n}\n.font-awesome_fa-circle_RFG4V:before {\n  content: \"\\F111\";\n}\n.font-awesome_fa-mail-reply_1ovuj:before,\n.font-awesome_fa-reply_1p4xy:before {\n  content: \"\\F112\";\n}\n.font-awesome_fa-github-alt_PGZGn:before {\n  content: \"\\F113\";\n}\n.font-awesome_fa-folder-o_28LsO:before {\n  content: \"\\F114\";\n}\n.font-awesome_fa-folder-open-o_3Hbbz:before {\n  content: \"\\F115\";\n}\n.font-awesome_fa-smile-o_3R1KH:before {\n  content: \"\\F118\";\n}\n.font-awesome_fa-frown-o_1PJe6:before {\n  content: \"\\F119\";\n}\n.font-awesome_fa-meh-o_1Yal3:before {\n  content: \"\\F11A\";\n}\n.font-awesome_fa-gamepad_DQkX5:before {\n  content: \"\\F11B\";\n}\n.font-awesome_fa-keyboard-o_1Zegg:before {\n  content: \"\\F11C\";\n}\n.font-awesome_fa-flag-o_2paT4:before {\n  content: \"\\F11D\";\n}\n.font-awesome_fa-flag-checkered_3Q50W:before {\n  content: \"\\F11E\";\n}\n.font-awesome_fa-terminal_1y_ce:before {\n  content: \"\\F120\";\n}\n.font-awesome_fa-code_373HL:before {\n  content: \"\\F121\";\n}\n.font-awesome_fa-mail-reply-all_1el1h:before,\n.font-awesome_fa-reply-all_1XbQQ:before {\n  content: \"\\F122\";\n}\n.font-awesome_fa-star-half-empty_NeM4g:before,\n.font-awesome_fa-star-half-full_3_GnR:before,\n.font-awesome_fa-star-half-o_1gMSG:before {\n  content: \"\\F123\";\n}\n.font-awesome_fa-location-arrow_gFy0a:before {\n  content: \"\\F124\";\n}\n.font-awesome_fa-crop_DFePA:before {\n  content: \"\\F125\";\n}\n.font-awesome_fa-code-fork_rNRd0:before {\n  content: \"\\F126\";\n}\n.font-awesome_fa-unlink_1hw62:before,\n.font-awesome_fa-chain-broken_3nVk7:before {\n  content: \"\\F127\";\n}\n.font-awesome_fa-question_EAoIA:before {\n  content: \"\\F128\";\n}\n.font-awesome_fa-info_2cQvQ:before {\n  content: \"\\F129\";\n}\n.font-awesome_fa-exclamation_297uN:before {\n  content: \"\\F12A\";\n}\n.font-awesome_fa-superscript_N7aMl:before {\n  content: \"\\F12B\";\n}\n.font-awesome_fa-subscript_ZG4gQ:before {\n  content: \"\\F12C\";\n}\n.font-awesome_fa-eraser_3NIuU:before {\n  content: \"\\F12D\";\n}\n.font-awesome_fa-puzzle-piece_3lKWq:before {\n  content: \"\\F12E\";\n}\n.font-awesome_fa-microphone_3_81_:before {\n  content: \"\\F130\";\n}\n.font-awesome_fa-microphone-slash_1DyxC:before {\n  content: \"\\F131\";\n}\n.font-awesome_fa-shield_1qKif:before {\n  content: \"\\F132\";\n}\n.font-awesome_fa-calendar-o_1BLCm:before {\n  content: \"\\F133\";\n}\n.font-awesome_fa-fire-extinguisher_3gz5K:before {\n  content: \"\\F134\";\n}\n.font-awesome_fa-rocket_lfSov:before {\n  content: \"\\F135\";\n}\n.font-awesome_fa-maxcdn_cD6Fn:before {\n  content: \"\\F136\";\n}\n.font-awesome_fa-chevron-circle-left_1aac7:before {\n  content: \"\\F137\";\n}\n.font-awesome_fa-chevron-circle-right_Evj_u:before {\n  content: \"\\F138\";\n}\n.font-awesome_fa-chevron-circle-up_tTcaI:before {\n  content: \"\\F139\";\n}\n.font-awesome_fa-chevron-circle-down_1oKtm:before {\n  content: \"\\F13A\";\n}\n.font-awesome_fa-html5_3LZaq:before {\n  content: \"\\F13B\";\n}\n.font-awesome_fa-css3_3hg4c:before {\n  content: \"\\F13C\";\n}\n.font-awesome_fa-anchor_2-wZ3:before {\n  content: \"\\F13D\";\n}\n.font-awesome_fa-unlock-alt_CLyLU:before {\n  content: \"\\F13E\";\n}\n.font-awesome_fa-bullseye_6Sp1E:before {\n  content: \"\\F140\";\n}\n.font-awesome_fa-ellipsis-h_4VBiE:before {\n  content: \"\\F141\";\n}\n.font-awesome_fa-ellipsis-v_Ktjfe:before {\n  content: \"\\F142\";\n}\n.font-awesome_fa-rss-square_4Vj2y:before {\n  content: \"\\F143\";\n}\n.font-awesome_fa-play-circle_ECzau:before {\n  content: \"\\F144\";\n}\n.font-awesome_fa-ticket_284VQ:before {\n  content: \"\\F145\";\n}\n.font-awesome_fa-minus-square_3w_Do:before {\n  content: \"\\F146\";\n}\n.font-awesome_fa-minus-square-o_qe1Jq:before {\n  content: \"\\F147\";\n}\n.font-awesome_fa-level-up_7RnC1:before {\n  content: \"\\F148\";\n}\n.font-awesome_fa-level-down_1rR4Q:before {\n  content: \"\\F149\";\n}\n.font-awesome_fa-check-square_3Qxfb:before {\n  content: \"\\F14A\";\n}\n.font-awesome_fa-pencil-square_3f_4W:before {\n  content: \"\\F14B\";\n}\n.font-awesome_fa-external-link-square_3TfmM:before {\n  content: \"\\F14C\";\n}\n.font-awesome_fa-share-square_4XEPu:before {\n  content: \"\\F14D\";\n}\n.font-awesome_fa-compass_3kP2n:before {\n  content: \"\\F14E\";\n}\n.font-awesome_fa-toggle-down_vVDIQ:before,\n.font-awesome_fa-caret-square-o-down_1Ao-B:before {\n  content: \"\\F150\";\n}\n.font-awesome_fa-toggle-up_1j96l:before,\n.font-awesome_fa-caret-square-o-up_1Lr5P:before {\n  content: \"\\F151\";\n}\n.font-awesome_fa-toggle-right_391jj:before,\n.font-awesome_fa-caret-square-o-right_Jc6ln:before {\n  content: \"\\F152\";\n}\n.font-awesome_fa-euro_1H752:before,\n.font-awesome_fa-eur_2JOH3:before {\n  content: \"\\F153\";\n}\n.font-awesome_fa-gbp_sXuSA:before {\n  content: \"\\F154\";\n}\n.font-awesome_fa-dollar_1Qw2b:before,\n.font-awesome_fa-usd_1Cyf0:before {\n  content: \"\\F155\";\n}\n.font-awesome_fa-rupee_3EdPr:before,\n.font-awesome_fa-inr_2v4ZE:before {\n  content: \"\\F156\";\n}\n.font-awesome_fa-cny_3RNlL:before,\n.font-awesome_fa-rmb_vAGyw:before,\n.font-awesome_fa-yen_UH2C8:before,\n.font-awesome_fa-jpy_CXaPK:before {\n  content: \"\\F157\";\n}\n.font-awesome_fa-ruble_1ms6_:before,\n.font-awesome_fa-rouble_fwC1R:before,\n.font-awesome_fa-rub_1c94U:before {\n  content: \"\\F158\";\n}\n.font-awesome_fa-won_1oqxL:before,\n.font-awesome_fa-krw_xc7hv:before {\n  content: \"\\F159\";\n}\n.font-awesome_fa-bitcoin_3h17C:before,\n.font-awesome_fa-btc_2EpsK:before {\n  content: \"\\F15A\";\n}\n.font-awesome_fa-file_2_TBG:before {\n  content: \"\\F15B\";\n}\n.font-awesome_fa-file-text_3uzzE:before {\n  content: \"\\F15C\";\n}\n.font-awesome_fa-sort-alpha-asc_l6x9i:before {\n  content: \"\\F15D\";\n}\n.font-awesome_fa-sort-alpha-desc_Au5Op:before {\n  content: \"\\F15E\";\n}\n.font-awesome_fa-sort-amount-asc_a4pl1:before {\n  content: \"\\F160\";\n}\n.font-awesome_fa-sort-amount-desc_sHYze:before {\n  content: \"\\F161\";\n}\n.font-awesome_fa-sort-numeric-asc_2fl5U:before {\n  content: \"\\F162\";\n}\n.font-awesome_fa-sort-numeric-desc_rZcNd:before {\n  content: \"\\F163\";\n}\n.font-awesome_fa-thumbs-up_32LEl:before {\n  content: \"\\F164\";\n}\n.font-awesome_fa-thumbs-down_115k7:before {\n  content: \"\\F165\";\n}\n.font-awesome_fa-youtube-square_1HADK:before {\n  content: \"\\F166\";\n}\n.font-awesome_fa-youtube_3PHGN:before {\n  content: \"\\F167\";\n}\n.font-awesome_fa-xing_2fXmL:before {\n  content: \"\\F168\";\n}\n.font-awesome_fa-xing-square_3AeWb:before {\n  content: \"\\F169\";\n}\n.font-awesome_fa-youtube-play__uWZW:before {\n  content: \"\\F16A\";\n}\n.font-awesome_fa-dropbox_1i2Rn:before {\n  content: \"\\F16B\";\n}\n.font-awesome_fa-stack-overflow_2tkuN:before {\n  content: \"\\F16C\";\n}\n.font-awesome_fa-instagram_1lV5f:before {\n  content: \"\\F16D\";\n}\n.font-awesome_fa-flickr_3JrtG:before {\n  content: \"\\F16E\";\n}\n.font-awesome_fa-adn_3a2Jf:before {\n  content: \"\\F170\";\n}\n.font-awesome_fa-bitbucket_12Rp4:before {\n  content: \"\\F171\";\n}\n.font-awesome_fa-bitbucket-square_Y0lMx:before {\n  content: \"\\F172\";\n}\n.font-awesome_fa-tumblr_18aB6:before {\n  content: \"\\F173\";\n}\n.font-awesome_fa-tumblr-square_3m4ld:before {\n  content: \"\\F174\";\n}\n.font-awesome_fa-long-arrow-down_2His0:before {\n  content: \"\\F175\";\n}\n.font-awesome_fa-long-arrow-up_vP_4l:before {\n  content: \"\\F176\";\n}\n.font-awesome_fa-long-arrow-left_1Uldc:before {\n  content: \"\\F177\";\n}\n.font-awesome_fa-long-arrow-right_1_jZV:before {\n  content: \"\\F178\";\n}\n.font-awesome_fa-apple_3f0-D:before {\n  content: \"\\F179\";\n}\n.font-awesome_fa-windows_2wDfa:before {\n  content: \"\\F17A\";\n}\n.font-awesome_fa-android_1Wzt9:before {\n  content: \"\\F17B\";\n}\n.font-awesome_fa-linux_3TBYa:before {\n  content: \"\\F17C\";\n}\n.font-awesome_fa-dribbble_IliEV:before {\n  content: \"\\F17D\";\n}\n.font-awesome_fa-skype_7ne23:before {\n  content: \"\\F17E\";\n}\n.font-awesome_fa-foursquare_52T_Z:before {\n  content: \"\\F180\";\n}\n.font-awesome_fa-trello_2ChtW:before {\n  content: \"\\F181\";\n}\n.font-awesome_fa-female_q-oMT:before {\n  content: \"\\F182\";\n}\n.font-awesome_fa-male_2PAqV:before {\n  content: \"\\F183\";\n}\n.font-awesome_fa-gittip_2fxKq:before,\n.font-awesome_fa-gratipay_xLz4x:before {\n  content: \"\\F184\";\n}\n.font-awesome_fa-sun-o_3QZ1O:before {\n  content: \"\\F185\";\n}\n.font-awesome_fa-moon-o_ZwK6C:before {\n  content: \"\\F186\";\n}\n.font-awesome_fa-archive_3FY1-:before {\n  content: \"\\F187\";\n}\n.font-awesome_fa-bug_20yJn:before {\n  content: \"\\F188\";\n}\n.font-awesome_fa-vk_1SLN3:before {\n  content: \"\\F189\";\n}\n.font-awesome_fa-weibo_3q9BS:before {\n  content: \"\\F18A\";\n}\n.font-awesome_fa-renren_27Rtg:before {\n  content: \"\\F18B\";\n}\n.font-awesome_fa-pagelines_3FZd_:before {\n  content: \"\\F18C\";\n}\n.font-awesome_fa-stack-exchange_1BbmA:before {\n  content: \"\\F18D\";\n}\n.font-awesome_fa-arrow-circle-o-right_1lS0I:before {\n  content: \"\\F18E\";\n}\n.font-awesome_fa-arrow-circle-o-left_270k0:before {\n  content: \"\\F190\";\n}\n.font-awesome_fa-toggle-left_q8rS1:before,\n.font-awesome_fa-caret-square-o-left_3leFq:before {\n  content: \"\\F191\";\n}\n.font-awesome_fa-dot-circle-o_fRUKP:before {\n  content: \"\\F192\";\n}\n.font-awesome_fa-wheelchair_2sPWn:before {\n  content: \"\\F193\";\n}\n.font-awesome_fa-vimeo-square_1nIhm:before {\n  content: \"\\F194\";\n}\n.font-awesome_fa-turkish-lira_1bCbG:before,\n.font-awesome_fa-try_1Olkg:before {\n  content: \"\\F195\";\n}\n.font-awesome_fa-plus-square-o_M6pBY:before {\n  content: \"\\F196\";\n}\n.font-awesome_fa-space-shuttle_9kmJU:before {\n  content: \"\\F197\";\n}\n.font-awesome_fa-slack_1EvN7:before {\n  content: \"\\F198\";\n}\n.font-awesome_fa-envelope-square_3aqlc:before {\n  content: \"\\F199\";\n}\n.font-awesome_fa-wordpress_2u9e0:before {\n  content: \"\\F19A\";\n}\n.font-awesome_fa-openid_2QLde:before {\n  content: \"\\F19B\";\n}\n.font-awesome_fa-institution_2uHKo:before,\n.font-awesome_fa-bank_D8hxY:before,\n.font-awesome_fa-university_3ECjv:before {\n  content: \"\\F19C\";\n}\n.font-awesome_fa-mortar-board_1em7v:before,\n.font-awesome_fa-graduation-cap_Y0mMc:before {\n  content: \"\\F19D\";\n}\n.font-awesome_fa-yahoo_33B-N:before {\n  content: \"\\F19E\";\n}\n.font-awesome_fa-google_1QYVJ:before {\n  content: \"\\F1A0\";\n}\n.font-awesome_fa-reddit_bwA4E:before {\n  content: \"\\F1A1\";\n}\n.font-awesome_fa-reddit-square_3rRiq:before {\n  content: \"\\F1A2\";\n}\n.font-awesome_fa-stumbleupon-circle_1TPid:before {\n  content: \"\\F1A3\";\n}\n.font-awesome_fa-stumbleupon_14d1U:before {\n  content: \"\\F1A4\";\n}\n.font-awesome_fa-delicious_3rkRQ:before {\n  content: \"\\F1A5\";\n}\n.font-awesome_fa-digg_3bIOw:before {\n  content: \"\\F1A6\";\n}\n.font-awesome_fa-pied-piper-pp_3j2RG:before {\n  content: \"\\F1A7\";\n}\n.font-awesome_fa-pied-piper-alt_3UjUa:before {\n  content: \"\\F1A8\";\n}\n.font-awesome_fa-drupal_WQObj:before {\n  content: \"\\F1A9\";\n}\n.font-awesome_fa-joomla_2UQVh:before {\n  content: \"\\F1AA\";\n}\n.font-awesome_fa-language_DOnO2:before {\n  content: \"\\F1AB\";\n}\n.font-awesome_fa-fax_1SV_d:before {\n  content: \"\\F1AC\";\n}\n.font-awesome_fa-building_1FVgz:before {\n  content: \"\\F1AD\";\n}\n.font-awesome_fa-child_2gTU4:before {\n  content: \"\\F1AE\";\n}\n.font-awesome_fa-paw_NcsFR:before {\n  content: \"\\F1B0\";\n}\n.font-awesome_fa-spoon_IxNyL:before {\n  content: \"\\F1B1\";\n}\n.font-awesome_fa-cube_1Mq1-:before {\n  content: \"\\F1B2\";\n}\n.font-awesome_fa-cubes_1tGnD:before {\n  content: \"\\F1B3\";\n}\n.font-awesome_fa-behance_3mdMe:before {\n  content: \"\\F1B4\";\n}\n.font-awesome_fa-behance-square_5ghK4:before {\n  content: \"\\F1B5\";\n}\n.font-awesome_fa-steam_RIwxM:before {\n  content: \"\\F1B6\";\n}\n.font-awesome_fa-steam-square_2QEJn:before {\n  content: \"\\F1B7\";\n}\n.font-awesome_fa-recycle_-U8tZ:before {\n  content: \"\\F1B8\";\n}\n.font-awesome_fa-automobile_3z3Dw:before,\n.font-awesome_fa-car_30pca:before {\n  content: \"\\F1B9\";\n}\n.font-awesome_fa-cab_DDNE1:before,\n.font-awesome_fa-taxi_22WsM:before {\n  content: \"\\F1BA\";\n}\n.font-awesome_fa-tree_3RDTB:before {\n  content: \"\\F1BB\";\n}\n.font-awesome_fa-spotify_3UDVW:before {\n  content: \"\\F1BC\";\n}\n.font-awesome_fa-deviantart_2ZxWy:before {\n  content: \"\\F1BD\";\n}\n.font-awesome_fa-soundcloud_2ALXb:before {\n  content: \"\\F1BE\";\n}\n.font-awesome_fa-database_1lI0N:before {\n  content: \"\\F1C0\";\n}\n.font-awesome_fa-file-pdf-o_3kglo:before {\n  content: \"\\F1C1\";\n}\n.font-awesome_fa-file-word-o_1UetZ:before {\n  content: \"\\F1C2\";\n}\n.font-awesome_fa-file-excel-o_A4QBn:before {\n  content: \"\\F1C3\";\n}\n.font-awesome_fa-file-powerpoint-o_rrLjs:before {\n  content: \"\\F1C4\";\n}\n.font-awesome_fa-file-photo-o_2UoDO:before,\n.font-awesome_fa-file-picture-o_3Xjli:before,\n.font-awesome_fa-file-image-o_2lPT_:before {\n  content: \"\\F1C5\";\n}\n.font-awesome_fa-file-zip-o_2FWRa:before,\n.font-awesome_fa-file-archive-o_2Mk5P:before {\n  content: \"\\F1C6\";\n}\n.font-awesome_fa-file-sound-o_1AcTq:before,\n.font-awesome_fa-file-audio-o_2PC2o:before {\n  content: \"\\F1C7\";\n}\n.font-awesome_fa-file-movie-o_VAP4m:before,\n.font-awesome_fa-file-video-o_34mPw:before {\n  content: \"\\F1C8\";\n}\n.font-awesome_fa-file-code-o_1tJvu:before {\n  content: \"\\F1C9\";\n}\n.font-awesome_fa-vine_26AR6:before {\n  content: \"\\F1CA\";\n}\n.font-awesome_fa-codepen_2F2Jy:before {\n  content: \"\\F1CB\";\n}\n.font-awesome_fa-jsfiddle_pH8-y:before {\n  content: \"\\F1CC\";\n}\n.font-awesome_fa-life-bouy_3M9kq:before,\n.font-awesome_fa-life-buoy_-dMf6:before,\n.font-awesome_fa-life-saver_1NRqc:before,\n.font-awesome_fa-support_6Q01X:before,\n.font-awesome_fa-life-ring_1x6lZ:before {\n  content: \"\\F1CD\";\n}\n.font-awesome_fa-circle-o-notch_cWGUO:before {\n  content: \"\\F1CE\";\n}\n.font-awesome_fa-ra_2liTj:before,\n.font-awesome_fa-resistance_59oYs:before,\n.font-awesome_fa-rebel_2UIOr:before {\n  content: \"\\F1D0\";\n}\n.font-awesome_fa-ge_1f9_K:before,\n.font-awesome_fa-empire_3Sw8V:before {\n  content: \"\\F1D1\";\n}\n.font-awesome_fa-git-square_DgHwD:before {\n  content: \"\\F1D2\";\n}\n.font-awesome_fa-git_1dhi0:before {\n  content: \"\\F1D3\";\n}\n.font-awesome_fa-y-combinator-square_lfSlT:before,\n.font-awesome_fa-yc-square_1Qf2g:before,\n.font-awesome_fa-hacker-news_CxkYC:before {\n  content: \"\\F1D4\";\n}\n.font-awesome_fa-tencent-weibo_2-fdG:before {\n  content: \"\\F1D5\";\n}\n.font-awesome_fa-qq_1OIck:before {\n  content: \"\\F1D6\";\n}\n.font-awesome_fa-wechat_7Wqz8:before,\n.font-awesome_fa-weixin_2rvXg:before {\n  content: \"\\F1D7\";\n}\n.font-awesome_fa-send_1PHOy:before,\n.font-awesome_fa-paper-plane_1JBzT:before {\n  content: \"\\F1D8\";\n}\n.font-awesome_fa-send-o_1K3Am:before,\n.font-awesome_fa-paper-plane-o_Am7EP:before {\n  content: \"\\F1D9\";\n}\n.font-awesome_fa-history_xEiAH:before {\n  content: \"\\F1DA\";\n}\n.font-awesome_fa-circle-thin_OCNZt:before {\n  content: \"\\F1DB\";\n}\n.font-awesome_fa-header_hMELn:before {\n  content: \"\\F1DC\";\n}\n.font-awesome_fa-paragraph_2r_mD:before {\n  content: \"\\F1DD\";\n}\n.font-awesome_fa-sliders_3eRoo:before {\n  content: \"\\F1DE\";\n}\n.font-awesome_fa-share-alt_3jAY7:before {\n  content: \"\\F1E0\";\n}\n.font-awesome_fa-share-alt-square_46dVM:before {\n  content: \"\\F1E1\";\n}\n.font-awesome_fa-bomb_1WRhh:before {\n  content: \"\\F1E2\";\n}\n.font-awesome_fa-soccer-ball-o_3rmya:before,\n.font-awesome_fa-futbol-o_Nqzpi:before {\n  content: \"\\F1E3\";\n}\n.font-awesome_fa-tty_3BPj2:before {\n  content: \"\\F1E4\";\n}\n.font-awesome_fa-binoculars_1vG29:before {\n  content: \"\\F1E5\";\n}\n.font-awesome_fa-plug_1Lbxt:before {\n  content: \"\\F1E6\";\n}\n.font-awesome_fa-slideshare_15ZAf:before {\n  content: \"\\F1E7\";\n}\n.font-awesome_fa-twitch_MNLu3:before {\n  content: \"\\F1E8\";\n}\n.font-awesome_fa-yelp_1c1W7:before {\n  content: \"\\F1E9\";\n}\n.font-awesome_fa-newspaper-o_1ecUe:before {\n  content: \"\\F1EA\";\n}\n.font-awesome_fa-wifi_dQ61U:before {\n  content: \"\\F1EB\";\n}\n.font-awesome_fa-calculator_2q6GV:before {\n  content: \"\\F1EC\";\n}\n.font-awesome_fa-paypal_3lmxL:before {\n  content: \"\\F1ED\";\n}\n.font-awesome_fa-google-wallet_2K_aw:before {\n  content: \"\\F1EE\";\n}\n.font-awesome_fa-cc-visa_2F8r8:before {\n  content: \"\\F1F0\";\n}\n.font-awesome_fa-cc-mastercard_T8WQ_:before {\n  content: \"\\F1F1\";\n}\n.font-awesome_fa-cc-discover_2QXm7:before {\n  content: \"\\F1F2\";\n}\n.font-awesome_fa-cc-amex_2w-j8:before {\n  content: \"\\F1F3\";\n}\n.font-awesome_fa-cc-paypal_gr0Zj:before {\n  content: \"\\F1F4\";\n}\n.font-awesome_fa-cc-stripe_5ubxJ:before {\n  content: \"\\F1F5\";\n}\n.font-awesome_fa-bell-slash_PIYu4:before {\n  content: \"\\F1F6\";\n}\n.font-awesome_fa-bell-slash-o_PTM9c:before {\n  content: \"\\F1F7\";\n}\n.font-awesome_fa-trash_-YVpH:before {\n  content: \"\\F1F8\";\n}\n.font-awesome_fa-copyright_3Cj5D:before {\n  content: \"\\F1F9\";\n}\n.font-awesome_fa-at_b7Ql8:before {\n  content: \"\\F1FA\";\n}\n.font-awesome_fa-eyedropper_1rpAm:before {\n  content: \"\\F1FB\";\n}\n.font-awesome_fa-paint-brush_3SJFh:before {\n  content: \"\\F1FC\";\n}\n.font-awesome_fa-birthday-cake_-17FP:before {\n  content: \"\\F1FD\";\n}\n.font-awesome_fa-area-chart_1fTy1:before {\n  content: \"\\F1FE\";\n}\n.font-awesome_fa-pie-chart_2TXFj:before {\n  content: \"\\F200\";\n}\n.font-awesome_fa-line-chart_20bFd:before {\n  content: \"\\F201\";\n}\n.font-awesome_fa-lastfm_3sP7Z:before {\n  content: \"\\F202\";\n}\n.font-awesome_fa-lastfm-square_3OBza:before {\n  content: \"\\F203\";\n}\n.font-awesome_fa-toggle-off_2TP0s:before {\n  content: \"\\F204\";\n}\n.font-awesome_fa-toggle-on_1ud4K:before {\n  content: \"\\F205\";\n}\n.font-awesome_fa-bicycle_r_nn3:before {\n  content: \"\\F206\";\n}\n.font-awesome_fa-bus_bm6kq:before {\n  content: \"\\F207\";\n}\n.font-awesome_fa-ioxhost_yWiPs:before {\n  content: \"\\F208\";\n}\n.font-awesome_fa-angellist_14KNT:before {\n  content: \"\\F209\";\n}\n.font-awesome_fa-cc_VsUyp:before {\n  content: \"\\F20A\";\n}\n.font-awesome_fa-shekel_3RcTu:before,\n.font-awesome_fa-sheqel_2_Sde:before,\n.font-awesome_fa-ils_CYDSg:before {\n  content: \"\\F20B\";\n}\n.font-awesome_fa-meanpath_8Utkv:before {\n  content: \"\\F20C\";\n}\n.font-awesome_fa-buysellads_3DmVj:before {\n  content: \"\\F20D\";\n}\n.font-awesome_fa-connectdevelop_24BDl:before {\n  content: \"\\F20E\";\n}\n.font-awesome_fa-dashcube_3gytt:before {\n  content: \"\\F210\";\n}\n.font-awesome_fa-forumbee_1Xmr9:before {\n  content: \"\\F211\";\n}\n.font-awesome_fa-leanpub_1qDwq:before {\n  content: \"\\F212\";\n}\n.font-awesome_fa-sellsy_w39BK:before {\n  content: \"\\F213\";\n}\n.font-awesome_fa-shirtsinbulk_3ht1E:before {\n  content: \"\\F214\";\n}\n.font-awesome_fa-simplybuilt_1V2xv:before {\n  content: \"\\F215\";\n}\n.font-awesome_fa-skyatlas_1HFEf:before {\n  content: \"\\F216\";\n}\n.font-awesome_fa-cart-plus_zqpg9:before {\n  content: \"\\F217\";\n}\n.font-awesome_fa-cart-arrow-down_vmvAL:before {\n  content: \"\\F218\";\n}\n.font-awesome_fa-diamond_2YKSj:before {\n  content: \"\\F219\";\n}\n.font-awesome_fa-ship_2d0Uf:before {\n  content: \"\\F21A\";\n}\n.font-awesome_fa-user-secret_1JgJF:before {\n  content: \"\\F21B\";\n}\n.font-awesome_fa-motorcycle_hAqgH:before {\n  content: \"\\F21C\";\n}\n.font-awesome_fa-street-view_3xS1E:before {\n  content: \"\\F21D\";\n}\n.font-awesome_fa-heartbeat_3SRsO:before {\n  content: \"\\F21E\";\n}\n.font-awesome_fa-venus_3jRFX:before {\n  content: \"\\F221\";\n}\n.font-awesome_fa-mars_2Le0W:before {\n  content: \"\\F222\";\n}\n.font-awesome_fa-mercury_3-x4u:before {\n  content: \"\\F223\";\n}\n.font-awesome_fa-intersex_26r-R:before,\n.font-awesome_fa-transgender_1hS0T:before {\n  content: \"\\F224\";\n}\n.font-awesome_fa-transgender-alt_3_fBb:before {\n  content: \"\\F225\";\n}\n.font-awesome_fa-venus-double_30rPd:before {\n  content: \"\\F226\";\n}\n.font-awesome_fa-mars-double_3Xnoh:before {\n  content: \"\\F227\";\n}\n.font-awesome_fa-venus-mars_2Ptfg:before {\n  content: \"\\F228\";\n}\n.font-awesome_fa-mars-stroke_f9_Cu:before {\n  content: \"\\F229\";\n}\n.font-awesome_fa-mars-stroke-v_1K5K9:before {\n  content: \"\\F22A\";\n}\n.font-awesome_fa-mars-stroke-h_3azEl:before {\n  content: \"\\F22B\";\n}\n.font-awesome_fa-neuter_1wUaY:before {\n  content: \"\\F22C\";\n}\n.font-awesome_fa-genderless_3mEtZ:before {\n  content: \"\\F22D\";\n}\n.font-awesome_fa-facebook-official_2NNdf:before {\n  content: \"\\F230\";\n}\n.font-awesome_fa-pinterest-p_1Xpu_:before {\n  content: \"\\F231\";\n}\n.font-awesome_fa-whatsapp_3G2qZ:before {\n  content: \"\\F232\";\n}\n.font-awesome_fa-server_NVGtN:before {\n  content: \"\\F233\";\n}\n.font-awesome_fa-user-plus_1UACc:before {\n  content: \"\\F234\";\n}\n.font-awesome_fa-user-times_24FFx:before {\n  content: \"\\F235\";\n}\n.font-awesome_fa-hotel_3W6s_:before,\n.font-awesome_fa-bed_1XbLs:before {\n  content: \"\\F236\";\n}\n.font-awesome_fa-viacoin_3b4Ln:before {\n  content: \"\\F237\";\n}\n.font-awesome_fa-train_2mIFj:before {\n  content: \"\\F238\";\n}\n.font-awesome_fa-subway_mahNW:before {\n  content: \"\\F239\";\n}\n.font-awesome_fa-medium_2UIgR:before {\n  content: \"\\F23A\";\n}\n.font-awesome_fa-yc_2pwL9:before,\n.font-awesome_fa-y-combinator_l4_A9:before {\n  content: \"\\F23B\";\n}\n.font-awesome_fa-optin-monster_2Vo1M:before {\n  content: \"\\F23C\";\n}\n.font-awesome_fa-opencart_2P3qK:before {\n  content: \"\\F23D\";\n}\n.font-awesome_fa-expeditedssl_1ay3x:before {\n  content: \"\\F23E\";\n}\n.font-awesome_fa-battery-4_1qRp1:before,\n.font-awesome_fa-battery_1TgW-:before,\n.font-awesome_fa-battery-full_2fsqT:before {\n  content: \"\\F240\";\n}\n.font-awesome_fa-battery-3_3WHzS:before,\n.font-awesome_fa-battery-three-quarters_dBjV8:before {\n  content: \"\\F241\";\n}\n.font-awesome_fa-battery-2_2Pgt2:before,\n.font-awesome_fa-battery-half_2taE9:before {\n  content: \"\\F242\";\n}\n.font-awesome_fa-battery-1_1R1Ww:before,\n.font-awesome_fa-battery-quarter_1sRcE:before {\n  content: \"\\F243\";\n}\n.font-awesome_fa-battery-0_1zrhu:before,\n.font-awesome_fa-battery-empty_2Mn-c:before {\n  content: \"\\F244\";\n}\n.font-awesome_fa-mouse-pointer_DbB5u:before {\n  content: \"\\F245\";\n}\n.font-awesome_fa-i-cursor_xvyzh:before {\n  content: \"\\F246\";\n}\n.font-awesome_fa-object-group_3K3tV:before {\n  content: \"\\F247\";\n}\n.font-awesome_fa-object-ungroup_1ylE-:before {\n  content: \"\\F248\";\n}\n.font-awesome_fa-sticky-note_1dK3l:before {\n  content: \"\\F249\";\n}\n.font-awesome_fa-sticky-note-o_2zvyB:before {\n  content: \"\\F24A\";\n}\n.font-awesome_fa-cc-jcb_Q7v9N:before {\n  content: \"\\F24B\";\n}\n.font-awesome_fa-cc-diners-club_338EC:before {\n  content: \"\\F24C\";\n}\n.font-awesome_fa-clone_2LPS7:before {\n  content: \"\\F24D\";\n}\n.font-awesome_fa-balance-scale_3o2it:before {\n  content: \"\\F24E\";\n}\n.font-awesome_fa-hourglass-o_15XJL:before {\n  content: \"\\F250\";\n}\n.font-awesome_fa-hourglass-1_2iRUs:before,\n.font-awesome_fa-hourglass-start_qhpOV:before {\n  content: \"\\F251\";\n}\n.font-awesome_fa-hourglass-2_2V0b5:before,\n.font-awesome_fa-hourglass-half_cF0Po:before {\n  content: \"\\F252\";\n}\n.font-awesome_fa-hourglass-3_2-ugV:before,\n.font-awesome_fa-hourglass-end_3l-g6:before {\n  content: \"\\F253\";\n}\n.font-awesome_fa-hourglass_1Ar7q:before {\n  content: \"\\F254\";\n}\n.font-awesome_fa-hand-grab-o_3I7_Y:before,\n.font-awesome_fa-hand-rock-o_1Tb8S:before {\n  content: \"\\F255\";\n}\n.font-awesome_fa-hand-stop-o_37eq3:before,\n.font-awesome_fa-hand-paper-o_2dp3p:before {\n  content: \"\\F256\";\n}\n.font-awesome_fa-hand-scissors-o_tLXdy:before {\n  content: \"\\F257\";\n}\n.font-awesome_fa-hand-lizard-o_2afn0:before {\n  content: \"\\F258\";\n}\n.font-awesome_fa-hand-spock-o_22lUn:before {\n  content: \"\\F259\";\n}\n.font-awesome_fa-hand-pointer-o_3EDBr:before {\n  content: \"\\F25A\";\n}\n.font-awesome_fa-hand-peace-o_3KVDU:before {\n  content: \"\\F25B\";\n}\n.font-awesome_fa-trademark_1pZSQ:before {\n  content: \"\\F25C\";\n}\n.font-awesome_fa-registered_2bkiQ:before {\n  content: \"\\F25D\";\n}\n.font-awesome_fa-creative-commons_19SOu:before {\n  content: \"\\F25E\";\n}\n.font-awesome_fa-gg_8EwZk:before {\n  content: \"\\F260\";\n}\n.font-awesome_fa-gg-circle_ixSHX:before {\n  content: \"\\F261\";\n}\n.font-awesome_fa-tripadvisor_3SR4I:before {\n  content: \"\\F262\";\n}\n.font-awesome_fa-odnoklassniki_18Bc_:before {\n  content: \"\\F263\";\n}\n.font-awesome_fa-odnoklassniki-square_2tvme:before {\n  content: \"\\F264\";\n}\n.font-awesome_fa-get-pocket_1kDeB:before {\n  content: \"\\F265\";\n}\n.font-awesome_fa-wikipedia-w_2bnVT:before {\n  content: \"\\F266\";\n}\n.font-awesome_fa-safari_1d_gp:before {\n  content: \"\\F267\";\n}\n.font-awesome_fa-chrome_2lYJX:before {\n  content: \"\\F268\";\n}\n.font-awesome_fa-firefox_3G1uV:before {\n  content: \"\\F269\";\n}\n.font-awesome_fa-opera_2EABz:before {\n  content: \"\\F26A\";\n}\n.font-awesome_fa-internet-explorer_2e6T2:before {\n  content: \"\\F26B\";\n}\n.font-awesome_fa-tv_pyAzy:before,\n.font-awesome_fa-television_1MplB:before {\n  content: \"\\F26C\";\n}\n.font-awesome_fa-contao_1BTJ5:before {\n  content: \"\\F26D\";\n}\n.font-awesome_fa-500px_2dpFP:before {\n  content: \"\\F26E\";\n}\n.font-awesome_fa-amazon_1J6OF:before {\n  content: \"\\F270\";\n}\n.font-awesome_fa-calendar-plus-o_up6cZ:before {\n  content: \"\\F271\";\n}\n.font-awesome_fa-calendar-minus-o_2wY7J:before {\n  content: \"\\F272\";\n}\n.font-awesome_fa-calendar-times-o_1jaLQ:before {\n  content: \"\\F273\";\n}\n.font-awesome_fa-calendar-check-o_3xoZC:before {\n  content: \"\\F274\";\n}\n.font-awesome_fa-industry_3LSV8:before {\n  content: \"\\F275\";\n}\n.font-awesome_fa-map-pin_1mpnW:before {\n  content: \"\\F276\";\n}\n.font-awesome_fa-map-signs_21LXb:before {\n  content: \"\\F277\";\n}\n.font-awesome_fa-map-o_1CDpd:before {\n  content: \"\\F278\";\n}\n.font-awesome_fa-map_18QCe:before {\n  content: \"\\F279\";\n}\n.font-awesome_fa-commenting_2oYYM:before {\n  content: \"\\F27A\";\n}\n.font-awesome_fa-commenting-o_2BRal:before {\n  content: \"\\F27B\";\n}\n.font-awesome_fa-houzz_13-hb:before {\n  content: \"\\F27C\";\n}\n.font-awesome_fa-vimeo_3vcPv:before {\n  content: \"\\F27D\";\n}\n.font-awesome_fa-black-tie_34h9B:before {\n  content: \"\\F27E\";\n}\n.font-awesome_fa-fonticons_aNgtF:before {\n  content: \"\\F280\";\n}\n.font-awesome_fa-reddit-alien_3f_aH:before {\n  content: \"\\F281\";\n}\n.font-awesome_fa-edge_3UUWF:before {\n  content: \"\\F282\";\n}\n.font-awesome_fa-credit-card-alt_oOWN1:before {\n  content: \"\\F283\";\n}\n.font-awesome_fa-codiepie_2amwQ:before {\n  content: \"\\F284\";\n}\n.font-awesome_fa-modx__HnMH:before {\n  content: \"\\F285\";\n}\n.font-awesome_fa-fort-awesome_1Pxvs:before {\n  content: \"\\F286\";\n}\n.font-awesome_fa-usb_2-FsD:before {\n  content: \"\\F287\";\n}\n.font-awesome_fa-product-hunt_3WqRr:before {\n  content: \"\\F288\";\n}\n.font-awesome_fa-mixcloud_2e01G:before {\n  content: \"\\F289\";\n}\n.font-awesome_fa-scribd_1bAIo:before {\n  content: \"\\F28A\";\n}\n.font-awesome_fa-pause-circle_3wI6c:before {\n  content: \"\\F28B\";\n}\n.font-awesome_fa-pause-circle-o_2MdRS:before {\n  content: \"\\F28C\";\n}\n.font-awesome_fa-stop-circle_3aZ6V:before {\n  content: \"\\F28D\";\n}\n.font-awesome_fa-stop-circle-o_2oIr6:before {\n  content: \"\\F28E\";\n}\n.font-awesome_fa-shopping-bag_2mD0w:before {\n  content: \"\\F290\";\n}\n.font-awesome_fa-shopping-basket_2ZYTJ:before {\n  content: \"\\F291\";\n}\n.font-awesome_fa-hashtag_1sHh4:before {\n  content: \"\\F292\";\n}\n.font-awesome_fa-bluetooth_1tJ1-:before {\n  content: \"\\F293\";\n}\n.font-awesome_fa-bluetooth-b_LmWTh:before {\n  content: \"\\F294\";\n}\n.font-awesome_fa-percent_3jbSX:before {\n  content: \"\\F295\";\n}\n.font-awesome_fa-gitlab_17NxC:before {\n  content: \"\\F296\";\n}\n.font-awesome_fa-wpbeginner_12WF2:before {\n  content: \"\\F297\";\n}\n.font-awesome_fa-wpforms_1qO7l:before {\n  content: \"\\F298\";\n}\n.font-awesome_fa-envira_3VCH-:before {\n  content: \"\\F299\";\n}\n.font-awesome_fa-universal-access_2BAWK:before {\n  content: \"\\F29A\";\n}\n.font-awesome_fa-wheelchair-alt_x86hz:before {\n  content: \"\\F29B\";\n}\n.font-awesome_fa-question-circle-o_HE6Iy:before {\n  content: \"\\F29C\";\n}\n.font-awesome_fa-blind_2GszD:before {\n  content: \"\\F29D\";\n}\n.font-awesome_fa-audio-description_1vruh:before {\n  content: \"\\F29E\";\n}\n.font-awesome_fa-volume-control-phone_2-hID:before {\n  content: \"\\F2A0\";\n}\n.font-awesome_fa-braille_JZYOH:before {\n  content: \"\\F2A1\";\n}\n.font-awesome_fa-assistive-listening-systems_-MRgD:before {\n  content: \"\\F2A2\";\n}\n.font-awesome_fa-asl-interpreting_2Czb5:before,\n.font-awesome_fa-american-sign-language-interpreting_21O_W:before {\n  content: \"\\F2A3\";\n}\n.font-awesome_fa-deafness_30DXf:before,\n.font-awesome_fa-hard-of-hearing_1Mzrx:before,\n.font-awesome_fa-deaf_20y_-:before {\n  content: \"\\F2A4\";\n}\n.font-awesome_fa-glide_31K4T:before {\n  content: \"\\F2A5\";\n}\n.font-awesome_fa-glide-g_2xpqn:before {\n  content: \"\\F2A6\";\n}\n.font-awesome_fa-signing_18ve9:before,\n.font-awesome_fa-sign-language_2kCDJ:before {\n  content: \"\\F2A7\";\n}\n.font-awesome_fa-low-vision_2-HWe:before {\n  content: \"\\F2A8\";\n}\n.font-awesome_fa-viadeo_1u1ez:before {\n  content: \"\\F2A9\";\n}\n.font-awesome_fa-viadeo-square_2hftx:before {\n  content: \"\\F2AA\";\n}\n.font-awesome_fa-snapchat_33pkT:before {\n  content: \"\\F2AB\";\n}\n.font-awesome_fa-snapchat-ghost_3Xx5A:before {\n  content: \"\\F2AC\";\n}\n.font-awesome_fa-snapchat-square_1PZbq:before {\n  content: \"\\F2AD\";\n}\n.font-awesome_fa-pied-piper_1iXBb:before {\n  content: \"\\F2AE\";\n}\n.font-awesome_fa-first-order_3mduz:before {\n  content: \"\\F2B0\";\n}\n.font-awesome_fa-yoast__hiOs:before {\n  content: \"\\F2B1\";\n}\n.font-awesome_fa-themeisle_3aDVe:before {\n  content: \"\\F2B2\";\n}\n.font-awesome_fa-google-plus-circle_oADtd:before,\n.font-awesome_fa-google-plus-official_2cz4z:before {\n  content: \"\\F2B3\";\n}\n.font-awesome_fa-fa_230yG:before,\n.font-awesome_fa-font-awesome_2p-G4:before {\n  content: \"\\F2B4\";\n}\n.font-awesome_fa-handshake-o_1BQgE:before {\n  content: \"\\F2B5\";\n}\n.font-awesome_fa-envelope-open_nzht3:before {\n  content: \"\\F2B6\";\n}\n.font-awesome_fa-envelope-open-o_1gQ7U:before {\n  content: \"\\F2B7\";\n}\n.font-awesome_fa-linode_1CKdY:before {\n  content: \"\\F2B8\";\n}\n.font-awesome_fa-address-book_1DopX:before {\n  content: \"\\F2B9\";\n}\n.font-awesome_fa-address-book-o_PVnwl:before {\n  content: \"\\F2BA\";\n}\n.font-awesome_fa-vcard_1mytB:before,\n.font-awesome_fa-address-card_10TH1:before {\n  content: \"\\F2BB\";\n}\n.font-awesome_fa-vcard-o_2z061:before,\n.font-awesome_fa-address-card-o_ZAnUS:before {\n  content: \"\\F2BC\";\n}\n.font-awesome_fa-user-circle_eFX66:before {\n  content: \"\\F2BD\";\n}\n.font-awesome_fa-user-circle-o_2CCV4:before {\n  content: \"\\F2BE\";\n}\n.font-awesome_fa-user-o_3pkn4:before {\n  content: \"\\F2C0\";\n}\n.font-awesome_fa-id-badge_B-CxE:before {\n  content: \"\\F2C1\";\n}\n.font-awesome_fa-drivers-license_YQC6r:before,\n.font-awesome_fa-id-card_2YGHP:before {\n  content: \"\\F2C2\";\n}\n.font-awesome_fa-drivers-license-o_2kniu:before,\n.font-awesome_fa-id-card-o_377Os:before {\n  content: \"\\F2C3\";\n}\n.font-awesome_fa-quora_1ygDM:before {\n  content: \"\\F2C4\";\n}\n.font-awesome_fa-free-code-camp_2oc8W:before {\n  content: \"\\F2C5\";\n}\n.font-awesome_fa-telegram_rrK8w:before {\n  content: \"\\F2C6\";\n}\n.font-awesome_fa-thermometer-4_17QiM:before,\n.font-awesome_fa-thermometer_2CY6f:before,\n.font-awesome_fa-thermometer-full_MmoVG:before {\n  content: \"\\F2C7\";\n}\n.font-awesome_fa-thermometer-3_1K5SW:before,\n.font-awesome_fa-thermometer-three-quarters_Z2c0u:before {\n  content: \"\\F2C8\";\n}\n.font-awesome_fa-thermometer-2_t0LRT:before,\n.font-awesome_fa-thermometer-half_R314u:before {\n  content: \"\\F2C9\";\n}\n.font-awesome_fa-thermometer-1_1AYjC:before,\n.font-awesome_fa-thermometer-quarter_2vzqc:before {\n  content: \"\\F2CA\";\n}\n.font-awesome_fa-thermometer-0_25pBF:before,\n.font-awesome_fa-thermometer-empty_3L7rA:before {\n  content: \"\\F2CB\";\n}\n.font-awesome_fa-shower_1mf85:before {\n  content: \"\\F2CC\";\n}\n.font-awesome_fa-bathtub_sWKu2:before,\n.font-awesome_fa-s15_3d8v3:before,\n.font-awesome_fa-bath_3ZQIh:before {\n  content: \"\\F2CD\";\n}\n.font-awesome_fa-podcast_2bMxn:before {\n  content: \"\\F2CE\";\n}\n.font-awesome_fa-window-maximize_3Nkvx:before {\n  content: \"\\F2D0\";\n}\n.font-awesome_fa-window-minimize_3hY91:before {\n  content: \"\\F2D1\";\n}\n.font-awesome_fa-window-restore_3rb68:before {\n  content: \"\\F2D2\";\n}\n.font-awesome_fa-times-rectangle_2WAZ6:before,\n.font-awesome_fa-window-close_3pJ9t:before {\n  content: \"\\F2D3\";\n}\n.font-awesome_fa-times-rectangle-o_3fndH:before,\n.font-awesome_fa-window-close-o_cWi6f:before {\n  content: \"\\F2D4\";\n}\n.font-awesome_fa-bandcamp_1chYL:before {\n  content: \"\\F2D5\";\n}\n.font-awesome_fa-grav_3rCDC:before {\n  content: \"\\F2D6\";\n}\n.font-awesome_fa-etsy_1ijNc:before {\n  content: \"\\F2D7\";\n}\n.font-awesome_fa-imdb_1UKQ-:before {\n  content: \"\\F2D8\";\n}\n.font-awesome_fa-ravelry_2Mh85:before {\n  content: \"\\F2D9\";\n}\n.font-awesome_fa-eercast_2kOfj:before {\n  content: \"\\F2DA\";\n}\n.font-awesome_fa-microchip_3-A90:before {\n  content: \"\\F2DB\";\n}\n.font-awesome_fa-snowflake-o__2tjp:before {\n  content: \"\\F2DC\";\n}\n.font-awesome_fa-superpowers_3XIZD:before {\n  content: \"\\F2DD\";\n}\n.font-awesome_fa-wpexplorer_3Mqmq:before {\n  content: \"\\F2DE\";\n}\n.font-awesome_fa-meetup_2IXst:before {\n  content: \"\\F2E0\";\n}\n.font-awesome_sr-only_3QD-2 {\n  position: absolute;\n  width: 1px;\n  height: 1px;\n  padding: 0;\n  margin: -1px;\n  overflow: hidden;\n  clip: rect(0, 0, 0, 0);\n  border: 0;\n}\n.font-awesome_sr-only-focusable_3JKtw:active,\n.font-awesome_sr-only-focusable_3JKtw:focus {\n  position: static;\n  width: auto;\n  height: auto;\n  margin: 0;\n  overflow: visible;\n  clip: auto;\n}\n", ""]);

	// exports
	exports.locals = {
		"fa": "font-awesome_fa_2otTb",
		"fa-lg": "font-awesome_fa-lg_2-2uP",
		"fa-2x": "font-awesome_fa-2x_2Mgjx",
		"fa-3x": "font-awesome_fa-3x_1gdsS",
		"fa-4x": "font-awesome_fa-4x_2VkGW",
		"fa-5x": "font-awesome_fa-5x_QKikc",
		"fa-fw": "font-awesome_fa-fw_1FdA5",
		"fa-ul": "font-awesome_fa-ul_2XTDQ",
		"fa-li": "font-awesome_fa-li_1vepp",
		"fa-border": "font-awesome_fa-border_6EUMg",
		"fa-pull-left": "font-awesome_fa-pull-left_3jHfw",
		"fa-pull-right": "font-awesome_fa-pull-right_20ZAt",
		"pull-right": "font-awesome_pull-right_1Mb60",
		"pull-left": "font-awesome_pull-left_30vXl",
		"fa-spin": "font-awesome_fa-spin_NsqCr",
		"fa-pulse": "font-awesome_fa-pulse_1Vv2f",
		"fa-rotate-90": "font-awesome_fa-rotate-90_1snKw",
		"fa-rotate-180": "font-awesome_fa-rotate-180_2hMM8",
		"fa-rotate-270": "font-awesome_fa-rotate-270_3eBDG",
		"fa-flip-horizontal": "font-awesome_fa-flip-horizontal_33SUC",
		"fa-flip-vertical": "font-awesome_fa-flip-vertical_klOOu",
		"fa-stack": "font-awesome_fa-stack_3fqsM",
		"fa-stack-1x": "font-awesome_fa-stack-1x_14Vb0",
		"fa-stack-2x": "font-awesome_fa-stack-2x_It5yP",
		"fa-inverse": "font-awesome_fa-inverse_1e1EX",
		"fa-glass": "font-awesome_fa-glass_34uit",
		"fa-music": "font-awesome_fa-music_3f2s5",
		"fa-search": "font-awesome_fa-search_1C7GK",
		"fa-envelope-o": "font-awesome_fa-envelope-o_3EWEI",
		"fa-heart": "font-awesome_fa-heart_3DHYT",
		"fa-star": "font-awesome_fa-star_2g4Ye",
		"fa-star-o": "font-awesome_fa-star-o_3ty_o",
		"fa-user": "font-awesome_fa-user_N3puO",
		"fa-film": "font-awesome_fa-film_2qmKe",
		"fa-th-large": "font-awesome_fa-th-large_2N4P8",
		"fa-th": "font-awesome_fa-th_3f0mR",
		"fa-th-list": "font-awesome_fa-th-list_3qelJ",
		"fa-check": "font-awesome_fa-check_3DXVm",
		"fa-remove": "font-awesome_fa-remove_1MIYz",
		"fa-close": "font-awesome_fa-close_1zysR",
		"fa-times": "font-awesome_fa-times_1Y-Cs",
		"fa-search-plus": "font-awesome_fa-search-plus_9OGuc",
		"fa-search-minus": "font-awesome_fa-search-minus_1j_Aj",
		"fa-power-off": "font-awesome_fa-power-off_vPefe",
		"fa-signal": "font-awesome_fa-signal_1VxWh",
		"fa-gear": "font-awesome_fa-gear_yfzjv",
		"fa-cog": "font-awesome_fa-cog_30mdw",
		"fa-trash-o": "font-awesome_fa-trash-o_2uFKh",
		"fa-home": "font-awesome_fa-home_3Fr6e",
		"fa-file-o": "font-awesome_fa-file-o_208AJ",
		"fa-clock-o": "font-awesome_fa-clock-o_3vfig",
		"fa-road": "font-awesome_fa-road_2017v",
		"fa-download": "font-awesome_fa-download_1TDS9",
		"fa-arrow-circle-o-down": "font-awesome_fa-arrow-circle-o-down_2M97h",
		"fa-arrow-circle-o-up": "font-awesome_fa-arrow-circle-o-up_2aqY-",
		"fa-inbox": "font-awesome_fa-inbox_3bWnM",
		"fa-play-circle-o": "font-awesome_fa-play-circle-o_3vU6r",
		"fa-rotate-right": "font-awesome_fa-rotate-right_1E_3J",
		"fa-repeat": "font-awesome_fa-repeat_27E0b",
		"fa-refresh": "font-awesome_fa-refresh_2AOlD",
		"fa-list-alt": "font-awesome_fa-list-alt_3nS4v",
		"fa-lock": "font-awesome_fa-lock_inyGT",
		"fa-flag": "font-awesome_fa-flag_1qWlx",
		"fa-headphones": "font-awesome_fa-headphones_13olw",
		"fa-volume-off": "font-awesome_fa-volume-off_1llC2",
		"fa-volume-down": "font-awesome_fa-volume-down_1jTgZ",
		"fa-volume-up": "font-awesome_fa-volume-up_2XIXx",
		"fa-qrcode": "font-awesome_fa-qrcode_17ZaI",
		"fa-barcode": "font-awesome_fa-barcode_1al4-",
		"fa-tag": "font-awesome_fa-tag_2CMhy",
		"fa-tags": "font-awesome_fa-tags_3kYb4",
		"fa-book": "font-awesome_fa-book_32JVT",
		"fa-bookmark": "font-awesome_fa-bookmark_1s2Fl",
		"fa-print": "font-awesome_fa-print_y2Ezw",
		"fa-camera": "font-awesome_fa-camera_Ls8dv",
		"fa-font": "font-awesome_fa-font_1VH0X",
		"fa-bold": "font-awesome_fa-bold_11qyx",
		"fa-italic": "font-awesome_fa-italic_1Gtc3",
		"fa-text-height": "font-awesome_fa-text-height_3db67",
		"fa-text-width": "font-awesome_fa-text-width_2yBeb",
		"fa-align-left": "font-awesome_fa-align-left_3DuVK",
		"fa-align-center": "font-awesome_fa-align-center_M9xyY",
		"fa-align-right": "font-awesome_fa-align-right_3Icru",
		"fa-align-justify": "font-awesome_fa-align-justify_maoNA",
		"fa-list": "font-awesome_fa-list_3CT1m",
		"fa-dedent": "font-awesome_fa-dedent_3p5N-",
		"fa-outdent": "font-awesome_fa-outdent_34S6p",
		"fa-indent": "font-awesome_fa-indent_2Y6xl",
		"fa-video-camera": "font-awesome_fa-video-camera_2Tfna",
		"fa-photo": "font-awesome_fa-photo_2f_lI",
		"fa-image": "font-awesome_fa-image_l6mTT",
		"fa-picture-o": "font-awesome_fa-picture-o_3srts",
		"fa-pencil": "font-awesome_fa-pencil_3o0Fh",
		"fa-map-marker": "font-awesome_fa-map-marker_1Lc7q",
		"fa-adjust": "font-awesome_fa-adjust_1uk96",
		"fa-tint": "font-awesome_fa-tint_23wIx",
		"fa-edit": "font-awesome_fa-edit_2ITK3",
		"fa-pencil-square-o": "font-awesome_fa-pencil-square-o_1jFx_",
		"fa-share-square-o": "font-awesome_fa-share-square-o_1bC_y",
		"fa-check-square-o": "font-awesome_fa-check-square-o_2eIdJ",
		"fa-arrows": "font-awesome_fa-arrows_2tovc",
		"fa-step-backward": "font-awesome_fa-step-backward_1aJ3J",
		"fa-fast-backward": "font-awesome_fa-fast-backward_rW1JQ",
		"fa-backward": "font-awesome_fa-backward_WWKjR",
		"fa-play": "font-awesome_fa-play_1vQTN",
		"fa-pause": "font-awesome_fa-pause_33hRm",
		"fa-stop": "font-awesome_fa-stop_1l1v_",
		"fa-forward": "font-awesome_fa-forward_3jJNW",
		"fa-fast-forward": "font-awesome_fa-fast-forward_2JwE2",
		"fa-step-forward": "font-awesome_fa-step-forward_3NyZe",
		"fa-eject": "font-awesome_fa-eject_GA_Jm",
		"fa-chevron-left": "font-awesome_fa-chevron-left_1EwAm",
		"fa-chevron-right": "font-awesome_fa-chevron-right_3RxN_",
		"fa-plus-circle": "font-awesome_fa-plus-circle_1SAMg",
		"fa-minus-circle": "font-awesome_fa-minus-circle_Th8wG",
		"fa-times-circle": "font-awesome_fa-times-circle_JfW7D",
		"fa-check-circle": "font-awesome_fa-check-circle_PBRDH",
		"fa-question-circle": "font-awesome_fa-question-circle_33ykP",
		"fa-info-circle": "font-awesome_fa-info-circle_2ZdWr",
		"fa-crosshairs": "font-awesome_fa-crosshairs_18oYo",
		"fa-times-circle-o": "font-awesome_fa-times-circle-o_3LLKK",
		"fa-check-circle-o": "font-awesome_fa-check-circle-o_1CW2E",
		"fa-ban": "font-awesome_fa-ban_1LOdy",
		"fa-arrow-left": "font-awesome_fa-arrow-left_2G_P0",
		"fa-arrow-right": "font-awesome_fa-arrow-right_15DTA",
		"fa-arrow-up": "font-awesome_fa-arrow-up_12gpU",
		"fa-arrow-down": "font-awesome_fa-arrow-down_2zqoH",
		"fa-mail-forward": "font-awesome_fa-mail-forward_2tQrw",
		"fa-share": "font-awesome_fa-share_3O8Dc",
		"fa-expand": "font-awesome_fa-expand_34Ihf",
		"fa-compress": "font-awesome_fa-compress_1JDdS",
		"fa-plus": "font-awesome_fa-plus_WEb-k",
		"fa-minus": "font-awesome_fa-minus_1WAd4",
		"fa-asterisk": "font-awesome_fa-asterisk_w7w6r",
		"fa-exclamation-circle": "font-awesome_fa-exclamation-circle_2bbrU",
		"fa-gift": "font-awesome_fa-gift_3bKvI",
		"fa-leaf": "font-awesome_fa-leaf_2B5Uf",
		"fa-fire": "font-awesome_fa-fire_1qsDr",
		"fa-eye": "font-awesome_fa-eye_3XRn0",
		"fa-eye-slash": "font-awesome_fa-eye-slash_18NEx",
		"fa-warning": "font-awesome_fa-warning_32nGg",
		"fa-exclamation-triangle": "font-awesome_fa-exclamation-triangle_ttuT-",
		"fa-plane": "font-awesome_fa-plane_3L5mD",
		"fa-calendar": "font-awesome_fa-calendar_1niuw",
		"fa-random": "font-awesome_fa-random_2RH42",
		"fa-comment": "font-awesome_fa-comment_2koYW",
		"fa-magnet": "font-awesome_fa-magnet_33k7m",
		"fa-chevron-up": "font-awesome_fa-chevron-up_2R5R_",
		"fa-chevron-down": "font-awesome_fa-chevron-down_746nC",
		"fa-retweet": "font-awesome_fa-retweet_2ma5b",
		"fa-shopping-cart": "font-awesome_fa-shopping-cart_276KU",
		"fa-folder": "font-awesome_fa-folder_2MMW6",
		"fa-folder-open": "font-awesome_fa-folder-open_1a3bX",
		"fa-arrows-v": "font-awesome_fa-arrows-v_27J04",
		"fa-arrows-h": "font-awesome_fa-arrows-h_3EAQ6",
		"fa-bar-chart-o": "font-awesome_fa-bar-chart-o_BMSPQ",
		"fa-bar-chart": "font-awesome_fa-bar-chart_3LGib",
		"fa-twitter-square": "font-awesome_fa-twitter-square_146CY",
		"fa-facebook-square": "font-awesome_fa-facebook-square_3IbRT",
		"fa-camera-retro": "font-awesome_fa-camera-retro_oM_mn",
		"fa-key": "font-awesome_fa-key_3bV7M",
		"fa-gears": "font-awesome_fa-gears_3cjY1",
		"fa-cogs": "font-awesome_fa-cogs_CqXH5",
		"fa-comments": "font-awesome_fa-comments_2lUtO",
		"fa-thumbs-o-up": "font-awesome_fa-thumbs-o-up_3cD9j",
		"fa-thumbs-o-down": "font-awesome_fa-thumbs-o-down_3AeCO",
		"fa-star-half": "font-awesome_fa-star-half_2zxdp",
		"fa-heart-o": "font-awesome_fa-heart-o_QI-Zl",
		"fa-sign-out": "font-awesome_fa-sign-out_2IOU5",
		"fa-linkedin-square": "font-awesome_fa-linkedin-square_3HkV4",
		"fa-thumb-tack": "font-awesome_fa-thumb-tack_2gcw0",
		"fa-external-link": "font-awesome_fa-external-link_1ku_O",
		"fa-sign-in": "font-awesome_fa-sign-in_1MYT-",
		"fa-trophy": "font-awesome_fa-trophy_3CyBM",
		"fa-github-square": "font-awesome_fa-github-square_1xm6W",
		"fa-upload": "font-awesome_fa-upload_wVRel",
		"fa-lemon-o": "font-awesome_fa-lemon-o_2v3hR",
		"fa-phone": "font-awesome_fa-phone_1EiFR",
		"fa-square-o": "font-awesome_fa-square-o_WbQ8x",
		"fa-bookmark-o": "font-awesome_fa-bookmark-o_1R5xe",
		"fa-phone-square": "font-awesome_fa-phone-square_3GkD1",
		"fa-twitter": "font-awesome_fa-twitter_cyUBg",
		"fa-facebook-f": "font-awesome_fa-facebook-f_3r4VF",
		"fa-facebook": "font-awesome_fa-facebook_f3EUw",
		"fa-github": "font-awesome_fa-github_MdgBC",
		"fa-unlock": "font-awesome_fa-unlock_XTSXp",
		"fa-credit-card": "font-awesome_fa-credit-card_28S4q",
		"fa-feed": "font-awesome_fa-feed_3tLbf",
		"fa-rss": "font-awesome_fa-rss_3_EzS",
		"fa-hdd-o": "font-awesome_fa-hdd-o_3ZoO6",
		"fa-bullhorn": "font-awesome_fa-bullhorn_3o7hz",
		"fa-bell": "font-awesome_fa-bell_26AZW",
		"fa-certificate": "font-awesome_fa-certificate_11sLt",
		"fa-hand-o-right": "font-awesome_fa-hand-o-right_2G1w_",
		"fa-hand-o-left": "font-awesome_fa-hand-o-left_2KTOL",
		"fa-hand-o-up": "font-awesome_fa-hand-o-up_3xrkS",
		"fa-hand-o-down": "font-awesome_fa-hand-o-down_3cWAN",
		"fa-arrow-circle-left": "font-awesome_fa-arrow-circle-left_2CgFw",
		"fa-arrow-circle-right": "font-awesome_fa-arrow-circle-right_35XcE",
		"fa-arrow-circle-up": "font-awesome_fa-arrow-circle-up_FHcwE",
		"fa-arrow-circle-down": "font-awesome_fa-arrow-circle-down_1NJKi",
		"fa-globe": "font-awesome_fa-globe_2fYFX",
		"fa-wrench": "font-awesome_fa-wrench_3snDo",
		"fa-tasks": "font-awesome_fa-tasks_2_oS8",
		"fa-filter": "font-awesome_fa-filter_1q5k8",
		"fa-briefcase": "font-awesome_fa-briefcase_aikwY",
		"fa-arrows-alt": "font-awesome_fa-arrows-alt_1vqY9",
		"fa-group": "font-awesome_fa-group_XbMo9",
		"fa-users": "font-awesome_fa-users_1PfY8",
		"fa-chain": "font-awesome_fa-chain_2QCgS",
		"fa-link": "font-awesome_fa-link_3kFkN",
		"fa-cloud": "font-awesome_fa-cloud_2l8rd",
		"fa-flask": "font-awesome_fa-flask_3iTak",
		"fa-cut": "font-awesome_fa-cut_17wpt",
		"fa-scissors": "font-awesome_fa-scissors_1xAHX",
		"fa-copy": "font-awesome_fa-copy_a2GP3",
		"fa-files-o": "font-awesome_fa-files-o_2pUmI",
		"fa-paperclip": "font-awesome_fa-paperclip_d4foW",
		"fa-save": "font-awesome_fa-save_10fTV",
		"fa-floppy-o": "font-awesome_fa-floppy-o_1MBo6",
		"fa-square": "font-awesome_fa-square_N1IJZ",
		"fa-navicon": "font-awesome_fa-navicon_3anpJ",
		"fa-reorder": "font-awesome_fa-reorder_2ukY7",
		"fa-bars": "font-awesome_fa-bars_3WARK",
		"fa-list-ul": "font-awesome_fa-list-ul_3s6_2",
		"fa-list-ol": "font-awesome_fa-list-ol_AP-DO",
		"fa-strikethrough": "font-awesome_fa-strikethrough_h0-a_",
		"fa-underline": "font-awesome_fa-underline_2PIFp",
		"fa-table": "font-awesome_fa-table_2mEeT",
		"fa-magic": "font-awesome_fa-magic_qWQg_",
		"fa-truck": "font-awesome_fa-truck_1AsFs",
		"fa-pinterest": "font-awesome_fa-pinterest_1xKnl",
		"fa-pinterest-square": "font-awesome_fa-pinterest-square_3Yhwf",
		"fa-google-plus-square": "font-awesome_fa-google-plus-square_90VGD",
		"fa-google-plus": "font-awesome_fa-google-plus_1Tp-z",
		"fa-money": "font-awesome_fa-money_32Lir",
		"fa-caret-down": "font-awesome_fa-caret-down_1crEO",
		"fa-caret-up": "font-awesome_fa-caret-up_2TwZv",
		"fa-caret-left": "font-awesome_fa-caret-left_39lOf",
		"fa-caret-right": "font-awesome_fa-caret-right_3p0nW",
		"fa-columns": "font-awesome_fa-columns_nToc3",
		"fa-unsorted": "font-awesome_fa-unsorted_2nhbR",
		"fa-sort": "font-awesome_fa-sort_F3dcY",
		"fa-sort-down": "font-awesome_fa-sort-down_3wTbK",
		"fa-sort-desc": "font-awesome_fa-sort-desc_3CQ5e",
		"fa-sort-up": "font-awesome_fa-sort-up_Ad_bv",
		"fa-sort-asc": "font-awesome_fa-sort-asc_3MlT5",
		"fa-envelope": "font-awesome_fa-envelope_3xnLD",
		"fa-linkedin": "font-awesome_fa-linkedin_25eMJ",
		"fa-rotate-left": "font-awesome_fa-rotate-left_3mzU5",
		"fa-undo": "font-awesome_fa-undo_hNldt",
		"fa-legal": "font-awesome_fa-legal_1C_3g",
		"fa-gavel": "font-awesome_fa-gavel_2ttLP",
		"fa-dashboard": "font-awesome_fa-dashboard_3bEM7",
		"fa-tachometer": "font-awesome_fa-tachometer_3R5zx",
		"fa-comment-o": "font-awesome_fa-comment-o_2pEPg",
		"fa-comments-o": "font-awesome_fa-comments-o_hQJKS",
		"fa-flash": "font-awesome_fa-flash_1DU_v",
		"fa-bolt": "font-awesome_fa-bolt_3iT3l",
		"fa-sitemap": "font-awesome_fa-sitemap_QKmtm",
		"fa-umbrella": "font-awesome_fa-umbrella_3fE2k",
		"fa-paste": "font-awesome_fa-paste_3RUtK",
		"fa-clipboard": "font-awesome_fa-clipboard_1Wx9E",
		"fa-lightbulb-o": "font-awesome_fa-lightbulb-o_3MZxy",
		"fa-exchange": "font-awesome_fa-exchange_1cgNj",
		"fa-cloud-download": "font-awesome_fa-cloud-download_2fd-7",
		"fa-cloud-upload": "font-awesome_fa-cloud-upload_BCKnV",
		"fa-user-md": "font-awesome_fa-user-md_3Unw6",
		"fa-stethoscope": "font-awesome_fa-stethoscope_3TPjy",
		"fa-suitcase": "font-awesome_fa-suitcase_2ZK-F",
		"fa-bell-o": "font-awesome_fa-bell-o_3iuFm",
		"fa-coffee": "font-awesome_fa-coffee_2tZxb",
		"fa-cutlery": "font-awesome_fa-cutlery_2dZZ2",
		"fa-file-text-o": "font-awesome_fa-file-text-o_3vkBr",
		"fa-building-o": "font-awesome_fa-building-o_1ML8l",
		"fa-hospital-o": "font-awesome_fa-hospital-o_2dZPM",
		"fa-ambulance": "font-awesome_fa-ambulance_3oMTO",
		"fa-medkit": "font-awesome_fa-medkit_3TuAD",
		"fa-fighter-jet": "font-awesome_fa-fighter-jet_2EPG4",
		"fa-beer": "font-awesome_fa-beer_25HMG",
		"fa-h-square": "font-awesome_fa-h-square_iRMP3",
		"fa-plus-square": "font-awesome_fa-plus-square_28zW8",
		"fa-angle-double-left": "font-awesome_fa-angle-double-left_3Q7bL",
		"fa-angle-double-right": "font-awesome_fa-angle-double-right_2R24L",
		"fa-angle-double-up": "font-awesome_fa-angle-double-up_2GMJK",
		"fa-angle-double-down": "font-awesome_fa-angle-double-down_IlK-a",
		"fa-angle-left": "font-awesome_fa-angle-left_7b-ty",
		"fa-angle-right": "font-awesome_fa-angle-right_RfvDx",
		"fa-angle-up": "font-awesome_fa-angle-up_2xGkU",
		"fa-angle-down": "font-awesome_fa-angle-down_3nIhI",
		"fa-desktop": "font-awesome_fa-desktop_7pHFF",
		"fa-laptop": "font-awesome_fa-laptop_2QHxL",
		"fa-tablet": "font-awesome_fa-tablet_eRAwh",
		"fa-mobile-phone": "font-awesome_fa-mobile-phone_3tGZx",
		"fa-mobile": "font-awesome_fa-mobile_ry_56",
		"fa-circle-o": "font-awesome_fa-circle-o_We1QB",
		"fa-quote-left": "font-awesome_fa-quote-left_tgvF3",
		"fa-quote-right": "font-awesome_fa-quote-right_2LbYu",
		"fa-spinner": "font-awesome_fa-spinner_1FgdF",
		"fa-circle": "font-awesome_fa-circle_RFG4V",
		"fa-mail-reply": "font-awesome_fa-mail-reply_1ovuj",
		"fa-reply": "font-awesome_fa-reply_1p4xy",
		"fa-github-alt": "font-awesome_fa-github-alt_PGZGn",
		"fa-folder-o": "font-awesome_fa-folder-o_28LsO",
		"fa-folder-open-o": "font-awesome_fa-folder-open-o_3Hbbz",
		"fa-smile-o": "font-awesome_fa-smile-o_3R1KH",
		"fa-frown-o": "font-awesome_fa-frown-o_1PJe6",
		"fa-meh-o": "font-awesome_fa-meh-o_1Yal3",
		"fa-gamepad": "font-awesome_fa-gamepad_DQkX5",
		"fa-keyboard-o": "font-awesome_fa-keyboard-o_1Zegg",
		"fa-flag-o": "font-awesome_fa-flag-o_2paT4",
		"fa-flag-checkered": "font-awesome_fa-flag-checkered_3Q50W",
		"fa-terminal": "font-awesome_fa-terminal_1y_ce",
		"fa-code": "font-awesome_fa-code_373HL",
		"fa-mail-reply-all": "font-awesome_fa-mail-reply-all_1el1h",
		"fa-reply-all": "font-awesome_fa-reply-all_1XbQQ",
		"fa-star-half-empty": "font-awesome_fa-star-half-empty_NeM4g",
		"fa-star-half-full": "font-awesome_fa-star-half-full_3_GnR",
		"fa-star-half-o": "font-awesome_fa-star-half-o_1gMSG",
		"fa-location-arrow": "font-awesome_fa-location-arrow_gFy0a",
		"fa-crop": "font-awesome_fa-crop_DFePA",
		"fa-code-fork": "font-awesome_fa-code-fork_rNRd0",
		"fa-unlink": "font-awesome_fa-unlink_1hw62",
		"fa-chain-broken": "font-awesome_fa-chain-broken_3nVk7",
		"fa-question": "font-awesome_fa-question_EAoIA",
		"fa-info": "font-awesome_fa-info_2cQvQ",
		"fa-exclamation": "font-awesome_fa-exclamation_297uN",
		"fa-superscript": "font-awesome_fa-superscript_N7aMl",
		"fa-subscript": "font-awesome_fa-subscript_ZG4gQ",
		"fa-eraser": "font-awesome_fa-eraser_3NIuU",
		"fa-puzzle-piece": "font-awesome_fa-puzzle-piece_3lKWq",
		"fa-microphone": "font-awesome_fa-microphone_3_81_",
		"fa-microphone-slash": "font-awesome_fa-microphone-slash_1DyxC",
		"fa-shield": "font-awesome_fa-shield_1qKif",
		"fa-calendar-o": "font-awesome_fa-calendar-o_1BLCm",
		"fa-fire-extinguisher": "font-awesome_fa-fire-extinguisher_3gz5K",
		"fa-rocket": "font-awesome_fa-rocket_lfSov",
		"fa-maxcdn": "font-awesome_fa-maxcdn_cD6Fn",
		"fa-chevron-circle-left": "font-awesome_fa-chevron-circle-left_1aac7",
		"fa-chevron-circle-right": "font-awesome_fa-chevron-circle-right_Evj_u",
		"fa-chevron-circle-up": "font-awesome_fa-chevron-circle-up_tTcaI",
		"fa-chevron-circle-down": "font-awesome_fa-chevron-circle-down_1oKtm",
		"fa-html5": "font-awesome_fa-html5_3LZaq",
		"fa-css3": "font-awesome_fa-css3_3hg4c",
		"fa-anchor": "font-awesome_fa-anchor_2-wZ3",
		"fa-unlock-alt": "font-awesome_fa-unlock-alt_CLyLU",
		"fa-bullseye": "font-awesome_fa-bullseye_6Sp1E",
		"fa-ellipsis-h": "font-awesome_fa-ellipsis-h_4VBiE",
		"fa-ellipsis-v": "font-awesome_fa-ellipsis-v_Ktjfe",
		"fa-rss-square": "font-awesome_fa-rss-square_4Vj2y",
		"fa-play-circle": "font-awesome_fa-play-circle_ECzau",
		"fa-ticket": "font-awesome_fa-ticket_284VQ",
		"fa-minus-square": "font-awesome_fa-minus-square_3w_Do",
		"fa-minus-square-o": "font-awesome_fa-minus-square-o_qe1Jq",
		"fa-level-up": "font-awesome_fa-level-up_7RnC1",
		"fa-level-down": "font-awesome_fa-level-down_1rR4Q",
		"fa-check-square": "font-awesome_fa-check-square_3Qxfb",
		"fa-pencil-square": "font-awesome_fa-pencil-square_3f_4W",
		"fa-external-link-square": "font-awesome_fa-external-link-square_3TfmM",
		"fa-share-square": "font-awesome_fa-share-square_4XEPu",
		"fa-compass": "font-awesome_fa-compass_3kP2n",
		"fa-toggle-down": "font-awesome_fa-toggle-down_vVDIQ",
		"fa-caret-square-o-down": "font-awesome_fa-caret-square-o-down_1Ao-B",
		"fa-toggle-up": "font-awesome_fa-toggle-up_1j96l",
		"fa-caret-square-o-up": "font-awesome_fa-caret-square-o-up_1Lr5P",
		"fa-toggle-right": "font-awesome_fa-toggle-right_391jj",
		"fa-caret-square-o-right": "font-awesome_fa-caret-square-o-right_Jc6ln",
		"fa-euro": "font-awesome_fa-euro_1H752",
		"fa-eur": "font-awesome_fa-eur_2JOH3",
		"fa-gbp": "font-awesome_fa-gbp_sXuSA",
		"fa-dollar": "font-awesome_fa-dollar_1Qw2b",
		"fa-usd": "font-awesome_fa-usd_1Cyf0",
		"fa-rupee": "font-awesome_fa-rupee_3EdPr",
		"fa-inr": "font-awesome_fa-inr_2v4ZE",
		"fa-cny": "font-awesome_fa-cny_3RNlL",
		"fa-rmb": "font-awesome_fa-rmb_vAGyw",
		"fa-yen": "font-awesome_fa-yen_UH2C8",
		"fa-jpy": "font-awesome_fa-jpy_CXaPK",
		"fa-ruble": "font-awesome_fa-ruble_1ms6_",
		"fa-rouble": "font-awesome_fa-rouble_fwC1R",
		"fa-rub": "font-awesome_fa-rub_1c94U",
		"fa-won": "font-awesome_fa-won_1oqxL",
		"fa-krw": "font-awesome_fa-krw_xc7hv",
		"fa-bitcoin": "font-awesome_fa-bitcoin_3h17C",
		"fa-btc": "font-awesome_fa-btc_2EpsK",
		"fa-file": "font-awesome_fa-file_2_TBG",
		"fa-file-text": "font-awesome_fa-file-text_3uzzE",
		"fa-sort-alpha-asc": "font-awesome_fa-sort-alpha-asc_l6x9i",
		"fa-sort-alpha-desc": "font-awesome_fa-sort-alpha-desc_Au5Op",
		"fa-sort-amount-asc": "font-awesome_fa-sort-amount-asc_a4pl1",
		"fa-sort-amount-desc": "font-awesome_fa-sort-amount-desc_sHYze",
		"fa-sort-numeric-asc": "font-awesome_fa-sort-numeric-asc_2fl5U",
		"fa-sort-numeric-desc": "font-awesome_fa-sort-numeric-desc_rZcNd",
		"fa-thumbs-up": "font-awesome_fa-thumbs-up_32LEl",
		"fa-thumbs-down": "font-awesome_fa-thumbs-down_115k7",
		"fa-youtube-square": "font-awesome_fa-youtube-square_1HADK",
		"fa-youtube": "font-awesome_fa-youtube_3PHGN",
		"fa-xing": "font-awesome_fa-xing_2fXmL",
		"fa-xing-square": "font-awesome_fa-xing-square_3AeWb",
		"fa-youtube-play": "font-awesome_fa-youtube-play__uWZW",
		"fa-dropbox": "font-awesome_fa-dropbox_1i2Rn",
		"fa-stack-overflow": "font-awesome_fa-stack-overflow_2tkuN",
		"fa-instagram": "font-awesome_fa-instagram_1lV5f",
		"fa-flickr": "font-awesome_fa-flickr_3JrtG",
		"fa-adn": "font-awesome_fa-adn_3a2Jf",
		"fa-bitbucket": "font-awesome_fa-bitbucket_12Rp4",
		"fa-bitbucket-square": "font-awesome_fa-bitbucket-square_Y0lMx",
		"fa-tumblr": "font-awesome_fa-tumblr_18aB6",
		"fa-tumblr-square": "font-awesome_fa-tumblr-square_3m4ld",
		"fa-long-arrow-down": "font-awesome_fa-long-arrow-down_2His0",
		"fa-long-arrow-up": "font-awesome_fa-long-arrow-up_vP_4l",
		"fa-long-arrow-left": "font-awesome_fa-long-arrow-left_1Uldc",
		"fa-long-arrow-right": "font-awesome_fa-long-arrow-right_1_jZV",
		"fa-apple": "font-awesome_fa-apple_3f0-D",
		"fa-windows": "font-awesome_fa-windows_2wDfa",
		"fa-android": "font-awesome_fa-android_1Wzt9",
		"fa-linux": "font-awesome_fa-linux_3TBYa",
		"fa-dribbble": "font-awesome_fa-dribbble_IliEV",
		"fa-skype": "font-awesome_fa-skype_7ne23",
		"fa-foursquare": "font-awesome_fa-foursquare_52T_Z",
		"fa-trello": "font-awesome_fa-trello_2ChtW",
		"fa-female": "font-awesome_fa-female_q-oMT",
		"fa-male": "font-awesome_fa-male_2PAqV",
		"fa-gittip": "font-awesome_fa-gittip_2fxKq",
		"fa-gratipay": "font-awesome_fa-gratipay_xLz4x",
		"fa-sun-o": "font-awesome_fa-sun-o_3QZ1O",
		"fa-moon-o": "font-awesome_fa-moon-o_ZwK6C",
		"fa-archive": "font-awesome_fa-archive_3FY1-",
		"fa-bug": "font-awesome_fa-bug_20yJn",
		"fa-vk": "font-awesome_fa-vk_1SLN3",
		"fa-weibo": "font-awesome_fa-weibo_3q9BS",
		"fa-renren": "font-awesome_fa-renren_27Rtg",
		"fa-pagelines": "font-awesome_fa-pagelines_3FZd_",
		"fa-stack-exchange": "font-awesome_fa-stack-exchange_1BbmA",
		"fa-arrow-circle-o-right": "font-awesome_fa-arrow-circle-o-right_1lS0I",
		"fa-arrow-circle-o-left": "font-awesome_fa-arrow-circle-o-left_270k0",
		"fa-toggle-left": "font-awesome_fa-toggle-left_q8rS1",
		"fa-caret-square-o-left": "font-awesome_fa-caret-square-o-left_3leFq",
		"fa-dot-circle-o": "font-awesome_fa-dot-circle-o_fRUKP",
		"fa-wheelchair": "font-awesome_fa-wheelchair_2sPWn",
		"fa-vimeo-square": "font-awesome_fa-vimeo-square_1nIhm",
		"fa-turkish-lira": "font-awesome_fa-turkish-lira_1bCbG",
		"fa-try": "font-awesome_fa-try_1Olkg",
		"fa-plus-square-o": "font-awesome_fa-plus-square-o_M6pBY",
		"fa-space-shuttle": "font-awesome_fa-space-shuttle_9kmJU",
		"fa-slack": "font-awesome_fa-slack_1EvN7",
		"fa-envelope-square": "font-awesome_fa-envelope-square_3aqlc",
		"fa-wordpress": "font-awesome_fa-wordpress_2u9e0",
		"fa-openid": "font-awesome_fa-openid_2QLde",
		"fa-institution": "font-awesome_fa-institution_2uHKo",
		"fa-bank": "font-awesome_fa-bank_D8hxY",
		"fa-university": "font-awesome_fa-university_3ECjv",
		"fa-mortar-board": "font-awesome_fa-mortar-board_1em7v",
		"fa-graduation-cap": "font-awesome_fa-graduation-cap_Y0mMc",
		"fa-yahoo": "font-awesome_fa-yahoo_33B-N",
		"fa-google": "font-awesome_fa-google_1QYVJ",
		"fa-reddit": "font-awesome_fa-reddit_bwA4E",
		"fa-reddit-square": "font-awesome_fa-reddit-square_3rRiq",
		"fa-stumbleupon-circle": "font-awesome_fa-stumbleupon-circle_1TPid",
		"fa-stumbleupon": "font-awesome_fa-stumbleupon_14d1U",
		"fa-delicious": "font-awesome_fa-delicious_3rkRQ",
		"fa-digg": "font-awesome_fa-digg_3bIOw",
		"fa-pied-piper-pp": "font-awesome_fa-pied-piper-pp_3j2RG",
		"fa-pied-piper-alt": "font-awesome_fa-pied-piper-alt_3UjUa",
		"fa-drupal": "font-awesome_fa-drupal_WQObj",
		"fa-joomla": "font-awesome_fa-joomla_2UQVh",
		"fa-language": "font-awesome_fa-language_DOnO2",
		"fa-fax": "font-awesome_fa-fax_1SV_d",
		"fa-building": "font-awesome_fa-building_1FVgz",
		"fa-child": "font-awesome_fa-child_2gTU4",
		"fa-paw": "font-awesome_fa-paw_NcsFR",
		"fa-spoon": "font-awesome_fa-spoon_IxNyL",
		"fa-cube": "font-awesome_fa-cube_1Mq1-",
		"fa-cubes": "font-awesome_fa-cubes_1tGnD",
		"fa-behance": "font-awesome_fa-behance_3mdMe",
		"fa-behance-square": "font-awesome_fa-behance-square_5ghK4",
		"fa-steam": "font-awesome_fa-steam_RIwxM",
		"fa-steam-square": "font-awesome_fa-steam-square_2QEJn",
		"fa-recycle": "font-awesome_fa-recycle_-U8tZ",
		"fa-automobile": "font-awesome_fa-automobile_3z3Dw",
		"fa-car": "font-awesome_fa-car_30pca",
		"fa-cab": "font-awesome_fa-cab_DDNE1",
		"fa-taxi": "font-awesome_fa-taxi_22WsM",
		"fa-tree": "font-awesome_fa-tree_3RDTB",
		"fa-spotify": "font-awesome_fa-spotify_3UDVW",
		"fa-deviantart": "font-awesome_fa-deviantart_2ZxWy",
		"fa-soundcloud": "font-awesome_fa-soundcloud_2ALXb",
		"fa-database": "font-awesome_fa-database_1lI0N",
		"fa-file-pdf-o": "font-awesome_fa-file-pdf-o_3kglo",
		"fa-file-word-o": "font-awesome_fa-file-word-o_1UetZ",
		"fa-file-excel-o": "font-awesome_fa-file-excel-o_A4QBn",
		"fa-file-powerpoint-o": "font-awesome_fa-file-powerpoint-o_rrLjs",
		"fa-file-photo-o": "font-awesome_fa-file-photo-o_2UoDO",
		"fa-file-picture-o": "font-awesome_fa-file-picture-o_3Xjli",
		"fa-file-image-o": "font-awesome_fa-file-image-o_2lPT_",
		"fa-file-zip-o": "font-awesome_fa-file-zip-o_2FWRa",
		"fa-file-archive-o": "font-awesome_fa-file-archive-o_2Mk5P",
		"fa-file-sound-o": "font-awesome_fa-file-sound-o_1AcTq",
		"fa-file-audio-o": "font-awesome_fa-file-audio-o_2PC2o",
		"fa-file-movie-o": "font-awesome_fa-file-movie-o_VAP4m",
		"fa-file-video-o": "font-awesome_fa-file-video-o_34mPw",
		"fa-file-code-o": "font-awesome_fa-file-code-o_1tJvu",
		"fa-vine": "font-awesome_fa-vine_26AR6",
		"fa-codepen": "font-awesome_fa-codepen_2F2Jy",
		"fa-jsfiddle": "font-awesome_fa-jsfiddle_pH8-y",
		"fa-life-bouy": "font-awesome_fa-life-bouy_3M9kq",
		"fa-life-buoy": "font-awesome_fa-life-buoy_-dMf6",
		"fa-life-saver": "font-awesome_fa-life-saver_1NRqc",
		"fa-support": "font-awesome_fa-support_6Q01X",
		"fa-life-ring": "font-awesome_fa-life-ring_1x6lZ",
		"fa-circle-o-notch": "font-awesome_fa-circle-o-notch_cWGUO",
		"fa-ra": "font-awesome_fa-ra_2liTj",
		"fa-resistance": "font-awesome_fa-resistance_59oYs",
		"fa-rebel": "font-awesome_fa-rebel_2UIOr",
		"fa-ge": "font-awesome_fa-ge_1f9_K",
		"fa-empire": "font-awesome_fa-empire_3Sw8V",
		"fa-git-square": "font-awesome_fa-git-square_DgHwD",
		"fa-git": "font-awesome_fa-git_1dhi0",
		"fa-y-combinator-square": "font-awesome_fa-y-combinator-square_lfSlT",
		"fa-yc-square": "font-awesome_fa-yc-square_1Qf2g",
		"fa-hacker-news": "font-awesome_fa-hacker-news_CxkYC",
		"fa-tencent-weibo": "font-awesome_fa-tencent-weibo_2-fdG",
		"fa-qq": "font-awesome_fa-qq_1OIck",
		"fa-wechat": "font-awesome_fa-wechat_7Wqz8",
		"fa-weixin": "font-awesome_fa-weixin_2rvXg",
		"fa-send": "font-awesome_fa-send_1PHOy",
		"fa-paper-plane": "font-awesome_fa-paper-plane_1JBzT",
		"fa-send-o": "font-awesome_fa-send-o_1K3Am",
		"fa-paper-plane-o": "font-awesome_fa-paper-plane-o_Am7EP",
		"fa-history": "font-awesome_fa-history_xEiAH",
		"fa-circle-thin": "font-awesome_fa-circle-thin_OCNZt",
		"fa-header": "font-awesome_fa-header_hMELn",
		"fa-paragraph": "font-awesome_fa-paragraph_2r_mD",
		"fa-sliders": "font-awesome_fa-sliders_3eRoo",
		"fa-share-alt": "font-awesome_fa-share-alt_3jAY7",
		"fa-share-alt-square": "font-awesome_fa-share-alt-square_46dVM",
		"fa-bomb": "font-awesome_fa-bomb_1WRhh",
		"fa-soccer-ball-o": "font-awesome_fa-soccer-ball-o_3rmya",
		"fa-futbol-o": "font-awesome_fa-futbol-o_Nqzpi",
		"fa-tty": "font-awesome_fa-tty_3BPj2",
		"fa-binoculars": "font-awesome_fa-binoculars_1vG29",
		"fa-plug": "font-awesome_fa-plug_1Lbxt",
		"fa-slideshare": "font-awesome_fa-slideshare_15ZAf",
		"fa-twitch": "font-awesome_fa-twitch_MNLu3",
		"fa-yelp": "font-awesome_fa-yelp_1c1W7",
		"fa-newspaper-o": "font-awesome_fa-newspaper-o_1ecUe",
		"fa-wifi": "font-awesome_fa-wifi_dQ61U",
		"fa-calculator": "font-awesome_fa-calculator_2q6GV",
		"fa-paypal": "font-awesome_fa-paypal_3lmxL",
		"fa-google-wallet": "font-awesome_fa-google-wallet_2K_aw",
		"fa-cc-visa": "font-awesome_fa-cc-visa_2F8r8",
		"fa-cc-mastercard": "font-awesome_fa-cc-mastercard_T8WQ_",
		"fa-cc-discover": "font-awesome_fa-cc-discover_2QXm7",
		"fa-cc-amex": "font-awesome_fa-cc-amex_2w-j8",
		"fa-cc-paypal": "font-awesome_fa-cc-paypal_gr0Zj",
		"fa-cc-stripe": "font-awesome_fa-cc-stripe_5ubxJ",
		"fa-bell-slash": "font-awesome_fa-bell-slash_PIYu4",
		"fa-bell-slash-o": "font-awesome_fa-bell-slash-o_PTM9c",
		"fa-trash": "font-awesome_fa-trash_-YVpH",
		"fa-copyright": "font-awesome_fa-copyright_3Cj5D",
		"fa-at": "font-awesome_fa-at_b7Ql8",
		"fa-eyedropper": "font-awesome_fa-eyedropper_1rpAm",
		"fa-paint-brush": "font-awesome_fa-paint-brush_3SJFh",
		"fa-birthday-cake": "font-awesome_fa-birthday-cake_-17FP",
		"fa-area-chart": "font-awesome_fa-area-chart_1fTy1",
		"fa-pie-chart": "font-awesome_fa-pie-chart_2TXFj",
		"fa-line-chart": "font-awesome_fa-line-chart_20bFd",
		"fa-lastfm": "font-awesome_fa-lastfm_3sP7Z",
		"fa-lastfm-square": "font-awesome_fa-lastfm-square_3OBza",
		"fa-toggle-off": "font-awesome_fa-toggle-off_2TP0s",
		"fa-toggle-on": "font-awesome_fa-toggle-on_1ud4K",
		"fa-bicycle": "font-awesome_fa-bicycle_r_nn3",
		"fa-bus": "font-awesome_fa-bus_bm6kq",
		"fa-ioxhost": "font-awesome_fa-ioxhost_yWiPs",
		"fa-angellist": "font-awesome_fa-angellist_14KNT",
		"fa-cc": "font-awesome_fa-cc_VsUyp",
		"fa-shekel": "font-awesome_fa-shekel_3RcTu",
		"fa-sheqel": "font-awesome_fa-sheqel_2_Sde",
		"fa-ils": "font-awesome_fa-ils_CYDSg",
		"fa-meanpath": "font-awesome_fa-meanpath_8Utkv",
		"fa-buysellads": "font-awesome_fa-buysellads_3DmVj",
		"fa-connectdevelop": "font-awesome_fa-connectdevelop_24BDl",
		"fa-dashcube": "font-awesome_fa-dashcube_3gytt",
		"fa-forumbee": "font-awesome_fa-forumbee_1Xmr9",
		"fa-leanpub": "font-awesome_fa-leanpub_1qDwq",
		"fa-sellsy": "font-awesome_fa-sellsy_w39BK",
		"fa-shirtsinbulk": "font-awesome_fa-shirtsinbulk_3ht1E",
		"fa-simplybuilt": "font-awesome_fa-simplybuilt_1V2xv",
		"fa-skyatlas": "font-awesome_fa-skyatlas_1HFEf",
		"fa-cart-plus": "font-awesome_fa-cart-plus_zqpg9",
		"fa-cart-arrow-down": "font-awesome_fa-cart-arrow-down_vmvAL",
		"fa-diamond": "font-awesome_fa-diamond_2YKSj",
		"fa-ship": "font-awesome_fa-ship_2d0Uf",
		"fa-user-secret": "font-awesome_fa-user-secret_1JgJF",
		"fa-motorcycle": "font-awesome_fa-motorcycle_hAqgH",
		"fa-street-view": "font-awesome_fa-street-view_3xS1E",
		"fa-heartbeat": "font-awesome_fa-heartbeat_3SRsO",
		"fa-venus": "font-awesome_fa-venus_3jRFX",
		"fa-mars": "font-awesome_fa-mars_2Le0W",
		"fa-mercury": "font-awesome_fa-mercury_3-x4u",
		"fa-intersex": "font-awesome_fa-intersex_26r-R",
		"fa-transgender": "font-awesome_fa-transgender_1hS0T",
		"fa-transgender-alt": "font-awesome_fa-transgender-alt_3_fBb",
		"fa-venus-double": "font-awesome_fa-venus-double_30rPd",
		"fa-mars-double": "font-awesome_fa-mars-double_3Xnoh",
		"fa-venus-mars": "font-awesome_fa-venus-mars_2Ptfg",
		"fa-mars-stroke": "font-awesome_fa-mars-stroke_f9_Cu",
		"fa-mars-stroke-v": "font-awesome_fa-mars-stroke-v_1K5K9",
		"fa-mars-stroke-h": "font-awesome_fa-mars-stroke-h_3azEl",
		"fa-neuter": "font-awesome_fa-neuter_1wUaY",
		"fa-genderless": "font-awesome_fa-genderless_3mEtZ",
		"fa-facebook-official": "font-awesome_fa-facebook-official_2NNdf",
		"fa-pinterest-p": "font-awesome_fa-pinterest-p_1Xpu_",
		"fa-whatsapp": "font-awesome_fa-whatsapp_3G2qZ",
		"fa-server": "font-awesome_fa-server_NVGtN",
		"fa-user-plus": "font-awesome_fa-user-plus_1UACc",
		"fa-user-times": "font-awesome_fa-user-times_24FFx",
		"fa-hotel": "font-awesome_fa-hotel_3W6s_",
		"fa-bed": "font-awesome_fa-bed_1XbLs",
		"fa-viacoin": "font-awesome_fa-viacoin_3b4Ln",
		"fa-train": "font-awesome_fa-train_2mIFj",
		"fa-subway": "font-awesome_fa-subway_mahNW",
		"fa-medium": "font-awesome_fa-medium_2UIgR",
		"fa-yc": "font-awesome_fa-yc_2pwL9",
		"fa-y-combinator": "font-awesome_fa-y-combinator_l4_A9",
		"fa-optin-monster": "font-awesome_fa-optin-monster_2Vo1M",
		"fa-opencart": "font-awesome_fa-opencart_2P3qK",
		"fa-expeditedssl": "font-awesome_fa-expeditedssl_1ay3x",
		"fa-battery-4": "font-awesome_fa-battery-4_1qRp1",
		"fa-battery": "font-awesome_fa-battery_1TgW-",
		"fa-battery-full": "font-awesome_fa-battery-full_2fsqT",
		"fa-battery-3": "font-awesome_fa-battery-3_3WHzS",
		"fa-battery-three-quarters": "font-awesome_fa-battery-three-quarters_dBjV8",
		"fa-battery-2": "font-awesome_fa-battery-2_2Pgt2",
		"fa-battery-half": "font-awesome_fa-battery-half_2taE9",
		"fa-battery-1": "font-awesome_fa-battery-1_1R1Ww",
		"fa-battery-quarter": "font-awesome_fa-battery-quarter_1sRcE",
		"fa-battery-0": "font-awesome_fa-battery-0_1zrhu",
		"fa-battery-empty": "font-awesome_fa-battery-empty_2Mn-c",
		"fa-mouse-pointer": "font-awesome_fa-mouse-pointer_DbB5u",
		"fa-i-cursor": "font-awesome_fa-i-cursor_xvyzh",
		"fa-object-group": "font-awesome_fa-object-group_3K3tV",
		"fa-object-ungroup": "font-awesome_fa-object-ungroup_1ylE-",
		"fa-sticky-note": "font-awesome_fa-sticky-note_1dK3l",
		"fa-sticky-note-o": "font-awesome_fa-sticky-note-o_2zvyB",
		"fa-cc-jcb": "font-awesome_fa-cc-jcb_Q7v9N",
		"fa-cc-diners-club": "font-awesome_fa-cc-diners-club_338EC",
		"fa-clone": "font-awesome_fa-clone_2LPS7",
		"fa-balance-scale": "font-awesome_fa-balance-scale_3o2it",
		"fa-hourglass-o": "font-awesome_fa-hourglass-o_15XJL",
		"fa-hourglass-1": "font-awesome_fa-hourglass-1_2iRUs",
		"fa-hourglass-start": "font-awesome_fa-hourglass-start_qhpOV",
		"fa-hourglass-2": "font-awesome_fa-hourglass-2_2V0b5",
		"fa-hourglass-half": "font-awesome_fa-hourglass-half_cF0Po",
		"fa-hourglass-3": "font-awesome_fa-hourglass-3_2-ugV",
		"fa-hourglass-end": "font-awesome_fa-hourglass-end_3l-g6",
		"fa-hourglass": "font-awesome_fa-hourglass_1Ar7q",
		"fa-hand-grab-o": "font-awesome_fa-hand-grab-o_3I7_Y",
		"fa-hand-rock-o": "font-awesome_fa-hand-rock-o_1Tb8S",
		"fa-hand-stop-o": "font-awesome_fa-hand-stop-o_37eq3",
		"fa-hand-paper-o": "font-awesome_fa-hand-paper-o_2dp3p",
		"fa-hand-scissors-o": "font-awesome_fa-hand-scissors-o_tLXdy",
		"fa-hand-lizard-o": "font-awesome_fa-hand-lizard-o_2afn0",
		"fa-hand-spock-o": "font-awesome_fa-hand-spock-o_22lUn",
		"fa-hand-pointer-o": "font-awesome_fa-hand-pointer-o_3EDBr",
		"fa-hand-peace-o": "font-awesome_fa-hand-peace-o_3KVDU",
		"fa-trademark": "font-awesome_fa-trademark_1pZSQ",
		"fa-registered": "font-awesome_fa-registered_2bkiQ",
		"fa-creative-commons": "font-awesome_fa-creative-commons_19SOu",
		"fa-gg": "font-awesome_fa-gg_8EwZk",
		"fa-gg-circle": "font-awesome_fa-gg-circle_ixSHX",
		"fa-tripadvisor": "font-awesome_fa-tripadvisor_3SR4I",
		"fa-odnoklassniki": "font-awesome_fa-odnoklassniki_18Bc_",
		"fa-odnoklassniki-square": "font-awesome_fa-odnoklassniki-square_2tvme",
		"fa-get-pocket": "font-awesome_fa-get-pocket_1kDeB",
		"fa-wikipedia-w": "font-awesome_fa-wikipedia-w_2bnVT",
		"fa-safari": "font-awesome_fa-safari_1d_gp",
		"fa-chrome": "font-awesome_fa-chrome_2lYJX",
		"fa-firefox": "font-awesome_fa-firefox_3G1uV",
		"fa-opera": "font-awesome_fa-opera_2EABz",
		"fa-internet-explorer": "font-awesome_fa-internet-explorer_2e6T2",
		"fa-tv": "font-awesome_fa-tv_pyAzy",
		"fa-television": "font-awesome_fa-television_1MplB",
		"fa-contao": "font-awesome_fa-contao_1BTJ5",
		"fa-500px": "font-awesome_fa-500px_2dpFP",
		"fa-amazon": "font-awesome_fa-amazon_1J6OF",
		"fa-calendar-plus-o": "font-awesome_fa-calendar-plus-o_up6cZ",
		"fa-calendar-minus-o": "font-awesome_fa-calendar-minus-o_2wY7J",
		"fa-calendar-times-o": "font-awesome_fa-calendar-times-o_1jaLQ",
		"fa-calendar-check-o": "font-awesome_fa-calendar-check-o_3xoZC",
		"fa-industry": "font-awesome_fa-industry_3LSV8",
		"fa-map-pin": "font-awesome_fa-map-pin_1mpnW",
		"fa-map-signs": "font-awesome_fa-map-signs_21LXb",
		"fa-map-o": "font-awesome_fa-map-o_1CDpd",
		"fa-map": "font-awesome_fa-map_18QCe",
		"fa-commenting": "font-awesome_fa-commenting_2oYYM",
		"fa-commenting-o": "font-awesome_fa-commenting-o_2BRal",
		"fa-houzz": "font-awesome_fa-houzz_13-hb",
		"fa-vimeo": "font-awesome_fa-vimeo_3vcPv",
		"fa-black-tie": "font-awesome_fa-black-tie_34h9B",
		"fa-fonticons": "font-awesome_fa-fonticons_aNgtF",
		"fa-reddit-alien": "font-awesome_fa-reddit-alien_3f_aH",
		"fa-edge": "font-awesome_fa-edge_3UUWF",
		"fa-credit-card-alt": "font-awesome_fa-credit-card-alt_oOWN1",
		"fa-codiepie": "font-awesome_fa-codiepie_2amwQ",
		"fa-modx": "font-awesome_fa-modx__HnMH",
		"fa-fort-awesome": "font-awesome_fa-fort-awesome_1Pxvs",
		"fa-usb": "font-awesome_fa-usb_2-FsD",
		"fa-product-hunt": "font-awesome_fa-product-hunt_3WqRr",
		"fa-mixcloud": "font-awesome_fa-mixcloud_2e01G",
		"fa-scribd": "font-awesome_fa-scribd_1bAIo",
		"fa-pause-circle": "font-awesome_fa-pause-circle_3wI6c",
		"fa-pause-circle-o": "font-awesome_fa-pause-circle-o_2MdRS",
		"fa-stop-circle": "font-awesome_fa-stop-circle_3aZ6V",
		"fa-stop-circle-o": "font-awesome_fa-stop-circle-o_2oIr6",
		"fa-shopping-bag": "font-awesome_fa-shopping-bag_2mD0w",
		"fa-shopping-basket": "font-awesome_fa-shopping-basket_2ZYTJ",
		"fa-hashtag": "font-awesome_fa-hashtag_1sHh4",
		"fa-bluetooth": "font-awesome_fa-bluetooth_1tJ1-",
		"fa-bluetooth-b": "font-awesome_fa-bluetooth-b_LmWTh",
		"fa-percent": "font-awesome_fa-percent_3jbSX",
		"fa-gitlab": "font-awesome_fa-gitlab_17NxC",
		"fa-wpbeginner": "font-awesome_fa-wpbeginner_12WF2",
		"fa-wpforms": "font-awesome_fa-wpforms_1qO7l",
		"fa-envira": "font-awesome_fa-envira_3VCH-",
		"fa-universal-access": "font-awesome_fa-universal-access_2BAWK",
		"fa-wheelchair-alt": "font-awesome_fa-wheelchair-alt_x86hz",
		"fa-question-circle-o": "font-awesome_fa-question-circle-o_HE6Iy",
		"fa-blind": "font-awesome_fa-blind_2GszD",
		"fa-audio-description": "font-awesome_fa-audio-description_1vruh",
		"fa-volume-control-phone": "font-awesome_fa-volume-control-phone_2-hID",
		"fa-braille": "font-awesome_fa-braille_JZYOH",
		"fa-assistive-listening-systems": "font-awesome_fa-assistive-listening-systems_-MRgD",
		"fa-asl-interpreting": "font-awesome_fa-asl-interpreting_2Czb5",
		"fa-american-sign-language-interpreting": "font-awesome_fa-american-sign-language-interpreting_21O_W",
		"fa-deafness": "font-awesome_fa-deafness_30DXf",
		"fa-hard-of-hearing": "font-awesome_fa-hard-of-hearing_1Mzrx",
		"fa-deaf": "font-awesome_fa-deaf_20y_-",
		"fa-glide": "font-awesome_fa-glide_31K4T",
		"fa-glide-g": "font-awesome_fa-glide-g_2xpqn",
		"fa-signing": "font-awesome_fa-signing_18ve9",
		"fa-sign-language": "font-awesome_fa-sign-language_2kCDJ",
		"fa-low-vision": "font-awesome_fa-low-vision_2-HWe",
		"fa-viadeo": "font-awesome_fa-viadeo_1u1ez",
		"fa-viadeo-square": "font-awesome_fa-viadeo-square_2hftx",
		"fa-snapchat": "font-awesome_fa-snapchat_33pkT",
		"fa-snapchat-ghost": "font-awesome_fa-snapchat-ghost_3Xx5A",
		"fa-snapchat-square": "font-awesome_fa-snapchat-square_1PZbq",
		"fa-pied-piper": "font-awesome_fa-pied-piper_1iXBb",
		"fa-first-order": "font-awesome_fa-first-order_3mduz",
		"fa-yoast": "font-awesome_fa-yoast__hiOs",
		"fa-themeisle": "font-awesome_fa-themeisle_3aDVe",
		"fa-google-plus-circle": "font-awesome_fa-google-plus-circle_oADtd",
		"fa-google-plus-official": "font-awesome_fa-google-plus-official_2cz4z",
		"fa-fa": "font-awesome_fa-fa_230yG",
		"fa-font-awesome": "font-awesome_fa-font-awesome_2p-G4",
		"fa-handshake-o": "font-awesome_fa-handshake-o_1BQgE",
		"fa-envelope-open": "font-awesome_fa-envelope-open_nzht3",
		"fa-envelope-open-o": "font-awesome_fa-envelope-open-o_1gQ7U",
		"fa-linode": "font-awesome_fa-linode_1CKdY",
		"fa-address-book": "font-awesome_fa-address-book_1DopX",
		"fa-address-book-o": "font-awesome_fa-address-book-o_PVnwl",
		"fa-vcard": "font-awesome_fa-vcard_1mytB",
		"fa-address-card": "font-awesome_fa-address-card_10TH1",
		"fa-vcard-o": "font-awesome_fa-vcard-o_2z061",
		"fa-address-card-o": "font-awesome_fa-address-card-o_ZAnUS",
		"fa-user-circle": "font-awesome_fa-user-circle_eFX66",
		"fa-user-circle-o": "font-awesome_fa-user-circle-o_2CCV4",
		"fa-user-o": "font-awesome_fa-user-o_3pkn4",
		"fa-id-badge": "font-awesome_fa-id-badge_B-CxE",
		"fa-drivers-license": "font-awesome_fa-drivers-license_YQC6r",
		"fa-id-card": "font-awesome_fa-id-card_2YGHP",
		"fa-drivers-license-o": "font-awesome_fa-drivers-license-o_2kniu",
		"fa-id-card-o": "font-awesome_fa-id-card-o_377Os",
		"fa-quora": "font-awesome_fa-quora_1ygDM",
		"fa-free-code-camp": "font-awesome_fa-free-code-camp_2oc8W",
		"fa-telegram": "font-awesome_fa-telegram_rrK8w",
		"fa-thermometer-4": "font-awesome_fa-thermometer-4_17QiM",
		"fa-thermometer": "font-awesome_fa-thermometer_2CY6f",
		"fa-thermometer-full": "font-awesome_fa-thermometer-full_MmoVG",
		"fa-thermometer-3": "font-awesome_fa-thermometer-3_1K5SW",
		"fa-thermometer-three-quarters": "font-awesome_fa-thermometer-three-quarters_Z2c0u",
		"fa-thermometer-2": "font-awesome_fa-thermometer-2_t0LRT",
		"fa-thermometer-half": "font-awesome_fa-thermometer-half_R314u",
		"fa-thermometer-1": "font-awesome_fa-thermometer-1_1AYjC",
		"fa-thermometer-quarter": "font-awesome_fa-thermometer-quarter_2vzqc",
		"fa-thermometer-0": "font-awesome_fa-thermometer-0_25pBF",
		"fa-thermometer-empty": "font-awesome_fa-thermometer-empty_3L7rA",
		"fa-shower": "font-awesome_fa-shower_1mf85",
		"fa-bathtub": "font-awesome_fa-bathtub_sWKu2",
		"fa-s15": "font-awesome_fa-s15_3d8v3",
		"fa-bath": "font-awesome_fa-bath_3ZQIh",
		"fa-podcast": "font-awesome_fa-podcast_2bMxn",
		"fa-window-maximize": "font-awesome_fa-window-maximize_3Nkvx",
		"fa-window-minimize": "font-awesome_fa-window-minimize_3hY91",
		"fa-window-restore": "font-awesome_fa-window-restore_3rb68",
		"fa-times-rectangle": "font-awesome_fa-times-rectangle_2WAZ6",
		"fa-window-close": "font-awesome_fa-window-close_3pJ9t",
		"fa-times-rectangle-o": "font-awesome_fa-times-rectangle-o_3fndH",
		"fa-window-close-o": "font-awesome_fa-window-close-o_cWi6f",
		"fa-bandcamp": "font-awesome_fa-bandcamp_1chYL",
		"fa-grav": "font-awesome_fa-grav_3rCDC",
		"fa-etsy": "font-awesome_fa-etsy_1ijNc",
		"fa-imdb": "font-awesome_fa-imdb_1UKQ-",
		"fa-ravelry": "font-awesome_fa-ravelry_2Mh85",
		"fa-eercast": "font-awesome_fa-eercast_2kOfj",
		"fa-microchip": "font-awesome_fa-microchip_3-A90",
		"fa-snowflake-o": "font-awesome_fa-snowflake-o__2tjp",
		"fa-superpowers": "font-awesome_fa-superpowers_3XIZD",
		"fa-wpexplorer": "font-awesome_fa-wpexplorer_3Mqmq",
		"fa-meetup": "font-awesome_fa-meetup_2IXst",
		"sr-only": "font-awesome_sr-only_3QD-2",
		"sr-only-focusable": "font-awesome_sr-only-focusable_3JKtw"
	};

/***/ },
/* 34 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "674f50d287a8c48dc19ba404d20fe713.eot";

/***/ },
/* 35 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "674f50d287a8c48dc19ba404d20fe713.eot";

/***/ },
/* 36 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "af7ae505a9eed503f8b8e6982036873e.woff2";

/***/ },
/* 37 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "fee66e712a8a08eef5805a46892932ad.woff";

/***/ },
/* 38 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "b06871f281fee6b241d60582ae9369b9.ttf";

/***/ },
/* 39 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "912ec66d7572ff821749319396470bde.svg";

/***/ },
/* 40 */
/***/ function(module, exports, __webpack_require__) {

	/*
		MIT License http://www.opensource.org/licenses/mit-license.php
		Author Tobias Koppers @sokra
	*/
	var stylesInDom = {},
		memoize = function(fn) {
			var memo;
			return function () {
				if (typeof memo === "undefined") memo = fn.apply(this, arguments);
				return memo;
			};
		},
		isOldIE = memoize(function() {
			return /msie [6-9]\b/.test(window.navigator.userAgent.toLowerCase());
		}),
		getHeadElement = memoize(function () {
			return document.head || document.getElementsByTagName("head")[0];
		}),
		singletonElement = null,
		singletonCounter = 0,
		styleElementsInsertedAtTop = [];

	module.exports = function(list, options) {
		if(false) {
			if(typeof document !== "object") throw new Error("The style-loader cannot be used in a non-browser environment");
		}

		options = options || {};
		// Force single-tag solution on IE6-9, which has a hard limit on the # of <style>
		// tags it will allow on a page
		if (typeof options.singleton === "undefined") options.singleton = isOldIE();

		// By default, add <style> tags to the bottom of <head>.
		if (typeof options.insertAt === "undefined") options.insertAt = "bottom";

		var styles = listToStyles(list);
		addStylesToDom(styles, options);

		return function update(newList) {
			var mayRemove = [];
			for(var i = 0; i < styles.length; i++) {
				var item = styles[i];
				var domStyle = stylesInDom[item.id];
				domStyle.refs--;
				mayRemove.push(domStyle);
			}
			if(newList) {
				var newStyles = listToStyles(newList);
				addStylesToDom(newStyles, options);
			}
			for(var i = 0; i < mayRemove.length; i++) {
				var domStyle = mayRemove[i];
				if(domStyle.refs === 0) {
					for(var j = 0; j < domStyle.parts.length; j++)
						domStyle.parts[j]();
					delete stylesInDom[domStyle.id];
				}
			}
		};
	}

	function addStylesToDom(styles, options) {
		for(var i = 0; i < styles.length; i++) {
			var item = styles[i];
			var domStyle = stylesInDom[item.id];
			if(domStyle) {
				domStyle.refs++;
				for(var j = 0; j < domStyle.parts.length; j++) {
					domStyle.parts[j](item.parts[j]);
				}
				for(; j < item.parts.length; j++) {
					domStyle.parts.push(addStyle(item.parts[j], options));
				}
			} else {
				var parts = [];
				for(var j = 0; j < item.parts.length; j++) {
					parts.push(addStyle(item.parts[j], options));
				}
				stylesInDom[item.id] = {id: item.id, refs: 1, parts: parts};
			}
		}
	}

	function listToStyles(list) {
		var styles = [];
		var newStyles = {};
		for(var i = 0; i < list.length; i++) {
			var item = list[i];
			var id = item[0];
			var css = item[1];
			var media = item[2];
			var sourceMap = item[3];
			var part = {css: css, media: media, sourceMap: sourceMap};
			if(!newStyles[id])
				styles.push(newStyles[id] = {id: id, parts: [part]});
			else
				newStyles[id].parts.push(part);
		}
		return styles;
	}

	function insertStyleElement(options, styleElement) {
		var head = getHeadElement();
		var lastStyleElementInsertedAtTop = styleElementsInsertedAtTop[styleElementsInsertedAtTop.length - 1];
		if (options.insertAt === "top") {
			if(!lastStyleElementInsertedAtTop) {
				head.insertBefore(styleElement, head.firstChild);
			} else if(lastStyleElementInsertedAtTop.nextSibling) {
				head.insertBefore(styleElement, lastStyleElementInsertedAtTop.nextSibling);
			} else {
				head.appendChild(styleElement);
			}
			styleElementsInsertedAtTop.push(styleElement);
		} else if (options.insertAt === "bottom") {
			head.appendChild(styleElement);
		} else {
			throw new Error("Invalid value for parameter 'insertAt'. Must be 'top' or 'bottom'.");
		}
	}

	function removeStyleElement(styleElement) {
		styleElement.parentNode.removeChild(styleElement);
		var idx = styleElementsInsertedAtTop.indexOf(styleElement);
		if(idx >= 0) {
			styleElementsInsertedAtTop.splice(idx, 1);
		}
	}

	function createStyleElement(options) {
		var styleElement = document.createElement("style");
		styleElement.type = "text/css";
		insertStyleElement(options, styleElement);
		return styleElement;
	}

	function createLinkElement(options) {
		var linkElement = document.createElement("link");
		linkElement.rel = "stylesheet";
		insertStyleElement(options, linkElement);
		return linkElement;
	}

	function addStyle(obj, options) {
		var styleElement, update, remove;

		if (options.singleton) {
			var styleIndex = singletonCounter++;
			styleElement = singletonElement || (singletonElement = createStyleElement(options));
			update = applyToSingletonTag.bind(null, styleElement, styleIndex, false);
			remove = applyToSingletonTag.bind(null, styleElement, styleIndex, true);
		} else if(obj.sourceMap &&
			typeof URL === "function" &&
			typeof URL.createObjectURL === "function" &&
			typeof URL.revokeObjectURL === "function" &&
			typeof Blob === "function" &&
			typeof btoa === "function") {
			styleElement = createLinkElement(options);
			update = updateLink.bind(null, styleElement);
			remove = function() {
				removeStyleElement(styleElement);
				if(styleElement.href)
					URL.revokeObjectURL(styleElement.href);
			};
		} else {
			styleElement = createStyleElement(options);
			update = applyToTag.bind(null, styleElement);
			remove = function() {
				removeStyleElement(styleElement);
			};
		}

		update(obj);

		return function updateStyle(newObj) {
			if(newObj) {
				if(newObj.css === obj.css && newObj.media === obj.media && newObj.sourceMap === obj.sourceMap)
					return;
				update(obj = newObj);
			} else {
				remove();
			}
		};
	}

	var replaceText = (function () {
		var textStore = [];

		return function (index, replacement) {
			textStore[index] = replacement;
			return textStore.filter(Boolean).join('\n');
		};
	})();

	function applyToSingletonTag(styleElement, index, remove, obj) {
		var css = remove ? "" : obj.css;

		if (styleElement.styleSheet) {
			styleElement.styleSheet.cssText = replaceText(index, css);
		} else {
			var cssNode = document.createTextNode(css);
			var childNodes = styleElement.childNodes;
			if (childNodes[index]) styleElement.removeChild(childNodes[index]);
			if (childNodes.length) {
				styleElement.insertBefore(cssNode, childNodes[index]);
			} else {
				styleElement.appendChild(cssNode);
			}
		}
	}

	function applyToTag(styleElement, obj) {
		var css = obj.css;
		var media = obj.media;

		if(media) {
			styleElement.setAttribute("media", media)
		}

		if(styleElement.styleSheet) {
			styleElement.styleSheet.cssText = css;
		} else {
			while(styleElement.firstChild) {
				styleElement.removeChild(styleElement.firstChild);
			}
			styleElement.appendChild(document.createTextNode(css));
		}
	}

	function updateLink(linkElement, obj) {
		var css = obj.css;
		var sourceMap = obj.sourceMap;

		if(sourceMap) {
			// http://stackoverflow.com/a/26603875
			css += "\n/*# sourceMappingURL=data:application/json;base64," + btoa(unescape(encodeURIComponent(JSON.stringify(sourceMap)))) + " */";
		}

		var blob = new Blob([css], { type: "text/css" });

		var oldSrc = linkElement.href;

		linkElement.href = URL.createObjectURL(blob);

		if(oldSrc)
			URL.revokeObjectURL(oldSrc);
	}


/***/ },
/* 41 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	exports.default = multiClicker;

	var _d = __webpack_require__(29);

	var _d2 = _interopRequireDefault(_d);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	var DOUBLE_CLICK_TIMEOUT = 300; // win7 default is 500 ms

	/*
	 * Use this function if you need both single-click and double-click handlers on
	 * a node.  If you only need one or the other, simply listen to the 'click' and
	 * 'dblclick' events.  Otherwise, you can use this function as follows:
	 *
	 * d3.select('.someclass').
	 *   on('click', multiClicker([
	 *     function(d, i) { // single click handler
	 *       // do single-click stuff with "d", "i", or "d3.select(this)", as usual
	 *     },
	 *     function(d, i) { // double click handler
	 *       // do double-click stuff with "d", "i", or "d3.select(this)", as usual
	 *     },
	 *   ]));
	 *
	 */
	function multiClicker(handlers) {
	  var timer = null;
	  var singleClick = handlers[0];
	  var doubleClick = handlers[1];
	  var clickEvent = null;

	  return function inner() {
	    var _this = this;

	    clearTimeout(timer);
	    /* eslint-disable prefer-rest-params */
	    var args = Array.prototype.slice.call(arguments, 0);
	    /* eslint-enable prefer-rest-params */
	    if (timer === null) {
	      clickEvent = _d2.default.event;
	      timer = setTimeout(function () {
	        timer = null;
	        _d2.default.event = clickEvent;
	        singleClick.apply(_this, args);
	      }, DOUBLE_CLICK_TIMEOUT);
	    } else {
	      timer = null;
	      doubleClick.apply(this, args);
	    }
	  };
	}

/***/ },
/* 42 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});

	var _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"]) _i["return"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError("Invalid attempt to destructure non-iterable instance"); } }; }();

	exports.default = init;

	var _d2 = __webpack_require__(29);

	var _d3 = _interopRequireDefault(_d2);

	var _deepEquals = __webpack_require__(43);

	var _deepEquals2 = _interopRequireDefault(_deepEquals);

	var _HistogramSelector = __webpack_require__(30);

	var _HistogramSelector2 = _interopRequireDefault(_HistogramSelector);

	var _SelectionBuilder = __webpack_require__(60);

	var _SelectionBuilder2 = _interopRequireDefault(_SelectionBuilder);

	var _AnnotationBuilder = __webpack_require__(61);

	var _AnnotationBuilder2 = _interopRequireDefault(_AnnotationBuilder);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

	// import downArrowImage from './down_arrow.png';

	function init(inPublicAPI, inModel) {
	  var publicAPI = inPublicAPI;
	  var model = inModel;
	  var displayOnlyScored = false;
	  var scorePopupDiv = null;
	  var dividerPopupDiv = null;
	  var dividerValuePopupDiv = null;

	  publicAPI.setScores = function (scores, defaultScore) {
	    // TODO make sure model.scores has the right format?
	    model.scores = scores;
	    model.defaultScore = defaultScore;
	    if (model.scores) {
	      // setup a bgColor
	      model.scores.forEach(function (score, i) {
	        var lightness = _d3.default.hsl(score.color).l;
	        // make bg darker for light colors.
	        var blend = lightness >= 0.45 ? 0.4 : 0.2;
	        var interp = _d3.default.interpolateRgb('#fff', score.color);
	        score.bgColor = interp(blend);
	      });
	    }
	  };

	  function enabled() {
	    return model.scores !== undefined;
	  }

	  if (model.provider.isA('ScoresProvider')) {
	    publicAPI.setScores(model.provider.getScores(), model.provider.getDefaultScore());
	  }

	  function defaultFieldData() {
	    return {
	      annotation: null
	    };
	  }

	  function createDefaultDivider(val, uncert) {
	    return {
	      value: val,
	      uncertainty: uncert
	    };
	  }

	  function getHistRange(def) {
	    var minRange = def.range[0];
	    var maxRange = def.range[1];
	    if (def.hobj) {
	      minRange = def.hobj.min;
	      maxRange = def.hobj.max;
	    }
	    if (minRange === maxRange) maxRange += 1;
	    return [minRange, maxRange];
	  }
	  // add implicit bounds for the histogram min/max to dividers list
	  function getRegionBounds(def) {
	    var _getHistRange = getHistRange(def),
	        _getHistRange2 = _slicedToArray(_getHistRange, 2),
	        minRange = _getHistRange2[0],
	        maxRange = _getHistRange2[1];

	    return [minRange].concat(def.dividers.map(function (div) {
	      return div.value;
	    }), maxRange);
	  }

	  function getUncertScale(def) {
	    // handle a zero range (like from the cumulative score histogram)
	    // const [minRange, maxRange] = getHistRange(def);
	    // return (maxRange - minRange);

	    // We are not going to scale uncertainty - use values in
	    // the same units as the histogram itself
	    return 1.0;
	  }

	  // Translate our dividers and regions into an annotation
	  // suitable for scoring this histogram.
	  function dividersToPartition(def, scores) {
	    if (!def.regions || !def.dividers || !scores) return null;
	    if (def.regions.length !== def.dividers.length + 1) return null;
	    var uncertScale = getUncertScale(def);

	    var partitionSelection = _SelectionBuilder2.default.partition(def.name, def.dividers);
	    partitionSelection.partition.dividers.forEach(function (div, index) {
	      div.uncertainty *= uncertScale;
	    });
	    // console.log('DBG partitionSelection', JSON.stringify(partitionSelection, 2));

	    // Construct a partition annotation:
	    var partitionAnnotation = null;
	    if (def.annotation && !model.provider.shouldCreateNewAnnotation()) {
	      // don't send a new selection unless it's changed.
	      var saveGen = partitionSelection.generation;
	      partitionSelection.generation = def.annotation.selection.generation;
	      var changeSet = { score: def.regions };
	      if (!(0, _deepEquals2.default)(partitionSelection, def.annotation.selection)) {
	        partitionSelection.generation = saveGen;
	        changeSet.selection = partitionSelection;
	      }
	      partitionAnnotation = _AnnotationBuilder2.default.update(def.annotation, { selection: partitionSelection, score: def.regions });
	    } else {
	      partitionAnnotation = _AnnotationBuilder2.default.annotation(partitionSelection, def.regions, 1, '');
	    }
	    _AnnotationBuilder2.default.updateReadOnlyFlag(partitionAnnotation, model.readOnlyFields);
	    return partitionAnnotation;
	  }

	  // retrieve annotation, and re-create dividers and regions
	  function partitionToDividers(scoreData, def, scores) {
	    // console.log('DBG return', JSON.stringify(scoreData, null, 2));
	    var uncertScale = getUncertScale(def);
	    var regions = scoreData.score;
	    var dividers = JSON.parse(JSON.stringify(scoreData.selection.partition.dividers));
	    dividers.forEach(function (div, index) {
	      div.uncertainty *= 1 / uncertScale;
	    });

	    // don't replace the default region with an empty region, so UI can display the default region.
	    if (regions.length > 0 && !(regions.length === 1 && regions[0] === model.defaultScore)) {
	      def.regions = [].concat(regions);
	      def.dividers = dividers;
	    }
	  }

	  // communicate with the server which regions/dividers have changed.
	  function sendScores(def) {
	    var passive = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : false;

	    var scoreData = dividersToPartition(def, model.scores);
	    if (scoreData === null) {
	      console.error('Cannot translate scores to send to provider');
	      return;
	    }
	    if (model.provider.isA('SelectionProvider')) {
	      if (!scoreData.name) {
	        _AnnotationBuilder2.default.setDefaultName(scoreData);
	        if (model.provider.isA('AnnotationStoreProvider')) {
	          scoreData.name = model.provider.getNextStoredAnnotationName(scoreData.name);
	        }
	      }
	      if (!passive) {
	        model.provider.setAnnotation(scoreData);
	      } else if (model.provider.isA('AnnotationStoreProvider') && model.provider.getStoredAnnotation(scoreData.id)) {
	        // Passive means we don't want to set the active annotation, but if there is
	        // a stored annotation matching these score dividers, we still need to update
	        // that stored annotation
	        model.provider.updateStoredAnnotations(_defineProperty({}, scoreData.id, scoreData));
	      }
	    }
	  }

	  function showScore(def) {
	    // show the regions when: editing, or when they are non-default. CSS rule makes visible on hover.
	    return def.editScore || typeof def.regions !== 'undefined' && (def.regions.length > 1 || def.regions[0] !== model.defaultScore);
	  }

	  function setEditScore(def, newEditScore) {
	    def.editScore = newEditScore;
	    // set existing annotation as current if we activate for editing
	    if (def.editScore && showScore(def) && def.annotation) {
	      // TODO special 'active' method to call, instead of an edit?
	      sendScores(def);
	    }
	    publicAPI.render(def.name);
	    // if one histogram is being edited, the others should be inactive.
	    if (newEditScore) {
	      Object.keys(model.fieldData).forEach(function (key) {
	        var d = model.fieldData[key];
	        if (d !== def) {
	          if (d.editScore) {
	            d.editScore = false;
	            publicAPI.render(d.name);
	          }
	        }
	      });
	    }
	  }

	  publicAPI.setDefaultScorePartition = function (fieldName) {
	    var def = model.fieldData[fieldName];
	    if (!def) return;
	    // possibly the best we can do - check for a threshold-like annotation
	    if (!def.lockAnnot || !(def.regions && def.regions.length === 2 && def.regions[0] === 0 && def.regions[1] === 2)) {
	      // create a divider halfway through.
	      var _getHistRange3 = getHistRange(def),
	          _getHistRange4 = _slicedToArray(_getHistRange3, 2),
	          minRange = _getHistRange4[0],
	          maxRange = _getHistRange4[1];

	      def.dividers = [createDefaultDivider(0.5 * (minRange + maxRange), 0)];
	      // set regions to 'no' | 'yes'
	      def.regions = [0, 2];
	      // clear any existing (local) annotation
	      def.annotation = null;
	      // set mode that prevents editing the annotation, except for the single divider.
	      def.lockAnnot = true;
	      sendScores(def);
	    }
	    // we might already have threshold annot, but need to score it.
	    setEditScore(def, true);
	  };

	  publicAPI.getScoreThreshold = function (fieldName) {
	    var def = model.fieldData[fieldName];
	    if (!def.lockAnnot) console.log('Wrong mode for score threshold, arbitrary results.');
	    return def.dividers[0].value;
	  };

	  var scoredHeaderClick = function scoredHeaderClick(d) {
	    displayOnlyScored = !displayOnlyScored;
	    publicAPI.render();
	  };

	  function getDisplayOnlyScored() {
	    return displayOnlyScored;
	  }

	  function numScoreIcons(def) {
	    if (!enabled()) return 0;
	    var count = 0;
	    if (model.provider.getStoredAnnotation && !publicAPI.isFieldActionDisabled(def.name, 'save')) {
	      count += 1;
	    }
	    if (!publicAPI.isFieldActionDisabled(def.name, 'score')) {
	      count += 1;
	    }
	    return count;
	  }

	  function annotationSameAsStored(annotation) {
	    var storedAnnot = model.provider.getStoredAnnotation(annotation.id);
	    if (!storedAnnot) return false;
	    if (annotation.generation === storedAnnot.generation) return true;
	    var savedGen = annotation.generation;
	    annotation.generation = storedAnnot.generation;
	    var ret = (0, _deepEquals2.default)(annotation, storedAnnot);
	    annotation.generation = savedGen;
	    return ret;
	  }

	  function createScoreIcons(iconCell) {
	    if (!enabled()) return;
	    // create/save partition annotation
	    if (model.provider.getStoredAnnotation) {
	      iconCell.append('i').classed(_HistogramSelector2.default.noSaveIcon, true).on('click', function (d) {
	        if (model.provider.getStoredAnnotation) {
	          var annotation = d.annotation;
	          var isSame = annotationSameAsStored(annotation);
	          if (!isSame) {
	            model.provider.setStoredAnnotation(annotation.id, annotation);
	          } else {
	            model.provider.setAnnotation(annotation);
	          }
	          publicAPI.render(d.name);
	          if (_d3.default.event) _d3.default.event.stopPropagation();
	        }
	      });
	    }

	    // start/stop scoring
	    iconCell.append('i').classed(_HistogramSelector2.default.scoreStartIcon, true).on('click', function (d) {
	      setEditScore(d, !d.editScore);
	      if (_d3.default.event) _d3.default.event.stopPropagation();
	    });
	  }

	  function updateScoreIcons(iconCell, def) {
	    if (!enabled()) return;

	    if (model.provider.getStoredAnnotation) {
	      // new/modified/unmodified annotation...
	      if (def.annotation) {
	        if (model.provider.getStoredAnnotation(def.annotation.id)) {
	          var isSame = annotationSameAsStored(def.annotation);
	          if (isSame) {
	            var isActive = def.annotation === model.provider.getAnnotation();
	            iconCell.select('.' + _HistogramSelector2.default.jsSaveIcon).attr('class', isActive ? _HistogramSelector2.default.unchangedActiveSaveIcon : _HistogramSelector2.default.unchangedSaveIcon);
	          } else {
	            iconCell.select('.' + _HistogramSelector2.default.jsSaveIcon).attr('class', _HistogramSelector2.default.modifiedSaveIcon);
	          }
	        } else {
	          iconCell.select('.' + _HistogramSelector2.default.jsSaveIcon).attr('class', _HistogramSelector2.default.newSaveIcon);
	        }
	      } else {
	        iconCell.select('.' + _HistogramSelector2.default.jsSaveIcon).attr('class', _HistogramSelector2.default.noSaveIcon);
	      }
	    }

	    iconCell.select('.' + _HistogramSelector2.default.jsScoreIcon).attr('class', def.editScore ? _HistogramSelector2.default.scoreEndIcon : _HistogramSelector2.default.scoreStartIcon);

	    // Override icon if disabled
	    if (publicAPI.isFieldActionDisabled(def.name, 'save')) {
	      iconCell.select('.' + _HistogramSelector2.default.jsSaveIcon).attr('class', _HistogramSelector2.default.noSaveIcon);
	    }

	    if (publicAPI.isFieldActionDisabled(def.name, 'score')) {
	      iconCell.select('.' + _HistogramSelector2.default.jsScoreIcon).attr('class', _HistogramSelector2.default.hideScoreIcon);
	    }
	  }

	  function createGroups(svgGr) {
	    // scoring interface background group, must be behind.
	    svgGr.insert('g', ':first-child').classed(_HistogramSelector2.default.jsScoreBackground, true);
	    svgGr.append('g').classed(_HistogramSelector2.default.score, true);
	  }

	  function createHeader(header) {
	    if (enabled()) {
	      header.append('span').on('click', scoredHeaderClick).append('i').classed(_HistogramSelector2.default.jsShowScoredIcon, true);
	      header.append('span').classed(_HistogramSelector2.default.jsScoredHeader, true).text('Only Scored').on('click', scoredHeaderClick);
	    }
	  }

	  function updateHeader() {
	    if (enabled()) {
	      _d3.default.select(model.container).select('.' + _HistogramSelector2.default.jsShowScoredIcon)
	      // apply class - 'false' should come first to not remove common base class.
	      .classed(getDisplayOnlyScored() ? _HistogramSelector2.default.allScoredIcon : _HistogramSelector2.default.onlyScoredIcon, false).classed(!getDisplayOnlyScored() ? _HistogramSelector2.default.allScoredIcon : _HistogramSelector2.default.onlyScoredIcon, true);
	    }
	  }

	  function createDragDivider(hitIndex, val, def, hobj) {
	    var dragD = null;
	    if (hitIndex >= 0) {
	      // start modifying existing divider
	      // it becomes a temporary copy if we go outside our bounds
	      dragD = { index: hitIndex,
	        newDivider: createDefaultDivider(undefined, def.dividers[hitIndex].uncertainty),
	        savedUncert: def.dividers[hitIndex].uncertainty,
	        low: hitIndex === 0 ? hobj.min : def.dividers[hitIndex - 1].value,
	        high: hitIndex === def.dividers.length - 1 ? hobj.max : def.dividers[hitIndex + 1].value
	      };
	    } else {
	      // create a temp divider to render.
	      dragD = { index: -1,
	        newDivider: createDefaultDivider(val, 0),
	        savedUncert: 0,
	        low: hobj.min,
	        high: hobj.max
	      };
	    }
	    return dragD;
	  }

	  // enforce that divider uncertainties can't overlap.
	  // Look at neighboring dividers for boundaries on this divider's uncertainty.
	  function clampDividerUncertainty(val, def, hitIndex, currentUncertainty) {
	    if (hitIndex < 0) return currentUncertainty;

	    var _getHistRange5 = getHistRange(def),
	        _getHistRange6 = _slicedToArray(_getHistRange5, 2),
	        minRange = _getHistRange6[0],
	        maxRange = _getHistRange6[1];

	    var maxUncertainty = 0.5 * (maxRange - minRange);
	    var uncertScale = getUncertScale(def);
	    // Note comparison with low/high divider is signed. If val indicates divider has been
	    // moved _past_ the neighboring divider, low/high will be negative.
	    if (hitIndex > 0) {
	      var low = def.dividers[hitIndex - 1].value + def.dividers[hitIndex - 1].uncertainty * uncertScale;
	      maxUncertainty = Math.min(maxUncertainty, (val - low) / uncertScale);
	    }
	    if (hitIndex < def.dividers.length - 1) {
	      var high = def.dividers[hitIndex + 1].value - def.dividers[hitIndex + 1].uncertainty * uncertScale;
	      maxUncertainty = Math.min((high - val) / uncertScale, maxUncertainty);
	    }
	    // make sure uncertainty is zero when val has passed a neighbor.
	    maxUncertainty = Math.max(maxUncertainty, 0);
	    return Math.min(maxUncertainty, currentUncertainty);
	  }

	  // clamp the drag divider specifically
	  function clampDragDividerUncertainty(val, def) {
	    if (def.dragDivider.index < 0) return;

	    def.dragDivider.newDivider.uncertainty = clampDividerUncertainty(val, def, def.dragDivider.index, def.dragDivider.savedUncert);
	    def.dividers[def.dragDivider.index].uncertainty = def.dragDivider.newDivider.uncertainty;
	  }

	  function moveDragDivider(val, def) {
	    if (!def.dragDivider) return;
	    if (def.dragDivider.index >= 0) {
	      // if we drag outside our bounds, make this a 'temporary' extra divider.
	      if (val < def.dragDivider.low) {
	        def.dragDivider.newDivider.value = val;
	        def.dividers[def.dragDivider.index].value = def.dragDivider.low;
	        clampDragDividerUncertainty(val, def);
	        def.dividers[def.dragDivider.index].uncertainty = 0;
	      } else if (val > def.dragDivider.high) {
	        def.dragDivider.newDivider.value = val;
	        def.dividers[def.dragDivider.index].value = def.dragDivider.high;
	        clampDragDividerUncertainty(val, def);
	        def.dividers[def.dragDivider.index].uncertainty = 0;
	      } else {
	        def.dividers[def.dragDivider.index].value = val;
	        clampDragDividerUncertainty(val, def);
	        def.dragDivider.newDivider.value = undefined;
	      }
	    } else {
	      def.dragDivider.newDivider.value = val;
	    }
	  }

	  // create a sorting helper method, to sort dividers based on div.value
	  // D3 bug: just an accessor didn't work - must use comparator function
	  var bisectDividers = _d3.default.bisector(function (a, b) {
	    return a.value - b.value;
	  }).left;

	  // where are we (to the left of) in the divider list?
	  // Did we hit one?
	  function dividerPick(overCoords, def, marginPx, minVal) {
	    var val = def.xScale.invert(overCoords[0]);
	    var index = bisectDividers(def.dividers, createDefaultDivider(val));
	    var hitIndex = -1;
	    if (def.dividers.length > 0) {
	      if (index === 0) {
	        hitIndex = 0;
	      } else if (index === def.dividers.length) {
	        hitIndex = index - 1;
	      } else {
	        hitIndex = def.dividers[index].value - val < val - def.dividers[index - 1].value ? index : index - 1;
	      }
	      var margin = def.xScale.invert(marginPx) - minVal;
	      // don't pick a divider outside the bounds of the histogram - pick the last region.
	      if (Math.abs(def.dividers[hitIndex].value - val) > margin || val < def.hobj.min || val > def.hobj.max) {
	        // we weren't close enough...
	        hitIndex = -1;
	      }
	    }
	    return [val, index, hitIndex];
	  }

	  function regionPick(overCoords, def, hobj) {
	    if (def.dividers.length === 0 || def.regions.length <= 1) return 0;
	    var val = def.xScale.invert(overCoords[0]);
	    var hitIndex = bisectDividers(def.dividers, createDefaultDivider(val));
	    return hitIndex;
	  }

	  function finishDivider(def, hobj) {
	    var forceDelete = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : false;

	    if (!def.dragDivider) return;
	    var val = def.dragDivider.newDivider.value;
	    // if val is defined, we moved an existing divider inside
	    // its region, and we just need to render. Otherwise...
	    if (val !== undefined || forceDelete) {
	      // drag 30 pixels out of the hist to delete.
	      var dragOut = def.xScale.invert(30) - hobj.min;
	      if (!def.lockAnnot && (forceDelete || val < hobj.min - dragOut || val > hobj.max + dragOut)) {
	        if (def.dragDivider.index >= 0) {
	          // delete a region.
	          if (forceDelete || def.dividers[def.dragDivider.index].value === def.dragDivider.high) {
	            def.regions.splice(def.dragDivider.index + 1, 1);
	          } else {
	            def.regions.splice(def.dragDivider.index, 1);
	          }
	          // delete the divider.
	          def.dividers.splice(def.dragDivider.index, 1);
	        }
	      } else {
	        // adding a divider, we make a new region.
	        var replaceRegion = true;
	        // if we moved a divider, delete the old region, unless it's one of the edge regions - those persist.
	        if (def.dragDivider.index >= 0) {
	          if (def.dividers[def.dragDivider.index].value === def.dragDivider.low && def.dragDivider.low !== hobj.min) {
	            def.regions.splice(def.dragDivider.index, 1);
	          } else if (def.dividers[def.dragDivider.index].value === def.dragDivider.high && def.dragDivider.high !== hobj.max) {
	            def.regions.splice(def.dragDivider.index + 1, 1);
	          } else {
	            replaceRegion = false;
	          }
	          // delete the old divider
	          def.dividers.splice(def.dragDivider.index, 1);
	        }
	        // add a new divider
	        def.dragDivider.newDivider.value = Math.min(hobj.max, Math.max(hobj.min, val));
	        // find the index based on dividers sorted by divider.value
	        var index = bisectDividers(def.dividers, def.dragDivider.newDivider);
	        def.dividers.splice(index, 0, def.dragDivider.newDivider);
	        // add a new region if needed, copies the score of existing region.
	        if (replaceRegion) {
	          def.regions.splice(index, 0, def.regions[index]);
	        }
	      }
	    } else if (def.dragDivider.index >= 0 && def.dividers[def.dragDivider.index].uncertainty !== def.dragDivider.newDivider.uncertainty) {
	      def.dividers[def.dragDivider.index].uncertainty = def.dragDivider.newDivider.uncertainty;
	    }
	    // make sure uncertainties don't overlap.
	    def.dividers.forEach(function (divider, index) {
	      divider.uncertainty = clampDividerUncertainty(divider.value, def, index, divider.uncertainty);
	    });
	    sendScores(def);
	    def.dragDivider = undefined;
	  }

	  function positionPopup(popupDiv, left, top) {
	    var clientRect = model.listContainer.getBoundingClientRect();
	    var popupRect = popupDiv.node().getBoundingClientRect();
	    if (popupRect.width + left > clientRect.width) {
	      popupDiv.style('left', 'auto');
	      popupDiv.style('right', 0);
	    } else {
	      popupDiv.style('right', null);
	      popupDiv.style('left', left + 'px');
	    }

	    if (popupRect.height + top > clientRect.height) {
	      popupDiv.style('top', 'auto');
	      popupDiv.style('bottom', 0);
	    } else {
	      popupDiv.style('bottom', null);
	      popupDiv.style('top', top + 'px');
	    }
	  }

	  function validateDividerVal(n) {
	    // is it a finite float number?
	    return !isNaN(parseFloat(n)) && isFinite(n);
	  }

	  function showDividerPopup(dPopupDiv, selectedDef, hobj, coord) {
	    var topMargin = 4;
	    var rowHeight = 28;
	    // 's' SI unit label won't work for a number entry field.
	    var formatter = _d3.default.format('.4g');
	    var uncertDispScale = 1; // was 100 for uncertainty as a %.

	    dPopupDiv.style('display', null);
	    positionPopup(dPopupDiv, coord[0] - topMargin - 0.5 * rowHeight, coord[1] + model.headerSize - (topMargin + 2 * rowHeight));

	    var selDivider = selectedDef.dividers[selectedDef.dragDivider.index];
	    var savedVal = selDivider.value;
	    selectedDef.dragDivider.savedUncert = selDivider.uncertainty;
	    dPopupDiv.on('mouseleave', function () {
	      if (selectedDef.dragDivider) {
	        moveDragDivider(savedVal, selectedDef);
	        finishDivider(selectedDef, hobj);
	      }
	      dPopupDiv.style('display', 'none');
	      selectedDef.dragDivider = undefined;
	      publicAPI.render(selectedDef.name);
	    });
	    var uncertInput = dPopupDiv.select('.' + _HistogramSelector2.default.jsDividerUncertaintyInput);
	    var valInput = dPopupDiv.select('.' + _HistogramSelector2.default.jsDividerValueInput).attr('value', formatter(selDivider.value)).property('value', formatter(selDivider.value)).on('input', function () {
	      // typing values, show feedback.
	      var val = _d3.default.event.target.value;
	      if (!validateDividerVal(val)) val = savedVal;
	      moveDragDivider(val, selectedDef);
	      uncertInput.property('value', formatter(uncertDispScale * selectedDef.dragDivider.newDivider.uncertainty));
	      publicAPI.render(selectedDef.name);
	    }).on('change', function () {
	      // committed to a value, show feedback.
	      var val = _d3.default.event.target.value;
	      if (!validateDividerVal(val)) val = savedVal;else {
	        val = Math.min(hobj.max, Math.max(hobj.min, val));
	        _d3.default.event.target.value = val;
	        savedVal = val;
	      }
	      moveDragDivider(val, selectedDef);
	      publicAPI.render(selectedDef.name);
	    }).on('keyup', function () {
	      // revert to last committed value
	      if (_d3.default.event.key === 'Escape') {
	        moveDragDivider(savedVal, selectedDef);
	        dPopupDiv.on('mouseleave')();
	      } else if (_d3.default.event.key === 'Enter' || _d3.default.event.key === 'Return') {
	        if (selectedDef.dragDivider) {
	          savedVal = selectedDef.dragDivider.newDivider.value === undefined ? selectedDef.dividers[selectedDef.dragDivider.index].value : selectedDef.dragDivider.newDivider.value;
	        }
	        // commit current value
	        dPopupDiv.on('mouseleave')();
	      }
	    });
	    // initial select/focus so use can immediately change the value.
	    valInput.node().select();
	    valInput.node().focus();

	    uncertInput.attr('value', formatter(uncertDispScale * selDivider.uncertainty)).property('value', formatter(uncertDispScale * selDivider.uncertainty)).on('input', function () {
	      // typing values, show feedback.
	      var uncert = _d3.default.event.target.value;
	      if (!validateDividerVal(uncert)) {
	        if (selectedDef.dragDivider) uncert = selectedDef.dragDivider.savedUncert;
	      } else {
	        uncert /= uncertDispScale;
	      }
	      if (selectedDef.dragDivider) {
	        selectedDef.dragDivider.newDivider.uncertainty = uncert;
	        if (selectedDef.dragDivider.newDivider.value === undefined) {
	          // don't use selDivider, might be out-of-date if the server sent us dividers.
	          selectedDef.dividers[selectedDef.dragDivider.index].uncertainty = uncert;
	        }
	      }
	      publicAPI.render(selectedDef.name);
	    }).on('change', function () {
	      // committed to a value, show feedback.
	      var uncert = _d3.default.event.target.value;
	      if (!validateDividerVal(uncert)) {
	        if (selectedDef.dragDivider) uncert = selectedDef.dragDivider.savedUncert;
	      } else {
	        // uncertainty is a % between 0 and 0.5
	        var _getHistRange7 = getHistRange(selectedDef),
	            _getHistRange8 = _slicedToArray(_getHistRange7, 2),
	            minRange = _getHistRange8[0],
	            maxRange = _getHistRange8[1];

	        uncert = Math.min(0.5 * (maxRange - minRange), Math.max(0, uncert / uncertDispScale));
	        _d3.default.event.target.value = formatter(uncertDispScale * uncert);
	        if (selectedDef.dragDivider) selectedDef.dragDivider.savedUncert = uncert;
	      }
	      if (selectedDef.dragDivider) {
	        selectedDef.dragDivider.newDivider.uncertainty = uncert;
	        if (selectedDef.dragDivider.newDivider.value === undefined) {
	          selectedDef.dividers[selectedDef.dragDivider.index].uncertainty = uncert;
	        }
	      }
	      publicAPI.render(selectedDef.name);
	    }).on('keyup', function () {
	      if (_d3.default.event.key === 'Escape') {
	        if (selectedDef.dragDivider) {
	          selectedDef.dragDivider.newDivider.uncertainty = selectedDef.dragDivider.savedUncert;
	        }
	        dPopupDiv.on('mouseleave')();
	      } else if (_d3.default.event.key === 'Enter' || _d3.default.event.key === 'Return') {
	        if (selectedDef.dragDivider) {
	          selectedDef.dragDivider.savedUncert = selectedDef.dragDivider.newDivider.uncertainty;
	        }
	        dPopupDiv.on('mouseleave')();
	      }
	    }).on('blur', function () {
	      if (selectedDef.dragDivider) {
	        var val = selectedDef.dragDivider.newDivider.value === undefined ? selectedDef.dividers[selectedDef.dragDivider.index].value : selectedDef.dragDivider.newDivider.value;
	        clampDragDividerUncertainty(val, selectedDef);
	        _d3.default.event.target.value = formatter(uncertDispScale * selectedDef.dragDivider.newDivider.uncertainty);
	      }
	      publicAPI.render(selectedDef.name);
	    });
	  }

	  function showDividerValuePopup(dPopupDiv, selectedDef, hobj, coord) {
	    var topMargin = 4;
	    var rowHeight = 28;
	    // 's' SI unit label won't work for a number entry field.
	    var formatter = _d3.default.format('.4g');

	    dPopupDiv.style('display', null);
	    positionPopup(dPopupDiv, coord[0] - topMargin - 0.5 * rowHeight, coord[1] + model.headerSize - (topMargin + 0.5 * rowHeight));

	    var selDivider = selectedDef.dividers[selectedDef.dragDivider.index];
	    var savedVal = selDivider.value;
	    selectedDef.dragDivider.savedUncert = selDivider.uncertainty;
	    dPopupDiv.on('mouseleave', function () {
	      if (selectedDef.dragDivider) {
	        moveDragDivider(savedVal, selectedDef);
	        finishDivider(selectedDef, hobj);
	      }
	      dPopupDiv.style('display', 'none');
	      selectedDef.dragDivider = undefined;
	      publicAPI.render();
	    });
	    var valInput = dPopupDiv.select('.' + _HistogramSelector2.default.jsDividerValueInput).attr('value', formatter(selDivider.value)).property('value', formatter(selDivider.value)).on('input', function () {
	      // typing values, show feedback.
	      var val = _d3.default.event.target.value;
	      if (!validateDividerVal(val)) val = savedVal;
	      moveDragDivider(val, selectedDef);
	      publicAPI.render(selectedDef.name);
	    }).on('change', function () {
	      // committed to a value, show feedback.
	      var val = _d3.default.event.target.value;
	      if (!validateDividerVal(val)) val = savedVal;else {
	        val = Math.min(hobj.max, Math.max(hobj.min, val));
	        _d3.default.event.target.value = val;
	        savedVal = val;
	      }
	      moveDragDivider(val, selectedDef);
	      publicAPI.render(selectedDef.name);
	    }).on('keyup', function () {
	      // revert to last committed value
	      if (_d3.default.event.key === 'Escape') {
	        moveDragDivider(savedVal, selectedDef);
	        dPopupDiv.on('mouseleave')();
	      } else if (_d3.default.event.key === 'Enter' || _d3.default.event.key === 'Return') {
	        // commit current value
	        dPopupDiv.on('mouseleave')();
	      }
	    });
	    // initial select/focus so use can immediately change the value.
	    valInput.node().select();
	    valInput.node().focus();
	  }

	  // Divider editing popup allows changing its value or uncertainty, or deleting it.
	  function createDividerPopup() {
	    var dPopupDiv = _d3.default.select(model.listContainer).append('div').classed(_HistogramSelector2.default.dividerPopup, true).style('display', 'none');
	    var table = dPopupDiv.append('table');
	    var tr1 = table.append('tr');
	    tr1.append('td').classed(_HistogramSelector2.default.popupCell, true).text('Value:');
	    tr1.append('td').classed(_HistogramSelector2.default.popupCell, true).append('input').classed(_HistogramSelector2.default.jsDividerValueInput, true).attr('type', 'number').attr('step', 'any').style('width', '6em');
	    var tr2 = table.append('tr');
	    tr2.append('td').classed(_HistogramSelector2.default.popupCell, true).text('Uncertainty:');
	    tr2.append('td').classed(_HistogramSelector2.default.popupCell, true).append('input').classed(_HistogramSelector2.default.jsDividerUncertaintyInput, true).attr('type', 'number').attr('step', 'any').style('width', '6em');
	    dPopupDiv.append('div').classed(_HistogramSelector2.default.scoreDashSpacer, true);
	    dPopupDiv.append('div').style('text-align', 'center').append('input').classed(_HistogramSelector2.default.scoreButton, true).style('align', 'center').attr('type', 'button').attr('value', 'Delete Divider').on('click', function () {
	      finishDivider(model.selectedDef, model.selectedDef.hobj, true);
	      dPopupDiv.style('display', 'none');
	      publicAPI.render();
	    });
	    return dPopupDiv;
	  }
	  // Divider editing popup allows changing its value, only.
	  function createDividerValuePopup() {
	    var dPopupDiv = _d3.default.select(model.listContainer).append('div').classed(_HistogramSelector2.default.dividerValuePopup, true).style('display', 'none');
	    var table = dPopupDiv.append('table');
	    var tr1 = table.append('tr');
	    tr1.append('td').classed(_HistogramSelector2.default.popupCell, true).text('Value:');
	    tr1.append('td').classed(_HistogramSelector2.default.popupCell, true).append('input').classed(_HistogramSelector2.default.jsDividerValueInput, true).attr('type', 'number').attr('step', 'any').style('width', '6em');
	    return dPopupDiv;
	  }

	  function showScorePopup(sPopupDiv, coord, selRow) {
	    // it seemed like a good idea to use getBoundingClientRect() to determine row height
	    // but it returns all zeros when the popup has been invisible...
	    var topMargin = 4;
	    var rowHeight = 26;

	    sPopupDiv.style('display', null);
	    positionPopup(sPopupDiv, coord[0] - topMargin - 0.6 * rowHeight, coord[1] + model.headerSize - (topMargin + (0.6 + selRow) * rowHeight));

	    sPopupDiv.selectAll('.' + _HistogramSelector2.default.jsScoreLabel).style('background-color', function (d, i) {
	      return i === selRow ? d.bgColor : '#fff';
	    });
	  }

	  function createScorePopup() {
	    var sPopupDiv = _d3.default.select(model.listContainer).append('div').classed(_HistogramSelector2.default.scorePopup, true).style('display', 'none').on('mouseleave', function () {
	      sPopupDiv.style('display', 'none');
	      model.selectedDef.dragDivider = undefined;
	    });
	    // create radio-buttons that allow choosing the score for the selected region
	    var scoreChoices = sPopupDiv.selectAll('.' + _HistogramSelector2.default.jsScoreChoice).data(model.scores);
	    scoreChoices.enter().append('label').classed(_HistogramSelector2.default.scoreLabel, true).text(function (d) {
	      return d.name;
	    }).each(function myLabel(data, index) {
	      // because we use 'each' and re-select the label, need to use parent 'index'
	      // instead of 'i' in the (d, i) => functions below - i is always zero.
	      var label = _d3.default.select(this);
	      label.append('span').classed(_HistogramSelector2.default.scoreSwatch, true).style('background-color', function (d) {
	        return d.color;
	      });
	      label.append('input').classed(_HistogramSelector2.default.scoreChoice, true).attr('name', 'score_choice_rb').attr('type', 'radio').attr('value', function (d) {
	        return d.name;
	      }).property('checked', function (d) {
	        return index === model.defaultScore;
	      }).on('click', function (d) {
	        // use click, not change, so we get notified even when current value is chosen.
	        var def = model.selectedDef;
	        def.regions[def.hitRegionIndex] = index;
	        def.dragDivider = undefined;
	        sPopupDiv.style('display', 'none');
	        sendScores(def);
	        publicAPI.render();
	      });
	    });
	    sPopupDiv.append('div').classed(_HistogramSelector2.default.scoreDashSpacer, true);
	    // create a button for creating a new divider, so we don't require
	    // the invisible alt/ctrl click to create one.
	    sPopupDiv.append('input').classed(_HistogramSelector2.default.scoreButton, true).attr('type', 'button').attr('value', 'New Divider').on('click', function () {
	      finishDivider(model.selectedDef, model.selectedDef.hobj);
	      sPopupDiv.style('display', 'none');
	      publicAPI.render();
	    });
	    return sPopupDiv;
	  }

	  function createPopups() {
	    if (enabled()) {
	      scorePopupDiv = _d3.default.select(model.listContainer).select('.' + _HistogramSelector2.default.jsScorePopup);
	      if (scorePopupDiv.empty()) {
	        scorePopupDiv = createScorePopup();
	      }
	      dividerPopupDiv = _d3.default.select(model.listContainer).select('.' + _HistogramSelector2.default.jsDividerPopup);
	      if (dividerPopupDiv.empty()) {
	        dividerPopupDiv = createDividerPopup();
	      }
	      dividerValuePopupDiv = _d3.default.select(model.listContainer).select('.' + _HistogramSelector2.default.jsDividerValuePopup);
	      if (dividerValuePopupDiv.empty()) {
	        dividerValuePopupDiv = createDividerValuePopup();
	      }
	    }
	  }

	  // when the Histogram1DProvider pushes a new histogram, it may have a new range.
	  // If needed, proportionally scale dividers into the new range.
	  function rescaleDividers(paramName, oldRangeMin, oldRangeMax) {
	    if (model.fieldData[paramName] && model.fieldData[paramName].hobj) {
	      var def = model.fieldData[paramName];
	      // Since we come in here whenever a new histogram gets pushed to the histo
	      // selector, avoid rescaling dividers unless there is actually a partition
	      // annotation on this field.
	      if (showScore(def)) {
	        var hobj = model.fieldData[paramName].hobj;
	        if (hobj.min !== oldRangeMin || hobj.max !== oldRangeMax) {
	          def.dividers.forEach(function (divider, index) {
	            if (oldRangeMax === oldRangeMin) {
	              // space dividers evenly in the middle - i.e. punt.
	              divider.value = (index + 1) / (def.dividers.length + 1) * (hobj.max - hobj.min) + hobj.min;
	            } else {
	              // this set the divider to hobj.min if the new hobj.min === hobj.max.
	              divider.value = (divider.value - oldRangeMin) / (oldRangeMax - oldRangeMin) * (hobj.max - hobj.min) + hobj.min;
	            }
	          });
	          sendScores(def, true);
	        }
	      }
	    }
	  }

	  function editingScore(def) {
	    return def.editScore;
	  }

	  function filterFieldNames(fieldNames) {
	    if (getDisplayOnlyScored()) {
	      // filter for fields that have scores
	      return fieldNames.filter(function (name) {
	        return showScore(model.fieldData[name]);
	      });
	    }
	    return fieldNames;
	  }

	  function prepareItem(def, idx, svgGr, tdsl) {
	    if (!enabled()) return;
	    if (typeof def.dividers === 'undefined') {
	      def.dividers = [];
	      def.regions = [model.defaultScore];
	      def.editScore = false;
	      def.lockAnnot = false;
	    }
	    var hobj = def.hobj;

	    var gScore = svgGr.select('.' + _HistogramSelector2.default.jsScore);
	    var drag = null;
	    if (def.editScore) {
	      // add temp dragged divider, if needed.
	      var dividerData = typeof def.dragDivider !== 'undefined' && def.dragDivider.newDivider.value !== undefined ? def.dividers.concat(def.dragDivider.newDivider) : def.dividers;
	      var dividers = gScore.selectAll('line').data(dividerData);
	      dividers.enter().append('line');
	      dividers.attr('x1', function (d) {
	        return def.xScale(d.value);
	      }).attr('y1', 0).attr('x2', function (d) {
	        return def.xScale(d.value);
	      }).attr('y2', function () {
	        return model.histHeight;
	      }).attr('stroke-width', 1).attr('stroke', 'black');
	      dividers.exit().remove();

	      var uncertScale = getUncertScale(def);
	      var uncertRegions = gScore.selectAll('.' + _HistogramSelector2.default.jsScoreUncertainty).data(dividerData);
	      uncertRegions.enter().append('rect').classed(_HistogramSelector2.default.jsScoreUncertainty, true).attr('rx', 8).attr('ry', 8);
	      uncertRegions.attr('x', function (d) {
	        return def.xScale(d.value - d.uncertainty * uncertScale);
	      }).attr('y', 0)
	      // to get a width, need to start from 'zero' of this scale, which is hobj.min
	      .attr('width', function (d, i) {
	        return def.xScale(hobj.min + 2 * d.uncertainty * uncertScale);
	      }).attr('height', function () {
	        return model.histHeight;
	      }).attr('fill', '#000').attr('opacity', function (d) {
	        return d.uncertainty > 0 ? '0.2' : '0';
	      });
	      uncertRegions.exit().remove();

	      var dragDivLabel = gScore.select('.' + _HistogramSelector2.default.jsScoreDivLabel);
	      if (typeof def.dragDivider !== 'undefined') {
	        if (dragDivLabel.empty()) {
	          dragDivLabel = gScore.append('text').classed(_HistogramSelector2.default.jsScoreDivLabel, true).attr('text-anchor', 'middle').attr('stroke', 'none').attr('background-color', '#fff').attr('dy', '.71em');
	        }
	        var formatter = _d3.default.format('.3s');
	        var divVal = def.dragDivider.newDivider.value !== undefined ? def.dragDivider.newDivider.value : def.dividers[def.dragDivider.index].value;
	        dragDivLabel.text(formatter(divVal)).attr('x', '' + def.xScale(divVal)).attr('y', '' + (model.histHeight + 2));
	      } else if (!dragDivLabel.empty()) {
	        dragDivLabel.remove();
	      }

	      // divider interaction events.
	      // Drag flow: drag a divider inside its current neighbors.
	      // A divider outside its neighbors or a new divider is a temp divider,
	      // added to the end of the list when rendering. Doesn't affect regions that way.
	      drag = _d3.default.behavior.drag().on('dragstart', function () {
	        var overCoords = publicAPI.getMouseCoords(tdsl);

	        var _dividerPick = dividerPick(overCoords, def, model.dragMargin, hobj.min),
	            _dividerPick2 = _slicedToArray(_dividerPick, 3),
	            val = _dividerPick2[0],
	            hitIndex = _dividerPick2[2];

	        if (!def.lockAnnot && (_d3.default.event.sourceEvent.altKey || _d3.default.event.sourceEvent.ctrlKey)) {
	          // create a temp divider to render.
	          def.dragDivider = createDragDivider(-1, val, def, hobj);
	          publicAPI.render();
	        } else if (hitIndex >= 0) {
	          // start dragging existing divider
	          // it becomes a temporary copy if we go outside our bounds
	          def.dragDivider = createDragDivider(hitIndex, undefined, def, hobj);
	          publicAPI.render();
	        }
	      }).on('drag', function () {
	        var overCoords = publicAPI.getMouseCoords(tdsl);
	        if (typeof def.dragDivider === 'undefined' || scorePopupDiv.style('display') !== 'none' || dividerPopupDiv.style('display') !== 'none' || dividerValuePopupDiv.style('display') !== 'none') return;
	        var val = def.xScale.invert(overCoords[0]);
	        moveDragDivider(val, def);
	        publicAPI.render(def.name);
	      }).on('dragend', function () {
	        if (typeof def.dragDivider === 'undefined' || scorePopupDiv.style('display') !== 'none' || dividerPopupDiv.style('display') !== 'none' || dividerValuePopupDiv.style('display') !== 'none') return;
	        finishDivider(def, hobj);
	        publicAPI.render();
	      });
	    } else {
	      gScore.selectAll('line').remove();
	      gScore.selectAll('.' + _HistogramSelector2.default.jsScoreUncertainty).remove();
	    }

	    // score regions
	    // there are implicit bounds at the min and max.
	    var regionBounds = getRegionBounds(def);
	    var scoreRegions = gScore.selectAll('.' + _HistogramSelector2.default.jsScoreRect).data(def.regions);
	    // duplicate background regions are opaque, for a solid bright color.
	    var scoreBgRegions = svgGr.select('.' + _HistogramSelector2.default.jsScoreBackground).selectAll('rect').data(def.regions);
	    var numRegions = def.regions.length;
	    [{ sel: scoreRegions, opacity: 0.2, class: _HistogramSelector2.default.scoreRegionFg }, { sel: scoreBgRegions, opacity: 1.0, class: _HistogramSelector2.default.scoreRegionBg }].forEach(function (reg) {
	      reg.sel.enter().append('rect').classed(reg.class, true);
	      // first and last region should hang 6 pixels over the start/end of the axis.
	      var overhang = 6;
	      reg.sel.attr('x', function (d, i) {
	        return def.xScale(regionBounds[i]) - (i === 0 ? overhang : 0);
	      }).attr('y', def.editScore ? 0 : model.histHeight)
	      // width might be === overhang if a divider is dragged all the way to min/max.
	      .attr('width', function (d, i) {
	        return def.xScale(regionBounds[i + 1]) - def.xScale(regionBounds[i]) + (i === 0 ? overhang : 0) + (i === numRegions - 1 ? overhang : 0);
	      })
	      // extend over the x-axis when editing.
	      .attr('height', def.editScore ? model.histHeight + model.histMargin.bottom - 3 : model.histMargin.bottom - 3).attr('fill', function (d) {
	        return model.scores[d].color;
	      }).attr('opacity', showScore(def) ? reg.opacity : '0');
	      reg.sel.exit().remove();
	    });

	    // invisible overlay to catch mouse events. Sized correctly in HistogramSelector
	    var svgOverlay = svgGr.select('.' + _HistogramSelector2.default.jsOverlay);
	    svgOverlay.on('click.score', function () {
	      // preventDefault() in dragstart didn't help, so watch for altKey or ctrlKey.
	      if (_d3.default.event.defaultPrevented || _d3.default.event.altKey || _d3.default.event.ctrlKey) return; // click suppressed (by drag handling)
	      var overCoords = publicAPI.getMouseCoords(tdsl);
	      if (overCoords[1] > model.histHeight) {
	        // def.editScore = !def.editScore;
	        // svgOverlay.style('cursor', def.editScore ? `url(${downArrowImage}) 12 22, auto` : 'pointer');
	        // if (def.editScore && model.provider.isA('HistogramBinHoverProvider')) {
	        //   const state = {};
	        //   state[def.name] = [-1];
	        //   model.provider.setHoverState({ state });
	        // }
	        // // set existing annotation as current if we activate for editing
	        // if (def.editScore && showScore(def) && def.annotation) {
	        //   // TODO special 'active' method to call, instead of an edit?
	        //   sendScores(def);
	        // }
	        // publicAPI.render(def.name);
	        return;
	      }
	      if (def.editScore) {
	        // if we didn't create or drag a divider, pick a region or divider
	        var hitRegionIndex = regionPick(overCoords, def, hobj);
	        // select a def, show popup.
	        def.hitRegionIndex = hitRegionIndex;
	        // create a temp divider in case we choose 'new |' from the popup.

	        var _dividerPick3 = dividerPick(overCoords, def, model.dragMargin, hobj.min),
	            _dividerPick4 = _slicedToArray(_dividerPick3, 3),
	            val = _dividerPick4[0],
	            hitIndex = _dividerPick4[2];

	        var coord = _d3.default.mouse(model.listContainer);
	        model.selectedDef = def;
	        if (hitIndex >= 0) {
	          // pick an existing divider, popup to edit value, uncertainty, or delete.
	          def.dragDivider = createDragDivider(hitIndex, undefined, def, hobj);
	          if (!def.lockAnnot) showDividerPopup(dividerPopupDiv, model.selectedDef, hobj, coord);else showDividerValuePopup(dividerValuePopupDiv, model.selectedDef, hobj, coord);
	        } else if (!def.lockAnnot) {
	          if (typeof def.dragDivider === 'undefined') {
	            def.dragDivider = createDragDivider(-1, val, def, hobj);
	          } else {
	            console.log('Internal: unexpected existing divider');
	            def.dragDivider.newDivider.value = val;
	          }

	          var selRow = def.regions[def.hitRegionIndex];
	          showScorePopup(scorePopupDiv, coord, selRow);
	        }
	      }
	    }).on('mousemove.score', function () {
	      var overCoords = publicAPI.getMouseCoords(tdsl);
	      if (def.editScore) {
	        var _dividerPick5 = dividerPick(overCoords, def, model.dragMargin, hobj.min),
	            _dividerPick6 = _slicedToArray(_dividerPick5, 3),
	            hitIndex = _dividerPick6[2];

	        var cursor = 'pointer';
	        // if we're over the bottom, indicate a click will shrink regions
	        if (overCoords[1] > model.histHeight) {// cursor = `url(${downArrowImage}) 12 22, auto`;
	          // if we're over a divider, indicate drag-to-move
	        } else if (def.dragIndex >= 0 || hitIndex >= 0) cursor = 'ew-resize';
	        // if modifiers are held down, we'll create a divider
	        else if (_d3.default.event.altKey || _d3.default.event.ctrlKey) cursor = 'crosshair';
	        svgOverlay.style('cursor', cursor);
	      } else {
	        // over the bottom, indicate we can start editing regions
	        var pickIt = overCoords[1] > model.histHeight;
	        svgOverlay.style('cursor', pickIt ? 'pointer' : 'default');
	      }
	    });
	    if (def.editScore) {
	      svgOverlay.call(drag);
	    } else {
	      svgOverlay.on('.drag', null);
	    }
	  }

	  function addSubscriptions() {
	    if (model.provider.isA('SelectionProvider')) {
	      model.subscriptions.push(model.provider.onAnnotationChange(function (annotation) {
	        if (annotation.selection.type === 'partition') {
	          var field = annotation.selection.partition.variable;
	          // ignore annotation if it's read-only and we aren't
	          if (annotation.readOnly && model.readOnlyFields.indexOf(field) === -1) return;
	          // Vice-versa: single mode, displaying read-only, ignore external annots.
	          if (!annotation.readOnly && model.fieldData[field].lockAnnot) return;

	          // respond to annotation.
	          model.fieldData[field].annotation = annotation;
	          partitionToDividers(annotation, model.fieldData[field], model.scores);

	          publicAPI.render(field);
	        }
	      }));
	    }
	  }

	  // Works if model.fieldData[field].hobj is undefined.
	  function updateFieldAnnotations(fieldsData) {
	    var fieldAnnotations = fieldsData;
	    if (!fieldAnnotations && model.provider.getFieldPartitions) {
	      fieldAnnotations = model.provider.getFieldPartitions();
	    }
	    if (fieldAnnotations) {
	      Object.keys(fieldAnnotations).forEach(function (field) {
	        var annotation = fieldAnnotations[field];
	        if (model.fieldData[field]) {
	          model.fieldData[field].annotation = annotation;
	          partitionToDividers(annotation, model.fieldData[field], model.scores);
	          publicAPI.render(field);
	        }
	      });
	    }
	  }

	  function clearFieldAnnotation(fieldName) {
	    model.fieldData[fieldName].annotation = null;
	    model.fieldData[fieldName].dividers = undefined;
	    model.fieldData[fieldName].regions = [model.defaultScore];
	    model.fieldData[fieldName].editScore = false;
	  }

	  return {
	    addSubscriptions: addSubscriptions,
	    createGroups: createGroups,
	    createHeader: createHeader,
	    createPopups: createPopups,
	    createScoreIcons: createScoreIcons,
	    defaultFieldData: defaultFieldData,
	    editingScore: editingScore,
	    enabled: enabled,
	    filterFieldNames: filterFieldNames,
	    getHistRange: getHistRange,
	    init: init,
	    numScoreIcons: numScoreIcons,
	    prepareItem: prepareItem,
	    rescaleDividers: rescaleDividers,
	    updateHeader: updateHeader,
	    updateFieldAnnotations: updateFieldAnnotations,
	    clearFieldAnnotation: clearFieldAnnotation,
	    updateScoreIcons: updateScoreIcons
	  };
	}

/***/ },
/* 43 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_ARRAY__, __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_ARRAY__ = [__webpack_require__(44), __webpack_require__(45), __webpack_require__(48), __webpack_require__(49), __webpack_require__(58)], __WEBPACK_AMD_DEFINE_RESULT__ = function (is, isObject, isArray, objEquals, arrEquals) {

	    /**
	     * Recursively checks for same properties and values.
	     */
	    function deepEquals(a, b, callback){
	        callback = callback || is;

	        var bothObjects = isObject(a) && isObject(b);
	        var bothArrays = !bothObjects && isArray(a) && isArray(b);

	        if (!bothObjects && !bothArrays) {
	            return callback(a, b);
	        }

	        function compare(a, b){
	            return deepEquals(a, b, callback);
	        }

	        var method = bothObjects ? objEquals : arrEquals;
	        return method(a, b, compare);
	    }

	    return deepEquals;

	}.apply(exports, __WEBPACK_AMD_DEFINE_ARRAY__), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 44 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_RESULT__ = function () {

	    /**
	     * Check if both arguments are egal.
	     */
	    function is(x, y){
	        // implementation borrowed from harmony:egal spec
	        if (x === y) {
	          // 0 === -0, but they are not identical
	          return x !== 0 || 1 / x === 1 / y;
	        }

	        // NaN !== NaN, but they are identical.
	        // NaNs are the only non-reflexive value, i.e., if x !== x,
	        // then x is a NaN.
	        // isNaN is broken: it converts its argument to number, so
	        // isNaN("foo") => true
	        return x !== x && y !== y;
	    }

	    return is;

	}.call(exports, __webpack_require__, exports, module), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 45 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_ARRAY__, __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_ARRAY__ = [__webpack_require__(46)], __WEBPACK_AMD_DEFINE_RESULT__ = function (isKind) {
	    /**
	     */
	    function isObject(val) {
	        return isKind(val, 'Object');
	    }
	    return isObject;
	}.apply(exports, __WEBPACK_AMD_DEFINE_ARRAY__), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 46 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_ARRAY__, __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_ARRAY__ = [__webpack_require__(47)], __WEBPACK_AMD_DEFINE_RESULT__ = function (kindOf) {
	    /**
	     * Check if value is from a specific "kind".
	     */
	    function isKind(val, kind){
	        return kindOf(val) === kind;
	    }
	    return isKind;
	}.apply(exports, __WEBPACK_AMD_DEFINE_ARRAY__), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 47 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_RESULT__ = function () {

	    var _rKind = /^\[object (.*)\]$/,
	        _toString = Object.prototype.toString,
	        UNDEF;

	    /**
	     * Gets the "kind" of value. (e.g. "String", "Number", etc)
	     */
	    function kindOf(val) {
	        if (val === null) {
	            return 'Null';
	        } else if (val === UNDEF) {
	            return 'Undefined';
	        } else {
	            return _rKind.exec( _toString.call(val) )[1];
	        }
	    }
	    return kindOf;
	}.call(exports, __webpack_require__, exports, module), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 48 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_ARRAY__, __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_ARRAY__ = [__webpack_require__(46)], __WEBPACK_AMD_DEFINE_RESULT__ = function (isKind) {
	    /**
	     */
	    var isArray = Array.isArray || function (val) {
	        return isKind(val, 'Array');
	    };
	    return isArray;
	}.apply(exports, __WEBPACK_AMD_DEFINE_ARRAY__), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 49 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_ARRAY__, __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_ARRAY__ = [__webpack_require__(50), __webpack_require__(51), __webpack_require__(45), __webpack_require__(44)], __WEBPACK_AMD_DEFINE_RESULT__ = function(hasOwn, every, isObject, is) {

	    // Makes a function to compare the object values from the specified compare
	    // operation callback.
	    function makeCompare(callback) {
	        return function(value, key) {
	            return hasOwn(this, key) && callback(value, this[key]);
	        };
	    }

	    function checkProperties(value, key) {
	        return hasOwn(this, key);
	    }

	    /**
	     * Checks if two objects have the same keys and values.
	     */
	    function equals(a, b, callback) {
	        callback = callback || is;

	        if (!isObject(a) || !isObject(b)) {
	            return callback(a, b);
	        }

	        return (every(a, makeCompare(callback), b) &&
	                every(b, checkProperties, a));
	    }

	    return equals;
	}.apply(exports, __WEBPACK_AMD_DEFINE_ARRAY__), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 50 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_RESULT__ = function () {

	    /**
	     * Safer Object.hasOwnProperty
	     */
	     function hasOwn(obj, prop){
	         return Object.prototype.hasOwnProperty.call(obj, prop);
	     }

	     return hasOwn;

	}.call(exports, __webpack_require__, exports, module), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 51 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_ARRAY__, __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_ARRAY__ = [__webpack_require__(52), __webpack_require__(54)], __WEBPACK_AMD_DEFINE_RESULT__ = function(forOwn, makeIterator) {

	    /**
	     * Object every
	     */
	    function every(obj, callback, thisObj) {
	        callback = makeIterator(callback, thisObj);
	        var result = true;
	        forOwn(obj, function(val, key) {
	            // we consider any falsy values as "false" on purpose so shorthand
	            // syntax can be used to check property existence
	            if (!callback(val, key, obj)) {
	                result = false;
	                return false; // break
	            }
	        });
	        return result;
	    }

	    return every;

	}.apply(exports, __WEBPACK_AMD_DEFINE_ARRAY__), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 52 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_ARRAY__, __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_ARRAY__ = [__webpack_require__(50), __webpack_require__(53)], __WEBPACK_AMD_DEFINE_RESULT__ = function (hasOwn, forIn) {

	    /**
	     * Similar to Array/forEach but works over object properties and fixes Don't
	     * Enum bug on IE.
	     * based on: http://whattheheadsaid.com/2010/10/a-safer-object-keys-compatibility-implementation
	     */
	    function forOwn(obj, fn, thisObj){
	        forIn(obj, function(val, key){
	            if (hasOwn(obj, key)) {
	                return fn.call(thisObj, obj[key], key, obj);
	            }
	        });
	    }

	    return forOwn;

	}.apply(exports, __WEBPACK_AMD_DEFINE_ARRAY__), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 53 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_ARRAY__, __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_ARRAY__ = [__webpack_require__(50)], __WEBPACK_AMD_DEFINE_RESULT__ = function (hasOwn) {

	    var _hasDontEnumBug,
	        _dontEnums;

	    function checkDontEnum(){
	        _dontEnums = [
	                'toString',
	                'toLocaleString',
	                'valueOf',
	                'hasOwnProperty',
	                'isPrototypeOf',
	                'propertyIsEnumerable',
	                'constructor'
	            ];

	        _hasDontEnumBug = true;

	        for (var key in {'toString': null}) {
	            _hasDontEnumBug = false;
	        }
	    }

	    /**
	     * Similar to Array/forEach but works over object properties and fixes Don't
	     * Enum bug on IE.
	     * based on: http://whattheheadsaid.com/2010/10/a-safer-object-keys-compatibility-implementation
	     */
	    function forIn(obj, fn, thisObj){
	        var key, i = 0;
	        // no need to check if argument is a real object that way we can use
	        // it for arrays, functions, date, etc.

	        //post-pone check till needed
	        if (_hasDontEnumBug == null) checkDontEnum();

	        for (key in obj) {
	            if (exec(fn, obj, key, thisObj) === false) {
	                break;
	            }
	        }


	        if (_hasDontEnumBug) {
	            var ctor = obj.constructor,
	                isProto = !!ctor && obj === ctor.prototype;

	            while (key = _dontEnums[i++]) {
	                // For constructor, if it is a prototype object the constructor
	                // is always non-enumerable unless defined otherwise (and
	                // enumerated above).  For non-prototype objects, it will have
	                // to be defined on this object, since it cannot be defined on
	                // any prototype objects.
	                //
	                // For other [[DontEnum]] properties, check if the value is
	                // different than Object prototype value.
	                if (
	                    (key !== 'constructor' ||
	                        (!isProto && hasOwn(obj, key))) &&
	                    obj[key] !== Object.prototype[key]
	                ) {
	                    if (exec(fn, obj, key, thisObj) === false) {
	                        break;
	                    }
	                }
	            }
	        }
	    }

	    function exec(fn, obj, key, thisObj){
	        return fn.call(thisObj, obj[key], key, obj);
	    }

	    return forIn;

	}.apply(exports, __WEBPACK_AMD_DEFINE_ARRAY__), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 54 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_ARRAY__, __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_ARRAY__ = [__webpack_require__(55), __webpack_require__(56), __webpack_require__(57)], __WEBPACK_AMD_DEFINE_RESULT__ = function(identity, prop, deepMatches) {

	    /**
	     * Converts argument into a valid iterator.
	     * Used internally on most array/object/collection methods that receives a
	     * callback/iterator providing a shortcut syntax.
	     */
	    function makeIterator(src, thisObj){
	        if (src == null) {
	            return identity;
	        }
	        switch(typeof src) {
	            case 'function':
	                // function is the first to improve perf (most common case)
	                // also avoid using `Function#call` if not needed, which boosts
	                // perf a lot in some cases
	                return (typeof thisObj !== 'undefined')? function(val, i, arr){
	                    return src.call(thisObj, val, i, arr);
	                } : src;
	            case 'object':
	                return function(val){
	                    return deepMatches(val, src);
	                };
	            case 'string':
	            case 'number':
	                return prop(src);
	        }
	    }

	    return makeIterator;

	}.apply(exports, __WEBPACK_AMD_DEFINE_ARRAY__), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 55 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_RESULT__ = function () {

	    /**
	     * Returns the first argument provided to it.
	     */
	    function identity(val){
	        return val;
	    }

	    return identity;

	}.call(exports, __webpack_require__, exports, module), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 56 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_RESULT__ = function () {

	    /**
	     * Returns a function that gets a property of the passed object
	     */
	    function prop(name){
	        return function(obj){
	            return obj[name];
	        };
	    }

	    return prop;

	}.call(exports, __webpack_require__, exports, module), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 57 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_ARRAY__, __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_ARRAY__ = [__webpack_require__(52), __webpack_require__(48)], __WEBPACK_AMD_DEFINE_RESULT__ = function(forOwn, isArray) {

	    function containsMatch(array, pattern) {
	        var i = -1, length = array.length;
	        while (++i < length) {
	            if (deepMatches(array[i], pattern)) {
	                return true;
	            }
	        }

	        return false;
	    }

	    function matchArray(target, pattern) {
	        var i = -1, patternLength = pattern.length;
	        while (++i < patternLength) {
	            if (!containsMatch(target, pattern[i])) {
	                return false;
	            }
	        }

	        return true;
	    }

	    function matchObject(target, pattern) {
	        var result = true;
	        forOwn(pattern, function(val, key) {
	            if (!deepMatches(target[key], val)) {
	                // Return false to break out of forOwn early
	                return (result = false);
	            }
	        });

	        return result;
	    }

	    /**
	     * Recursively check if the objects match.
	     */
	    function deepMatches(target, pattern){
	        if (target && typeof target === 'object' &&
	            pattern && typeof pattern === 'object') {
	            if (isArray(target) && isArray(pattern)) {
	                return matchArray(target, pattern);
	            } else {
	                return matchObject(target, pattern);
	            }
	        } else {
	            return target === pattern;
	        }
	    }

	    return deepMatches;

	}.apply(exports, __WEBPACK_AMD_DEFINE_ARRAY__), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 58 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_ARRAY__, __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_ARRAY__ = [__webpack_require__(44), __webpack_require__(48), __webpack_require__(59)], __WEBPACK_AMD_DEFINE_RESULT__ = function(is, isArray, every) {

	    /**
	     * Compares if both arrays have the same elements
	     */
	    function equals(a, b, callback){
	        callback = callback || is;

	        if (!isArray(a) || !isArray(b)) {
	            return callback(a, b);
	        }

	        if (a.length !== b.length) {
	            return false;
	        }

	        return every(a, makeCompare(callback), b);
	    }

	    function makeCompare(callback) {
	        return function(value, i) {
	            return i in this && callback(value, this[i]);
	        };
	    }

	    return equals;

	}.apply(exports, __WEBPACK_AMD_DEFINE_ARRAY__), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 59 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_ARRAY__, __WEBPACK_AMD_DEFINE_RESULT__;!(__WEBPACK_AMD_DEFINE_ARRAY__ = [__webpack_require__(54)], __WEBPACK_AMD_DEFINE_RESULT__ = function (makeIterator) {

	    /**
	     * Array every
	     */
	    function every(arr, callback, thisObj) {
	        callback = makeIterator(callback, thisObj);
	        var result = true;
	        if (arr == null) {
	            return result;
	        }

	        var i = -1, len = arr.length;
	        while (++i < len) {
	            // we iterate over sparse items since there is no way to make it
	            // work properly on IE 7-8. see #64
	            if (!callback(arr[i], i, arr) ) {
	                result = false;
	                break;
	            }
	        }

	        return result;
	    }

	    return every;
	}.apply(exports, __WEBPACK_AMD_DEFINE_ARRAY__), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));


/***/ },
/* 60 */
/***/ function(module, exports) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	// ----------------------------------------------------------------------------
	// Internal helpers
	// ----------------------------------------------------------------------------

	var generation = 0;

	function setInitialGenerationNumber(genNum) {
	  generation = genNum;
	}

	function intersect(a, b) {
	  var result = [];
	  a.sort();
	  b.sort();

	  while (a.length && b.length) {
	    if (a[0] < b[0]) {
	      a.shift();
	    } else if (a[0] > b[0]) {
	      b.shift();
	    } else {
	      result.push(a.shift());
	      b.shift();
	    }
	  }
	  return result;
	}

	function clone(obj, fieldList, defaults) {
	  var clonedObj = {};
	  fieldList.forEach(function (name) {
	    if (defaults && obj[name] === undefined && defaults[name] !== undefined) {
	      clonedObj[name] = defaults[name];
	    } else {
	      clonedObj[name] = obj[name];
	    }
	    if (Array.isArray(clonedObj[name])) {
	      clonedObj[name] = clonedObj[name].map(function (i) {
	        return i;
	      });
	    }
	  });
	  return clonedObj;
	}

	var endpointToRuleOperator = {
	  o: '<',
	  '*': '<='
	};

	var ruleTypes = exports.ruleTypes = {
	  '3L': { terms: 3, operators: { values: [['<', '<=']], index: [1] }, variable: 0, values: [2] },
	  '3R': { terms: 3, operators: { values: [['>', '>=']], index: [1] }, variable: 2, values: [0] },
	  '5C': { terms: 5, operators: { values: [['<', '<='], ['<', '<=']], index: [1, 3] }, variable: 2, values: [0, 4] },
	  multi: { terms: -1, operators: null },
	  logical: { operators: { values: ['not', 'and', 'or', 'xor'], index: [0] } },
	  row: {}
	};

	// ----------------------------------------------------------------------------
	// Public builder method
	// ----------------------------------------------------------------------------

	function empty() {
	  generation += 1;
	  return {
	    type: 'empty',
	    generation: generation
	  };
	}

	// ----------------------------------------------------------------------------

	function partition(variable, dividers) {
	  generation += 1;
	  return {
	    type: 'partition',
	    generation: generation,
	    partition: {
	      variable: variable,
	      dividers: dividers.map(function (divider) {
	        return clone(divider, ['value', 'uncertainty', 'closeToLeft'], { closeToLeft: false });
	      })
	    }
	  };
	}

	// ----------------------------------------------------------------------------

	function range(vars) {
	  generation += 1;
	  var variables = {};
	  var selection = {
	    type: 'range',
	    generation: generation,
	    range: {
	      variables: variables
	    }
	  };

	  // Fill variables
	  Object.keys(vars).forEach(function (name) {
	    variables[name] = vars[name].map(function (interval) {
	      return clone(interval, ['interval', 'endpoints', 'uncertainty'], { endpoints: '**' });
	    });
	    variables[name].sort(function (a, b) {
	      return a.interval[0] - b.interval[0];
	    });
	  });

	  return selection;
	}

	// ----------------------------------------------------------------------------

	function rule() {
	  var type = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : 'multi';
	  var terms = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : [];
	  var roles = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : [];

	  generation += 1;
	  // FIXME ?? deepClone ??
	  return {
	    type: 'rule',
	    generation: generation,
	    rule: {
	      type: type,
	      terms: terms,
	      roles: roles
	    }
	  };
	}

	// ----------------------------------------------------------------------------

	function variableToRule(name, ranges) {
	  var terms = ['or'];
	  ranges.forEach(function (clause) {
	    terms.push({
	      type: '5C',
	      terms: [clause.interval[0], endpointToRuleOperator[clause.endpoints[0]], name, endpointToRuleOperator[clause.endpoints[1]], clause.interval[1]]
	    });
	  });
	  if (terms.length === 2) {
	    // one range, don't need the logical 'or'
	    return terms[1];
	  }
	  return {
	    type: 'logical',
	    terms: terms
	  };
	}

	// ----------

	function rangeToRule(selection) {
	  var terms = ['and'];
	  var vars = selection.range.variables;
	  Object.keys(vars).forEach(function (name) {
	    terms.push(variableToRule(name, vars[name]));
	  });
	  return rule('logical', terms);
	}

	// ----------

	function partitionToRule(selection) {
	  var roles = [];
	  var _selection$partition = selection.partition,
	      dividers = _selection$partition.dividers,
	      variable = _selection$partition.variable;

	  var terms = dividers.map(function (divider, idx, array) {
	    if (idx === 0) {
	      return {
	        type: '3L',
	        terms: [variable, divider.closeToLeft ? '<' : '<=', divider.value]
	      };
	    }
	    return {
	      type: '5C',
	      terms: [array[idx - 1].value, array[idx - 1].closeToLeft ? '<' : '<=', variable, divider.closeToLeft ? '<' : '<=', divider.value]
	    };
	  });
	  var lastDivider = dividers.slice(-1);
	  terms.push({
	    type: '3R',
	    terms: [lastDivider.value, lastDivider.closeToLeft ? '<' : '<=', variable]
	  });

	  // Fill roles with partition number
	  while (roles.length < terms.length) {
	    roles.push({ partition: roles.length });
	  }

	  return rule('multi', terms, roles);
	}

	// ----------------------------------------------------------------------------

	function convertToRuleSelection(selection) {
	  if (selection.type === 'range') {
	    return rangeToRule(selection);
	  }
	  if (selection.type === 'partition') {
	    return partitionToRule(selection);
	  }
	  if (selection.type === 'empty') {
	    return selection;
	  }

	  throw new Error('Convertion to rule not supported with selection of type ' + selection.type);
	}

	// ----------------------------------------------------------------------------

	function markModified(selection) {
	  generation += 1;
	  return Object.assign({}, selection, { generation: generation });
	}

	// ----------------------------------------------------------------------------

	function hasField(selection, fieldNames) {
	  if (!selection || selection.type === 'empty') {
	    return false;
	  }
	  var fieldsToLookup = [].concat(fieldNames);

	  if (selection.type === 'range') {
	    var fields = Object.keys(selection.range.variables);
	    var match = intersect(fieldsToLookup, fields);
	    return match.length > 0;
	  }
	  if (selection.type === 'partition') {
	    return fieldsToLookup.indexOf(selection.partition.variable) !== -1;
	  }

	  console.log('SelectionBuilder::hasField does not handle selection of type', selection.type);

	  return false;
	}

	// ----------------------------------------------------------------------------
	// Exposed object
	// ----------------------------------------------------------------------------

	var EMPTY_SELECTION = empty();

	exports.default = {
	  convertToRuleSelection: convertToRuleSelection,
	  empty: empty,
	  EMPTY_SELECTION: EMPTY_SELECTION,
	  hasField: hasField,
	  markModified: markModified,
	  partition: partition,
	  range: range,
	  rule: rule,
	  setInitialGenerationNumber: setInitialGenerationNumber
	};

/***/ },
/* 61 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});

	var _UUID = __webpack_require__(62);

	var _SelectionBuilder = __webpack_require__(60);

	var _SelectionBuilder2 = _interopRequireDefault(_SelectionBuilder);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	// ----------------------------------------------------------------------------
	// Internal helpers
	// ----------------------------------------------------------------------------

	var generation = 0;

	function setInitialGenerationNumber(genNum) {
	  generation = genNum;
	}

	// ----------------------------------------------------------------------------
	// Public builder method
	// ----------------------------------------------------------------------------

	function annotation(selection, score) {
	  var weight = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : 1;
	  var rationale = arguments.length > 3 && arguments[3] !== undefined ? arguments[3] : '';
	  var name = arguments.length > 4 && arguments[4] !== undefined ? arguments[4] : '';

	  generation += 1;
	  return {
	    id: (0, _UUID.generateUUID)(),
	    generation: generation,
	    selection: selection,
	    score: score,
	    weight: weight,
	    rationale: rationale,
	    name: name
	  };
	}

	// ----------------------------------------------------------------------------

	function update(annotationObject, changeSet) {
	  var updatedAnnotation = Object.assign({}, annotationObject, changeSet);

	  var changeDetected = false;
	  Object.keys(updatedAnnotation).forEach(function (key) {
	    if (updatedAnnotation[key] !== annotationObject[key]) {
	      changeDetected = true;
	    }
	  });

	  if (changeDetected) {
	    generation += 1;
	    updatedAnnotation.generation = generation;
	  }

	  return updatedAnnotation;
	}

	// ----------------------------------------------------------------------------

	function updateReadOnlyFlag(annotationToEdit, readOnlyFields) {
	  if (!annotationToEdit || !annotationToEdit.selection || !readOnlyFields) {
	    return;
	  }

	  annotationToEdit.readOnly = _SelectionBuilder2.default.hasField(annotationToEdit.selection, readOnlyFields);
	}

	// ----------------------------------------------------------------------------

	function fork(annotationObj) {
	  var id = (0, _UUID.generateUUID)();
	  generation += 1;
	  return Object.assign({}, annotationObj, { generation: generation, id: id });
	}

	function setDefaultName(annotationObject) {
	  if (annotationObject.selection.type === 'range') {
	    var rangeNames = Object.keys(annotationObject.selection.range.variables);
	    if (rangeNames.length > 0) {
	      annotationObject.name = rangeNames[0];
	      if (rangeNames.length > 1) {
	        annotationObject.name += ' & ' + rangeNames[1];
	      }
	      if (rangeNames.length > 2) {
	        annotationObject.name += ' &...';
	      }
	    } else {
	      annotationObject.name = 'empty';
	    }
	    annotationObject.name += ' (range)';
	  } else if (annotationObject.selection.type === 'partition') {
	    annotationObject.name = annotationObject.selection.partition.variable + ' (partition)';
	  } else {
	    annotationObject.name = 'unknown';
	  }
	}

	// ----------------------------------------------------------------------------

	function markModified(annotationObject) {
	  generation += 1;
	  return Object.assign({}, annotationObject, { generation: generation });
	}

	// ----------------------------------------------------------------------------
	// Exposed object
	// ----------------------------------------------------------------------------

	var EMPTY_ANNOTATION = annotation(_SelectionBuilder2.default.EMPTY_SELECTION, []);

	exports.default = {
	  annotation: annotation,
	  EMPTY_ANNOTATION: EMPTY_ANNOTATION,
	  fork: fork,
	  markModified: markModified,
	  setDefaultName: setDefaultName,
	  setInitialGenerationNumber: setInitialGenerationNumber,
	  update: update,
	  updateReadOnlyFlag: updateReadOnlyFlag
	};

/***/ },
/* 62 */
/***/ function(module, exports) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	exports.generateUUID = generateUUID;
	/**
	 * The following method was adapted from code found here:
	 *
	 *    http://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript
	 */

	/* global window */
	/* eslint-disable no-bitwise */

	function generateUUID() {
	  var d = Date.now();
	  if (window.performance && typeof window.performance.now === 'function') {
	    d += window.performance.now(); // use high-precision timer if available
	  }
	  var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
	    var r = (d + Math.random() * 16) % 16 | 0;
	    d = Math.floor(d / 16);
	    return (c === 'x' ? r : r & 0x3 | 0x8).toString(16);
	  });
	  return uuid;
	}

	exports.default = {
	  generateUUID: generateUUID
	};

/***/ },
/* 63 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	exports.newInstance = undefined;
	exports.extend = extend;

	var _d = __webpack_require__(29);

	var _d2 = _interopRequireDefault(_d);

	var _FieldSelector = __webpack_require__(64);

	var _FieldSelector2 = _interopRequireDefault(_FieldSelector);

	var _CompositeClosureHelper = __webpack_require__(2);

	var _CompositeClosureHelper2 = _interopRequireDefault(_CompositeClosureHelper);

	var _template = __webpack_require__(66);

	var _template2 = _interopRequireDefault(_template);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	// ----------------------------------------------------------------------------
	// Global
	// ----------------------------------------------------------------------------

	// ----------------------------------------------------------------------------
	// Field Selector
	// ----------------------------------------------------------------------------

	function fieldSelector(publicAPI, model) {
	  // private variables
	  var hideField = {
	    minMax: false,
	    hist: false,
	    minMaxWidth: 0,
	    histWidth: 0
	  };

	  // storage for 1d histograms
	  if (!model.histograms) {
	    model.histograms = {};
	  }

	  // public API
	  publicAPI.resize = function () {
	    publicAPI.render();
	  };

	  publicAPI.setContainer = function (el) {
	    if (model.container) {
	      while (model.container.firstChild) {
	        model.container.removeChild(model.container.firstChild);
	      }
	      model.container = null;
	    }

	    model.container = el;

	    if (el) {
	      _d2.default.select(model.container).html(_template2.default);
	      _d2.default.select(model.container).select('.fieldSelector').classed(_FieldSelector2.default.fieldSelector, true);

	      model.fieldShowHistogram = model.fieldShowHistogram && model.provider.isA('Histogram1DProvider');
	      // append headers for histogram columns
	      if (model.fieldShowHistogram) {
	        var header = _d2.default.select(model.container).select('thead').select('tr');
	        header.append('th').text('Min').classed(_FieldSelector2.default.jsHistMin, true);
	        header.append('th').text('Histogram').classed(_FieldSelector2.default.jsSparkline, true);
	        header.append('th').text('Max').classed(_FieldSelector2.default.jsHistMax, true);
	      }
	      publicAPI.render();
	    }
	  };

	  publicAPI.render = function () {
	    if (!model.container) {
	      return;
	    }

	    var legendSize = 15;

	    // Apply style
	    _d2.default.select(model.container).select('thead').classed(_FieldSelector2.default.thead, true);
	    _d2.default.select(model.container).select('tbody').classed(_FieldSelector2.default.tbody, true);
	    _d2.default.select(model.container).select('th.field-selector-mode').on('click', function (d) {
	      model.displayUnselected = !model.displayUnselected;
	      publicAPI.render();
	    }).select('i')
	    // apply class - 'false' should come first to not remove common base class.
	    .classed(!model.displayUnselected ? _FieldSelector2.default.allFieldsIcon : _FieldSelector2.default.selectedFieldsIcon, false).classed(model.displayUnselected ? _FieldSelector2.default.allFieldsIcon : _FieldSelector2.default.selectedFieldsIcon, true);

	    var data = model.displayUnselected ? model.provider.getFieldNames() : model.provider.getActiveFieldNames();
	    var totalNum = model.displayUnselected ? data.length : model.provider.getFieldNames().length;

	    // Update header label
	    _d2.default.select(model.container).select('th.field-selector-label').style('text-align', 'left').text(model.displayUnselected ? 'Only Selected (' + data.length + ' total)' : 'Only Selected (' + data.length + ' / ' + totalNum + ' total)').on('click', function (d) {
	      model.displayUnselected = !model.displayUnselected;
	      publicAPI.render();
	    });

	    // test for too-long rows
	    var hideMore = model.container.scrollWidth > model.container.clientWidth;
	    if (hideMore) {
	      if (!hideField.minMax) {
	        hideField.minMax = true;
	        hideField.minMaxWidth = model.container.scrollWidth;
	        // if we hide min/max, we may also need to hide hist, so trigger another resize
	        setTimeout(publicAPI.resize, 0);
	      } else if (!hideField.hist) {
	        hideField.hist = true;
	        hideField.histWidth = model.container.scrollWidth;
	      }
	    } else if (hideField.minMax) {
	      // if we've hidden something, see if we can re-show it.
	      if (hideField.hist) {
	        if (model.container.scrollWidth - hideField.histWidth > 0) {
	          hideField.hist = false;
	          hideField.histWidth = 0;
	          // if we show hist, we may also need to show min/max, so trigger another resize
	          setTimeout(publicAPI.resize, 0);
	        }
	      } else if (hideField.minMax) {
	        if (model.container.scrollWidth - hideField.minMaxWidth > 0) {
	          hideField.minMax = false;
	          hideField.minMaxWidth = 0;
	        }
	      }
	    }
	    var header = _d2.default.select(model.container).select('thead').select('tr');
	    header.selectAll('.' + _FieldSelector2.default.jsHistMin).style('display', hideField.minMax ? 'none' : null);
	    header.selectAll('.' + _FieldSelector2.default.jsSparkline).style('display', hideField.hist ? 'none' : null);
	    header.selectAll('.' + _FieldSelector2.default.jsHistMax).style('display', hideField.minMax ? 'none' : null);

	    // Handle variables
	    var variablesContainer = _d2.default.select(model.container).select('tbody.fields').selectAll('tr').data(data);

	    variablesContainer.enter().append('tr');
	    variablesContainer.exit().remove();

	    // Apply on each data item
	    function renderField(fieldName, index) {
	      var field = model.provider.getField(fieldName);
	      var fieldContainer = _d2.default.select(this);
	      var legendCell = fieldContainer.select('.' + _FieldSelector2.default.jsLegend);
	      var fieldCell = fieldContainer.select('.' + _FieldSelector2.default.jsFieldName);

	      // Apply style to row (selected/unselected)
	      fieldContainer.classed(!field.active ? _FieldSelector2.default.selectedRow : _FieldSelector2.default.unselectedRow, false).classed(field.active ? _FieldSelector2.default.selectedRow : _FieldSelector2.default.unselectedRow, true).on('click', function (name) {
	        model.provider.toggleFieldSelection(name);
	      });

	      // Create missing DOM element if any
	      if (legendCell.empty()) {
	        legendCell = fieldContainer.append('td').classed(_FieldSelector2.default.legend, true);

	        fieldCell = fieldContainer.append('td').classed(_FieldSelector2.default.fieldName, true);
	      }

	      // Apply legend
	      if (model.provider.isA('LegendProvider')) {
	        var _model$provider$getLe = model.provider.getLegend(fieldName),
	            color = _model$provider$getLe.color,
	            shape = _model$provider$getLe.shape;

	        legendCell.html('<svg class=\'' + _FieldSelector2.default.legendSvg + '\' width=\'' + legendSize + '\' height=\'' + legendSize + '\'\n                  fill=\'' + color + '\' stroke=\'black\'><use xlink:href=\'' + shape + '\'/></svg>');
	      } else {
	        legendCell.html('<i></i>').select('i').classed(!field.active ? _FieldSelector2.default.selectedRow : _FieldSelector2.default.unselectedRow, false).classed(field.active ? _FieldSelector2.default.selectedRow : _FieldSelector2.default.unselectedRow, true);
	      }

	      // Apply field name
	      fieldCell.text(fieldName);

	      if (model.fieldShowHistogram) {
	        var minCell = fieldContainer.select('.' + _FieldSelector2.default.jsHistMin);
	        var histCell = fieldContainer.select('.' + _FieldSelector2.default.jsSparkline);
	        var maxCell = fieldContainer.select('.' + _FieldSelector2.default.jsHistMax);

	        if (histCell.empty()) {
	          minCell = fieldContainer.append('td').classed(_FieldSelector2.default.jsHistMin, true);
	          histCell = fieldContainer.append('td').classed(_FieldSelector2.default.sparkline, true);
	          maxCell = fieldContainer.append('td').classed(_FieldSelector2.default.jsHistMax, true);
	          histCell.append('svg').classed(_FieldSelector2.default.sparklineSvg, true).attr('width', model.fieldHistWidth).attr('height', model.fieldHistHeight);
	        }

	        // make sure our data is ready. If not, render will be called when loaded.
	        var hobj = model.histograms ? model.histograms[fieldName] : null;
	        if (hobj) {
	          histCell.style('display', hideField.hist ? 'none' : null);

	          // only do work if histogram is displayed.
	          if (!hideField.hist) {
	            var cmax = 1.0 * _d2.default.max(hobj.counts);
	            var hsize = hobj.counts.length;
	            var hdata = histCell.select('svg').selectAll('.' + _FieldSelector2.default.jsHistRect).data(hobj.counts);

	            hdata.enter().append('rect');
	            // changes apply to both enter and update data join:
	            hdata.attr('class', function (d, i) {
	              return i % 2 === 0 ? _FieldSelector2.default.histRectEven : _FieldSelector2.default.histRectOdd;
	            }).attr('pname', fieldName).attr('y', function (d) {
	              return model.fieldHistHeight * (1.0 - d / cmax);
	            }).attr('x', function (d, i) {
	              return model.fieldHistWidth / hsize * i;
	            }).attr('height', function (d) {
	              return model.fieldHistHeight * (d / cmax);
	            }).attr('width', model.fieldHistWidth / hsize);

	            hdata.exit().remove();

	            if (model.provider.isA('HistogramBinHoverProvider')) {
	              histCell.select('svg').on('mousemove', function inner(d, i) {
	                var mCoords = _d2.default.mouse(this);
	                var binNum = Math.floor(mCoords[0] / model.fieldHistWidth * hsize);
	                var state = {};
	                state[fieldName] = [binNum];
	                model.provider.setHoverState({ state: state });
	              }).on('mouseout', function (d, i) {
	                var state = {};
	                state[fieldName] = [-1];
	                model.provider.setHoverState({ state: state });
	              });
	            }
	          }

	          var formatter = _d2.default.format('.3s');
	          minCell.text(formatter(hobj.min)).style('display', hideField.minMax ? 'none' : null);
	          maxCell.text(formatter(hobj.max)).style('display', hideField.minMax ? 'none' : null);
	        }
	      }
	    }

	    // Render all fields
	    variablesContainer.each(renderField);
	  };

	  function handleHoverUpdate(data) {
	    var svg = _d2.default.select(model.container);
	    Object.keys(data.state).forEach(function (pName) {
	      var binList = data.state[pName];
	      svg.selectAll('rect[pname=\'' + pName + '\']').classed(_FieldSelector2.default.histoHilite, function (d, i) {
	        return binList.indexOf(-1) === -1;
	      }).classed(_FieldSelector2.default.binHilite, function (d, i) {
	        return binList.indexOf(i) >= 0;
	      });
	    });
	  }

	  // Make sure default values get applied
	  publicAPI.setContainer(model.container);

	  model.subscriptions.push({ unsubscribe: publicAPI.setContainer });

	  model.subscriptions.push(model.provider.onFieldChange(function () {
	    publicAPI.render();
	    model.histogram1DDataSubscription.update(model.provider.getFieldNames(), {
	      numberOfBins: model.numberOfBins,
	      partial: true
	    });
	  }));

	  if (model.fieldShowHistogram) {
	    if (model.provider.isA('Histogram1DProvider')) {
	      model.histogram1DDataSubscription = model.provider.subscribeToHistogram1D(function (allHistogram1d) {
	        // Below, we're asking for partial updates, so we just update our
	        // cache with anything that came in.
	        Object.keys(allHistogram1d).forEach(function (paramName) {
	          model.histograms[paramName] = allHistogram1d[paramName];
	        });
	        publicAPI.render();
	      }, model.provider.getFieldNames(), {
	        numberOfBins: model.numberOfBins,
	        partial: true
	      });

	      model.subscriptions.push(model.histogram1DDataSubscription);
	    }
	  }

	  if (model.provider.isA('HistogramBinHoverProvider')) {
	    model.subscriptions.push(model.provider.onHoverBinChange(handleHoverUpdate));
	  }
	}

	// ----------------------------------------------------------------------------
	// Object factory
	// ----------------------------------------------------------------------------

	var DEFAULT_VALUES = {
	  container: null,
	  provider: null,
	  displayUnselected: true,
	  fieldShowHistogram: true,
	  fieldHistWidth: 120,
	  fieldHistHeight: 15,
	  numberOfBins: 32
	};

	// ----------------------------------------------------------------------------

	function extend(publicAPI, model) {
	  var initialValues = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : {};

	  Object.assign(model, DEFAULT_VALUES, initialValues);

	  _CompositeClosureHelper2.default.destroy(publicAPI, model);
	  _CompositeClosureHelper2.default.isA(publicAPI, model, 'VizComponent');
	  _CompositeClosureHelper2.default.get(publicAPI, model, ['provider', 'container', 'fieldShowHistogram', 'numberOfBins']);
	  _CompositeClosureHelper2.default.set(publicAPI, model, ['fieldShowHistogram', 'numberOfBins']);

	  fieldSelector(publicAPI, model);
	}

	// ----------------------------------------------------------------------------

	var newInstance = exports.newInstance = _CompositeClosureHelper2.default.newInstance(extend);

	// ----------------------------------------------------------------------------

	exports.default = { newInstance: newInstance, extend: extend };

/***/ },
/* 64 */
/***/ function(module, exports, __webpack_require__) {

	// style-loader: Adds some css to the DOM by adding a <style> tag

	// load the styles
	var content = __webpack_require__(65);
	if(typeof content === 'string') content = [[module.id, content, '']];
	// add the styles to the DOM
	var update = __webpack_require__(40)(content, {});
	if(content.locals) module.exports = content.locals;
	// Hot Module Replacement
	if(false) {
		// When the styles change, update the <style> tags
		if(!content.locals) {
			module.hot.accept("!!../../../css-loader/index.js?modules&importLoaders=1&localIdentName=[name]_[local]_[hash:base64:5]!../../../postcss-loader/index.js!./FieldSelector.mcss", function() {
				var newContent = require("!!../../../css-loader/index.js?modules&importLoaders=1&localIdentName=[name]_[local]_[hash:base64:5]!../../../postcss-loader/index.js!./FieldSelector.mcss");
				if(typeof newContent === 'string') newContent = [[module.id, newContent, '']];
				update(newContent);
			});
		}
		// When the module is disposed, remove the <style> tags
		module.hot.dispose(function() { update(); });
	}

/***/ },
/* 65 */
/***/ function(module, exports, __webpack_require__) {

	exports = module.exports = __webpack_require__(32)();
	// imports
	exports.i(__webpack_require__(33), undefined);

	// module
	exports.push([module.id, "/*empty styles allow for d3 selection in javascript*/\n.FieldSelector_jsFieldName_2o3MA,\n.FieldSelector_jsHistMax_ADpEH,\n.FieldSelector_jsHistMin_3v8Z7,\n.FieldSelector_jsHistRect_BB62l,\n.FieldSelector_jsLegend_OMfn-,\n.FieldSelector_jsSparkline_Emd2I {\n\n}\n\n.FieldSelector_fieldSelector_3ispn {\n  font-family: \"Optima\", \"Linux Biolinum\", \"URW Classico\", sans;\n}\n\n.FieldSelector_icon_3mh4- {\n  -webkit-user-select: none;\n     -moz-user-select: none;\n      -ms-user-select: none;\n          user-select: none;\n  cursor: pointer;\n}\n\n.FieldSelector_selectedFieldsIcon_1YYn2 {\n}\n\n.FieldSelector_allFieldsIcon_2uJNF {\n}\n\n.FieldSelector_legend_3VTym {\n  text-align: center;\n  padding: 5px;\n}\n.FieldSelector_legendSvg_2Fppx {\n  vertical-align: middle;\n}\n\n.FieldSelector_fieldName_3kzPX {\n  width: 100%;\n  white-space: nowrap;\n  overflow: hidden;\n  text-overflow: ellipsis;\n}\n\n.FieldSelector_row_1eL0E {\n  -webkit-user-select: none;\n     -moz-user-select: none;\n      -ms-user-select: none;\n          user-select: none;\n  cursor: pointer;\n}\n\n.FieldSelector_unselectedRow_19z3L {\n  opacity: 0.5;\n}\n\n.FieldSelector_selectedRow_3gv4X {\n  opacity: 1;\n}\n\n.FieldSelector_row_1eL0E:hover {\n  background-color: #ccd;\n}\n\n.FieldSelector_thead_2Hf6- {\n  -webkit-user-select: none;\n     -moz-user-select: none;\n      -ms-user-select: none;\n          user-select: none;\n  cursor: pointer;\n}\n\n.FieldSelector_tbody_DcKpN {\n}\n\n.FieldSelector_sparkline_uFn5R {\n  padding: 2px;\n}\n\n.FieldSelector_sparklineSvg_3AwcD {\n  vertical-align: middle;\n}\n\n.FieldSelector_histRect_3lBb4 {\n  stroke: none;\n  shape-rendering: crispEdges;\n}\n.FieldSelector_histRectEven_oR2j1 {\n  fill: #8089B8;\n}\n.FieldSelector_histRectOdd_3IPnY {\n  fill: #7780AB;\n}\n\n.FieldSelector_histoHilite_3mv0M {\n  fill: #999;\n}\n\n.FieldSelector_binHilite_2YtJs {\n  fill: #001EB8;\n}\n", ""]);

	// exports
	exports.locals = {
		"jsFieldName": "FieldSelector_jsFieldName_2o3MA",
		"jsHistMax": "FieldSelector_jsHistMax_ADpEH",
		"jsHistMin": "FieldSelector_jsHistMin_3v8Z7",
		"jsHistRect": "FieldSelector_jsHistRect_BB62l",
		"jsLegend": "FieldSelector_jsLegend_OMfn-",
		"jsSparkline": "FieldSelector_jsSparkline_Emd2I",
		"fieldSelector": "FieldSelector_fieldSelector_3ispn",
		"icon": "FieldSelector_icon_3mh4- " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + "",
		"selectedFieldsIcon": "FieldSelector_selectedFieldsIcon_1YYn2 FieldSelector_icon_3mh4- " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-check-square-o"] + "",
		"allFieldsIcon": "FieldSelector_allFieldsIcon_2uJNF FieldSelector_icon_3mh4- " + __webpack_require__(33).locals["fa"] + " " + __webpack_require__(33).locals["fa-fw"] + " " + __webpack_require__(33).locals["fa-square-o"] + "",
		"legend": "FieldSelector_legend_3VTym FieldSelector_jsLegend_OMfn-",
		"legendSvg": "FieldSelector_legendSvg_2Fppx",
		"fieldName": "FieldSelector_fieldName_3kzPX FieldSelector_jsFieldName_2o3MA",
		"row": "FieldSelector_row_1eL0E",
		"unselectedRow": "FieldSelector_unselectedRow_19z3L FieldSelector_row_1eL0E",
		"selectedRow": "FieldSelector_selectedRow_3gv4X FieldSelector_row_1eL0E",
		"thead": "FieldSelector_thead_2Hf6-",
		"tbody": "FieldSelector_tbody_DcKpN",
		"sparkline": "FieldSelector_sparkline_uFn5R FieldSelector_jsSparkline_Emd2I",
		"sparklineSvg": "FieldSelector_sparklineSvg_3AwcD",
		"histRect": "FieldSelector_histRect_3lBb4 FieldSelector_jsHistRect_BB62l",
		"histRectEven": "FieldSelector_histRectEven_oR2j1 FieldSelector_histRect_3lBb4 FieldSelector_jsHistRect_BB62l",
		"histRectOdd": "FieldSelector_histRectOdd_3IPnY FieldSelector_histRect_3lBb4 FieldSelector_jsHistRect_BB62l",
		"histoHilite": "FieldSelector_histoHilite_3mv0M",
		"binHilite": "FieldSelector_binHilite_2YtJs"
	};

/***/ },
/* 66 */
/***/ function(module, exports) {

	module.exports = "<table class=\"fieldSelector\">\n  <thead>\n    <tr><th class=\"field-selector-mode\"><i></i></th><th class=\"field-selector-label\"></th></tr>\n  </thead>\n  <tbody class=\"fields\"></tbody>\n</table>\n";

/***/ },
/* 67 */
/***/ function(module, exports, __webpack_require__) {

	var json_stringify = __webpack_require__(68).stringify;
	var json_parse     = __webpack_require__(70);

	module.exports = function(options) {
	    return  {
	        parse: json_parse(options),
	        stringify: json_stringify
	    }
	};
	//create the default method members with no options applied for backwards compatibility
	module.exports.parse = json_parse();
	module.exports.stringify = json_stringify;


/***/ },
/* 68 */
/***/ function(module, exports, __webpack_require__) {

	var BigNumber = __webpack_require__(69);

	/*
	    json2.js
	    2013-05-26

	    Public Domain.

	    NO WARRANTY EXPRESSED OR IMPLIED. USE AT YOUR OWN RISK.

	    See http://www.JSON.org/js.html


	    This code should be minified before deployment.
	    See http://javascript.crockford.com/jsmin.html

	    USE YOUR OWN COPY. IT IS EXTREMELY UNWISE TO LOAD CODE FROM SERVERS YOU DO
	    NOT CONTROL.


	    This file creates a global JSON object containing two methods: stringify
	    and parse.

	        JSON.stringify(value, replacer, space)
	            value       any JavaScript value, usually an object or array.

	            replacer    an optional parameter that determines how object
	                        values are stringified for objects. It can be a
	                        function or an array of strings.

	            space       an optional parameter that specifies the indentation
	                        of nested structures. If it is omitted, the text will
	                        be packed without extra whitespace. If it is a number,
	                        it will specify the number of spaces to indent at each
	                        level. If it is a string (such as '\t' or '&nbsp;'),
	                        it contains the characters used to indent at each level.

	            This method produces a JSON text from a JavaScript value.

	            When an object value is found, if the object contains a toJSON
	            method, its toJSON method will be called and the result will be
	            stringified. A toJSON method does not serialize: it returns the
	            value represented by the name/value pair that should be serialized,
	            or undefined if nothing should be serialized. The toJSON method
	            will be passed the key associated with the value, and this will be
	            bound to the value

	            For example, this would serialize Dates as ISO strings.

	                Date.prototype.toJSON = function (key) {
	                    function f(n) {
	                        // Format integers to have at least two digits.
	                        return n < 10 ? '0' + n : n;
	                    }

	                    return this.getUTCFullYear()   + '-' +
	                         f(this.getUTCMonth() + 1) + '-' +
	                         f(this.getUTCDate())      + 'T' +
	                         f(this.getUTCHours())     + ':' +
	                         f(this.getUTCMinutes())   + ':' +
	                         f(this.getUTCSeconds())   + 'Z';
	                };

	            You can provide an optional replacer method. It will be passed the
	            key and value of each member, with this bound to the containing
	            object. The value that is returned from your method will be
	            serialized. If your method returns undefined, then the member will
	            be excluded from the serialization.

	            If the replacer parameter is an array of strings, then it will be
	            used to select the members to be serialized. It filters the results
	            such that only members with keys listed in the replacer array are
	            stringified.

	            Values that do not have JSON representations, such as undefined or
	            functions, will not be serialized. Such values in objects will be
	            dropped; in arrays they will be replaced with null. You can use
	            a replacer function to replace those with JSON values.
	            JSON.stringify(undefined) returns undefined.

	            The optional space parameter produces a stringification of the
	            value that is filled with line breaks and indentation to make it
	            easier to read.

	            If the space parameter is a non-empty string, then that string will
	            be used for indentation. If the space parameter is a number, then
	            the indentation will be that many spaces.

	            Example:

	            text = JSON.stringify(['e', {pluribus: 'unum'}]);
	            // text is '["e",{"pluribus":"unum"}]'


	            text = JSON.stringify(['e', {pluribus: 'unum'}], null, '\t');
	            // text is '[\n\t"e",\n\t{\n\t\t"pluribus": "unum"\n\t}\n]'

	            text = JSON.stringify([new Date()], function (key, value) {
	                return this[key] instanceof Date ?
	                    'Date(' + this[key] + ')' : value;
	            });
	            // text is '["Date(---current time---)"]'


	        JSON.parse(text, reviver)
	            This method parses a JSON text to produce an object or array.
	            It can throw a SyntaxError exception.

	            The optional reviver parameter is a function that can filter and
	            transform the results. It receives each of the keys and values,
	            and its return value is used instead of the original value.
	            If it returns what it received, then the structure is not modified.
	            If it returns undefined then the member is deleted.

	            Example:

	            // Parse the text. Values that look like ISO date strings will
	            // be converted to Date objects.

	            myData = JSON.parse(text, function (key, value) {
	                var a;
	                if (typeof value === 'string') {
	                    a =
	/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}(?:\.\d*)?)Z$/.exec(value);
	                    if (a) {
	                        return new Date(Date.UTC(+a[1], +a[2] - 1, +a[3], +a[4],
	                            +a[5], +a[6]));
	                    }
	                }
	                return value;
	            });

	            myData = JSON.parse('["Date(09/09/2001)"]', function (key, value) {
	                var d;
	                if (typeof value === 'string' &&
	                        value.slice(0, 5) === 'Date(' &&
	                        value.slice(-1) === ')') {
	                    d = new Date(value.slice(5, -1));
	                    if (d) {
	                        return d;
	                    }
	                }
	                return value;
	            });


	    This is a reference implementation. You are free to copy, modify, or
	    redistribute.
	*/

	/*jslint evil: true, regexp: true */

	/*members "", "\b", "\t", "\n", "\f", "\r", "\"", JSON, "\\", apply,
	    call, charCodeAt, getUTCDate, getUTCFullYear, getUTCHours,
	    getUTCMinutes, getUTCMonth, getUTCSeconds, hasOwnProperty, join,
	    lastIndex, length, parse, prototype, push, replace, slice, stringify,
	    test, toJSON, toString, valueOf
	*/


	// Create a JSON object only if one does not already exist. We create the
	// methods in a closure to avoid creating global variables.

	var JSON = module.exports;

	(function () {
	    'use strict';

	    function f(n) {
	        // Format integers to have at least two digits.
	        return n < 10 ? '0' + n : n;
	    }

	    var cx = /[\u0000\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,
	        escapable = /[\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,
	        gap,
	        indent,
	        meta = {    // table of character substitutions
	            '\b': '\\b',
	            '\t': '\\t',
	            '\n': '\\n',
	            '\f': '\\f',
	            '\r': '\\r',
	            '"' : '\\"',
	            '\\': '\\\\'
	        },
	        rep;


	    function quote(string) {

	// If the string contains no control characters, no quote characters, and no
	// backslash characters, then we can safely slap some quotes around it.
	// Otherwise we must also replace the offending characters with safe escape
	// sequences.

	        escapable.lastIndex = 0;
	        return escapable.test(string) ? '"' + string.replace(escapable, function (a) {
	            var c = meta[a];
	            return typeof c === 'string'
	                ? c
	                : '\\u' + ('0000' + a.charCodeAt(0).toString(16)).slice(-4);
	        }) + '"' : '"' + string + '"';
	    }


	    function str(key, holder) {

	// Produce a string from holder[key].

	        var i,          // The loop counter.
	            k,          // The member key.
	            v,          // The member value.
	            length,
	            mind = gap,
	            partial,
	            value = holder[key],
	            isBigNumber = value != null && (value instanceof BigNumber || value.isBigNumber);;

	// If the value has a toJSON method, call it to obtain a replacement value.

	        if (value && typeof value === 'object' &&
	                typeof value.toJSON === 'function') {
	            value = value.toJSON(key);
	        }

	// If we were called with a replacer function, then call the replacer to
	// obtain a replacement value.

	        if (typeof rep === 'function') {
	            value = rep.call(holder, key, value);
	        }

	// What happens next depends on the value's type.

	        switch (typeof value) {
	        case 'string':
	            if (isBigNumber) {
	                return value;
	            } else {
	                return quote(value);
	            }

	        case 'number':

	// JSON numbers must be finite. Encode non-finite numbers as null.

	            return isFinite(value) ? String(value) : 'null';

	        case 'boolean':
	        case 'null':

	// If the value is a boolean or null, convert it to a string. Note:
	// typeof null does not produce 'null'. The case is included here in
	// the remote chance that this gets fixed someday.

	            return String(value);

	// If the type is 'object', we might be dealing with an object or an array or
	// null.

	        case 'object':

	// Due to a specification blunder in ECMAScript, typeof null is 'object',
	// so watch out for that case.

	            if (!value) {
	                return 'null';
	            }

	// Make an array to hold the partial results of stringifying this object value.

	            gap += indent;
	            partial = [];

	// Is the value an array?

	            if (Object.prototype.toString.apply(value) === '[object Array]') {

	// The value is an array. Stringify every element. Use null as a placeholder
	// for non-JSON values.

	                length = value.length;
	                for (i = 0; i < length; i += 1) {
	                    partial[i] = str(i, value) || 'null';
	                }

	// Join all of the elements together, separated with commas, and wrap them in
	// brackets.

	                v = partial.length === 0
	                    ? '[]'
	                    : gap
	                    ? '[\n' + gap + partial.join(',\n' + gap) + '\n' + mind + ']'
	                    : '[' + partial.join(',') + ']';
	                gap = mind;
	                return v;
	            }

	// If the replacer is an array, use it to select the members to be stringified.

	            if (rep && typeof rep === 'object') {
	                length = rep.length;
	                for (i = 0; i < length; i += 1) {
	                    if (typeof rep[i] === 'string') {
	                        k = rep[i];
	                        v = str(k, value);
	                        if (v) {
	                            partial.push(quote(k) + (gap ? ': ' : ':') + v);
	                        }
	                    }
	                }
	            } else {

	// Otherwise, iterate through all of the keys in the object.

	                Object.keys(value).forEach(function(k) {
	                    var v = str(k, value);
	                    if (v) {
	                        partial.push(quote(k) + (gap ? ': ' : ':') + v);
	                    }
	                });
	            }

	// Join all of the member texts together, separated with commas,
	// and wrap them in braces.

	            v = partial.length === 0
	                ? '{}'
	                : gap
	                ? '{\n' + gap + partial.join(',\n' + gap) + '\n' + mind + '}'
	                : '{' + partial.join(',') + '}';
	            gap = mind;
	            return v;
	        }
	    }

	// If the JSON object does not yet have a stringify method, give it one.

	    if (typeof JSON.stringify !== 'function') {
	        JSON.stringify = function (value, replacer, space) {

	// The stringify method takes a value and an optional replacer, and an optional
	// space parameter, and returns a JSON text. The replacer can be a function
	// that can replace values, or an array of strings that will select the keys.
	// A default replacer method can be provided. Use of the space parameter can
	// produce text that is more easily readable.

	            var i;
	            gap = '';
	            indent = '';

	// If the space parameter is a number, make an indent string containing that
	// many spaces.

	            if (typeof space === 'number') {
	                for (i = 0; i < space; i += 1) {
	                    indent += ' ';
	                }

	// If the space parameter is a string, it will be used as the indent string.

	            } else if (typeof space === 'string') {
	                indent = space;
	            }

	// If there is a replacer, it must be a function or an array.
	// Otherwise, throw an error.

	            rep = replacer;
	            if (replacer && typeof replacer !== 'function' &&
	                    (typeof replacer !== 'object' ||
	                    typeof replacer.length !== 'number')) {
	                throw new Error('JSON.stringify');
	            }

	// Make a fake root object containing our value under the key of ''.
	// Return the result of stringifying the value.

	            return str('', {'': value});
	        };
	    }
	}());


/***/ },
/* 69 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_RESULT__;/*! bignumber.js v4.0.2 https://github.com/MikeMcl/bignumber.js/LICENCE */

	;(function (globalObj) {
	    'use strict';

	    /*
	      bignumber.js v4.0.2
	      A JavaScript library for arbitrary-precision arithmetic.
	      https://github.com/MikeMcl/bignumber.js
	      Copyright (c) 2017 Michael Mclaughlin <M8ch88l@gmail.com>
	      MIT Expat Licence
	    */


	    var BigNumber,
	        isNumeric = /^-?(\d+(\.\d*)?|\.\d+)(e[+-]?\d+)?$/i,
	        mathceil = Math.ceil,
	        mathfloor = Math.floor,
	        notBool = ' not a boolean or binary digit',
	        roundingMode = 'rounding mode',
	        tooManyDigits = 'number type has more than 15 significant digits',
	        ALPHABET = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ$_',
	        BASE = 1e14,
	        LOG_BASE = 14,
	        MAX_SAFE_INTEGER = 0x1fffffffffffff,         // 2^53 - 1
	        // MAX_INT32 = 0x7fffffff,                   // 2^31 - 1
	        POWS_TEN = [1, 10, 100, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12, 1e13],
	        SQRT_BASE = 1e7,

	        /*
	         * The limit on the value of DECIMAL_PLACES, TO_EXP_NEG, TO_EXP_POS, MIN_EXP, MAX_EXP, and
	         * the arguments to toExponential, toFixed, toFormat, and toPrecision, beyond which an
	         * exception is thrown (if ERRORS is true).
	         */
	        MAX = 1E9;                                   // 0 to MAX_INT32


	    /*
	     * Create and return a BigNumber constructor.
	     */
	    function constructorFactory(config) {
	        var div, parseNumeric,

	            // id tracks the caller function, so its name can be included in error messages.
	            id = 0,
	            P = BigNumber.prototype,
	            ONE = new BigNumber(1),


	            /********************************* EDITABLE DEFAULTS **********************************/


	            /*
	             * The default values below must be integers within the inclusive ranges stated.
	             * The values can also be changed at run-time using BigNumber.config.
	             */

	            // The maximum number of decimal places for operations involving division.
	            DECIMAL_PLACES = 20,                     // 0 to MAX

	            /*
	             * The rounding mode used when rounding to the above decimal places, and when using
	             * toExponential, toFixed, toFormat and toPrecision, and round (default value).
	             * UP         0 Away from zero.
	             * DOWN       1 Towards zero.
	             * CEIL       2 Towards +Infinity.
	             * FLOOR      3 Towards -Infinity.
	             * HALF_UP    4 Towards nearest neighbour. If equidistant, up.
	             * HALF_DOWN  5 Towards nearest neighbour. If equidistant, down.
	             * HALF_EVEN  6 Towards nearest neighbour. If equidistant, towards even neighbour.
	             * HALF_CEIL  7 Towards nearest neighbour. If equidistant, towards +Infinity.
	             * HALF_FLOOR 8 Towards nearest neighbour. If equidistant, towards -Infinity.
	             */
	            ROUNDING_MODE = 4,                       // 0 to 8

	            // EXPONENTIAL_AT : [TO_EXP_NEG , TO_EXP_POS]

	            // The exponent value at and beneath which toString returns exponential notation.
	            // Number type: -7
	            TO_EXP_NEG = -7,                         // 0 to -MAX

	            // The exponent value at and above which toString returns exponential notation.
	            // Number type: 21
	            TO_EXP_POS = 21,                         // 0 to MAX

	            // RANGE : [MIN_EXP, MAX_EXP]

	            // The minimum exponent value, beneath which underflow to zero occurs.
	            // Number type: -324  (5e-324)
	            MIN_EXP = -1e7,                          // -1 to -MAX

	            // The maximum exponent value, above which overflow to Infinity occurs.
	            // Number type:  308  (1.7976931348623157e+308)
	            // For MAX_EXP > 1e7, e.g. new BigNumber('1e100000000').plus(1) may be slow.
	            MAX_EXP = 1e7,                           // 1 to MAX

	            // Whether BigNumber Errors are ever thrown.
	            ERRORS = true,                           // true or false

	            // Change to intValidatorNoErrors if ERRORS is false.
	            isValidInt = intValidatorWithErrors,     // intValidatorWithErrors/intValidatorNoErrors

	            // Whether to use cryptographically-secure random number generation, if available.
	            CRYPTO = false,                          // true or false

	            /*
	             * The modulo mode used when calculating the modulus: a mod n.
	             * The quotient (q = a / n) is calculated according to the corresponding rounding mode.
	             * The remainder (r) is calculated as: r = a - n * q.
	             *
	             * UP        0 The remainder is positive if the dividend is negative, else is negative.
	             * DOWN      1 The remainder has the same sign as the dividend.
	             *             This modulo mode is commonly known as 'truncated division' and is
	             *             equivalent to (a % n) in JavaScript.
	             * FLOOR     3 The remainder has the same sign as the divisor (Python %).
	             * HALF_EVEN 6 This modulo mode implements the IEEE 754 remainder function.
	             * EUCLID    9 Euclidian division. q = sign(n) * floor(a / abs(n)).
	             *             The remainder is always positive.
	             *
	             * The truncated division, floored division, Euclidian division and IEEE 754 remainder
	             * modes are commonly used for the modulus operation.
	             * Although the other rounding modes can also be used, they may not give useful results.
	             */
	            MODULO_MODE = 1,                         // 0 to 9

	            // The maximum number of significant digits of the result of the toPower operation.
	            // If POW_PRECISION is 0, there will be unlimited significant digits.
	            POW_PRECISION = 0,                       // 0 to MAX

	            // The format specification used by the BigNumber.prototype.toFormat method.
	            FORMAT = {
	                decimalSeparator: '.',
	                groupSeparator: ',',
	                groupSize: 3,
	                secondaryGroupSize: 0,
	                fractionGroupSeparator: '\xA0',      // non-breaking space
	                fractionGroupSize: 0
	            };


	        /******************************************************************************************/


	        // CONSTRUCTOR


	        /*
	         * The BigNumber constructor and exported function.
	         * Create and return a new instance of a BigNumber object.
	         *
	         * n {number|string|BigNumber} A numeric value.
	         * [b] {number} The base of n. Integer, 2 to 64 inclusive.
	         */
	        function BigNumber( n, b ) {
	            var c, e, i, num, len, str,
	                x = this;

	            // Enable constructor usage without new.
	            if ( !( x instanceof BigNumber ) ) {

	                // 'BigNumber() constructor call without new: {n}'
	                if (ERRORS) raise( 26, 'constructor call without new', n );
	                return new BigNumber( n, b );
	            }

	            // 'new BigNumber() base not an integer: {b}'
	            // 'new BigNumber() base out of range: {b}'
	            if ( b == null || !isValidInt( b, 2, 64, id, 'base' ) ) {

	                // Duplicate.
	                if ( n instanceof BigNumber ) {
	                    x.s = n.s;
	                    x.e = n.e;
	                    x.c = ( n = n.c ) ? n.slice() : n;
	                    id = 0;
	                    return;
	                }

	                if ( ( num = typeof n == 'number' ) && n * 0 == 0 ) {
	                    x.s = 1 / n < 0 ? ( n = -n, -1 ) : 1;

	                    // Fast path for integers.
	                    if ( n === ~~n ) {
	                        for ( e = 0, i = n; i >= 10; i /= 10, e++ );
	                        x.e = e;
	                        x.c = [n];
	                        id = 0;
	                        return;
	                    }

	                    str = n + '';
	                } else {
	                    if ( !isNumeric.test( str = n + '' ) ) return parseNumeric( x, str, num );
	                    x.s = str.charCodeAt(0) === 45 ? ( str = str.slice(1), -1 ) : 1;
	                }
	            } else {
	                b = b | 0;
	                str = n + '';

	                // Ensure return value is rounded to DECIMAL_PLACES as with other bases.
	                // Allow exponential notation to be used with base 10 argument.
	                if ( b == 10 ) {
	                    x = new BigNumber( n instanceof BigNumber ? n : str );
	                    return round( x, DECIMAL_PLACES + x.e + 1, ROUNDING_MODE );
	                }

	                // Avoid potential interpretation of Infinity and NaN as base 44+ values.
	                // Any number in exponential form will fail due to the [Ee][+-].
	                if ( ( num = typeof n == 'number' ) && n * 0 != 0 ||
	                  !( new RegExp( '^-?' + ( c = '[' + ALPHABET.slice( 0, b ) + ']+' ) +
	                    '(?:\\.' + c + ')?$',b < 37 ? 'i' : '' ) ).test(str) ) {
	                    return parseNumeric( x, str, num, b );
	                }

	                if (num) {
	                    x.s = 1 / n < 0 ? ( str = str.slice(1), -1 ) : 1;

	                    if ( ERRORS && str.replace( /^0\.0*|\./, '' ).length > 15 ) {

	                        // 'new BigNumber() number type has more than 15 significant digits: {n}'
	                        raise( id, tooManyDigits, n );
	                    }

	                    // Prevent later check for length on converted number.
	                    num = false;
	                } else {
	                    x.s = str.charCodeAt(0) === 45 ? ( str = str.slice(1), -1 ) : 1;
	                }

	                str = convertBase( str, 10, b, x.s );
	            }

	            // Decimal point?
	            if ( ( e = str.indexOf('.') ) > -1 ) str = str.replace( '.', '' );

	            // Exponential form?
	            if ( ( i = str.search( /e/i ) ) > 0 ) {

	                // Determine exponent.
	                if ( e < 0 ) e = i;
	                e += +str.slice( i + 1 );
	                str = str.substring( 0, i );
	            } else if ( e < 0 ) {

	                // Integer.
	                e = str.length;
	            }

	            // Determine leading zeros.
	            for ( i = 0; str.charCodeAt(i) === 48; i++ );

	            // Determine trailing zeros.
	            for ( len = str.length; str.charCodeAt(--len) === 48; );
	            str = str.slice( i, len + 1 );

	            if (str) {
	                len = str.length;

	                // Disallow numbers with over 15 significant digits if number type.
	                // 'new BigNumber() number type has more than 15 significant digits: {n}'
	                if ( num && ERRORS && len > 15 && ( n > MAX_SAFE_INTEGER || n !== mathfloor(n) ) ) {
	                    raise( id, tooManyDigits, x.s * n );
	                }

	                e = e - i - 1;

	                 // Overflow?
	                if ( e > MAX_EXP ) {

	                    // Infinity.
	                    x.c = x.e = null;

	                // Underflow?
	                } else if ( e < MIN_EXP ) {

	                    // Zero.
	                    x.c = [ x.e = 0 ];
	                } else {
	                    x.e = e;
	                    x.c = [];

	                    // Transform base

	                    // e is the base 10 exponent.
	                    // i is where to slice str to get the first element of the coefficient array.
	                    i = ( e + 1 ) % LOG_BASE;
	                    if ( e < 0 ) i += LOG_BASE;

	                    if ( i < len ) {
	                        if (i) x.c.push( +str.slice( 0, i ) );

	                        for ( len -= LOG_BASE; i < len; ) {
	                            x.c.push( +str.slice( i, i += LOG_BASE ) );
	                        }

	                        str = str.slice(i);
	                        i = LOG_BASE - str.length;
	                    } else {
	                        i -= len;
	                    }

	                    for ( ; i--; str += '0' );
	                    x.c.push( +str );
	                }
	            } else {

	                // Zero.
	                x.c = [ x.e = 0 ];
	            }

	            id = 0;
	        }


	        // CONSTRUCTOR PROPERTIES


	        BigNumber.another = constructorFactory;

	        BigNumber.ROUND_UP = 0;
	        BigNumber.ROUND_DOWN = 1;
	        BigNumber.ROUND_CEIL = 2;
	        BigNumber.ROUND_FLOOR = 3;
	        BigNumber.ROUND_HALF_UP = 4;
	        BigNumber.ROUND_HALF_DOWN = 5;
	        BigNumber.ROUND_HALF_EVEN = 6;
	        BigNumber.ROUND_HALF_CEIL = 7;
	        BigNumber.ROUND_HALF_FLOOR = 8;
	        BigNumber.EUCLID = 9;


	        /*
	         * Configure infrequently-changing library-wide settings.
	         *
	         * Accept an object or an argument list, with one or many of the following properties or
	         * parameters respectively:
	         *
	         *   DECIMAL_PLACES  {number}  Integer, 0 to MAX inclusive
	         *   ROUNDING_MODE   {number}  Integer, 0 to 8 inclusive
	         *   EXPONENTIAL_AT  {number|number[]}  Integer, -MAX to MAX inclusive or
	         *                                      [integer -MAX to 0 incl., 0 to MAX incl.]
	         *   RANGE           {number|number[]}  Non-zero integer, -MAX to MAX inclusive or
	         *                                      [integer -MAX to -1 incl., integer 1 to MAX incl.]
	         *   ERRORS          {boolean|number}   true, false, 1 or 0
	         *   CRYPTO          {boolean|number}   true, false, 1 or 0
	         *   MODULO_MODE     {number}           0 to 9 inclusive
	         *   POW_PRECISION   {number}           0 to MAX inclusive
	         *   FORMAT          {object}           See BigNumber.prototype.toFormat
	         *      decimalSeparator       {string}
	         *      groupSeparator         {string}
	         *      groupSize              {number}
	         *      secondaryGroupSize     {number}
	         *      fractionGroupSeparator {string}
	         *      fractionGroupSize      {number}
	         *
	         * (The values assigned to the above FORMAT object properties are not checked for validity.)
	         *
	         * E.g.
	         * BigNumber.config(20, 4) is equivalent to
	         * BigNumber.config({ DECIMAL_PLACES : 20, ROUNDING_MODE : 4 })
	         *
	         * Ignore properties/parameters set to null or undefined.
	         * Return an object with the properties current values.
	         */
	        BigNumber.config = BigNumber.set = function () {
	            var v, p,
	                i = 0,
	                r = {},
	                a = arguments,
	                o = a[0],
	                has = o && typeof o == 'object'
	                  ? function () { if ( o.hasOwnProperty(p) ) return ( v = o[p] ) != null; }
	                  : function () { if ( a.length > i ) return ( v = a[i++] ) != null; };

	            // DECIMAL_PLACES {number} Integer, 0 to MAX inclusive.
	            // 'config() DECIMAL_PLACES not an integer: {v}'
	            // 'config() DECIMAL_PLACES out of range: {v}'
	            if ( has( p = 'DECIMAL_PLACES' ) && isValidInt( v, 0, MAX, 2, p ) ) {
	                DECIMAL_PLACES = v | 0;
	            }
	            r[p] = DECIMAL_PLACES;

	            // ROUNDING_MODE {number} Integer, 0 to 8 inclusive.
	            // 'config() ROUNDING_MODE not an integer: {v}'
	            // 'config() ROUNDING_MODE out of range: {v}'
	            if ( has( p = 'ROUNDING_MODE' ) && isValidInt( v, 0, 8, 2, p ) ) {
	                ROUNDING_MODE = v | 0;
	            }
	            r[p] = ROUNDING_MODE;

	            // EXPONENTIAL_AT {number|number[]}
	            // Integer, -MAX to MAX inclusive or [integer -MAX to 0 inclusive, 0 to MAX inclusive].
	            // 'config() EXPONENTIAL_AT not an integer: {v}'
	            // 'config() EXPONENTIAL_AT out of range: {v}'
	            if ( has( p = 'EXPONENTIAL_AT' ) ) {

	                if ( isArray(v) ) {
	                    if ( isValidInt( v[0], -MAX, 0, 2, p ) && isValidInt( v[1], 0, MAX, 2, p ) ) {
	                        TO_EXP_NEG = v[0] | 0;
	                        TO_EXP_POS = v[1] | 0;
	                    }
	                } else if ( isValidInt( v, -MAX, MAX, 2, p ) ) {
	                    TO_EXP_NEG = -( TO_EXP_POS = ( v < 0 ? -v : v ) | 0 );
	                }
	            }
	            r[p] = [ TO_EXP_NEG, TO_EXP_POS ];

	            // RANGE {number|number[]} Non-zero integer, -MAX to MAX inclusive or
	            // [integer -MAX to -1 inclusive, integer 1 to MAX inclusive].
	            // 'config() RANGE not an integer: {v}'
	            // 'config() RANGE cannot be zero: {v}'
	            // 'config() RANGE out of range: {v}'
	            if ( has( p = 'RANGE' ) ) {

	                if ( isArray(v) ) {
	                    if ( isValidInt( v[0], -MAX, -1, 2, p ) && isValidInt( v[1], 1, MAX, 2, p ) ) {
	                        MIN_EXP = v[0] | 0;
	                        MAX_EXP = v[1] | 0;
	                    }
	                } else if ( isValidInt( v, -MAX, MAX, 2, p ) ) {
	                    if ( v | 0 ) MIN_EXP = -( MAX_EXP = ( v < 0 ? -v : v ) | 0 );
	                    else if (ERRORS) raise( 2, p + ' cannot be zero', v );
	                }
	            }
	            r[p] = [ MIN_EXP, MAX_EXP ];

	            // ERRORS {boolean|number} true, false, 1 or 0.
	            // 'config() ERRORS not a boolean or binary digit: {v}'
	            if ( has( p = 'ERRORS' ) ) {

	                if ( v === !!v || v === 1 || v === 0 ) {
	                    id = 0;
	                    isValidInt = ( ERRORS = !!v ) ? intValidatorWithErrors : intValidatorNoErrors;
	                } else if (ERRORS) {
	                    raise( 2, p + notBool, v );
	                }
	            }
	            r[p] = ERRORS;

	            // CRYPTO {boolean|number} true, false, 1 or 0.
	            // 'config() CRYPTO not a boolean or binary digit: {v}'
	            // 'config() crypto unavailable: {crypto}'
	            if ( has( p = 'CRYPTO' ) ) {

	                if ( v === true || v === false || v === 1 || v === 0 ) {
	                    if (v) {
	                        v = typeof crypto == 'undefined';
	                        if ( !v && crypto && (crypto.getRandomValues || crypto.randomBytes)) {
	                            CRYPTO = true;
	                        } else if (ERRORS) {
	                            raise( 2, 'crypto unavailable', v ? void 0 : crypto );
	                        } else {
	                            CRYPTO = false;
	                        }
	                    } else {
	                        CRYPTO = false;
	                    }
	                } else if (ERRORS) {
	                    raise( 2, p + notBool, v );
	                }
	            }
	            r[p] = CRYPTO;

	            // MODULO_MODE {number} Integer, 0 to 9 inclusive.
	            // 'config() MODULO_MODE not an integer: {v}'
	            // 'config() MODULO_MODE out of range: {v}'
	            if ( has( p = 'MODULO_MODE' ) && isValidInt( v, 0, 9, 2, p ) ) {
	                MODULO_MODE = v | 0;
	            }
	            r[p] = MODULO_MODE;

	            // POW_PRECISION {number} Integer, 0 to MAX inclusive.
	            // 'config() POW_PRECISION not an integer: {v}'
	            // 'config() POW_PRECISION out of range: {v}'
	            if ( has( p = 'POW_PRECISION' ) && isValidInt( v, 0, MAX, 2, p ) ) {
	                POW_PRECISION = v | 0;
	            }
	            r[p] = POW_PRECISION;

	            // FORMAT {object}
	            // 'config() FORMAT not an object: {v}'
	            if ( has( p = 'FORMAT' ) ) {

	                if ( typeof v == 'object' ) {
	                    FORMAT = v;
	                } else if (ERRORS) {
	                    raise( 2, p + ' not an object', v );
	                }
	            }
	            r[p] = FORMAT;

	            return r;
	        };


	        /*
	         * Return a new BigNumber whose value is the maximum of the arguments.
	         *
	         * arguments {number|string|BigNumber}
	         */
	        BigNumber.max = function () { return maxOrMin( arguments, P.lt ); };


	        /*
	         * Return a new BigNumber whose value is the minimum of the arguments.
	         *
	         * arguments {number|string|BigNumber}
	         */
	        BigNumber.min = function () { return maxOrMin( arguments, P.gt ); };


	        /*
	         * Return a new BigNumber with a random value equal to or greater than 0 and less than 1,
	         * and with dp, or DECIMAL_PLACES if dp is omitted, decimal places (or less if trailing
	         * zeros are produced).
	         *
	         * [dp] {number} Decimal places. Integer, 0 to MAX inclusive.
	         *
	         * 'random() decimal places not an integer: {dp}'
	         * 'random() decimal places out of range: {dp}'
	         * 'random() crypto unavailable: {crypto}'
	         */
	        BigNumber.random = (function () {
	            var pow2_53 = 0x20000000000000;

	            // Return a 53 bit integer n, where 0 <= n < 9007199254740992.
	            // Check if Math.random() produces more than 32 bits of randomness.
	            // If it does, assume at least 53 bits are produced, otherwise assume at least 30 bits.
	            // 0x40000000 is 2^30, 0x800000 is 2^23, 0x1fffff is 2^21 - 1.
	            var random53bitInt = (Math.random() * pow2_53) & 0x1fffff
	              ? function () { return mathfloor( Math.random() * pow2_53 ); }
	              : function () { return ((Math.random() * 0x40000000 | 0) * 0x800000) +
	                  (Math.random() * 0x800000 | 0); };

	            return function (dp) {
	                var a, b, e, k, v,
	                    i = 0,
	                    c = [],
	                    rand = new BigNumber(ONE);

	                dp = dp == null || !isValidInt( dp, 0, MAX, 14 ) ? DECIMAL_PLACES : dp | 0;
	                k = mathceil( dp / LOG_BASE );

	                if (CRYPTO) {

	                    // Browsers supporting crypto.getRandomValues.
	                    if (crypto.getRandomValues) {

	                        a = crypto.getRandomValues( new Uint32Array( k *= 2 ) );

	                        for ( ; i < k; ) {

	                            // 53 bits:
	                            // ((Math.pow(2, 32) - 1) * Math.pow(2, 21)).toString(2)
	                            // 11111 11111111 11111111 11111111 11100000 00000000 00000000
	                            // ((Math.pow(2, 32) - 1) >>> 11).toString(2)
	                            //                                     11111 11111111 11111111
	                            // 0x20000 is 2^21.
	                            v = a[i] * 0x20000 + (a[i + 1] >>> 11);

	                            // Rejection sampling:
	                            // 0 <= v < 9007199254740992
	                            // Probability that v >= 9e15, is
	                            // 7199254740992 / 9007199254740992 ~= 0.0008, i.e. 1 in 1251
	                            if ( v >= 9e15 ) {
	                                b = crypto.getRandomValues( new Uint32Array(2) );
	                                a[i] = b[0];
	                                a[i + 1] = b[1];
	                            } else {

	                                // 0 <= v <= 8999999999999999
	                                // 0 <= (v % 1e14) <= 99999999999999
	                                c.push( v % 1e14 );
	                                i += 2;
	                            }
	                        }
	                        i = k / 2;

	                    // Node.js supporting crypto.randomBytes.
	                    } else if (crypto.randomBytes) {

	                        // buffer
	                        a = crypto.randomBytes( k *= 7 );

	                        for ( ; i < k; ) {

	                            // 0x1000000000000 is 2^48, 0x10000000000 is 2^40
	                            // 0x100000000 is 2^32, 0x1000000 is 2^24
	                            // 11111 11111111 11111111 11111111 11111111 11111111 11111111
	                            // 0 <= v < 9007199254740992
	                            v = ( ( a[i] & 31 ) * 0x1000000000000 ) + ( a[i + 1] * 0x10000000000 ) +
	                                  ( a[i + 2] * 0x100000000 ) + ( a[i + 3] * 0x1000000 ) +
	                                  ( a[i + 4] << 16 ) + ( a[i + 5] << 8 ) + a[i + 6];

	                            if ( v >= 9e15 ) {
	                                crypto.randomBytes(7).copy( a, i );
	                            } else {

	                                // 0 <= (v % 1e14) <= 99999999999999
	                                c.push( v % 1e14 );
	                                i += 7;
	                            }
	                        }
	                        i = k / 7;
	                    } else {
	                        CRYPTO = false;
	                        if (ERRORS) raise( 14, 'crypto unavailable', crypto );
	                    }
	                }

	                // Use Math.random.
	                if (!CRYPTO) {

	                    for ( ; i < k; ) {
	                        v = random53bitInt();
	                        if ( v < 9e15 ) c[i++] = v % 1e14;
	                    }
	                }

	                k = c[--i];
	                dp %= LOG_BASE;

	                // Convert trailing digits to zeros according to dp.
	                if ( k && dp ) {
	                    v = POWS_TEN[LOG_BASE - dp];
	                    c[i] = mathfloor( k / v ) * v;
	                }

	                // Remove trailing elements which are zero.
	                for ( ; c[i] === 0; c.pop(), i-- );

	                // Zero?
	                if ( i < 0 ) {
	                    c = [ e = 0 ];
	                } else {

	                    // Remove leading elements which are zero and adjust exponent accordingly.
	                    for ( e = -1 ; c[0] === 0; c.splice(0, 1), e -= LOG_BASE);

	                    // Count the digits of the first element of c to determine leading zeros, and...
	                    for ( i = 1, v = c[0]; v >= 10; v /= 10, i++);

	                    // adjust the exponent accordingly.
	                    if ( i < LOG_BASE ) e -= LOG_BASE - i;
	                }

	                rand.e = e;
	                rand.c = c;
	                return rand;
	            };
	        })();


	        // PRIVATE FUNCTIONS


	        // Convert a numeric string of baseIn to a numeric string of baseOut.
	        function convertBase( str, baseOut, baseIn, sign ) {
	            var d, e, k, r, x, xc, y,
	                i = str.indexOf( '.' ),
	                dp = DECIMAL_PLACES,
	                rm = ROUNDING_MODE;

	            if ( baseIn < 37 ) str = str.toLowerCase();

	            // Non-integer.
	            if ( i >= 0 ) {
	                k = POW_PRECISION;

	                // Unlimited precision.
	                POW_PRECISION = 0;
	                str = str.replace( '.', '' );
	                y = new BigNumber(baseIn);
	                x = y.pow( str.length - i );
	                POW_PRECISION = k;

	                // Convert str as if an integer, then restore the fraction part by dividing the
	                // result by its base raised to a power.
	                y.c = toBaseOut( toFixedPoint( coeffToString( x.c ), x.e ), 10, baseOut );
	                y.e = y.c.length;
	            }

	            // Convert the number as integer.
	            xc = toBaseOut( str, baseIn, baseOut );
	            e = k = xc.length;

	            // Remove trailing zeros.
	            for ( ; xc[--k] == 0; xc.pop() );
	            if ( !xc[0] ) return '0';

	            if ( i < 0 ) {
	                --e;
	            } else {
	                x.c = xc;
	                x.e = e;

	                // sign is needed for correct rounding.
	                x.s = sign;
	                x = div( x, y, dp, rm, baseOut );
	                xc = x.c;
	                r = x.r;
	                e = x.e;
	            }

	            d = e + dp + 1;

	            // The rounding digit, i.e. the digit to the right of the digit that may be rounded up.
	            i = xc[d];
	            k = baseOut / 2;
	            r = r || d < 0 || xc[d + 1] != null;

	            r = rm < 4 ? ( i != null || r ) && ( rm == 0 || rm == ( x.s < 0 ? 3 : 2 ) )
	                       : i > k || i == k &&( rm == 4 || r || rm == 6 && xc[d - 1] & 1 ||
	                         rm == ( x.s < 0 ? 8 : 7 ) );

	            if ( d < 1 || !xc[0] ) {

	                // 1^-dp or 0.
	                str = r ? toFixedPoint( '1', -dp ) : '0';
	            } else {
	                xc.length = d;

	                if (r) {

	                    // Rounding up may mean the previous digit has to be rounded up and so on.
	                    for ( --baseOut; ++xc[--d] > baseOut; ) {
	                        xc[d] = 0;

	                        if ( !d ) {
	                            ++e;
	                            xc = [1].concat(xc);
	                        }
	                    }
	                }

	                // Determine trailing zeros.
	                for ( k = xc.length; !xc[--k]; );

	                // E.g. [4, 11, 15] becomes 4bf.
	                for ( i = 0, str = ''; i <= k; str += ALPHABET.charAt( xc[i++] ) );
	                str = toFixedPoint( str, e );
	            }

	            // The caller will add the sign.
	            return str;
	        }


	        // Perform division in the specified base. Called by div and convertBase.
	        div = (function () {

	            // Assume non-zero x and k.
	            function multiply( x, k, base ) {
	                var m, temp, xlo, xhi,
	                    carry = 0,
	                    i = x.length,
	                    klo = k % SQRT_BASE,
	                    khi = k / SQRT_BASE | 0;

	                for ( x = x.slice(); i--; ) {
	                    xlo = x[i] % SQRT_BASE;
	                    xhi = x[i] / SQRT_BASE | 0;
	                    m = khi * xlo + xhi * klo;
	                    temp = klo * xlo + ( ( m % SQRT_BASE ) * SQRT_BASE ) + carry;
	                    carry = ( temp / base | 0 ) + ( m / SQRT_BASE | 0 ) + khi * xhi;
	                    x[i] = temp % base;
	                }

	                if (carry) x = [carry].concat(x);

	                return x;
	            }

	            function compare( a, b, aL, bL ) {
	                var i, cmp;

	                if ( aL != bL ) {
	                    cmp = aL > bL ? 1 : -1;
	                } else {

	                    for ( i = cmp = 0; i < aL; i++ ) {

	                        if ( a[i] != b[i] ) {
	                            cmp = a[i] > b[i] ? 1 : -1;
	                            break;
	                        }
	                    }
	                }
	                return cmp;
	            }

	            function subtract( a, b, aL, base ) {
	                var i = 0;

	                // Subtract b from a.
	                for ( ; aL--; ) {
	                    a[aL] -= i;
	                    i = a[aL] < b[aL] ? 1 : 0;
	                    a[aL] = i * base + a[aL] - b[aL];
	                }

	                // Remove leading zeros.
	                for ( ; !a[0] && a.length > 1; a.splice(0, 1) );
	            }

	            // x: dividend, y: divisor.
	            return function ( x, y, dp, rm, base ) {
	                var cmp, e, i, more, n, prod, prodL, q, qc, rem, remL, rem0, xi, xL, yc0,
	                    yL, yz,
	                    s = x.s == y.s ? 1 : -1,
	                    xc = x.c,
	                    yc = y.c;

	                // Either NaN, Infinity or 0?
	                if ( !xc || !xc[0] || !yc || !yc[0] ) {

	                    return new BigNumber(

	                      // Return NaN if either NaN, or both Infinity or 0.
	                      !x.s || !y.s || ( xc ? yc && xc[0] == yc[0] : !yc ) ? NaN :

	                        // Return ±0 if x is ±0 or y is ±Infinity, or return ±Infinity as y is ±0.
	                        xc && xc[0] == 0 || !yc ? s * 0 : s / 0
	                    );
	                }

	                q = new BigNumber(s);
	                qc = q.c = [];
	                e = x.e - y.e;
	                s = dp + e + 1;

	                if ( !base ) {
	                    base = BASE;
	                    e = bitFloor( x.e / LOG_BASE ) - bitFloor( y.e / LOG_BASE );
	                    s = s / LOG_BASE | 0;
	                }

	                // Result exponent may be one less then the current value of e.
	                // The coefficients of the BigNumbers from convertBase may have trailing zeros.
	                for ( i = 0; yc[i] == ( xc[i] || 0 ); i++ );
	                if ( yc[i] > ( xc[i] || 0 ) ) e--;

	                if ( s < 0 ) {
	                    qc.push(1);
	                    more = true;
	                } else {
	                    xL = xc.length;
	                    yL = yc.length;
	                    i = 0;
	                    s += 2;

	                    // Normalise xc and yc so highest order digit of yc is >= base / 2.

	                    n = mathfloor( base / ( yc[0] + 1 ) );

	                    // Not necessary, but to handle odd bases where yc[0] == ( base / 2 ) - 1.
	                    // if ( n > 1 || n++ == 1 && yc[0] < base / 2 ) {
	                    if ( n > 1 ) {
	                        yc = multiply( yc, n, base );
	                        xc = multiply( xc, n, base );
	                        yL = yc.length;
	                        xL = xc.length;
	                    }

	                    xi = yL;
	                    rem = xc.slice( 0, yL );
	                    remL = rem.length;

	                    // Add zeros to make remainder as long as divisor.
	                    for ( ; remL < yL; rem[remL++] = 0 );
	                    yz = yc.slice();
	                    yz = [0].concat(yz);
	                    yc0 = yc[0];
	                    if ( yc[1] >= base / 2 ) yc0++;
	                    // Not necessary, but to prevent trial digit n > base, when using base 3.
	                    // else if ( base == 3 && yc0 == 1 ) yc0 = 1 + 1e-15;

	                    do {
	                        n = 0;

	                        // Compare divisor and remainder.
	                        cmp = compare( yc, rem, yL, remL );

	                        // If divisor < remainder.
	                        if ( cmp < 0 ) {

	                            // Calculate trial digit, n.

	                            rem0 = rem[0];
	                            if ( yL != remL ) rem0 = rem0 * base + ( rem[1] || 0 );

	                            // n is how many times the divisor goes into the current remainder.
	                            n = mathfloor( rem0 / yc0 );

	                            //  Algorithm:
	                            //  1. product = divisor * trial digit (n)
	                            //  2. if product > remainder: product -= divisor, n--
	                            //  3. remainder -= product
	                            //  4. if product was < remainder at 2:
	                            //    5. compare new remainder and divisor
	                            //    6. If remainder > divisor: remainder -= divisor, n++

	                            if ( n > 1 ) {

	                                // n may be > base only when base is 3.
	                                if (n >= base) n = base - 1;

	                                // product = divisor * trial digit.
	                                prod = multiply( yc, n, base );
	                                prodL = prod.length;
	                                remL = rem.length;

	                                // Compare product and remainder.
	                                // If product > remainder.
	                                // Trial digit n too high.
	                                // n is 1 too high about 5% of the time, and is not known to have
	                                // ever been more than 1 too high.
	                                while ( compare( prod, rem, prodL, remL ) == 1 ) {
	                                    n--;

	                                    // Subtract divisor from product.
	                                    subtract( prod, yL < prodL ? yz : yc, prodL, base );
	                                    prodL = prod.length;
	                                    cmp = 1;
	                                }
	                            } else {

	                                // n is 0 or 1, cmp is -1.
	                                // If n is 0, there is no need to compare yc and rem again below,
	                                // so change cmp to 1 to avoid it.
	                                // If n is 1, leave cmp as -1, so yc and rem are compared again.
	                                if ( n == 0 ) {

	                                    // divisor < remainder, so n must be at least 1.
	                                    cmp = n = 1;
	                                }

	                                // product = divisor
	                                prod = yc.slice();
	                                prodL = prod.length;
	                            }

	                            if ( prodL < remL ) prod = [0].concat(prod);

	                            // Subtract product from remainder.
	                            subtract( rem, prod, remL, base );
	                            remL = rem.length;

	                             // If product was < remainder.
	                            if ( cmp == -1 ) {

	                                // Compare divisor and new remainder.
	                                // If divisor < new remainder, subtract divisor from remainder.
	                                // Trial digit n too low.
	                                // n is 1 too low about 5% of the time, and very rarely 2 too low.
	                                while ( compare( yc, rem, yL, remL ) < 1 ) {
	                                    n++;

	                                    // Subtract divisor from remainder.
	                                    subtract( rem, yL < remL ? yz : yc, remL, base );
	                                    remL = rem.length;
	                                }
	                            }
	                        } else if ( cmp === 0 ) {
	                            n++;
	                            rem = [0];
	                        } // else cmp === 1 and n will be 0

	                        // Add the next digit, n, to the result array.
	                        qc[i++] = n;

	                        // Update the remainder.
	                        if ( rem[0] ) {
	                            rem[remL++] = xc[xi] || 0;
	                        } else {
	                            rem = [ xc[xi] ];
	                            remL = 1;
	                        }
	                    } while ( ( xi++ < xL || rem[0] != null ) && s-- );

	                    more = rem[0] != null;

	                    // Leading zero?
	                    if ( !qc[0] ) qc.splice(0, 1);
	                }

	                if ( base == BASE ) {

	                    // To calculate q.e, first get the number of digits of qc[0].
	                    for ( i = 1, s = qc[0]; s >= 10; s /= 10, i++ );
	                    round( q, dp + ( q.e = i + e * LOG_BASE - 1 ) + 1, rm, more );

	                // Caller is convertBase.
	                } else {
	                    q.e = e;
	                    q.r = +more;
	                }

	                return q;
	            };
	        })();


	        /*
	         * Return a string representing the value of BigNumber n in fixed-point or exponential
	         * notation rounded to the specified decimal places or significant digits.
	         *
	         * n is a BigNumber.
	         * i is the index of the last digit required (i.e. the digit that may be rounded up).
	         * rm is the rounding mode.
	         * caller is caller id: toExponential 19, toFixed 20, toFormat 21, toPrecision 24.
	         */
	        function format( n, i, rm, caller ) {
	            var c0, e, ne, len, str;

	            rm = rm != null && isValidInt( rm, 0, 8, caller, roundingMode )
	              ? rm | 0 : ROUNDING_MODE;

	            if ( !n.c ) return n.toString();
	            c0 = n.c[0];
	            ne = n.e;

	            if ( i == null ) {
	                str = coeffToString( n.c );
	                str = caller == 19 || caller == 24 && ne <= TO_EXP_NEG
	                  ? toExponential( str, ne )
	                  : toFixedPoint( str, ne );
	            } else {
	                n = round( new BigNumber(n), i, rm );

	                // n.e may have changed if the value was rounded up.
	                e = n.e;

	                str = coeffToString( n.c );
	                len = str.length;

	                // toPrecision returns exponential notation if the number of significant digits
	                // specified is less than the number of digits necessary to represent the integer
	                // part of the value in fixed-point notation.

	                // Exponential notation.
	                if ( caller == 19 || caller == 24 && ( i <= e || e <= TO_EXP_NEG ) ) {

	                    // Append zeros?
	                    for ( ; len < i; str += '0', len++ );
	                    str = toExponential( str, e );

	                // Fixed-point notation.
	                } else {
	                    i -= ne;
	                    str = toFixedPoint( str, e );

	                    // Append zeros?
	                    if ( e + 1 > len ) {
	                        if ( --i > 0 ) for ( str += '.'; i--; str += '0' );
	                    } else {
	                        i += e - len;
	                        if ( i > 0 ) {
	                            if ( e + 1 == len ) str += '.';
	                            for ( ; i--; str += '0' );
	                        }
	                    }
	                }
	            }

	            return n.s < 0 && c0 ? '-' + str : str;
	        }


	        // Handle BigNumber.max and BigNumber.min.
	        function maxOrMin( args, method ) {
	            var m, n,
	                i = 0;

	            if ( isArray( args[0] ) ) args = args[0];
	            m = new BigNumber( args[0] );

	            for ( ; ++i < args.length; ) {
	                n = new BigNumber( args[i] );

	                // If any number is NaN, return NaN.
	                if ( !n.s ) {
	                    m = n;
	                    break;
	                } else if ( method.call( m, n ) ) {
	                    m = n;
	                }
	            }

	            return m;
	        }


	        /*
	         * Return true if n is an integer in range, otherwise throw.
	         * Use for argument validation when ERRORS is true.
	         */
	        function intValidatorWithErrors( n, min, max, caller, name ) {
	            if ( n < min || n > max || n != truncate(n) ) {
	                raise( caller, ( name || 'decimal places' ) +
	                  ( n < min || n > max ? ' out of range' : ' not an integer' ), n );
	            }

	            return true;
	        }


	        /*
	         * Strip trailing zeros, calculate base 10 exponent and check against MIN_EXP and MAX_EXP.
	         * Called by minus, plus and times.
	         */
	        function normalise( n, c, e ) {
	            var i = 1,
	                j = c.length;

	             // Remove trailing zeros.
	            for ( ; !c[--j]; c.pop() );

	            // Calculate the base 10 exponent. First get the number of digits of c[0].
	            for ( j = c[0]; j >= 10; j /= 10, i++ );

	            // Overflow?
	            if ( ( e = i + e * LOG_BASE - 1 ) > MAX_EXP ) {

	                // Infinity.
	                n.c = n.e = null;

	            // Underflow?
	            } else if ( e < MIN_EXP ) {

	                // Zero.
	                n.c = [ n.e = 0 ];
	            } else {
	                n.e = e;
	                n.c = c;
	            }

	            return n;
	        }


	        // Handle values that fail the validity test in BigNumber.
	        parseNumeric = (function () {
	            var basePrefix = /^(-?)0([xbo])(?=\w[\w.]*$)/i,
	                dotAfter = /^([^.]+)\.$/,
	                dotBefore = /^\.([^.]+)$/,
	                isInfinityOrNaN = /^-?(Infinity|NaN)$/,
	                whitespaceOrPlus = /^\s*\+(?=[\w.])|^\s+|\s+$/g;

	            return function ( x, str, num, b ) {
	                var base,
	                    s = num ? str : str.replace( whitespaceOrPlus, '' );

	                // No exception on ±Infinity or NaN.
	                if ( isInfinityOrNaN.test(s) ) {
	                    x.s = isNaN(s) ? null : s < 0 ? -1 : 1;
	                } else {
	                    if ( !num ) {

	                        // basePrefix = /^(-?)0([xbo])(?=\w[\w.]*$)/i
	                        s = s.replace( basePrefix, function ( m, p1, p2 ) {
	                            base = ( p2 = p2.toLowerCase() ) == 'x' ? 16 : p2 == 'b' ? 2 : 8;
	                            return !b || b == base ? p1 : m;
	                        });

	                        if (b) {
	                            base = b;

	                            // E.g. '1.' to '1', '.1' to '0.1'
	                            s = s.replace( dotAfter, '$1' ).replace( dotBefore, '0.$1' );
	                        }

	                        if ( str != s ) return new BigNumber( s, base );
	                    }

	                    // 'new BigNumber() not a number: {n}'
	                    // 'new BigNumber() not a base {b} number: {n}'
	                    if (ERRORS) raise( id, 'not a' + ( b ? ' base ' + b : '' ) + ' number', str );
	                    x.s = null;
	                }

	                x.c = x.e = null;
	                id = 0;
	            }
	        })();


	        // Throw a BigNumber Error.
	        function raise( caller, msg, val ) {
	            var error = new Error( [
	                'new BigNumber',     // 0
	                'cmp',               // 1
	                'config',            // 2
	                'div',               // 3
	                'divToInt',          // 4
	                'eq',                // 5
	                'gt',                // 6
	                'gte',               // 7
	                'lt',                // 8
	                'lte',               // 9
	                'minus',             // 10
	                'mod',               // 11
	                'plus',              // 12
	                'precision',         // 13
	                'random',            // 14
	                'round',             // 15
	                'shift',             // 16
	                'times',             // 17
	                'toDigits',          // 18
	                'toExponential',     // 19
	                'toFixed',           // 20
	                'toFormat',          // 21
	                'toFraction',        // 22
	                'pow',               // 23
	                'toPrecision',       // 24
	                'toString',          // 25
	                'BigNumber'          // 26
	            ][caller] + '() ' + msg + ': ' + val );

	            error.name = 'BigNumber Error';
	            id = 0;
	            throw error;
	        }


	        /*
	         * Round x to sd significant digits using rounding mode rm. Check for over/under-flow.
	         * If r is truthy, it is known that there are more digits after the rounding digit.
	         */
	        function round( x, sd, rm, r ) {
	            var d, i, j, k, n, ni, rd,
	                xc = x.c,
	                pows10 = POWS_TEN;

	            // if x is not Infinity or NaN...
	            if (xc) {

	                // rd is the rounding digit, i.e. the digit after the digit that may be rounded up.
	                // n is a base 1e14 number, the value of the element of array x.c containing rd.
	                // ni is the index of n within x.c.
	                // d is the number of digits of n.
	                // i is the index of rd within n including leading zeros.
	                // j is the actual index of rd within n (if < 0, rd is a leading zero).
	                out: {

	                    // Get the number of digits of the first element of xc.
	                    for ( d = 1, k = xc[0]; k >= 10; k /= 10, d++ );
	                    i = sd - d;

	                    // If the rounding digit is in the first element of xc...
	                    if ( i < 0 ) {
	                        i += LOG_BASE;
	                        j = sd;
	                        n = xc[ ni = 0 ];

	                        // Get the rounding digit at index j of n.
	                        rd = n / pows10[ d - j - 1 ] % 10 | 0;
	                    } else {
	                        ni = mathceil( ( i + 1 ) / LOG_BASE );

	                        if ( ni >= xc.length ) {

	                            if (r) {

	                                // Needed by sqrt.
	                                for ( ; xc.length <= ni; xc.push(0) );
	                                n = rd = 0;
	                                d = 1;
	                                i %= LOG_BASE;
	                                j = i - LOG_BASE + 1;
	                            } else {
	                                break out;
	                            }
	                        } else {
	                            n = k = xc[ni];

	                            // Get the number of digits of n.
	                            for ( d = 1; k >= 10; k /= 10, d++ );

	                            // Get the index of rd within n.
	                            i %= LOG_BASE;

	                            // Get the index of rd within n, adjusted for leading zeros.
	                            // The number of leading zeros of n is given by LOG_BASE - d.
	                            j = i - LOG_BASE + d;

	                            // Get the rounding digit at index j of n.
	                            rd = j < 0 ? 0 : n / pows10[ d - j - 1 ] % 10 | 0;
	                        }
	                    }

	                    r = r || sd < 0 ||

	                    // Are there any non-zero digits after the rounding digit?
	                    // The expression  n % pows10[ d - j - 1 ]  returns all digits of n to the right
	                    // of the digit at j, e.g. if n is 908714 and j is 2, the expression gives 714.
	                      xc[ni + 1] != null || ( j < 0 ? n : n % pows10[ d - j - 1 ] );

	                    r = rm < 4
	                      ? ( rd || r ) && ( rm == 0 || rm == ( x.s < 0 ? 3 : 2 ) )
	                      : rd > 5 || rd == 5 && ( rm == 4 || r || rm == 6 &&

	                        // Check whether the digit to the left of the rounding digit is odd.
	                        ( ( i > 0 ? j > 0 ? n / pows10[ d - j ] : 0 : xc[ni - 1] ) % 10 ) & 1 ||
	                          rm == ( x.s < 0 ? 8 : 7 ) );

	                    if ( sd < 1 || !xc[0] ) {
	                        xc.length = 0;

	                        if (r) {

	                            // Convert sd to decimal places.
	                            sd -= x.e + 1;

	                            // 1, 0.1, 0.01, 0.001, 0.0001 etc.
	                            xc[0] = pows10[ ( LOG_BASE - sd % LOG_BASE ) % LOG_BASE ];
	                            x.e = -sd || 0;
	                        } else {

	                            // Zero.
	                            xc[0] = x.e = 0;
	                        }

	                        return x;
	                    }

	                    // Remove excess digits.
	                    if ( i == 0 ) {
	                        xc.length = ni;
	                        k = 1;
	                        ni--;
	                    } else {
	                        xc.length = ni + 1;
	                        k = pows10[ LOG_BASE - i ];

	                        // E.g. 56700 becomes 56000 if 7 is the rounding digit.
	                        // j > 0 means i > number of leading zeros of n.
	                        xc[ni] = j > 0 ? mathfloor( n / pows10[ d - j ] % pows10[j] ) * k : 0;
	                    }

	                    // Round up?
	                    if (r) {

	                        for ( ; ; ) {

	                            // If the digit to be rounded up is in the first element of xc...
	                            if ( ni == 0 ) {

	                                // i will be the length of xc[0] before k is added.
	                                for ( i = 1, j = xc[0]; j >= 10; j /= 10, i++ );
	                                j = xc[0] += k;
	                                for ( k = 1; j >= 10; j /= 10, k++ );

	                                // if i != k the length has increased.
	                                if ( i != k ) {
	                                    x.e++;
	                                    if ( xc[0] == BASE ) xc[0] = 1;
	                                }

	                                break;
	                            } else {
	                                xc[ni] += k;
	                                if ( xc[ni] != BASE ) break;
	                                xc[ni--] = 0;
	                                k = 1;
	                            }
	                        }
	                    }

	                    // Remove trailing zeros.
	                    for ( i = xc.length; xc[--i] === 0; xc.pop() );
	                }

	                // Overflow? Infinity.
	                if ( x.e > MAX_EXP ) {
	                    x.c = x.e = null;

	                // Underflow? Zero.
	                } else if ( x.e < MIN_EXP ) {
	                    x.c = [ x.e = 0 ];
	                }
	            }

	            return x;
	        }


	        // PROTOTYPE/INSTANCE METHODS


	        /*
	         * Return a new BigNumber whose value is the absolute value of this BigNumber.
	         */
	        P.absoluteValue = P.abs = function () {
	            var x = new BigNumber(this);
	            if ( x.s < 0 ) x.s = 1;
	            return x;
	        };


	        /*
	         * Return a new BigNumber whose value is the value of this BigNumber rounded to a whole
	         * number in the direction of Infinity.
	         */
	        P.ceil = function () {
	            return round( new BigNumber(this), this.e + 1, 2 );
	        };


	        /*
	         * Return
	         * 1 if the value of this BigNumber is greater than the value of BigNumber(y, b),
	         * -1 if the value of this BigNumber is less than the value of BigNumber(y, b),
	         * 0 if they have the same value,
	         * or null if the value of either is NaN.
	         */
	        P.comparedTo = P.cmp = function ( y, b ) {
	            id = 1;
	            return compare( this, new BigNumber( y, b ) );
	        };


	        /*
	         * Return the number of decimal places of the value of this BigNumber, or null if the value
	         * of this BigNumber is ±Infinity or NaN.
	         */
	        P.decimalPlaces = P.dp = function () {
	            var n, v,
	                c = this.c;

	            if ( !c ) return null;
	            n = ( ( v = c.length - 1 ) - bitFloor( this.e / LOG_BASE ) ) * LOG_BASE;

	            // Subtract the number of trailing zeros of the last number.
	            if ( v = c[v] ) for ( ; v % 10 == 0; v /= 10, n-- );
	            if ( n < 0 ) n = 0;

	            return n;
	        };


	        /*
	         *  n / 0 = I
	         *  n / N = N
	         *  n / I = 0
	         *  0 / n = 0
	         *  0 / 0 = N
	         *  0 / N = N
	         *  0 / I = 0
	         *  N / n = N
	         *  N / 0 = N
	         *  N / N = N
	         *  N / I = N
	         *  I / n = I
	         *  I / 0 = I
	         *  I / N = N
	         *  I / I = N
	         *
	         * Return a new BigNumber whose value is the value of this BigNumber divided by the value of
	         * BigNumber(y, b), rounded according to DECIMAL_PLACES and ROUNDING_MODE.
	         */
	        P.dividedBy = P.div = function ( y, b ) {
	            id = 3;
	            return div( this, new BigNumber( y, b ), DECIMAL_PLACES, ROUNDING_MODE );
	        };


	        /*
	         * Return a new BigNumber whose value is the integer part of dividing the value of this
	         * BigNumber by the value of BigNumber(y, b).
	         */
	        P.dividedToIntegerBy = P.divToInt = function ( y, b ) {
	            id = 4;
	            return div( this, new BigNumber( y, b ), 0, 1 );
	        };


	        /*
	         * Return true if the value of this BigNumber is equal to the value of BigNumber(y, b),
	         * otherwise returns false.
	         */
	        P.equals = P.eq = function ( y, b ) {
	            id = 5;
	            return compare( this, new BigNumber( y, b ) ) === 0;
	        };


	        /*
	         * Return a new BigNumber whose value is the value of this BigNumber rounded to a whole
	         * number in the direction of -Infinity.
	         */
	        P.floor = function () {
	            return round( new BigNumber(this), this.e + 1, 3 );
	        };


	        /*
	         * Return true if the value of this BigNumber is greater than the value of BigNumber(y, b),
	         * otherwise returns false.
	         */
	        P.greaterThan = P.gt = function ( y, b ) {
	            id = 6;
	            return compare( this, new BigNumber( y, b ) ) > 0;
	        };


	        /*
	         * Return true if the value of this BigNumber is greater than or equal to the value of
	         * BigNumber(y, b), otherwise returns false.
	         */
	        P.greaterThanOrEqualTo = P.gte = function ( y, b ) {
	            id = 7;
	            return ( b = compare( this, new BigNumber( y, b ) ) ) === 1 || b === 0;

	        };


	        /*
	         * Return true if the value of this BigNumber is a finite number, otherwise returns false.
	         */
	        P.isFinite = function () {
	            return !!this.c;
	        };


	        /*
	         * Return true if the value of this BigNumber is an integer, otherwise return false.
	         */
	        P.isInteger = P.isInt = function () {
	            return !!this.c && bitFloor( this.e / LOG_BASE ) > this.c.length - 2;
	        };


	        /*
	         * Return true if the value of this BigNumber is NaN, otherwise returns false.
	         */
	        P.isNaN = function () {
	            return !this.s;
	        };


	        /*
	         * Return true if the value of this BigNumber is negative, otherwise returns false.
	         */
	        P.isNegative = P.isNeg = function () {
	            return this.s < 0;
	        };


	        /*
	         * Return true if the value of this BigNumber is 0 or -0, otherwise returns false.
	         */
	        P.isZero = function () {
	            return !!this.c && this.c[0] == 0;
	        };


	        /*
	         * Return true if the value of this BigNumber is less than the value of BigNumber(y, b),
	         * otherwise returns false.
	         */
	        P.lessThan = P.lt = function ( y, b ) {
	            id = 8;
	            return compare( this, new BigNumber( y, b ) ) < 0;
	        };


	        /*
	         * Return true if the value of this BigNumber is less than or equal to the value of
	         * BigNumber(y, b), otherwise returns false.
	         */
	        P.lessThanOrEqualTo = P.lte = function ( y, b ) {
	            id = 9;
	            return ( b = compare( this, new BigNumber( y, b ) ) ) === -1 || b === 0;
	        };


	        /*
	         *  n - 0 = n
	         *  n - N = N
	         *  n - I = -I
	         *  0 - n = -n
	         *  0 - 0 = 0
	         *  0 - N = N
	         *  0 - I = -I
	         *  N - n = N
	         *  N - 0 = N
	         *  N - N = N
	         *  N - I = N
	         *  I - n = I
	         *  I - 0 = I
	         *  I - N = N
	         *  I - I = N
	         *
	         * Return a new BigNumber whose value is the value of this BigNumber minus the value of
	         * BigNumber(y, b).
	         */
	        P.minus = P.sub = function ( y, b ) {
	            var i, j, t, xLTy,
	                x = this,
	                a = x.s;

	            id = 10;
	            y = new BigNumber( y, b );
	            b = y.s;

	            // Either NaN?
	            if ( !a || !b ) return new BigNumber(NaN);

	            // Signs differ?
	            if ( a != b ) {
	                y.s = -b;
	                return x.plus(y);
	            }

	            var xe = x.e / LOG_BASE,
	                ye = y.e / LOG_BASE,
	                xc = x.c,
	                yc = y.c;

	            if ( !xe || !ye ) {

	                // Either Infinity?
	                if ( !xc || !yc ) return xc ? ( y.s = -b, y ) : new BigNumber( yc ? x : NaN );

	                // Either zero?
	                if ( !xc[0] || !yc[0] ) {

	                    // Return y if y is non-zero, x if x is non-zero, or zero if both are zero.
	                    return yc[0] ? ( y.s = -b, y ) : new BigNumber( xc[0] ? x :

	                      // IEEE 754 (2008) 6.3: n - n = -0 when rounding to -Infinity
	                      ROUNDING_MODE == 3 ? -0 : 0 );
	                }
	            }

	            xe = bitFloor(xe);
	            ye = bitFloor(ye);
	            xc = xc.slice();

	            // Determine which is the bigger number.
	            if ( a = xe - ye ) {

	                if ( xLTy = a < 0 ) {
	                    a = -a;
	                    t = xc;
	                } else {
	                    ye = xe;
	                    t = yc;
	                }

	                t.reverse();

	                // Prepend zeros to equalise exponents.
	                for ( b = a; b--; t.push(0) );
	                t.reverse();
	            } else {

	                // Exponents equal. Check digit by digit.
	                j = ( xLTy = ( a = xc.length ) < ( b = yc.length ) ) ? a : b;

	                for ( a = b = 0; b < j; b++ ) {

	                    if ( xc[b] != yc[b] ) {
	                        xLTy = xc[b] < yc[b];
	                        break;
	                    }
	                }
	            }

	            // x < y? Point xc to the array of the bigger number.
	            if (xLTy) t = xc, xc = yc, yc = t, y.s = -y.s;

	            b = ( j = yc.length ) - ( i = xc.length );

	            // Append zeros to xc if shorter.
	            // No need to add zeros to yc if shorter as subtract only needs to start at yc.length.
	            if ( b > 0 ) for ( ; b--; xc[i++] = 0 );
	            b = BASE - 1;

	            // Subtract yc from xc.
	            for ( ; j > a; ) {

	                if ( xc[--j] < yc[j] ) {
	                    for ( i = j; i && !xc[--i]; xc[i] = b );
	                    --xc[i];
	                    xc[j] += BASE;
	                }

	                xc[j] -= yc[j];
	            }

	            // Remove leading zeros and adjust exponent accordingly.
	            for ( ; xc[0] == 0; xc.splice(0, 1), --ye );

	            // Zero?
	            if ( !xc[0] ) {

	                // Following IEEE 754 (2008) 6.3,
	                // n - n = +0  but  n - n = -0  when rounding towards -Infinity.
	                y.s = ROUNDING_MODE == 3 ? -1 : 1;
	                y.c = [ y.e = 0 ];
	                return y;
	            }

	            // No need to check for Infinity as +x - +y != Infinity && -x - -y != Infinity
	            // for finite x and y.
	            return normalise( y, xc, ye );
	        };


	        /*
	         *   n % 0 =  N
	         *   n % N =  N
	         *   n % I =  n
	         *   0 % n =  0
	         *  -0 % n = -0
	         *   0 % 0 =  N
	         *   0 % N =  N
	         *   0 % I =  0
	         *   N % n =  N
	         *   N % 0 =  N
	         *   N % N =  N
	         *   N % I =  N
	         *   I % n =  N
	         *   I % 0 =  N
	         *   I % N =  N
	         *   I % I =  N
	         *
	         * Return a new BigNumber whose value is the value of this BigNumber modulo the value of
	         * BigNumber(y, b). The result depends on the value of MODULO_MODE.
	         */
	        P.modulo = P.mod = function ( y, b ) {
	            var q, s,
	                x = this;

	            id = 11;
	            y = new BigNumber( y, b );

	            // Return NaN if x is Infinity or NaN, or y is NaN or zero.
	            if ( !x.c || !y.s || y.c && !y.c[0] ) {
	                return new BigNumber(NaN);

	            // Return x if y is Infinity or x is zero.
	            } else if ( !y.c || x.c && !x.c[0] ) {
	                return new BigNumber(x);
	            }

	            if ( MODULO_MODE == 9 ) {

	                // Euclidian division: q = sign(y) * floor(x / abs(y))
	                // r = x - qy    where  0 <= r < abs(y)
	                s = y.s;
	                y.s = 1;
	                q = div( x, y, 0, 3 );
	                y.s = s;
	                q.s *= s;
	            } else {
	                q = div( x, y, 0, MODULO_MODE );
	            }

	            return x.minus( q.times(y) );
	        };


	        /*
	         * Return a new BigNumber whose value is the value of this BigNumber negated,
	         * i.e. multiplied by -1.
	         */
	        P.negated = P.neg = function () {
	            var x = new BigNumber(this);
	            x.s = -x.s || null;
	            return x;
	        };


	        /*
	         *  n + 0 = n
	         *  n + N = N
	         *  n + I = I
	         *  0 + n = n
	         *  0 + 0 = 0
	         *  0 + N = N
	         *  0 + I = I
	         *  N + n = N
	         *  N + 0 = N
	         *  N + N = N
	         *  N + I = N
	         *  I + n = I
	         *  I + 0 = I
	         *  I + N = N
	         *  I + I = I
	         *
	         * Return a new BigNumber whose value is the value of this BigNumber plus the value of
	         * BigNumber(y, b).
	         */
	        P.plus = P.add = function ( y, b ) {
	            var t,
	                x = this,
	                a = x.s;

	            id = 12;
	            y = new BigNumber( y, b );
	            b = y.s;

	            // Either NaN?
	            if ( !a || !b ) return new BigNumber(NaN);

	            // Signs differ?
	             if ( a != b ) {
	                y.s = -b;
	                return x.minus(y);
	            }

	            var xe = x.e / LOG_BASE,
	                ye = y.e / LOG_BASE,
	                xc = x.c,
	                yc = y.c;

	            if ( !xe || !ye ) {

	                // Return ±Infinity if either ±Infinity.
	                if ( !xc || !yc ) return new BigNumber( a / 0 );

	                // Either zero?
	                // Return y if y is non-zero, x if x is non-zero, or zero if both are zero.
	                if ( !xc[0] || !yc[0] ) return yc[0] ? y : new BigNumber( xc[0] ? x : a * 0 );
	            }

	            xe = bitFloor(xe);
	            ye = bitFloor(ye);
	            xc = xc.slice();

	            // Prepend zeros to equalise exponents. Faster to use reverse then do unshifts.
	            if ( a = xe - ye ) {
	                if ( a > 0 ) {
	                    ye = xe;
	                    t = yc;
	                } else {
	                    a = -a;
	                    t = xc;
	                }

	                t.reverse();
	                for ( ; a--; t.push(0) );
	                t.reverse();
	            }

	            a = xc.length;
	            b = yc.length;

	            // Point xc to the longer array, and b to the shorter length.
	            if ( a - b < 0 ) t = yc, yc = xc, xc = t, b = a;

	            // Only start adding at yc.length - 1 as the further digits of xc can be ignored.
	            for ( a = 0; b; ) {
	                a = ( xc[--b] = xc[b] + yc[b] + a ) / BASE | 0;
	                xc[b] = BASE === xc[b] ? 0 : xc[b] % BASE;
	            }

	            if (a) {
	                xc = [a].concat(xc);
	                ++ye;
	            }

	            // No need to check for zero, as +x + +y != 0 && -x + -y != 0
	            // ye = MAX_EXP + 1 possible
	            return normalise( y, xc, ye );
	        };


	        /*
	         * Return the number of significant digits of the value of this BigNumber.
	         *
	         * [z] {boolean|number} Whether to count integer-part trailing zeros: true, false, 1 or 0.
	         */
	        P.precision = P.sd = function (z) {
	            var n, v,
	                x = this,
	                c = x.c;

	            // 'precision() argument not a boolean or binary digit: {z}'
	            if ( z != null && z !== !!z && z !== 1 && z !== 0 ) {
	                if (ERRORS) raise( 13, 'argument' + notBool, z );
	                if ( z != !!z ) z = null;
	            }

	            if ( !c ) return null;
	            v = c.length - 1;
	            n = v * LOG_BASE + 1;

	            if ( v = c[v] ) {

	                // Subtract the number of trailing zeros of the last element.
	                for ( ; v % 10 == 0; v /= 10, n-- );

	                // Add the number of digits of the first element.
	                for ( v = c[0]; v >= 10; v /= 10, n++ );
	            }

	            if ( z && x.e + 1 > n ) n = x.e + 1;

	            return n;
	        };


	        /*
	         * Return a new BigNumber whose value is the value of this BigNumber rounded to a maximum of
	         * dp decimal places using rounding mode rm, or to 0 and ROUNDING_MODE respectively if
	         * omitted.
	         *
	         * [dp] {number} Decimal places. Integer, 0 to MAX inclusive.
	         * [rm] {number} Rounding mode. Integer, 0 to 8 inclusive.
	         *
	         * 'round() decimal places out of range: {dp}'
	         * 'round() decimal places not an integer: {dp}'
	         * 'round() rounding mode not an integer: {rm}'
	         * 'round() rounding mode out of range: {rm}'
	         */
	        P.round = function ( dp, rm ) {
	            var n = new BigNumber(this);

	            if ( dp == null || isValidInt( dp, 0, MAX, 15 ) ) {
	                round( n, ~~dp + this.e + 1, rm == null ||
	                  !isValidInt( rm, 0, 8, 15, roundingMode ) ? ROUNDING_MODE : rm | 0 );
	            }

	            return n;
	        };


	        /*
	         * Return a new BigNumber whose value is the value of this BigNumber shifted by k places
	         * (powers of 10). Shift to the right if n > 0, and to the left if n < 0.
	         *
	         * k {number} Integer, -MAX_SAFE_INTEGER to MAX_SAFE_INTEGER inclusive.
	         *
	         * If k is out of range and ERRORS is false, the result will be ±0 if k < 0, or ±Infinity
	         * otherwise.
	         *
	         * 'shift() argument not an integer: {k}'
	         * 'shift() argument out of range: {k}'
	         */
	        P.shift = function (k) {
	            var n = this;
	            return isValidInt( k, -MAX_SAFE_INTEGER, MAX_SAFE_INTEGER, 16, 'argument' )

	              // k < 1e+21, or truncate(k) will produce exponential notation.
	              ? n.times( '1e' + truncate(k) )
	              : new BigNumber( n.c && n.c[0] && ( k < -MAX_SAFE_INTEGER || k > MAX_SAFE_INTEGER )
	                ? n.s * ( k < 0 ? 0 : 1 / 0 )
	                : n );
	        };


	        /*
	         *  sqrt(-n) =  N
	         *  sqrt( N) =  N
	         *  sqrt(-I) =  N
	         *  sqrt( I) =  I
	         *  sqrt( 0) =  0
	         *  sqrt(-0) = -0
	         *
	         * Return a new BigNumber whose value is the square root of the value of this BigNumber,
	         * rounded according to DECIMAL_PLACES and ROUNDING_MODE.
	         */
	        P.squareRoot = P.sqrt = function () {
	            var m, n, r, rep, t,
	                x = this,
	                c = x.c,
	                s = x.s,
	                e = x.e,
	                dp = DECIMAL_PLACES + 4,
	                half = new BigNumber('0.5');

	            // Negative/NaN/Infinity/zero?
	            if ( s !== 1 || !c || !c[0] ) {
	                return new BigNumber( !s || s < 0 && ( !c || c[0] ) ? NaN : c ? x : 1 / 0 );
	            }

	            // Initial estimate.
	            s = Math.sqrt( +x );

	            // Math.sqrt underflow/overflow?
	            // Pass x to Math.sqrt as integer, then adjust the exponent of the result.
	            if ( s == 0 || s == 1 / 0 ) {
	                n = coeffToString(c);
	                if ( ( n.length + e ) % 2 == 0 ) n += '0';
	                s = Math.sqrt(n);
	                e = bitFloor( ( e + 1 ) / 2 ) - ( e < 0 || e % 2 );

	                if ( s == 1 / 0 ) {
	                    n = '1e' + e;
	                } else {
	                    n = s.toExponential();
	                    n = n.slice( 0, n.indexOf('e') + 1 ) + e;
	                }

	                r = new BigNumber(n);
	            } else {
	                r = new BigNumber( s + '' );
	            }

	            // Check for zero.
	            // r could be zero if MIN_EXP is changed after the this value was created.
	            // This would cause a division by zero (x/t) and hence Infinity below, which would cause
	            // coeffToString to throw.
	            if ( r.c[0] ) {
	                e = r.e;
	                s = e + dp;
	                if ( s < 3 ) s = 0;

	                // Newton-Raphson iteration.
	                for ( ; ; ) {
	                    t = r;
	                    r = half.times( t.plus( div( x, t, dp, 1 ) ) );

	                    if ( coeffToString( t.c   ).slice( 0, s ) === ( n =
	                         coeffToString( r.c ) ).slice( 0, s ) ) {

	                        // The exponent of r may here be one less than the final result exponent,
	                        // e.g 0.0009999 (e-4) --> 0.001 (e-3), so adjust s so the rounding digits
	                        // are indexed correctly.
	                        if ( r.e < e ) --s;
	                        n = n.slice( s - 3, s + 1 );

	                        // The 4th rounding digit may be in error by -1 so if the 4 rounding digits
	                        // are 9999 or 4999 (i.e. approaching a rounding boundary) continue the
	                        // iteration.
	                        if ( n == '9999' || !rep && n == '4999' ) {

	                            // On the first iteration only, check to see if rounding up gives the
	                            // exact result as the nines may infinitely repeat.
	                            if ( !rep ) {
	                                round( t, t.e + DECIMAL_PLACES + 2, 0 );

	                                if ( t.times(t).eq(x) ) {
	                                    r = t;
	                                    break;
	                                }
	                            }

	                            dp += 4;
	                            s += 4;
	                            rep = 1;
	                        } else {

	                            // If rounding digits are null, 0{0,4} or 50{0,3}, check for exact
	                            // result. If not, then there are further digits and m will be truthy.
	                            if ( !+n || !+n.slice(1) && n.charAt(0) == '5' ) {

	                                // Truncate to the first rounding digit.
	                                round( r, r.e + DECIMAL_PLACES + 2, 1 );
	                                m = !r.times(r).eq(x);
	                            }

	                            break;
	                        }
	                    }
	                }
	            }

	            return round( r, r.e + DECIMAL_PLACES + 1, ROUNDING_MODE, m );
	        };


	        /*
	         *  n * 0 = 0
	         *  n * N = N
	         *  n * I = I
	         *  0 * n = 0
	         *  0 * 0 = 0
	         *  0 * N = N
	         *  0 * I = N
	         *  N * n = N
	         *  N * 0 = N
	         *  N * N = N
	         *  N * I = N
	         *  I * n = I
	         *  I * 0 = N
	         *  I * N = N
	         *  I * I = I
	         *
	         * Return a new BigNumber whose value is the value of this BigNumber times the value of
	         * BigNumber(y, b).
	         */
	        P.times = P.mul = function ( y, b ) {
	            var c, e, i, j, k, m, xcL, xlo, xhi, ycL, ylo, yhi, zc,
	                base, sqrtBase,
	                x = this,
	                xc = x.c,
	                yc = ( id = 17, y = new BigNumber( y, b ) ).c;

	            // Either NaN, ±Infinity or ±0?
	            if ( !xc || !yc || !xc[0] || !yc[0] ) {

	                // Return NaN if either is NaN, or one is 0 and the other is Infinity.
	                if ( !x.s || !y.s || xc && !xc[0] && !yc || yc && !yc[0] && !xc ) {
	                    y.c = y.e = y.s = null;
	                } else {
	                    y.s *= x.s;

	                    // Return ±Infinity if either is ±Infinity.
	                    if ( !xc || !yc ) {
	                        y.c = y.e = null;

	                    // Return ±0 if either is ±0.
	                    } else {
	                        y.c = [0];
	                        y.e = 0;
	                    }
	                }

	                return y;
	            }

	            e = bitFloor( x.e / LOG_BASE ) + bitFloor( y.e / LOG_BASE );
	            y.s *= x.s;
	            xcL = xc.length;
	            ycL = yc.length;

	            // Ensure xc points to longer array and xcL to its length.
	            if ( xcL < ycL ) zc = xc, xc = yc, yc = zc, i = xcL, xcL = ycL, ycL = i;

	            // Initialise the result array with zeros.
	            for ( i = xcL + ycL, zc = []; i--; zc.push(0) );

	            base = BASE;
	            sqrtBase = SQRT_BASE;

	            for ( i = ycL; --i >= 0; ) {
	                c = 0;
	                ylo = yc[i] % sqrtBase;
	                yhi = yc[i] / sqrtBase | 0;

	                for ( k = xcL, j = i + k; j > i; ) {
	                    xlo = xc[--k] % sqrtBase;
	                    xhi = xc[k] / sqrtBase | 0;
	                    m = yhi * xlo + xhi * ylo;
	                    xlo = ylo * xlo + ( ( m % sqrtBase ) * sqrtBase ) + zc[j] + c;
	                    c = ( xlo / base | 0 ) + ( m / sqrtBase | 0 ) + yhi * xhi;
	                    zc[j--] = xlo % base;
	                }

	                zc[j] = c;
	            }

	            if (c) {
	                ++e;
	            } else {
	                zc.splice(0, 1);
	            }

	            return normalise( y, zc, e );
	        };


	        /*
	         * Return a new BigNumber whose value is the value of this BigNumber rounded to a maximum of
	         * sd significant digits using rounding mode rm, or ROUNDING_MODE if rm is omitted.
	         *
	         * [sd] {number} Significant digits. Integer, 1 to MAX inclusive.
	         * [rm] {number} Rounding mode. Integer, 0 to 8 inclusive.
	         *
	         * 'toDigits() precision out of range: {sd}'
	         * 'toDigits() precision not an integer: {sd}'
	         * 'toDigits() rounding mode not an integer: {rm}'
	         * 'toDigits() rounding mode out of range: {rm}'
	         */
	        P.toDigits = function ( sd, rm ) {
	            var n = new BigNumber(this);
	            sd = sd == null || !isValidInt( sd, 1, MAX, 18, 'precision' ) ? null : sd | 0;
	            rm = rm == null || !isValidInt( rm, 0, 8, 18, roundingMode ) ? ROUNDING_MODE : rm | 0;
	            return sd ? round( n, sd, rm ) : n;
	        };


	        /*
	         * Return a string representing the value of this BigNumber in exponential notation and
	         * rounded using ROUNDING_MODE to dp fixed decimal places.
	         *
	         * [dp] {number} Decimal places. Integer, 0 to MAX inclusive.
	         * [rm] {number} Rounding mode. Integer, 0 to 8 inclusive.
	         *
	         * 'toExponential() decimal places not an integer: {dp}'
	         * 'toExponential() decimal places out of range: {dp}'
	         * 'toExponential() rounding mode not an integer: {rm}'
	         * 'toExponential() rounding mode out of range: {rm}'
	         */
	        P.toExponential = function ( dp, rm ) {
	            return format( this,
	              dp != null && isValidInt( dp, 0, MAX, 19 ) ? ~~dp + 1 : null, rm, 19 );
	        };


	        /*
	         * Return a string representing the value of this BigNumber in fixed-point notation rounding
	         * to dp fixed decimal places using rounding mode rm, or ROUNDING_MODE if rm is omitted.
	         *
	         * Note: as with JavaScript's number type, (-0).toFixed(0) is '0',
	         * but e.g. (-0.00001).toFixed(0) is '-0'.
	         *
	         * [dp] {number} Decimal places. Integer, 0 to MAX inclusive.
	         * [rm] {number} Rounding mode. Integer, 0 to 8 inclusive.
	         *
	         * 'toFixed() decimal places not an integer: {dp}'
	         * 'toFixed() decimal places out of range: {dp}'
	         * 'toFixed() rounding mode not an integer: {rm}'
	         * 'toFixed() rounding mode out of range: {rm}'
	         */
	        P.toFixed = function ( dp, rm ) {
	            return format( this, dp != null && isValidInt( dp, 0, MAX, 20 )
	              ? ~~dp + this.e + 1 : null, rm, 20 );
	        };


	        /*
	         * Return a string representing the value of this BigNumber in fixed-point notation rounded
	         * using rm or ROUNDING_MODE to dp decimal places, and formatted according to the properties
	         * of the FORMAT object (see BigNumber.config).
	         *
	         * FORMAT = {
	         *      decimalSeparator : '.',
	         *      groupSeparator : ',',
	         *      groupSize : 3,
	         *      secondaryGroupSize : 0,
	         *      fractionGroupSeparator : '\xA0',    // non-breaking space
	         *      fractionGroupSize : 0
	         * };
	         *
	         * [dp] {number} Decimal places. Integer, 0 to MAX inclusive.
	         * [rm] {number} Rounding mode. Integer, 0 to 8 inclusive.
	         *
	         * 'toFormat() decimal places not an integer: {dp}'
	         * 'toFormat() decimal places out of range: {dp}'
	         * 'toFormat() rounding mode not an integer: {rm}'
	         * 'toFormat() rounding mode out of range: {rm}'
	         */
	        P.toFormat = function ( dp, rm ) {
	            var str = format( this, dp != null && isValidInt( dp, 0, MAX, 21 )
	              ? ~~dp + this.e + 1 : null, rm, 21 );

	            if ( this.c ) {
	                var i,
	                    arr = str.split('.'),
	                    g1 = +FORMAT.groupSize,
	                    g2 = +FORMAT.secondaryGroupSize,
	                    groupSeparator = FORMAT.groupSeparator,
	                    intPart = arr[0],
	                    fractionPart = arr[1],
	                    isNeg = this.s < 0,
	                    intDigits = isNeg ? intPart.slice(1) : intPart,
	                    len = intDigits.length;

	                if (g2) i = g1, g1 = g2, g2 = i, len -= i;

	                if ( g1 > 0 && len > 0 ) {
	                    i = len % g1 || g1;
	                    intPart = intDigits.substr( 0, i );

	                    for ( ; i < len; i += g1 ) {
	                        intPart += groupSeparator + intDigits.substr( i, g1 );
	                    }

	                    if ( g2 > 0 ) intPart += groupSeparator + intDigits.slice(i);
	                    if (isNeg) intPart = '-' + intPart;
	                }

	                str = fractionPart
	                  ? intPart + FORMAT.decimalSeparator + ( ( g2 = +FORMAT.fractionGroupSize )
	                    ? fractionPart.replace( new RegExp( '\\d{' + g2 + '}\\B', 'g' ),
	                      '$&' + FORMAT.fractionGroupSeparator )
	                    : fractionPart )
	                  : intPart;
	            }

	            return str;
	        };


	        /*
	         * Return a string array representing the value of this BigNumber as a simple fraction with
	         * an integer numerator and an integer denominator. The denominator will be a positive
	         * non-zero value less than or equal to the specified maximum denominator. If a maximum
	         * denominator is not specified, the denominator will be the lowest value necessary to
	         * represent the number exactly.
	         *
	         * [md] {number|string|BigNumber} Integer >= 1 and < Infinity. The maximum denominator.
	         *
	         * 'toFraction() max denominator not an integer: {md}'
	         * 'toFraction() max denominator out of range: {md}'
	         */
	        P.toFraction = function (md) {
	            var arr, d0, d2, e, exp, n, n0, q, s,
	                k = ERRORS,
	                x = this,
	                xc = x.c,
	                d = new BigNumber(ONE),
	                n1 = d0 = new BigNumber(ONE),
	                d1 = n0 = new BigNumber(ONE);

	            if ( md != null ) {
	                ERRORS = false;
	                n = new BigNumber(md);
	                ERRORS = k;

	                if ( !( k = n.isInt() ) || n.lt(ONE) ) {

	                    if (ERRORS) {
	                        raise( 22,
	                          'max denominator ' + ( k ? 'out of range' : 'not an integer' ), md );
	                    }

	                    // ERRORS is false:
	                    // If md is a finite non-integer >= 1, round it to an integer and use it.
	                    md = !k && n.c && round( n, n.e + 1, 1 ).gte(ONE) ? n : null;
	                }
	            }

	            if ( !xc ) return x.toString();
	            s = coeffToString(xc);

	            // Determine initial denominator.
	            // d is a power of 10 and the minimum max denominator that specifies the value exactly.
	            e = d.e = s.length - x.e - 1;
	            d.c[0] = POWS_TEN[ ( exp = e % LOG_BASE ) < 0 ? LOG_BASE + exp : exp ];
	            md = !md || n.cmp(d) > 0 ? ( e > 0 ? d : n1 ) : n;

	            exp = MAX_EXP;
	            MAX_EXP = 1 / 0;
	            n = new BigNumber(s);

	            // n0 = d1 = 0
	            n0.c[0] = 0;

	            for ( ; ; )  {
	                q = div( n, d, 0, 1 );
	                d2 = d0.plus( q.times(d1) );
	                if ( d2.cmp(md) == 1 ) break;
	                d0 = d1;
	                d1 = d2;
	                n1 = n0.plus( q.times( d2 = n1 ) );
	                n0 = d2;
	                d = n.minus( q.times( d2 = d ) );
	                n = d2;
	            }

	            d2 = div( md.minus(d0), d1, 0, 1 );
	            n0 = n0.plus( d2.times(n1) );
	            d0 = d0.plus( d2.times(d1) );
	            n0.s = n1.s = x.s;
	            e *= 2;

	            // Determine which fraction is closer to x, n0/d0 or n1/d1
	            arr = div( n1, d1, e, ROUNDING_MODE ).minus(x).abs().cmp(
	                  div( n0, d0, e, ROUNDING_MODE ).minus(x).abs() ) < 1
	                    ? [ n1.toString(), d1.toString() ]
	                    : [ n0.toString(), d0.toString() ];

	            MAX_EXP = exp;
	            return arr;
	        };


	        /*
	         * Return the value of this BigNumber converted to a number primitive.
	         */
	        P.toNumber = function () {
	            return +this;
	        };


	        /*
	         * Return a BigNumber whose value is the value of this BigNumber raised to the power n.
	         * If m is present, return the result modulo m.
	         * If n is negative round according to DECIMAL_PLACES and ROUNDING_MODE.
	         * If POW_PRECISION is non-zero and m is not present, round to POW_PRECISION using
	         * ROUNDING_MODE.
	         *
	         * The modular power operation works efficiently when x, n, and m are positive integers,
	         * otherwise it is equivalent to calculating x.toPower(n).modulo(m) (with POW_PRECISION 0).
	         *
	         * n {number} Integer, -MAX_SAFE_INTEGER to MAX_SAFE_INTEGER inclusive.
	         * [m] {number|string|BigNumber} The modulus.
	         *
	         * 'pow() exponent not an integer: {n}'
	         * 'pow() exponent out of range: {n}'
	         *
	         * Performs 54 loop iterations for n of 9007199254740991.
	         */
	        P.toPower = P.pow = function ( n, m ) {
	            var k, y, z,
	                i = mathfloor( n < 0 ? -n : +n ),
	                x = this;

	            if ( m != null ) {
	                id = 23;
	                m = new BigNumber(m);
	            }

	            // Pass ±Infinity to Math.pow if exponent is out of range.
	            if ( !isValidInt( n, -MAX_SAFE_INTEGER, MAX_SAFE_INTEGER, 23, 'exponent' ) &&
	              ( !isFinite(n) || i > MAX_SAFE_INTEGER && ( n /= 0 ) ||
	                parseFloat(n) != n && !( n = NaN ) ) || n == 0 ) {
	                k = Math.pow( +x, n );
	                return new BigNumber( m ? k % m : k );
	            }

	            if (m) {
	                if ( n > 1 && x.gt(ONE) && x.isInt() && m.gt(ONE) && m.isInt() ) {
	                    x = x.mod(m);
	                } else {
	                    z = m;

	                    // Nullify m so only a single mod operation is performed at the end.
	                    m = null;
	                }
	            } else if (POW_PRECISION) {

	                // Truncating each coefficient array to a length of k after each multiplication
	                // equates to truncating significant digits to POW_PRECISION + [28, 41],
	                // i.e. there will be a minimum of 28 guard digits retained.
	                // (Using + 1.5 would give [9, 21] guard digits.)
	                k = mathceil( POW_PRECISION / LOG_BASE + 2 );
	            }

	            y = new BigNumber(ONE);

	            for ( ; ; ) {
	                if ( i % 2 ) {
	                    y = y.times(x);
	                    if ( !y.c ) break;
	                    if (k) {
	                        if ( y.c.length > k ) y.c.length = k;
	                    } else if (m) {
	                        y = y.mod(m);
	                    }
	                }

	                i = mathfloor( i / 2 );
	                if ( !i ) break;
	                x = x.times(x);
	                if (k) {
	                    if ( x.c && x.c.length > k ) x.c.length = k;
	                } else if (m) {
	                    x = x.mod(m);
	                }
	            }

	            if (m) return y;
	            if ( n < 0 ) y = ONE.div(y);

	            return z ? y.mod(z) : k ? round( y, POW_PRECISION, ROUNDING_MODE ) : y;
	        };


	        /*
	         * Return a string representing the value of this BigNumber rounded to sd significant digits
	         * using rounding mode rm or ROUNDING_MODE. If sd is less than the number of digits
	         * necessary to represent the integer part of the value in fixed-point notation, then use
	         * exponential notation.
	         *
	         * [sd] {number} Significant digits. Integer, 1 to MAX inclusive.
	         * [rm] {number} Rounding mode. Integer, 0 to 8 inclusive.
	         *
	         * 'toPrecision() precision not an integer: {sd}'
	         * 'toPrecision() precision out of range: {sd}'
	         * 'toPrecision() rounding mode not an integer: {rm}'
	         * 'toPrecision() rounding mode out of range: {rm}'
	         */
	        P.toPrecision = function ( sd, rm ) {
	            return format( this, sd != null && isValidInt( sd, 1, MAX, 24, 'precision' )
	              ? sd | 0 : null, rm, 24 );
	        };


	        /*
	         * Return a string representing the value of this BigNumber in base b, or base 10 if b is
	         * omitted. If a base is specified, including base 10, round according to DECIMAL_PLACES and
	         * ROUNDING_MODE. If a base is not specified, and this BigNumber has a positive exponent
	         * that is equal to or greater than TO_EXP_POS, or a negative exponent equal to or less than
	         * TO_EXP_NEG, return exponential notation.
	         *
	         * [b] {number} Integer, 2 to 64 inclusive.
	         *
	         * 'toString() base not an integer: {b}'
	         * 'toString() base out of range: {b}'
	         */
	        P.toString = function (b) {
	            var str,
	                n = this,
	                s = n.s,
	                e = n.e;

	            // Infinity or NaN?
	            if ( e === null ) {

	                if (s) {
	                    str = 'Infinity';
	                    if ( s < 0 ) str = '-' + str;
	                } else {
	                    str = 'NaN';
	                }
	            } else {
	                str = coeffToString( n.c );

	                if ( b == null || !isValidInt( b, 2, 64, 25, 'base' ) ) {
	                    str = e <= TO_EXP_NEG || e >= TO_EXP_POS
	                      ? toExponential( str, e )
	                      : toFixedPoint( str, e );
	                } else {
	                    str = convertBase( toFixedPoint( str, e ), b | 0, 10, s );
	                }

	                if ( s < 0 && n.c[0] ) str = '-' + str;
	            }

	            return str;
	        };


	        /*
	         * Return a new BigNumber whose value is the value of this BigNumber truncated to a whole
	         * number.
	         */
	        P.truncated = P.trunc = function () {
	            return round( new BigNumber(this), this.e + 1, 1 );
	        };


	        /*
	         * Return as toString, but do not accept a base argument, and include the minus sign for
	         * negative zero.
	         */
	        P.valueOf = P.toJSON = function () {
	            var str,
	                n = this,
	                e = n.e;

	            if ( e === null ) return n.toString();

	            str = coeffToString( n.c );

	            str = e <= TO_EXP_NEG || e >= TO_EXP_POS
	                ? toExponential( str, e )
	                : toFixedPoint( str, e );

	            return n.s < 0 ? '-' + str : str;
	        };


	        P.isBigNumber = true;

	        if ( config != null ) BigNumber.config(config);

	        return BigNumber;
	    }


	    // PRIVATE HELPER FUNCTIONS


	    function bitFloor(n) {
	        var i = n | 0;
	        return n > 0 || n === i ? i : i - 1;
	    }


	    // Return a coefficient array as a string of base 10 digits.
	    function coeffToString(a) {
	        var s, z,
	            i = 1,
	            j = a.length,
	            r = a[0] + '';

	        for ( ; i < j; ) {
	            s = a[i++] + '';
	            z = LOG_BASE - s.length;
	            for ( ; z--; s = '0' + s );
	            r += s;
	        }

	        // Determine trailing zeros.
	        for ( j = r.length; r.charCodeAt(--j) === 48; );
	        return r.slice( 0, j + 1 || 1 );
	    }


	    // Compare the value of BigNumbers x and y.
	    function compare( x, y ) {
	        var a, b,
	            xc = x.c,
	            yc = y.c,
	            i = x.s,
	            j = y.s,
	            k = x.e,
	            l = y.e;

	        // Either NaN?
	        if ( !i || !j ) return null;

	        a = xc && !xc[0];
	        b = yc && !yc[0];

	        // Either zero?
	        if ( a || b ) return a ? b ? 0 : -j : i;

	        // Signs differ?
	        if ( i != j ) return i;

	        a = i < 0;
	        b = k == l;

	        // Either Infinity?
	        if ( !xc || !yc ) return b ? 0 : !xc ^ a ? 1 : -1;

	        // Compare exponents.
	        if ( !b ) return k > l ^ a ? 1 : -1;

	        j = ( k = xc.length ) < ( l = yc.length ) ? k : l;

	        // Compare digit by digit.
	        for ( i = 0; i < j; i++ ) if ( xc[i] != yc[i] ) return xc[i] > yc[i] ^ a ? 1 : -1;

	        // Compare lengths.
	        return k == l ? 0 : k > l ^ a ? 1 : -1;
	    }


	    /*
	     * Return true if n is a valid number in range, otherwise false.
	     * Use for argument validation when ERRORS is false.
	     * Note: parseInt('1e+1') == 1 but parseFloat('1e+1') == 10.
	     */
	    function intValidatorNoErrors( n, min, max ) {
	        return ( n = truncate(n) ) >= min && n <= max;
	    }


	    function isArray(obj) {
	        return Object.prototype.toString.call(obj) == '[object Array]';
	    }


	    /*
	     * Convert string of baseIn to an array of numbers of baseOut.
	     * Eg. convertBase('255', 10, 16) returns [15, 15].
	     * Eg. convertBase('ff', 16, 10) returns [2, 5, 5].
	     */
	    function toBaseOut( str, baseIn, baseOut ) {
	        var j,
	            arr = [0],
	            arrL,
	            i = 0,
	            len = str.length;

	        for ( ; i < len; ) {
	            for ( arrL = arr.length; arrL--; arr[arrL] *= baseIn );
	            arr[ j = 0 ] += ALPHABET.indexOf( str.charAt( i++ ) );

	            for ( ; j < arr.length; j++ ) {

	                if ( arr[j] > baseOut - 1 ) {
	                    if ( arr[j + 1] == null ) arr[j + 1] = 0;
	                    arr[j + 1] += arr[j] / baseOut | 0;
	                    arr[j] %= baseOut;
	                }
	            }
	        }

	        return arr.reverse();
	    }


	    function toExponential( str, e ) {
	        return ( str.length > 1 ? str.charAt(0) + '.' + str.slice(1) : str ) +
	          ( e < 0 ? 'e' : 'e+' ) + e;
	    }


	    function toFixedPoint( str, e ) {
	        var len, z;

	        // Negative exponent?
	        if ( e < 0 ) {

	            // Prepend zeros.
	            for ( z = '0.'; ++e; z += '0' );
	            str = z + str;

	        // Positive exponent
	        } else {
	            len = str.length;

	            // Append zeros.
	            if ( ++e > len ) {
	                for ( z = '0', e -= len; --e; z += '0' );
	                str += z;
	            } else if ( e < len ) {
	                str = str.slice( 0, e ) + '.' + str.slice(e);
	            }
	        }

	        return str;
	    }


	    function truncate(n) {
	        n = parseFloat(n);
	        return n < 0 ? mathceil(n) : mathfloor(n);
	    }


	    // EXPORT


	    BigNumber = constructorFactory();
	    BigNumber['default'] = BigNumber.BigNumber = BigNumber;


	    // AMD.
	    if ( true ) {
	        !(__WEBPACK_AMD_DEFINE_RESULT__ = function () { return BigNumber; }.call(exports, __webpack_require__, exports, module), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));

	    // Node.js and other environments that support module.exports.
	    } else if ( typeof module != 'undefined' && module.exports ) {
	        module.exports = BigNumber;

	    // Browser.
	    } else {
	        if ( !globalObj ) globalObj = typeof self != 'undefined' ? self : Function('return this')();
	        globalObj.BigNumber = BigNumber;
	    }
	})(this);


/***/ },
/* 70 */
/***/ function(module, exports, __webpack_require__) {

	var BigNumber = null;
	/*
	    json_parse.js
	    2012-06-20

	    Public Domain.

	    NO WARRANTY EXPRESSED OR IMPLIED. USE AT YOUR OWN RISK.

	    This file creates a json_parse function.
	    During create you can (optionally) specify some behavioural switches

	        require('json-bigint')(options)

	            The optional options parameter holds switches that drive certain
	            aspects of the parsing process:
	            * options.strict = true will warn about duplicate-key usage in the json.
	              The default (strict = false) will silently ignore those and overwrite
	              values for keys that are in duplicate use.

	    The resulting function follows this signature:
	        json_parse(text, reviver)
	            This method parses a JSON text to produce an object or array.
	            It can throw a SyntaxError exception.

	            The optional reviver parameter is a function that can filter and
	            transform the results. It receives each of the keys and values,
	            and its return value is used instead of the original value.
	            If it returns what it received, then the structure is not modified.
	            If it returns undefined then the member is deleted.

	            Example:

	            // Parse the text. Values that look like ISO date strings will
	            // be converted to Date objects.

	            myData = json_parse(text, function (key, value) {
	                var a;
	                if (typeof value === 'string') {
	                    a =
	/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}(?:\.\d*)?)Z$/.exec(value);
	                    if (a) {
	                        return new Date(Date.UTC(+a[1], +a[2] - 1, +a[3], +a[4],
	                            +a[5], +a[6]));
	                    }
	                }
	                return value;
	            });

	    This is a reference implementation. You are free to copy, modify, or
	    redistribute.

	    This code should be minified before deployment.
	    See http://javascript.crockford.com/jsmin.html

	    USE YOUR OWN COPY. IT IS EXTREMELY UNWISE TO LOAD CODE FROM SERVERS YOU DO
	    NOT CONTROL.
	*/

	/*members "", "\"", "\/", "\\", at, b, call, charAt, f, fromCharCode,
	    hasOwnProperty, message, n, name, prototype, push, r, t, text
	*/

	var json_parse = function (options) {
	    "use strict";

	// This is a function that can parse a JSON text, producing a JavaScript
	// data structure. It is a simple, recursive descent parser. It does not use
	// eval or regular expressions, so it can be used as a model for implementing
	// a JSON parser in other languages.

	// We are defining the function inside of another function to avoid creating
	// global variables.


	// Default options one can override by passing options to the parse()
	    var _options = {
	        "strict": false,  // not being strict means do not generate syntax errors for "duplicate key"
	        "storeAsString": false // toggles whether the values should be stored as BigNumber (default) or a string
	    };


	// If there are options, then use them to override the default _options
	    if (options !== undefined && options !== null) {
	        if (options.strict === true) {
	            _options.strict = true;
	        }
	        if (options.storeAsString === true) {
	            _options.storeAsString = true;
	        }
	    }


	    var at,     // The index of the current character
	        ch,     // The current character
	        escapee = {
	            '"':  '"',
	            '\\': '\\',
	            '/':  '/',
	            b:    '\b',
	            f:    '\f',
	            n:    '\n',
	            r:    '\r',
	            t:    '\t'
	        },
	        text,

	        error = function (m) {

	// Call error when something is wrong.

	            throw {
	                name:    'SyntaxError',
	                message: m,
	                at:      at,
	                text:    text
	            };
	        },

	        next = function (c) {

	// If a c parameter is provided, verify that it matches the current character.

	            if (c && c !== ch) {
	                error("Expected '" + c + "' instead of '" + ch + "'");
	            }

	// Get the next character. When there are no more characters,
	// return the empty string.

	            ch = text.charAt(at);
	            at += 1;
	            return ch;
	        },

	        number = function () {
	// Parse a number value.

	            var number,
	                string = '';

	            if (ch === '-') {
	                string = '-';
	                next('-');
	            }
	            while (ch >= '0' && ch <= '9') {
	                string += ch;
	                next();
	            }
	            if (ch === '.') {
	                string += '.';
	                while (next() && ch >= '0' && ch <= '9') {
	                    string += ch;
	                }
	            }
	            if (ch === 'e' || ch === 'E') {
	                string += ch;
	                next();
	                if (ch === '-' || ch === '+') {
	                    string += ch;
	                    next();
	                }
	                while (ch >= '0' && ch <= '9') {
	                    string += ch;
	                    next();
	                }
	            }
	            number = +string;
	            if (!isFinite(number)) {
	                error("Bad number");
	            } else {
	                if (BigNumber == null)
	                  BigNumber = __webpack_require__(69);
	                //if (number > 9007199254740992 || number < -9007199254740992)
	                // Bignumber has stricter check: everything with length > 15 digits disallowed
	                if (string.length > 15)
	                   return (_options.storeAsString === true) ? string : new BigNumber(string);
	                return number;
	            }
	        },

	        string = function () {

	// Parse a string value.

	            var hex,
	                i,
	                string = '',
	                uffff;

	// When parsing for string values, we must look for " and \ characters.

	            if (ch === '"') {
	                while (next()) {
	                    if (ch === '"') {
	                        next();
	                        return string;
	                    }
	                    if (ch === '\\') {
	                        next();
	                        if (ch === 'u') {
	                            uffff = 0;
	                            for (i = 0; i < 4; i += 1) {
	                                hex = parseInt(next(), 16);
	                                if (!isFinite(hex)) {
	                                    break;
	                                }
	                                uffff = uffff * 16 + hex;
	                            }
	                            string += String.fromCharCode(uffff);
	                        } else if (typeof escapee[ch] === 'string') {
	                            string += escapee[ch];
	                        } else {
	                            break;
	                        }
	                    } else {
	                        string += ch;
	                    }
	                }
	            }
	            error("Bad string");
	        },

	        white = function () {

	// Skip whitespace.

	            while (ch && ch <= ' ') {
	                next();
	            }
	        },

	        word = function () {

	// true, false, or null.

	            switch (ch) {
	            case 't':
	                next('t');
	                next('r');
	                next('u');
	                next('e');
	                return true;
	            case 'f':
	                next('f');
	                next('a');
	                next('l');
	                next('s');
	                next('e');
	                return false;
	            case 'n':
	                next('n');
	                next('u');
	                next('l');
	                next('l');
	                return null;
	            }
	            error("Unexpected '" + ch + "'");
	        },

	        value,  // Place holder for the value function.

	        array = function () {

	// Parse an array value.

	            var array = [];

	            if (ch === '[') {
	                next('[');
	                white();
	                if (ch === ']') {
	                    next(']');
	                    return array;   // empty array
	                }
	                while (ch) {
	                    array.push(value());
	                    white();
	                    if (ch === ']') {
	                        next(']');
	                        return array;
	                    }
	                    next(',');
	                    white();
	                }
	            }
	            error("Bad array");
	        },

	        object = function () {

	// Parse an object value.

	            var key,
	                object = {};

	            if (ch === '{') {
	                next('{');
	                white();
	                if (ch === '}') {
	                    next('}');
	                    return object;   // empty object
	                }
	                while (ch) {
	                    key = string();
	                    white();
	                    next(':');
	                    if (_options.strict === true && Object.hasOwnProperty.call(object, key)) {
	                        error('Duplicate key "' + key + '"');
	                    }
	                    object[key] = value();
	                    white();
	                    if (ch === '}') {
	                        next('}');
	                        return object;
	                    }
	                    next(',');
	                    white();
	                }
	            }
	            error("Bad object");
	        };

	    value = function () {

	// Parse a JSON value. It could be an object, an array, a string, a number,
	// or a word.

	        white();
	        switch (ch) {
	        case '{':
	            return object();
	        case '[':
	            return array();
	        case '"':
	            return string();
	        case '-':
	            return number();
	        default:
	            return ch >= '0' && ch <= '9' ? number() : word();
	        }
	    };

	// Return the json_parse function. It will have access to all of the above
	// functions and variables.

	    return function (source, reviver) {
	        var result;

	        text = source + '';
	        at = 0;
	        ch = ' ';
	        result = value();
	        white();
	        if (ch) {
	            error("Syntax error");
	        }

	// If there is a reviver function, we recursively walk the new structure,
	// passing each name/value pair to the reviver function for possible
	// transformation, starting with a temporary root object that holds the result
	// in an empty key. If there is not a reviver function, we simply return the
	// result.

	        return typeof reviver === 'function'
	            ? (function walk(holder, key) {
	                var k, v, value = holder[key];
	                if (value && typeof value === 'object') {
	                    Object.keys(value).forEach(function(k) {
	                        v = walk(value, k);
	                        if (v !== undefined) {
	                            value[k] = v;
	                        } else {
	                            delete value[k];
	                        }
	                    });
	                }
	                return reviver.call(holder, key, value);
	            }({'': result}, ''))
	            : result;
	    };
	}

	module.exports = json_parse;


/***/ }
/******/ ]);