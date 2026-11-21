package my.konglog.tag;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * 게시 글이 하나라도 붙은 태그와, 태그마다 붙은 게시 글 수를 읽는다.
     *
     * <p>글에서 출발해 태그로 묶는다. 그래서 글이 없는 태그와 초안에만 붙은 태그는 처음부터 나오지 않는다.
     * 순서는 정하지 않는다. 정렬은 부르는 쪽에서 한다.
     */
    @Query("""
            select new my.konglog.tag.TagResponse(t.name, count(p))
            from Post p join p.tags t
            where p.status = my.konglog.post.PostStatus.PUBLISHED
            group by t.name
            """)
    List<TagResponse> findWithPublishedPostCount();
}
