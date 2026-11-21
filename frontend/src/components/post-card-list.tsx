"use client";

import { useState } from "react";
import { PostCard } from "@/components/post-card";
import type { PostList, PostSummary } from "@/lib/api";
import { morePostsPath } from "@/lib/routes";

/** 다음 묶음을 받는다. 받지 못하면 undefined를 돌려준다. */
async function fetchMorePosts(
  cursor: string,
  tag?: string,
): Promise<PostList | undefined> {
  try {
    const response = await fetch(morePostsPath(cursor, tag));
    if (!response.ok) {
      return undefined;
    }
    // 이 주소(/internal/posts)는 모양을 확인한 응답만 돌려준다. 여기서 다시 확인하지 않는다.
    return (await response.json()) as PostList;
  } catch {
    return undefined;
  }
}

/*
 * 글 목록 화면의 글 카드 목록과 "더 보기" 버튼.
 * 처음 묶음은 서버에서 받아 넘겨주고, 그 뒤의 묶음은 버튼을 누를 때 브라우저에서 받아 뒤에 붙인다.
 * 받은 글을 기억해야 해서 클라이언트 컴포넌트다.
 */
export function PostCardList({
  initialPosts,
  initialCursor,
  tag,
}: {
  initialPosts: PostSummary[];
  /** 처음 묶음의 nextCursor. 더 받을 글이 없으면 null이다. */
  initialCursor: string | null;
  /** 목록을 거른 태그. 이어 받을 때도 같은 태그를 넣어야 한다. */
  tag?: string;
}) {
  const [posts, setPosts] = useState(initialPosts);
  const [cursor, setCursor] = useState(initialCursor);
  const [status, setStatus] = useState<"idle" | "loading" | "failed">("idle");

  async function loadMore() {
    if (cursor === null || status === "loading") {
      return;
    }
    setStatus("loading");
    const more = await fetchMorePosts(cursor, tag);
    if (more === undefined) {
      setStatus("failed");
      return;
    }
    setPosts((current) => [...current, ...more.posts]);
    setCursor(more.nextCursor);
    setStatus("idle");
  }

  return (
    <>
      <ul className="flex flex-col gap-6">
        {posts.map((post) => (
          <li key={post.slug}>
            <PostCard post={post} titleAs="h2" />
          </li>
        ))}
      </ul>

      {/* 남은 글이 없으면 버튼을 숨긴다. */}
      {cursor !== null && (
        <div className="flex flex-col items-center gap-2">
          {status === "failed" && (
            <p role="alert" className="text-small text-danger">
              글을 더 불러오지 못했어요.
            </p>
          )}
          {/* 불러오는 동안에도 포커스를 잃지 않도록 disabled 대신 aria-disabled를 쓴다. */}
          <button
            type="button"
            aria-disabled={status === "loading"}
            onClick={loadMore}
            className="flex min-h-11 items-center rounded-pill border border-bean-deep bg-bean px-6 text-small font-bold text-on-bean aria-disabled:border-line aria-disabled:bg-surface-sunken aria-disabled:font-normal aria-disabled:text-ink-muted"
          >
            {status === "loading"
              ? "불러오고 있어요"
              : status === "failed"
                ? "다시 시도"
                : "더 보기"}
          </button>
        </div>
      )}
    </>
  );
}
