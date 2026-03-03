package com.example.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final Path baseDir;

    public WebMvcConfig(AppProperties props) {
        this.baseDir = Paths.get(props.getUpload().getBaseDir()).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseLocation = baseDir.toUri().toString();

        registry.addResourceHandler("/thumbnails/**")
                .addResourceLocations(baseLocation + "thumbnails/");
    }

    @Bean
    public ResourceRegionHttpMessageConverter resourceRegionHttpMessageConverter() {
        return new ResourceRegionHttpMessageConverter();
    }
}
