# AI 中转服务

一个极窄的中转层，用途只有一个：**让模型密钥不要被打进 APK**。

```
之前：  App ────Bearer ZHI PU_KEY────→ 智谱 API        （key 在 APK 的 dex 里，反编译可见）
现在：  App ──X-App-Token──→ 本服务 ──Bearer KEY──→ 智谱 API
                                        ↑
                                  key 只在这台机器上
```

## 为什么值得多这一层

`buildConfigField` 注入的密钥是**编译期字符串常量**，一定会出现在 `classes*.dex` 里。
实测当前 `app-debug.apk` 的 `classes7.dex` 能直接搜到 key 前缀 —— 也就是说，
只要 APK 发出去，密钥就等于公开了。

改成中转后，App 里只有「服务器地址 + 自定义令牌」两样东西。
令牌泄露的最坏结果是你自己吊销它换一个，厂商密钥不会跟着丢。

## 零依赖

只用 Node 内置模块（`http` / `fs` / `crypto` / `fetch`），需要 **Node 18+**。
不用 `npm install`，拷到任何机器上都能跑。

## 本机启动

```powershell
# 用隔离环境里的 node（推荐）
& "C:\Users\ASUS\.workbuddy\binaries\node\versions\22.22.2-3\node.exe" .\proxy.mjs

# 或者系统 node
node .\proxy.mjs
```

看到这几行就起来了：

```
[12:00:00.123] INFO  AI 中转服务已启动  http://127.0.0.1:8787
[12:00:00.123] INFO  模型密钥  已配置
[12:00:00.124] INFO  访问令牌  已配置
```

**本机不需要配任何东西** —— 服务会自动去项目根 `local.properties` 里读 `ZHIPU_API_KEY`。
只有 `APP_TOKEN` 需要你自己定一个（见下面「两个令牌要对齐」）。

## 验证服务本身（不启 App 也能测）

```powershell
# 1. 健康检查 —— 不用令牌
curl http://127.0.0.1:8787/healthz
# => {"ok":true,"keyConfigured":true,"tokenRequired":true,"model":"glm-4-flash"}

# 2. 令牌不对 —— 应该被挡回来
curl -X POST http://127.0.0.1:8787/v1/chat ^
     -H "Content-Type: application/json" -H "X-App-Token: wrong" ^
     -d "{\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}"
# => {"error":{"code":"unauthorized","message":"令牌无效"}}

# 3. 令牌正确 —— 应该拿到真实回复
curl -X POST http://127.0.0.1:8787/v1/chat ^
     -H "Content-Type: application/json" -H "X-App-Token: 你的令牌" ^
     -d "{\"messages\":[{\"role\":\"user\",\"content\":\"用一句话介绍你自己\"}]}"
# => {"reply":"...","model":"glm-4-flash","usage":{"total_tokens":42}}
```

第 2 步是**必须验证**的一步：它证明没有令牌的人没法拿你的 key 烧 token。

## 两个令牌要对齐

| 位置 | 配置项 | 作用 |
|---|---|---|
| 服务端 `server/.env` 或根 `local.properties` | `APP_TOKEN` | 服务认这个值 |
| App 端 `local.properties` | `AI_PROXY_TOKEN` | App 请求时带上这个值 |

**两边必须写成一样的字符串**，否则 App 会一直收到「令牌无效」。

**服务端没配 `APP_TOKEN` 时，`/v1/chat` 会全部返回 401**，不会退化成一个裸奔的开放代理 ——
这是刻意的安全兜底，避免"先跑起来再说"然后忘掉鉴权。

## App 侧对应的配置项目根 local.properties 里多加两行：

```properties
# 模拟器访问宿主机用 10.0.2.2；真机改成电脑的局域网 IP
AI_PROXY_URL=http://10.0.2.2:8787/
# 必须和服务端 APP_TOKEN 一致
AI_PROXY_TOKEN=你定的那个令牌
```

改完重新编译 APK（`BuildConfig` 是在编译期注入的，改了必须重编）。

## 想让真机也能连

三步：

1. 服务端 `HOST=0.0.0.0`（`.env` 里改），重启服务
2. Windows 防火墙放行 8787 端口
3. `AI_PROXY_URL` 改成 `http://<电脑的局域网IP>:8787/`

注意这时候是**明文 HTTP**，只在自己家的可信网络里这么用；上公网得上 HTTPS。

## 部署到服务器

配置全部走进程环境变量，符合 Twelve-Factor：

```bash
PORT=8787 \
HOST=0.0.0.0 \
APP_TOKEN=你的令牌 \
ZHIPU_API_KEY=厂商密钥 \
node proxy.mjs
```

前面挂一层 Nginx 做 HTTPS 就齐了。App 侧把 `AI_PROXY_URL` 换成 `https://你的域名/` 即可，
Java 代码不用改。

## 接口契约

| 方法 | 路径 | 请求体 | 成功返回 |
|---|---|---|---|
| GET | `/healthz` | 无 | `{ok, keyConfigured, tokenRequired, model}` |
| POST | `/v1/chat` | `{model?, messages:[{role, content}]}` | `{reply, model, usage:{total_tokens}}` |

失败统一返回 `{error:{code, message}}`，HTTP 状态码贴合语义：

| 状态码 | code | 含义 |
|---|---|---|
| 400 | `bad_json` / `bad_request` | 请求体有问题 |
| 401 | `unauthorized` | 令牌不对 |
| 401 | `server_not_configured` | 服务端没配 APP_TOKEN |
| 502 | `upstream_error` / `upstream_unreachable` / `upstream_bad_json` / `empty_reply` | 上游出问题了 |
| 503 | `no_upstream_key` | 服务端没配 ZHIPU_API_KEY |
| 504 | `upstream_timeout` | 上游响应超时 |

## 这一层不做什么

刻意保持窄，这些都没做，将来需要再加：

- **不限流**。单机自用够，公网暴露必须加。
- **不做多租户**。没有用户体系，一个令牌走天下。
- **不支持流式**。一次拿完整回复（`stream:false`）。
- **不缓存**。同样的提问会原样再烧一次 token。
- **model 不让客户端指定**，写死在服务端 `DEFAULT_MODEL`。防止别人拿你的 key 去调贵模型。
