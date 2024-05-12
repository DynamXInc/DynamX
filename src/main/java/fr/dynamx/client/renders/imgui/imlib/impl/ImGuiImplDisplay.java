package fr.dynamx.client.renders.imgui.imlib.impl;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiKey;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

public class ImGuiImplDisplay {
    private boolean mouseButtons[] = new boolean[ImGuiMouseButton.COUNT];
    private long time = 0;

    public void init() {
        ImGuiIO io = ImGui.getIO();
        io.setBackendPlatformName("lwjgl2_display");

        // keyboard
        int[] keymap = new int[ImGuiKey.COUNT];
        keymap[ImGuiKey.Tab] = Keyboard.KEY_TAB;
        keymap[ImGuiKey.LeftArrow] = Keyboard.KEY_LEFT;
        keymap[ImGuiKey.RightArrow] = Keyboard.KEY_RIGHT;
        keymap[ImGuiKey.UpArrow] = Keyboard.KEY_UP;
        keymap[ImGuiKey.DownArrow] = Keyboard.KEY_DOWN;
        keymap[ImGuiKey.PageUp] = Keyboard.KEY_PRIOR;
        keymap[ImGuiKey.PageDown] = Keyboard.KEY_NEXT;
        keymap[ImGuiKey.Home] = Keyboard.KEY_HOME;
        keymap[ImGuiKey.End] = Keyboard.KEY_END;
        keymap[ImGuiKey.Insert] = Keyboard.KEY_INSERT;
        keymap[ImGuiKey.Delete] = Keyboard.KEY_DELETE;
        keymap[ImGuiKey.Backspace] = Keyboard.KEY_BACK;
        keymap[ImGuiKey.Space] = Keyboard.KEY_SPACE;
        keymap[ImGuiKey.Enter] = Keyboard.KEY_RETURN;
        keymap[ImGuiKey.Escape] = Keyboard.KEY_ESCAPE;
        keymap[ImGuiKey.KeyPadEnter] = Keyboard.KEY_NUMPADENTER;
        keymap[ImGuiKey.A] = Keyboard.KEY_A;
        keymap[ImGuiKey.C] = Keyboard.KEY_C;
        keymap[ImGuiKey.V] = Keyboard.KEY_V;
        keymap[ImGuiKey.X] = Keyboard.KEY_X;
        keymap[ImGuiKey.Y] = Keyboard.KEY_Y;
        keymap[ImGuiKey.Z] = Keyboard.KEY_Z;
        io.setKeyMap(keymap);
    }

    public void newFrame() {
        ImGuiIO io = ImGui.getIO();

        // set display size
        float ww = (float)Display.getWidth();
        float wh = (float)Display.getHeight();
        io.setDisplaySize(ww, wh);
        io.setDisplayFramebufferScale(1, 1);

        // set delta
        long nutime = System.currentTimeMillis();
        float delta =
            time > 0 ? (float)(((double)nutime - time) / 1000.0) : 1.0f / 60;
        // prevent failed assert for delta > 0.0f
        io.setDeltaTime((delta > 0.0f) ? delta : 0.01f);
        time = nutime;

        // mouse input
        io.setMousePos((float)Mouse.getX(), wh - (float)Mouse.getY());
        for (int i = 0; i < mouseButtons.length; i++) {
            io.setMouseDown(i, mouseButtons[i] || Mouse.isButtonDown(i));
            mouseButtons[i] = false;
        }
    }

    public void onMouse() {
        if (Mouse.getEventDWheel() != 0)
            onMouseWheel(Mouse.getEventDWheel());
        if (Mouse.getEventButton() != -1) {
            onMouseButton(
                Mouse.getEventButton(), Mouse.getEventButtonState()
            );
        }
    }

    public void onMouseButton(int button, boolean pressed) {
        if (pressed && button > 0 && button < mouseButtons.length) {
            mouseButtons[button] = true;
        }
    }

    public void onMouseWheel(int scrolldelta) {
        ImGuiIO io = ImGui.getIO();
        io.setMouseWheel(io.getMouseWheel() + (float)(scrolldelta / 120));
    }

    public void onKey() {
        int key = Keyboard.getEventKey() == 0 ?
            Keyboard.getEventCharacter() : Keyboard.getEventKey();
        onKey(key, Keyboard.getEventKeyState());
    }

    public void onKey(int key, boolean pressed) {
        ImGuiIO io = ImGui.getIO();
        io.setKeysDown(key, pressed);
        io.setKeyCtrl(io.getKeysDown(Keyboard.KEY_LCONTROL) ||
                      io.getKeysDown(Keyboard.KEY_RCONTROL));
        io.setKeyShift(io.getKeysDown(Keyboard.KEY_LSHIFT) ||
                       io.getKeysDown(Keyboard.KEY_RSHIFT));
        io.setKeyAlt(io.getKeysDown(Keyboard.KEY_LMENU) ||
                       io.getKeysDown(Keyboard.KEY_RMENU));
        io.setKeySuper(io.getKeysDown(Keyboard.KEY_LMETA) ||
                       io.getKeysDown(Keyboard.KEY_RMETA));

        io.addInputCharacter(Keyboard.getEventCharacter());
    }
}
