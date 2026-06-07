package me.psikuvit.betterblog.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.psikuvit.betterblog.dto.PostReportDto;
import me.psikuvit.betterblog.dto.PostReportRequest;
import me.psikuvit.betterblog.dto.PostRequest;
import me.psikuvit.betterblog.entity.Post;
import me.psikuvit.betterblog.entity.User;
import me.psikuvit.betterblog.exception.BadRequestException;
import me.psikuvit.betterblog.exception.UnauthorizedException;
import me.psikuvit.betterblog.service.AuthService;
import me.psikuvit.betterblog.service.LinkPreviewService;
import me.psikuvit.betterblog.service.PostReportService;
import me.psikuvit.betterblog.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class PostController {

    private final PostService postService;
    private final AuthService authService;
    private final LinkPreviewService linkPreviewService;
    private final PostReportService postReportService;

    @GetMapping
    public ResponseEntity<Page<Post>> getPosts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String visibility,
            Pageable pageable) {
        if (StringUtils.hasText(q)) {
            return ResponseEntity.ok(postService.searchPosts(q, pageable));
        }

        if (StringUtils.hasText(tag)) {
            return ResponseEntity.ok(postService.getPostsByTag(tag, pageable));
        }

        if (StringUtils.hasText(visibility)) {
            throw new BadRequestException("Visibility filter is not supported on this endpoint; use /admin/posts when authenticated as admin");
        }

        return ResponseEntity.ok(postService.getPublicPosts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Post> getPost(@PathVariable String id) {
        Post post = postService.getPost(id, getCurrentUser());
        return ResponseEntity.ok(post);
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<PostReportDto> reportPost(
            @PathVariable String id,
            @Valid @RequestBody PostReportRequest request) {
        User currentUser = getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postReportService.reportPost(id, request, currentUser));
    }

    @PostMapping
    public ResponseEntity<Post> createPost(@Valid @RequestBody PostRequest request) {
        User currentUser = getCurrentUser();
        Post post = postService.createPost(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Post> updatePost(
            @PathVariable String id,
            @Valid @RequestBody PostRequest request) {
        User currentUser = getCurrentUser();
        Post post = postService.updatePost(id, request, currentUser);
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable String id) {
        User currentUser = getCurrentUser();
        postService.deletePost(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewUrl(@RequestParam String url) {
        Map<String, Object> result = new HashMap<>();
        result.put("preview", linkPreviewService.fetchPreview(url));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/export")
    public ResponseEntity<String> exportPosts(
            @RequestParam(required = false, defaultValue = "json") String format,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String q) {
        Page<Post> page = StringUtils.hasText(q)
                ? postService.searchPosts(q, PageRequest.of(0, 1000))
                : StringUtils.hasText(tags)
                        ? postService.getPostsByTag(tags, PageRequest.of(0, 1000))
                        : postService.getPublicPosts(PageRequest.of(0, 1000));

        try {
            if ("csv".equalsIgnoreCase(format)) {
                return ResponseEntity.ok(toCsv(page.getContent()));
            }

            return ResponseEntity.ok(toJson(page.getContent()));
        } catch (Exception ex) {
            throw new BadRequestException("Unable to export posts");
        }
    }

    @GetMapping("/public/count")
    public ResponseEntity<Map<String, Long>> getPublicPostCount() {
        long count = postService.countByVisibility(Post.Visibility.PUBLIC);
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importPosts(@RequestBody Map<String, Object> request) {
        User currentUser = getCurrentUser();
        String format = String.valueOf(request.getOrDefault("format", "json"));
        String payload = request.get("payload") == null ? null : request.get("payload").toString();

        if (!StringUtils.hasText(payload)) {
            throw new BadRequestException("Import payload is required");
        }

        List<PostRequest> items = parseImportPayload(format, payload);
        int importedCount = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            try {
                postService.createPost(items.get(i), currentUser);
                importedCount++;
            } catch (Exception ex) {
                errors.add("Item " + (i + 1) + ": " + ex.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("importedCount", importedCount);
        result.put("errors", errors);
        result.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(result);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        return authService.getUserEntityByUsername(authentication.getName());
    }


    private String toCsv(List<Post> posts) {
        StringBuilder builder = new StringBuilder();
        builder.append("id,title,slug,excerpt,visibility,authorUsername\n");
        for (Post post : posts) {
            builder.append(safeCsv(post.getId() == null ? null : post.getId())).append(',')
                    .append(safeCsv(post.getTitle())).append(',')
                    .append(safeCsv(post.getSlug())).append(',')
                    .append(safeCsv(post.getExcerpt())).append(',')
                    .append(safeCsv(post.getVisibility() == null ? null : post.getVisibility().name())).append(',')
                    .append(safeCsv(post.getAuthor() == null ? null : post.getAuthor().getUsername()))
                    .append('\n');
        }
        return builder.toString();
    }

    private String safeCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }

    private List<PostRequest> parseImportPayload(String format, String payload) {
        if ("csv".equalsIgnoreCase(format)) {
            return parseCsvPayload(payload);
        }

        return parseJsonPayload(payload);
    }

    private List<PostRequest> parseJsonPayload(String payload) {
        String trimmed = payload.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new BadRequestException("JSON payload must be an array of post objects");
        }

        List<PostRequest> requests = new ArrayList<>();
        for (String object : splitJsonObjects(trimmed.substring(1, trimmed.length() - 1))) {
            if (!StringUtils.hasText(object)) {
                continue;
            }

            requests.add(PostRequest.builder()
                    .title(jsonValue(object, "title"))
                    .slug(jsonValue(object, "slug"))
                    .excerpt(jsonValue(object, "excerpt"))
                    .content(defaultString(jsonValue(object, "content"), ""))
                    .visibility(defaultString(jsonValue(object, "visibility"), "PUBLIC"))
                    .coverImageUrl(jsonValue(object, "coverImageUrl"))
                    .sourceUrl(jsonValue(object, "sourceUrl"))
                    .sourcePreviewTitle(jsonValue(object, "sourcePreviewTitle"))
                    .sourcePreviewDescription(jsonValue(object, "sourcePreviewDescription"))
                    .sourcePreviewImage(jsonValue(object, "sourcePreviewImage"))
                    .originalAuthor(jsonValue(object, "originalAuthor"))
                    .legacyId(jsonValue(object, "legacyId"))
                    .build());
        }

        return requests;
    }

    private List<String> splitJsonObjects(String payload) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;

        for (int i = 0; i < payload.length(); i++) {
            char c = payload.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    objects.add(payload.substring(start, i + 1));
                    start = -1;
                }
            }
        }

        return objects;
    }

    private String jsonValue(String object, String key) {
        String needle = "\"" + key + "\"";
        int keyIndex = object.indexOf(needle);
        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = object.indexOf(':', keyIndex + needle.length());
        if (colonIndex < 0) {
            return null;
        }

        int index = colonIndex + 1;
        while (index < object.length() && Character.isWhitespace(object.charAt(index))) {
            index++;
        }

        if (index >= object.length()) {
            return null;
        }

        if (object.startsWith("null", index)) {
            return null;
        }

        if (object.charAt(index) != '"') {
            return null;
        }

        index++;
        StringBuilder value = new StringBuilder();
        boolean escaping = false;

        while (index < object.length()) {
            char c = object.charAt(index);
            if (escaping) {
                value.append(switch (c) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case '"' -> '"';
                    case '\\' -> '\\';
                    default -> c;
                });
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                break;
            } else {
                value.append(c);
            }
            index++;
        }

        return value.toString();
    }

    private String defaultString(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String toJson(List<Post> posts) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < posts.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(postToJson(posts.get(i)));
        }
        builder.append(']');
        return builder.toString();
    }

    private String postToJson(Post post) {
        return "{" +
                "\"id\":" + (post.getId() == null ? "null" : post.getId()) +
                ",\"title\":" + jsonString(post.getTitle()) +
                ",\"slug\":" + jsonString(post.getSlug()) +
                ",\"excerpt\":" + jsonString(post.getExcerpt()) +
                ",\"content\":" + jsonString(post.getContent()) +
                ",\"visibility\":" + jsonString(post.getVisibility() == null ? null : post.getVisibility().name()) +
                ",\"coverImageUrl\":" + jsonString(post.getCoverImageUrl()) +
                ",\"authorUsername\":" + jsonString(post.getAuthor() == null ? null : post.getAuthor().getUsername()) +
                "}";
    }

    private String jsonString(String value) {
        if (!StringUtils.hasText(value)) {
            return "null";
        }

        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return '"' + escaped + '"';
    }

    private List<PostRequest> parseCsvPayload(String payload) {
        String[] lines = payload.split("\\R");
        if (lines.length < 2) {
            throw new BadRequestException("CSV payload must contain a header row and at least one data row");
        }

        String[] headers = lines[0].split(",");
        List<PostRequest> requests = new ArrayList<>();

        for (int i = 1; i < lines.length; i++) {
            if (!StringUtils.hasText(lines[i])) {
                continue;
            }

            String[] values = lines[i].split(",", -1);
            Map<String, String> row = new HashMap<>();
            for (int j = 0; j < headers.length && j < values.length; j++) {
                row.put(headers[j].trim(), values[j].trim().replaceAll("^\"|\"$", ""));
            }

            requests.add(PostRequest.builder()
                    .title(row.get("title"))
                    .slug(row.get("slug"))
                    .excerpt(row.get("excerpt"))
                    .content(row.getOrDefault("content", ""))
                    .visibility(row.getOrDefault("visibility", "PUBLIC"))
                    .coverImageUrl(row.get("coverImageUrl"))
                    .sourceUrl(row.get("sourceUrl"))
                    .sourcePreviewTitle(row.get("sourcePreviewTitle"))
                    .sourcePreviewDescription(row.get("sourcePreviewDescription"))
                    .sourcePreviewImage(row.get("sourcePreviewImage"))
                    .originalAuthor(row.get("originalAuthor"))
                    .legacyId(row.get("legacyId"))
                    .build());
        }

        return requests;
    }
}