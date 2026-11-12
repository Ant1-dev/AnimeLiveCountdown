import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { firstValueFrom } from 'rxjs';
import { LEAD_TIME_OPTIONS, NotificationConfig, Reminder } from '../../models/notification.model';
import { NotificationService } from '../../services/notifications/notification.service';
import { PushService } from '../../services/notifications/push.service';
import { ReminderService } from '../../services/notifications/reminder.service';
import { PwaInstallService } from '../../services/notifications/pwa-install.service';

export interface ReminderDialogData {
  mediaId: number;
  title: string;
  nextEpisode?: number | null;
}

/** Set, change or remove the reminder for one show. */
@Component({
  selector: 'app-reminder-dialog',
  imports: [MatDialogModule, FormsModule, RouterLink],
  templateUrl: './reminder-dialog.component.html',
  styleUrl: './reminder-dialog.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReminderDialogComponent implements OnInit {
  protected data = inject<ReminderDialogData>(MAT_DIALOG_DATA);
  private dialogRef = inject(MatDialogRef<ReminderDialogComponent>);
  private reminders = inject(ReminderService);
  private notifications = inject(NotificationService);
  private snackBar = inject(MatSnackBar);
  protected push = inject(PushService);
  protected install = inject(PwaInstallService);

  protected readonly leadOptions = LEAD_TIME_OPTIONS;
  protected loading = signal(true);
  protected saving = signal(false);
  protected error = signal('');
  protected config = signal<NotificationConfig | null>(null);
  protected existing = signal<Reminder | null>(null);
  protected discordWebhookSet = signal(false);

  protected leadMinutes = signal(0);
  protected pushChosen = signal(true);
  protected discordChosen = signal(false);

  protected pushAvailable = computed(() => !!this.config()?.pushEnabled && this.push.supported);
  protected pushBlocked = computed(() => this.push.state() === 'denied');

  async ngOnInit(): Promise<void> {
    try {
      const [config, existing] = await Promise.all([
        firstValueFrom(this.notifications.config()),
        firstValueFrom(this.reminders.get(this.data.mediaId)),
      ]);
      this.config.set(config);
      this.existing.set(existing);
      if (config.discordEnabled) {
        const settings = await firstValueFrom(this.reminders.settings());
        this.discordWebhookSet.set(settings.discordWebhookSet);
      }
      await this.push.refreshState();
      if (existing) {
        this.leadMinutes.set(existing.leadMinutes);
        this.pushChosen.set(existing.push);
        this.discordChosen.set(existing.discord && this.discordWebhookSet());
      } else {
        this.pushChosen.set(this.pushAvailable() && !this.pushBlocked());
      }
    } catch {
      this.error.set("Couldn't load your reminder settings. Try again in a moment.");
    } finally {
      this.loading.set(false);
    }
  }

  async save(): Promise<void> {
    this.saving.set(true);
    this.error.set('');
    let pushNote = '';
    try {
      if (this.pushChosen() && this.pushAvailable() && this.push.state() !== 'on') {
        const state = await this.push.enable().catch(() => 'off' as const);
        if (state !== 'on') {
          pushNote = state === 'denied'
            ? ' Browser notifications are blocked, so it will only show in the bell.'
            : ' Browser notifications are off, so it will only show in the bell.';
        }
      }
      await firstValueFrom(this.reminders.save(this.data.mediaId, {
        leadMinutes: Number(this.leadMinutes()),
        push: this.pushChosen(),
        discord: this.discordChosen(),
      }));
      this.snackBar.open(`Reminder set for ${this.data.title}.${pushNote}`, 'Close', { duration: pushNote ? 6000 : 3000 });
      this.dialogRef.close(true);
    } catch {
      this.error.set("Couldn't save the reminder. Try again in a moment.");
    } finally {
      this.saving.set(false);
    }
  }

  async remove(): Promise<void> {
    this.saving.set(true);
    try {
      await firstValueFrom(this.reminders.remove(this.data.mediaId));
      this.snackBar.open(`Reminder for ${this.data.title} removed. The show stays in your favorites.`, 'Close', { duration: 4000 });
      this.dialogRef.close(false);
    } catch {
      this.error.set("Couldn't remove the reminder. Try again in a moment.");
    } finally {
      this.saving.set(false);
    }
  }
}
