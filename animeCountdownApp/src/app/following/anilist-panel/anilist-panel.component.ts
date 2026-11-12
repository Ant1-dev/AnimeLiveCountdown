import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AniListService } from '../../services/anilist.service';
import { AniListImportResult, AniListStatus } from '../../models/follow.model';

/**
 * Connect AniList / Re-sync / Disconnect on the Following page. Hidden entirely when the server has
 * no AniList credentials. After AniList redirects back (?anilist=connected) it runs the first import.
 */
@Component({
  selector: 'app-anilist-panel',
  imports: [DatePipe],
  templateUrl: './anilist-panel.component.html',
  styleUrl: './anilist-panel.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AniListPanelComponent implements OnInit {
  private aniList = inject(AniListService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  protected status = signal<AniListStatus | null>(null);
  protected busy = signal<'connect' | 'import' | 'disconnect' | null>(null);
  protected result = signal<AniListImportResult | null>(null);
  protected message = signal<string>('');
  protected isError = signal(false);

  ngOnInit(): void {
    const outcome = this.route.snapshot.queryParamMap.get('anilist');
    if (outcome) {
      // Drop ?anilist= so a reload doesn't import again
      this.router.navigate([], { queryParams: { anilist: null }, queryParamsHandling: 'merge', replaceUrl: true });
    }

    this.aniList
      .status()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (status) => {
          this.status.set(status);
          if (outcome === 'connected' && status.connected) {
            this.runImport();
          } else if (outcome === 'denied') {
            this.show('AniList connection was cancelled. Nothing was imported.', true);
          } else if (outcome === 'error') {
            this.show("Couldn't connect to AniList. Try again.", true);
          }
        },
        error: () => this.status.set(null),
      });
  }

  connect(): void {
    this.busy.set('connect');
    this.aniList.connect().subscribe({
      error: () => {
        this.busy.set(null);
        this.show("Couldn't start the AniList connection. Try again.", true);
      },
    });
  }

  runImport(): void {
    this.busy.set('import');
    this.result.set(null);
    this.message.set('');
    this.aniList.importList().subscribe({
      next: (result) => {
        this.result.set(result);
        this.status.update((s) => (s ? { ...s, lastSyncedAt: result.syncedAt } : s));
        this.busy.set(null);
      },
      error: (error: HttpErrorResponse) => {
        this.busy.set(null);
        if (error.status === 409 && error.error?.reason === 'TOKEN_INVALID') {
          this.status.update((s) => (s ? { ...s, connected: false } : s));
        }
        this.show(error.error?.error || "Couldn't import from AniList. Try again in a minute.", true);
      },
    });
  }

  disconnect(): void {
    this.busy.set('disconnect');
    this.aniList.disconnect().subscribe({
      next: () => {
        this.status.update((s) => (s ? { ...s, connected: false, userName: null, lastSyncedAt: null } : s));
        this.result.set(null);
        this.busy.set(null);
        this.show('AniList disconnected. Your followed shows and progress stay here.', false);
      },
      error: () => {
        this.busy.set(null);
        this.show("Couldn't disconnect AniList. Try again.", true);
      },
    });
  }

  private show(message: string, isError: boolean): void {
    this.message.set(message);
    this.isError.set(isError);
  }
}
