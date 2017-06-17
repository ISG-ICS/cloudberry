var path = require("path"),
    webpack = require("webpack"),
    loaders = require("./node_modules/paraviewweb/config/webpack.loaders.js"),
    plugins = [];
if(process.env.NODE_ENV === "production") {
    console.log("==> Production build");
    plugins.push(new webpack.DefinePlugin({
        "process.env": {
            NODE_ENV: JSON.stringify("production"),
        },
    }));
}
module.exports = {
    plugins,
    entry: "./src/CloudberryDemo.js",
    output: {
        path: "./dist",
        filename: "CloudberryDemo.js",
    },
    module: {
        loaders: [
            { test: require.resolve("./src/CloudberryDemo.js"), loader: "expose?CloudberryDemo" },
        ].concat(loaders),
    },
    resolve: {
        alias: {
            PVWStyle: path.resolve("./node_modules/paraviewweb/style"),
        },
    },
    postcss: [
        require("autoprefixer")({ browsers: ["last 2 versions"] }),
    ],
};
