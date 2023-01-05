package fr.dynamx.api.network.sync;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.sync.PhysicsEntitySynchronizer;
import fr.dynamx.common.network.sync.variables.EntityTransformsVariable;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Predicate;

public class SynchronizedEntityVariableRegistry {
    private static final Map<Class<?>, List<SynchronizedEntityVariable>> baseSyncVarRegistry = new HashMap<>();
    private static final Map<Class<?>, String> classToMod = new HashMap<>();
    private static final Map<SynchronizedEntityVariable, Field> fieldMap = HashBiMap.create();
    private static final BiMap<SynchronizedEntityVariable, Integer> syncVarRegistry = HashBiMap.create();
    @Getter
    private static final Map<Integer, EntityVariableSerializer<?>> serializerMap = new HashMap<>();

    public static void discoverSyncVars(FMLConstructionEvent event) {
        Set<ASMDataTable.ASMData> modData = event.getASMHarvestedData().getAll(SynchronizedEntityVariable.SynchronizedPhysicsModule.class.getName());
        for (ASMDataTable.ASMData data : modData) {
            String name = data.getClassName();
            try {
                Class<?> classToParse = Class.forName(data.getClassName());
                classToMod.put(classToParse, data.getCandidate().getContainedMods().get(0).getModId());
                for (Field f : classToParse.getDeclaredFields()) {
                    //System.out.println("Test " + f.getName() + " " + EntityVariable.class.isAssignableFrom(f.getType()) + " " + f.getType().isAssignableFrom(EntityTransformsVariable.class) + " " + f.isAnnotationPresent(SynchronizedEntityVariable.class));
                    if (EntityVariable.class.isAssignableFrom(f.getType()) && f.isAnnotationPresent(SynchronizedEntityVariable.class)) {
                        //System.out.println("Detected in " + classToParse);
                        SynchronizedEntityVariable property = f.getAnnotation(SynchronizedEntityVariable.class);
                        if (!baseSyncVarRegistry.containsKey(classToParse))
                            baseSyncVarRegistry.put(classToParse, new ArrayList<>());
                        baseSyncVarRegistry.get(classToParse).add(property);
                        fieldMap.put(property, f);
                    }
                }
                if(baseSyncVarRegistry.containsKey(classToParse))
                    DynamXMain.log.debug("Detected " + baseSyncVarRegistry.get(classToParse).size()+" synchronized entity variables in " + classToParse);
                else
                    DynamXMain.log.warn("Failed to detect any synchronized entity variable in " + classToParse);
            } catch (Exception e) {
                throw new RuntimeException("Failed to detect synchronized entity variables in " + name);
            }
        }
    }

    private static int getIndex(SynchronizedEntityVariable of, List<SynchronizedEntityVariable> in) {
        for (int i = 0; i < in.size(); i++) {
            if (in.get(i).equals(of)) {
                return i;
            }
        }
        throw new IllegalArgumentException(of + " is not in input list " + in);
    }

    private static EntityVariableSerializer<?> findSerializer(SynchronizedEntityVariable res) { //TODO CLEAN & ADD DEBUG LOGS
        Field f = fieldMap.get(res);
        /*System.out.println(f.getName());
        System.out.println(f.getGenericType());
        System.out.println(f.getType().getGenericSuperclass());*/
        ParameterizedType type = null;
        if (f.getGenericType() instanceof ParameterizedType) {
            type = (ParameterizedType) f.getGenericType();
        } else if (f.getType().getGenericSuperclass() instanceof ParameterizedType) {
            type = (ParameterizedType) f.getType().getGenericSuperclass();
        } else {
            throw new IllegalArgumentException("Bad entity variable " + f + " name : " + res.name());
        }
        EntityVariableSerializer<?> serializer = EntityVariableTypes.getSerializerRegistry().get(type.getActualTypeArguments()[0]);
        if (serializer == null && type.getActualTypeArguments()[0] instanceof ParameterizedType) {
            serializer = EntityVariableTypes.getSerializerRegistry().get(((ParameterizedType) type.getActualTypeArguments()[0]).getRawType());
            System.out.println("Second try " + serializer + " " + ((ParameterizedType) type.getActualTypeArguments()[0]).getRawType());
        }
        if (serializer == null) {
            //System.out.println(EntityVariableTypes.getSerializerRegistry());
            throw new IllegalArgumentException("Bad entity variable " + f + " " + type.getActualTypeArguments()[0] + " name : " + res.name());
        }
        return serializer;
    }

    /**
     * Sorts variable ids in alphabetical order
     */
    public static void sortRegistry(Predicate<String> useMod) {
        DynamXMain.log.info("Sorting SynchronizedVariables registry ids...");
        List<SynchronizedEntityVariable> buff = new ArrayList<>();
        for (Class<?> res : baseSyncVarRegistry.keySet()) {
            if (useMod.test(classToMod.get(res))) {
                buff.addAll(baseSyncVarRegistry.get(res));
            }
        }
        buff.sort(Comparator.comparing(SynchronizedEntityVariable::name)); //Unique sorting
        DynamXMain.log.debug("Fixing SynchronizedVariables registry ids...");
        syncVarRegistry.clear();
        serializerMap.clear();
        for (SynchronizedEntityVariable res : buff) {
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
                for (SynchronizedEntityVariable variable : baseSyncVarRegistry.get(clazz)) {
                    Field f = fieldMap.get(variable);
                    f.setAccessible(true);
                    //System.out.println(instance + " plus " + variable + " " + variable.name() + " " + variable.hashCode() + " " + f.getName() + " " + f.getDeclaringClass() + " " + f + " " + clazz);
                    EntityVariable<?> v = (EntityVariable<?>) f.get(instance);
                    f.setAccessible(false);
                    v.init(variable.name(), findSerializer(variable));
                    synchronizer.registerVariable(syncVarRegistry.get(variable), v);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }
}
