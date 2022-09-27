package fr.dynamx.utils;

public enum EnumPlayerStandOnTop {
    ALWAYS, NEVER, PROGRESSIVE;

    public static EnumPlayerStandOnTop fromString(String targetName) {
        for (EnumPlayerStandOnTop enumPlayerStandOnTop : values()) {
            if (enumPlayerStandOnTop.name().equalsIgnoreCase(targetName)) {
                return enumPlayerStandOnTop;
            }
        }
        throw new IllegalArgumentException("Invalid player stand on top value '" + targetName + "'");
    }
}