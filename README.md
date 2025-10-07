# 雪球超级爬虫
<a href="http://junit.org/"><img src="https://img.shields.io/badge/Test-JUnit-orange.svg?style=flat"></a>
<a href="http://jsoup.org/"><img src="https://img.shields.io/badge/Dependency-Jsoup-yellow.svg?style=flat"></a>
<a href="http://jackson-users.ning.com/"><img src="https://img.shields.io/badge/Dependency-Jackson-blue.svg?style=flat"></a>
<a href="http://dev.mysql.com/"><img src="https://img.shields.io/badge/Database-MySQL-red.svg?style=flat"></a>

> 一个围绕 Collector → Mapper → Consumer 三段流水线构建的雪球数据采集框架，支持按需组装任意指标并扩展新的抓取策略。

## 快速上手

### 1. 准备运行环境
- **JDK 8**（项目大量使用 Java 8 函数式特性）
- **Maven 3.6+**（用于依赖管理与示例代码编译）
- 可以访问 https://xueqiu.com 的网络环境，用于人工登陆并复制 Cookie

### 2. 克隆并编译
```bash
git clone https://github.com/decaywood/XueQiuSuperSpider.git
cd XueQiuSuperSpider
mvn -DskipTests compile
```
> 在受限网络环境下构建可能会受到 Maven 仓库 403/超时影响，可提前配置企业或本地镜像。

### 3. 配置雪球鉴权信息
1. 浏览器登陆雪球网页版，打开开发者工具复制最新的 Cookie。
2. 编辑 `src/main/resources/config.sys`，为以下键填写真实取值：
   ```properties
   xueqiu.cookie.xq_a_token = <浏览器 Cookie 中的 xq_a_token>
   xueqiu.cookie.xq_r_token = <浏览器 Cookie 中的 xq_r_token>
   xueqiu.cookie.u = <浏览器 Cookie 中的 u>
   xueqiu.cookie.device_id = <浏览器 Cookie 中的 device_id>
   xueqiu.cookie.xq_is_login = 1
   ```
3. 如果更习惯粘贴完整的 Cookie 字符串，也可以直接填充 `cookies =` 这一行，框架会在 `CookieProcessor` 中优先解析分拆后的键值。
4. 可选：在同一配置文件中覆盖默认请求头（`xueqiu.header.*`），或者通过 `java.util.concurrent.ForkJoinPool.common.parallelism` 调整并发度。

### 4. 运行示例抓取
框架没有提供单独的 `main` 启动类，推荐通过 JUnit 或独立的 Runner 运行示例逻辑。以下代码片段展示了如何抓取 24 小时内的美股热榜并附带走势数据：

```java
StockScopeHotRankCollector collector =
        new StockScopeHotRankCollector(StockScopeHotRankCollector.Scope.US_WITHIN_24_HOUR);
StockToStockWithAttributeMapper attributeMapper = new StockToStockWithAttributeMapper();
StockToStockWithStockTrendMapper trendMapper =
        new StockToStockWithStockTrendMapper(StockTrend.Period.ONE_DAY);

List<Stock> stocks = collector.get()
        .parallelStream()
        .map(attributeMapper.andThen(trendMapper))
        .collect(Collectors.toList());
```

将上述逻辑放入任意测试类后，可执行：
```bash
mvn -Dtest=MyHotRankExampleTest test
```
成功后即可在日志中看到走势历史数据条数及基础行情指标。

## 进阶配置

### 动态调整接口与参数
- 所有 REST Endpoint 均映射在 `src/main/resources/url-mapper.properties`，无需改动源码即可切换到最新的雪球 v5 接口。
- `RequestParaBuilder` 新增 UTF-8 编码和时间戳 Hook，可在编写自定义 Mapper/Collector 时直接复用。

### 自动退避与重试
默认的 `DefaultTimeWaitingStrategy` 提供指数退避+抖动策略；必要时可以通过构造函数覆盖等待区间，或实现自定义策略后在各 Collector/Mapper 中注入。

### Cookie 管理
`CookieProcessor` 会首先尝试读取拆分后的 cookie 配置，缺失时回退到 `cookies` 字符串。也可以将该处理器注入到自定义的 `HttpRequestHelper` 中以满足多账号轮换的需求。

## 项目结构速览
```
info/                 项目文档与策略记录
src/main/java/        采集框架主体代码
├── collector/        针对不同入口的采集实现
├── mapper/           组装行业、股价、股东等衍生信息
├── consumer/         自定义落地处理（可扩展）
├── timeWaitingStrategy/ 等待与退避策略
└── utils/            配置加载、HTTP 请求等工具
src/main/resources/   `config.sys` 与接口映射配置
src/test/java/        用例与示例
```

## 常见问题
- **请求被 401/403 拦截**：确认 Cookie 未过期、账号已登陆，必要时减少并发或拉长等待策略。
- **接口结构变化**：优先在 `url-mapper.properties` 中调整到新的接口路径，再视情况补充参数构造逻辑。
- **长时间抓取被限流**：调低 `ForkJoinPool` 并行度，或扩展轮询账号并在 `CookieProcessor` 中实现切换。

## 更新日志

2016.4.12 -- 更新 url: http -> https

2016.4.10 -- 增加自动登录接口,用户在 config.sys 下配置用户名以及密码等信息可自动登录并提高爬虫权限。

2016.2.20 -- 修复雪球后台 cookie 设置变更所导致的程序更新 cookie 失效问题 Issues #2

2015.12.4 -- 增加根据股票获取公司信息功能（公司名称、组织形式、成立日期、经营范围、主营业务、地区代码、所属板块）

2015.12.7 -- 游资追踪增加模糊匹配特性

2015.12.10 -- 添加了必要的代码注释，添加了统计一阳穿三线个股的 example

2015.12.14 -- 更新高级特性，增加 RMI 分布式数据抓取特性，可将需要访问网络的组件部署至 Slave 集群，分散流量，防止被反爬虫机制锁定。 [RMI 高级特性](https://github.com/decaywood/XueQiuSuperSpider/blob/master/info/RMI.md)

2020.10.07 -- 移除了 RMI 特性，精简框架内容。增加股票讨论区爬取、评论爬取以及相关户信息爬取

2020.10.08 -- 移除了自动获取 cookie 功能，需要从浏览器复制 cookie 并写入 config.sys 文件

## 贡献规范
- 参数对象请实现 `DeepCopy` 接口，模块间传递时保持只读
- 域变量尽量声明为 `final`
- 可变对象请提供 `EmptyObject` 默认值以规避空指针
- Mapper 与 Mapper 输入输出类型一致时需满足交换律，保证组合顺序安全

## License
[MIT](https://github.com/decaywood/XueQiuSuperSpider/blob/master/LICENSE)
