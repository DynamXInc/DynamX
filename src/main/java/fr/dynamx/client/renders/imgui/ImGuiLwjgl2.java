package fr.dynamx.client.renders.imgui;

import imgui.*;
import imgui.callback.ImStrConsumer;
import imgui.callback.ImStrSupplier;
import imgui.flag.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;


// Class based on default glfw backend
public class ImGuiLwjgl2 {
    private final long[] keyOwnerWindows = new long[512];


    // Internal data
    private boolean wantUpdateMonitors = true;
    private boolean windowFocused = false;

    private final int[] keyMap = new int[ImGuiKey.COUNT];
    private final int[] mouseCursors = new int[ImGuiMouseCursor.COUNT];


    // For mouse tracking
    private final boolean[] mouseJustPressed = new boolean[ImGuiMouseButton.COUNT];
    private final ImVec2 mousePosBackup = new ImVec2();
    private final double[] mouseX = new double[1];
    private final double[] mouseY = new double[1];

    /**
     * @param Offset scroll offset
     */
    public void scrollCallback(final float Offset) {
        final ImGuiIO io = ImGui.getIO();
        io.setMouseWheel(io.getMouseWheel() + Offset);
    }

    /**
     * @param c pressed char
     */
    public void charCallback(final int c) {
        final ImGuiIO io = ImGui.getIO();
        io.addInputCharacter(c);
    }

    public void updateWindowFocus() {
        if (windowFocused != Display.isActive()) {
            windowFocused = Display.isActive();
            ImGui.getIO().addFocusEvent(windowFocused);
        }
    }

    /**
     * Method to do an initialization of the {@link ImGuiLwjgl2} state. It SHOULD be called before calling the {@link ImGuiLwjgl2#newFrame(float, float, float)} method.
     *
     * @return true if everything initialized
     */
    public boolean init() {
        final ImGuiIO io = ImGui.getIO();

        io.addBackendFlags(ImGuiBackendFlags.HasSetMousePos);
        io.setBackendPlatformName("imgui_java_impl_lwjgl2");

        // Keyboard mapping. ImGui will use those indices to peek into the io.KeysDown[] array.
        keyMap[ImGuiKey.Tab] = Keyboard.KEY_TAB;
        keyMap[ImGuiKey.LeftArrow] = Keyboard.KEY_LEFT;
        keyMap[ImGuiKey.RightArrow] = Keyboard.KEY_RIGHT;
        keyMap[ImGuiKey.UpArrow] = Keyboard.KEY_UP;
        keyMap[ImGuiKey.DownArrow] = Keyboard.KEY_DOWN;
        keyMap[ImGuiKey.PageUp] = Keyboard.KEY_PRIOR;
        keyMap[ImGuiKey.PageDown] = Keyboard.KEY_NEXT;
        keyMap[ImGuiKey.Home] = Keyboard.KEY_HOME;
        keyMap[ImGuiKey.End] = Keyboard.KEY_END;
        keyMap[ImGuiKey.Insert] = Keyboard.KEY_INSERT;
        keyMap[ImGuiKey.Delete] = Keyboard.KEY_DELETE;
        keyMap[ImGuiKey.Backspace] = Keyboard.KEY_BACK;
        keyMap[ImGuiKey.Space] = Keyboard.KEY_SPACE;
        keyMap[ImGuiKey.Enter] = Keyboard.KEY_RETURN;
        keyMap[ImGuiKey.Escape] = Keyboard.KEY_ESCAPE;
        keyMap[ImGuiKey.KeyPadEnter] = Keyboard.KEY_NUMPADENTER;
        keyMap[ImGuiKey.A] = Keyboard.KEY_A;
        keyMap[ImGuiKey.C] = Keyboard.KEY_C;
        keyMap[ImGuiKey.V] = Keyboard.KEY_V;
        keyMap[ImGuiKey.X] = Keyboard.KEY_X;
        keyMap[ImGuiKey.Y] = Keyboard.KEY_Y;
        keyMap[ImGuiKey.Z] = Keyboard.KEY_Z;

        io.setKeyMap(keyMap);

        io.setGetClipboardTextFn(new ImStrSupplier() {
            @Override
            public String get() {
                Clipboard clipboard = getClipboard();
                if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                    try {
                        return (String) clipboard.getData(DataFlavor.stringFlavor);
                    } catch (Exception e) {
                        return null;
                    }
                }

                return null;
            }
        });

