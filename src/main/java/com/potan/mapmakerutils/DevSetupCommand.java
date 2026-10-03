package com.potan.mapmakerutils;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

final class DevSetupCommand {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mapmakerutils.json");

    static void register() {
        try {
            DevSetupConfig.load(CONFIG_PATH);
        } catch (IOException e) {
            MapMakerUtils.LOGGER.error("Could not load devsetup config {}", CONFIG_PATH, e);
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("devsetup")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> execute(context.getSource()))));
    }

    private static int execute(CommandSourceStack source) {
        List<String> commands;
        try {
            commands = DevSetupConfig.load(CONFIG_PATH);
        } catch (IOException e) {
            source.sendFailure(Component.translatable("mapmakerutils.error.devsetup_config", e.getMessage()));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("mapmakerutils.feedback.devsetup", commands.size()), false);
        for (String command : commands) {
            source.getServer().getCommands().performPrefixedCommand(source, command);
        }
        return commands.size();
    }
}
