package com.wgdnkns.taoist.client.gui;

import com.wgdnkns.taoist.Taoistwith15dogs;
import com.wgdnkns.taoist.item.TalismanControl;
import com.wgdnkns.taoist.item.YellowTalismanItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;

/**
 * 符兵装备菜单 (3×9 = 27格生物箱子)
 * =====================================
 * 背景: shulker_box.png (176×167)
 *
 * 【持久化原理 - 防止双击落】
 *   save()  → 27格全部存 INVENTORY_TAG + 自动穿戴(仅护甲/武器上手)
 *   loadFrom() → 仅从 INVENTORY_TAG 读取, 不从装备槽补
 *   → 物品只存在于 INVENTORY_TAG 中, 干净双向
 *
 * 【自动穿戴规则】
 *   黄符 → PersistentData (叠穿, 不影响HEAD)
 *   护甲 → 各类型最多1件穿上
 *   武器(Sword/Digger/ProjectileWeapon/Trident) → 优先主手, 次副手
 *   其他杂货 → 不装备, 只存在27格中
 */
public class FollowerMenu extends AbstractContainerMenu {
    public static final int FOLLOWER_SLOT_COUNT = 27;

    private static final int CONTAINER_LEFT = 8;
    private static final int CONTAINER_TOP  = 18;
    private static final int SLOT = 18;
    private static final int PLAYER_INV_TOP    = 84;
    private static final int PLAYER_HOTBAR_TOP = 142;

    private final FollowerContainer followerContainer;
    private final Mob follower;

