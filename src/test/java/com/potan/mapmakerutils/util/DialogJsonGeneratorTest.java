package com.potan.mapmakerutils.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.potan.mapmakerutils.util.DialogJsonGenerator.*;
import java.util.List;

public class DialogJsonGeneratorTest {

    @Test
    public void testCommonFields() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:notice";
        model.title = "Main Title";
        model.externalTitle = "Pause Menu Title";
        model.canCloseWithEscape = false;
        model.pause = false;
        model.afterAction = "wait_for_response";

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals("minecraft:notice", loaded.type);
        assertEquals("Main Title", loaded.title);
        assertEquals("Pause Menu Title", loaded.externalTitle);
        assertFalse(loaded.canCloseWithEscape);
        assertFalse(loaded.pause);
        assertEquals("wait_for_response", loaded.afterAction);
    }

    @Test
    public void testNoticeDialog() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:notice";
        model.title = "Notice Title";
        
        ClickAction noticeAct = new ClickAction("gui.ok", "open_url");
        noticeAct.tooltip = "Click to open site";
        noticeAct.width = 180;
        noticeAct.action.url = "https://example.com";
        model.noticeAction = noticeAct;

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals("minecraft:notice", loaded.type);
        assertEquals("gui.ok", loaded.noticeAction.label);
        assertEquals("Click to open site", loaded.noticeAction.tooltip);
        assertEquals(180, loaded.noticeAction.width);
        assertEquals("open_url", loaded.noticeAction.action.type);
        assertEquals("https://example.com", loaded.noticeAction.action.url);
    }

    @Test
    public void testConfirmationDialog() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:confirmation";
        model.title = "Confirm Title";
        
        ClickAction yesAct = new ClickAction("gui.yes", "run_command");
        yesAct.action.command = "/say YesClicked";
        model.confirmYes = yesAct;

        ClickAction noAct = new ClickAction("gui.no", "suggest_command");
        noAct.action.command = "/say NoClicked";
        model.confirmNo = noAct;

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals("minecraft:confirmation", loaded.type);
        
        assertEquals("gui.yes", loaded.confirmYes.label);
        assertEquals("run_command", loaded.confirmYes.action.type);
        assertEquals("/say YesClicked", loaded.confirmYes.action.command);

        assertEquals("gui.no", loaded.confirmNo.label);
        assertEquals("suggest_command", loaded.confirmNo.action.type);
        assertEquals("/say NoClicked", loaded.confirmNo.action.command);
    }

    @Test
    public void testMultiActionDialog() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:multi_action";
        model.title = "Multi Action Title";
        model.columns = 3;

        ClickAction act1 = new ClickAction("Button 1", "close");
        ClickAction act2 = new ClickAction("Button 2", "change_page");
        act2.action.page = 4;

        model.actions.add(act1);
        model.actions.add(act2);

        ClickAction exit = new ClickAction("Exit Button", "copy_to_clipboard");
        exit.action.copyValue = "Copied text";
        model.exitAction = exit;

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals("minecraft:multi_action", loaded.type);
        assertEquals(3, loaded.columns);
        assertEquals(2, loaded.actions.size());

        assertEquals("Button 1", loaded.actions.get(0).label);
        assertEquals("close", loaded.actions.get(0).action.type);

        assertEquals("Button 2", loaded.actions.get(1).label);
        assertEquals("change_page", loaded.actions.get(1).action.type);
        assertEquals(4, loaded.actions.get(1).action.page);

        assertEquals("Exit Button", loaded.exitAction.label);
        assertEquals("copy_to_clipboard", loaded.exitAction.action.type);
        assertEquals("Copied text", loaded.exitAction.action.copyValue);
    }

    @Test
    public void testServerLinksDialog() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:server_links";
        model.title = "Server Links";
        model.columns = 1;
        model.buttonWidth = 200;

        ClickAction exit = new ClickAction("Back", "close");
        model.exitAction = exit;

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals("minecraft:server_links", loaded.type);
        assertEquals(1, loaded.columns);
        assertEquals(200, loaded.buttonWidth);
        assertEquals("Back", loaded.exitAction.label);
        assertEquals("close", loaded.exitAction.action.type);
    }

    @Test
    public void testDialogListDialog() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:dialog_list";
        model.title = "Dialog List";
        model.columns = 2;
        model.buttonWidth = 120;
        model.dialogs.add("my_datapack:dialog1");
        model.dialogs.add("{\"type\":\"minecraft:notice\",\"title\":\"Inline Dialog\"}");

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals("minecraft:dialog_list", loaded.type);
        assertEquals(2, loaded.columns);
        assertEquals(120, loaded.buttonWidth);
        assertEquals(2, loaded.dialogs.size());
        assertEquals("my_datapack:dialog1", loaded.dialogs.get(0));
        assertTrue(loaded.dialogs.get(1).contains("Inline Dialog"));
    }

    @Test
    public void testBodyPlainMessage() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:notice";
        model.title = "Plain Message Test";

        BodyElement body = new BodyElement();
        body.type = "minecraft:plain_message";
        body.contents = "Test Message Contents";
        body.width = 250;
        model.body.add(body);

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals(1, loaded.body.size());
        assertEquals("minecraft:plain_message", loaded.body.get(0).type);
        assertEquals("Test Message Contents", loaded.body.get(0).contents);
        assertEquals(250, loaded.body.get(0).width);
    }

    @Test
    public void testBodyItem() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:notice";
        model.title = "Item Test";

        BodyElement body = new BodyElement();
        body.type = "minecraft:item";
        body.itemId = "minecraft:apple";
        body.itemCount = 5;
        body.itemComponents = "{\"minecraft:custom_name\":\"\\\"Golden Apple\\\"\"}";
        body.description = "A golden apple.";
        body.showDecoration = false;
        body.showTooltip = false;
        body.itemWidth = 32;
        body.itemHeight = 32;
        model.body.add(body);

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals(1, loaded.body.size());
        BodyElement loadedItem = loaded.body.get(0);
        assertEquals("minecraft:item", loadedItem.type);
        assertEquals("minecraft:apple", loadedItem.itemId);
        assertEquals(5, loadedItem.itemCount);
        assertTrue(loadedItem.itemComponents.contains("minecraft:custom_name"));
        assertEquals("A golden apple.", loadedItem.description);
        assertFalse(loadedItem.showDecoration);
        assertFalse(loadedItem.showTooltip);
        assertEquals(32, loadedItem.itemWidth);
        assertEquals(32, loadedItem.itemHeight);
    }

    @Test
    public void testInputText() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:notice";
        model.title = "Text Input Test";

        InputControl input = new InputControl();
        input.type = "minecraft:text";
        input.key = "username";
        input.label = "Enter Username";
        input.width = 180;
        input.labelVisible = false;
        input.initialText = "Player";
        input.maxLength = 16;
        model.inputs.add(input);

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals(1, loaded.inputs.size());
        InputControl loadedInput = loaded.inputs.get(0);
        assertEquals("minecraft:text", loadedInput.type);
        assertEquals("username", loadedInput.key);
        assertEquals("Enter Username", loadedInput.label);
        assertEquals(180, loadedInput.width);
        assertFalse(loadedInput.labelVisible);
        assertEquals("Player", loadedInput.initialText);
        assertEquals(16, loadedInput.maxLength);
        assertFalse(loadedInput.multiline);
    }

    @Test
    public void testInputTextMultiline() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:notice";
        model.title = "Multiline Input Test";

        InputControl input = new InputControl();
        input.type = "minecraft:text";
        input.key = "bio";
        input.label = "Enter Bio";
        input.multiline = true;
        input.maxLines = 5;
        input.multilineHeight = 100;
        model.inputs.add(input);

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals(1, loaded.inputs.size());
        InputControl loadedInput = loaded.inputs.get(0);
        assertTrue(loadedInput.multiline);
        assertEquals(5, loadedInput.maxLines);
        assertEquals(100, loadedInput.multilineHeight);
    }

    @Test
    public void testInputBoolean() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:notice";
        model.title = "Boolean Input Test";

        InputControl input = new InputControl();
        input.type = "minecraft:boolean";
        input.key = "sound_enabled";
        input.label = "Enable Sound";
        input.initialBoolean = true;
        input.onTrue = "yes";
        input.onFalse = "no";
        model.inputs.add(input);

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals(1, loaded.inputs.size());
        InputControl loadedInput = loaded.inputs.get(0);
        assertEquals("minecraft:boolean", loadedInput.type);
        assertEquals("sound_enabled", loadedInput.key);
        assertEquals("Enable Sound", loadedInput.label);
        assertTrue(loadedInput.initialBoolean);
        assertEquals("yes", loadedInput.onTrue);
        assertEquals("no", loadedInput.onFalse);
    }

    @Test
    public void testInputSingleOption() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:notice";
        model.title = "Single Option Test";

        InputControl input = new InputControl();
        input.type = "minecraft:single_option";
        input.key = "difficulty";
        input.label = "Difficulty";
        input.width = 150;
        input.labelVisible = true;
        input.options.add(new Option("easy", "Easy", false));
        input.options.add(new Option("normal", "Normal", true));
        input.options.add(new Option("hard", "Hard", false));
        model.inputs.add(input);

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals(1, loaded.inputs.size());
        InputControl loadedInput = loaded.inputs.get(0);
        assertEquals("minecraft:single_option", loadedInput.type);
        assertEquals("difficulty", loadedInput.key);
        assertEquals(3, loadedInput.options.size());
        
        assertEquals("easy", loadedInput.options.get(0).id);
        assertEquals("Easy", loadedInput.options.get(0).display);
        assertFalse(loadedInput.options.get(0).initial);

        assertEquals("normal", loadedInput.options.get(1).id);
        assertEquals("Normal", loadedInput.options.get(1).display);
        assertTrue(loadedInput.options.get(1).initial);
    }

    @Test
    public void testInputNumberRange() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:notice";
        model.title = "Number Range Test";

        InputControl input = new InputControl();
        input.type = "minecraft:number_range";
        input.key = "volume";
        input.label = "Volume";
        input.labelFormat = "options.volume_value";
        input.width = 200;
        input.start = 0.0f;
        input.end = 100.0f;
        input.step = 5.0f;
        input.initialFloat = 50.0f;
        model.inputs.add(input);

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals(1, loaded.inputs.size());
        InputControl loadedInput = loaded.inputs.get(0);
        assertEquals("minecraft:number_range", loadedInput.type);
        assertEquals("volume", loadedInput.key);
        assertEquals("options.volume_value", loadedInput.labelFormat);
        assertEquals(0.0f, loadedInput.start);
        assertEquals(100.0f, loadedInput.end);
        assertEquals(5.0f, loadedInput.step);
        assertEquals(50.0f, loadedInput.initialFloat);
    }

    @Test
    public void testStaticActionShowDialog() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:notice";
        model.title = "Show Dialog Test";

        ClickAction act1 = new ClickAction("Show Dialog 1", "show_dialog");
        act1.action.showDialogId = "my_datapack:sub_dialog";
        model.noticeAction = act1;

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals("show_dialog", loaded.noticeAction.action.type);
        assertEquals("my_datapack:sub_dialog", loaded.noticeAction.action.showDialogId);
    }

    @Test
    public void testStaticActionCustom() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:notice";
        model.title = "Custom Action Test";

        ClickAction act1 = new ClickAction("Custom Click", "custom");
        act1.action.customId = "my_custom_id";
        act1.action.customPayload = "{\"data\":\"value\"}";
        model.noticeAction = act1;

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals("custom", loaded.noticeAction.action.type);
        assertEquals("my_custom_id", loaded.noticeAction.action.customId);
        assertTrue(loaded.noticeAction.action.customPayload.contains("value"));
    }

    @Test
    public void testDynamicActionRunCommand() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:notice";
        model.title = "Dynamic Run Command Test";

        ClickAction act1 = new ClickAction("Dynamic Run", "dynamic/run_command");
        act1.action.dynamicTemplate = "say Hello ${username}";
        model.noticeAction = act1;

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals("dynamic/run_command", loaded.noticeAction.action.type);
        assertEquals("say Hello ${username}", loaded.noticeAction.action.dynamicTemplate);
    }

    @Test
    public void testDynamicActionCustom() {
        DialogModel model = new DialogModel();
        model.type = "minecraft:notice";
        model.title = "Dynamic Custom Test";

        ClickAction act1 = new ClickAction("Dynamic Custom", "dynamic/custom");
        act1.action.dynamicCustomId = "my_dynamic_custom";
        act1.action.dynamicAdditions = "{\"add\":\"data\"}";
        model.noticeAction = act1;

        String json = DialogJsonGenerator.serialize(model, false);
        DialogModel loaded = DialogJsonGenerator.deserialize(json);

        assertEquals("dynamic/custom", loaded.noticeAction.action.type);
        assertEquals("my_dynamic_custom", loaded.noticeAction.action.dynamicCustomId);
        assertTrue(loaded.noticeAction.action.dynamicAdditions.contains("data"));
    }
}
