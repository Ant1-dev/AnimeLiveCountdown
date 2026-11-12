# Share cards, episode reactions, Most Hyped

Design note written before implementation. Covers three features that ship together.

## 1. Share cards (link previews)

### Problem
The frontend is a client-rendered SPA on Vercel. Discord, X, Slack, iMessage and friends fetch the
raw HTML and never run JS, so every `/media/:id` link shows the generic site preview from
`index.html`.

### Choice: user-agent rewrite on Vercel → backend-rendered HTML + JPEG card
- **`vercel.json`** gets one rewrite, placed before the SPA catch-all:
  `/media/:id` **when the `user-agent` header matches a link-preview bot**
  (Discordbot, Twitterbot, Slackbot, facebookexternalhit/Facebot (iMessage sends these),
  WhatsApp, TelegramBot, LinkedInBot, …) is proxied to
  `https://api.animelivecountdown.com/api/share/media/:id`.
  Browsers and search engines (Googlebot renders JS) keep getting the SPA unchanged.
  This is the same mechanism the site already uses for `/sitemap.xml`.
- **Backend `GET /api/share/media/{id}`** returns a tiny HTML page with per-show
  `og:*` / `twitter:*` tags: title, description ("Episode 5 airs in 2d 4h · …"), canonical URL
  (`https://animelivecountdown.com/media/{id}`) and `og:image`.
- **Backend `GET /api/share/media/{id}/card.jpg`** renders a 1200×630 card with Java2D (no new
  dependency): darkened banner background, cover art, title, and the countdown line
  ("Ep 5 airs in 2d 4h"). Rendered images are cached in memory by `(id, countdown text)`; cover and
  banner bytes are cached separately. Only `https` images from AniList hosts are fetched.
- **Freshness:** both responses send `Cache-Control: public, max-age=300, s-maxage=300`. The
  `og:image` URL carries a `?v=<5-minute bucket>` so a re-scraped page always points at a fresh
  image instead of one a platform cached earlier.
- If rendering fails (e.g. no fonts on the host), the image endpoint redirects to the cover art so
  the preview still has a picture.

Why not the alternatives:
- *Vercel Edge Middleware / `@vercel/og`:* the project uses the legacy `builds` config with
  `@vercel/static-build`; adding functions means new build entries and a runtime that can't be
  exercised locally without the Vercel CLI and account. The backend already has the data.
- *Prerendering every page:* the countdown goes stale immediately.

Base URLs are `@Value` settings with production defaults:
`app.share.site-url` (default `https://animelivecountdown.com`) and `app.share.api-url`
(default `https://api.animelivecountdown.com`).

### Share button
`ShareService.share(id, title)` uses the Web Share API when available, otherwise copies
`<origin>/media/<id>` to the clipboard and shows a snackbar. Used by a `app-share-button` on the
media page and a single "Share" row in the show card's info tooltip (the card is a shared file, so
that's the only change there).

## 2. Episode reactions

### Data model — new table `episode_reaction` (additive)
| column | type | notes |
|---|---|---|
| id | bigserial PK | |
| user_id | FK → app_user | |
| media_id | bigint | AniList id; no FK so the schedule sync can't be blocked by it |
| episode | int | ≥ 1 |
| reaction | varchar(16) | enum name |
| created_at / updated_at | timestamptz | `updated_at` drives "recent" for Most Hyped |

Unique `(user_id, media_id, episode)` → one reaction per user per episode. Indexes on
`(media_id, episode)` and `updated_at`.

Fixed set (`ReactionType`): `FIRE 🔥`, `LOVE 😍`, `LOL 😂`, `SHOCK 😱`, `SAD 😭`, `MEH 😐`.

### Rules
- Only signed-in users can react; counts are public.
- You can react only to an episode that has aired: `1 ≤ episode ≤ latest aired`, where latest aired
  is `nextEpisode - 1` while a show is releasing, or `totalEpisodes` once finished (from
  `media_info`, falling back to `media`).
- Reacting again replaces your reaction; reacting with the same emoji is a no-op; DELETE removes it.

### Endpoints
- `GET /api/reactions/{mediaId}` — summary for the latest aired episode:
  `{ mediaId, episode, counts: { FIRE: 3, … }, total, mine }` (`episode`/`mine` may be null).
  `mine` is filled when the request carries a valid access token.
- `GET /api/reactions/{mediaId}/{episode}` — same for a specific episode.
- `PUT /api/auth/reactions/{mediaId}/{episode}` body `{ "reaction": "FIRE" }` → updated summary.
- `DELETE /api/auth/reactions/{mediaId}/{episode}` → updated summary.

Errors: 400 unknown reaction / episode not aired, 404 unknown show, 401 not signed in.

### UI
`app-episode-reactions` on the media page (under the countdown): "Episode N — how was it?" with the
six emoji buttons and counts; your pick is highlighted. Signed-out visitors see counts and a
"Sign in to react" hint. Hidden when no episode has aired.

## 3. Most Hyped this week

`GET /api/hype?limit=10` (public, cached in memory for 5 minutes):
```
score = 3 × reactions in the last 7 days
      + 2 × new followers in the last 7 days
      + 1 × total followers (favmedia)
```
Only shows currently `RELEASING` in `media` are ranked; score 0 is dropped; ties break on
AniList popularity. Response: `[{ media: <Media>, score, recentReactions, newFollowers, followers }]`.
The ranking is a pure function (`HypeRanker`) fed by three grouped count queries.

Home gets an `app-most-hyped` section (show cards + a "🔥 12 reactions · 40 followers" caption),
hidden when the list is empty.

## Out of scope
- Comments, per-user reaction history, moderation.
- Reactions on episodes other than the latest from the UI (the API supports any aired episode).
- Real-time count updates (counts refresh on page load / after your own reaction).
- Pre-rendering share cards for search engines.
