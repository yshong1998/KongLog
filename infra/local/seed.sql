-- 로컬 테스트용 글과 태그. 화면을 만들며 확인할 때 쓴다.
-- 저장소 루트에서 실행한다. 테이블이 있어야 하므로 백엔드를 한 번 실행한 뒤에 쓴다.
--   docker compose -f infra/local/compose.yaml exec -T db psql -U konglog -d konglog < infra/local/seed.sql
--
-- 로컬 전용이다. Flyway 파일이 아니라서 운영 DB에는 들어가지 않는다.
-- 여러 번 실행해도 결과가 같다. 슬러그가 같은 글은 이 파일의 내용으로 덮어쓰고,
-- 이 파일에 없는 글(직접 써 넣은 글)은 건드리지 않는다.
--
-- 들어가는 것 (제목, 태그, 날짜는 화면 시안과 맞췄다)
--   게시 글 24개: 글 목록 첫 화면 20개와 "더 보기" 4개
--   초안 2개: 어떤 화면에도 나오면 안 된다. 슬러그가 draft-로 시작한다
--   태그 13개: 그중 Kubernetes는 초안에만 붙어 있어 태그 목록에 나오면 안 된다
--
-- 일부러 넣은 경우
--   docker-localhost               소제목, 코드 블록, 표, 이미지, 링크가 모두 있고 수정일이 있다
--   react-state-not-updating,
--   react-index-as-key             발행일이 초까지 같다. 순서와 "더 보기"가 겹치거나 빠지지 않는지 본다
--   timestamp-vs-timestamptz       한국 시간 10월 7일 0시 30분에 게시했다. UTC로는 10월 6일이다
--   postgres-index-not-used        제목과 요약이 길다
--   why-i-built-this-blog          태그가 없다
--   github-actions-run-tests-on-pr 태그 이름에 띄어쓰기가 있다 (GitHub Actions)

\set ON_ERROR_STOP on
\set QUIET on
set client_encoding = 'UTF8';

begin;

-- 넣을 글. content가 비어 있으면 아래에서 요약으로 짧은 본문을 만든다.
create temp table seed_post (
    slug         text primary key,
    title        text        not null,
    summary      text        not null,
    status       text        not null,
    published_at timestamptz,
    edited_at    timestamptz,
    content      text
) on commit drop;

-- 글에 붙일 태그. 없는 슬러그를 적으면 여기서 오류가 난다.
create temp table seed_post_tag (
    slug     text not null references seed_post (slug),
    tag_name text not null,
    primary key (slug, tag_name)
) on commit drop;

