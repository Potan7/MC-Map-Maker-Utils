package com.potan.mapmakerutils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceKey;

public final class ReloadDiagnostics {
    private static final Pattern LINE = Pattern.compile("(?i)\\bline\\s+(\\d+)");
    private static final Pattern FUNCTION = Pattern.compile("Failed to load function ([a-z0-9_.-]+):([a-z0-9/._-]+)");
    private static final Pattern FILE = Pattern.compile("(?<![a-zA-Z0-9_./-])(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+\\.json");
    private static final Pattern ID = Pattern.compile("([a-z0-9_.-]+):([a-z0-9/._-]+)");
    private static final Set<String> DATA_LOGGERS = Set.of(
            "net.minecraft.resources.RegistryDataLoader", "net.minecraft.tags.TagLoader",
            "net.minecraft.server.ServerFunctionLibrary", "net.minecraft.server.ReloadableServerRegistries",
            "net.minecraft.server.ReloadableServerResources", "net.minecraft.server.ServerAdvancementManager",
            "net.minecraft.advancements.AdvancementTree", "net.minecraft.world.item.crafting.RecipeManager");
    private static final Map<MinecraftServer, Capture> ACTIVE = new WeakHashMap<>();
    public record Location(String file, int line) { }

    public static final class Capture extends AbstractAppender {
        private final Set<Location> entries = new LinkedHashSet<>();
        private final Logger root = (Logger) LogManager.getRootLogger();
        private boolean finished;

        Capture() {
            super("mapmakerutils-reload-" + UUID.randomUUID(), null, null, true, Property.EMPTY_ARRAY);
            start();
            root.addAppender(this);
        }

        @Override
        public synchronized void append(LogEvent event) {
            if (finished || !accepts(event.getLoggerName(), event.getLevel())) return;
            entries.addAll(locations(event.getLoggerName(), event.getMessage().getFormattedMessage(), event.getThrown()));
        }

        synchronized List<Location> finish(Throwable failure) {
            finished = true;
            root.removeAppender(this);
            stop();
            return List.copyOf(entries);
        }

        synchronized void add(String file, Throwable error) {
            if (!finished) entries.add(new Location(file, lineNumber(error)));
        }
    }

    public static synchronized Capture begin(MinecraftServer server) {
        if (ACTIVE.containsKey(server)) return null;
        Capture capture = new Capture();
        ACTIVE.put(server, capture);
        return capture;
    }

    public static void finish(MinecraftServer server, CommandSourceStack source, Capture capture, Throwable failure) {
        List<Location> errors = capture.finish(failure);
        synchronized (ReloadDiagnostics.class) {
            ACTIVE.remove(server, capture);
        }
        server.execute(() -> {
            for (Location error : errors) {
                source.sendFailure(error.line() > 0
                        ? Component.translatable("mapmakerutils.reload.location", error.file(), error.line())
                        : Component.translatable("mapmakerutils.reload.location_unknown", error.file()));
            }
        });
    }

    public static synchronized void recordRegistryError(ResourceKey<?> key, Throwable error) {
        for (Capture capture : ACTIVE.values()) capture.add(registryFile(key), error);
    }

    public static String registryFile(ResourceKey<?> key) {
        return "data/" + key.identifier().getNamespace() + "/" + key.registry().getPath()
                + "/" + key.identifier().getPath() + ".json";
    }

    static List<Location> locations(String logger, String message, Throwable error) {
        List<Location> locations = new ArrayList<>();
        int line = lineNumber(error);
        var function = FUNCTION.matcher(message);
        if (function.find()) {
            return List.of(new Location("data/" + function.group(1) + "/function/" + function.group(2) + ".mcfunction", line));
        }
        // Registry errors are recorded from their ResourceKeys, not guessed from the stack trace.
        if (logger.equals("net.minecraft.resources.RegistryDataLoader")) return locations;
        var files = FILE.matcher(message);
        while (files.find()) {
            String file = files.group();
            int colon = file.indexOf(':');
            if (colon >= 0) file = "data/" + file.substring(0, colon) + "/" + file.substring(colon + 1);
            locations.add(new Location(file, line));
        }
        if (locations.isEmpty()) {
            var id = ID.matcher(message);
            if (id.find()) locations.add(new Location(id.group(), line));
        }
        return locations;
    }

    static boolean accepts(String logger, Level level) {
        return logger != null && level.isMoreSpecificThan(Level.WARN)
                && (DATA_LOGGERS.contains(logger) || logger.startsWith("net.minecraft.server.packs.")
                || logger.startsWith("net.fabricmc.fabric.impl.resource."));
    }

    static int lineNumber(Throwable error) {
        Set<Throwable> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        while (error != null && seen.add(error)) {
            if (error.getMessage() != null) {
                var match = LINE.matcher(error.getMessage());
                if (match.find()) {
                    try { return Integer.parseInt(match.group(1)); }
                    catch (NumberFormatException ignored) { }
                }
            }
            error = error.getCause();
        }
        return -1;
    }
}
