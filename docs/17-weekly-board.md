# 十周按人任务板

对照 CoursePilot-Evo 的 Project：按周拆，每个人每周都有条目。GitHub Issues 用同一套标题；Issue 勾选是执行面，本文是真源。姓名填进 [人员表](team/team-signup-template.csv)。

**不要给每个人建交代码文件夹。** 代码按模块进仓库：

| 人 | 只往这里交 |
| --- | --- |
| A | `backend/src/main/java/com/shu/campusbuddy/auth/` 与 `common/` |
| B | `backend/.../activity/` |
| C | `backend/.../reco/` |
| D | `backend/.../credit/` |
| E | `miniprogram/` |
| F | `web/`、`deploy/` |

公共契约：`contracts/`。第 1–4 周以文档和空工程为主，第 5 周才大规模写业务。

## 第 1 周 需求对齐

| 人 | 本周必须完成 | 验收 |
| --- | --- | --- |
| A | 冻结登录用例；确认不收集学校 SSO 密码；整理人员 GitHub | 16 + 本仓 Collaborators 名单草稿 |
| B | 冻结活动/报名用例与状态机名词 | 与 04 用语一致，无「余量/剩余」混用 |
| C | 冻结推荐输入输出：`batchId, degraded, reason` | 12 与 04 一致 |
| D | 冻结信用流水与通知类型 | 无「直接改分」表述 |
| E | 小程序信息架构：首页/列表/详情/发布/我的 | 页面清单进 e-miniprogram |
| F | Web 信息架构 + 后台菜单 | 页面清单进 f-web |

## 第 2 周 契约冻结

| 人 | 本周必须完成 | 验收 |
| --- | --- | --- |
| A | `/auth/*` `/users/me` `/health` 字段与正反例 | OpenAPI 可过 `scripts/check_contracts.py` |
| B | 活动发布体、报名 40902/03/04 | examples 里至少 1 条满员反例 |
| C | `/reco/for-you` 与 event 埋点 | `degraded` 出现在 schema |
| D | 信用/通知/后台 overview 字段 | 看板数字全部有路径 |
| E | 对照 OpenAPI 列小程序将调用的 operationId | 不自造字段名 |
| F | 对照 OpenAPI 列 portal/admin operationId | 看板不前端累加 |

## 第 3 周 设计

| 人 | 本周必须完成 | 验收 |
| --- | --- | --- |
| A | 用户/令牌 ER；邮箱域白名单表 | 11 中有 campus_id、school_email |
| B | 活动/报名 ER；唯一索引说明 | `(activity_id,user_id)` 写进 11 |
| C | 召回-打分-降级序列图 | 无在线全表 UserCF |
| D | 流水表、通知表、Redis key 命名 | 无 UPDATE score |
| E | 小程序线框（可用手绘照片，不提交大图包） | 路径与 app.json 规划一致 |
| F | portal/admin 线框 + Compose 端口表 | 3306/6379/8080/5173/5174 |

## 第 4 周 空工程可跑

| 人 | 本周必须完成 | 验收 |
| --- | --- | --- |
| A | Spring Boot 能启动；`GET /api/v1/health` 返回封套 | curl 见 code=0 |
| B | 包路径与表结构 SQL 草案放 `backend/src/main/resources/db/`（实现周再执行） | 不抢跑写报名接口 |
| C | 特征/权重配置草稿，不接真实流量 | 配置 schema 已在 OpenAPI |
| D | Redis key 前缀文档；Knife4j 依赖列入 pom（实现周再开） | README 写清未启用 |
| E | 开发者工具能打开本仓 `miniprogram/` | 占位首页 |
| F | `docker compose up mysql redis`；portal/admin 占位页 | compose 健康检查绿 |

## 第 5 周 迭代一（登录与发布）

