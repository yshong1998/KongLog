package my.konglog.post;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 독자가 보는 글을 읽는 조회 전용 리포지토리.
 *
 * <p>여기 있는 모든 조회는 게시 글만 읽는다. 초안은 어떤 조건으로도 나오지 않는다.
 * 태그와 커서는 있을 때만 조건에 넣어야 해서, 조회문을 직접 조립한다.
 */
@Repository
public class PostQueryRepository {

    private static final String HAS_TAG = " and exists (select 1 from p.tags t where t.name = :tagName)";

    private final EntityManager entityManager;

    public PostQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 게시 글을 발행일 최신순으로 읽는다. 발행일이 같으면 id가 큰 글이 먼저다.
     *
     * @param tagName 이 이름의 태그가 붙은 글만 읽는다. null이면 거르지 않는다
     * @param after 이 글 다음부터 읽는다. null이면 처음부터 읽는다
     * @param limit 읽을 최대 개수
     */
    public List<Post> findPublished(String tagName, PostCursor after, int limit) {
        StringBuilder jpql = new StringBuilder("select p from Post p where p.status = :status");
        if (tagName != null) {
            jpql.append(HAS_TAG);
        }
        if (after != null) {
            jpql.append(" and (p.publishedAt < :publishedAt or (p.publishedAt = :publishedAt and p.id < :id))");
        }
        jpql.append(" order by p.publishedAt desc, p.id desc");

        TypedQuery<Post> query = entityManager.createQuery(jpql.toString(), Post.class)
                .setParameter("status", PostStatus.PUBLISHED)
                .setMaxResults(limit);
        if (tagName != null) {
            query.setParameter("tagName", tagName);
        }
        if (after != null) {
            query.setParameter("publishedAt", after.publishedAt());
            query.setParameter("id", after.id());
        }
        return query.getResultList();
    }

    /** 조건에 맞는 게시 글의 전체 수. 커서와 상관없이 센다. */
    public long countPublished(String tagName) {
        StringBuilder jpql = new StringBuilder("select count(p) from Post p where p.status = :status");
        if (tagName != null) {
            jpql.append(HAS_TAG);
        }

        TypedQuery<Long> query = entityManager.createQuery(jpql.toString(), Long.class)
                .setParameter("status", PostStatus.PUBLISHED);
        if (tagName != null) {
            query.setParameter("tagName", tagName);
        }
        return query.getSingleResult();
    }

    /** 슬러그로 게시 글 하나를 태그와 함께 읽는다. 없는 글이거나 초안이면 비어 있다. */
    public Optional<Post> findPublishedBySlug(String slug) {
        return entityManager
                .createQuery(
                        "select p from Post p left join fetch p.tags where p.slug = :slug and p.status = :status",
                        Post.class)
                .setParameter("slug", slug)
                .setParameter("status", PostStatus.PUBLISHED)
                .getResultStream()
                .findFirst();
    }

    /**
     * 글들의 태그를 조회 한 번으로 미리 읽어 둔다.
     * 이렇게 하지 않으면 글마다 태그를 읽는 조회가 한 번씩 더 나간다.
     */
    public void loadTags(List<Post> posts) {
        if (posts.isEmpty()) {
            return;
        }
        entityManager.createQuery("select p from Post p left join fetch p.tags where p in :posts", Post.class)
                .setParameter("posts", posts)
                .getResultList();
    }
}
