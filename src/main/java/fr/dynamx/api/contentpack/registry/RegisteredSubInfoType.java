package fr.dynamx.api.contentpack.registry;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * TODO UPDATE DOC
 /**
 * Registers a sub info type, its key should be unique or an IllegalArgumentException is thrown <br>
 * Should be called before any content pack is loaded (in addons initialization for example)
 */
/**
 * Creates a new sub info type registry entry
 *
 * @param key     should be the name of this sub info in the info file (or contain if not strict as wheels, shapes and seats)
 * @param creator a {@link ISubInfoTypeCreator} creating an instance of the sub info type
 * @param strict  if strict (default value), it will check if key is equals, else if key is contained in vehicle info field
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisteredSubInfoType {
    String name();
    SubInfoTypeRegistries[] registries();
    boolean strictName() default true;
}