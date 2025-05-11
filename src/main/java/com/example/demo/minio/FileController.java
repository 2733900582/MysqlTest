package com.example.demo.minio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final MinioService minioService;

    public FileController(MinioService minioService) {
        this.minioService = minioService;
    }

    /**
     * 上传文件
     * @param file 文件
     * @return 文件URL
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String url = minioService.uploadFile(file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("url", url);
            response.put("fileName", file.getOriginalFilename());
            response.put("size", file.getSize());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     * @param fileName 文件名
     * @return 文件流
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String fileName) {
        try {
            InputStream inputStream = minioService.downloadFile(fileName);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除文件
     * @param fileName 文件名
     * @return 操作结果
     */
    @DeleteMapping("/{fileName}")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
        try {
            minioService.deleteFile(fileName);
            return ResponseEntity.ok("文件删除成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取文件URL
     * @param fileName 文件名
     * @return 文件URL
     */
    @GetMapping("/url/{fileName}")
    public ResponseEntity<?> getFileUrl(@PathVariable String fileName, 
                                        @RequestParam(defaultValue = "3600") Integer expiry) {
        try {
            if (!minioService.isFileExist(fileName)) {
                return ResponseEntity.notFound().build();
            }
            
            String url = minioService.getFileUrl(fileName, expiry);
            
            Map<String, Object> response = new HashMap<>();
            response.put("url", url);
            response.put("fileName", fileName);
            response.put("expiry", expiry);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取URL失败: " + e.getMessage());
        }
    }
}
