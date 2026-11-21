import type { NextRequest } from "next/server";
import { getPosts } from "@/lib/api";

/*
 * 글 목록의 다음 묶음을 JSON으로 내려 준다. 글 목록 화면의 "더 보기"가 브라우저에서 부른다.
 * 브라우저는 백엔드 주소를 모르므로, 이 주소가 대신 백엔드에서 받아 와 그대로 돌려준다.
 * 처음 20개는 화면을 만들 때 서버에서 읽으므로 여기서는 이어 받기(cursor)만 받는다.
 */
export async function GET(request: NextRequest) {
  const params = request.nextUrl.searchParams;
  const cursor = params.get("cursor");
  if (!cursor) {
    return Response.json({ message: "cursor가 필요해요" }, { status: 400 });
  }

  const result = await getPosts({
    tag: params.get("tag") || undefined,
    cursor,
  });
  if (!result.ok) {
    // 백엔드에 닿지 못했거나 백엔드가 오류를 돌려준 경우다.
    return Response.json(
      { message: "글을 불러오지 못했어요" },
      { status: 502 },
    );
  }
  return Response.json(result.data);
}
