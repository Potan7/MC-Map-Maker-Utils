package com.potan.mapmakerutils.mixin.client;

import com.potan.mapmakerutils.ModGlobalState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.DatapackLoadFailureScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DatapackLoadFailureScreen.class)
public class DatapackLoadFailureScreenMixin extends Screen {

    protected DatapackLoadFailureScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    public void renderErrorDetails(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        // 에러 내용이 없으면 아무것도 안 함
        if (ModGlobalState.lastDatapackErrorDetails == null) return;

        // 마인크래프트 폰트 높이 (보통 9)
        int fontHeight = this.font.lineHeight;

        // 여러 줄일 경우를 대비해 나눔
        String[] lines = ModGlobalState.lastDatapackErrorDetails.split("\n");

        // 화면 상단에서 30픽셀 내려온 곳부터 시작
        int y = 30;

        for (String line : lines) {
            for (var wrapped : this.font.split(Component.literal(line), Math.max(1, this.width - 40))) {
                graphics.centeredText(this.font, wrapped, this.width / 2, y, 0xFFFFFFFF);
                y += fontHeight + 2;
            }
        }
    }

    @Override
    public void onClose() {
        // 화면 닫을 때 데이터 초기화
        ModGlobalState.lastDatapackErrorDetails = null;
        super.onClose();
    }
}
