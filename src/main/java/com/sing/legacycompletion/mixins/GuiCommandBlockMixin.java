package com.sing.legacycompletion.mixins;

import com.sing.legacycompletion.ICompletionList;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCommandBlock;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ITabCompleter;
import net.minecraft.util.TabCompleter;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(GuiCommandBlock.class)
public abstract class GuiCommandBlockMixin extends GuiScreen implements ITabCompleter {
    @Shadow
    private TabCompleter tabCompleter;
    @Shadow
    private GuiTextField commandTextField;
    @Shadow
    private GuiTextField previousOutputTextField;
    @Shadow
    private GuiButton cancelBtn;
    @Shadow
    private GuiButton doneBtn;
    @Unique
    private boolean legacyCompletion$hasCompleted;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void initialize(CallbackInfo ci) {
        ((ICompletionList) tabCompleter).legacyCompletion$setRenderGrowDown();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        ((ICompletionList) tabCompleter).legacyCompletion$handleMouse(x, y);
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    protected void mouseClicked(int x, int y, int button, CallbackInfo c) {
        ((ICompletionList) tabCompleter).legacyCompletion$mouseClicked(x, y, button);
    }

    /**
     * @author MQ-sing
     * @reason Add completion list for command blocks
     */
    @Overwrite
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        this.tabCompleter.resetRequested();
        ICompletionList list = (ICompletionList) this.tabCompleter;
        if (!legacyCompletion$hasCompleted && list.legacyCompletion$hasCompletion())
            legacyCompletion$hasCompleted = true;
        switch (keyCode) {
            case 1:
                this.actionPerformed(this.cancelBtn);
                break;
            case 15:
                if (!legacyCompletion$hasCompleted) tabCompleter.resetDidComplete();
                else {
                    if (tabCompleter.didComplete) list.legacyCompletion$moveBy(1);
                    tabCompleter.complete();
                }
                break;
            case 200:
                list.legacyCompletion$moveBy(-1);
                break;
            case 208:
                list.legacyCompletion$moveBy(1);
                break;
            case 28:
            case 156:
                this.actionPerformed(this.doneBtn);
                break;
            default: {
                this.commandTextField.textboxKeyTyped(typedChar, keyCode);
                this.tabCompleter.resetDidComplete();
                break;
            }
        }
    }

    /**
     * @author MQ-sing
     * @reason Remove the entity selectors tip&insert completer render logic
     */
    @Overwrite
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, I18n.format("advMode.setCommand"), this.width / 2, 20, 16777215);
        this.drawString(this.fontRenderer, I18n.format("advMode.command"), this.width / 2 - 150, 40, 10526880);
        this.commandTextField.drawTextBox();
        if (!this.previousOutputTextField.getText().isEmpty()) {
            int i = 80 * this.fontRenderer.FONT_HEIGHT + 1;
            this.drawString(this.fontRenderer, I18n.format("advMode.previousOutput"), this.width / 2 - 150, i + 4, 10526880);
            this.previousOutputTextField.drawTextBox();
        }
        ((ICompletionList) this.tabCompleter).legacyCompletion$render();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
