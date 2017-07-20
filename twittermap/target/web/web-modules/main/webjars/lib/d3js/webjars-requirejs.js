requirejs.config({
    paths: { "d3js": webjars.path("d3js", "d3") },
    shim: { "d3js": { "exports": "d3" } }
});
