# 工程、调试与提交规范 v1

五人共用规范改写为六人。目标：代码边界清楚、报名不错额、接口不分叉、隐私不进 Git。当前仅文档，没有已通过这些规范的业务服务。

## 1. 结构与依赖

字段以 `contracts/openapi.yaml` 为准，禁止一端 `remainCount` 一端 `leftNum`。未知 JSON 字段拒绝。成功只认 HTTP 200 且 `code===0`。

| 模块 | 结构 | 禁止 |
| --- | --- | --- |
| 后端 | 单仓单应用；controller DTO → service → domain/state machine → mapper | 按人名建目录；Controller 里扣名额；Entity 直接返回给前端；多套冲突算法 |
| 推荐 | recall → rank → rerank → reason；离线 job 与在线查询分离 | 在线全表扫描算 UserCF；把模型分数当余量 |
| 小程序 | pages → components → api | 页面里 if 信用分判断能不能报 |
| Web | views → api adapter → pinia | 每个页复制 Axios；看板前端 reduce 当官方指标 |

一段规则一个 Owner。工具类只放稳定、多调用方需要的逻辑。

函数做一件事。核心规则（名额、冲突、加分）必须能单测。

## 2. 类型、配置、状态

金额类字段本期没有。人数、余量用整数；信用分用整数 0–100。未知不要改成 0。DTO、Entity、VO 分开。

配置唯一入口：端口、JWT 密钥、微信 AppId、Redis、OSS、CORS。缺必要配置时启动失败。禁止默认密钥 `secret` 进生产 compose。

状态迁移只在 service。迟到的报名响应不能覆盖更新后的余量展示：前端以最后一次 GET 为准。

## 3. 错误与调试

统一 code/message/field/retryable。满员、冲突、信用不足是业务错误；Redis 超时是系统错误。不能 catch Exception 后返回空列表装成功。

日志字段：time、level、requestId、uidHash、path、status、durationMs。禁止验证码、JWT、手机号明文、学号明文。

Debug 顺序：最小复现 → requestId 串起网关与 service → 看活动 status 与 remain → 看 Redis 计数是否与 DB 一致 → 看前端是否用了缓存余量。不要靠把上限改成 999 掩盖竞态。

## 4. 安全

- 对象级授权：只能改自己的活动；管理员走 /admin。
- SQL 参数化；排序字段白名单。
- 文本不 v-html 渲染用户输入。
- 上传限制 jpg/png、2MB、服务端改名、禁路径穿越。
- 限流默认 60 次/分钟/用户；发布每日 3 条。
- `.env` 不入库。泄露先轮换再删文件。

这些降低已识别风险，不承诺“无安全问题”。

## 5. 构建与行数

UTF-8、LF。后端 Maven Wrapper + JUnit；Web ESLint/Prettier；小程序按微信开发者工具构建。第一次加可执行代码就加 README 启动方式。

有效代码行：去空行去纯注释，排除 target、node_modules、生成代码、静态图。禁止为凑 10000 行加空方法。测试代码计入“测试与脚本”桶，不计业务模块虚高。

## 6. 提交前

- 只 stage 任务内文件，不要把问卷回执、密钥和数据库 dump 加进来。
- 接口变更同步 `contracts/openapi.yaml`、examples 与 04 契约。
- 跑与自己模块相关的测试。
- 写清限制：微信未通、Redis 降级、造数。

## 7. PR

短分支：`feat/a-jwt`、`feat/b-enroll-lock`、`feat/c-hot-fallback`。main 可集成。普通 PR 至少一名队友；契约由提供者+消费者；名额/信用/权重由 Owner 看。
