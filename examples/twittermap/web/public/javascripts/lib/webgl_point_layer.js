var WebGLPointLayer = L.CanvasLayer.extend({

    initialize: function(options) {
        // Call initialize() from the parent class
        L.CanvasLayer.prototype.initialize.call(this, options);

        this._initGL();
        this._initTextures();
        this._cid = 0;
        // Twitter Blue
        this._pointColorX = 29 / 255.0;
        this._pointColorY = 161 / 255.0;
        this._pointColorZ = 242 / 255.0;
    },


    onAdd: function(map) {
        // Call onAdd() from the parent class
        L.CanvasLayer.prototype.onAdd.call(this, map);

        map.on("mousemove", this._mousemove, this);
    },


    setData: function(data) {
        if ( this._checkData(data) ) {
            this._data = data;
            this._initBuffers();
        }
    },


    // ??? Never called
    appendData: function(data) {
        if ( this._checkData(data) ) {
            this._data = this._data.concat(data);
            this._initBuffers();
        }
    },


    getCurrentPointID: function() {
        if ( this._cid == 0 )
            return "";
        else
            return this._data[this._cid - 1][2];
    },


    setCurrentPoint: function(x, y) {
        var gl = this._gl;
        if ( gl == null ) return;

        var canvas = this.getCanvas();
        var pixel_value = new Uint8Array(4);

        gl.bindFramebuffer(gl.FRAMEBUFFER, this._fb);
        gl.readPixels(x, canvas.height - 1 - y, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixel_value);
        // console.log(pixel_value);
        gl.bindFramebuffer(gl.FRAMEBUFFER, null);

        var id = 0;
        for ( var i = 0; i < 3; ++i ) id = id*256 + pixel_value[i];

        if ( id > 0 && id != this._cid ) {
            this._cid = id;
            this.render();
        }
    },


    setPointSize: function(pointSize) {
        this._pointSize = pointSize;
    },

    setPointColor: function(x, y, z) {
        this._pointColorX = x / 255.0;
        this._pointColorY = y / 255.0;
        this._pointColorZ = z / 255.0;
    },

    _initGL: function() {
        var canvas = this.getCanvas();
        try {
          this._gl = canvas.getContext("experimental-webgl", {
            antialias: true,
            preserveDrawingBuffer: true,
            premultipliedAlpha: false
          });
          if (!this._gl) canvas.getContext("webgl", {
            antialias: true,
            preserveDrawingBuffer: true,
            premultipliedAlpha: false
          });
        } catch (e) {}
        if (!this._gl) {
            alert("Could not initialise WebGL, sorry :-(");
            return;
        }

        var gl = this._gl;
        var pixel_value = new Uint8Array(4);

        // Shader setup
        this._programs = new Array(2);

        var vertexShader = gl.createShader(gl.VERTEX_SHADER);
        gl.shaderSource(vertexShader, vertCode);
        gl.compileShader(vertexShader);
        if (!gl.getShaderParameter(vertexShader, gl.COMPILE_STATUS)) {
            alert(gl.getShaderInfoLog(vertexShader));
        }

        var fragmentShader = gl.createShader(gl.FRAGMENT_SHADER);
        gl.shaderSource(fragmentShader, fragCode);
        gl.compileShader(fragmentShader);
        if (!gl.getShaderParameter(fragmentShader, gl.COMPILE_STATUS)) {
            alert(gl.getShaderInfoLog(fragmentShader));
        }

        this._programs[0] = gl.createProgram();

        gl.attachShader(this._programs[0], vertexShader);
        gl.attachShader(this._programs[0], fragmentShader);
        gl.linkProgram(this._programs[0]);
        if (!gl.getProgramParameter(this._programs[0], gl.LINK_STATUS)) {
            alert("Could not initialise shaders");
        }

        gl.useProgram(this._programs[0]);

        this._programs[0].matLoc = gl.getUniformLocation(this._programs[0], "u_matrix");
        this._programs[0].pointSize = gl.getUniformLocation(this._programs[0], "u_pointSize");
        this._programs[0].colorLoc = gl.getUniformLocation(this._programs[0], "u_color");
        this._programs[0].selectedLoc = gl.getUniformLocation(this._programs[0], "u_selected");
        this._programs[0].vertLoc = gl.getAttribLocation(this._programs[0], "a_vertex");
        this._programs[0].indexLoc = gl.getAttribLocation(this._programs[0], "a_index");

        var fragmentShader2 = gl.createShader(gl.FRAGMENT_SHADER);
        gl.shaderSource(fragmentShader2, fragCode2);
        gl.compileShader(fragmentShader2);
        if (!gl.getShaderParameter(fragmentShader2, gl.COMPILE_STATUS)) {
            alert(gl.getShaderInfoLog(fragmentShader2));
        }

        this._programs[1] = gl.createProgram();
        gl.attachShader(this._programs[1], vertexShader);
        gl.attachShader(this._programs[1], fragmentShader2);
        gl.linkProgram(this._programs[1]);
        if (!gl.getProgramParameter(this._programs[1], gl.LINK_STATUS)) {
            alert("Could not initialise shaders");
        }

        gl.useProgram(this._programs[1]);

        this._programs[1].matLoc = gl.getUniformLocation(this._programs[1], "u_matrix");
        this._programs[1].pointSize = gl.getUniformLocation(this._programs[1], "u_pointSize");
        this._programs[1].vertLoc = gl.getAttribLocation(this._programs[1], "a_vertex");
        this._programs[1].indexLoc = gl.getAttribLocation(this._programs[1], "a_index");

        gl.useProgram(null);

        this._vertBuffer = gl.createBuffer();
    },


    _checkData: function(data) {
        if ( data == null ) return false;
        try {
            for ( var i = 0; i < data.length; ++i )
                if ( data[i].length !== 3 ) return false;
            return true;

        } catch (err) {
            console.log(err);
            return false;
        }
    },


    _initBuffers: function() {
        var gl = this._gl;
        if ( gl == null || this._data == null ) return;

        var verts = [];
        // ??? Why not id = 0; id < this._data.length
        for ( var id = 1; id <= this._data.length; ++id ) {
            var pixel = this._LatLongToPixel_XY(this._data[id - 1][0], this._data[id - 1][1]);

            // ??? is r, g, b here the color? Or we just need to present the data in 3-dimensions?
            ///// id = r + 256*g + 256^2*b
            ///// id is up to 2^12 which is 1 billion
            var r = Math.floor(id / (65536));
            var g = Math.floor((id % (65536)) / 256);
            var b = id % 256;

            verts.push(pixel.x, pixel.y, r, g, b);
        }

        this._vertArray = new Float32Array(verts);
        var fsize = this._vertArray.BYTES_PER_ELEMENT;

        gl.bindBuffer(gl.ARRAY_BUFFER, this._vertBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, this._vertArray, gl.STATIC_DRAW);

        gl.useProgram(this._programs[0]);
        gl.vertexAttribPointer(this._programs[0].vertLoc, 2, gl.FLOAT, false, fsize*5, 0);
        gl.enableVertexAttribArray(this._programs[0].vertLoc);
        gl.vertexAttribPointer(this._programs[0].indexLoc, 3, gl.FLOAT, false, fsize*5, fsize*2);
        gl.enableVertexAttribArray(this._programs[0].indexLoc);

        gl.useProgram(this._programs[1]);
        gl.vertexAttribPointer(this._programs[1].vertLoc, 2, gl.FLOAT, false, fsize*5, 0);
        gl.enableVertexAttribArray(this._programs[1].vertLoc);
        gl.vertexAttribPointer(this._programs[1].indexLoc, 3, gl.FLOAT, false, fsize*5, fsize*2);
        gl.enableVertexAttribArray(this._programs[1].indexLoc);

        this.render();
    },


    _initTextures: function() {
        var gl = this._gl;
        if ( gl == null ) return;

        var canvas = this.getCanvas();
        // console.log(canvas.width + ' x ' + canvas.height);

        this._fbTexture = gl.createTexture();
        gl.bindTexture(gl.TEXTURE_2D, this._fbTexture);
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, canvas.width, canvas.height, 0, gl.RGBA, gl.UNSIGNED_BYTE, null);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);

        this._fb = gl.createFramebuffer();
        gl.bindFramebuffer(gl.FRAMEBUFFER, this._fb);
        gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, this._fbTexture, 0);
        gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    },


    _reset: function() {
        L.CanvasLayer.prototype._reset.call(this);

        var gl = this._gl;
        if ( gl == null ) return;

        var canvas = this.getCanvas();
        var fbTexture = this._fbTexture;

        gl.bindTexture(gl.TEXTURE_2D, fbTexture);
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, canvas.width, canvas.height, 0, gl.RGBA, gl.UNSIGNED_BYTE, null);
    },


    _mousemove: function(e) {
        var canvas = this.getCanvas();
        var rect = canvas.getBoundingClientRect();
        this.setCurrentPoint(Math.floor(e.originalEvent.clientX - rect.left + 0.5),
                             Math.floor(e.originalEvent.clientY - rect.top + 0.5));
    },


    render: function() {
        var gl = this._gl, map = this._map;
        if ( gl == null || map == null ) return;

        var canvas = this.getCanvas();
        var pixelsToWebGLMatrix = new Float32Array(16);
        var mapMatrix = new Float32Array(16);

        pixelsToWebGLMatrix.set([2 / canvas.width, 0, 0, 0, 0, -2 / canvas.height, 0, 0, 0, 0, 0, 0, -1, 1, 0, 1]);
        var pointSize = Math.max(map.getZoom() - 4.0, this._pointSize);
        // var pointSize = Math.max(map.getZoom() - 4.0, 1.0);
        console.log("map.getZoom() = " + map.getZoom() + ", pointSize = " + pointSize);
        mapMatrix.set(pixelsToWebGLMatrix);
        var bounds = map.getBounds();
        var topLeft = new L.LatLng(bounds.getNorth(), bounds.getWest());
        var offset = this._LatLongToPixel_XY(topLeft.lat, topLeft.lng);
        var scale = Math.pow(2, map.getZoom());
        this._scaleMatrix(mapMatrix, scale, scale);
        this._translateMatrix(mapMatrix, -offset.x, -offset.y);

        gl.viewport(0, 0, canvas.width, canvas.height);

        // Pass 1
        // Draw the blue pinned points

        gl.useProgram(this._programs[1]);
        gl.bindFramebuffer(this._gl.FRAMEBUFFER, this._fb);
        gl.disable(gl.BLEND);

        gl.clearColor(0.0, 0.0, 0.0, 0.0);
        gl.clear(gl.COLOR_BUFFER_BIT);

        gl.vertexAttrib1f(this._programs[1].aPointSize, pointSize);
        gl.uniformMatrix4fv(this._programs[1].matLoc, false, mapMatrix);
        gl.uniform1f(this._programs[1].pointSize, 1.2*pointSize);

        if ( this._data )
            gl.drawArrays(gl.POINTS, 0, this._data.length);

        // Pass 2
        // Compute the cursor location to pinned points/tweets map

        gl.useProgram(this._programs[0]);
        gl.bindFramebuffer(gl.FRAMEBUFFER, null);
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
        gl.enable(gl.BLEND);

        gl.clearColor(0.0, 0.0, 0.0, 0.0);
        gl.clear(gl.COLOR_BUFFER_BIT);

        gl.vertexAttrib1f(this._programs[0].aPointSize, 10.0);
        gl.uniformMatrix4fv(this._programs[0].matLoc, false, mapMatrix);

        console.log("pointColor = [" + this._pointColorX + "," + this._pointColorY + "," + this._pointColorZ + "]");
        gl.uniform3f(this._programs[0].colorLoc,
          this._pointColorX,
          this._pointColorY,
          this._pointColorZ);
        gl.uniform1f(this._programs[0].selectedLoc, this._cid);
        gl.uniform1f(this._programs[0].pointSize, pointSize);

        if ( this._data )
            gl.drawArrays(gl.POINTS, 0, this._data.length);
    },


    _LatLongToPixel_XY: function(latitude, longitude) {
        // display webgl data on google maps
        var pi_180 = Math.PI / 180.0;
        var pi_4 = Math.PI * 4;
        var sinLatitude = Math.sin(latitude * pi_180);
        var pixelY = (0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (pi_4)) * 256;
        var pixelX = ((longitude + 180) / 360) * 256;
        var pixel = {
            x: pixelX,
            y: pixelY
        };
        return pixel;
    },


    _translateMatrix: function(matrix, tx, ty) {
        // translation is in last column of matrix
        matrix[12] += matrix[0] * tx + matrix[4] * ty;
        matrix[13] += matrix[1] * tx + matrix[5] * ty;
        matrix[14] += matrix[2] * tx + matrix[6] * ty;
        matrix[15] += matrix[3] * tx + matrix[7] * ty;
    },


    _scaleMatrix: function(matrix, scaleX, scaleY) {
        // scaling x and y, which is just scaling first two columns of matrix
        matrix[0] *= scaleX;
        matrix[1] *= scaleX;
        matrix[2] *= scaleX;
        matrix[3] *= scaleX;
        matrix[4] *= scaleY;
        matrix[5] *= scaleY;
        matrix[6] *= scaleY;
        matrix[7] *= scaleY;
    }
});