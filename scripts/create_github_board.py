#!/usr/bin/env python3
"""Create one GitHub Issue per person per week (CoursePilot style: [A1][W1], not a shared weekly card)."""
from __future__ import annotations

import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

OWNER = "goat-yang"
REPO = "CampusBuddy-Docs"
API = "https://api.github.com"

WEEKS = {
    1: "需求对齐",
    2: "契约冻结",
    3: "设计",
    4: "空工程可跑",
    5: "迭代一 登录与发布",
    6: "迭代一 报名闭环 V0.1",
    7: "迭代二 推荐与信用",
    8: "迭代二 运营后台 V0.5",
    9: "测试",
    10: "部署与答辩",
}

TRD = {
    "A": ("docs/trd/a-auth.md", "`backend/.../auth` 与 `common`"),
    "B": ("docs/trd/b-activity.md", "`backend/.../activity`"),
    "C": ("docs/trd/c-reco.md", "`backend/.../reco`"),
    "D": ("docs/trd/d-credit-ops.md", "`backend/.../credit`"),
    "E": ("docs/trd/e-miniprogram.md", "`miniprogram/`"),
    "F": ("docs/trd/f-web.md", "`web/` 与 `deploy/`"),
}

# (week, owner, title, scope, accept)
SLICES = [
    (1, "A", "冻结校园登录：工号 + 上大邮箱 OTP，禁止 SSO 密码", "确认登录用例；整理人员 GitHub；写清不收集学校统一认证密码", "docs/16 与 Collaborators 名单草稿齐"),
    (1, "B", "冻结活动与报名状态机名词", "统一 DRAFT/PENDING/SIGNUP 等词；禁止「余量/剩余」混用", "与 docs/04 用语一致"),
    (1, "C", "冻结推荐输出 batchId、degraded、reason", "对齐 docs/12 与契约字段", "三字段进入 04/OpenAPI 草案"),
    (1, "D", "冻结信用流水与通知类型", "禁止「直接改分」表述", "流水只追加写进需求"),
    (1, "E", "列出小程序信息架构", "首页/列表/详情/发布/报名/消息/我的", "页面清单进 e-miniprogram"),
    (1, "F", "列出 Web 用户端与后台菜单", "portal 与 admin 菜单", "页面清单进 f-web"),
    (2, "A", "冻结鉴权 OpenAPI 与正反例", "`/auth/*` `/users/me` `/health`", "python scripts/check_contracts.py 通过"),
    (2, "B", "冻结活动发布体与报名冲突码", "发布体；40902/40903/40904", "examples 至少一条满员反例"),
    (2, "C", "冻结 /reco/for-you 与埋点字段", "scene、eventType、degraded", "degraded 出现在 schema"),
    (2, "D", "冻结信用、通知与看板字段", "overview 数字全部有路径", "F 不能前端累加"),
    (2, "E", "列出小程序将调用的 operationId", "对照 OpenAPI，不自造字段名", "清单进 e-miniprogram"),
    (2, "F", "列出 portal/admin 将调用的 operationId", "看板不前端 reduce", "清单进 f-web"),
    (3, "A", "画出用户与令牌 ER，含邮箱域白名单", "campus_id、school_email", "写入 docs/11"),
    (3, "B", "画出活动报名 ER 与唯一索引", "(activity_id, user_id)", "写入 docs/11"),
    (3, "C", "画出召回-打分-降级序列图", "禁止在线全表 UserCF", "图进 docs/12 或 c-reco"),
    (3, "D", "命名流水表、通知表与 Redis key", "禁止 UPDATE score", "key 前缀文档"),
    (3, "E", "小程序线框", "路径与 app.json 规划一致；不提交大图包", "线框可手绘"),
    (3, "F", "portal/admin 线框与 Compose 端口表", "3306/6379/8080/5173/5174", "端口写入 18"),
    (4, "A", "让 Spring Boot 启动并返回 health 封套", "GET /api/v1/health → code=0", "curl 可复现"),
    (4, "B", "提交建表 SQL 草案，不抢跑报名接口", "backend/src/main/resources/db/", "本周无报名 API"),
    (4, "C", "写下特征与权重配置草稿", "不接真实流量", "配置已在 OpenAPI"),
    (4, "D", "写 Redis 前缀；pom 列入 Knife4j 但不启用", "README 写清未启用", "无业务分接口也可"),
    (4, "E", "用开发者工具打开本仓 miniprogram", "占位首页可编译", "touristappid 即可"),
    (4, "F", "compose 拉起 MySQL 与 Redis，占位页可开", "docker compose up mysql redis", "健康检查绿"),
    (5, "A", "实现邮箱 OTP、首次设密、平台密码与 JWT", "非上大邮箱 40001；60s 重发 42900", "CAMPUS_OTP / CAMPUS_PASSWORD 可登录"),
    (5, "B", "实现创建活动 DRAFT/PENDING 与敏感词拦截", "未审活动不出现在 GET /activities", "敏感词 40001"),
    (5, "C", "提供降级推荐：hot 且 degraded=true", "HTTP 200 code=0", "前端不 toast 失败"),
    (5, "D", "接通 Redis、限流，并把敏感词加载给 B", "无信用接口也可先合", "B 能调词库"),
    (5, "E", "做小程序登录页、绑学号提示与发布表单", "字段名=OpenAPI", "未绑学报名不可点通"),
    (5, "F", "做 Web 工号登录/OTP 与发布表单", "与 E 同一 API", "不要求 SSO 密码"),
    (6, "A", "实现 /users/me；未绑学号报名返回 40300", "E/F 文案「先绑定学号」", "微信未绑不可报"),
    (6, "B", "实现报名锁、候补与 40902/03/04", "Redis DECR + 唯一索引", "并发上限 1 不超员"),
    (6, "C", "降级热度榜只读可曝光活动", "不读未审活动", "列表无 PENDING"),
    (6, "D", "实现发布审核接口；报名不改信用", "PASS 后才 SIGNUP", "驳回立即不可见"),
    (6, "E", "做列表、详情、报名、我的已报", "满员用 40903 文案", "报名 ≤3 步"),
    (6, "F", "做用户端报名闭环与后台待审列表", "审核 PASS/REJECT", "与小程序同一条活动"),
    (7, "A", "实现兴趣标签与脱敏个人主页", "主页无学号、邮箱明文", "公开 profile 无联系方式"),
    (7, "B", "实现签到、结束活动与收藏；结束时调 D 结算", "finish 后 FINISHED", "到场名单交给 D"),
    (7, "C", "实现加权打分、reason 与 /reco/event", "reason 非空；失败仍 200+degraded", "埋点字段齐全"),
    (7, "D", "实现信用流水 +2/-5/-10", "禁止 UPDATE score", "可追溯 before/after"),
    (7, "E", "首页走 for-you，做曝光埋点与只读信用页", "degraded 不 toast 失败", "eventType=EXPOSE"),
    (7, "F", "做信用流水页；推荐可选接入", "数字来自接口", "不前端加总分"),
    (8, "A", "管理员角色只放进 JWT", "role 不来自 body", "普通用户打 /admin 得 40300"),
    (8, "B", "实现候补递补、下线、名单仅发起人可见", "非发起人 403/404", "取消后按序递补"),
    (8, "C", "实现 /admin/reco/config，权重和必须为 1.00", "否则 40001", "F 表单调此接口"),
    (8, "D", "实现通知、举报、看板与调分流水", "F 看板禁止 reduce", "四条 stats 路径齐"),
    (8, "E", "做消息、评价与举报入口", "未读数用 unreadCount", "不另开未读接口"),
    (8, "F", "做 admin 全菜单与 ECharts 接 overview", "与接口字段一一对应", "V0.5 可演示"),
    (9, "A", "补登录、OTP、越权测试", "报告交给 F 汇总", "越权对象 40400"),
    (9, "B", "做并发报名与冲突回归", "不超员", "40904 message 含冲突标题"),
    (9, "C", "自测降级开关与 reason", "Redis 停仍 200", "degraded=true"),
    (9, "D", "核流水一致性、审核与看板", "改分可追溯", "P0 关闭"),
    (9, "E", "开发者工具走通主路径并录屏", "素材给 F", "发布≤5 步报名≤3 步"),
    (9, "F", "汇总测试报告与缺陷表", "关闭 P0", "干净环境冒烟"),
    (10, "A", "准备 DEMO 账号与答辩登录脚本", "不展示真实同学数据", "DEMO 标记可识别"),
    (10, "B", "准备演示活动种子并标明假数据", "不写成真实运营", "种子可反复导入"),
    (10, "C", "准备一条 reason 演示和一条 degraded 演示", "口播降级", "两条都能 200"),
    (10, "D", "让看板有数并弹出一条通知", "Compose 稳定", "overview 非全 0"),
    (10, "E", "把扫码预览步骤写入 docs/13", "打到同一后端", "真机不用 localhost"),
    (10, "F", "发布网页、视频、行数与答辩数据页", "端口与 13 一致", "行数统计不含空方法"),
]

