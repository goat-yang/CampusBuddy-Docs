# web（Owner F）

Vue 3 + Vite。两个入口，同一套 API 客户端：

- `apps/portal` 师生 Web，默认 5173
- `apps/admin` 运营后台，默认 5174，只认 JWT `role=ADMIN`

当前是占位页。请求必须带 `Authorization: Bearer`（登录后）、写操作带 `Idempotency-Key`（UUID）。解析规则与小程序相同：只认封套四字段。

```bash
npm install
npm run dev:portal
npm run dev:admin
```
