package com.Toine.animeCountdownBackend.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SitemapConfig {

    private final CacheManager cacheManager;

    public SitemapConfig(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    // Refresh sitemap cache daily at 3 am
    @Scheduled(cron = "0 0 3 * * ?")
    public void refreshSitemapCache() {
        if (cacheManager.getCache("sitemapCache") != null) {
            cacheManager.getCache("sitemapCache").evict("mediaSitemap");
        }
    }
}