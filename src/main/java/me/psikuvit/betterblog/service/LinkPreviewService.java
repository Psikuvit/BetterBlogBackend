package me.psikuvit.betterblog.service;

import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.dto.LinkPreviewResponse;
import me.psikuvit.betterblog.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LinkPreviewService {

    private static final int MAX_HTML_CHARS = 250_000;

    public LinkPreviewResponse fetchPreview(String url) {
        String normalizedUrl = normalizeAndValidateUrl(url);

        try {
            String html = fetchHtml(normalizedUrl);
            if (!StringUtils.hasText(html)) {
                throw new BadRequestException("Unable to fetch preview data from the provided URL");
            }

            return LinkPreviewResponse.builder()
                    .url(normalizedUrl)
                    .title(firstNonBlank(extractMeta(html, "og:title"), extractMeta(html, "twitter:title"), extractTitleTag(html)))
                    .description(firstNonBlank(extractMeta(html, "og:description"), extractMeta(html, "twitter:description"), extractMeta(html, "description")))
                    .image(firstNonBlank(extractMeta(html, "og:image"), extractMeta(html, "twitter:image"), extractMeta(html, "twitter:image:src")))
                    .build();
        } catch (IOException e) {
            throw new BadRequestException("Unable to fetch preview data");
        }
    }

    private String normalizeAndValidateUrl(String url) {
        if (!StringUtils.hasText(url)) {
            throw new BadRequestException("URL is required");
        }

        String trimmed = url.trim();
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new BadRequestException("Only http and https URLs are supported");
            }
            if (!StringUtils.hasText(uri.getHost())) {
                throw new BadRequestException("Invalid URL: " + url);
            }
            return uri.toString();
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid URL: " + url);
        }
    }

    private String fetchHtml(String url) throws IOException {
        URL target = URI.create(url).toURL();
        HttpURLConnection connection = (HttpURLConnection) target.openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "BetterBlog/1.0");

        int status = connection.getResponseCode();
        if (status >= 400) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
                if (builder.length() > MAX_HTML_CHARS) {
                    break;
                }
            }
        }
        return builder.toString();
    }

    private String extractMeta(String html, String property) {
        Pattern pattern = Pattern.compile(
                "<meta[^>]+(?:property|name)=[\"']" + Pattern.quote(property) + "[\"'][^>]+content=[\"']([^\"']+)[\"']",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String extractTitleTag(String html) {
        Pattern pattern = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.group(1).replaceAll("<[^>]+>", "").trim() : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}

