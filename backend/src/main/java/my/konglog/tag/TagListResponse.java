package my.konglog.tag;

import java.util.List;

/**
 * 태그 목록 응답.
 *
 * @param tags 게시 글이 있는 태그들. 글이 많은 순이고, 글 수가 같으면 이름순이다. 없으면 빈 목록이다
 */
public record TagListResponse(List<TagResponse> tags) {
}