| 人 | 本周必须完成 | 验收 |
| --- | --- | --- |
| A | OTP 发送 + CAMPUS_OTP 首次设密 + CAMPUS_PASSWORD + JWT | 非上大邮箱 40001；60s 重发 42900 |
| B | 创建活动 DRAFT/PENDING；敏感词 40001 | 未审活动不出现在 GET /activities |
| C | 空实现：`/reco/for-you` 直接返回 hot 且 `degraded=true` | HTTP 200 code=0 |
| D | Redis 接通；限流中间件；敏感词词库加载给 B 调 | 无业务分接口也可先合 |
| E | 登录页（微信）+ 绑定学号提示；活动发布表单调 B | 字段名=OpenAPI |
| F | Web 登录（工号+密码/OTP）；发布表单 | 与 E 同一 API |

## 第 6 周 迭代一（报名闭环 = V0.1）

| 人 | 本周必须完成 | 验收 |
| --- | --- | --- |
| A | `/users/me` 资料；未绑学号报名 40300 | E/F 文案「先绑定学号」 |
| B | 报名锁、重复 40902、满员 40903、冲突 40904、候补 | 并发单测或脚本 |
| C | 维持降级热度榜，开始接可曝光活动查询 | 不读未审活动 |
| D | 发布审核接口给后台；完成报名不改信用 | PASS 后才 SIGNUP |
| E | 列表/详情/报名/我的已报 | 满员用 40903 文案 |
| F | 同左用户端 + 后台待审列表 | 审核 PASS/REJECT |

## 第 7 周 迭代二（推荐与信用）

| 人 | 本周必须完成 | 验收 |
| --- | --- | --- |
| A | 兴趣标签读写；个人主页脱敏 | 主页无学号邮箱 |
| B | 签到、结束活动、收藏；结束时调 D 结算 | finish 后 status=FINISHED |
| C | 加权打分 + reason；埋点 `/reco/event` | reason 非空；失败仍 200+degraded |
| D | 流水完整、完成+2/迟到取消-5/爽约-10 | 禁止 UPDATE score |
| E | 首页走 for-you；曝光埋点；信用页只读规则 | degraded 不 toast 失败 |
| F | 用户端推荐可选；信用流水页 | 数字来自接口 |

## 第 8 周 迭代二（运营后台 = V0.5）

| 人 | 本周必须完成 | 验收 |
| --- | --- | --- |
| A | 管理员角色只发 JWT，不在前端写死 | role 不来自 body |
| B | 候补递补、下线、名单仅发起人可见 | 非发起人 403/404 |
| C | `/admin/reco/config` 权重和=1.00 | 否则 40001 |
| D | 通知、举报、看板四接口、调分只追加流水 | F 看板禁止 reduce |
| E | 消息列表、评价、举报入口 | 未读数用 unreadCount |
| F | admin 全菜单 + ECharts 接 overview | 与接口字段一一对应 |

## 第 9 周 测试

| 人 | 本周必须完成 | 验收 |
| --- | --- | --- |
| A | 登录/OTP/越权用例 | 报告写入 docs 测试节或 F 汇总 |
| B | 并发报名与冲突回归 | 不超员 |
| C | 降级开关与 reason 自测 | Redis 停仍 200 |
| D | 流水一致性、审核、看板 | 改分可追溯 |
| E | 真机或开发者工具走通主路径 | 录屏素材给 F |
| F | 汇总测试报告与缺陷表 | 关闭 P0 |

## 第 10 周 部署与答辩

| 人 | 本周必须完成 | 验收 |
| --- | --- | --- |
| A | 演示账号 DEMO 标记；答辩登录脚本 | 不展示真实同学数据 |
| B | 演示用活动种子（假数据注明） | 不写成真实运营 |
| C | 推荐演示：一条带 reason，一条 degraded | 口播降级 |
| D | 看板有数；通知能弹出一条 | Compose 稳定 |
| E | 开发者工具扫码预览步骤写进 13 | 打到同一后端 |
| F | 网页发布、视频、行数统计、答辩 PPT 数据页 | 端口与 13 一致 |

周五例会只问：本周表里自己那格是否可勾。跨周任务写进 Issue 评论，不在私人目录堆文件。
