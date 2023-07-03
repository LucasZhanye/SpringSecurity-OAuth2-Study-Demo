package com.example.demo3.entity;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Data
public class WeChatEntity implements OAuth2User {
    // 用户的唯一标识
    private String openid;

    // 用户昵称
    private String nickname;

    // 用户的性别，值为1表示男，值为2表示女，值为0表示未知
    private Integer sex;

    // 用户个人资料填写的省份
    private String province;

    // 普通用户个人资料填写的城市
    private String city;

    // 国家，如中国为CN
    private String country;

    // 用户头像,最后一个数值代表正方形头像大小（有0、46、64、96、132数值可选，0代表640*640正方形头像），
    // 用户没有头像时该项为空。若用户更换头像，原有头像URL将失效。
    private String headimgurl;

    // 用户特权信息
    private List<String> privilege;

    // 只有在用户将公众号绑定到微信开放平台帐号后，才会出现该字段。
    private String unionid;

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    /**
     不可以返回null，在构建实体时会有断言
     **/
    @Override
    public String getName() {
        return nickname;
    }
}
