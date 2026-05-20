package com.wgdnkns.taoist.mixin;

import com.wgdnkns.taoist.Taoistwith15dogs;
import com.wgdnkns.taoist.item.TalismanControl;
import net.minecraft.world.entity.PathfinderMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.Drowned$DrownedGoToWaterGoal")
public abstract class DrownedMixin {

    @Accessor("mob")
    public abstract PathfinderMob getMob();

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void canUse(CallbackInfoReturnable<Boolean> cir) {
        if (TalismanControl.isControlledByAnyPlayer(getMob(), Taoistwith15dogs.YELLOW_TALISMAN.get())) {
            cir.setReturnValue(false);
        }
    }
}
