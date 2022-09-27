package fr.dynamx.api.contentpack.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a property in an object's config file : <br>
 * The loader will search for this property in the corresponding info file (or {@link fr.dynamx.api.contentpack.object.subinfo.SubInfoType}) <br>
 * and automatically parse and load its value in the java field owning this annotation (note that the java field can be private but <strong>NOT</strong> final)
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PackFileProperty {
    /**
     * @return The recognized names of this property in the config file/sub category
     */
    String[] configNames();

    /**
     * All parsers are in {@link fr.dynamx.api.contentpack.registry.DefinitionType.DynamXDefinitionTypes}
     *
     * @return A parser for this property, you should left the default, AUTO, parser in most cases (except vectors of different formats for example)
     */
    DefinitionType.DynamXDefinitionTypes type() default DefinitionType.DynamXDefinitionTypes.AUTO;

    /**
     * If a required property is missing in a config file, the user will have an error message in the loading log
     *
     * @return If the property if required in the config file, true by default
     */
    boolean required() default true;

    /**
     * For documentation generator
     *
     * @return Translation key for the description, leave empty to use default ClassName.ConfigName
     */
    String description() default "";

    /**
     * For documentation generator
     *
     * @return The default value of this field
     */
    String defaultValue() default "";
}
