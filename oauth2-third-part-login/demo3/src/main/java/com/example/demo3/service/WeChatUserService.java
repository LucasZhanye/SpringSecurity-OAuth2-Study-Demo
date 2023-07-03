package com.example.demo3.service;


import com.example.demo3.converter.WeChatUserHttpMessageConverter;
import com.example.demo3.entity.WeChatEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownContentTypeException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

/** 3. 定义provider，覆盖userservice，设置自定义的
 * https://api.weixin.qq.com/sns/userinfo?
 * access_token=ACCESS_TOKEN
 * &openid=OPENID
 * &lang=zh_CN
 */
public class WeChatUserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private static final ParameterizedTypeReference<WeChatEntity> PARAMETERIZED_RESPONSE_TYPE = new ParameterizedTypeReference<WeChatEntity>() {
    };


    private RestOperations restOperations;

    public WeChatUserService() {
        WeChatUserHttpMessageConverter weChatUserHttpMessageConverter = new WeChatUserHttpMessageConverter();
        RestTemplate restTemplate = new RestTemplate(Arrays.asList(weChatUserHttpMessageConverter));
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        this.restOperations = restTemplate;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        Assert.notNull(userRequest, "userRequest cannot be null");
        if (!StringUtils.hasText(userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri())) {
            OAuth2Error oauth2Error = new OAuth2Error("missing_user_info_uri", "Missing required UserInfo Uri in UserInfoEndpoint for Client Registration: " + userRequest.getClientRegistration().getRegistrationId(), (String)null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        } else {
            String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
            if (!StringUtils.hasText(userNameAttributeName)) {
                OAuth2Error oauth2Error = new OAuth2Error("missing_user_name_attribute", "Missing required \"user name\" attribute name in UserInfoEndpoint for Client Registration: " + userRequest.getClientRegistration().getRegistrationId(), (String)null);
                throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
            } else {
                ResponseEntity<WeChatEntity> response = this.getResponse(userRequest);
                WeChatEntity userAttributes = (WeChatEntity)response.getBody();
                System.out.println("-------user = " +  userAttributes);
                return userAttributes;
            }

        }
    }

    private ResponseEntity<WeChatEntity> getResponse(OAuth2UserRequest userRequest) {
        OAuth2Error oauth2Error;
        try {
            // 发起Get请求，请求参数是query参数，需要自己拼接
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add("access_token", userRequest.getAccessToken().getTokenValue());
            // 获取access token时，其他参数被存储在了userRequest中，从里面把openid拿出来
            queryParams.add("openid", (String) userRequest.getAdditionalParameters().get("openid"));
            queryParams.add("lang", "zh_CN");

            URI uri = UriComponentsBuilder.fromUriString(userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri()).queryParams(queryParams).build().toUri();

            ResponseEntity<WeChatEntity> retData = this.restOperations.exchange(uri, HttpMethod.GET, null, PARAMETERIZED_RESPONSE_TYPE);

            return retData;
        } catch (OAuth2AuthorizationException var6) {
            oauth2Error = var6.getError();
            StringBuilder errorDetails = new StringBuilder();
            errorDetails.append("Error details: [");
            errorDetails.append("UserInfo Uri: ").append(userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri());
            errorDetails.append(", Error Code: ").append(oauth2Error.getErrorCode());
            if (oauth2Error.getDescription() != null) {
                errorDetails.append(", Error Description: ").append(oauth2Error.getDescription());
            }

            errorDetails.append("]");
            oauth2Error = new OAuth2Error("invalid_user_info_response", "An error occurred while attempting to retrieve the UserInfo Resource: " + errorDetails.toString(), (String)null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), var6);
        } catch (UnknownContentTypeException var7) {
            String errorMessage = "An error occurred while attempting to retrieve the UserInfo Resource from '" + userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri() + "': response contains invalid content type '" + var7.getContentType().toString() + "'. The UserInfo Response should return a JSON object (content type 'application/json') that contains a collection of name and value pairs of the claims about the authenticated End-User. Please ensure the UserInfo Uri in UserInfoEndpoint for Client Registration '" + userRequest.getClientRegistration().getRegistrationId() + "' conforms to the UserInfo Endpoint, as defined in OpenID Connect 1.0: 'https://openid.net/specs/openid-connect-core-1_0.html#UserInfo'";
            oauth2Error = new OAuth2Error("invalid_user_info_response", errorMessage, (String)null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), var7);
        } catch (RestClientException var8) {
            oauth2Error = new OAuth2Error("invalid_user_info_response", "An error occurred while attempting to retrieve the UserInfo Resource: " + var8.getMessage(), (String)null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), var8);
        }
    }
}

