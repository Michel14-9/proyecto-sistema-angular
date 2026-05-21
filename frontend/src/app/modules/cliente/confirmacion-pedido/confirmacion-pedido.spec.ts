import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfirmacionPedido } from './confirmacion-pedido';

describe('ConfirmacionPedido', () => {
  let component: ConfirmacionPedido;
  let fixture: ComponentFixture<ConfirmacionPedido>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmacionPedido]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConfirmacionPedido);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
