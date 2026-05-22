package com.necro.raid.dens.common.registry;

import com.necro.raid.dens.common.CobblemonRaidDens;
import com.necro.raid.dens.common.structure.RaidDenPool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaidDenRegistry {
    private static final Map<ResourceLocation, RaidDenPool> DEN_POOL = new HashMap<>();

    private static final Map<ResourceLocation, RaidStructureData> TEMPLATES = new HashMap<>();
    public static final ResourceLocation DEFAULT = ResourceLocation.fromNamespaceAndPath(CobblemonRaidDens.MOD_ID, "raid_den/basic");
    private static final RaidStructureData EMPTY_TEMPLATE = new RaidStructureData(Vec3.ZERO, Vec3.ZERO, Vec3.ZERO);

    public static void register(RaidDenPool pool) {
        if (DEN_POOL.containsKey(pool.getId())) DEN_POOL.get(pool.getId()).addDens(pool);
        else DEN_POOL.put(pool.getId(), pool);
    }

    public static void register(ResourceLocation structure, CompoundTag tag) {
        TEMPLATES.put(structure, new RaidStructureData(tag));
    }

    public static List<ResourceLocation> getStructures(ResourceLocation pool) {
        if (!DEN_POOL.containsKey(pool)) return List.of();
        else return DEN_POOL.get(pool).getDens();
    }

    public static boolean isNotValidStructure(ResourceLocation structure) {
        return !TEMPLATES.containsKey(structure);
    }

    public static Vec3 getOffset(ResourceLocation structure) {
        return getTemplateData(structure).offset;
    }

    public static Vec3 getPlayerPos(ResourceLocation structure) {
        return getTemplateData(structure).playerPos;
    }

    public static Vec3 getBossPos(ResourceLocation structure) {
        return getTemplateData(structure).bossPos;
    }

    private static RaidStructureData getTemplateData(ResourceLocation structure) {
        RaidStructureData data = TEMPLATES.get(structure);
        if (data != null) return data;

        RaidStructureData fallback = TEMPLATES.get(DEFAULT);
        if (fallback != null) return fallback;

        CobblemonRaidDens.LOGGER.error("Missing fallback raid den template data for {}", DEFAULT);
        return EMPTY_TEMPLATE;
    }

    public static void clearTemplates() {
        TEMPLATES.clear();
    }

    public static void clearDenPools() {
        DEN_POOL.clear();
    }

    public static void clear() {
        clearTemplates();
    }

    private static class RaidStructureData {
        private final Vec3 offset;
        private final Vec3 playerPos;
        private final Vec3 bossPos;

        private RaidStructureData(Vec3 offset, Vec3 playerPos, Vec3 bossPos) {
            this.offset = offset;
            this.playerPos = playerPos;
            this.bossPos = bossPos;
        }

        private RaidStructureData(CompoundTag tag) {
            this(
                new Vec3(tag.getDouble("offset_x"), tag.getDouble("offset_y"), tag.getDouble("offset_z")),
                new Vec3(tag.getDouble("player_x"), tag.getDouble("player_y"), tag.getDouble("player_z")),
                new Vec3(tag.getDouble("boss_x"), tag.getDouble("boss_y"), tag.getDouble("boss_z"))
            );
        }
    }
}
