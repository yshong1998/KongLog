---
title: 글 목록의 "더 보기"를 페이지 번호가 아니라 커서로 만든 이유
date: 2026-11-22
stage: 1
type: 결정
tags: [Spring, JPA, 페이지네이션]
status: 메모
related: 이슈 #8, 풀 리퀘스트 #33, 커밋 7bfca02, backend/src/main/java/my/konglog/post/PostCursor.java
---

## 한 줄 요약

글 목록의 "더 보기"는 "몇 개 건너뛰고 몇 개"가 아니라 "이 글 다음부터 몇 개"로 읽게 만들었다. 기준은 마지막 글의 발행일과 id이고, 이 둘을 글자 하나로 묶은 것이 커서다.

## 상황

이슈 #8에서 `GET /api/posts`를 만들었다. 조건은 게시 글만, 발행일 최신순, 20개씩 "더 보기", 태그 하나로 거르기, 전체 글 수 함께 내려주기였다. 이슈에 "커서 방식"이라고 적혀 있었고, 커서 페이지네이션은 처음 만들어 봤다.

## 선택지

### 1. 페이지 번호 (`Pageable`)

"최신순으로 20개 건너뛰고 20개"를 DB에 시킨다.

- 장점: Spring Data가 `Pageable`과 `Page`를 제공해서 직접 쓸 코드가 거의 없다. "7쪽으로 바로 가기"가 된다.
- 단점: 1쪽을 본 뒤 새 글이 게시되면 글이 한 칸씩 밀려, 1쪽의 마지막 글이 2쪽의 첫 글로 다시 나온다. DB는 건너뛸 글도 읽은 뒤 버리므로 뒤쪽일수록 느리다.

### 2. 커서를 직접 만들기

마지막으로 내려준 글을 기준으로 "발행일이 이 글보다 앞선 글 20개"를 시킨다.

- 장점: 읽는 사이에 새 글이 게시돼도 밀리거나 겹치지 않는다. 색인에서 기준 위치로 바로 가서 필요한 만큼만 읽는다.
- 단점: 원하는 쪽으로 바로 갈 수 없다. 커서를 글자로 바꾸고 다시 읽는 코드, "다음 글이 있는지" 판단하는 코드를 직접 써야 한다.

### 3. Spring Data의 Scroll API (`ScrollPosition`, `Window`)

커서 방식을 Spring Data가 제공하는 도구로 쓰는 방법.

- 장점: "이 위치 다음부터" 조건을 Spring이 만들어 준다.
- 단점: 위치를 API로 주고받을 글자로 바꾸는 코드는 여전히 직접 써야 한다.
- 이 프로젝트에서 실행해 보지 않았다. 코드가 얼마나 줄어드는지는 확인하지 않았다.

## 고른 것과 이유

2번을 골랐다. 화면이 쪽 번호가 아니라 "더 보기" 버튼이라 앞에서부터 이어 받기만 하면 된다.

### 커서에 발행일과 id를 함께 넣었다

발행일만 쓰면 발행일이 같은 글에서 문제가 생긴다. 테스트용 글의 `react-state-not-updating`과 `react-index-as-key`는 발행일이 초까지 같다. 둘 사이에서 끊기면 "발행일이 더 앞선 글"이라는 조건으로는 남은 한 글이 빠진다. 그래서 발행일이 같으면 id로 순서를 정하고, 조건도 둘을 함께 본다.

```
order by p.publishedAt desc, p.id desc
```

```
p.publishedAt < :afterPublishedAt
or (p.publishedAt = :afterPublishedAt and p.id < :afterId)
```

정렬 기준과 커서에 든 값이 똑같아야 한다. 테이블을 만들 때(V1) 같은 순서의 색인을 이미 만들어 두었다.

```sql
create index idx_post_published on post (published_at desc, id desc) where status = 'PUBLISHED';
```

### 커서를 글자 하나로 바꿨다

발행일과 id를 `2026-11-04T00:00:00Z,3`처럼 이어 붙인 뒤 Base64로 바꾼다. 결과는 `MjAyNi0xMS0wNFQwMDowMDowMFosMw` 같은 글자다.

```java
public String encode() {
    String raw = publishedAt + "," + id;
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
}
```

