package com.phuonghieuto.backend.auth_service.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginPageController {

    @GetMapping("/login-page")
    public String login() {
        return "login";
    }

    @GetMapping("/oauth2/redirect")
    public String oauth2Redirect() {
        return "oauth2-redirect";
    }
}