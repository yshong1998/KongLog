import { COPYRIGHT_YEAR, GITHUB_URL, SITE_NAME } from "@/lib/site";

/** 모든 공개 화면 아래쪽에 보이는 푸터. 저작권 표기와 외부 링크를 둔다. */
export function SiteFooter() {
  return (
    <footer className="mt-16 border-t border-line">
      <div className="mx-auto flex max-w-wide flex-wrap items-center gap-x-6 px-4 py-4 text-small text-ink-muted">
        <span className="grow">
          © {COPYRIGHT_YEAR} {SITE_NAME}
        </span>
        <a
          href={GITHUB_URL}
          rel="noopener noreferrer"
          className="flex min-h-11 items-center text-link underline"
        >
          GitHub
        </a>
      </div>
    </footer>
  );
}
