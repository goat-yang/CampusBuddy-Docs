# 协作与 PR

小组 **只维护这一个 GitHub 仓库**。现在合入的是 Markdown 与空工程；第 5 周起业务代码也在这里提 Pull Request。不要另开一个“只放代码”的仓，也不要每人一个私人目录。

按周任务真源：[17 周看板](17-weekly-board.md)。GitHub Issues 标题 `[W1] …`～`[W10] …`，每条含 A–F 勾选（#7–#16），模块史诗 #1–#6。

GitHub **Project** 看板需要 token 带 `project` 权限，脚本建不了。仓库页 Projects → New project → 把 #7–#16 加进去，一列一周即可。重复跑 `python scripts/create_github_board.py` 不会建重复 Issue。

当前成员 GitHub 账号待收集，收集后 Settings → Collaborators 发邀请，对方接受才有写权限。

## 1. 怎么交

从 `main` 拉短分支，例如 `docs/a-login-use-case`、`feat/b-enroll-lock`。做完最小可验收切片 → 自己跑相关检查 → 开 PR → 至少一名队友看完再合并。不要把推荐公式和登录页放在同一个 PR。

改接口先改 [契约](04-api-contract.md)，再改调用方。`main` 保持可看、以后也可构建。

## 2. 谁核

| 改动 | 谁核 |
| --- | --- |
| 文档、范围 | A 或提出需求的人 |
| 活动/报名接口与页面 | B + E 或 F |
| 推荐 | C + 一个调用方 |
| 信用/后台 | D + F |
| 登录、上大邮箱验证码、学号绑定 | A 核后端；E 或 F 核对应端页面 |
| Compose | F + 一名后端 |

## 3. 不要提交

`.env`、微信密钥、问卷原始答卷、填好的队员表、数据库 dump。密钥泄露要先在微信/云上作废，再处理历史。
