precision mediump float;
uniform vec3 u_LightPos;       // light position transformed to view space
varying vec3 v_Position;       // interpolated position for this fragment.
varying vec4 v_Color;          // color from the vertex shader interpolated to fragment
varying vec3 v_Normal;         // interpolated normal for this fragment.
void main() {
    // used for attenuation calculation
    float distance = length(u_LightPos - v_Position);
    // direction vector from the light to the vertex
    vec3 lightVector = normalize(u_LightPos - v_Position);
    // dot product of the light vector and vertex normal. Max illumination when
    // they are both pointing in the same direction
    float diffuse = max(dot(v_Normal, lightVector), 0.1);
    // Add attenuation.
    diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));
    // final color is color multiplied by the diffuse illumination
    gl_FragColor = v_Color * diffuse;
}
