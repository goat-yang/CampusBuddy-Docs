#!/usr/bin/env python3
"""Create labels, weekly issues (A-F checklists), optional Project v2. Idempotent by title."""
from __future__ import annotations

import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path

OWNER = "goat-yang"
REPO = "CampusBuddy-Docs"
API = "https://api.github.com"
GRAPHQL = "https://api.github.com/graphql"

WEEKS = [
    (1, "需求对齐"),
    (2, "契约冻结"),
    (3, "设计"),
    (4, "空工程可跑"),
    (5, "迭代一 登录与发布"),
    (6, "迭代一 报名闭环 V0.1"),
    (7, "迭代二 推荐与信用"),
    (8, "迭代二 运营后台 V0.5"),
    (9, "测试"),
    (10, "部署与答辩"),
]

# Keep in sync with docs/17-weekly-board.md
TASKS = {
    1: {
        "A": "冻结登录用例；确认不收集学校 SSO 密码；整理人员 GitHub",
        "B": "冻结活动/报名用例与状态机名词",
        "C": "冻结推荐输入输出：batchId, degraded, reason",
        "D": "冻结信用流水与通知类型",
        "E": "小程序信息架构：首页/列表/详情/发布/我的",
        "F": "Web 信息架构 + 后台菜单",
    },
    2: {
        "A": "/auth/* /users/me /health 字段与正反例",
        "B": "活动发布体、报名 40902/03/04",
        "C": "/reco/for-you 与 event 埋点",
        "D": "信用/通知/后台 overview 字段",
        "E": "对照 OpenAPI 列小程序将调用的 operationId",
        "F": "对照 OpenAPI 列 portal/admin operationId",
    },
    3: {
        "A": "用户/令牌 ER；邮箱域白名单表",
        "B": "活动/报名 ER；唯一索引 (activity_id,user_id)",
        "C": "召回-打分-降级序列图",
        "D": "流水表、通知表、Redis key 命名",
        "E": "小程序线框",
        "F": "portal/admin 线框 + Compose 端口表",
    },
    4: {
        "A": "Spring Boot 启动；GET /api/v1/health 封套",
        "B": "表结构 SQL 草案，不抢跑报名接口",
        "C": "特征/权重配置草稿",
        "D": "Redis key 前缀；Knife4j 依赖列入 pom",
        "E": "开发者工具打开 miniprogram/",
        "F": "docker compose up mysql redis；占位页",
    },
    5: {
        "A": "OTP + CAMPUS_OTP 首次设密 + CAMPUS_PASSWORD + JWT",
        "B": "创建活动 DRAFT/PENDING；敏感词 40001",
        "C": "/reco/for-you 返回 hot 且 degraded=true",
        "D": "Redis、限流、敏感词加载给 B",
        "E": "登录页 + 绑定学号提示 + 发布表单",
        "F": "Web 工号登录/OTP + 发布表单",
    },
    6: {
        "A": "/users/me；未绑学号报名 40300",
        "B": "报名锁、40902/03/04、候补",
        "C": "降级热度榜只读可曝光活动",
        "D": "发布审核接口；报名不改信用",
        "E": "列表/详情/报名/我的已报",
        "F": "用户端同左 + 后台待审",
    },
    7: {
        "A": "兴趣标签；个人主页脱敏",
        "B": "签到、结束、收藏；结束调 D 结算",
        "C": "加权打分 + reason + /reco/event",
        "D": "流水 complete+2 / lateCancel-5 / noshow-10",
        "E": "首页 for-you；曝光埋点；信用只读",
        "F": "信用流水页；数字来自接口",
    },
    8: {
        "A": "管理员角色只在 JWT",
        "B": "候补递补、下线、名单仅发起人",
        "C": "/admin/reco/config 权重和=1.00",
        "D": "通知、举报、看板、调分只追加流水",
        "E": "消息、评价、举报；unreadCount",
        "F": "admin 全菜单 + ECharts 接 overview",
    },
    9: {
        "A": "登录/OTP/越权测试",
        "B": "并发报名与冲突回归",
        "C": "降级开关与 reason 自测",
        "D": "流水一致性、审核、看板",
        "E": "开发者工具走通主路径并录屏",
        "F": "汇总测试报告与缺陷表",
    },
    10: {
        "A": "DEMO 账号与答辩登录脚本",
        "B": "演示活动种子（标明假数据）",
        "C": "一条 reason + 一条 degraded 演示",
        "D": "看板有数；一条通知",
        "E": "扫码预览步骤写入 docs/13",
        "F": "网页、视频、行数、答辩数据页",
    },
}

EPICS = [
    ("[Epic][A] 鉴权与上大邮箱 OTP", "owner-a", "docs/trd/a-auth.md\nbackend/.../auth + common"),
    ("[Epic][B] 活动报名状态机", "owner-b", "docs/trd/b-activity.md\nbackend/.../activity"),
    ("[Epic][C] 可解释推荐与降级", "owner-c", "docs/trd/c-reco.md\nbackend/.../reco"),
    ("[Epic][D] 信用、通知、后台", "owner-d", "docs/trd/d-credit-ops.md\nbackend/.../credit"),
    ("[Epic][E] 微信小程序", "owner-e", "docs/trd/e-miniprogram.md\nminiprogram/"),
    ("[Epic][F] Web 用户端、后台与部署", "owner-f", "docs/trd/f-web.md\nweb/ deploy/"),
]


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
    print("No GitHub token", file=sys.stderr)
    raise SystemExit(1)


