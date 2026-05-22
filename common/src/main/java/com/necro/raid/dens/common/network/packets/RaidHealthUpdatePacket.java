package com.necro.raid.dens.common.network.packets;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.necro.raid.dens.common.CobblemonRaidDens;
import com.necro.raid.dens.common.network.ClientPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record RaidHealthUpdatePacket(List<Integer> entityIds, List<Float> health) implements CustomPacketPayload, ClientPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CobblemonRaidDens.MOD_ID, "raid_health_update");
    public static final Type<RaidHealthUpdatePacket> PACKET_TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, RaidHealthUpdatePacket> CODEC = StreamCodec.ofMember(RaidHealthUpdatePacket::write, RaidHealthUpdatePacket::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(this.entityIds, FriendlyByteBuf::writeInt);
        buf.writeCollection(this.health, FriendlyByteBuf::writeFloat);
    }

    public static RaidHealthUpdatePacket read(FriendlyByteBuf buf) {
        return new RaidHealthUpdatePacket(buf.readCollection(ArrayList::new, FriendlyByteBuf::readInt), buf.readCollection(ArrayList::new, FriendlyByteBuf::readFloat));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }

    @Override
    public void handleClient() {
        if (Minecraft.getInstance().level == null) return;

        int count = Math.min(this.entityIds().size(), this.health().size());
        for (int i = 0; i < count; i++) {
            float healthRatio = this.health().get(i);
            if (!Float.isFinite(healthRatio)) continue;

            Entity entity = Minecraft.getInstance().level.getEntity(this.entityIds().get(i));
            if (entity instanceof PokemonEntity pokemon) {
                int maxHealth = pokemon.getPokemon().getMaxHealth();
                if (maxHealth <= 0) continue;

                pokemon.getPokemon().setCurrentHealth((int) (Mth.clamp(healthRatio, 0F, 1F) * maxHealth));
            }
        }
    }
}
