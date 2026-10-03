package com.potan.mapmakerutils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

final class DevSetupConfig {
    private static final String DEFAULT_CONFIG = """
            {
              "devSetupCommands": [
                "gamerule minecraft:advance_time false",
                "time set noon",
                "gamerule minecraft:advance_weather false",
                "weather clear",
                "gamerule minecraft:spawn_mobs false",
                "gamerule minecraft:mob_griefing false"
              ]
            }
            """;

    static List<String> load(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, DEFAULT_CONFIG, StandardOpenOption.CREATE_NEW);
        }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(path));
            if (!root.isJsonObject() || !root.getAsJsonObject().has("devSetupCommands")
                    || !root.getAsJsonObject().get("devSetupCommands").isJsonArray()) {
                throw new IOException("devSetupCommands must be an array of command strings.");
            }
            List<String> commands = new ArrayList<>();
            for (JsonElement entry : root.getAsJsonObject().getAsJsonArray("devSetupCommands")) {
                if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                    throw new IOException("Each devSetupCommands entry must be a string.");
                }
                String command = entry.getAsString().strip();
                if (command.startsWith("/")) command = command.substring(1).strip();
                if (command.isEmpty()) throw new IOException("Commands must not be empty.");
                commands.add(command);
            }
            return List.copyOf(commands);
        } catch (JsonParseException e) {
            throw new IOException("Invalid JSON: " + e.getMessage(), e);
        }
    }
}