- 주소에 그대로 넣을 수 있다. 발행일에 든 `:`을 따로 처리하지 않아도 된다.
- 프론트는 커서의 뜻을 몰라도 된다. 받은 그대로 돌려주기만 하므로, 나중에 커서 내용을 바꿔도 프론트 코드는 그대로다.
- Base64는 암호가 아니다. 누구나 풀어 볼 수 있어서 숨겨야 할 값은 넣지 않았다.

커서는 바깥에서 들어오는 값이라 아무 글자나 올 수 있다. 읽을 때 형식이 맞지 않으면 `IllegalArgumentException`을 던지고, 컨트롤러가 400으로 바꾼다. 걸러 주지 않으면 500이 나간다.

### 다음 글이 있는지는 하나 더 읽어서 안다

20개를 달라고 하면 21개를 읽는다. 21개가 읽히면 다음 글이 있다는 뜻이고, 20개만 내려준 뒤 20번째 글로 커서를 만든다. 20개 이하로 읽히면 `nextCursor`는 `null`이다. 다음 글이 있는지 알려고 조회를 한 번 더 하지 않아도 된다.

```java
List<Post> found = postRepository.findPublished(tagName, afterPublishedAt, afterId, Limit.of(size + 1));
boolean hasNext = found.size() > size;
List<Post> posts = hasNext ? found.subList(0, size) : found;
```

글이 딱 20개일 때 `nextCursor`가 `null`인지는 테스트(`글이_딱_20개면_다음_커서가_없다`)로 확인했다.

### 태그는 조회 한 번으로 따로 읽는다

글의 태그는 필요할 때 읽는 방식이라, 그대로 두면 글 20개를 응답으로 바꿀 때 태그 조회가 20번 더 나간다. 글을 읽는 조회에 `join fetch`로 태그를 붙이면 될 것 같지만, 개수 제한과 함께 쓸 수 없다. 태그가 2개인 글은 결과가 2줄이 되어 DB가 "글 20개"를 셀 수 없기 때문이다. 그래서 글 20개를 먼저 읽고, 그 20개만 태그와 함께 한 번 더 읽는다.

```java
@Query("select p from Post p left join fetch p.tags where p in :posts")
List<Post> findWithTags(List<Post> posts);
```

결과는 받지 않는다. 같은 트랜잭션 안에서는 같은 글이 같은 객체라서, 먼저 읽은 글에 태그가 채워진다. 서버를 띄워 조회문을 찍어 보니 요청 한 번에 조회가 세 번 나갔다: 글 목록, 태그, 전체 수.

## 감수한 것

- 원하는 쪽으로 바로 갈 수 없다. 쪽 번호가 필요한 화면(관리자 글 목록 등)이 생기면 그쪽은 `Pageable`을 본다.
- 전체 글 수는 커서와 상관없는 값이라 요청마다 따로 센다.
- 이어 받을 때는 `tag`와 `size`를 처음과 같게 넣어야 한다. 커서에는 위치만 들어 있고 조건은 들어 있지 않다.
- 조회가 실제로 색인을 쓰는지는 실행 계획으로 확인하지 않았다. 글이 26개뿐인 로컬 DB로는 의미 있는 확인이 어렵다.
- 6단계에서 조회순 정렬이 생기면 정렬 기준이 바뀌므로 커서에 담는 값도 바뀐다. 그때 Scroll API를 다시 본다.

## 배운 점

- 커서 방식의 핵심은 "정렬 기준과 똑같은 값으로 위치를 기억한다"는 것이다. 정렬 기준에 겹칠 수 있는 값(발행일)이 있으면 겹치지 않는 값(id)을 하나 더 붙인다.
- 하나 더 읽어 보는 것만으로 다음 글이 있는지 알 수 있다.
- 개수 제한이 있는 조회에는 목록(태그)을 `join fetch`로 붙일 수 없다. 둘로 나눠 읽는다.

## 글로 쓸 때 강조할 것

- 페이지 번호 방식에서 글이 겹치는 장면을 실제 요청 두 번으로 보여 주기.
- 발행일이 같은 두 글에서 id가 없으면 어떻게 빠지는지.
- 커서가 암호가 아니라는 점과, 그래도 뜻을 숨기는 이유.
