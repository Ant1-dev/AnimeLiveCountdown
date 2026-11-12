// show.component.ts
import { CommonModule, NgOptimizedImage } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  OnInit,
  OnChanges,
  SimpleChanges,
  computed,
} from '@angular/core';
import { Media } from '../../../../models/schedule.model';
import { MediaInfoService } from '../../../../services/media.info.service';
import { MediaTimeService } from '../../../../services/media-time.service';
import { SkeletonModule } from 'primeng/skeleton';
import { TimeRemaining } from '../../../../models/time-remaining.model';
import { MatIcon } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TooltipModule, Tooltip } from 'primeng/tooltip';
import { FollowingService } from '../../../../services/following.service';
import { AuthService } from '../../../../auth/auth.service';
import { Observable } from 'rxjs';
import { ScheduleNoteBadgeComponent } from '../../../../shared-components/schedule-note-badge/schedule-note-badge.component';
import { ShareService } from '../../../../services/share.service';
import { ReminderDialogService } from '../../../../services/notifications/reminder-dialog.service';

@Component({
  selector: 'app-show',
  imports: [
    CommonModule,
    SkeletonModule,
    MatIcon,
    TooltipModule,
    Tooltip,
    NgOptimizedImage,
    ScheduleNoteBadgeComponent,
  ],
  templateUrl: './show.component.html',
  styleUrl: './show.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShowComponent implements OnInit, OnChanges {
  media = input<Media>();
  renderPriority = input<'high' | 'medium' | 'low'>('low');
  // If true, show "Airing Now" when countdown finishes. If false, hide card completely.
  showAiringNow = input<boolean>(false);

  private mediaInfoService = inject(MediaInfoService);
  private mediaTimeService = inject(MediaTimeService);
  private following = inject(FollowingService);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private shareService = inject(ShareService);
  private reminderDialog = inject(ReminderDialogService);

  protected user = this.authService.user;

  // The viewer's follow of this show (with episode progress), if they follow it
  protected follow = computed(() => this.following.get(this.media()?.id));

  timeRemaining = computed<TimeRemaining | null>(() => {
    const mediaData = this.media();
    if (!mediaData || !mediaData.id) return null;
    return this.mediaTimeService.getTimeRemaining(mediaData.id);
  });

  isAiringNow = computed(() => {
    return this.timeRemaining() === null && this.showAiringNow();
  });

  // Favorited shows that stopped airing are kept by the backend with status FINISHED
  isFinished = computed(() => this.media()?.status === 'FINISHED');

  getDisplayTitle(): string {
    return this.mediaTimeService.getDisplayTitle(this.media());
  }

  retrieveId(): void {
    const mediaData = this.media();
    if (mediaData) {
      this.mediaInfoService.getMediaId(mediaData.id);
    }
  }

  share(): void {
    const mediaData = this.media();
    if (mediaData) this.shareService.shareMedia(mediaData.id, this.getDisplayTitle());
  }

  toggleFollow(event: Event): void {
    // The card is a link; keep the click on the button
    event.preventDefault();
    event.stopPropagation();
    const media = this.media();
    if (!media) return;
    if (!this.user()) {
      this.notify('Sign in to follow shows');
      return;
    }
    const following = this.follow() !== undefined;
    const request: Observable<unknown> = following
      ? this.following.unfollow(media.id)
      : this.following.follow(media.id);
    request.subscribe({
      next: () => this.notify(following ? 'Unfollowed' : 'Following! It is now in My Schedule'),
      error: () => this.notify(following ? "Couldn't unfollow. Try again." : "Couldn't follow. Try again."),
    });
  }

  markWatched(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    const media = this.media();
    if (!media) return;
    this.following.markNextWatched(media.id).subscribe({
      error: () => this.notify("Couldn't save your progress. Try again."),
    });
  }

  private notify(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom',
    });
  }

  addReminder(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    const mediaData = this.media();
    if (!mediaData) return;
    this.reminderDialog.open({
      mediaId: mediaData.id,
      title: this.getDisplayTitle(),
      nextEpisode: mediaData.next_Airing_Episode,
    });
  }

  ngOnInit(): void {
    this.mediaTimeService.initializeTimer(this.media());
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Only reinitialize timer if media input actually changed
    if (changes['media'] && !changes['media'].firstChange) {
      const newMedia = changes['media'].currentValue;
      if (newMedia) {
        this.mediaTimeService.initializeTimer(newMedia);
      }
    }
  }

  tooltipOptions = {
    showDelay: 150,
    autoHide: false,
  };
}
