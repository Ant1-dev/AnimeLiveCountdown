package com.Toine.animeCountdownBackend.services.notifications;

/** Which channels a user's reminder for one show uses. */
public record ReminderChannels(Long userId, boolean pushEnabled, boolean discordEnabled) {
}
