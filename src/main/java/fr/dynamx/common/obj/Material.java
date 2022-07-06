package fr.dynamx.common.obj;

import fr.dynamx.common.obj.texture.MaterialTexture;
import org.lwjgl.util.vector.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class Material
{
    private final String name;
    public Vector3f diffuseColor;
    public Vector3f ambientColor;
    public final Map<String, MaterialTexture> diffuseTexture = new HashMap<>();
    public final Map<String, MaterialTexture> ambientTexture = new HashMap<>();
    public float transparency;

    public Material(String name) {
        transparency = 1f;
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
