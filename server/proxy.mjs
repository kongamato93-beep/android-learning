/**
 * AI 中转服务 —— 让密钥永远待在服务器这边。
 *
 * 为什么要有这一层：
 *   之前 App 直连模型厂商，密钥要靠 buildConfigField 编进 APK，
 *   结果 dex 里能直接搜到明文 key（等于把钥匙跟安装包一起发出去）。
 *   现在改成 App → 这台服务 → 模型厂商：
 *     - App 侧只需要一个服务器地址 + 一个自定义令牌，不含任何厂商密钥
 *     - 密钥只在服务器进程的内存/环境变量里，从不出现在网络另一端的包里
 *     - 换成别家模型时，只改这里的上游地址，App 一行都不用动
 *
 * 刻意做成【零第三方依赖】：只用 Node 内置模块，拷到任何有 Node 18+ 的机器上直接跑。
 *
 * 接口契约（App 端 ProxyProvider 按这个解析）：
 *   GET  /healthz   → { ok, keyConfigured, tokenRequired }
 *   POST /v1/chat   → { reply, model, usage: { total_tokens } }
 *   出错统一         → { error: { code, message } }
 */

import http from 'node:http';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { timingSafeEqual } from 'node:crypto';

// ─────────────────────────────────────────────────────────────
// 1. 路径与配置
// ─────────────────────────────────────────────────────────────

const HERE = dirname(fileURLToPath(import.meta.url));       // server/
const ROOT = join(HERE, '..');                               // 项目根

/**
 * 解析 key=value 形式的文本文件。
 * 兼容三件事：注释行、空行、值被引号包裹（手抄配置时很容易写成 KEY="abc"）。
 */
function parseKeyValueFile(file) {
    if (!existsSync(file)) return {};
    const out = {};
    for (const rawLine of readFileSync(file, 'utf8').split(/\r?\n/)) {
        const line = rawLine.trim();
        if (!line || line.startsWith('#')) continue;
        const eq = line.indexOf('=');
        if (eq <= 0) continue;
        const key = line.slice(0, eq).trim();
        let val = line.slice(eq + 1).trim();
        if ((val.startsWith('"') && val.endsWith('"'))
            || (val.startsWith("'") && val.endsWith("'"))) {
            val = val.slice(1, -1).trim();
        }
        out[key] = val;
    }
    return out;
}

// 配置来源优先级：进程环境变量 > server/.env > 项目根 local.properties
// 第三条是本机开发的便利兜底：local.properties 里已经有 ZHIPU_API_KEY，
// 本地调试就不必再抄一份到 .env。部署到服务器上时不要依赖它。
const FILE_CONF = {
    ...parseKeyValueFile(join(ROOT, 'local.properties')),
    ...parseKeyValueFile(join(HERE, '.env')),
};

/** 取一个配置项，空串视为“没配”，逐级往下降 */
function conf(name, fallback = '') {
    const fromEnv = process.env[name];
    if (fromEnv !== undefined && String(fromEnv).trim() !== '') return String(fromEnv).trim();
    const fromFile = FILE_CONF[name];
    if (fromFile !== undefined && String(fromFile).trim() !== '') return String(fromFile).trim();
    return fallback;
}

const HOST = conf('HOST', '127.0.0.1');        // 想让手机也连得上就改成 0.0.0.0
const PORT = Number(conf('PORT', '8787'));
const APP_TOKEN = conf('APP_TOKEN');           // App 来访时带的令牌
const ZHIPU_API_KEY = conf('ZHIPU_API_KEY');   // 真正的模型密钥，绝不外传

const BASE = conf('ZHIPU_BASE_URL', 'https://open.bigmodel.cn/api/paas/v4/');
const UPSTREAM_URL = (BASE.endsWith('/') ? BASE : BASE + '/') + 'chat/completions';
const UPSTREAM_TIMEOUT_MS = Number(conf('UPSTREAM_TIMEOUT_MS', '60000'));
const DEFAULT_MODEL = conf('DEFAULT_MODEL', 'glm-4-flash');

/** 请求体上限 1MB。大于这个量级的对话本来也不该一口气发过来。 */
const MAX_BODY_BYTES = 1024 * 1024;

// ─────────────────────────────────────────────────────────────
// 2. 工具函数
// ─────────────────────────────────────────────────────────────

/** 日志里绝不出现令牌和密钥本体 */
function log(level, msg, extra = '') {
    const now = new Date().toISOString().slice(11, 23);   // HH:mm:ss.mmm
    console.log(`[${now}] ${level.padEnd(5)} ${msg}${extra ? '  ' + extra : ''}`);
}

