import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HypeEntry } from '../../models/reaction.model';
import { ReactionService } from '../../services/reaction.service';
import { ShowComponent } from '../weekly-schedule/shows/show/show.component';

/** "Most hyped this week": airing shows ranked by recent reactions and followers. Hidden when empty. */
@Component({
  selector: 'app-most-hyped',
  imports: [ShowComponent],
  templateUrl: './most-hyped.component.html',
  styleUrl: './most-hyped.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MostHypedComponent implements OnInit {
  private reactionService = inject(ReactionService);
  private destroyRef = inject(DestroyRef);

  protected entries = signal<HypeEntry[]>([]);

  ngOnInit(): void {
    this.reactionService
      .mostHyped(10)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (entries) => this.entries.set(entries ?? []),
        error: () => this.entries.set([]),
      });
  }

  protected caption(entry: HypeEntry): string {
    const parts: string[] = [];
    if (entry.recentReactions > 0) {
      parts.push(`🔥 ${entry.recentReactions} ${entry.recentReactions === 1 ? 'reaction' : 'reactions'}`);
    }
    if (entry.followers > 0) {
      parts.push(`${entry.followers} ${entry.followers === 1 ? 'follower' : 'followers'}`);
    }
    return parts.join(' · ');
  }
}
