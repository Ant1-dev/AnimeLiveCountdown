import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, distinctUntilChanged, of, switchMap } from 'rxjs';
import { AuthService } from '../../auth/auth.service';
import { ReactionSummary, ReactionType } from '../../models/reaction.model';
import { ReactionService } from '../../services/reaction.service';

const ORDER: ReactionType[] = ['FIRE', 'LOVE', 'LOL', 'SHOCK', 'SAD', 'MEH'];
const LABELS: Record<ReactionType, string> = {
  FIRE: 'Fire',
  LOVE: 'Loved it',
  LOL: 'Funny',
  SHOCK: 'Shocking',
  SAD: 'Sad',
  MEH: 'Meh',
};

/** Reactions to the latest aired episode of a show. Counts are public; reacting needs sign-in. */
@Component({
  selector: 'app-episode-reactions',
  templateUrl: './episode-reactions.component.html',
  styleUrl: './episode-reactions.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EpisodeReactionsComponent {
  mediaId = input.required<number>();

  private reactionService = inject(ReactionService);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);

  protected summary = signal<ReactionSummary | null>(null);
  protected pending = signal(false);
  protected signedIn = this.authService.isAuthenticated;

  protected options = computed(() => {
    const s = this.summary();
    if (!s) return [];
    return ORDER.map((type) => ({
      type,
      emoji: s.emojis[type],
      label: LABELS[type],
      count: s.counts[type] ?? 0,
      mine: s.mine === type,
    }));
  });

  constructor() {
    // Reload when the show changes or the visitor signs in/out (the summary includes "mine")
    const key = computed(() => `${this.mediaId()}|${this.authService.user()?.id ?? ''}`);
    toObservable(key)
      .pipe(
        distinctUntilChanged(),
        switchMap(() => this.authService.whenSessionChecked()),
        switchMap(() => this.reactionService.latest(this.mediaId()).pipe(catchError(() => of(null)))),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((summary) => this.summary.set(summary));
  }

  toggle(type: ReactionType): void {
    const current = this.summary();
    if (!current?.episode || this.pending()) return;
    if (!this.signedIn()) {
      this.notify('Sign in to react to episodes');
      return;
    }

    this.pending.set(true);
    const request = current.mine === type
      ? this.reactionService.remove(current.mediaId, current.episode)
      : this.reactionService.react(current.mediaId, current.episode, type);
    request.subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.pending.set(false);
      },
      error: () => {
        this.pending.set(false);
        this.notify("Couldn't save your reaction. Try again.");
      },
    });
  }

  private notify(message: string): void {
    this.snackBar.open(message, 'Close', { duration: 3000, horizontalPosition: 'center', verticalPosition: 'bottom' });
  }
}
