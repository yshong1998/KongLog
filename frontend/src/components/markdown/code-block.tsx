import type { ComponentProps } from "react";
import { CopyCodeButton } from "./copy-code-button";

type CodeBlockProps = ComponentProps<"pre"> & {
  /** Shiki가 붙인 언어 이름. 언어를 적지 않은 코드 블록은 "text"다. */
  "data-language"?: string;
};

/*
 * 코드 블록. 위 줄에 언어 이름과 복사 버튼을 두고, 긴 줄은 블록 안에서 가로로 스크롤한다.
 * 글자 색은 Shiki가 붙인 CSS 변수(globals.css의 --syntax-*)에서 온다.
 * 바탕이 어두워서 가로 스크롤바도 어두운 모양(color-scheme: dark)으로 그린다.
 */
export function CodeBlock({
  "data-language": language,
  className,
  style,
  children,
  ...props
}: CodeBlockProps) {
  const label = language && language !== "text" ? language : undefined;

  return (
    <figure className="overflow-hidden rounded-md bg-code-bg text-code-ink">
      <div className="flex items-center justify-between gap-2 pl-4">
        {/* 언어 이름이 없어도 복사 버튼이 오른쪽에 붙도록 자리를 남긴다. */}
        <span className="font-mono text-small">{label}</span>
        <CopyCodeButton />
      </div>
      <pre
        {...props}
        className={`${className ?? ""} overflow-x-auto px-4 pb-4 font-mono [color-scheme:dark] text-code [&_code]:bg-transparent [&_code]:p-0`}
        style={style}
      >
        {children}
      </pre>
    </figure>
  );
}
