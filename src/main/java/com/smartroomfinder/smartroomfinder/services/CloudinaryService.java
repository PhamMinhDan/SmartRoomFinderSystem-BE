package com.smartroomfinder.smartroomfinder.services;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadFile(MultipartFile file) throws IOException {

        Map<String, Object> options = new HashMap<>();

        boolean isVideo = file.getContentType() != null &&
                file.getContentType().startsWith("video");

        if (isVideo) {
            options.put("resource_type", "video");
            options.put("quality", "auto"); // tối ưu nhưng không mờ
        } else {
            options.put("resource_type", "image");
            options.put("quality", "auto:good"); // giữ nét tốt
            options.put("fetch_format", "auto");
            options.put("crop", "limit");
            options.put("width", 2000);
            options.put("height", 2000);
        }


        options.put("chunk_size", 6000000); // 6MB

        Map uploaded = cloudinary.uploader().upload(file.getBytes(), options);

        return uploaded.get("secure_url").toString();
    }
}
