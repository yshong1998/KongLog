"use client";

import { useEffect, useState } from "react";

export type CopyStatus = "idle" | "copied" | "failed";

const RESET_AFTER_MS = 2000;

/**
 * 글자를 클립보드에 넣고 결과를 잠깐 기억한다. 2초 뒤에 처음 상태로 돌아간다.
 * 코드 복사와 링크 복사 버튼이 함께 쓴다.
 */
export function useCopy(): [CopyStatus, (text: string | null | undefined) => void] {
  const [status, setStatus] = useState<CopyStatus>("idle");

  useEffect(() => {
    if (status === "idle") {
      return;
    }
    const timer = setTimeout(() => setStatus("idle"), RESET_AFTER_MS);
    return () => clearTimeout(timer);
  }, [status]);

  function copy(text: string | null | undefined) {
    if (text == null) {
      setStatus("failed");
      return;
    }
    navigator.clipboard.writeText(text).then(
      () => setStatus("copied"),
      () => setStatus("failed"),
    );
  }

  return [status, copy];
}
