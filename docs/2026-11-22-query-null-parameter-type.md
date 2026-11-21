---
title: "@Query에서 ':값 is null'을 썼더니 PostgreSQL이 타입을 모른다고 했다"
date: 2026-11-22
stage: 1
type: 문제 해결
tags: [Spring, JPA, PostgreSQL]
status: 메모
related: 이슈 #8, 풀 리퀘스트 #33, 커밋 74223c8(고치기 전), 1752391(고친 뒤), backend/src/main/java/my/konglog/post/PostRepository.java
---

## 한 줄 요약

`@Query`에서 "값이 있을 때만 거는 조건"을 `(:값 is null or ...)`로 쓸 때, 값이 시각(`Instant`)이면 PostgreSQL이 타입을 알 수 없다는 오류를 낸다. `cast(:값 as Instant) is null`로 타입을 알려 주면 된다.

## 상황

글 목록 조회는 조건이 두 개고, 둘 다 있을 수도 없을 수도 있다. 태그는 `tag` 값이 왔을 때만, 커서는 첫 요청이 아닐 때만 붙는다.

처음에는 `EntityManager`로 조회문을 코드에서 조립했다(커밋 74223c8).

```java
public List<Post> findPublished(String tagName, PostCursor after, int limit) {
    StringBuilder jpql = new StringBuilder("select p from Post p where p.status = :status");
    if (tagName != null) {
        jpql.append(HAS_TAG);
    }
    if (after != null) {
        jpql.append(" and (p.publishedAt < :publishedAt or (p.publishedAt = :publishedAt and p.id < :id))");
    }
    jpql.append(" order by p.publishedAt desc, p.id desc");

    TypedQuery<Post> query = entityManager.createQuery(jpql.toString(), Post.class)
            .setParameter("status", PostStatus.PUBLISHED)
            .setMaxResults(limit);
    if (tagName != null) {
        query.setParameter("tagName", tagName);
    }
    if (after != null) {
        query.setParameter("publishedAt", after.publishedAt());
        query.setParameter("id", after.id());
    }
    return query.getResultList();
}
```

동작은 했지만, 풀 리퀘스트를 읽다가 "무슨 조회인지 알려면 코드를 너무 많이 읽어야 한다"는 지적이 나왔다. 그래서 조회문을 `@Query`에 통째로 적는 방식으로 바꾸기로 했다.

## 겪은 것

아래처럼 바꾸고 테스트를 돌렸더니 `PostListApiTest` 17개 중 15개가 실패했다. 통과한 2개는 조회까지 가지 않고 400으로 끝나는 테스트였다.

```java
@Query("""
        select p from Post p
        where p.status = my.konglog.post.PostStatus.PUBLISHED
          and (:tagName is null or exists (select 1 from p.tags t where t.name = :tagName))
          and (:publishedAt is null
               or p.publishedAt < :publishedAt
               or (p.publishedAt = :publishedAt and p.id < :id))
        order by p.publishedAt desc, p.id desc
        """)
List<Post> findPublished(String tagName, Instant publishedAt, Long id, Limit limit);
```

오류 원문(예외가 감싸진 순서대로).

```
jakarta.servlet.ServletException: Request processing failed: org.springframework.dao.InvalidDataAccessResourceUsageException: JDBC exception executing SQL [ERROR: could not determine data type of parameter $3] [select p1_0.id,p1_0.content,p1_0.created_at,p1_0.edited_at,p1_0.published_at,p1_0.slug,p1_0.status,p1_0.summary,p1_0.title,p1_0.updated_at from post p1_0 where p1_0.status='PUBLISHED' and (? is null or exists(select 1 from post_tag t1_0 join tag t1_1 on t1_1.id=t1_0.tag_id where t1_1.name=? and p1_0.id=t1_0.post_id)) and (? is null or p1_0.published_at<? or (p1_0.published_at=? and p1_0.id<?)) order by p1_0.published_at desc,p1_0.id desc fetch first ? rows only]
Caused by: org.hibernate.exception.SQLGrammarException: JDBC exception executing SQL [ERROR: could not determine data type of parameter $3] ...
Caused by: org.postgresql.util.PSQLException: ERROR: could not determine data type of parameter $3
```

`$3`은 세 번째 물음표, 곧 `(? is null or p1_0.published_at<? ...`의 첫 물음표다. `:publishedAt is null` 자리다.

