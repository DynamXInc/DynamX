package fr.dynamx.client.renders.imgui.imlib.core;

import imgui.ImFontAtlas;

import java.io.IOException;

public interface ImGuiInitInterface {
    void preInit();

    void loadFont(ImFontAtlas font, short[] ranges) throws IOException;

    void postInit();
}
