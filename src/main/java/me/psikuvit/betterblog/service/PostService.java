package me.psikuvit.betterblog.service;

import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.dto.PostRequest;
import me.psikuvit.betterblog.entity.Post;
import me.psikuvit.betterblog.entity.Post.Visibility;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.exception.ResourceNotFoundException;
import me.psikuvit.betterblog.exception.BadRequestException;
import me.psikuvit.betterblog.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ActivityLogService activityLogService;

    public Post createPost(PostRequest request, User author) {
        if (postRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new BadRequestException("Post with this slug already exists");
        }

        Post post = Post.builder()
                .title(request.getTitle())
                .slug(request.getSlug())
                .excerpt(request.getExcerpt())
                .content(request.getContent())
                .tags(request.getTags())
                .visibility(Visibility.valueOf(request.getVisibility()))
                .coverImageUrl(request.getCoverImageUrl())
                .sourceUrl(request.getSourceUrl())
                .sourcePreviewTitle(request.getSourcePreviewTitle())
                .sourcePreviewDescription(request.getSourcePreviewDescription())
                .sourcePreviewImage(request.getSourcePreviewImage())
                .originalAuthor(request.getOriginalAuthor())
                .legacyId(request.getLegacyId())
                .author(author)
                .isPublic(request.getVisibility().equals("PUBLIC"))
                .build();

        post = postRepository.save(post);
        activityLogService.logActivity(author, "POST_CREATED", "Post", post.getId(), post.getTitle());
        return post;
    }

    public Post updatePost(String postId, PostRequest request, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getAuthor().getId().equals(user.getId()) && !user.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("You don't have permission to edit this post");
        }

        post.setTitle(request.getTitle());
        post.setSlug(request.getSlug());
        post.setExcerpt(request.getExcerpt());
        post.setContent(request.getContent());
        post.setTags(request.getTags());
        post.setVisibility(Visibility.valueOf(request.getVisibility()));
        post.setCoverImageUrl(request.getCoverImageUrl());
        post.setPublic(request.getVisibility().equals("PUBLIC"));

        post = postRepository.save(post);
        activityLogService.logActivity(user, "POST_UPDATED", "Post", post.getId(), post.getTitle());
        return post;
    }

    public void deletePost(String postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getAuthor().getId().equals(user.getId()) && !user.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("You don't have permission to delete this post");
        }

        postRepository.delete(post);
        activityLogService.logActivity(user, "POST_DELETED", "Post", post.getId(), post.getTitle());
    }

    public Post getPost(String postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    public Page<Post> getPublicPosts(Pageable pageable) {
        return postRepository.findByVisibility(Visibility.PUBLIC, pageable);
    }

    public Page<Post> getPostsByVisibility(Visibility visibility, Pageable pageable) {
        return postRepository.findByVisibility(visibility, pageable);
    }

    public Page<Post> getPostsByTag(String tag, Pageable pageable) {
        return postRepository.findByTag(tag, pageable);
    }

    public Page<Post> searchPosts(String query, Pageable pageable) {
        return postRepository.searchPosts(query, pageable);
    }

    public Page<Post> getUserPosts(User user, Pageable pageable) {
        return postRepository.findByAuthor(user, pageable);
    }
}


