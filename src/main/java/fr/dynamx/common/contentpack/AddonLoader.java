package fr.dynamx.common.contentpack;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.aym.acslib.api.services.mps.ModProtectionContainer;
import fr.dynamx.api.contentpack.DynamXAddon;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
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
     * -- GETTER --
     *
     * @return Loaded addons
     */
    @Getter
    private static final Map<String, AddonInfo> addons = new HashMap<>();

    /**
     * MPS event subscribers
     */
    private static final Map<String, Method> mpsInitSubscribers = new HashMap<>();

    protected static void discoverAddons(FMLConstructionEvent event) {
        Set<ASMDataTable.ASMData> modData = event.getASMHarvestedData().getAll(DynamXAddon.class.getName());
        for (ASMDataTable.ASMData data : modData) {
            if (canRunOn(data.getAnnotationInfo().get("sides"), event.getSide())) {
                String name = data.getClassName();
                try {
                    Class<?> addon = Class.forName(data.getClassName());
                    DynamXAddon an = addon.getAnnotation(DynamXAddon.class);
                    name = an.modid() + ":" + an.name();
                    log.debug("Found addon candidate {} of mod {}", an.name(), an.modid());
                    boolean found = false;
                    for (Method md : addon.getDeclaredMethods()) {
                        if (md.isAnnotationPresent(DynamXAddon.AddonEventSubscriber.class)) {
                            if (!Modifier.isStatic(md.getModifiers()))
                                throw new IllegalArgumentException("Addon's @AddonEventSubscriber init method must have static access !");
                            if (md.getParameterCount() == 1 && md.getParameterTypes()[0].equals(ModProtectionContainer.class)) {
                                log.debug("Found MPS init subscriber for addon {} of mod {}", an.name(), an.modid());
                                mpsInitSubscribers.put(an.modid(), md);
                                continue;
                            }
                            if (md.getParameterCount() != 0)
                                throw new IllegalArgumentException("Addon's @AddonEventSubscriber init method must have 0 parameters !");
                            getAddons().put(an.modid(), new AddonInfo(an.modid(), an.name(), an.version(), md, an.requiredOnClient()));
                            found = true;
                        }
                    }
                    if (!found)
                        throw new IllegalArgumentException("Addon class " + name + " (" + data.getClassName() + ") with not @AddonEventSubscriber init method");
                } catch (Exception e) {
                    //log.error("Addon " + name + " cannot be loaded !", e);
                    DynamXErrorManager.addError("DynamX initialization", DynamXErrorManager.INIT_ERRORS, "addon_load_error", ErrorLevel.FATAL, name, "Addon class: " + data.getClassName(), e, 900);
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
    public static void initMpsAddons(ModProtectionContainer mpsContainer) {
        for (Map.Entry<String, Method> addon : mpsInitSubscribers.entrySet()) {
            try {
                addon.getValue().invoke(null, mpsContainer);
            } catch (Exception e) {
                log.error("MPS addon {} cannot be initialized !", addon.getKey(), e);
                DynamXErrorManager.addError("DynamX initialization", DynamXErrorManager.INIT_ERRORS, "addon_init_error", ErrorLevel.FATAL, addon.getKey(), "Initializing mps dependencies", e);
            }
        }
        log.info("Loaded MPS addons: {}", mpsInitSubscribers.keySet());
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
                log.error("Addon {} cannot be initialized !", addon.toString(), e);
                DynamXErrorManager.addError("DynamX initialization", DynamXErrorManager.INIT_ERRORS, "addon_init_error", ErrorLevel.FATAL, addon.getAddonName(), "Initializing the addon", e);
            }
        }
        log.info("Loaded addons: {}", getAddons().values());
        ProgressManager.pop(bar);
    }

    /**
     * @param addon The addon id as specified in its annotation (may also be the modid of the addon)
     * @return True if the addon is loaded
     */
    public static boolean isAddonLoaded(String addon) {
        return getAddons().containsKey(addon);
    }
}
