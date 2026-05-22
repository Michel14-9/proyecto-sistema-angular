import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MetricasCocina } from './metricas-cocina';

describe('MetricasCocina', () => {
  let component: MetricasCocina;
  let fixture: ComponentFixture<MetricasCocina>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MetricasCocina]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MetricasCocina);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