function sendJson(res, status, payload) {
    const body = JSON.stringify(payload);
    res.writeHead(status, {
        'Content-Type': 'application/json; charset=utf-8',
        'Content-Length': Buffer.byteLength(body),
        // 开发期方便用浏览器/curl 直接调试；上了公网建议把 * 换成 App 域名
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'Content-Type, X-App-Token',
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    });
    res.end(body);
}

/** 用恒定时间比较，避免“逐字节比对、第一个字节不同就早退”被拿来试探令牌 */
function tokenMatches(provided) {
    if (!APP_TOKEN) return false;
    if (typeof provided !== 'string' || provided.length === 0) return false;
    const a = Buffer.from(provided, 'utf8');
    const b = Buffer.from(APP_TOKEN, 'utf8');
    if (a.length !== b.length) return false;
    return timingSafeEqual(a, b);
}

/** 把请求体整块读进来，顺手做体积限制 */
function readBody(req) {
    return new Promise((resolve, reject) => {
        let size = 0;
        const chunks = [];
        req.on('data', (chunk) => {
            size += chunk.length;
            if (size > MAX_BODY_BYTES) {
                reject(new Error('payload_too_large'));
                req.destroy();
                return;
            }
            chunks.push(chunk);
        });
        req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
        req.on('error', reject);
    });
}

// ─────────────────────────────────────────────────────────────
// 3. 两个接口的实现
// ─────────────────────────────────────────────────────────────

/** 健康检查：给 curl 和 App 的“服务没起来”提示用，不泄露任何密钥内容 */
function handleHealth(res) {
    sendJson(res, 200, {
        ok: true,
        // 只回“配了/没配”，不回值，防止误把密钥打到日志或被别人看到
        keyConfigured: ZHIPU_API_KEY.length > 0,
        tokenRequired: APP_TOKEN.length > 0,
        model: DEFAULT_MODEL,
    });
}

/**
 * 转发一次 Chat Completions 请求。
 *
 * 这里刻意做了一件事：**只把 {reply, model, usage} 三个字段回给 App**。
 * 上游的完整响应里可能带指纹、敏感 policy 信息等，全量透传没必要，
 * 也让 App 端拿到的形状稳定 —— 将来换服务商，客户端解析代码不用改。
 */
async function handleChat(req, res) {
    const startedAt = Date.now();

    // 3.1 鉴权：令牌没配对就挡回去，绝不让它碰到上游
    if (!tokenMatches(req.headers['x-app-token'])) {
        if (!APP_TOKEN) {
            log('ERROR', 'APP_TOKEN 未配置，拒绝所有 /v1/chat 请求');
            sendJson(res, 401, {
                error: { code: 'server_not_configured', message: '服务端未配置 APP_TOKEN' }
            });
        } else {
            log('WARN', 'token mismatch');
            sendJson(res, 401, { error: { code: 'unauthorized', message: '令牌无效' } });
        }
        return;
    }

    if (!ZHIPU_API_KEY) {
        sendJson(res, 503, {
            error: { code: 'no_upstream_key', message: '服务端未配置 ZHIPU_API_KEY' }
        });
        return;
    }

    // 3.2 解析客户端请求体
    let parsed;
    try {
        parsed = JSON.parse(await readBody(req));
    } catch (e) {
        sendJson(res, 400, { error: { code: 'bad_json', message: '请求体不是合法 JSON' } });
        return;
    }

    const messages = Array.isArray(parsed.messages) ? parsed.messages : null;
    if (!messages || messages.length === 0) {
        sendJson(res, 400, { error: { code: 'bad_request', message: 'messages 不能为空' } });
        return;
    }

    // 逐条检查结构。把脏数据在服务端挡掉，模型那边就不会返回看不懂的报错。
    for (const m of messages) {
        if (!m || typeof m.role !== 'string' || typeof m.content !== 'string') {
            sendJson(res, 400, {
                error: { code: 'bad_request', message: '每条消息都需要 role 和 content 字段' }
            });
            return;
        }
    }

    // 3.3 组装上游请求
    // model 由服务端兜底决定，不让客户端随便指定 —— 否则别人可以拿你的 key 去烧贵模型
    const upstreamBody = JSON.stringify({
        model: DEFAULT_MODEL,
        messages,
        stream: false,
    });

    // 3.4 调上游
    let upstreamRes;
    try {
        upstreamRes = await fetch(UPSTREAM_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + ZHIPU_API_KEY,
            },
            body: upstreamBody,
            signal: AbortSignal.timeout(UPSTREAM_TIMEOUT_MS),
        });
    } catch (e) {
        const timedOut = e && (e.name === 'TimeoutError' || e.name === 'AbortError');
        log('ERROR', timedOut ? 'upstream timeout' : 'upstream unreachable',
            e && e.message ? e.message : '');
        sendJson(res, timedOut ? 504 : 502, {
            error: {
                code: timedOut ? 'upstream_timeout' : 'upstream_unreachable',
                message: timedOut ? '模型服务响应超时' : '连不上模型服务',
            }
        });
        return;
    }

    const raw = await upstreamRes.text();
    const cost = Date.now() - startedAt;

    if (!upstreamRes.ok) {
        // 错误体可能不是 JSON，取不到就用状态码兜底
        let msg = `上游返回 HTTP ${upstreamRes.status}`;
        try {
            const j = JSON.parse(raw);
            msg = (j.error && j.error.message) || j.message || msg;
        } catch { /* 保持默认文案 */ }
        log('WARN', `upstream ${upstreamRes.status}`, `cost=${cost}ms msg=${String(msg).slice(0, 120)}`);
        sendJson(res, upstreamRes.status === 401 ? 401 : 502, {
            error: { code: 'upstream_error', message: String(msg).slice(0, 300) }
        });
        return;
    }

    // 3.5 拆模型回复
    let reply = '';
    let model = '';
    let totalTokens = -1;
    try {
        const j = JSON.parse(raw);
        const first = (j.choices && j.choices[0]) || {};
        reply = (first.message && typeof first.message.content === 'string')
            ? first.message.content.trim() : '';
        model = j.model || DEFAULT_MODEL;
        if (j.usage && typeof j.usage.total_tokens === 'number') {
            totalTokens = j.usage.total_tokens;
        }
    } catch (e) {
        log('ERROR', 'upstream json parse failed', String(raw).slice(0, 200));
        sendJson(res, 502, { error: { code: 'upstream_bad_json', message: '上游返回无法解析' } });
        return;
    }

    if (!reply) {
        log('WARN', 'upstream returned empty content', `cost=${cost}ms`);
        sendJson(res, 502, { error: { code: 'empty_reply', message: '模型没有返回内容' } });
        return;
    }

    log('INFO', `ok model=${model}`, `tokens=${totalTokens} cost=${cost}ms len=${reply.length}`);
    sendJson(res, 200, { reply, model, usage: { total_tokens: totalTokens } });
}

