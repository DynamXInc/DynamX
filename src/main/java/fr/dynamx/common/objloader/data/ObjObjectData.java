package fr.dynamx.common.objloader.data;

import fr.dynamx.common.objloader.Mesh;
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

}
