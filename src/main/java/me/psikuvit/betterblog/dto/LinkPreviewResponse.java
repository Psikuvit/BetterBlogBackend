package me.psikuvit.betterblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkPreviewResponse {
    private String url;
    private String title;
    private String description;
    private String image;
}

