package fr.dynamx.common.obj;

import fr.dynamx.api.obj.IObjObject;

import javax.vecmath.Vector3f;

public class SimpleObjObject implements IObjObject {
    private final Mesh mesh = new Mesh();
    private Vector3f center;
    private final String name;

    public SimpleObjObject(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Vector3f getCenter() {
        return center;
    }

    @Override
    public void setCenter(Vector3f center) {
        this.center = center;
    }

    @Override
    public Mesh getMesh() {
        return mesh;
    }
}
