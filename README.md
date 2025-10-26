# UltimateVotePlus

## v1.7.6
- Announce 방송을 3줄로 분리:
  1) `[알림] 마인리스트 추천 부탁드립니다! &b보상 &f받아가세요`
  2) `&7[ &e보상보기 클릭 &7]` (RUN_COMMAND: `/마인리스트 보상`)
  3) `&7[ &b추천링크 클릭 &7]` (OPEN_URL: `links.minelist`)
- 랭킹 월별 초기화: 매달 변경 감지 시 `byPlayer`/`bySite` 누적 랭킹 자동 초기화 (기본 타임존: `monthly-reward.timezone`).
- `links.minelist`는 `config.yml`에서 수정 가능.

# UltimateVotePlus

## v1.7.4
- **자동공지(/자동공지, autonotice.yml)**에서 나가는 메시지에는 더 이상 **[보상보기 클릭]** 버튼을 붙이지 않습니다. (요청 반영)
- **config.yml → announce** 방송에는 계속 **[링크] + " / 추천하시고 보상 받아가세요" + [보상보기 클릭]** UI가 붙습니다.
- 버전 업: plugin.yml / pom.xml → `1.7.4`.

# UltimateVotePlus

## v1.7.3
- 자동공지(announce)와 /자동공지 메시지에 **[보상보기 클릭]** 버튼을 일괄 추가 (RUN_COMMAND: `/마인리스트 보상`).
- 자동공지(announce) 메시지에 `링크 / 추천하시고 보상 받아가세요` 문구와 **[링크]** 버튼(OPEN_URL: `links.minelist`)을 함께 출력.
- 기본 설정값 업데이트: `announce.message` = `&a[알림]&f 마인리스트 추천 부탁드립니다` (나머지 링크/보상보기는 코드에서 자동 부착).
- 버전 업: plugin.yml / pom.xml -> `1.7.3`.

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
