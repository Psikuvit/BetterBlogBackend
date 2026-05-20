package me.psikuvit.betterblog.repository;

import me.psikuvit.betterblog.entity.ShareLink;
import me.psikuvit.betterblog.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ShareLinkRepository extends MongoRepository<ShareLink, String> {
    Optional<ShareLink> findByToken(String token);
    Page<ShareLink> findByUser(User user, Pageable pageable);
    void deleteByExpiresAtBefore(LocalDateTime expiresAt);
}

