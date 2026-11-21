"use client";

import Link from "next/link";
import { useEffect, useId, useRef, useState } from "react";
import type { MenuItem } from "@/lib/site";

/*
 * 헤더의 주 메뉴.
 * 넓은 화면에서는 메뉴가 항상 보이고, 좁은 화면에서는 메뉴 버튼으로 펼치고 접는다.
 * 펼침 상태를 기억해야 해서 클라이언트 컴포넌트다.
 */
export function SiteMenu({ items }: { items: MenuItem[] }) {
  const [open, setOpen] = useState(false);
  const menuId = useId();
  const buttonRef = useRef<HTMLButtonElement>(null);

  // 펼친 상태에서 Esc를 누르면 접고, 포커스를 메뉴 버튼으로 돌려놓는다.
  useEffect(() => {
    if (!open) {
      return;
    }
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setOpen(false);
        buttonRef.current?.focus();
      }
    }
    document.addEventListener("keydown", closeOnEscape);
    return () => document.removeEventListener("keydown", closeOnEscape);
  }, [open]);

  return (
    <>
      <button
        ref={buttonRef}
        type="button"
        aria-label="메뉴"
        aria-expanded={open}
        aria-controls={menuId}
        onClick={() => setOpen((current) => !current)}
        className="ml-auto flex size-11 items-center justify-center rounded-pill sm:hidden"
      >
        <svg
          aria-hidden="true"
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.75"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          {open ? (
            <path d="M6 6l12 12M18 6L6 18" />
          ) : (
            <path d="M4 7h16M4 12h16M4 17h16" />
          )}
        </svg>
      </button>

      <nav
        id={menuId}
        aria-label="주 메뉴"
        className={
          open
            ? "flex basis-full flex-col sm:basis-auto sm:flex-row sm:gap-2"
            : "hidden sm:flex sm:gap-2"
        }
      >
        {items.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            onClick={() => setOpen(false)}
            className="flex min-h-11 items-center font-semibold hover:underline sm:px-4"
          >
            {item.label}
          </Link>
        ))}
      </nav>
    </>
  );
}
