package com.wgdnkns.taoist.mixin;

import com.wgdnkns.taoist.Taoistwith15dogs;
import com.wgdnkns.taoist.item.TalismanControl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Drowned;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Drowned.class)
public abstract class DrownedOkTargetMixin {
    @Inject(method = "okTarget", at = @At("HEAD"), cancellable = true)
    private void okTarget(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        Drowned drowned = (Drowned) (Object) this;
        if (TalismanControl.isControlledByAnyPlayer(drowned, Taoistwith15dogs.YELLOW_TALISMAN.get())) {
            cir.setReturnValue(true);
        }
    }
}
