# 开发环境

对照 CoursePilot 的环境目录：本仓用 **一套 Compose + 一份 `.env.example`**，不按人复制 JDK。F 维护 Compose；各人在自己模块目录开发。

## 必备

| 软件 | 版本 | 谁用 |
| --- | --- | --- |
| JDK | 17 | A B C D |
| Maven | 3.9+（或之后补的 `mvnw`） | A B C D |
| Node.js | 20 LTS | F |
| 微信开发者工具 | 稳定版 | E |
| Docker Desktop | 能跑 compose v2 | 全组，F 主责 |
| Python | 3.11+ | 改契约时跑 `scripts/check_contracts.py` |

```bash
cp .env.example .env
cd deploy
docker compose up mysql redis
```

后端（空工程）：

```bash
cd backend
mvn -q test
mvn spring-boot:run
curl http://127.0.0.1:8080/api/v1/health
```

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

- 业务镜像 profile 名 `app`，现在默认不启，等第 5 周能编译再 `docker compose --profile app up --build`
- 缺 `JWT_SECRET` 不得用字符串 `secret` 上演示机
- Windows 换行 LF；不要提交 `target/` `node_modules/` `.env`
