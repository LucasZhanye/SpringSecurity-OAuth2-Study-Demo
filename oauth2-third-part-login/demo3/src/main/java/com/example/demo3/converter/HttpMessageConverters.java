package com.example.demo3.converter;

import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.ClassUtils;

final class HttpMessageConverters {
    private static final boolean jackson2Present;
    private static final boolean gsonPresent;
    private static final boolean jsonbPresent;

    private HttpMessageConverters() {
    }

    static GenericHttpMessageConverter<Object> getJsonMessageConverter() {
        if (jackson2Present) {
            return new MappingJackson2HttpMessageConverter();
        } else if (gsonPresent) {
            return new GsonHttpMessageConverter();
        } else {
            return jsonbPresent ? new JsonbHttpMessageConverter() : null;
        }
    }

    static {
        ClassLoader classLoader = com.example.demo3.converter.HttpMessageConverters.class.getClassLoader();
        jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) && ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
        gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
        jsonbPresent = ClassUtils.isPresent("javax.json.bind.Jsonb", classLoader);
    }
}
