# 네트워크 오목 (2025COMP2TermProject)

간단한 네트워크 오목 프로젝트입니다. 서버-클라이언트 구조로 동작하며,
두 명의 클라이언트가 접속하면 서버에서 게임을 진행합니다. 서버는 이동을
브로드캐스트하고 승리 여부를 판정하여 기록합니다.

**요약:**
- **언어:** Java (JDK 필요)
- **포트:** 5000 (기본)

**파일 및 역할:**
- `OmokServer/OmokServer.java`: 서버 진입점, 클라이언트 연결 관리 및 게임 흐름 제어
- `OmokServer/ClientHandler.java`: 각 클라이언트 연결을 처리하는 스레드
- `OmokServer/GameBoard.java`: 서버 측 게임 상태(보드, 턴, 승리 판정, 기록)
- `OmokClient/OmokClient.java`: 클라이언트 진입점 및 게임 창 생성
- `OmokClient/NetworkHandler.java`: 서버와의 통신(수신/전송 및 메시지 처리)
- `OmokClient/BoardPanel.java`: 보드 GUI 및 마우스 입력 처리
- `OmokClient/GameRule.java`: 승리/무승부 판정 유틸리티

**사전 준비(Requirements)**
- Java JDK가 설치되어 있어야 합니다 (JDK 8 이상 권장).
- Windows에서 PowerShell을 사용하여 실행 예시를 제공합니다.

**빌드 (PowerShell)**
다음 명령으로 모든 소스 파일을 컴파일하고 `out` 폴더에 클래스파일을 생성합니다.

```bat
# 프로젝트 루트(이 README가 있는 폴더)에서 실행
javac -d out OmokServer\*.java OmokClient\*.java
```

**서버 실행 (PowerShell)**
서버는 포트 5000으로 대기합니다. 같은 컴퓨터에서 실행하려면 아래처럼 실행하세요.

```bat
java -cp out OmokServer
```

**클라이언트 실행 (PowerShell)**
클라이언트를 실행하면 서버 IP 입력을 요청합니다. 같은 컴퓨터라면 `localhost`를 입력합니다.

```bat
java -cp out OmokClient
```

클라이언트는 서버에 접속되면 플레이어 ID(1 또는 2)를 받아 게임을 시작합니다.

**게임 방법 (간단)**
- 보드에서 빈 칸을 클릭하면 해당 위치로 이동 요청을 서버에 전송합니다.
- 서버가 유효성을 검증한 뒤 모든 클라이언트에 이동을 브로드캐스트합니다.
- 5연속을 만든 플레이어가 승리하며, 서버는 `record.txt`에 결과를 기록합니다.

**네트워크/방화벽 주의사항**
- 서버가 동작하는 컴퓨터의 방화벽에서 포트 5000을 허용해야 외부 클라이언트가 접속할 수 있습니다.

**문제 해결(Troubleshooting)**
- 컴파일 에러 발생 시: JDK가 설치되어 있고 `javac`가 PATH에 포함되어 있는지 확인하세요.
- 클라이언트가 서버에 연결되지 않음: 서버가 실행 중인지, 올바른 IP/포트로 접속했는지 확인하세요.
- 기록 파일 `record.txt`는 서버 실행 디렉터리에 생성됩니다.

---
원하시면 README에 더 자세한 사용 예시(스샷, UI 설명)나 테스트 스크립트도 추가해드리겠습니다.

we will make omok project
