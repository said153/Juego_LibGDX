// Vertex Shader para efecto Glow estilo Tron
attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;

varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoords;

void main() {
    v_position = vec3(u_worldTrans * vec4(a_position, 1.0));
    v_normal = normalize(mat3(u_worldTrans) * a_normal);
    v_texCoords = a_texCoord0;
    
    gl_Position = u_projViewTrans * u_worldTrans * vec4(a_position, 1.0);
}
