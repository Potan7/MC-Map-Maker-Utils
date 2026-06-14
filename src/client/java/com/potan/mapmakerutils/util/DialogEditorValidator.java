package com.potan.mapmakerutils.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.potan.mapmakerutils.util.DialogJsonGenerator.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class DialogEditorValidator {

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }

    public static ValidationResult validateForExport(DialogModel model, RegistryAccess registryAccess) {
        if (model.type.equals("minecraft:multi_action") && model.actions.isEmpty()) {
            return ValidationResult.error("Multi-action dialogs require at least one action.");
        }

        Set<String> keys = new HashSet<>();
        for (int i = 0; i < model.inputs.size(); i++) {
            InputControl input = model.inputs.get(i);
            String key = input.key == null ? "" : input.key;
            if (!key.matches("^[A-Za-z0-9_]+$")) {
                return ValidationResult.error("Input #" + (i + 1) + " key must contain only letters, numbers, and underscores.");
            }
            if (!keys.add(key)) {
                return ValidationResult.error("Input key '" + key + "' is duplicated.");
            }
            if (input.type.equals("minecraft:single_option")) {
                if (input.options.isEmpty()) {
                    return ValidationResult.error("Single-option input #" + (i + 1) + " requires at least one option.");
                }
                long initialCount = input.options.stream().filter(option -> option.initial).count();
                if (initialCount > 1) {
                    return ValidationResult.error("Single-option input #" + (i + 1) + " has multiple initial options.");
                }
                for (Option option : input.options) {
                    if (option.id == null || option.id.isEmpty()) {
                        return ValidationResult.error("Single-option input #" + (i + 1) + " contains an empty option ID.");
                    }
                }
            }
            if (input.type.equals("minecraft:number_range")) {
                if (input.step != null && input.step <= 0) {
                    return ValidationResult.error("Number-range input #" + (i + 1) + " step must be greater than 0.");
                }
                if (input.initialFloat != null
                        && (input.initialFloat < Math.min(input.start, input.end)
                        || input.initialFloat > Math.max(input.start, input.end))) {
                    return ValidationResult.error("Number-range input #" + (i + 1) + " initial value is outside its range.");
                }
            }
            if (input.type.equals("minecraft:text") && input.initialText.length() > input.maxLength) {
                return ValidationResult.error("Text input #" + (i + 1) + " initial value exceeds its maximum length.");
            }
        }

        if (model.type.equals("minecraft:dialog_list")) {
            if (model.dialogs.isEmpty()) {
                return ValidationResult.error("Dialog-list dialogs require at least one dialog.");
            }
            for (String dialog : model.dialogs) {
                if (!isValidIdentifier(dialog) && !isJsonObject(dialog)) {
                    return ValidationResult.error("Dialog list contains an invalid dialog ID or inline dialog object.");
                }
            }
        }

        for (int i = 0; i < model.body.size(); i++) {
            BodyElement body = model.body.get(i);
            if (body.type.equals("minecraft:item")) {
                if (!isValidIdentifier(body.itemId) || body.itemId.equals("minecraft:air")) {
                    return ValidationResult.error("Item body #" + (i + 1) + " has an invalid or empty item ID.");
                }
                if (!isJsonObjectOrEmpty(body.itemComponents)) {
                    return ValidationResult.error("Item body #" + (i + 1) + " components must be a JSON object.");
                }
            }
        }

        ValidationResult actionResult;
        switch (model.type) {
            case "minecraft:notice":
                actionResult = validateAction(model.noticeAction);
                if (!actionResult.isValid()) return actionResult;
                break;
            case "minecraft:confirmation":
                actionResult = validateAction(model.confirmYes);
                if (!actionResult.isValid()) return actionResult;
                actionResult = validateAction(model.confirmNo);
                if (!actionResult.isValid()) return actionResult;
                break;
            case "minecraft:multi_action":
                actionResult = validateAction(model.exitAction);
                if (!actionResult.isValid()) return actionResult;
                for (ClickAction action : model.actions) {
                    actionResult = validateAction(action);
                    if (!actionResult.isValid()) return actionResult;
                }
                break;
            case "minecraft:server_links", "minecraft:dialog_list":
                actionResult = validateAction(model.exitAction);
                if (!actionResult.isValid()) return actionResult;
                break;
        }

        try {
            JsonElement json = JsonParser.parseString(DialogJsonGenerator.serialize(model, false));
            com.mojang.serialization.DynamicOps<JsonElement> ops = JsonOps.INSTANCE;
            if (registryAccess != null) {
                ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
            }
            var result = net.minecraft.server.dialog.Dialog.DIRECT_CODEC.parse(ops, json);
            String codecError = result.error().map(error -> error.message()).orElse("");
            if (!codecError.isEmpty() && !codecError.contains("Can't find value")) {
                throw new IllegalArgumentException(codecError);
            }
        } catch (Exception e) {
            return ValidationResult.error("Minecraft rejected this dialog: " + e.getMessage());
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateAction(ClickAction action) {
        if (action == null || action.action == null) return ValidationResult.success();
        String type = cleanType(action.action.type);
        switch (type) {
            case "", "close":
                return ValidationResult.success();
            case "none", "wait_for_response":
                return ValidationResult.error("'" + type + "' can only be used as an after action.");
            case "open_url":
                if (!isValidUrl(action.action.url)) {
                    return ValidationResult.error("Open URL action contains an invalid URL.");
                }
                return ValidationResult.success();
            case "show_dialog":
                if (!isValidIdentifier(action.action.showDialogId) && !isJsonObject(action.action.showDialogId)) {
                    return ValidationResult.error("Show-dialog action requires a valid dialog ID or inline dialog object.");
                }
                return ValidationResult.success();
            case "custom":
                if (!isValidIdentifier(action.action.customId) || !isJsonValueOrEmpty(action.action.customPayload)) {
                    return ValidationResult.error("Custom action contains an invalid ID or payload.");
                }
                return ValidationResult.success();
            case "dynamic/custom":
                if (!isValidIdentifier(action.action.dynamicCustomId) || !isJsonObjectOrEmpty(action.action.dynamicAdditions)) {
                    return ValidationResult.error("Dynamic custom action contains an invalid ID or additions object.");
                }
                return ValidationResult.success();
            case "run_command", "suggest_command", "change_page", "copy_to_clipboard", "dynamic/run_command":
                return ValidationResult.success();
            default:
                return ValidationResult.error("Unknown button action type: " + action.action.type);
        }
    }

    public static String cleanType(String type) {
        if (type == null) return "";
        int separator = type.indexOf(':');
        return separator >= 0 ? type.substring(separator + 1) : type;
    }

    public static boolean isValidIdentifier(String value) {
        return value != null && value.matches("^(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+$");
    }

    public static boolean isValidUrl(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            return URI.create(value).isAbsolute();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isJsonObjectOrEmpty(String value) {
        if (value == null || value.isBlank()) return true;
        return isJsonObject(value);
    }

    public static boolean isJsonObject(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            return JsonParser.parseString(value).isJsonObject();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isJsonValueOrEmpty(String value) {
        if (value == null || value.isBlank()) return true;
        try {
            JsonParser.parseString(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
