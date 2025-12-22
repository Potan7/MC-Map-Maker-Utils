package com.potan.mapmakerutils.mixin.client;

import com.potan.mapmakerutils.MapMakerUtilsClient;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey; // 임포트 필수
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(RegistryDataLoader.class)
public class RegistryErrorMixin {

    @Inject(
            method = "logErrors",
            at = @At("HEAD"),
            remap = true
    )
    private static void captureRegistryErrors(
            // Map<ResourceKey<?>, Exception> 형태입니다.
            Map<ResourceKey<?>, Exception> errors,
            CallbackInfoReturnable<?> cir
    ) {
        if (errors != null && !errors.isEmpty()) {
            // 첫 번째 오류를 가져옵니다.
            Map.Entry<ResourceKey<?>, Exception> entry = errors.entrySet().iterator().next();

            ResourceKey<?> fileKey = entry.getKey();
            Exception exception = entry.getValue();

            String filePath = fileKey.identifier().toString();
            String errorMsg = exception.getMessage();

            // 메시지 다듬기
            MapMakerUtilsClient.lastDatapackErrorDetails =
                    "§c[Path]: " + filePath +
                            "\n§7[Msg]: " + errorMsg;

            System.out.println("!!! Registry Error File: " + filePath);
        }
    }
}