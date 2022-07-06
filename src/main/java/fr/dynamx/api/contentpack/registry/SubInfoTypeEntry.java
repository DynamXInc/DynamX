package fr.dynamx.api.contentpack.registry;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;

import java.lang.reflect.InvocationTargetException;

/**
 * A sub info category registry entry <br>
 *     Should be registered in the corresponding {@link SubInfoTypesRegistry}
 *
 * @param <T> The class owning the {@link ISubInfoType} of this sub info category
 * @see ISubInfoType
 */
public class SubInfoTypeEntry<T extends ISubInfoTypeOwner<?>>
{
    private final String key;
    private final ISubInfoTypeCreator<T> creator;
    /**
     * If strict, then the name must equals to the name in info file, else it can be contained in it
     */
    private final boolean strict;

    /**
     * Creates a new sub info type registry entry
     *
     * @param key should be the name of this sub info in the info file (or contain if not strict as wheels, shapes and seats)
     * @param clazz the class corresponding to this property, with a constructor having <strong>one parameter of the type of T</strong>
     */
    public SubInfoTypeEntry(String key, Class<? extends ISubInfoType<T>> clazz)
    {
        this(key, clazz, true);
    }

    /**
     * Creates a new sub info type registry entry
     *
     * @param key should be the name of this sub info in the info file (or contain if not strict as wheels, shapes and seats)
     * @param clazz the class corresponding to this property, with a constructor having <strong>one parameter of the type of T</strong>
     * @param strict if strict (default value), it will check if key is equals, else if key is contained in vehicle info field
     */
    public SubInfoTypeEntry(String key, Class<? extends ISubInfoType<T>> clazz, boolean strict)
    {
        this(key, (owner, infoTypeName) -> {
            try {
                return clazz.getConstructor(ISubInfoTypeOwner.class).newInstance(owner);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException("Inaccessible ISubProperty "+clazz+". The constructor must have one parameter of type ISubInfoTypeOwner.", e);
            }
        }, strict);
    }

    /**
     * Creates a new sub info type registry entry
     *
     * @param key should be the name of this sub info in the info file (or contain if not strict as wheels, shapes and seats)
     * @param creator a {@link ISubInfoTypeCreator} creating an instance of the sub info type
     */
    public SubInfoTypeEntry(String key, ISubInfoTypeCreator<T> creator)
    {
        this(key, creator, true);
    }

    /**
     * Creates a new sub info type registry entry
     *
     * @param key should be the name of this sub info in the info file (or contain if not strict as wheels, shapes and seats)
     * @param creator a {@link ISubInfoTypeCreator} creating an instance of the sub info type
     * @param strict if strict (default value), it will check if key is equals, else if key is contained in vehicle info field
     */
    public SubInfoTypeEntry(String key, ISubInfoTypeCreator<T> creator, boolean strict)
    {
        this.key = key.toLowerCase();
        this.creator = creator;
        this.strict = strict;
    }

    /**
     * @return True if with is the key of this sub info type (or contains it if not strict as wheels, shapes and seats)
     */
    public boolean matches(String with) {
        return strict ? key.equalsIgnoreCase(with) : with.toLowerCase().contains(key);
    }

    /**
     * Creates an instance of this sub info
     *
     * @param name The sub info name (useful if sub info matching is not strict)
     */
    public ISubInfoType<T> create(T owner, String name) {
        return creator.apply(owner, name);
    }

    /**
     * @return The key of this entry
     */
    public String getKey() {
        return key;
    }
}
