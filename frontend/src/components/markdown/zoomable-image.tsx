"use client";

import { useEffect, useRef, useState, type ComponentProps } from "react";
import { createPortal } from "react-dom";

/*
 * 본문 이미지. 본문 폭에 맞춰 보여 주고, 누르면 화면 가득 크게 본다.
 *
 * - 크게 보기는 브라우저의 <dialog>를 써서 Esc로 닫히고, 닫으면 포커스가 버튼으로 돌아온다.
 * - 마크다운의 이미지는 문단(<p>) 안에 들어 있다. <dialog>는 문단 안에 둘 수 없어서
 *   열 때만 <body> 바로 아래에 만든다(createPortal).
 * - 이미지 크기를 미리 알 수 없어서 next/image 대신 img를 쓴다.
 * - 버튼은 안의 이미지 크기에 맞춰 폭이 정해져서, 이미지의 max-width: 100%가 먹지 않는다.
 *   그래서 바깥 span을 크기 기준(@container)으로 두고, 이미지 폭을 그 폭(100cqw)까지로 막는다.
 */
export function ZoomableImage({ src, alt, title }: ComponentProps<"img">) {
  const [open, setOpen] = useState(false);
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    if (open) {
      dialogRef.current?.showModal();
    }
  }, [open]);

  if (typeof src !== "string" || src === "") {
    return null;
  }

  return (
    <span className="block @container">
      <button
        type="button"
        aria-label={alt ? `이미지 크게 보기: ${alt}` : "이미지 크게 보기"}
        onClick={() => setOpen(true)}
        className="cursor-zoom-in rounded-md"
      >
        {/* eslint-disable-next-line @next/next/no-img-element -- 크기를 모르는 이미지라 img를 쓴다. */}
        <img
          src={src}
          alt={alt ?? ""}
          title={title}
          loading="lazy"
          decoding="async"
          className="h-auto max-w-[100cqw] rounded-md"
        />
      </button>
      {open &&
        createPortal(
          <dialog
            ref={dialogRef}
            aria-label={alt || "크게 본 이미지"}
            onClose={() => setOpen(false)}
            // 이미지 바깥(바탕)을 눌러도 닫는다.
            onClick={(event) => {
              if (event.target === event.currentTarget) {
                event.currentTarget.close();
              }
            }}
            className="m-auto max-h-none max-w-none bg-transparent p-4 backdrop:bg-bg"
          >
            <div className="flex flex-col items-end gap-2">
              <form method="dialog">
                <button
                  type="submit"
                  className="min-h-11 rounded-pill border border-line-strong bg-surface px-4 text-small text-ink"
                >
                  닫기
                </button>
              </form>
              {/* eslint-disable-next-line @next/next/no-img-element -- 위와 같은 이유 */}
              <img
                src={src}
                alt={alt ?? ""}
                className="max-h-[calc(100dvh-6rem)] max-w-[calc(100vw-2rem)] rounded-md"
              />
            </div>
          </dialog>,
          document.body,
        )}
    </span>
  );
}
