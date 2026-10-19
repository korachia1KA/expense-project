import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService, TOKEN_KEY } from './auth.service';

/**
 * Unit tests for AuthService.
 *
 * HttpTestingController intercepts HttpClient calls, so no real network request is made. We assert
 * on the request the service sent, then "flush" a fake response back to it.
 * provideRouter gets a 'login' route because logout() navigates there.
 */
describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([{ path: 'login', children: [] }])],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify()); // fails the test if an unexpected request was made

  it('starts logged out', () => {
    expect(service.isLoggedIn()).toBeFalse();
    expect(service.token).toBeNull();
  });

  it('stores the token and user after login', () => {
    service.login('demo@example.com', 'password123').subscribe();

    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'demo@example.com', password: 'password123' });
    req.flush({ token: 'abc.def.ghi', user: { id: 1, name: 'Demo', email: 'demo@example.com' } });

    expect(service.isLoggedIn()).toBeTrue();
    expect(service.token).toBe('abc.def.ghi');
    expect(localStorage.getItem(TOKEN_KEY)).toBe('abc.def.ghi');
    expect(service.currentUser()?.name).toBe('Demo');
  });

  it('clears everything on logout', () => {
    service.register('A', 'a@b.com', 'password1').subscribe();
    http.expectOne('/api/auth/register').flush({ token: 't', user: { id: 1, name: 'A', email: 'a@b.com' } });

    service.logout();

    expect(service.isLoggedIn()).toBeFalse();
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
  });
});
