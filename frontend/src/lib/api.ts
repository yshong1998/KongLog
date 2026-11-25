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
 * - size: 받을 개수(1부터 20까지). 넣지 않으면 백엔드 기본값인 20개다.
 * - tag: 이 태그가 붙은 글만 받는다. 태그 이름 그대로 넣는다.
 * - cursor: 앞선 응답의 nextCursor. 그다음 글부터 받는다. 이어 받을 때는 tag와 size를 처음과 같게 넣는다.
 */
export async function getPosts({
  size,
  tag,
  cursor,
}: { size?: number; tag?: string; cursor?: string } = {}): Promise<
  ApiResult<PostList>
> {
  // 태그 이름에 한글이나 띄어쓰기가 있을 수 있다. URLSearchParams가 인코딩해 준다.
  const params = new URLSearchParams();
  if (size !== undefined) {
    params.set("size", String(size));
  }
  if (tag !== undefined) {
    params.set("tag", tag);
  }
  if (cursor !== undefined) {
    params.set("cursor", cursor);
  }
  const query = params.size > 0 ? `?${params}` : "";
  const body = await getJson(`/api/posts${query}`);
  return isPostList(body) ? { ok: true, data: body } : { ok: false };
}

/** 글 한 편. 글 상세 화면에서 쓴다. */
export type Post = PostSummary & {
  /** 마크다운 원문 */
  content: string;
  /** 게시한 뒤 본문을 고친 시각. 고친 적이 없으면 null이다. 항상 UTC로 온다. */
  editedAt: string | null;
};

/** 글 한 편을 받은 결과. 없는 글과 초안은 notFound로 구분한다. */
export type PostResult = ApiResult<Post> | { ok: false; notFound: true };

function isPost(value: unknown): value is Post {
  if (!isRecord(value) || !isPostSummary(value)) {
    return false;
  }
  const rest: Record<string, unknown> = value;
  return (
    typeof rest.content === "string" &&
    (rest.editedAt === null ||
      (typeof rest.editedAt === "string" &&
        !Number.isNaN(Date.parse(rest.editedAt))))
  );
}

/**
 * 게시 글 한 편을 슬러그로 받는다.
 * 없는 글과 초안은 백엔드가 똑같이 404로 준다. 슬러그 모양이 잘못돼 400이 와도 없는 글로 본다.
 */
export async function getPost(slug: string): Promise<PostResult> {
  try {
    const response = await fetch(
      apiUrl(`/api/posts/${encodeURIComponent(slug)}`),
      { signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS) },
    );
    if (response.status === 404 || response.status === 400) {
      return { ok: false, notFound: true };
    }
    if (!response.ok) {
      return { ok: false };
    }
    const body: unknown = await response.json();
    return isPost(body) ? { ok: true, data: body } : { ok: false };
  } catch {
    return { ok: false };
  }
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
