package com.potan.mapmakerutils.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.potan.mapmakerutils.ReloadDiagnostics;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.ReloadCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Mixin(ReloadCommand.class)
public class ReloadCommandMixin {
    @WrapOperation(method = "reloadPacks", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/MinecraftServer;reloadResources(Ljava/util/Collection;)Ljava/util/concurrent/CompletableFuture;"))
    private static CompletableFuture<Void> mapmakerutils$reportReloadErrors(
            MinecraftServer server, Collection<String> packs, Operation<CompletableFuture<Void>> original,
            @Local(argsOnly = true) CommandSourceStack source) {
        var capture = ReloadDiagnostics.begin(server);
        if (capture == null) {
            source.sendFailure(Component.translatable("mapmakerutils.reload.busy"));
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> reload;
        try {
            reload = original.call(server, packs);
        } catch (RuntimeException e) {
            ReloadDiagnostics.finish(server, source, capture, e);
            throw e;
        }
        reload.whenComplete((ignored, failure) -> ReloadDiagnostics.finish(server, source, capture, failure));
        return reload;
    }
}
