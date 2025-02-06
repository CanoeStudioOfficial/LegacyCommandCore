package com.sing.legacycompletion.mixins;

import net.minecraft.client.gui.*;
import net.minecraft.util.TabCompleter;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.ChatTabCompleter.class)
public abstract class ChatTabCompleterMixin extends TabCompleter {
    private ChatTabCompleterMixin(GuiTextField textFieldIn, boolean hasTargetBlockIn) {super(textFieldIn, hasTargetBlockIn);}

    /**
     * @author MQ-sing
     * @reason Remove vanilla tab completion behavior that simply shows all available completion items when press TAB
     */
    @Inject(at=@At("HEAD"),method = "complete",cancellable = true)
    public void complete(CallbackInfo ci) {
        super.complete();
        ci.cancel();
    }
}
