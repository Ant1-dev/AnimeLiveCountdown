import { inject, Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/**
 * Shares a show page. Uses the native share sheet (Web Share API) where the browser has one,
 * otherwise copies the link. Link previews for the shared URL come from the backend
 * (/api/share/media/:id) via the bot rewrite in vercel.json.
 */
@Injectable({ providedIn: 'root' })
export class ShareService {
  private snackBar = inject(MatSnackBar);

  async shareMedia(mediaId: number, title?: string): Promise<void> {
    if (typeof window === 'undefined') return;
    const url = `${window.location.origin}/media/${mediaId}`;
    const text = title ? `${title} — live countdown to the next episode` : 'Live countdown to the next episode';

    if (typeof navigator.share === 'function') {
      try {
        await navigator.share({ title: title ?? 'Anime Live Countdown', text, url });
        return;
      } catch (err) {
        // The user closed the share sheet: nothing to do
        if (err instanceof DOMException && err.name === 'AbortError') return;
        // Otherwise (e.g. not allowed in this context) fall back to copying
      }
    }

    if (await this.copy(url)) {
      this.notify('Link copied');
    } else {
      this.notify(`Copy this link: ${url}`, 8000);
    }
  }

  private async copy(text: string): Promise<boolean> {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {
      return false;
    }
  }

  private notify(message: string, duration = 3000): void {
    this.snackBar.open(message, 'Close', {
      duration,
      horizontalPosition: 'center',
      verticalPosition: 'bottom',
    });
  }
}
