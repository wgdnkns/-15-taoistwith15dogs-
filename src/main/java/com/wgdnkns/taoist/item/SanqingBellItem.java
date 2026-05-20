package com.wgdnkns.taoist.item;

import com.wgdnkns.taoist.Taoistwith15dogs;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.UUID;

public class SanqingBellItem extends Item {
    private static final double COMMAND_RANGE = 32.0;
    private static final int COMMAND_COOLDOWN_TICKS = 20;

    public SanqingBellItem(Properties properties) {
        super(properties);
    }

    private static boolean isAnyoneAttackingThis(Player player, Level level, LivingEntity target) {
        UUID targetUUID = target.getUUID();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(COMMAND_RANGE))) {
            if (!TalismanControl.isControlledByPlayer(mob, Taoistwith15dogs.YELLOW_TALISMAN.get(), player.getUUID()))
                continue;
            UUID cmd = TalismanControl.getCommandedTargetUuid(mob);
            if (targetUUID.equals(cmd)) {
                return true;
            }
        }
        return false;
    }

    private static void cancelAllAttacks(Player player, Level level) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(COMMAND_RANGE))) {
            if (!TalismanControl.isControlledByPlayer(mob, Taoistwith15dogs.YELLOW_TALISMAN.get(), player.getUUID()))
                continue;
            TalismanControl.clearCommandTargetTags(mob);
            mob.setTarget(null);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player,
                entity -> entity instanceof LivingEntity && entity != player, COMMAND_RANGE);
        if (!(hit instanceof EntityHitResult entityHitResult)) {
            return InteractionResultHolder.pass(stack);
        }

        Entity hitEntity = entityHitResult.getEntity();
        if (!(hitEntity instanceof LivingEntity target)) {
            return InteractionResultHolder.pass(stack);
        }
        if (target instanceof Player) {
            return InteractionResultHolder.pass(stack);
        }
        // 被控制生物不能作为目标
        if (target instanceof Mob && target.getTags().contains(TalismanControl.TALISMAN_TAG)) {
            return InteractionResultHolder.pass(stack);
        }

        if (isAnyoneAttackingThis(player, level, target)) {
            cancelAllAttacks(player, level);
            player.displayClientMessage(
                    Component.translatable("message.taoist_with_15_dogs.cancel_mass_attack"), true);
            player.swing(hand, true);
            player.getCooldowns().addCooldown(this, COMMAND_COOLDOWN_TICKS);
            return InteractionResultHolder.consume(stack);
        }

        var controlled = new ArrayList<Mob>();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(COMMAND_RANGE))) {
            if (!TalismanControl.isControllableType(mob)) continue;
            if (!TalismanControl.isControlledByPlayer(mob, Taoistwith15dogs.YELLOW_TALISMAN.get(), player.getUUID()))
                continue;
            if (mob == target) continue;
            controlled.add(mob);
        }
        if (controlled.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.taoist_with_15_dogs.no_followers"), true);
            return InteractionResultHolder.pass(stack);
        }

        TalismanControl.setCommandedTarget(player, target.getUUID());
        for (Mob mob : controlled) {
            TalismanControl.setCommandedTarget(mob, target.getUUID());
            mob.setTarget(target);
            mob.getNavigation().moveTo(target, 1.2);
        }
        player.displayClientMessage(
                Component.translatable("message.taoist_with_15_dogs.target_selected"), true);

        player.swing(hand, true);
        player.getCooldowns().addCooldown(this, COMMAND_COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }
}
