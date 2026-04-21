import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ColumnaPedidos } from './columna-pedidos';

describe('ColumnaPedidos', () => {
  let component: ColumnaPedidos;
  let fixture: ComponentFixture<ColumnaPedidos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ColumnaPedidos]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ColumnaPedidos);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
