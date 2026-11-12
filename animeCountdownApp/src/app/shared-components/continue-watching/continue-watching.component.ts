import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FollowingService } from '../../services/following.service';
import { MediaTimeService } from '../../services/media-time.service';
import { Follow } from '../../models/follow.model';
import { AnimePathPipe } from '../../seo/anime-path.pipe';

/** "Continue watching": followed shows with aired episodes the user hasn't marked watched. */
@Component({
  selector: 'app-continue-watching',
  imports: [AnimePathPipe, RouterLink],
  templateUrl: './continue-watching.component.html',
  styleUrl: './continue-watching.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContinueWatchingComponent {
  protected following = inject(FollowingService);
  private mediaTime = inject(MediaTimeService);
  private snackBar = inject(MatSnackBar);

  // Media IDs with a progress update in flight, so a double click doesn't skip an episode
  protected saving = signal<ReadonlySet<number>>(new Set());

  title(item: Follow): string {
    return this.mediaTime.getDisplayTitle(item.media);
  }

  markWatched(item: Follow): void {
    const id = item.media.id;
    if (this.saving().has(id)) return;
    this.saving.set(new Set([...this.saving(), id]));
    this.following.markNextWatched(id).subscribe({
      error: () => {
        this.done(id);
        this.snackBar.open("Couldn't save your progress. Try again.", 'Close', { duration: 3000 });
      },
      complete: () => this.done(id),
    });
  }

  private done(id: number): void {
    const next = new Set(this.saving());
    next.delete(id);
    this.saving.set(next);
  }
}
