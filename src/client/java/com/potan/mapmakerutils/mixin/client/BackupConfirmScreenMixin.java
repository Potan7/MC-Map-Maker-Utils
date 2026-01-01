package com.potan.mapmakerutils.mixin.client;

import com.potan.mapmakerutils.MapMakerUtilsClient;
import com.potan.mapmakerutils.ModGlobalState;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackupConfirmScreen.class)
public class BackupConfirmScreenMixin extends Screen {
    @Unique
    private String currentWorldId;

    @Shadow @Final
    protected BackupConfirmScreen.Listener onProceed;

    protected BackupConfirmScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addIgnoreOption(CallbackInfo ci) {
        currentWorldId = ModGlobalState.lastOpenedWorldName;
        ModGlobalState.lastOpenedWorldName = null;

        if (currentWorldId != null) {
            // 무시 옵션 버튼 추가
            this.addRenderableWidget(
                    Button.builder(Component.translatable("mapmakerutils.ui.session_ignore_button"),
                                    button -> {
                                        if (currentWorldId != null) {
                                            ModGlobalState.ignoredConfirmScreenMaps.add(currentWorldId);
                                        }
                                        this.onProceed.proceed(false, true);
                                    })
                            .pos(this.width / 2 - 100, this.height / 4 + 120 + 12)
                            .size(200, 20)
                            .build()
            );
        }
    }


}
