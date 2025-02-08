package com.sing.legacycompletion.mixins;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.network.play.server.SPacketTabComplete;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ITickable;
import org.spongepowered.asm.mixin.*;

import java.util.List;

@Mixin(NetHandlerPlayServer.class)
public abstract class NetHandlerPlayServerMixin implements INetHandlerPlayServer, ITickable {
    @Shadow
    @Final
    private MinecraftServer server;
    @Shadow
    public EntityPlayerMP player;
    /**
     * @author MQ-sing
     * @reason add list.sort call
     */
    @Overwrite
    public void processTabComplete(CPacketTabComplete packetIn)
    {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.player.getServerWorld());
        List<String> list = Lists.<String>newArrayList();
        list.addAll(this.server.getTabCompletions(this.player, packetIn.getMessage(), packetIn.getTargetBlock(), packetIn.hasTargetBlock()));
        list.sort(String::compareTo);
        this.player.connection.sendPacket(new SPacketTabComplete(list.toArray(new String[list.size()])));
    }
}
