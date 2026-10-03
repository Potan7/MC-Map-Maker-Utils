# Map Maker's Utils (MMU)
**Streamline your Map Making & Data Pack workflow in Minecraft 26.3**
마인크래프트 맵 제작과 데이터팩 개발을 위한 유틸리티 모드입니다.

## Download / 다운로드
[Modrinth](https://modrinth.com/mod/map-maker-utils)


---

## Overview / 개요
**Map Maker's Utils** is a lightweight Fabric mod designed to reduce repetitive tasks for map makers and data pack developers. It provides essential commands and UI improvements to handle registry changes and data pack debugging more efficiently.

이 모드는 맵 제작자와 데이터팩 개발자가 겪는 반복적인 작업을 줄여주기 위해 설계되었습니다. 편리한 파일 관리 기능은 물론, 레지스트리 변경이나 데이터팩 오류 수정을 위한 UI 개선 기능을 제공합니다.

---

## Features / 주요 기능

### Debug & UI / 디버그 및 UI 개선
*   **Reload Diagnostics / 리로드 오류 표시**
    *   **EN**: Shows only the file path (or resource ID if no path is supplied) and available line number for data pack loading errors during `/reload`. Covers functions, advancements, recipes, loot tables, predicates, item modifiers, tags and other reloadable data. Codec/validation errors that provide no source line are marked "line number unavailable". Requires this mod on the server.
    *   **KR**: `/reload` 중 데이터팩 로딩 오류가 발생하면 파일 경로(경로가 제공되지 않으면 리소스 ID)와 확인 가능한 줄 번호만 짧게 출력합니다. 함수, 발전 과제, 레시피, 전리품 테이블, 조건자, 아이템 수정자, 태그 및 기타 리로드 대상 데이터를 포함합니다. 원본 줄 번호를 제공하지 않는 Codec·검증 오류는 '줄 번호 없음'으로 표시하며, 서버에 모드가 설치되어 있어야 합니다.
*   **Safe Mode Error Visualization / 안전 모드 오류 시각화**
    *   **EN**: When entering Safe Mode due to a data pack failure, the specific error log is displayed at the top of the screen for quick debugging.
    *   **KR**: 데이터팩 파싱 실패로 '안전 모드' 진입 시, 화면 상단에 원인이 되는 오류 로그를 직접 표시하여 빠른 수정을 돕습니다.
*   **Experimental Session Bypass / 실험적 설정 세션 무시**
    *   **EN**: Adds an "Ignore for this session" button to the Experimental Registry warning screen. Once clicked, the warning won't appear for that specific map until the game is restarted.
    *   **KR**: 레지스트리 수정 시 발생하는 '실험적 설정' 경고창에 '이번 세션 무시' 버튼을 추가합니다. 버튼을 누르면 게임 재부팅 전까지 해당 맵에서 경고창이 더 이상 뜨지 않습니다.

### Workflow Tools / 작업 효율 도구
*   **`/devsetup`**
    *   **EN**: Runs configurable world development commands. Defaults to frozen time at noon, clear frozen weather, disabled mob spawning, and disabled mob griefing. Requires game master permission (level 2, like `/gamerule`). Works in singleplayer with cheats and on servers with this mod installed.
    *   **KR**: 개발용 월드 설정 명령을 일괄 실행합니다. 기본값은 정오 시간 고정, 맑은 날씨 고정, 몹 스폰 방지, 몹 파괴 방지입니다. `/gamerule`과 같은 명령어 권한(레벨 2)이 필요하며, 치트를 허용한 싱글플레이 또는 모드가 설치된 서버에서 사용할 수 있습니다.
*   **`/dialogeditor [dialogId]`** (after 1.2.0)
    *   **EN**: Opens an in-game editor for creating a new Dialog or editing an existing Dialog by ID. Supports all Minecraft Dialog types, validates and normalizes input values, saves the JSON into a world data pack, and applies changes immediately through registry hot-swapping. Shows a warning screen if the target datapack does not exist yet (added in 1.2.1).
    *   **KR**: 새로운 Dialog를 만들거나 ID로 기존 Dialog를 편집할 수 있는 인게임 편집기를 엽니다. Minecraft의 모든 Dialog 유형을 지원하며, 입력값을 검증 및 보정하고 월드 데이터팩에 JSON을 저장한 뒤 레지스트리 hot-swap을 통해 변경사항을 즉시 적용합니다. 아직 존재하지 않는 데이터팩에 저장하려 할 시 경고 화면을 띄워줍니다 (1.2.1 추가).
*   **`/openpackfolder`**
    *   **EN**: (Singleplayer Only) Instantly opens the `datapacks` folder of the current world in your OS file explorer.
    *   **KR**: (싱글플레이 전용) 현재 월드의 `datapacks` 폴더를 파일 탐색기로 즉시 엽니다.
*   **`/openvscode`** (after 1.2.0)
    *   **EN**: (Singleplayer Only) Opens the current world's `datapacks` folder directly in **Visual Studio Code**. (Requires `code` command in PATH)
    *   **KR**: (싱글플레이 전용) 현재 월드의 `datapacks` 폴더를 **Visual Studio Code**로 즉시 엽니다. (시스템 PATH에 `code` 명령어가 등록되어 있어야 합니다.)
*   **`/hardreload`**
    *   **EN**: (Singleplayer Only) Reloads the entire world (functions like re-joining). Useful for applying deep changes that `/reload` cannot handle.
    *   **KR**: (싱글플레이 전용) 맵을 통째로 리로드합니다. 게임을 나갔다 들어오는 것과 동일하며, 일반 리로드로 적용되지 않는 변경 사항을 확인할 때 유용합니다.

### Clipboard Utilities / 클립보드 유틸리티
*   **`/copypos`**: Copies current position `(x y z)` as integers to the clipboard. / 현재 위치 좌표를 정수 형태로 클립보드에 복사합니다.
    *   **`/copypos me`**: Explicitly copies your own position. / 자신의 현재 위치를 복사합니다.
    *   **`/copypos lookat`**: Copies the coordinates of the block you are currently looking at. / 현재 바라보고 있는 블록의 좌표를 복사합니다.
*   **`/copyrot`**: Copies exact facing direction `(Yaw Pitch)` to the clipboard. / 현재 바라보는 방향 값을 클립보드에 복사합니다.

---

## Development setup config / 개발 환경 설정
The mod creates `config/mapmakerutils.json` on startup. Edit `devSetupCommands` to add, remove, or reorder commands. The file is read again on every `/devsetup`; no restart is needed. On multiplayer servers, edit the server's config. Commands run in order as the caller, with their position, dimension and permissions. A leading `/` is optional. Invalid JSON or entries prevent execution without overwriting the file; individual command errors are reported by Minecraft.

모드 실행 시 `config/mapmakerutils.json`이 생성됩니다. `devSetupCommands` 목록을 수정해 명령을 추가·삭제하거나 순서를 바꿀 수 있습니다. `/devsetup`을 실행할 때마다 파일을 다시 읽으므로 재시작이 필요 없습니다. 멀티플레이에서는 서버의 설정 파일을 수정하세요. 명령은 실행자의 위치·차원·권한을 기준으로 순서대로 실행하며, 앞의 `/`는 생략할 수 있습니다. 잘못된 JSON이나 목록 항목은 실행을 중단하고 기존 파일을 유지합니다. 개별 명령어 오류는 Minecraft가 표시합니다.

```json
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
```

## Requirements / 요구 사양
*   **Minecraft**: 26.3 (v26.3-1.3.0). For Minecraft 26.2, use the `26.2` branch (v26.2-1.2.2).
*   **Java**: 25+
*   **Fabric Loader**: 0.19.5+
*   **Fabric API**: Required

## License / 라이선스
This mod is available under the **MIT License**.

### 최신 기능에 대하여
- 현재 최신 기능은 항상 최신 버전을 기준으로 제작됩니다. 이전 버전에서는 일부 기능이 지원되지 않을 수 있습니다. 만약 요청이 들어온다면 이전 버전에서도 사용할 수 있도록 기능을 추가할 수도 있습니다. (예: `/dialogeditor` 명령어는 1.2.0 에 추가되었으며 이는 26.1.2 버전 이상에서만 사용할 수 있습니다.)

---

## Changelog / 변경 사항
### v26.3-1.3.0
- Show registry file paths, including registry folders, on the data pack loading failure screen; wrap long paths to fit the screen.
- Add `/devsetup` with configurable commands in `config/mapmakerutils.json` for world development settings.
- Report data pack loading errors during `/reload` as file paths and available line numbers, including functions, tags and reloadable registries.
- `/devsetup` 개발 환경 설정 명령어와 커스텀 명령 목록 설정을 추가했습니다.
- `/reload` 중 데이터팩 오류의 파일 경로와 확인 가능한 줄 번호를 채팅에 표시합니다.

### v26.3-1.2.2
- Update to Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 and Gradle 9.6.0.
- Use Minecraft's mouse button constants so the dialog editor scrollbar works with the SDL input backend.
- Migrate `/openpackfolder` to Minecraft's `Blaze3D.openPath` API.

### v26.2-1.2.2
- Generate new data packs with the running Minecraft version's data pack format.
- Align CI and Mixin compatibility with Java 25 and pin Fabric Loom to 1.17.21.

### v26.2-1.2.1 & v26.1.2-1.2.1
- **EN**: Added a safety confirmation dialog when attempting to save a dialog to a non-existent datapack.
- **KR**: 존재하지 않는 데이터팩에 다이얼로그를 저장하려고 할 때 안전 확인 경고창을 띄우는 기능을 추가했습니다.


### v26.1.2-1.2.0
- Added `/dialogeditor [dialogId]`, an in-game editor for creating and editing Minecraft 26.1.2 Dialog JSON files.
    - Supports `notice`, `confirmation`, `multi_action`, `server_links`, and `dialog_list`.
    - Supports body elements, input controls, actions, and inline or referenced dialogs.
    - Validates and normalizes values before exporting JSON.
    - Saves Dialog files into a selected world data pack and hot-swaps them into the running singleplayer world.
- Added `/openvscode` command to open current world's datapack folder directly in Visual Studio Code (requires `code` command in PATH).
- Added '/copypos lookat' subcommand to copy coordinates of the block the player is currently looking at.
    - `/copypos` is same as `/copypos me` for backward compatibility.

### 1.1.0 (1.21.11)
Added Error Log display on Safe Mode screen.

Added "Ignore for session" button for Experimental/Registry warnings.

Synchronized Mod ID and Filename for consistency.

Updated versioning scheme to MCVersion-ModVersion.
