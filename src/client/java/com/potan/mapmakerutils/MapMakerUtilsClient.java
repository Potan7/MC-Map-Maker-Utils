package com.potan.mapmakerutils;

import java.io.File;
import java.nio.file.Path;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class MapMakerUtilsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Register client-side commands
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("openpackfolder").executes(this::openDatapackFolder));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("hardreload").executes(this::hardReload));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("copypos")
				.executes(this::copyPos)
				.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("me").executes(this::copyPos))
				.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("lookat").executes(this::copyLookAtPos)));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("copyrot").executes(this::copyRot));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("openvscode").executes(this::openWithVSCode));
		});
	}

	int openWithVSCode(CommandContext<FabricClientCommandSource> context) {
		Minecraft mc = Minecraft.getInstance();
		IntegratedServer server = mc.getSingleplayerServer();

		if (server != null) {
			try {
				Path datapckPath = server.getWorldPath(LevelResource.DATAPACK_DIR);
				File file = datapckPath.toFile();
				if (!file.exists()) file.mkdirs();

				String path = file.getAbsolutePath();
				String os = System.getProperty("os.name").toLowerCase();
				ProcessBuilder pb;
				
				if (os.contains("win")) {
					pb = new ProcessBuilder("cmd.exe", "/c", "code", path);
				} else {
					pb = new ProcessBuilder("code", path);
				}
				
				pb.start();
				context.getSource().sendFeedback(Component.translatable("mapmakerutils.feedback.vscode_opening"));
			} catch (Exception e) {
				context.getSource().sendError(Component.translatable("mapmakerutils.error.vscode_failed"));
				return 0;
			}
		} else {
			context.getSource().sendError(Component.translatable("mapmakerutils.error.singleplayer_only"));
			return 0;
		}
		return Command.SINGLE_SUCCESS;
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
			ModGlobalState.WorldToRejoin = server.getWorldPath(LevelResource.ROOT).getParent().getFileName().toString();
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
			String posString = pos.getX() + " " + pos.getY() + " " + pos.getZ();
			mc.keyboardHandler.setClipboard(posString);
			context.getSource().sendFeedback(Component.translatable("mapmakerutils.feedback.position_copied", posString));
			return Command.SINGLE_SUCCESS;
		}
		return 0;
	}

	int copyLookAtPos(CommandContext<FabricClientCommandSource> context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
			BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
			BlockPos pos = blockHit.getBlockPos();
			String posString = pos.getX() + " " + pos.getY() + " " + pos.getZ();

			mc.keyboardHandler.setClipboard(posString);
			context.getSource().sendFeedback(Component.translatable("mapmakerutils.feedback.lookat_position_copied", posString));
			return Command.SINGLE_SUCCESS;
		} else {
			context.getSource().sendError(Component.translatable("mapmakerutils.error.no_block_looked_at"));
			return 0;
		}
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