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

/**
* Asterix SDK - Beta Version
* @author Eugenia Gabrielov <genia.likes.science@gmail.com>
* 
* This is a Javascript helper file for generating AQL queries for AsterixDB (https://code.google.com/p/asterixdb/) 
*/


/**
* AsterixDBConnection
* 
* This is a handler for connections to a local AsterixDB REST API Endpoint. 
* This initialization takes as input a configuraiton object, and initializes
* same basic functionality. 
*/
function AsterixDBConnection(configuration) {
    // Initialize AsterixDBConnection properties
    this._properties = {};
    
    // Set dataverse as null for now, this needs to be set by the user.
    this._properties["dataverse"] = "";
    
    // By default, we will wait for calls to the REST API to complete. The query method
    // sends a different setting when executed asynchronously. Calls that do not specify a mode
    // will be executed synchronously.
    this._properties["mode"] = "synchronous";
    
    // These are the default error behaviors for Asterix and ajax errors, respectively. 
    // They can be overridden by calling initializing your AsterixDBConnection like so:
    // adb = new AsterixDBConnection({
    //                                  "error" : function(data) {
    //                                              // override here...
    //                                  });
    // and similarly for ajax_error, just pass in the configuration as a json object.
    this._properties["error"] = function(data) {
        alert("Asterix REST API Error:\n" + data["error-code"][0] + "\n" + data["error-code"][1]);
    };
    
    this._properties["ajax_error"] = function(message) {
        alert("[Ajax Error]\n" + message);
    };

    // This is the default path to the local Asterix REST API. Can be overwritten for remote configurations
    // or for demo setup purposes (such as with a proxy handler with Python or PHP.
    this._properties["endpoint_root"] = "http://localhost:19002/";
    
    // If we have passed in a configuration, we will update the internal properties
    // using that configuration. You can do things such as include a new endpoint_root,
    // a new error function, a new dataverse, etc. You can even store extra info.
    //
    // NOTE Long-term, this should have more strict limits.
    var configuration = configuration || {};

    for (var key in configuration) {
        this._properties[key] = configuration[key];
    }
    
    return this;
}


/**
* dataverse
*
* Sets dataverse for execution for the AsterixDBConnection.
*/
AsterixDBConnection.prototype.dataverse = function(dataverseName) {
    this._properties["dataverse"] = dataverseName;
    
    return this;
};


/**
* query (http://asterix.ics.uci.edu/documentation/api.html#QueryApi)
* 
* @param statements, statements of an AQL query
* @param successFn, a function to execute if this query is run successfully
* @param mode, a string either "synchronous" or "asynchronous", depending on preferred
*               execution mode. 
*/
AsterixDBConnection.prototype.query = function(statements, successFn, mode) {
 
    if ( typeof statements === 'string') {
        statements = [ statements ];
    }
    
    var m = typeof mode ? mode : "synchronous";
    
    // DEBUG
    //alert(statements.join("\n"));
     
    var query = "use dataverse " + this._properties["dataverse"] + ";\n" + statements.join("\n");
    
    this._api(
        {
            "query" : query,
            "mode"  : m
        },
        successFn, 
        "query"
    );

    return this;
};

/**
* query_status (http://asterix.ics.uci.edu/documentation/api.html#QueryStatusApi)
* 
* @param handle, a json object of the form {"handle" : handleObject}, where
*                   the handle object is an opaque handle previously returned
*                   from an asynchronous call.
* @param successFn, a function to call on successful execution of this API call.
*/
AsterixDBConnection.prototype.query_status = function(handle, successFn) {
    this._api(
        handle,
        successFn,
        "query/status"
    );

    return this;
};


/**
* query_result (http://asterix.ics.uci.edu/documentation/api.html#AsynchronousResultApi)
* 
* handle, a json object of the form {"handle" : handleObject}, where
*           the handle object is an opaque handle previously returned
*           from an asynchronous call.
* successFn, a function to call on successful execution of this API call.
*/
AsterixDBConnection.prototype.query_result = function(handle, successFn) {
    this._api(
        handle,
        successFn,
        "query/result"
    ); 

    return this;
};


/**
* ddl (http://asterix.ics.uci.edu/documentation/api.html#DdlApi)
* 
* @param statements, statements to run through ddl api
* @param successFn, a function to execute if they are successful
*/
AsterixDBConnection.prototype.ddl = function(statements, successFn) {
    if ( typeof statements === 'string') {
        statements = [ statements ];
    }
    
    this._api(
        {
            "ddl" :  "use dataverse " + this._properties["dataverse"] + ";\n" + statements.join("\n")
        },
        successFn,
        "ddl"
    );
}


