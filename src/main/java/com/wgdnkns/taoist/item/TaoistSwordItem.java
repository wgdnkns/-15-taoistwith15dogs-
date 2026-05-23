package com.wgdnkns.taoist.item;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TaoistSwordItem extends Item {
    private static final float EXTRA_UNDEAD_DAMAGE = 3.0F;

    public TaoistSwordItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.getType().is(EntityTypeTags.UNDEAD)) {
            target.hurt(attacker.damageSources().mobAttack(attacker), EXTRA_UNDEAD_DAMAGE);
        }
        return true;
    }
}
