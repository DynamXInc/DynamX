package fr.dynamx.core.client.handlers.hud;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.core.client.handlers.KeyHandler;
import fr.dynamx.core.common.entities.BaseVehicleEntity;
import fr.dynamx.core.common.entities.modules.engines.BasicEngineModule;
import fr.hermes.api.mc.client.HmMinecraftClient;
import fr.hermes.api.mod.HermesPlatform;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class BaseController implements IVehicleController {
    @SideOnly(Side.CLIENT)
    protected static final HmMinecraftClient MC = HermesPlatform.getInstance().getClient();

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
    @SideOnly(Side.CLIENT)
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
    @SideOnly(Side.CLIENT)
    public void update() {
        if (((IModuleContainer.ISeatsContainer) entity).getSeats().isEntityDriving(MC.hm$getPlayer())) {
            accelerating = MC.hm$getGameSettings().hm$isForwardKeyDown();
            reversing = MC.hm$getGameSettings().hm$isBackKeyDown();
            turningLeft = MC.hm$getGameSettings().hm$isLeftKeyDown();
            turningRight = MC.hm$getGameSettings().hm$isRightKeyDown();
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
