package me.psikuvit.betterblog.service;

import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.entity.ActivityLog;
import me.psikuvit.betterblog.entity.ActivityLog.Severity;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.repository.ActivityLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public void logActivity(User user, String action, String resourceType, String resourceId, String resourceName) {
        logActivity(user, action, resourceType, resourceId, resourceName, null, Severity.INFO);
    }

    public void logActivity(User user, String action, String resourceType, String resourceId, String resourceName, String details, Severity severity) {
        ActivityLog log = ActivityLog.builder()
                .user(user)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .resourceName(resourceName)
                .details(details)
                .severity(severity)
                .build();

        activityLogRepository.save(log);
    }

    public Page<ActivityLog> getUserActivity(User user, Pageable pageable) {
        return activityLogRepository.findByUser(user, pageable);
    }

    public Page<ActivityLog> getUserActivityByAction(User user, String action, Pageable pageable) {
        return activityLogRepository.findByUserAndAction(user, action, pageable);
    }

    public Page<ActivityLog> getAllActivity(Pageable pageable) {
        return activityLogRepository.findAll(pageable);
    }

    public Page<ActivityLog> getActivityBySeverity(Severity severity, Pageable pageable) {
        return activityLogRepository.findBySeverity(severity, pageable);
    }

    public Page<ActivityLog> getActivityByAction(String action, Pageable pageable) {
        return activityLogRepository.findByAction(action, pageable);
    }
}

