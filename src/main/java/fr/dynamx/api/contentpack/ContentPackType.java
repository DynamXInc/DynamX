package fr.dynamx.api.contentpack;

/**
 * The type of a content pack
 */
public enum ContentPackType {
    NOTSET(false),
    FOLDER(false),
    DNXPACK(true),
    ZIP(true),
    BUILTIN(true);

    private final boolean isCompressed;

    ContentPackType(boolean isCompressed) {
        this.isCompressed = isCompressed;
    }

    public boolean isCompressed() {
        return isCompressed;
    }
}