def req(method: str, url: str, tok: str, body=None, extra=None):
    data = None if body is None else json.dumps(body).encode("utf-8")
    headers = {
        "Authorization": f"Bearer {tok}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "campusbuddy-board",
    }
    if extra:
        headers.update(extra)
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as resp:
            raw = resp.read().decode("utf-8")
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as exc:
        err = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {url} -> {exc.code} {err}") from exc


def existing_issue_titles(tok: str) -> set[str]:
    titles = set()
    page = 1
    while True:
        items = req(
            "GET",
            f"{API}/repos/{OWNER}/{REPO}/issues?state=all&per_page=100&page={page}",
            tok,
        )
        if not items:
            break
        for it in items:
            if "pull_request" not in it:
                titles.add(it["title"])
        if len(items) < 100:
            break
        page += 1
    return titles


def ensure_labels(tok: str) -> None:
    wanted = []
    colors = {
        "week": "1d76db",
        "owner": "0e8a16",
        "type": "5319e7",
        "epic": "b60205",
    }
    for i in range(1, 11):
        wanted.append((f"week-{i}", colors["week"], f"Week {i}"))
    for owner in "ABCDEF":
        wanted.append((f"owner-{owner.lower()}", colors["owner"], f"Owner {owner}"))
    wanted.append(("type-docs", colors["type"], "docs/design"))
    wanted.append(("type-code", colors["type"], "implementation"))
    wanted.append(("epic", colors["epic"], "module epic"))
    have = {x["name"] for x in req("GET", f"{API}/repos/{OWNER}/{REPO}/labels?per_page=100", tok)}
    for name, color, desc in wanted:
        if name in have:
            continue
        req(
            "POST",
            f"{API}/repos/{OWNER}/{REPO}/labels",
            tok,
            {"name": name, "color": color, "description": desc},
        )
        print("label", name)


def create_issue(tok: str, title: str, body: str, labels: list[str], have: set[str]) -> None:
    if title in have:
        print("skip", title)
        return
    req(
        "POST",
        f"{API}/repos/{OWNER}/{REPO}/issues",
        tok,
        {"title": title, "body": body, "labels": labels},
    )
    have.add(title)
    print("issue", title)


def week_body(week: int, subtitle: str) -> str:
    lines = [
        f"对照 https://github.com/{OWNER}/{REPO}/blob/main/docs/17-weekly-board.md 第 {week} 周。",
        "",
        f"**主题：{subtitle}**",
        "",
        "不要按人名建文件夹。代码按模块目录交，PR 进本仓库。",
        "",
    ]
    for owner in "ABCDEF":
        lines.append(f"- [ ] **{owner}**：{TASKS[week][owner]}")
    lines += [
        "",
        "字段以 `contracts/openapi.yaml` 为准。Agent 先读 `contracts/AGENT_CONTRACT.md`。",
        f"Label: `week-{week}`",
    ]
    return "\n".join(lines)


def try_project(tok: str) -> None:
    query = {
        "query": """
        query($login:String!) {
          user(login:$login) { id }
        }
        """,
        "variables": {"login": OWNER},
    }
    try:
        data = req("POST", GRAPHQL, tok, query)
    except RuntimeError as exc:
        print("project skip (graphql)", exc)
        return
    uid = (((data.get("data") or {}).get("user") or {}).get("id"))
    if not uid:
        print("project skip: no user id", data)
        return
    created = req(
        "POST",
        GRAPHQL,
        tok,
        {
            "query": """
            mutation($ownerId:ID!, $title:String!) {
              createProjectV2(input:{ownerId:$ownerId, title:$title}) {
                projectV2 { id url }
              }
            }
            """,
            "variables": {"ownerId": uid, "title": "CampusBuddy 十周任务板"},
        },
    )
    print("project", json.dumps(created, ensure_ascii=False)[:1000])


def main() -> None:
    tok = token()
    ensure_labels(tok)
    have = existing_issue_titles(tok)
    for title, label, paths in EPICS:
        create_issue(
            tok,
            title,
            f"模块史诗。路径：\n\n```\n{paths}\n```\n\n按周任务见 docs/17-weekly-board.md。",
            ["epic", label, "type-docs"],
            have,
        )
    for week, subtitle in WEEKS:
        labels = [f"week-{week}", "type-docs" if week <= 4 else "type-code"]
        for owner in "ABCDEF":
            labels.append(f"owner-{owner.lower()}")
        title = f"[W{week}] {subtitle}"
        create_issue(tok, title, week_body(week, subtitle), labels, have)
    try_project(tok)
    print("done")


if __name__ == "__main__":
    # Allow running from repo root without installing extra deps.
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    main()
