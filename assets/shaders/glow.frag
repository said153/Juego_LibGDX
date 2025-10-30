// Fragment Shader para efecto Glow estilo Tron
#ifdef GL_ES
precision mediump float;
#endif

uniform vec3 u_cameraPosition;
uniform vec3 u_glowColor;
uniform float u_glowIntensity;

varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoords;

void main() {
    // Calcular dirección de la vista
    vec3 viewDir = normalize(u_cameraPosition - v_position);
    
    // Calcular el efecto Fresnel (brillo en los bordes)
    float fresnelTerm = 1.0 - max(0.0, dot(viewDir, v_normal));
    fresnelTerm = pow(fresnelTerm, 3.0);
    
    // Aplicar color de glow con intensidad
    vec3 glowEffect = u_glowColor * fresnelTerm * u_glowIntensity;
    
    // Color base + efecto glow
    vec3 finalColor = u_glowColor * 0.3 + glowEffect;
    
    // Agregar transparencia basada en el efecto Fresnel
    float alpha = 0.7 + fresnelTerm * 0.3;
    
    gl_FragColor = vec4(finalColor, alpha);
}
