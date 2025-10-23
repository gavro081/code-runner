package com.github.gavro081.apiserver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
//@RequestMapping("/api")
public class ApiServerController {
    @GetMapping("/")
    String index(){
        return "index";
    }
}
