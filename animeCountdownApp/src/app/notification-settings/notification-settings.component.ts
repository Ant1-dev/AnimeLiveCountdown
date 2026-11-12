import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { LEAD_TIME_OPTIONS, NotificationConfig, NotificationSettings, Reminder } from '../models/notification.model';
import { NotificationService } from '../services/notifications/notification.service';
import { PushService } from '../services/notifications/push.service';
import { PwaInstallService } from '../services/notifications/pwa-install.service';
import { ReminderService } from '../services/notifications/reminder.service';

/** Settings page: channels (browser push, Discord), calendar feed, installing, and all reminders. */
@Component({
  selector: 'app-notification-settings',
  imports: [FormsModule, RouterLink, DatePipe],
  templateUrl: './notification-settings.component.html',
  styleUrl: './notification-settings.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationSettingsComponent implements OnInit {
  private reminderService = inject(ReminderService);
  private notifications = inject(NotificationService);
  private snackBar = inject(MatSnackBar);
  protected push = inject(PushService);
  protected install = inject(PwaInstallService);

  protected readonly leadOptions = LEAD_TIME_OPTIONS;
  protected config = signal<NotificationConfig | null>(null);
  protected settings = signal<NotificationSettings | null>(null);
  protected reminders = signal<Reminder[]>([]);
  protected loading = signal(true);
  protected loadError = signal(false);
  protected busy = signal<string | null>(null);

  protected webhookInput = signal('');
  protected webhookError = signal('');

  async ngOnInit(): Promise<void> {
    try {
      const [config, settings, reminders] = await Promise.all([
        firstValueFrom(this.notifications.config()),
        firstValueFrom(this.reminderService.settings()),
        firstValueFrom(this.reminderService.list()),
      ]);
      this.config.set(config);
      this.settings.set(settings);
      this.reminders.set(reminders);
      await this.push.refreshState();
    } catch {
      this.loadError.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  // ---- Browser push ----

  async enablePush(): Promise<void> {
    await this.run('push', async () => {
      const state = await this.push.enable();
      if (state === 'denied') this.toast('Notifications are blocked. Allow them in your browser\'s site settings.');
      else if (state === 'on') this.toast('Browser notifications are on for this device.');
    });
  }

  async disablePush(): Promise<void> {
    await this.run('push', async () => {
      await this.push.disable();
      this.toast('Browser notifications are off for this device.');
    });
  }

  async testPush(): Promise<void> {
    await this.run('push-test', async () => {
      const { sent } = await this.push.sendTest();
      this.toast(sent > 0 ? `Test sent to ${sent} device${sent === 1 ? '' : 's'}.` : 'No device received it. Turn notifications off and on again.');
    });
  }

  // ---- Discord ----

  async saveWebhook(): Promise<void> {
    this.webhookError.set('');
    await this.run('discord', async () => {
      try {
        const result = await firstValueFrom(this.reminderService.setDiscordWebhook(this.webhookInput().trim()));
        this.settings.update((s) => s && { ...s, discordWebhookSet: true, discordWebhookHint: result.discordWebhookHint });
        this.webhookInput.set('');
        this.toast('Discord webhook saved. Turn on Discord for each reminder you want there.');
      } catch (error) {
        this.webhookError.set(errorMessage(error, "Couldn't save the webhook."));
      }
    });
  }

  async removeWebhook(): Promise<void> {
    await this.run('discord', async () => {
      await firstValueFrom(this.reminderService.removeDiscordWebhook());
      this.settings.update((s) => s && { ...s, discordWebhookSet: false, discordWebhookHint: null });
      this.toast('Discord webhook removed.');
    });
  }

  async testDiscord(): Promise<void> {
    await this.run('discord-test', async () => {
      try {
        await firstValueFrom(this.reminderService.testDiscord());
        this.toast('Test message sent to Discord.');
      } catch (error) {
        if ((error as HttpErrorResponse).status === 410) {
          this.settings.update((s) => s && { ...s, discordWebhookSet: false, discordWebhookHint: null });
        }
        this.toast(errorMessage(error, "Discord didn't accept the message."));
      }
    });
  }

  // ---- Calendar ----

  async createCalendar(): Promise<void> {
    await this.run('calendar', async () => {
      const urls = await firstValueFrom(this.reminderService.regenerateCalendar());
      const hadOne = !!this.settings()?.calendarUrl;
      this.settings.update((s) => s && { ...s, ...urls });
      this.toast(hadOne ? 'New calendar link created. The old link no longer works.' : 'Calendar link created.');
    });
  }

  async disableCalendar(): Promise<void> {
    await this.run('calendar', async () => {
      await firstValueFrom(this.reminderService.disableCalendar());
      this.settings.update((s) => s && { ...s, calendarUrl: null, calendarWebcalUrl: null });
      this.toast('Calendar link turned off.');
    });
  }

  async copyCalendarUrl(): Promise<void> {
    const url = this.settings()?.calendarUrl;
    if (!url) return;
    try {
      await navigator.clipboard.writeText(url);
      this.toast('Calendar link copied.');
    } catch {
      this.toast('Copy failed. Select the link and copy it manually.');
    }
  }

  // ---- Reminders ----

  async updateReminder(reminder: Reminder, change: Partial<Pick<Reminder, 'leadMinutes' | 'push' | 'discord'>>): Promise<void> {
    const next = { ...reminder, ...change };
    this.reminders.update((list) => list.map((r) => (r.mediaId === reminder.mediaId ? next : r)));
    try {
      if (change.push && this.push.state() !== 'on' && this.config()?.pushEnabled) await this.push.enable();
      await firstValueFrom(this.reminderService.save(reminder.mediaId, {
        leadMinutes: Number(next.leadMinutes), push: next.push, discord: next.discord,
      }));
    } catch {
      this.reminders.update((list) => list.map((r) => (r.mediaId === reminder.mediaId ? reminder : r)));
      this.toast("Couldn't update the reminder. Try again in a moment.");
    }
  }

  async removeReminder(reminder: Reminder): Promise<void> {
    await this.run('reminder-' + reminder.mediaId, async () => {
      await firstValueFrom(this.reminderService.remove(reminder.mediaId));
      this.reminders.update((list) => list.filter((r) => r.mediaId !== reminder.mediaId));
      this.toast(`Reminder for ${reminder.title ?? 'this show'} removed.`);
    });
  }

  private async run(key: string, action: () => Promise<void>): Promise<void> {
    this.busy.set(key);
    try {
      await action();
    } catch {
      this.toast('Something went wrong. Try again in a moment.');
    } finally {
      this.busy.set(null);
    }
  }

  private toast(message: string): void {
    this.snackBar.open(message, 'Close', { duration: 4000 });
  }
}

function errorMessage(error: unknown, fallback: string): string {
  const body = (error as HttpErrorResponse)?.error;
  return typeof body?.error === 'string' ? body.error : fallback;
}
