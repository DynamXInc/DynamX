package fr.dynamx.client.renders.imgui.imlib.core;

import fr.dynamx.client.renders.imgui.imlib.impl.ImGuiImplDisplay;
import fr.dynamx.client.renders.imgui.imlib.impl.ImGuiImplGl2;
import fr.dynamx.common.DynamXMain;
import imgui.*;
import imgui.extension.implot.ImPlot;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiConfigFlags;
import org.apache.commons.compress.utils.IOUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ImGuiImpl {
    private final static ImGuiImplDisplay imGuiImplDisplay = new ImGuiImplDisplay();
    private final static ImGuiImplGl2 imGuiImplGl2 = new ImGuiImplGl2();

    public static ImFont defaultFont;
    public static ImFont font21;
    private static final List<ImGuiInitInterface> initFunctions = new ArrayList<>();
    private static boolean isCreated = false;

    public static void create() {
        if (isCreated) {
            return;
        }
        ImGui.createContext();
        ImPlot.createContext();

        final ImGuiIO data = ImGui.getIO();
        data.setIniFilename("imguilib.ini");
        data.setFontGlobalScale(1F);

        for (ImGuiInitInterface callback : initFunctions) {
            callback.preInit();
        }

        final ImFontAtlas fonts = data.getFonts();

        final ImFontConfig basicConfig = new ImFontConfig();
        basicConfig.setName("alibaba 18px");


        try {
            byte[] byteArray = IOUtils.toByteArray(Objects.requireNonNull(DynamXMain.class.getResourceAsStream("/assets/dynamxmod/font/minecraftregular-bmg3.otf")));
            defaultFont = fonts.addFontFromMemoryTTF(byteArray, 18);

            for (ImGuiInitInterface callback : initFunctions) {
                //callback.loadFont(fonts, glyphRanges);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        fonts.build();
        basicConfig.destroy();

        // data.setConfigFlags(ImGuiConfigFlags.DockingEnable);
        // In case you want to enable Viewports on Windows, you have to do this instead of the above line:
        // data.setConfigFlags(ImGuiConfigFlags.DockingEnable | ImGuiConfigFlags.ViewportsEnable);

        for (ImGuiInitInterface callback : initFunctions) {
            callback.postInit();
        }

        imGuiImplDisplay.init();
        imGuiImplGl2.init();
        isCreated = true;
        initFunctions.clear();
    }

    private static void styleDark(ImGuiStyle style) {
        style.setAlpha(0.95f);
        style.setDisabledAlpha(0.6000000238418579f);
        style.setWindowPadding(8.0f, 8.0f);
        style.setWindowRounding(4.0f);
        style.setWindowBorderSize(0.0f);
        style.setWindowMinSize(32.0f, 32.0f);
        style.setWindowTitleAlign(0.5f, 0.5f);
        style.setWindowMenuButtonPosition(1);
        style.setChildRounding(0.0f);
        style.setChildBorderSize(1.0f);
        style.setPopupRounding(4.0f);
        style.setPopupBorderSize(1.0f);
        style.setFramePadding(8.0f, 6.0f);
        style.setFrameRounding(5.5f);
        style.setFrameBorderSize(0.0f);
        style.setItemSpacing(8.0f, 4.0f);
        style.setItemInnerSpacing(4.0f, 4.0f);
        style.setCellPadding(4.0f, 2.0f);
        style.setIndentSpacing(21.0f);
        style.setColumnsMinSpacing(6.0f);
        style.setScrollbarSize(11.0f);
        style.setScrollbarRounding(2.5f);
        style.setGrabMinSize(10.0f);
        style.setGrabRounding(2.0f);
        style.setTabRounding(3.5f);
        style.setTabBorderSize(0.0f);
        style.setTabMinWidthForCloseButton(0.0f);
        style.setColorButtonPosition(2);
        style.setButtonTextAlign(0.5f, 0.5f);
        style.setSelectableTextAlign(0.0f, 0.0f);
        style.setColor(ImGuiCol.Text, 1.0f, 1.0f, 1.0f, 1.0f);
        style.setColor(ImGuiCol.TextDisabled, 0.5921568870544434f, 0.5921568870544434f, 0.5921568870544434f, 1.0f);
        style.setColor(ImGuiCol.WindowBg, 0.1450980454683304f, 0.1450980454683304f, 0.1490196138620377f, 1.0f);
        style.setColor(ImGuiCol.ChildBg, 0.1450980454683304f, 0.1450980454683304f, 0.1490196138620377f, 1.0f);
        style.setColor(ImGuiCol.PopupBg, 0.1450980454683304f, 0.1450980454683304f, 0.1490196138620377f, 1.0f);
        style.setColor(ImGuiCol.Border, 0.3058823645114899f, 0.3058823645114899f, 0.3058823645114899f, 1.0f);
        style.setColor(ImGuiCol.BorderShadow, 0.3058823645114899f, 0.3058823645114899f, 0.3058823645114899f, 1.0f);
        style.setColor(ImGuiCol.FrameBg, 0.2000000029802322f, 0.2000000029802322f, 0.2156862765550613f, 1.0f);
        style.setColor(ImGuiCol.FrameBgHovered, 0.2558823645114899f, 0.2558823645114899f, 0.2558823645114899f, 1.0f);
        style.setColor(ImGuiCol.FrameBgActive, 0.3000000029802322f, 0.3000000029802322f, 0.3156862765550613f, 1.0f);
        style.setColor(ImGuiCol.TitleBg, 0.1450980454683304f, 0.1450980454683304f, 0.1490196138620377f, 1.0f);
        style.setColor(ImGuiCol.TitleBgActive, 0.1450980454683304f, 0.1450980454683304f, 0.1490196138620377f, 1.0f);
        style.setColor(ImGuiCol.TitleBgCollapsed, 0.1450980454683304f, 0.1450980454683304f, 0.1490196138620377f, 1.0f);
        style.setColor(ImGuiCol.MenuBarBg, 0.2000000029802322f, 0.2000000029802322f, 0.2156862765550613f, 1.0f);
        style.setColor(ImGuiCol.ScrollbarBg, 0.2000000029802322f, 0.2000000029802322f, 0.2156862765550613f, 1.0f);
        style.setColor(ImGuiCol.ScrollbarGrab, 0.321568638086319f, 0.321568638086319f, 0.3333333432674408f, 1.0f);
        style.setColor(ImGuiCol.ScrollbarGrabHovered, 0.3529411852359772f, 0.3529411852359772f, 0.3725490272045135f, 1.0f);
        style.setColor(ImGuiCol.ScrollbarGrabActive, 0.3529411852359772f, 0.3529411852359772f, 0.3725490272045135f, 1.0f);
        style.setColor(ImGuiCol.CheckMark, 0.0f, 0.4666666686534882f, 0.7843137383460999f, 1.0f);
        style.setColor(ImGuiCol.SliderGrab, 0.1137254908680916f, 0.5921568870544434f, 0.9254902005195618f, 1.0f);
        style.setColor(ImGuiCol.Button, 0.2000000029802322f, 0.2000000029802322f, 0.2156862765550613f, 1.0f);
        style.setColor(ImGuiCol.ButtonHovered, 0.1137254908680916f, 0.5921568870544434f, 0.9254902005195618f, 1.0f);
        style.setColor(ImGuiCol.ButtonActive, 0.1137254908680916f, 0.5921568870544434f, 0.9254902005195618f, 1.0f);
        style.setColor(ImGuiCol.Header, 0.2000000029802322f, 0.2000000029802322f, 0.2156862765550613f, 1.0f);
        style.setColor(ImGuiCol.HeaderHovered, 0.1137254908680916f, 0.5921568870544434f, 0.9254902005195618f, 1.0f);
        style.setColor(ImGuiCol.HeaderActive, 0.0f, 0.4666666686534882f, 0.7843137383460999f, 1.0f);
        style.setColor(ImGuiCol.Separator, 0.3058823645114899f, 0.3058823645114899f, 0.3058823645114899f, 1.0f);
        style.setColor(ImGuiCol.SeparatorHovered, 0.3058823645114899f, 0.3058823645114899f, 0.3058823645114899f, 1.0f);
        style.setColor(ImGuiCol.SeparatorActive, 0.3058823645114899f, 0.3058823645114899f, 0.3058823645114899f, 1.0f);
        style.setColor(ImGuiCol.ResizeGrip, 0.1450980454683304f, 0.1450980454683304f, 0.1490196138620377f, 1.0f);
        style.setColor(ImGuiCol.ResizeGripHovered, 0.2000000029802322f, 0.2000000029802322f, 0.2156862765550613f, 1.0f);
        style.setColor(ImGuiCol.ResizeGripActive, 0.321568638086319f, 0.321568638086319f, 0.3333333432674408f, 1.0f);
        style.setColor(ImGuiCol.Tab, 0.1450980454683304f, 0.1450980454683304f, 0.1490196138620377f, 1.0f);
        style.setColor(ImGuiCol.TabHovered, 0.1137254908680916f, 0.5921568870544434f, 0.9254902005195618f, 1.0f);
        style.setColor(ImGuiCol.TabActive, 0.0f, 0.4666666686534882f, 0.7843137383460999f, 1.0f);
        style.setColor(ImGuiCol.TabUnfocused, 0.1450980454683304f, 0.1450980454683304f, 0.1490196138620377f, 1.0f);
        style.setColor(ImGuiCol.TabUnfocusedActive, 0.0f, 0.4666666686534882f, 0.7843137383460999f, 1.0f);
        style.setColor(ImGuiCol.TextSelectedBg, 0.0f, 0.4666666686534882f, 0.7843137383460999f, 1.0f);
        style.setColor(ImGuiCol.DragDropTarget, 0.1450980454683304f, 0.1450980454683304f, 0.1490196138620377f, 1.0f);
        style.setColor(ImGuiCol.NavHighlight, 0.1450980454683304f, 0.1450980454683304f, 0.1490196138620377f, 1.0f);
        style.setColor(ImGuiCol.NavWindowingHighlight, 1.0f, 1.0f, 1.0f, 0.699999988079071f);
        style.setColor(ImGuiCol.NavWindowingDimBg, 0.800000011920929f, 0.800000011920929f, 0.800000011920929f, 0.2000000029802322f);
        style.setColor(ImGuiCol.ModalWindowDimBg, 0.1450980454683304f, 0.1450980454683304f, 0.1490196138620377f, 1.0f);
    }

    public static void styleDark() {
        styleDark(ImGui.getStyle());
    }

    public static void handleKey() {
        imGuiImplDisplay.onKey();
    }

    public static void handleMouse() {
        imGuiImplDisplay.onMouse();
    }

    public static void draw(final RenderInterface runnable) {
        if (isCreated) {
            imGuiImplDisplay.newFrame(); // Handle keyboard and mouse interactions
            ImGui.newFrame();
            runnable.render(ImGui.getIO());
            ImGui.render();

            imGuiImplGl2.newFrame();
            imGuiImplGl2.renderDrawData(ImGui.getDrawData());

            if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
                ImGui.updatePlatformWindows();
                ImGui.renderPlatformWindowsDefault();
            }
        }
    }

}