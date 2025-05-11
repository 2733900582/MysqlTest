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
echo -e "${GREEN}Java集成说明已保存到 minio-java-integration.md 文件中${NC}"
