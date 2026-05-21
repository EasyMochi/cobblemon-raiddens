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

    public Player getPlayer(String player) {
        for (Player value : this.players.values()) {
            if (value.getName().getString().equals(player)) return value;
        }
        return null;
    }

    public void removePlayer(Player player) {
        this.players.remove(player.getUUID());
    }
}