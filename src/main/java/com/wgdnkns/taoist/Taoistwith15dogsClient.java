package com.wgdnkns.taoist;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.function.Consumer;

@EventBusSubscriber(modid = Taoistwith15dogs.MODID, value = Dist.CLIENT)
@SuppressWarnings("resource")
public class Taoistwith15dogsClient {

    private static final double COMMAND_RANGE = 32.0;
    private static int particleTickCounter = 0;
    private static final int PARTICLE_INTERVAL = 10; // 每10个tick生成一次粒子（约0.5秒）
    private static boolean lastBellCooldown = false;
    private static int lastBellCommandedEntityId = -1;
    private static long lastBellCommandedGameTime = 0L;
    private static final long BELL_COMMAND_HIDE_TICKS = 200L;
    private static final RenderType SOLID_ARROW = RenderType.create(
            "taoist_with_15_dogs_solid_arrow",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(false)
    );

    private record ClientContext(Minecraft minecraft, ClientLevel level, Player player) {
    }

    private static ClientContext getContext() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            return null;
        }
        return new ClientContext(minecraft, level, player);
    }

    private static void withContext(Consumer<ClientContext> consumer) {
        ClientContext context = getContext();
        if (context != null) {
            consumer.accept(context);
        }
    }

    // 移除了构造器中的配置屏幕注册代码

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        withContext(context -> {
            ClientLevel level = context.level();
            Player player = context.player();
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();

            boolean bellCooldownNow = player.getCooldowns().isOnCooldown(Taoistwith15dogs.SANQING_BELL.get());
            if (bellCooldownNow && !lastBellCooldown) {
                HitResult hit = ProjectileUtil.getHitResultOnViewVector(player, entity -> entity instanceof LivingEntity && entity != player, COMMAND_RANGE);
                if (hit instanceof EntityHitResult entityHitResult) {
                    Entity entity = entityHitResult.getEntity();
                    if (entity instanceof LivingEntity living && living != player) {
                        lastBellCommandedEntityId = living.getId();
                        lastBellCommandedGameTime = level.getGameTime();
                    }
                }
            }
            lastBellCooldown = bellCooldownNow;

            boolean hasTaoistSword = mainHandItem.is(Taoistwith15dogs.TAOIST_SWORD.get())
                    || offHandItem.is(Taoistwith15dogs.TAOIST_SWORD.get());

            if (!hasTaoistSword) {
                particleTickCounter = 0;
                return;
            }

            particleTickCounter++;
            if (particleTickCounter < PARTICLE_INTERVAL) {
                return;
            }
            particleTickCounter = 0;

            RandomSource random = player.getRandom();
            DustParticleOptions pinkDust = new DustParticleOptions(
                    new Vector3f(1.0F, 0.4F, 0.7F),
                    1.0F
            );

            for (int i = 0; i < 4; i++) {
                double xOffset = (random.nextDouble() - 0.5) * 0.6;
                double yOffset = random.nextDouble() * 1.5;
                double zOffset = (random.nextDouble() - 0.5) * 0.6;

                level.addParticle(
                        pinkDust,
                        player.getX() + xOffset,
                        player.getY() + yOffset,
                        player.getZ() + zOffset,
                        0, 0.03, 0
                );
            }
        });
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        withContext(context -> renderArrow(event, context));
    }

    private static void renderArrow(RenderLevelStageEvent event, ClientContext context) {
        Player player = context.player();
        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();
        boolean hasSanqingBell = mainHandItem.is(Taoistwith15dogs.SANQING_BELL.get())
                || offHandItem.is(Taoistwith15dogs.SANQING_BELL.get());
        if (!hasSanqingBell) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(Taoistwith15dogs.SANQING_BELL.get())) {
            return;
        }

        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player, entity -> entity instanceof LivingEntity && entity != player, COMMAND_RANGE);
        if (!(hit instanceof EntityHitResult entityHitResult)) {
            return;
        }
        Entity entity = entityHitResult.getEntity();
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        if (living == player) {
            return;
        }
        if (shouldHideArrowForCommandedTarget(context.level().getGameTime(), living)) {
            return;
        }

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        float time = (context.level().getGameTime() + partial) * 0.35F;
        float alpha = 0.45F + 0.55F * (0.5F + 0.5F * Mth.sin(time));
        float green = 0.02F + 0.08F * (0.5F + 0.5F * Mth.sin(time * 1.7F));
        float blue = 0.02F + 0.08F * (0.5F + 0.5F * Mth.sin(time * 1.3F));
        float red = 1.0F;

        Vec3 camPos = event.getCamera().getPosition();
        Vec3 pos = living.getPosition(partial);
        double x = pos.x;
        double y = living.getBoundingBox().maxY + 0.35;
        double z = pos.z;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(x - camPos.x, y - camPos.y, z - camPos.z);
        poseStack.mulPose(event.getCamera().rotation());
        var bufferSource = context.minecraft().renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(SOLID_ARROW);
        drawSolidDownArrow(poseStack, buffer, red, green, blue, alpha);
        bufferSource.endBatch(SOLID_ARROW);
        poseStack.popPose();
    }

    private static boolean shouldHideArrowForCommandedTarget(long gameTime, LivingEntity target) {
        if (lastBellCommandedEntityId != target.getId()) {
            return false;
        }
        return gameTime - lastBellCommandedGameTime <= BELL_COMMAND_HIDE_TICKS;
    }

    private static void drawSolidDownArrow(PoseStack poseStack,
                                           VertexConsumer buffer,
                                           float r, float g, float b, float a) {
        PoseStack.Pose pose = poseStack.last();
        float fx = 0.0F;
        float fy = 0.0F;
        float fz = 0.0F;

        float headY = fy - 0.22F;
        float headBaseY = fy - 0.02F;
        float tailTopY = fy + 0.28F;
        float headW = 0.18F;
        float tailW = 0.035F;

        tri(pose, buffer,
                fx, headY, fz,
                fx - headW, headBaseY, fz,
                fx + headW, headBaseY, fz,
                r, g, b, a);

        quad(pose, buffer,
                fx - tailW, tailTopY, fz,
                fx + tailW, tailTopY, fz,
                fx + tailW, headBaseY, fz,
                fx - tailW, headBaseY, fz,
                r, g, b, a);
    }

    private static void tri(PoseStack.Pose pose,
                            VertexConsumer buffer,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3,
                            float r, float g, float b, float a) {
        buffer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
        buffer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
        buffer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
    }

    private static void quad(PoseStack.Pose pose,
                             VertexConsumer buffer,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             float r, float g, float b, float a) {
        buffer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
        buffer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
        buffer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);

        buffer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
        buffer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
        buffer.addVertex(pose, x4, y4, z4).setColor(r, g, b, a);
    }
}
