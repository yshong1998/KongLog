import Link from "next/link";
import { SiteMenu } from "@/components/site-menu";
import { MAIN_MENU, SITE_NAME } from "@/lib/site";

/** 모든 공개 화면 위쪽에 보이는 헤더. 블로그 이름을 누르면 홈으로 간다. */
export function SiteHeader() {
  return (
    <header className="border-b border-line">
      <div className="mx-auto flex max-w-wide flex-wrap items-center gap-x-6 px-4 py-2">
        <Link href="/" className="flex min-h-11 items-center gap-2">
          <span
            aria-hidden="true"
            className="block size-4 rounded-pill border border-bean-deep bg-bean"
          />
          <span className="font-display text-wordmark">{SITE_NAME}</span>
        </Link>
        <SiteMenu items={MAIN_MENU} />
      </div>
    </header>
  );
}
