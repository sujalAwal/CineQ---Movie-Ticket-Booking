package com.awal.cineq.file.controller;

import com.awal.cineq.exception.FileStorageException;
import com.awal.cineq.file.service.FileStorageService;
import com.awal.cineq.file.dto.FileUploadResponse;
import com.awal.cineq.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/files")
public class FileUploadController {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = fileStorageService.storeFile(file);

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/files/download/")
                    .path(fileName)
                    .toUriString();

            FileUploadResponse response = new FileUploadResponse(
                    fileName,
                    fileDownloadUri,
                    file.getContentType(),
                    file.getSize(),
                    "File uploaded successfully"
            );

            return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", response));

        } catch (FileStorageException | IOException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ex.getMessage(), 400));
        }
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = fileStorageService.getFilePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(fileName);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{fileName:.+}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String fileName) {
        try {
            fileStorageService.deleteFile(fileName);
            return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
        } catch (IOException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to delete file: " + ex.getMessage(), 400));
        }
    }

    private String determineContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "pdf":
                return "application/pdf";
            default:
                return "application/octet-stream";
        }
    }
}