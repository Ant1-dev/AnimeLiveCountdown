import {
  Component,
  DestroyRef,
  effect,
  inject,
  OnInit,
  signal,
  Injector,
  ElementRef,
  ChangeDetectionStrategy,
} from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs';
import { SearchService } from '../../services/search.service';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ResultComponent } from './result/result.component';
import { MediaInfo } from '../../models/media.info.model';

@Component({
  selector: 'app-searchbar',
  imports: [ReactiveFormsModule, CommonModule, ResultComponent, MatIconModule],
  templateUrl: './searchbar.component.html',
  styleUrl: './searchbar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:click)': 'clickOutside($event)'
  }
})
export class SearchbarComponent implements OnInit {
  searchResults = signal<MediaInfo[]>([]);
  showResults = signal<boolean>(true);
  searchControl = new FormControl('');
  searchTerm = signal<string>('');
  searchService = inject(SearchService);
  destroyRef = inject(DestroyRef);
  injector = inject(Injector);
  private searchTerm$ = toObservable(this.searchTerm);
  private elementRef = inject(ElementRef);

  constructor() {
    effect(() => {
      this.searchTerm$
        .pipe(
          switchMap((term) => this.searchService.search(term)),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe((results) => {
          this.searchResults.set(results.slice(0, 5));
          this.showResults.set(true);
        });
    });
  }

  ngOnInit(): void {
    const subscription1 = this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        tap((newValue) => this.searchTerm.set(newValue || ''))
      )
      .subscribe();

    this.destroyRef.onDestroy(() => {
      subscription1.unsubscribe();
    });
  }

  clickOutside(event: Event): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.showResults.set(false);
    } else {
      if (this.searchResults().length > 0) {
        this.showResults.set(true);
      }
    }
  }

  onSearchInputClick(): void {
    if (this.searchResults().length > 0) {
      this.showResults.set(true);
    }
  }
}
