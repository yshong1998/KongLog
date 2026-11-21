---
title: "태그 이름순을 DB가 아니라 자바에서 정렬했다. DB의 이름순이 자바와 달랐다"
date: 2026-11-22
stage: 1
type: 결정
tags: [Spring, JPA, PostgreSQL, Java]
status: 메모
related: 이슈 #9, 풀 리퀘스트 #34, 커밋 7e2b999, backend/src/main/java/my/konglog/tag/TagQueryService.java, backend/src/main/java/my/konglog/tag/TagRepository.java
---

## 한 줄 요약

같은 "이름순"이라도 PostgreSQL(`en_US.utf8`)과 자바의 문자열 순서는 다르다. 태그 목록 API는 글 목록의 태그 순서와 맞추려고 조회문에 `order by`를 넣지 않고 자바의 `Comparator`로 정렬했다.

## 상황

`GET /api/tags`는 게시 글이 있는 태그를 "글이 많은 순, 글 수가 같으면 이름순"으로 내려준다.

"이름순"은 이미 한 군데서 쓰고 있었다. 글 목록 API(`GET /api/posts`)는 글마다 태그 이름의 배열을 자바에서 정렬해서 내려준다.

```java
List<String> tagNames = post.getTags().stream().map(Tag::getName).sorted().toList();
```

태그 목록을 만들면서 처음부터 자바에서 정렬하도록 썼다. DB의 글자 순서는 DB 설정을 따르니 자바와 다를 수 있다고 짐작했기 때문이다. 실제로 다른지는 그때 확인하지 않았다.

확인은 그 뒤에 했다. 서버를 띄워 API 결과를 DB에서 직접 센 값과 비교하려고 psql로 같은 집계를 돌렸는데, 글 수가 1인 태그들의 순서가 API와 달랐다.

```
-- psql: order by count(*) desc, t.name
네트워크|1
GitHub Actions|1
Java|1
Next.js|1
```

```
// API (자바 정렬)
{"name":"GitHub Actions","postCount":1},{"name":"Java","postCount":1},{"name":"Next.js","postCount":1},{"name":"네트워크","postCount":1}
```

태그 이름 전체를 두 가지로 정렬해 봤다.

```
konglog=# select string_agg(name, ', ' order by name) from tag;
네트워크, Docker, Git, GitHub Actions, HTTP, Java, JPA, Kubernetes, Next.js, PostgreSQL, React, Spring, TypeScript

konglog=# select string_agg(name, ', ' order by name collate "C") from tag;
Docker, Git, GitHub Actions, HTTP, JPA, Java, Kubernetes, Next.js, PostgreSQL, React, Spring, TypeScript, 네트워크
```

다른 곳은 두 군데다.

- 한글: DB 기본 순서는 `네트워크`가 맨 앞, 자바는 맨 뒤.
- 대소문자: DB 기본 순서는 `Java`가 `JPA` 앞, 자바는 `JPA`가 `Java` 앞. 자바는 글자 코드로 비교해서 대문자 `P`가 소문자 `a`보다 앞이다.

로컬 DB의 설정.

```
konglog=# select datcollate, datctype, datlocprovider from pg_database where datname='konglog';
en_US.utf8|en_US.utf8|c
```

`postgres:18` 이미지, `PostgreSQL 18.7 (Debian 18.7-1.pgdg13+5) on aarch64-unknown-linux-gnu`.

## 선택지

1. **조회문에 `order by count(p) desc, t.name`만 붙인다.**
   - 장점: 가장 단순하다. 조회문만 보면 결과의 순서까지 알 수 있다.
   - 단점: 글 수가 같은 태그끼리의 순서가 글 목록의 태그 순서와 다르다. 순서가 DB 설정을 따라가서, 배포 환경의 DB 설정이 로컬과 다르면 순서도 달라질 수 있다.
2. **조회문에서 `collate "C"`로 정렬한다.**
   - 장점: 조회문에서 정렬하면서 자바와 같은 순서가 나온다(위 psql 결과).
   - 단점: 조회문이 JPQL이라 SQL과 문법이 다르다. Hibernate에 `collate()` 함수가 있다고 알고 있지만 돌려 보지 않았다.
3. **조회문은 순서를 정하지 않고, 자바의 `Comparator`로 정렬한다.**
   - 장점: DB 설정과 상관없이 순서가 항상 같다. 글 목록과 같은 기준(자바의 기본 문자열 순서)을 쓴다.
   - 단점: 조회 결과를 모두 읽은 뒤에 정렬한다. 조회문만 봐서는 순서를 알 수 없다.

## 고른 것과 이유

