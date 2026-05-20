package com.wgdnkns.taoist.item;

import com.wgdnkns.taoist.Taoistwith15dogs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
        if (!mob.getType().is(EntityTypeTags.UNDEAD)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack currentTalisman = TalismanControl.getTalismanItem(mob);
        boolean hasOtherInPersist = !currentTalisman.isEmpty() && !currentTalisman.is(this);
        boolean hasOtherInHead = !mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && !mob.getItemBySlot(EquipmentSlot.HEAD).is(this);
        if (hasOtherInPersist || hasOtherInHead) {
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

        TalismanControl.setTalismanItem(mob, placed);

        // 保存生物自带的装备到背包（先读取已有内容, 再补充）
        var data = mob.getPersistentData();
        ListTag existingList = new ListTag();
        if (data.contains(TalismanControl.INVENTORY_TAG, Tag.TAG_LIST)) {
            existingList = data.getList(TalismanControl.INVENTORY_TAG, Tag.TAG_COMPOUND);
        } else {
            for (int i = 0; i < 27; i++) existingList.add(new CompoundTag());
        }

        // 把黄符放进第一个空位
        for (int i = 0; i < existingList.size(); i++) {
            if (existingList.getCompound(i).isEmpty()) {
                existingList.set(i, placed.save(mob.registryAccess()));
                break;
            }
        }

        // 把生物当前装备（弓/盔甲等）也存进去, 如果背包里还没有的话
        for (var slot : EquipmentSlot.values()) {
            var eq = mob.getItemBySlot(slot);
            if (eq.isEmpty()) continue;
            if (eq.getItem() instanceof YellowTalismanItem) continue; // 黄符不存
            boolean found = false;
            for (int i = 0; i < existingList.size(); i++) {
                var tag = existingList.getCompound(i);
                if (tag.isEmpty()) continue;
                var existing = ItemStack.parse(mob.registryAccess(), tag).orElse(ItemStack.EMPTY);
                if (ItemStack.isSameItemSameComponents(existing, eq)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (int i = 0; i < existingList.size(); i++) {
                    if (existingList.getCompound(i).isEmpty()) {
                        existingList.set(i, eq.save(mob.registryAccess()));
                        break;
                    }
                }
            }
        }
        data.put(TalismanControl.INVENTORY_TAG, existingList);

        mob.setPersistenceRequired();
        mob.addTag(TalismanControl.TALISMAN_TAG);
        TalismanControl.clearOwnerTags(mob);
        mob.addTag(TalismanControl.ownerTag(player.getUUID()));
        TalismanControl.applyBuffs(mob);
        mob.setTarget(null);
        return InteractionResult.CONSUME;
    }
}
