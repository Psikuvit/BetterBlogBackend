package me.psikuvit.betterblog.service;

import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.dto.PostRequest;
import me.psikuvit.betterblog.entity.Post;
import me.psikuvit.betterblog.entity.Post.Visibility;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.exception.ResourceNotFoundException;
import me.psikuvit.betterblog.exception.BadRequestException;
import me.psikuvit.betterblog.exception.ForbiddenException;
import me.psikuvit.betterblog.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ActivityLogService activityLogService;
    private final LinkPreviewService linkPreviewService;
    private final PostAccessService postAccessService;
    private final AppSettingsService appSettingsService;

    public Post createPost(PostRequest request, User author) {
        long postCount = postRepository.countByAuthor(author);
        int maxPosts = appSettingsService.getMaxPostsPerUser();
        if (postCount >= maxPosts) {
            throw new BadRequestException("Maximum posts per user limit reached (" + maxPosts + ")");
        }

        if (postRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new BadRequestException("Post with this slug already exists");
        }

        Visibility visibility = parseVisibility(request.getVisibility());
        if (author.getRole() == User.Role.USER && visibility == Visibility.ADMIN_PRIVATE) {
            throw new ForbiddenException("You cannot create staff-private posts");
        }

        LinkPreviewData preview = resolvePreview(request);
        LocalDateTime now = LocalDateTime.now();

        Post post = Post.builder()
                .title(request.getTitle())
                .slug(request.getSlug())
                .excerpt(request.getExcerpt())
                .content(request.getContent())
                .tags(request.getTags())
                .visibility(visibility)
                .coverImageUrl(request.getCoverImageUrl())
                .sourceUrl(request.getSourceUrl())
                .sourcePreviewTitle(preview.title())
                .sourcePreviewDescription(preview.description())
                .sourcePreviewImage(preview.image())
                .originalAuthor(request.getOriginalAuthor())
                .legacyId(request.getLegacyId())
                .author(author)
                .isPublic(visibility == Visibility.PUBLIC)
                .createdAt(now)
                .updatedAt(now)
                .importedAt(request.getLegacyId() != null ? now : null)
                .publishedAt(visibility == Visibility.PUBLIC ? now : null)
                .build();

        applyVisibilityMetadata(post, visibility, author);

        post = postRepository.save(post);
        activityLogService.logActivity(author, "POST_CREATED", "Post", post.getId(), post.getTitle());
        return post;
    }

    public Post updatePost(String postId, PostRequest request, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        postAccessService.assertCanEdit(post, user);

        Visibility targetVisibility = postAccessService.resolveVisibilityForUpdate(
                post, request.getVisibility(), user);

        post.setTitle(request.getTitle());
        post.setSlug(request.getSlug());
        post.setExcerpt(request.getExcerpt());
        post.setContent(request.getContent());
        post.setTags(request.getTags());
        post.setCoverImageUrl(request.getCoverImageUrl());
        post.setSourceUrl(request.getSourceUrl());

        LinkPreviewData preview = resolvePreview(request);
        post.setSourcePreviewTitle(preview.title());
        post.setSourcePreviewDescription(preview.description());
        post.setSourcePreviewImage(preview.image());
        post.setOriginalAuthor(request.getOriginalAuthor());
        post.setLegacyId(request.getLegacyId());

        applyVisibilityChange(post, targetVisibility, user);

        LocalDateTime now = LocalDateTime.now();
        post.setUpdatedAt(now);
        if (post.getVisibility() == Visibility.PUBLIC && post.getPublishedAt() == null) {
            post.setPublishedAt(now);
        }

        post = postRepository.save(post);
        activityLogService.logActivity(user, "POST_UPDATED", "Post", post.getId(), post.getTitle());
        return post;
    }

    public void deletePost(String postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        postAccessService.assertCanDelete(post, user);

        postRepository.delete(post);
        activityLogService.logActivity(user, "POST_DELETED", "Post", post.getId(), post.getTitle());
    }

    public Post getPost(String postId, User viewer) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        postAccessService.assertCanView(post, viewer);
        return post;
    }

    public Page<Post> getPublicPosts(Pageable pageable) {
        return postRepository.findByVisibility(Visibility.PUBLIC, pageable);
    }

    public long countByVisibility(Visibility visibility) {
        return postRepository.countByVisibility(visibility);
    }

    public Page<Post> getPostsByVisibility(Visibility visibility, Pageable pageable) {
        return postRepository.findByVisibility(visibility, pageable);
    }

    public Page<Post> getPostsByTag(String tag, Pageable pageable) {
        return postRepository.findPublicPostsByTag(tag, pageable);
    }

    public Page<Post> searchPosts(String query, Pageable pageable) {
        return postRepository.searchPublicPosts(query, pageable);
    }

    public Page<Post> getUserPosts(User profileUser, User viewer, Pageable pageable) {
        if (viewer != null && profileUser.getId().equals(viewer.getId())) {
            return postRepository.findByAuthor(profileUser, pageable);
        }
        return postRepository.findByAuthorAndVisibility(profileUser, Visibility.PUBLIC, pageable);
    }

    public Page<Post> getAdminAccessiblePosts(Pageable pageable) {
        return postRepository.findAdminAccessiblePosts(pageable);
    }

    private void applyVisibilityChange(Post post, Visibility targetVisibility, User editor) {
        Visibility previous = post.getVisibility();

        if (targetVisibility == Visibility.ADMIN_PRIVATE
                && (editor.getRole() == User.Role.ADMIN || editor.getRole() == User.Role.MODERATOR)
                && previous == Visibility.PUBLIC) {
            postAccessService.applyStaffPrivateMetadata(post, editor);
            return;
        }

        if (targetVisibility == Visibility.PUBLIC) {
            post.setVisibility(Visibility.PUBLIC);
            post.setPublic(true);
            postAccessService.clearStaffPrivateMetadata(post);
            return;
        }

        if (targetVisibility == Visibility.PRIVATE && editor.getRole() == User.Role.USER) {
            postAccessService.applyUserPrivateMetadata(post);
            return;
        }

        post.setVisibility(targetVisibility);
        post.setPublic(targetVisibility == Visibility.PUBLIC);
        if (targetVisibility != Visibility.ADMIN_PRIVATE) {
            postAccessService.clearStaffPrivateMetadata(post);
        }
    }

    private void applyVisibilityMetadata(Post post, Visibility visibility, User author) {
        if (visibility == Visibility.ADMIN_PRIVATE
                && (author.getRole() == User.Role.ADMIN || author.getRole() == User.Role.MODERATOR)) {
            postAccessService.applyStaffPrivateMetadata(post, author);
        } else if (visibility == Visibility.PRIVATE) {
            postAccessService.applyUserPrivateMetadata(post);
        }
    }

    private Visibility parseVisibility(String value) {
        try {
            return Visibility.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid visibility value: " + value);
        }
    }

    private LinkPreviewData resolvePreview(PostRequest request) {
        if (request.getSourceUrl() == null || request.getSourceUrl().isBlank()) {
            return new LinkPreviewData(
                    request.getSourcePreviewTitle(),
                    request.getSourcePreviewDescription(),
                    request.getSourcePreviewImage());
        }

        var preview = linkPreviewService.fetchPreview(request.getSourceUrl());
        return new LinkPreviewData(preview.getTitle(), preview.getDescription(), preview.getImage());
    }

    private record LinkPreviewData(String title, String description, String image) { }
}
