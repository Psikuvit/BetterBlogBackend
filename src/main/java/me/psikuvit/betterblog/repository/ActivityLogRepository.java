package me.psikuvit.betterblog.repository;

import me.psikuvit.betterblog.entity.ActivityLog;
import me.psikuvit.betterblog.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {
    Page<ActivityLog> findByUser(User user, Pageable pageable);
    Page<ActivityLog> findByUserAndAction(User user, String action, Pageable pageable);
    Page<ActivityLog> findBySeverity(ActivityLog.Severity severity, Pageable pageable);
    Page<ActivityLog> findByAction(String action, Pageable pageable);
}

