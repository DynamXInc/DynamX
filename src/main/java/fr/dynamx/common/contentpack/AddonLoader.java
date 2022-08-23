package fr.dynamx.common.contentpack;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.DynamXAddon;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.asm.ModAnnotation;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static fr.dynamx.common.DynamXMain.log;

/**
 * DynamX addons loader
 *
 * @see DynamXAddon
 */
public class AddonLoader {
    /**
     * Loaded addons
     */
    private static final Map<String, AddonInfo> addons = new HashMap<>();

    protected static void discoverAddons(FMLConstructionEvent event) {
        Set<ASMDataTable.ASMData> modData = event.getASMHarvestedData().getAll(DynamXAddon.class.getName());
        for (ASMDataTable.ASMData data : modData) {
            if (canRunOn(data.getAnnotationInfo().get("sides"), event.getSide())) {
                String name = data.getClassName();
                try {
                    Class<?> addon = Class.forName(data.getClassName());
                    DynamXAddon an = addon.getAnnotation(DynamXAddon.class);
                    name = an.modid() + ":" + an.name();
                    log.debug("Found addon candidate " + an.name() + " of mod " + an.modid());
                    boolean found = false;
                    for (Method md : addon.getDeclaredMethods()) {
                        if (md.isAnnotationPresent(DynamXAddon.AddonEventSubscriber.class)) {
                            if (!Modifier.isStatic(md.getModifiers()))
                                throw new IllegalArgumentException("Addon's @AddonEventSubscriber init method must have static access !");
                            if (md.getParameterCount() != 0)
                                throw new IllegalArgumentException("Addon's @AddonEventSubscriber init method must have 0 parameters !");
                            getAddons().put(an.modid(), new AddonInfo(an.modid(), an.name(), an.version(), md, an.requiredOnClient()));
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        throw new IllegalArgumentException("Addon class " + name + " (" + data.getClassName() + ") with not @AddonEventSubscriber init method");
                } catch (Exception e) {
                    //log.error("Addon " + name + " cannot be loaded !", e);
                    DynamXErrorManager.addError("DynamX initialization", "addon_load_error", ErrorLevel.FATAL, name, "Addon class: " + data.getClassName(), e, 900);
                }
            }
        }
    }

    /**
     * Checks the sides where the addon is allowed to run
     *
     * @param addonSides annotation data
     */
    private static boolean canRunOn(Object addonSides, Side current) {
        if (addonSides == null)
            return true; //default behavior
        for (ModAnnotation.EnumHolder s : (Iterable<ModAnnotation.EnumHolder>) addonSides) {
            if (s.getValue().equalsIgnoreCase(current.name()))
                return true;
        }
        return false;
    }

    /**
     * Initializes all addons (discovered in init method)
     */
    public static void initAddons() {
        ProgressManager.ProgressBar bar = ProgressManager.push("Loading DynamX addons", 1);
        bar.step("Initialize addons");
        for (AddonInfo addon : getAddons().values()) {
            try {
                Optional<ModContainer> container = Loader.instance().getActiveModList().stream().filter(p -> p.getModId().equals(addon.getModId())).findFirst();
                ModContainer current = Loader.instance().activeModContainer();
                container.ifPresent(modContainer -> Loader.instance().setActiveModContainer(modContainer));
                addon.initAddon();
                container.ifPresent(modContainer -> Loader.instance().setActiveModContainer(current));
            } catch (Exception e) {
                log.error("Addon " + addon.toString() + " cannot be initialized !", e);
                DynamXErrorManager.addError("DynamX initialization", "addon_init_error", ErrorLevel.FATAL, addon.getAddonName(), null, e);
            }
        }
        log.info("Loaded addons are " + getAddons().values());
        ProgressManager.pop(bar);
    }

    /**
     * @param addon The addon id as specified in its annotation (may also be the modid of the addon)
     * @return True if the addon is loaded
     */
    public static boolean isAddonLoaded(String addon) {
        return getAddons().containsKey(addon);
    }

    /**
     * @return Loaded addons
     */
    public static Map<String, AddonInfo> getAddons() {
        return addons;
    }
}
