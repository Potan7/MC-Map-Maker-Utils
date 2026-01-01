package com.potan.mapmakerutils.mixin.client;

import com.potan.mapmakerutils.ModGlobalState;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldOpenFlows.class)
public class WorldOpenFlowsMixin {

    @Inject(
            method = "askForBackup",
            at = @At("HEAD"),
            cancellable = true
    )
    private void CheckSkip(LevelStorageSource.LevelStorageAccess levelStorageAccess, boolean bl, Runnable runnable, Runnable runnable2, CallbackInfo ci)
    {
        System.out.println("WorldOpenFlowsMixin: askForBackup called. bl=" + bl);
        if (!bl)
        {
            String worldName = levelStorageAccess.getLevelId();
            ModGlobalState.lastOpenedWorldName = worldName;
            if (ModGlobalState.ignoredConfirmScreenMaps.contains(worldName))
            {
                // 무시 목록에 있으면 백업 확인 스크린을 건너뜀
                runnable.run();
                ci.cancel();
            }
        }
    }
}
