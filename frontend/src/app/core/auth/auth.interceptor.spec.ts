import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { TOKEN_KEY, USER_KEY } from './auth.service';

/** Proves the interceptor adds the JWT header, and logs out on a 401 from a protected endpoint. */
describe('authInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', children: [] }]),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('adds the Bearer token when logged in', () => {
    localStorage.setItem(TOKEN_KEY, 'my-jwt');
    client.get('/api/transactions').subscribe();
    const req = http.expectOne('/api/transactions');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-jwt');
    req.flush({});
  });

  it('sends no Authorization header when logged out', () => {
    client.get('/api/transactions').subscribe();
    const req = http.expectOne('/api/transactions');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('logs the user out when the API answers 401', () => {
    localStorage.setItem(TOKEN_KEY, 'expired-jwt');
    localStorage.setItem(USER_KEY, JSON.stringify({ id: 1, name: 'A', email: 'a@b.com' }));
    client.get('/api/summary').subscribe({ error: () => undefined });
    http.expectOne('/api/summary').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
  });
});
