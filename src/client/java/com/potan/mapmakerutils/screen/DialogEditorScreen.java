package com.potan.mapmakerutils.screen;

import com.potan.mapmakerutils.ModGlobalState;
import com.potan.mapmakerutils.util.DialogJsonGenerator;
import com.potan.mapmakerutils.util.DialogJsonGenerator.*;
import com.potan.mapmakerutils.util.DialogEditorValidator;
import com.potan.mapmakerutils.util.DialogDatapackManager;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.Registries;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistrationInfo;
import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceKey;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DialogEditorScreen extends Screen {

    private final DialogModel model;
    private String namespace = "my_datapack";
    private String filename = "new_dialog";
    private String datapackName = "mapmakerutils_generated";

    // Navigation state
    private enum ScreenState {
        MAIN, EDIT_BODY, EDIT_INPUT, EDIT_ACTIONS, EDIT_SINGLE_ACTION, LIST_EXISTING
    }
    private ScreenState state = ScreenState.MAIN;
    private int selectedElementIndex = -1;
    private ClickAction selectedClickAction = null;
    private net.minecraft.client.gui.components.CommandSuggestions commandSuggestions = null;

    // UI Widgets for MAIN state
    private EditBox namespaceField;
    private EditBox filenameField;
    private EditBox datapackField;
    private EditBox titleField;
    private EditBox externalTitleField;
    private Button typeButton;
    private Button escapeButton;
    private Button pauseButton;
    private Button afterActionButton;
    private Button addBodyBtn;
    private Button addInputBtn;
    private Button editActionsBtn;
    private Button loadListBtn;
    private Button copyJsonBtn;
    private Button saveBtn;
    private Button showBtn;
    private EditBox actionColumnsField;
    private EditBox actionButtonWidthField;

    private final java.util.List<Button> bodyEditButtons = new java.util.ArrayList<>();
    private final java.util.List<Button> bodyDeleteButtons = new java.util.ArrayList<>();
    private final java.util.List<Button> inputEditButtons = new java.util.ArrayList<>();
    private final java.util.List<Button> inputDeleteButtons = new java.util.ArrayList<>();

    private int scrollAmount = 0;
    private boolean isScrolling = false;

    // Sub-editor widgets (generic reused widgets)
    private EditBox editBox1; // contents / itemId / key
    private EditBox editBox2; // width / itemCount / label
    private EditBox editBox3; // components / width
    private EditBox editBox4; // description / label_format / url / command
    private EditBox editBox5; // start / tooltip
    private EditBox editBox6; // end / copyValue
    private EditBox editBox7; // step / showDialogId
    private EditBox editBox8; // initialFloat / customId

    private Button subDropdownButton1; // body type / input type / action type
    private Button subCheckboxButton1; // showDecoration / labelVisible / initialBoolean
    private Button subCheckboxButton2; // showTooltip / multiline / optionInitial

    private final List<net.minecraft.client.gui.components.AbstractWidget> mainWidgets = new ArrayList<>();
    private final List<net.minecraft.client.gui.components.AbstractWidget> subWidgets = new ArrayList<>();
    private final List<net.minecraft.client.gui.components.AbstractWidget> listWidgets = new ArrayList<>();
    private final List<net.minecraft.client.gui.components.AbstractWidget> actionsWidgets = new ArrayList<>();
    private final List<String> existingDialogsList = new ArrayList<>();
    private String editorMessage = "";
    private boolean editorMessageIsError = false;

    public DialogEditorScreen(DialogModel model) {
        super(Component.literal("Dialog Editor"));
        this.model = model != null ? model : new DialogModel();
    }

    public DialogEditorScreen(DialogModel model, String namespace, String filename) {
        this(model);
        this.namespace = namespace;
        this.filename = filename;
    }

    public DialogEditorScreen(DialogModel model, String namespace, String filename, String datapackName) {
        this(model);
        this.namespace = namespace;
        this.filename = filename;
        this.datapackName = datapackName;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.mainWidgets.clear();
        this.subWidgets.clear();
        this.listWidgets.clear();
        this.actionsWidgets.clear();
        this.actionColumnsField = null;
        this.actionButtonWidthField = null;

        bodyEditButtons.clear();
        bodyDeleteButtons.clear();
        inputEditButtons.clear();
        inputDeleteButtons.clear();

        int splitX = this.width / 2;

        // --- 1. MAIN STATE WIDGETS ---
        namespaceField = new EditBox(this.font, 10, 20, (splitX - 30) / 2, 14, Component.translatable("mapmakerutils.ui.editor.namespace"));
        namespaceField.setValue(this.namespace);
        namespaceField.setResponder(val -> this.namespace = val);
        this.addWidgetToMain(namespaceField);

        filenameField = new EditBox(this.font, splitX / 2 + 5, 20, (splitX - 30) / 2, 14, Component.translatable("mapmakerutils.ui.editor.filename"));
        filenameField.setValue(this.filename);
        filenameField.setResponder(val -> this.filename = val);
        this.addWidgetToMain(filenameField);

        datapackField = new EditBox(this.font, 10, 50, splitX - 20, 14, Component.translatable("mapmakerutils.ui.editor.datapack_folder"));
        datapackField.setValue(this.datapackName);
        datapackField.setResponder(val -> this.datapackName = val);
        this.addWidgetToMain(datapackField);

        titleField = new EditBox(this.font, 10, 80, splitX - 20, 14, Component.translatable("mapmakerutils.ui.editor.title"));
        titleField.setValue(model.title);
        titleField.setResponder(val -> model.title = val);
        this.addWidgetToMain(titleField);

        externalTitleField = new EditBox(this.font, 10, 110, splitX - 20, 14, Component.translatable("mapmakerutils.ui.editor.external_title"));
        externalTitleField.setValue(model.externalTitle);
        externalTitleField.setResponder(val -> model.externalTitle = val);
        this.addWidgetToMain(externalTitleField);

        typeButton = Button.builder(Component.literal("Type: " + model.type.replace("minecraft:", "")), b -> {
            cycleDialogType();
            b.setMessage(Component.literal("Type: " + model.type.replace("minecraft:", "")));
            init();
        }).bounds(10, 135, splitX - 20, 14).build();
        this.addWidgetToMain(typeButton);

        escapeButton = Button.builder(Component.literal("Esc Closes: " + model.canCloseWithEscape), b -> {
            model.canCloseWithEscape = !model.canCloseWithEscape;
            b.setMessage(Component.literal("Esc Closes: " + model.canCloseWithEscape));
        }).bounds(10, 155, (splitX - 25) / 2, 14).build();
        this.addWidgetToMain(escapeButton);

        pauseButton = Button.builder(Component.literal("Pauses Game: " + model.pause), b -> {
            model.pause = !model.pause;
            b.setMessage(Component.literal("Pauses Game: " + model.pause));
        }).bounds(splitX / 2 + 5, 155, (splitX - 25) / 2, 14).build();
        this.addWidgetToMain(pauseButton);

        afterActionButton = Button.builder(Component.literal("After: " + model.afterAction), b -> {
            cycleAfterAction();
            b.setMessage(Component.literal("After: " + model.afterAction));
        }).bounds(10, 175, splitX - 20, 14).build();
        this.addWidgetToMain(afterActionButton);

        // Scrollable Lists (dynamic buttons added here)
        addBodyBtn = Button.builder(Component.literal("+ Body Element"), b -> {
            BodyElement elem = new BodyElement();
            model.body.add(elem);
            selectedElementIndex = model.body.size() - 1;
            state = ScreenState.EDIT_BODY;
            init();
        }).bounds(10, 200, (splitX - 25) / 2, 14).build();
        this.addWidgetToMain(addBodyBtn);

        addInputBtn = Button.builder(Component.literal("+ Input Control"), b -> {
            InputControl input = new InputControl();
            model.inputs.add(input);
            selectedElementIndex = model.inputs.size() - 1;
            state = ScreenState.EDIT_INPUT;
            init();
        }).bounds(splitX / 2 + 5, 200, (splitX - 25) / 2, 14).build();
        this.addWidgetToMain(addInputBtn);

        editActionsBtn = Button.builder(Component.literal("Edit Footer Actions"), b -> {
            state = ScreenState.EDIT_ACTIONS;
            init();
        }).bounds(10, 220, splitX - 20, 14).build();
        this.addWidgetToMain(editActionsBtn);

        // Load Existing Dialog Button (Placed under WYSIWYG Preview)
        int previewWidth = 220;
        int previewX = splitX + (this.width - splitX - previewWidth) / 2;
        int dynamicHeight = 50;
        for (BodyElement elem : model.body) {
            if (elem.type.equals("minecraft:plain_message")) {
                var wrapped = this.font.split(Component.literal(elem.contents), previewWidth - 20);
                dynamicHeight += wrapped.size() * 9 + 4;
            } else if (elem.type.equals("minecraft:item")) {
                dynamicHeight += 20;
            }
        }
        for (InputControl input : model.inputs) {
            dynamicHeight += 22;
        }
        dynamicHeight += 25;
        int previewY = this.height / 2 - dynamicHeight / 2;
        int loadBtnY = previewY + dynamicHeight + 8;

        loadListBtn = Button.builder(Component.literal("Load Existing Dialog"), b -> {
            state = ScreenState.LIST_EXISTING;
            init();
        }).bounds(previewX, loadBtnY, previewWidth, 16).build();
        this.addWidgetToMain(loadListBtn);

        // Dynamic Edit/Delete buttons for Body elements
        int startY = 245;
        for (int i = 0; i < model.body.size(); i++) {
            final int index = i;
            int btnY = startY + i * 20 - 4;
            Button editBtn = Button.builder(Component.literal("Edit"), b -> {
                selectedElementIndex = index;
                state = ScreenState.EDIT_BODY;
                init();
            }).bounds(splitX - 100, btnY, 35, 14).build();
            this.addWidgetToMain(editBtn);
            bodyEditButtons.add(editBtn);

            Button deleteBtn = Button.builder(Component.literal("Del"), b -> {
                model.body.remove(index);
                init();
            }).bounds(splitX - 60, btnY, 35, 14).build();
            this.addWidgetToMain(deleteBtn);
            bodyDeleteButtons.add(deleteBtn);
        }

        // Dynamic Edit/Delete buttons for Input elements
        int inputStartY = startY + model.body.size() * 20 + 20;
        for (int i = 0; i < model.inputs.size(); i++) {
            final int index = i;
            int btnY = inputStartY + i * 20 - 4;
            Button editBtn = Button.builder(Component.literal("Edit"), b -> {
                selectedElementIndex = index;
                state = ScreenState.EDIT_INPUT;
                init();
            }).bounds(splitX - 100, btnY, 35, 14).build();
            this.addWidgetToMain(editBtn);
            inputEditButtons.add(editBtn);

            Button deleteBtn = Button.builder(Component.literal("Del"), b -> {
                model.inputs.remove(index);
                init();
            }).bounds(splitX - 60, btnY, 35, 14).build();
            this.addWidgetToMain(deleteBtn);
            inputDeleteButtons.add(deleteBtn);
        }

        // Footer Actions
        int footerBtnWidth = (splitX - 30) / 3;
        copyJsonBtn = Button.builder(Component.literal("Copy Inline JSON"), b -> {
            if (!validateForExport()) return;
            String json = DialogJsonGenerator.serialize(model, false);
            this.minecraft.keyboardHandler.setClipboard(json);
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.literal("§a[MapMakerUtils] Inline JSON copied to clipboard!"));
            }
        }).bounds(10, this.height - 25, footerBtnWidth, 18).build();
        this.addWidgetToMain(copyJsonBtn);

        saveBtn = Button.builder(Component.literal("Save"), b -> {
            saveToDatapack();
        }).bounds(10 + footerBtnWidth + 5, this.height - 25, footerBtnWidth, 18).build();
        this.addWidgetToMain(saveBtn);

        showBtn = Button.builder(Component.literal("Save & Show"), b -> {
            if (saveToDatapack()) {
                if (this.minecraft.player != null) {
                    this.minecraft.player.connection.sendCommand("dialog show @s " + this.namespace + ":" + this.filename);
                    this.onClose();
                }
            }
        }).bounds(10 + (footerBtnWidth + 5) * 2, this.height - 25, footerBtnWidth, 18).build();
        this.addWidgetToMain(showBtn);


        // --- 2. SUB-EDITOR STATE WIDGETS ---
        int colWidth = (splitX - 30) / 2;
        int col2X = splitX / 2 + 5;

        editBox1 = new EditBox(this.font, 10, 40, colWidth, 16, Component.literal(""));
        this.addWidgetToSub(editBox1);
        editBox2 = new EditBox(this.font, col2X, 40, colWidth, 16, Component.literal(""));
        this.addWidgetToSub(editBox2);
        editBox3 = new EditBox(this.font, 10, 70, colWidth, 16, Component.literal(""));
        this.addWidgetToSub(editBox3);
        editBox4 = new EditBox(this.font, col2X, 70, colWidth, 16, Component.literal(""));
        this.addWidgetToSub(editBox4);
        editBox5 = new EditBox(this.font, 10, 100, colWidth, 16, Component.literal(""));
        this.addWidgetToSub(editBox5);
        editBox6 = new EditBox(this.font, col2X, 100, colWidth, 16, Component.literal(""));
        this.addWidgetToSub(editBox6);
        editBox7 = new EditBox(this.font, 10, 130, colWidth, 16, Component.literal(""));
        this.addWidgetToSub(editBox7);
        editBox8 = new EditBox(this.font, col2X, 130, colWidth, 16, Component.literal(""));
        this.addWidgetToSub(editBox8);

        subDropdownButton1 = Button.builder(Component.literal("Select Type"), b -> {
            cycleSubDropdownType();
        }).bounds(10, 20, splitX - 20, 16).build();
        this.addWidgetToSub(subDropdownButton1);

        subCheckboxButton1 = Button.builder(Component.literal("Toggle 1"), b -> {
            toggleSubCheckbox(1, b);
        }).bounds(10, 160, (splitX - 25) / 2, 16).build();
        this.addWidgetToSub(subCheckboxButton1);

        subCheckboxButton2 = Button.builder(Component.literal("Toggle 2"), b -> {
            toggleSubCheckbox(2, b);
        }).bounds(col2X, 160, (splitX - 25) / 2, 16).build();
        this.addWidgetToSub(subCheckboxButton2);

        Button backBtn = Button.builder(Component.literal("Back to Main List"), b -> {
            applySubEditorValues();
            if (state == ScreenState.EDIT_SINGLE_ACTION) {
                state = ScreenState.EDIT_ACTIONS;
            } else {
                state = ScreenState.MAIN;
            }
            init();
        }).bounds(10, this.height - 25, splitX - 20, 18).build();
        this.addWidgetToSub(backBtn);

        // LIST_EXISTING state widgets
        if (state == ScreenState.LIST_EXISTING) {
            existingDialogsList.clear();
            existingDialogsList.addAll(scanExistingDialogs(this.minecraft));
            
            int listStartY = 30;
            for (int i = 0; i < existingDialogsList.size(); i++) {
                String dialogId = existingDialogsList.get(i);
                if (listStartY + i * 20 > this.height - 50) break;
                Button loadBtn = Button.builder(Component.literal(dialogId), b -> {
                    loadDialogById(dialogId);
                }).bounds(10, listStartY + i * 20, splitX - 20, 16).build();
                this.addWidgetToList(loadBtn);
            }
            
            Button listBackBtn = Button.builder(Component.literal("Back to Main"), b -> {
                state = ScreenState.MAIN;
                init();
            }).bounds(10, this.height - 25, splitX - 20, 18).build();
            this.addWidgetToList(listBackBtn);
        }

        // EDIT_ACTIONS state widgets
        if (state == ScreenState.EDIT_ACTIONS) {
            int currentY = 20;
            if (model.type.equals("minecraft:notice")) {
                addButtonWithAction(splitX, currentY, "mapmakerutils.ui.editor.action.edit_notice_ok", model.noticeAction);
                currentY += 22;
            } else if (model.type.equals("minecraft:confirmation")) {
                addButtonWithAction(splitX, currentY, "mapmakerutils.ui.editor.action.edit_confirm_yes", model.confirmYes);
                currentY += 22;
                addButtonWithAction(splitX, currentY, "mapmakerutils.ui.editor.action.edit_confirm_no", model.confirmNo);
                currentY += 22;
            } else {
                addButtonWithAction(splitX, currentY, "mapmakerutils.ui.editor.action.edit_exit", model.exitAction);
                currentY += 22;
                
                actionColumnsField = new EditBox(this.font, 10, currentY, splitX - 20, 14, Component.translatable("mapmakerutils.ui.editor.field.columns"));
                actionColumnsField.setHint(Component.translatable("mapmakerutils.ui.editor.field.columns_hint"));
                actionColumnsField.setValue(String.valueOf(model.columns));
                actionColumnsField.setResponder(val -> {
                    try {
                        int parsed = Integer.parseInt(val.trim());
                        if (parsed >= 1) {
                            model.columns = parsed;
                        }
                    } catch (NumberFormatException ignored) {}
                });
                this.addWidgetToActions(actionColumnsField);
                currentY += 20;
                
                if (model.type.equals("minecraft:server_links") || model.type.equals("minecraft:dialog_list")) {
                    actionButtonWidthField = new EditBox(this.font, 10, currentY, splitX - 20, 14, Component.translatable("mapmakerutils.ui.editor.field.button_width"));
                    actionButtonWidthField.setHint(Component.translatable("mapmakerutils.ui.editor.field.button_width_hint"));
                    actionButtonWidthField.setValue(String.valueOf(model.buttonWidth));
                    actionButtonWidthField.setResponder(val -> {
                        try {
                            int parsed = Integer.parseInt(val.trim());
                            if (parsed >= 1) {
                                model.buttonWidth = parsed;
                            }
                        } catch (NumberFormatException ignored) {}
                    });
                    this.addWidgetToActions(actionButtonWidthField);
                    currentY += 20;
                }
                
                currentY += 10;
                
                if (model.type.equals("minecraft:multi_action")) {
                    Button addActBtn = Button.builder(Component.literal("+ Add Action"), b -> {
                        ClickAction newAct = new ClickAction("", "close");
                        model.actions.add(newAct);
                        init();
                    }).bounds(10, currentY, splitX - 20, 14).build();
                    this.addWidgetToActions(addActBtn);
                    currentY += 20;
                    
                    for (int i = 0; i < model.actions.size(); i++) {
                        final int idx = i;
                        ClickAction act = model.actions.get(i);
                        
                        String actLabel = act.label.isEmpty() ? "(No Label)" : act.label;
                        String cleanType = act.action.type;
                        if (cleanType != null && cleanType.contains(":")) {
                            cleanType = cleanType.substring(cleanType.indexOf(":") + 1);
                        }
                        String btnText = "#" + (i + 1) + ": " + actLabel + " (" + cleanType + ")";
                        
                        Button editBtn = Button.builder(Component.literal(btnText), b -> {
                            selectedClickAction = act;
                            state = ScreenState.EDIT_SINGLE_ACTION;
                            init();
                        }).bounds(10, currentY - 2, splitX - 70, 14).build();
                        this.addWidgetToActions(editBtn);
                        
                        Button delBtn = Button.builder(Component.literal("Del"), b -> {
                            model.actions.remove(idx);
                            init();
                        }).bounds(splitX - 55, currentY - 2, 45, 14).build();
                        this.addWidgetToActions(delBtn);
                        
                        currentY += 18;
                    }
                } else if (model.type.equals("minecraft:dialog_list")) {
                    EditBox newDialogBox = new EditBox(this.font, 10, currentY, splitX - 70, 14, Component.translatable("mapmakerutils.ui.editor.field.new_dialog_id_hint"));
                    newDialogBox.setHint(Component.literal("namespace:id"));
                    this.addWidgetToActions(newDialogBox);
                    
                    Button addDialogBtn = Button.builder(Component.literal("Add"), b -> {
                        String newId = newDialogBox.getValue().trim();
                        if (DialogEditorValidator.isValidIdentifier(newId) || DialogEditorValidator.isJsonObject(newId)) {
                            model.dialogs.add(newId);
                            init();
                        } else {
                            error("Enter a valid dialog ID or inline dialog object.");
                        }
                    }).bounds(splitX - 55, currentY, 45, 14).build();
                    this.addWidgetToActions(addDialogBtn);
                    currentY += 20;
                    
                    for (int i = 0; i < model.dialogs.size(); i++) {
                        final int idx = i;
                        String dl = model.dialogs.get(i);
                        
                        Button dlBtn = Button.builder(Component.literal("#" + (i + 1) + ": " + dl), b -> {})
                            .bounds(10, currentY - 2, splitX - 70, 14).build();
                        dlBtn.active = false;
                        this.addWidgetToActions(dlBtn);
                        
                        Button delBtn = Button.builder(Component.literal("Del"), b -> {
                            model.dialogs.remove(idx);
                            init();
                        }).bounds(splitX - 55, currentY - 2, 45, 14).build();
                        this.addWidgetToActions(delBtn);
                        
                        currentY += 18;
                    }
                }
            }
            
            Button actionsBackBtn = Button.builder(Component.literal("Back to Main"), b -> {
                applyActionEditorValues();
                state = ScreenState.MAIN;
                init();
            }).bounds(10, this.height - 25, splitX - 20, 18).build();
            this.addWidgetToActions(actionsBackBtn);
        }

        // Initialize state visibility
        setWidgetVisibility();
        setupSubEditorFields();

        // 스크롤 한계 보정 및 위젯 배치 갱신
        int maxScroll = getMaxScroll();
        if (this.scrollAmount > maxScroll) {
            this.scrollAmount = maxScroll;
        }
        updateWidgetPositions();
    }

    private void addWidgetToActions(net.minecraft.client.gui.components.AbstractWidget widget) {
        this.addRenderableWidget(widget);
        this.actionsWidgets.add(widget);
    }

    private void addButtonWithAction(int splitX, int y, String labelKey, ClickAction action) {
        String actType = action.action.type;
        String cleanType = actType;
        if (cleanType != null && cleanType.contains(":")) {
            cleanType = cleanType.substring(cleanType.indexOf(":") + 1);
        }
        
        Component btnLabel = Component.translatable(labelKey).append(" (" + cleanType + ")");
        
        Button btn = Button.builder(btnLabel, b -> {
            this.selectedClickAction = action;
            this.state = ScreenState.EDIT_SINGLE_ACTION;
            this.init();
        }).bounds(10, y, splitX - 20, 16).build();
        this.addWidgetToActions(btn);
    }

    private void addWidgetToMain(net.minecraft.client.gui.components.AbstractWidget widget) {
        this.addRenderableWidget(widget);
        this.mainWidgets.add(widget);
    }

    private void addWidgetToSub(net.minecraft.client.gui.components.AbstractWidget widget) {
        this.addRenderableWidget(widget);
        this.subWidgets.add(widget);
    }

    private void addWidgetToList(net.minecraft.client.gui.components.AbstractWidget widget) {
        this.addRenderableWidget(widget);
        this.listWidgets.add(widget);
    }

    private void setWidgetVisibility() {
        boolean main = (state == ScreenState.MAIN);
        boolean list = (state == ScreenState.LIST_EXISTING);
        boolean actions = (state == ScreenState.EDIT_ACTIONS);
        boolean sub = (state == ScreenState.EDIT_BODY || state == ScreenState.EDIT_INPUT || state == ScreenState.EDIT_SINGLE_ACTION);
        
        if (datapackField != null) {
            datapackField.visible = main;
            datapackField.active = main;
        }
        
        for (var w : mainWidgets) {
            w.visible = main;
            w.active = main;
        }
        for (var w : subWidgets) {
            w.visible = sub;
            w.active = sub;
        }
        for (var w : listWidgets) {
            w.visible = list;
            w.active = list;
        }
        for (var w : actionsWidgets) {
            w.visible = actions;
            w.active = actions;
        }
    }

    private void cycleDialogType() {
        switch (model.type) {
            case "minecraft:notice":
                model.type = "minecraft:confirmation";
                break;
            case "minecraft:confirmation":
                model.type = "minecraft:multi_action";
                break;
            case "minecraft:multi_action":
                model.type = "minecraft:server_links";
                break;
            case "minecraft:server_links":
                model.type = "minecraft:dialog_list";
                break;
            case "minecraft:dialog_list":
                model.type = "minecraft:notice";
                break;
        }
    }

    private void cycleAfterAction() {
        switch (model.afterAction) {
            case "close":
                model.afterAction = "none";
                if (model.pause) {
                    model.pause = false;
                    warn("After action 'none' requires pause=false; pause was disabled.");
                }
                break;
            case "none":
                model.afterAction = "wait_for_response";
                break;
            case "wait_for_response":
                model.afterAction = "close";
                break;
        }
    }

    private void cycleSubDropdownType() {
        if (state == ScreenState.EDIT_BODY && selectedElementIndex >= 0) {
            BodyElement elem = model.body.get(selectedElementIndex);
            elem.type = elem.type.equals("minecraft:plain_message") ? "minecraft:item" : "minecraft:plain_message";
            subDropdownButton1.setMessage(Component.literal("Type: " + elem.type.replace("minecraft:", "")));
            setupSubEditorFields();
        } else if (state == ScreenState.EDIT_INPUT && selectedElementIndex >= 0) {
            InputControl input = model.inputs.get(selectedElementIndex);
            switch (input.type) {
                case "minecraft:text":
                    input.type = "minecraft:boolean";
                    break;
                case "minecraft:boolean":
                    input.type = "minecraft:single_option";
                    break;
                case "minecraft:single_option":
                    input.type = "minecraft:number_range";
                    break;
                case "minecraft:number_range":
                    input.type = "minecraft:text";
                    break;
            }
            subDropdownButton1.setMessage(Component.literal("Type: " + input.type.replace("minecraft:", "")));
            setupSubEditorFields();
        } else if (state == ScreenState.EDIT_SINGLE_ACTION && selectedClickAction != null) {
            String currentType = selectedClickAction.action.type;
            String cleanType = currentType;
            if (cleanType != null && cleanType.contains(":")) {
                cleanType = cleanType.substring(cleanType.indexOf(":") + 1);
            }
            
            String nextType = "close";
            switch (cleanType != null ? cleanType : "") {
                case "close", "none", "wait_for_response": nextType = "open_url"; break;
                case "open_url": nextType = "run_command"; break;
                case "run_command": nextType = "suggest_command"; break;
                case "suggest_command": nextType = "change_page"; break;
                case "change_page": nextType = "copy_to_clipboard"; break;
                case "copy_to_clipboard": nextType = "show_dialog"; break;
                case "show_dialog": nextType = "custom"; break;
                case "custom": nextType = "dynamic/run_command"; break;
                case "dynamic/run_command": nextType = "dynamic/custom"; break;
                case "dynamic/custom": nextType = "close"; break;
            }
            
            if (nextType.equals("open_url") || nextType.equals("run_command") || nextType.equals("suggest_command") ||
                nextType.equals("change_page") || nextType.equals("copy_to_clipboard") || nextType.equals("show_dialog") ||
                nextType.equals("custom") || nextType.equals("close")) {
                selectedClickAction.action.type = "minecraft:" + nextType;
            } else {
                selectedClickAction.action.type = nextType;
            }
            
            subDropdownButton1.setMessage(Component.literal("Type: " + selectedClickAction.action.type));
            setupSubEditorFields();
        }
    }

    private void toggleSubCheckbox(int id, Button btn) {
        if (state == ScreenState.EDIT_BODY && selectedElementIndex >= 0) {
            BodyElement elem = model.body.get(selectedElementIndex);
            if (id == 1) {
                elem.showDecoration = !elem.showDecoration;
                btn.setMessage(Component.literal("Show Decoration: " + elem.showDecoration));
            } else {
                elem.showTooltip = !elem.showTooltip;
                btn.setMessage(Component.literal("Show Tooltip: " + elem.showTooltip));
            }
        } else if (state == ScreenState.EDIT_INPUT && selectedElementIndex >= 0) {
            InputControl input = model.inputs.get(selectedElementIndex);
            if (id == 1) {
                if (input.type.equals("minecraft:text") || input.type.equals("minecraft:single_option")) {
                    input.labelVisible = !input.labelVisible;
                    btn.setMessage(Component.literal("Label Visible: " + input.labelVisible));
                } else if (input.type.equals("minecraft:boolean")) {
                    input.initialBoolean = !input.initialBoolean;
                    btn.setMessage(Component.literal("Initial Checked: " + input.initialBoolean));
                }
            } else {
                if (input.type.equals("minecraft:text")) {
                    input.multiline = !input.multiline;
                    btn.setMessage(Component.literal("Multiline: " + input.multiline));
                    setupSubEditorFields();
                }
            }
        }
    }

    // --- Sub Editor Fields Configuration ---

    private void setupSubEditorFields() {
        if (state == ScreenState.MAIN) return;

        // Reset text boxes visibility
        editBox1.visible = editBox2.visible = editBox3.visible = editBox4.visible = false;
        editBox5.visible = editBox6.visible = editBox7.visible = editBox8.visible = false;
        subDropdownButton1.visible = false;
        subCheckboxButton1.visible = false;
        subCheckboxButton2.visible = false;

        // Reset commandSuggestions
        this.commandSuggestions = null;

        if (state == ScreenState.EDIT_BODY && selectedElementIndex >= 0) {
            BodyElement elem = model.body.get(selectedElementIndex);
            subDropdownButton1.visible = true;
            subDropdownButton1.setMessage(Component.literal("Type: " + elem.type.replace("minecraft:", "")));

            if (elem.type.equals("minecraft:plain_message")) {
                setupField(editBox1, "mapmakerutils.ui.editor.field.contents", elem.contents);
                setupField(editBox2, "mapmakerutils.ui.editor.field.width", String.valueOf(elem.width));
            } else if (elem.type.equals("minecraft:item")) {
                setupField(editBox1, "mapmakerutils.ui.editor.field.item_id", elem.itemId);
                setupField(editBox2, "mapmakerutils.ui.editor.field.item_count", String.valueOf(elem.itemCount));
                setupField(editBox3, "mapmakerutils.ui.editor.field.item_components", elem.itemComponents);
                setupField(editBox4, "mapmakerutils.ui.editor.field.description", elem.description);
                setupField(editBox5, "mapmakerutils.ui.editor.field.width", String.valueOf(elem.itemWidth));
                setupField(editBox6, "mapmakerutils.ui.editor.field.height", String.valueOf(elem.itemHeight));
                
                subCheckboxButton1.visible = true;
                subCheckboxButton1.setMessage(Component.literal("Show Decoration: " + elem.showDecoration));
                subCheckboxButton2.visible = true;
                subCheckboxButton2.setMessage(Component.literal("Show Tooltip: " + elem.showTooltip));
            }
        } else if (state == ScreenState.EDIT_INPUT && selectedElementIndex >= 0) {
            InputControl input = model.inputs.get(selectedElementIndex);
            subDropdownButton1.visible = true;
            subDropdownButton1.setMessage(Component.literal("Type: " + input.type.replace("minecraft:", "")));

            setupField(editBox1, "mapmakerutils.ui.editor.field.key", input.key);
            setupField(editBox2, "mapmakerutils.ui.editor.field.label", input.label);
            setupField(editBox3, "mapmakerutils.ui.editor.field.width", String.valueOf(input.width));

            if (input.type.equals("minecraft:text")) {
                setupField(editBox4, "mapmakerutils.ui.editor.field.initial_value", input.initialText);
                setupField(editBox5, "mapmakerutils.ui.editor.field.max_length", String.valueOf(input.maxLength));
                subCheckboxButton1.visible = true;
                subCheckboxButton1.setMessage(Component.literal("Label Visible: " + input.labelVisible));
                subCheckboxButton2.visible = true;
                subCheckboxButton2.setMessage(Component.literal("Multiline: " + input.multiline));

                if (input.multiline) {
                    setupField(editBox6, "mapmakerutils.ui.editor.field.max_lines", String.valueOf(input.maxLines));
                    setupField(editBox7, "mapmakerutils.ui.editor.field.multiline_height", String.valueOf(input.multilineHeight));
                }
            } else if (input.type.equals("minecraft:boolean")) {
                subCheckboxButton1.visible = true;
                subCheckboxButton1.setMessage(Component.literal("Initial Checked: " + input.initialBoolean));
                setupField(editBox4, "mapmakerutils.ui.editor.field.value_on_true", input.onTrue);
                setupField(editBox5, "mapmakerutils.ui.editor.field.value_on_false", input.onFalse);
            } else if (input.type.equals("minecraft:single_option")) {
                subCheckboxButton1.visible = true;
                subCheckboxButton1.setMessage(Component.literal("Label Visible: " + input.labelVisible));
                StringBuilder sb = new StringBuilder();
                for (Option opt : input.options) {
                    sb.append(opt.id).append(":").append(opt.display).append(opt.initial ? ":init" : "").append(",");
                }
                if (sb.length() > 0) sb.setLength(sb.length() - 1);
                setupField(editBox4, "mapmakerutils.ui.editor.field.options", sb.toString());
            } else if (input.type.equals("minecraft:number_range")) {
                setupField(editBox4, "mapmakerutils.ui.editor.field.label_format", input.labelFormat);
                setupField(editBox5, "mapmakerutils.ui.editor.field.start", String.valueOf(input.start));
                setupField(editBox6, "mapmakerutils.ui.editor.field.end", String.valueOf(input.end));
                setupField(editBox7, "mapmakerutils.ui.editor.field.step", input.step == null ? "" : String.valueOf(input.step));
                setupField(editBox8, "mapmakerutils.ui.editor.field.initial_value", input.initialFloat == null ? "" : String.valueOf(input.initialFloat));
            }

        } else if (state == ScreenState.EDIT_SINGLE_ACTION && selectedClickAction != null) {
            subDropdownButton1.visible = true;
            subDropdownButton1.setMessage(Component.literal("Type: " + selectedClickAction.action.type));

            setupField(editBox1, "mapmakerutils.ui.editor.field.btn_label", selectedClickAction.label);
            setupField(editBox2, "mapmakerutils.ui.editor.field.btn_tooltip", selectedClickAction.tooltip);
            setupField(editBox3, "mapmakerutils.ui.editor.field.btn_width", String.valueOf(selectedClickAction.width));

            String actType = selectedClickAction.action.type;
            String cleanType = actType;
            if (cleanType != null && cleanType.contains(":")) {
                cleanType = cleanType.substring(cleanType.indexOf(":") + 1);
            }
            
            if ("open_url".equals(cleanType)) {
                setupField(editBox4, "mapmakerutils.ui.editor.field.url", selectedClickAction.action.url);
            } else if ("run_command".equals(cleanType) || "suggest_command".equals(cleanType)) {
                setupField(editBox4, "mapmakerutils.ui.editor.field.command", selectedClickAction.action.command);
                // Command suggestions popup is disabled as requested by the user
                this.commandSuggestions = null;
            } else if ("change_page".equals(cleanType)) {
                setupField(editBox4, "mapmakerutils.ui.editor.field.page_number", String.valueOf(selectedClickAction.action.page));
            } else if ("copy_to_clipboard".equals(cleanType)) {
                setupField(editBox4, "mapmakerutils.ui.editor.field.copy_value", selectedClickAction.action.copyValue);
            } else if ("show_dialog".equals(cleanType)) {
                setupField(editBox4, "mapmakerutils.ui.editor.field.show_dialog_id", selectedClickAction.action.showDialogId);
            } else if ("custom".equals(cleanType)) {
                setupField(editBox4, "mapmakerutils.ui.editor.field.custom_id", selectedClickAction.action.customId);
                setupField(editBox5, "mapmakerutils.ui.editor.field.custom_payload", selectedClickAction.action.customPayload);
            } else if ("dynamic/run_command".equals(cleanType)) {
                setupField(editBox4, "mapmakerutils.ui.editor.field.dynamic_template", selectedClickAction.action.dynamicTemplate);
            } else if ("dynamic/custom".equals(cleanType)) {
                setupField(editBox4, "mapmakerutils.ui.editor.field.custom_id", selectedClickAction.action.dynamicCustomId);
                setupField(editBox5, "mapmakerutils.ui.editor.field.dynamic_additions", selectedClickAction.action.dynamicAdditions);
            }
        }
    }

    private void setupField(EditBox box, String placeholderKey, String value) {
        box.visible = true;
        box.setHint(Component.translatable(placeholderKey));
        box.setValue(value);
    }

    private void applySubEditorValues() {
        if (state == ScreenState.MAIN) return;
        clearEditorMessage();

        if (state == ScreenState.EDIT_BODY && selectedElementIndex >= 0) {
            BodyElement elem = model.body.get(selectedElementIndex);
            if (elem.type.equals("minecraft:plain_message")) {
                elem.contents = editBox1.getValue();
                elem.width = readClampedInt(editBox2, 200, 1, 1024, "Message width");
            } else if (elem.type.equals("minecraft:item")) {
                elem.itemId = editBox1.getValue();
                elem.itemCount = readClampedInt(editBox2, 1, 1, 99, "Item count");
                elem.itemComponents = editBox3.getValue();
                elem.description = editBox4.getValue();
                elem.itemWidth = readClampedInt(editBox5, 16, 1, 256, "Item width");
                elem.itemHeight = readClampedInt(editBox6, 16, 1, 256, "Item height");
            }
        } else if (state == ScreenState.EDIT_INPUT && selectedElementIndex >= 0) {
            InputControl input = model.inputs.get(selectedElementIndex);
            input.key = editBox1.getValue();
            input.label = editBox2.getValue();
            input.width = readClampedInt(editBox3, 200, 1, 1024, "Input width");

            if (input.type.equals("minecraft:text")) {
                input.initialText = editBox4.getValue();
                input.maxLength = readClampedInt(editBox5, 32, 1, Integer.MAX_VALUE, "Maximum length");
                if (input.multiline) {
                    input.maxLines = readOptionalPositiveInt(editBox6, "Maximum lines");
                    input.multilineHeight = readOptionalClampedInt(editBox7, 1, 512, "Multiline height");
                }
            } else if (input.type.equals("minecraft:boolean")) {
                input.onTrue = editBox4.getValue();
                input.onFalse = editBox5.getValue();
            } else if (input.type.equals("minecraft:single_option")) {
                input.options.clear();
                String rawOpts = editBox4.getValue();
                if (!rawOpts.trim().isEmpty()) {
                    for (String part : rawOpts.split(",")) {
                        String[] sub = part.split(":");
                        String id = sub.length > 0 ? sub[0] : "";
                        String disp = sub.length > 1 ? sub[1] : id;
                        boolean init = sub.length > 2 && sub[2].equals("init");
                        input.options.add(new Option(id, disp, init));
                    }
                }
            } else if (input.type.equals("minecraft:number_range")) {
                input.labelFormat = editBox4.getValue();
                input.start = readFloat(editBox5, 0f, "Range start");
                input.end = readFloat(editBox6, 10f, "Range end");
                input.step = readOptionalPositiveFloat(editBox7, "Range step");
                input.initialFloat = readOptionalRangeFloat(editBox8, input.start, input.end, "Initial range value");
            }

        } else if (state == ScreenState.EDIT_SINGLE_ACTION && selectedClickAction != null) {
            selectedClickAction.label = editBox1.getValue();
            selectedClickAction.tooltip = editBox2.getValue();
            selectedClickAction.width = readClampedInt(editBox3, 150, 1, 1024, "Button width");

            String actType = selectedClickAction.action.type;
            String cleanType = actType;
            if (cleanType != null && cleanType.contains(":")) {
                cleanType = cleanType.substring(cleanType.indexOf(":") + 1);
            }
            
            if ("open_url".equals(cleanType)) {
                selectedClickAction.action.url = editBox4.getValue();
            } else if ("run_command".equals(cleanType) || "suggest_command".equals(cleanType)) {
                selectedClickAction.action.command = editBox4.getValue();
            } else if ("change_page".equals(cleanType)) {
                selectedClickAction.action.page = readClampedInt(editBox4, 1, 1, Integer.MAX_VALUE, "Page number");
            } else if ("copy_to_clipboard".equals(cleanType)) {
                selectedClickAction.action.copyValue = editBox4.getValue();
            } else if ("show_dialog".equals(cleanType)) {
                selectedClickAction.action.showDialogId = editBox4.getValue();
            } else if ("custom".equals(cleanType)) {
                selectedClickAction.action.customId = editBox4.getValue();
                selectedClickAction.action.customPayload = editBox5.getValue();
            } else if ("dynamic/run_command".equals(cleanType)) {
                selectedClickAction.action.dynamicTemplate = editBox4.getValue();
            } else if ("dynamic/custom".equals(cleanType)) {
                selectedClickAction.action.dynamicCustomId = editBox4.getValue();
                selectedClickAction.action.dynamicAdditions = editBox5.getValue();
            }
            
            boolean hasAction = cleanType != null && !cleanType.equals("close") && !cleanType.equals("none") && !cleanType.equals("wait_for_response");
            if (hasAction && (selectedClickAction.label.equals("gui.ok") || selectedClickAction.label.equals("gui.yes") || selectedClickAction.label.equals("gui.no") || selectedClickAction.label.isEmpty())) {
                selectedClickAction.label = "";
            }
        }
    }

    private void applyActionEditorValues() {
        clearEditorMessage();
        if (actionColumnsField != null) {
            model.columns = readClampedInt(actionColumnsField, 2, 1, Integer.MAX_VALUE, "Columns");
        }
        if (actionButtonWidthField != null) {
            model.buttonWidth = readClampedInt(actionButtonWidthField, 150, 1, 1024, "Button width");
        }
    }

    private int readClampedInt(EditBox field, int fallback, int min, int max, String name) {
        int entered;
        try {
            entered = Integer.parseInt(field.getValue());
        } catch (Exception e) {
            field.setValue(String.valueOf(fallback));
            warn(name + " was invalid and was reset to " + fallback + ".");
            return fallback;
        }
        int corrected = Math.max(min, Math.min(max, entered));
        if (corrected != entered) {
            field.setValue(String.valueOf(corrected));
            warn(name + " must be between " + min + " and " + max + "; changed to " + corrected + ".");
        }
        return corrected;
    }

    private int readOptionalPositiveInt(EditBox field, String name) {
        if (field.getValue().isBlank()) return 0;
        return readClampedInt(field, 1, 1, Integer.MAX_VALUE, name);
    }

    private int readOptionalClampedInt(EditBox field, int min, int max, String name) {
        if (field.getValue().isBlank()) return 0;
        return readClampedInt(field, min, min, max, name);
    }

    private float readFloat(EditBox field, float fallback, String name) {
        try {
            float value = Float.parseFloat(field.getValue());
            if (Float.isFinite(value)) return value;
        } catch (Exception ignored) {
        }
        field.setValue(String.valueOf(fallback));
        warn(name + " was invalid and was reset to " + fallback + ".");
        return fallback;
    }

    private Float readOptionalPositiveFloat(EditBox field, String name) {
        if (field.getValue().isBlank()) return null;
        float value = readFloat(field, 1f, name);
        if (value > 0) return value;
        field.setValue("1.0");
        warn(name + " must be greater than 0; changed to 1.0.");
        return 1f;
    }

    private Float readOptionalRangeFloat(EditBox field, float start, float end, String name) {
        if (field.getValue().isBlank()) return null;
        float value = readFloat(field, (start + end) / 2f, name);
        float min = Math.min(start, end);
        float max = Math.max(start, end);
        float corrected = Math.max(min, Math.min(max, value));
        if (corrected != value) {
            field.setValue(String.valueOf(corrected));
            warn(name + " must be inside the range; changed to " + corrected + ".");
        }
        return corrected;
    }

    private void warn(String message) {
        if (editorMessage.isEmpty() || !editorMessageIsError) {
            editorMessage = message;
            editorMessageIsError = false;
        }
    }

    private void error(String message) {
        editorMessage = message;
        editorMessageIsError = true;
    }

    private void clearEditorMessage() {
        editorMessage = "";
        editorMessageIsError = false;
    }

    private boolean saveToDatapack() {
        this.namespace = namespaceField.getValue().trim();
        this.filename = filenameField.getValue().trim();
        this.datapackName = datapackField.getValue().trim();
        if (this.datapackName.isEmpty()) {
            this.datapackName = "mapmakerutils_generated";
        }

        if (this.namespace.isEmpty() || this.filename.isEmpty()) {
            error("Namespace and filename cannot be empty.");
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.literal("§c[MapMakerUtils] Namespace and Filename cannot be empty!"));
            }
            return false;
        }

        // Security check: restrict namespace, filename, and datapackName to safe characters to prevent directory traversal
        if (!this.namespace.matches("^[a-z0-9_.-]+$") || !this.filename.matches("^[a-z0-9_.-]+$")) {
            error("Namespace and filename must use lowercase letters, numbers, underscores, hyphens, or periods.");
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.literal("§c[MapMakerUtils] Namespace and Filename must be lowercase alphanumeric, underscores, hyphens, or periods!"));
            }
            return false;
        }
        if (!this.datapackName.matches("^[a-zA-Z0-9_.-]+$")) {
            error("Datapack name contains invalid characters.");
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.literal("§c[MapMakerUtils] Datapack name must be alphanumeric, underscores, hyphens, or periods!"));
            }
            return false;
        }

        if (!validateForExport()) {
            return false;
        }

        IntegratedServer server = this.minecraft.getSingleplayerServer();
        if (server == null) {
            error("Saving dialogs is available in singleplayer worlds only.");
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.literal("§c[MapMakerUtils] Singleplayer only!"));
            }
            return false;
        }

        DialogDatapackManager.SaveResult result = DialogDatapackManager.saveToDatapack(
            server,
            this.namespace,
            this.filename,
            this.datapackName,
            model
        );
        if (!result.isSuccess()) {
            error(result.getErrorMessage());
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.literal("§c[MapMakerUtils] " + result.getErrorMessage()));
            }
            return false;
        }

        if (this.minecraft.player != null) {
            String dialogIdStr = this.namespace + ":" + this.filename;
            if (result.isHotSwapped()) {
                this.minecraft.player.sendSystemMessage(Component.literal("§a[MapMakerUtils] Dialog saved and hot-swapped successfully to " + dialogIdStr));
            } else {
                this.minecraft.player.sendSystemMessage(Component.literal("§e[MapMakerUtils] Dialog saved to " + dialogIdStr + ", but hot-swap failed: " + result.getHotSwapError()));
            }
        }

        return true;
    }

    private boolean validateForExport() {
        clearEditorMessage();
        DialogEditorValidator.ValidationResult result = DialogEditorValidator.validateForExport(
            model,
            this.minecraft.level != null ? this.minecraft.level.registryAccess() : null
        );
        if (!result.isValid()) {
            error(result.getMessage());
            return false;
        }
        return true;
    }

    // --- Render Logic (Left GUI list & Right WYSIWYG Live Preview) ---

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        int splitX = this.width / 2;
        
        guiGraphics.fill(splitX, 0, splitX + 1, this.height, 0xFF555555); // Vertical split line

        if (state == ScreenState.MAIN) {
            int scrollY = this.scrollAmount;

            // 1. 설명 라벨 렌더링
            drawScrollableText(guiGraphics, Component.translatable("mapmakerutils.ui.editor.label.namespace").getString(), 10, 10 - scrollY, 0xFFAAAAAA);
            drawScrollableText(guiGraphics, Component.translatable("mapmakerutils.ui.editor.label.filename").getString(), splitX / 2 + 5, 10 - scrollY, 0xFFAAAAAA);
            drawScrollableText(guiGraphics, Component.translatable("mapmakerutils.ui.editor.label.datapack_folder").getString(), 10, 40 - scrollY, 0xFFAAAAAA);
            drawScrollableText(guiGraphics, Component.translatable("mapmakerutils.ui.editor.label.title").getString(), 10, 70 - scrollY, 0xFFAAAAAA);
            drawScrollableText(guiGraphics, Component.translatable("mapmakerutils.ui.editor.label.external_title").getString(), 10, 100 - scrollY, 0xFFAAAAAA);

            // 2. 동적 리스트 및 텍스트 렌더링
            int startY = 245;
            drawScrollableText(guiGraphics, "Body Elements List:", 10, startY - 10 - scrollY, 0xFFAAAAAA);
            for (int i = 0; i < model.body.size(); i++) {
                BodyElement elem = model.body.get(i);
                String label = "#" + (i + 1) + ": " + elem.type.replace("minecraft:", "");
                drawScrollableText(guiGraphics, label, 15, startY + i * 20 - scrollY, 0xFFFFFFFF);
            }

            int inputStartY = startY + model.body.size() * 20 + 20;
            drawScrollableText(guiGraphics, "Input Controls List:", 10, inputStartY - 10 - scrollY, 0xFFAAAAAA);
            for (int i = 0; i < model.inputs.size(); i++) {
                InputControl input = model.inputs.get(i);
                String label = "#" + (i + 1) + ": " + input.type.replace("minecraft:", "") + " (" + input.key + ")";
                drawScrollableText(guiGraphics, label, 15, inputStartY + i * 20 - scrollY, 0xFFFFFFFF);
            }

            // 3. 스크롤바 비주얼 렌더링
            int topLimit = 5;
            int bottomLimit = this.height - 30;
            int visibleHeight = bottomLimit - topLimit;
            int maxScroll = getMaxScroll();

            if (maxScroll > 0) {
                int totalHeight = 245 + model.body.size() * 20 + 20 + model.inputs.size() * 20 + 10;
                int barHeight = Math.max(15, (visibleHeight * visibleHeight) / totalHeight);
                int barY = topLimit + (this.scrollAmount * (visibleHeight - barHeight)) / maxScroll;
                int barX = splitX - 6;

                // 스크롤 트랙
                guiGraphics.fill(barX, topLimit, barX + 4, bottomLimit, 0x30000000);
                // 스크롤바
                int barColor = this.isScrolling ? 0xFFCCCCCC : 0xFF888888;
                guiGraphics.fill(barX, barY, barX + 4, barY + barHeight, barColor);
            }
        } else {
            String headLabel = "";
            if (state == ScreenState.EDIT_BODY) headLabel = "Editing Body Element #" + (selectedElementIndex + 1);
            else if (state == ScreenState.EDIT_INPUT) headLabel = "Editing Input Control #" + (selectedElementIndex + 1);
            else if (state == ScreenState.EDIT_ACTIONS) headLabel = "Editing Footer Actions";
            else if (state == ScreenState.EDIT_SINGLE_ACTION) headLabel = "Editing Click Action";
            else if (state == ScreenState.LIST_EXISTING) headLabel = "Select Existing Dialog";

            guiGraphics.text(this.font, headLabel, 10, 5, 0xFFFFFF55);

            // EDIT_ACTIONS state description text is removed to prevent layout overlap
            
            // EDIT_SINGLE_ACTION 명령어 입력 상자 시인성을 위한 테두리 강조
            if (state == ScreenState.EDIT_SINGLE_ACTION && selectedClickAction != null) {
                String actType = selectedClickAction.action.type;
                String cleanType = actType;
                if (cleanType != null && cleanType.contains(":")) {
                    cleanType = cleanType.substring(cleanType.indexOf(":") + 1);
                }
                if ("run_command".equals(cleanType) || "suggest_command".equals(cleanType)) {
                    drawOutline(guiGraphics, editBox4.getX() - 1, editBox4.getY() - 1, editBox4.getWidth() + 2, editBox4.getHeight() + 2, 0xFF888888);
                }
            }
        }

        // --- RIGHT SIDE: WYSIWYG LIVE PREVIEW PANEL ---
        int previewWidth = 220;
        int previewX = splitX + (this.width - splitX - previewWidth) / 2;
        int previewY = this.height / 2 - 100;

        // Dynamic Height Calculation
        int dynamicHeight = 50; // title + margins
        for (BodyElement elem : model.body) {
            if (elem.type.equals("minecraft:plain_message")) {
                var wrapped = this.font.split(Component.literal(elem.contents), previewWidth - 20);
                dynamicHeight += wrapped.size() * 9 + 4;
            } else if (elem.type.equals("minecraft:item")) {
                dynamicHeight += 20;
            }
        }
        for (InputControl input : model.inputs) {
            dynamicHeight += 22;
        }
        dynamicHeight += 25; // button/footer area

        previewY = this.height / 2 - dynamicHeight / 2;

        // Draw Dialog Background Box
        guiGraphics.fill(previewX, previewY, previewX + previewWidth, previewY + dynamicHeight, 0xD0101010);
        drawOutline(guiGraphics, previewX, previewY, previewWidth, dynamicHeight, 0xFF555555);

        // Render Title
        int titleW = this.font.width(model.title);
        guiGraphics.text(this.font, model.title, previewX + (previewWidth - titleW) / 2, previewY + 10, 0xFFFFFFFF);

        // Render Body
        int currentY = previewY + 25;
        for (BodyElement elem : model.body) {
            if (elem.type.equals("minecraft:plain_message")) {
                var wrapped = this.font.split(Component.literal(elem.contents), previewWidth - 20);
                for (var line : wrapped) {
                    guiGraphics.text(this.font, line, previewX + 10, currentY, 0xFFAAAAAA);
                    currentY += 9;
                }
                currentY += 4;
            } else if (elem.type.equals("minecraft:item")) {
                ItemStack stack = getItemStack(elem.itemId, elem.itemCount);
                guiGraphics.item(stack, previewX + 10, currentY);
                guiGraphics.itemDecorations(this.font, stack, previewX + 10, currentY);
                guiGraphics.text(this.font, elem.description, previewX + 30, currentY + 4, 0xFFAAAAAA);
                currentY += 20;
            }
        }

        // Render Inputs Mockup
        for (InputControl input : model.inputs) {
            if (input.labelVisible) {
                guiGraphics.text(this.font, input.label, previewX + 10, currentY, 0xFF888888);
                currentY += 10;
            }
            if (input.type.equals("minecraft:text")) {
                guiGraphics.fill(previewX + 10, currentY, previewX + previewWidth - 10, currentY + 10, 0xFF000000);
                drawOutline(guiGraphics, previewX + 10, currentY, previewWidth - 20, 10, 0xFF444444);
                guiGraphics.text(this.font, input.initialText, previewX + 12, currentY + 1, 0xFF888888);
            } else if (input.type.equals("minecraft:boolean")) {
                guiGraphics.fill(previewX + 10, currentY, previewX + 18, currentY + 8, 0xFF000000);
                drawOutline(guiGraphics, previewX + 10, currentY, 8, 8, 0xFF444444);
                if (input.initialBoolean) {
                    guiGraphics.text(this.font, "x", previewX + 12, currentY, 0xFF55FF55);
                }
            } else if (input.type.equals("minecraft:single_option")) {
                guiGraphics.fill(previewX + 10, currentY, previewX + previewWidth - 10, currentY + 10, 0xFF333333);
                drawOutline(guiGraphics, previewX + 10, currentY, previewWidth - 20, 10, 0xFF555555);
                String display = "Options...";
                for (Option opt : input.options) {
                    if (opt.initial) { display = opt.display; break; }
                }
                int optW = this.font.width(display);
                guiGraphics.text(this.font, display, previewX + (previewWidth - optW) / 2, currentY + 1, 0xFFDDDDDD);
            } else if (input.type.equals("minecraft:number_range")) {
                guiGraphics.fill(previewX + 10, currentY + 4, previewX + previewWidth - 10, currentY + 5, 0xFF555555);
                guiGraphics.fill(previewX + previewWidth / 2 - 3, currentY, previewX + previewWidth / 2 + 3, currentY + 8, 0xFF888888);
            }
            currentY += 12;
        }

        // Render Footer Buttons
        int buttonY = previewY + dynamicHeight - 20;
        if (model.type.equals("minecraft:notice")) {
            drawMockButton(guiGraphics, model.noticeAction.label, previewX + 35, buttonY, 150);
        } else if (model.type.equals("minecraft:confirmation")) {
            drawMockButton(guiGraphics, model.confirmYes.label, previewX + 5, buttonY, 100);
            drawMockButton(guiGraphics, model.confirmNo.label, previewX + 115, buttonY, 100);
        } else if (model.type.equals("minecraft:multi_action")) {
            int btnW = 100;
            if (model.actions.size() > 0) drawMockButton(guiGraphics, model.actions.get(0).label, previewX + 5, buttonY, btnW);
            if (model.actions.size() > 1) drawMockButton(guiGraphics, model.actions.get(1).label, previewX + 115, buttonY, btnW);
        } else if (model.type.equals("minecraft:server_links") || model.type.equals("minecraft:dialog_list")) {
            drawMockButton(guiGraphics, "Server Links/List...", previewX + 35, buttonY, 150);
        }

        if (this.commandSuggestions != null) {
            this.fixCommandSuggestionsPosition();
            this.commandSuggestions.extractRenderState(guiGraphics, mouseX, mouseY);
        }

        if (!editorMessage.isEmpty()) {
            int color = editorMessageIsError ? 0xFFFF5555 : 0xFFFFAA00;
            Component message = Component.literal(editorMessage);
            int messageWidth = this.font.width(message);
            guiGraphics.fill(4, this.height - 43, Math.min(this.width - 4, messageWidth + 12), this.height - 29, 0xD0000000);
            guiGraphics.text(this.font, message, 8, this.height - 40, color);
        }
    }

    private void drawOutline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private void drawMockButton(GuiGraphicsExtractor guiGraphics, String text, int x, int y, int width) {
        guiGraphics.fill(x, y, x + width, y + 14, 0xFF444444);
        drawOutline(guiGraphics, x, y, width, 14, 0xFF666666);
        int tw = this.font.width(text);
        guiGraphics.text(this.font, text, x + (width - tw) / 2, y + 3, 0xFFFFFFFF);
    }

    private ItemStack getItemStack(String itemId, int count) {
        try {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
            if (item != null && item != Items.AIR) {
                return new ItemStack(item, count);
            }
        } catch (Exception e) {
            // ignore
        }
        return new ItemStack(Items.STONE, count);
    }

    public static List<String> scanExistingDialogs(net.minecraft.client.Minecraft mc) {
        return DialogDatapackManager.scanExistingDialogs(mc.getSingleplayerServer());
    }

    private void loadDialogById(String dialogId) {
        IntegratedServer server = this.minecraft.getSingleplayerServer();
        if (server == null) {
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.literal("§cSingleplayer only!"));
            }
            return;
        }

        DialogDatapackManager.LoadResult result = DialogDatapackManager.loadDialog(server, dialogId);
        if (!result.isSuccess()) {
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.literal("§cFailed to load: " + result.getErrorMessage()));
            }
            return;
        }

        DialogModel loadedModel = result.getModel();
        this.namespace = result.getNamespace();
        this.filename = result.getFilename();
        this.datapackName = result.getDatapackName();
        this.model.type = loadedModel.type;
        this.model.title = loadedModel.title;
        this.model.externalTitle = loadedModel.externalTitle;
        this.model.afterAction = loadedModel.afterAction;
        this.model.canCloseWithEscape = loadedModel.canCloseWithEscape;
        this.model.pause = loadedModel.pause;
        this.model.body = loadedModel.body;
        this.model.inputs = loadedModel.inputs;
        this.model.noticeAction = loadedModel.noticeAction;
        this.model.confirmYes = loadedModel.confirmYes;
        this.model.confirmNo = loadedModel.confirmNo;
        this.model.actions = loadedModel.actions;
        this.model.columns = loadedModel.columns;
        this.model.exitAction = loadedModel.exitAction;
        this.model.buttonWidth = loadedModel.buttonWidth;
        this.model.dialogs = loadedModel.dialogs;
        
        if (namespaceField != null) namespaceField.setValue(this.namespace);
        if (filenameField != null) filenameField.setValue(this.filename);
        if (datapackField != null) datapackField.setValue(this.datapackName);
        if (titleField != null) titleField.setValue(this.model.title);
        if (externalTitleField != null) externalTitleField.setValue(this.model.externalTitle);
        
        state = ScreenState.MAIN;
        init();
    }

    private int getMaxScroll() {
        int totalHeight = 245 + model.body.size() * 20 + 20 + model.inputs.size() * 20 + 10;
        int visibleHeight = this.height - 35;
        return Math.max(0, totalHeight - visibleHeight);
    }

    private void updateWidgetPositions() {
        int splitX = this.width / 2;
        int scrollY = this.scrollAmount;

        if (state == ScreenState.MAIN) {
            setWidgetY(namespaceField, 20 - scrollY);
            setWidgetY(filenameField, 20 - scrollY);
            setWidgetY(datapackField, 50 - scrollY);
            setWidgetY(titleField, 80 - scrollY);
            setWidgetY(externalTitleField, 110 - scrollY);

            setWidgetY(typeButton, 135 - scrollY);
            setWidgetY(escapeButton, 155 - scrollY);
            setWidgetY(pauseButton, 155 - scrollY);
            setWidgetY(afterActionButton, 175 - scrollY);

            setWidgetY(addBodyBtn, 200 - scrollY);
            setWidgetY(addInputBtn, 200 - scrollY);
            setWidgetY(editActionsBtn, 220 - scrollY);

            int startY = 245;
            for (int i = 0; i < model.body.size(); i++) {
                int btnY = startY + i * 20 - 4 - scrollY;
                if (i < bodyEditButtons.size()) setWidgetY(bodyEditButtons.get(i), btnY);
                if (i < bodyDeleteButtons.size()) setWidgetY(bodyDeleteButtons.get(i), btnY);
            }

            int inputStartY = startY + model.body.size() * 20 + 20;
            for (int i = 0; i < model.inputs.size(); i++) {
                int btnY = inputStartY + i * 20 - 4 - scrollY;
                if (i < inputEditButtons.size()) setWidgetY(inputEditButtons.get(i), btnY);
                if (i < inputDeleteButtons.size()) setWidgetY(inputDeleteButtons.get(i), btnY);
            }

            int topLimit = 5;
            int bottomLimit = this.height - 30;

            if (namespaceField != null) {
                namespaceField.visible = (namespaceField.getY() >= topLimit && namespaceField.getY() + namespaceField.getHeight() <= bottomLimit);
                namespaceField.active = namespaceField.visible;
            }
            if (filenameField != null) {
                filenameField.visible = (filenameField.getY() >= topLimit && filenameField.getY() + filenameField.getHeight() <= bottomLimit);
                filenameField.active = filenameField.visible;
            }
            if (datapackField != null) {
                datapackField.visible = (datapackField.getY() >= topLimit && datapackField.getY() + datapackField.getHeight() <= bottomLimit);
                datapackField.active = datapackField.visible;
            }
            if (titleField != null) {
                titleField.visible = (titleField.getY() >= topLimit && titleField.getY() + titleField.getHeight() <= bottomLimit);
                titleField.active = titleField.visible;
            }
            if (externalTitleField != null) {
                externalTitleField.visible = (externalTitleField.getY() >= topLimit && externalTitleField.getY() + externalTitleField.getHeight() <= bottomLimit);
                externalTitleField.active = externalTitleField.visible;
            }
            if (typeButton != null) {
                typeButton.visible = (typeButton.getY() >= topLimit && typeButton.getY() + typeButton.getHeight() <= bottomLimit);
                typeButton.active = typeButton.visible;
            }
            if (escapeButton != null) {
                escapeButton.visible = (escapeButton.getY() >= topLimit && escapeButton.getY() + escapeButton.getHeight() <= bottomLimit);
                escapeButton.active = escapeButton.visible;
            }
            if (pauseButton != null) {
                pauseButton.visible = (pauseButton.getY() >= topLimit && pauseButton.getY() + pauseButton.getHeight() <= bottomLimit);
                pauseButton.active = pauseButton.visible;
            }
            if (afterActionButton != null) {
                afterActionButton.visible = (afterActionButton.getY() >= topLimit && afterActionButton.getY() + afterActionButton.getHeight() <= bottomLimit);
                afterActionButton.active = afterActionButton.visible;
            }
            if (addBodyBtn != null) {
                addBodyBtn.visible = (addBodyBtn.getY() >= topLimit && addBodyBtn.getY() + addBodyBtn.getHeight() <= bottomLimit);
                addBodyBtn.active = addBodyBtn.visible;
            }
            if (addInputBtn != null) {
                addInputBtn.visible = (addInputBtn.getY() >= topLimit && addInputBtn.getY() + addInputBtn.getHeight() <= bottomLimit);
                addInputBtn.active = addInputBtn.visible;
            }
            if (editActionsBtn != null) {
                editActionsBtn.visible = (editActionsBtn.getY() >= topLimit && editActionsBtn.getY() + editActionsBtn.getHeight() <= bottomLimit);
                editActionsBtn.active = editActionsBtn.visible;
            }

            for (int i = 0; i < model.body.size(); i++) {
                if (i < bodyEditButtons.size()) {
                    Button edit = bodyEditButtons.get(i);
                    edit.visible = (edit.getY() >= topLimit && edit.getY() + edit.getHeight() <= bottomLimit);
                    edit.active = edit.visible;
                }
                if (i < bodyDeleteButtons.size()) {
                    Button del = bodyDeleteButtons.get(i);
                    del.visible = (del.getY() >= topLimit && del.getY() + del.getHeight() <= bottomLimit);
                    del.active = del.visible;
                }
            }

            for (int i = 0; i < model.inputs.size(); i++) {
                if (i < inputEditButtons.size()) {
                    Button edit = inputEditButtons.get(i);
                    edit.visible = (edit.getY() >= topLimit && edit.getY() + edit.getHeight() <= bottomLimit);
                    edit.active = edit.visible;
                }
                if (i < inputDeleteButtons.size()) {
                    Button del = inputDeleteButtons.get(i);
                    del.visible = (del.getY() >= topLimit && del.getY() + del.getHeight() <= bottomLimit);
                    del.active = del.visible;
                }
            }
        }
    }

    private void setWidgetY(net.minecraft.client.gui.components.AbstractWidget widget, int y) {
        if (widget != null) {
            widget.setY(y);
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (this.commandSuggestions != null) {
            this.fixCommandSuggestionsPosition();
            if (this.commandSuggestions.keyPressed(event)) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        if (this.commandSuggestions != null) {
            this.fixCommandSuggestionsPosition();
            if (this.commandSuggestions.mouseScrolled(amountY)) {
                return true;
            }
        }
        if (state == ScreenState.MAIN && mouseX < this.width / 2) {
            double speed = 15.0;
            this.scrollAmount = Math.max(0, this.scrollAmount - (int)(amountY * speed));
            int maxScroll = getMaxScroll();
            if (this.scrollAmount > maxScroll) {
                this.scrollAmount = maxScroll;
            }
            updateWidgetPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amountX, amountY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.commandSuggestions != null) {
            this.fixCommandSuggestionsPosition();
            if (this.commandSuggestions.mouseClicked(event)) {
                return true;
            }
        }
        if (state == ScreenState.MAIN) {
            int splitX = this.width / 2;
            int topLimit = 5;
            int bottomLimit = this.height - 30;
            int visibleHeight = bottomLimit - topLimit;
            int maxScroll = getMaxScroll();

            if (maxScroll > 0) {
                int totalHeight = 245 + model.body.size() * 20 + 20 + model.inputs.size() * 20 + 10;
                int barHeight = Math.max(15, (visibleHeight * visibleHeight) / totalHeight);
                int barY = topLimit + (this.scrollAmount * (visibleHeight - barHeight)) / maxScroll;
                int barX = splitX - 6;

                double mouseX = event.x();
                double mouseY = event.y();

                if (event.button() == 0 && mouseX >= barX && mouseX <= barX + 4 && mouseY >= barY && mouseY <= barY + barHeight) {
                    this.isScrolling = true;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            this.isScrolling = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (this.isScrolling && state == ScreenState.MAIN && event.button() == 0) {
            int topLimit = 5;
            int bottomLimit = this.height - 30;
            int visibleHeight = bottomLimit - topLimit;
            int maxScroll = getMaxScroll();
            int totalHeight = 245 + model.body.size() * 20 + 20 + model.inputs.size() * 20 + 10;
            int barHeight = Math.max(15, (visibleHeight * visibleHeight) / totalHeight);

            double mouseY = event.y();

            double scrollPercent = (mouseY - topLimit - (barHeight / 2.0)) / (double)(visibleHeight - barHeight);
            scrollPercent = Math.max(0.0, Math.min(1.0, scrollPercent));
            this.scrollAmount = (int)(scrollPercent * maxScroll);
            updateWidgetPositions();
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    private void drawScrollableText(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, String text, int x, int y, int color) {
        int topLimit = 5;
        int bottomLimit = this.height - 30;
        if (y >= topLimit && y <= bottomLimit - 8) {
            guiGraphics.text(this.font, text, x, y, color);
        }
    }


    private void fixCommandSuggestionsPosition() {
        if (this.commandSuggestions == null) return;
        try {
            java.lang.reflect.Field suggestionsField = net.minecraft.client.gui.components.CommandSuggestions.class.getDeclaredField("suggestions");
            suggestionsField.setAccessible(true);
            Object suggestionsListObj = suggestionsField.get(this.commandSuggestions);
            if (suggestionsListObj == null) return;

            java.lang.reflect.Field rectField = suggestionsListObj.getClass().getDeclaredField("rect");
            rectField.setAccessible(true);
            net.minecraft.client.renderer.Rect2i rect = (net.minecraft.client.renderer.Rect2i) rectField.get(suggestionsListObj);
            if (rect == null) return;

            java.lang.reflect.Field yField = null;
            try {
                yField = net.minecraft.client.renderer.Rect2i.class.getDeclaredField("y");
            } catch (NoSuchFieldException e) {
                int intCount = 0;
                for (java.lang.reflect.Field f : net.minecraft.client.renderer.Rect2i.class.getDeclaredFields()) {
                    if (f.getType() == int.class) {
                        intCount++;
                        if (intCount == 2) {
                            yField = f;
                            break;
                        }
                    }
                }
            }

            if (yField != null) {
                yField.setAccessible(true);
                int targetY = editBox4.getY() + editBox4.getHeight() + 1;
                int popupHeight = rect.getHeight();
                if (targetY + popupHeight > this.height) {
                    targetY = editBox4.getY() - popupHeight - 1;
                }
                yField.setInt(rect, targetY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
