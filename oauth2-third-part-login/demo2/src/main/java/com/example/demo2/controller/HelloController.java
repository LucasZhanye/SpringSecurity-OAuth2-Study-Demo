package com.example.demo2.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String Hello() {
        return "Hello World";
    }

    @GetMapping("/user")
    public Map<String, Object> hello(@RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient oAuth2AuthorizedClient) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> map = new HashMap<>(2);
        map.put("oauth2client", oAuth2AuthorizedClient);
        map.put("authentication", authentication);

        return map;
    }
}
