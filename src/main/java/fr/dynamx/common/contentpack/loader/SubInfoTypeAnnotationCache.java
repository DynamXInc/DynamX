package fr.dynamx.common.contentpack.loader;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal cache for loaded {@link PackFilePropertyData}
 *
 * @see PackFileProperty
 */
public class SubInfoTypeAnnotationCache {
    private static final Map<Class<?>, Map<String, PackFilePropertyData<?>>> cache = new HashMap<>();

    /**
     * Checks in the cache to find the specified property data, or loads properties of the given object's class to find it
     *
     * @param loadedObject The object containing the property
     * @param propertyName The property name
     * @return A matching {@link PackFilePropertyData}, or null if not found
     */
    @Nullable
    public static PackFilePropertyData<?> getFieldFor(INamedObject loadedObject, String propertyName) {
        return getOrLoadData(loadedObject.getClass()).get(propertyName);
    }

    /**
     * Checks in the cache to get the properties of the given class, or loads them
     *
     * @param from The class
     * @return All the properties found in the given class
     */
    @Nonnull
    public static Map<String, PackFilePropertyData<?>> getOrLoadData(Class<?> from) {
        if (!cache.containsKey(from))
            load(from);
        return cache.get(from);
    }

    private static void load(Class<?> toCache) {
        Map<String, PackFilePropertyData<?>> data = new HashMap<>();
        List<PackFilePropertyData<?>> fields = new ArrayList<>();
        for (Field f : toCache.getDeclaredFields()) {
            if (f.isAnnotationPresent(PackFileProperty.class)) {
                DefinitionType<?> type = f.getAnnotation(PackFileProperty.class).type().type;
                if (type == null)
                    type = DefinitionType.getParserOf(f.getType());
                if (type != null) {
                    PackFileProperty property = f.getAnnotation(PackFileProperty.class);
                    for (String configName : f.getAnnotation(PackFileProperty.class).configNames()) {
                        PackFilePropertyData<?> d = new PackFilePropertyData<>(f, configName, type, property.required(),
                                property.newConfigName().isEmpty() ? null : property.newConfigName(), property.description(), property.defaultValue());
                        data.put(d.getConfigFieldName(), d);
                        fields.add(d);
                    }
                    for (String oldName : f.getAnnotation(PackFileProperty.class).oldNames()) {
                        PackFilePropertyData<?> d = new PackFilePropertyData<>(f, oldName, type, f.getAnnotation(PackFileProperty.class).required(),
                                f.getAnnotation(PackFileProperty.class).configNames()[0], "", "");
                        data.put(oldName, d);
                        fields.add(d);
                    }
                } else {
                    throw new IllegalArgumentException("No parser for field " + f.getName() + " of " + toCache.getName());
                }
            }
        }
        if (toCache.getSuperclass() != null) {
            Map<String, PackFilePropertyData<?>> dataMap = getOrLoadData(toCache.getSuperclass());
            data.putAll(dataMap);
            fields.addAll(dataMap.values());
        }
        //ContentPackDocGenerator.generateDoc(toCache.getSimpleName(), "fr_fr", fields);
        //System.out.println("Found "+data.size()+" fields in "+toCache.getName());
        cache.put(toCache, data);
    }
}
