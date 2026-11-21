package my.konglog.post;

import jakarta.persistence.Column;
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
import my.konglog.tag.Tag;

/** 블로그 글. 본문은 마크다운 원문으로 저장한다. */
@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    /** 주소에 쓰는 영문 이름. 중복될 수 없다. */
    @Column(nullable = false, length = 200)
    private String slug;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 무엇이든 바뀌면 갱신된다. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** 게시한 뒤 본문을 고쳤을 때만 채워진다. 독자에게 보이는 "수정일"이다. */
    @Column(name = "edited_at")
    private Instant editedAt;

    @ManyToMany
    @JoinTable(
            name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    protected Post() {
        // JPA가 쓰는 생성자
    }

    private Post(String title, String slug, String summary, String body) {
        this.title = title;
        this.slug = slug;
        this.summary = summary;
        this.body = body;
        this.status = PostStatus.DRAFT;
    }

    /** 새 글은 항상 초안으로 시작한다. */
    public static Post draft(String title, String slug, String summary, String body) {
        return new Post(title, slug, summary, body);
    }

    /** 글을 게시한다. 이미 게시된 글의 발행일은 바꾸지 않는다. */
    public void publish(Instant now) {
        if (status == PostStatus.PUBLISHED) {
            return;
        }
        this.status = PostStatus.PUBLISHED;
        this.publishedAt = now;
    }

    public void addTag(Tag tag) {
        tags.add(tag);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getSummary() {
        return summary;
    }

    public String getBody() {
        return body;
    }

    public PostStatus getStatus() {
        return status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }
}
