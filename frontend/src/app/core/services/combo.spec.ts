import { TestBed } from '@angular/core/testing';

import { Combo } from './combo';

describe('Combo', () => {
  let service: Combo;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Combo);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
