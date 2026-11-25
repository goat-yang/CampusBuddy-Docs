# 部署骨架

F 所有。当前还没有打好的业务镜像。

```bash
cp ../.env.example ../.env
docker compose --env-file ../.env up -d mysql redis
```

Docker Hub 超时见 [开发环境](../docs/18-dev-environment.md)：先把 `mysql:8.0` 和 `redis:7-alpine` 从镜像站拉下来并打回原名，再执行上面的命令。

等 `backend` 能 `mvn -q test` 后再：

```bash
docker compose --profile app up --build
```

端口：MySQL 3306、Redis 6379、API 8080。小程序真机不要填 `localhost`。
