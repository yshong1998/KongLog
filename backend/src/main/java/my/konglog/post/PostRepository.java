package my.konglog.post;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 글 저장소.
 *
 * <p>독자가 보는 조회(이름에 Published가 든 것)는 게시 글만 읽는다. 초안은 어떤 조건으로도 나오지 않는다.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 게시 글을 발행일 최신순으로 읽는다. 발행일이 같으면 id가 큰 글이 먼저다.
     *
     * <p>목록에 필요한 칸만 읽는다. 본문은 읽지 않는다.
     *
     * <p>태그와 커서는 값이 있을 때만 조건이 된다. "(값 is null or 조건)"은 값이 없으면 앞쪽이 참이라 통과한다.
     * 발행일에만 cast가 붙은 이유: 값이 없는 시각을 그대로 넘기면 PostgreSQL이 타입을 알 수 없다는 오류를 낸다.
     *
     * @param tagName 이 이름의 태그가 붙은 글만 읽는다. 대소문자는 구분하지 않는다. null이면 거르지 않는다
     * @param afterPublishedAt 이 발행일의 글 다음부터 읽는다. null이면 처음부터 읽는다
     * @param afterId 발행일이 같은 글 중에서는 이 id 다음부터 읽는다. afterPublishedAt과 함께 넣는다
     * @param limit 읽을 최대 개수
     */
    @Query("""
            select new my.konglog.post.PostSummaryRow(p.id, p.title, p.slug, p.summary, p.publishedAt)
            from Post p
            where p.status = my.konglog.post.PostStatus.PUBLISHED
              and (:tagName is null or exists (select 1 from p.tags t where lower(t.name) = lower(cast(:tagName as String))))
              and (cast(:afterPublishedAt as Instant) is null
                   or p.publishedAt < :afterPublishedAt
                   or (p.publishedAt = :afterPublishedAt and p.id < :afterId))
            order by p.publishedAt desc, p.id desc
            """)
    List<PostSummaryRow> findPublished(String tagName, Instant afterPublishedAt, Long afterId, Limit limit);

    /**
     * 조건에 맞는 게시 글의 전체 수. 어디까지 읽었는지와 상관없이 센다.
     *
     * @param tagName 이 이름의 태그가 붙은 글만 센다. null이면 거르지 않는다
     */
    @Query("""
            select count(p) from Post p
            where p.status = my.konglog.post.PostStatus.PUBLISHED
              and (:tagName is null or exists (select 1 from p.tags t where lower(t.name) = lower(cast(:tagName as String))))
            """)
    long countPublished(String tagName);

    /**
     * 글들에 붙은 태그 이름을 조회 한 번으로 읽는다.
     *
     * <p>글마다 따로 읽으면 글 수만큼 조회가 더 나간다. 태그가 없는 글은 결과에 없다. 빈 목록으로는 부르지 않는다.
     */
    @Query("""
            select new my.konglog.post.PostTagName(p.id, t.name)
            from Post p join p.tags t
            where p.id in :postIds
            """)
    List<PostTagName> findTagNames(List<Long> postIds);

    /**
     * 슬러그로 게시 글 하나를 태그와 함께 읽는다.
     *
     * <p>없는 글이거나 초안이면 비어 있다. 부르는 쪽에서는 둘을 구분할 수 없다.
     */
    @Query("""
            select p from Post p left join fetch p.tags
            where p.slug = :slug
              and p.status = my.konglog.post.PostStatus.PUBLISHED
            """)
    Optional<Post> findPublishedBySlug(String slug);
}
