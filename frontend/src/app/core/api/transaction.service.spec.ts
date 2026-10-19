import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TransactionService } from './transaction.service';

describe('TransactionService', () => {
  let service: TransactionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TransactionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends only the filters that have a value', () => {
    service.list({ month: '2026-10', type: '', categoryId: null, q: 'coffee', page: 0, size: 20 }).subscribe();

    // expectOne with a predicate lets us inspect the query parameters.
    const req = http.expectOne((r) => r.url === '/api/transactions');
    expect(req.request.params.get('month')).toBe('2026-10');
    expect(req.request.params.get('q')).toBe('coffee');
    expect(req.request.params.get('page')).toBe('0');       // 0 is a real value, so it IS sent
    expect(req.request.params.has('type')).toBeFalse();     // '' means "no filter"
    expect(req.request.params.has('categoryId')).toBeFalse();
    req.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });
});
