package me.psikuvit.betterblog.repository;

import me.psikuvit.betterblog.entity.Post;
import me.psikuvit.betterblog.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    Optional<Post> findBySlug(String slug);
    Page<Post> findByAuthor(User author, Pageable pageable);
    Page<Post> findByVisibility(Post.Visibility visibility, Pageable pageable);
    long countByVisibility(Post.Visibility visibility);

    @Query("{ $or: [ { 'visibility': 'PUBLIC' }, { $and: [ { 'visibility': 'PRIVATE' }, { 'author': ?0 } ] } ] }")
    Page<Post> findAccessiblePosts(User author, Pageable pageable);

    @Query("{ $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'content': { $regex: ?0, $options: 'i' } } ] }")
    Page<Post> searchPosts(String query, Pageable pageable);

    @Query("{ 'tags': { $in: [?0] } }")
    Page<Post> findByTag(String tag, Pageable pageable);

    Page<Post> findByAuthorAndVisibility(User author, Post.Visibility visibility, Pageable pageable);

    long countByAuthor(User author);

    @Query("{ $or: [ { 'visibility': 'PUBLIC' }, { 'visibility': 'ADMIN_PRIVATE' } ] }")
    Page<Post> findAdminAccessiblePosts(Pageable pageable);

    @Query("{ 'visibility': 'PUBLIC', $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'content': { $regex: ?0, $options: 'i' } } ] }")
    Page<Post> searchPublicPosts(String query, Pageable pageable);

    @Query("{ 'visibility': 'PUBLIC', 'tags': { $in: [?0] } }")
    Page<Post> findPublicPostsByTag(String tag, Pageable pageable);
}

