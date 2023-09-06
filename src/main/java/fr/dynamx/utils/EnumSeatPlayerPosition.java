package fr.dynamx.utils;

public enum EnumSeatPlayerPosition {
    LYING,
    SITTING,
    STANDING;

    public static EnumSeatPlayerPosition fromString(String targetName) {
        for (EnumSeatPlayerPosition enumSeatPlayerPosition : values()) {
            if (enumSeatPlayerPosition.name().equalsIgnoreCase(targetName)) {
                return enumSeatPlayerPosition;
            }
        }
        throw new IllegalArgumentException("Invalid seat player position '" + targetName + "'");
    }
}