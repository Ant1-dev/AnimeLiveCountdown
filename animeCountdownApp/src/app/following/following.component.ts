import { Component, inject, computed, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FollowingService } from '../services/following.service';
import { ShowComponent } from '../home/weekly-schedule/shows/show/show.component';
import { MediaSkeletonComponent } from '../home/shared-home/media-skeleton/media-skeleton.component';
import { ContinueWatchingComponent } from '../shared-components/continue-watching/continue-watching.component';
import { AniListPanelComponent } from './anilist-panel/anilist-panel.component';
import { SeoService } from '../seo/seo.service';

type RenderPriority = 'high' | 'medium' | 'low';

/** The shows the signed-in user follows (formerly "Favorites"). */
@Component({
  selector: 'app-following',
  imports: [ShowComponent, MediaSkeletonComponent, ContinueWatchingComponent, AniListPanelComponent, RouterLink],
  templateUrl: './following.component.html',
  styleUrl: './following.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FollowingComponent {
  // Signed-in pages: keep them out of search results
  private readonly _seo = inject(SeoService).setPage({
    title: 'Following',
    description: 'Your personal Anime Live Countdown page.',
    path: '/following',
    noindex: true,
  });

  protected following = inject(FollowingService);

  // Next to air first; finished and unscheduled shows last
  protected media = computed(() =>
    this.following
      .follows()
      .map((f) => f.media)
      .sort((a, b) => time(a.next_Airing_At) - time(b.next_Airing_At))
  );

  getShowPriority(index: number): RenderPriority {
    if (index < 8) {
      return 'high';
    } else if (index < 16) {
      return 'medium';
    }
    return 'low';
  }
}

function time(value: Date | string | null | undefined): number {
  return value ? new Date(value).getTime() : Number.MAX_SAFE_INTEGER;
}
