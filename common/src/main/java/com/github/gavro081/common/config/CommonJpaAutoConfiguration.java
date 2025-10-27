package com.github.gavro081.common.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EntityScan(basePackages = "com.github.gavro081.common.model")
public class CommonJpaAutoConfiguration {
}
