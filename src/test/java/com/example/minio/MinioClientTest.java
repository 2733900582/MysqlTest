package com.example.minio;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * MinIO客户端测试类
 * 这个类提供了一个简单的方法来测试MinIO连接和基本操作
 * 可以直接运行main方法进行测试
 */
public class MinioClientTest {

    // MinIO服务器配置
    private static final String ENDPOINT = "http://yuanbaocat.work:9000";
    private static final String ACCESS_KEY = "minioadmin";
    private static final String SECRET_KEY = "minioadmin";
    private static final String BUCKET_NAME = "testbucket";

    public static void main(String[] args) {
        try {
            // 创建MinIO客户端
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(ENDPOINT)
                    .credentials(ACCESS_KEY, SECRET_KEY)
                    .build();

            System.out.println("成功连接到MinIO服务器: " + ENDPOINT);

            // 测试连接
            testConnection(minioClient);

            // 测试存储桶操作
            testBucketOperations(minioClient);

            // 测试文件操作
            testObjectOperations(minioClient);

            System.out.println("所有测试完成!");

        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试MinIO连接
     */
    private static void testConnection(MinioClient minioClient) throws Exception {
        System.out.println("\n===== 测试MinIO连接 =====");
        
        // 列出所有存储桶
        List<Bucket> bucketList = minioClient.listBuckets();
        System.out.println("存储桶列表:");
        for (Bucket bucket : bucketList) {
            System.out.println(" - " + bucket.name() + " (创建时间: " + bucket.creationDate() + ")");
        }
    }

    /**
     * 测试存储桶操作
     */
    private static void testBucketOperations(MinioClient minioClient) throws Exception {
        System.out.println("\n===== 测试存储桶操作 =====");
        
        // 检查测试桶是否存在
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build());
        System.out.println("存储桶 '" + BUCKET_NAME + "' 是否存在: " + bucketExists);
        
        // 如果不存在，创建测试桶
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
            System.out.println("存储桶 '" + BUCKET_NAME + "' 创建成功");
        }
    }

    /**
     * 测试文件操作
     */
    private static void testObjectOperations(MinioClient minioClient) throws Exception {
        System.out.println("\n===== 测试文件操作 =====");
        
        // 创建测试文件
        String objectName = "test-file-" + System.currentTimeMillis() + ".txt";
        String content = "这是MinIO测试文件内容 - " + System.currentTimeMillis();
        ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        
        // 上传文件
        System.out.println("上传文件: " + objectName);
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(objectName)
                        .stream(bais, bais.available(), -1)
                        .contentType("text/plain")
                        .build());
        System.out.println("文件上传成功");
        
        // 获取文件信息
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(objectName)
                        .build());
        System.out.println("文件信息: ");
        System.out.println(" - 名称: " + stat.object());
        System.out.println(" - 大小: " + stat.size() + " 字节");
        System.out.println(" - 类型: " + stat.contentType());
        System.out.println(" - ETag: " + stat.etag());
        
        // 获取文件URL
        String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(objectName)
                        .method(Method.GET)
                        .expiry(60 * 60) // 1小时
                        .build());
        System.out.println("文件URL (1小时有效): " + url);
        
        // 列出存储桶中的文件
        System.out.println("\n存储桶 '" + BUCKET_NAME + "' 中的文件:");
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(BUCKET_NAME)
                        .build());
        for (Result<Item> result : results) {
            Item item = result.get();
            System.out.println(" - " + item.objectName() + " (大小: " + item.size() + " 字节)");
        }
        
//        // 删除文件
//        System.out.println("\n删除文件: " + objectName);
//        minioClient.removeObject(
//                RemoveObjectArgs.builder()
//                        .bucket(BUCKET_NAME)
//                        .object(objectName)
//                        .build());
//        System.out.println("文件删除成功");
    }
}
