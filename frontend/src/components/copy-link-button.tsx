"use client";

import { useCopy, type CopyStatus } from "@/lib/use-copy";

const LABEL: Record<CopyStatus, string> = {
  idle: "링크 복사",
  copied: "링크를 복사했어요",
  failed: "복사하지 못했어요",
};

/** 지금 보고 있는 글의 주소를 복사한다. 목차를 눌러 붙은 #소제목은 빼고 복사한다. */
export function CopyLinkButton() {
  const [status, copy] = useCopy();

  return (
    <button
      type="button"
      onClick={() => copy(window.location.origin + window.location.pathname)}
      className="flex min-h-11 items-center gap-2 rounded-pill border border-line-strong bg-surface px-6 text-small font-semibold"
    >
      <svg
        aria-hidden="true"
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1" />
        <path d="M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1" />
      </svg>
      <span aria-live="polite">{LABEL[status]}</span>
    </button>
  );
}
