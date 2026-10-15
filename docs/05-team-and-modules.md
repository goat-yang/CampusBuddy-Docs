# 六人分工

六人：四名后端、两名前端。Web 端和小程序端共用同一套后端接口。分工与课程开发方案一致：一人主导一块，跨端互相 backup，文档责任到人。姓名组内填写。

| 成员 | 角色 | 主要负责内容 | 预估代码量 |
| --- | --- | --- | --- |
| A | 项目经理 / 后端 | 项目计划、需求分析；用户表、学号/工号登录、**上大邮箱验证码**、本平台密码、JWT/RBAC、个人主页 | 1500 行 + 需求文档 |
| B | 后端开发 | 活动发布与检索、报名状态机、候补递补、签到、收藏 | 1700 行 + 数据库设计文档 |
| C | 算法 / 后端开发 | 智能推荐：召回、打分、UserCF、离线特征、埋点、推荐配置接口 | 1800 行 + 推荐算法设计说明 |
| D | 后端开发 | 信用体系、消息通知与定时任务、举报风控、敏感词、管理后台统计接口、Redis | 1600 行 + 接口文档（Knife4j） |
| E | 小程序前端 | 小程序全部页面：首页推荐流、列表、详情、发布、报名、消息、我的 | 2400 行 |
| F | Web 前端 / 测试 | Web 用户端与管理后台、ECharts 看板；测试用例、部署文档、演示视频 | 2800 行 + 测试文档 |

合计目标约 10800–14600 行有效代码，满足课程不低于 10000 行。后端四人维护同一个 Spring Boot 工程，按模块分包，不要拆成四个服务。

**不要按人名建交代码文件夹**（不要 `A/`、`张三/`）。对照 CoursePilot：模块目录 + Issue/Project 按周拆任务。

| 成员 | 仓库路径 |
| --- | --- |
| A | `backend/src/main/java/com/shu/campusbuddy/auth/`、`common/` |
| B | `backend/src/main/java/com/shu/campusbuddy/activity/` |
| C | `backend/src/main/java/com/shu/campusbuddy/reco/` |
| D | `backend/src/main/java/com/shu/campusbuddy/credit/` |
| E | `miniprogram/` |
| F | `web/`、`deploy/` |

按周每人任务：[17 周看板](17-weekly-board.md)。环境：[18](18-dev-environment.md)。

子 TRD：[A](trd/a-auth.md) · [B](trd/b-activity.md) · [C](trd/c-reco.md) · [D](trd/d-credit-ops.md) · [E](trd/e-miniprogram.md) · [F](trd/f-web.md)。校园身份规则：[16](16-campus-identity.md)。

## A 负责：上大邮箱验证码（V1.0 登录主路径）

这是 A 的后端所有权，不拆给 D（D 只提供 Redis，不写发码逻辑），也不拆给 E/F（前端只调接口、画页面）。

| A 做 | 不做 / 交给谁 |
| --- | --- |
| `POST /auth/otp/send`：校验学号、学校邮箱域白名单、60 秒限发、6 位码、5 分钟 TTL | E/F：登录页、发码按钮、倒计时文案 |
| 把验证码发到 `@shu.edu.cn` 等白名单域（SMTP 配置走环境变量） | 不发手机短信（课设不做）；不调学校短信网关 |
| `CAMPUS_OTP` 首次设本平台密码；之后 `CAMPUS_PASSWORD` | 不收集、不校验学校统一认证密码 |
| 未绑定 `campusId` 的微信账号报名/发布返回 403 | E/F 展示「先绑定学号」 |
| 邮箱域名单、演示账号 `DEMO` 标记、验证码不写日志明文 | SSO 申请由 A 对接信息办，获批后再加 `SHU_OAUTH` |

验收归 A：非上大邮箱拒发；重复发送 429；码错 40001；过期重发；同一学号在小程序和网页能登进同一个用户。

```mermaid
flowchart LR
  A[A 用户鉴权] --> E[E 小程序]
  A --> F[F Web与后台]
  B[B 活动报名] --> E
  B --> F
  B --> C[C 推荐]
  D[D 信用通知后台] --> F
  D --> C
  C --> E
  F --> DEP[同一后端部署]
  E --> DEP
```

接口变更先改 [契约](04-api-contract.md)。E 与 F 不得在前端实现名额、信用或推荐分。C 不写报名锁。部署见 [落地部署](13-deployment.md)。
