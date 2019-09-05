/* specify mapbox token */
mapboxgl.accessToken = 'pk.eyJ1IjoiamVyZW15bGkiLCJhIjoiY2lrZ2U4MWI4MDA4bHVjajc1am1weTM2aSJ9.JHiBmawEKGsn3jiRK_d0Gw';

/* mapbox object with default configuration */
const map = new mapboxgl.Map({
    container: 'map',
    style: 'mapbox://styles/mapbox/light-v9',
    center: [-96.35, 39.5],
    zoom: 3.7,
    maxZoom: 17,
    minZoom: 0
});

/* use timestamp to label every sending request */
let currentTimestamp;
/* WebSocket object */
let socket;
/* zoom level after last time */
let zoomLevel = Math.floor(map.getZoom());
/* id for the timer used to send heartbeat pack of WebSocket */
let timerId = 0;
/* keyword of query */
let query = "";

/**
 * get the screen status and store the result in a json
 */
function getScreenStatus() {
    let bounds = map.getBounds();
    let ne = bounds.getNorthEast();
    let sw = bounds.getSouthWest();
    return {
        "currentZoom": Math.floor(map.getZoom()),
        "maxLng": ne.lng,
        "maxLat": ne.lat,
        "minLng": sw.lng,
        "minLat": sw.lat
    };
}

/**
 binding function to move end action
 */
map.on("moveend", () => {
    statusChange("move");
});

/**
 * binding function to zoom end action
 */
map.on('zoomend', () => {
    statusChange("zoom");
});

/**
 * get the clustering algorithm
 */
function getClusteringAlgorithm() {
    const algorithms = {
        "HGC": 0,
        "I-KMeans": 1,
        "KMeans": 2
    };
    return algorithms[document.getElementById("clusteringAlgorithm").value];
}

/**
 * get the choice for certain checkbox with id = elementId
 * @param elementId id of the target html element
 */
function getChoice(elementId) {
    let choices = {
        true: 1,
        false: 0
    };
    return choices[document.getElementById(elementId).checked];
}

/**
 * send the edge (bundle, tree cut) request to the middle layer
 */
function sendEdgesRequest() {
    // with the date and zoom level unspecified
    sendingRequest(undefined, undefined, 2);
}

/**
 * send the cluster (point) request to the middle layer
 */
function sendClusterRequest() {
    let clusteringControl = getChoice("cluster");
    let zoom = undefined;
    // if cluster is not selected, directly use the lowest zoom level
    if (clusteringControl === 0) {
        zoom = 18;
    }
    /// with the date unspecified
    sendingRequest(zoom, undefined, 1);
}

/**
 * construct and send the request's json string
 */
function sendingRequest(zoom, date, option) {
    let status = getScreenStatus();
    let minLng = status['minLng'];
    let minLat = status['minLat'];
    let maxLng = status['maxLng'];
    let maxLat = status['maxLat'];
    if (zoom === undefined) zoom = status['currentZoom'];
    if (socket === undefined) return;
    const clusteringAlgorithm = getClusteringAlgorithm();
    let clusteringControl = getChoice("cluster");
    let bundlingControl = getChoice("bundle");
    let cutControl = getChoice("treeCut");
    let sendingObj = {
        query: query,
        lowerLongitude: minLng,
        upperLongitude: maxLng,
        lowerLatitude: minLat,
        upperLatitude: maxLat,
        clusteringAlgorithm: clusteringAlgorithm,
        bundling: bundlingControl,
        treeCut: cutControl,
        clustering: clusteringControl,
        zoom: zoom,
        timestamp: currentTimestamp,
        option: option
    };
    if (date !== undefined) {
        sendingObj['date'] = date;
    }
    const sendingJSON = JSON.stringify(sendingObj);
    socket.send(sendingJSON);
}

/**
 * Draw the layer after receiving edge data
 * @param data received edge data
 */
function receiveEdges(data) {
    const edgeLayer = new MapboxLayer({
        id: 'edge',
        type: LineLayer,
        opacity: 0.1,
        data: data,
        getSourcePosition: d => d.from,
        getTargetPosition: d => d.to,
        getWidth: d => {
            let temp = Math.min(15, Math.ceil(Math.pow(d.width, 1 / 2)));
            return Math.max(temp, 3);
        },
        getColor: [57, 73, 171]
    });
    removeEdgeLayer();
    map.addLayer(edgeLayer);
}

/**
 * Draw the layer after receiving cluster data
 * @param data received cluster data
 */
function receiveClusterPoints(data) {
    const clusterLayer = new MapboxLayer({
        id: 'cluster',
        type: ScatterplotLayer,
        data: data,
        pickable: true,
        opacity: 0.8,
        stroked: false,
        filled: true,
        radiusScale: 100,
        radiusMinPixels: 5,
        radiusMaxPixels: 25,
        getPosition: d => d.coordinates,
        getRadius: d => d.size,
        getFillColor: d => [57, 73, 171],
    });
    removeClusterLayer();
    map.addLayer(clusterLayer);
}

/**
 * Updating the statistics data of points
 * @param pointsJson the json containing points data
 */
function updatePointsStats(pointsJson) {
    document.getElementById('repliesCnt').innerHTML = "Reply Tweets Count: " + pointsJson['repliesCnt'] + " / 15722639";
    document.getElementById('pointsCnt').innerHTML = "Points Count: " + pointsJson['pointsCnt'];
    document.getElementById('clustersCnt').innerHTML = "Clusters Count: " + pointsJson['clustersCnt'];
}

