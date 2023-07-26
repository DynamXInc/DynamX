package fr.dynamx.common.objloader.data;

import lombok.Getter;
import lombok.Setter;

import javax.vecmath.Vector3f;

public class ObjObjectData{
    @Getter
    private final String name;
    @Getter
    private final Mesh mesh = new Mesh();
    @Getter
    @Setter
    private Vector3f center;

    public ObjObjectData(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ObjObjectData{" +
                "name='" + name + '\'' +
                '}';
    }

    /**
     * Releases memory when the vao objects has been compiled, or the server started
     */
    public void clearData() {
       /*getMesh().indices = null;
        getMesh().vertices = null;
        getMesh().materialForEachVertex = null;*/
    }
}
