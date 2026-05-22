package com.necro.raid.dens.common.util;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.activestate.ActivePokemonState;
import com.cobblemon.mod.common.pokemon.activestate.ShoulderedState;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import com.necro.raid.dens.common.CobblemonRaidDens;
import com.necro.raid.dens.common.blocks.BlockTags;
import com.necro.raid.dens.common.blocks.entity.RaidCrystalBlockEntity;
import com.necro.raid.dens.common.components.ModComponents;
import com.necro.raid.dens.common.data.dimension.RaidRegion;
import com.necro.raid.dens.common.dimensions.ModDimensions;
import com.necro.raid.dens.common.items.ItemTags;
import com.necro.raid.dens.common.network.RaidDenNetworkMessages;
import com.necro.raid.dens.common.raids.RaidInstance;
import com.necro.raid.dens.common.raids.RaidState;
import com.necro.raid.dens.common.raids.helpers.RaidHelper;
import com.necro.raid.dens.common.raids.helpers.RaidJoinHelper;
import com.necro.raid.dens.common.raids.helpers.RaidRegionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RaidUtils {
    private static final Set<String> POKEMON_BLACKLIST = new HashSet<>();
    private static final Set<String> ABILITY_BLACKLIST = new HashSet<>();
    private static final Set<String> HELD_ITEM_BLACKLIST = new HashSet<>();
    private static final Set<String> MOVE_BLACKLIST = new HashSet<>();
    private static final Set<String> COMMAND_BLACKLIST = new HashSet<>();
    private static int MAX_COMMAND_SPLIT;

    public static boolean isPokemonBlacklisted(Pokemon pokemon) {
        return POKEMON_BLACKLIST.contains(pokemon.getSpecies().getName().toLowerCase());
    }

    public static boolean isAbilityBlacklisted(Ability ability) {
        return ABILITY_BLACKLIST.contains(ability.getName());
    }

    public static boolean isHeldItemBlacklisted(@Nullable ItemStack item) {
        if (item == null) return false;
        return HELD_ITEM_BLACKLIST.contains(item.getItem().toString());
    }

    public static boolean isMoveBlacklisted(String move) {
        return MOVE_BLACKLIST.contains(move);
    }

    public static boolean isCommandBlacklisted(String command) {
        return COMMAND_BLACKLIST.contains(command);
    }

    public static int getMaxCommandSplit() {
        return MAX_COMMAND_SPLIT;
    }

    public static boolean hasSkyAccess(LevelReader level, BlockPos blockPos) {
        return level.canSeeSky(blockPos);
    }

    public static void teleportPlayerToRaid(ServerPlayer player, MinecraftServer server, RaidRegion region) {
        ServerLevel level = ModDimensions.getRaidDimension(server);
        if (level == null) return;

        ((IRaidTeleporter) player).crd_setHomePos(getSafeTeleportPos(player.level(), player.position()));
        ((IRaidTeleporter) player).crd_setHomeLevel(player.level().dimension().location());

        Vec3 playerPos = region.getPlayerPos();
        teleportPlayerSafe(player, level, playerPos, 180f, 0f);
    }

    public static boolean rescueFromRaidDimension(ServerPlayer player) {
        if (player.getServer() == null || !isRaidDimension(player.level())) return false;

        RaidDenNetworkMessages.JOIN_RAID.accept(player, false);
        RaidHelper.removeRequests(player.getUUID());
        RaidJoinHelper.removeParticipant(player);

        ServerLevel targetLevel = null;
        Vec3 targetPos = null;
        if (player instanceof IRaidTeleporter teleporter) {
            targetLevel = teleporter.crd_getHomeLevel();
            targetPos = teleporter.crd_getHomePos();
        }

        if (targetLevel == null || isRaidDimension(targetLevel)) {
            targetLevel = player.getServer().overworld();
            targetPos = Vec3.atBottomCenterOf(targetLevel.getSharedSpawnPos());
        }
        else if (targetPos == null) {
            targetPos = Vec3.atBottomCenterOf(targetLevel.getSharedSpawnPos());
        }

        teleportPlayerSafe(player, targetLevel, getSafeTeleportPos(targetLevel, targetPos), player.getYRot(), player.getXRot());
        if (player instanceof IRaidTeleporter teleporter) teleporter.crd_clearHome();
        return true;
    }

    public static void teleportPlayerSafe(ServerPlayer player, ServerLevel level, Vec3 targetPos, float yaw, float pitch) {
        PlayerExtensionsKt.party(player).forEach(pokemon -> {
            if (pokemon.getState() instanceof ActivePokemonState && !(pokemon.getState() instanceof ShoulderedState)) pokemon.recall();
        });

        player.stopRiding();
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(level, targetPos.x(), targetPos.y(), targetPos.z(), new HashSet<>(), yaw, pitch);
    }

    public static Vec3 getSafeTeleportPos(LevelReader level, Vec3 targetPos) {
        BlockPos targetBlockPos = BlockPos.containing(targetPos);
        if (isSafeStandingPos(level, targetBlockPos)) return targetPos;

        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 2;
        int startY = Math.max(minY, Math.min(maxY, targetBlockPos.getY()));
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(targetBlockPos.getX(), startY, targetBlockPos.getZ());

        for (int y = startY; y >= minY; y--) {
            mutable.set(targetBlockPos.getX(), y, targetBlockPos.getZ());
            if (isSafeStandingPos(level, mutable)) return Vec3.atBottomCenterOf(mutable);
        }

        return targetPos;
    }

    private static boolean isSafeStandingPos(LevelReader level, BlockPos blockPos) {
        BlockState feet = level.getBlockState(blockPos);
        BlockState head = level.getBlockState(blockPos.above());
        BlockState below = level.getBlockState(blockPos.below());
        return feet.getCollisionShape(level, blockPos).isEmpty()
            && head.getCollisionShape(level, blockPos.above()).isEmpty()
            && below.isFaceSturdy(level, blockPos.below(), Direction.UP);
    }

    public static void leaveRaid(Player player) {
        RaidDenNetworkMessages.JOIN_RAID.accept((ServerPlayer) player, false);

        RaidJoinHelper.Participant participant = RaidJoinHelper.getParticipant(player);
        if (participant == null) return;
        UUID raid = participant.raid();

        RaidRegion region = RaidRegionHelper.getRegion(raid);
        if (region == null) {
            leaveRaidFallback(player);
            return;
        }

        ServerLevel level = ModDimensions.getRaidDimension(player.getServer());
        if (level == null) return;

        RaidInstance instance = RaidHelper.ACTIVE_RAIDS.get(raid);
        if (instance == null) {
            leaveRaidFallback(player);
            return;
        }
        else if (instance.getRaidState() != RaidState.NOT_STARTED && BattleRegistry.getBattleByParticipatingPlayer((ServerPlayer) player) != null) {
            instance.removePlayer((ServerPlayer) player);
            RaidJoinHelper.removeParticipant(player);
            return;
        }

        int players = level.getEntitiesOfClass(Player.class, region.bound(), p -> RaidJoinHelper.isParticipating(p, false)).size();
        if (players > (isRaidDimension(player.level()) ? 1 : 0)) {
            RaidJoinHelper.removeParticipant(player);
            return;
        }

        int items = level.getEntitiesOfClass(ItemEntity.class, region.bound(), item -> item.getOwner() != null).size();
        if (items > 0 && isRaidDimension(player.level())) {
            RaidJoinHelper.removeParticipant(player);
            return;
        }

        instance.removePlayer((ServerPlayer) player);
        instance.closeRaid(player.getServer());

        PokemonEntity entity = instance.getBossEntity();
        if (!entity.isRemoved()) ((IRaidAccessor) entity).crd_flagForRemoval();
    }

    private static void leaveRaidFallback(Player player) {
        RaidHelper.removeRequests(player.getUUID());
        RaidJoinHelper.removeParticipant(player);
    }

    public static boolean isRaidDenKey(ItemStack itemStack) {
        return itemStack.is(ItemTags.RAID_DEN_KEY) || itemStack.getOrDefault(ModComponents.RAID_DEN_KEY.value(), false);
    }

    public static boolean isRaidDimension(Level level) {
        return level.dimensionTypeRegistration().is(ModDimensions.RAID_DIM_TYPE);
    }

    public static boolean cannotBreak(Player player, Level level) {
        return RaidUtils.isRaidDimension(level) && !player.isCreative();
    }

    @SuppressWarnings("unused")
    public static boolean cannotPlace(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        return RaidUtils.isRaidDimension(level) && !player.isCreative() && !(level.getBlockState(hitResult.getBlockPos()).is(BlockTags.CAN_INTERACT));
    }

    @SuppressWarnings("unused")
    public static boolean canBreak(Level level, Player player, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
        if (RaidUtils.cannotBreak(player, level)) return false;
        else return !(blockEntity instanceof RaidCrystalBlockEntity raidCrystal) || raidCrystal.getPlayerCount() == 0;
    }

    public static InteractionResult canPlace(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        return RaidUtils.cannotPlace(player, level, hand, hitResult) ? InteractionResult.FAIL : InteractionResult.PASS;
    }

    public static void init() {
        POKEMON_BLACKLIST.addAll(List.of(CobblemonRaidDens.BLACKLIST_CONFIG.pokemon));
        ABILITY_BLACKLIST.addAll(List.of(CobblemonRaidDens.BLACKLIST_CONFIG.abilities));
        HELD_ITEM_BLACKLIST.addAll(List.of(CobblemonRaidDens.BLACKLIST_CONFIG.held_items));
        MOVE_BLACKLIST.addAll(List.of(CobblemonRaidDens.BLACKLIST_CONFIG.moves));
        COMMAND_BLACKLIST.addAll(List.of(CobblemonRaidDens.BLACKLIST_CONFIG.commands));

        int maxSplit = 1;
        for (String command : COMMAND_BLACKLIST) {
            maxSplit = Math.max(maxSplit, command.split(" ").length);
        }
        MAX_COMMAND_SPLIT = Math.min(maxSplit, 4);
    }
}
