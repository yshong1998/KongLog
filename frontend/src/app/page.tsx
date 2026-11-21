import { Suspense } from "react";
import { pingBackend } from "@/lib/api";

/*
 * 0단계 확인용 화면.
 * 디자인 시스템 값이 적용됐는지, 백엔드와 통신하는지만 보여 준다.
 * 1단계에서 실제 홈 화면으로 바꾼다.
 */
export default function Home() {
  return (
    <div className="flex max-w-content flex-col gap-6 pt-16">
      <h1 className="text-h1">개발하다 만난 문제를 콩알만큼씩 적어 둬요.</h1>
      <p className="text-ink-muted">
        아직 뼈대만 있어요. 실제 화면은 1단계에서 만들어요.
      </p>

      <section
        aria-labelledby="backend-status-title"
        className="flex flex-col gap-2 rounded-md border border-line bg-surface p-6"
      >
        <h2 id="backend-status-title" className="text-h3">
          백엔드 연결
        </h2>
        <Suspense
          fallback={<p className="text-ink-muted">확인하고 있어요.</p>}
        >
          <BackendStatus />
        </Suspense>
      </section>

      <section
        aria-labelledby="tokens-title"
        className="flex flex-col gap-4 rounded-md border border-line bg-surface p-6"
      >
        <h2 id="tokens-title" className="text-h3">
          디자인 시스템 확인
        </h2>
        <div className="flex flex-wrap items-center gap-2 text-small">
          <span className="rounded-pill bg-bean-soft px-4 py-1">태그</span>
          <span className="rounded-pill bg-bean-deep px-4 py-1 font-bold text-on-bean-deep">
            선택된 태그
          </span>
          <button
            type="button"
            className="min-h-11 rounded-pill border border-bean-deep bg-bean px-6 font-bold text-on-bean"
          >
            기본 버튼
          </button>
          <button
            type="button"
            className="min-h-11 rounded-pill border border-line-strong bg-surface px-6 font-semibold"
          >
            보조 버튼
          </button>
        </div>
        <pre className="overflow-x-auto rounded-md bg-code-bg p-4 font-mono text-code text-code-ink">
          git commit -m &quot;KongLog 시작&quot;
        </pre>
      </section>
    </div>
  );
}

async function BackendStatus() {
  const result = await pingBackend();

  if (!result.ok) {
    return (
      <p className="text-danger">
        연결하지 못했어요. 백엔드가 떠 있는지 확인해 주세요.
      </p>
    );
  }

  return (
    <p>
      연결됐어요.
      {result.service !== "" && (
        <span className="text-ink-muted"> 응답한 서비스: {result.service}</span>
      )}
    </p>
  );
}
