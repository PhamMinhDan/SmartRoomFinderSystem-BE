package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.services.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm", "video/quicktime"
    );

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, @RequestParam("secureId") String secureId) {
        try {
            String url = cloudinaryService.uploadFile(file, secureId);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/chat-media")
    public ResponseEntity<?> uploadChatMedia(@RequestParam("file") MultipartFile file, @RequestParam("secureId") String secureId) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File rỗng"));
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("error", "File vượt quá 10 MB"));
        }

        String mime = file.getContentType();
        if (mime == null || !ALLOWED_MIME.contains(mime.toLowerCase())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Chỉ chấp nhận ảnh (jpg/png/gif/webp) hoặc video (mp4/webm/mov)"));
        }

        String fileType = mime.startsWith("video") ? "VIDEO" : "IMAGE";

        try {
            String url = cloudinaryService.uploadFile(file, secureId);
            return ResponseEntity.ok(Map.of(
                    "url",      url,
                    "fileType", fileType,
                    "mimeType", mime,
                    "size",     file.getSize()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload thất bại: " + e.getMessage()));
        }
    }
}

