package fr.dynamx.api.entities.modules;

import fr.aym.acsguis.component.GuiComponent;
import fr.dynamx.core.common.entities.modules.engines.CarEngineModule;
import fr.hermes.api.mc.utils.HmResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Reads inputs of vehicle controls from the user and add elements to the HUD while driving <br>
 * Used on client side
 *
 * @see IPhysicsModule
 * @see CarEngineModule
 */
public interface IVehicleController {
    /**
     * Called each tick to read controls
     */
    void update();

    /**
     * Called to create the vehicle HUD when the driver mounts, on client side <br>
     * Nullable : display nothing for this controller
     */
    @Nullable
    GuiComponent createHud();

    /**
     * Called to get the vehicle HUD style when the driver mounts, on client side <br>
     * Nullable : display nothing for this controller
     */
    @Nullable
    List<HmResourceLocation> getHudCssStyles();
}
