# 接口真源（给人和 Agent）

**字段、类型、枚举、HTTP 与 `code` 以本目录为准。** Markdown [docs/04-api-contract.md](../docs/04-api-contract.md) 讲行为与所有权，不得另造同名 DTO。改接口必须同时改：

1. `openapi.yaml`
2. `examples/` 正反例
3. `docs/04-api-contract.md` 与受影响子 TRD
4. 提供方与调用方代码（有代码之后）

未知属性默认拒绝。客户端不得传 `userId`、`role`、`creditScore` 来提权。服务尚未实现；样例是契约，不是线上流量。

```bash
python -m pip install -r requirements-dev.txt
python scripts/check_contracts.py
```
