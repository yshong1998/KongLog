package my.konglog.tag;

import java.util.List;

/** 태그 목록 응답. 게시 글이 있는 태그가 없으면 빈 목록이다. */
public record TagListResponse(List<TagCountResponse> tags) {
}
