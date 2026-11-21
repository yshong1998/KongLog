-- 글과 태그. 글 하나에 태그 여러 개, 태그 하나에 글 여러 개가 붙는다.

create table post (
    id           bigint generated always as identity primary key,
    title        varchar(200) not null,
    -- 주소에 쓰는 영문 이름. /posts/{slug}
    slug         varchar(200) not null,
    summary      varchar(500) not null,
    -- 마크다운 원문
    body         text         not null,
    -- DRAFT(초안) 또는 PUBLISHED(게시)
    status       varchar(20)  not null,
    published_at timestamptz,
    created_at   timestamptz  not null,
    -- 무엇이든 바뀌면 갱신한다. 관리자 목록의 정렬 기준
    updated_at   timestamptz  not null,
    -- 게시한 뒤 본문을 고쳤을 때만 채운다. 독자에게 보이는 "수정일"
    edited_at    timestamptz,

    constraint uq_post_slug unique (slug),
    constraint ck_post_status check (status in ('DRAFT', 'PUBLISHED')),
    -- 게시된 글에는 발행일이 반드시 있다
    constraint ck_post_published_at check (status <> 'PUBLISHED' or published_at is not null)
);

-- 독자용 목록은 게시 글만 최신순으로 읽는다
create index ix_post_published on post (published_at desc, id desc) where status = 'PUBLISHED';

create table tag (
    id   bigint generated always as identity primary key,
    name varchar(50) not null,

    constraint uq_tag_name unique (name)
);

create table post_tag (
    post_id bigint not null references post (id) on delete cascade,
    tag_id  bigint not null references tag (id),

    primary key (post_id, tag_id)
);

-- 태그로 글을 찾을 때 쓴다. 기본 키는 post_id가 앞이라 이 방향을 돕지 못한다
create index ix_post_tag_tag on post_tag (tag_id);
