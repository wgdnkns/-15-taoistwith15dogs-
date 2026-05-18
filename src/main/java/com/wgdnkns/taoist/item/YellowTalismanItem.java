package com.wgdnkns.taoist.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class YellowTalismanItem extends Item {
    public YellowTalismanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Mob mob)) {
            return InteractionResult.PASS;
        }
        if (!(mob instanceof Zombie || mob instanceof AbstractSkeleton)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && !mob.getItemBySlot(EquipmentSlot.HEAD).is(this)) {
            return InteractionResult.PASS;
        }
        if (TalismanControl.isControlledByAnyPlayer(mob, this)) {
            return InteractionResult.CONSUME;
        }

        ItemStack placed;
        if (player.getAbilities().instabuild) {
            placed = new ItemStack(this);
        } else {
            placed = stack.split(1);
            if (placed.isEmpty()) {
                return InteractionResult.CONSUME;
            }
        }

        mob.setItemSlot(EquipmentSlot.HEAD, placed);
        mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
        mob.setPersistenceRequired();
        mob.addTag(TalismanControl.TALISMAN_TAG);
        TalismanControl.clearOwnerTags(mob);
        mob.addTag(TalismanControl.ownerTag(player.getUUID()));
        TalismanControl.applyBuffs(mob);
        mob.setTarget(null);
        return InteractionResult.CONSUME;
    }
}
