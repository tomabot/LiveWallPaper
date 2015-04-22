// a vertex shader is called once for each vertex
// an attribute is an input parameter to the shader
attribute vec4 a_Position;

void main()
{
    // gl_Position is the final position (after transformations)
    // of each vertext
    gl_Position = a_Position;
}