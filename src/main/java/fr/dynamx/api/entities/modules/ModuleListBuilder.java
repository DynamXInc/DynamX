package fr.dynamx.api.entities.modules;

import lombok.Getter;

import java.util.List;

/**
 * Helper class to build a {@link IBaseModule} list
 */
@Getter
public class ModuleListBuilder {
    /**
     * -- GETTER --
     *
     * @return The current modules list
     */
    private final List<IBaseModule> moduleList;

    /**
     * The modules will be added to the given list <br>
     * It may already contain modules
     *
     * @param moduleList The module list
     */
    public ModuleListBuilder(List<? extends IBaseModule> moduleList) {
        this.moduleList = (List<IBaseModule>) moduleList;
    }

    /**
     * Adds a module to the list
     *
     * @param module The module
     */
    public void add(IBaseModule module) {
        moduleList.add(module);
    }

    /**
     * @param clazz The module class
     * @return True if there is already a module of this type
     */
    public boolean hasModuleOfClass(Class<? extends IBaseModule> clazz) {
        return moduleList.stream().anyMatch(m -> clazz.isAssignableFrom(m.getClass()));
    }

    /**
     * @param clazz The module class
     * @param <Y>   The module type
     * @return The module of the given type, or null
     */
    public <Y extends IBaseModule> Y getByClass(Class<Y> clazz) {
        return (Y) moduleList.stream().filter(m -> clazz.isAssignableFrom(m.getClass())).findFirst().orElse(null);
    }
}