/**
 * Updating the statistics data of edges
 * @param edgesJson the json containing edges data
 */
function updateEdgesStats(edgesJson) {
    document.getElementById('repliesCnt').innerHTML = "Reply Tweets Count: " + edgesJson['repliesCnt'] + " / 15722639";
    document.getElementById('edgesCnt').innerHTML = "Edges Count: " + edgesJson['edgesCnt'];
    document.getElementById('bundledEdgesCnt').innerHTML = "Bundled Edges Count: " + (edgesJson['edgesCnt'] - edgesJson['isolatedEdgesCnt']);
}

/**
 * remove edge layer from the map
 */
function removeEdgeLayer() {
    if (map.getLayer('edge') !== undefined) {
        map.removeLayer('edge');
    }
}

/**
 * remove cluster layer from the map
 */
function removeClusterLayer() {
    if (map.getLayer('cluster') !== undefined) {
        map.removeLayer('cluster');
    }
}

/**
 * remove both layers
 */
function removeLayer() {
    removeEdgeLayer();
    removeClusterLayer();
}

/**
 * draw graph function associated with the show button in the main page
 */
function drawGraph() {
    removeLayer();
    query = document.getElementById("keyword-textbox").value;
    socket = new WebSocket("ws://localhost:9000/replies");

    /**
     * socket on open function, sending the first batch request of the incremental query
     */
    socket.onopen = function () {
        currentTimestamp = Date.now();
        // with the date and zoom level unspecified
        sendingRequest(undefined, undefined, 0);
        keepAlive();
    };

    /**
     * function for socket open event, sending the first batch request of the incremental query
     * @param event the event associated with the message receiving action, which carries the data
     */
    socket.onmessage = function (event) {
        let json = JSON.parse(event.data);
        let option = json['option'];
        let timestamp = json['timestamp'];
        // drop packages that has wrong timestamp (outdated packages)
        if (timestamp !== currentTimestamp.toString()) {
            console.log('data is dropped');
            return;
        }
        // incremental response result
        if (option === 0) {
            // not finished, continue sending request
            if (json['flag'] !== 'Y') {
                // with the zoom level unspecified
                sendingRequest(undefined, json['date'], 0);
            }
            statusChange("incremental");
        }
        else {
            let data = JSON.parse(json['data']);
            // cluster response result
            if (option === 1) {
                updatePointsStats(json);
                receiveClusterPoints(data);
            }
            // edge response result
            else {
                updateEdgesStats(json);
                receiveEdges(data);
            }
        }

    };
}

/**
 * handler function for all events associated with the checkbox / map
 * @param changeEvent
 */
function statusChange(changeEvent) {
    let status = getScreenStatus();
    let pointStatus = getChoice("point");
    let clusterStatus = getChoice("cluster");
    let edgeStatus = getChoice("edge");
    let bundleStatus = getChoice("bundle");
    let treeCutStatus = getChoice("treeCut");
    let pointDraw = 0;
    let edgeDraw = 0;
    if (changeEvent === 'point' || changeEvent === 'cluster' || changeEvent === 'treeCut' || changeEvent === 'incremental') {
        // select cluster checkbox without select point, uncheck the cluster and send alert
        if (clusterStatus && !pointStatus) {
            alert("Please check cluster with points.");
            $('#cluster').prop('checked', false);
            clusterStatus = 0;
        }
        // select treecut checkbox without select cluster, uncheck the treecut and send alert
        if (!clusterStatus && treeCutStatus) {
            alert("Please select tree cut with cluster and edge.");
            $('#treeCut').prop('checked', false);
        }
        // if cluster or point checkbox is selected, call the function to send cluster request
        // otherwise remove the cluster layer
        if (clusterStatus || pointStatus) pointDraw = 1;
        else removeClusterLayer();
        // when conversion happens between cluster and point, edge need to be redrawed, call the function to send edge request
        if (edgeStatus) edgeDraw = 1;
    }
    if (changeEvent === 'edge' || changeEvent === 'bundle' || changeEvent === 'treeCut' || changeEvent === 'incremental') {
        // select bundle checkbox without select edge, uncheck the bundle and send alert
        if (bundleStatus && !edgeStatus) {
            alert("Please check bundle with edge.");
            $('#bundle').prop('checked', false);
            bundleStatus = 0;
        }
        // select tree cut checkbox without select edge, uncheck the tree cut and send alert
        if (!edgeStatus && treeCutStatus) {
            alert("Please select tree cut with cluster and edge.");
            $('#treeCut').prop('checked', false);
            treeCutStatus = 0;
        }
        // if bundle or edge or tree cut checkbox is selected, call the function to send edge request
        // otherwise remove the edge layer
        if (bundleStatus || edgeStatus || treeCutStatus) edgeDraw = 1;
        else removeEdgeLayer();
    }
    // for map status change event, call the function to send edge request, meanwhile record the current zoom level
    // the recorded zoom level will be used for the detection of the zoom level change
    if (changeEvent === 'move' || (changeEvent === 'zoom' && zoomLevel !== status['currentZoom'])) {
        if (pointStatus) pointDraw = 1;
        if (edgeStatus) edgeDraw = 1;
        zoomLevel = status['currentZoom'];
    }
    if (pointDraw) sendClusterRequest(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
    if (edgeDraw) sendEdgesRequest(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
}

/**
 * send heartbeat package to keep the connection alive
 */
function keepAlive() {
    const timeout = 20000;
    if (socket.readyState === WebSocket.OPEN) {
        socket.send('');
    }
    timerId = setTimeout(keepAlive, timeout);
}