insert into seed_post (slug, title, summary, status, published_at, edited_at, content)
values
    ('docker-localhost',
     'Docker 컨테이너에서 localhost가 안 잡혔던 이유',
     '컨테이너 안에서 localhost는 내 컴퓨터가 아니라 컨테이너 자신이었어요. 원인을 찾고 고친 과정을 적었어요.',
     'PUBLISHED', '2026-11-18T00:00:00Z', '2026-11-19T02:00:00Z',
     $md$내 컴퓨터에서 잘 돌던 API 서버를 Docker 컨테이너로 옮겼더니 DB에 연결하지 못했어요. 원인은 한 단어, `localhost`였어요.

## 증상

내 컴퓨터에 띄운 MySQL에 `localhost:3306`으로 잘 붙던 서버가, 컨테이너 안에서는 시작하자마자 이 오류를 내고 죽었어요.

```text
java.net.ConnectException: Connection refused
```

처음에는 DB가 꺼진 줄 알았어요. 그런데 내 컴퓨터의 터미널에서는 같은 주소로 잘 접속됐어요.

```bash
mysql -h localhost -P 3306 -u app -p
```

## 원인

컨테이너는 자기만의 네트워크 공간을 가져요. 그래서 컨테이너 안에서 `localhost`는 내 컴퓨터가 아니라 **그 컨테이너 자신**을 가리켜요. API 컨테이너 안에는 MySQL이 없으니 연결이 거절된 거예요.

![내 컴퓨터와 컨테이너가 저마다 localhost를 가진 모습](https://placehold.co/960x540.png?text=localhost)

> 같은 이름이라도 어디에서 부르느냐에 따라 가리키는 곳이 달라요.

## 해결

### 내 컴퓨터의 DB에 붙을 때

Docker Desktop에서는 `host.docker.internal`이라는 이름이 내 컴퓨터를 가리켜요. DB 호스트 환경 변수만 바꿨어요.

```yaml
services:
  api:
    environment:
      DB_HOST: host.docker.internal
```

Linux에서는 이 이름이 기본으로 잡히지 않아서 한 줄을 더 넣어야 해요.

```yaml
services:
  api:
    extra_hosts:
      - "host.docker.internal:host-gateway"
```

### DB도 컨테이너로 띄울 때

DB도 컨테이너로 띄운다면 더 간단해요. 같은 Compose 파일 안에서는 서비스 이름을 호스트로 쓰면 돼요.

```yaml
services:
  api:
    environment:
      DB_HOST: db
  db:
    image: mysql:8.4
```

코드는 환경 변수를 읽기만 해요. 아래 줄은 일부러 길게 두었어요. 좁은 화면에서 코드 블록 안에서만 옆으로 밀리는지 볼 수 있어요.

```java
String url = "jdbc:mysql://" + System.getenv("DB_HOST") + ":3306/app?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
```

### 상황별로 쓰는 호스트 이름

| API가 도는 곳 | DB가 도는 곳 | DB 호스트 |
| --- | --- | --- |
| 내 컴퓨터 | 내 컴퓨터 | `localhost` |
| 컨테이너 | 내 컴퓨터 | `host.docker.internal` |
| 컨테이너 | 같은 Compose의 컨테이너 | 서비스 이름 (예: `db`) |

더 자세한 설명은 [Docker 공식 문서의 Compose 네트워킹](https://docs.docker.com/compose/how-tos/networking/)에 있어요.

## 정리

- 컨테이너 안의 localhost는 컨테이너 자신이에요.
- 내 컴퓨터에 붙으려면 host.docker.internal을 써요.
- 컨테이너끼리는 서비스 이름으로 붙어요.$md$),

    ('slow-post-list-n-plus-one',
     '목록 API가 느려서 로그를 열어 봤더니',
     '글 목록 하나를 불러오는데 쿼리가 글 수만큼 더 나가고 있었어요. N+1 문제를 처음 직접 겪은 기록이에요.',
     'PUBLISHED', '2026-11-12T00:00:00Z', null, null),

    ('git-rebase-notes',
     'git rebase가 무서워서 정리해 본 것들',
     'merge만 쓰다가 처음 rebase를 해 봤어요. 충돌이 났을 때 되돌리는 방법부터 적어 뒀어요.',
     'PUBLISHED', '2026-11-05T00:00:00Z', null, null),

    ('useeffect-runs-twice',
     'useEffect가 두 번 실행돼서 당황한 날',
     '개발 환경에서만 요청이 두 번씩 나갔어요. 버그가 아니라 의도된 동작이라는 걸 알게 된 과정이에요.',
     'PUBLISHED', '2026-10-29T00:00:00Z', null, null),

    ('cors-is-a-server-problem',
     'CORS 오류는 프론트에서 고치는 게 아니었다',
     '브라우저 콘솔만 보고 프론트 코드를 한참 고쳤어요. 서버 응답 헤더를 봐야 한다는 걸 정리했어요.',
     'PUBLISHED', '2026-10-21T00:00:00Z', null, null),

    ('transactional-self-invocation',
     '@Transactional을 붙였는데 롤백이 안 됐다',
     '같은 클래스 안에서 부른 메서드에는 트랜잭션이 걸리지 않았어요. 프록시가 무엇인지 처음 찾아봤어요.',
     'PUBLISHED', '2026-10-19T00:00:00Z', null, null),

    ('react-state-not-updating',
     '상태를 바꿨는데 화면이 그대로였던 이유',
     '배열에 push만 하고 다시 넣었더니 화면이 바뀌지 않았어요. 새 배열을 만들어야 하는 이유를 적었어요.',
     'PUBLISHED', '2026-10-16T00:00:00Z', null, null),

    ('react-index-as-key',
     'key에 index를 썼다가 입력값이 뒤섞였다',
     '목록 가운데 항목을 지웠더니 아래 칸의 입력값이 위로 올라왔어요. key가 하는 일을 정리했어요.',
     'PUBLISHED', '2026-10-16T00:00:00Z', null, null),

    ('dockerfile-cache-busting',
     'Dockerfile 캐시가 매번 깨졌던 이유',
     '코드 한 줄만 고쳐도 의존성을 처음부터 다시 받았어요. COPY의 순서를 바꿔 해결했어요.',
     'PUBLISHED', '2026-10-14T00:00:00Z', null, null),

    ('http-400-vs-404',
     '400과 404 중 무엇을 내려야 할지 헷갈렸다',
     '없는 글을 요청했을 때와 잘못된 값을 보냈을 때를 같은 오류로 내리고 있었어요. 기준을 정해 봤어요.',
     'PUBLISHED', '2026-10-12T00:00:00Z', null, null),

    ('lazy-initialization-exception',
     'LazyInitializationException을 처음 만난 날',
     '컨트롤러에서 태그를 꺼내려는 순간 오류가 났어요. 트랜잭션이 끝난 뒤에는 읽을 수 없다는 걸 배웠어요.',
     'PUBLISHED', '2026-10-09T00:00:00Z', null, null),

    ('timestamp-vs-timestamptz',
     'timestamp와 timestamptz, 9시간이 어긋난 이유',
     '저장한 시각이 화면에서 9시간 앞서 보였어요. 시간대를 어디에서 붙이는지 따라가 봤어요.',
     'PUBLISHED', '2026-10-06T15:30:00Z', null, null),

    ('git-undo-commit',
     '커밋을 잘못 올렸을 때 되돌리는 세 가지 방법',
     'reset, revert, amend를 언제 쓰는지 헷갈려서 상황별로 나눠 적었어요.',
     'PUBLISHED', '2026-10-04T00:00:00Z', null, null),

    ('react-function-recreated',
     '렌더링할 때마다 함수가 새로 만들어진다는 것',
     '자식 컴포넌트가 이유 없이 다시 그려졌어요. 넘겨준 함수가 매번 새것이었다는 걸 알게 됐어요.',
     'PUBLISHED', '2026-10-02T00:00:00Z', null, null),

    ('compose-dev-db',
     'Compose로 개발 DB를 띄우며 배운 것',
     '내 컴퓨터에 DB를 직접 깔지 않고 컨테이너로 띄웠어요. 데이터를 남기는 방법과 지우는 방법을 적었어요.',
     'PUBLISHED', '2026-09-30T00:00:00Z', null, null),

    ('spring-two-beans',
     '빈이 두 개라 주입에 실패했을 때',
     '같은 타입의 빈이 둘이라 서버가 뜨지 않았어요. 오류 메시지를 읽는 법부터 정리했어요.',
     'PUBLISHED', '2026-09-27T00:00:00Z', null, null),

    ('typescript-any-to-unknown',
     'any를 지우다가 알게 된 unknown',
     'any를 unknown으로 바꿨더니 오류가 쏟아졌어요. 그 오류가 원래 잡아야 했던 것들이었어요.',
     'PUBLISHED', '2026-09-24T00:00:00Z', null, null),

    ('jpa-dirty-checking',
     '변경 감지를 모르고 save를 또 불렀다',
     '값만 바꿔도 저장된다는 걸 모르고 매번 save를 불렀어요. 영속성 컨텍스트가 하는 일을 적었어요.',
     'PUBLISHED', '2026-09-21T00:00:00Z', null, null),

    ('use-state-in-server-component',
     '서버 컴포넌트에서 useState를 쓰려다 만난 오류',
     '파일 맨 위에 한 줄이 빠져 있었어요. 서버 컴포넌트와 클라이언트 컴포넌트를 나누는 기준을 정리했어요.',
     'PUBLISHED', '2026-09-18T00:00:00Z', null, null),

    ('postgres-index-not-used',
     '색인을 걸었는데도 느렸던 쿼리를 EXPLAIN ANALYZE로 한 줄씩 읽어 보며 원인을 좁혀 간 기록',
     '색인을 만들었는데도 쿼리가 빨라지지 않았어요. 실행 계획을 한 줄씩 읽어 보니 조건에 함수를 씌운 탓에 색인을 타지 못하고 있었어요. 실행 계획에서 무엇을 먼저 봐야 하는지, 색인을 타는 조건과 타지 못하는 조건이 어떻게 다른지, 고치기 전과 후에 걸린 시간이 얼마나 달라졌는지를 순서대로 적었어요. 요약이 두 줄을 넘을 때 어떻게 보이는지 확인하려고 일부러 길게 쓴 글이에요.',
     'PUBLISHED', '2026-09-15T00:00:00Z', null, null),

    ('docker-image-size',
     '이미지 용량을 줄이려고 해 본 세 가지',
     '1GB가 넘던 이미지를 줄여 봤어요. 베이스 이미지, 여러 단계 빌드, 제외할 파일 목록 순서로 해 봤어요.',
     'PUBLISHED', '2026-09-12T00:00:00Z', null, null),

    ('valid-annotation-ignored',
     '@Valid가 조용히 무시된 이유',
     '빈 값을 보내도 그대로 저장됐어요. 의존성 하나가 빠져 있어 검사가 아예 돌지 않고 있었어요.',
     'PUBLISHED', '2026-09-09T00:00:00Z', null, null),

    ('github-actions-run-tests-on-pr',
     '풀 리퀘스트마다 테스트가 자동으로 돌게 만들기',
     '테스트를 깜빡하고 올린 뒤로 자동 검사를 붙였어요. 처음 써 본 워크플로 파일을 한 줄씩 풀어 봤어요.',
     'PUBLISHED', '2026-09-06T00:00:00Z', null, null),

    ('why-i-built-this-blog',
     '블로그를 직접 만들기로 한 이유',
     '이미 있는 서비스를 쓰면 될 일을 굳이 직접 만들기로 했어요. 그 이유와 정해 둔 범위를 적었어요.',
     'PUBLISHED', '2026-09-03T00:00:00Z', null, null),

    ('draft-kubernetes-notes',
     'Kubernetes를 배워 보려고 시작한 메모',
     '아직 쓰는 중인 초안이에요. 이 글이 화면에 보이면 초안을 거르지 못한 거예요.',
     'DRAFT', null, null, null),

    ('draft-untitled-idea',
     '제목만 적어 둔 글',
     '아직 쓰는 중인 초안이에요. 이 글이 화면에 보이면 초안을 거르지 못한 거예요.',
     'DRAFT', null, null,
     '나중에 쓸 내용을 적어 둘 자리예요.');

insert into seed_post_tag (slug, tag_name)
values
    ('docker-localhost', 'Docker'),
    ('docker-localhost', '네트워크'),
    ('slow-post-list-n-plus-one', 'JPA'),
    ('slow-post-list-n-plus-one', 'Spring'),
    ('git-rebase-notes', 'Git'),
    ('useeffect-runs-twice', 'React'),
    ('cors-is-a-server-problem', 'HTTP'),
    ('cors-is-a-server-problem', 'Spring'),
    ('transactional-self-invocation', 'Spring'),
    ('react-state-not-updating', 'React'),
    ('react-index-as-key', 'React'),
    ('dockerfile-cache-busting', 'Docker'),
    ('http-400-vs-404', 'HTTP'),
    ('lazy-initialization-exception', 'JPA'),
    ('lazy-initialization-exception', 'Spring'),
    ('timestamp-vs-timestamptz', 'PostgreSQL'),
    ('timestamp-vs-timestamptz', 'Java'),
    ('git-undo-commit', 'Git'),
    ('react-function-recreated', 'React'),
    ('react-function-recreated', 'TypeScript'),
    ('compose-dev-db', 'Docker'),
    ('spring-two-beans', 'Spring'),
    ('typescript-any-to-unknown', 'TypeScript'),
    ('jpa-dirty-checking', 'JPA'),
    ('use-state-in-server-component', 'React'),
    ('use-state-in-server-component', 'Next.js'),
    ('postgres-index-not-used', 'PostgreSQL'),
    ('docker-image-size', 'Docker'),
    ('valid-annotation-ignored', 'Spring'),
    ('github-actions-run-tests-on-pr', 'Git'),
    ('github-actions-run-tests-on-pr', 'GitHub Actions'),
    ('draft-kubernetes-notes', 'Kubernetes'),
    ('draft-kubernetes-notes', 'Docker');

-- 태그: 이미 있는 이름은 그대로 둔다.
insert into tag (name)
select distinct tag_name
from seed_post_tag
on conflict (name) do nothing;

-- 글: 슬러그가 같으면 이 파일의 내용으로 덮어쓴다.
-- 시각은 now()가 아니라 정해 둔 값을 써서 언제 실행해도 같다.
insert into post (title, slug, summary, content, status, published_at, created_at, updated_at, edited_at)
select s.title,
       s.slug,
       s.summary,
       coalesce(s.content, format($md$%s

## 상황

테스트용 글이에요. 목록과 상세 화면을 확인하려고 넣어 둔 본문이라 내용은 짧아요.

## 원인

목차가 생기는지 볼 수 있게 소제목을 세 개 두었어요.

## 정리

- 이 글의 슬러그는 `%s`예요.$md$, s.summary, s.slug)),
       s.status,
       s.published_at,
       coalesce(s.published_at, timestamptz '2026-11-20T00:00:00Z'),
       coalesce(s.edited_at, s.published_at, timestamptz '2026-11-20T00:00:00Z'),
       s.edited_at
from seed_post s
-- 발행일이 같은 글도 매번 같은 순서로 들어가게 한다.
order by s.published_at nulls last, s.slug
on conflict (slug) do update
    set title        = excluded.title,
        summary      = excluded.summary,
        content      = excluded.content,
        status       = excluded.status,
        published_at = excluded.published_at,
        created_at   = excluded.created_at,
        updated_at   = excluded.updated_at,
        edited_at    = excluded.edited_at;

-- 글과 태그의 연결: 이 파일의 글에 붙은 것만 지우고 다시 넣는다.
delete from post_tag
where post_id in (select p.id from post p join seed_post s on s.slug = p.slug);

insert into post_tag (post_id, tag_id)
select p.id, t.id
from seed_post_tag st
join post p on p.slug = st.slug
join tag t on t.name = st.tag_name;

commit;

-- 넣은 뒤의 상태. 이 파일만 넣었다면 DRAFT 2, PUBLISHED 24가 나온다.
select status as "상태", count(*) as "글 수"
from post
group by status
order by status;
