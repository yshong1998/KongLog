"use client";

import { useEffect, useState } from "react";
import type { Heading } from "@/lib/markdown";

/** 화면 위에서 이만큼(px) 아래까지 올라온 소제목을 "지금 읽는 소제목"으로 본다. */
const ACTIVE_OFFSET_PX = 96;

/**
 * 지금 읽는 소제목의 id. 화면 위쪽을 지난 마지막 소제목이다.
 * 페이지 끝에 닿으면 마지막 소제목이 위까지 올라오지 못해도 그것을 고른다.
 */
function useActiveHeading(ids: string[]): string | undefined {
  const [active, setActive] = useState<string>();
  const key = ids.join("\n");

  useEffect(() => {
    if (key === "") {
      return;
    }
    const targets = key.split("\n");
    let frame = 0;

    function update() {
      frame = 0;
      const atBottom =
        window.innerHeight + window.scrollY >=
        document.documentElement.scrollHeight - 2;
      let current: string | undefined;
      for (const id of targets) {
        const element = document.getElementById(id);
        if (element === null) {
          continue;
        }
        if (element.getBoundingClientRect().top > ACTIVE_OFFSET_PX) {
          break;
        }
        current = id;
      }
      setActive(atBottom ? targets.at(-1) : current);
    }

    function schedule() {
      if (frame === 0) {
        frame = requestAnimationFrame(update);
      }
    }

    schedule();
    window.addEventListener("scroll", schedule, { passive: true });
    window.addEventListener("resize", schedule);
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener("scroll", schedule);
      window.removeEventListener("resize", schedule);
    };
  }, [key]);

  return active;
}

function HeadingLinks({
  headings,
  active,
}: {
  headings: Heading[];
  active?: string;
}) {
  return (
    <ul className="flex flex-col">
      {headings.map((heading) => {
        const current = heading.id === active;
        return (
          <li key={heading.id} className={heading.depth === 3 ? "pl-4" : ""}>
            <a
              href={`#${heading.id}`}
              aria-current={current ? "location" : undefined}
              className={`flex min-h-11 items-center ${
                current
                  ? "font-bold text-link underline decoration-2 underline-offset-4"
                  : "hover:underline"
              }`}
            >
              {heading.text}
            </a>
          </li>
        );
      })}
    </ul>
  );
}

/*
 * 글의 목차. 소제목(##, ###)에서 만든다. 소제목이 2개 미만이면 그리지 않는다.
 *
 * - aside: 넓은 화면에서 본문 옆에 둔다. 지금 읽는 소제목을 굵은 글자와 밑줄로 강조한다.
 * - inline: 좁은 화면에서 본문 위에 둔다. 접혀 있고, 누르면 펼쳐진다.
 */
export function TableOfContents({
  headings,
  variant,
}: {
  headings: Heading[];
  variant: "aside" | "inline";
}) {
  const active = useActiveHeading(
    variant === "aside" ? headings.map((h) => h.id) : [],
  );

  if (headings.length < 2) {
    return null;
  }

  if (variant === "inline") {
    return (
      <details className="rounded-md bg-surface-sunken text-small">
        <summary className="flex min-h-11 cursor-pointer items-center px-6 font-bold">
          목차
        </summary>
        <nav aria-label="목차" className="px-6 pb-4">
          <HeadingLinks headings={headings} />
        </nav>
      </details>
    );
  }

  return (
    <nav
      aria-labelledby="toc-title"
      className="flex flex-col gap-2 rounded-md bg-surface-sunken p-6 text-small"
    >
      <h2 id="toc-title" className="font-bold text-ink-muted">
        목차
      </h2>
      <HeadingLinks headings={headings} active={active} />
    </nav>
  );
}
