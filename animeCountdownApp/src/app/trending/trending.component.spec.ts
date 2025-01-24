import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TrendingComponent } from './trending.component';

describe('TrendingComponent', () => {
  let component: TrendingComponent;
  let fixture: ComponentFixture<TrendingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TrendingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TrendingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
