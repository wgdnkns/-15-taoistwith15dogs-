package com.wgdnkns.taoist.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 符兵装备屏幕
 * =============
 * 背景: shulker_box.png (176×167 像素)
 *
 * 布局:
 *   标题(生物名称)居中显示在顶部
 *   上面 3×9 容器网格 (x=8, y=18)
 *   下面 玩家背包 3×9 (y=84) + 快捷栏 (y=142)
 *
 * 【调整方法】
 *   标题位置: 修改构造中的 titleLabelX / titleLabelY
 *   槽位位置: 请去 FollowerMenu.java 修改坐标常量
 */
public class FollowerScreen extends AbstractContainerScreen<FollowerMenu> {
    private static final ResourceLocation CONTAINER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/shulker_box.png");

    public FollowerScreen(FollowerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight++; // shulker_box.png 是 167 像素高
        this.titleLabelX = 8;  // 左上角
        this.titleLabelY = 6;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CONTAINER_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }
}
