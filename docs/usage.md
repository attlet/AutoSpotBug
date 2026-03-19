# AutoSpotBug 사용 매뉴얼

GitLab에서 관리되는 Java 프로젝트를 자동으로 SpotBugs 정적분석하여 XML 보고서를 추출하는 도구입니다.

---

## 목차

1. [사전 요구사항](#1-사전-요구사항)
2. [빌드](#2-빌드)
3. [프로젝트 목록 설정](#3-프로젝트-목록-설정)
4. [실행](#4-실행)
5. [동작 방식](#5-동작-방식)
6. [출력 결과](#6-출력-결과)
7. [옵션 전체 목록](#7-옵션-전체-목록)
8. [SpotBugs init script 커스터마이징](#8-spotbugs-init-script-커스터마이징)
9. [트러블슈팅](#9-트러블슈팅)

---

## 1. 사전 요구사항

| 항목 | 버전 | 비고 |
|------|------|------|
| JDK | 17 이상 | `java -version` 으로 확인 |
| Git | 2.x 이상 | 분석 대상 프로젝트 clone 용 |
| GitLab PAT | - | `read_repository` 권한 필요 |

> **분석 대상 프로젝트의 빌드 도구**
> 프로젝트에 `gradlew` / `mvnw` Wrapper가 있으면 별도 설치 불필요합니다.
> Wrapper가 없으면 시스템에 `gradle` 또는 `mvn`이 PATH에 등록되어 있어야 합니다.

> **GitLab Personal Access Token 발급**
> GitLab → 우측 상단 프로필 → Edit profile → Access Tokens
> → `read_repository` 권한 선택 후 생성

---

## 2. 빌드

```bash
# 저장소 clone
git clone https://github.com/attlet/AutoSpotBug.git
cd AutoSpotBug

# Fat JAR 빌드
./gradlew shadowJar
```

빌드 완료 시 `build/libs/autospotbug.jar` 가 생성됩니다.

---

## 3. 프로젝트 목록 설정

`config/projects.yaml` 에 분석할 GitLab 프로젝트 목록을 작성합니다.

```yaml
projects:
  - name: admin-service           # 보고서 파일명에 사용될 식별자
    path: group/admin-service     # GitLab 프로젝트 경로 (namespace/repo)
    branch: develop               # 분석 대상 브랜치

  - name: batch-job
    path: group/batch-job
    branch: develop
```

| 필드 | 필수 | 설명 |
|------|------|------|
| `name` | ✅ | 출력 XML 파일명 prefix. 영문/숫자/하이픈 권장 |
| `path` | ✅ | GitLab URL 뒤의 경로. `그룹명/레포명` 형식 |
| `branch` | ✅ | 분석할 브랜치명. 생략 시 `develop` 기본값 적용 |

---

## 4. 실행

### 기본 실행

```bash
java -jar build/libs/autospotbug.jar \
  --gitlab-url https://gitlab.company.com \
  --token YOUR_PERSONAL_ACCESS_TOKEN
```

### 토큰을 환경변수로 전달 (권장)

토큰을 `--token` 인수로 전달하면 shell history에 남습니다. 환경변수 사용을 권장합니다.

```bash
# Linux / macOS
export GITLAB_TOKEN=YOUR_PERSONAL_ACCESS_TOKEN

# Windows
set GITLAB_TOKEN=YOUR_PERSONAL_ACCESS_TOKEN

# --token 생략 가능
java -jar build/libs/autospotbug.jar --gitlab-url https://gitlab.company.com
```

### 개발 중 Gradle로 직접 실행

```bash
./gradlew run --args="--gitlab-url https://gitlab.company.com --token YOUR_TOKEN"
```

### 경로 옵션을 직접 지정하는 경우

```bash
java -jar build/libs/autospotbug.jar \
  --gitlab-url https://gitlab.company.com \
  --token YOUR_TOKEN \
  --config /path/to/my-projects.yaml \
  --output /path/to/reports \
  --workspace /path/to/workspace
```

---

## 5. 동작 방식

프로젝트 1개당 아래 순서로 처리됩니다.

```
1. Git clone / pull
   - workspace/{name}/.git 이 없으면 → single-branch clone
   - 이미 존재하면 → fetch → checkout → reset --hard origin/{branch}

2. 빌드 도구 자동 감지
   - build.gradle 또는 build.gradle.kts 존재 → GRADLE
   - pom.xml 존재 → MAVEN
   - 둘 다 없으면 → 오류

3. SpotBugs 실행 (대상 프로젝트 파일 수정 없음)
   [Gradle]
   - gradlew.bat (Windows) / gradlew (Linux·macOS) 우선 사용
   - 없으면 시스템 gradle 사용
   - 실행: spotbugsMain -x test --init-script spotbugs-init.gradle --continue --no-daemon
   - SpotBugs 설정: effort=max, reportLevel=low, ignoreFailures=true

   [Maven]
   - mvnw.cmd (Windows) / mvnw (Linux·macOS) 우선 사용
   - 없으면 시스템 mvn 사용
   - 실행: com.github.spotbugs:spotbugs-maven-plugin:spotbugs
           -DxmlOutput=true -Dspotbugs.effort=Max -Dspotbugs.threshold=Low
           -Dspotbugs.failOnError=false --batch-mode -DskipTests=true

4. XML 보고서 수집
   [Gradle] **/build/reports/spotbugs/*.xml
   [Maven]  **/target/spotbugsXml.xml

5. output/ 에 저장
   단일 모듈  → {name}-spotbugs.xml
   멀티 모듈  → {name}-{module}-spotbugs.xml
```

---

## 6. 출력 결과

### 보고서 파일명 규칙

| 프로젝트 구조 | 저장 파일명 예시 |
|---|---|
| 단일 모듈 | `admin-service-spotbugs.xml` |
| 멀티 모듈 (core, api) | `batch-job-core-spotbugs.xml`, `batch-job-api-spotbugs.xml` |

### 콘솔 출력 예시

```
[10:23:01] INFO     ==================================================
[10:23:01] INFO     [admin-service] 분석 시작
[10:23:01] INFO     [admin-service] clone (branch: develop) → workspace/admin-service
[10:23:10] INFO     [admin-service] 빌드 도구: GRADLE
[10:23:10] INFO     [admin-service] Gradle SpotBugs 실행 중...
[10:23:45] INFO     [admin-service] 보고서 저장: output/admin-service-spotbugs.xml
[10:23:45] INFO     ============================================================
[10:23:45] INFO     분석 완료: 2개 프로젝트 / 성공 2 / 실패 0
[10:23:45] INFO     [성공]
[10:23:45] INFO       ✔ admin-service → output/admin-service-spotbugs.xml
[10:23:45] INFO       ✔ batch-job → output/batch-job-spotbugs.xml
[10:23:45] INFO     보고서 저장 위치: /absolute/path/to/output
[10:23:45] INFO     ============================================================
```

### 로그 파일

실행 디렉토리에 `spotbugs-run.log` 로 동일한 내용이 저장됩니다.

### 종료 코드

| 종료 코드 | 의미 |
|-----------|------|
| `0` | 모든 프로젝트 성공 |
| `1` | 1개 이상 프로젝트 실패 (CI 파이프라인 연동 가능) |

### IntelliJ에서 XML 열기

> SpotBugs 탭 → Import Bug Collection → 생성된 XML 파일 선택

---

## 7. 옵션 전체 목록

```
java -jar build/libs/autospotbug.jar [OPTIONS]

필수:
  --gitlab-url <url>     GitLab 서버 URL
                         예: https://gitlab.company.com

선택:
  --token      <token>   GitLab Personal Access Token
                         미입력 시 환경변수 GITLAB_TOKEN 사용
  --config     <path>    프로젝트 목록 YAML 파일 경로
                         (기본: config/projects.yaml)
  --output     <path>    XML 보고서 저장 디렉토리
                         (기본: output/)
  --workspace  <path>    Git clone 임시 저장 디렉토리
                         (기본: workspace/)
  -h, --help             도움말 출력
```

---

## 8. SpotBugs init script 커스터마이징

Gradle 프로젝트 분석 시 사용되는 init script는 JAR 내부에 번들되어 있습니다.
SpotBugs 설정(effort, reportLevel 등)을 변경하려면 실행 디렉토리에 아래 파일을 생성하세요.
**로컬 파일이 있으면 JAR 내부 번들보다 우선 적용됩니다.**

```
AutoSpotBug/
└── resources/
    └── spotbugs-init.gradle   ← 이 파일이 존재하면 우선 사용
```

기본 설정값:

| 항목 | 값 | 설명 |
|------|-----|------|
| `effort` | `max` | 분석 정밀도 최대 |
| `reportLevel` | `low` | 낮은 심각도 버그까지 포함 |
| `ignoreFailures` | `true` | 버그 발견 시에도 빌드 실패 처리 안 함 |
| 출력 형식 | XML only | HTML 비활성화 |

---

## 9. 트러블슈팅

### git clone 실패

```
git clone 실패: fatal: repository not found
```

- `projects.yaml` 의 `path` 가 올바른지 확인 (`그룹명/레포명`)
- GitLab PAT의 `read_repository` 권한 확인
- `--gitlab-url` 끝에 `/` 없는지 확인

---

### SpotBugs XML 보고서를 찾을 수 없음

```
SpotBugs XML 보고서를 찾을 수 없습니다. 컴파일 오류를 확인하세요.
```

프로젝트가 컴파일 가능한지 수동으로 확인합니다.

```bash
cd workspace/프로젝트명

# Gradle
./gradlew compileJava

# Maven
./mvnw compile
```

JDK 버전도 확인합니다.
```bash
java -version
```

---

### Gradle Wrapper 실행 권한 오류 (Linux / macOS)

AutoSpotBug가 자동으로 실행 권한을 부여하지만, 수동 적용도 가능합니다.

```bash
chmod +x workspace/프로젝트명/gradlew

# 전체 일괄 적용
find workspace/ -name gradlew -exec chmod +x {} \;
```

---

### 재실행 시 전체 재clone 원하는 경우

`workspace/` 를 삭제하면 다음 실행 시 모든 프로젝트를 새로 clone합니다.

```bash
rm -rf workspace/
```

특정 프로젝트만 재clone하려면 해당 디렉토리만 삭제합니다.

```bash
rm -rf workspace/admin-service
```
