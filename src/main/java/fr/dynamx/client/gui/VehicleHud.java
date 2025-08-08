package fr.dynamx.client.gui;

import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.aym.acsguis.utils.GuiTextureSprite;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.sync.ClientEntityNetHandler;
import fr.dynamx.client.camera.CameraSystem;
import fr.dynamx.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.client.network.ClientPhysicsSyncManager;
import fr.dynamx.common.entities.PackPhysicsEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

public class VehicleHud extends GuiFrame {
    private final PackPhysicsEntity<?, ?> riddenEntity;
    private GuiLabel netWarning;
    private final List<ResourceLocation> styleSheets = new ArrayList<>();

    public VehicleHud(IModuleContainer.ISeatsContainer entity) {
        super(new GuiScaler.Identity());
        this.riddenEntity = entity.cast();
        CameraSystem.setupCamera(entity);
        setCssClass("root");
        List<IVehicleController> controllers = new ArrayList<>(((ClientEntityNetHandler) entity.cast().getSynchronizer()).getControllers());
        if (MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.CreateHud(this, styleSheets, entity.getSeats().isLocalPlayerDriving(), this.riddenEntity, controllers))) {
            return;
        }
        controllers.forEach(c -> {
            List<ResourceLocation> hudStyle = c.getHudCssStyles();
            if (hudStyle != null)
                styleSheets.addAll(hudStyle);
            GuiComponent hud = c.createHud();
            if (hud != null) {
                add(hud);
            }
        });
        if (!(entity.cast().getSynchronizer() instanceof ClientPhysicsEntitySynchronizer)) {
            return;
        }
        netWarning = new GuiLabel("") {
            @Override
            public void drawTexturedBackground(int mouseX, int mouseY, float partialTicks) {
                if (!ClientPhysicsSyncManager.hasBadConnection()) {
                    return;
                }
                int k = 0;
                int l;
                if (ClientPhysicsSyncManager.pingMs > 80) {
                    if (riddenEntity.ticksExisted % (20 * 3) >= (20 * 2)) {
                        l = -1;
                    } else if (ClientPhysicsSyncManager.pingMs < 100) {
                        l = 1;
                    } else if (ClientPhysicsSyncManager.pingMs < 150) {
                        l = 2;
                    } else if (ClientPhysicsSyncManager.pingMs < 200) {
                        l = 3;
                    } else {
                        l = 4;
                    }
                } else {
                    k = 1;
                    l = (int) (Minecraft.getSystemTime() / 100L & 7L);
                    if (l > 4) {
                        l = 8 - l;
                    }
                }
                if (l >= 0) {
                    //GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    mc.getTextureManager().bindTexture(Gui.ICONS);
                    float x = getScreenX();
                    float y = getScreenY() + 5;
                    GuiTextureSprite.drawScaledCustomSizeModalRect(x, y, (float) (k * 10), (float) (176 + l * 8), 10, 8, 10, 8, 256.0F, 256.0F);
                }
            }
        };
        netWarning.setCssId("network_warning");
        netWarning.getStyleCustomizer().setPaddingLeft(14);
        add(netWarning);
    }

    @Override
    public boolean tick() {
        if (!super.tick()) {
            return false;
        }
        if (netWarning != null && ClientPhysicsSyncManager.pingMs > 100 && riddenEntity.ticksExisted % (20 * 3) < (20 * 2))
            netWarning.setText(ClientPhysicsSyncManager.getPingMessage());
        else if (netWarning != null && !netWarning.getText().isEmpty())
            netWarning.setText("");
        return true;
    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return styleSheets;
    }
}
