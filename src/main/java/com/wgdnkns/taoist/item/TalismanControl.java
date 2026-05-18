package com.wgdnkns.taoist.item;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayList;
import java.util.UUID;

public final class TalismanControl {
    public static final String TALISMAN_TAG = "taoist_with_15_dogs_talisman";
    public static final String OWNER_TAG_PREFIX = "taoist_with_15_dogs_owner_";
    public static final String COMMAND_TARGET_TAG_PREFIX = "taoist_with_15_dogs_cmd_";
    public static final String DOOR_GOAL_TAG = "taoist_with_15_dogs_open_door";
    private static final String DIG_LAST_BREAK_TICK = "taoist_with_15_dogs_dig_last_break";
    private static final int DIG_COOLDOWN_TICKS = 30;
    public static final ResourceLocation BUFF_HEALTH_ID = ResourceLocation.fromNamespaceAndPath("taoist_with_15_dogs", "talisman_health");
    public static final ResourceLocation BUFF_ATTACK_ID = ResourceLocation.fromNamespaceAndPath("taoist_with_15_dogs", "talisman_attack");
    public static final ResourceLocation BUFF_SPEED_ID = ResourceLocation.fromNamespaceAndPath("taoist_with_15_dogs", "talisman_speed");

    private TalismanControl() {
    }

    public static String ownerTag(UUID owner) {
        return OWNER_TAG_PREFIX + owner;
    }

    public static void clearOwnerTags(Entity entity) {
        for (String tag : new ArrayList<>(entity.getTags())) {
            if (tag.startsWith(OWNER_TAG_PREFIX)) {
                entity.removeTag(tag);
            }
        }
    }

