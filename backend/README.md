# backend（一个 Spring Boot，四人分包）

**不要按人名建文件夹交作业。** 代码都进本目录，用包区分所有权，用 PR 合入 `main`。

| 包 | Owner | 允许写 |
| --- | --- | --- |
| `com.shu.campusbuddy.common` | A 先建，全组只读使用 | 封套、错误码、Idempotency |
| `com.shu.campusbuddy.auth` | A | 登录、邮箱验证码、JWT、用户资料 |
| `com.shu.campusbuddy.activity` | B | 活动、报名锁、候补、签到、收藏 |
| `com.shu.campusbuddy.reco` | C | 推荐与降级、埋点、权重配置接口 |
| `com.shu.campusbuddy.credit` | D | 信用流水、通知、敏感词、举报、后台统计 |

JDK 25、Spring Boot 3.5.16、MySQL 8、Redis 7。契约：`../contracts/openapi.yaml`。

A 已实现：学校邮箱验证码、首次设置本平台密码、工号密码登录、微信 `code` 预览、JWT 刷新、`/users/me`、兴趣标签、脱敏主页、封套与幂等。B/C/D 的业务接口还没有。报名和发布接口加上 `com.shu.campusbuddy.auth.RequireCampusBound` 后，未绑定学号会返回 40300，文案包含「先绑定学号」。当前用户用 `AuthContext.require()`，角色只认 JWT。

用户表在 `src/main/resources/db/migration/V1__a_user.sql`。活动表请从 V2 起，不要再占用 V1。

```bash
./mvnw -q test
./mvnw spring-boot:run
```

Windows：`mvnw.cmd -q test`。单测用 H2，不连真实邮箱和微信。从 `backend/` 启动时会读取上一级 `.env`，且不会覆盖已经存在的环境变量。`JWT_SECRET` 必须是 32 位以上，不能是 `secret`。

`SPRING_DATA_REDIS_HOST` 留空时，验证码限额存在本机内存。设成 `127.0.0.1` 后走 Redis，键前缀是 `cb:a:otp:`。`MAIL_USERNAME` 和 `MAIL_PASSWORD` 都填上才会发信；没配邮件时发码返回 50000，验证码不会写进日志。`DEMO_CAMPUS_ID` 和 `DEMO_PASSWORD` 都有值时，启动会补一个 `affiliation=DEMO` 的密码账号，方便答辩。这两个值不要提交。后端起来之后可以跑 `python scripts/demo_login.py`，它只打印用户编号和昵称，令牌写到 `backend/.demo-session.json`。
