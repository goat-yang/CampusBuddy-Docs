# 公共接口契约 v1.0

小程序、Web 用户端、管理后台共用前缀 `/api/v1`。**字段名、类型、枚举、HTTP 与 `code` 以 [contracts/openapi.yaml](../contracts/openapi.yaml) 为真源**；本文写行为、状态机与所有权。Agent 先读 [AGENT_CONTRACT.md](../contracts/AGENT_CONTRACT.md)。服务尚未实现。

## 1. 调用与封套

`Authorization: Bearer <accessToken>`。Access Token 不超过 2 小时。刷新只用 `POST /auth/refresh`。权限看服务端角色，不看 body 里的 `userId`/`role`。不属于当前用户的对象统一 404。

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "requestId": "uuid"
}
```

`code=0` 成功。时间字段均为北京时间 `yyyy-MM-dd HH:mm:ss`。分页：`pageNum`（从 1）、`pageSize`（默认 10，最大 50）、`total`。列表 `data` 为 `{ "total": 0, "list": [] }`。写接口（报名、发布、审核、调分）请求头带 `Idempotency-Key`。

| HTTP | code | 含义 |
| ---: | --- | --- |
| 200 | 0 | 成功 |
| 400 | 40001 | 参数校验失败，`message` 指出字段 |
| 401 | 40100 | 未登录或令牌过期 |
| 403 | 40300 | 已登录无权限 |
| 404 | 40400 | 不存在或不属于你 |
| 409 | 40901 活动状态不允许；40902 重复报名；40903 已满员；40904 时间冲突；40905 幂等键内容不一致 | |
| 429 | 42900 | 限流或每日发布上限 |
| 500 | 50000 | 系统错误 |

报名失败必须用 40902/40903/40904，方便两端写同一套文案。

## 2. 共用对象

**UserBrief** `{ id, nickname, avatar, creditScore, creditLevel, college }`  
`creditLevel`：`EXCELLENT|GOOD|NORMAL|RESTRICTED`（对应 90–100 / 75–89 / 60–74 / 0–59）。不含手机号、学号/工号、openid、学校邮箱明文。

**ActivityCard** `{ id, title, type, tags, campus, place, startTime, endTime, enrollDeadline, minNum, maxNum, enrollNum, remainCount, status, cover, publisher, reason }`  
`reason` 仅推荐接口有值，列表接口为 `null`。`type`：`MEAL|SPORT|STUDY|TRIP|CONTEST|FUN|OTHER`。

**ActivityDetail** 在 Card 上增加 `{ description, images, publisherProfile, myEnrollmentStatus, waitNum }`。

**Enrollment** `{ id, activityId, user, status, applyMsg, signupTime }`  
`status`：`APPLYING|APPROVED|WAITLIST|REJECTED|CANCELED|NOSHOW|DONE`。

活动 `status`：`DRAFT|PENDING|REJECTED|PUBLISHED|SIGNUP|GROUPED|ONGOING|FINISHED|OFFLINE`。对外列表只出现 `PUBLISHED`（未到报名但已审过，可选）与 `SIGNUP`。可报名仅 `SIGNUP`。

## 3. 接口清单（开发方案 42 条 + 验证码发送）

路径均相对 `/api/v1`。责任与开发方案一致。校园身份规则见 [16](16-campus-identity.md)。

### 认证与用户（7 + 发码）A

| # | 方法 | 路径 | 请求 | 成功 data |
| ---: | --- | --- | --- | --- |
| — | GET | /health | — | `{ "status":"UP", "service":"campusbuddy" }` 无鉴权，仅探活 |
| — | POST | /auth/otp/send | `{ "channel":"EMAIL", "campusId", "email" }` | `{ "ttlSec": 300 }` |
| 1 | POST | /auth/login | `{ "mode":"CAMPUS_PASSWORD"\|"CAMPUS_OTP"\|"WECHAT", "campusId"?, "password"?, "email"?, "otp"?, "code"? }` | `{ accessToken, refreshToken, expiresIn, user }` |
| 2 | POST | /auth/refresh | `{ "refreshToken" }` | 同登录令牌字段 |
| 3 | GET | /users/me | — | 完整资料：Card 用户字段 + `campusIdMasked, grade, major, campus, bio, emailMasked` |
| 4 | PUT | /users/me | `{ nickname, avatar, college, grade, major, campus, bio }` | 更新后的 me |
| 5 | GET | /users/me/interests | — | `{ "tagIds": number[], "tags": [{id,name,category}] }` |
| 6 | PUT | /users/me/interests | `{ "tagIds": number[] }` 长度 3–10 | 同 GET |
| 7 | GET | /users/{id}/profile | — | 主页：UserBrief + 信用档 + 历史活动数 + 评价摘要；无联系方式、无学号 |

`CAMPUS_OTP`：校验邮箱验证码后，若该 `campusId` 尚未有密码则请求里必须带 `password`（首次设本平台密码）。`CAMPUS_PASSWORD`：`campusId` + 本平台密码。`WECHAT`：小程序 `code` 换 openid；未绑定 `campusId` 的账号只能浏览，报名/发布返回 40300，`message` 提示先绑定学号。

`/auth/otp/send` 只接受学校邮箱域（见 16）。60 秒内重复发送 → 42900。`channel=SMS` 接口可留枚举，V1.0 固定返回 40001「本期仅支持学校邮箱验证码」。不接收、不转发学校统一认证密码。`mode=SHU_OAUTH` 预留，未获信息办批准前不要实现。

### 活动（8）B

| # | 方法 | 路径 | 请求 | 成功 data |
| ---: | --- | --- | --- | --- |
| 8 | POST | /activities | 见下方发布体；草稿 `status=DRAFT` 可不审 | `{ id, status }` |
| 9 | PUT | /activities/{id} | 同发布体；仅 DRAFT/PENDING 可改全量，SIGNUP 只改说明与地点补充 | ActivityDetail |
| 10 | PATCH | /activities/{id}/offline | `{ "reason"? }` | `{ id, status:"OFFLINE" }` |
| 11 | GET | /activities | query：`type,tag,campus,keyword,timeRange,minCredit,sort,pageNum,pageSize` | `{ total, list: ActivityCard[] }` |
| 12 | GET | /activities/{id} | — | ActivityDetail |
| 13 | GET | /activities/hot | `pageSize` | `{ list: ActivityCard[] }` 来自 Redis ZSet |
| 14 | POST | /files/image | `multipart/form-data` 字段 `file`，jpg/png ≤2MB | `{ url }` |
| 15 | GET | /users/me/activities | `role=published|enrolled` | `{ total, list: ActivityCard[] }` |

发布体：

```json
{
  "title": "今晚宝山操场跑步",
  "type": "SPORT",
  "tagIds": [1, 2],
  "description": "轻松慢跑 5 公里",
  "campus": "BAOSHAN",
  "place": "操场",
  "placeNote": "西门集合",
  "startTime": "2026-10-20 19:30:00",
  "endTime": "2026-10-20 21:00:00",
  "enrollDeadline": "2026-10-20 19:00:00",
  "minNum": 3,
  "maxNum": 8,
  "imageUrls": [],
  "status": "PENDING"
}
```

校验：开始时间 ≥ 现在+30 分钟；截止 ≤ 开始；2≤maxNum≤20；minNum≤maxNum；标签 1–5 个。标题说明过敏感词 → 40001。

`sort`：`time|hot|credit|distance`。`timeRange`：`tonight|tomorrow|week|custom`，custom 时再传 `startFrom,startTo`。

### 报名与名单（6）B

| # | 方法 | 路径 | 请求 | 成功 data |
| ---: | --- | --- | --- | --- |
| 16 | POST | /activities/{id}/enroll | `{ "applyMsg": "≤200字" }` | `{ enrollmentId, status, remainCount }` |
| 17 | DELETE | /activities/{id}/enroll | — | `{ remainCount, promotedUserId? }` 取消后若有候补则自动递补 |
| 18 | GET | /activities/{id}/enrollments | 仅发起人/管理员 | `{ total, list: Enrollment[] }` |
| 19 | POST | /enrollments/{id}/audit | `{ "action":"APPROVE"|"REJECT"|"REMOVE", "reason"? }` | Enrollment |
| 20 | POST | /activities/{id}/sign | `{ "token" }` 签到码或扫码内容 | `{ signed: true }` |
| 21 | POST | /activities/{id}/finish | `{ "presentUserIds": number[] }` 缺席者记爽约 | `{ creditSettled: true }` |

报名顺序：状态=SIGNUP → 未截止 → 未重复 → 余量（满则 WAITLIST，候补开关默认开）→ 时间冲突 → 信用≥40 → 非黑名单。满员且候补关闭 → 40903。冲突 → 40904，`message` 含冲突活动标题与时间。

### 推荐（7）C

| # | 方法 | 路径 | 请求 | 成功 data |
| ---: | --- | --- | --- | --- |
| 22 | GET | /reco/for-you | `scene=home|search, campus, pageNum, pageSize` | `{ batchId, degraded, list: ActivityCard[] }` |
| 23 | GET | /reco/reasons/{activityId} | — | `{ reason, factors: {tag,time,geo,credit,hot,cf} }` |
| 24 | GET | /reco/similar/{activityId} | `pageSize` | `{ list: ActivityCard[] }` |
| 25 | GET | /reco/candidates/{activityId} | 发起人；P2 | `{ list: UserBrief[] }` |
| 26 | POST | /reco/invite | `{ activityId, userIds: number[] }` P2 | `{ sent: number }` |
| 27 | POST | /reco/event | `{ batchId, activityId, eventType, position, scene }` | `{ ok: true }` |
| 28 | POST | /reco/refresh | — | `{ ok: true }` 清该用户缓存 |

`eventType`：`EXPOSE|CLICK|SIGNUP|IGNORE|DISLIKE`。`degraded=true` 时 list 仍是热度榜，前端不弹错误。Redis 不可用必须走这条。

### 信用、消息、收藏、评价、举报（11）D 为主，收藏 B

| # | 方法 | 路径 | 责任 | 请求 / data |
| ---: | --- | --- | --- | --- |
| 29 | GET | /users/me/credit/logs | D | data：`{ total, list:[{ id, changeScore, beforeScore, afterScore, reason, bizType, createTime }] }` |
| 30 | GET | /credit/rule | D | `{ init:100, complete:+2, lateCancel:-5, noshow:-10, banPublishBelow:60, banEnrollBelow:40 }` |
| 31 | GET | /notifications | D | query 分页；list：`{ id, type, title, content, bizId, readFlag, createTime }` |
| 32 | PATCH | /notifications/{id}/read | D | `{ readFlag: true }` |
| 33 | POST | /notifications/subscribe | D | `{ templateIds: string[] }` 小程序授权上报 |
| 34 | POST | /favorites/{id} | B | 收藏活动；data `{ favorited: true }` |
| 35 | GET | /favorites | B | `{ total, list: ActivityCard[] }` |
| 36 | POST | /reviews | D | `{ activityId, toUserId?, score:1-5, content, tags[] }` |
| 37 | GET | /activities/{id}/reviews | D | `{ total, list:[{ fromUser, score, content, createTime }] }` |
| 38 | POST | /reports | D | `{ targetType:"ACTIVITY"|"USER", targetId, reasonType, content, evidenceUrls[] }` |
| 39 | GET | /reports/{id} | D | 举报人看自己的工单进度 |

`reasonType`：`AD|HARASS|FAKE|DISCRIMINATION|OTHER`。通知未读数：GET /notifications 的响应可带 `unreadCount`，不再单开接口。

### 管理后台（3 组，计 3 个编号槽 + 下列子路径）D，推荐配置由 C 实现、经 /admin 暴露

为与开发方案 42 条对齐，后台计为 40–42 三条「工作台」，内部子路径如下，均需管理员角色。

**40 审核与用户**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /admin/activities/pending | `{ total, list: ActivityDetail[] }` |
| POST | /admin/activities/{id}/audit | `{ "action":"PASS"|"REJECT", "reason"? }` |
| GET | /admin/users | query：`nickname,college,creditMin,creditMax,pageNum,pageSize` |
| PATCH | /admin/users/{id}/status | `{ "status":"NORMAL"|"BANNED_PUBLISH"|"BANNED_ENROLL"|"BANNED_ALL" }` |
| PATCH | /admin/users/{id}/credit | `{ "delta": -10, "reason": "..." }` 只追加流水 |

**41 举报、推荐参数**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /admin/reports | 工单列表 |
| PATCH | /admin/reports/{id} | `{ "action":"CONFIRM"|"DENY", "result":"..." }` |
| GET | /admin/reco/config | `{ weights:{ tag,time,geo,credit,hot,cf }, recallSize, cacheTtlSec }` |
| PUT | /admin/reco/config | 同结构；weights 之和必须为 1.00，否则 40001。实现归 C |

**42 看板与日志**

| 方法 | 路径 | data |
| --- | --- | --- |
| GET | /admin/stats/overview | `{ users, dau, todayActivities, todayEnrolls, groupRate, recoCvr }` |
| GET | /admin/stats/activity-type | `[{ type, publishCount, enrollCount, groupRate }]` |
| GET | /admin/stats/credit-distribution | `{ EXCELLENT, GOOD, NORMAL, RESTRICTED }` |
| GET | /admin/logs | `{ total, list:[{ adminId, module, action, target, ip, createTime }] }` |

看板数字只来自这些接口，禁止前端自己累加。

## 4. 两端如何对接

| 能力 | 小程序 E | Web 用户 F | 后台 F |
| --- | --- | --- | --- |
| 登录 | WECHAT 预览；报名须已绑学号 | CAMPUS_PASSWORD / 首次 CAMPUS_OTP | 管理员本平台密码 + 角色 |
| 列表/详情/报名 | 11/12/16 | 同左 | 不报名 |
| 首页推荐 | 22 + 27 曝光 | 可选同 22 | — |
| 发布 | 8 + 14 | 8 + 14 | 40 审核 |
| apiBase | 见 [部署](13-deployment.md) | `VITE_API_BASE` | 同域或 `VITE_API_BASE` |

字段名禁止一端 `remainCount` 一端 `leftNum`。新增字段先改本文再改三个客户端。
