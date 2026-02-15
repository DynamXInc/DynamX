package fr.hermes.api.mc.utils;

public interface HmGameSettings {
    boolean hm$isFirstPersonView();
    boolean hm$isReverseThirdPersonView();

    boolean hm$isForwardKeyDown();
    boolean hm$isBackKeyDown();
    boolean hm$isLeftKeyDown();
    boolean hm$isRightKeyDown();
    boolean hm$isJumpKeyDown();

    boolean hm$isInvertMouse();
}
