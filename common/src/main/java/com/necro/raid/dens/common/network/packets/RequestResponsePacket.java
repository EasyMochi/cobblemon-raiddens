package com.necro.raid.dens.common.network.packets;

import com.necro.raid.dens.common.CobblemonRaidDens;
import com.necro.raid.dens.common.blocks.entity.RaidCrystalBlockEntity;
import com.necro.raid.dens.common.data.dimension.RaidRegion;
import com.necro.raid.dens.common.events.RaidEvents;
import com.necro.raid.dens.common.events.RaidJoinEvent;
import com.necro.raid.dens.common.network.ServerPacket;
import com.necro.raid.dens.common.raids.RaidInstance;
import com.necro.raid.dens.common.raids.helpers.RaidHelper;
import com.necro.raid.dens.common.raids.RequestHandler;
import com.necro.raid.dens.common.raids.helpers.RaidJoinHelper;
import com.necro.raid.dens.common.raids.helpers.RaidRegionHelper;
import com.necro.raid.dens.common.util.ComponentUtils;
import com.necro.raid.dens.common.util.RaidUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record RequestResponsePacket(boolean accept, String player) implements CustomPacketPayload, ServerPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CobblemonRaidDens.MOD_ID, "request_response");
    public static final Type<RequestResponsePacket> PACKET_TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, RequestResponsePacket> CODEC = StreamCodec.ofMember(RequestResponsePacket::write, RequestResponsePacket::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.accept);
        buf.writeUtf(this.player);
    }

    public static RequestResponsePacket read(FriendlyByteBuf buf) {
        return new RequestResponsePacket(buf.readBoolean(), buf.readUtf());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }

    @Override
    public void handleServer(ServerPlayer host) {
        RequestHandler handler = RaidHelper.getRequest(host);
        if (handler == null) return;

        UUID playerId = handler.getPlayerId(this.player);
        if (playerId == null) return;

        ServerPlayer player = host.server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            handler.removePlayer(playerId);
            return;
        }

        RaidCrystalBlockEntity blockEntity = handler.getBlockEntity();
        if (this.accept) this.acceptRequest(host, player, blockEntity);
        else this.denyRequest(host, player);
    }

    private void acceptRequest(ServerPlayer host, ServerPlayer player, RaidCrystalBlockEntity blockEntity) {
        if (blockEntity.isFull()) {
            RaidJoinHelper.removeFromQueue(player, true);
            this.removeRequest(host, player);
            host.displayClientMessage(ComponentUtils.getSystemMessage("message.cobblemonraiddens.raid.lobby_is_full"), true);
            player.displayClientMessage(ComponentUtils.getSystemMessage("message.cobblemonraiddens.raid.lobby_is_full"), true);
            return;
        }

        boolean success = RaidEvents.RAID_JOIN.postWithResult(new RaidJoinEvent(player, false, blockEntity.getRaidBoss()));
        if (!success) {
            RaidJoinHelper.removeFromQueue(player, true);
            this.removeRequest(host, player);
            return;
        }

        RaidRegion region = RaidRegionHelper.getRegion(blockEntity.getUuid());
        RaidInstance raid = RaidHelper.ACTIVE_RAIDS.get(blockEntity.getUuid());

        if (region != null && raid != null && RaidJoinHelper.isInQueue(player) && !RaidJoinHelper.isParticipating(player, false)) {
            if (player.getServer() == null) {
                RaidJoinHelper.removeFromQueue(player, true);
                this.removeRequest(host, player);
                return;
            }

            if (!RaidJoinHelper.addQueuedParticipant(player, blockEntity.getUuid(), false, true)) {
                this.removeRequest(host, player);
                return;
            }
            this.removeRequest(host, player);

            raid.addPlayer(player);
            RaidUtils.teleportPlayerToRaid(player, player.getServer(), region);
            blockEntity.syncAspects(player);
        }
        else {
            RaidJoinHelper.removeFromQueue(player, true);
            this.removeRequest(host, player);
            host.displayClientMessage(ComponentUtils.getSystemMessage("message.cobblemonraiddens.raid.request_time_out"), true);
        }
    }

    private void denyRequest(ServerPlayer host, ServerPlayer player) {
        if (RaidJoinHelper.isInQueue(player)) {
            RaidJoinHelper.removeFromQueue(player, true);
            this.removeRequest(host, player);
            player.displayClientMessage(ComponentUtils.getSystemMessage(
                Component.translatable("message.cobblemonraiddens.raid.rejected_request", host.getName())), true
            );
            host.displayClientMessage(ComponentUtils.getSystemMessage("message.cobblemonraiddens.raid.confirm_deny_request"), true);
        }
        else {
            this.removeRequest(host, player);
            host.displayClientMessage(ComponentUtils.getSystemMessage("message.cobblemonraiddens.raid.request_time_out"), true);
        }
    }

    private void removeRequest(ServerPlayer host, ServerPlayer player) {
        RequestHandler handler = RaidHelper.REQUEST_QUEUE.get(host.getUUID());
        if (handler != null) handler.removePlayer(player);
    }
}
