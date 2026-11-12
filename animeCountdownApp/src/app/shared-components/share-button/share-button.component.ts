import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { ShareService } from '../../services/share.service';

@Component({
  selector: 'app-share-button',
  imports: [MatIcon],
  template: `
    <button type="button" class="share-button" (click)="share($event)" [attr.aria-label]="'Share ' + (title() || 'this show')">
      <mat-icon class="share-icon" aria-hidden="true">share</mat-icon>
      <span>Share</span>
    </button>
  `,
  styles: `
    .share-button {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      min-height: 40px;
      padding: 0 16px;
      border: 1px solid rgba(0, 166, 251, 0.5);
      border-radius: 20px;
      background: rgba(0, 166, 251, 0.12);
      color: #fff;
      font: inherit;
      font-weight: 500;
      cursor: pointer;
      transition: background-color 150ms ease, border-color 150ms ease;
    }
    .share-button:hover {
      background: rgba(0, 166, 251, 0.25);
      border-color: var(--accent, #00a6fb);
    }
    .share-button:focus-visible {
      outline: 2px solid var(--accent, #00a6fb);
      outline-offset: 2px;
    }
    .share-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShareButtonComponent {
  mediaId = input.required<number>();
  title = input<string | undefined>();

  private shareService = inject(ShareService);

  share(event: Event): void {
    // Cards wrap their content in a link; don't navigate when sharing
    event.preventDefault();
    event.stopPropagation();
    this.shareService.shareMedia(this.mediaId(), this.title());
  }
}
