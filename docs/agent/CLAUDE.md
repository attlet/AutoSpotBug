# CLAUDE.md - Project Guide

> 세부 가이드는 `docs` 폴더를 참조하세요.

## "go" 명령어 워크플로우

사용자가 "go"를 입력하면 다음 순서로 진행:

1. **plan.md 확인**: `plan#{num}.md` 파일에서 다음 미완료 테스트(`[ ]`)를 찾음 ( ex.plan#1.md)
2. **테스트 시나리오 설계**: Given-When-Then 형식으로 테스트 케이스 상세화
3. **구현 계획 제시**: 테스트 통과를 위한 최소 구현 방안 제안 — 반드시 포함:
   - 생성할 파일 목록
   - 수정할 파일 목록
   - `constraints.md` 충돌 여부
4. **사용자 승인 대기**: 계획을 사용자에게 보여주고 승인 요청 (승인 없이 구현 진행 불가)
5. **승인 후 구현**: 승인되면 TDD 사이클(Red→Green→Refactor) 진행

## Project Overview

Kotlin + Spring Boot 기반 SNS 백엔드 서비스

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 1.8.20 / Java 17 |
| Framework | Spring Boot 3.2.5 |
| DB/ORM | MariaDB, Spring Data JPA, QueryDSL 5.1.0 |
| Messaging | RabbitMQ (Local) / Kafka (Prod) |
| Auth | JWT + GitHub OAuth2 |

## Package Structure



## Commands

```bash
./gradlew build                    # 빌드
./gradlew test                     # 전체 테스트
./gradlew bootRun --args='--spring.profiles.active=local'  # 로컬 실행
```

## Code Conventions

- Google Java Style Guide 기반
- 메서드는 단일 책임, early return 패턴 사용
- 예외: `ErrorCode` + `CustomException`
- DTO 변환: MapStruct 사용
- 주석 작성 시, 클래스 및 메서드 위에 KDoc 스타일로 작성

### JPA 쿼리 작성 규칙

| 방식 | 사용 기준 | 예시 |
|------|-----------|------|
| Derived Method | 조건 1~2개, 의미가 직관적일 때 | `findByEmail`, `findByIsDeletedFalse` |
| `@Query` JPQL | 조건 3개 이상, 조인 포함, 메서드명이 30자 초과 | `@Query("SELECT r FROM Review r WHERE ...")` |
| QueryDSL | 동적 조건, fetchJoin, 페이징 | `ReviewRepositoryCustomImpl` |

**규칙 요약:**
- Derived Method 이름이 30자를 넘거나 조건이 3개 이상이면 반드시 `@Query` 사용
- `@Query` 사용 시 메서드명은 의도를 표현 (`findActiveById`, `findActiveByMemberAndContent`)
- `@Param`으로 파라미터를 명시적으로 바인딩

## AI Checklist

- [ ] 가장 작은 단위의 변경인가?
- [ ] 기존 도메인 패턴을 따랐는가?
- [ ] `constraints.md`와 충돌하는가? (충돌 시 구현 전 사용자에게 명시)
- [ ] 생성/수정할 파일 목록을 구현 전에 먼저 나열했는가?
- [ ] 기존 파일 수정으로 대체 가능한가?
- [ ] 테스트 코드 실행/작성을 제안했는가?
- [ ] 새 파일 생성 시 `git add`를 실행했는가?

## Git Policy

- **새 파일 생성 시 자동 `git add`**: AI가 새로운 파일을 생성하면 즉시 `git add <파일경로>`를 실행하여 Git 추적에 포함시킨다.

## Related Docs

- [AI-Human 협업 정책](docs/agentMd/collaboration-policy.md)
- [테스트 정책](docs/agentMd/testing-policy.md)
- [계획 템플릿](docs/agentMd/plan-template.md)
- [증강형 코딩 원칙](docs/agentMd/augmented-coding.md)
- [설계 제약](docs/agentMd/constraints.md) - **구현 전 반드시 확인**