    public static UUID getOwnerUuid(Entity entity) {
        for (String tag : entity.getTags()) {
            if (tag.startsWith(OWNER_TAG_PREFIX)) {
                String value = tag.substring(OWNER_TAG_PREFIX.length());
                try {
                    return UUID.fromString(value);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public static boolean isControlledByAnyPlayer(LivingEntity entity, Item talismanItem) {
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        return head.is(talismanItem) && entity.getTags().contains(TALISMAN_TAG) && getOwnerUuid(entity) != null;
    }

    public static boolean isControlledByPlayer(LivingEntity entity, Item talismanItem, UUID owner) {
        if (!isControlledByAnyPlayer(entity, talismanItem)) {
            return false;
        }
        UUID actual = getOwnerUuid(entity);
        return owner.equals(actual);
    }

    public static void clearCommandTargetTags(Entity entity) {
        for (String tag : new ArrayList<>(entity.getTags())) {
            if (tag.startsWith(COMMAND_TARGET_TAG_PREFIX)) {
                entity.removeTag(tag);
            }
        }
    }

    public static void setCommandedTarget(Entity entity, UUID target) {
        clearCommandTargetTags(entity);
        entity.addTag(COMMAND_TARGET_TAG_PREFIX + target);
    }

    public static UUID getCommandedTargetUuid(Entity entity) {
        for (String tag : entity.getTags()) {
            if (tag.startsWith(COMMAND_TARGET_TAG_PREFIX)) {
                String value = tag.substring(COMMAND_TARGET_TAG_PREFIX.length());
                try {
                    return UUID.fromString(value);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public static void applyBuffs(Mob mob) {
        applyPermanentModifier(mob.getAttribute(Attributes.MAX_HEALTH), BUFF_HEALTH_ID, 2.0);
        applyPermanentModifier(mob.getAttribute(Attributes.ATTACK_DAMAGE), BUFF_ATTACK_ID, 2.0);
        applyPermanentModifier(mob.getAttribute(Attributes.MOVEMENT_SPEED), BUFF_SPEED_ID, 0.05);
        mob.setHealth(mob.getMaxHealth());
    }

    public static void ensureCanOpenDoors(Mob mob) {
        if (mob.getTags().contains(DOOR_GOAL_TAG)) {
            return;
        }
        if (mob.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
            navigation.setCanPassDoors(true);
        }
        mob.goalSelector.addGoal(1, new OpenDoorGoal(mob, true));
        mob.addTag(DOOR_GOAL_TAG);
    }

    @SuppressWarnings("resource")
    public static void tickWitherSkeletonDig(Mob mob) {
        if (!(mob instanceof WitherSkeleton)) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (mob.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(false);
            navigation.setCanPassDoors(false);
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            clearDigTimer(mob);
            return;
        }

        if ((mob.tickCount & 9) == 0 && hasReachablePath(mob, target)) {
            clearDigTimer(mob);
            return;
        }

        BlockPos breakPos = findDigCandidate(level, mob, target);
        if (breakPos == null) {
            clearDigTimer(mob);
            return;
        }

        double tdx = target.getX() - (breakPos.getX() + 0.5);
        double tdz = target.getZ() - (breakPos.getZ() + 0.5);
        if (tdx * tdx + tdz * tdz > 64.0) {
            clearDigTimer(mob);
            return;
        }

        double dist = mob.distanceToSqr(breakPos.getX() + 0.5, breakPos.getY() + 0.5, breakPos.getZ() + 0.5);
        if (dist > 16.0) {
            mob.getNavigation().moveTo(breakPos.getX() + 0.5, breakPos.getY() + 0.5, breakPos.getZ() + 0.5, 1.2);
            return;
        }

        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(breakPos.getX() + 0.5, breakPos.getY() + 0.5, breakPos.getZ() + 0.5);

        long gameTime = level.getGameTime();
        var data = mob.getPersistentData();
        long lastBreak = data.getLong(DIG_LAST_BREAK_TICK);
        if (gameTime - lastBreak < DIG_COOLDOWN_TICKS) {
            return;
        }

        BlockState state = level.getBlockState(breakPos);
        if (state.isAir() || state.getDestroySpeed(level, breakPos) < 0.0F) {
            clearDigTimer(mob);
            return;
        }

        mob.swing(InteractionHand.MAIN_HAND);
        data.putLong(DIG_LAST_BREAK_TICK, gameTime);

        boolean destroyed;
        if (state.getBlock() instanceof DoorBlock) {
            destroyed = breakDoor(level, breakPos, state, mob);
        } else {
            destroyed = level.destroyBlock(breakPos, true, mob);
        }
        if (destroyed) {
            if (mob.getBbHeight() > 2.1F) {
                tryDestroyBlock(level, breakPos.above(), mob);
                tryDestroyBlock(level, breakPos.above(2), mob);
            }
            if (target.isAlive()) {
                double dy = target.getY() - mob.getY();
                if (dy >= 1.5) {
                    BlockPos belowTarget = target.blockPosition().below();
                    if (mob.distanceToSqr(belowTarget.getX() + 0.5, belowTarget.getY() + 0.5, belowTarget.getZ() + 0.5) <= 64.0) {
                        tryDestroyBlock(level, belowTarget, mob);
                        tryDestroyBlock(level, belowTarget.below(), mob);
                    }
                }
            }
        }
    }

    private static BlockPos findDigCandidate(ServerLevel level, Mob mob, LivingEntity target) {
        if (target == null) {
            return null;
        }

        double dy = target.getY() - mob.getY();
        if (dy >= 1.5) {
            BlockPos belowTarget = target.blockPosition().below();
            BlockState support = level.getBlockState(belowTarget);
            if (!support.isAir() && support.getDestroySpeed(level, belowTarget) >= 0.0F) {
                if (mob.distanceToSqr(belowTarget.getX() + 0.5, belowTarget.getY() + 0.5, belowTarget.getZ() + 0.5) <= 64.0) {
                    return belowTarget;
                }
            }
        }

        Vec3 from = mob.getEyePosition();
        Vec3 toEye = target.getEyePosition();
        Vec3 toMid = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        Vec3 toFeet = target.position().add(0.0, 0.1, 0.0);

        BlockPos feetHit = firstDestroyableBlockOnRay(level, mob, from, toFeet);
        BlockPos midHit = firstDestroyableBlockOnRay(level, mob, from, toMid);
        BlockPos eyeHit = firstDestroyableBlockOnRay(level, mob, from, toEye);
        return closestToMob(mob, feetHit, midHit, eyeHit);
    }

    private static BlockPos firstDestroyableBlockOnRay(ServerLevel level, Mob mob, Vec3 from, Vec3 to) {
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return null;
        }
        return pos;
    }

    private static BlockPos closestToMob(Mob mob, BlockPos a, BlockPos b, BlockPos c) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        if (a != null) {
            best = a;
            bestDist = mob.distanceToSqr(a.getX() + 0.5, a.getY() + 0.5, a.getZ() + 0.5);
        }
        if (b != null) {
            double d = mob.distanceToSqr(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5);
            if (d < bestDist) {
                best = b;
                bestDist = d;
            }
        }
        if (c != null) {
            double d = mob.distanceToSqr(c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5);
            if (d < bestDist) {
                best = c;
            }
        }
        return best;
    }

    private static boolean hasReachablePath(Mob mob, LivingEntity target) {
        Path path = mob.getNavigation().createPath(target, 0);
        if (path == null) {
            return false;
        }
        if (path.canReach()) {
            return true;
        }
        Node node = path.getEndNode();
        if (node == null) {
            return false;
        }
        int i = node.x - target.getBlockX();
        int j = node.z - target.getBlockZ();
        return (double) (i * i + j * j) <= 2.25;
    }

    private static void clearDigTimer(Mob mob) {
        mob.getPersistentData().remove(DIG_LAST_BREAK_TICK);
    }

    private static boolean breakDoor(ServerLevel level, BlockPos pos, BlockState state, Mob breaker) {
        if (!(state.getBlock() instanceof DoorBlock)) {
            return false;
        }
        BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockPos upper = lower.above();
        boolean destroyed = false;
        BlockState lowerState = level.getBlockState(lower);
        if (lowerState.getBlock() instanceof DoorBlock) {
            destroyed |= level.destroyBlock(lower, false, breaker);
        }
        BlockState upperState = level.getBlockState(upper);
        if (upperState.getBlock() instanceof DoorBlock) {
            destroyed |= level.destroyBlock(upper, false, breaker);
        }
        return destroyed;
    }

    private static void tryDestroyBlock(ServerLevel level, BlockPos pos, Mob breaker) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return;
        }
        level.destroyBlock(pos, false, breaker);
    }

    private static void applyPermanentModifier(AttributeInstance instance, ResourceLocation id, double amount) {
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }
}
