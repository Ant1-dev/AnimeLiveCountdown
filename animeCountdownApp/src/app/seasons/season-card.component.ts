import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgOptimizedImage, NgTemplateOutlet } from '@angular/common';
import { SeasonMedia } from '../models/season.model';
import { timeUntil } from '../services/season.service';

@Component({
  selector: 'app-season-card',
  imports: [RouterLink, NgOptimizedImage, NgTemplateOutlet],
  template: `
    @let m = media();
    <ng-template #body>
      <div class="cover">
        @if (m.coverimage) {
          <img [ngSrc]="m.coverimage" [alt]="title()" width="230" height="325" [priority]="priority()" />
        }
        @if (m.avgscore) {
          <span class="score" [attr.aria-label]="'Score ' + m.avgscore + ' percent'">★ {{ m.avgscore }}%</span>
        }
        @if (airingChip(); as chip) {
          <span class="chip" [class.soon]="chip.soon">{{ chip.text }}</span>
        }
      </div>
      <div class="info">
        <h3 class="title" [title]="title()">{{ title() }}</h3>
        <p class="meta">{{ meta() }}</p>
        @if (m.genres.length) {
          <p class="genres">{{ m.genres.slice(0, 3).join(' · ') }}</p>
        }
      </div>
    </ng-template>

    @if (external()) {
      <a class="card" [href]="'https://anilist.co/anime/' + m.id" target="_blank" rel="noopener noreferrer">
        <ng-container *ngTemplateOutlet="body" />
      </a>
    } @else {
      <a class="card" [routerLink]="['/media', m.id]">
        <ng-container *ngTemplateOutlet="body" />
      </a>
    }
  `,
  styleUrl: './season-card.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeasonCardComponent {
  media = input.required<SeasonMedia>();
  /** Past-season shows aren't in our database, so they link to AniList instead. */
  external = input(false);
  now = input(Date.now());
  priority = input(false);

  title = computed(() => this.media().engtitle || this.media().romtitle || 'Untitled');

  meta = computed(() => {
    const m = this.media();
    const parts: string[] = [];
    if (m.format) parts.push(m.format.replace('_', ' '));
    if (m.totalepisodes) parts.push(`${m.totalepisodes} ep${m.totalepisodes === 1 ? '' : 's'}`);
    parts.push(statusLabel(m.status));
    return parts.join(' · ');
  });

  airingChip = computed<{ text: string; soon: boolean } | null>(() => {
    const m = this.media();
    if (!m.airingat) return null;
    const at = new Date(m.airingat).getTime();
    const now = this.now();
    if (at <= now) return null;
    const soon = at - now < 24 * 3600 * 1000;
    if (m.status === 'NOT_YET_RELEASED' || m.nextepisode === 1) {
      return { text: `Premieres ${timeUntil(at, now)}`, soon };
    }
    return { text: `Ep ${m.nextepisode} ${timeUntil(at, now)}`, soon };
  });
}

function statusLabel(status?: string): string {
  switch (status) {
    case 'RELEASING': return 'Airing';
    case 'NOT_YET_RELEASED': return 'Upcoming';
    case 'FINISHED': return 'Finished';
    case 'HIATUS': return 'On hiatus';
    case 'CANCELLED': return 'Cancelled';
    default: return status ?? '';
  }
}
