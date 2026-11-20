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

export type PingResult =
  | { ok: true; service: string }
  | { ok: false };

/** 백엔드가 떠 있는지 확인한다. 연결하지 못하면 오류를 던지지 않고 ok: false를 돌려준다. */
export async function pingBackend(): Promise<PingResult> {
  try {
    const response = await fetch(apiUrl("/api/ping"), {
      signal: AbortSignal.timeout(2000),
    });
    if (!response.ok) {
      return { ok: false };
    }
    const body: unknown = await response.json();
    const service =
      typeof body === "object" &&
      body !== null &&
      "service" in body &&
      typeof body.service === "string"
        ? body.service
        : "";
    return { ok: true, service };
  } catch {
    return { ok: false };
  }
}
