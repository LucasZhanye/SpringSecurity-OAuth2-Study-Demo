package com.example.demo3.config;

import com.example.demo3.service.WeChatUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.*;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
@EnableWebSecurity(debug = true)
public class SpringSecurityConfig {

    private final static String WECHAT_APPID = "appid";
    private final static String WECHAT_SECRET = "secret";
    private final static String WECHAT_FRAGMENT = "wechat_redirect";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,  ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                .authorizeHttpRequests()
                .anyRequest()
                .authenticated().and()
                .oauth2Login(oauth2LoginCustomizer -> {
                    // 授权端点配置
                    oauth2LoginCustomizer.authorizationEndpoint().authorizationRequestResolver(customOAuth2AuthorizationRequestResolver(clientRegistrationRepository));
                    // 获取token端点配置
                    oauth2LoginCustomizer.tokenEndpoint().accessTokenResponseClient(customOAuth2AccessTokenResponseClient());
                    // 获取用户信息端点配置
                    oauth2LoginCustomizer.userInfoEndpoint().userService(new WeChatUserService());
                });

        return http.build();
    }

    /**
     * 1. 自定义微信获取授权码的uri
     * https://open.weixin.qq.com/connect/oauth2/authorize?
     * appid=wx807d86fb6b3d4fd2
     * &redirect_uri=http%3A%2F%2Fdevelopers.weixin.qq.com
     * &response_type=code
     * &scope=snsapi_userinfo
     * &state=STATE  非必须
     * #wechat_redirect
     * 微信比较特殊，比如不是clientid，而是appid，还强制需要一个锚点#wechat+redirect
     * @return
     */
    public OAuth2AuthorizationRequestResolver customOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        // 定义一个默认的oauth2请求解析器
        DefaultOAuth2AuthorizationRequestResolver oAuth2AuthorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        // 进行自定义
        Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer = (builder) -> {
            builder.attributes(attributeConsumer -> {
                // 判断registrationId是否为wechat
                String registrationId = (String) attributeConsumer.get(OAuth2ParameterNames.REGISTRATION_ID);

                if ("wechat".equals(registrationId)) {
                    // 替换参数名称
                    builder.parameters(this::replaceWechatUriParamter);
                    // 增加锚点，需要在uri构建中添加
                    builder.authorizationRequestUri((uriBuilder) -> {
                        uriBuilder.fragment(WECHAT_FRAGMENT);
                        return uriBuilder.build();
                    });
                }
            });
        };
        // 设置authorizationRequestCustomizer
        oAuth2AuthorizationRequestResolver.setAuthorizationRequestCustomizer(authorizationRequestCustomizer);

        return oAuth2AuthorizationRequestResolver;
    }

    /**
     * 替换Uri参数，parameterMap是保存的请求的各个参数
     * @param parameterMap
     */
    private void replaceWechatUriParamter(Map<String, Object> parameterMap) {
        Map<String, Object> linkedHashMap = new LinkedHashMap<>();

        // 遍历所有参数，有序的,替换掉clientId为appid
        parameterMap.forEach((k, v) -> {
            if (OAuth2ParameterNames.CLIENT_ID.equals(k)) {
                linkedHashMap.put(WECHAT_APPID, v);
            } else {
                linkedHashMap.put(k, v);
            }
        });
        // 清空原始的paramterMap
        parameterMap.clear();
        // 将新的linkedHashMap存入paramterMap
        parameterMap.putAll(linkedHashMap);
    }

    /**
     * 2. 自定义请求access_token时的请求体转换器
     * 获取access_token
     * https://api.weixin.qq.com/sns/oauth2/access_token?
     * appid=APPID
     * &secret=SECRET
     * &code=CODE 从上一个请求响应中获取
     * &grant_type=authorization_code  框架帮忙填写了
     */
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> customOAuth2AccessTokenResponseClient() {
        // 定义默认的Token响应客户端
        DefaultAuthorizationCodeTokenResponseClient oAuth2AccessTokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();

        // 定义默认的转换器
        OAuth2AuthorizationCodeGrantRequestEntityConverter oAuth2AuthorizationCodeGrantRequestEntityConverter = new OAuth2AuthorizationCodeGrantRequestEntityConverter();
        // 自定义参数转换器
        Converter<OAuth2AuthorizationCodeGrantRequest, MultiValueMap<String, String>> customParameterConverter = (authorizationCodeGrantRequest) -> {
            ClientRegistration clientRegistration = authorizationCodeGrantRequest.getClientRegistration();
            OAuth2AuthorizationExchange authorizationExchange = authorizationCodeGrantRequest.getAuthorizationExchange();
            MultiValueMap<String, String> parameters = new LinkedMultiValueMap();
            parameters.add("grant_type", authorizationCodeGrantRequest.getGrantType().getValue());
            parameters.add("code", authorizationExchange.getAuthorizationResponse().getCode());
            String redirectUri = authorizationExchange.getAuthorizationRequest().getRedirectUri();
            String codeVerifier = (String)authorizationExchange.getAuthorizationRequest().getAttribute("code_verifier");
            if (redirectUri != null) {
                parameters.add("redirect_uri", redirectUri);
            }

            parameters.add(WECHAT_APPID, clientRegistration.getClientId());

            parameters.add(WECHAT_SECRET, clientRegistration.getClientSecret());

            if (codeVerifier != null) {
                parameters.add("code_verifier", codeVerifier);
            }

            System.out.println("test---------------");
            return parameters;
        };
        // 设置自定义参数转换器
        oAuth2AuthorizationCodeGrantRequestEntityConverter.setParametersConverter(customParameterConverter);

        // 自定义RestTemplate处理响应content-type为“text/plain”
        OAuth2AccessTokenResponseHttpMessageConverter oAuth2AccessTokenResponseHttpMessageConverter = new OAuth2AccessTokenResponseHttpMessageConverter();
        oAuth2AccessTokenResponseHttpMessageConverter.setSupportedMediaTypes(Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON));

        // 处理TOKEN_TYPE为null的问题，自定义accessTokenResponseParametersConverter，给TOKEN_TYPE赋值
        // 因为已经有默认的处理了，只是需要给token_type赋值
        Converter<Map<String, Object>, OAuth2AccessTokenResponse> setAccessTokenResponseConverter = (paramMap) -> {
            DefaultMapOAuth2AccessTokenResponseConverter defaultMapOAuth2AccessTokenResponseConverter = new DefaultMapOAuth2AccessTokenResponseConverter();
            paramMap.put(OAuth2ParameterNames.TOKEN_TYPE, OAuth2AccessToken.TokenType.BEARER.getValue());
            return defaultMapOAuth2AccessTokenResponseConverter.convert(paramMap);
        };

        // 设置这个转换器
        oAuth2AccessTokenResponseHttpMessageConverter.setAccessTokenResponseConverter(setAccessTokenResponseConverter);

        RestTemplate restTemplate = new RestTemplate(Arrays.asList(new FormHttpMessageConverter(), oAuth2AccessTokenResponseHttpMessageConverter));
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

        // 设置自定义转换器
        oAuth2AccessTokenResponseClient.setRequestEntityConverter(oAuth2AuthorizationCodeGrantRequestEntityConverter);
        // 设置自定义RestTemplate
        oAuth2AccessTokenResponseClient.setRestOperations(restTemplate);

        return oAuth2AccessTokenResponseClient;
    }

}