// ─────────────────────────────────────────────────────────────
// 4. 启动 HTTP 服务
// ─────────────────────────────────────────────────────────────

const server = http.createServer((req, res) => {
    const startedAt = Date.now();
    const url = (req.url || '/').split('?')[0];

    // 预检请求直接放行：开发期用浏览器测试时会用到
    if (req.method === 'OPTIONS') {
        sendJson(res, 204, {});
        return;
    }

    let promise;
    if (url === '/healthz' && req.method === 'GET') {
        promise = Promise.resolve(handleHealth(res));
    } else if (url === '/v1/chat' && req.method === 'POST') {
        promise = handleChat(req, res);
    } else {
        promise = Promise.resolve(sendJson(res, 404, {
            error: { code: 'not_found', message: '没有这个接口' }
        }));
    }

    promise.catch((e) => {
        log('ERROR', `handler crashed ${req.method} ${url}`, String(e && e.message || e));
        if (!res.headersSent) {
            sendJson(res, 500, { error: { code: 'internal', message: '服务端内部错误' } });
        }
    }).finally(() => {
        if (req.method !== 'OPTIONS' && url !== '/healthz') {
            log('DEBUG', `${req.method} ${url} ${res.statusCode}`, `cost=${Date.now() - startedAt}ms`);
        }
    });
});

server.listen(PORT, HOST, () => {
    log('INFO', `AI 中转服务已启动  http://${HOST}:${PORT}`);
    log('INFO', `上游地址  ${UPSTREAM_URL}`);
    log('INFO', `默认模型  ${DEFAULT_MODEL}`);
    log('INFO', `模型密钥  ${ZHIPU_API_KEY ? '已配置' : '【未配置】'}`);
    log('INFO', `访问令牌  ${APP_TOKEN ? '已配置' : '【未配置，/v1/chat 会全部拒绝】'}`);
    if (!ZHIPU_API_KEY) {
        log('WARN', '没有密钥：在 server/.env 或项目根 local.properties 里写 ZHIPU_API_KEY');
    }
    if (!APP_TOKEN) {
        log('WARN', '没有令牌：在 server/.env 或项目根 local.properties 里写 APP_TOKEN，'
            + '并和 App 的 AI_PROXY_TOKEN 保持一致');
    }
});

// Windows 上 Ctrl+C 也能干净退出
process.on('SIGINT', () => {
    log('INFO', '收到退出信号，正在关闭服务');
    server.close(() => process.exit(0));
});
