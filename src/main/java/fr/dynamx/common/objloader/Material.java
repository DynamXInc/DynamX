package fr.dynamx.common.objloader;


import fr.dynamx.client.renders.model.texture.MaterialTexture;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class Material {
    @Getter
    private final String name;
    public Vector3f diffuseColor = new Vector3f();
    public Vector3f ambientColor = new Vector3f();
    public final Map<String, MaterialTexture> diffuseTexture = new HashMap<>();
    public final Map<String, MaterialTexture> ambientTexture = new HashMap<>();
    public float transparency = 1f;
    public final Map<String, IndexPair> indexPairPerObject = new HashMap<>();

    @AllArgsConstructor
    public static class IndexPair {
        @Getter
        @Setter
        private int startIndex, finalIndex;
    }
}
