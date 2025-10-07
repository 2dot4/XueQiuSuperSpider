package org.decaywood.utils;

import org.decaywood.GlobalSystemConfigLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * @author: decaywood
 * @date: 2015/11/23 14:27
 */
public class HttpRequestHelper {

    private static final String HEADER_CONFIG_PREFIX = "xueqiu.header.";

    private static final Map<String, String> DEFAULT_HEADERS;

    static {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Connection", "keep-alive");
        headers.put("X-Requested-With", "XMLHttpRequest");
        DEFAULT_HEADERS = Collections.unmodifiableMap(headers);
    }

    private final Map<String, String> config;
    private final String webSite;
    private boolean post;
    private boolean gzip;

    public HttpRequestHelper(String webSite) {
        this(webSite, true);
    }

    public HttpRequestHelper(String webSite, boolean gzip) {
        GlobalSystemConfigLoader.loadConfig();
        this.webSite = webSite;
        this.config = new LinkedHashMap<>();
        if (gzip) {
            this.gzipDecode()
                    .addToHeader("Accept-Encoding", "gzip,deflate,sdch");
        } else {
            this.addToHeader("Accept-Encoding", "utf-8");
        }
        applyDefaultHeaders();
        applyConfiguredHeaders();
    }

    public HttpRequestHelper post() {
        this.post = true;
        return this;
    }

    public HttpRequestHelper gzipDecode() {
        this.gzip = true;
        return this;
    }

    public HttpRequestHelper addToHeader(String key, String val) {
        this.config.put(key, val);
        return this;
    }

    public HttpRequestHelper addToHeader(String key, int val) {
        this.config.put(key, String.valueOf(val));
        return this;
    }

    public String request(URL url) throws IOException {
        return request(url, this.config);
    }


    public String request(URL url, Map<String, String> config) throws IOException {
        HttpURLConnection httpURLConn = null;
        try {
            httpURLConn = (HttpURLConnection) url.openConnection();
            if (post) httpURLConn.setRequestMethod("POST");
            httpURLConn.setDoOutput(true);
            for (Map.Entry<String, String> entry : config.entrySet())
                httpURLConn.setRequestProperty(entry.getKey(), entry.getValue());
            httpURLConn.connect();
            InputStream in = httpURLConn.getInputStream();
            if (gzip) in = new GZIPInputStream(in);
            BufferedReader bd = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            String text;
            while ((text = bd.readLine()) != null) builder.append(text);
            return builder.toString();
        } finally {
            if (httpURLConn != null) httpURLConn.disconnect();
        }
    }

    private void applyDefaultHeaders() {
        DEFAULT_HEADERS.forEach(this::addHeaderIfAbsent);

        String cookie = FileLoader.loadCookie(webSite);
        if (StringUtils.isNotNull(cookie) && !EmptyObject.emptyString.equals(cookie)) {
            addHeaderIfAbsent("Cookie", cookie);
        }

        addHeaderIfAbsent("Referer", webSite);

        try {
            URI uri = URI.create(webSite);
            String host = uri.getHost();
            if (host != null) {
                addHeaderIfAbsent("Host", host);
                String origin = uri.getScheme() + "://" + host;
                addHeaderIfAbsent("Origin", origin);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void applyConfiguredHeaders() {
        Properties properties = System.getProperties();
        for (String propertyName : properties.stringPropertyNames()) {
            if (propertyName.startsWith(HEADER_CONFIG_PREFIX)) {
                String headerKey = propertyName.substring(HEADER_CONFIG_PREFIX.length());
                if (headerKey.length() == 0) {
                    continue;
                }
                String value = properties.getProperty(propertyName);
                if (Objects.nonNull(value)) {
                    this.addToHeader(headerKey.trim(), value.trim());
                }
            }
        }
    }

    private void addHeaderIfAbsent(String key, String value) {
        if (StringUtils.isNotNull(key) && StringUtils.isNotNull(value)) {
            this.config.putIfAbsent(key, value);
        }
    }

}
