package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
public class SitemapController {

    @Autowired
    private MediaInfoRepository mediaInfoRepository;

    @Cacheable(value = "sitemapCache", key = "'mediaSitemap'")
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> generateSitemap() {
        // get trending media (top 20 by popularity)
        Pageable pageable = PageRequest.of(0, 20);
        List<MediaInfoEntity> trendingMedia = mediaInfoRepository.findAllByOrderByPopularityDesc(pageable).getContent();

        // get all media IDs for complete sitemap
        List<Long> allMediaIds = mediaInfoRepository.findAllIds();

        String today = LocalDate.now().toString();

        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n")
                .append("  <url>\n")
                .append("    <loc>https://animelivecountdown.com/</loc>\n")
                .append("    <lastmod>").append(today).append("</lastmod>\n")
                .append("    <changefreq>daily</changefreq>\n")
                .append("    <priority>1.0</priority>\n")
                .append("  </url>\n");

        // add trending media pages with higher priority
        for (MediaInfoEntity media : trendingMedia) {
            String lastMod = today;
            if (media.getAiringat() != null) {
                lastMod = LocalDate.ofInstant(media.getAiringat(), ZoneId.systemDefault())
                        .format(DateTimeFormatter.ISO_LOCAL_DATE);
            }

            sitemap.append("  <url>\n")
                    .append("    <loc>https://animelivecountdown.com/media/").append(media.getId()).append("</loc>\n")
                    .append("    <lastmod>").append(lastMod).append("</lastmod>\n")
                    .append("    <changefreq>daily</changefreq>\n")
                    .append("    <priority>0.9</priority>\n")
                    .append("  </url>\n");
        }

        // add remaining media with lower priority
        // add IDs that weren't already added from trending
        for (Long id : allMediaIds) {
            // Skip if already in trending
            if (trendingMedia.stream().anyMatch(m -> m.getId().equals(id))) {
                continue;
            }

            sitemap.append("  <url>\n")
                    .append("    <loc>https://animelivecountdown.com/media/").append(id).append("</loc>\n")
                    .append("    <lastmod>").append(today).append("</lastmod>\n")
                    .append("    <changefreq>weekly</changefreq>\n")
                    .append("    <priority>0.7</priority>\n")
                    .append("  </url>\n");
        }

        sitemap.append("</urlset>");

        return ResponseEntity.ok().body(sitemap.toString());
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRobots() {
        StringBuilder robots = new StringBuilder();
        robots.append("User-agent: *\n")
                .append("Allow: /\n\n")
                .append("Sitemap: https://animelivecountdown.com/sitemap.xml");

        return ResponseEntity.ok().body(robots.toString());
    }
}