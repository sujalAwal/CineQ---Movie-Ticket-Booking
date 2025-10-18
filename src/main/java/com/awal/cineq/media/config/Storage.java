package com.awal.cineq.media.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Getter
@Component
public class Storage {
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.file.max-size:2097152}") // 2MB default
    private long maxFileSize;
}
