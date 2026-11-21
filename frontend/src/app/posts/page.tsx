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

async function FilteredPosts({
  searchParams,
}: Pick<PageProps<"/posts">, "searchParams">) {
  const tag = readTag((await searchParams).tag);
  const [posts, tags] = await Promise.all([getPosts({ tag }), getTags()]);

  return (
    <>
      <PageTitle
        count={
          posts.ok ? `${tag ?? "전체"} ${posts.data.totalCount}개` : undefined
        }
      />

      {tags.ok ? (
        // 게시 글이 하나도 없으면 태그도 없다. 그때는 필터를 그리지 않는다.
        tags.data.length > 0 && (
          <TagFilter tags={tags.data.map((t) => t.name)} selected={tag} />
        )
      ) : (
        <p className="text-danger">
          태그를 불러오지 못했어요. 잠시 뒤에 다시 시도해 주세요.
        </p>
      )}

      {!posts.ok ? (
        <p className="text-danger">
          글을 불러오지 못했어요. 잠시 뒤에 다시 시도해 주세요.
        </p>
      ) : posts.data.posts.length > 0 ? (
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
            <span className="font-semibold text-ink">{tag}</span> 태그가 붙은
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
