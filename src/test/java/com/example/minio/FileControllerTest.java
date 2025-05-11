package com.example.minio;

import com.example.demo.MinioApplication;
import com.example.demo.minio.MinioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = MinioApplication.class)
@AutoConfigureMockMvc
public class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MinioService minioService;

    @Test
    public void testUploadFile() throws Exception {
        // 模拟文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "这是测试文件内容".getBytes(StandardCharsets.UTF_8)
        );

        // 模拟MinioService.uploadFile方法的返回值
        when(minioService.uploadFile(any(MockMultipartFile.class)))
                .thenReturn("http://minio-server:9000/testbucket/test-file.txt");

        // 执行上传请求并验证
        mockMvc.perform(multipart("/api/files/upload")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://minio-server:9000/testbucket/test-file.txt"))
                .andExpect(jsonPath("$.fileName").value("test-file.txt"));
    }

    @Test
    public void testDownloadFile() throws Exception {
        // 模拟MinioService.downloadFile方法的返回值
        ByteArrayInputStream inputStream = new ByteArrayInputStream("这是测试文件内容".getBytes(StandardCharsets.UTF_8));
        when(minioService.downloadFile(anyString())).thenReturn(inputStream);

        // 执行下载请求并验证
        mockMvc.perform(get("/api/files/download/test-file.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"test-file.txt\""))
                .andExpect(content().bytes("这是测试文件内容".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testDeleteFile() throws Exception {
        // 执行删除请求并验证
        mockMvc.perform(delete("/api/files/test-file.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("文件删除成功"));
    }

    @Test
    public void testGetFileUrl() throws Exception {
        // 模拟MinioService.isFileExist和getFileUrl方法的返回值
        when(minioService.isFileExist(anyString())).thenReturn(true);
        when(minioService.getFileUrl(anyString(), anyInt()))
                .thenReturn("http://minio-server:9000/testbucket/test-file.txt?X-Amz-Algorithm=AWS4-HMAC-SHA256&...");

        // 执行获取URL请求并验证
        mockMvc.perform(get("/api/files/url/test-file.txt")
                .param("expiry", "3600"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.fileName").value("test-file.txt"))
                .andExpect(jsonPath("$.expiry").value(3600));
    }

    @Test
    public void testGetFileUrlNotFound() throws Exception {
        // 模拟文件不存在的情况
        when(minioService.isFileExist(anyString())).thenReturn(false);

        // 执行获取URL请求并验证
        mockMvc.perform(get("/api/files/url/non-existent-file.txt"))
                .andExpect(status().isNotFound());
    }
}
