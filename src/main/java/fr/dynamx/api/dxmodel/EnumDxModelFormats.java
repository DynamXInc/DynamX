package fr.dynamx.api.dxmodel;

public enum EnumDxModelFormats {

    OBJ, GLTF;

    public static boolean isValidFormat(String path){
        for (EnumDxModelFormats value : values()) {
            if(path.endsWith(value.name().toLowerCase()))
                return true;
        }
        return false;
    }
}
