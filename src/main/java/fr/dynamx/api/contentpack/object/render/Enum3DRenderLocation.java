package fr.dynamx.api.contentpack.object.render;

public enum Enum3DRenderLocation {
    NONE, WORLD, ALL;

    public static Enum3DRenderLocation fromString(String targetName) {
        for (Enum3DRenderLocation entityequipmentslot : values()) {
            if (entityequipmentslot.name().equalsIgnoreCase(targetName)) {
                return entityequipmentslot;
            }
        }
        throw new IllegalArgumentException("Invalid render location '" + targetName + "'");
    }
}
