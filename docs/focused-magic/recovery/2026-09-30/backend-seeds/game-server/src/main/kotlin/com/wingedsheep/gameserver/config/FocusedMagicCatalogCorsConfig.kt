package com.wingedsheep.gameserver.config

import com.wingedsheep.engine.registry.CardRegistry
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class FocusedMagicCatalogCorsConfig(private val cardRegistry: CardRegistry) : WebMvcConfigurer {
    @Bean
    fun focusedMagicCatalogGate() = ApplicationRunner {
        val required = listOf(
            "Gemstone Caverns",
            "Oscorp Industries",
            "Barrowgoyf",
            "Daze",
            "Fatal Push",
            "Force of Will",
            "Murktide Regent",
            "Sheoldred's Edict",
            "Tamiyo, Inquisitive Student",
            "Consign to Memory",
            "Dauthi Voidwalker",
            "Engineered Explosives",
            "Hydroblast",
            "Null Rod",
            "Toxic Deluge",
            "Blood Scrivener",
            "Currency Converter",
            "Cutthroat il-Dal",
            "Demonfire",
            "Gathan Raiders",
            "Geier Reach Sanitarium",
            "Izzet Charm",
            "Jagged Poppet",
            "Library of Leng",
            "Persist",
            "Taste for Mayhem",
            "The Biblioplex",
            "Surgical Extraction",
            "Grey Ogre",
        )
        val missing = required.filterNot(cardRegistry::hasCard)
        check(missing.isEmpty()) {
            "FOCUSED_MAGIC_CATALOG_GATE FAIL missing=${missing.joinToString(" | ")}"
        }
        println("FOCUSED_MAGIC_CATALOG_GATE PASS registrySize=${cardRegistry.size} required=${required.size}")
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/cards/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "OPTIONS")
    }
}
