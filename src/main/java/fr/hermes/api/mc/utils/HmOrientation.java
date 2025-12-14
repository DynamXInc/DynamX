package fr.hermes.api.mc.utils;

import lombok.Getter;
import org.joml.Vector3f;

public enum HmOrientation {
    DOWN(new Vector3f(0, -1, 0)),
    UP(new Vector3f(0, 1, 0)),
    NORTH(new Vector3f(0, 0, -1)),
    SOUTH(new Vector3f(0, 0, 1)),
    WEST(new Vector3f(-1, 0, 0)),
    EAST(new Vector3f(1, 0, 0)),;

    @Getter
    private final Vector3f directionVec;

    HmOrientation(Vector3f directionVec) {
        this.directionVec = directionVec;
    }
}
