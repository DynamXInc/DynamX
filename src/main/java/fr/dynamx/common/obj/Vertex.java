package fr.dynamx.common.obj;


import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

public class Vertex {

    private final Vector3f pos;
    private final Vector2f texCoords;
    private final Vector3f normal;
    private final Vector3f tangent;

    public Vertex(Vector3f pos, Vector2f texCoords, Vector3f normal, Vector3f tangent) {
        this.pos = pos;
        this.texCoords = texCoords;
        this.normal = normal;
        this.tangent = tangent;
    }

    public Vector3f getPos() {
        return pos;
    }

    public Vector2f getTexCoords() {
        return texCoords;
    }

    public Vector3f getNormal() {
        return normal;
    }

    public Vector3f getTangent() {
        return tangent;
    }

}
