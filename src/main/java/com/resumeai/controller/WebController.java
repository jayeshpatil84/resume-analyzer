package com.resumeai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves Thymeleaf HTML pages.
 */
@Controller
public class WebController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/history")
    public String history() {
        return "history";
    }
}
