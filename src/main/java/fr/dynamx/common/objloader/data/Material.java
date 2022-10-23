package fr.dynamx.common.objloader.data;


import fr.dynamx.client.renders.model.texture.MaterialTexture;
import lombok.*;
//import net.optifine.shaders.MultiTexID;


import javax.vecmath.Vector3f;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class Material {
    @Getter
    private final String name;
    public Vector3f diffuseColor = new Vector3f(1,1,1);
    public Vector3f ambientColor = new Vector3f(1,1,1);
    public final Map<String, MaterialTexture> diffuseTexture = new HashMap<>();
    public final Map<String, MaterialTexture> ambientTexture = new HashMap<>();
    public final Map<String, MaterialTexture> specularTexture = new HashMap<>();
    public final Map<String, MaterialTexture> normalTexture = new HashMap<>();
    public float transparency = 1f;

    @Override
    public String toString() {
        return name;
    }

    @ToString
    @AllArgsConstructor
    public static class IndexPair {
        @Getter
        @Setter
        private int startIndex, finalIndex;
    }
}
