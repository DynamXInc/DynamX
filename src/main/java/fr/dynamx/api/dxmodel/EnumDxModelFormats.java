package fr.dynamx.api.dxmodel;

public enum EnumDxModelFormats {

    OBJ, GLTF, JSON;

    public static boolean isDxModel(String path){
        return path.endsWith(OBJ.name().toLowerCase()) || path.endsWith(GLTF.name().toLowerCase());
    }
}
