import Link from "next/link";
import type { PostSummary } from "@/lib/api";
import { formatDate } from "@/lib/date";
import { postPath, tagPath } from "@/lib/routes";

/*
 * 글 하나를 보여 주는 카드. 대표 이미지는 두지 않는다.
 * 카드 어디를 눌러도 글 상세로 가도록 제목 링크의 누르는 영역을 카드 전체로 넓혔다.
 * 태그 링크는 그 위에 놓여서 따로 눌린다.
 */
export function PostCard({ post }: { post: PostSummary }) {
  return (
    <article className="relative flex flex-col gap-2 rounded-md border border-line bg-surface p-6">
      <h3 className="text-h2">
        <Link
          href={postPath(post.slug)}
          className="after:absolute after:inset-0 after:rounded-md hover:underline"
        >
          {post.title}
        </Link>
      </h3>
      <p className="line-clamp-2">{post.summary}</p>
      <div className="flex flex-wrap items-center gap-x-2 text-small">
        {post.tags.length > 0 && (
          <ul className="flex flex-wrap gap-x-2">
            {post.tags.map((tag) => (
              <li key={tag}>
                {/* 보이는 알약 모양은 작게 두고, 누르는 영역만 44px로 맞춘다. */}
                <Link
                  href={tagPath(tag)}
                  className="group relative flex min-h-11 items-center rounded-pill"
                >
                  <span className="rounded-pill bg-bean-soft px-4 py-1 group-hover:underline">
                    {tag}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
        <time dateTime={post.publishedAt} className="text-ink-muted">
          {formatDate(post.publishedAt)}
        </time>
      </div>
    </article>
  );
}
