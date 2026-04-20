import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MisCuentas } from './mis-cuentas';

describe('MisCuentas', () => {
  let component: MisCuentas;
  let fixture: ComponentFixture<MisCuentas>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MisCuentas]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MisCuentas);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
