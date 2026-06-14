package com.potan.mapmakerutils.util;

import com.google.gson.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DialogJsonGenerator {

    // --- Data Model Definitions ---

    public static class DialogModel {
        public String type = "minecraft:notice";
        public String title = "Untitled Dialog";
        public String externalTitle = "";
        public String afterAction = "close";
        public boolean canCloseWithEscape = true;
        public boolean pause = true;

        public List<BodyElement> body = new ArrayList<>();
        public List<InputControl> inputs = new ArrayList<>();

        // Specific actions for type Notice & Confirmation
        public ClickAction noticeAction = new ClickAction("gui.ok", "close");
        public ClickAction confirmYes = new ClickAction("gui.yes", "close");
        public ClickAction confirmNo = new ClickAction("gui.no", "close");

        // Specific fields for Multi-Action, Links, List
        public List<ClickAction> actions = new ArrayList<>();
        public int columns = 2;
        public ClickAction exitAction = new ClickAction("gui.back", "close");

        // Server links & Dialog list
        public int buttonWidth = 150;
        public List<String> dialogs = new ArrayList<>(); // Registry IDs or inline JSONs
    }

    public static class BodyElement {
        public String type = "minecraft:plain_message"; // plain_message, item
        
        // plain_message
        public String contents = "";
        public int width = 200;

        // item
        public String itemId = "minecraft:stone";
        public int itemCount = 1;
        public String itemComponents = ""; // JSON representation of components
        public String description = "";
        public boolean showDecoration = true;
        public boolean showTooltip = true;
        public int itemWidth = 16;
        public int itemHeight = 16;
    }

    public static class InputControl {
        public String type = "minecraft:text"; // text, boolean, single_option, number_range
        public String key = "";
        public String label = "";
        public int width = 200;
        public boolean labelVisible = true;

        // text
        public String initialText = "";
        public int maxLength = 32;
        public boolean multiline = false;
        public int maxLines = 0;
        public int multilineHeight = 0;

        // boolean
        public boolean initialBoolean = false;
        public String onTrue = "true";
        public String onFalse = "false";

        // single_option
        public List<Option> options = new ArrayList<>();

        // number_range
        public String labelFormat = "options.generic_value";
        public float start = 0f;
        public float end = 10f;
        public Float step = null;
        public Float initialFloat = null;
    }

    public static class Option {
        public String id = "";
        public String display = "";
        public boolean initial = false;

        public Option(String id, String display, boolean initial) {
            this.id = id;
            this.display = display;
            this.initial = initial;
        }
    }

    public static class ClickAction {
        public String label = "";
        public String tooltip = "";
        public int width = 150;
        public ActionObject action = new ActionObject();

        public ClickAction(String label, String actionType) {
            this.label = label;
            this.action.type = actionType;
        }
    }

    public static class ActionObject {
        public String type = "close"; // close, none, wait_for_response, open_url, run_command, suggest_command, change_page, copy_to_clipboard, show_dialog, custom, dynamic/run_command, dynamic/custom
        public String url = "";
        public String command = "";
        public int page = 1;
        public String copyValue = "";
        public String showDialogId = ""; // namespace:id or inline json
        public String customId = "";
        public String customPayload = "";
        public String dynamicTemplate = "";
        public String dynamicCustomId = "";
        public String dynamicAdditions = "";
    }

    // --- Helper for Text Components ---

    private static JsonElement parseTextComponent(String input) {
        if (input == null) return new JsonPrimitive("");
        String trimmed = input.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                return JsonParser.parseString(trimmed);
            } catch (Exception e) {
                // Fall back to plain string
            }
        }
        return new JsonPrimitive(input);
    }

    private static String stringifyTextComponent(JsonElement element) {
        if (element == null) return "";
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return element.toString();
    }

    private static String cleanNamespacedType(String type) {
        if (type == null) return "";
        int separator = type.indexOf(':');
        return separator >= 0 ? type.substring(separator + 1) : type;
    }

    private static String normalizeAfterAction(String afterAction) {
        return switch (cleanNamespacedType(afterAction)) {
            case "none" -> "none";
            case "wait_for_response" -> "wait_for_response";
            default -> "close";
        };
    }

    private static String normalizeInputKey(String key, int index) {
        String value = key == null ? "" : key.replaceAll("[^A-Za-z0-9_]", "_");
        return value.isEmpty() ? "input_" + index : value;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // --- Serialization logic ---

    public static String serialize(DialogModel model, boolean pretty) {
        JsonObject root = new JsonObject();
        root.addProperty("type", model.type);
        root.add("title", parseTextComponent(model.title));

        if (model.externalTitle != null && !model.externalTitle.isEmpty()) {
            root.add("external_title", parseTextComponent(model.externalTitle));
        }

        if (!model.canCloseWithEscape) {
            root.addProperty("can_close_with_escape", false);
        }

        String afterAction = normalizeAfterAction(model.afterAction);
        boolean pause = afterAction.equals("none") ? false : model.pause;
        if (!pause) {
            root.addProperty("pause", false);
        }

        if (!afterAction.equals("close")) {
            root.addProperty("after_action", afterAction);
        }

        // Body Elements
        if (model.body != null && !model.body.isEmpty()) {
            JsonArray bodyArray = new JsonArray();
            for (BodyElement elem : model.body) {
                JsonObject elemObj = new JsonObject();
                elemObj.addProperty("type", elem.type);
                if (elem.type.equals("minecraft:plain_message")) {
                    elemObj.add("contents", parseTextComponent(elem.contents));
                    if (elem.width != 200) {
                        elemObj.addProperty("width", clamp(elem.width, 1, 1024));
                    }
                } else if (elem.type.equals("minecraft:item")) {
                    JsonObject itemObj = new JsonObject();
                    String itemId = elem.itemId == null || elem.itemId.isBlank() || elem.itemId.equals("minecraft:air")
                        ? "minecraft:stone"
                        : elem.itemId;
                    itemObj.addProperty("id", itemId);
                    if (elem.itemCount != 1) {
                        itemObj.addProperty("count", clamp(elem.itemCount, 1, 99));
                    }
                    if (elem.itemComponents != null && !elem.itemComponents.trim().isEmpty()) {
                        try {
                            JsonElement compJson = JsonParser.parseString(elem.itemComponents);
                            if (compJson.isJsonObject()) {
                                itemObj.add("components", compJson);
                            }
                        } catch (Exception e) {
                            // Invalid component patches are omitted.
                        }
                    }
                    elemObj.add("item", itemObj);

                    if (elem.description != null && !elem.description.isEmpty()) {
                        elemObj.add("description", parseTextComponent(elem.description));
                    }
                    if (!elem.showDecoration) {
                        elemObj.addProperty("show_decorations", false);
                    }
                    if (!elem.showTooltip) {
                        elemObj.addProperty("show_tooltip", false);
                    }
                    if (elem.itemWidth != 16) {
                        elemObj.addProperty("width", clamp(elem.itemWidth, 1, 256));
                    }
                    if (elem.itemHeight != 16) {
                        elemObj.addProperty("height", clamp(elem.itemHeight, 1, 256));
                    }
                }
                bodyArray.add(elemObj);
            }
            root.add("body", bodyArray);
        }

        // Inputs
        if (model.inputs != null && !model.inputs.isEmpty()) {
            JsonArray inputsArray = new JsonArray();
            for (int inputIndex = 0; inputIndex < model.inputs.size(); inputIndex++) {
                InputControl input = model.inputs.get(inputIndex);
                JsonObject inputObj = new JsonObject();
                inputObj.addProperty("type", input.type);
                inputObj.addProperty("key", normalizeInputKey(input.key, inputIndex + 1));
                inputObj.add("label", parseTextComponent(input.label));

                switch (input.type) {
                    case "minecraft:text":
                        if (input.width != 200) {
                            inputObj.addProperty("width", clamp(input.width, 1, 1024));
                        }
                        if (!input.labelVisible) {
                            inputObj.addProperty("label_visible", false);
                        }
                        int maxLength = Math.max(1, input.maxLength);
                        String initialText = input.initialText == null ? "" : input.initialText;
                        if (initialText.length() > maxLength) {
                            initialText = initialText.substring(0, maxLength);
                        }
                        if (!initialText.isEmpty()) {
                            inputObj.addProperty("initial", initialText);
                        }
                        if (maxLength != 32) {
                            inputObj.addProperty("max_length", maxLength);
                        }
                        if (input.multiline) {
                            JsonObject multilineObj = new JsonObject();
                            if (input.maxLines > 0) {
                                multilineObj.addProperty("max_lines", input.maxLines);
                            }
                            if (input.multilineHeight > 0) {
                                multilineObj.addProperty("height", clamp(input.multilineHeight, 1, 512));
                            }
                            inputObj.add("multiline", multilineObj);
                        }
                        break;
                    case "minecraft:boolean":
                        if (input.initialBoolean) {
                            inputObj.addProperty("initial", true);
                        }
                        if (input.onTrue != null && !input.onTrue.equals("true")) {
                            inputObj.addProperty("on_true", input.onTrue);
                        }
                        if (input.onFalse != null && !input.onFalse.equals("false")) {
                            inputObj.addProperty("on_false", input.onFalse);
                        }
                        break;
                    case "minecraft:single_option":
                        if (input.width != 200) {
                            inputObj.addProperty("width", clamp(input.width, 1, 1024));
                        }
                        if (!input.labelVisible) {
                            inputObj.addProperty("label_visible", false);
                        }
                        JsonArray optionsArray = new JsonArray();
                        List<Option> options = input.options.isEmpty()
                            ? List.of(new Option("option", "", true))
                            : input.options;
                        boolean initialWritten = false;
                        for (Option opt : options) {
                            JsonObject optObj = new JsonObject();
                            String optionId = opt.id == null || opt.id.isEmpty() ? "option" : opt.id;
                            optObj.addProperty("id", optionId);
                            if (opt.display != null && !opt.display.isEmpty()) {
                                optObj.add("display", parseTextComponent(opt.display));
                            }
                            if (opt.initial && !initialWritten) {
                                optObj.addProperty("initial", true);
                                initialWritten = true;
                            }
                            optionsArray.add(optObj);
                        }
                        inputObj.add("options", optionsArray);
                        break;
                    case "minecraft:number_range":
                        if (input.labelFormat != null && !input.labelFormat.equals("options.generic_value")) {
                            inputObj.addProperty("label_format", input.labelFormat);
                        }
                        if (input.width != 200) {
                            inputObj.addProperty("width", clamp(input.width, 1, 1024));
                        }
                        inputObj.addProperty("start", input.start);
                        inputObj.addProperty("end", input.end);
                        if (input.step != null && input.step > 0) {
                            inputObj.addProperty("step", input.step);
                        }
                        if (input.initialFloat != null
                            && input.initialFloat >= Math.min(input.start, input.end)
                            && input.initialFloat <= Math.max(input.start, input.end)) {
                            inputObj.addProperty("initial", input.initialFloat);
                        }
                        break;
                }
                inputsArray.add(inputObj);
            }
            root.add("inputs", inputsArray);
        }

        // Type Specific Click Actions / Fields
        switch (model.type) {
            case "minecraft:notice":
                if (!isDefaultNoticeAction(model.noticeAction)) {
                    root.add("action", serializeClickAction(model.noticeAction));
                }
                break;
            case "minecraft:confirmation":
                root.add("yes", serializeClickAction(model.confirmYes));
                root.add("no", serializeClickAction(model.confirmNo));
                break;
            case "minecraft:multi_action":
                JsonArray actArray = new JsonArray();
                for (ClickAction action : model.actions) {
                    actArray.add(serializeClickAction(action));
                }
                if (actArray.isEmpty()) {
                    actArray.add(serializeClickAction(new ClickAction("", "close")));
                }
                root.add("actions", actArray);
                if (model.columns != 2) {
                    root.addProperty("columns", Math.max(1, model.columns));
                }
                if (model.exitAction != null && !model.exitAction.label.isEmpty()) {
                    root.add("exit_action", serializeClickAction(model.exitAction));
                }
                break;
            case "minecraft:server_links":
                if (model.columns != 2) {
                    root.addProperty("columns", Math.max(1, model.columns));
                }
                if (model.buttonWidth != 150) {
                    root.addProperty("button_width", clamp(model.buttonWidth, 1, 1024));
                }
                if (model.exitAction != null && !model.exitAction.label.isEmpty()) {
                    root.add("exit_action", serializeClickAction(model.exitAction));
                }
                break;
            case "minecraft:dialog_list":
                JsonArray dlArray = new JsonArray();
                for (String dl : model.dialogs) {
                    if (dl.trim().startsWith("{")) {
                        try {
                            dlArray.add(JsonParser.parseString(dl));
                        } catch (Exception e) {
                            dlArray.add(dl);
                        }
                    } else {
                        dlArray.add(dl);
                    }
                }
                root.add("dialogs", dlArray);
                if (model.columns != 2) {
                    root.addProperty("columns", Math.max(1, model.columns));
                }
                if (model.buttonWidth != 150) {
                    root.addProperty("button_width", clamp(model.buttonWidth, 1, 1024));
                }
                if (model.exitAction != null && !model.exitAction.label.isEmpty()) {
                    root.add("exit_action", serializeClickAction(model.exitAction));
                }
                break;
        }

        Gson gson = pretty ? new GsonBuilder().setPrettyPrinting().create() : new Gson();
        return gson.toJson(root);
    }

    private static JsonObject serializeClickAction(ClickAction action) {
        JsonObject obj = new JsonObject();
        if (action == null) {
            obj.add("label", new JsonPrimitive(""));
            return obj;
        }
        
        String actType = action.action == null ? "" : action.action.type;
        String cleanType = cleanNamespacedType(actType);
        boolean hasAction = isUsableAction(action, cleanType);

        obj.add("label", parseButtonLabel(action.label));

        if (action.tooltip != null && !action.tooltip.isEmpty()) {
            obj.add("tooltip", parseTextComponent(action.tooltip));
        }
        if (action.width != 150) {
            obj.addProperty("width", clamp(action.width, 1, 1024));
        }

        if (!hasAction) {
            return obj;
        }

        JsonObject actObj = new JsonObject();
        actObj.addProperty("type", actType);

        switch (cleanType != null ? cleanType : "") {
            case "open_url":
                actObj.addProperty("url", action.action.url != null ? action.action.url : "");
                break;
            case "run_command":
                actObj.addProperty("command", action.action.command != null ? action.action.command : "");
                break;
            case "suggest_command":
                actObj.addProperty("command", action.action.command != null ? action.action.command : "");
                break;
            case "change_page":
                actObj.addProperty("page", action.action.page);
                break;
            case "copy_to_clipboard":
                actObj.addProperty("value", action.action.copyValue != null ? action.action.copyValue : "");
                break;
            case "show_dialog":
                String sdId = action.action.showDialogId != null ? action.action.showDialogId : "";
                if (sdId.trim().startsWith("{")) {
                    try {
                        actObj.add("dialog", JsonParser.parseString(sdId));
                    } catch (Exception e) {
                        actObj.addProperty("dialog", sdId);
                    }
                } else {
                    actObj.addProperty("dialog", sdId);
                }
                break;
            case "custom":
                actObj.addProperty("id", action.action.customId != null ? action.action.customId : "");
                if (action.action.customPayload != null && !action.action.customPayload.isEmpty()) {
                    if (action.action.customPayload.trim().startsWith("{")) {
                        try {
                            actObj.add("payload", JsonParser.parseString(action.action.customPayload));
                        } catch (Exception e) {
                            actObj.addProperty("payload", action.action.customPayload);
                        }
                    } else {
                        actObj.addProperty("payload", action.action.customPayload);
                    }
                }
                break;
            case "dynamic/run_command":
                actObj.addProperty("template", action.action.dynamicTemplate != null ? action.action.dynamicTemplate : "");
                break;
            case "dynamic/custom":
                actObj.addProperty("id", action.action.dynamicCustomId != null ? action.action.dynamicCustomId : "");
                if (action.action.dynamicAdditions != null && !action.action.dynamicAdditions.isEmpty()) {
                    try {
                        JsonElement additions = JsonParser.parseString(action.action.dynamicAdditions);
                        if (additions.isJsonObject()) {
                            actObj.add("additions", additions);
                        }
                    } catch (Exception e) {
                        // Invalid compound additions are omitted.
                    }
                }
                break;
        }

        obj.add("action", actObj);
        return obj;
    }

    private static boolean isUsableAction(ClickAction action, String cleanType) {
        if (action.action == null) return false;
        return switch (cleanType) {
            case "open_url" -> isValidUrl(action.action.url);
            case "run_command", "suggest_command", "copy_to_clipboard", "dynamic/run_command" -> true;
            case "change_page" -> action.action.page > 0;
            case "show_dialog" -> isInlineObject(action.action.showDialogId) || isValidIdentifier(action.action.showDialogId);
            case "custom" -> isValidIdentifier(action.action.customId);
            case "dynamic/custom" -> isValidIdentifier(action.action.dynamicCustomId);
            default -> false;
        };
    }

    private static boolean isValidUrl(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            URI uri = URI.create(value);
            return uri.isAbsolute();
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean isValidIdentifier(String value) {
        return value != null && value.matches("^(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+$");
    }

    private static boolean isInlineObject(String value) {
        if (value == null || !value.trim().startsWith("{")) return false;
        try {
            return JsonParser.parseString(value).isJsonObject();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isDefaultNoticeAction(ClickAction action) {
        return action != null
            && action.action != null
            && cleanNamespacedType(action.action.type).equals("close")
            && "gui.ok".equals(action.label)
            && (action.tooltip == null || action.tooltip.isEmpty())
            && action.width == 150;
    }

    private static JsonElement parseButtonLabel(String label) {
        String value = label == null ? "" : label;
        if (value.equals("gui.ok") || value.equals("gui.yes") || value.equals("gui.no") || value.equals("gui.back")) {
            JsonObject translated = new JsonObject();
            translated.addProperty("translate", value);
            return translated;
        }
        return parseTextComponent(value);
    }

    private static String stringifyButtonLabel(JsonElement element) {
        if (element != null && element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.size() == 1 && object.has("translate") && object.get("translate").isJsonPrimitive()) {
                return object.get("translate").getAsString();
            }
        }
        return stringifyTextComponent(element);
    }

    // --- Deserialization logic ---

    public static DialogModel deserialize(String jsonString) {
        DialogModel model = new DialogModel();
        JsonElement rootEl = JsonParser.parseString(jsonString);
        if (!rootEl.isJsonObject()) return model;
        JsonObject root = rootEl.getAsJsonObject();

        if (root.has("type")) {
            model.type = root.get("type").getAsString();
        }
        if (root.has("title")) {
            model.title = stringifyTextComponent(root.get("title"));
        }
        if (root.has("external_title")) {
            model.externalTitle = stringifyTextComponent(root.get("external_title"));
        }
        if (root.has("can_close_with_escape")) {
            model.canCloseWithEscape = root.get("can_close_with_escape").getAsBoolean();
        }
        if (root.has("pause")) {
            model.pause = root.get("pause").getAsBoolean();
        }
        if (root.has("after_action")) {
            model.afterAction = root.get("after_action").getAsString();
        }

        // Body Elements
        if (root.has("body")) {
            JsonElement bodyEl = root.get("body");
            JsonArray bodyArray = bodyEl.isJsonArray() ? bodyEl.getAsJsonArray() : new JsonArray();
            if (bodyEl.isJsonObject()) bodyArray.add(bodyEl.getAsJsonObject());

            for (JsonElement e : bodyArray) {
                if (!e.isJsonObject()) continue;
                JsonObject elemObj = e.getAsJsonObject();
                BodyElement elem = new BodyElement();
                if (elemObj.has("type")) {
                    elem.type = elemObj.get("type").getAsString();
                }
                
                if (elem.type.equals("minecraft:plain_message")) {
                    if (elemObj.has("contents")) {
                        elem.contents = stringifyTextComponent(elemObj.get("contents"));
                    }
                    if (elemObj.has("width")) {
                        elem.width = elemObj.get("width").getAsInt();
                    }
                } else if (elem.type.equals("minecraft:item")) {
                    if (elemObj.has("item")) {
                        JsonObject itemObj = elemObj.getAsJsonObject("item");
                        if (itemObj.has("id")) elem.itemId = itemObj.get("id").getAsString();
                        if (itemObj.has("count")) elem.itemCount = itemObj.get("count").getAsInt();
                        if (itemObj.has("components")) {
                            elem.itemComponents = itemObj.get("components").toString();
                        }
                    }
                    if (elemObj.has("description")) {
                        elem.description = stringifyTextComponent(elemObj.get("description"));
                    }
                    if (elemObj.has("show_decorations")) {
                        elem.showDecoration = elemObj.get("show_decorations").getAsBoolean();
                    }
                    if (elemObj.has("show_tooltip")) {
                        elem.showTooltip = elemObj.get("show_tooltip").getAsBoolean();
                    }
                    if (elemObj.has("width")) {
                        elem.itemWidth = elemObj.get("width").getAsInt();
                    }
                    if (elemObj.has("height")) {
                        elem.itemHeight = elemObj.get("height").getAsInt();
                    }
                }
                model.body.add(elem);
            }
        }

        // Inputs
        if (root.has("inputs")) {
            JsonElement inputsEl = root.get("inputs");
            JsonArray inputsArray = inputsEl.isJsonArray() ? inputsEl.getAsJsonArray() : new JsonArray();
            for (JsonElement e : inputsArray) {
                if (!e.isJsonObject()) continue;
                JsonObject inputObj = e.getAsJsonObject();
                InputControl input = new InputControl();
                if (inputObj.has("type")) input.type = inputObj.get("type").getAsString();
                if (inputObj.has("key")) input.key = inputObj.get("key").getAsString();
                if (inputObj.has("label")) input.label = stringifyTextComponent(inputObj.get("label"));

                switch (input.type) {
                    case "minecraft:text":
                        if (inputObj.has("width")) input.width = inputObj.get("width").getAsInt();
                        if (inputObj.has("label_visible")) input.labelVisible = inputObj.get("label_visible").getAsBoolean();
                        if (inputObj.has("initial")) input.initialText = inputObj.get("initial").getAsString();
                        if (inputObj.has("max_length")) input.maxLength = inputObj.get("max_length").getAsInt();
                        if (inputObj.has("multiline")) {
                            input.multiline = true;
                            JsonElement mlEl = inputObj.get("multiline");
                            if (mlEl.isJsonObject()) {
                                JsonObject mlObj = mlEl.getAsJsonObject();
                                if (mlObj.has("max_lines")) input.maxLines = mlObj.get("max_lines").getAsInt();
                                if (mlObj.has("height")) input.multilineHeight = mlObj.get("height").getAsInt();
                            }
                        }
                        break;
                    case "minecraft:boolean":
                        if (inputObj.has("initial")) input.initialBoolean = inputObj.get("initial").getAsBoolean();
                        if (inputObj.has("on_true")) input.onTrue = inputObj.get("on_true").getAsString();
                        if (inputObj.has("on_false")) input.onFalse = inputObj.get("on_false").getAsString();
                        break;
                    case "minecraft:single_option":
                        if (inputObj.has("width")) input.width = inputObj.get("width").getAsInt();
                        if (inputObj.has("label_visible")) input.labelVisible = inputObj.get("label_visible").getAsBoolean();
                        if (inputObj.has("options")) {
                            JsonArray optArray = inputObj.getAsJsonArray("options");
                            for (JsonElement o : optArray) {
                                if (!o.isJsonObject()) continue;
                                JsonObject oObj = o.getAsJsonObject();
                                String id = oObj.has("id") ? oObj.get("id").getAsString() : "";
                                String disp = oObj.has("display") ? stringifyTextComponent(oObj.get("display")) : "";
                                boolean init = oObj.has("initial") && oObj.get("initial").getAsBoolean();
                                input.options.add(new Option(id, disp, init));
                            }
                        }
                        break;
                    case "minecraft:number_range":
                        if (inputObj.has("label_format")) input.labelFormat = inputObj.get("label_format").getAsString();
                        if (inputObj.has("width")) input.width = inputObj.get("width").getAsInt();
                        if (inputObj.has("start")) input.start = inputObj.get("start").getAsFloat();
                        if (inputObj.has("end")) input.end = inputObj.get("end").getAsFloat();
                        if (inputObj.has("step")) input.step = inputObj.get("step").getAsFloat();
                        if (inputObj.has("initial")) {
                            input.initialFloat = inputObj.get("initial").getAsFloat();
                        }
                        break;
                }
                model.inputs.add(input);
            }
        }

        // Actions / Custom fields depending on Type
        switch (model.type) {
            case "minecraft:notice":
                if (root.has("action")) {
                    model.noticeAction = deserializeClickAction(root.getAsJsonObject("action"));
                    if (model.noticeAction.label.isEmpty()) {
                        model.noticeAction.label = "gui.ok";
                    }
                }
                break;
            case "minecraft:confirmation":
                if (root.has("yes")) {
                    model.confirmYes = deserializeClickAction(root.getAsJsonObject("yes"));
                    if (model.confirmYes.label.isEmpty()) {
                        model.confirmYes.label = "gui.yes";
                    }
                }
                if (root.has("no")) {
                    model.confirmNo = deserializeClickAction(root.getAsJsonObject("no"));
                    if (model.confirmNo.label.isEmpty()) {
                        model.confirmNo.label = "gui.no";
                    }
                }
                break;
            case "minecraft:multi_action":
                if (root.has("actions")) {
                    JsonArray actArray = root.getAsJsonArray("actions");
                    for (JsonElement a : actArray) {
                        if (a.isJsonObject()) model.actions.add(deserializeClickAction(a.getAsJsonObject()));
                    }
                }
                if (root.has("columns")) model.columns = root.get("columns").getAsInt();
                if (root.has("exit_action")) {
                    model.exitAction = deserializeClickAction(root.getAsJsonObject("exit_action"));
                    if (model.exitAction.label.isEmpty()) {
                        model.exitAction.label = "gui.back";
                    }
                }
                break;
            case "minecraft:server_links":
                if (root.has("columns")) model.columns = root.get("columns").getAsInt();
                if (root.has("button_width")) model.buttonWidth = root.get("button_width").getAsInt();
                if (root.has("exit_action")) {
                    model.exitAction = deserializeClickAction(root.getAsJsonObject("exit_action"));
                    if (model.exitAction.label.isEmpty()) {
                        model.exitAction.label = "gui.back";
                    }
                }
                break;
            case "minecraft:dialog_list":
                if (root.has("dialogs")) {
                    JsonArray dlArray = root.getAsJsonArray("dialogs");
                    for (JsonElement d : dlArray) {
                        model.dialogs.add(d.isJsonObject() ? d.toString() : d.getAsString());
                    }
                }
                if (root.has("columns")) model.columns = root.get("columns").getAsInt();
                if (root.has("button_width")) model.buttonWidth = root.get("button_width").getAsInt();
                if (root.has("exit_action")) {
                    model.exitAction = deserializeClickAction(root.getAsJsonObject("exit_action"));
                    if (model.exitAction.label.isEmpty()) {
                        model.exitAction.label = "gui.back";
                    }
                }
                break;
        }

        return model;
    }

    private static ClickAction deserializeClickAction(JsonObject obj) {
        String label = obj.has("label") ? stringifyButtonLabel(obj.get("label")) : "";
        ClickAction action = new ClickAction(label, "close");
        
        if (obj.has("tooltip")) action.tooltip = stringifyTextComponent(obj.get("tooltip"));
        if (obj.has("width")) action.width = obj.get("width").getAsInt();
        
        if (obj.has("action")) {
            JsonObject actObj = obj.getAsJsonObject("action");
            if (actObj.has("type")) action.action.type = actObj.get("type").getAsString();
            
            switch (cleanNamespacedType(action.action.type)) {
                case "open_url":
                    if (actObj.has("url")) action.action.url = actObj.get("url").getAsString();
                    break;
                case "run_command":
                case "suggest_command":
                    if (actObj.has("command")) action.action.command = actObj.get("command").getAsString();
                    break;
                case "change_page":
                    if (actObj.has("page")) action.action.page = actObj.get("page").getAsInt();
                    break;
                case "copy_to_clipboard":
                    if (actObj.has("value")) action.action.copyValue = actObj.get("value").getAsString();
                    break;
                case "show_dialog":
                    if (actObj.has("dialog")) {
                        JsonElement sdEl = actObj.get("dialog");
                        action.action.showDialogId = sdEl.isJsonObject() ? sdEl.toString() : sdEl.getAsString();
                    }
                    break;
                case "custom":
                    if (actObj.has("id")) action.action.customId = actObj.get("id").getAsString();
                    if (actObj.has("payload")) {
                        JsonElement pEl = actObj.get("payload");
                        action.action.customPayload = pEl.isJsonObject() ? pEl.toString() : pEl.getAsString();
                    }
                    break;
                case "dynamic/run_command":
                    if (actObj.has("template")) action.action.dynamicTemplate = actObj.get("template").getAsString();
                    break;
                case "dynamic/custom":
                    if (actObj.has("id")) action.action.dynamicCustomId = actObj.get("id").getAsString();
                    if (actObj.has("additions")) {
                        action.action.dynamicAdditions = actObj.get("additions").toString();
                    }
                    break;
            }
        }
        
        return action;
    }
}