3번을 골랐다.

```java
// TagRepository: 순서는 정하지 않는다
@Query("""
        select new my.konglog.tag.TagResponse(t.name, count(p))
        from Post p join p.tags t
        where p.status = my.konglog.post.PostStatus.PUBLISHED
        group by t.name
        """)
List<TagResponse> findWithPublishedPostCount();
```

```java
// TagQueryService
private static final Comparator<TagResponse> MOST_POSTS_FIRST =
        Comparator.comparingLong(TagResponse::postCount).reversed().thenComparing(TagResponse::name);

public TagListResponse getTagsWithPublishedPosts() {
    List<TagResponse> tags = tagRepository.findWithPublishedPostCount().stream()
            .sorted(MOST_POSTS_FIRST)
            .toList();
    return new TagListResponse(tags);
}
```

- 태그는 많아야 수십 개라 전부 읽어 정렬해도 부담이 없다. 글 목록처럼 일부만 잘라 읽는 조회였다면 DB에서 정렬해야 했다.
- 프론트엔드에 "두 API의 이름순은 같은 기준"이라고 말할 수 있다.
- 정렬 기준은 `Tag` 엔티티가 아니라 응답용 `TagResponse`에 건다. 글 수는 엔티티에 없고 조회 결과에만 있는 값이다.
- `Comparator`를 `static final` 상수로 둔 것은 성능 때문이 아니다. `MOST_POSTS_FIRST`라는 이름을 붙이고, "왜 여기서 정렬하는지" 주석을 달 자리를 만들려는 것이다. 메서드 안에 바로 써도 동작은 같다.

풀 리퀘스트를 검토하며 "쿼리에서 정렬할 수는 없나, 쿼리가 복잡해지나"라는 질문이 나왔다. 복잡해지지는 않는다. `order by` 한 줄이다. 고르지 않은 이유는 복잡함이 아니라 위 1번의 단점이다.

## 감수한 것

- "이름순은 자바의 기본 문자열 순서"라는 약속이 세 곳에 따로 적혀 있다: `PostSummaryResponse`, `PostDetailResponse`의 `.sorted()`와 `TagQueryService`의 `thenComparing(TagResponse::name)`. 한 곳만 바꾸면 어긋난다.
- 자바의 기본 순서는 사람이 기대하는 순서가 아닐 수 있다. `JPA`가 `Java`보다 앞이고, 소문자로 시작하는 이름은 대문자 이름 전체보다 뒤로 간다.
- 다시 검토할 조건: 태그가 수백 개로 늘거나 태그 목록을 잘라 읽게 되면 DB 정렬(2번)로 옮긴다. 대소문자를 가리지 않는 순서나 한글 가나다순이 필요해지면 세 곳을 함께 바꾼다.

확인하지 않은 것.

- 로컬 DB(`en_US.utf8`)가 왜 한글을 영문보다 앞에 놓는지.
- 배포 환경의 DB 설정이 무엇이 될지. 아직 배포 환경이 없다.
- JPQL에서 `collate()`가 실제로 도는지.
- `collate "C"`와 자바의 순서가 모든 글자에서 같은지. 지금 있는 태그 13개로만 비교했다. 이모지 같은 글자는 넣어 보지 않았다.

## 배운 점

- "이름순"은 하나가 아니다. DB는 설정(collation, 글자를 비교하는 규칙)에 따라 순서를 정하고, 자바의 `String` 기본 비교는 글자 코드 순서다.
- PostgreSQL에서 `order by 칸 collate "C"`를 쓰면 언어 규칙 없이 글자 코드 순서로 정렬한다.
- 같은 정렬 기준을 두 API가 쓰면, 한쪽은 DB에서 다른 쪽은 자바에서 정렬하는 순간 어긋날 수 있다.
- 짐작으로 고른 것도 실제 데이터로 확인해 봐야 한다. 한글 태그(`네트워크`)와 대소문자가 섞인 태그(`Java`, `JPA`)가 테스트용 글에 있어서 차이가 바로 보였다. 영문 태그 몇 개만 넣은 테스트는 어느 방식으로 정렬해도 통과했을 것이다.

## 글로 쓸 때 강조할 것

- psql의 두 줄 결과를 나란히 놓기. `order by name`과 `order by name collate "C"`.
- `Java`와 `JPA` 중 무엇이 앞인지 독자에게 먼저 물어보기.
- "쿼리가 복잡해져서"가 아니라 "기준을 하나로 맞추려고" 자바에서 정렬했다는 점.
- 태그 수가 적다는 전제가 깨지면 이 선택도 바뀐다는 점.
