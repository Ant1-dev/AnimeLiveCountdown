import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { inject } from '@angular/core';

/**
 * Trailer "facade": a thumbnail with a play button until the visitor asks for the video, then the
 * player (youtube-nocookie / Dailymotion). Loading an embedded player up front costs ~1 MB of
 * JavaScript and sets third-party cookies, so it only happens on click.
 */
@Component({
  selector: 'app-trailer',
  template: `
    @if (embedUrl(); as url) {
      <section class="trailer" aria-labelledby="trailer-heading">
        <h2 id="trailer-heading" class="info-heading">Trailer</h2>
        <div class="frame">
          @if (playing()) {
            <iframe
              [src]="url"
              [title]="title() + ' trailer'"
              allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
              allowfullscreen
              referrerpolicy="strict-origin-when-cross-origin"></iframe>
          } @else {
            <button type="button" class="facade" (click)="playing.set(true)" [attr.aria-label]="'Play the ' + title() + ' trailer'">
              @if (thumbnail()) {
                <img [src]="thumbnail()" alt="" loading="lazy" decoding="async" />
              }
              <span class="play" aria-hidden="true"></span>
            </button>
          }
        </div>
      </section>
    }
  `,
  styles: `
    .trailer { margin: 1.5rem 0; }
    .frame { position: relative; width: 100%; max-width: 720px; aspect-ratio: 16 / 9; border-radius: 12px; overflow: hidden; background: #000; }
    iframe, .facade { position: absolute; inset: 0; width: 100%; height: 100%; border: 0; }
    .facade { padding: 0; cursor: pointer; background: #000; }
    .facade img { width: 100%; height: 100%; object-fit: cover; opacity: 0.85; transition: opacity 150ms ease; }
    .facade:hover img, .facade:focus-visible img { opacity: 1; }
    .facade:focus-visible { outline: 3px solid var(--accent); outline-offset: -3px; }
    .play { position: absolute; left: 50%; top: 50%; width: 68px; height: 48px; transform: translate(-50%, -50%); border-radius: 12px; background: rgba(0, 0, 0, 0.75); }
    .play::after { content: ''; position: absolute; left: 27px; top: 14px; border-style: solid; border-width: 10px 0 10px 17px; border-color: transparent transparent transparent #fff; }
    .facade:hover .play { background: var(--accent); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrailerComponent {
  private sanitizer = inject(DomSanitizer);

  site = input<string | undefined>();
  videoId = input<string | undefined>();
  thumbnailUrl = input<string | undefined>();
  title = input('');

  protected playing = signal(false);

  /** Only known video hosts with a plain id are embedded. */
  protected embedUrl = computed<SafeResourceUrl | null>(() => {
    const id = this.videoId();
    if (!id || !/^[A-Za-z0-9_-]{4,32}$/.test(id)) return null;
    switch (this.site()) {
      case 'youtube':
        return this.sanitizer.bypassSecurityTrustResourceUrl(`https://www.youtube-nocookie.com/embed/${id}?autoplay=1&rel=0`);
      case 'dailymotion':
        return this.sanitizer.bypassSecurityTrustResourceUrl(`https://www.dailymotion.com/embed/video/${id}?autoplay=1`);
      default:
        return null;
    }
  });

  protected thumbnail = computed(() => {
    const id = this.videoId();
    if (this.site() === 'youtube' && id) return `https://i.ytimg.com/vi/${id}/hqdefault.jpg`;
    return this.thumbnailUrl() || '';
  });
}
