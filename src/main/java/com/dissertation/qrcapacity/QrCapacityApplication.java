package com.dissertation.qrcapacity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class QrCapacityApplication {

	public static void main(String[] args) {
		SpringApplication.run(QrCapacityApplication.class, args);
	}

	public class WebMvcConfig implements WebMvcConfigurer {
		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/images/**")
					.addResourceLocations("file:src/main/resources/static/images/");
		}
	}

}
