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
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.potan.mapmakerutils.screen.DialogEditorScreen;
import com.potan.mapmakerutils.util.DialogJsonGenerator;
import com.potan.mapmakerutils.util.DialogDatapackManager;
import java.nio.file.Files;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.resources.Identifier;

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

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("dialogeditor")
				.executes(this::openEditorEmpty)
				.then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("dialogId", StringArgumentType.greedyString())
					.suggests((context, builder) -> {
						String remaining = builder.getRemaining().toLowerCase();
						for (String id : DialogDatapackManager.scanExistingDialogs(Minecraft.getInstance().getSingleplayerServer())) {
							if (id.toLowerCase().startsWith(remaining)) {
								builder.suggest(id);
							}
						}
						return builder.buildFuture();
					})
					.executes(this::openEditorExisting)));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("testdialogeditor")
				.executes(this::runTest));
		});

		if ("true".equals(System.getProperty("mapmakerutils.dialogE2E"))) {
			ClientTickEvents.START_CLIENT_TICK.register(new ClientTickEvents.StartTick() {
				private boolean e2eStarted = false;
				private int e2eTickDelay = 0;
				private boolean e2eTestExecuted = false;

				@Override
				public void onStartTick(Minecraft client) {
					if (client.screen instanceof net.minecraft.client.gui.screens.TitleScreen && !e2eStarted) {
						e2eTickDelay++;
						if (e2eTickDelay > 60) { // wait 3 seconds
							e2eStarted = true;
							client.execute(() -> {
								try {
									client.createWorldOpenFlows().openWorld("New World", () -> {});
								} catch (Exception e) {
									writeE2EResult(false, "Failed to open world: " + e.getMessage());
									client.stop();
								}
							});
						}
					}

					if (client.level != null && client.player != null && e2eStarted && !e2eTestExecuted) {
						e2eTestExecuted = true;
						client.execute(() -> {
							try {
								DialogEditorScreen screen = new DialogEditorScreen(null);
								client.setScreen(screen);
								
								boolean success = screen.runAutomationTest();
								client.setScreen(null);
								
								writeE2EResult(success, screen.getAutomationReport());
								client.stop();
							} catch (Exception e) {
								writeE2EResult(false, "Automation exception: " + e.getMessage());
								client.stop();
							}
						});
					}
				}
			});
		}
	}

	private static void writeE2EResult(boolean success, String report) {
		try {
			File dir = new File("../build/dialog-e2e");
			if (!dir.getParentFile().exists()) {
				dir = new File("build/dialog-e2e");
			}
			if (!dir.exists()) dir.mkdirs();
			File file = new File(dir, "result.json");
			
			String json;
			if (success) {
				json = "{\n" +
				       "  \"passed\": true,\n" +
				       "  \"message\": \"" + report + "\",\n" +
				       "  \"log\": \"run/logs/latest.log\"\n" +
				       "}";
			} else {
				String msg = report.startsWith("FAIL: ") ? report.substring(6) : report;
				String step = "unknown";
				String expected = "null";
				String actual = "null";
				
				if (msg.contains("Input width was not clamped")) {
					step = "number-range-width-clamp";
					expected = "1";
					if (msg.contains("actual=")) {
						actual = msg.substring(msg.indexOf("actual=") + 7).trim();
					}
				} else if (msg.contains("Range initial value was not clamped")) {
					step = "number-range-initial-clamp";
					expected = "10";
					if (msg.contains("actual=")) {
						actual = msg.substring(msg.indexOf("actual=") + 7).trim();
					}
				}
				
				json = "{\n" +
				       "  \"passed\": false,\n" +
				       "  \"step\": \"" + step + "\",\n" +
				       "  \"expected\": " + expected + ",\n" +
				       "  \"actual\": " + actual + ",\n" +
				       "  \"message\": \"" + msg.replace("\"", "\\\"").replace("\n", "\\n") + "\",\n" +
				       "  \"log\": \"run/logs/latest.log\"\n" +
				       "}";
			}
			java.nio.file.Files.writeString(file.toPath(), json);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	int runTest(CommandContext<FabricClientCommandSource> context) {
		Minecraft mc = Minecraft.getInstance();
		mc.execute(() -> {
			DialogEditorScreen screen = new DialogEditorScreen(null);
			mc.setScreen(screen);
			
			boolean success = screen.runAutomationTest();
			mc.setScreen(null); // Close the screen
			
			if (success) {
				context.getSource().sendFeedback(Component.literal("§a[MapMakerUtils] In-Game UI automation test PASSED!"));
			} else {
				context.getSource().sendError(Component.literal("§c[MapMakerUtils] In-Game UI automation test FAILED!"));
			}
		});
		return Command.SINGLE_SUCCESS;
	}

	int openEditorEmpty(CommandContext<FabricClientCommandSource> context) {
		Minecraft mc = Minecraft.getInstance();
		mc.execute(() -> {
			mc.setScreen(new DialogEditorScreen(null));
		});
		return Command.SINGLE_SUCCESS;
	}

	int openEditorExisting(CommandContext<FabricClientCommandSource> context) {
		Minecraft mc = Minecraft.getInstance();
		String dialogIdStr = StringArgumentType.getString(context, "dialogId");

		Identifier dialogId;
		try {
			dialogId = Identifier.parse(dialogIdStr);
		} catch (Exception e) {
			context.getSource().sendError(Component.literal("§cInvalid dialog ID format. Must be namespace:id"));
			return 0;
		}

		String namespace = dialogId.getNamespace();
		String filename = dialogId.getPath();

		final String finalNamespace = namespace;
		final String finalFilename = filename;

		IntegratedServer server = mc.getSingleplayerServer();
		if (server != null) {
			try {
				Path datapackDir = server.getWorldPath(LevelResource.DATAPACK_DIR);
				File dialogFile = null;
				File[] datapacks = datapackDir.toFile().listFiles();
				String datapackName = "mapmakerutils_generated";
				long latestTime = -1;
				if (datapacks != null) {
					for (File dp : datapacks) {
						if (dp.isDirectory()) {
							File targetFile = new File(dp, "data/" + finalNamespace + "/dialog/" + finalFilename + ".json");
							if (targetFile.exists()) {
								if (targetFile.lastModified() > latestTime) {
									latestTime = targetFile.lastModified();
									dialogFile = targetFile;
									datapackName = dp.getName();
								}
							}
						}
					}
				}

				final String finalDatapackName = datapackName;
				if (dialogFile != null && dialogFile.exists()) {
					String json = Files.readString(dialogFile.toPath());
					DialogJsonGenerator.DialogModel model = DialogJsonGenerator.deserialize(json);
					mc.execute(() -> {
						mc.setScreen(new DialogEditorScreen(model, finalNamespace, finalFilename, finalDatapackName));
					});
				} else {
					mc.execute(() -> {
						mc.setScreen(new DialogEditorScreen(null, finalNamespace, finalFilename, finalDatapackName));
					});
				}
			} catch (Exception e) {
				context.getSource().sendError(Component.literal("§cError reading dialog file: " + e.getMessage()));
				return 0;
			}
		} else {
			context.getSource().sendError(Component.translatable("mapmakerutils.error.singleplayer_only"));
			return 0;
		}

		return Command.SINGLE_SUCCESS;
	}

	int openWithVSCode(CommandContext<FabricClientCommandSource> context) {
		Minecraft mc = Minecraft.getInstance();
		IntegratedServer server = mc.getSingleplayerServer();

		if (server != null) {
			try {
				Path datapckPath = server.getWorldPath(LevelResource.DATAPACK_DIR).toAbsolutePath().normalize();
				
				// Security check: Verify that the path is strictly inside the game's saves directory
				Path savesDir = mc.gameDirectory.toPath().resolve("saves").toAbsolutePath().normalize();
				if (!datapckPath.startsWith(savesDir)) {
					context.getSource().sendError(Component.literal("§cSecurity Error: Datapack directory is outside the game's saves folder!"));
					return 0;
				}

				File file = datapckPath.toFile();
				if (!file.exists()) file.mkdirs();

				String path = file.getAbsolutePath();
				
				// Execute VS Code directly without 'cmd.exe /c' wrapper to prevent shell command injection
				ProcessBuilder pb = new ProcessBuilder("code", path);
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