package fr.dynamx.client.handlers.hud;

import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.style.AutoStyleHandler;
import fr.aym.acsguis.component.style.ComponentStyleManager;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.aym.acsguis.component.textarea.UpdatableGuiLabel;
import fr.aym.acsguis.cssengine.selectors.EnumSelectorContext;
import fr.aym.acsguis.cssengine.style.EnumCssStyleProperties;
import fr.aym.acsguis.utils.GuiConstants;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.camera.CameraSystem;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.client.handlers.KeyHandler;
import fr.dynamx.common.contentpack.type.vehicle.EngineInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.HelicopterEngineModule;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.common.physics.entities.modules.HelicopterEnginePhysicsHandler;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = DynamXConstants.ID, value = Side.CLIENT)
public class HelicopterController implements IVehicleController {
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/vehicle_hud.css");
    private static final Minecraft MC = Minecraft.getMinecraft();

    protected final BaseVehicleEntity<?> entity;
    protected final HelicopterEngineModule engine;

    public boolean accelerating, handbraking, reversing;
    public boolean turningLeft, turningRight, isEngineStarted;
    public byte onCooldown;

    /**
     * @param entity is assumed to implement {@link IModuleContainer.ISeatsContainer}
     */
    public HelicopterController(BaseVehicleEntity<?> entity, HelicopterEngineModule engine) {
        this.entity = entity;
        this.engine = engine;

        isEngineStarted = engine.isEngineStarted();
        handbraking = engine.isHandBraking();

        CameraSystem.setCameraZoom(entity.getPackInfo().getDefaultZoomLevel());

        while (CarController.car_brake.isPressed()) ;
        while (CarController.speedLimiter.isPressed()) ;
        while (CarController.car_engineOn.isPressed()) ;
        while (CarController.toggleLockDoor.isPressed()) ;
    }

    @SubscribeEvent
    public static void tickMouse(MouseEvent event) {
        if (MC.player.getRidingEntity() instanceof HelicopterEntity) {
            //System.out.println("Dx is " + event.getDx() + " Dy is " + event.getDy());
            HelicopterEnginePhysicsHandler.AngleBack = event.getDx();
            HelicopterEnginePhysicsHandler.AngleFront = event.getDy();

        }
    }


