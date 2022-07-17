package fr.dynamx.api.entities.modules;

import java.util.List;

/**
 * Helper class to build a {@link IPhysicsModule} list
 */
public class ModuleListBuilder {
    private final List<IPhysicsModule<?>> list;

    /**
     * The modules will be added to the given list <br>
     * It may already contain modules
     *
     * @param moduleList The module list
     */
    public ModuleListBuilder(List<IPhysicsModule<?>> moduleList) {
        this.list = moduleList;
    }

    /**
     * Adds a module to the list
     *
     * @param module The module
     */
    public void add(IPhysicsModule<?> module) {
        list.add(module);
    }

    /**
     * @param clazz The module class
     * @return True if there is already a module of this type
     */
    public boolean hasModuleOfClass(Class<? extends IPhysicsModule<?>> clazz) {
        return getByClass(clazz) != null;
    }

    /**
     * @param clazz The module class
     * @param <Y>   The module type
     * @return The module of the given type, or null
     */
    public <Y extends IPhysicsModule<?>> Y getByClass(Class<Y> clazz) {
        return (Y) list.stream().filter(m -> m.getClass() == clazz).findFirst().orElse(null);
    }

    /**
     * @return The current modules list
     */
    public List<IPhysicsModule<?>> getList() {
        return list;
    }
}
