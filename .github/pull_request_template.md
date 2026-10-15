## 范围

- Owner：A / B / C / D / E / F
- 只改这些路径：
- 关联 Issue：

## 契约

- [ ] 未改字段名，或已同时改 `contracts/openapi.yaml` + `docs/04-api-contract.md` + examples
- [ ] 请求/响应仍是 `{code,message,data,requestId}`
- [ ] 写接口带 `Idempotency-Key`（若适用）

## 校验

- [ ] `python scripts/check_contracts.py`（改了 contracts 时）
- [ ] 本模块测试或手工步骤：
- [ ] 未提交 `.env`、密钥、dump、真实学号

## Agent / 人工

不要添加 Cursor 或其他工具的 `Co-authored-by`。
