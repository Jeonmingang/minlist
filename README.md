# UltimateVotePlus v1.1.0

- Java 8 / Spigot 1.16.5 / CatServer 호환
- 기능
  - `/마인리스트` : 채팅에 **마인리스트/마인페이지 추천 링크** 출력 (링크는 `config.yml`에서 설정)
  - `/마인리스트 설정` (OP) : **GUI로 보상 아이템 설정** → NuVotifier 투표 수신 시 해당 아이템 자동 지급
  - 30초 간격(기본) **전체 채팅 안내**: 추천 부탁 메시지(+링크), 간격/문구/활성화 여부 설정 가능
  - 오프라인 보상 큐: 접속 시 자동 지급
- NuVotifier(VotifierEvent) 리플렉션 훅 사용 → 외부 의존성 없이 이벤트 수신

## 설치
1) NuVotifier 설치 + 포트 포워딩(8192/TCP). 마인리스트 설정 페이지에서 Votifier 정보를 입력.
2) 본 플러그인 jar를 `plugins/`에 넣고 재시작.
3) `config.yml`에서 `links.minelist`, `links.minepage`를 본인 서버 페이지로 변경.

## 사용 예
- `/마인리스트` → 채팅에 설정된 두 링크 노출
- `/마인리스트 설정` → GUI에서 좌측(10~16) 마인리스트 보상, 우측(28~34) 마인페이지 보상 배치 → **저장**
- 투표 수신 시 해당 아이템을 인벤토리에 지급(오프라인이면 큐에 저장)

## 설정 (config.yml)
```yml
links:
  minelist: "https://minelist.kr/servers/your-server-id"
  minepage: "https://mine.page/servers/your-server-id"
announce:
  enabled: true
  interval-seconds: 30
  message: "&a[알림]&f 마인리스트/마인페이지 추천 부탁드립니다! &e{minelist}&f / &b{minepage}"
rewards:
  slots:
    minelist: [10,11,12,13,14,15,16]
    minepage: [28,29,30,31,32,33,34]
  data:
    minelist: []
    minepage: []
queue-offline: true
```

## 마이그레이션 노트 (v1.1.0)
- 이전 `UltimateVoteReward`(v1.0.0) 사용자:
  - `/추천보상` → `/마인리스트`, `/마인리스트 설정`, `/마인리스트 리로드`로 통합
  - 보상 방식: **GUI 아이템 지급**이 기본 (직렬화 저장)
  - `queued.yml` 포맷 호환
- 권한: `uvp.admin` (기본 OP)
