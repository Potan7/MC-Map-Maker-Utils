package com.potan.mapmakerutils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DevSetupConfigCheck {
    public static void main(String[] args) throws IOException {
        Path directory = Files.createTempDirectory("mmu-devsetup-check-");
        Path config = directory.resolve("mapmakerutils.json");
        try {
            assert DevSetupConfig.load(config).equals(List.of(
                    "gamerule minecraft:advance_time false", "time set noon",
                    "gamerule minecraft:advance_weather false", "weather clear",
                    "gamerule minecraft:spawn_mobs false", "gamerule minecraft:mob_griefing false"));
            Files.writeString(config, "{\"devSetupCommands\":[\" /say custom \",\"gamemode creative @s\"]}");
            assert DevSetupConfig.load(config).equals(List.of("say custom", "gamemode creative @s"));
            Files.writeString(config, "{\"devSetupCommands\":[]}");
            assert DevSetupConfig.load(config).isEmpty();
            for (String invalid : List.of("{", "null", "{}", "{\"devSetupCommands\":true}",
                    "{\"devSetupCommands\":[null]}", "{\"devSetupCommands\":[12]}",
                    "{\"devSetupCommands\":[\"say valid\",\" / \"]}")) {
                Files.writeString(config, invalid);
                try {
                    DevSetupConfig.load(config);
                    throw new AssertionError("Invalid config accepted: " + invalid);
                } catch (IOException expected) {
                    assert Files.readString(config).equals(invalid) : "Invalid config was overwritten";
                }
            }
        } finally {
            Files.deleteIfExists(config);
            Files.delete(directory);
        }
    }
}
