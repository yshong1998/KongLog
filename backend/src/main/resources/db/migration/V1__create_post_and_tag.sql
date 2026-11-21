-- 글과 태그. 1단계(글 읽기)의 모든 읽기 API가 이 세 테이블 위에 올라간다.

create table post (
    id           bigint generated always as identity primary key,
    title        varchar(200) not null,
    -- 주소에 쓰는 영문 이름. /posts/{slug}
    slug         varchar(200) not null,
    summary      varchar(500) not null,
    -- 마크다운 원문. 화면에 맞게 바꾸는 일은 프론트엔드가 한다.
    content      text         not null,
    -- DRAFT(초안) 또는 PUBLISHED(게시). 독자에게는 PUBLISHED만 보인다.
    status       varchar(20)  not null,
    -- 발행일. 초안일 때는 비어 있다.
    published_at timestamptz,
    created_at   timestamptz  not null,
    -- 어떤 변경이든 바뀌는 시각. 관리자 화면의 정렬에 쓴다.
    updated_at   timestamptz  not null,
    -- 게시한 뒤 본문을 고친 시각. 독자에게 "수정일"로 보여 주는 값이라 updated_at과 따로 둔다.
    edited_at    timestamptz,
    constraint uq_post_slug unique (slug),
    constraint ck_post_status check (status in ('DRAFT', 'PUBLISHED')),
    constraint ck_post_published_at check (status <> 'PUBLISHED' or published_at is not null)
);

-- 글 목록은 게시 글을 발행일 최신순으로 읽는다. 발행일이 같으면 id로 순서를 정한다.
create index idx_post_published on post (published_at desc, id desc) where status = 'PUBLISHED';

create table tag (
    id   bigint generated always as identity primary key,
    name varchar(50) not null,
    constraint uq_tag_name unique (name)
);

-- 글 하나에 태그 여러 개, 태그 하나에 글 여러 개.
create table post_tag (
    post_id bigint not null references post (id) on delete cascade,
    tag_id  bigint not null references tag (id),
    primary key (post_id, tag_id)
);

-- 기본 키가 (post_id, tag_id) 순서라, 태그로 글을 찾는 쪽에는 색인이 따로 필요하다.
create index idx_post_tag_tag_id on post_tag (tag_id);
