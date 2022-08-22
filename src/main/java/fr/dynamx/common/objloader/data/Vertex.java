package fr.dynamx.common.objloader.data;


import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

@AllArgsConstructor
public class Vertex {
    @Getter
    private final Vector3f pos;
    @Getter
    private final Vector2f texCoords;
    @Getter
    private final Vector3f normal;
    @Getter
    private final Vector3f tangent;

}
