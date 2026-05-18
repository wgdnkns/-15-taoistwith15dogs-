package com.wgdnkns.taoist.mixin;

import com.wgdnkns.taoist.Taoistwith15dogs;
import com.wgdnkns.taoist.item.TalismanControl;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void isSunBurnTick(CallbackInfoReturnable<Boolean> cir) {
        Mob mob = (Mob) (Object) this;
        if (!(mob instanceof Zombie || mob instanceof AbstractSkeleton)) {
            return;
        }
        if (TalismanControl.isControlledByAnyPlayer(mob, Taoistwith15dogs.YELLOW_TALISMAN.get())) {
            cir.setReturnValue(false);
        }
    }
}

