/*global process, require */

(function () {

    "use strict";

    // Ensure we have a promise implementation.
    //
    if (typeof Promise === 'undefined') {
        var Promise = require("es6-promise").Promise;
        global.Promise = Promise;
    }

    var args = process.argv,
        os = require("os"),
        fs = require("fs"),
        less = require("less"),
        mkdirp = require("mkdirp"),
        path = require("path");

    var SOURCE_FILE_MAPPINGS_ARG = 2;
    var TARGET_ARG = 3;
    var OPTIONS_ARG = 4;

    var sourceFileMappings = JSON.parse(args[SOURCE_FILE_MAPPINGS_ARG]);
    var target = args[TARGET_ARG];
    var optionsString = args[OPTIONS_ARG];

    var sourcesToProcess = sourceFileMappings.length;
    var results = [];
    var problems = [];

    function parseDone() {
        if (--sourcesToProcess === 0) {
            console.log("\u0010" + JSON.stringify({results: results, problems: problems}));
        }
    }

    function throwIfErr(e) {
        if (e) throw e;
    }

    sourceFileMappings.forEach(function (sourceFileMapping) {

        // Reparse options each time so we get a different object that can be modified
        var options = JSON.parse(optionsString);

        var input = sourceFileMapping[0];
        var outputFile = sourceFileMapping[1].replace(".less", options.compress ? ".min.css" : ".css");
        var output = path.join(target, outputFile);
        var sourceMapOutput = output + ".map";

        options.sourceMap = {
            sourceMapBasepath: path.dirname(input),
            sourceMapFullFilename: path.basename(sourceMapOutput),
            sourceMapOutputFilename: path.basename(outputFile)
        };
        options.filename = input;
        options.plugins = [];

        var writeSourceMap = function (content, onDone) {
            if (content) { // NOTE: this is workaround for https://github.com/less/less.js/issues/2430
                if (options.relativeImports) {
                    // replace leading part in included assets with "../"
                    content = JSON.parse(content);
                    for (var i = 0, s = content.sources, l = s.length; i < l; i++) {
                        options.paths.forEach(function(path) {
                            // for windows replace \ with /
                            path = path.replace(/\\/g, "/");
                            if (path[path.length - 1] !== "/")
                                path += "/";
                            if (s[i].substr(0, path.length) === path)
                                s[i] = "../" + s[i].substr(path.length);
                        });
                    }
                    content = JSON.stringify(content);
                }
            }

            mkdirp(path.dirname(sourceMapOutput), function (e) {
                throwIfErr(e);
                fs.writeFile(sourceMapOutput, content, "utf8", onDone);
            });
        };


        var writeOutput = function (content, onDone) {
            mkdirp(path.dirname(output), function (e) {
                throwIfErr(e);
                fs.writeFile(output, content, "utf8", onDone);
            });
        };

        var handleResult = function (result) {
            writeOutput(result.css, function (e) {
                throwIfErr(e);

                writeSourceMap(result.map, function (e) {
                    throwIfErr(e);

                    var imports = [];
                    var files = result.imports;
                    for (var file in files) {
                        if (files.hasOwnProperty(file)) {
                            imports.push(file);
                        }
                    }

                    results.push({
                        source: input,
                        result: {
                            filesRead: [input].concat(imports),
                            filesWritten: options.sourceMap ? [output, sourceMapOutput] : [output]
                        }
                    });

                    parseDone();
                });
            });
        };

        fs.readFile(input, "utf8", function (e, content) {
            throwIfErr(e);


            function handleLessError(e) {
                if (e.line != undefined && e.column != undefined) {
                    problems.push({
                        message: e.message,
                        severity: "error",
                        lineNumber: e.line,
                        characterOffset: e.column,
                        lineContent: content.split("\n")[e.line - 1],
                        source: input
                    });
                } else {
                    throw e;
                }
                results.push({
                    source: input,
                    result: null
                });

                parseDone();
            }

            less.render(content, options)
                .then(handleResult, handleLessError);
        });
    });
})();