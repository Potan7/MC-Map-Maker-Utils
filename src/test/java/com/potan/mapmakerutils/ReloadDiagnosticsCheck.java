package com.potan.mapmakerutils;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;

import java.util.List;

public class ReloadDiagnosticsCheck {
    public static void main(String[] args) {
        assert ReloadDiagnostics.accepts("net.minecraft.tags.TagLoader", Level.ERROR);
        assert !ReloadDiagnostics.accepts("unrelated.logger", Level.ERROR);
        assert !ReloadDiagnostics.accepts("net.minecraft.tags.TagLoader", Level.INFO);
        assert ReloadDiagnostics.registryFile(ResourceKey.create(Registries.ADVANCEMENT,
                Identifier.parse("test:folder/broken"))).equals("data/test/advancement/folder/broken.json");
        assert ReloadDiagnostics.registryFile(ResourceKey.create(Registries.ADVANCEMENT,
                Identifier.parse("potan:broken"))).equals("data/potan/advancement/broken.json");
        assert ReloadDiagnostics.registryFile(ResourceKey.create(Registries.RECIPE,
                Identifier.parse("potan:broken"))).equals("data/potan/recipe/broken.json");
        assert ReloadDiagnostics.registryFile(ResourceKey.create(Registries.FEATURE,
                Identifier.parse("potan:folder/broken"))).equals("data/potan/worldgen/feature/folder/broken.json");
        assert ReloadDiagnostics.lineNumber(new RuntimeException("outer",
                new IllegalArgumentException("Malformed JSON at line 17 column 4"))) == 17;
        assert ReloadDiagnostics.lineNumber(new RuntimeException("Missing required field")) == -1;
        var jsonError = new IllegalArgumentException("Malformed JSON at line 9 column 4");
        assert ReloadDiagnostics.locations("net.minecraft.tags.TagLoader",
                "Couldn't read tag list test:tag from test:tags/item/tag.json in data pack example", jsonError)
                .contains(new ReloadDiagnostics.Location("data/test/tags/item/tag.json", 9));
        assert ReloadDiagnostics.locations("net.minecraft.resources.RegistryDataLoader",
                "Registry loading errors: minecraft:advancement", jsonError).isEmpty();

        var capture = new ReloadDiagnostics.Capture();
        try {
            IllegalArgumentException parseError;
            try {
                CommandFunction.fromLines(Identifier.parse("test:broken"), new CommandDispatcher<CommandSourceStack>(),
                        null, List.of("# comment", "/say invalid"));
                throw new AssertionError("Invalid function accepted");
            } catch (IllegalArgumentException e) {
                parseError = e;
            }
            LogManager.getLogger("net.minecraft.server.ServerFunctionLibrary")
                    .error("Failed to load function test:broken", parseError);
            capture.add("data/test/recipe/broken.json", new IllegalArgumentException("Missing ingredient"));
            capture.add("data/test/recipe/broken.json", new IllegalArgumentException("Missing ingredient"));
            var found = capture.finish(null);
            assert found.size() == 2 : found;
            assert found.contains(new ReloadDiagnostics.Location("data/test/function/broken.mcfunction", 2));
            assert found.contains(new ReloadDiagnostics.Location("data/test/recipe/broken.json", -1));
            capture.append(Log4jLogEvent.newBuilder().setLoggerName("net.minecraft.server.ServerFunctionLibrary")
                    .setLevel(Level.ERROR).setMessage(new SimpleMessage("Failed to load function test:too_late")).build());
            assert capture.finish(null).size() == 2;
        } finally {
            capture.finish(null);
        }
        assert new ReloadDiagnostics.Capture().finish(null).isEmpty();
    }
}
