package com.wgdnkns.taoist;

import com.wgdnkns.taoist.client.gui.FollowerMenu;
import com.wgdnkns.taoist.entity.ThrownCopperCoinSword;
import com.wgdnkns.taoist.item.BaguaMirrorItem;
import com.wgdnkns.taoist.item.CopperCoinSwordItem;
import com.wgdnkns.taoist.item.LightningTaoistSwordItem;
import com.wgdnkns.taoist.item.SanqingBellItem;
import com.wgdnkns.taoist.item.TalismanControl;
import com.wgdnkns.taoist.item.TaoistSwordItem;
import com.wgdnkns.taoist.item.YellowTalismanItem;
import com.wgdnkns.taoist.network.SelectedFollowerPayload;
import com.wgdnkns.taoist.network.TalismanSyncHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Mod(Taoistwith15dogs.MODID)
public class Taoistwith15dogs {
    public static final String MODID = "taoist_with_15_dogs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<FollowerMenu>> FOLLOWER_MENU = MENUS.register("follower",
            () -> new MenuType<>((id, inv) -> new FollowerMenu(id, inv), FeatureFlags.VANILLA_SET));

    // 朱砂
    public static final DeferredItem<Item> CINNABAR = ITEMS.registerItem("cinnabar", Item::new);

    // 铜钱
    public static final DeferredItem<Item> COPPER_COIN = ITEMS.registerItem("copper_coin", Item::new);

    // 黄符
    public static final DeferredItem<Item> YELLOW_TALISMAN = ITEMS.registerItem("yellow_talisman",
            properties -> new YellowTalismanItem(properties.stacksTo(54)));
    //三清铃
    public static final DeferredItem<Item> SANQING_BELL = ITEMS.registerItem("sanqing_bell",
            properties -> new SanqingBellItem(properties.stacksTo(1)));

    // 桃木剑
    public static final DeferredItem<Item> TAOIST_SWORD = ITEMS.registerItem("taoist_sword",
            properties -> new TaoistSwordItem(properties
                    .stacksTo(1)
                    .durability(250)
                    .attributes(ItemAttributeModifiers.builder()
                            .add(Attributes.ATTACK_DAMAGE,
                                    new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 3.5, AttributeModifier.Operation.ADD_VALUE),
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

    // 铜钱剑
    public static final DeferredItem<Item> COPPER_COIN_SWORD = ITEMS.registerItem("copper_coin_sword",
            properties -> new CopperCoinSwordItem(properties
                    .stacksTo(1)
                    .durability(200)
                    .attributes(ItemAttributeModifiers.builder()
                            .add(Attributes.ATTACK_DAMAGE,
                                    new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 5.5, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .add(Attributes.ATTACK_SPEED,
                                    new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.0, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .build())
            ));

    // 投掷铜钱剑实体
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownCopperCoinSword>> THROWN_COPPER_COIN_SWORD = ENTITY_TYPES.register("thrown_copper_coin_sword",
            () -> EntityType.Builder.<ThrownCopperCoinSword>of(ThrownCopperCoinSword::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20)
                    .build("thrown_copper_coin_sword"));

    // 方块
    public static final DeferredBlock<Block> LIGHTNING_WOOD = BLOCKS.registerBlock("lightning_wood",
            RotatedPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG));
    public static final DeferredBlock<Block> LIGHTNING_PLANKS = BLOCKS.registerSimpleBlock("lightning_planks",
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));

    // 方块物品
    public static final DeferredItem<BlockItem> LIGHTNING_WOOD_ITEM = ITEMS.registerSimpleBlockItem(LIGHTNING_WOOD);
    public static final DeferredItem<BlockItem> LIGHTNING_PLANKS_ITEM = ITEMS.registerSimpleBlockItem(LIGHTNING_PLANKS);

    // 八卦镜
    public static final DeferredItem<Item> BAGUA_MIRROR = ITEMS.registerItem("bagua_mirror",
            properties -> new BaguaMirrorItem(properties.stacksTo(1)));

    // 雷电桃木剑
    public static final DeferredItem<Item> LIGHTNING_TAOIST_SWORD = ITEMS.registerItem("lightning_taoist_sword",
            properties -> new LightningTaoistSwordItem(properties
                    .stacksTo(1)
                    .durability(500)
                    .attributes(ItemAttributeModifiers.builder()
                            .add(Attributes.ATTACK_DAMAGE,
                                    new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 8.5, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .add(Attributes.ATTACK_SPEED,
                                    new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -1.5, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .build())));

    // 创造模式标签页
    @SuppressWarnings("unused")
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.taoist_with_15_dogs"))
            .icon(() -> TAOIST_SWORD.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(TAOIST_SWORD.get());
                output.accept(CINNABAR.get());
                output.accept(YELLOW_TALISMAN.get());
                output.accept(SANQING_BELL.get());
                output.accept(COPPER_COIN.get());
                output.accept(COPPER_COIN_SWORD.get());
                output.accept(BAGUA_MIRROR.get());
                output.accept(LIGHTNING_WOOD_ITEM.get());
                output.accept(LIGHTNING_PLANKS_ITEM.get());
                output.accept(LIGHTNING_TAOIST_SWORD.get());
            })
            .build());

    public Taoistwith15dogs(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENUS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);
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

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(MODID);
        registrar.playToClient(
                SelectedFollowerPayload.TYPE,
                SelectedFollowerPayload.STREAM_CODEC,
                (payload, context) -> TalismanSyncHandler.setSelectedFollowerUuid(payload.selectedFollowerUuid())
        );
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
            if (newTarget instanceof Mob mob && TalismanControl.isControllableType(mob)
                    && TalismanControl.isControlledByAnyPlayer(mob, YELLOW_TALISMAN.get())
                    && !isRetaliation(golem, mob)) {
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
        if (!TalismanControl.isControllableType(mob)) {
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
            if (!isRetaliation(mob, player)) {
                event.setNewAboutToBeSetTarget(null);
            }
            return;
        }

        if (isCommandedTarget(mob, newTarget)) {
            return;
        }
        // 也检查mob自己的command tag
        UUID mobCmd = TalismanControl.getCommandedTargetUuid(mob);
        if (mobCmd != null && mobCmd.equals(newTarget.getUUID())) {
            return;
        }
        // 友伤判定: 不能攻击同样是该玩家控制的符兵
        if (newTarget instanceof Mob targetMob && TalismanControl.isControlledByPlayer(targetMob, YELLOW_TALISMAN.get(), owner)) {
            event.setNewAboutToBeSetTarget(null);
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
            if (current instanceof Mob mob && TalismanControl.isControllableType(mob)
                    && TalismanControl.isControlledByAnyPlayer(mob, YELLOW_TALISMAN.get())
                    && !isRetaliation(golem, mob)) {
                golem.setTarget(null);
            }
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!TalismanControl.isControllableType(mob)) {
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

        // 用mob自己的commanded target tag判断, 不再用owner的
        UUID mobTargetCmd = TalismanControl.getCommandedTargetUuid(mob);

        if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
            if (mobTargetCmd != null && level instanceof ServerLevel serverLevel) {
                var entity = serverLevel.getEntity(mobTargetCmd);
                if (entity instanceof LivingEntity living && living.isAlive() && living != ownerPlayer && living != mob && mob.distanceToSqr(living) <= 1024.0) {
                    mob.setTarget(living);
                    return;
                }
                // 目标死亡或丢失 → 清除该mob的命令
                TalismanControl.clearCommandTargetTags(mob);
                mob.setTarget(null);
            }
            // 无目标: 跟随主人, 近距离停防挤
            double distanceSqr = mob.distanceToSqr(ownerPlayer);
            if (distanceSqr > 64.0) {
                mob.getNavigation().moveTo(ownerPlayer, 1.1);
            } else if (distanceSqr < 16.0) {
                mob.getNavigation().stop();
            }
        } else {
            // 有目标: 攻击模式, 不禁用寻路 → OpenDoorGoal可开门
            if (mobTargetCmd != null && !mobTargetCmd.equals(mob.getTarget().getUUID()) && !isRetaliation(mob, mob.getTarget())) {
                var entity = level instanceof ServerLevel sl ? sl.getEntity(mobTargetCmd) : null;
                if (entity instanceof LivingEntity living && living.isAlive() && living != ownerPlayer && living != mob && mob.distanceToSqr(living) <= 1024.0) {
                    mob.setTarget(living);
                } else {
                    TalismanControl.clearCommandTargetTags(mob);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            BaguaMirrorItem.scareUndead(player);
            if (player.tickCount % 20 == 0 && player instanceof ServerPlayer serverPlayer) {
                UUID uuid = TalismanControl.getSelectedFollowerUuid(player);
                serverPlayer.connection.send(new SelectedFollowerPayload(uuid));
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!mob.getTags().contains(TalismanControl.TALISMAN_TAG)) return;
        if (mob.level().isClientSide) return;

        var data = mob.getPersistentData();
        if (!data.contains(TalismanControl.INVENTORY_TAG, Tag.TAG_LIST)) return;

        var list = data.getList(TalismanControl.INVENTORY_TAG, Tag.TAG_COMPOUND);
        var pos = mob.position();
        var level = mob.level();

        var equipped = java.util.Arrays.asList(
                mob.getItemBySlot(EquipmentSlot.HEAD),
                mob.getItemBySlot(EquipmentSlot.CHEST),
                mob.getItemBySlot(EquipmentSlot.LEGS),
                mob.getItemBySlot(EquipmentSlot.FEET),
                mob.getItemBySlot(EquipmentSlot.MAINHAND),
                mob.getItemBySlot(EquipmentSlot.OFFHAND)
        );

        for (int i = 0; i < list.size(); i++) {
            var tag = list.getCompound(i);
            if (tag.isEmpty()) continue;
            var stack = ItemStack.parse(mob.registryAccess(), tag).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof YellowTalismanItem) continue;

            // 跳过已装备的物品 (会自然掉落, 防止双击落)
            boolean alreadyEquipped = false;
            for (var eq : equipped) {
                if (ItemStack.isSameItemSameComponents(stack, eq)) {
                    alreadyEquipped = true;
                    break;
                }
            }
            if (alreadyEquipped) continue;

            level.addFreshEntity(new ItemEntity(level, pos.x, pos.y, pos.z, stack));
        }

        data.remove(TalismanControl.INVENTORY_TAG);
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide) return;
        Player player = event.getEntity();
        if (!player.getMainHandItem().isEmpty()) return;
        if (!(event.getTarget() instanceof Mob mob)) return;
        if (!mob.getTags().contains(TalismanControl.TALISMAN_TAG)) return;
        if (!TalismanControl.isControllableType(mob) && mob instanceof Slime) return;

        event.setCanceled(true);
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> FollowerMenu.createServerMenu(containerId, playerInventory, mob),
                Component.translatable("container.taoist_with_15_dogs.follower")
        ));
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