눈에 띈 점: 같은 모양인 첫 번째 물음표(`:tagName is null`)는 문제가 되지 않았다. 글자(`String`)는 괜찮고 시각(`Instant`)만 걸렸다.

## 시도한 것

1. `:publishedAt is null` 그대로: 위 오류로 실패.
2. `cast(:publishedAt as Instant) is null`: 테스트 33개 모두 통과.

`:id is null`로 바꿔 검사하는 방법, 조회를 "처음부터"와 "커서 다음부터" 두 메서드로 나누는 방법도 떠올렸지만 해 보지 않았다. 2번이 바로 통과했고, 조회문 하나로 유지하고 싶었다.

## 원인

직접 확인한 것.

- PostgreSQL은 물음표 자리의 타입을 주변에서 알아낸다. `published_at < ?`는 옆의 칸을 보고 시각이라고 안다. `? is null`은 옆에 아무것도 없어서 알아낼 방법이 없다. psql에서 그대로 재현된다.

```
konglog=# prepare a as select 1 where $1 is null;
ERROR:  could not determine data type of parameter $1
```

- 타입을 알려 주면 된다. 둘 다 오류 없이 실행됐다.

```
prepare c as select 1 where cast($1 as timestamptz) is null;
prepare d(text) as select 1 where $1 is null;
```

확인하지 않은 것.

- 왜 글자는 괜찮고 시각만 걸렸는지. 값을 넘기는 쪽(JDBC 드라이버)이 값 없는 글자는 "글자 타입"이라고 알려 주고, 값 없는 시각은 타입을 알려 주지 않기 때문이라고 짐작하지만 확인하지 않았다.
- 시각에 값이 있을 때도 같은 오류가 나는지. 실패한 테스트는 첫 요청(커서 없음)에서 이미 멈춰서, 값이 있는 경우는 따로 보지 않았다.

## 해결

`is null`을 검사하는 자리에만 `cast`를 붙였다(커밋 1752391). 인자 이름도 무엇의 발행일인지 드러나게 바꿨다.

```java
@Query("""
        select p from Post p
        where p.status = my.konglog.post.PostStatus.PUBLISHED
          and (:tagName is null or exists (select 1 from p.tags t where t.name = :tagName))
          and (cast(:afterPublishedAt as Instant) is null
               or p.publishedAt < :afterPublishedAt
               or (p.publishedAt = :afterPublishedAt and p.id < :afterId))
        order by p.publishedAt desc, p.id desc
        """)
List<Post> findPublished(String tagName, Instant afterPublishedAt, Long afterId, Limit limit);
```

Hibernate가 만든 SQL에서 그 자리는 이렇게 바뀌었다.

```
고치기 전: (? is null or p1_0.published_at<? or ...
고친 뒤:   (cast(? as timestamp(6) with time zone) is null or p1_0.published_at<? or ...
```

확인: `./gradlew test` 33개 통과. 서버를 띄워 로컬의 테스트용 글(게시 24개, 초안 2개)로 첫 요청, 커서로 이어 받기, 태그 필터를 불러 봤고 결과가 고치기 전과 같았다.

## 배운 점

- `(:값 is null or 조건)`은 "값이 있을 때만 거는 조건"을 `@Query` 하나로 쓰는 방법이다. 값이 없으면 앞쪽이 참이라 조건 전체가 통과한다.
- PostgreSQL은 물음표 자리의 타입을 반드시 알아야 한다. 비교할 칸이 옆에 없는 자리(`? is null`)는 `cast`로 알려 줘야 할 수 있다.
- 조회문을 코드로 조립하면 오타가 그 조회를 실행할 때 드러나고, `@Query`는 서버가 뜰 때 드러난다. 다만 이번 오류는 값을 넣어 실행해야만 드러나는 종류라, 어느 쪽이든 테스트가 잡아 줘야 했다.
- 방식을 바꿀 때 API 테스트가 그대로 있어서 15개 실패로 바로 알 수 있었다. 테스트는 조회를 어떻게 썼는지가 아니라 API의 결과를 보기 때문에 한 줄도 고치지 않았다.

## 글로 쓸 때 강조할 것

- 조립하는 코드와 `@Query`를 나란히 놓고, 어느 쪽이 읽기 쉬운지 보여 주기.
- 오류 메시지의 `$3`이 조회문의 어느 자리인지 세어 보는 과정.
- psql 한 줄로 재현되는 가장 작은 예(`prepare a as select 1 where $1 is null;`).
- 조건이 더 늘어나면 `@Query`도 길어진다는 한계.
