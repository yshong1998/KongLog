import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { PostCardList } from "@/components/post-card-list";
import { TagFilter } from "@/components/tag-filter";
import { getPosts, getTags } from "@/lib/api";
import { POSTS_PATH } from "@/lib/routes";

export const metadata: Metadata = {
  title: "글",
};

/** 글 목록. 태그로 글을 좁혀 찾는다. 고른 태그는 주소(/posts?tag=이름)에 남는다. */
export default function PostsPage({ searchParams }: PageProps<"/posts">) {
  return (
    <div className="mx-auto flex max-w-content flex-col gap-6 pt-10">
      {/* 주소의 태그와 글은 요청마다 새로 읽는다. 읽는 동안에는 아래가 먼저 보인다. */}
      <Suspense
        fallback={
          <>
            <PageTitle />
            <p className="text-ink-muted">글을 불러오고 있어요.</p>
          </>
        }
      >
        <FilteredPosts searchParams={searchParams} />
      </Suspense>
    </div>
  );
}

/** 화면 제목과 글 수. 글 수는 읽은 뒤에만 보인다. */
function PageTitle({ count }: { count?: string }) {
  return (
    <div className="flex flex-wrap items-baseline gap-x-4 gap-y-2">
      <h1 className="text-h1">글</h1>
      {count !== undefined && (
        <p className="text-small text-ink-muted">{count}</p>
      )}
    </div>
  );
}

/** 주소의 tag 값을 읽는다. 같은 이름이 여러 번 오면 첫 번째만 쓰고, 비어 있으면 고르지 않은 것으로 본다. */
function readTag(value: string | string[] | undefined): string | undefined {
  const first = Array.isArray(value) ? value[0] : value;
  return first === undefined || first === "" ? undefined : first;
}

/** 영문 대문자(A–Z)만 소문자로 바꾼다. 그 밖의 글자는 그대로 둔다. */
function foldAsciiCase(value: string): string {
  return value.replace(/[A-Z]/g, (letter) => letter.toLowerCase());
}

/**
 * 주소의 태그 값에 맞는 저장된 태그 이름을 찾는다. 맞는 태그가 없으면 주소의 값을 그대로 쓴다.
 *
 * 백엔드는 태그를 대소문자 구분 없이 거르므로 ?tag=spring으로도 Spring 글이 온다.
 * 화면의 선택 표시와 문구는 저장된 이름(Spring)에 맞춘다. 주소는 바꾸지 않는다.
 * 영문 A–Z만 접어 비교한다. 백엔드의 lower()와 결과가 갈릴 수 있는 글자(É 등)는
 * 맞는 태그가 없는 것으로 본다.
 */
function storedTagName(tag: string, names: string[]): string {
  if (names.includes(tag)) {
    return tag;
  }
  const folded = foldAsciiCase(tag);
  return names.find((name) => foldAsciiCase(name) === folded) ?? tag;
}

async function FilteredPosts({
  searchParams,
}: Pick<PageProps<"/posts">, "searchParams">) {
  const tag = readTag((await searchParams).tag);
  const [posts, tags] = await Promise.all([getPosts({ tag }), getTags()]);
  // 글을 불러오지 못하면 오류 화면(app/error.tsx)을 보여 준다. 태그만 실패하면 이 화면 안에 알린다.
  if (!posts.ok) {
    throw new Error("글 목록을 불러오지 못했어요");
  }

  const tagNames = tags.ok ? tags.data.map((t) => t.name) : [];
  // 화면에 보여 줄 태그 이름. 글을 부를 때는 주소의 값(tag)을 그대로 쓴다.
  const shownTag =
    tag === undefined ? undefined : storedTagName(tag, tagNames);

  return (
    <>
      <PageTitle
        count={`${shownTag ?? "전체"} ${posts.data.totalCount}개`}
      />

      {tags.ok ? (
        // 게시 글이 하나도 없으면 태그도 없다. 그때는 필터를 그리지 않는다.
        tagNames.length > 0 && (
          <TagFilter tags={tagNames} selected={shownTag} />
        )
      ) : (
        <p className="text-danger">
          태그를 불러오지 못했어요. 잠시 뒤에 다시 시도해 주세요.
        </p>
      )}

      {posts.data.posts.length > 0 ? (
        // 태그가 바뀌면 "더 보기"로 받아 둔 글을 버리고 새 목록으로 시작하도록 key를 준다.
        <PostCardList
          key={tag ?? ""}
          initialPosts={posts.data.posts}
          initialCursor={posts.data.nextCursor}
          tag={tag}
        />
      ) : tag === undefined ? (
        <p className="text-ink-muted">아직 글이 없어요</p>
      ) : (
        <div className="flex flex-col items-start gap-4">
          <p className="text-ink-muted">
            <span className="font-semibold text-ink">{shownTag}</span> 태그가 붙은
            글이 없어요
          </p>
          <Link
            href={POSTS_PATH}
            className="flex min-h-11 items-center rounded-pill border border-line-strong bg-surface px-6 text-small font-semibold"
          >
            조건 지우기
          </Link>
        </div>
      )}
    </>
  );
}
