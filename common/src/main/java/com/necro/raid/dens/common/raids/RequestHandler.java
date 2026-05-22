package com.necro.raid.dens.common.raids;

import com.necro.raid.dens.common.blocks.entity.RaidCrystalBlockEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RequestHandler {
    private final RaidCrystalBlockEntity blockEntity;
    private final Map<UUID, Player> players;

    public RequestHandler(RaidCrystalBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
        this.players = new HashMap<>();
    }

    public RaidCrystalBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public void addPlayer(Player player) {
        this.players.put(player.getUUID(), player);
    }

    public UUID getPlayerId(String player) {
        for (Player value : this.players.values()) {
            if (value.getName().getString().equals(player)) return value.getUUID();
        }
        return null;
    }

    public Player getPlayer(String player) {
        UUID playerId = this.getPlayerId(player);
        return playerId == null ? null : this.players.get(playerId);
    }

    public void removePlayer(Player player) {
        this.removePlayer(player.getUUID());
    }

    public void removePlayer(UUID player) {
        this.players.remove(player);
    }
}
