package me.psikuvit.betterblog.service;

import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.dto.AppSettingsDto;
import me.psikuvit.betterblog.dto.UpdateAppSettingsRequest;
import me.psikuvit.betterblog.entity.AppSettings;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.exception.BadRequestException;
import me.psikuvit.betterblog.repository.AppSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class AppSettingsService {

    private final AppSettingsRepository appSettingsRepository;

    public AppSettingsDto getSettings() {
        return toDto(getOrCreateSettings());
    }

    public AppSettingsDto updateSettings(UpdateAppSettingsRequest request, User admin) {
        if (request.getMaxPostsPerUser() == null) {
            throw new BadRequestException("maxPostsPerUser is required");
        }

        AppSettings settings = getOrCreateSettings();
        settings.setMaxPostsPerUser(request.getMaxPostsPerUser());
        settings.setUpdatedAt(LocalDateTime.now());
        settings.setUpdatedBy(admin.getUsername());
        settings = appSettingsRepository.save(settings);
        return toDto(settings);
    }

    public int getMaxPostsPerUser() {
        return getOrCreateSettings().getMaxPostsPerUser();
    }

    private AppSettings getOrCreateSettings() {
        return appSettingsRepository.findById(AppSettings.GLOBAL_ID)
                .orElseGet(() -> appSettingsRepository.save(AppSettings.builder()
                        .id(AppSettings.GLOBAL_ID)
                        .maxPostsPerUser(50)
                        .build()));
    }

    private AppSettingsDto toDto(AppSettings settings) {
        return AppSettingsDto.builder()
                .maxPostsPerUser(settings.getMaxPostsPerUser())
                .updatedAt(settings.getUpdatedAt())
                .updatedBy(settings.getUpdatedBy())
                .build();
    }
}