    @Override
    public void update() {
        if (((IModuleContainer.ISeatsContainer) entity).getSeats().isLocalPlayerDriving() && engine.getEngineProperties() != null) {
            if (KeyHandler.KEY_POWERUP.isPressed()) {
                if (onCooldown == 0) {
                    HelicopterEngineModule engine = entity.getModuleByType(HelicopterEngineModule.class);
                    if (engine != null) {
                        engine.setPower(engine.getPower() + 0.05f);
                    }
                    onCooldown = 5;
                }
            }
            if (KeyHandler.KEY_POWERDOWN.isPressed()) {
                if (onCooldown == 0) {
                    HelicopterEngineModule engine = entity.getModuleByType(HelicopterEngineModule.class);
                    if (engine != null) {
                        engine.setPower(engine.getPower() - 0.05f);
                        System.out.println("down");
                    }
                    onCooldown = 5;
                }
            }
            if (accelerating != MC.gameSettings.keyBindForward.isKeyDown()) {
                accelerating = MC.gameSettings.keyBindForward.isKeyDown();
            }
            if (reversing != MC.gameSettings.keyBindBack.isKeyDown()) {
                reversing = MC.gameSettings.keyBindBack.isKeyDown();
            }
            if (engine.getEngineProperties()[VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR.ordinal()] == 0) //point mort
            {
                if (CarController.car_brake.isPressed()) {
                    handbraking = !handbraking;
                }
            } else if (handbraking != CarController.car_brake.isKeyDown()) {
                handbraking = CarController.car_brake.isKeyDown();
            }
            if (turningLeft != MC.gameSettings.keyBindLeft.isKeyDown() || turningRight != MC.gameSettings.keyBindRight.isKeyDown()) {
                turningLeft = MC.gameSettings.keyBindLeft.isKeyDown();
                turningRight = MC.gameSettings.keyBindRight.isKeyDown();
            }
            if (onCooldown > 0)
                onCooldown--;
            if (CarController.car_engineOn.isPressed()) {
                if (onCooldown == 0) {
                    isEngineStarted = !isEngineStarted;
                    onCooldown = 40;
                }
            }
            /* todo update this if (CarController.toggleLockDoor.isPressed()) {
                if (onCooldown == 0) {
                    if (entity instanceof IModuleContainer.IDoorContainer && ((IModuleContainer.IDoorContainer) entity).getDoors() != null) {
                        PartSeat seat = ((IModuleContainer.ISeatsContainer) entity).getSeats().getRidingSeat(MC.player);
                        DoorsModule doors = ((IModuleContainer.IDoorContainer) entity).getDoors();
                        DynamXContext.getNetwork().sendToServer(new MessageOpenDoor(entity, !doors.isDoorOpened(seat.getLinkedPartDoor(entity))));
                    }
                    onCooldown = 30;
                }
            }*/

            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.VehicleControllerUpdateEvent<>(entity, this));
            int controls = 0;
            if (accelerating)
                controls = controls | 1;
            if (handbraking)
                controls = controls | 2;
            if (reversing)
                controls = controls | 4;
            if (turningLeft)
                controls = controls | 8;
            if (turningRight)
                controls = controls | 16;
            if (isEngineStarted)
                controls = controls | 32;
            engine.setControls(controls);
        }
    }

    //HUD

    @Override
    @SideOnly(Side.CLIENT)
    public GuiComponent<?> createHud() {
        GuiPanel panel = new GuiPanel();
        float maxRpm = entity.getPackInfo().getSubPropertyByType(EngineInfo.class).getMaxRevs() + 3000; // todo CONFIGURABLE
        float scale = 90f / 300;
        GuiPanel speed = new GuiPanel() {
            private float prevRpm, rpm;

            @Override
            public void tick() {
                super.tick();
                prevRpm = rpm;
                //Don't use modified maxRpm here
                rpm = engine.getEngineProperty(VehicleEntityProperties.EnumEngineProperties.REVS) * entity.getPackInfo().getSubPropertyByType(EngineInfo.class).getMaxRevs();
            }

            @Override
            public void drawBackground(int mouseX, int mouseY, float partialTicks) {
                super.drawBackground(mouseX, mouseY, partialTicks);

                GL11.glDisable(GL11.GL_SCISSOR_TEST);

                ResourceLocation loc = new ResourceLocation(DynamXConstants.ID, "textures/waw.png");
                Minecraft.getMinecraft().getTextureManager().bindTexture(loc);

                GlStateManager.pushMatrix();
                GlStateManager.translate(getScreenX(), getScreenY(), 0);

                GlStateManager.scale(scale, scale, 1);

                GlStateManager.enableTexture2D();
                GL11.glColor4f(1, 1, 1, 1);

                float curRpm = prevRpm + (this.rpm - this.prevRpm) * partialTicks;
                float tierMaxRpm = maxRpm / 3;

                float y = (curRpm * 300) / tierMaxRpm;
                if (curRpm >= tierMaxRpm) {
                    y = 300;
                }

                float f1 = 0.00390625F;
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferbuilder = tessellator.getBuffer();
                bufferbuilder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_TEX);
                bufferbuilder.pos(150, 150, 0).tex(0.5, 0.5).endVertex();
                bufferbuilder.pos(0, 300 - y, 0).tex(0, (300 - y) / 300).endVertex();
                bufferbuilder.pos(0, 300, 0).tex(0, 300f / 300).endVertex();
                tessellator.draw();

                if (curRpm >= tierMaxRpm) {
                    float x = ((curRpm - tierMaxRpm) * 300) / tierMaxRpm;
                    if (curRpm >= tierMaxRpm * 2) {
                        x = 300;
                    }

                    bufferbuilder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_TEX);
                    bufferbuilder.pos(150, 150, 0).tex(0.5, 0.5).endVertex();
                    bufferbuilder.pos(x, 0, 0).tex(x / 300, 0).endVertex();
                    bufferbuilder.pos(0, 0, 0).tex(0, 0).endVertex();
                    tessellator.draw();
                }

                if (curRpm >= tierMaxRpm * 2) {
                    y = ((curRpm - tierMaxRpm * 2) * 300) / tierMaxRpm;
                    if (curRpm >= maxRpm) {
                        y = 300;
                    }

                    bufferbuilder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_TEX);
                    bufferbuilder.pos(150, 150, 0).tex(0.5, 0.5).endVertex();
                    bufferbuilder.pos(300, y, 0).tex(1, y / 300).endVertex();
                    bufferbuilder.pos(300, 0, 0).tex(1, 0).endVertex();
                    tessellator.draw();
                }

                GlStateManager.popMatrix();
            }
        };
        speed.setCssId("speed_pane");
        float[] engineProperties = engine.getEngineProperties();
        speed.add(new UpdatableGuiLabel("%s", s -> String.format(s, engine.isEngineStarted() ? (int) engineProperties[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()] : "--", "")).setCssId("engine_speed"));
        // speed.add(new UpdatableGuiLabel("%d", s -> String.format(s, (int) (engineProperties[VehicleEntityProperties.EnumEngineProperties.REVS.ordinal()] * entity.getPackInfo().getSubPropertyByType(EngineInfo.class).getMaxRevs()), "")).setCssId("engine_rpm"));
        panel.add(speed);

        speed.add(new UpdatableGuiLabel("power %f", s -> String.format(s, engine.getPower())).setCssId("engine_gear"));

        panel.add(new UpdatableGuiLabel("                             AngleFront %f", s -> String.format(s, HelicopterEnginePhysicsHandler.AngleFront)).setCssId("engine_gear"));

        //Debug
        String cclass = ClientDebugSystem.enableDebugDrawing ? "hud_label_debug" : "hud_label_hidden";
        panel.add(new UpdatableGuiLabel("Handbrake : %s", s -> String.format(s, (engine.isHandBraking() ? "§cON" : "§aOFF"))).setCssId("handbrake_state").setCssClass(cclass));
        panel.add(new UpdatableGuiLabel("Sounds : %s", s -> String.format(s, (engine.getCurrentEngineSound() == null ? "none" : engine.getCurrentEngineSound().getSoundName()))).setCssId("engine_sounds").setCssClass(cclass));

        panel.setCssId("engine_hud");



        int nmL = (int) (maxRpm / 1000);

        for (int i = 1; i <= nmL; i++) {
            double angle = ((i * 3f / 2 * Math.PI) / nmL) - Math.PI / 3;

            float r = (150 - 29) * scale;

            double halfLetter = 6 * scale;

            double x = (45 - Math.cos(angle) * r) - Math.abs(halfLetter * Math.cos(angle));
            double y = (45 - Math.sin(angle) * r) - Math.abs(halfLetter * Math.sin(angle)) - 2;

            float power = engine.getPower();

            speed.add(new GuiLabel("v: " + i+" p: "+power).setCssClass("rpm_letter").getStyle().addAutoStyleHandler(new AutoStyleHandler<ComponentStyleManager>() {
                @Override
                public boolean handleProperty(EnumCssStyleProperties property, EnumSelectorContext context, ComponentStyleManager target) {
                    if (property == EnumCssStyleProperties.LEFT) {
                        target.getXPos().setAbsolute(-(float) x, GuiConstants.ENUM_RELATIVE_POS.END);
                        return true;
                    }
                    if (property == EnumCssStyleProperties.TOP) {
                        target.getYPos().setAbsolute((float) y);
                        return true;
                    }
                    if (property == EnumCssStyleProperties.COLOR) {
                        if (angle > Math.PI - Math.PI / 3) {
                            target.setForegroundColor(0xFFE23F3F);
                        }
                        return true;
                    }
                    return false;
                }

                @Override
                public Collection<EnumCssStyleProperties> getModifiedProperties(ComponentStyleManager target) {
                    return Arrays.asList(EnumCssStyleProperties.LEFT, EnumCssStyleProperties.TOP, EnumCssStyleProperties.COLOR);
                }
            }).getOwner());
        }

        return panel;
    }

    @Override
    public List<ResourceLocation> getHudCssStyles() {
        return Collections.singletonList(STYLE);
    }

    protected String getGearString(int gear) {
        return gear == -1 ? "R" : gear == 0 ? (engine.isHandBraking() ? TextFormatting.RED + "P" : "N") : "" + gear;
    }
}
