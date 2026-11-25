import type { Metadata } from "next";
import { cacheLife } from "next/cache";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense, type ReactNode } from "react";
import { CopyLinkButton } from "@/components/copy-link-button";
import { MarkdownBody } from "@/components/markdown/markdown-components";
import { TableOfContents } from "@/components/table-of-contents";
import { getPost, type Post } from "@/lib/api";
import { formatDate } from "@/lib/date";
import { renderMarkdown, type Heading } from "@/lib/markdown";
import { tagPath } from "@/lib/routes";

type LoadedPost = {
  post: Omit<Post, "content">;
  body: ReactNode;
  headings: Heading[];
};

/**
 * 글을 받아 본문을 변환한 결과를 캐시한다. 글은 자주 바뀌지 않아서 1시간마다 새로 읽는다.
 * - 없는 글과 초안은 null이다. 곧 게시될 수 있어서 짧게(몇 분) 캐시한다.
 * - 연결 실패와 서버 오류는 던진다. 던진 결과는 캐시에 남지 않아서 다음 요청이 다시 시도한다.
 */
async function loadPost(slug: string): Promise<LoadedPost | null> {
  "use cache";
  const result = await getPost(slug);
  if (result.ok) {
    cacheLife("hours");
    const { content, ...post } = result.data;
    const { body, headings } = await renderMarkdown(content);
    return { post, body, headings };
  }
  if ("notFound" in result) {
    cacheLife("minutes");
    return null;
  }
  throw new Error(`글을 불러오지 못했어요: ${slug}`);
}

export async function generateMetadata({
  params,
}: PageProps<"/posts/[slug]">): Promise<Metadata> {
  const { slug } = await params;
  // 불러오지 못하면 undefined다. 제목 없이 두고, 화면은 오류 화면이 맡는다.
  const loaded = await loadPost(slug).catch(() => undefined);
  if (loaded === undefined) {
    return {};
  }
  // 없는 글과 초안은 "페이지를 찾을 수 없음" 화면과 같은 제목을 쓴다.
  if (loaded === null) {
    return { title: "페이지를 찾을 수 없음" };
  }
  return { title: loaded.post.title, description: loaded.post.summary };
}

/** 글 상세. 글 머리, 목차, 본문, 글 끝 동작 순서다. */
export default function PostPage({ params }: PageProps<"/posts/[slug]">) {
  return (
    <div className="pt-10">
      {/* 주소의 슬러그는 요청 때 알 수 있다. 그동안 아래 문구가 먼저 보인다. */}
      <Suspense
        fallback={
          <p className="mx-auto max-w-content text-ink-muted">
            글을 불러오고 있어요.
          </p>
        }
      >
        <PostArticle params={params} />
      </Suspense>
    </div>
  );
}

async function PostArticle({
  params,
}: Pick<PageProps<"/posts/[slug]">, "params">) {
  const { slug } = await params;
  // 불러오지 못하면 loadPost가 던지고, 오류 화면(app/error.tsx)이 보인다. 없는 글이면 null이다.
  const loaded = await loadPost(slug);

  if (loaded === null) {
    notFound();
  }

  const { post, body, headings } = loaded;
  const hasToc = headings.length >= 2;

  return (
    // 넓은 화면에서는 목차가 본문 오른쪽에 붙는다. 목차가 없으면 본문을 가운데 둔다.
    <div
      className={
        hasToc
          ? "flex flex-col gap-10 lg:flex-row lg:items-start"
          : "mx-auto max-w-content"
      }
    >
      <article className="flex min-w-0 max-w-content flex-1 flex-col gap-6">
        <PostHeader post={post} />
        {hasToc && (
          <div className="lg:hidden">
            <TableOfContents headings={headings} variant="inline" />
          </div>
        )}
        <MarkdownBody>{body}</MarkdownBody>
        <div className="mt-10 flex">
          <CopyLinkButton />
        </div>
      </article>

      {hasToc && (
        <aside className="hidden lg:sticky lg:top-6 lg:block lg:w-60 lg:shrink-0">
          <TableOfContents headings={headings} variant="aside" />
        </aside>
      )}
    </div>
  );
}

/** 글 머리: 태그, 제목, 발행일, 수정일(고친 적이 있을 때만). */
function PostHeader({ post }: { post: LoadedPost["post"] }) {
  return (
    <header className="flex flex-col gap-2">
      {post.tags.length > 0 && (
        <ul className="flex flex-wrap gap-2 text-small">
          {post.tags.map((tag) => (
            <li key={tag}>
              <Link
                href={tagPath(tag)}
                className="flex min-h-11 items-center rounded-pill bg-bean-soft px-4 hover:underline"
              >
                {tag}
              </Link>
            </li>
          ))}
        </ul>
      )}
      <h1 className="text-h1">{post.title}</h1>
      <p className="text-small text-ink-muted">
        <time dateTime={post.publishedAt}>{formatDate(post.publishedAt)}</time>
        {post.editedAt !== null && (
          <>
            {" · 수정 "}
            <time dateTime={post.editedAt}>{formatDate(post.editedAt)}</time>
          </>
        )}
      </p>
    </header>
  );
}
