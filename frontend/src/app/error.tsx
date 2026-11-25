"use client";

/**
 * 화면을 그리다 오류가 났을 때 보이는 화면. 백엔드에 연결하지 못했을 때가 대표적이다.
 * 오류 경계는 클라이언트 컴포넌트여야 한다. 헤더와 푸터(루트 레이아웃)는 그대로 남는다.
 *
 * retry()는 이 화면 안쪽을 서버에서 다시 받아 그린다. 성공하면 원래 화면으로 바뀐다.
 */
export default function ErrorPage({ retry }: { retry: () => void }) {
  return (
    <div
      role="alert"
      className="mx-auto flex max-w-content flex-col gap-6 pt-16"
    >
      <div className="flex flex-col gap-2">
        <h1 className="text-h1">잠시 문제가 생겼어요</h1>
        <p className="text-ink-muted">
          내용을 불러오지 못했어요. 잠시 뒤에 다시 시도해 주세요.
        </p>
      </div>
      <div className="flex">
        <button
          type="button"
          onClick={() => retry()}
          className="flex min-h-11 items-center rounded-pill bg-bean px-6 text-small font-semibold text-on-bean"
        >
          다시 시도
        </button>
      </div>
    </div>
  );
}
