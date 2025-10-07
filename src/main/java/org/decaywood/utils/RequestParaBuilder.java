package org.decaywood.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author: decaywood
 * @date: 2015/11/23 16:57
 */
public class RequestParaBuilder {

    private final String target;
    private final Map<String, List<Supplier<String>>> parameters;

    public RequestParaBuilder(String target) {
        this.target = target;
        this.parameters = new LinkedHashMap<>();
    }

    public RequestParaBuilder addParameter(String key, String val) {
        return addSupplier(key, () -> val);
    }

    public RequestParaBuilder addParameter(String key, boolean val) {
        return addSupplier(key, () -> Boolean.toString(val));
    }

    public RequestParaBuilder addParameter(String key, int val) {
        return addSupplier(key, () -> Integer.toString(val));
    }

    public RequestParaBuilder addParameter(String key, long val) {
        return addSupplier(key, () -> Long.toString(val));
    }

    public RequestParaBuilder addDynamicParameter(String key, Supplier<String> supplier) {
        return addSupplier(key, supplier);
    }

    private RequestParaBuilder addSupplier(String key, Supplier<String> supplier) {
        parameters.computeIfAbsent(key, k -> new ArrayList<>()).add(supplier);
        return this;
    }

    public String build() {
        StringBuilder builder = new StringBuilder(target);
        if (parameters.isEmpty()) {
            return builder.toString();
        }
        builder.append('?');
        boolean first = true;
        for (Map.Entry<String, List<Supplier<String>>> entry : parameters.entrySet()) {
            String encodedKey = encode(entry.getKey());
            for (Supplier<String> supplier : entry.getValue()) {
                String value = supplier == null ? null : supplier.get();
                if (!first) {
                    builder.append('&');
                } else {
                    first = false;
                }
                builder.append(encodedKey).append('=');
                builder.append(encode(value));
            }
        }
        return builder.toString();
    }

    private String encode(String text) {
        if (text == null) {
            return "";
        }
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

}
