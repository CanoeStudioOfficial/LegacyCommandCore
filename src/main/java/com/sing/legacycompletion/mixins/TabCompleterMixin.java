package com.sing.legacycompletion.mixins;

import com.sing.legacycompletion.ICompletionList;
import com.sing.legacycompletion.utils.Utils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.TabCompleter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.Rectangle;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mixin(TabCompleter.class)
@ParametersAreNonnullByDefault
public abstract class TabCompleterMixin implements ICompletionList {
   @Shadow
   protected int completionIdx;
   @Shadow
   protected List<String> completions;
   @Final
   @Shadow
   protected GuiTextField textField;
   @Shadow
   public boolean didComplete;
   @Shadow
   protected boolean requestedCompletions;

    @Shadow
    private void requestCompletions(String s) {}

    @Unique
    private static final int VISIBLE_ROWS = 9;
    @Unique
    public int legacyCompletion$scrollOffset;
    @Unique
    public boolean legacyCompletion$renderGrowDown=false;
    /**
     * @author MQ-sing
     * @reason rewrite logic
     */
    @Overwrite
    public void complete(){
        if(this.completions.isEmpty())return;
        if (this.didComplete)
        {
            this.textField.deleteFromCursor(Utils.getLastArgumentStart(textField.getCursorPosition(),textField.getText()) - this.textField.getCursorPosition());
        }else {
            this.didComplete=true;
            textField.deleteFromCursor(Utils.getLastArgumentStart(textField.getCursorPosition(),textField.getText())-textField.getCursorPosition());//(this.textField.getCursorPosition()-item.length());
        }
        final String item = this.completions.get(this.completionIdx);
        this.textField.writeText(TextFormatting.getTextWithoutFormattingCodes(item));
    }
    /**
     * @author MQ-sing
     * @reason Move completion logic to complete() method
     */
    @Overwrite
    public void setCompletions(String... newCompl)
    {
        if (!this.requestedCompletions) return;
        String[] commandNameComplete = net.minecraftforge.client.ClientCommandHandler.instance.latestAutoComplete;
        if(commandNameComplete!=null&&commandNameComplete.length!=0){
            this.completions=Arrays.asList(commandNameComplete);
        }else if(newCompl.length!=0){
            this.completions=Arrays.asList(newCompl);
        }else this.completions= Collections.emptyList();
    }
    @Override
    public void legacyCompletion$moveBy(int offset){
        completionIdx+=offset;
        if(completionIdx>=completions.size())completionIdx-=completions.size();
        else if(completionIdx<0)completionIdx+=completions.size();

        if (completionIdx < legacyCompletion$scrollOffset) {
            legacyCompletion$scrollOffset = completionIdx;
        } else if (completionIdx >= legacyCompletion$scrollOffset + VISIBLE_ROWS) {
            legacyCompletion$scrollOffset = completionIdx - VISIBLE_ROWS + 1;
        }
        legacyCompletion$scrollOffset = MathHelper.clamp(
                legacyCompletion$scrollOffset,
                0,
                Math.max(0,completions.size() - VISIBLE_ROWS)
        );
    }
    @Override
    public void legacyCompletion$render(){
        final int lastArgStart = Utils.getLastArgumentStart(textField.getCursorPosition(), textField.getText());
        final int drawX= textField.getEnableBackgroundDrawing()? textField.x + 4 : textField.x;
        final int drawY = textField.getEnableBackgroundDrawing() ? textField.y + (textField.height - 8) / 2 : textField.y;
        if(this.completions.isEmpty())return;
        final String item = completions.get(completionIdx);
        if(!didComplete &&completions.size()==1&&
                        lastArgStart + item.length()
                                <=textField.getCursorPosition())return;
        final FontRenderer fontRenderer = textField.fontRenderer;
        final String text = fontRenderer.trimStringToWidth(textField.getText().substring(textField.lineScrollOffset), textField.getWidth());
        int left = drawX+ fontRenderer.getStringWidth(textField.getText().substring(textField.lineScrollOffset,lastArgStart));
        final int renderStart = MathHelper.clamp(legacyCompletion$scrollOffset, 0, Math.max(completions.size() - VISIBLE_ROWS,0));
        final int renderEnd = renderStart +Math.min(VISIBLE_ROWS,completions.size());
        int boxWidth=1;
        final int completionStart = textField.getCursorPosition()-lastArgStart;
        if(completionStart<item.length() && item.startsWith(textField.getText().substring(lastArgStart,textField.getCursorPosition())))
            fontRenderer.drawString(item.substring(completionStart),drawX+ fontRenderer.getStringWidth(text),drawY,0xFF777777);
        for(String completion : completions){
            boxWidth=Math.max(boxWidth,fontRenderer.getStringWidth(completion));
        }
        final int itemHeight = fontRenderer.FONT_HEIGHT + 2;
        if(!legacyCompletion$renderGrowDown) {
            int top=drawY-3;
            for (int i = renderEnd - 1; i >= renderStart; --i) {
                final int lastTop = top;
                top -= itemHeight;
                Gui.drawRect(left, top, left + boxWidth, lastTop, 0xC0000000);
                fontRenderer.drawStringWithShadow(completions.get(i), left, top, i == completionIdx ? 0xFFFFFF55 : 0xFF999999);
            }
        }else{
            int top=drawY+3+fontRenderer.FONT_HEIGHT+5;
            for (int i = renderStart; i < renderEnd; ++i) {
                final int lastTop = top;
                top += itemHeight;
                Gui.drawRect(left, lastTop, left + boxWidth, top, 0xC0000000);
                fontRenderer.drawStringWithShadow(completions.get(i), left, lastTop, i == completionIdx ? 0xFFFFFF55 : 0xFF999999);
            }
        }
    }
    @Override
    public boolean legacyCompletion$hasCompletion(){
        return !this.completions.isEmpty();
    }
    @Inject(method ="resetDidComplete",at=@At("TAIL"))
    public void resetDidComplete(CallbackInfo ci){
        this.completionIdx = 0;
        String s = this.textField.getText().substring(0, this.textField.getCursorPosition());
        this.requestCompletions(s);
        if(s.endsWith(" "))this.completions=Collections.emptyList();
    }
    @Override
    public void legacyCompletion$handleMouse(int x,int y){
        final Rectangle box = legacyCompletion$getCompletionListBox();
        if(box!=null&&box.contains(x,y)){
            final int dWheel = Mouse.getEventDWheel();
            if(dWheel<0){
                legacyCompletion$scrollOffset++;
                if(legacyCompletion$scrollOffset>=Math.max(0,completions.size()-VISIBLE_ROWS))legacyCompletion$scrollOffset=Math.max(0,completions.size()-VISIBLE_ROWS);
            }else if(dWheel>0){
                legacyCompletion$scrollOffset--;
                if(legacyCompletion$scrollOffset<0)legacyCompletion$scrollOffset=0;
            }
            completionIdx=MathHelper.clamp((y-box.getY()) / (textField.fontRenderer.FONT_HEIGHT + 2)+legacyCompletion$scrollOffset,0,completions.size()-1);
        }
    }
    @Override
    public void legacyCompletion$mouseClicked(int mouseX, int mouseY, int mouseButton){
        final Rectangle box = legacyCompletion$getCompletionListBox();
        if(box!=null&&box.contains(mouseX,mouseY)){
            complete();
        }
    }

