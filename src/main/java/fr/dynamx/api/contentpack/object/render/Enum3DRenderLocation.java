package fr.dynamx.api.contentpack.object.render;

/**
 * Determines the locations (world, guis, ...) where a 3D model should be used to render an item
 */
public enum Enum3DRenderLocation {
    /**
     * The item should not be rendered in 3D, the json model will always be used
     */
    NONE,
    /**
     * The item should be rendered in 3D when in the world <br>
     * The json model will be used in guis
     */
    WORLD,
    /**
     * The item should always be rendered in 3D (in world and guis) (default behavior)
     */
    ALL;

    public static Enum3DRenderLocation fromString(String targetName) {
        for (Enum3DRenderLocation entityequipmentslot : values()) {
            if (entityequipmentslot.name().equalsIgnoreCase(targetName)) {
                return entityequipmentslot;
            }
        }
        throw new IllegalArgumentException("Invalid render location '" + targetName + "'");
    }
}
