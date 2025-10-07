package org.decaywood;

import org.decaywood.utils.FileLoader;
import org.decaywood.utils.StringUtils;

import java.util.Map;

/**
 * @author: decaywood
 * @date: 2016/04/10 20:01
 */
public interface CookieProcessor {


    default void updateCookie(String website) {

        GlobalSystemConfigLoader.loadConfig();

        String cookies = System.getProperty("cookies");
        if (StringUtils.isNotNull(cookies) && cookies.trim().length() > 0) {
            FileLoader.updateCookie(cookies.trim(), website);
            return;
        }

        Map<String, String> cookieValues = FileLoader.loadCookieValues();
        if (cookieValues.isEmpty()) {
            throw new IllegalStateException("未检测到有效的雪球 Cookie，请在 config.sys 中配置 xueqiu.cookie.* 属性或手动注入 cookies 参数。");
        }
        StringBuilder builder = new StringBuilder();
        cookieValues.forEach((key, value) -> {
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(key).append("=").append(value);
        });
        FileLoader.updateCookie(builder.toString(), website);
    }

    @Deprecated
    default void login(String areacode,
                       String userID,
                       String passwd,
                       boolean rememberMe) {
        throw new UnsupportedOperationException("雪球登录已改版，请在浏览器登录后同步 Cookie 或接入专用自动化脚本。");
    }

}
