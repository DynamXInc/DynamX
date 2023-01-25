package fr.dynamx.client.handlers.hud;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.handlers.KeyHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.PartSeat;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.BasicEngineModule;
import fr.dynamx.common.entities.modules.DoorsModule;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

public abstract class BaseController implements IVehicleController {
    protected static final Minecraft MC = Minecraft.getMinecraft();

    protected final BaseVehicleEntity<?> entity;

    @Getter
    @Setter
    protected boolean accelerating, reversing;
    @Getter
    @Setter
    protected boolean handbraking;
    @Getter
    @Setter
    protected boolean turningLeft, turningRight, isEngineStarted;
    @Getter
    @Setter
    protected byte onCooldown;

    /**
     * @param entity is assumed to implement {@link IModuleContainer.ISeatsContainer}
     */
    public BaseController(BaseVehicleEntity<?> entity, BasicEngineModule engine) {
        this.entity = entity;
        isEngineStarted = engine.isEngineStarted();
        handbraking = engine.isHandBraking();
        while (KeyHandler.KEY_HANDBRAKE.isPressed()) ;
        while (KeyHandler.KEY_SPEED_LIMITIER.isPressed()) ;
        while (KeyHandler.KEY_ENGINE_ON.isPressed()) ;
        while (KeyHandler.KEY_LOCK_DOOR.isPressed()) ;
    }

    protected abstract void updateControls();

    @Override
    public void update() {
        if (((IModuleContainer.ISeatsContainer) entity).getSeats().isLocalPlayerDriving()) {
            accelerating = MC.gameSettings.keyBindForward.isKeyDown();
            reversing = MC.gameSettings.keyBindBack.isKeyDown();
            turningLeft = MC.gameSettings.keyBindLeft.isKeyDown();
            turningRight = MC.gameSettings.keyBindRight.isKeyDown();
            if (KeyHandler.KEY_HANDBRAKE.isPressed())
                handbraking = !handbraking;
            if (onCooldown > 0)
                onCooldown--;
            if (KeyHandler.KEY_ENGINE_ON.isPressed()) {
                if (onCooldown == 0) {
                    isEngineStarted = !isEngineStarted;
                    onCooldown = 40;
                }
            }
            updateControls();
        }
    }
}
