# backend（一个 Spring Boot，四人分包）

**不要按人名建文件夹交作业。** 代码都进本目录，用包区分所有权，用 PR 合入 `main`。

| 包 | Owner | 允许写 |
| --- | --- | --- |
| `com.shu.campusbuddy.common` | A 先建，全组只读使用 | 封套、错误码、Idempotency |
| `com.shu.campusbuddy.auth` | A | 登录、邮箱验证码、JWT、用户资料 |
| `com.shu.campusbuddy.activity` | B | 活动、报名锁、候补、签到、收藏 |
| `com.shu.campusbuddy.reco` | C | 推荐与降级、埋点、权重配置接口 |
| `com.shu.campusbuddy.credit` | D | 信用流水、通知、敏感词、举报、后台统计 |

JDK 17、Spring Boot 3、MySQL 8、Redis 7。契约：`../contracts/openapi.yaml`。当前只有空工程，没有业务实现。

```bash
./mvnw -q test
```

Windows：`mvnw.cmd -q test`。第一次补业务代码时同时补该包的测试和本 README 启动步骤。
