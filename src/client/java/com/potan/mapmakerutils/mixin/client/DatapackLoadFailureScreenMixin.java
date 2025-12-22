package com.potan.mapmakerutils.mixin.client;

import com.potan.mapmakerutils.MapMakerUtilsClient;
import net.minecraft.client.gui.GuiGraphics;
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

    @Inject(method = "render", at = @At("TAIL"))
    public void renderErrorDetails(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // 에러 내용이 없으면 아무것도 안 함
        if (MapMakerUtilsClient.lastDatapackErrorDetails == null) return;

        // 마인크래프트 폰트 높이 (보통 9)
        int fontHeight = this.font.lineHeight;

        // 여러 줄일 경우를 대비해 나눔
        String[] lines = MapMakerUtilsClient.lastDatapackErrorDetails.split("\n");

        // 화면 상단에서 30픽셀 내려온 곳부터 시작
        int y = 30;

        for (String line : lines) {
            // [핵심] 표준 메서드 drawCenteredString 사용
            // 색상: 0xFFFFFFFF (앞의 FF가 불투명도 100%를 의미. 이거 없으면 투명해서 안 보임)
            guiGraphics.drawCenteredString(this.font, line, this.width / 2, y, 0xFFFFFFFF);

            // 다음 줄로 이동 (폰트 높이 + 2픽셀 여백)
            y += fontHeight + 2;
        }
    }

    @Override
    public void onClose() {
        // 화면 닫을 때 데이터 초기화
        MapMakerUtilsClient.lastDatapackErrorDetails = null;
        super.onClose();
    }
}