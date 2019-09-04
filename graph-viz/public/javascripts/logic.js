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
let current_timestamp;
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

map.on("moveend", () => {
    let status = getScreenStatus();
    if (document.getElementById("point").checked === true) {
        renderClusterPoints(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
    } else {
        removeClusterLayer();
    }
    if (document.getElementById("edge").checked === true) {
        renderClusterEdges(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
    } else {
        removeEdgeClusterLayer();
    }
});

map.on('zoomend', () => {
    let status = getScreenStatus();
    if (zoomLevel !== status['currentZoom']) {
        if (document.getElementById("point").checked === true) {
            renderClusterPoints(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
        } else {
            removeClusterLayer();
        }
        if (document.getElementById("edge").checked === true) {
            renderClusterEdges(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
        } else {
            removeEdgeClusterLayer();
        }
        zoomLevel = status['currentZoom'];
    }
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

function renderClusterEdges(minLng, minLat, maxLng, maxLat, zoom) {
    if (socket === undefined) {
        return;
    }
    let clusteringAlgorithm = getClusteringAlgorithm();
    let clustering_control = getChoice("drawPoints");
    let bundling_control = getChoice("bundle");
    let cut_control = getChoice("tree_cut");
    if (clustering_control === 0 && cut_control === 1) {
        alert("Please select tree cut with drawPoints.");
        $('#tree_cut').prop('checked', false);
    }
    const sendingObj = {
        query: query,
        lowerLongitude: minLng,
        upperLongitude: maxLng,
        lowerLatitude: minLat,
        upperLatitude: maxLat,
        clusteringAlgorithm: clusteringAlgorithm,
        bundling: bundling_control,
        treeCut: cut_control,
        clustering: clustering_control,
        zoom: zoom,
        timestamp: current_timestamp,
        option: 2
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

function renderClusterPoints(minLng, minLat, maxLng, maxLat, zoom) {
    if (socket === undefined) {
        return;
    }
    const clusteringAlgorithm = getClusteringAlgorithm();
    let clustering_control = getChoice("drawPoints");
    if (clustering_control === 0) {
        zoom = 18;
    }
    const sendingObj = {
        query: query,
        lowerLongitude: minLng,
        upperLongitude: maxLng,
        lowerLatitude: minLat,
        upperLatitude: maxLat,
        clusteringAlgorithm: clusteringAlgorithm,
        clustering: clustering_control,
        zoom: zoom,
        timestamp: current_timestamp,
        bundling: 0,
        treeCut: 0,
        option: 1
    };
    const sendingJSON = JSON.stringify(sendingObj);
    socket.send(sendingJSON);
}

function receiveClusterPoints(data) {
    const iconLayer = new MapboxLayer({
        id: 'drawPoints',
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


function removeEdgeClusterLayer() {
    if (map.getLayer('edgeCluster') !== undefined) {
        map.removeLayer('edgeCluster');
    }
}

function removeClusterLayer() {
    if (map.getLayer('drawPoints') !== undefined) {
        map.removeLayer('drawPoints');
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
        current_timestamp = Date.now();
        const clusteringAlgorithm = getClusteringAlgorithm();
        const sendingObj = {
            query: query, clusteringAlgorithm: clusteringAlgorithm, timestamp: current_timestamp, option: 0
        };
        const sendingJSON = JSON.stringify(sendingObj);
        socket.send(sendingJSON);
        keepAlive();
    };

    socket.onmessage = function (event) {
        let status = getScreenStatus();
        let json = JSON.parse(event.data);
        let option = json['option'];
        let timestamp = json['timestamp'];
        if (timestamp !== current_timestamp.toString()) {
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
                    timestamp: current_timestamp,
                    option: 0
                };
                const sendingJSON = JSON.stringify(sendingObj);
                socket.send(sendingJSON);
            }
            renderClusterPoints(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
            if (document.getElementById("edge").checked === true) {
                renderClusterEdges(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
            }
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

    socket.onclose = function (event) {
        cancelKeepAlive();
    }
}

function edge_change() {
    let status = getScreenStatus();
    if (document.getElementById("edge").checked === true) {
        renderClusterEdges(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
    } else {
        if (document.getElementById("bundle").checked === true) {
            $('#bundle').prop('checked', false);
        }
        removeEdgeClusterLayer();
    }
}

function bundle_change() {
    let status = getScreenStatus();
    if (document.getElementById("edge").checked === false) {
        $('#bundle').prop('checked', false);
        alert("Please check with edge.");
        return;
    }
    if (document.getElementById("bundle").checked === true) {
        renderClusterEdges(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
    } else {
        if (document.getElementById("edge").checked === false) {
            removeEdgeClusterLayer();
        } else {
            renderClusterEdges(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
        }
    }
}

function point_change() {
    let status = getScreenStatus();
    if (document.getElementById("point").checked === true) {
        renderClusterPoints(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
    } else {
        removeClusterLayer();
        $('#drawPoints').prop('checked', false);
    }
}

function cluster_change() {
    let status = getScreenStatus();
    if (document.getElementById("drawPoints").checked === true) {
        document.getElementById("point").checked = true;
    } else {
        removeClusterLayer();
    }
    if (document.getElementById("point").checked === true) {
        renderClusterPoints(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
    }
    if (document.getElementById("edge").checked === true) {
        renderClusterEdges(status['minLng'], status['minLat'], status['maxLng'], status['maxLat'], status['currentZoom']);
    }
}

// send heartbeat package to keep the connection alive
function keepAlive() {
    // console.log("send heartbeat pack.");
    const timeout = 20000;
    if (socket.readyState === WebSocket.OPEN) {
        socket.send('');
    }
    timerId = setTimeout(keepAlive, timeout);
}

function cancelKeepAlive() {
    if (timerId) {
        clearTimeout(timerId);
    }
}