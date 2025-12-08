package demo.bigwork.config;
//Spring Boot公開存取硬碟(server)上的資料夾(靜態資源 static resources)

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer{
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploads/**")	//URL
			.addResourceLocations("file:uploads/");	//storage's path
	}	
}
