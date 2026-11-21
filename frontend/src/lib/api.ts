const DEFAULT_API_BASE_URL = "http://localhost:8080";

/** 백엔드 주소와 경로를 이어 붙인다. 슬래시가 겹치거나 빠져도 하나로 맞춘다. */
export function apiUrl(
  path: string,
  baseUrl: string = process.env.API_BASE_URL ?? DEFAULT_API_BASE_URL,
): string {
  const base = baseUrl.replace(/\/+$/, "");
  const rest = path.replace(/^\/+/, "");
  return `${base}/${rest}`;
}

const REQUEST_TIMEOUT_MS = 5000;

/** 백엔드 호출의 결과. 연결 실패, 오류 응답, 약속과 다른 모양은 모두 ok: false다. */
export type ApiResult<T> = { ok: true; data: T } | { ok: false };

/** 글 목록에 실리는 글 하나. 본문은 들어 있지 않다. */
export type PostSummary = {
  title: string;
  slug: string;
  summary: string;
  /** 태그 이름들. 태그가 없으면 빈 배열이다. */
  tags: string[];
  /** 발행 시각. 항상 UTC로 온다. 예: "2026-11-18T00:00:00Z" */
  publishedAt: string;
};

export type PostList = {
  posts: PostSummary[];
  /** 조건에 맞는 게시 글의 전체 수. 이번에 받은 개수가 아니다. */
  totalCount: number;
  /** 이어서 받을 때 넣는 값. 더 받을 글이 없으면 null이다. */
  nextCursor: string | null;
};

export type Tag = {
  name: string;
  /** 이 태그가 붙은 게시 글 수. */
  postCount: number;
};

/** JSON 응답을 읽는다. 연결하지 못했거나 오류 응답이면 undefined를 돌려준다. */
async function getJson(path: string): Promise<unknown> {
  try {
    const response = await fetch(apiUrl(path), {
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    });
    if (!response.ok) {
      return undefined;
    }
    return await response.json();
  } catch {
    return undefined;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isPostSummary(value: unknown): value is PostSummary {
  return (
    isRecord(value) &&
    typeof value.title === "string" &&
    typeof value.slug === "string" &&
    typeof value.summary === "string" &&
    Array.isArray(value.tags) &&
    value.tags.every((tag) => typeof tag === "string") &&
    typeof value.publishedAt === "string" &&
    !Number.isNaN(Date.parse(value.publishedAt))
  );
}

function isPostList(value: unknown): value is PostList {
  return (
    isRecord(value) &&
    Array.isArray(value.posts) &&
    value.posts.every(isPostSummary) &&
    typeof value.totalCount === "number" &&
    (value.nextCursor === null || typeof value.nextCursor === "string")
  );
}

function isTag(value: unknown): value is Tag {
  return (
    isRecord(value) &&
    typeof value.name === "string" &&
    typeof value.postCount === "number"
  );
}

/**
 * 게시 글 목록을 발행일 최신순으로 받는다.
 * size는 받을 개수(1부터 20까지)이고, 넣지 않으면 백엔드 기본값인 20개다.
 */
export async function getPosts({
  size,
}: { size?: number } = {}): Promise<ApiResult<PostList>> {
  const query = size === undefined ? "" : `?size=${size}`;
  const body = await getJson(`/api/posts${query}`);
  return isPostList(body) ? { ok: true, data: body } : { ok: false };
}

/** 게시 글이 있는 태그를 글이 많은 순으로 받는다. */
export async function getTags(): Promise<ApiResult<Tag[]>> {
  const body = await getJson("/api/tags");
  if (
    !isRecord(body) ||
    !Array.isArray(body.tags) ||
    !body.tags.every(isTag)
  ) {
    return { ok: false };
  }
  return { ok: true, data: body.tags };
}
