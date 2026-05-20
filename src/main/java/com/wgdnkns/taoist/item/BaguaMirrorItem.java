package com.wgdnkns.taoist.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.wgdnkns.taoist.Taoistwith15dogs;

public class BaguaMirrorItem extends Item {
    private static final double FEAR_RANGE = 10.0;

    public BaguaMirrorItem(Properties properties) {
        super(properties);
    }

    public static boolean isHoldingMirror(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.is(Taoistwith15dogs.BAGUA_MIRROR.get()) || off.is(Taoistwith15dogs.BAGUA_MIRROR.get());
    }

    public static void scareUndead(Player player) {
        if (!isHoldingMirror(player)) return;
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        for (Mob mob : serverLevel.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(FEAR_RANGE))) {
            if (mob.getType().is(EntityTypeTags.UNDEAD)) {
                mob.setTarget(null);
                mob.getNavigation().moveTo(
                        mob.getX() + (mob.getX() - player.getX()) * 2,
                        mob.getY(),
                        mob.getZ() + (mob.getZ() - player.getZ()) * 2,
                        1.5
                );
            }
        }
    }
}
