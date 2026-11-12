import { Pipe, PipeTransform } from '@angular/core';
import { animePath, preferredTitle } from './slug';

/** Anything that identifies a show: the schedule, info and season models all name titles differently. */
export interface ShowRef {
  id: number;
  engtitle?: string | null;
  romtitle?: string | null;
  title_English?: string | null;
  title_Romaji?: string | null;
  title?: string | { english?: string | null; romaji?: string | null } | null;
}

/**
 * Canonical link to a show page.
 *   [routerLink]="show | animePath"            show is any show model
 *   [routerLink]="episode.mediaId | animePath: episode.title"
 */
@Pipe({ name: 'animePath' })
export class AnimePathPipe implements PipeTransform {
  transform(show: ShowRef | number | null | undefined, title?: string | null): string {
    if (show == null) return '/';
    if (typeof show === 'number') return animePath(show, title);
    const nested = typeof show.title === 'object' && show.title
      ? preferredTitle(show.title.english, show.title.romaji)
      : (typeof show.title === 'string' ? show.title : '');
    const named = title || preferredTitle(show.engtitle ?? show.title_English, show.romtitle ?? show.title_Romaji) || nested;
    return animePath(show.id, named);
  }
}
