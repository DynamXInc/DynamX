package fr.dynamx.api.contentpack.registry;

import fr.dynamx.api.contentpack.object.INamedObject;

import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An IPackFilePropertyFixer will handle old info file config formats (old/deprecated property names or formats) <br>
 * Should be registered with @{@link PackFilePropertyFixer}
 *
 * @see fr.dynamx.api.contentpack.object.part.BasePart
 */
public interface IPackFilePropertyFixer
{
    /**
     * Fixes the given input
     *
     * @param object The object where this input was found
     * @param key The property name
     * @param value The property value
     * @return A {@link FixResult} if the property was modified, or null
     */
    @Nullable
    FixResult fixInputField(INamedObject object, String key, String value);

    /**
     * The result of fixing an old property
     */
    class FixResult {
        /**
         * The new property name (can be the same as the old)
         */
        private final String newKey;
        /**
         * If a deprecation warning should be displayed
         */
        private final boolean isDeprecation;
        /**
         * If the old key should be used to handle the property <br>
         * This allows to specify a 'newKey' different from the old one, that will be showed to the user, but internally use the old key to handle this property
         */
        private final boolean keepOldKey;

        public FixResult(String newKey, boolean isDeprecation) {
            this(newKey, isDeprecation, false);
        }

        public FixResult(String newKey, boolean isDeprecation, boolean keepOldKey) {
            this.newKey = newKey;
            this.isDeprecation = isDeprecation;
            this.keepOldKey = keepOldKey;
        }

        public String newKey() {
            return newKey;
        }

        public String newValue(String oldValue) {return oldValue;}

        public boolean isDeprecation() {
            return isDeprecation;
        }

        public boolean isKeepOldKey() {
            return keepOldKey;
        }
    }

    /**
     * Registers a {@link fr.dynamx.api.contentpack.object.IPackInfoReloadListener} in the given {@link SubInfoTypeRegistries}
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface PackFilePropertyFixer {
        /**
         * @return The {@link SubInfoTypeRegistries} where the properties should be fixed
         */
        SubInfoTypeRegistries[] registries();
    }
}
