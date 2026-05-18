package com.wgdnkns.taoist;

import com.wgdnkns.taoist.item.SanqingBellItem;
import com.wgdnkns.taoist.item.TalismanControl;
import com.wgdnkns.taoist.item.YellowTalismanItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.List;

@Mod(Taoistwith15dogs.MODID)
public class Taoistwith15dogs {
    public static final String MODID = "taoist_with_15_dogs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // 朱砂
    public static final DeferredItem<Item> CINNABAR = ITEMS.registerItem("cinnabar", Item::new);

    // 黄符
    public static final DeferredItem<Item> YELLOW_TALISMAN = ITEMS.registerItem("yellow_talisman",
            YellowTalismanItem::new);

    public static final DeferredItem<Item> SANQING_BELL = ITEMS.registerItem("sanqing_bell",
            properties -> new SanqingBellItem(properties.stacksTo(1)));

    // 桃木剑
    public static final DeferredItem<Item> TAOIST_SWORD = ITEMS.registerItem("taoist_sword",
            properties -> new Item(properties
                    .stacksTo(1)
                    .durability(250)
                    .attributes(ItemAttributeModifiers.builder()
                            .add(Attributes.ATTACK_DAMAGE,
                                    new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.5, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .add(Attributes.ATTACK_SPEED,
                                    new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -1.5, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .build())
                    .component(DataComponents.LORE, new ItemLore(List.of(
                            Component.translatable("item.taoist_with_15_dogs.taoist_sword.lore1").withStyle(ChatFormatting.LIGHT_PURPLE),
                            Component.translatable("item.taoist_with_15_dogs.taoist_sword.lore2").withStyle(ChatFormatting.DARK_RED)
                    )))
            ));

    // 创造模式标签页：包含桃木剑、朱砂，图标固定为桃木剑
    @SuppressWarnings("unused")
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.taoist_with_15_dogs"))
            .icon(() -> TAOIST_SWORD.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(TAOIST_SWORD.get());
                output.accept(CINNABAR.get());
                output.accept(YELLOW_TALISMAN.get());
                output.accept(SANQING_BELL.get());
            })
            .build());

    public Taoistwith15dogs(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }
        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());
        Config.ITEM_STRINGS.get().forEach(item -> LOGGER.info("ITEM >> {}", item));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget == null) {
            return;
        }
        if (entity instanceof IronGolem golem) {
            if ((newTarget instanceof Zombie || newTarget instanceof AbstractSkeleton)
                    && TalismanControl.isControlledByAnyPlayer(newTarget, YELLOW_TALISMAN.get())
                    && !isRetaliation(golem, newTarget)) {
                event.setNewAboutToBeSetTarget(null);
            }
            return;
        }
        if (!(entity instanceof Mob mob)) {
            return;
        }
        if (newTarget == mob) {
            event.setNewAboutToBeSetTarget(null);
            return;
        }
        if (!(mob instanceof Zombie || mob instanceof AbstractSkeleton)) {
            return;
        }
        if (!TalismanControl.isControlledByAnyPlayer(mob, YELLOW_TALISMAN.get())) {
            return;
        }
        var owner = TalismanControl.getOwnerUuid(mob);
        if (owner == null) {
            return;
        }
        if (newTarget instanceof Player player) {
            if (owner.equals(player.getUUID())) {
                event.setNewAboutToBeSetTarget(null);
                return;
            }
            if (!isRetaliation(mob, newTarget)) {
                event.setNewAboutToBeSetTarget(null);
            }
            return;
        }

        if (isCommandedTarget(mob, newTarget)) {
            return;
        }
        if (isRetaliation(mob, newTarget)) {
            return;
        }
        event.setNewAboutToBeSetTarget(null);
    }

    @SubscribeEvent
    @SuppressWarnings("resource")
    public void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof IronGolem golem) {
            Level level = golem.level();
            if (level.isClientSide) {
                return;
            }
            LivingEntity current = golem.getTarget();
            if ((current instanceof Zombie || current instanceof AbstractSkeleton)
                    && TalismanControl.isControlledByAnyPlayer(current, YELLOW_TALISMAN.get())
                    && !isRetaliation(golem, current)) {
                golem.setTarget(null);
            }
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!(mob instanceof Zombie || mob instanceof AbstractSkeleton)) {
            return;
        }
        if (!TalismanControl.isControlledByAnyPlayer(mob, YELLOW_TALISMAN.get())) {
            return;
        }
        Level level = mob.level();
        if (level.isClientSide) {
            return;
        }
        var owner = TalismanControl.getOwnerUuid(mob);
        if (owner == null) {
            return;
        }
        Player ownerPlayer = level.getPlayerByUUID(owner);
        if (ownerPlayer == null) {
            return;
        }
        if (mob instanceof WitherSkeleton) {
            TalismanControl.tickWitherSkeletonDig(mob);
        } else {
            TalismanControl.ensureCanOpenDoors(mob);
        }
        if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
            var commanded = TalismanControl.getCommandedTargetUuid(ownerPlayer);
            if (commanded != null && level instanceof ServerLevel serverLevel) {
                var entity = serverLevel.getEntity(commanded);
                if (entity instanceof LivingEntity living && living.isAlive() && living != ownerPlayer && living != mob && mob.distanceToSqr(living) <= 1024.0) {
                    mob.setTarget(living);
                    return;
                }
                TalismanControl.clearCommandTargetTags(ownerPlayer);
            }
            double distanceSqr = mob.distanceToSqr(ownerPlayer);
            if (distanceSqr > 64.0) {
                mob.getNavigation().moveTo(ownerPlayer, 1.1);
            } else if (distanceSqr < 16.0) {
                mob.getNavigation().stop();
            }
        } else {
            var commanded = TalismanControl.getCommandedTargetUuid(ownerPlayer);
            if (commanded != null && mob.getTarget() != null && !commanded.equals(mob.getTarget().getUUID()) && !isRetaliation(mob, mob.getTarget())) {
                if (level instanceof ServerLevel serverLevel) {
                    var entity = serverLevel.getEntity(commanded);
                    if (entity instanceof LivingEntity living && living.isAlive() && living != ownerPlayer && living != mob && mob.distanceToSqr(living) <= 1024.0) {
                        mob.setTarget(living);
                    } else {
                        TalismanControl.clearCommandTargetTags(ownerPlayer);
                    }
                }
            }
        }
    }

    @SuppressWarnings("resource")
    private static boolean isCommandedTarget(Mob mob, LivingEntity target) {
        var owner = TalismanControl.getOwnerUuid(mob);
        if (owner == null) {
            return false;
        }
        var ownerPlayer = mob.level().getPlayerByUUID(owner);
        if (ownerPlayer == null) {
            return false;
        }
        var commanded = TalismanControl.getCommandedTargetUuid(ownerPlayer);
        return commanded != null && commanded.equals(target.getUUID());
    }

    private static boolean isRetaliation(Mob mob, LivingEntity target) {
        LivingEntity lastHurtBy = mob.getLastHurtByMob();
        if (lastHurtBy == null || lastHurtBy != target) {
            return false;
        }
        int lastHurtTick = mob.getLastHurtByMobTimestamp();
        return mob.tickCount - lastHurtTick <= 100;
    }
}
