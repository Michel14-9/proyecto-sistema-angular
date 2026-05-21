import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Registrate } from './registrate';

describe('Registrate', () => {
  let component: Registrate;
  let fixture: ComponentFixture<Registrate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Registrate]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Registrate);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
