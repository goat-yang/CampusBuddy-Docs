# 部署骨架

F 所有。当前没有已发布的业务镜像。

```bash
cp ../.env.example ../.env
docker compose up mysql redis
```

等 `backend` 能 `mvn -q test` 后再：

```bash
docker compose --profile app up --build
```

端口：MySQL 3306、Redis 6379、API 8080。小程序真机不要填 `localhost`。
