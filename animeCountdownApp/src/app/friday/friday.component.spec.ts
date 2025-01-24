import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FridayComponent } from './friday.component';

describe('FridayComponent', () => {
  let component: FridayComponent;
  let fixture: ComponentFixture<FridayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FridayComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FridayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
