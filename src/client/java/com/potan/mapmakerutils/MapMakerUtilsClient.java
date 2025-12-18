package com.potan.mapmakerutils;

import java.io.File;
import java.nio.file.Path;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.network.chat.Component;

public class MapMakerUtilsClient implements ClientModInitializer {
	public static String WorldToRejoin = null;

	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("openpackfolder").executes(this::openDatapackFolder));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("hardreload").executes(this::hardReload));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("copypos").executes(this::copyPos));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("copyrot").executes(this::copyRot));
		});
	}

	int openDatapackFolder(CommandContext<FabricClientCommandSource> context) {
		Minecraft mc = Minecraft.getInstance();
		IntegratedServer server = mc.getSingleplayerServer();

		if (server != null) {
			try {
				Path datapckPath = server.getWorldPath(LevelResource.DATAPACK_DIR);
				File file = datapckPath.toFile();
				if (file.exists()) {
					Util.getPlatform().openFile(file);
					context.getSource().sendFeedback(Component.translatable("mapmakerutils.feedback.datapack_folder_opened"));
				}
				else {
					file.mkdirs();
					Util.getPlatform().openFile(file);
					context.getSource().sendFeedback(Component.translatable("mapmakerutils.feedback.datapack_folder_created"));
				}
					
			} catch (Exception e) {
				context.getSource().sendError(Component.translatable("mapmakerutils.feedback.failed_to_open_datapack", e.getMessage()));
				return 0;
			}
		} else {
			context.getSource().sendError(Component.translatable("mapmakerutils.error.singleplayer_only"));
			return 0;
		}
		return Command.SINGLE_SUCCESS;
	}

	int hardReload(CommandContext<FabricClientCommandSource> context) {
		
		Minecraft mc = Minecraft.getInstance();
		IntegratedServer server = mc.getSingleplayerServer();

		if (server != null) {
			WorldToRejoin = server.getWorldPath(LevelResource.ROOT).getParent().getFileName().toString();
			mc.disconnectFromWorld(Component.literal("Reload Map"));
			context.getSource().sendFeedback(Component.translatable("mapmakerutils.feedback.reloading_world"));
		} else {
			context.getSource().sendError(Component.translatable("mapmakerutils.error.singleplayer_only"));
			return 0;
		}

		return Command.SINGLE_SUCCESS;
	}

	int copyPos(CommandContext<FabricClientCommandSource> context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			BlockPos pos = mc.player.blockPosition();
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			String posString = x + " " + y + " " + z;
			mc.keyboardHandler.setClipboard(posString);
			context.getSource().sendFeedback(Component.translatable("mapmakerutils.feedback.position_copied", posString));
			return Command.SINGLE_SUCCESS;
		}
		return 0;
	}

	int copyRot(CommandContext<FabricClientCommandSource> context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			float yaw = mc.player.getYRot();
			float pitch = mc.player.getXRot();
			String rotString = yaw + " " + pitch;
			mc.keyboardHandler.setClipboard(rotString);
			context.getSource().sendFeedback(Component.translatable("mapmakerutils.feedback.rotation_copied", rotString));
			return Command.SINGLE_SUCCESS;
		}

		return 0;
	}
}