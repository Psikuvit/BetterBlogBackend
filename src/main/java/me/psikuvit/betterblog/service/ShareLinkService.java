package me.psikuvit.betterblog.service;

import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.dto.ShareLinkRequest;
import me.psikuvit.betterblog.entity.Post;
import me.psikuvit.betterblog.entity.ShareLink;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.exception.ResourceNotFoundException;
import me.psikuvit.betterblog.exception.BadRequestException;
import me.psikuvit.betterblog.repository.PostRepository;
import me.psikuvit.betterblog.repository.ShareLinkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final PostRepository postRepository;
    private final ActivityLogService activityLogService;

    public ShareLink createShareLink(ShareLinkRequest request, User user) {
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new BadRequestException("You can only create links for your own posts");
        }

        LocalDateTime expiresAt = calculateExpiry(request.getExpiresIn());
        String token = UUID.randomUUID().toString();

        ShareLink link = ShareLink.builder()
                .post(post)
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .maxAccess(request.getMaxAccess())
                .createdAt(LocalDateTime.now())
                .build();

        link = shareLinkRepository.save(link);
        activityLogService.logActivity(user, "SHARING_LINK_CREATED", "ShareLink", link.getId(), post.getTitle());
        return link;
    }

    public void revokeShareLink(String linkId, User user) {
        ShareLink link = shareLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));

        if (!link.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only revoke your own links");
        }

        shareLinkRepository.delete(link);
        activityLogService.logActivity(user, "SHARING_LINK_REVOKED", "ShareLink", linkId, link.getPost().getTitle());
    }

    public Post accessViaLink(String token) {
        ShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired link"));

        if (link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Link has expired");
        }

        if (link.getMaxAccess() != null && link.getAccessCount() >= link.getMaxAccess()) {
            throw new ResourceNotFoundException("Link access count exceeded");
        }

        link.setAccessCount(link.getAccessCount() + 1);
        shareLinkRepository.save(link);

        return link.getPost();
    }

    public Page<ShareLink> getUserShareLinks(User user, Pageable pageable) {
        return shareLinkRepository.findByUser(user, pageable);
    }

    private LocalDateTime calculateExpiry(String expiresIn) {
        return switch (expiresIn) {
            case "1h" -> LocalDateTime.now().plusHours(1);
            case "1d" -> LocalDateTime.now().plusDays(1);
            case "30d" -> LocalDateTime.now().plusDays(30);
            case "90d" -> LocalDateTime.now().plusDays(90);
            default -> LocalDateTime.now().plusDays(7);
        };
    }
}

