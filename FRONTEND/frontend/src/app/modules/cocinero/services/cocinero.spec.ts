import { TestBed } from '@angular/core/testing';

import { CocineroService } from './cocinero';

describe('CocineroService', () => {
  let service: CocineroService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CocineroService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
