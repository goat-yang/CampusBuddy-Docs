# Agent 编码契约（必须遵守）

本文件给写代码的人或模型。先读完再改仓库。字段真源是 [openapi.yaml](openapi.yaml)，行为真源是 [docs/04-api-contract.md](../docs/04-api-contract.md)。冲突时以 OpenAPI 的字段名/类型/枚举为准，以 Markdown 的状态机与所有权为准，然后提 PR 同时改两份。

## 0. 不要做的事

- 不要按人名建 `A/` `张三/` 这类交作业目录。代码进 `backend/` `miniprogram/` `web/`。
- 不要新增与 OpenAPI 不同名的字段（`leftNum`、`remain`、`token` 当 accessToken）。
- 不要在客户端判断能不能报名、能不能发布、推荐分怎么算。
- 不要实现 `SHU_OAUTH`，不要接收学校统一认证密码。
- 不要把验证码、JWT、学号明文写进日志或示例可复制密钥。
- 不要在一个 PR 里同时改两个 Owner 的包，除非契约变更且双方都在 PR 里。

## 1. 传输规则

| 项 | 规则 |
| --- | --- |
| Base | `{origin}/api/v1`，不要再套一层 `/api` |
| 成功 | HTTP 200 且 `code === 0`。只看这个，不要用「有 data 就算成功」 |
| 失败 | HTTP 与 `code` 必须配对（见 OpenAPI `ErrorEnvelope`）。`data` 可为 `null` 或 `{field, retryable}` |
| 时间 | 字符串 `yyyy-MM-dd HH:mm:ss`，北京时间，不要 ISO-8601 `T` |
| 分页 | query `pageNum` 从 1；`pageSize` 默认 10 最大 50；列表 data 固定 `{total, list}` |
| 鉴权 | 除 `/auth/login` `/auth/otp/send` `/auth/refresh` `/health` 外带 `Authorization: Bearer` |
| 写操作 | `POST/PUT/PATCH/DELETE` 业务写必须带 `Idempotency-Key`（UUID）。同一 key 重放必须返回第一次结果；key 相同 body 不同 → `40905` |
| 未知字段 | 请求 JSON `additionalProperties: false`，多一个字段 → `40001` |
| 提权 | body 里的 `userId`/`role`/`creditScore` 一律忽略；权限只看 JWT |

客户端伪代码：

```
if http 401 or body.code == 40100: 清 token，去登录
if body.code != 0: 用 code 映射文案，不要用 HTTP 文本
use body.data
```

## 2. 所有权（改错包会被打回）

| Owner | 可写路径 | 可写代码 |
| --- | --- | --- |
| A | `/auth/*` `/users/me` `/users/{id}/profile` `/health` `common` | `backend/.../auth` `backend/.../common` |
| B | `/activities*` `/enrollments*` `/files/image` `/favorites*` | `backend/.../activity` |
| C | `/reco/*` `/admin/reco/config` | `backend/.../reco` |
| D | `/users/me/credit` `/credit/rule` `/notifications*` `/reviews*` `/reports*` `/admin/*`（reco config 除外） | `backend/.../credit` |
| E | 小程序页面与 `miniprogram/api` 客户端 | `miniprogram/` |
| F | `web/apps/portal` `web/apps/admin` `deploy/` 测试文档 | `web/` `deploy/` |

E/F 只生成 OpenAPI 里出现过的请求，只读取 OpenAPI 里出现过的响应字段。

## 3. 关键业务（实现时的硬约束）

- 报名：Redis `DECR` 名额 + DB 唯一 `(activity_id, user_id)`。满员候补开 → `status=WAITLIST` 且 HTTP 200 `code=0`；候补关 → HTTP 409 `code=40903`。冲突 → `40904`，message 含冲突活动标题。
- 信用：只 INSERT 流水再汇总，禁止 `UPDATE users SET credit_score=?`。
- 推荐：`degraded=true` 时仍 HTTP 200 `code=0`，list 为热度榜；前端不 toast 失败。
- 公开列表不得出现未审核活动。
- OTP：只向学校邮箱域发码；`channel=SMS` → `40001`；60 秒内重发 → `42900`。

## 4. 改接口的合法流程

1. 改 `contracts/openapi.yaml` 与 `contracts/examples/`
2. 改 `docs/04-api-contract.md` 与对应 TRD
3. `python scripts/check_contracts.py` 通过
4. 同一 PR 改提供方；调用方若会编译失败必须同 PR 或先标 breaking 并拆两个紧挨着的 PR

## 5. 完成定义（Agent 自检）

- 正例 JSON 能被自己的 DTO 反序列化，字段集合 ⊆ OpenAPI required+optional
- 每个写接口至少有一个 4xx 反例
- 单测不依赖真实微信/SMTP；用 wiremock 或内存桩
- README 启动命令在空 `.env.example` 复制后能跑到 `/health`
