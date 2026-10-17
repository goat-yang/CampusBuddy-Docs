# 协作与 PR

小组 **只维护这一个 GitHub 仓库**。现在合入的是 Markdown 与空工程；第 5 周起业务代码也在这里提 Pull Request。不要另开一个“只放代码”的仓，也不要每人一个私人目录。

任务按 **人** 拆，对标 CoursePilot 的 `[E1 草案]`：一条 Issue 只属于一个人。标题 `[A1][W1]` … `[F10][W10]`。不要再用「一周一张、A–F 六个勾选」。清单见 [17](17-weekly-board.md)。模块史诗仍是 #1–#6。

Project：https://github.com/users/goat-yang/projects/2 。新 Issue 若没自动出现，在看板 **Add item** 搜索 `[A1]` 或 `owner-a`。`python scripts/create_github_board.py` 按标题去重。

仓库协作者已经是这六人。A goat-yang 是管理员，其余五人有写权限。

| 角色 | GitHub | 模块 |
| --- | --- | --- |
| A | goat-yang | 鉴权、上大邮箱验证码 |
| B | hl-matrix | 活动、报名 |
| C | Tsuki0798 | 推荐 |
| D | cyyy135 | 信用、通知、后台接口 |
| E | lewubai | 小程序 |
| F | 3099276887 | Web、测试、部署 |

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

`.env`、微信密钥、问卷原始答卷、学号、手机号、邮箱、数据库 dump。人员表只放 GitHub 用户名。密钥泄露要先在微信/云上作废，再处理历史。
