import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DashboardCocina } from './dashboard-cocina';

describe('DashboardCocina', () => {
  let component: DashboardCocina;
  let fixture: ComponentFixture<DashboardCocina>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardCocina]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DashboardCocina);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
