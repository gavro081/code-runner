package com.github.gavro081.apiserver.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class IndexController {
    @Value("${server.port}")
    private String port;

    @GetMapping()
    String index(){
        return "api index: " + port;
    }
}
