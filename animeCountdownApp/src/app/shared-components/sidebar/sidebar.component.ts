import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ScheduleService } from '../../services/schedule.service';
import { Media } from '../../models/schedule.model';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeStyle } from '@angular/platform-browser';

@Component({
  selector: 'app-sidebar',
  imports: [CommonModule, RouterLink],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
})
export class SidebarComponent implements OnInit {
  private scheduleService = inject(ScheduleService);
  private destroyRef = inject(DestroyRef);
  private sanitizer = inject(DomSanitizer);
  media = signal<Media[]>([]);
  error = signal<string>('');

  getBackgroundImage(banner: string | undefined): SafeStyle {
    const imageUrl = banner || '/images/defaultBanner.png';
    return this.sanitizer.bypassSecurityTrustStyle(`url("${imageUrl}")`);
  }

  ngOnInit(): void {
    const subscription = this.scheduleService
      .renderMedia('trending', this.error())
      .subscribe({
        next: (media) => {
          if (media) {
            this.media.set(media);
          }
        },
        error: (error) => {
          this.error.set(error);
          console.log(error);
        },
      });

    this.destroyRef.onDestroy(() => {
      subscription.unsubscribe();
    });
  }

  concat(mediaName: string): string {
    if (mediaName.length >= 25) {
      mediaName = mediaName.slice(0, 21);
      mediaName += "...";
    }
    return mediaName
  }

}
