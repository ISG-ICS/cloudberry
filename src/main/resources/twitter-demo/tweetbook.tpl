<!DOCTYPE html>
<html>
<head>
    <meta charset='utf-8'/>
    <title>ASTERIX Demo</title>
    <meta name='viewport' content='initial-scale=1,maximum-scale=1,user-scalable=no'/>
    <link href='https://api.tiles.mapbox.com/mapbox-gl-js/v0.14.0/mapbox-gl.css' rel='stylesheet'/>
    <link href="//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap.min.css" rel="stylesheet" type="text/css">
    <link href="static/css/dc.css" rel="stylesheet" type="text/css">
    <link rel="stylesheet" href="http://cdn.leafletjs.com/leaflet/v0.7.7/leaflet.css"/>
    <link href='https://api.mapbox.com/mapbox.js/v2.3.0/mapbox.css' rel='stylesheet'/>
    <script src="static/js/asterix-sdk-stable.js"></script>
    <script src="http://code.jquery.com/jquery.min.js"></script>
    <script src="static/js/bootstrap.min.js"></script>
    <script src="http://cdn.leafletjs.com/leaflet/v0.7.7/leaflet.js"></script>
    <script src="//d3js.org/d3.v3.min.js" charset="utf-8"></script>
    <script src="static/js/crossfilter.min.js"></script>
    <script src="static/js/dc.min.js"></script>
    <script src="static/js/tweetbook.js"></script>
    <script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/jquery-ui.min.js"></script>
    <script src="//platform.twitter.com/widgets.js" charset="utf-8"></script>
    <style>
        body {
            margin: 0;
            padding: 0;
        }

        #map {
            position: absolute;
            top: 0;
            bottom: 0;
            width: 80%;
            /*z-index: -1;*/
        }

        #sidebar {
            position: relative;
            top: 0;
            bottom: 0;
            left: 80%;
            width: 20%;
            height: 700px;
            overflow: auto;
        }

        #time-series {
            position: absolute;
            top: 82%;
            left: 2.5%;
            width: 80%;
            height: 15%;
        }

        #input {
            position: absolute;
            left: 20%;
            top: 2%;
            width: 50%;
        }

        #input-group {
            position: relative;
            width: 80%;
        }

        .info {
            padding: 6px 8px;
            font: 14px/16px Arial, Helvetica, sans-serif;
            background: white;
            background: rgba(255, 255, 255, 0.8);
            box-shadow: 0 0 15px rgba(0, 0, 0, 0.2);
            border-radius: 5px;
        }

        .info h4 {
            margin: 0 0 5px;
            color: #777;
        }

        .legend {
            line-height: 18px;
            color: #555;
        }

        .legend i {
            width: 18px;
            height: 18px;
            float: left;
            margin-right: 8px;
            opacity: 0.7;
        }

        blockquote{
            padding: 5px 10px;
            margin: 0 0 10px;
            font-size: 12px;
        }

        blockquote p{
            font-size: 14px;
        }
    </style>
</head>
<body>
<div id='map'></div>
<div id='input'>
    <form class="form-inline" id="input-form">
        <div class="form-group" id="input-group">
            <label class="sr-only">Key words</label>
            <input type="text" class="form-control " id="keyword-textbox" placeholder="Key words">
        </div>
        <button type="button" class="btn btn-primary" id="submit-button">Submit</button>
    </form>
</div>
<div id='time-series'></div>
<div id='sidebar'>
    <ul class="nav nav-pills nav-justified ">
        <li role="presentation" class="active"><a href="#hashtag" data-toggle="pill">Hashtag</a></li>
        <li role="presentation"><a href="#tweet" data-toggle="pill">Tweets</a></li>
        <li role="presentation"><a href="#aql" data-toggle="pill">AQL</a></li>
    </ul>
    <div class="tab-content">
        <div id="hashtag" class="tab-pane active">
            <table class="table" id="hashcount">
                <thead>
                <tr>
                    <th>Hashtag</th>
                </tr>
                </thead>
                <tbody>
                </tbody>
            </table>
        </div>
        <div id="tweet" class="tab-pane">
        </div>
        <div id="aql" class="tab-pane">
            <table class="table" id="tweets">
                <thead>
                <tr>
                    <th>AQL</th>
                </tr>
                </thead>
                <tbody>
                </tbody>
            </table>
        </div>
    </div>
</div>
</div>
</body>
</html>