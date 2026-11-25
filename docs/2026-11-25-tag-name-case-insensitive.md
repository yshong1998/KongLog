---
title: "태그 이름의 대소문자를 무시하려고 lower(name)에 유일 색인을 걸었다. 그러자 seed.sql과 null 값이 깨졌다"
date: 2026-11-25
stage: 1
type: 결정
tags: [PostgreSQL, JPA, Flyway, Java]
status: 메모
related: 이슈 #40, 커밋 3cd32bf, 5aa6924, f94f4c1, 93bf54e, backend/src/main/resources/db/migration/V2__tag_name_case_insensitive.sql, backend/src/main/java/my/konglog/tag/Tag.java, backend/src/main/java/my/konglog/post/PostRepository.java, infra/local/seed.sql
---

## 한 줄 요약

"대소문자만 다른 태그 금지"는 `uq_tag_name`을 지우고 `lower(name)` 유일 색인으로 바꿔 DB가 지키게 했다. 소문자로 바꾸는 일은 자바가 아니라 DB의 `lower()` 한 곳에서만 한다. 그 바람에 seed.sql의 `on conflict (name)`이 깨졌고, 조회에서는 `lower(:tagName)`이 null일 때 타입 오류가 났다.

## 상황

기획(2026-11-22)에서 태그 이름 규칙을 정했다. 표기는 처음 만든 그대로 두고, 같은 태그인지는 대소문자를 구분하지 않고 비교한다. `spring`, `Spring`, `SPRING`은 같은 태그다. 주소로 거를 때도 마찬가지다(`/posts?tag=spring`).

V1의 태그 테이블은 이름이 대소문자까지 같아야만 막았다.

```sql
create table tag (
    id   bigint generated always as identity primary key,
    name varchar(50) not null,
    constraint uq_tag_name unique (name)
);
```

## 선택지

### 1. 대소문자만 다른 이름을 어디서 막나

1. **자바에서 저장 전에 찾아본다** (`existsByNameIgnoreCase`).
   - 단점: 동시에 두 요청이 오면 둘 다 "없다"를 보고 둘 다 저장한다. 결국 DB 규칙이 필요하다.
2. **소문자로 바꾼 칸(`name_lower`)을 하나 더 두고 거기에 unique.**
   - 단점: 값을 누가 채우나. 자바가 채우면 자바의 소문자 규칙이 들어간다. 칸이 늘어난다.
3. **`lower(name)`에 유일 색인.**
   - 장점: 칸을 늘리지 않는다. 비교 기준이 DB의 `lower()` 하나다. 태그 필터 조회 `lower(t.name) = lower(?)`에도 이 색인이 쓰일 수 있다.

3번을 골랐다.

### 2. 옛 `uq_tag_name`을 두나 지우나

- 둔다: seed.sql의 `on conflict (name) do nothing`이 그대로 돈다. 하지만 새 색인이 옛 규칙이 막던 것을 모두 막으니 같은 일을 두 번 한다. 그리고 seed가 `spring`이 이미 있는 DB에 `Spring`을 넣으면 `on conflict (name)`이 잡지 못하고 새 색인에서 오류가 난다.
- 지운다: 규칙이 하나다. seed.sql을 고쳐야 한다.

지웠다.

```sql
-- V2__tag_name_case_insensitive.sql
alter table tag drop constraint uq_tag_name;

create unique index uq_tag_name_lower on tag (lower(name));
```

### 3. 소문자를 자바와 DB 중 어디서 바꾸나

이슈에 "소문자로 바꾸는 방식이 자바와 DB에서 다를 수 있다. 한쪽 기준으로 맞춰 달라"는 메모가 있었다. 이름순이 자바와 DB에서 달랐던 일(`docs/2026-11-22-tag-name-sort-order.md`)과 같은 종류다.

유일 색인이 DB에 있으니 기준도 DB로 했다. 필터 조회도 `lower()`를 양쪽에 DB에서 건다. 자바 코드에는 `toLowerCase()`가 한 번도 나오지 않는다.

```java
// 고치기 전
and (:tagName is null or exists (select 1 from p.tags t where t.name = :tagName))
// 고친 뒤
and (:tagName is null or exists (select 1 from p.tags t where lower(t.name) = lower(cast(:tagName as String))))
```

### 4. 길이 50자를 어떻게 세나

DB 칸은 `varchar(50)`이고 PostgreSQL은 글자 수로 센다. 자바의 `String.length()`는 UTF-16 단위라 이모지 하나가 2다. 그래서 `codePointCount`로 셌다. `"😀" + "a".repeat(49)`는 자바 `length()`로 51이지만 만들 수 있고, DB에도 들어간다(테스트로 확인).

## 겪은 것

### seed.sql이 새 테이블에서 실패했다

V2가 적용된 스키마에 고치기 전 seed.sql을 돌렸더니:

```
ERROR:  there is no unique or exclusion constraint matching the ON CONFLICT specification
```

`on conflict (name)`은 `name` 칸 그대로의 유일 규칙을 찾는데, 그 규칙을 지웠기 때문이다. 이슈에 미리 적혀 있던 그대로다.

고친 것:

