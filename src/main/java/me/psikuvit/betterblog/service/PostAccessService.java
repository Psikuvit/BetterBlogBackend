package me.psikuvit.betterblog.service;

import me.psikuvit.betterblog.entity.Post;
import me.psikuvit.betterblog.entity.Post.Visibility;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.exception.ForbiddenException;
import org.springframework.stereotype.Service;

@Service
public class PostAccessService {

    public boolean isUserPrivate(Post post) {
        return post.getVisibility() == Visibility.PRIVATE && post.getMadePrivateBy() == null;
    }

    public boolean isStaffPrivate(Post post) {
        return post.getVisibility() == Visibility.ADMIN_PRIVATE;
    }

    public boolean isAuthor(Post post, User user) {
        return post.getAuthor() != null
                && user != null
                && post.getAuthor().getId().equals(user.getId());
    }

    public void assertCanView(Post post, User viewer) {
        if (post.getVisibility() == Visibility.PUBLIC) {
            return;
        }

        if (viewer == null) {
            throw new ForbiddenException("You don't have permission to view this post");
        }

        if (isAuthor(post, viewer)) {
            return;
        }

        if (isUserPrivate(post)) {
            throw new ForbiddenException("You don't have permission to view this post");
        }

        if (isStaffPrivate(post)) {
            if (viewer.getRole() == User.Role.ADMIN) {
                return;
            }
            if (viewer.getRole() == User.Role.MODERATOR
                    && post.getMadePrivateByRole() == User.Role.MODERATOR) {
                return;
            }
            throw new ForbiddenException("You don't have permission to view this post");
        }

        throw new ForbiddenException("You don't have permission to view this post");
    }

    public void assertCanEdit(Post post, User editor) {
        if (editor.getRole() == User.Role.ADMIN) {
            if (post.getVisibility() == Visibility.PUBLIC || isStaffPrivate(post)) {
                return;
            }
            throw new ForbiddenException("Admins cannot edit user-private posts");
        }

        if (editor.getRole() == User.Role.MODERATOR) {
            if (post.getVisibility() == Visibility.PUBLIC) {
                return;
            }
            if (isStaffPrivate(post) && post.getMadePrivateByRole() == User.Role.MODERATOR) {
                return;
            }
            throw new ForbiddenException("You don't have permission to edit this post");
        }

        if (isAuthor(post, editor)) {
            if (isStaffPrivate(post)) {
                throw new ForbiddenException("This post was made private by staff and cannot be edited by the author");
            }
            return;
        }

        throw new ForbiddenException("You don't have permission to edit this post");
    }

    public void assertCanDelete(Post post, User user) {
        if (user.getRole() == User.Role.ADMIN) {
            if (isUserPrivate(post)) {
                throw new ForbiddenException("Admins cannot delete user-private posts through moderation");
            }
            if (post.getVisibility() == Visibility.PUBLIC || isStaffPrivate(post)) {
                return;
            }
        }

        if (isAuthor(post, user) && !isStaffPrivate(post)) {
            return;
        }

        throw new ForbiddenException("You don't have permission to delete this post");
    }

    public Visibility resolveVisibilityForUpdate(Post post, String requestedVisibility, User editor) {
        Visibility target;
        try {
            target = Visibility.valueOf(requestedVisibility.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new me.psikuvit.betterblog.exception.BadRequestException(
                    "Invalid visibility value: " + requestedVisibility);
        }

        if (editor.getRole() == User.Role.USER) {
            if (target == Visibility.ADMIN_PRIVATE) {
                throw new ForbiddenException("You cannot set staff-private visibility");
            }
            if (isStaffPrivate(post)) {
                throw new ForbiddenException("This post was made private by staff");
            }
            return target;
        }

        if (editor.getRole() == User.Role.MODERATOR) {
            if (target == Visibility.ADMIN_PRIVATE || (target == Visibility.PRIVATE && post.getVisibility() == Visibility.PUBLIC)) {
                return Visibility.ADMIN_PRIVATE;
            }
            if (target == Visibility.PUBLIC && isStaffPrivate(post)) {
                if (post.getMadePrivateByRole() != User.Role.MODERATOR) {
                    throw new ForbiddenException("Moderators can only publish posts they privatized");
                }
                return Visibility.PUBLIC;
            }
            if (target == Visibility.PUBLIC && post.getVisibility() == Visibility.PUBLIC) {
                return Visibility.PUBLIC;
            }
            throw new ForbiddenException("Moderators cannot change visibility this way");
        }

        if (editor.getRole() == User.Role.ADMIN) {
            if (isUserPrivate(post)) {
                throw new ForbiddenException("Admins cannot change visibility on user-private posts");
            }
            if (target == Visibility.PRIVATE) {
                return Visibility.ADMIN_PRIVATE;
            }
            return target;
        }

        return target;
    }

    public void applyStaffPrivateMetadata(Post post, User staff) {
        post.setVisibility(Visibility.ADMIN_PRIVATE);
        post.setMadePrivateBy(staff.getUsername());
        post.setMadePrivateByRole(staff.getRole());
        post.setMadePrivateAt(java.time.LocalDateTime.now());
        post.setPublic(false);
    }

    public void clearStaffPrivateMetadata(Post post) {
        post.setMadePrivateBy(null);
        post.setMadePrivateByRole(null);
        post.setMadePrivateAt(null);
    }

    public void applyUserPrivateMetadata(Post post) {
        post.setVisibility(Visibility.PRIVATE);
        clearStaffPrivateMetadata(post);
        post.setPublic(false);
    }
}
