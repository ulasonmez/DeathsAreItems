#version 150
#moj_import <fog.glsl>
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float Desaturate;
uniform float AlphaCutoff;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in float vertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    vec4 sprite = texture(Sampler0, texCoord0);
    float brightness = max(sprite.r, max(sprite.g, sprite.b));
    if (sprite.a < AlphaCutoff) discard;
    vec3 rgb = mix(sprite.rgb, vec3(brightness), Desaturate);
    float alpha = mix(1.0, sprite.a, Desaturate);
    vec4 color = vec4(rgb, alpha) * vertexColor * ColorModulator;
    if (color.a < 0.1) discard;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
