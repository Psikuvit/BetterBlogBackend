package me.psikuvit.betterblog.repository;

import me.psikuvit.betterblog.entity.AppSettings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppSettingsRepository extends MongoRepository<AppSettings, String> {
}
