package com.canoestudio.legacycommandcore.mixins;

import com.canoestudio.legacycommandcore.ICompletionList;
import com.canoestudio.legacycommandcore.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.command.ICommand;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.TabCompleter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.IClientCommand;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.Rectangle;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@Mixin(TabCompleter.class)
@ParametersAreNonnullByDefault
public abstract class TabCompleterMixin implements ICompletionList {
    @Unique
    private static final int VISIBLE_ROWS = 9;
    @Shadow
    public boolean didComplete;
    @Unique
    public int legacyCompletion$scrollOffset;
    @Unique
    public boolean legacyCompletion$renderGrowDown = false;
    @Shadow
    protected int completionIdx;
    @Shadow
    protected List<String> completions;
    @Final
    @Shadow
    protected GuiTextField textField;
    @Shadow
    protected boolean requestedCompletions;
    @Final
    @Shadow
    protected boolean hasTargetBlock;
    @Unique
    private List<String> legacyCompletion$clientCompletion=null;
    /**
     * @author MQ-sing
     * @reason rewrite logic
     */
    @Overwrite
    public void complete() {
        if (this.completions.isEmpty()) return;
        if (!this.didComplete) {
            this.didComplete = true;
        }
        textField.deleteFromCursor(Utils.getLastArgumentStart(textField.getCursorPosition(), textField.getText()) - textField.getCursorPosition());
        final String item = this.completions.get(this.completionIdx);
        this.textField.writeText(TextFormatting.getTextWithoutFormattingCodes(item));
    }

    /**
     * @author MQ-sing
     * @reason Move completion logic to complete() method
     */
    @Overwrite
    public void setCompletions(String... serverCompletion) {
        if (!this.requestedCompletions) return;
        if(this.legacyCompletion$clientCompletion!=null) {
            this.completions =legacyCompletion$clientCompletion;
            this.completions.addAll(Arrays.asList(serverCompletion));
            completions.sort(null);
            legacyCompletion$clientCompletion=null;
        }else this.completions=Arrays.asList(serverCompletion);
    }

    @Override
    public void legacyCompletion$moveBy(int offset) {
        completionIdx += offset;
        if (completionIdx >= completions.size()) completionIdx -= completions.size();
        else if (completionIdx < 0) completionIdx += completions.size();
        if (completionIdx < legacyCompletion$scrollOffset) {
            legacyCompletion$scrollOffset = completionIdx;
        } else if (completionIdx >= legacyCompletion$scrollOffset + VISIBLE_ROWS) {
            legacyCompletion$scrollOffset = completionIdx - VISIBLE_ROWS + 1;
        }
        legacyCompletion$scrollOffset = MathHelper.clamp(
                legacyCompletion$scrollOffset,
                0,
                Math.max(0, completions.size() - VISIBLE_ROWS)
        );
    }

    @Override
    public void legacyCompletion$render() {
        final Rectangle box = legacyCompletion$getCompletionListBox();
        if (box == null) return;
        final int lastArgStart = Utils.getLastArgumentStart(textField.getCursorPosition(), textField.getText());
        final int drawX = textField.getEnableBackgroundDrawing() ? textField.x + 4 : textField.x;
        final int drawY = textField.getEnableBackgroundDrawing() ? textField.y + (textField.height - 8) / 2 : textField.y;
        final String item = completions.get(completionIdx);
        final FontRenderer fontRenderer = textField.fontRenderer;
        final String text = fontRenderer.trimStringToWidth(textField.getText().substring(textField.lineScrollOffset), textField.getWidth());
        final int renderStart = MathHelper.clamp(legacyCompletion$scrollOffset, 0, Math.max(completions.size() - VISIBLE_ROWS, 0));
        final int renderEnd = renderStart + Math.min(VISIBLE_ROWS, completions.size());
        final int completionStart = textField.getCursorPosition() - lastArgStart;
        // for some reason it will happen
        if(completionStart<0)return;
        if (completionStart < item.length() && item.startsWith(textField.getText().substring(lastArgStart, textField.getCursorPosition())))
            fontRenderer.drawString(item.substring(completionStart), drawX + fontRenderer.getStringWidth(text), drawY, 0xFF777777);
        final int itemHeight = fontRenderer.FONT_HEIGHT + 2;
        int top = box.getY();
        if (!legacyCompletion$renderGrowDown) {
            for (int i = renderStart; i < renderEnd; ++i) {
                final int lastTop = top;
                top += itemHeight;
                Gui.drawRect(box.getX(), lastTop, box.getX() + box.getWidth(), top, 0xC0000000);
                fontRenderer.drawStringWithShadow(completions.get(i), box.getX(), lastTop, i == completionIdx ? 0xFFFFFF55 : 0xFF999999);
            }
        } else {
            for (int i = renderStart; i < renderEnd; ++i) {
                final int lastTop = top;
                top += itemHeight;
                Gui.drawRect(box.getX(), lastTop, box.getX() + box.getWidth(), top, 0xC0000000);
                fontRenderer.drawStringWithShadow(completions.get(i), box.getX(), lastTop, i == completionIdx ? 0xFFFFFF55 : 0xFF999999);
            }
        }
    }

    @Override
    public boolean legacyCompletion$hasCompletion() {
        return !this.completions.isEmpty();
    }

