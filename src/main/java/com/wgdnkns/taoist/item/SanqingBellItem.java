package com.wgdnkns.taoist.item;

import com.wgdnkns.taoist.Taoistwith15dogs;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.MethodsReturnNonnullByDefault;
import javax.annotation.ParametersAreNonnullByDefault;

import java.util.ArrayList;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SanqingBellItem extends Item {
    private static final double COMMAND_RANGE = 32.0;
    private static final int COMMAND_COOLDOWN_TICKS = 10;

    public SanqingBellItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player, entity -> entity instanceof LivingEntity && entity != player, COMMAND_RANGE);
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

        var controlled = new ArrayList<Mob>();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(COMMAND_RANGE))) {
            if (!(mob instanceof Zombie || mob instanceof AbstractSkeleton)) {
                continue;
            }
            if (!TalismanControl.isControlledByPlayer(mob, Taoistwith15dogs.YELLOW_TALISMAN.get(), player.getUUID())) {
                continue;
            }
            if (mob == target) {
                continue;
            }
            controlled.add(mob);
        }

        if (controlled.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }

        TalismanControl.setCommandedTarget(player, target.getUUID());
        for (Mob mob : controlled) {
            TalismanControl.setCommandedTarget(mob, target.getUUID());
            mob.setTarget(target);
        }

        player.swing(hand, true);
        player.getCooldowns().addCooldown(this, COMMAND_COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }
}
