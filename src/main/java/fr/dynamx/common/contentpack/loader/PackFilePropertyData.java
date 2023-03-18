package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.doc.ContentPackDocGenerator;
import fr.dynamx.utils.doc.DocLocale;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Represents a configuration field, contains the corresponding field, and the "parser" ({@link DefinitionType}) able to translate the string value of this property into the correct type
 *
 * @param <T> The corresponding class field type
 * @see fr.dynamx.api.contentpack.registry.PackFileProperty
 */
public class PackFilePropertyData<T> {
    private final Field field;
    private final String configFieldName;
    private final DefinitionType<T> type;
    private final boolean required;
    private final String description;
    private final String defaultValue;

    public PackFilePropertyData(Field field, String configFieldName, DefinitionType<T> type, boolean required, String description, String defaultValue) {
        this.field = field;
        this.configFieldName = configFieldName;
        this.type = type;
        this.required = required;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public boolean isRequired() {
        return required;
    }

    /**
     * Parses the provided config value (string format) into the appropriate object to affect it to the class field
     */
    public T parse(String value) {
        return type.getValue(value);
    }

    public Field getField() {
        return field;
    }

    /**
     * @return The name of this property in the config file
     */
    public String getConfigFieldName() {
        return configFieldName;
    }

    /**
     * @return The parser of this property
     */
    public DefinitionType<T> getType() {
        return type;
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
        String docKey = description.isEmpty() ? field.getDeclaringClass().getSimpleName() + "." + configFieldName : description;
        String sep = "|";
        String typeName = locale.format(this.type.getTypeName());
        builder.append(sep).append(configFieldName).append(sep).append(typeName).append(sep)
                .append(locale.format(docKey)).append(sep).append(defaultValue.isEmpty() ? "   " : defaultValue).append(sep).append("\n");
    }
}
