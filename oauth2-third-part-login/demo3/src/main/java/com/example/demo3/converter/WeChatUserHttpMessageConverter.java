package com.example.demo3.converter;


import com.example.demo3.entity.WeChatEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


public class WeChatUserHttpMessageConverter extends AbstractHttpMessageConverter<WeChatEntity> {
    private static final ParameterizedTypeReference<WeChatEntity> STRING_OBJECT_MAP;
    private static final Charset DEFAULT_CHARSET;

    private GenericHttpMessageConverter<Object> jsonMessageConverter = HttpMessageConverters.getJsonMessageConverter();

    static {
        DEFAULT_CHARSET = StandardCharsets.UTF_8;
        STRING_OBJECT_MAP = new ParameterizedTypeReference<WeChatEntity>() {
        };
    }

    public WeChatUserHttpMessageConverter() {
        super(DEFAULT_CHARSET, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return  WeChatEntity.class.isAssignableFrom(clazz);
    }

    @Override
    protected WeChatEntity readInternal(Class<? extends WeChatEntity> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        try {
            WeChatEntity weChatEntity = (WeChatEntity)this.jsonMessageConverter.read(STRING_OBJECT_MAP.getType(), (Class)null, inputMessage);
            return weChatEntity;

        } catch (Exception var5) {
            throw new HttpMessageNotReadableException("An error occurred reading the OAuth 2.0 Access Token Response: " + var5.getMessage(), var5, inputMessage);
        }

    }

    @Override
    protected void writeInternal(WeChatEntity weChatEntity, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {

    }
}

