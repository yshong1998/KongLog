import { readFile } from "node:fs/promises";
import path from "node:path";
import type { Metadata } from "next";
import { MarkdownBody } from "@/components/markdown/markdown-components";
import { renderMarkdown } from "@/lib/markdown";
import { AUTHOR_NAME, AUTHOR_TAGLINE } from "@/lib/site";

export const metadata: Metadata = {
  title: "소개",
  description: AUTHOR_TAGLINE,
};

/** 소개 본문 파일. 배포할 때만 바뀐다. */
const ABOUT_FILE = path.join(process.cwd(), "src/content/about.md");

/**
 * 소개 본문을 읽어 글 상세와 같은 방식으로 변환한다.
 * 파일은 배포할 때만 바뀌어서 빌드할 때 한 번 읽고 캐시한다.
 */
async function loadAbout() {
  "use cache";
  const source = await readFile(ABOUT_FILE, "utf8");
  const { body } = await renderMarkdown(source);
  return body;
}

/** 소개. 이름과 한 줄 소개, 자기소개 본문(연락 수단 포함) 순서다. */
export default async function AboutPage() {
  const body = await loadAbout();

  return (
    <article className="mx-auto flex max-w-content flex-col gap-6 pt-10">
      <header className="flex flex-col gap-2">
        <h1 className="text-h1">{AUTHOR_NAME}</h1>
        <p className="text-ink-muted">{AUTHOR_TAGLINE}</p>
      </header>
      <MarkdownBody>{body}</MarkdownBody>
    </article>
  );
}
