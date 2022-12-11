package fr.dynamx.api.contentpack.registry;

import fr.dynamx.api.contentpack.object.INamedObject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//TODO doc
public interface IPackFilePropertyFixer
{
    FixResult fixInputField(INamedObject object, String key, String value);

    class FixResult {
        private final String newKey;
        private final boolean isDeprecation, keepOldKey;

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

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface PackFilePropertyFixer {
        SubInfoTypeRegistries[] registries();
    }
}
