package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeEntry;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Registry for ModularVehicles sub info categories, such as shapes, wheels, seats, trailer attach or steering wheel <br>
 * You can create your custom registries
 *
 * @see SubInfoTypeEntry
 * @see ISubInfoTypeOwner
 * @see fr.dynamx.api.contentpack.object.subinfo.ISubInfoType
 */
public class SubInfoTypesRegistry<T extends ISubInfoTypeOwner<?>> {
    private final Map<String, SubInfoTypeEntry<T>> ENTRIES = new LinkedHashMap<>();
    private final Map<Class<? extends INamedObject>, IPackFilePropertyFixer> PROPERTY_FIXERS = new LinkedHashMap<>();

    /**
     * Protected : use {@link RegisteredSubInfoType} annotation
     */
    protected void addSubInfoType(SubInfoTypeEntry<T> entry) {
        if (ENTRIES.containsKey(entry.getKey()))
            throw new IllegalArgumentException("Sub info type entry with name " + entry.getKey() + " is already registered !");
        ENTRIES.put(entry.getKey(), entry);
    }

    /**
     * @return A collection of all registered sub info types
     */
    public Collection<SubInfoTypeEntry<T>> getRegisteredEntries() {
        return ENTRIES.values();
    }

    protected void addSubInfoTypePropertiesFixer(Class<? extends INamedObject> subInfoTypeClass, IPackFilePropertyFixer fixer) {
        if (PROPERTY_FIXERS.containsKey(subInfoTypeClass))
            throw new IllegalArgumentException("Property fixer for " + subInfoTypeClass + " is already registered !");
        PROPERTY_FIXERS.put(subInfoTypeClass, fixer);
    }

    @Nullable
    public IPackFilePropertyFixer getSubInfoTypePropertiesFixer(Class<? extends INamedObject> subInfoTypeClass) {
        return PROPERTY_FIXERS.get(subInfoTypeClass);
    }

    public static void discoverSubInfoTypes(FMLConstructionEvent event) {
        Set<ASMDataTable.ASMData> modData = event.getASMHarvestedData().getAll(RegisteredSubInfoType.class.getName());
        List<Class<?>> exploredClasses = new ArrayList<>();
        for (ASMDataTable.ASMData data : modData) {
            String name = data.getClassName();
            try {
                Class<?> object = Class.forName(data.getClassName());
                if (!object.isAnnotationPresent(RegisteredSubInfoType.class) || exploredClasses.contains(object))
                    continue;
                exploredClasses.add(object);
                if (!ISubInfoType.class.isAssignableFrom(object))
                    throw new IllegalArgumentException("Only ISubInfoType objects can have the RegisteredSubInfoType annotation. Errored class: " + object);

                RegisteredSubInfoType an = object.getAnnotation(RegisteredSubInfoType.class);
                Class<? extends ISubInfoTypeOwner<?>> subInfoTypeClass = null;
                if (an.registries().length >= 1)
                    subInfoTypeClass = an.registries()[0].getInfoOwnerType();
                //Find the right constructor
                Constructor<?> constructor = null;
                for (Constructor<?> cons : object.getDeclaredConstructors()) {
                    if (subInfoTypeClass != null && (Arrays.equals(cons.getParameterTypes(), new Class[]{subInfoTypeClass, String.class}) || Arrays.equals(cons.getParameterTypes(), new Class[]{subInfoTypeClass}))) {
                        constructor = cons;
                        break;
                    }
                    if (Arrays.equals(cons.getParameterTypes(), new Class[]{ISubInfoTypeOwner.class, String.class}) || Arrays.equals(cons.getParameterTypes(), new Class[]{ISubInfoTypeOwner.class})) {
                        constructor = cons;
                        break;
                    }
                }
                if (constructor == null) {
                    throw new NoSuchMethodException("@RegisteredSubInfoType class must have a constructor with parameters (ISubInfoTypeOwner, String) or (ISubInfoTypeOwner)");
                }
                //And register it
                Constructor<?> finalConstructor = constructor;
                for (SubInfoTypeRegistries registry : an.registries()) {
                    if (!registry.getInfoLoader().hasSubInfoTypesRegistry())
                        throw new IllegalArgumentException("No sub info type registry on registry " + registry);
                    registry.getInfoLoader().getSubInfoTypesRegistry().addSubInfoType(new SubInfoTypeEntry<>(an.name(), (obj, objName) -> {
                        try {
                            return (ISubInfoType) (finalConstructor.getParameterTypes().length == 1 ? finalConstructor.newInstance(obj) : finalConstructor.newInstance(obj, objName));
                        } catch (InstantiationException | IllegalAccessException |
                                 InvocationTargetException e) {
                            throw new RuntimeException("Error with " + name, e);
                        }
                    }, an.strictName()));
                }
            } catch (Exception e) {
                //log.error("Cannot load @RegisteredSubInfoType annotation in class " + name + " !", e);
                DynamXErrorManager.addError("DynamX initialization", DynamXErrorManager.INIT_ERRORS, "addon_error", ErrorLevel.FATAL, name, "Cannot load @RegisteredSubInfoType annotation in class " + data.getClassName(), e, 900);
            }
        }
    }
}
