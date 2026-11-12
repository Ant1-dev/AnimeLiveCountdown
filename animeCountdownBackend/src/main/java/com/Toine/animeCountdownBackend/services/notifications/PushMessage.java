package com.Toine.animeCountdownBackend.services.notifications;

/**
 * What the service worker shows. {@code url} is a site path opened on click; {@code tag} replaces
 * an earlier notification with the same tag instead of stacking.
 */
public record PushMessage(String title, String body, String url, String icon, String tag) {
}
