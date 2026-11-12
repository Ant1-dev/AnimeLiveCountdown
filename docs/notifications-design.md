# Episode notifications, calendar feed and PWA: design

Written before implementation. Scope: reminders for followed shows (in-app bell, browser push,
Discord), a per-user ICS calendar feed, and making the site installable.

## Data model (all additive, `ddl-auto=update`)

| Table | Columns | Notes |
| --- | --- | --- |
| `notification_entity` (existing, extended) | + `media_id`, `episode`, `airing_at`, `title`, `image_url`, `kind`, `read_at` | Unique `(user_id, media_id, episode)`: one row per user+show+episode. The row is both the bell entry and the dedupe record. |
| `reminder` (new) | `user_id`, `media_id`, `lead_minutes` (0 = at airing, max 1440), `push_enabled`, `discord_enabled`, `created_at` | Unique `(user_id, media_id)`. Only applies while the show is in the user's favorites. |
| `push_subscription` (new) | `user_id`, `endpoint` (unique), `p256dh`, `auth`, `expiration_time`, `created_at`, `last_success_at` | One per browser/device. Deleted on 404/410 or when `expiration_time` passes. |
| `notification_settings` (new) | `user_id` (PK), `discord_webhook_url`, `calendar_token` (unique), `calendar_token_created_at` | Per-user channel settings. |

Reminders require following: saving a reminder for a show that is not in favorites also adds it to
favorites. Unfollowing stops reminders (the job joins on `favmedia`); the reminder row is kept, so
re-following restores the previous preference.

## Due-episode selection and dedupe

`EpisodeReminderJob` (own class, `@Scheduled`, default every 30 s) asks the DB for reminders of
followed shows whose next episode airs in `(now - 15 min, now + 30 s + 24 h]`, then filters in Java
(`DueEpisodeSelector`, pure and unit tested):

```
sendAt = airingAt - lead
due    = now + early >= sendAt  &&  airingAt > now - grace      (early = job interval, grace = 15 min)
```

* Sending up to one interval early guarantees "at airing" fires before the just-aired sync (every
  minute) advances `media.next_airing_episode`.
* A reminder created inside the lead window, or a server that was down, still sends once, until 15
  minutes after airing ("is out now" wording after airing).
* Dedupe: `INSERT ... ON CONFLICT (user_id, media_id, episode) DO NOTHING`. Only if a row was
  inserted are push/Discord sent, so each user+show+episode is notified once, even with several
  backend instances. A delayed episode is not re-notified.

## Channels

* **In-app (bell)**: always; it is the notification row.
* **Web Push**: VAPID (`VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, `VAPID_SUBJECT`). Payload encrypted
  per RFC 8291 (`aes128gcm`) with the JDK only (ECDH P-256, HKDF via HmacSHA256, AES-GCM, ES256),
  no new dependencies; verified against the RFC 8291 test vector. 404/410 deletes the
  subscription. Push UI is hidden when the keys are unset.
* **Discord: per-user webhook URL** (chosen over bot DMs). A webhook needs no bot token, no shared
  server, and no DM permissions; the user picks any channel they control (a private server works as
  a personal inbox). A bot DM would need the owner to run a bot, every user to share a server with
  it, and DMs from server members to be allowed, and the site's Discord OAuth scopes
  (`identify email`) don't help with any of that. URLs are validated against
  `https://discord.com/api/webhooks/{id}/{token}` (and `discordapp.com`, `ptb.`, `canary.`) to
  prevent SSRF; a 404 from Discord clears the stored URL. Hidden unless
  `DISCORD_WEBHOOK_NOTIFICATIONS_ENABLED=true`.

## Calendar feed

`GET /api/calendar/{token}.ics` (public, authenticated only by the 256-bit random token) returns
`text/calendar` with one `VEVENT` per followed show's next episode: `DTSTART`/`DTEND` in UTC
(`...Z`), duration from `media_info.episode_duration` (default 24 min), stable `UID`
`{mediaId}-{episode}@animelivecountdown.com`, a link to the show page, RFC 5545 escaping and 75-octet
line folding, `REFRESH-INTERVAL`/`X-PUBLISHED-TTL` of 1 hour. The settings page shows the
`webcal://` and `https://` URLs and can regenerate (old URL stops working) or disable the feed.

## Endpoints

Public: `GET /api/notifications/config` (push enabled + VAPID public key, Discord enabled),
`GET /api/calendar/{token}.ics`.

Signed in (`/api/auth/**`, already authenticated by `SecurityConfig`; no change to it):

* `GET /api/auth/notifications` (recent 30 + unread count), `POST /api/auth/notifications/{id}/read`,
  `POST /api/auth/notifications/read-all`
* `GET /api/auth/reminders`, `GET /api/auth/reminders/{mediaId}`, `PUT /api/auth/reminders/{mediaId}`
  `{leadMinutes, push, discord}`, `DELETE /api/auth/reminders/{mediaId}`
* `POST /api/auth/push/subscribe` (browser `PushSubscription` JSON), `POST /api/auth/push/unsubscribe`
  `{endpoint}`, `POST /api/auth/push/test`
* `GET /api/auth/notification-settings`, `PUT|DELETE /api/auth/notification-settings/discord`,
  `POST /api/auth/notification-settings/discord/test`, `POST|DELETE /api/auth/calendar/token`

## Frontend

* `public/sw.js` at the site root: `push` (show notification, tell open tabs to refresh the bell),
  `notificationclick` (focus or open the show page). No fetch caching, so deploys never serve stale
  app code.
* Notification bell component (replaces the static icon in the header): unread badge, panel with
  recent notifications, mark read, link to settings, install button when available.
* Reminder dialog opened by the show card's "Add reminder": lead time (at airing, 5/15/30 min,
  1/2 h, 1 day) and channels; asks for push permission on save if push is chosen.
* `/notifications` settings page (also linked from the profile menu): push on this device + test,
  Discord webhook + test, calendar feed (copy/regenerate/disable), all reminders, install app.
* PWA: fixed manifest link (it pointed at a missing file), name/short name, `id`, `start_url`,
  `scope`, theme/background `#101116`, 192/512 icons plus maskable versions, `beforeinstallprompt`
  install button, iOS "Add to Home Screen" hint (iOS only delivers push to installed web apps).

## Out of scope

* Full future-episode lists in the calendar (the DB only stores each show's next episode; events
  roll forward as the feed refreshes).
* Email notifications, Discord bot DMs, per-device channel choice, offline caching.
* Re-notifying when an episode is delayed after a reminder was sent.
