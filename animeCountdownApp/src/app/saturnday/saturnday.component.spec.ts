import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SaturndayComponent } from './saturnday.component';

describe('SaturndayComponent', () => {
  let component: SaturndayComponent;
  let fixture: ComponentFixture<SaturndayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SaturndayComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SaturndayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
