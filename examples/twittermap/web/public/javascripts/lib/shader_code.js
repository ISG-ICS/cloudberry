// noinspection JSAnnotator
var vertCode = `
        uniform mat4 u_matrix;
        uniform float u_pointSize;
        attribute vec4 a_vertex;
        attribute vec4 a_index;
        varying vec4 v_index;

        void main() {
            gl_PointSize = u_pointSize;
            gl_Position = u_matrix * a_vertex;
            v_index = a_index;
        }
    `;


// noinspection JSAnnotator
var fragCode = `
        precision mediump float;
        uniform vec3 u_color;
        uniform float u_selected;
        varying vec4 v_index;

        void main() {
            float idx = v_index[0]*65536.0 + v_index[1]*256.0 + v_index[2];
            float border = 0.05;
            float radius = 0.5;
            // ??? color0 not used?
            vec4 color0 = vec4(0.0, 0.0, 0.0, 0.0);
            vec4 color1 = abs(idx - u_selected)< 1e-4 ? vec4(1.0, 0.0, 0.0, 1.0) : vec4(u_color, 0.75);
            //Draw points with the color as the actual **ID**
            //vec4 color1 = abs(idx - u_selected)< 1e-4 ? vec4(1.0, 0.0, 0.0, 1.0) : vec4(v_index[0], v_index[1], v_index[2], 0.75);

            vec2 m = gl_PointCoord.xy - vec2(0.5, 0.5);
            float dist = radius - sqrt(m.x*m.x + m.y*m.y);
            float t = 0.0;
            if ( dist > border )
                t = 1.0;
            else if ( dist > 0.0 )
                t = dist / border;
            gl_FragColor = mix(color0, color1, t);
        }
    `;


// noinspection JSAnnotator
var fragCode2 = `
        precision mediump float;
        varying vec4 v_index;

        void main() {
            float border = 0.05;
            float radius = 0.5;
            vec4 color0 = vec4(0.0, 0.0, 0.0, 1.0);
            // ??? color1 is the ID of the corresponding tweet rather than color of the point?
            vec4 color1 = vec4(v_index.xyz/255.0, 1.0);

            // ??? What's the value of gl_PointCoord.xy? Where is (0.5, 0.5)?
            vec2 m = gl_PointCoord.xy - vec2(0.5, 0.5);
            // Is the dist used? The point to be detected seems to be the one within the gl_PointSize?
            float dist = radius - sqrt(m.x*m.x + m.y*m.y);
            // color0 is never used?
            //gl_FragColor = dist > border ? color1 : color1;
            gl_FragColor = dist > border ? color1 : color0;
        }
    `;
