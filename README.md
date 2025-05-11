# MinIO对象存储服务搭建指南

本文档提供了在云服务器上使用Docker快速搭建MinIO对象存储服务的详细步骤。

## 环境要求

- 云服务器（Linux系统）
- Docker 已安装
- 可用端口: 9000 (API端口), 9001 (控制台端口)

## 搭建步骤

### 1. 创建目录结构

```bash
mkdir -p ./minio/data
mkdir -p ./minio/config
```

### 2. 启动MinIO容器

使用Docker命令启动MinIO容器:

```bash
docker run -d \
  --name minio \
  -p 9000:9000 \
  -p 9001:9001 \
  -v $(pwd)/minio/data:/data \
  -v $(pwd)/minio/config:/root/.minio \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin" \
  --restart always \
  minio/minio server /data --console-address ":9001"
```

### 3. 检查容器状态

```bash
docker ps | grep minio
```

### 4. 配置防火墙（如果需要）

确保服务器防火墙已开放9000和9001端口:

```bash
sudo ufw allow 9000
sudo ufw allow 9001
```

## 访问MinIO

安装完成后，可以通过以下方式访问MinIO:

- API端口: `http://服务器IP:9000`
- 控制台: `http://服务器IP:9001`
- 默认用户名: `minioadmin`
- 默认密码: `minioadmin`

## Java应用集成

### Maven依赖

在您的`pom.xml`文件中添加以下依赖:

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.2</version>
</dependency>
```

### Spring Boot集成

在Spring Boot应用中，创建MinIO服务类:

```java
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;

@Service
public class MinioService {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${minio.bucketName}")
    private String bucketName;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("初始化MinIO客户端失败", e);
        }
    }

    /**
     * 上传文件
     * @param file 文件
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file) throws Exception {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
        
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .method(Method.GET)
                        .build());
    }

    /**
     * 下载文件
     * @param fileName 文件名
     * @return 文件输入流
     */
    public InputStream downloadFile(String fileName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build());
    }

    /**
     * 删除文件
     * @param fileName 文件名
     */
    public void deleteFile(String fileName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build());
    }
    
    /**
     * 获取文件URL
     * @param fileName 文件名
     * @param expiry URL过期时间（秒）
     * @return 文件URL
     */
    public String getFileUrl(String fileName, int expiry) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .expiry(expiry)
                        .method(Method.GET)
                        .build());
    }
    
    /**
     * 检查文件是否存在
     * @param fileName 文件名
     * @return 是否存在
     */
    public boolean isFileExist(String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 配置文件

在Spring Boot的`application.yml`中添加MinIO配置:

```yaml
# MinIO配置
minio:
  endpoint: http://your-server-ip:9000  # 替换为您的MinIO服务器IP
  accessKey: minioadmin
  secretKey: minioadmin
  bucketName: testbucket
```

### 控制器示例

创建一个控制器来处理文件上传和下载:

```java
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

    @Autowired
    private MinioService minioService;

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
```

## 一键部署脚本

为了简化部署过程，您可以使用以下脚本一键部署MinIO:

```bash
#!/bin/bash

# 设置颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${YELLOW}开始搭建MinIO对象存储服务...${NC}"

# 创建必要的目录
echo -e "${GREEN}创建目录结构...${NC}"
mkdir -p ./minio/data
mkdir -p ./minio/config

# 检查是否已存在minio容器
CONTAINER_EXISTS=$(docker ps -a | grep minio)
if [ -n "$CONTAINER_EXISTS" ]; then
  echo -e "${YELLOW}发现已存在的MinIO容器，正在停止并移除...${NC}"
  docker stop minio
  docker rm minio
fi

# 使用Docker命令启动MinIO
echo -e "${GREEN}启动MinIO容器...${NC}"
docker run -d \
  --name minio \
  -p 9000:9000 \
  -p 9001:9001 \
  -v $(pwd)/minio/data:/data \
  -v $(pwd)/minio/config:/root/.minio \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin" \
  --restart always \
  minio/minio server /data --console-address ":9001"

# 等待MinIO启动
echo -e "${YELLOW}等待MinIO容器启动 (10秒)...${NC}"
sleep 10

# 检查MinIO是否正常运行
echo -e "${GREEN}检查MinIO是否正常运行...${NC}"
CONTAINER_STATUS=$(docker ps | grep minio)

if [ -n "$CONTAINER_STATUS" ]; then
  echo -e "${GREEN}MinIO服务已成功启动!${NC}"
  echo -e "${GREEN}API端口: http://localhost:9000${NC}"
  echo -e "${GREEN}控制台: http://localhost:9001${NC}"
  echo -e "${GREEN}用户名: minioadmin${NC}"
  echo -e "${GREEN}密码: minioadmin${NC}"
  
  # 获取服务器IP地址
  SERVER_IP=$(hostname -I | awk '{print $1}')
  if [ -n "$SERVER_IP" ]; then
    echo -e "${GREEN}您也可以通过以下地址访问:${NC}"
    echo -e "${GREEN}API端口: http://$SERVER_IP:9000${NC}"
    echo -e "${GREEN}控制台: http://$SERVER_IP:9001${NC}"
  fi
else
  echo -e "${RED}MinIO服务启动失败!${NC}"
  echo -e "${YELLOW}请检查Docker日志:${NC}"
  docker logs minio
fi

echo -e "${GREEN}MinIO对象存储服务搭建完成!${NC}"
echo -e "${YELLOW}注意: 这是一个开发环境配置，生产环境请进一步加强安全性配置。${NC}"
```

将上述脚本保存为`setup-minio.sh`，然后执行以下命令:

```bash
chmod +x setup-minio.sh
./setup-minio.sh
```

## 常见问题

1. **端口被占用**
   
   如果9000或9001端口已被占用，可以修改端口映射:
   ```bash
   docker run -d \
     --name minio \
     -p 9090:9000 \
     -p 9091:9001 \
     -v $(pwd)/minio/data:/data \
     -v $(pwd)/minio/config:/root/.minio \
     -e "MINIO_ROOT_USER=minioadmin" \
     -e "MINIO_ROOT_PASSWORD=minioadmin" \
     --restart always \
     minio/minio server /data --console-address ":9001"
   ```

2. **容器无法启动**
   
   检查Docker日志:
   ```bash
   docker logs minio
   ```

3. **无法访问MinIO**
   
   确保服务器防火墙已开放9000和9001端口。

## 注意事项

1. 此配置适用于开发和测试环境，生产环境需要更多安全配置
2. 默认用户名和密码为 minioadmin/minioadmin，生产环境应使用更强的密码
3. 数据存储在容器外的挂载卷中，确保数据安全
4. 考虑使用HTTPS来保护数据传输
5. 根据需要配置适当的存储桶策略和访问控制

## 联系我们
accesskey:tbV8uiWYRaLaqEkb5hqe
secretkey:TQ5Q2hMWM3jPgqAGVowWq61HcCr1AprbBjajVsnq

```bash
#  上传文件
curl -X POST http://localhost:8888/api/files/upload -F "file=@/Users/huzhengyuan/Documents/coding/hzy/mysql-replication-demo/example.txt"
#  下载文件
curl http://localhost:8888/api/files/download/example.txt
#  删除文件
curl -X DELETE http://localhost:8888/api/files/example.txt
#  获取文件URL
curl http://localhost:8888/api/files/url/example.txt