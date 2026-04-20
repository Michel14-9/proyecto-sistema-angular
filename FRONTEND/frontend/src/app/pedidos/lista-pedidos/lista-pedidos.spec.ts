import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListaPedidos } from './lista-pedidos';

describe('ListaPedidos', () => {
  let component: ListaPedidos;
  let fixture: ComponentFixture<ListaPedidos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListaPedidos]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ListaPedidos);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
