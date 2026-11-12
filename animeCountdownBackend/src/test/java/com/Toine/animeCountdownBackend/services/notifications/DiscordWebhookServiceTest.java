package com.Toine.animeCountdownBackend.services.notifications;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiscordWebhookServiceTest {

    @Test
    void acceptsOnlyDiscordWebhookUrls() {
        assertTrue(DiscordWebhookService.isValidWebhookUrl("https://discord.com/api/webhooks/123456789012345678/abcDEF_ghi-JKLmnopqrstuvwxyz0123"));
        assertTrue(DiscordWebhookService.isValidWebhookUrl("https://canary.discord.com/api/webhooks/123456789012345678/abcDEF_ghi-JKLmnopqrstuvwxyz0123"));
        assertTrue(DiscordWebhookService.isValidWebhookUrl("https://discordapp.com/api/v10/webhooks/123456789012345678/abcDEF_ghi-JKLmnopqrstuvwxyz0123"));

        assertFalse(DiscordWebhookService.isValidWebhookUrl(null));
        assertFalse(DiscordWebhookService.isValidWebhookUrl("http://discord.com/api/webhooks/123456789012345678/abcDEF_ghi-JKLmnopqrstuvwxyz0123"));
        assertFalse(DiscordWebhookService.isValidWebhookUrl("https://discord.com.evil.net/api/webhooks/123456789012345678/abcDEF_ghi-JKLmnopqrstuvwxyz0123"));
        assertFalse(DiscordWebhookService.isValidWebhookUrl("https://evil.net/?https://discord.com/api/webhooks/123456789012345678/abcDEF_ghi-JKLmnopqrstuvwxyz0123"));
        assertFalse(DiscordWebhookService.isValidWebhookUrl("https://discord.com/api/webhooks/123456789012345678/abc/../../users"));
    }

    @Test
    void masksTheSecretToken() {
        assertEquals("https://discord.com/api/webhooks/123456789012345678/••••••",
                DiscordWebhookService.mask("https://discord.com/api/webhooks/123456789012345678/secret-token-value"));
        assertNull(DiscordWebhookService.mask(null));
    }

    @Test
    void disabledServiceSendsNothing() {
        DiscordWebhookService off = new DiscordWebhookService(false, null);
        assertFalse(off.isEnabled());
        assertEquals(DiscordWebhookService.Result.FAILED,
                off.send("https://discord.com/api/webhooks/123456789012345678/abcDEF_ghi-JKLmnopqrstuvwxyz0123", "t", "d", null, null, null));
    }
}
