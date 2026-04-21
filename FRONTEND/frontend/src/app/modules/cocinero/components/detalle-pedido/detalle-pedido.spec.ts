import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DetallePedidoComponent } from './detalle-pedido';

describe('DetallePedidoComponent', () => {
  let component: DetallePedidoComponent;
  let fixture: ComponentFixture<DetallePedidoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetallePedidoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DetallePedidoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
