# UltimateVotePlus v1.2.0

- 보상 미지급 이슈 개선: 플레이어 대소문자 불일치 시 온라인 검색 보완(`allow-fuzzy-online-lookup`), 디버그 로그 추가.
- **보상 지급 시 전체 브로드캐스트 + 누적 카운트**(`votes.yml`) 추가.
- 명령 `/마인리스트 테스트 <닉> [minelist|minepage]` 추가.

## 새 설정 키
```yml
reward:
  queue-offline: true
  allow-fuzzy-online-lookup: true
broadcast-on-reward:
  enabled: true
  message: "&a[추천]&f {player} 님이 &e{site}&f 추천 완료! &7[ 누적 추천수 {count_total} ]"
debug:
  log: true
```
카운트 플레이스홀더: `{count_total}`, `{count_minelist}`, `{count_minepage}`


## v1.6.2
- 전체공지(자동공지/announce) 및 추천 브로드캐스트 메시지에 **[보상보기]** 클릭 버튼 추가 (RUN_COMMAND: `/마인리스트 보상보기`).
- `/마인리스트 보상보기` 추가: 현재 설정된 보상을 **보기 전용 GUI**로 열람 (아이템 끄내기/넣기 불가).
- 기존 기능 전체 유지. 대상: 1.16.5 / Java 11.
