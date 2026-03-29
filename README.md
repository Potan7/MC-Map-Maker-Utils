# Map Maker's Utils
Minecraft fabric mod

26.1 - 1.21.11

마인크래프트에 맵 제작에 유용한 커맨드들을 추가합니다.

adds useful commands for map making in Minecraft.

※ 추가되었으면 하는 기능 있으면 언제든지 알려주세요!

## 추가된 기능

### Commands

- /openpackfolder
  - datapack 폴더를 엽니다.
  - open the datapack folder.
- /hardreload
  - 맵을 통째로 리로드합니다. (나갔다 들어온 것과 동일)
  - reloads the entire map. (same as leaving and re-entering)
- /copypos
  - 클립보드에 현재 위치 좌표를 정수로 복사합니다.
    - copies the current position coordinates as integers to the clipboard.
- /copyrot
  -  클립보드에 현재 바라보는 방향(회전값)을 복사합니다.
  - copies the current facing direction (rotation values) to the clipboard.

### Features

- 데이터팩 파싱 오류 시 오류 메시지를 경고창에 출력합니다.
  - Displays error messages in a warning dialog when there is a datapack parsing error.

- 레지스트리를 수정한 데이터팩에서 뜨는 실험적 세계 경고를 이번 게임 동안 무시하는 버튼을 추가합니다.
  - Adds a button to ignore the experimental world warning that appears in datapacks that modify the registry for the duration of this game.