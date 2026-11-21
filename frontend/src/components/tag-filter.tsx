"use client";

import Link from "next/link";
import { useState } from "react";
import { POSTS_PATH, tagPath } from "@/lib/routes";

/** 접힌 상태에서 보여 주는 태그 수. 이보다 많으면 "태그 더 보기"로 펼친다. */
const COLLAPSED_TAG_COUNT = 6;

const TAG_BUTTON = "flex min-h-11 items-center rounded-pill px-4";

/*
 * 글 목록의 태그 필터. "전체"와 태그 버튼들이고 한 번에 하나만 고른다.
 * 고른 태그는 주소(/posts?tag=이름)에 남으므로 버튼은 모두 링크다.
 * 펼침 상태를 기억해야 해서 클라이언트 컴포넌트다.
 */
export function TagFilter({
  tags,
  selected,
}: {
  /** 태그 이름들. 글이 많은 순이다. */
  tags: string[];
  /** 지금 고른 태그. 없으면 "전체"가 선택된 것이다. */
  selected?: string;
}) {
  const [expanded, setExpanded] = useState(false);

  // 접힌 상태에서도 고른 태그는 보이게 한다. 뒤쪽 태그를 골랐으면 앞쪽 태그들 뒤에 붙인다.
  const firstTags = tags.slice(0, COLLAPSED_TAG_COUNT);
  const selectedIsHidden =
    selected !== undefined &&
    tags.includes(selected) &&
    !firstTags.includes(selected);
  const collapsedTags = selectedIsHidden ? [...firstTags, selected] : firstTags;
  const visibleTags = expanded ? tags : collapsedTags;
  const canExpand = tags.length > collapsedTags.length;

  return (
    <nav aria-label="태그로 거르기">
      <ul className="flex flex-wrap gap-2 text-small">
        <li>
          <TagLink href={POSTS_PATH} selected={selected === undefined}>
            전체
          </TagLink>
        </li>
        {visibleTags.map((tag) => (
          <li key={tag}>
            <TagLink href={tagPath(tag)} selected={tag === selected}>
              {tag}
            </TagLink>
          </li>
        ))}
        {canExpand && (
          <li>
            <button
              type="button"
              aria-expanded={expanded}
              onClick={() => setExpanded((current) => !current)}
              className={`${TAG_BUTTON} border border-line-strong bg-surface`}
            >
              {expanded ? "태그 접기" : "태그 더 보기"}
            </button>
          </li>
        )}
      </ul>
    </nav>
  );
}

/* 선택된 태그는 색만이 아니라 체크 표시와 굵은 글자로도 구분한다. */
function TagLink({
  href,
  selected,
  children,
}: {
  href: string;
  selected: boolean;
  children: React.ReactNode;
}) {
  return (
    <Link
      href={href}
      aria-current={selected ? "true" : undefined}
      className={
        selected
          ? `${TAG_BUTTON} gap-1 bg-bean-deep font-bold text-on-bean-deep`
          : `${TAG_BUTTON} bg-bean-soft hover:underline`
      }
    >
      {selected && (
        <svg
          aria-hidden="true"
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M5 12l5 5 9-10" />
        </svg>
      )}
      {children}
    </Link>
  );
}
