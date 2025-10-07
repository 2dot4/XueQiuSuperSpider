package org.decaywood.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author: decaywood
 * @date: 2015/11/23 18:40
 */
public abstract class FileLoader {

    private static final String COOKIE_FLUSH_PATH = "cookie/index.txt";
    private static final String ROOT_PATH = FileLoader.class.getResource("").getPath();
    private static final String COOKIE_PROPERTY_PREFIX = "xueqiu.cookie.";

    private static final Map<String, String> cookie = new ConcurrentHashMap<>();//多线程写状态，存在并发

    /**
     * 加载最新cookie
     * @param key 关键字
     * @return cookie
     */
    public static String loadCookie(String key) {
        String cookies = System.getProperty("cookies");
        if (StringUtils.isNotNull(cookies) && cookies.trim().length() > 0) {
            return cookies.trim();
        }
        if (cookie.containsKey(key)) return cookie.get(key);
        String aggregated = aggregateCookieFromProperties();
        if (aggregated != null) {
            cookie.put(key, aggregated);
            return aggregated;
        }
        return EmptyObject.emptyString;
    }

    public static Map<String, String> loadCookieValues() {
        String aggregated = aggregateCookieFromProperties();
        if (aggregated == null) {
            return Collections.emptyMap();
        }
        return parseCookieString(aggregated);
    }

    private static String aggregateCookieFromProperties() {
        Properties properties = System.getProperties();
        Set<String> propertyNames = properties.stringPropertyNames();
        List<String> parts = propertyNames.stream()
                .filter(name -> name.startsWith(COOKIE_PROPERTY_PREFIX))
                .sorted()
                .map(name -> buildCookiePair(name, properties.getProperty(name)))
                .filter(StringUtils::isNotNull)
                .collect(Collectors.toCollection(ArrayList::new));
        if (parts.isEmpty()) {
            return null;
        }
        return String.join("; ", parts);
    }

    private static String buildCookiePair(String propertyName, String value) {
        if (!StringUtils.isNotNull(value) || value.trim().length() == 0) {
            return null;
        }
        String cookieKey = propertyName.substring(COOKIE_PROPERTY_PREFIX.length());
        if (cookieKey.length() == 0) {
            return null;
        }
        return cookieKey + "=" + value.trim();
    }

    private static Map<String, String> parseCookieString(String aggregated) {
        if (aggregated == null || aggregated.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        String[] pairs = aggregated.split(";\\s*");
        Map<String, String> result = new ConcurrentHashMap<>();
        for (String pair : pairs) {
            if (pair.trim().isEmpty()) {
                continue;
            }
            int index = pair.indexOf('=');
            if (index <= 0) {
                continue;
            }
            String key = pair.substring(0, index).trim();
            String val = pair.substring(index + 1).trim();
            if (!key.isEmpty()) {
                result.put(key, val);
            }
        }
        return result;
    }

    /**
     * 更新cookie
     * @param cookie cookie内容
     * @param key 关键字
     */
    public static void updateCookie(String cookie, String key) {
        if (StringUtils.isNotNull(cookie) && cookie.trim().length() > 0) {
            FileLoader.cookie.put(key, cookie);
            String replacedKey = key.contains(".com") ? key.substring(7, key.indexOf(".com")) : key;
            updateCookie(COOKIE_FLUSH_PATH, cookie, replacedKey);
        }
    }

    public static void updateCookie(String rawPath, String text, String key) {
        updateCookie(rawPath, text, key, new StringBuilder(), true);
    }

    /**
     * 若文件不存在则创建文件
     * @param rawPath 文件相对路径
     * @param text 更新内容
     * @param key 文件名字
     * @param  append 是否追加
     */
    public static void updateCookie(String rawPath, String text, String key, StringBuilder builder, boolean append) {
        String path = ROOT_PATH + rawPath;
        File cookie = new File(path);
        String p;
        if(!cookie.exists()) updateCookie(builder.append("../").toString() + rawPath, text, key, builder, append);
        else {

            try {
                p = path.replace("index", key);

                File file = new File(p);
                boolean success = true;
                if(!file.exists()) success = file.createNewFile();
                if(!success) throw new Exception();

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(p, append))) {
                    writer.write(text);
                }

            } catch (Exception e) {
                System.out.println("FileLoader -> 写入失败!");
            }
        }
    }

    /**
     * 加载文件内容（文件必须存在）
     * @param rawPath 文件相对位置
     * @return File
     */
    private static File loadFile(String rawPath, StringBuilder builder) {
        String path = ROOT_PATH + rawPath;
        File file = new File(path);

        if(!file.exists()) return loadFile(builder.append("../").toString() + rawPath, builder);
        else return file;
    }

    /**
     * 加载文件内容（文件必须存在）
     * @param rawPath 文件相对位置
     * @return 文件内容
     */
    private static String loadFileContent(String rawPath, StringBuilder builder) {
        File file = loadFile(rawPath, builder);
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String text;
            while ((text = reader.readLine()) != null) {
                content.append(text);
            }
        } catch (IOException e) {
            System.out.println("FileLoader -> 读取失败!");
        }
        return content.toString();
    }

    public static File loadFile(String rawPath) {
        return loadFile(rawPath, new StringBuilder());
    }

    public static String loadFileContent(String rawPath) {
        return loadFileContent(rawPath, new StringBuilder());
    }

}
