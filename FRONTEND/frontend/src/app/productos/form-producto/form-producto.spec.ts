import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FormProducto } from './form-producto';

describe('FormProducto', () => {
  let component: FormProducto;
  let fixture: ComponentFixture<FormProducto>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormProducto]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FormProducto);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
