import { ChangeDetectionStrategy, Component, PLATFORM_ID, effect, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { AiredEvent, MediaTimeService } from '../../services/media-time.service';

const VISIBLE_MS = 9000;
const CONFETTI_COLORS = ['#00a6fb', '#00fbdc', '#f44370', '#ffd166', '#6a55fa'];

interface Piece {
  left: number;
  delay: number;
  duration: number;
  color: string;
  rotate: number;
}

/**
 * A small toast (plus confetti, unless the visitor prefers reduced motion) when a countdown on
 * the page reaches zero. Listens to MediaTimeService.justAired.
 */
@Component({
  selector: 'app-airing-celebration',
  imports: [RouterLink, MatIcon],
  templateUrl: './airing-celebration.component.html',
  styleUrl: './airing-celebration.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiringCelebrationComponent {
  private mediaTime = inject(MediaTimeService);
  private isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private timers = new Map<number, ReturnType<typeof setTimeout>>();

  protected events = this.mediaTime.justAired;
  protected confetti = signal<Piece[]>([]);
  private confettiTimer?: ReturnType<typeof setTimeout>;
  private seen = new Set<string>();

  constructor() {
    effect(() => {
      for (const event of this.events()) {
        const key = `${event.mediaId}-${event.at}`;
        if (this.seen.has(key)) continue;
        this.seen.add(key);
        this.celebrate(event);
      }
    });
  }

  private celebrate(event: AiredEvent): void {
    if (!this.isBrowser) return;
    clearTimeout(this.timers.get(event.mediaId));
    this.timers.set(event.mediaId, setTimeout(() => this.dismiss(event.mediaId), VISIBLE_MS));
    if (!this.prefersReducedMotion()) this.burst();
  }

  private burst(): void {
    this.confetti.set(Array.from({ length: 36 }, (_, i) => ({
      left: Math.random() * 100,
      delay: Math.random() * 0.4,
      duration: 1.8 + Math.random() * 1.2,
      color: CONFETTI_COLORS[i % CONFETTI_COLORS.length],
      rotate: Math.random() * 360,
    })));
    clearTimeout(this.confettiTimer);
    this.confettiTimer = setTimeout(() => this.confetti.set([]), 3500);
  }

  private prefersReducedMotion(): boolean {
    try {
      return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    } catch {
      return false;
    }
  }

  protected dismiss(mediaId: number): void {
    clearTimeout(this.timers.get(mediaId));
    this.timers.delete(mediaId);
    this.mediaTime.dismissAired(mediaId);
  }
}
