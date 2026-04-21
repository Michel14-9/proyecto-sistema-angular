import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ItemPedido } from './item-pedido';

describe('ItemPedido', () => {
  let component: ItemPedido;
  let fixture: ComponentFixture<ItemPedido>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ItemPedido]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ItemPedido);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