    @Unique
    Rectangle legacyCompletion$getCompletionListBox(){
        final int drawX= textField.getEnableBackgroundDrawing()? textField.x + 4 : textField.x;
        final int drawY = textField.getEnableBackgroundDrawing() ? textField.y + (textField.height - 8) / 2 : textField.y;
        final int lastArgStart = Utils.getLastArgumentStart(textField.getCursorPosition(), textField.getText());
        if(this.completions.isEmpty())return null;
        final String item = completions.get(completionIdx);
        if(!didComplete &&completions.size()==1&&
                lastArgStart + item.length()
                        <=textField.getCursorPosition())return null;
        int left =drawX+ textField.fontRenderer.getStringWidth(textField.getText().substring(textField.lineScrollOffset,lastArgStart));
        int boxWidth=1;
        for(String completion : completions){
            boxWidth=Math.max(boxWidth,textField.fontRenderer.getStringWidth(completion));
        }
        final int height = (textField.fontRenderer.FONT_HEIGHT + 2) * Math.min(completions.size(), VISIBLE_ROWS);
        if(!legacyCompletion$renderGrowDown) {
            final int bottom = drawY - 3;
            final int top = bottom - height;
            return new Rectangle(left, top, boxWidth, height);
        }else{
            final int bottom = drawY + 3;
            final int top = bottom + height;
            return new Rectangle(left, top, boxWidth, height);
        }
    }
    @Override
    public void legacyCompletion$setRenderGrowDown(){
        legacyCompletion$renderGrowDown=true;
    }
}
