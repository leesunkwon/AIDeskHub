# AI Desk Hub

B2B 범용 온디바이스 AI 거치형 비서 앱입니다.

안드로이드 태블릿을 가로형 스마트 디스플레이처럼 사용하며, 기관 안내, 음성 질의응답, 관리자 설정, 로컬 데이터 기반 안내 기능을 목표로 합니다.

## 현재 구현 상태

- 가로 모드 고정 태블릿 UI
- 상태바/내비게이션 바를 숨기는 키오스크형 풀스크린 모드
- 블랙 풀스크린 대기 화면과 커스텀 라인 그래픽
- 일반 사용자 모드와 관리자 모드 기본 구조
- 관리자 비밀번호 기반 진입 흐름
- Android `SpeechRecognizer` 기반 음성 질문 입력
- Gemini REST API 기반 단순 질의응답
- `gemini-3.1-flash-lite` 모델 호출
- `BuildConfig.GEMINI_API_KEY` 기반 API 키 주입
- 실제 API 키를 Git에 올리지 않는 로컬 secret 파일 구조

## 예정 기능

- Room 기반 로컬 지식 데이터베이스
- FTS 기반 로컬 검색
- 기관별 FAQ/운영 시간/부서 연락처 관리
- 길 안내용 이미지 등록 및 캐러셀 표시
- CSV/JSON Import, Export
- Android TTS 답변 읽기
- 오프라인 호출어 인식

## 프로젝트 구조

```text
AIDeskHub/
  app/
    src/main/
      java/com/kotlinsun/aideskhub/
        ai/       Gemini REST API 클라이언트
        voice/    음성 인식 컨트롤러
        data/     로컬 데이터 확장 준비 영역
        model/    화면 상태와 기본 모델
        admin/    Import/Export 확장 준비 영역
        ui/       커스텀 UI View
```

## API 키 설정

실제 Gemini API 키는 Git에 올리지 않습니다.

1. 예시 파일을 복사합니다.

```bash
cp AIDeskHub/secrets.properties.example AIDeskHub/secrets.properties
```

2. `AIDeskHub/secrets.properties`에 실제 키를 입력합니다.

```properties
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
```

3. 앱 빌드 시 Gradle이 `BuildConfig.GEMINI_API_KEY`로 주입합니다.

`secrets.properties`는 `.gitignore`에 포함되어 Git에 커밋되지 않습니다.

## 빌드

Android Studio에서 `AIDeskHub/` 디렉터리를 열거나 아래 명령을 실행합니다.

```bash
cd AIDeskHub
./gradlew :app:assembleDebug
```

로컬에 Java Runtime이 잡히지 않는 경우 Android Studio 내장 JDK를 사용할 수 있습니다.

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

## 권한

앱은 다음 권한을 사용합니다.

- `RECORD_AUDIO`: 음성 질문 인식
- `INTERNET`: Gemini REST API 호출

## 개발 원칙

- 서버 데이터베이스 없이 기기 내부 저장을 우선합니다.
- 실제 API 키와 로컬 설정 파일은 Git에 포함하지 않습니다.
- B2B 화이트라벨 적용을 고려해 기관별 커스터마이징 가능한 구조를 지향합니다.
- 이번 단계의 AI 질의응답은 단순 음성 질문과 Gemini 답변 표시까지를 범위로 합니다.
