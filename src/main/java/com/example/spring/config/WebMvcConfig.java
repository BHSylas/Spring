package com.example.spring.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;

import java.util.List;

@Configuration
public class WebMvcConfig {

    @Bean
    public ResourceRegionHttpMessageConverter resourceRegionHttpMessageConverter() {
        return new ResourceRegionHttpMessageConverter();
    }

    @Bean
    public ApplicationRunner runner(List<HttpMessageConverter<?>> converters) {
        return args -> {
            System.out.println("=== Registered Converters ===");
            converters.forEach(c ->
            {
                System.out.println(c.getClass().getName());
                if (c instanceof ResourceRegionHttpMessageConverter rr) {
                    System.out.println("Supports video/mp4: " +
                            rr.canWrite(ResourceRegion.class, MediaType.valueOf("video/mp4")));
                }
            }

            );
        };
    }

}
