package fr.dynamx.common.objloader;

import fr.dynamx.client.renders.model.texture.MaterialTexture;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.util.vector.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class Material {
    @Getter
    private final String name;
    public Vector3f diffuseColor;
    public Vector3f ambientColor;
    public final Map<String, MaterialTexture> diffuseTexture = new HashMap<>();
    public final Map<String, MaterialTexture> ambientTexture = new HashMap<>();
    public float transparency;
    public final Map<String, IndexPair> indexPairPerObject = new HashMap<>();

    public Material(String name) {
        this.name = name;
        transparency = 1f;
    }

    public static class IndexPair {
        @Getter
        @Setter
        private int startIndex, finalIndex;

        public IndexPair(int startIndex, int finalIndex) {
            this.startIndex = startIndex;
            this.finalIndex = finalIndex;
        }
    }
}
