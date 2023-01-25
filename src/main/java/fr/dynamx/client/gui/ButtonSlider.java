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
    private float volume = 1.0F;
    private boolean pressed;
    private final String offDisplayString = I18n.format("options.off");

    public ButtonSlider(int buttonId, int x, int y, boolean isMaster, String buttonText) {
        super(buttonId, x, y, isMaster ? 310 : 150, 20, "");
        categoryName = buttonText;
        volume = ClientProxy.SOUND_HANDLER.getMasterVolume();
        displayString = categoryName + ": " + getDisplayString();
    }

    @Override
    protected int getHoverState(boolean mouseOver) {
        return 0;
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            if (pressed) {
                volume = (float) (mouseX - (x + 4)) / (float) (width - 8);
                volume = MathHelper.clamp(volume, 0.0F, 1.0F);
                ClientProxy.SOUND_HANDLER.setMasterVolume(volume);
                displayString = categoryName + ": " + getDisplayString();
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexturedModalRect(x + (int) (volume * (float) (width - 8)), y, 0, 66, 4, 20);
            drawTexturedModalRect(x + (int) (volume * (float) (width - 8)) + 4, y, 196, 66, 4, 20);
        }
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            volume = (float) (mouseX - (x + 4)) / (float) (width - 8);
            volume = MathHelper.clamp(volume, 0.0F, 1.0F);
            ClientProxy.SOUND_HANDLER.setMasterVolume(volume);
            displayString = categoryName + ": " + getDisplayString();
            pressed = true;
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
        if (pressed) {
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        pressed = false;
    }

    protected String getDisplayString() {
        return volume == 0.0F ? offDisplayString : (int) (volume * 100.0F) + "%";
    }
}