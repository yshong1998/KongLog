import rehypeShiki from "@shikijs/rehype";
import { toJsxRuntime } from "hast-util-to-jsx-runtime";
import type { ReactNode } from "react";
import { Fragment, jsx, jsxs } from "react/jsx-runtime";
import rehypeSlug from "rehype-slug";
import remarkGfm from "remark-gfm";
import remarkParse from "remark-parse";
import remarkRehype from "remark-rehype";
import { createCssVariablesTheme, type ShikiTransformer } from "shiki";
import { unified } from "unified";
import { markdownComponents } from "@/components/markdown/markdown-components";

/*
 * 마크다운을 서버에서 React 요소로 바꾼다. 독자 브라우저에는 변환 코드가 가지 않는다.
 *
 * 1. remark-parse, remark-gfm: 마크다운을 읽는다. 표, 취소선, 자동 링크(GFM)도 읽는다.
 * 2. remark-rehype: HTML 구조로 바꾼다. 본문에 직접 쓴 HTML은 기본값대로 버린다.
 * 3. rehype-slug: 소제목마다 id를 붙인다. 목차와 소제목 링크가 이 id를 쓴다.
 * 4. Shiki: 코드 블록을 언어별로 색칠한다. 색은 코드에 적지 않고 CSS 변수 이름만 붙인다.
 *    실제 값은 globals.css의 --syntax-* 다.
 * 5. hast-util-to-jsx-runtime: React 요소로 바꾼다. 이때 코드 블록, 이미지, 표 같은
 *    요소를 markdown-components의 컴포넌트로 바꿔 끼운다.
 */

/** 코드 블록에 언어 이름을 남긴다. 화면의 언어 표시가 이 값을 읽는다. */
const keepLanguage: ShikiTransformer = {
  name: "konglog:keep-language",
  pre(node) {
    node.properties["data-language"] = this.options.lang;
  },
};

const processor = unified()
  .use(remarkParse)
  .use(remarkGfm)
  .use(remarkRehype)
  .use(rehypeSlug)
  .use(rehypeShiki, {
    theme: createCssVariablesTheme({ name: "konglog" }),
    // 글에 나오는 언어만 그때그때 불러온다.
    lazy: true,
    // 언어를 적지 않았거나 Shiki가 모르는 언어면 색 없이 그대로 보여 준다.
    defaultLanguage: "text",
    fallbackLanguage: "text",
    transformers: [keepLanguage],
  });

/** 본문의 소제목 하나. 목차를 만들 때 쓴다. */
export type Heading = {
  id: string;
  text: string;
  /** 2는 `##`, 3은 `###` */
  depth: 2 | 3;
};

export type RenderedMarkdown = {
  body: ReactNode;
  /** 본문에 나온 순서대로 담은 `##`, `###` 소제목 */
  headings: Heading[];
};

type HastNode = Awaited<ReturnType<typeof processor.run>>["children"][number];

function textOf(node: HastNode): string {
  if (node.type === "text") {
    return node.value;
  }
  if ("children" in node) {
    return node.children.map(textOf).join("");
  }
  return "";
}

function collectHeadings(nodes: HastNode[], headings: Heading[]): Heading[] {
  for (const node of nodes) {
    if (node.type !== "element") {
      continue;
    }
    const depth =
      node.tagName === "h2" ? 2 : node.tagName === "h3" ? 3 : undefined;
    const id = node.properties.id;
    if (depth !== undefined && typeof id === "string") {
      headings.push({ id, text: textOf(node).trim(), depth });
      continue;
    }
    collectHeadings(node.children, headings);
  }
  return headings;
}

/**
 * 마크다운 원문을 본문 요소와 소제목 목록으로 바꾼다. 본문은 MarkdownBody로 감싸서 쓴다.
 *
 * `'use cache'` 범위 안에서 불러야 한다. Shiki가 코드를 색칠할 때 안에서 Date.now()를 쓰는데,
 * 캐시 컴포넌트가 켜진 상태에서 캐시 밖에서 부르면 빌드가 미리 그리기(프리렌더)를 멈춘다.
 */
export async function renderMarkdown(
  source: string,
): Promise<RenderedMarkdown> {
  const tree = await processor.run(processor.parse(source));
  return {
    body: toJsxRuntime(tree, {
      Fragment,
      jsx,
      jsxs,
      components: markdownComponents,
    }),
    headings: collectHeadings(tree.children, []),
  };
}
