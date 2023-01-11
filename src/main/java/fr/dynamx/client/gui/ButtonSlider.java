package fr.dynamx.client.gui;

import fr.dynamx.client.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.MathHelper;

import static fr.aym.acsguis.api.GuiAPIClientHelper.mc;

public class ButtonSlider extends GuiButton {
    private final String categoryName;
    public float volume = 1.0F;
    public boolean pressed;
    private final String offDisplayString = I18n.format("options.off");

    public ButtonSlider(int buttonId, int x, int y, boolean master, String ButtonText) {
        super(buttonId, x, y, master ? 310 : 150, 20, "");
        this.categoryName = ButtonText;
        this.volume = ClientProxy.SOUND_HANDLER.getMasterVolume();
        this.displayString = this.categoryName + ": " + getDisplayString();
    }

    @Override
    protected int getHoverState(boolean mouseOver) {
        return 0;
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            if (pressed) {
                this.volume = (float) (mouseX - (this.x + 4)) / (float) (this.width - 8);
                this.volume = MathHelper.clamp(this.volume, 0.0F, 1.0F);
                ClientProxy.SOUND_HANDLER.setMasterVolume(this.volume);
                this.displayString = this.categoryName + ": " + getDisplayString();
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexturedModalRect(this.x + (int) (this.volume * (float) (this.width - 8)), this.y, 0, 66, 4, 20);
            this.drawTexturedModalRect(this.x + (int) (this.volume * (float) (this.width - 8)) + 4, this.y, 196, 66, 4, 20);
        }
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            this.volume = (float) (mouseX - (this.x + 4)) / (float) (this.width - 8);
            this.volume = MathHelper.clamp(this.volume, 0.0F, 1.0F);
            ClientProxy.SOUND_HANDLER.setMasterVolume(this.volume);
            this.displayString = this.categoryName + ": " + getDisplayString();
            this.pressed = true;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void playPressSound(SoundHandler soundHandlerIn) {
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        if (this.pressed) {
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }

        this.pressed = false;
    }

    protected String getDisplayString() {
        return volume == 0.0F ? this.offDisplayString : (int) (volume * 100.0F) + "%";
    }

}