    public FollowerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new FollowerContainer(), null);
    }

    public FollowerMenu(int containerId, Inventory playerInventory, FollowerContainer followerContainer, Mob follower) {
        super(Taoistwith15dogs.FOLLOWER_MENU.get(), containerId);
        this.followerContainer = followerContainer;
        this.follower = follower;

        checkContainerSize(followerContainer, FOLLOWER_SLOT_COUNT);
        followerContainer.startOpen(playerInventory.player);

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new FollowerSlot(followerContainer, col + row * 9, CONTAINER_LEFT + col * SLOT, CONTAINER_TOP + row * SLOT));

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + (row + 1) * 9, CONTAINER_LEFT + col * SLOT, PLAYER_INV_TOP + row * SLOT));

        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, CONTAINER_LEFT + col * SLOT, PLAYER_HOTBAR_TOP));
    }

    public static FollowerMenu createServerMenu(int containerId, Inventory playerInventory, Mob follower) {
        FollowerContainer container = new FollowerContainer();
        container.loadFrom(follower);
        return new FollowerMenu(containerId, playerInventory, container, follower);
    }

    public Mob getFollower() { return follower; }

    @Override public boolean stillValid(Player player) {
        if (follower == null) return true;
        return follower.isAlive() && player.distanceToSqr(follower) < 64.0;
    }

    @Override public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (follower != null && container == followerContainer) save();
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (follower != null) save();
        followerContainer.stopOpen(player);
    }

    /** 保存27格到INVENTORY_TAG + 自动穿戴护甲/武器 */
    private void save() {
        if (follower == null) return;

        // 清空装备槽
        follower.setItemSlot(EquipmentSlot.HEAD,    ItemStack.EMPTY);
        follower.setItemSlot(EquipmentSlot.CHEST,   ItemStack.EMPTY);
        follower.setItemSlot(EquipmentSlot.LEGS,    ItemStack.EMPTY);
        follower.setItemSlot(EquipmentSlot.FEET,    ItemStack.EMPTY);
        follower.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        follower.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        CompoundTag data = follower.getPersistentData();
        // 不清除 TALISMAN_SLOT_TAG, 扫完再决定

        boolean hasTalisman = false;
        ListTag list = new ListTag();
        for (int i = 0; i < FOLLOWER_SLOT_COUNT; i++) {
            ItemStack stack = followerContainer.getItem(i);
            if (stack.isEmpty()) {
                list.add(new CompoundTag());
                continue;
            }

            // 黄符 → 标记 + 正常存入list
            if (stack.getItem() instanceof YellowTalismanItem) {
                hasTalisman = true;
            }

            // 护甲 → 对应装备槽
            if (stack.getItem() instanceof ArmorItem armor) {
                EquipmentSlot slot = armor.getEquipmentSlot();
                if (follower.getItemBySlot(slot).isEmpty()) {
                    follower.setItemSlot(slot, stack.copy());
                }
            } else if (isWeaponOrTool(stack)) {
                // 武器 → 主手→副手
                if (follower.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
                    follower.setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
                } else if (follower.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
                    follower.setItemSlot(EquipmentSlot.OFFHAND, stack.copy());
                }
            }

            list.add(stack.save(follower.registryAccess()));
        }
        data.put(TalismanControl.INVENTORY_TAG, list);

        if (hasTalisman) {
            // 有黄符 → 同步 TALISMAN_SLOT_TAG
            for (int i = 0; i < FOLLOWER_SLOT_COUNT; i++) {
                ItemStack s = followerContainer.getItem(i);
                if (s.getItem() instanceof YellowTalismanItem) {
                    data.put(TalismanControl.TALISMAN_SLOT_TAG, s.save(follower.registryAccess()));
                    break;
                }
            }
        } else {
            // 黄符被取出 → 释放符兵, 保留 INVENTORY_TAG (物品不掉)
            data.remove(TalismanControl.TALISMAN_SLOT_TAG);
            TalismanControl.clearOwnerTags(follower);
            follower.removeTag(TalismanControl.TALISMAN_TAG);
            follower.setTarget(null);
            follower.getNavigation().stop();
            follower.setCustomName(null);
            follower.setCustomNameVisible(false);

            var healthAttr = follower.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (healthAttr != null) healthAttr.removeModifier(TalismanControl.BUFF_HEALTH_ID);
            var attackAttr = follower.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            if (attackAttr != null) attackAttr.removeModifier(TalismanControl.BUFF_ATTACK_ID);
            var speedAttr = follower.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) speedAttr.removeModifier(TalismanControl.BUFF_SPEED_ID);
            if (follower instanceof net.minecraft.world.entity.monster.Drowned drowned) {
                var drownedAttack = drowned.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                if (drownedAttack != null) drownedAttack.removeModifier(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("taoist_with_15_dogs", "drowned_attack"));
                var waterSpeed = drowned.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.WATER_MOVEMENT_EFFICIENCY);
                if (waterSpeed != null) waterSpeed.removeModifier(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("taoist_with_15_dogs", "drowned_water_speed"));
            }
        }
    }

    /** 判定是否为可装备到手上的武器/工具 */
    private static boolean isWeaponOrTool(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof SwordItem
                || item instanceof DiggerItem
                || item instanceof ProjectileWeaponItem
                || item instanceof TridentItem
                || item instanceof MaceItem;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack r = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            r = stack.copy();
            if (index < FOLLOWER_SLOT_COUNT) {
                if (!moveItemStackTo(stack, FOLLOWER_SLOT_COUNT, slots.size(), true)) return ItemStack.EMPTY;
            } else {
                // 从玩家背包到符兵容器: 检查每个目标槽的 mayPlace
                boolean moved = false;
                for (int i = 0; i < FOLLOWER_SLOT_COUNT; i++) {
                    if (slots.get(i).mayPlace(stack)) {
                        if (moveItemStackTo(stack, i, i + 1, false)) {
                            moved = true;
                            break;
                        }
                    }
                }
                if (!moved) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return r;
    }

    // ================================================================
    //  自定义槽: 检查容器的 canPlaceItem
    // ================================================================
    static class FollowerSlot extends Slot {
        FollowerSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }
    }

    // ================================================================
    //  自定义容器: 限制 + 持久化
    // ================================================================
    static class FollowerContainer extends SimpleContainer {
        FollowerContainer() { super(FOLLOWER_SLOT_COUNT); }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (stack.isEmpty()) return true;
            boolean hasHead = false, hasChest = false, hasLegs = false, hasFeet = false;
            int weaponCount = 0, talismanCount = 0;

            for (int i = 0; i < getContainerSize(); i++) {
                if (i == slot) continue;
                ItemStack existing = getItem(i);
                if (existing.isEmpty()) continue;

                if (existing.getItem() instanceof YellowTalismanItem) {
                    talismanCount++;
                } else if (existing.getItem() instanceof ArmorItem a) {
                    switch (a.getEquipmentSlot()) {
                        case HEAD  -> hasHead = true;
                        case CHEST -> hasChest = true;
                        case LEGS  -> hasLegs = true;
                        case FEET  -> hasFeet = true;
                    }
                } else if (isWeaponOrTool(existing)) {
                    weaponCount++;
                }
            }

            if (stack.getItem() instanceof YellowTalismanItem) return talismanCount < 1;
            if (stack.getItem() instanceof ArmorItem a) {
                return switch (a.getEquipmentSlot()) {
                    case HEAD  -> !hasHead;
                    case CHEST -> !hasChest;
                    case LEGS  -> !hasLegs;
                    case FEET  -> !hasFeet;
                    default -> false;
                };
            }
            if (isWeaponOrTool(stack)) return weaponCount < 2;

            // 杂货(非护甲非武器非黄符) → 不限数量
            return true;
        }

        /** 从符兵 PersistentData 读取 (仅从INVENTORY_TAG, 干净双向) */
        void loadFrom(Mob mob) {
            CompoundTag data = mob.getPersistentData();

            if (data.contains(TalismanControl.INVENTORY_TAG, Tag.TAG_LIST)) {
                ListTag list = data.getList(TalismanControl.INVENTORY_TAG, Tag.TAG_COMPOUND);
                for (int i = 0; i < Math.min(list.size(), FOLLOWER_SLOT_COUNT); i++) {
                    CompoundTag tag = list.getCompound(i);
                    if (!tag.isEmpty()) {
                        setItem(i, ItemStack.parse(mob.registryAccess(), tag).orElse(ItemStack.EMPTY));
                    }
                }
            }
        }
    }
}
