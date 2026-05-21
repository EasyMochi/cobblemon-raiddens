package com.necro.raid.dens.common.showdown.events;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.runner.ShowdownService;
import com.necro.raid.dens.common.CobblemonRaidDens;

public interface ShowdownEvent extends AbstractEvent {
    @Override
    default void execute(RaidContext context) {
        if (context.battle() == null) return;
        send(context.battle());
    }

    String build(PokemonBattle battle);

    default void send(PokemonBattle battle) {
        String message = this.build(battle);
        if (message == null || message.isBlank()) return;

        try {
            ShowdownService.Companion.getService().send(battle.getBattleId(), new String[]{message});
        }
        catch (Exception e) {
            CobblemonRaidDens.LOGGER.warn("Failed to send Showdown event {} for battle {}", this.getClass().getSimpleName(), battle.getBattleId(), e);
        }
    }
}
