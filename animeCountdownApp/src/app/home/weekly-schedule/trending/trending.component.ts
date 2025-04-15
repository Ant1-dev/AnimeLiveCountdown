import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SkeletonModule } from 'primeng/skeleton';
import { HeroCarouselComponent } from './hero-carousel/hero-carousel.component';
import { MediaInfo } from '../../../models/media.info.model';
import { MediaInfoService } from '../../../services/media.info.service';
import { SeasonPopComponent } from "./season-pop/season-pop.component";

@Component({
  selector: 'app-trending',
  imports: [CommonModule, SkeletonModule, HeroCarouselComponent, SeasonPopComponent],
  templateUrl: './trending.component.html',
  styleUrl: './trending.component.css',
})
export class TrendingComponent {

}
