mapboxgl.accessToken = 'pk.eyJ1IjoiamVyZW15bGkiLCJhIjoiY2lrZ2U4MWI4MDA4bHVjajc1am1weTM2aSJ9.JHiBmawEKGsn3jiRK_d0Gw';
const map = new mapboxgl.Map({
    container: 'map',
    style: 'mapbox://styles/mapbox/light-v9',
    center: [-96.35, 39.5],
    zoom: 3.7,
    maxZoom: 17,
    minZoom: 0
});
// use timestamp to label every sending request
let currentTimestamp;
let socket;
// zoom level after last time
let zoomLevel = Math.floor(map.getZoom());
// id for the timer used to send heartbeat pack of WebSocket
let timerId = 0;
let query = "";

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

let x;
map.on("moveend", () => {
    statusChange("move");
});
map.on('zoomend', () => {
    statusChange("zoom");
});

function getClusteringAlgorithm() {
    const algorithms = {
        "HGC": 0,
        "I-KMeans": 1,
        "KMeans": 2
    };
    return algorithms[document.getElementById("clusteringAlgorithm").value];
}

function getChoice(elementId) {
    let choices = {
        true: 1,
        false: 0
    };
    return choices[document.getElementById(elementId).checked];
}

function sendEdgesRequest(minLng, minLat, maxLng, maxLat, zoom) {
    sendingRequest(minLng, minLat, maxLng, maxLat, zoom, 2);
}

function sendClusterRequest(minLng, minLat, maxLng, maxLat, zoom) {
    let clusteringControl = getChoice("cluster");
    if (clusteringControl === 0) {
        zoom = 18;
    }
    sendingRequest(minLng, minLat, maxLng, maxLat, zoom, 1);
}

function sendingRequest(minLng, minLat, maxLng, maxLat, zoom, option) {
    if (socket === undefined) {
        return;
    }
    const clusteringAlgorithm = getClusteringAlgorithm();
    let clusteringControl = getChoice("cluster");
    let bundlingControl = getChoice("bundle");
    let cutControl = getChoice("treeCut");
    const sendingObj = {
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
    const sendingJSON = JSON.stringify(sendingObj);
    socket.send(sendingJSON);
}

function receiveClusterEdges(data) {
    const iconLayer = new MapboxLayer({
        id: 'edgeCluster',
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
    removeEdgeClusterLayer();
    map.addLayer(iconLayer);
}

function receiveClusterPoints(data) {
    const iconLayer = new MapboxLayer({
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
    map.addLayer(iconLayer);
}

function updatePointsStats(json) {
    document.getElementById('repliesCnt').innerHTML = "Reply Tweets Count: " + json['repliesCnt'] + " / 15722639";
    document.getElementById('pointsCnt').innerHTML = "Points Count: " + json['pointsCnt'];
    document.getElementById('clustersCnt').innerHTML = "Clusters Count: " + json['clustersCnt'];
}

function updateEdgesStats(json) {
    document.getElementById('repliesCnt').innerHTML = "Reply Tweets Count: " + json['repliesCnt'] + " / 15722639";
    document.getElementById('edgesCnt').innerHTML = "Edges Count: " + json['edgesCnt'];
    document.getElementById('bundledEdgesCnt').innerHTML = "Bundled Edges Count: " + (json['edgesCnt'] - json['isolatedEdgesCnt']);
}

function removeEdgeClusterLayer() {
    if (map.getLayer('edgeCluster') !== undefined) {
        map.removeLayer('edgeCluster');
    }
}

function removeClusterLayer() {
    if (map.getLayer('cluster') !== undefined) {
        map.removeLayer('cluster');
    }
}

function removeLayer() {
    removeEdgeClusterLayer();
    removeClusterLayer();
}

function drawGraph() {
    removeLayer();
    query = document.getElementById("keyword-textbox").value;
    socket = new WebSocket("ws://localhost:9000/replies");

    socket.onopen = function (e) {
        currentTimestamp = Date.now();
        const clusteringAlgorithm = getClusteringAlgorithm();
        const sendingObj = {
            query: query,
            clusteringAlgorithm: clusteringAlgorithm,
            timestamp: currentTimestamp,
            option: 0
        };
        const sendingJSON = JSON.stringify(sendingObj);
        socket.send(sendingJSON);
        keepAlive();
    };

    socket.onmessage = function (event) {
        let json = JSON.parse(event.data);
        let option = json['option'];
        let timestamp = json['timestamp'];
        // deal with package that has wrong timestamp (outdated packages)
        if (timestamp !== currentTimestamp.toString()) {
            console.log('data is dropped');
            return;
        }
        if (option === 0) {
            let stopStatus = json['flag'];
            let date = json['date'];
            let clusteringAlgorithm = getClusteringAlgorithm();
            if (stopStatus !== 'Y') {
                const sendingObj = {
                    query: query,
                    clusteringAlgorithm: clusteringAlgorithm,
                    date: date,
                    timestamp: currentTimestamp,
                    option: 0
                };
                const sendingJSON = JSON.stringify(sendingObj);
                socket.send(sendingJSON);
            }
            statusChange("incremental");
        } else if (option === 1) {
            updatePointsStats(json);
            data = JSON.parse(json['data']);
            receiveClusterPoints(data);
        } else {
            updateEdgesStats(json);
            data = JSON.parse(json['data']);
            receiveClusterEdges(data);
        }
    };
}

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
        if (clusterStatus && !pointStatus) {
            alert("Please check cluster with points.");
            $('#cluster').prop('checked', false);
            clusterStatus = 0;
        }
        if (!clusterStatus && treeCutStatus) {
            alert("Please select tree cut with cluster and edge.");
            $('#treeCut').prop('checked', false);
        }
        if (clusterStatus || pointStatus) pointDraw = 1;
        else removeClusterLayer();
        if (edgeStatus) edgeDraw = 1;
    }
    if (changeEvent === 'edge' || changeEvent === 'bundle' || changeEvent === 'treeCut' || changeEvent === 'incremental') {
        if (bundleStatus && !edgeStatus) {
            alert("Please check bundle with edge.");
            $('#bundle').prop('checked', false);
            bundleStatus = 0;
        }
        if (!edgeStatus && treeCutStatus) {
            alert("Please select tree cut with cluster and edge.");
            $('#treeCut').prop('checked', false);
            treeCutStatus = 0;
        }
        if (bundleStatus || edgeStatus || treeCutStatus) edgeDraw = 1;
        else removeEdgeClusterLayer();
    }
    if (changeEvent === 'move' || (changeEvent === 'zoom' && zoomLevel !== status['currentZoom'])) {
        if (pointStatus) pointDraw = 1;
        if (edgeStatus) edgeDraw = 1;
        zoomLevel = status['currentZoom'];
    }
    if (pointDraw) sendClusterRequest(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
    if (edgeDraw) sendEdgesRequest(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
}

// send heartbeat package to keep the connection alive
function keepAlive() {
    const timeout = 20000;
    if (socket.readyState === WebSocket.OPEN) {
        socket.send('');
    }
    timerId = setTimeout(keepAlive, timeout);
}