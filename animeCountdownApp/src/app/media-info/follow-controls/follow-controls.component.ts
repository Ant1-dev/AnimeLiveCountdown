import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../../auth/auth.service';
import { FollowingService } from '../../services/following.service';

/** Follow / Unfollow and episode progress on a show's page. Signed-in users only. */
@Component({
  selector: 'app-follow-controls',
  templateUrl: './follow-controls.component.html',
  styleUrl: './follow-controls.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FollowControlsComponent {
  mediaId = input.required<number>();

  private following = inject(FollowingService);
  protected user = inject(AuthService).user;

  protected follow = computed(() => this.following.get(this.mediaId()));
  protected busy = signal(false);
  protected error = signal('');

  protected canGoBack = computed(() => (this.follow()?.watchedEpisode ?? 0) > 0);
  protected canGoForward = computed(() => {
    const f = this.follow();
    return !!f && (f.latestAiredEpisode == null || f.watchedEpisode < f.latestAiredEpisode);
  });

  toggleFollow(): void {
    const id = this.mediaId();
    this.run(this.follow() ? this.following.unfollow(id) : this.following.follow(id), (e) =>
      e.status === 404 ? "This show can't be followed yet. Try again after it appears in the schedule." : null
    );
  }

  step(delta: number): void {
    const f = this.follow();
    if (!f) return;
    this.run(this.following.setProgress(this.mediaId(), f.watchedEpisode + delta));
  }

  private run(request: Observable<unknown>, explain?: (e: HttpErrorResponse) => string | null): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    request.subscribe({
      error: (e: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(explain?.(e) ?? "Couldn't save that. Try again.");
      },
      complete: () => this.busy.set(false),
    });
  }
}
