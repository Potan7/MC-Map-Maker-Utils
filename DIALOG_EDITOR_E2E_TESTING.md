# Dialog Editor E2E Testing Guide

This document is written for AI coding agents that need to test, diagnose, and
repair the Minecraft Dialog Editor by repeatedly running its in-game UI
automation.

## Scope

- Target Minecraft version: `26.1.2`
- Workspace: repository root
- E2E command: `/testdialogeditor`
- Main implementation:
  - `src/client/java/com/potan/mapmakerutils/screen/DialogEditorScreen.java`
  - `src/client/java/com/potan/mapmakerutils/MapMakerUtilsClient.java`
- Runtime log: `run/logs/latest.log`

The E2E scenario sends `MouseButtonEvent` instances through the screen's real
`mouseClicked` path. It does not directly call button callbacks. Text field
values are assigned after the automation clicks and focuses the actual
`EditBox`.

This tests editor screen navigation and widget behavior, but it does not prove
that native OS mouse coordinates or keyboard input work.

## Current Known State

At the time this document was written:

- `./gradlew.bat build` succeeds.
- Unit tests and Minecraft `Dialog.DIRECT_CODEC` tests succeed.
- `/testdialogeditor` currently fails at:

```text
FAIL: Input width was not clamped through the editor; actual=200
```

Expected value: `1`

The scenario enters `0` in the `number_range` input width field, clicks the
screen's `Back to Main List` button, and expects `readClampedInt` to update the
model width from `200` to `1`.

Do not assume the E2E suite passes until this failure is fixed and the command
reports `PASSED`.

## What The Scenario Tests

The current `/testdialogeditor` scenario attempts to verify:

1. The Dialog Type button changes `notice` to `confirmation`.
2. The Add Body button opens the body editor.
3. The body editor Back button applies values and clamps width.
4. The Add Input button opens the input editor.
5. The input type button cycles to `number_range`.
6. Input width and initial range values are clamped.
7. Confirmation action buttons open the action editor.
8. Action type and command fields can be edited.
9. Save creates a dialog JSON file.
10. The saved dialog is hot-swapped into the dialog registry.
11. Multi Action can add an action through its Add Action button.
12. Confirmation, Multi Action, Server Links, and Dialog List JSON can save.
13. An invalid URL blocks Copy Inline JSON.
14. The generated temporary dialog file is deleted after the test.

## Manual E2E Run

Build first:

```powershell
.\gradlew.bat build
```

Start the development client:

```powershell
.\gradlew.bat runClient
```

In Minecraft:

1. Enter a singleplayer world.
2. Open chat.
3. Run:

```text
/testdialogeditor
```

Success output:

```text
[MapMakerUtils] In-Game UI automation test PASSED!
PASS: clicked navigation, body, input, action, save, type, and validation controls
```

Failure output:

```text
[MapMakerUtils] In-Game UI automation test FAILED!
FAIL: <failed assertion and actual value>
```

The development client must be restarted after rebuilding. An already running
client continues to use the old compiled classes.

## Agent Diagnosis Loop

An AI agent should use this process:

1. Read this document.
2. Read the latest failure:

```powershell
Get-Content -Tail 200 run\logs\latest.log
```

3. Locate the failure message and stack trace:

```powershell
Select-String -Path run\logs\latest.log -Pattern "automation test|FAIL:|IllegalStateException" -Context 2,8
```

4. Inspect the scenario near `runAutomationTest()` and the production method
   exercised by the failed step.
5. Fix the production bug if one exists. Fix the scenario only when its
   assumptions about the real screen are wrong.
6. Run:

```powershell
.\gradlew.bat build
```

7. Restart the development client.
8. Enter a singleplayer world and run `/testdialogeditor`.
9. Repeat until the in-game command reports `PASSED`.
10. Re-run `.\gradlew.bat build` once more before reporting completion.

## Failure Interpretation

### Widget is not clickable

Example:

```text
FAIL: Widget is not clickable: <name> at (<x>, <y>) while screen state is <state>
```

Check:

- Is the widget part of the current screen state?
- Is it below the visible region?
- Did `automationScrollIntoView` handle it?
- Did a previous click call `init()` and replace widget references?

Widgets are recreated whenever `init()` runs. Never keep a list index or widget
reference across a screen transition unless the field itself is refreshed by
`init()`.

### Value was not clamped

Example:

```text
FAIL: Input width was not clamped through the editor; actual=200
```

Inspect:

- `automationSetField(...)`
- `applySubEditorValues()`
- `readClampedInt(...)`
- The active `state`
- `selectedElementIndex`
- The current input type
- Which `editBoxN` represents the field

Add actual values to assertions rather than returning generic failures.

### Save failed

Check:

- `editorMessage`
- `validateForExport()`
- The generated JSON
- `run/logs/latest.log`
- The world datapack folder

The temporary test file is expected at:

```text
run/saves/<world>/datapacks/mapmakerutils_generated/data/test_temp/dialog/test_gui.json
```

It is deleted in the automation `finally` block after the scenario.

### Minecraft rejected the dialog

The editor validates with Minecraft `26.1.2`'s own `Dialog.DIRECT_CODEC`.
Diagnose the generated JSON and the reported CODEC error. Do not add Misode or
`vanilla-mcdoc` as runtime dependencies.

## Important Implementation Rules

- Use actual widget click paths for navigation and button behavior.
- Direct model mutation is acceptable only for setup that has no editable UI,
  or when the scenario explicitly documents the limitation.
- Prefer assertions containing expected and actual values.
- Keep temporary test resources under namespace `test_temp`.
- Always clean up generated test dialog files.
- Do not delete or alter user worlds outside the specific temporary dialog
  file.
- Keep unit tests. E2E tests complement unit tests; they do not replace them.
- Do not treat a successful build as an E2E pass.

## Recommended Unattended Runner

The current E2E command still requires a person or desktop automation to:

1. Launch Minecraft.
2. Enter a singleplayer world.
3. Run `/testdialogeditor`.
4. Read the result.

For fully unattended AI iteration, implement a test-only runner activated by:

```powershell
.\gradlew.bat runClient -PdialogE2E
```

The runner should:

1. Detect `dialogE2E` at development runtime only.
2. Automatically open a dedicated test world.
3. Open `DialogEditorScreen`.
4. Run the click-based scenario after screen initialization.
5. Write a machine-readable result:

```text
build/dialog-e2e/result.json
```

Suggested result format:

```json
{
  "passed": false,
  "step": "number-range-width-clamp",
  "expected": 1,
  "actual": 200,
  "message": "Input width was not clamped through the editor",
  "log": "run/logs/latest.log"
}
```

6. Exit Minecraft after writing the result.

An AI agent can then repeat:

```text
Run .\gradlew.bat runClient -PdialogE2E.
Read build/dialog-e2e/result.json and run/logs/latest.log.
Diagnose and fix the reported failure.
Repeat until passed.
```

Do not claim fully unattended E2E support until this runner is implemented and
verified.

## Completion Criteria

The Dialog Editor E2E work is complete only when all are true:

- `.\gradlew.bat build` succeeds.
- Unit tests pass.
- `/testdialogeditor` reports `PASSED` inside a singleplayer world.
- No unexpected exception appears in `run/logs/latest.log`.
- The temporary dialog file is removed.
- The final report states whether the run used the manual command or the future
  unattended runner.
