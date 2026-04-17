import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegistroPago } from './registro-pago';

describe('RegistroPago', () => {
  let component: RegistroPago;
  let fixture: ComponentFixture<RegistroPago>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegistroPago]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegistroPago);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
