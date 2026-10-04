package com.wingedsheep.gameserver.config
import com.wingedsheep.engine.registry.CardRegistry
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
@Configuration
class FocusedMagicCatalogCorsConfig(private val c:CardRegistry):WebMvcConfigurer{
 @Bean fun g()=ApplicationRunner{check(c.hasCard("Gemstone Caverns")&&c.hasCard("Oscorp Industries")){"FOCUSED_MAGIC_CATALOG_GATE FAIL"};println("FOCUSED_MAGIC_CATALOG_GATE PASS registrySize=${c.size}")}
 override fun addCorsMappings(r:CorsRegistry){
  r.addMapping("/api/cards/**").allowedOriginPatterns("*").allowedMethods("GET","OPTIONS")
  r.addMapping("/api/printings").allowedOriginPatterns("*").allowedMethods("GET","OPTIONS")
 }
}
