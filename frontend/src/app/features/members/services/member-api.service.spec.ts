import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MemberApiService } from './member-api.service';
import { environment } from '../../../../environments/environment';
import {
  MembersListResponse,
  InviteMemberRequest,
  InviteResponse,
  UpdateMemberRoleRequest,
} from '../models/member.model';

describe('MemberApiService', () => {
  let service: MemberApiService;
  let httpMock: HttpTestingController;

  const baseUrl = `${environment.apiUrl}/api/v1/members`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MemberApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should list members', () => {
    const mockResponse: MembersListResponse = { members: [], pendingInvites: [] };

    service.list().subscribe(res => expect(res).toEqual(mockResponse));

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should invite a member', () => {
    const request: InviteMemberRequest = {
      email: 'user@test.com',
      role: 'CREATIVE',
      position: 'DESIGNER_GRAFICO',
    };
    const mockResponse: InviteResponse = {
      id: 1,
      email: 'user@test.com',
      role: 'CREATIVE',
      position: 'DESIGNER_GRAFICO',
      inviteLink: 'http://localhost/invite/token123',
      expiresAt: '2026-05-01T00:00:00Z',
    };

    service.invite(request).subscribe(res => expect(res).toEqual(mockResponse));

    const req = httpMock.expectOne(`${baseUrl}/invite`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });

  it('should remove a member', () => {
    service.remove(42).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/42`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should update member role', () => {
    const request: UpdateMemberRoleRequest = { role: 'MANAGER' };

    service.updateRole(42, request).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/42/role`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(request);
    req.flush(null);
  });
});
