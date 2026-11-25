import type { Metadata } from "next";
import Link from "next/link";
import { POSTS_PATH } from "@/lib/routes";

export const metadata: Metadata = {
  title: "페이지를 찾을 수 없음",
};

/**
 * 없는 주소, 없는 글, 초안 글에서 보이는 화면.
 * 글 상세가 notFound()를 부를 때와 어느 주소에도 맞지 않을 때 모두 이 화면이 나온다.
 */
export default function NotFound() {
  return (
    <div className="mx-auto flex max-w-content flex-col gap-6 pt-16">
      <div className="flex flex-col gap-2">
        <h1 className="text-h1">페이지를 찾을 수 없어요</h1>
        <p className="text-ink-muted">
          주소가 바뀌었거나 없는 페이지예요. 주소를 다시 확인해 주세요.
        </p>
      </div>
      <div className="flex flex-wrap gap-4">
        <Link
          href="/"
          className="flex min-h-11 items-center rounded-pill bg-bean px-6 text-small font-semibold text-on-bean"
        >
          홈으로 가기
        </Link>
        <Link
          href={POSTS_PATH}
          className="flex min-h-11 items-center rounded-pill border border-line-strong bg-surface px-6 text-small font-semibold"
        >
          글 목록으로 가기
        </Link>
      </div>
    </div>
  );
}
