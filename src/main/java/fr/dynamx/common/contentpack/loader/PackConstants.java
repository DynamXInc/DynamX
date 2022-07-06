package fr.dynamx.common.contentpack.loader;

import com.jme3.math.FastMath;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PackConstants {

    PI(FastMath.PI), HALF_PI(FastMath.HALF_PI);

    private final double value;
}
