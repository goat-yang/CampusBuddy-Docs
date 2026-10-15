# miniprogram（Owner E）

微信小程序客户端。只调 `../contracts/openapi.yaml` 里的路径，响应只读 `code`/`message`/`data`/`requestId`。不要在前端发明第二套字段名。

当前是空壳，W5 才接登录与列表。本地：

1. 微信开发者工具导入本目录
2. `project.config.json` 的 `appid` 换成课程号；未批下来用测试号
3. `config.js` 的 `BASE_URL` 指向本机可被真机访问的 `/api/v1`（不是小程序里的 localhost）

禁止提交 `project.private.config.json` 和密钥。
