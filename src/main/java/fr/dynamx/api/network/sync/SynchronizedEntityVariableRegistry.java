package fr.dynamx.api.network.sync;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.sync.PhysicsEntitySynchronizer;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Predicate;

/**
 * A registry for all the EntityVariableSerializers, for each synchronized entity variable of each class
 */
public class SynchronizedEntityVariableRegistry {
    private static final Map<Class<?>, List<String>> baseSyncVarRegistry = new HashMap<>();
    private static final Map<Class<?>, String> classToMod = new HashMap<>();
    private static final Map<String, Field> fieldMap = HashBiMap.create();
    private static final BiMap<String, Integer> syncVarRegistry = HashBiMap.create();
    @Getter
    private static final Map<Integer, EntityVariableSerializer<?>> serializerMap = new HashMap<>();

    /**
     * Discovers all the synchronized entity variables in the loaded mods
     */
    public static void discoverSyncVars(FMLConstructionEvent event) {
        Set<ASMDataTable.ASMData> modData = event.getASMHarvestedData().getAll(SynchronizedEntityVariable.SynchronizedPhysicsModule.class.getName());
        for (ASMDataTable.ASMData data : modData) {
            String name = data.getClassName();
            try {
                Class<?> classToParse = Class.forName(data.getClassName());
                String modid = data.getAnnotationInfo().get("modid").toString();
                classToMod.put(classToParse, modid);
                for (Field f : classToParse.getDeclaredFields()) {
                    if (EntityVariable.class.isAssignableFrom(f.getType()) && f.isAnnotationPresent(SynchronizedEntityVariable.class)) {
                        SynchronizedEntityVariable property = f.getAnnotation(SynchronizedEntityVariable.class);
                        if (!baseSyncVarRegistry.containsKey(classToParse))
                            baseSyncVarRegistry.put(classToParse, new ArrayList<>());
                        String propName = classToParse.getSimpleName() + "." + property.name();
                        DynamXMain.log.debug("Registered synchronized entity variable " + propName + " in " + classToParse.getName());
                        baseSyncVarRegistry.get(classToParse).add(propName);
                        fieldMap.put(propName, f);
                    }
                }
                if (!baseSyncVarRegistry.containsKey(classToParse))
                    DynamXMain.log.error("Failed to detect any synchronized entity variable in " + classToParse);
            } catch (Exception e) {
                throw new RuntimeException("Failed to detect synchronized entity variables in " + name, e);
            }
        }
    }

    /**
     * Finds the index of the given variable in the given list
     *
     * @param of The variable to find
     * @param in The list to search in
     * @return The index of the variable
     */
    private static int getIndex(String of, List<String> in) {
        for (int i = 0; i < in.size(); i++) {
            if (in.get(i).equals(of)) {
                return i;
            }
        }
        throw new IllegalArgumentException(of + " is not in input list " + in);
    }

    /**
     * Finds the serializer of the given variable
     *
     * @param variableName The name of the variable
     * @return The serializer of the variable
     * @throws IllegalArgumentException If the variable is not registered
     */
    private static EntityVariableSerializer<?> findSerializer(String variableName) {
        Field f = fieldMap.get(variableName);
        ParameterizedType type;
        if (f.getGenericType() instanceof ParameterizedType) {
            type = (ParameterizedType) f.getGenericType();
        } else if (f.getType().getGenericSuperclass() instanceof ParameterizedType) {
            type = (ParameterizedType) f.getType().getGenericSuperclass();
        } else {
            throw new IllegalArgumentException("Bad entity variable " + f + " name : " + variableName);
        }
        EntityVariableSerializer<?> serializer = EntityVariableTypes.getSerializerRegistry().get(type.getActualTypeArguments()[0]);
        if (serializer == null && type.getActualTypeArguments()[0] instanceof ParameterizedType) {
            serializer = EntityVariableTypes.getSerializerRegistry().get(((ParameterizedType) type.getActualTypeArguments()[0]).getRawType());
            if (serializer == null)
                DynamXMain.log.error("Cannot find serializer for entity variable " + variableName + ". Tried: " + ((ParameterizedType) type.getActualTypeArguments()[0]).getRawType());
        }
        if (serializer == null) {
            DynamXMain.log.error("Cannot find serializer for entity variable " + variableName + ". Tried: " + type.getActualTypeArguments()[0] + ". Generic type is " + f.getGenericType() + ". Generic superclass is " + f.getType().getGenericSuperclass());
            throw new IllegalArgumentException("Bad entity variable " + f + " " + type.getActualTypeArguments()[0] + " name : " + variableName);
        }
        return serializer;
    }

    /**
     * Sorts variable ids in alphabetical order
     */
    public static void sortRegistry(Predicate<String> useMod) {
        DynamXMain.log.debug("Sorting SynchronizedVariables registry ids...");
        List<String> buff = new ArrayList<>();
        for (Class<?> res : baseSyncVarRegistry.keySet()) {
            if (useMod.test(classToMod.get(res))) {
                buff.addAll(baseSyncVarRegistry.get(res));
            }
        }
        buff.sort(Comparator.comparing(String::toString)); //Unique sorting
        syncVarRegistry.clear();
        serializerMap.clear();
        for (String res : buff) {
            int index = getIndex(res, buff);
            DynamXMain.log.debug("Add : " + res + " = " + index);
            syncVarRegistry.put(res, index);
            serializerMap.put(index, findSerializer(res));
        }
    }

    @SneakyThrows
    public static void addVarsOf(PhysicsEntitySynchronizer<?> synchronizer, Object instance) {
        Class<?> clazz = instance.getClass();
        while (clazz != null) {
            if (baseSyncVarRegistry.containsKey(clazz)) {
                for (String variable : baseSyncVarRegistry.get(clazz)) {
                    Field f = fieldMap.get(variable);
                    f.setAccessible(true);
                    EntityVariable<?> v = (EntityVariable<?>) f.get(instance);
                    f.setAccessible(false);
                    v.init(variable, findSerializer(variable));
                    if (syncVarRegistry.containsKey(variable))
                        synchronizer.registerVariable(syncVarRegistry.get(variable), v);
                    else
                        DynamXMain.log.error("SynchronizedVariable " + variable + " not registered !");
                }
            }
            clazz = clazz.getSuperclass();
        }
    }
}
