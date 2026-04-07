import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ClientApiService } from './client-api.service';
import { environment } from '@env/environment';
import { Client, ClientRequest } from '../models/client.model';

describe('ClientApiService', () => {
  let service: ClientApiService;
  let httpMock: HttpTestingController;

  const baseUrl = `${environment.apiUrl}/api/v1/clients`;

  const mockClient: Client = {
    id: 1,
    name: 'Acme Corp',
    company: 'Acme',
    email: 'contact@acme.com',
    phone: '+55 11 99999-0000',
    logoUrl: null,
    active: true,
    createdAt: '2026-01-01T00:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ClientApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should list clients without params', () => {
    service.list().subscribe(res => expect(res).toEqual([mockClient]));

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush([mockClient]);
  });

  it('should list clients with search param', () => {
    service.list({ search: 'acme' }).subscribe(res => expect(res).toEqual([mockClient]));

    const req = httpMock.expectOne(`${baseUrl}?search=acme`);
    expect(req.request.method).toBe('GET');
    req.flush([mockClient]);
  });

  it('should list clients with active filter', () => {
    service.list({ active: true }).subscribe(res => expect(res).toEqual([mockClient]));

    const req = httpMock.expectOne(`${baseUrl}?active=true`);
    expect(req.request.method).toBe('GET');
    req.flush([mockClient]);
  });

  it('should list clients with both search and active', () => {
    service.list({ search: 'acme', active: false }).subscribe(res => expect(res).toEqual([mockClient]));

    const req = httpMock.expectOne(`${baseUrl}?search=acme&active=false`);
    expect(req.request.method).toBe('GET');
    req.flush([mockClient]);
  });

  it('should get client by id', () => {
    service.getById(1).subscribe(res => expect(res).toEqual(mockClient));

    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockClient);
  });

  it('should create a client', () => {
    const request: ClientRequest = { name: 'Acme Corp', company: 'Acme', email: 'contact@acme.com' };

    service.create(request).subscribe(res => expect(res).toEqual(mockClient));

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockClient);
  });

  it('should update a client', () => {
    const request: ClientRequest = { name: 'Acme Updated', company: 'Acme' };

    service.update(1, request).subscribe(res => expect(res).toEqual(mockClient));

    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush(mockClient);
  });

  it('should toggle active status', () => {
    const inactiveClient: Client = { ...mockClient, active: false };

    service.toggleActive(1).subscribe(res => expect(res).toEqual(inactiveClient));

    const req = httpMock.expectOne(`${baseUrl}/1/toggle`);
    expect(req.request.method).toBe('PATCH');
    req.flush(inactiveClient);
  });

  it('should upload logo (FormData)', () => {
    const file = new File(['logo'], 'logo.png', { type: 'image/png' });
    const updatedClient: Client = { ...mockClient, logoUrl: '/uploads/logo.png' };

    service.uploadLogo(1, file).subscribe(res => expect(res).toEqual(updatedClient));

    const req = httpMock.expectOne(`${baseUrl}/1/logo`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    expect((req.request.body as FormData).get('file')).toEqual(file);
    req.flush(updatedClient);
  });

  it('should remove logo', () => {
    service.removeLogo(1).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/1/logo`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
