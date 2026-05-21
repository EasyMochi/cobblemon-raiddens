package com.necro.raid.dens.common.data.raid;

import com.necro.raid.dens.common.CobblemonRaidDens;
import com.necro.raid.dens.common.raids.helpers.RaidHelper;
import net.minecraft.nbt.CompoundTag;

public record RaidResetContext(long gameTime, long systemTime, long globalCycle) {
    public RaidResetContext(long gameTime) {
        this(gameTime, currentGlobalCycle());
    }

    public RaidResetContext(long gameTime, long globalCycle) {
        this(gameTime, System.currentTimeMillis(), globalCycle);
    }

    public CompoundTag save(CompoundTag compoundTag) {
        compoundTag.putLong("game_time", this.gameTime);
        compoundTag.putLong("system_time", this.systemTime);
        compoundTag.putLong("global_cycle", this.globalCycle);
        return compoundTag;
    }

    public long nextReset(long gameTime) {
        return switch (CobblemonRaidDens.CONFIG.reset_mode) {
            case GAME_TIME -> Math.max(0L, CobblemonRaidDens.CONFIG.reset_time * 20L - (gameTime - this.gameTime));
            case SYSTEM_TIME -> Math.max(0L, CobblemonRaidDens.CONFIG.reset_time * 1000L - (System.currentTimeMillis() - this.systemTime)) / 50L;
            case GLOBAL_GAME_TIME -> Math.max(0L, CobblemonRaidDens.CONFIG.reset_time * 20L - (gameTime - currentGlobalGameTime()));
            case GLOBAL_SYSTEM_TIME -> Math.max(0L, CobblemonRaidDens.CONFIG.reset_time * 1000L - (System.currentTimeMillis() - currentGlobalSystemTime())) / 50L;
        };
    }

    public boolean shouldReset(long gameTime) {
        return switch (CobblemonRaidDens.CONFIG.reset_mode) {
            case GAME_TIME -> gameTime - this.gameTime > CobblemonRaidDens.CONFIG.reset_time * 20L;
            case SYSTEM_TIME -> System.currentTimeMillis() - this.systemTime > CobblemonRaidDens.CONFIG.reset_time * 1000L;
            default -> currentGlobalCycle() > this.globalCycle;
        };
    }

    public static RaidResetContext load(CompoundTag compoundTag) {
        return load(compoundTag, 0L);
    }

    public static RaidResetContext load(CompoundTag compoundTag, long fallbackGameTime) {
        if (compoundTag == null || compoundTag.isEmpty()) return new RaidResetContext(fallbackGameTime);
        long gameTime = compoundTag.contains("game_time") ? compoundTag.getLong("game_time") : fallbackGameTime;
        long systemTime = compoundTag.contains("system_time") ? compoundTag.getLong("system_time") : System.currentTimeMillis();
        long globalCycle = compoundTag.contains("global_cycle") ? compoundTag.getLong("global_cycle") : currentGlobalCycle();
        return new RaidResetContext(gameTime, systemTime, globalCycle);
    }

    private static long currentGlobalCycle() {
        return RaidHelper.INSTANCE == null ? 0L : RaidHelper.getGlobalCycle().globalCycle();
    }

    private static long currentGlobalGameTime() {
        return RaidHelper.INSTANCE == null ? 0L : RaidHelper.getGlobalCycle().gameTime();
    }

    private static long currentGlobalSystemTime() {
        return RaidHelper.INSTANCE == null ? System.currentTimeMillis() : RaidHelper.getGlobalCycle().systemTime();
    }
}