OLD_WEEKLY = {f"[W{w}] {title}" for w, title in WEEKS.items()}


def token() -> str:
    env = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if env:
        return env.strip()
    out = subprocess.check_output(
        ["git", "credential", "fill"],
        input="protocol=https\nhost=github.com\n\n",
        text=True,
    )
    for line in out.splitlines():
        if line.startswith("password="):
            return line.split("=", 1)[1]
    raise SystemExit("No GitHub token")


def req(method: str, url: str, tok: str, body=None):
    data = None if body is None else json.dumps(body).encode("utf-8")
    headers = {
        "Authorization": f"Bearer {tok}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "campusbuddy-board",
    }
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as resp:
            raw = resp.read().decode("utf-8")
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as exc:
        err = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {url} -> {exc.code} {err}") from exc


def all_issues(tok: str) -> list[dict]:
    items = []
    page = 1
    while True:
        batch = req(
            "GET",
            f"{API}/repos/{OWNER}/{REPO}/issues?state=all&per_page=100&page={page}",
            tok,
        )
        if not batch:
            break
        items.extend(x for x in batch if "pull_request" not in x)
        if len(batch) < 100:
            break
        page += 1
    return items


def slice_title(week: int, owner: str, title: str) -> str:
    return f"[{owner}{week}][W{week}] {title}"


