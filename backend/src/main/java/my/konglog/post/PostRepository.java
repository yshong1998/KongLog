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
     * <p>태그와 커서는 값이 있을 때만 조건이 된다. "(값 is null or 조건)"은 값이 없으면 앞쪽이 참이라 통과한다.
     * 발행일에만 cast가 붙은 이유: 값이 없는 시각을 그대로 넘기면 PostgreSQL이 타입을 알 수 없다는 오류를 낸다.
     *
     * @param tagName 이 이름의 태그가 붙은 글만 읽는다. null이면 거르지 않는다
     * @param afterPublishedAt 이 발행일의 글 다음부터 읽는다. null이면 처음부터 읽는다
     * @param afterId 발행일이 같은 글 중에서는 이 id 다음부터 읽는다. afterPublishedAt과 함께 넣는다
     * @param limit 읽을 최대 개수
     */
    @Query("""
            select p from Post p
            where p.status = my.konglog.post.PostStatus.PUBLISHED
              and (:tagName is null or exists (select 1 from p.tags t where t.name = :tagName))
              and (cast(:afterPublishedAt as Instant) is null
                   or p.publishedAt < :afterPublishedAt
                   or (p.publishedAt = :afterPublishedAt and p.id < :afterId))
            order by p.publishedAt desc, p.id desc
            """)
    List<Post> findPublished(String tagName, Instant afterPublishedAt, Long afterId, Limit limit);

    /**
     * 조건에 맞는 게시 글의 전체 수. 어디까지 읽었는지와 상관없이 센다.
     *
     * @param tagName 이 이름의 태그가 붙은 글만 센다. null이면 거르지 않는다
     */
    @Query("""
            select count(p) from Post p
            where p.status = my.konglog.post.PostStatus.PUBLISHED
              and (:tagName is null or exists (select 1 from p.tags t where t.name = :tagName))
            """)
    long countPublished(String tagName);

    /**
     * 글들을 태그와 함께 조회 한 번으로 읽는다.
     *
     * <p>이미 읽어 둔 글에 태그를 채우는 데 쓴다. 이렇게 하지 않으면 글마다 태그를 읽는 조회가 한 번씩 더 나간다.
     * 빈 목록으로는 부르지 않는다.
     */
    @Query("select p from Post p left join fetch p.tags where p in :posts")
    List<Post> findWithTags(List<Post> posts);

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
