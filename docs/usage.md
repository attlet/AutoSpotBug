# AutoSpotBug 사용 가이드

> **AutoSpotBug란?**
> GitLab에 올라가 있는 Java 프로젝트들을 자동으로 내려받아 버그를 찾아주는 도구입니다.
> 분석 결과는 XML 파일로 저장되며, IntelliJ에서 바로 열어볼 수 있습니다.

---

## 목차

1. [처음 한 번만 하는 준비 작업](#1-처음-한-번만-하는-준비-작업)
2. [분석할 프로젝트 목록 등록하기](#2-분석할-프로젝트-목록-등록하기)
3. [실행하기](#3-실행하기)
4. [결과 확인하기](#4-결과-확인하기)
5. [문제가 생겼을 때](#5-문제가-생겼을-때)

---

## 1. 처음 한 번만 하는 준비 작업

### 1-1. Java 설치 확인

AutoSpotBug를 실행하려면 Java 17 이상이 필요합니다.
아래 명령어를 터미널(명령 프롬프트)에 입력해 버전을 확인하세요.

```
java -version
```

결과가 아래처럼 `17` 이상이면 됩니다.

```
openjdk version "17.0.x" ...
```

버전이 낮거나 java 명령어를 찾을 수 없다면 [Java 17 다운로드](https://adoptium.net/)에서 설치하세요.

---

### 1-2. Git 설치 확인

```
git --version
```

`git version 2.x.x` 처럼 나오면 정상입니다. 없으면 [Git 다운로드](https://git-scm.com/)에서 설치하세요.

---

### 1-3. AutoSpotBug 내려받기 및 빌드

터미널을 열고 아래 명령어를 순서대로 입력합니다.

**Windows (명령 프롬프트 또는 PowerShell):**

```
git clone https://github.com/attlet/AutoSpotBug.git
cd AutoSpotBug
gradlew.bat shadowJar
```

**Mac / Linux (터미널):**

```
git clone https://github.com/attlet/AutoSpotBug.git
cd AutoSpotBug
./gradlew shadowJar
```

완료되면 `build/libs/autospotbug.jar` 파일이 생성됩니다. 이 파일이 AutoSpotBug 실행 파일입니다.

---

### 1-4. GitLab 토큰 발급받기

> **토큰이란?** GitLab에서 본인 계정을 증명하는 비밀번호 같은 것입니다. 이 토큰이 있어야 AutoSpotBug가 GitLab에서 프로젝트를 내려받을 수 있습니다.

1. 사내 GitLab에 로그인합니다.
2. 오른쪽 위 프로필 사진 클릭 → **Edit profile** 클릭
3. 왼쪽 메뉴에서 **Access Tokens** 클릭
4. 아래처럼 입력 후 **Create personal access token** 클릭:
   - Token name: `autospotbug` (구분하기 쉬운 이름)
   - Expiration date: 원하는 만료일 (공백이면 무기한)
   - **`read_repository` 체크** ← 이것만 체크하면 됩니다
5. 생성된 토큰을 복사해서 안전한 곳에 저장해두세요. **페이지를 나가면 다시 볼 수 없습니다.**

---

## 2. 분석할 프로젝트 목록 등록하기

### 2-1. 설정 파일 열기

AutoSpotBug 폴더 안에 있는 `conf/projects.yaml` 파일을 메모장이나 텍스트 편집기로 엽니다.

```
AutoSpotBug/
└── conf/
    └── projects.yaml   ← 이 파일을 엽니다
```

---

### 2-2. 프로젝트 추가하기

파일 내용을 아래 형식에 맞게 작성합니다.

**프로젝트 경로 확인 방법:**
GitLab에서 분석할 프로젝트를 열면 주소가 이렇게 나옵니다:

```
https://gitlab.company.com/그룹명/레포이름
```

여기서 `그룹명/레포이름` 부분이 `path`에 들어갑니다.

---

#### Spring Boot / Gradle 프로젝트

```yaml
projects:
  - name: admin-service         # 보고서 파일 이름 앞에 붙는 이름 (자유롭게 지정)
    path: group/admin-service   # GitLab URL에서 도메인 뒷부분
    branch: develop             # 분석할 브랜치 이름
```

#### Maven 프로젝트

Maven 프로젝트도 형식은 동일합니다. 빌드 도구는 자동으로 감지됩니다.

```yaml
  - name: batch-job
    path: group/batch-job
    branch: develop
```

#### Makefile로 빌드하는 순수 Java 프로젝트

빌드 결과물(JAR 파일)이 어디에 생기는지 알아야 합니다. 모르면 개발자에게 물어보세요.

```yaml
  - name: legacy-daemon
    path: group/legacy-daemon
    branch: develop
    build_tool: makefile              # Makefile 프로젝트임을 명시
    make_target: build                # Makefile에서 실행할 타겟 (모르면 생략)
    jar_path: dist/legacy-daemon.jar  # 빌드 후 JAR 파일 위치
```

JAR 파일이 아닌 클래스 폴더를 사용하는 경우:

```yaml
  - name: another-daemon
    path: group/another-daemon
    branch: develop
    build_tool: makefile
    classes_dir: out/production/classes  # 클래스 파일이 있는 폴더
```

---

### 2-3. 여러 프로젝트 한 번에 등록하기

`-` 로 시작하는 항목을 여러 개 작성하면 됩니다.

```yaml
projects:
  - name: admin-service
    path: group/admin-service
    branch: develop

  - name: batch-job
    path: group/batch-job
    branch: develop

  - name: legacy-daemon
    path: group/legacy-daemon
    branch: develop
    build_tool: makefile
    jar_path: dist/legacy-daemon.jar
```

> **주의:** 들여쓰기(띄어쓰기)가 틀리면 오류가 납니다. 각 항목 앞의 공백 개수를 동일하게 맞춰주세요.

---

## 3. 실행하기

터미널에서 AutoSpotBug 폴더로 이동한 후 아래 명령어를 실행합니다.

`https://gitlab.company.com` 부분은 **실제 사내 GitLab 주소**로,
`여기에_토큰_붙여넣기` 부분은 **1-4단계에서 복사한 토큰**으로 바꾸세요.

**Windows:**

```
java -jar build\libs\autospotbug.jar --gitlab-url https://gitlab.company.com --token 여기에_토큰_붙여넣기
```

**Mac / Linux:**

```
java -jar build/libs/autospotbug.jar --gitlab-url https://gitlab.company.com --token 여기에_토큰_붙여넣기
```

---

### 토큰을 명령어에 직접 쓰고 싶지 않은 경우 (보안)

명령어에 토큰을 직접 쓰면 기록에 남을 수 있습니다. 아래처럼 환경변수로 먼저 설정하면 `--token` 을 생략할 수 있습니다.

**Windows (명령 프롬프트):**

```
set GITLAB_TOKEN=여기에_토큰_붙여넣기
java -jar build\libs\autospotbug.jar --gitlab-url https://gitlab.company.com
```

**Mac / Linux:**

```
export GITLAB_TOKEN=여기에_토큰_붙여넣기
java -jar build/libs/autospotbug.jar --gitlab-url https://gitlab.company.com
```

---

### 실행 중 화면

정상적으로 실행되면 아래처럼 진행 상황이 출력됩니다.

```
[10:23:01] INFO  [admin-service] 분석 시작
[10:23:01] INFO  [admin-service] clone (branch: develop) → workspace/admin-service
[10:23:10] INFO  [admin-service] 빌드 도구: GRADLE
[10:23:10] INFO  [admin-service] Gradle SpotBugs 실행 중...
[10:23:44] INFO  [admin-service] 보고서 저장: output/admin-service-spotbugs.xml
...
[10:24:25] INFO  분석 완료: 2개 프로젝트 / 성공 2 / 실패 0
[10:24:25] INFO    ✔ admin-service → output/admin-service-spotbugs.xml
[10:24:25] INFO    ✔ legacy-daemon → output/legacy-daemon-spotbugs.xml
```

완료까지 프로젝트 크기에 따라 수 분이 걸릴 수 있습니다.

---

## 4. 결과 확인하기

### 보고서 파일 위치

분석이 끝나면 `output/` 폴더에 XML 파일이 생성됩니다.

```
AutoSpotBug/
└── output/
    ├── admin-service-spotbugs.xml
    ├── batch-job-core-spotbugs.xml   ← 멀티모듈 프로젝트는 모듈별로 분리
    ├── batch-job-api-spotbugs.xml
    └── legacy-daemon-spotbugs.xml
```

---

### IntelliJ에서 보고서 열기

1. IntelliJ에서 **SpotBugs** 플러그인을 설치합니다.
   - File → Settings → Plugins → `SpotBugs` 검색 후 설치
2. 아래 메뉴로 보고서를 불러옵니다:
   - SpotBugs 탭 → **Import Bug Collection** → XML 파일 선택

---

### 로그 파일

실행 내역은 `spotbugs-run.log` 파일에도 저장됩니다. 문제가 생겼을 때 이 파일을 참고하세요.

---

## 5. 문제가 생겼을 때

### "repository not found" 또는 "clone 실패" 메시지

GitLab에서 프로젝트를 찾지 못한 경우입니다.

**확인 목록:**
- `conf/projects.yaml`의 `path` 값이 GitLab URL에서 도메인 이후 경로와 정확히 일치하는지 확인
  - GitLab 주소: `https://gitlab.company.com/team/my-project`
  - `path` 값: `team/my-project`
- GitLab에서 해당 프로젝트에 접근 권한이 있는지 확인
- 1-4단계에서 발급한 토큰에 `read_repository` 권한이 있는지 확인
- `--gitlab-url` 주소 끝에 `/` 가 없는지 확인 (`https://gitlab.company.com/` ❌, `https://gitlab.company.com` ✅)

---

### "SpotBugs XML 보고서를 찾을 수 없습니다" 메시지

프로젝트 코드에 컴파일 오류가 있어서 분석을 진행할 수 없는 경우입니다.

해당 프로젝트의 개발자에게 `develop` 브랜치 코드가 정상적으로 빌드되는지 확인을 요청하세요.

---

### "분석 대상을 자동으로 찾을 수 없습니다" 메시지 (Makefile 프로젝트)

Makefile 프로젝트에서 빌드 결과물 위치를 찾지 못한 경우입니다.

`conf/projects.yaml`에 빌드 결과물 경로를 직접 지정해야 합니다. 해당 프로젝트 개발자에게 `make` 실행 후 JAR 파일이나 클래스 파일이 어느 폴더에 생성되는지 확인하세요.

```yaml
- name: legacy-daemon
  path: group/legacy-daemon
  branch: develop
  build_tool: makefile
  jar_path: dist/legacy-daemon.jar    # JAR 파일 경로를 여기에 입력
```

---

### "make 빌드 실패" 메시지

- Windows 환경이라면 `make`가 설치되어 있는지 확인하세요. (설치: [winget](https://winget.run/pkg/GnuWin32/Make) 또는 `choco install make`)
- `make_target` 값이 해당 프로젝트 Makefile에 실제로 정의된 타겟 이름인지 확인하세요.
- `make_target`을 생략하면 Makefile의 첫 번째 타겟이 실행됩니다.

---

### 인터넷이 안 되는 환경에서 Makefile 프로젝트 분석 실패

Makefile 프로젝트는 처음 실행 시 SpotBugs 분석 도구를 인터넷에서 자동으로 내려받습니다.
사내 방화벽으로 인터넷이 차단된 경우, 아래 경로에 SpotBugs를 수동으로 복사하면 다운로드를 건너뜁니다.

```
AutoSpotBug/
└── workspace/
    └── .spotbugs-cache/
        └── spotbugs-4.8.6/
            └── lib/
                └── spotbugs.jar   ← 이 파일이 있으면 다운로드 생략
```

SpotBugs 배포판은 [SpotBugs GitHub Releases](https://github.com/spotbugs/spotbugs/releases/tag/4.8.6)에서 `spotbugs-4.8.6.zip`을 내려받아 압축을 풀면 됩니다.

---

### 재실행 시 처음부터 새로 내려받고 싶은 경우

`workspace/` 폴더를 삭제하면 다음 실행 시 모든 프로젝트를 새로 내려받습니다.

**Windows:**

```
rmdir /s /q workspace
```

**Mac / Linux:**

```
rm -rf workspace/
```

특정 프로젝트만 새로 내려받고 싶다면 해당 폴더만 삭제하세요.

**Windows:**

```
rmdir /s /q workspace\admin-service
```

**Mac / Linux:**

```
rm -rf workspace/admin-service
```

---

## 부록: 전체 옵션 목록

```
java -jar build/libs/autospotbug.jar [옵션]

필수:
  --gitlab-url <주소>    사내 GitLab 서버 주소
                         예: https://gitlab.company.com

선택:
  --token <토큰>         GitLab 개인 액세스 토큰
                         생략 시 환경변수 GITLAB_TOKEN 사용
  --config <경로>        프로젝트 목록 파일 경로
                         (기본값: conf/projects.yaml)
  --output <경로>        보고서 저장 폴더
                         (기본값: output/)
  --workspace <경로>     임시 작업 폴더 (프로젝트 clone 위치)
                         (기본값: workspace/)
  -h, --help             도움말 출력
```
