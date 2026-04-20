import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MisDatos } from './mis-datos';

describe('MisDatos', () => {
  let component: MisDatos;
  let fixture: ComponentFixture<MisDatos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MisDatos]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MisDatos);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
