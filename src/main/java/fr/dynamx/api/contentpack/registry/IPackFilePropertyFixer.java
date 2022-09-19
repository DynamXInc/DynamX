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
        private final boolean isDeprecation;

        public FixResult(String newKey, boolean isDeprecation) {
            this.newKey = newKey;
            this.isDeprecation = isDeprecation;
        }

        public String newKey() {
            return newKey;
        }

        public String newValue(String oldValue) {return oldValue;}

        public boolean isDeprecation() {
            return isDeprecation;
        }
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface PackFilePropertyFixer {
        SubInfoTypeRegistries[] registries();
    }
}
