package com.example.minio;

import com.example.demo.MinioApplication;
import com.example.demo.minio.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = MinioApplication.class)
public class MinioServiceTest {

    @Autowired
    private MinioService minioService;

    private String testFileName;
    private String testFileContent;

    @BeforeEach
    public void setup() {
        testFileName = "test-file-" + System.currentTimeMillis() + ".txt";
        testFileContent = "这是一个MinIO测试文件内容 - " + System.currentTimeMillis();
    }

    @Test
    public void testUploadFile() throws Exception {
        // 创建模拟的MultipartFile
        MultipartFile file = new MockMultipartFile(
                testFileName,
                testFileName,
                MediaType.TEXT_PLAIN_VALUE,
                testFileContent.getBytes(StandardCharsets.UTF_8)
        );

        // 上传文件
        String url = minioService.uploadFile(file);
        
        // 验证URL不为空
        assertNotNull(url);
        System.out.println("上传文件URL: " + url);
        
        // 验证文件是否存在
        boolean exists = minioService.isFileExist(extractFileNameSimple(url));
        assertTrue(exists, "上传的文件应该存在");
    }

    @Test
    public void testDownloadFile() throws Exception {
        // 先上传一个文件
        MultipartFile file = new MockMultipartFile(
                testFileName,
                testFileName,
                MediaType.TEXT_PLAIN_VALUE,
                testFileContent.getBytes(StandardCharsets.UTF_8)
        );
        String url = minioService.uploadFile(file);

        // 下载文件
        InputStream inputStream = minioService.downloadFile(extractFileNameSimple(url));
        
        // 验证输入流不为空
        assertNotNull(inputStream);
        
        // 读取输入流内容并验证
        byte[] bytes = inputStream.readAllBytes();
        String downloadedContent = new String(bytes, StandardCharsets.UTF_8);
        assertEquals(testFileContent, downloadedContent, "下载的文件内容应该与上传的内容一致");
        
        inputStream.close();
    }

    @Test
    public void testGetFileUrl() throws Exception {
        // 先上传一个文件
        MultipartFile file = new MockMultipartFile(
                testFileName,
                testFileName,
                "text/plain",
                testFileContent.getBytes(StandardCharsets.UTF_8)
        );
        minioService.uploadFile(file);
        
        // 获取文件URL，过期时间设为60秒
        String url = minioService.getFileUrl(testFileName, 60);
        
        // 验证URL不为空
        assertNotNull(url);
        System.out.println("文件URL (60秒过期): " + url);
    }

    @Test
    public void testDeleteFile() throws Exception {
        // 先上传一个文件
        MultipartFile file = new MockMultipartFile(
                testFileName,
                testFileName,
                "text/plain",
                testFileContent.getBytes(StandardCharsets.UTF_8)
        );
        minioService.uploadFile(file);
        
        // 验证文件存在
        boolean existsBefore = minioService.isFileExist(testFileName);
        assertTrue(existsBefore, "删除前文件应该存在");
        
        // 删除文件
        minioService.deleteFile(testFileName);
        
        // 验证文件已被删除
        boolean existsAfter = minioService.isFileExist(testFileName);
        assertFalse(existsAfter, "删除后文件不应该存在");
    }

    @Test
    public void testFileExistence() throws Exception {
        // 测试不存在的文件
        String nonExistentFile = "non-existent-file-" + System.currentTimeMillis() + ".txt";
        boolean exists = minioService.isFileExist(nonExistentFile);
        assertFalse(exists, "不存在的文件应该返回false");
        
        // 上传一个文件
        MultipartFile file = new MockMultipartFile(
                testFileName,
                testFileName,
                "text/plain",
                testFileContent.getBytes(StandardCharsets.UTF_8)
        );
        minioService.uploadFile(file);
        
        // 测试存在的文件
        exists = minioService.isFileExist(testFileName);
        assertTrue(exists, "存在的文件应该返回true");
    }

    //  从URL中提取文件名
    public static String extractFileNameSimple(String url) {
        String path = url.split("\\?")[0]; // 去掉查询参数
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
