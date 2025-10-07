# 雪球超级爬虫策略现状评估

## 现有实现概览
- 框架通过 `HttpRequestHelper` 统一封装请求，仅向雪球服务端发送 `Accept-Encoding`、`Referer`、`Cookie` 与 `Host` 头信息，且默认使用 `HttpURLConnection` 的 Java UA。【F:src/main/java/org/decaywood/utils/HttpRequestHelper.java†L24-L83】
- Cookie 在 `config.sys` 中手动配置，运行时只要 JVM 属性 `cookies` 存在就直接使用，不再尝试自动登录或刷新。【F:src/main/resources/config.sys†L1-L10】【F:src/main/java/org/decaywood/utils/FileLoader.java†L21-L35】
- 所有数据接口都写死在 `URLMapper` 枚举中，多数仍然指向 2015~2016 年的旧版 `xueqiu.com` v4/v1 路由，例如 `stock/forchartk/stocklist.json`、`v4/stock/quote.json` 等。【F:src/main/java/org/decaywood/utils/URLMapper.java†L9-L33】
- `CookieProcessor` 中残留了基于明文 HTTP GET 的旧登录逻辑，依赖早已失效的 `/user/login` 接口，且未携带 CSRF 令牌或验证码处理流程。【F:src/main/java/org/decaywood/CookieProcessor.java†L26-L65】
- URL 参数拼接完全未做编码处理，直接拼接键值对，遇到包含特殊字符的参数（例如股票筛选条件中的空格、中文或逻辑表达式）会生成无效 URL。【F:src/main/java/org/decaywood/utils/RequestParaBuilder.java†L11-L38】

## 有效性分析
1. **接口大面积迁移至 v5 + 新域名**：雪球在 2019 年后陆续把行情、筛选等接口迁移到 `https://stock.xueqiu.com/v5/...`，旧路径大多返回 302/404 或要求额外签名。现有硬编码 URL 与最新线上路径不符，直接请求会失败或拿到跳转 HTML。【F:src/main/java/org/decaywood/utils/URLMapper.java†L9-L33】
2. **鉴权策略升级**：当前实现仅依赖手工填入的 `Cookie`，而新版接口除了 `xq_a_token`/`xq_r_token` 外，还会校验 `u`、`device_id` 等多枚 Cookie 以及 `X-Requested-With`、`User-Agent`、`Accept` 等头部，缺失时常返回 401/403。`HttpRequestHelper` 不设置这些关键头部，极易被风控识别为机器人。【F:src/main/java/org/decaywood/utils/HttpRequestHelper.java†L24-L83】
3. **登录与 Cookie 刷新失效**：`CookieProcessor#login` 仍试图通过 HTTP GET 方式调用 `/user/login`，不仅未使用 HTTPS，也没有发送验证码、csrf、RSA 加密密码等当前登录流程必需的数据，因此无法再自动获取有效 Cookie。【F:src/main/java/org/decaywood/CookieProcessor.java†L26-L65】
4. **参数编码缺失导致请求异常**：筛选器、搜索等 Collector 需要传递包含比较符、中文行业名称或空格的参数，直接拼接会在新版服务的严格校验下被判定为非法请求，进一步降低成功率。【F:src/main/java/org/decaywood/utils/RequestParaBuilder.java†L11-L38】

综合来看，如果不手动在浏览器抓取全部最新 Cookie，并补齐额外请求头，大部分 Collector/Mapper 已无法拿到完整数据，策略“开箱即用”地运行基本失效。

## 修改与升级建议
1. **更新接口映射**：
   - 针对行情、K 线、F10、组合等功能，改为使用雪球 v5 接口（如 `https://stock.xueqiu.com/v5/stock/chart/kline.json`、`https://stock.xueqiu.com/v5/stock/f10/` 系列）。
   - 将 `URLMapper` 拆分为可配置的映射或外部 YAML，使接口变动时无需重新编译。【F:src/main/java/org/decaywood/utils/URLMapper.java†L9-L33】

2. **补齐请求头与鉴权信息**：
   - 在 `HttpRequestHelper` 中新增 `User-Agent`、`Accept`, `Accept-Language`, `Connection`, `X-Requested-With`, `Origin` 等字段，并允许通过配置覆盖，以模拟真实浏览器流量。【F:src/main/java/org/decaywood/utils/HttpRequestHelper.java†L24-L83】
   - 扩展 `FileLoader`/配置文件，分别维护 `xq_a_token`、`xq_r_token`, `u`, `device_id` 等关键 Cookie，而不是拼成一整串字符串，便于自动刷新。【F:src/main/java/org/decaywood/utils/FileLoader.java†L21-L44】

3. **重写 Cookie 刷新流程**：
   - 放弃旧的 `/user/login` 伪登录逻辑，改为：
     1. 在浏览器登录后复制整份 Cookie；
     2. 通过 headless 浏览器或短信登录 API 自动化获取；
     3. 或者在本地搭建 Selenium 脚本定期同步。
   - 接入验证码/滑块识别前，先增加验证码触发时的异常兜底，提示人工介入。【F:src/main/java/org/decaywood/CookieProcessor.java†L26-L65】

4. **引入 URL 编码与签名逻辑**：
   - `RequestParaBuilder` 应改用 `URLEncoder`，同时对新版接口要求的 `_t`、`_` 时间戳或签名字段提供 Hook，确保参数合法。【F:src/main/java/org/decaywood/utils/RequestParaBuilder.java†L11-L38】

5. **节流与指纹模拟**：结合新版反爬机制，增加随机等待、IP 轮换、TLS 指纹模拟等策略，避免高频访问被封锁。框架已有 `TimeWaitingStrategy`，可以引入指数回退、抖动等改进以配合新的风控节奏。

通过以上改造，仍可在保证账号安全的前提下恢复大部分数据抓取能力，但需要持续跟踪雪球接口与风控策略的更新。

## 改造落实情况
- `HttpRequestHelper` 增强默认请求头并支持从 `xueqiu.header.*` 配置覆盖，同时自动拼装配置化 Cookie，确保模拟真实浏览器流量。【F:src/main/java/org/decaywood/utils/HttpRequestHelper.java†L12-L117】
- `FileLoader`/`CookieProcessor` 支持拆分式 `xueqiu.cookie.*` 配置，自动拼接并校验 Cookie 值，去掉失效的旧登录流程。【F:src/main/java/org/decaywood/utils/FileLoader.java†L1-L125】【F:src/main/java/org/decaywood/CookieProcessor.java†L1-L37】
- `URLMapper` 改为读取 `url-mapper.properties`，并内置雪球 v5 接口作为默认值，接口升级时无需重新编译即可调整。【F:src/main/java/org/decaywood/utils/URLMapper.java†L1-L53】【F:src/main/resources/url-mapper.properties†L1-L21】
- `RequestParaBuilder` 引入 UTF-8 编码与动态参数 Hook，自动添加时间戳等防重参数以满足新接口校验。【F:src/main/java/org/decaywood/utils/RequestParaBuilder.java†L1-L64】【F:src/main/java/org/decaywood/mapper/stockFirst/StockToStockWithStockTrendMapper.java†L64-L75】
- 默认 `TimeWaitingStrategy` 加入指数退避 + 抖动，避免高频请求触发风控。【F:src/main/java/org/decaywood/timeWaitingStrategy/DefaultTimeWaitingStrategy.java†L1-L62】
