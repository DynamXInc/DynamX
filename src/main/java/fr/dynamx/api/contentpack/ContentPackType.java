package fr.dynamx.api.contentpack;

public enum ContentPackType {
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
