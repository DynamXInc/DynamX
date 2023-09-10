package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.doc.ContentPackDocGenerator;
import fr.dynamx.utils.doc.DocLocale;
import lombok.Getter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Represents a configuration field, contains the corresponding field, and the "parser" ({@link DefinitionType}) able to translate the string value of this property into the correct type
 *
 * @param <T> The corresponding class field type
 * @see fr.dynamx.api.contentpack.registry.PackFileProperty
 */
public class PackFilePropertyData<T> {
    @Getter
    private final Field field;
    /**
     * The name of this property in the config file
     */
    @Getter
    private final String configFieldName;
    @Getter
    private final String[] aliases;
    /**
     * The parser of this property
     */
    @Getter
    private final DefinitionType<T> type;
    @Getter
    private final boolean required;
    @Getter
    private final String description;
    @Getter
    private final String defaultValue;

    public PackFilePropertyData(Field field, String configFieldName, String[] aliases, DefinitionType<T> type, boolean required, String description, String defaultValue) {
        this.field = field;
        this.configFieldName = configFieldName;
        this.aliases = aliases;
        this.type = type;
        this.required = required;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    /**
     * Parses the provided config value (string format) into the appropriate object to affect it to the class field
     */
    public T parse(String value) {
        return type.getValue(value);
    }

    /**
     * Parses the string value of this field an injects it into the given object (assuming it has the java field)
     *
     * @param on    The loading object
     * @param value The string value of this property
     * @return The corresponding property data, or null if parse failed
     * @throws IllegalAccessException If reflection fails
     */
    @SuppressWarnings("unchecked")
    public PackFilePropertyData<T> apply(INamedObject on, String value) throws IllegalAccessException {
        T val;
        try {
            val = parse(value);
        } catch (Exception e) {
            DynamXErrorManager.addError(on.getPackName(), DynamXErrorManager.PACKS_ERRORS, "property_parse_error", ErrorLevel.HIGH, on.getName(), getConfigFieldName(), e);
            return null; //Error while parsing
        }
        if(Modifier.isFinal(field.getModifiers()))
            throw new IllegalArgumentException("Field " + field + " is final : cannot use it as a PackFileProperty !");
        field.setAccessible(true);
        field.set(on, val);
        field.setAccessible(false);
        return this;
    }

    public void writeDocLine(StringBuilder builder, DocLocale locale, ContentPackDocGenerator.DocType type) {
        if (isRequired()) {
            if (type != ContentPackDocGenerator.DocType.REQUIRED)
                return;
        } else {
            if (type != ContentPackDocGenerator.DocType.OPTIONAL)
                return;
        }
        if(!configFieldName.equals(aliases[0]))
            return;
        String docKey = description.isEmpty() ? field.getDeclaringClass().getSimpleName() + "." + configFieldName : description;
        String sep = "|";
        String typeName = locale.format(this.type.getTypeName());
        builder.append(sep);
        builder.append(Arrays.stream(aliases).reduce((s, s2) -> s + ", " + s2).orElse("error"));
        builder.append(sep).append(typeName).append(sep)
                .append(locale.format(docKey)).append(sep).append(defaultValue.isEmpty() ? " - " : defaultValue).append(sep).append("\n");
    }
}
