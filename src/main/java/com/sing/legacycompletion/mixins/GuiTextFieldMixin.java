package com.sing.legacycompletion.mixins;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiTextField.class)
public abstract class GuiTextFieldMixin extends Gui {
    @Shadow private String text;
    @Shadow
    public int lineScrollOffset;
    @Shadow private int selectionEnd;
    @Final
    @Shadow
    public FontRenderer fontRenderer;
    @Shadow
    public int getWidth(){return 0;}
    /**
     * @author MQ-sing
     * @reason Let cursor back not to back entire screen text
     */
    @Overwrite
    public void setSelectionPos(int position)
    {
        int i = this.text.length();

        if (position > i)
        {
            position = i;
        }

        if (position <= 0)
        {
            position = 0;
        }

        this.selectionEnd = position;

        if (this.fontRenderer != null)
        {
            if (this.lineScrollOffset > i)
            {
                this.lineScrollOffset = i;
            }

            int j = this.getWidth();
            String s = this.fontRenderer.trimStringToWidth(this.text.substring(this.lineScrollOffset), j);
            int k = s.length() + this.lineScrollOffset;

            if (position == this.lineScrollOffset)
            {
                if(position!=0) this.lineScrollOffset--;
//                this.lineScrollOffset -= this.fontRenderer.trimStringToWidth(this.text, j, true).length();
            }

            if (position > k)
            {
                this.lineScrollOffset += position - k;
            }
            else if (position <= this.lineScrollOffset)
            {
                this.lineScrollOffset -= this.lineScrollOffset - position;
            }

            this.lineScrollOffset = MathHelper.clamp(this.lineScrollOffset, 0, i);
        }
    }
}
