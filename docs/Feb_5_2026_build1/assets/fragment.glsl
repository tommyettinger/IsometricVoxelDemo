#ifdef GL_ES
#extension GL_OES_standard_derivatives : enable
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif
varying vec2 v_texCoords;
varying LOWP vec4 v_color;
uniform sampler2D u_texture;
// This uniform must be set after the batch starts to the dimensions of the Texture being rendered.
uniform vec2 u_textureResolution;

// Adapted from https://www.shadertoy.com/view/ltfXWS by Permutator, CC0 licensed (no attribution necessary).

vec2 v2len(vec2 a, vec2 b) {
    return sqrt(a*a+b*b);
}

void main() {
    vec2 uv = v_texCoords * u_textureResolution;
    vec2 seam = floor(uv+.5);
    uv = seam + clamp((uv-seam)/v2len(dFdx(uv),dFdy(uv)), -.5, .5);
    gl_FragColor = texture2D(u_texture, uv/u_textureResolution) * v_color;
}
