package com.phegondev.InventoryManagementSystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Serves uploaded product images from the same folder the API writes to, so the Angular
 * dev server (port 4200) can load them via {@code http://localhost:5050/products/...}.
 */
@Configuration
public class WebStaticResourceConfig implements WebMvcConfigurer {

    @Value("${ims.upload.images-relative:../frontend/public/products/}")
    private String imagesRelativePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Path.of(System.getProperty("user.dir")).resolve(imagesRelativePath).normalize();
        String location = dir.toAbsolutePath().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/products/**").addResourceLocations(location);
    }
}
