package me.psikuvit.betterblog.repository;

import me.psikuvit.betterblog.entity.PostReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostReportRepository extends MongoRepository<PostReport, String> {
    Page<PostReport> findByStatus(PostReport.Status status, Pageable pageable);
}