/**
* update (http://asterix.ics.uci.edu/documentation/api.html#UpdateApi)
*
* @param statements, statement(s) for an update API call
* @param successFn, a function to run if this is executed successfully.
* 
* This is an AsterixDBConnection handler for the update API. It passes statements provided
* to the internal API endpoint handler.
*/
AsterixDBConnection.prototype.update = function(statements, successFn) {
    if ( typeof statements === 'string') {
        statements = [ statements ];
    }
    
    // DEBUG
    // alert(statements.join("\n"));
    
    this._api(
        {
            "statements" : "use dataverse " + this._properties["dataverse"] + ";\n" + statements.join("\n")
        },
        successFn,
        "update"
    );
}


/**
* meta
* @param statements, a string or a list of strings representing an Asterix query object
* @param successFn, a function to execute if call succeeds
*
* Queries without a dataverse. This is a work-around for an Asterix REST API behavior
* that sometiems throws an error. This is handy for Asterix Metadata queries.
*/
AsterixDBConnection.prototype.meta = function(statements, successFn) {

    if ( typeof statements === 'string') {
        statements = [ statements ];
    }
    
    var query = statements.join("\n");
    
    this._api(
        {
            "query" : query,
            "mode"  : "synchronous"
        },
        successFn, 
        "query"
    );

    return this;
}


/**
* _api
*
* @param json, the data to be passed with the request
* @param onSuccess, the success function to be run if this succeeds
* @param endpoint, a string representing one of the Asterix API endpoints 
* 
* Documentation of endpoints is here:
* http://asterix.ics.uci.edu/documentation/api.html
*
* This is treated as an internal method for making the actual call to the API.
*/
AsterixDBConnection.prototype._api = function(json, onSuccess, endpoint) {

    // The success function is called if the response is successful and returns data,
    // or is just OK.
    var success_fn = onSuccess;
    
    // This is the error function. Called if something breaks either on the Asterix side
    // or in the Ajax call.
    var error_fn = this._properties["error"];
    var ajax_error_fn = this._properties["ajax_error"];
    
    // This is the target endpoint from the REST api, called as a string.
    var endpoint_url = this._properties["endpoint_root"] + endpoint;    

    // This SDK does not rely on jQuery, but utilizes its Ajax capabilities when present.
    if (window.jQuery) {
        $.ajax({
        
            // The Asterix API does not accept post requests.
            type        : 'GET',
            
            // This is the endpoint url provided by combining the default
            // or reconfigured endpoint root along with the appropriate api endpoint
            // such as "query" or "update".
            url         : endpoint_url,
            
            // This is the data in the format specified on the API documentation.
            data        : json,
            
            // We send out the json datatype to make sure our data is parsed correctly. 
            dataType    : "json",
            
            // The success option calls a function on success, which in this case means
            // something was returned from the API. However, this does not mean the call succeeded
            // on the REST API side, it just means we got something back. This also contains the
            // error return codes, which need to be handled before we call th success function.
            success     : function(data) {

                // Check Asterix Response for errors
                // See http://asterix.ics.uci.edu/documentation/api.html#ErrorCodes
                if (data["error-code"]) { 
                    error_fn(data);
                    
                // Otherwise, run our provided success function
                } else {
                    success_fn(data);
                }
            },
            
            // This is the function that gets called if there is an ajax-related (non-Asterix)
            // error. Network errors, empty response bodies, syntax errors, and a number of others
            // can pop up. 
            error       : function(data) {

                // Some of the Asterix API endpoints return empty responses on success.
                // However, the ajax function treats these as errors while reporting a
                // 200 OK code with no payload. So we will check for that, otherwise 
                // alert of an error. An example response is as follows:
                // {"readyState":4,"responseText":"","status":200,"statusText":"OK"}
                if (data["status"] == 200 && data["responseText"] == "") {
                    success_fn(data);
                } else {
                    alert("[Ajax Error]\n" + JSON.stringify(data));
                }
            }
        });
        
    } else {
    
        // NOTE: This section is in progress; currently API requires jQuery.
    
        // First, we encode the parameters of the query to create a new url.
        api_endpoint = endpoint_url + "?" + Object.keys(json).map(function(k) {
            return encodeURIComponent(k) + '=' + encodeURIComponent(json[k])
        }).join('&');
       
        // Now, create an XMLHttp object to carry our request. We will call the
        // UI callback function on ready.
        var xmlhttp;
        xmlhttp = new XMLHttpRequest();
        xmlhttp.open("GET", endpoint_url, true);
        xmlhttp.send(null);
        
        xmlhttp.onreadystatechange = function(){
            if (xmlhttp.readyState == 4) {
                if (xmlhttp.status === 200) {
                    alert(xmlhttp.responseText);
                    //success.call(null, xmlHttp.responseText);
                } else {
                    //error.call(null, xmlHttp.responseText);
                }
            } else {
                // Still processing
            }
        };
    }
    return this;
};

