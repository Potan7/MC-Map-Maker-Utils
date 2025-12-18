package com.potan.mapmakerutils.mixin.client;

import com.potan.mapmakerutils.MapMakerUtilsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component component) {
        super(component);
    }

    @Unique
    private int rejoinDelay = 0;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        if (MapMakerUtilsClient.WorldToRejoin == null)
            return;

        String worldName = MapMakerUtilsClient.WorldToRejoin;
        MapMakerUtilsClient.WorldToRejoin = null;

        minecraft.execute(() -> {
            this.minecraft.createWorldOpenFlows()
                .openWorld(worldName, () -> {
                    this.minecraft.setScreen(new TitleScreen());
                    System.out.println("Rejoining world is failed: " + worldName);
                });
        });

    }

}
