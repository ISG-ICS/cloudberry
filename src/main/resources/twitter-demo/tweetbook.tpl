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
    <link rel="stylesheet" type="text/css" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css">
    <link rel="stylesheet" href="static/css/bootstrap.vertical-tabs.css">
    <script src="static/js/asterix-sdk-stable.js"></script>
    <script src="http://code.jquery.com/jquery.min.js"></script>
    <script src="static/js/bootstrap.min.js"></script>
    <script src="http://cdn.leafletjs.com/leaflet/v0.7.7/leaflet.js"></script>
    <script src="//d3js.org/d3.v3.min.js" charset="utf-8"></script>
    <script src="static/js/crossfilter.min.js"></script>
    <script src="static/js/dc.min.js"></script>
    <script src="static/js/tweetbook.js"></script>
    <script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/jquery-ui.min.js"></script>
    <style>
        body {
            margin: 0;
            padding: 0;
        }

        #map {
            position: absolute;
            top: 0;
            bottom: 0;
            width: 100%;
            /*z-index: -1;*/
        }

        #sidebar {
            position: relative;
            top: 0;
            bottom: 0;
            left: 96%;
            width: 24%;
            height: 700px;
            overflow: auto;
            margin-left: 0%;
            -webkit-transition: all 0.5s ease;
            -moz-transition: all 0.5s ease;
            -o-transition: all 0.5s ease;
            transition: all 0.5s ease;
            background-color: white;
            opacity: 0.8;
        }

        #sidebar.toggled {
            margin-left: -20%;
        }               

        #time-series {
            position: absolute;
            top: 82%;
            left: 2.5%;
            width: 100%;
            height: 15%;
        }

        #input {
            position: absolute;
            left: 27%;
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
        .fa-2x {
            font-size: 2em;
        }
        .fa {
            position: relative;
            display: table-cell;
            width: 60px;
            height: 36px;
            text-align: center;
            vertical-align: middle;
            font-size:20px;
        }
        .col-xs-2{
            padding-left: 0;
            padding-right: 0;
        }
        .col-xs-10{
            padding-left: 0;
            padding-right: 0;
        }
    </style>
</head>
<body>
<div id='map'></div>
<div id='input'>
    <form class="form-inline" id="input-form">
        <div class="form-group" id="input-group">
            <label class="sr-only">Key words</label>
            <input type="text" class="form-control " id="keyword-textbox" placeholder="Type keywords here, e.g. 'trump'">
        </div>
        <button type="button" class="btn btn-primary" id="submit-button" style="margin-bottom: 7px">Submit</button>
    </form>
</div>
<div id='time-series'></div>
<div id='sidebar'>
    <div class="col-xs-2">
        <ul class="nav nav-tabs tabs-left">
            <li role="presentation"><a href="#hashtag" data-toggle="tab"><i class="fa fa-hashtag fa-2x" ></i></a></li>
            <li role="presentation"><a href="#tweet" data-toggle="tab"><i class="fa fa-twitter fa-2x" ></i></a></li>
            <li role="presentation"><a href="#aql" data-toggle="tab"><i class="fa fa-code fa-2x"></i></a></li>
            <li role="presentation" class="active"><a href="#about" data-toggle="tab"><i class="fa fa-info-circle fa-2x"></i></a></li>
        </ul>
    </div>
    <div class="col-xs-10">
        <div class="tab-content">
            <div id="hashtag" class="tab-pane">
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
            <div id="about" class="tab-pane active">
                <h1> About </h1>
                <p><b>Cloudberry</b> is a research prototype to support interactive analytics and visualization of large amounts of spatial-temporal data. </p>
                <p> Basic Information: </p>
                <ul>
                    <li>Data set: Tweets</li>
                    <li>Number of records: 18,953,055</li>
                    <li>Collection period: From 2016-03-31 to 2016-04-07</li>
                    <li>Total data size: 17G bytes</li>
                    <li><a href="https://github.com/ISG-ICS/cloudberry">Source code</a></li>
                </ul>
                <p>The backend is running the big data management system <b>Apache AsterixDB</b> to support large compute clusters. For questions and comments, please contact <b>cloudberry@ics.uci.edu</b></p>
            </div>
        </div>
    </div>
</div>
</div>
</body>
</html>