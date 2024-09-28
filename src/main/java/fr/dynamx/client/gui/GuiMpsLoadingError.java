package fr.dynamx.client.gui;

import fr.dynamx.common.DynamXMain;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.fml.client.CustomModLoadingErrorDisplayException;
import net.minecraftforge.fml.client.GuiCustomModLoadingErrorScreen;

import java.io.IOException;

public class GuiMpsLoadingError extends GuiCustomModLoadingErrorScreen {
    private final CustomModLoadingErrorDisplayException customException;
    private final GuiMainMenu parent;

    public GuiMpsLoadingError(CustomModLoadingErrorDisplayException customException, GuiMainMenu parent) {
        super(customException);
        this.customException = customException;
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        customException.initGui(this, fontRenderer);
        this.buttonList.removeIf(button -> button.id == 10);
        this.buttonList.stream().filter(button -> button.id == 11).findFirst().ifPresent(button -> button.x = 50);
        // TODO TRANSLATION ?
        GuiButton button = new GuiButton(48, this.width / 2 + 5, this.height - 38, this.width / 2 - 55, 20, "Ignore and continue");
        button.packedFGColour = 0xFF00FF00;
        this.buttonList.add(button);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 48) {
            DynamXMain.memoizedLoadingError = null;
            this.mc.displayGuiScreen(parent);
        } else {
            super.actionPerformed(button);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            DynamXMain.memoizedLoadingError = null;
            this.mc.displayGuiScreen(parent);
        }
    }
}