        io.setSetClipboardTextFn(new ImStrConsumer() {
            @Override
            public void accept(final String str) {
                getClipboard().setContents(new StringSelection(str), null);
            }
        });

        mouseCursors[ImGuiMouseCursor.Arrow] = Cursor.DEFAULT_CURSOR;
        mouseCursors[ImGuiMouseCursor.TextInput] = Cursor.TEXT_CURSOR;
        mouseCursors[ImGuiMouseCursor.ResizeAll] = Cursor.DEFAULT_CURSOR;
        mouseCursors[ImGuiMouseCursor.ResizeNS] = Cursor.N_RESIZE_CURSOR;
        mouseCursors[ImGuiMouseCursor.ResizeEW] = Cursor.E_RESIZE_CURSOR;
        mouseCursors[ImGuiMouseCursor.ResizeNESW] = Cursor.NE_RESIZE_CURSOR;
        mouseCursors[ImGuiMouseCursor.ResizeNWSE] = Cursor.NW_RESIZE_CURSOR;
        mouseCursors[ImGuiMouseCursor.Hand] = Cursor.HAND_CURSOR;
        mouseCursors[ImGuiMouseCursor.NotAllowed] = Cursor.WAIT_CURSOR;

        updateMonitors();

        if (io.hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            throw new UnsupportedOperationException("Its impossible to implement it without using native apis lol so L");
        }