    @Inject(method = "resetDidComplete", at = @At("TAIL"))
    public void resetDidComplete(CallbackInfo ci) {
        this.completionIdx = 0;
        String s = this.textField.getText().substring(0, this.textField.getCursorPosition());
        this.requestCompletions(s);
        if (s.endsWith(" ")) this.completions = Collections.emptyList();
    }

    @Override
    public void legacyCompletion$handleMouse(int x, int y) {
        final Rectangle box = legacyCompletion$getCompletionListBox();
        if (box != null && box.contains(x, y)) {
            final int dWheel = Mouse.getEventDWheel();
            if (dWheel < 0) {
                legacyCompletion$scrollOffset++;
                if (legacyCompletion$scrollOffset >= Math.max(0, completions.size() - VISIBLE_ROWS))
                    legacyCompletion$scrollOffset = Math.max(0, completions.size() - VISIBLE_ROWS);
            } else if (dWheel > 0) {
                legacyCompletion$scrollOffset--;
                if (legacyCompletion$scrollOffset < 0) legacyCompletion$scrollOffset = 0;
            }
            completionIdx = MathHelper.clamp((y - box.getY()) / (textField.fontRenderer.FONT_HEIGHT + 2) + legacyCompletion$scrollOffset, 0, completions.size() - 1);
        }
    }

    @Override
    public void legacyCompletion$mouseClicked(int mouseX, int mouseY, int mouseButton) {
        final Rectangle box = legacyCompletion$getCompletionListBox();
        if (box != null && box.contains(mouseX, mouseY)) {
            complete();
        }
    }

    @Unique
    Rectangle legacyCompletion$getCompletionListBox() {
        final int drawX = textField.getEnableBackgroundDrawing() ? textField.x + 4 : textField.x;
        final int drawY = textField.getEnableBackgroundDrawing() ? textField.y + (textField.height - 8) / 2 : textField.y;
        final int lastArgStart = Utils.getLastArgumentStart(textField.getCursorPosition(), textField.getText());
        if (this.completions.isEmpty()) return null;
        final String item = completions.get(completionIdx);
        if (!didComplete && completions.size() == 1 &&
                lastArgStart + item.length()
                        <= textField.getCursorPosition()) return null;
        int left = drawX + textField.fontRenderer.getStringWidth(textField.getText().substring(textField.lineScrollOffset, lastArgStart));
        int boxWidth = 1;
        for (String completion : completions) {
            boxWidth = Math.max(boxWidth, textField.fontRenderer.getStringWidth(completion));
        }
        final int height = (textField.fontRenderer.FONT_HEIGHT + 2) * Math.min(completions.size(), VISIBLE_ROWS);
        if (!legacyCompletion$renderGrowDown) {
            final int bottom = drawY - 3;
            final int top = bottom - height;
            return new Rectangle(left, top, boxWidth, height);
        } else {
            final int bottom = drawY + 3 + textField.fontRenderer.FONT_HEIGHT + 5;
            return new Rectangle(left, bottom, boxWidth, height);
        }
    }

    @Override
    public void legacyCompletion$setRenderGrowDown() {
        legacyCompletion$renderGrowDown = true;
    }

    /**
     * @author MQ-sing
     * @reason let it always provide a completion
     */
    @Overwrite
    private void requestCompletions(String prefix) {
        final Minecraft mc = Minecraft.getMinecraft();
        final boolean sendInChat = mc.currentScreen instanceof GuiChat;
        if (sendInChat && prefix.isEmpty()) return;
        //no longer use the function that will throw an IndexOutOfBoundsException when a empty string passed in
//        net.minecraftforge.client.ClientCommandHandler.instance.autoComplete(prefix);
        final int index = prefix.indexOf(' ');
        final IntegratedServer server = mc.getIntegratedServer();
        final Map<String, ICommand> commands = ClientCommandHandler.instance.getCommands();
        if (server != null&&sendInChat) {
            if (index == -1) {
                final boolean useSlash = prefix.startsWith("/");
                this.legacyCompletion$clientCompletion=new ArrayList<>();
                for (Map.Entry<String, ICommand> command : commands.entrySet()) {
                    if (command.getValue().getName().startsWith(useSlash?prefix.substring(1):prefix)&&
                            command.getValue().checkPermission(server, mc.player)
                            && (!(command.getValue() instanceof IClientCommand) ||
                            useSlash &&
                                    !((IClientCommand) command.getValue()).allowUsageWithoutPrefix(mc.player, prefix))) {
                        this.legacyCompletion$clientCompletion.add(useSlash ? '/' + command.getKey() : command.getKey());
                    }
                }
                //TODO not to simply sort it
                this.legacyCompletion$clientCompletion.sort(String::compareTo);
            } else if(!prefix.trim().isEmpty()){
                String[] params = prefix.split(" ",-1);
                final String commandName = params[0];
                final ICommand command = commands.get(commandName.substring(1));
                if (command != null) {
                    legacyCompletion$clientCompletion = new ArrayList<>(command.getTabCompletions(server, mc.player,Utils.dropFirst(params), mc.player.getPosition()));
                }
            }
        }
        mc.player.connection.sendPacket(new CPacketTabComplete(prefix, this.getTargetBlockPos(), this.hasTargetBlock));
        this.requestedCompletions = true;
    }

    @Shadow
    public BlockPos getTargetBlockPos() {
        return null;
    }
}
