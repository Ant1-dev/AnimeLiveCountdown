import { ChangeDetectionStrategy, Component, ElementRef, inject, LOCALE_ID, signal } from '@angular/core';
import { formatDate } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from '../../auth/auth.service';
import { NotificationService } from '../../services/notifications/notification.service';
import { PushService } from '../../services/notifications/push.service';
import { PwaInstallService } from '../../services/notifications/pwa-install.service';
import { AppNotification, isDelayKind } from '../../models/notification.model';
import { SignInComponent } from '../sign-in/sign-in.component';

@Component({
  selector: 'app-notification-bell',
  imports: [MatIcon, RouterLink],
  templateUrl: './notification-bell.component.html',
  styleUrl: './notification-bell.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:click)': 'closeOnOutsideClick($event)',
    '(document:keydown.escape)': 'open.set(false)',
  },
})
export class NotificationBellComponent {
  private host = inject(ElementRef<HTMLElement>);
  private router = inject(Router);
  private dialog = inject(MatDialog);
  private locale = inject(LOCALE_ID);
  protected notifications = inject(NotificationService);
  protected install = inject(PwaInstallService);
  protected user = inject(AuthService).user;

  protected open = signal(false);

  constructor() {
    // Registers the service worker on every page so the site is installable and push keeps working
    inject(PushService);
  }

  toggle(): void {
    this.open.update((open) => !open);
    if (this.open() && this.user()) this.notifications.refresh();
  }

  openItem(notification: AppNotification): void {
    this.notifications.markRead(notification);
    this.open.set(false);
    if (notification.mediaId) this.router.navigate(['/media', notification.mediaId]);
  }

  isDelay(notification: AppNotification): boolean {
    return isDelayKind(notification.kind);
  }

  /** Delay alerts carry the new date in airingAt; show it in the viewer's time zone. */
  messageOf(notification: AppNotification): string {
    if (!isDelayKind(notification.kind) || !notification.airingAt) return notification.message;
    const when = formatDate(notification.airingAt, 'EEE, MMM d, h:mm a', this.locale);
    if (notification.message.startsWith('On break')) {
      return `On break until ${when}` + (notification.episode ? ` (episode ${notification.episode})` : '');
    }
    const cut = notification.message.indexOf(' delayed to ');
    return cut >= 0 ? `${notification.message.slice(0, cut)} delayed to ${when}` : notification.message;
  }

  signIn(): void {
    this.open.set(false);
    this.dialog.open(SignInComponent, { panelClass: 'sign-in-panel' });
  }

  installApp(): void {
    this.install.install();
  }

  closeOnOutsideClick(event: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) this.open.set(false);
  }

  timeAgo(iso: string): string {
    const seconds = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000);
    if (seconds < 60) return 'just now';
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes} min ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours} h ago`;
    const days = Math.floor(hours / 24);
    return days === 1 ? 'yesterday' : `${days} days ago`;
  }
}
