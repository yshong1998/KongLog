import Link from "next/link";
import { Suspense } from "react";
import { PostCard } from "@/components/post-card";
import { getPosts, getTags } from "@/lib/api";
import { tagPath } from "@/lib/routes";
import { SITE_INTRO, SITE_TAGLINE } from "@/lib/site";

const RECENT_POST_COUNT = 5;

/** 홈. "누가 쓰는 블로그인지"와 "최근에 무엇을 썼는지"를 보여 준다. */
export default function Home() {
  return (
    <>
      <section
        aria-label="소개"
        className="flex max-w-content flex-col gap-2 pt-16 pb-10"
      >
        <h1 className="text-h1">{SITE_TAGLINE}</h1>
        <p className="text-ink-muted">{SITE_INTRO}</p>
        <p>
          <Link
            href="/about"
            className="inline-flex min-h-11 items-center text-link underline"
          >
            소개 더 보기
          </Link>
        </p>
      </section>

      {/* 글과 태그는 요청마다 새로 읽는다. 읽는 동안에는 아래 문구가 먼저 보인다. */}
      <Suspense
        fallback={<p className="text-ink-muted">글을 불러오고 있어요.</p>}
      >
        <RecentPostsAndTags />
      </Suspense>
    </>
  );
}

async function RecentPostsAndTags() {
  const [posts, tags] = await Promise.all([
    getPosts({ size: RECENT_POST_COUNT }),
    getTags(),
  ]);
  // 글을 불러오지 못하면 오류 화면(app/error.tsx)을 보여 준다. 태그만 실패하면 이 화면 안에 알린다.
  if (!posts.ok) {
    throw new Error("최근 글을 불러오지 못했어요");
  }

  // 게시 글이 없으면 태그 모음을 숨긴다. 보여 줄 태그가 없을 때도 숨긴다.
  const noPosts = posts.data.posts.length === 0;
  const noTags = tags.ok && tags.data.length === 0;
  const showTags = !noPosts && !noTags;

  return (
    // 넓은 화면에서는 네 칸 중 세 칸이 최근 글, 한 칸이 태그 모음이다.
    <div className="grid gap-10 lg:grid-cols-4">
      <section
        aria-labelledby="recent-title"
        className="flex min-w-0 flex-col gap-6 lg:col-span-3"
      >
        <h2 id="recent-title" className="text-h3">
          최근 글
        </h2>
        {noPosts ? (
          <p className="text-ink-muted">아직 글이 없어요</p>
        ) : (
          <>
            <ul className="flex flex-col gap-6">
              {posts.data.posts.map((post) => (
                <li key={post.slug}>
                  <PostCard post={post} />
                </li>
              ))}
            </ul>
            <div className="flex">
              <Link
                href="/posts"
                className="flex min-h-11 items-center rounded-pill border border-line-strong bg-surface px-6 text-small font-semibold"
              >
                전체 글 보기
              </Link>
            </div>
          </>
        )}
      </section>

      {showTags && (
        <aside
          aria-labelledby="tags-title"
          className="flex min-w-0 flex-col gap-6"
        >
          <h2 id="tags-title" className="text-h3">
            태그
          </h2>
          {tags.ok ? (
            <ul className="flex flex-wrap gap-2 text-small">
              {tags.data.map((tag) => (
                <li key={tag.name}>
                  <Link
                    href={tagPath(tag.name)}
                    className="flex min-h-11 items-center gap-2 rounded-pill bg-bean-soft px-4 hover:underline"
                  >
                    {tag.name}
                    <span className="text-ink-muted">
                      <span className="sr-only">글 </span>
                      {tag.postCount}
                      <span className="sr-only">개</span>
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-danger">
              태그를 불러오지 못했어요. 잠시 뒤에 다시 시도해 주세요.
            </p>
          )}
        </aside>
      )}
    </div>
  );
}