def body_for(week: int, owner: str, title: str, scope: str, accept: str) -> str:
    trd, path = TRD[owner]
    kind = "文档/设计" if week <= 4 else "实现/验收"
    return f"""## 完成后能做什么
{title}

主要负责人：成员 {owner}（姓名填 [人员表](https://github.com/{OWNER}/{REPO}/blob/main/docs/team/team-signup-template.csv)）
计划周次：第 {week} 周 · {WEEKS[week]}（{kind}）

## 属于模块的哪一块
- TRD：https://github.com/{OWNER}/{REPO}/blob/main/{trd}
- 只改：{path}
- 契约：`contracts/openapi.yaml`；Agent 先读 `contracts/AGENT_CONTRACT.md`

## 本次范围
- {scope}

## 验收
- {accept}

一条 Issue 只属于 **{owner}**。不要把 A–F 写进同一张卡片。不要按人名建交代码文件夹。
对照 https://github.com/{OWNER}/{REPO}/blob/main/docs/17-weekly-board.md
"""


def main() -> None:
    tok = token()
    issues = all_issues(tok)
    by_title = {i["title"]: i for i in issues}

    created = 0
    for week, owner, title, scope, accept in SLICES:
        full = slice_title(week, owner, title)
        labels = [f"week-{week}", f"owner-{owner.lower()}", "type-docs" if week <= 4 else "type-code"]
        if full in by_title:
            print("skip", full)
            continue
        created_issue = req(
            "POST",
            f"{API}/repos/{OWNER}/{REPO}/issues",
            tok,
            {"title": full, "body": body_for(week, owner, title, scope, accept), "labels": labels},
        )
        by_title[full] = created_issue
        created += 1
        print("issue", created_issue.get("number"), full)
        time.sleep(0.35)

    for old in issues:
        title = old["title"]
        if title in OLD_WEEKLY and old["state"] == "open":
            req(
                "POST",
                f"{API}/repos/{OWNER}/{REPO}/issues/{old['number']}/comments",
                tok,
                {
                    "body": "已按 CoursePilot 方式拆成「一人一条」。请改用标题 `[A1][W1]` … `[F10][W10]` 的 Issue，本条关闭以免看板重复。"
                },
            )
            req(
                "PATCH",
                f"{API}/repos/{OWNER}/{REPO}/issues/{old['number']}",
                tok,
                {"state": "closed", "state_reason": "not_planned"},
            )
            print("closed", old["number"], title)

    print("created", created, "open_person_slices", sum(1 for t in by_title if t.startswith("[A") or t.startswith("[B")))
    print("done")


if __name__ == "__main__":
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    main()