```sql
-- 고치기 전
on conflict (name) do nothing;
...
join tag t on t.name = st.tag_name;

-- 고친 뒤
on conflict ((lower(name))) do nothing;
...
join tag t on lower(t.name) = lower(st.tag_name);
```

- `on conflict`에는 칸 대신 식을 쓸 수 있다. 식은 괄호를 한 겹 더 씌운다: `((lower(name)))`. 색인의 식과 같아야 그 색인을 찾는다.
- 글과 태그를 잇는 `join`도 바꿔야 했다. 직접 넣어 둔 `docker` 태그가 있는 상태에서 seed를 돌리면, 새 `Docker`는 들어가지 않고(on conflict) 옛 `join`은 `Docker`를 찾지 못해 글 5개가 태그 없이 남는다.
- 확인: 소문자 `docker`를 먼저 넣고 seed를 돌리면 표기는 `docker`로 남고 글 5개가 이어졌다.

실수 하나: 처음에 seed를 worktree가 아니라 원래 폴더(`main`)의 파일로 돌려서, 고쳤는데도 같은 오류가 났다. 직접 `insert ... on conflict ((lower(name)))`를 쳐 보고서야 알았다.

### tag 값이 없을 때 조회가 모두 실패했다

필터를 `lower(:tagName)`으로 바꾸자 `PostListApiTest`, `PostListQueryTest`의 테스트 14개가 실패했다. 테스트 결과 파일(`build/test-results/test/*.xml`)에서 `ERROR:` 줄을 세어 보니 이것이 84번 나왔다. 테스트 하나에 여러 번 찍힌다.

```
ERROR: function lower(bytea) does not exist
```

`tag` 값이 없으면 Hibernate가 null을 타입 없이 넘기고, PostgreSQL은 이 값을 `bytea`로 본다. 고치기 전에는 `t.name = :tagName`이라 비교 상대(`varchar`)를 보고 타입을 알았다. `lower(?)`에는 그런 단서가 없다. `docs/2026-11-22-query-null-parameter-type.md`에서 발행일에 `cast(:afterPublishedAt as Instant)`를 붙인 것과 같은 문제라, 같은 방법(`cast(:tagName as String)`)으로 고쳤다.

## 고른 것과 이유

- 규칙을 지키는 곳은 DB 하나다(유일 색인). 자바는 이름 다듬기와 검사만 한다.
- 소문자로 바꾸는 기준도 DB의 `lower()` 하나다. 자바와 DB가 다르게 바꿀 수 있는 글자가 있어도 어긋날 일이 없다.
- 기획에 없던 것 하나를 더 막았다: 줄바꿈 말고 다른 제어 문자(탭, null 글자 등). 글 목록 API가 `tag`에 제어 문자가 있으면 400을 내서, 그런 이름의 태그는 주소로 거를 수 없다. 사용자에게 물어보고 정했다.
- 줄바꿈은 공백을 줄이기 전에 검사한다. 먼저 줄이면 `"Spring\nBoot"`가 `"Spring Boot"`가 되어 줄바꿈이 있었는지 알 수 없다.

## 감수한 것

- `lower()`의 결과는 DB 설정(`lc_ctype`, 로컬은 `en_US.utf8`)을 따른다. 배포 DB의 설정이 다르면 몇몇 글자의 비교가 달라질 수 있다. 영어와 한글 태그에서는 차이가 없다.
- `lower()`는 대소문자 접기(case folding)가 아니다. 독일어 `ß`와 `SS`는 같다고 보지 않는다.
- 운영 DB에 이미 대소문자만 다른 태그가 있으면 V2가 실패한다. 지금은 운영 DB가 없다.
- 이미 있는 태그와 대소문자만 다른 이름을 입력하면 기존 태그를 붙이는 규칙은 3단계에서 만든다. 지금 그런 이름을 저장하면 `DataIntegrityViolationException`이다.

확인하지 않은 것.

- 필터 조회가 실제로 `uq_tag_name_lower` 색인을 쓰는지(`explain`). 태그가 13개라 써도 안 써도 차이가 없다.
- `lower(cast(? as String))`이 색인 식 `lower((name)::text)`과 같은 식으로 잡히는지.

## 배운 점

- 유일 규칙을 바꾸면 그 규칙에 기대던 `on conflict`가 깨진다. 규칙을 찾는 SQL을 함께 찾아봐야 한다.
- `on conflict`에는 식도 쓸 수 있다. 괄호를 한 겹 더 씌운다.
- `:값 is null` 문제는 시각만의 일이 아니다. 비교 상대 없이 함수에 넘기는 값이면 문자열도 같은 오류가 난다. 오류의 `bytea`가 단서다.
- worktree에서 일할 때 명령을 원래 폴더에서 돌리면 고치기 전 파일이 돈다.

## 글로 쓸 때 강조할 것

- "같은 태그"의 기준을 어디에 둘지 고르는 과정. 자바의 중복 확인만으로는 동시에 오는 요청을 못 막는다.
- `ERROR: function lower(bytea) does not exist`에서 `bytea`가 어디서 왔는지.
- `on conflict ((lower(name)))`의 괄호 두 겹.
