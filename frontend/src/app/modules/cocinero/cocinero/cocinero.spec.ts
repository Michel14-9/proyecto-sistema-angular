import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Cocinero } from './cocinero';

describe('Cocinero', () => {
  let component: Cocinero;
  let fixture: ComponentFixture<Cocinero>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Cocinero]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Cocinero);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
