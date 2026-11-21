package my.konglog.post;

/** 글의 상태. 독자에게는 PUBLISHED인 글만 보인다. */
public enum PostStatus {
    /** 초안 */
    DRAFT,
    /** 게시 */
    PUBLISHED
}
