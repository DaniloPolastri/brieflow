import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { InviteApiService } from './invite-api.service';
import { environment } from '@env/environment';
import { InviteInfo, AcceptInviteRequest } from '../models/invite.model';
import { TokenResponse } from '@core/models/user.model';

describe('InviteApiService', () => {
  let service: InviteApiService;
  let httpMock: HttpTestingController;

  const baseUrl = `${environment.apiUrl}/api/v1/invite`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InviteApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should get invite info', () => {
    const token = 'abc123token';
    const mockInfo: InviteInfo = {
      workspaceName: 'Agencia XYZ',
      email: 'novo@agencia.com',
      role: 'CREATIVE',
      position: 'DESIGNER_GRAFICO',
      invitedByName: 'Danilo',
      userExists: false,
    };

    service.getInfo(token).subscribe(res => expect(res).toEqual(mockInfo));

    const req = httpMock.expectOne(`${baseUrl}/${token}`);
    expect(req.request.method).toBe('GET');
    req.flush(mockInfo);
  });

  it('should accept an invite for new user', () => {
    const token = 'abc123token';
    const request: AcceptInviteRequest = { name: 'Novo Usuario', password: 'senha123' };
    const mockResponse: TokenResponse = {
      accessToken: 'access.jwt.token',
      refreshToken: 'refresh.jwt.token',
      expiresIn: 900,
      user: {
        id: 5,
        name: 'Novo Usuario',
        email: 'novo@agencia.com',
        workspaceId: 1,
        workspaceName: 'Agencia XYZ',
        role: 'CREATIVE',
        position: 'DESIGNER_GRAFICO',
      },
    };

    service.accept(token, request).subscribe(res => expect(res).toEqual(mockResponse));

    const req = httpMock.expectOne(`${baseUrl}/${token}/accept`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });

  it('should accept an invite for existing user', () => {
    const token = 'abc123token';
    const request: AcceptInviteRequest = { password: 'senha123' };
    const mockResponse: TokenResponse = {
      accessToken: 'access.jwt.token',
      refreshToken: 'refresh.jwt.token',
      expiresIn: 900,
      user: {
        id: 3,
        name: 'Usuario Existente',
        email: 'existente@agencia.com',
        workspaceId: 1,
        workspaceName: 'Agencia XYZ',
        role: 'MANAGER',
        position: 'SOCIAL_MEDIA',
      },
    };

    service.accept(token, request).subscribe(res => expect(res).toEqual(mockResponse));

    const req = httpMock.expectOne(`${baseUrl}/${token}/accept`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });
});
