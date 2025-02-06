package com.sing.legacycompletion.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketTabComplete;
import net.minecraft.util.ITabCompleter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.ParametersAreNonnullByDefault;

@Mixin(NetHandlerPlayClient.class)
@ParametersAreNonnullByDefault
public abstract class NetHandlerPlayClientMixin implements INetHandlerPlayClient {
    @Shadow
    private Minecraft client;

    /**
     * @author MQ-sing
     * @reason Remove Arrays.copy call to
     */
    @Overwrite
    public void handleTabComplete(SPacketTabComplete packetIn){
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.client);
        String[] astring = packetIn.getMatches();
//        Arrays.sort(astring);
        if (this.client.currentScreen instanceof ITabCompleter)
        {
            ((ITabCompleter)this.client.currentScreen).setCompletions(astring);
        }
    }
}
