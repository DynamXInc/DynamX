package fr.dynamx.api.contentpack.registry;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Registers a {@link fr.dynamx.api.contentpack.object.subinfo.ISubInfoType} in the associated {@link fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisteredSubInfoType {
    /**
     * @return the name of this sub info in the info file, should be unique for the associated {@link SubInfoTypeRegistries}
     */
    String name();

    /**
     * @return The {@link SubInfoTypeRegistries} that will be able to read this {@link fr.dynamx.api.contentpack.object.subinfo.ISubInfoType}
     */
    SubInfoTypeRegistries[] registries();

    /**
     * @return if strict (default value), it will check if 'name' is equals, else if 'name' is contained, in the name of the sub property in the info file
     */
    boolean strictName() default true;
}