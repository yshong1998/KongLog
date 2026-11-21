package my.konglog.post;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import my.konglog.tag.Tag;

/** 블로그 글. 본문은 마크다운 원문으로 저장한다. */
@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA가 쓰는 기본 생성자
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    /** 주소에 쓰는 영문 이름. 중복될 수 없다. */
    private String slug;

    private String summary;

    private String content;

    @Enumerated(EnumType.STRING)
    private PostStatus status;

    /** 발행일. 초안일 때는 null이다. */
    private Instant publishedAt;

    private Instant createdAt;

    /** 어떤 변경이든 바뀌는 시각. */
    private Instant updatedAt;

    /** 게시한 뒤 본문을 고친 시각. 고친 적이 없으면 null이다. 독자에게 "수정일"로 보여 준다. */
    private Instant editedAt;

    @ManyToMany
    @JoinTable(
            name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    private Post(String title, String slug, String summary, String content) {
        this.title = title;
        this.slug = slug;
        this.summary = summary;
        this.content = content;
        this.status = PostStatus.DRAFT;
    }

    /** 새 글은 항상 초안으로 시작한다. */
    public static Post draft(String title, String slug, String summary, String content) {
        return new Post(title, slug, summary, content);
    }

    /** 글을 게시 상태로 바꾸고 발행일을 적는다. */
    public void publish(Instant publishedAt) {
        this.status = PostStatus.PUBLISHED;
        this.publishedAt = publishedAt;
    }

    public void addTag(Tag tag) {
        tags.add(tag);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** 밖에서 고칠 수 없는 Set을 돌려준다. 태그는 addTag로만 붙인다. 그래서 Lombok에 맡기지 않고 직접 쓴다. */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }
}
