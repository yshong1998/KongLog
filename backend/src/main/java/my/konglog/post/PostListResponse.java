package my.konglog.post;

import java.util.List;

/**
 * 글 목록 응답.
 *
 * @param posts 글들. 조건에 맞는 글이 없으면 빈 목록이다
 * @param totalCount 조건(태그)에 맞는 게시 글의 전체 수. 이번에 내려준 개수가 아니다
 * @param nextCursor 다음 글을 받을 때 cursor로 넘길 값. 더 받을 글이 없으면 null이다
 */
public record PostListResponse(List<PostSummaryResponse> posts, long totalCount, String nextCursor) {
}
