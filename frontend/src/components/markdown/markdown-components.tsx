import type { Components } from "hast-util-to-jsx-runtime";
import type { ComponentProps, ReactNode } from "react";
import { CodeBlock } from "./code-block";
import { ZoomableImage } from "./zoomable-image";

/*
 * 마크다운에서 나온 HTML 요소를 그릴 컴포넌트들. 본문 서식은 모두 여기서 정한다.
 * 블록 사이 간격은 MarkdownBody가 정하고, 여기서는 요소 자체의 모양만 정한다.
 */

/** renderMarkdown이 만든 본문을 감싼다. 블록 사이 간격과 본문 폭을 정한다. */
export function MarkdownBody({ children }: { children: ReactNode }) {
  return (
    <div className="max-w-content min-w-0 [&>*+*]:mt-6 [&>h1]:mt-10 [&>h2]:mt-10 [&>h3]:mt-10">
      {children}
    </div>
  );
}

/** 본문 링크로 허용하는 주소. 그 밖의 주소(javascript: 등)는 링크로 만들지 않는다. */
function isSafeHref(href: string): boolean {
  const scheme = /^([a-z][a-z0-9+.-]*):/i.exec(href)?.[1].toLowerCase();
  // 스킴이 없으면 같은 사이트 안의 주소다(/posts/..., #소제목).
  return scheme === undefined || ["http", "https", "mailto"].includes(scheme);
}

function Anchor({ href, children, ...props }: ComponentProps<"a">) {
  if (href === undefined || !isSafeHref(href)) {
    return <span>{children}</span>;
  }
  return (
    <a {...props} href={href} className="text-link underline">
      {children}
    </a>
  );
}

/**
 * 소제목. 누르면 그 위치의 링크가 주소에 반영된다.
 * 글자 높이가 44px보다 낮아서, 보이는 모양과 줄 간격은 두고 누르는 영역만 위아래로 넓힌다.
 */
function headingLink(Tag: "h2" | "h3", className: string) {
  return function Heading({ id, children }: ComponentProps<"h2">) {
    return (
      <Tag id={id} className={`scroll-mt-6 ${className}`}>
        {id === undefined ? (
          children
        ) : (
          <a
            href={`#${id}`}
            className="relative inline-block hover:underline after:absolute after:inset-x-0 after:-inset-y-2"
          >
            {children}
          </a>
        )}
      </Tag>
    );
  };
}

/** 표가 화면보다 넓으면 표 안에서만 가로로 스크롤한다. */
function Table(props: ComponentProps<"table">) {
  return (
    // 키보드로도 스크롤할 수 있게 포커스를 받는다.
    <div
      tabIndex={0}
      role="region"
      aria-label="표"
      className="overflow-x-auto rounded-md border border-line"
    >
      <table {...props} className="w-full border-collapse text-small" />
    </div>
  );
}

export const markdownComponents: Partial<Components> = {
  // 본문의 `#`은 글 제목(h1)과 겹치지 않게 h2 모양으로 보인다.
  h1: (props) => <h1 {...props} className="text-h2" />,
  h2: headingLink("h2", "text-h2"),
  h3: headingLink("h3", "text-h3"),
  h4: (props) => <h4 {...props} className="font-semibold" />,
  h5: (props) => <h5 {...props} className="font-semibold" />,
  h6: (props) => <h6 {...props} className="font-semibold" />,
  a: Anchor,
  ul: (props) => (
    <ul {...props} className="list-disc pl-6 [&>li+li]:mt-2 [&_ul]:mt-2 [&_ol]:mt-2" />
  ),
  ol: (props) => (
    <ol {...props} className="list-decimal pl-6 [&>li+li]:mt-2 [&_ul]:mt-2 [&_ol]:mt-2" />
  ),
  blockquote: (props) => (
    <blockquote
      {...props}
      className="border-l-4 border-line-strong pl-4 text-ink-muted [&>*+*]:mt-4"
    />
  ),
  table: Table,
  th: (props) => (
    <th
      {...props}
      className="whitespace-nowrap bg-surface-sunken px-4 py-2 text-left font-semibold"
    />
  ),
  td: (props) => <td {...props} className="border-t border-line px-4 py-2" />,
  hr: (props) => <hr {...props} className="border-line" />,
  // 인라인 코드. 코드 블록 안의 code는 CodeBlock이 모양을 덮어쓴다.
  code: (props) => (
    <code
      {...props}
      className="rounded-sm bg-surface-sunken px-1 font-mono text-code"
    />
  ),
  pre: CodeBlock,
  img: ZoomableImage,
};
