package my.konglog.tag;

import jakarta.persistence.EntityManager;
import java.util.List;
import my.konglog.post.PostStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** 독자가 보는 태그를 읽는 조회 전용 리포지토리. */
@Repository
@Transactional(readOnly = true)
public class TagQueryRepository {

    private final EntityManager entityManager;

    public TagQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 게시 글이 하나라도 있는 태그를 글이 많은 순으로 읽는다. 글 수가 같으면 이름순이다.
     * 게시 글에서 출발해 태그로 묶기 때문에, 초안에만 붙은 태그와 안 쓰는 태그는 나오지 않는다.
     */
    public List<TagCountResponse> findTagsWithPublishedPostCount() {
        return entityManager
                .createQuery(
                        """
                        select new my.konglog.tag.TagCountResponse(t.name, count(p))
                        from Post p join p.tags t
                        where p.status = :status
                        group by t.id, t.name
                        order by count(p) desc, t.name asc
                        """,
                        TagCountResponse.class)
                .setParameter("status", PostStatus.PUBLISHED)
                .getResultList();
    }
}
