---
title: 목록에 안 쓰는 본문을 DB에서 두 번씩 읽고 있었다
date: 2026-11-25
stage: 1
type: 결정
tags: [JPA, Hibernate, Spring Data JPA, 성능]
status: 메모
related: 이슈 #37, backend/src/main/java/my/konglog/post/PostRepository.java, PostQueryService.java, PostSummaryRow.java, PostTagName.java, backend/src/test/java/my/konglog/post/PostListQueryTest.java
---

## 한 줄 요약

엔티티로 고르면 JPA는 칸을 전부 읽는다. 목록처럼 일부 칸만 필요한 조회는 엔티티 대신 필요한 칸만 담는 record로 받았다.

## 상황

글 목록 API(`GET /api/posts`) 응답에는 본문이 없다. 그런데 요청마다 글 20개의 본문(`content`)을 DB에서 두 번 읽고 있었다. PM 세션이 1단계 백엔드를 검토하다 찾았다(이슈 #37).

고치기 전 코드. 목록을 엔티티로 읽고, 태그는 `join fetch`로 한 번 더 읽어 같은 객체에 채웠다.

```java
// PostRepository
@Query("""
        select p from Post p
        where p.status = my.konglog.post.PostStatus.PUBLISHED
          and ...
        order by p.publishedAt desc, p.id desc
        """)
List<Post> findPublished(String tagName, Instant afterPublishedAt, Long afterId, Limit limit);

@Query("select p from Post p left join fetch p.tags where p in :posts")
List<Post> findWithTags(List<Post> posts);

// PostQueryService
List<Post> found = postRepository.findPublished(tagName, afterPublishedAt, afterId, Limit.of(size + 1));
...
// 결과는 받지 않는다. 같은 트랜잭션 안에서는 같은 글이 같은 객체라서, 위에서 읽은 글에 태그가 채워진다.
if (!posts.isEmpty()) {
    postRepository.findWithTags(posts);
}
```

그때 나간 SQL(이슈 #37에 PM이 붙인 것과, 고치기 전 코드로 새 테스트를 돌렸을 때 실패 메시지에 나온 것).

```text
select p1_0.id,p1_0.content,p1_0.created_at,p1_0.edited_at,p1_0.published_at,p1_0.slug,p1_0.status,p1_0.summary,p1_0.title,p1_0.up... from post p1_0 where p1_0.status='PUBLISHED' and ... fetch first ? rows only
select p1_0.id,p1_0.content,...,t1_1.id,t1_1.name,... from post p1_0 left join post_tag ... where p1_0.id in (?,?,?,?,?)
select count(p1_0.id) from post p1_0 where p1_0.status='PUBLISHED' and ...
```

두 번째 조회는 태그만 채우려던 것인데, `select p ... join fetch`라서 글의 칸도 전부(본문 포함) 다시 읽었다. 처음에는 이 부분을 몰랐다.

## 선택지

1. **필요한 칸만 담는 record로 받기 (`select new ...`)**
   - 장점: 의존성이 늘지 않는다. 엔티티는 늘 완전한 글로 남는다. 읽는 칸이 SQL에 그대로 보인다.
   - 단점: 엔티티와 따로 노는 record가 생긴다(`PostSummaryRow`, `PostTagName`). `select new`로는 `List` 같은 컬렉션을 채울 수 없어서 태그는 따로 읽어 붙여야 한다.
2. **`select new Post(...)`로 본문이 null인 엔티티 만들기**
   - 장점: 서비스 코드가 `Post`를 그대로 쓴다.
   - 단점: `Post`에 생성자를 열어야 한다. "엔티티는 `draft()` 같은 정해 둔 메서드로만 만든다"는 규칙과 부딪힌다. 겉보기엔 진짜인데 본문이 빈 글이 생기고, 저장 쪽에 섞이면 본문을 null로 덮을 수 있다. JPA가 관리하는 객체가 아니라 태그도 채워지지 않는다.
3. **`content`에 `@Basic(fetch = FetchType.LAZY)` 붙이기**
   - 장점: `select p`를 그대로 두고도 본문을 `getContent()`할 때만 읽는다. 원래 바라던 모양에 가장 가깝다.
   - 단점: Hibernate 바이트코드 강화(빌드 때 컴파일된 클래스를 고쳐 쓰는 기능)가 있어야 동작한다. 강화가 없으면 이 설정은 무시되고 본문을 그대로 읽는다고 알고 있다. **직접 확인하지는 않았다.** 빌드 플러그인이 하나 늘고 빌드 방식이 바뀐다.
4. 엔티티 그래프로 본문만 빼기: 엔티티 그래프는 연관(태그 등)에 쓰는 기능이고, 일반 칸을 빼는 데는 쓸 수 없다고 알고 있다. 이것도 직접 확인하지는 않았다.

## 고른 것과 이유

1번. 의존성 없이 끝나고, 엔티티를 반쪽짜리로 만들지 않는다. 목록 조회는 읽기 전용이라 엔티티가 필요 없다.

리뷰에서 "Post로 받되 본문만 select하지 않고, DTO에서 빼면 되지 않나"라는 질문이 나왔다. 정리한 답: 응답 DTO(`PostSummaryResponse`)에는 원래 본문이 없었다. 문제는 응답이 아니라 DB에서 읽는 것이었다. 그리고 엔티티로 고르면 칸을 빼는 방법이 위 2번, 3번뿐이다.

고친 뒤 코드.

```java
// 목록에 필요한 칸만
public record PostSummaryRow(Long id, String title, String slug, String summary, Instant publishedAt) {}
public record PostTagName(Long postId, String tagName) {}

@Query("""
        select new my.konglog.post.PostSummaryRow(p.id, p.title, p.slug, p.summary, p.publishedAt)
        from Post p
        where ...
        order by p.publishedAt desc, p.id desc
        """)
List<PostSummaryRow> findPublished(String tagName, Instant afterPublishedAt, Long afterId, Limit limit);

@Query("""
        select new my.konglog.post.PostTagName(p.id, t.name)
        from Post p join p.tags t
        where p.id in :postIds
        """)
List<PostTagName> findTagNames(List<Long> postIds);

// PostQueryService: 태그를 글 id로 묶어 붙인다
Map<Long, List<String>> tagNamesByPostId = rows.isEmpty()
        ? Map.of()
        : postRepository.findTagNames(rows.stream().map(PostSummaryRow::id).toList()).stream()
                .collect(groupingBy(PostTagName::postId, mapping(PostTagName::tagName, toList())));
```

고친 뒤 SQL(서버를 `--spring.jpa.show-sql=true`로 띄워 `GET /api/posts?size=5`를 불렀을 때).

```text
select p1_0.id,p1_0.title,p1_0.slug,p1_0.summary,p1_0.published_at from post p1_0 where p1_0.status='PUBLISHED' and (? is null or exists(...)) and (cast(? as timestamp(6) with time z...
select p1_0.id,t1_1.name from post p1_0 join post_tag t1_0 on p1_0.id=t1_0.post_id join tag t1_1 on t1_1.id=t1_0.tag_id where p1_0.id in (?,?,?,?,?)
select count(p1_0.id) from post p1_0 where p1_0.status='PUBLISHED' and (? is null or exists(...))
```

요청당 조회는 전처럼 3번이다. `size=20`, `size=5`, `tag=Spring` 모두 3번이었고 `content`는 어디에도 없었다.

## 감수한 것

- 목록용 record가 둘 늘었다. `Post`에 칸이 생기면 목록에 필요한지 따로 챙겨야 한다.
- 태그 이름 정렬을 DB가 아니라 Java(`sorted()`)에서 한다. 전에도 Java에서 했으니 순서는 같다.
- 글 상세(`GET /api/posts/{슬러그}`)는 본문이 필요하니 엔티티 조회를 그대로 둔다.
- 다시 볼 조건: 엔티티 조회인데 큰 칸을 빼야 하는 곳이 여러 군데 생기면 3번(바이트코드 강화)을 다시 검토한다.

## 확인한 방법

SQL을 눈으로 보는 것만으로는 다음에 깨져도 모른다. 그래서 테스트로 남겼다. Hibernate의 `StatementInspector`(DB로 보내기 전에 SQL을 받아 보는 기능)로 SQL을 모았다.

```java
@SpringBootTest(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=my.konglog.post.SqlRecorder")
```

- `SqlRecorder`는 Hibernate가 직접 만들어서 스프링 빈으로 주입할 수 없다. 그래서 기록을 static 리스트에 둔다.
- 테스트 두 개: 목록 SQL에 `content`가 없다, 글이 5개일 때와 20개일 때 조회 수가 똑같이 3번이다.
- 고치기 전 코드로 돌리면 본문 테스트가 실패하는 것을 확인했다(위 "상황"의 첫 SQL이 그 실패 메시지다). 조회 수 테스트는 전에도 3번이라 통과했다.

## 배운 점

- `join fetch`는 연관만 더 읽는 게 아니라, 앞에 고른 엔티티의 칸도 전부 다시 읽는다.
- 응답에 안 나가는 것과 DB에서 안 읽는 것은 다른 문제다.
- 동작이 같다는 것은 기존 테스트를 고치지 않고 통과시키는 것으로 보였다.

## 글로 쓸 때 강조할 것

- "DTO에서 빼면 되지 않나?"라는 질문에서 시작하면 읽는 사람이 따라오기 쉽다.
- 고치기 전과 후의 SQL을 나란히 보여 준다.
