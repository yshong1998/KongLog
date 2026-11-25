"use client";

import { useRef } from "react";
import { useCopy, type CopyStatus } from "@/lib/use-copy";

const LABEL: Record<CopyStatus, string> = {
  idle: "복사",
  copied: "복사했어요",
  failed: "복사하지 못했어요",
};

/*
 * 코드 블록의 복사 버튼. 누르면 같은 코드 블록의 코드를 클립보드에 넣는다.
 * 코드를 속성으로 받지 않고 화면의 코드를 읽는다. 받으면 같은 코드가 HTML에 두 번 실린다.
 * 결과는 버튼 문구로 알려서 색만으로 구분하지 않는다.
 */
export function CopyCodeButton() {
  const [status, copy] = useCopy();
  const buttonRef = useRef<HTMLButtonElement>(null);

  return (
    <button
      ref={buttonRef}
      type="button"
      onClick={() =>
        copy(
          buttonRef.current?.closest("figure")?.querySelector("pre code")
            ?.textContent,
        )
      }
      className="min-h-11 rounded-pill px-4 text-small hover:underline"
    >
      <span aria-live="polite">{LABEL[status]}</span>
    </button>
  );
}
