package me.psikuvit.betterblog.repository;

import me.psikuvit.betterblog.entity.ApiToken;
import me.psikuvit.betterblog.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiTokenRepository extends MongoRepository<ApiToken, String> {
    Optional<ApiToken> findByToken(String token);
    List<ApiToken> findByUser(User user);
    void deleteByExpiresAtBefore(java.time.LocalDateTime expiresAt);
}

