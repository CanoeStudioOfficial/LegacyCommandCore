package com.canoestudio.legacycommandcore;

public interface ICompletionList {
    void legacyCompletion$render();
    void legacyCompletion$moveBy(int offset);
    boolean legacyCompletion$hasCompletion();
    void legacyCompletion$handleMouse(int mouseX, int mouseY);
    void legacyCompletion$mouseClicked(int mouseX, int mouseY, int mouseButton);
    void legacyCompletion$setRenderGrowDown();
}
