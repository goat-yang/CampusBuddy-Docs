# 开发环境

对照 CoursePilot 的环境目录：本仓用 **一套 Compose + 一份 `.env.example`**，不按人复制 JDK。F 维护 Compose；各人在自己模块目录开发。

## 必备

| 软件 | 版本 | 谁用 |
| --- | --- | --- |
| JDK | 25 | A B C D |
| Spring Boot | 3.5.16 | A B C D |
| Maven | 3.9.16，仓库自带 `backend/mvnw`、`mvnw.cmd` | A B C D |
| Node.js | 20 LTS | F |
| 微信开发者工具 | 稳定版 | E |
| Docker Desktop | 能跑 compose v2 | 全组，F 主责 |
| Python | 3.11+ | 改契约时跑 `scripts/check_contracts.py` |

```bash
cp .env.example .env
```

`.env` 留在本机。至少把 `JWT_SECRET` 换成 32 位以上随机串，数据库密码不要继续用 `changeme`。`MAIL_*` 填能发信的 SMTP 后，邮箱验证码才能真的发出；单测不依赖它。

```bash
cd deploy
docker compose --env-file ../.env up -d mysql redis
```

Docker Hub（`registry-1.docker.io`）超时的时候，先从镜像站拉取再打回 Compose 使用的名字：

```bash
docker pull dockerproxy.net/library/mysql:8.0
docker tag dockerproxy.net/library/mysql:8.0 mysql:8.0
docker pull dockerproxy.net/library/redis:7-alpine
docker tag dockerproxy.net/library/redis:7-alpine redis:7-alpine
```

`dockerproxy.net` 不可用时再试 `docker.1panel.live`、`docker.m.daocloud.io`。不要把镜像站前缀写进 `docker-compose.yml`。

后端（空工程）：

```bash
cd backend
mvnw.cmd -q test
mvnw.cmd spring-boot:run
curl http://127.0.0.1:8080/api/v1/health
```

macOS / Linux 把 `mvnw.cmd` 换成 `./mvnw`。本机已经装了 Maven 3.9+ 时，`mvn` 同样可以。

成功响应必须是封套：`code=0`，`data.status=UP`。

Web 占位：

```bash
cd web
npm install
npm run dev:portal
npm run dev:admin
```

小程序：用开发者工具打开 `miniprogram/`。真机不要把 `config.js` 的 `BASE_URL` 写成 localhost。

## 约定

- 编译目标锁定 JDK 25，Spring Boot 锁定 3.5.16。本机 JDK 低于 25 时 `mvn test` 会失败。装了更高版本可以编译，不要改 `java.version`
- 业务镜像 profile 名 `app`，现在默认不启，等第 5 周能编译再 `docker compose --profile app up --build`
- 缺 `JWT_SECRET` 不得用字符串 `secret` 上演示机
- Windows 换行 LF；不要提交 `target/` `node_modules/` `.env`
