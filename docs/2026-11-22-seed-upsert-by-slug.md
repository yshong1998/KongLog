---
title: 테스트용 데이터를 다 지우고 넣지 않고 슬러그 기준으로 덮어쓴 이유
date: 2026-11-22
stage: 1
type: 결정
tags: [PostgreSQL, 테스트 데이터]
status: 메모
related: 이슈 #5, 풀 리퀘스트 #24, infra/local/seed.sql
---

## 한 줄 요약

로컬 테스트용 글은 테이블을 비우고 다시 넣는 대신, 슬러그가 같은 글만 덮어쓰게(`on conflict (slug) do update`) 만들었다. 로컬 DB에 직접 써 넣은 글이 지워지지 않게 하려는 것이다.

## 상황

화면을 만들며 확인할 테스트용 글을 로컬 DB에 넣는 SQL 파일(`infra/local/seed.sql`)이 필요했다. 이슈 #5의 조건 중 하나가 "여러 번 실행해도 같은 결과가 되게 만들기"였다. 그냥 `insert`만 쓰면 두 번째 실행에서 슬러그가 겹쳐 오류가 난다.

```
constraint uq_post_slug unique (slug)
```

## 선택지

### 1. 다 지우고 넣기

```sql
truncate post, tag, post_tag restart identity;
insert into post (...) values (...);
```

- 장점: 가장 짧다. 실행하고 나면 DB가 파일과 완전히 같다. id도 매번 1부터다.
- 단점: 로컬 DB에 있던 다른 글도 전부 지운다. 3단계에서 에디터가 생기면 로컬에서 직접 글을 써 보게 되는데, 테스트용 글을 다시 넣는 순간 그 글이 사라진다.

### 2. 테스트용 글만 지우고 다시 넣기

```sql
delete from post where slug in ('docker-localhost', ...);
insert into post (...) values (...);
```

- 장점: 직접 쓴 글은 남는다.
- 단점: 실행할 때마다 테스트용 글의 id가 바뀐다. 슬러그 목록을 지우는 곳과 넣는 곳에 두 번 적어야 한다.

### 3. 슬러그가 같으면 덮어쓰기

```sql
insert into post (title, slug, summary, content, status, published_at, created_at, updated_at, edited_at)
select ...
from seed_post s
on conflict (slug) do update
    set title        = excluded.title,
        summary      = excluded.summary,
        content      = excluded.content,
        status       = excluded.status,
        published_at = excluded.published_at,
        created_at   = excluded.created_at,
        updated_at   = excluded.updated_at,
        edited_at    = excluded.edited_at;
```

- 장점: 직접 쓴 글은 남는다. 테스트용 글의 id가 바뀌지 않는다. 슬러그에 이미 중복 금지 제약이 있어서 따로 준비할 것이 없다.
- 단점: 바꿀 칸을 하나씩 적어야 해서 길다. 칸이 늘면 여기도 고쳐야 한다. 파일에서 뺀 글은 DB에 그대로 남는다.

## 고른 것과 이유

3번을 골랐다.

- 로컬 DB는 테스트용 글만 있는 곳이 아니다. 직접 써 본 글이 섞일 수 있고, 테스트용 글을 다시 넣는다고 그것까지 지워지면 안 된다.
- 여러 번 실행해도 id까지 같다. 두 번 실행한 뒤 글과 태그 전체를 id 포함해서 뽑아 비교했고, 차이가 없었다.
- 테스트용 글을 손으로 고쳐 놓아도 다시 실행하면 파일의 내용으로 돌아온다. 제목을 바꿔 둔 뒤 다시 실행해서 확인했다.

넣을 글은 먼저 임시 테이블(`seed_post`, `seed_post_tag`)에 담고, 거기서 `post`, `tag`, `post_tag`로 옮긴다. 슬러그 목록을 한 곳에만 적으려는 것이다.

글과 태그의 연결(`post_tag`)은 덮어쓸 내용이 없는 표라서, 테스트용 글에 붙은 것만 지우고 다시 넣었다.

```sql
delete from post_tag
where post_id in (select p.id from post p join seed_post s on s.slug = p.slug);

insert into post_tag (post_id, tag_id)
select p.id, t.id
from seed_post_tag st
join post p on p.slug = st.slug
join tag t on t.name = st.tag_name;
```

태그는 이름만 있어서 이미 있으면 그대로 둔다.

```sql
insert into tag (name)
select distinct tag_name
from seed_post_tag
on conflict (name) do nothing;
```

전체를 한 트랜잭션(`begin` ~ `commit`)으로 묶고 `\set ON_ERROR_STOP on`을 넣었다. 없는 슬러그에 태그를 붙이게 고친 파일로 실행했을 때 아래 오류로 멈췄고(종료 코드 3), DB는 그대로였다.

```
ERROR:  insert or update on table "seed_post_tag" violates foreign key constraint "seed_post_tag_slug_fkey"
DETAIL:  Key (slug)=(no-such-slug) is not present in table "seed_post".
```

## 감수한 것

- 파일에서 글을 빼도 DB에서는 지워지지 않는다. 깨끗한 상태가 필요하면 DB를 지우고(`docker compose -f infra/local/compose.yaml down -v`) 다시 띄운 뒤 넣어야 한다.
- 테스트용 글과 슬러그가 같은 글을 직접 써 두었다면 덮어써진다.
- 다시 실행할 때마다 id 번호가 글 수만큼 건너뛴다. 덮어쓴 글도 번호표는 한 장씩 뽑고 버리기 때문이다. 세 번 실행하고 글 하나를 직접 넣은 뒤 `post_id_seq`의 `last_value`가 105였다. 글의 id가 바뀌는 것은 아니라서 문제는 없다.
- 다시 검토할 조건: 테스트용 글을 자주 빼고 바꾸게 돼서 "파일과 DB가 완전히 같아야 한다"가 더 중요해지면 1번이나, 테스트용 글에 표시를 해 두고 그것만 지우는 방법을 본다.

## 배운 점

- "여러 번 실행해도 같은 결과"를 만드는 방법은 여러 가지고, 무엇을 지켜야 하는지(다른 데이터, id)에 따라 답이 달라진다.
- `on conflict`는 중복 금지 제약이 있는 칸을 기준으로 쓴다. 슬러그와 태그 이름에 제약을 미리 걸어 둔 덕에 바로 쓸 수 있었다.
- 덮어쓰는 경우에도 id 번호는 소비된다.

## 글로 쓸 때 강조할 것

- 세 가지 방법을 같은 데이터로 나란히 보여 주기: 실행 전후에 직접 쓴 글과 id가 어떻게 되는지.
- "다 지우고 넣기"가 틀린 방법은 아니라는 점. 테스트 전용 DB라면 그쪽이 더 낫다.
