package com.wgdnkns.taoist.mixin;

import com.wgdnkns.taoist.Taoistwith15dogs;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private static final float EXTRA_DAMAGE_UNDEAD = 12.5F;

    /**
     * 在 {@link LivingEntity#hurt(DamageSource, float)} 方法执行前，
     * 修改传入的伤害值 amount。
     */
    @ModifyVariable(
            method = "hurt",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float modifyHurtDamage(float amount, DamageSource source, float originalAmount) {
        // 获取直接攻击者（可能是箭、三叉戟等，但我们需要手持物品的攻击者）
        // 注意：source.getDirectEntity() 可能是箭，source.getEntity() 是发射者
        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            // 判断武器是否是桃木剑（通过注册名比较，避免直接引用主类静态字段可能引起的类加载问题）
            if (weapon.is(Taoistwith15dogs.TAOIST_SWORD.get())) {
                LivingEntity target = (LivingEntity) (Object) this;
                if (target.getType().is(EntityTypeTags.UNDEAD)) {
                    return amount + EXTRA_DAMAGE_UNDEAD;
                }
            }
        }
        return amount;
    }
}