#!/usr/bin/env bash
# Image resizer for the frontend (served at img.animelivecountdown.com through Caddy).
# Resizes AniList covers and banners to WebP at a few fixed widths, instead of Vercel Image
# Optimization, whose free tier allows only 5,000 transformations a month.
#
# Safe to re-run: replaces the running container. Usage on the VM: bash ~/imgproxy.sh
#
# Only AniList images and only the presets below are accepted, so the server can't be used as an
# open image proxy. The widths must match IMAGE_WIDTHS in animeCountdownApp/src/app/shared/images.ts.
set -euo pipefail

IMAGE=ghcr.io/imgproxy/imgproxy:v4.0.24

preset() { echo "w$1=resize:fit:$1:0/quality:70/format:webp"; }
PRESETS="$(preset 256),$(preset 640),$(preset 1080),$(preset 1920)"

docker pull -q "$IMAGE"
docker rm -f imgproxy >/dev/null 2>&1 || true
docker run -d \
  --name imgproxy \
  --restart unless-stopped \
  -p 127.0.0.1:8081:8080 \
  -e IMGPROXY_ALLOWED_SOURCES="https://s4.anilist.co/" \
  -e IMGPROXY_ONLY_PRESETS=true \
  -e IMGPROXY_PRESETS="$PRESETS" \
  -e IMGPROXY_MAX_SRC_RESOLUTION=20 \
  -e IMGPROXY_DOWNLOAD_TIMEOUT=10 \
  -e IMGPROXY_TTL=31536000 \
  --memory 512m \
  "$IMAGE"

docker ps --filter name=imgproxy --format '{{.Names}} {{.Status}}'
