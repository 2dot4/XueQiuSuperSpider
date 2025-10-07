package org.decaywood.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author: decaywood
 * @date: 2015/11/24 18:54
 */
public enum URLMapper {

    /*--------------------------------  Xue Qiu     --------------------------------------*/
    MAIN_PAGE("main_page", "https://xueqiu.com"),
    STOCK_MAIN_PAGE("stock_main_page", "https://stock.xueqiu.com/v5/stock/screener/quote/list.json"),
    USER_INFO_JSON("user_info_json", "https://xueqiu.com/query/v1/user/show.json"),
    USER_COMMENT_JSON("user_comment_json", "https://xueqiu.com/v4/statuses/user_timeline.json"),
    COMMENTS_INFO_JSON("comments_info_json", "https://xueqiu.com/statuses/comments.json"),
    COMPREHENSIVE_PAGE("comprehensive_page", "https://xueqiu.com/hq"),
    HU_SHEN_NEWS_REF_JSON("hu_shen_news_ref_json", "https://stock.xueqiu.com/v5/news/query/list.json"),
    STOCK_SHAREHOLDERS_JSON("stock_shareholders_json", "https://stock.xueqiu.com/v5/stock/f10/cn/shareholder.json"),
    STOCK_SELECTOR_JSON("stock_selector_json", "https://stock.xueqiu.com/v5/stock/screener/screen.json"),
    LONGHUBANG_JSON("longhubang_json", "https://stock.xueqiu.com/v5/stock/f10/cn/lhb.json"),
    STOCK_INDUSTRY_JSON("stock_industry_json", "https://stock.xueqiu.com/v5/stock/f10/cn/company.json"),
    CUBE_REBALANCING_JSON("cube_rebalancing_json", "https://xueqiu.com/cubes/rebalancing/history.json"),
    CUBE_TREND_JSON("cube_trend_json", "https://xueqiu.com/cubes/nav_daily/all.json"),
    CUBES_RANK_JSON("cubes_rank_json", "https://xueqiu.com/cubes/discover/rank/cube/list.json"),
    MARKET_QUOTATIONS_RANK_JSON("market_quotations_rank_json", "https://stock.xueqiu.com/v5/stock/hot_rank/list.json"),
    SCOPE_STOCK_RANK_JSON("scope_stock_rank_json", "https://stock.xueqiu.com/v5/stock/hot_rank/stock/list.json"),
    STOCK_TREND_JSON("stock_trend_json", "https://stock.xueqiu.com/v5/stock/chart/kline.json"),
    STOCK_JSON("stock_json", "https://stock.xueqiu.com/v5/stock/quote.json"),
    INDUSTRY_JSON("industry_json", "https://stock.xueqiu.com/v5/stock/screener/industry/list.json"),

    /*--------------------------------  NetEase     --------------------------------------*/

    NETEASE_MAIN_PAGE("netease_main_page", "https://quotes.money.163.com/stock"),
    STOCK_CAPITAL_FLOW("stock_capital_flow", "https://quotes.money.163.com/service/zjlx_chart.html");


    URLMapper(String configKey, String defaultValue) {
        this.configKey = configKey;
        this.defaultValue = defaultValue;
    }

    private final String configKey;
    private final String defaultValue;

    private static final Properties URL_CONFIG = loadProperties();

    @Override
    public String toString() {
        String systemOverride = System.getProperty("xueqiu.url." + configKey);
        if (systemOverride != null && systemOverride.trim().length() > 0) {
            return systemOverride.trim();
        }
        return URL_CONFIG.getProperty(configKey, defaultValue);
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = URLMapper.class.getClassLoader().getResourceAsStream("url-mapper.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            System.out.println("URLMapper -> 配置文件 url-mapper.properties 读取失败，使用内置默认值");
        }
        return properties;
    }
}
