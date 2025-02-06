package com.sing.legacycompletion.mixins;

import com.sing.legacycompletion.ICompletionList;
import net.minecraft.client.gui.*;
import net.minecraft.util.TabCompleter;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(GuiChat.class)
public abstract class GuiChatMixin extends GuiScreen {
    @Shadow
    protected GuiTextField inputField;
    @Shadow
    private TabCompleter tabCompleter;
    @Unique
    private boolean legacyCompletion$hasCompleted;

    @Shadow
    public void getSentHistory(int i) {
    }
    @Inject(method = "handleMouseInput",at=@At("TAIL"))
    protected void handleMouseInput(CallbackInfo ci){
        int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        ((ICompletionList)tabCompleter).legacyCompletion$handleMouse(x,y);
    }
    @Inject(method = "mouseClicked",at=@At("TAIL"))
    protected void mouseClicked(int x,int y,int button,CallbackInfo c){
        ((ICompletionList)tabCompleter).legacyCompletion$mouseClicked(x,y,button);
    }
    /**
     * @author MQ-sing
     * @reason Rewrite main logic in chat screen
     */
    @Overwrite
    protected void keyTyped(char typedChar, int keyCode) {
        this.tabCompleter.resetRequested();
        ICompletionList list = (ICompletionList) this.tabCompleter;
        if (!legacyCompletion$hasCompleted && list.legacyCompletion$hasCompletion()) legacyCompletion$hasCompleted = true;
        switch (keyCode) {
            case 1:
                mc.displayGuiScreen(null);
                break;
            case 15:
                if(!legacyCompletion$hasCompleted) tabCompleter.resetDidComplete();
                else {
                    if(tabCompleter.didComplete)list.legacyCompletion$moveBy(1);
                    tabCompleter.complete();
                }
                break;
            case 200:
                if (legacyCompletion$hasCompleted)
                    list.legacyCompletion$moveBy(-1);
                else this.getSentHistory(-1);
                break;
            case 208:
                if (legacyCompletion$hasCompleted) {
                    list.legacyCompletion$moveBy(1);
                } else this.getSentHistory(1);
                break;
            case 201:
                this.mc.ingameGUI.getChatGUI().scroll(this.mc.ingameGUI.getChatGUI().getLineCount() - 1);
                break;
            case 209:
                this.mc.ingameGUI.getChatGUI().scroll(-this.mc.ingameGUI.getChatGUI().getLineCount() + 1);
                break;
            case 28:
            case 156: {
                String s = this.inputField.getText().trim();
                if (!s.isEmpty()) {
                    this.sendChatMessage(s);
                }
                //TODO make it configurable
                this.mc.displayGuiScreen(null);
                break;
            }
            default: {
                this.inputField.textboxKeyTyped(typedChar, keyCode);
                this.tabCompleter.resetDidComplete();
                break;
            }
        }
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    public void drawCompletionItem(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        ((ICompletionList) tabCompleter).legacyCompletion$render();
    }
}