        return true;
    }

    /**
     * Updates {@link ImGuiIO} state.
     *
     * @param delta    deltatime between frames
     * @param fbHeight framebuffer height
     * @param fbWidth  framebuffer width
     */
    public void newFrame(float fbWidth, float fbHeight, float delta) {
        final ImGuiIO io = ImGui.getIO();

        float winHeight = Display.getHeight();
        float winWidth = Display.getWidth();

        io.setDisplaySize(winWidth, winHeight);
        if (winWidth > 0 && winHeight > 0) {
            final float scaleX = fbWidth / winWidth;
            final float scaleY = fbHeight / winHeight;
            io.setDisplayFramebufferScale(scaleX, scaleY);
        }
        if (wantUpdateMonitors) {
            updateMonitors();
        }

        io.setDeltaTime(delta);

        updateMousePosAndButtons();
        updateWindowFocus();
        updateKeyboard();
//        updateMouseCursor();
    }


    private void updateMousePosAndButtons() {
        final ImGuiIO io = ImGui.getIO();

        for (int i = 0; i < ImGuiMouseButton.COUNT; i++) {
            // If a mouse press event came, always pass it as "mouse held this frame", so we don't miss click-release events that are shorter than 1 frame.
            io.setMouseDown(i, Mouse.isButtonDown(i));
        }

        io.getMousePos(mousePosBackup);
        io.setMousePos(-Float.MAX_VALUE, -Float.MAX_VALUE);
        io.setMouseHoveredViewport(0);

        final ImGuiPlatformIO platformIO = ImGui.getPlatformIO();

        for (int n = 0; n < platformIO.getViewportsSize(); n++) {
            final ImGuiViewport viewport = platformIO.getViewports(n);

            final boolean focused = Display.isActive() && Mouse.isInsideWindow();

            // Update mouse buttons
            if (focused) {
                for (int i = 0; i < ImGuiMouseButton.COUNT; i++) {
                    io.setMouseDown(i, Mouse.isButtonDown(i));
                }
            }

            // Set OS mouse position from Dear ImGui if requested (rarely used, only when ImGuiConfigFlags_NavEnableSetMousePos is enabled by user)
            // (When multi-viewports are enabled, all Dear ImGui positions are same as OS positions)
            if (io.getWantSetMousePos() && focused) {
                Mouse.setCursorPosition((int) (mousePosBackup.x - viewport.getPosX()), (int) (mousePosBackup.y - viewport.getPosY()));
            }

            // Set Dear ImGui mouse position from OS position
            if (focused) {
                // Single viewport mode: mouse position in client window coordinates (io.MousePos is (0,0) when the mouse is in the upper-left corner of the app window)
                io.setMousePos((float) Mouse.getX(), (float) Display.getHeight() - Mouse.getY());
            }
        }
    }

    /*private void updateMouseCursor() {
        final ImGuiIO io = ImGui.getIO();

        final boolean noCursorChange = io.hasConfigFlags(ImGuiConfigFlags.NoMouseCursorChange);
        final boolean cursorDisabled = Mouse.isGrabbed();

        if (noCursorChange || cursorDisabled) {
            return;
        }

        final int imguiCursor = ImGui.getMouseCursor();
        final ImGuiPlatformIO platformIO = ImGui.getPlatformIO();

        for (int n = 0; n < platformIO.getViewportsSize(); n++) {
            final long windowPtr = platformIO.getViewports(n).getPlatformHandle();

            if (imguiCursor == ImGuiMouseCursor.None || io.getMouseDrawCursor()) {
                // Hide OS mouse cursor if imgui is drawing it or if it wants no cursor
                Mouse.setGrabbed(false);
            } else {
                // Show OS mouse cursor
                // FIXME-PLATFORM: Unfocused windows seems to fail changing the mouse cursor with GLFW 3.2, but 3.3 works here.
                int cursorId = mouseCursors[imguiCursor] != 0 ? mouseCursors[imguiCursor] : mouseCursors[ImGuiMouseCursor.Arrow];
                Display.setNativeCursor(org.lwjgl.input.Cursor);
                glfwSetCursor(windowPtr, cursorId);
                glfwSetInputMode(windowPtr, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            }
        }
    }*/

    private void updateMonitors() {
        final ImGuiPlatformIO platformIO = ImGui.getPlatformIO();

        platformIO.resizeMonitors(0);

        platformIO.pushMonitors(Display.getX(), Display.getY(), Display.getWidth(), Display.getHeight(), 0, 0, 0, 0, 0);

        wantUpdateMonitors = false;
    }

    private void updateKeyboard() {
        final ImGuiIO io = ImGui.getIO();

        for (int key : keyMap) {
            boolean pressed = Keyboard.isKeyDown(key);
            if (key < keyOwnerWindows.length && pressed != io.getKeysDown(key)) {
                io.setKeysDown(key, pressed);

                switch (key) {
                    case Keyboard.KEY_LCONTROL:
                    case Keyboard.KEY_RCONTROL:
                        io.setKeyCtrl(io.getKeysDown(Keyboard.KEY_LCONTROL) || io.getKeysDown(Keyboard.KEY_RCONTROL));
                        break;
                    case Keyboard.KEY_LSHIFT:
                    case Keyboard.KEY_RSHIFT:
                        io.setKeyShift(io.getKeysDown(Keyboard.KEY_LSHIFT) || io.getKeysDown(Keyboard.KEY_RSHIFT));
                        break;
                    case Keyboard.KEY_LMENU:
                    case Keyboard.KEY_RMENU:
                        io.setKeyAlt(io.getKeysDown(Keyboard.KEY_LMENU) || io.getKeysDown(Keyboard.KEY_RMENU));
                        break;
                    case Keyboard.KEY_LMETA:
                    case Keyboard.KEY_RMETA:
                        io.setKeySuper(io.getKeysDown(Keyboard.KEY_LMETA) || io.getKeysDown(Keyboard.KEY_RMETA));
                        break;
                }
            }
        }
    }

    private static Clipboard getClipboard() {
        return Toolkit.getDefaultToolkit().getSystemClipboard();
    }
}
