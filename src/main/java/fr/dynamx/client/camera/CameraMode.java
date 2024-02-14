package fr.dynamx.client.camera;

import com.jme3.math.Quaternion;

import java.util.function.BiFunction;

/**
 * Vehicle camera modes for {@link CameraSystem}
 */
public enum CameraMode {
    /**
     * Automatically between FIXED and FREE according to thirdPersonView value
     */
    AUTO((i, q) -> {
        if (i != 0)
            q.set(0, q.getY(), 0, q.getW());
        return null;
    }),
    /**
     * Keeps vehicle rotation on all axis
     */
    FIXED((i, q) -> null),
    /**
     * Keeps vehicle rotation on Y axis
     */
    FREE((i, q) -> {
        q.set(0, 0, 0, 1);
        return null;
    });

    /**
     * Function giving thirdPersonView value and camera rotation quaternion, should return null
     */
    public final BiFunction<Integer, Quaternion, Object> rotator;

    /**
     * Function giving thirdPersonView value and camera rotation quaternion, should return null
     */
    CameraMode(BiFunction<Integer, Quaternion, Object> rotator) {
        this.rotator = rotator;
    }
}