package com.potan.mapmakerutils.mixin;

import com.potan.mapmakerutils.ReloadDiagnostics;
import net.minecraft.ReportedException;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(RegistryDataLoader.class)
public class RegistryReloadErrorMixin {
    @Inject(method = "logErrors", at = @At("HEAD"))
    private static void mapmakerutils$captureLocations(Map<ResourceKey<?>, Exception> errors,
                                                      CallbackInfoReturnable<ReportedException> cir) {
        errors.forEach(ReloadDiagnostics::recordRegistryError);
    }
}
