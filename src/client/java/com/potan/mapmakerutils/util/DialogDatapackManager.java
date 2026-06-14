package com.potan.mapmakerutils.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.potan.mapmakerutils.util.DialogJsonGenerator.DialogModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DialogDatapackManager {

    public static class SaveResult {
        private final boolean success;
        private final String errorMessage;
        private final boolean hotSwapped;
        private final String hotSwapError;

        public SaveResult(boolean success, String errorMessage, boolean hotSwapped, String hotSwapError) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.hotSwapped = hotSwapped;
            this.hotSwapError = hotSwapError;
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isHotSwapped() { return hotSwapped; }
        public String getHotSwapError() { return hotSwapError; }

        public static SaveResult failure(String errorMessage) {
            return new SaveResult(false, errorMessage, false, null);
        }

        public static SaveResult success(boolean hotSwapped, String hotSwapError) {
            return new SaveResult(true, null, hotSwapped, hotSwapError);
        }
    }

    public static class LoadResult {
        private final boolean success;
        private final String errorMessage;
        private final DialogModel model;
        private final String namespace;
        private final String filename;
        private final String datapackName;

        public LoadResult(boolean success, String errorMessage, DialogModel model, String namespace, String filename, String datapackName) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.model = model;
            this.namespace = namespace;
            this.filename = filename;
            this.datapackName = datapackName;
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public DialogModel getModel() { return model; }
        public String getNamespace() { return namespace; }
        public String getFilename() { return filename; }
        public String getDatapackName() { return datapackName; }

        public static LoadResult failure(String errorMessage) {
            return new LoadResult(false, errorMessage, null, null, null, null);
        }

        public static LoadResult success(DialogModel model, String namespace, String filename, String datapackName) {
            return new LoadResult(true, null, model, namespace, filename, datapackName);
        }
    }

    public static List<String> scanExistingDialogs(IntegratedServer server) {
        List<String> list = new ArrayList<>();
        if (server == null) return list;
        try {
            Path datapackDir = server.getWorldPath(LevelResource.DATAPACK_DIR);
            File[] datapacks = datapackDir.toFile().listFiles();
            if (datapacks != null) {
                for (File dp : datapacks) {
                    if (dp.isDirectory()) {
                        File dataDir = new File(dp, "data");
                        if (dataDir.exists() && dataDir.isDirectory()) {
                            File[] namespaces = dataDir.listFiles();
                            if (namespaces != null) {
                                for (File ns : namespaces) {
                                    if (ns.isDirectory()) {
                                        File dialogDir = new File(ns, "dialog");
                                        if (dialogDir.exists() && dialogDir.isDirectory()) {
                                            File[] files = dialogDir.listFiles();
                                            if (files != null) {
                                                for (File f : files) {
                                                    if (f.isFile() && f.getName().endsWith(".json")) {
                                                        String name = f.getName().substring(0, f.getName().length() - 5);
                                                        list.add(ns.getName() + ":" + name);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return list;
    }

    public static LoadResult loadDialog(IntegratedServer server, String dialogId) {
        if (server == null) {
            return LoadResult.failure("Integrated server is not running.");
        }

        String ns = "my_datapack";
        String fn = dialogId;
        if (dialogId.contains(":")) {
            String[] parts = dialogId.split(":", 2);
            ns = parts[0];
            fn = parts[1];
        }

        try {
            Path datapackDir = server.getWorldPath(LevelResource.DATAPACK_DIR);
            File dialogFile = null;
            File[] datapacks = datapackDir.toFile().listFiles();
            String detectedDatapackName = "mapmakerutils_generated";
            long latestTime = -1;
            if (datapacks != null) {
                for (File dp : datapacks) {
                    if (dp.isDirectory()) {
                        File targetFile = new File(dp, "data/" + ns + "/dialog/" + fn + ".json");
                        if (targetFile.exists()) {
                            if (targetFile.lastModified() > latestTime) {
                                latestTime = targetFile.lastModified();
                                dialogFile = targetFile;
                                detectedDatapackName = dp.getName();
                            }
                        }
                    }
                }
            }

            if (dialogFile != null && dialogFile.exists()) {
                String json = Files.readString(dialogFile.toPath());
                DialogModel loadedModel = DialogJsonGenerator.deserialize(json);
                return LoadResult.success(loadedModel, ns, fn, detectedDatapackName);
            } else {
                return LoadResult.failure("Dialog file not found.");
            }
        } catch (Exception e) {
            return LoadResult.failure(e.getMessage());
        }
    }

    public static SaveResult saveToDatapack(
            IntegratedServer server,
            String namespace,
            String filename,
            String datapackName,
            DialogModel model
    ) {
        if (server == null) {
            return SaveResult.failure("Saving dialogs is available in singleplayer worlds only.");
        }

        try {
            Path datapackDir = server.getWorldPath(LevelResource.DATAPACK_DIR);
            Path targetDatapack = datapackDir.resolve(datapackName);
            Path dialogDir = targetDatapack.resolve("data").resolve(namespace).resolve("dialog");

            Files.createDirectories(dialogDir);

            // Write pack.mcmeta if not present
            Path mcmeta = targetDatapack.resolve("pack.mcmeta");
            if (!Files.exists(mcmeta)) {
                String desc = "{\"pack\":{\"pack_format\":61,\"description\":\"Generated by MapMakerUtils Dialog Editor\"}}";
                Files.writeString(mcmeta, desc);
            }

            Path targetFile = dialogDir.resolve(filename + ".json");
            String json = DialogJsonGenerator.serialize(model, true);
            Files.writeString(targetFile, json);

            // Hot-swap in-memory registry
            boolean hotSwapped = false;
            String hotSwapError = null;
            try {
                JsonElement jsonElement = JsonParser.parseString(json);
                DataResult<Holder<net.minecraft.server.dialog.Dialog>> parseResult =
                        net.minecraft.server.dialog.Dialog.CODEC.parse(JsonOps.INSTANCE, jsonElement);
                Holder<net.minecraft.server.dialog.Dialog> dialogHolder =
                        parseResult.getOrThrow(msg -> new RuntimeException("Failed to parse Dialog: " + msg));
                net.minecraft.server.dialog.Dialog parsedDialog = dialogHolder.value();

                String dialogIdStr = namespace + ":" + filename;

                // 1. Hot-swap server registry
                Registry<net.minecraft.server.dialog.Dialog> dialogRegistry =
                        server.registryAccess().lookupOrThrow(Registries.DIALOG);
                hotSwapRegistry(dialogRegistry, json, parsedDialog, dialogIdStr);

                // 2. Hot-swap client registry
                try {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null) {
                        Registry<net.minecraft.server.dialog.Dialog> clientRegistry =
                                mc.level.registryAccess().lookupOrThrow(Registries.DIALOG);
                        hotSwapRegistry(clientRegistry, json, parsedDialog, dialogIdStr);
                    }
                } catch (Exception ce) {
                    // ignore client registry hot-swap failure
                }

                hotSwapped = true;
            } catch (Exception e) {
                e.printStackTrace();
                hotSwapError = e.getMessage();
            }

            return SaveResult.success(hotSwapped, hotSwapError);
        } catch (Exception e) {
            return SaveResult.failure("Failed to save file: " + e.getMessage());
        }
    }

    private static void hotSwapRegistry(
            Registry<net.minecraft.server.dialog.Dialog> registry,
            String json,
            net.minecraft.server.dialog.Dialog parsedDialog,
            String dialogIdStr
    ) throws Exception {
        if (registry instanceof MappedRegistry) {
            MappedRegistry<net.minecraft.server.dialog.Dialog> mappedRegistry =
                    (MappedRegistry<net.minecraft.server.dialog.Dialog>) registry;

            java.lang.reflect.Field frozenField = MappedRegistry.class.getDeclaredField("frozen");
            frozenField.setAccessible(true);
            boolean wasFrozen = frozenField.getBoolean(mappedRegistry);
            frozenField.setBoolean(mappedRegistry, false);

            try {
                ResourceKey<net.minecraft.server.dialog.Dialog> resourceKey = ResourceKey.create(
                        Registries.DIALOG,
                        Identifier.parse(dialogIdStr)
                );

                java.util.Optional<Holder.Reference<net.minecraft.server.dialog.Dialog>> existingHolder =
                        mappedRegistry.get(Identifier.parse(dialogIdStr));

                if (existingHolder.isPresent()) {
                    Holder.Reference<net.minecraft.server.dialog.Dialog> holderRef = existingHolder.get();
                    net.minecraft.server.dialog.Dialog existingValue = holderRef.value();

                    java.lang.reflect.Field toIdField = MappedRegistry.class.getDeclaredField("toId");
                    toIdField.setAccessible(true);
                    java.util.Map<net.minecraft.server.dialog.Dialog, Integer> toIdMap =
                            (java.util.Map<net.minecraft.server.dialog.Dialog, Integer>) toIdField.get(mappedRegistry);

                    java.lang.reflect.Field byValueField = MappedRegistry.class.getDeclaredField("byValue");
                    byValueField.setAccessible(true);
                    java.util.Map<net.minecraft.server.dialog.Dialog, Holder.Reference<net.minecraft.server.dialog.Dialog>> byValueMap =
                            (java.util.Map<net.minecraft.server.dialog.Dialog, Holder.Reference<net.minecraft.server.dialog.Dialog>>) byValueField.get(mappedRegistry);

                    int intId = -1;
                    if (existingValue != null) {
                        intId = mappedRegistry.getId(existingValue);
                    }
                    if (intId == -1) {
                        try {
                            java.lang.reflect.Field byIdField = MappedRegistry.class.getDeclaredField("byId");
                            byIdField.setAccessible(true);
                            java.util.List<?> byIdList = (java.util.List<?>) byIdField.get(mappedRegistry);
                            intId = byIdList.indexOf(holderRef);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    if (existingValue != null) {
                        toIdMap.remove(existingValue);
                        byValueMap.remove(existingValue);
                    }

                    if (intId != -1) {
                        toIdMap.put(parsedDialog, intId);
                    }
                    byValueMap.put(parsedDialog, holderRef);

                    java.lang.reflect.Field valueField = Holder.Reference.class.getDeclaredField("value");
                    valueField.setAccessible(true);
                    valueField.set(holderRef, parsedDialog);
                } else {
                    mappedRegistry.register(
                            resourceKey,
                            parsedDialog,
                            new RegistrationInfo(
                                    java.util.Optional.empty(),
                                    Lifecycle.stable()
                            )
                    );
                }
            } finally {
                frozenField.setBoolean(mappedRegistry, wasFrozen);
            }
        }
    }
}
