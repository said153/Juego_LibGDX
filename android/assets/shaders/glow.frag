// Fragment Shader para efectos de glow TRON
#ifdef GL_ES
precision mediump float;
#endif

varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoords;

uniform vec3 u_cameraPosition;
uniform vec4 u_diffuseColor;
uniform vec4 u_emissiveColor;
uniform vec3 u_ambientLight;
uniform float u_shininess;

void main() {
    vec3 normal = normalize(v_normal);
    vec3 viewDir = normalize(u_cameraPosition - v_position);
    
    // Color base difuso
    vec3 diffuse = u_diffuseColor.rgb;
    
    // Color emisivo (neón brillante)
    vec3 emissive = u_emissiveColor.rgb * u_emissiveColor.a;
    
    // Luz ambiente
    vec3 ambient = u_ambientLight * diffuse;
    
    // Fresnel effect (bordes brillantes)
    float fresnel = pow(1.0 - max(dot(viewDir, normal), 0.0), 3.0);
    vec3 fresnelGlow = emissive * fresnel * 2.0;
    
    // Specular (brillo)
    vec3 reflectDir = reflect(-viewDir, normal);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), u_shininess);
    vec3 specular = vec3(spec) * 0.5;
    
    // Efecto de glow intenso para neón
    float glowIntensity = length(emissive);
    vec3 glow = emissive * (1.0 + glowIntensity * 0.5);
    
    // Combinación final
    vec3 finalColor = ambient + diffuse * 0.3 + glow + fresnelGlow + specular;
    
    // Aumentar brillo general para efecto TRON
    finalColor = finalColor * 1.3;
    
    gl_FragColor = vec4(finalColor, u_diffuseColor.a);
}
