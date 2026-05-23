package com.wgdnkns.taoist.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CinnabarItem extends Item {
    public CinnabarItem(Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(0)
                .saturationModifier(0)
                .effect(() -> new MobEffectInstance(MobEffects.POISON, 100, 0), 1.0F)
                .alwaysEdible()
                .build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.taoist_with_15_dogs.cinnabar_eat"), true);
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
